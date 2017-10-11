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

package com.heimuheimu.naiverpc.client.broadcast;

import com.heimuheimu.naiverpc.client.DirectRpcClient;
import com.heimuheimu.naiverpc.client.DirectRpcClientListener;
import com.heimuheimu.naiverpc.constant.BeanStatusEnum;
import com.heimuheimu.naiverpc.monitor.client.RpcClientThreadPoolMonitorFactory;
import com.heimuheimu.naiverpc.net.SocketConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

/**
 * {@link RpcBroadcastClient} 实现类，RPC 调用请求会并行发送至 {@code ParallelRpcBroadcastClient} 中的多个 RPC 服务提供方进行执行，并返回结果 {@code Map}，
 * {@code Map} 的 Key 为 RPC 服务提供方主机地址，Value 为 {@link BroadcastResponse}。
 *
 * <p>
 *     当 {@code RpcBroadcastClient} 不再使用时，应调用 {@link #close()} 方法进行资源释放。
 * </p>
 *
 * <h3>可用性</h3>
 * <blockquote>
 * {@code ParallelRpcBroadcastClient} 中不可用的 {@code DirectRpcClient} 将会被自动移除，并启动恢复线程尝试进行恢复，如果恢复失败，等待下一次恢复的周期为 5 秒。
 * </blockquote>
 *
 * <h3>监听器</h3>
 * <blockquote>
 * 当 {@code ParallelRpcBroadcastClient} 中的 {@code DirectRpcClient} 被创建、关闭、恢复后，以及 RPC 调用失败时均会触发 {@link RpcBroadcastClientListener} 相应的事件进行通知。
 * </blockquote>
 *
 * <h3>数据监控</h3>
 * <blockquote>
 * 可通过 {@link RpcClientThreadPoolMonitorFactory} 获取并行执行 RPC 调用请求使用的线程池监控数据。
 * </blockquote>
 *
 * <p><strong>说明：</strong> {@code ParallelRpcBroadcastClient } 类是线程安全的，可在多个线程中使用同一个实例。</p>
 *
 * @author heimuheimu
 */
public class ParallelRpcBroadcastClient implements RpcBroadcastClient {

    private static final Logger RPC_CONNECTION_LOG = LoggerFactory.getLogger("NAIVERPC_CONNECTION_LOG");

    private static final Logger LOG = LoggerFactory.getLogger(ParallelRpcBroadcastClient.class);

    /**
     * 提供 RPC 服务的主机地址数组，由主机名和端口组成，":"符号分割，例如：localhost:4182
     */
    private final String[] hosts;

    /**
     * 提供 RPC 服务的主机地址索引 {@code Map}，Key 为主机地址，Value 为该地址在 {@link #hosts} 中的索引位置
     */
    private final Map<String, Integer> hostIndexMap;

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
     * {@code RpcBroadcastClient} 事件监听器，允许为 {@code null}
     */
    private final RpcBroadcastClientListener rpcBroadcastClientListener;

    /**
     * 并行执行 RPC 调用请求使用的线程池
     */
    private final ThreadPoolExecutor executorService;

    /**
     * RPC 服务提供方恢复任务是否运行
     */
    private boolean isRescueTaskRunning = false;

    /**
     * RPC 服务提供方恢复任务使用的私有锁
     */
    private final Object rescueTaskLock = new Object();

    /**
     * 移除不可用 {@code DirectRpcClient} 使用的私有锁
     */
    private final Object removeInactiveRpcClientLock = new Object();

    /**
     * 当前 {@code ParallelRpcBroadcastClient} 所处状态
     */
    private volatile BeanStatusEnum state = BeanStatusEnum.NORMAL;

    /**
     * {@code DirectRpcClient} 列表，该列表顺序、大小与 {@link #hosts} 一致，如果某个 {@code host} 当前不可用，其在列表中的值为 {@code null}
     */
    private final CopyOnWriteArrayList<DirectRpcClient> clientList = new CopyOnWriteArrayList<>();

