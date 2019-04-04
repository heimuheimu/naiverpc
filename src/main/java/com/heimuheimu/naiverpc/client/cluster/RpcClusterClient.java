/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 heimuheimu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.heimuheimu.naiverpc.client.cluster;

import com.heimuheimu.naiverpc.client.DirectRpcClient;
import com.heimuheimu.naiverpc.client.DirectRpcClientListener;
import com.heimuheimu.naiverpc.client.RpcClient;
import com.heimuheimu.naiverpc.exception.RpcException;
import com.heimuheimu.naiverpc.exception.TimeoutException;
import com.heimuheimu.naiverpc.exception.TooBusyException;
import com.heimuheimu.naiverpc.facility.clients.DirectRpcClientList;
import com.heimuheimu.naiverpc.facility.clients.DirectRpcClientListListener;
import com.heimuheimu.naiverpc.monitor.client.RpcClusterClientMonitor;
import com.heimuheimu.naiverpc.net.SocketConfiguration;
import com.heimuheimu.naiverpc.util.LogBuildUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RPC 服务调用方使用的集群客户端，RPC 调用请求将根据轮询策略（Round-Robin）调度至相应的 {@link DirectRpcClient} 中执行。
 *
 * <p>
 *     当 {@code RpcClusterClient} 不再使用时，应调用 {@link #close()} 方法进行资源释放。
 * </p>
 *
 * <h3>可用性</h3>
 * <blockquote>
 * {@code RpcClusterClient} 中不可用的 {@code DirectRpcClient} 将会被自动移除，并启动恢复线程尝试进行恢复，如果恢复失败，等待下一次恢复的周期为 5 秒。<br>
 * 刚恢复的 {@code DirectRpcClient} 存在一分钟的保护期，保护期内，该直连客户端的 RPC 调用量以 15 秒为周期逐步增加。
 * </blockquote>
 *
 * <h3>监听器</h3>
 * <blockquote>
 * 当 {@code RpcClusterClient} 中的 {@code DirectRpcClient} 被创建、关闭、恢复后，均会触发 {@link DirectRpcClientListener} 相应的事件进行通知。
 * </blockquote>
 *
 * <p><strong>说明：</strong>{@code RpcClusterClient} 类是线程安全的，可在多个线程中使用同一个实例。</p>
 *
 * @author heimuheimu
 * @see DirectRpcClient
 */
public class RpcClusterClient implements RpcClient {

    private static final Logger LOG = LoggerFactory.getLogger(RpcClusterClient.class);

    /**
     * 提供 RPC 服务的主机地址数组，由主机名和端口组成，":"符号分割，例如：localhost:4182
     */
    private final String[] hosts;

    /**
     * RPC 直连客户端列表
     */
    private final DirectRpcClientList directRpcClientList;

    /**
     * 记录已获取 {@code DirectRpcClient} 的次数，用于做负载均衡
     */
    private final AtomicLong count = new AtomicLong(0);

    /**
     * RPC 集群客户端信息监控器
     */
    private final RpcClusterClientMonitor rpcClusterClientMonitor = RpcClusterClientMonitor.getInstance();

    /**
     * 构造一个 RPC 服务调用方使用的集群客户端，创建 {@code DirectRpcClient} 时， {@link Socket} 配置信息使用 {@link SocketConfiguration#DEFAULT}，
     * RPC 调用超时时间设置为 5 秒，最小压缩字节数设置为 64 KB，RPC 调用过慢最小时间设置为 50 毫秒，心跳检测时间设置为 30 秒。
     *
     * @param hosts 提供 RPC 服务的主机地址数组，由主机名和端口组成，":"符号分割，例如：localhost:4182，不允许为 {@code null} 或空数组
     * @param directRpcClientListener 创建 {@code DirectRpcClient} 使用的 {@code DirectRpcClient} 事件监听器，允许为 {@code null}
     * @param directRpcClientListListener {@link DirectRpcClientList} 事件监听器，允许为 {@code null}
     * @throws IllegalStateException 如果所有提供 RPC 服务的主机地址都不可用，将会抛出此异常
     * @see DirectRpcClientList
     */
    public RpcClusterClient(String[] hosts, DirectRpcClientListener directRpcClientListener,
                            DirectRpcClientListListener directRpcClientListListener) throws IllegalStateException {
        this(hosts, null, 5000, 64 * 1024, 50, 30, directRpcClientListener, directRpcClientListListener);
    }

    /**
     * 构造一个 RPC 服务调用方使用的集群客户端。
     *
     * @param hosts 提供 RPC 服务的主机地址数组，由主机名和端口组成，":"符号分割，例如：localhost:4182，不允许为 {@code null} 或空数组
     * @param configuration 创建 {@code DirectRpcClient} 使用的 {@link Socket} 配置信息，允许为 {@code null}
     * @param timeout 创建 {@code DirectRpcClient} 使用的 RPC 调用超时时间，单位：毫秒，不能小于等于 0
     * @param compressionThreshold 创建 {@code DirectRpcClient} 使用的最小压缩字节数，不能小于等于 0
     * @param slowExecutionThreshold 创建 {@code DirectRpcClient} 使用的 RPC 调用过慢最小时间，单位：毫秒，不能小于等于 0
     * @param heartbeatPeriod 创建 {@code DirectRpcClient} 使用的心跳检测时间，单位：秒，如果该值小于等于 0，则不进行检测
     * @param directRpcClientListener 创建 {@code DirectRpcClient} 使用的 {@code DirectRpcClient} 事件监听器，允许为 {@code null}
     * @param directRpcClientListListener {@link DirectRpcClientList} 事件监听器，允许为 {@code null}
     * @throws IllegalStateException  如果所有提供 RPC 服务的主机地址都不可用，将会抛出此异常
     * @see DirectRpcClientList
     */
    public RpcClusterClient(String[] hosts, SocketConfiguration configuration, int timeout, int compressionThreshold,
                            int slowExecutionThreshold, int heartbeatPeriod, DirectRpcClientListener directRpcClientListener,
                            DirectRpcClientListListener directRpcClientListListener) throws IllegalStateException {
        this.hosts = hosts;
        this.directRpcClientList = new DirectRpcClientList("RpcClusterClient", hosts, configuration, timeout, compressionThreshold,
                slowExecutionThreshold, heartbeatPeriod, directRpcClientListener, directRpcClientListListener);
    }

    @Override
    public Object execute(Method method, Object[] args) throws IllegalStateException, TimeoutException, TooBusyException, RpcException {
        return execute(method, args, -1);
    }

    @Override
    public Object execute(Method method, Object[] args, long timeout) throws IllegalStateException, TimeoutException, TooBusyException, RpcException {
        return execute(method, args, timeout, 3);
    }

    @Override
    public void close() {
        directRpcClientList.close();
    }

    @Override
    public String toString() {
        return "RpcClusterClient{" +
                "hosts=" + Arrays.toString(hosts) +
                ", directRpcClientList=" + directRpcClientList +
                ", count=" + count +
                '}';
    }

    private Object execute(Method method, Object[] args, long timeout, int tooBusyRetryTimes)
            throws IllegalStateException, TimeoutException, TooBusyException, RpcException {
        LinkedHashMap<String, Object> parameterMap = new LinkedHashMap<>();
        parameterMap.put("method", method);
        parameterMap.put("args", args);
        parameterMap.put("timeout", timeout);
        parameterMap.put("tooBusyRetryTimes", tooBusyRetryTimes);

        DirectRpcClient client = getClient(parameterMap);
        try {
            if (timeout > 0) {
                return client.execute(method, args, timeout);
            } else {
                return client.execute(method, args);
            }
        } catch (TooBusyException ex) {
            if (tooBusyRetryTimes > 0) {
                --tooBusyRetryTimes;
                LOG.error("RPC execute failed: `too busy, left retry times: {}`. Host: `{}`. Method: `{}`. Arguments: `{}`. Hosts: `{}`.",
                        tooBusyRetryTimes, client.getHost(), method, args, hosts); // lgtm [java/print-array]
                return execute(method, args, timeout, tooBusyRetryTimes);
            } else {
                LOG.error("RPC execute failed: `too busy, no more retry`. Host: `{}`. Method: `{}`. Arguments: `{}`. Hosts: `{}`.",
                        client.getHost(), method, args, hosts); // lgtm [java/print-array]
                throw ex;
            }
        }
    }

    /**
     * 获得本次使用的 RPC 服务调用客户端。
     *
     * @param parameterMap 方法执行参数
     * @return RPC 服务调用客户端
     * @throws IllegalStateException 如果没有可用的 RPC 服务调用客户端，将抛出此异常
     */
    private DirectRpcClient getClient(Map<String, Object> parameterMap) throws IllegalStateException {
        DirectRpcClient client = directRpcClientList.orAvailableClient(getClientIndex());
        if (client == null || !client.isActive()) {
            String errorMessage = LogBuildUtil.buildMethodExecuteFailedLog("RpcClusterClient#execute(Method method, Object[] args, long timeout)",
                    "no available client", parameterMap);
            LOG.error(errorMessage);
            rpcClusterClientMonitor.onUnavailable();
            throw new IllegalStateException(errorMessage);
        }
        return client;
    }

    /**
     * 获得本次使用的 RPC 服务调用客户端索引。
     *
     * @return RPC 服务调用客户端索引
     */
    private int getClientIndex() {
        int clientIndex = 0;
        int currentRetryTimes = 0;
        int maxRetryTimes = hosts.length;
        while (currentRetryTimes++ < maxRetryTimes) {
            clientIndex = (int) (Math.abs(count.incrementAndGet()) % hosts.length);
            if (!isSkipThisRound(clientIndex)) {
                break;
            } else {
                LOG.debug("DirectRpcClient skip this round: `{}`. `clientIndex`:`{}`. `hosts`:`{}`.", hosts[clientIndex], clientIndex, Arrays.toString(hosts));
            }
        }
        return clientIndex;
    }

    /**
     * 判断该位置的 RPC 服务调用客户端是否轮空本轮使用，用于保护刚恢复的客户端突然进入太多请求。
     *
     * @param clientIndex RPC 服务调用客户端位置索引
     * @return 是否轮空本轮使用
     */
    private boolean isSkipThisRound(int clientIndex) {
        long rescueTime = directRpcClientList.getRescueTime(clientIndex);
        long intervalTime = System.currentTimeMillis() - rescueTime;
        if (intervalTime < 60000) { //在 60 秒以内，才有可能轮空
            double randomNum = Math.random();
            if (intervalTime < 15000) { //在 15 秒内，70% 概率轮空
                return randomNum > 0.3;
            } else if (intervalTime < 30000) {//在 15 - 30 秒内，50% 概率轮空
                return randomNum > 0.5;
            } else if (intervalTime < 45000) { //在 30 - 45 秒内，30% 概率轮空
                return randomNum > 0.7;
            } else { //在 45 - 60 秒内，10% 概率轮空
                return randomNum > 0.9;
            }
        }
        return false;
    }
}
