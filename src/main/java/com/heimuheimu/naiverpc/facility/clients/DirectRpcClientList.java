/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 heimuheimu
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

package com.heimuheimu.naiverpc.facility.clients;

import com.heimuheimu.naiverpc.client.DirectRpcClient;
import com.heimuheimu.naiverpc.client.DirectRpcClientListener;
import com.heimuheimu.naiverpc.constant.BeanStatusEnum;
import com.heimuheimu.naiverpc.facility.Methods;
import com.heimuheimu.naiverpc.net.SocketConfiguration;
import com.heimuheimu.naiverpc.util.LogBuildUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * RPC 直连客户端列表，提供客户端自动恢复功能。
 *
 * <h3>连接信息日志 Log4j 配置</h3>
 * <strong>注意：</strong> <code>${log.output.directory}</code> 为占位替换符，请自行定义。
 * <blockquote>
 * <pre>
 * log4j.logger.NAIVERPC_CONNECTION_LOG=INFO, NAIVERPC_CONNECTION_LOG
 * log4j.additivity.NAIVERPC_CONNECTION_LOG=false
 * log4j.appender.NAIVERPC_CONNECTION_LOG=org.apache.log4j.DailyRollingFileAppender
 * log4j.appender.NAIVERPC_CONNECTION_LOG.file=${log.output.directory}/naiverpc/connection.log
 * log4j.appender.NAIVERPC_CONNECTION_LOG.encoding=UTF-8
 * log4j.appender.NAIVERPC_CONNECTION_LOG.DatePattern=_yyyy-MM-dd
 * log4j.appender.NAIVERPC_CONNECTION_LOG.layout=org.apache.log4j.PatternLayout
 * log4j.appender.NAIVERPC_CONNECTION_LOG.layout.ConversionPattern=%d{ISO8601} %-5p : %m%n
 * </pre>
 * </blockquote>
 *
 * <p><strong>说明：</strong>{@code DirectRpcClientList} 类是线程安全的，可在多个线程中使用同一个实例。</p>
 *
 * @author heimuheimu
 */
public class DirectRpcClientList implements Closeable {

    private static final Logger RPC_CONNECTION_LOG = LoggerFactory.getLogger("NAIVERPC_CONNECTION_LOG");

    private static final Logger LOG = LoggerFactory.getLogger(DirectRpcClientList.class);

    /**
     * RPC 直连客户端列表名称
     */
    private final String name;

    /**
     * 提供 RPC 服务的主机地址数组，由主机名和端口组成，":"符号分割，例如：localhost:4182
     */
    private final String[] hosts;

    /**
     * RPC 服务提供方最后一次从不可用状态中恢复的时间戳列表，该列表顺序、大小与 {@link #hosts} 一致，如果一直保持可用，在列表中的值为 0
     */
    private final AtomicLongArray rescueTimeArray;

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
     * 创建 {@code DirectRpcClient} 使用的 RPC 调用过慢最小时间，单位：毫秒，不能小于等于 0
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
     * RPC 直连客户端列表事件监听器
     */
    private final DirectRpcClientListListener listener;

    /**
     * RPC 直连客户端列表，该列表顺序、大小与 {@link #hosts} 一致
     * <p>
     *     如果某个 RPC 直连客户端不可用，该客户端在列表中的值为 {@code null}
     * </p>
     */
    private final CopyOnWriteArrayList<DirectRpcClient> clientList = new CopyOnWriteArrayList<>();

    /**
     * {@link #clientList} 元素发生变更操作时，使用的私有锁
     */
    private final Object clientListUpdateLock = new Object();

    /**
     * RPC 直连客户端恢复任务是否运行
     */
    private boolean isRescueTaskRunning = false;

    /**
     * RPC 直连客户端恢复任务使用的私有锁
     */
    private final Object rescueTaskLock = new Object();

    /**
     * RPC 直连客户端列表所处状态
     */
    private volatile BeanStatusEnum state = BeanStatusEnum.NORMAL;