    /**
     * 构造一个 RPC 服务调用方使用的广播客户端，并行执行 RPC 调用请求使用的线程池大小为 500，创建 {@code DirectRpcClient} 时，
     * {@link Socket} 配置信息使用 {@link SocketConfiguration#DEFAULT}，RPC 调用超时时间设置为 5 秒，最小压缩字节数设置为 64 KB，
     * RPC 调用过慢最小时间设置为 50 毫秒，心跳检测时间设置为 30 秒。
     *
     * @param hosts 提供 RPC 服务的主机地址数组，由主机名和端口组成，":"符号分割，例如：localhost:4182，不允许为 {@code null} 或空数组
     * @param directRpcClientListener 创建 {@code DirectRpcClient} 使用的 {@code DirectRpcClient} 事件监听器，允许为 {@code null}
     * @param rpcBroadcastClientListener {@code RpcBroadcastClient} 事件监听器，允许为 {@code null}
     * @throws IllegalArgumentException 如果提供 RPC 服务的主机地址数组为 {@code null} 或空数组，将会抛出此异常
     * @throws IllegalStateException 如果所有提供 RPC 服务的主机地址都不可用，将会抛出此异常
     * @see DirectRpcClient
     */
    public ParallelRpcBroadcastClient(String[] hosts, DirectRpcClientListener directRpcClientListener,
            RpcBroadcastClientListener rpcBroadcastClientListener) throws IllegalArgumentException, IllegalStateException {
        this(hosts, null, 5000, 64 * 1024, 50, 30, directRpcClientListener, rpcBroadcastClientListener,500);
    }

