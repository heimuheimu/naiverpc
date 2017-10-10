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
import com.heimuheimu.naiverpc.constant.BeanStatusEnum;
import com.heimuheimu.naiverpc.exception.RpcException;
import com.heimuheimu.naiverpc.exception.TimeoutException;
import com.heimuheimu.naiverpc.exception.TooBusyException;
import com.heimuheimu.naiverpc.net.SocketConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

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
 * 当 {@code RpcClusterClient} 中的 {@code DirectRpcClient} 被创建、关闭、恢复后，均会触发 {@link RpcClusterClientListener} 相应的事件进行通知。
 * </blockquote>
 *
 * <p><strong>说明：</strong>{@code RpcClusterClient} 类是线程安全的，可在多个线程中使用同一个实例。</p>
 *
 * @author heimuheimu
 * @see DirectRpcClient
 */
public class RpcClusterClient implements RpcClient {

    private static final Logger RPC_CONNECTION_LOG = LoggerFactory.getLogger("NAIVERPC_CONNECTION_LOG");

    private static final Logger LOG = LoggerFactory.getLogger(RpcClusterClient.class);

    /**
     * 提供 RPC 服务的主机地址数组，由主机名和端口组成，":"符号分割，例如：localhost:4182
     */
    private final String[] hosts;

    /**
     * RPC 服务提供方最后一次从不可用状态中恢复的时间戳列表，该列表顺序、大小与 {@link #hosts} 一致，如果一直保持可用，在列表中的值为 0
     */
    private final AtomicLongArray rescueTimeArray;

    /**
     * 记录已获取 {@code DirectRpcClient} 的次数，用于做负载均衡
     */
    private final AtomicLong count = new AtomicLong(0);

    /**
     * {@code DirectRpcClient} 列表，该列表顺序、大小与 {@link #hosts} 一致，如果某个 {@code host} 当前不可用，其在列表中的值为 {@code null}
     */
    private final CopyOnWriteArrayList<DirectRpcClient> clientList = new CopyOnWriteArrayList<>();

    /**
     * 当前可用的 {@code DirectRpcClient} 列表
     */
    private final CopyOnWriteArrayList<DirectRpcClient> aliveClientList = new CopyOnWriteArrayList<>();

    /**
     * 创建 {@code DirectRpcClient} 使用的 {@link Socket} 配置信息，允许为 {@code null}
     */
    private final SocketConfiguration configuration;

    /**
     * 创建 {@code DirectRpcClient} 使用的 RPC 调用超时时间，单位：毫秒，不能小于等于 0
     */
    private final int timeout;

    /**
     * 创建 {@code DirectRpcClient} 使用的最小压缩字节数，不能小于等于 0
     */
    private final int compressionThreshold;

    /**
     * 创建 {@code DirectRpcClient} 使用的 RPC 执行过慢最小时间，单位：毫秒，不能小于等于 0
     */
    private final int slowExecutionThreshold;

    /**
     * 创建 {@code DirectRpcClient} 使用的心跳检测时间，单位：秒
     */
    private final int heartbeatPeriod;

    /**
     * 创建 {@code DirectRpcClient} 使用的 {@code DirectRpcClient} 事件监听器，允许为 {@code null}
     */
    private final DirectRpcClientListener directRpcClientListener;

    /**
     * {@code RpcClusterClient} 事件监听器，允许为 {@code null}
     */
    private final RpcClusterClientListener rpcClusterClientListener;

    /**
     * RPC 服务提供方恢复任务是否运行
     */
    private boolean isRescueTaskRunning = false;

    /**
     * RPC 服务提供方恢复任务使用的私有锁
     */
    private final Object rescueTaskLock = new Object();

    /**
     * 当前集群客户端所处状态
     */
    private volatile BeanStatusEnum state = BeanStatusEnum.NORMAL;