    /**
     * 构造一个 RPC 直连客户端列表，提供客户端自动恢复功能。
     *
     * @param name RPC 直连客户端列表名称
     * @param hosts 提供 RPC 服务的主机地址数组，由主机名和端口组成，":"符号分割，例如：localhost:4182
     * @param configuration 创建 {@code DirectRpcClient} 使用的 {@link Socket} 配置信息，允许为 {@code null}
     * @param timeout 创建 {@code DirectRpcClient} 使用的 RPC 调用超时时间，单位：毫秒，不能小于等于 0
     * @param compressionThreshold 创建 {@code DirectRpcClient} 使用的最小压缩字节数，不能小于等于 0
     * @param slowExecutionThreshold 创建 {@code DirectRpcClient} 使用的 RPC 调用过慢最小时间，单位：毫秒，不能小于等于 0
     * @param heartbeatPeriod 创建 {@code DirectRpcClient} 使用的心跳检测时间，单位：秒，如果该值小于等于 0，则不进行检测
     * @param directRpcClientListener 创建 {@code DirectRpcClient} 使用的 {@code DirectRpcClient} 事件监听器，允许为 {@code null}
     * @param listener RPC 直连客户端列表事件监听器，允许为 {@code null}
     * @throws IllegalStateException 如果所有 RPC 直连客户端均不可用，将会抛出此异常
     */
    public DirectRpcClientList(String name, String[] hosts, SocketConfiguration configuration, int timeout, int compressionThreshold,
                               int slowExecutionThreshold, int heartbeatPeriod, DirectRpcClientListener directRpcClientListener,
                               DirectRpcClientListListener listener) throws IllegalStateException {
        this.name = name;
        this.hosts = hosts;
        this.rescueTimeArray = new AtomicLongArray(hosts.length);
        this.configuration = configuration;
        this.timeout = timeout;
        this.compressionThreshold = compressionThreshold;
        this.slowExecutionThreshold = slowExecutionThreshold;
        this.heartbeatPeriod = heartbeatPeriod;
        this.directRpcClientListener = directRpcClientListener;
        this.listener = listener;
        boolean hasAvailableClient = false;
        for (String host : hosts) {
            boolean isSuccess = createClient(-1, host);
            if (isSuccess) {
                hasAvailableClient = true;
                RPC_CONNECTION_LOG.info("Add `{}` to `{}` is success. Hosts: `{}`.", host, name, hosts);
                Methods.invokeIfNotNull("DirectRpcClientListListener#onCreated(String host)", getParameterMap(-1, host),
                        listener, () -> listener.onCreated(name, host));
            } else {
                RPC_CONNECTION_LOG.error("Add `{}` to `{}` failed. Hosts: `{}`.", host, name, hosts);
                Methods.invokeIfNotNull("DirectRpcClientListListener#onClosed(String host)", getParameterMap(-1, host),
                        listener, () -> listener.onClosed(name, host, false));
            }
        }
        if ( !hasAvailableClient ) {
            LOG.error("There is no available `DirectRpcClient`. `name`:`" + name + "`. hosts:`" + Arrays.toString(hosts) + "`.");
            throw new IllegalStateException("There is no available `DirectRpcClient`. `name`:`" + name + "`. hosts:`" + Arrays.toString(hosts) + "`.");
        }
    }

    /**
     * 获得 RPC 地址数组，不会返回 {@code null} 或空数组。
     *
     * @return RPC 地址数组
     */
    public String[] getHosts() {
        String[] copyHosts = new String[hosts.length];
        System.arraycopy(hosts, 0, copyHosts, 0, hosts.length);
        return copyHosts;
    }

    /**
     * 获得指定索引对应的 RPC 直连客户端，如果该客户端不可用，则随机获取一个可用客户端返回，如果当前没有可用客户端，将返回 {@code null}。
     *
     * @param clientIndex 索引位置
     * @param excludeClientIndices 随机获取可用客户端时，需要排除的索引位置
     * @return RPC 直连客户端，可能返回 {@code null}
     * @throws IndexOutOfBoundsException 如果索引位置越界，将抛出此异常
     */
    public DirectRpcClient orAvailableClient(int clientIndex, int... excludeClientIndices) {
        if (state != BeanStatusEnum.NORMAL) { // 当前 RPC 直连客户端列表已关闭，直接返回 null
            return null;
        }
        DirectRpcClient client = get(clientIndex);
        if (client == null) {
            client = getAvailableClient(excludeClientIndices);
        }
        return client;
    }