    /**
     * 构造一个 RPC 服务调用方使用的广播客户端。
     *
     * @param hosts 提供 RPC 服务的主机地址数组，由主机名和端口组成，":"符号分割，例如：localhost:4182，不允许为 {@code null} 或空数组
     * @param configuration 创建 {@code DirectRpcClient} 使用的 {@link Socket} 配置信息，允许为 {@code null}
     * @param timeout 创建 {@code DirectRpcClient} 使用的 RPC 调用超时时间，单位：毫秒，不能小于等于 0
     * @param compressionThreshold 创建 {@code DirectRpcClient} 使用的最小压缩字节数，不能小于等于 0
     * @param slowExecutionThreshold 创建 {@code DirectRpcClient} 使用的 RPC 调用过慢最小时间，单位：毫秒，不能小于等于 0
     * @param heartbeatPeriod 创建 {@code DirectRpcClient} 使用的心跳检测时间，单位：秒
     * @param directRpcClientListener 创建 {@code DirectRpcClient} 使用的 {@code DirectRpcClient} 事件监听器，允许为 {@code null}
     * @param rpcBroadcastClientListener {@code RpcBroadcastClient} 事件监听器，允许为 {@code null}
     * @param maximumPoolSize 并行执行 RPC 调用请求使用的线程池大小，不能小于等于 0
     * @throws IllegalArgumentException 如果提供 RPC 服务的主机地址数组为 {@code null} 或空数组，将会抛出此异常
     * @throws IllegalArgumentException 如果 RPC 调用超时时间小于等于 0，将会抛出此异常
     * @throws IllegalArgumentException 如果最小压缩字节数小于等于 0，将会抛出此异常
     * @throws IllegalArgumentException 如果 RPC 调用过慢最小时间小于等于 0，将会抛出此异常
     * @throws IllegalArgumentException 如果并行执行 RPC 调用请求使用的线程池大小小于等于 0，将会抛出此异常
     * @throws IllegalStateException  如果所有提供 RPC 服务的主机地址都不可用，将会抛出此异常
     * @see DirectRpcClient
     */
    public ParallelRpcBroadcastClient(String[] hosts, SocketConfiguration configuration, int timeout, int compressionThreshold,
            int slowExecutionThreshold, int heartbeatPeriod, DirectRpcClientListener directRpcClientListener,
            RpcBroadcastClientListener rpcBroadcastClientListener, int maximumPoolSize) throws IllegalArgumentException, IllegalStateException {
        if (hosts == null || hosts.length == 0) {
            LOG.error("Create ParallelRpcBroadcastClient failed: `hosts could not be empty`. Hosts: `" + Arrays.toString(hosts)
                    + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout
                    + "`. CompressionThreshold: `" + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold
                    + "`. HeartbeatPeriod: `" + heartbeatPeriod + "`. DirectRpcClientListener: `" + directRpcClientListener
                    + "`. RpcBroadcastClientListener: `" + rpcBroadcastClientListener + "`. MaximumPoolSize: `" + maximumPoolSize + "`.");
            throw new IllegalArgumentException("Create ParallelRpcBroadcastClient failed: `hosts could not be empty`. Hosts: `" + Arrays.toString(hosts)
                    + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout
                    + "`. CompressionThreshold: `" + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold
                    + "`. HeartbeatPeriod: `" + heartbeatPeriod + "`. DirectRpcClientListener: `" + directRpcClientListener
                    + "`. RpcBroadcastClientListener: `" + rpcBroadcastClientListener + "`. MaximumPoolSize: `" + maximumPoolSize + "`.");
        }
        if (timeout <= 0) {
            LOG.error("Create ParallelRpcBroadcastClient failed: `timeout could not be equal or less than 0`. Hosts: `" + Arrays.toString(hosts)
                    + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout
                    + "`. CompressionThreshold: `" + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold
                    + "`. HeartbeatPeriod: `" + heartbeatPeriod + "`. DirectRpcClientListener: `" + directRpcClientListener
                    + "`. RpcBroadcastClientListener: `" + rpcBroadcastClientListener + "`. MaximumPoolSize: `" + maximumPoolSize + "`.");
            throw new IllegalArgumentException("Create ParallelRpcBroadcastClient failed: `timeout could not be equal or less than 0`. Hosts: `"
                    + Arrays.toString(hosts) + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout
                    + "`. CompressionThreshold: `" + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold
                    + "`. HeartbeatPeriod: `" + heartbeatPeriod + "`. DirectRpcClientListener: `" + directRpcClientListener
                    + "`. RpcBroadcastClientListener: `" + rpcBroadcastClientListener + "`. MaximumPoolSize: `" + maximumPoolSize + "`.");
        }
        if (compressionThreshold <= 0) {
            LOG.error("Create ParallelRpcBroadcastClient failed: `compressionThreshold could not be equal or less than 0`. Hosts: `"
                    + Arrays.toString(hosts) + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout
                    + "`. CompressionThreshold: `" + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold
                    + "`. HeartbeatPeriod: `" + heartbeatPeriod + "`. DirectRpcClientListener: `" + directRpcClientListener
                    + "`. RpcBroadcastClientListener: `" + rpcBroadcastClientListener + "`. MaximumPoolSize: `" + maximumPoolSize + "`.");
            throw new IllegalArgumentException("Create ParallelRpcBroadcastClient failed: `compressionThreshold could not be equal or less than 0`. Hosts: `"
                    + Arrays.toString(hosts) + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout
                    + "`. CompressionThreshold: `" + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold
                    + "`. HeartbeatPeriod: `" + heartbeatPeriod + "`. DirectRpcClientListener: `" + directRpcClientListener
                    + "`. RpcBroadcastClientListener: `" + rpcBroadcastClientListener + "`. MaximumPoolSize: `" + maximumPoolSize + "`.");
        }
        if (slowExecutionThreshold <= 0) {
            LOG.error("Create ParallelRpcBroadcastClient failed: `slowExecutionThreshold could not be equal or less than 0`. Hosts: `"
                    + Arrays.toString(hosts) + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout
                    + "`. CompressionThreshold: `" + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold
                    + "`. HeartbeatPeriod: `" + heartbeatPeriod + "`. DirectRpcClientListener: `" + directRpcClientListener
                    + "`. RpcBroadcastClientListener: `" + rpcBroadcastClientListener + "`. MaximumPoolSize: `" + maximumPoolSize + "`.");
            throw new IllegalArgumentException("Create ParallelRpcBroadcastClient failed: `slowExecutionThreshold could not be equal or less than 0`. Hosts: `"
                    + Arrays.toString(hosts) + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout
                    + "`. CompressionThreshold: `" + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold
                    + "`. HeartbeatPeriod: `" + heartbeatPeriod + "`. DirectRpcClientListener: `" + directRpcClientListener
                    + "`. RpcBroadcastClientListener: `" + rpcBroadcastClientListener + "`. MaximumPoolSize: `" + maximumPoolSize + "`.");
        }
        if (maximumPoolSize <= 0) {
            LOG.error("Create ParallelRpcBroadcastClient failed: `maximumPoolSize could not be equal or less than 0`. Hosts: `"
                    + Arrays.toString(hosts) + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout
                    + "`. CompressionThreshold: `" + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold
                    + "`. HeartbeatPeriod: `" + heartbeatPeriod + "`. DirectRpcClientListener: `" + directRpcClientListener
                    + "`. RpcBroadcastClientListener: `" + rpcBroadcastClientListener + "`. MaximumPoolSize: `" + maximumPoolSize + "`.");
            throw new IllegalArgumentException("Create ParallelRpcBroadcastClient failed: `maximumPoolSize could not be equal or less than 0`. Hosts: `"
                    + Arrays.toString(hosts) + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout
                    + "`. CompressionThreshold: `" + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold
                    + "`. HeartbeatPeriod: `" + heartbeatPeriod + "`. DirectRpcClientListener: `" + directRpcClientListener
                    + "`. RpcBroadcastClientListener: `" + rpcBroadcastClientListener + "`. MaximumPoolSize: `" + maximumPoolSize + "`.");
        }
        this.hosts = hosts;
        this.configuration = configuration;
        this.timeout = timeout;
        this.compressionThreshold = compressionThreshold;
        this.slowExecutionThreshold = slowExecutionThreshold;
        this.heartbeatPeriod = heartbeatPeriod;
        this.directRpcClientListener = directRpcClientListener;
        this.rpcBroadcastClientListener = rpcBroadcastClientListener;
        this.executorService = new ThreadPoolExecutor(0, maximumPoolSize,
                60L, TimeUnit.SECONDS, new SynchronousQueue<>(), new NamedThreadFactory());
        RpcClientThreadPoolMonitorFactory.get().register(executorService);
        this.hostIndexMap = new HashMap<>();
        int activeClientCount = 0;
        for (int i = 0; i < hosts.length; i++) {
            hostIndexMap.put(hosts[i], i);
            boolean isSuccess = createRpcClient(-1, hosts[i]);
            if (isSuccess) {
                activeClientCount ++;
                RPC_CONNECTION_LOG.info("Add `{}` to ParallelRpcBroadcastClient is success. Hosts: `{}`.", hosts[i], Arrays.toString(hosts));
                if (rpcBroadcastClientListener != null) {
                    try {
                        rpcBroadcastClientListener.onCreated(hosts[i]);
                    } catch (Exception e) {
                        LOG.error("Call RpcBroadcastClientListener#onCreated() failed. Host: `" + hosts[i] + "`. Hosts: `" + Arrays.toString(hosts) + "`.", e);
                    }
                }
            } else {
                RPC_CONNECTION_LOG.error("Add `{}` to ParallelRpcBroadcastClient is failed. Hosts: `{}`.", hosts[i], Arrays.toString(hosts));
                if (rpcBroadcastClientListener != null) {
                    try {
                        rpcBroadcastClientListener.onClosed(hosts[i], false);
                    } catch (Exception e) {
                        LOG.error("Call RpcBroadcastClientListener#onClosed() failed. Host: `" + hosts[i] + "`. Hosts: `" + Arrays.toString(hosts) + "`.", e);
                    }
                }
            }
        }
        if (activeClientCount == 0) {
            LOG.error("Create ParallelRpcBroadcastClient failed: `there is no active DirectRpcClient`. Hosts: `"
                    + Arrays.toString(hosts) + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout
                    + "`. CompressionThreshold: `" + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold
                    + "`. HeartbeatPeriod: `" + heartbeatPeriod + "`. DirectRpcClientListener: `" + directRpcClientListener
                    + "`. RpcBroadcastClientListener: `" + rpcBroadcastClientListener + "`. MaximumPoolSize: `" + maximumPoolSize + "`.");
            throw new IllegalStateException("Create ParallelRpcBroadcastClient failed: `there is no active DirectRpcClient`. Hosts: `"
                    + Arrays.toString(hosts) + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout
                    + "`. CompressionThreshold: `" + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold
                    + "`. HeartbeatPeriod: `" + heartbeatPeriod + "`. DirectRpcClientListener: `" + directRpcClientListener
                    + "`. RpcBroadcastClientListener: `" + rpcBroadcastClientListener + "`. MaximumPoolSize: `" + maximumPoolSize + "`.");
        }
    }