    /**
     * 构造一个 RPC 服务调用方使用的集群客户端，创建 {@code DirectRpcClient} 时， {@link Socket} 配置信息使用 {@link SocketConfiguration#DEFAULT}，
     * RPC 调用超时时间设置为 5 秒，最小压缩字节数设置为 64 KB，RPC 执行过慢最小时间设置为 50 毫秒，心跳检测时间设置为 30 秒。
     *
     * @param hosts 提供 RPC 服务的主机地址数组，由主机名和端口组成，":"符号分割，例如：localhost:4182，不允许为 {@code null} 或空数组
     * @throws IllegalArgumentException 如果提供 RPC 服务的主机地址数组为 {@code null} 或空数组，将会抛出此异常
     * @throws IllegalStateException 如果所有提供 RPC 服务的主机地址都不可用，将会抛出此异常
     * @see DirectRpcClient
     */
    public RpcClusterClient(String[] hosts, DirectRpcClientListener directRpcClientListener, RpcClusterClientListener rpcClusterClientListener)
            throws IllegalArgumentException, IllegalStateException {
        this(hosts, null, 5000, 64 * 1024, 50, 30, directRpcClientListener, rpcClusterClientListener);
    }

    /**
     * 构造一个 RPC 服务调用方使用的集群客户端。
     *
     * @param hosts 提供 RPC 服务的主机地址数组，由主机名和端口组成，":"符号分割，例如：localhost:4182，不允许为 {@code null} 或空数组
     * @param configuration 创建 {@code DirectRpcClient} 使用的 {@link Socket} 配置信息，允许为 {@code null}
     * @param timeout 创建 {@code DirectRpcClient} 使用的 RPC 调用超时时间，单位：毫秒，不能小于等于 0
     * @param compressionThreshold 创建 {@code DirectRpcClient} 使用的最小压缩字节数，不能小于等于 0
     * @param slowExecutionThreshold 🈵️创建 {@code DirectRpcClient} 使用的 RPC 执行过慢最小时间，单位：毫秒，不能小于等于 0
     * @param heartbeatPeriod 创建 {@code DirectRpcClient} 使用的心跳检测时间，单位：秒
     * @param directRpcClientListener 创建 {@code DirectRpcClient} 使用的 {@code DirectRpcClient} 事件监听器，允许为 {@code null}
     * @param rpcClusterClientListener {@code RpcClusterClient} 事件监听器，允许为 {@code null}
     * @throws IllegalArgumentException 如果提供 RPC 服务的主机地址数组为 {@code null} 或空数组，将会抛出此异常
     * @throws IllegalArgumentException 如果 RPC 调用超时时间小于等于 0，将会抛出此异常
     * @throws IllegalArgumentException 如果最小压缩字节数小于等于 0，将会抛出此异常
     * @throws IllegalArgumentException 如果 RPC 执行过慢最小时间小于等于 0，将会抛出此异常
     * @throws IllegalStateException  如果所有提供 RPC 服务的主机地址都不可用，将会抛出此异常
     */
    public RpcClusterClient(String[] hosts, SocketConfiguration configuration, int timeout, int compressionThreshold,
            int slowExecutionThreshold, int heartbeatPeriod, DirectRpcClientListener directRpcClientListener,
            RpcClusterClientListener rpcClusterClientListener) throws IllegalArgumentException, IllegalStateException {
        if (hosts == null || hosts.length == 0) {
            LOG.error("Create RpcClusterClient failed: `hosts could not be empty`. Hosts: `" + Arrays.toString(hosts)
                    + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout + "`. CompressionThreshold: `"
                    + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold + "`. HeartbeatPeriod: `"
                    + heartbeatPeriod + "`. DirectRpcClientListener: `" + directRpcClientListener + "`. RpcClusterClientListener: `"
                    + rpcClusterClientListener + "`.");
            throw new IllegalArgumentException("Create RpcClusterClient failed: `hosts could not be empty`. Hosts: `" + Arrays.toString(hosts)
                    + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout + "`. CompressionThreshold: `"
                    + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold + "`. HeartbeatPeriod: `"
                    + heartbeatPeriod + "`. DirectRpcClientListener: `" + directRpcClientListener + "`. RpcClusterClientListener: `"
                    + rpcClusterClientListener + "`.");
        }
        if (timeout <= 0) {
            LOG.error("Create RpcClusterClient failed: `timeout could not be equal or less than 0`. Hosts: `" + Arrays.toString(hosts)
                    + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout + "`. CompressionThreshold: `"
                    + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold + "`. HeartbeatPeriod: `"
                    + heartbeatPeriod + "`. DirectRpcClientListener: `" + directRpcClientListener + "`. RpcClusterClientListener: `"
                    + rpcClusterClientListener + "`.");
            throw new IllegalArgumentException("Create RpcClusterClient failed: `timeout could not be equal or less than 0`. Hosts: `" + Arrays.toString(hosts)
                    + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout + "`. CompressionThreshold: `"
                    + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold + "`. HeartbeatPeriod: `"
                    + heartbeatPeriod + "`. DirectRpcClientListener: `" + directRpcClientListener + "`. RpcClusterClientListener: `"
                    + rpcClusterClientListener + "`.");
        }
        if (compressionThreshold <= 0) {
            LOG.error("Create RpcClusterClient failed: `compressionThreshold could not be equal or less than 0`. Hosts: `" + Arrays.toString(hosts)
                    + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout + "`. CompressionThreshold: `"
                    + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold + "`. HeartbeatPeriod: `"
                    + heartbeatPeriod + "`. DirectRpcClientListener: `" + directRpcClientListener + "`. RpcClusterClientListener: `"
                    + rpcClusterClientListener + "`.");
            throw new IllegalArgumentException("Create RpcClusterClient failed: `compressionThreshold could not be equal or less than 0`. Hosts: `" + Arrays.toString(hosts)
                    + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout + "`. CompressionThreshold: `"
                    + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold + "`. HeartbeatPeriod: `"
                    + heartbeatPeriod + "`. DirectRpcClientListener: `" + directRpcClientListener + "`. RpcClusterClientListener: `"
                    + rpcClusterClientListener + "`.");
        }
        if (slowExecutionThreshold <= 0) {
            LOG.error("Create RpcClusterClient failed: `slowExecutionThreshold could not be equal or less than 0`. Hosts: `" + Arrays.toString(hosts)
                    + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout + "`. CompressionThreshold: `"
                    + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold + "`. HeartbeatPeriod: `"
                    + heartbeatPeriod + "`. DirectRpcClientListener: `" + directRpcClientListener + "`. RpcClusterClientListener: `"
                    + rpcClusterClientListener + "`.");
            throw new IllegalArgumentException("Create RpcClusterClient failed: `slowExecutionThreshold could not be equal or less than 0`. Hosts: `" + Arrays.toString(hosts)
                    + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout + "`. CompressionThreshold: `"
                    + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold + "`. HeartbeatPeriod: `"
                    + heartbeatPeriod + "`. DirectRpcClientListener: `" + directRpcClientListener + "`. RpcClusterClientListener: `"
                    + rpcClusterClientListener + "`.");
        }
        this.hosts = hosts;
        this.rescueTimeArray = new AtomicLongArray(hosts.length);
        this.configuration = configuration;
        this.timeout = timeout;
        this.compressionThreshold = compressionThreshold;
        this.slowExecutionThreshold = slowExecutionThreshold;
        this.heartbeatPeriod = heartbeatPeriod;
        this.directRpcClientListener = directRpcClientListener;
        this.rpcClusterClientListener = rpcClusterClientListener;
        for (String host : hosts) {
            boolean isSuccess = createRpcClient(-1, host);
            if (isSuccess) {
                RPC_CONNECTION_LOG.info("Add `{}` to cluster is success. Hosts: `{}`.", host, Arrays.toString(hosts));
                if (rpcClusterClientListener != null) {
                    try {
                        rpcClusterClientListener.onCreated(host);
                    } catch (Exception e) {
                        LOG.error("Call RpcClusterClientListener#onCreated() failed. Host: `" + host + "`. Hosts: `" + Arrays.toString(hosts) + "`.", e);
                    }
                }
            } else {
                RPC_CONNECTION_LOG.error("Add `{}` to cluster is failed. Hosts: `{}`.", host, Arrays.toString(hosts));
                if (rpcClusterClientListener != null) {
                    try {
                        rpcClusterClientListener.onClosed(host, false);
                    } catch (Exception e) {
                        LOG.error("Call RpcClusterClientListener#onClosed() failed. Host: `" + host + "`. Hosts: `" + Arrays.toString(hosts) + "`.", e);
                    }
                }
            }
        }
        if (aliveClientList.isEmpty()) {
            LOG.error("Create RpcClusterClient failed: `there is no active DirectRpcClient`. Hosts: `" + Arrays.toString(hosts)
                    + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout + "`. CompressionThreshold: `"
                    + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold + "`. HeartbeatPeriod: `"
                    + heartbeatPeriod + "`. DirectRpcClientListener: `" + directRpcClientListener + "`. RpcClusterClientListener: `"
                    + rpcClusterClientListener + "`.");
            throw new IllegalStateException("Create RpcClusterClient failed: `there is no active DirectRpcClient`. Hosts: `" + Arrays.toString(hosts)
                    + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout + "`. CompressionThreshold: `"
                    + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold + "`. HeartbeatPeriod: `"
                    + heartbeatPeriod + "`. DirectRpcClientListener: `" + directRpcClientListener + "`. RpcClusterClientListener: `"
                    + rpcClusterClientListener + "`.");
        }
    }