    /**
     * 获得指定索引对应的 RPC 直连客户端，如果该客户端不可用，则返回 {@code null}。
     *
     * @param clientIndex 索引位置
     * @return 索引对应的 RPC 直连客户端，可能返回 {@code null}
     * @throws IndexOutOfBoundsException 如果索引位置越界，将抛出此异常
     */
    public DirectRpcClient get(int clientIndex) throws IndexOutOfBoundsException {
        if (state != BeanStatusEnum.NORMAL) { // 当前 RPC 直连客户端列表已关闭，直接返回 null
            return null;
        }
        if (clientIndex >= hosts.length) {
            String errorMessage = LogBuildUtil.buildMethodExecuteFailedLog("DirectRpcClientList#get(int clientIndex)",
                    "client index out of range", getParameterMap(clientIndex, null));
            LOG.error(errorMessage);
            throw new IndexOutOfBoundsException(errorMessage);
        }
        DirectRpcClient rpcClient = clientList.get(clientIndex);
        if (rpcClient != null) {
            if (!rpcClient.isActive()) {
                LOG.debug("DirectRpcClient is unavailable. `clientIndex`:`{}`. `host`:`{}`.", clientIndex, hosts[clientIndex]);
                removeUnavailableClient(rpcClient);
                rpcClient = null;
            } else {
                LOG.debug("Choose DirectRpcClient success. `clientIndex`:`{}`. `host`:`{}`.", clientIndex, hosts[clientIndex]);
            }
        } else {
            LOG.debug("DirectRpcClient is null. `clientIndex`:`{}`. `host`:`{}`.", clientIndex, hosts[clientIndex]);
            startRescueTask(); // make sure rescue task is running
        }
        return rpcClient;
    }

    /**
     * 随机获取一个可用客户端返回，如果当前没有可用客户端，将返回 {@code null}。
     *
     * @param excludeClientIndices 随机获取可用客户端时，需要排除的索引位置
     * @return RPC 直连客户端，可能返回 {@code null}
     */
    public DirectRpcClient getAvailableClient(int... excludeClientIndices) {
        if (state != BeanStatusEnum.NORMAL) { // 当前 RPC 直连客户端列表已关闭，直接返回 null
            return null;
        }
        List<DirectRpcClient> availableClientList = new ArrayList<>();
        for (int i = 0; i < hosts.length; i++) {
            boolean isExcludeIndex = false;
            for (int excludeClientIndex : excludeClientIndices) {
                if (i == excludeClientIndex) {
                    LOG.debug("Exclude client index. `clientIndex`:`{}`. `host`:`{}`.", i, hosts[i]);
                    isExcludeIndex = true;
                    break;
                }
            }
            if (!isExcludeIndex) {
                DirectRpcClient client = clientList.get(i);
                if (client != null && client.isActive()) {
                    availableClientList.add(client);
                    LOG.debug("Add to available client list. `clientIndex`:`{}`. `host`:`{}`.", i, hosts[i]);
                } else {
                    LOG.debug("Unavailable client. `clientIndex`:`{}`. `host`:`{}`.", i, hosts[i]);
                }
            }
        }
        if (!availableClientList.isEmpty()) {
            DirectRpcClient availableClient = availableClientList.get(new Random().nextInt(availableClientList.size()));
            LOG.debug("Choose random available client success. `host`:`{}`.", availableClient.getHost());
            return availableClient;
        } else {
            LOG.debug("Choose random available client failed: `there is no available client`.");
            return null;
        }
    }