    @Override
    public String[] getHosts() {
        return hosts;
    }

    @Override
    public Map<String, BroadcastResponse> execute(Method method, Object[] args) throws IllegalStateException {
        return execute(hosts, method, args, timeout);
    }

    @Override
    public Map<String, BroadcastResponse> execute(Method method, Object[] args, long timeout) throws IllegalStateException {
        return execute(hosts, method, args, timeout);
    }

    @Override
    public Map<String, BroadcastResponse> execute(String[] hosts, Method method, Object[] args) throws IllegalStateException, IllegalArgumentException {
        return execute(hosts, method, args, timeout);
    }

    @Override
    public Map<String, BroadcastResponse> execute(String[] hosts, Method method, Object[] args, long timeout) throws IllegalStateException, IllegalArgumentException {
        if (state != BeanStatusEnum.NORMAL) {
            LOG.error("RPC broadcast failed: `ParallelRpcBroadcastClient has been closed`. Method: `" + method + "`. Arguments: `"
                    + Arrays.toString(args) + "`. Timeout: `" + timeout + "`. Hosts: `" + Arrays.toString(hosts) + "`.");
            throw new IllegalStateException("RPC broadcast failed: `ParallelRpcBroadcastClient has been closed`. Method: `" + method + "`. Arguments: `"
                    + Arrays.toString(args) + "`. Timeout: `" + timeout + "`. Hosts: `" + Arrays.toString(hosts) + "`.");
        }
        if (hosts == null || hosts.length == 0) {
            LOG.error("RPC broadcast failed: `hosts could not be null or empty`. Method: `" + method + "`. Arguments: `"
                    + Arrays.toString(args) + "`. Timeout: `" + timeout + "`. Hosts: `" + Arrays.toString(hosts) + "`.");
            throw new IllegalArgumentException("RPC broadcast failed: `hosts could not be null or empty`. Method: `" + method + "`. Arguments: `"
                    + Arrays.toString(args) + "`. Timeout: `" + timeout + "`. Hosts: `" + Arrays.toString(hosts) + "`.");
        }
        Map<String, BroadcastResponse> responseMap = new HashMap<>();
        Map<String, Future<BroadcastResponse>> futureMap = new HashMap<>();
        List<String> failedExecutedHostList = new ArrayList<>();
        for (String host : hosts) {
            Integer index = hostIndexMap.get(host);
            if (index != null) {
                DirectRpcClient client = getRpcClient(index);
                if (client != null) {
                    try {
                        Future<BroadcastResponse> future = executorService.submit(new RpcExecuteTask(client, method, args, timeout));
                        futureMap.put(client.getHost(), future);
                    } catch (RejectedExecutionException e) {
                        RpcClientThreadPoolMonitorFactory.get().onRejected();
                        LOG.error("RPC broadcast failed: `broadcast thread pool is too busy`. Host: `" + host + "`. Method: `" + method + "`. Arguments: `"
                                + Arrays.toString(args) + "`. Timeout: `" + timeout + "`. Hosts: `" + Arrays.toString(hosts) + "`.", e);
                        BroadcastResponse response = new BroadcastResponse();
                        response.setHost(host);
                        response.setCode(BroadcastResponse.CODE_ERROR);
                        response.setException(e);
                        responseMap.put(host, response);
                        failedExecutedHostList.add(host);
                    }
                } else {
                    LOG.error("RPC broadcast failed: `invalid host`. Host: `" + host + "`. Method: `" + method + "`. Arguments: `"
                            + Arrays.toString(args) + "`. Timeout: `" + timeout + "`. Hosts: `" + Arrays.toString(hosts) + "`.");
                    BroadcastResponse response = new BroadcastResponse();
                    response.setHost(host);
                    response.setCode(BroadcastResponse.CODE_INVALID_HOST);
                    responseMap.put(host, response);
                    failedExecutedHostList.add(host);
                }
            } else {
                LOG.error("RPC broadcast failed: `unknown host`. Host: `" + host + "`. Method: `" + method + "`. Arguments: `"
                        + Arrays.toString(args) + "`. Timeout: `" + timeout + "`. Hosts: `" + Arrays.toString(hosts) + "`.");
                BroadcastResponse response = new BroadcastResponse();
                response.setHost(host);
                response.setCode(BroadcastResponse.CODE_UNKNOWN_HOST);
                responseMap.put(host, response);
                failedExecutedHostList.add(host);
            }
        }
        for (String host : futureMap.keySet()) {
            Future<BroadcastResponse> future = futureMap.get(host);
            try {
                BroadcastResponse response = future.get();
                responseMap.put(host, response);
                if (!response.isSuccess()) {
                    if (response.getException() != null) {
                        LOG.error("RPC broadcast failed: `" + response.getException().getMessage() + "`. Host: `" + host + "`. Method: `" + method
                                + "`. Arguments: `" + Arrays.toString(args) + "`. Timeout: `" + timeout + "`. Hosts: `" + Arrays.toString(hosts) + "`.", response.getException());
                    } else {
                        LOG.error("RPC broadcast failed: `error code [" + response.getCode() + "]`. Host: `" + host + "`. Method: `" + method
                                + "`. Arguments: `" + Arrays.toString(args) + "`. Timeout: `" + timeout + "`. Hosts: `" + Arrays.toString(hosts) + "`.");
                    }
                    failedExecutedHostList.add(host);
                }
            } catch (Exception e) { //should not happen
                LOG.error("RPC broadcast failed: `" + e.getMessage() + "`. Host: `" + host + "`. Method: `" + method
                        + "`. Arguments: `" + Arrays.toString(args) + "`. Timeout: `" + timeout + "`. Hosts: `"
                        + Arrays.toString(hosts) + "`.", e);
                BroadcastResponse response = new BroadcastResponse();
                response.setHost(host);
                response.setCode(BroadcastResponse.CODE_ERROR);
                response.setException(e);
                responseMap.put(host, response);
                failedExecutedHostList.add(host);
            }
        }
        if (rpcBroadcastClientListener != null) {
            for (String failedExecutedHost : failedExecutedHostList) {
                try {
                    rpcBroadcastClientListener.onFailedExecuted(failedExecutedHost, method, args);
                } catch (Exception e) {
                    LOG.error("Call RpcBroadcastClientListener#onFailedExecuted() failed. Host: `" + failedExecutedHost + "`. Hosts: `" + Arrays.toString(hosts) + "`.", e);
                }
            }
        }
        return responseMap;
    }