    @Override
    public Object execute(Method method, Object[] args) throws IllegalStateException, TimeoutException, TooBusyException, RpcException {
        return execute(method, args, timeout);
    }

    @Override
    public Object execute(Method method, Object[] args, long timeout) throws IllegalStateException, TimeoutException, TooBusyException, RpcException {
        return execute(method, args, timeout, 3);
    }

    private Object execute(Method method, Object[] args, long timeout, int tooBusyRetryTimes) throws IllegalStateException, TimeoutException, TooBusyException, RpcException {
        if (state == BeanStatusEnum.NORMAL) {
            DirectRpcClient client = getClient();
            if (client != null) {
                LOG.debug("Choose DirectRpcClient success. Host: `{}`. Method: `{}`. Arguments: `{}`. Hosts: `{}`.",
                        client.getHost(), method, args, hosts);
                try {
                    return client.execute(method, args, timeout);
                } catch (TooBusyException ex) {
                    if (tooBusyRetryTimes > 0) {
                        --tooBusyRetryTimes;
                        LOG.error("RPC execute failed: `too busy, left retry times: {}`. Host: `{}`. Method: `{}`. Arguments: `{}`. Hosts: `{}`.",
                                tooBusyRetryTimes, client.getHost(), method, args, hosts);
                        return execute(method, args, timeout, tooBusyRetryTimes);
                    } else {
                        LOG.error("RPC execute failed: `too busy, no more retry`. Host: `{}`. Method: `{}`. Arguments: `{}`. Hosts: `{}`.",
                                client.getHost(), method, args, hosts);
                        throw ex;
                    }
                }
            } else {
                LOG.error("RPC execute failed: `there is no active DirectRpcClient`. Method: `" + method + "`. Arguments: `"
                        + Arrays.toString(args) + "`. Timeout: `" + timeout + "`. Hosts: `" + Arrays.toString(hosts) + "`.");
                throw new IllegalStateException("RPC execute failed: `there is no active DirectRpcClient`. Method: `" + method
                        + "`. Arguments: `" + Arrays.toString(args) + "`. Timeout: `" + timeout + "`. Hosts: `" + Arrays.toString(hosts) + "`.");
            }
        } else {
            LOG.error("RPC execute failed: `RpcClusterClient has been closed`. Method: `" + method + "`. Arguments: `"
                    + Arrays.toString(args) + "`. Timeout: `" + timeout + "`. Hosts: `" + Arrays.toString(hosts) + "`.");
            throw new IllegalStateException("RPC execute failed: `RpcClusterClient has been closed`. Method: `" + method + "`. Arguments: `"
                    + Arrays.toString(args) + "`. Timeout: `" + timeout + "`. Hosts: `" + Arrays.toString(hosts) + "`.");
        }
    }