    /**
     * 获得指定索引对应的 RPC 直连客户端最后一次恢复时间戳，默认返回 0。
     *
     * @param clientIndex 索引位置
     * @return 索引对应的 RPC 直连客户端最后一次恢复时间戳
     * @throws IndexOutOfBoundsException 如果索引位置越界，将抛出此异常
     */
    public long getRescueTime(int clientIndex) throws IndexOutOfBoundsException {
        if (clientIndex >= hosts.length) {
            String errorMessage = LogBuildUtil.buildMethodExecuteFailedLog("DirectRpcClientList#getRescueTime(int clientIndex)",
                    "client index out of range", getParameterMap(clientIndex, null));
            LOG.error(errorMessage);
            throw new IndexOutOfBoundsException(errorMessage);
        }
        return rescueTimeArray.get(clientIndex);
    }

    @Override
    public synchronized void close() {
        if (state != BeanStatusEnum.CLOSED) {
            state = BeanStatusEnum.CLOSED;
            for (DirectRpcClient client : clientList) {
                if (client != null) {
                    client.close();
                }
            }
            RPC_CONNECTION_LOG.info("DirectRpcClientList has been closed. `name`:`{}`. `hosts`:`{}`.", name, hosts);
        }
    }

    @Override
    public String toString() {
        return "DirectRpcClientList{" +
                "name='" + name + '\'' +
                ", hosts=" + Arrays.toString(hosts) +
                ", configuration=" + configuration +
                ", timeout=" + timeout +
                ", compressionThreshold=" + compressionThreshold +
                ", slowExecutionThreshold=" + slowExecutionThreshold +
                ", heartbeatPeriod=" + heartbeatPeriod +
                ", listener=" + listener +
                ", clientList=" + clientList +
                ", isRescueTaskRunning=" + isRescueTaskRunning +
                ", state=" + state +
                '}';
    }

    /**
     * 根据提供 RPC 服务的主机地址，创建一个 RPC 直连客户端，并将其放入列表指定索引位置，如果索引位置小于 0，则在列表中新增该直连客户端。
     *
     * @param clientIndex 索引位置，如果为 -1，则在列表中添加
     * @param host 提供 RPC 服务的主机地址
     * @return 是否创建成功
     */
    private boolean createClient(int clientIndex, String host) {
        DirectRpcClient client = null;
        try {
            client = new DirectRpcClient(host, configuration, timeout, compressionThreshold, slowExecutionThreshold,
                    heartbeatPeriod, directRpcClientListener, this::removeUnavailableClient);
        } catch (Exception ignored) {}

        synchronized (clientListUpdateLock) {
            if (client != null && client.isActive()) {
                if (clientIndex < 0) {
                    clientList.add(client);
                } else {
                    clientList.set(clientIndex, client);
                }
                LOG.debug("Add `DirectRpcClient` to client list success." + LogBuildUtil.build(getParameterMap(clientIndex, host)));
                return true;
            } else {
                if (clientIndex < 0) {
                    clientList.add(null);
                } else {
                    clientList.set(clientIndex, null);
                }
                LOG.error("Add `DirectRpcClient` to client list failed." + LogBuildUtil.build(getParameterMap(clientIndex, host)));
                return false;
            }
        }
    }

    /**
     * 从列表中移除不可用的 RPC 直连客户端。
     *
     * @param unavailableClient 不可用的 RPC 直连客户端
     */
    private void removeUnavailableClient(DirectRpcClient unavailableClient) throws NullPointerException {
        if (unavailableClient == null) { //should not happen, just for bug detection
            throw new NullPointerException("Remove unavailable client failed: `null client`." +
                    LogBuildUtil.build(getParameterMap(-1, null)));
        }
        boolean isRemoveSuccess = false;
        int clientIndex;
        synchronized (clientListUpdateLock) {
            clientIndex = clientList.indexOf(unavailableClient);
            if (clientIndex >= 0) {
                clientList.set(clientIndex, null);
                isRemoveSuccess = true;
                LOG.debug("Remove `DirectRpcClient` from client list success." + LogBuildUtil.build(getParameterMap(clientIndex, unavailableClient.getHost())));
            }
        }
        if (isRemoveSuccess) {
            startRescueTask();
            Methods.invokeIfNotNull("DirectRpcClientListListener#onClosed(String host)", getParameterMap(clientIndex, unavailableClient.getHost()),
                    listener, () -> listener.onClosed(name, unavailableClient.getHost(), unavailableClient.isOffline()));
        }
    }