    @Override
    public synchronized void close() {
        if (state != BeanStatusEnum.CLOSED) {
            state = BeanStatusEnum.CLOSED;
            executorService.shutdown();
            for (DirectRpcClient rpcClient : clientList) {
                if (rpcClient != null) {
                    rpcClient.close();
                }
            }
            RPC_CONNECTION_LOG.info("ParallelRpcBroadcastClient has been closed. Hosts: `{}`.", Arrays.toString(hosts));
        }
    }

    @Override
    public String toString() {
        return "ParallelRpcBroadcastClient{" +
                "hosts=" + Arrays.toString(hosts) +
                ", configuration=" + configuration +
                ", timeout=" + timeout +
                ", compressionThreshold=" + compressionThreshold +
                ", slowExecutionThreshold=" + slowExecutionThreshold +
                ", heartbeatPeriod=" + heartbeatPeriod +
                ", directRpcClientListener=" + directRpcClientListener +
                ", rpcBroadcastClientListener=" + rpcBroadcastClientListener +
                ", isRescueTaskRunning=" + isRescueTaskRunning +
                ", state=" + state +
                '}';
    }

    private DirectRpcClient getRpcClient(int clientIndex) {
        DirectRpcClient client = clientList.get(clientIndex);
        if (client != null) {
            if (!client.isActive()) {
                boolean isRemoveSuccess = false;
                synchronized (removeInactiveRpcClientLock) {
                    if (client == clientList.get(clientIndex)) {
                        clientList.set(clientIndex, null);
                        RPC_CONNECTION_LOG.error("Remove `" + client.getHost() + "` from ParallelRpcBroadcastClient. Hosts: `" + Arrays.toString(hosts) + "`.");
                        isRemoveSuccess = true;
                    }
                }
                if (isRemoveSuccess) {
                    if (rpcBroadcastClientListener != null) {
                        try {
                            rpcBroadcastClientListener.onClosed(client.getHost(), client.isOffline());
                        } catch (Exception e) {
                            LOG.error("Call RpcBroadcastClientListener#onClosed() failed. Host: `" + client.getHost() + "`. Hosts: `" + Arrays.toString(hosts) + "`.", e);
                        }
                    }
                }
                client = null;
            }
        }
        if (client == null) {
            startRescueTask();
        }
        return client;
    }