    @Override
    public synchronized void close() {
        if (state != BeanStatusEnum.CLOSED) {
            state = BeanStatusEnum.CLOSED;
            for (DirectRpcClient rpcClient : aliveClientList) {
                try {
                    rpcClient.close();
                } catch (Exception e) {
                    LOG.error("Close `" + rpcClient.getHost() + "` failed. Hosts: `" + Arrays.toString(hosts) + "`.", e);
                }
            }
            RPC_CONNECTION_LOG.info("RpcClusterClient has been closed. Hosts: `{}`.", Arrays.toString(hosts));
        }
    }

    @Override
    public String toString() {
        return "RpcClusterClient{" +
                "hosts=" + Arrays.toString(hosts) +
                ", rescueTimeArray=" + rescueTimeArray +
                ", count=" + count +
                ", configuration=" + configuration +
                ", timeout=" + timeout +
                ", compressionThreshold=" + compressionThreshold +
                ", slowExecutionThreshold=" + slowExecutionThreshold +
                ", heartbeatPeriod=" + heartbeatPeriod +
                ", directRpcClientListener=" + directRpcClientListener +
                ", rpcClusterClientListener=" + rpcClusterClientListener +
                ", isRescueTaskRunning=" + isRescueTaskRunning +
                ", rescueTaskLock=" + rescueTaskLock +
                ", state=" + state +
                '}';
    }