    /**
     * 获得方法运行的通用参数 {@code Map}，用于日志打印。
     *
     * @param clientIndex 索引位置，允许小于 0
     * @param host RPC 地址，允许为 {@code null} 或空
     * @return 通用参数 {@code Map}
     */
    private Map<String, Object> getParameterMap(int clientIndex, String host) {
        Map<String, Object> parameterMap = new LinkedHashMap<>();
        if (clientIndex >= 0) {
            parameterMap.put("clientIndex", clientIndex);
        }
        if (host != null && !host.isEmpty()) {
            parameterMap.put("host", host);
        }
        parameterMap.put("name", name);
        parameterMap.put("hosts", hosts);
        return parameterMap;
    }

    /**
     * 启动 RPC 直连客户端重连恢复任务。
     */
    private void startRescueTask() {
        if (state == BeanStatusEnum.NORMAL) {
            synchronized (rescueTaskLock) {
                if (!isRescueTaskRunning) {
                    Thread rescueThread = new Thread() {

                        @Override
                        public void run() {
                            long startTime = System.currentTimeMillis();
                            RPC_CONNECTION_LOG.info("DirectRpcClient rescue task has been started. `name`:`{}`. `hosts`:`{}`.", name, Arrays.toString(hosts));
                            try {
                                while (state == BeanStatusEnum.NORMAL) {
                                    boolean hasRecovered = true;
                                    for (int i = 0; i < hosts.length; i++) {
                                        if (clientList.get(i) == null) {
                                            String host = hosts[i];
                                            boolean isSuccess = createClient(i, host);
                                            if (isSuccess) {
                                                rescueTimeArray.set(i, System.currentTimeMillis());
                                                RPC_CONNECTION_LOG.info("Rescue `{}` success. `name`:`{}`. `hosts`:`{}`.", host, name, Arrays.toString(hosts));
                                                Methods.invokeIfNotNull("DirectRpcClientListListener#onRecovered(String host)", getParameterMap(i, host),
                                                        listener, () -> listener.onRecovered(name, host));
                                            } else {
                                                hasRecovered = false;
                                                RPC_CONNECTION_LOG.warn("Rescue `{}` failed. `name`:`{}`. `hosts`:`{}`.", host, name, Arrays.toString(hosts));
                                            }
                                        }
                                    }
                                    if (hasRecovered) {
                                        break;
                                    } else {
                                        Thread.sleep(5000); //还有未恢复的客户端，等待 5s 后继续尝试
                                    }
                                }
                                RPC_CONNECTION_LOG.info("DirectRpcClient rescue task has been finished. Cost: {}ms. `name`:`{}`. `hosts`:`{}`.",
                                        System.currentTimeMillis() - startTime, name, hosts);
                            } catch (Exception e) { //should not happen, just for bug detection
                                RPC_CONNECTION_LOG.info("DirectRpcClient rescue task execute failed: `{}`. Cost: {}ms. `name`:`{}`. `hosts`:`{}`.",
                                        e.getMessage(), System.currentTimeMillis() - startTime, name, hosts);
                                LOG.error("DirectRpcClient rescue task executed failed. `name`:`" + name + "`. `hosts`:`"
                                        + Arrays.toString(hosts) + "`.", e);
                            } finally {
                                rescueOver();
                            }
                        }

                        private void rescueOver() {
                            synchronized (rescueTaskLock) {
                                isRescueTaskRunning = false;
                            }
                        }

                    };
                    rescueThread.setName("naiverpc-client-rescue-task");
                    rescueThread.setDaemon(true);
                    rescueThread.start();
                    isRescueTaskRunning = true;
                }
            }
        }
    }
}