    private boolean createRpcClient(int clientIndex, String host) {
        DirectRpcClient client = null;
        try {
            client = new DirectRpcClient(host, configuration, timeout, compressionThreshold, slowExecutionThreshold,
                    heartbeatPeriod, directRpcClientListener);
        } catch (Exception e) {
            LOG.error("Create DirectRpcClient for ParallelRpcBroadcastClient failed. Host: `" + host + "`. Hosts: `"
                    + Arrays.toString(hosts) + "`.", e);
        }
        if (client != null && client.isActive()) {
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
                            RPC_CONNECTION_LOG.info("ParallelRpcBroadcastClient rescue task has been started. Hosts: `{}`", Arrays.toString(hosts));
                            try {
                                int activeClientCount = 0;
                                while (state == BeanStatusEnum.NORMAL && activeClientCount < hosts.length) {
                                    activeClientCount = 0;
                                    for (int i = 0; i < hosts.length; i++) {
                                        if (clientList.get(i) == null) {
                                            boolean isSuccess = createRpcClient(i, hosts[i]);
                                            if (isSuccess) {
                                                activeClientCount ++;
                                                RPC_CONNECTION_LOG.info("Rescue `{}` to ParallelRpcBroadcastClient success. Hosts: `{}`.", hosts[i], Arrays.toString(hosts));
                                                if (rpcBroadcastClientListener != null) {
                                                    try {
                                                        rpcBroadcastClientListener.onRecovered(hosts[i]);
                                                    } catch (Exception e) {
                                                        LOG.error("Call RpcBroadcastClientListener#onRecovered() failed. Host: `" + hosts[i] + "`. Hosts: `" + Arrays.toString(hosts) + "`.", e);
                                                    }
                                                }
                                            } else {
                                                RPC_CONNECTION_LOG.warn("Rescue `{}` to ParallelRpcBroadcastClient failed. Hosts: `{}`.", hosts[i], Arrays.toString(hosts));
                                            }
                                        } else {
                                            activeClientCount ++;
                                        }
                                    }
                                    if (activeClientCount < hosts.length) {
                                        Thread.sleep(5000); //delay 5000ms
                                    }
                                }
                                rescueOver();
                                RPC_CONNECTION_LOG.info("ParallelRpcBroadcastClient rescue task has been finished. Cost: {}ms. Hosts: `{}`",
                                        System.currentTimeMillis() - startTime, hosts);
                            } catch (Exception e) {
                                rescueOver();
                                RPC_CONNECTION_LOG.info("ParallelRpcBroadcastClient rescue task executed failed: `{}`. Cost: {}ms. Hosts: `{}`",
                                        e.getMessage(), System.currentTimeMillis() - startTime, hosts);
                                LOG.error("ParallelRpcBroadcastClient rescue task executed failed. Hosts: `" + Arrays.toString(hosts) + "`", e);
                            }
                        }

                        private void rescueOver() {
                            synchronized (rescueTaskLock) {
                                isRescueTaskRunning = false;
                            }
                        }

                    };
                    rescueThread.setName("naiverpc-broadcast-client-rescue-task");
                    rescueThread.setDaemon(true);
                    rescueThread.start();
                    isRescueTaskRunning = true;
                }
            }
        }
    }

    private static class RpcExecuteTask implements Callable<BroadcastResponse> {

        private final DirectRpcClient rpcClient;

        private final Method method;

        private final Object[] args;

        private final long timeout;

        private RpcExecuteTask(DirectRpcClient rpcClient, Method method, Object[] args, long timeout) {
            this.rpcClient = rpcClient;
            this.method = method;
            this.args = args;
            this.timeout = timeout;
        }

        @Override
        public BroadcastResponse call() {
            BroadcastResponse response = new BroadcastResponse();
            response.setHost(rpcClient.getHost());
            try {
                response.setResult(rpcClient.execute(method, args, timeout));
            } catch (Exception e) {
                response.setCode(BroadcastResponse.CODE_ERROR);
                response.setException(e);
            }
            return response;
        }

    }

}