    private boolean createRpcClient(int clientIndex, String host) {
        DirectRpcClient client = null;
        try {
            client = new DirectRpcClient(host, configuration, timeout, compressionThreshold, slowExecutionThreshold, heartbeatPeriod, directRpcClientListener);
        } catch (Exception e) {
            LOG.error("Create DirectRpcClient for RpcClusterClient failed. Host: `" + host + "`. Hosts: `" + Arrays.toString(hosts) + "`.", e);
        }
        if (client != null && client.isActive()) {
            aliveClientList.add(client);
            if (clientIndex < 0) {
                clientList.add(client);
            } else {
                clientList.set(clientIndex, client);
            }
            return true;
        } else {
            if (clientIndex < 0) {
                clientList.add(null);
            } else {
                clientList.set(clientIndex, null);
            }
            return false;
        }
    }

    private DirectRpcClient getClient() {
        int clientIndex = (int) (Math.abs(count.incrementAndGet()) % hosts.length);
        DirectRpcClient client = clientList.get(clientIndex);
        if (client != null) {
            if (!client.isActive()) {
                boolean isRemoveSuccess = aliveClientList.remove(client);
                if (isRemoveSuccess) {
                    clientList.set(clientIndex, null);
                    if (rpcClusterClientListener != null) {
                        try {
                            rpcClusterClientListener.onClosed(client.getHost(), client.isOffline());
                        } catch (Exception e) {
                            LOG.error("Call RpcClusterClientListener#onClosed() failed. Host: `" + client.getHost() + "`. Hosts: `" + Arrays.toString(hosts) + "`.", e);
                        }
                    }
                    RPC_CONNECTION_LOG.error("Remove `" + client.getHost() + "` from cluster. Hosts: `" + Arrays.toString(hosts) + "`.");
                }
                client = null;
            } else if (isSkipThisRound(clientIndex)) {
                LOG.debug("DirectRpcClient skip this round: `{}`. Client index: `{}`. Hosts: `{}`.", client.getHost(), clientIndex, Arrays.toString(hosts));
                client = null;
            }
        }
        //如果该 RPC 服务调用客户端暂时不可用，则从可用池里面随机挑选
        if (client == null) {
            int aliveClientSize = aliveClientList.size();
            if (aliveClientSize > 0) {
                int aliveClientIndex = (new Random()).nextInt(aliveClientSize);
                try {
                    client = aliveClientList.get(aliveClientIndex);
                } catch (IndexOutOfBoundsException e) { //should not happen
                    LOG.error("No active DirectRpcClient due to IndexOutOfBoundsException. Index: `" + aliveClientIndex + "`. Size: `"
                            + aliveClientList.size() + "`. Hosts: `" + Arrays.toString(hosts) + "`.");
                }
            }

            if (aliveClientList.size() < hosts.length) {
                startRescueTask();
            }
        }
        return client;
    }

    /**
     * 判断该位置的 RPC 服务调用客户端是否轮空本轮使用，用于保护刚恢复的客户端突然进入太多请求
     *
     * @param clientIndex RPC 服务调用客户端位置索引
     * @return 是否轮空本轮使用
     */
    private boolean isSkipThisRound(int clientIndex) {
        long rescueTime = rescueTimeArray.get(clientIndex);
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

    /**
     * 启动 RPC 调用客户端重连恢复任务
     */
    private void startRescueTask() {
        if (state == BeanStatusEnum.NORMAL) {
            synchronized (rescueTaskLock) {
                if (!isRescueTaskRunning) {
                    Thread rescueThread = new Thread() {

                        @Override
                        public void run() {
                            long startTime = System.currentTimeMillis();
                            RPC_CONNECTION_LOG.info("RpcClusterClient rescue task has been started. Hosts: `{}`", Arrays.toString(hosts));
                            try {
                                while (state == BeanStatusEnum.NORMAL &&
                                        aliveClientList.size() < hosts.length) {
                                    for (int i = 0; i < hosts.length; i++) {
                                        if (clientList.get(i) == null) {
                                            boolean isSuccess = createRpcClient(i, hosts[i]);
                                            if (isSuccess) {
                                                rescueTimeArray.set(i, System.currentTimeMillis());
                                                RPC_CONNECTION_LOG.info("Rescue `{}` to cluster success. Hosts: `{}`.", hosts[i], Arrays.toString(hosts));
                                                if (rpcClusterClientListener != null) {
                                                    try {
                                                        rpcClusterClientListener.onRecovered(hosts[i]);
                                                    } catch (Exception e) {
                                                        LOG.error("Call RpcClusterClientListener#onRecovered() failed. Host: `" + hosts[i] + "`. Hosts: `" + Arrays.toString(hosts) + "`.", e);
                                                    }
                                                }
                                            } else {
                                                RPC_CONNECTION_LOG.warn("Rescue `{}` to cluster failed. Hosts: `{}`.", hosts[i], Arrays.toString(hosts));
                                            }
                                        }
                                    }
                                    if (aliveClientList.size() < hosts.length) {
                                        Thread.sleep(5000); //delay 5000ms
                                    }
                                }
                                rescueOver();
                                RPC_CONNECTION_LOG.info("RpcClusterClient rescue task has been finished. Cost: {}ms. Hosts: `{}`",
                                        System.currentTimeMillis() - startTime, hosts);
                            } catch (Exception e) {
                                rescueOver();
                                RPC_CONNECTION_LOG.info("RpcClusterClient rescue task executed failed: `{}`. Cost: {}ms. Hosts: `{}`",
                                        e.getMessage(), System.currentTimeMillis() - startTime, hosts);
                                LOG.error("RpcClusterClient rescue task executed failed. Hosts: `" + Arrays.toString(hosts) + "`", e);
                            }
                        }

                        private void rescueOver() {
                            synchronized (rescueTaskLock) {
                                isRescueTaskRunning = false;
                            }
                        }

                    };
                    rescueThread.setName("naiverpc-cluster-client-rescue-task");
                    rescueThread.setDaemon(true);
                    rescueThread.start();
                    isRescueTaskRunning = true;
                }
            }
        }
    }

}
