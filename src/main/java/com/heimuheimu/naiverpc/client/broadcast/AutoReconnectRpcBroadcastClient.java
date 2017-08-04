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
import com.heimuheimu.naiverpc.client.RpcClient;
import com.heimuheimu.naiverpc.client.RpcClientListener;
import com.heimuheimu.naiverpc.constant.BeanStatusEnum;
import com.heimuheimu.naiverpc.monitor.thread.ThreadPoolMonitor;
import com.heimuheimu.naiverpc.monitor.thread.ThreadPoolType;
import com.heimuheimu.naiverpc.net.SocketConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 广播 RPC 服务调用客户端实现类，当与 RPC 服务提供者断开连接时，当前实现具备自动重连功能（每 5 秒进行一次重连尝试）。
 *
 * @author heimuheimu
 * @ThreadSafe
 */
public class AutoReconnectRpcBroadcastClient implements RpcBroadcastClient {

    private static final Logger RPC_CONNECTION_LOG = LoggerFactory.getLogger("NAIVERPC_CONNECTION_LOG");

    private static final Logger LOG = LoggerFactory.getLogger(AutoReconnectRpcBroadcastClient.class);

    /**
     * 提供 RPC 服务的主机地址数组，由主机名和端口组成，":"符号分割，例如：localhost:4182
     */
    private final String[] hosts;

    /**
     * 提供 RPC 服务的主机地址索引 Map，Key 为主机地址，Value 为该地址在 {@link #hosts} 中的索引位置
     * <p>注意：为保证线程安全，该 Map 仅允许在构造函数中执行修改操作</p>
     */
    private final Map<String, Integer> hostIndexMap;

    /**
     * 创建 RPC 服务调用客户端所使用的 Socket 配置信息
     */
    private final SocketConfiguration configuration;

    /**
     * RPC 服务调用默认超时时间，单位：毫秒
     */
    private final int timeout;

    /**
     * 最小压缩字节数，当 Value 字节数小于或等于该值，不进行压缩，不能小于等于0
     */
    private final int compressionThreshold;

    /**
     * 心跳检测时间，单位：秒，在该周期时间内单个 RPC 服务调用客户端使用的数据管道如果没有任何数据交互，将会发送一个心跳请求数据包
     */
    private final int heartbeatPeriod;

    /**
     * RPC 服务调用客户端监听器
     */
    private final RpcClientListener rpcClientListener;

    /**
     * 并行执行 RPC 调用使用的线程池
     */
    private final ThreadPoolExecutor executorService;

    /**
     * RPC 服务调用客户端恢复任务是否运行
     */
    private boolean isRescueTaskRunning = false;

    /**
     * RPC 服务调用客户端恢复任务使用的私有锁
     */
    private final Object rescueTaskLock = new Object();

    /**
     * 移除不可用 RPC 服务调用客户端实用的私有锁
     */
    private final Object removeInactiveRpcClientLock = new Object();

    /**
     * 当前 RPC 服务调用集群客户端所处状态
     */
    private volatile BeanStatusEnum state = BeanStatusEnum.NORMAL;

    /**
     * RPC 服务调用客户端列表，该列表顺序、大小与 {@link #hosts} 一致
     * <p>
     *     如果某个 RPC 服务调用客户端不可用，该客户端在列表中的值为 {@code null}
     * </p>
     */
    private final CopyOnWriteArrayList<RpcClient> clientList = new CopyOnWriteArrayList<>();

    /**
     * 构造一个广播 RPC 服务调用客户端
     * <p>该客户端的 RPC 服务调用超时时间设置为 5 秒，最小压缩字节数设置为 64 KB，心跳检测时间为 30 秒，并行执行 RPC 调用使用的线程池为 200</p>
     *
     * @param hosts 提供 RPC 服务的主机地址数组，由主机名和端口组成，":"符号分割，例如：localhost:4182
     * @throws IllegalArgumentException 如果 RPC 服务的主机地址数组为 {@code null} 或 空数组
     */
    public AutoReconnectRpcBroadcastClient(String[] hosts) {
        this(hosts, null, 5000, 64 * 1024, 30, null, 200);
    }

    /**
     * 构造一个广播 RPC 服务调用客户端
     *
     * @param hosts 提供 RPC 服务的主机地址数组，由主机名和端口组成，":"符号分割，例如：localhost:4182
     * @param configuration 创建 RPC 服务调用客户端所使用的 Socket 配置信息
     * @param timeout RPC 服务调用默认超时时间，单位：毫秒，不能小于等于0
     * @param compressionThreshold 最小压缩字节数，当 Value 字节数小于或等于该值，不进行压缩，不能小于等于0
     * @param heartbeatPeriod 心跳检测时间，单位：秒，在该周期时间内单个 RPC 服务调用客户端使用的数据管道如果没有任何数据交互，将会发送一个心跳请求数据包，如果该值小于等于 0，则不进行检测
     * @param rpcClientListener RPC 服务调用客户端监听器，允许为 {@code null}
     * @param maximumPoolSize 并行执行 RPC 调用使用的线程池最大大小，不能小于等于0
     * @throws IllegalArgumentException 如果 RPC 服务的主机地址数组为 {@code null} 或 空数组
     * @throws IllegalArgumentException 如果 timeout 小于等于 0
     * @throws IllegalArgumentException 如果 compressionThreshold 小于等于 0
     * @throws IllegalArgumentException 如果 maximumPoolSize 小于等于 0
     * @throws IllegalStateException  如果在创建过程中所有提供 RPC 服务的主机都不可用
     */
    public AutoReconnectRpcBroadcastClient(String[] hosts, SocketConfiguration configuration, int timeout, int compressionThreshold,
                                           int heartbeatPeriod, RpcClientListener rpcClientListener, int maximumPoolSize)
            throws IllegalArgumentException, IllegalStateException{
        if (hosts == null || hosts.length == 0) {
            LOG.error("Create AutoReconnectRpcBroadcastClient failed. Hosts could not be empty. Hosts: `" + Arrays.toString(hosts)
                    + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout
                    + "`. Compression threshold: `" + compressionThreshold + "`. Heartbeat period: `" + heartbeatPeriod + "`. Maximum pool size: `" + maximumPoolSize + "`.");
            throw new IllegalArgumentException("Create AutoReconnectRpcBroadcastClient failed. Hosts could not be empty. Hosts: `" + Arrays.toString(hosts)
                    + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout
                    + "`. Compression threshold: `" + compressionThreshold + "`. Heartbeat period: `" + heartbeatPeriod + "`. Maximum pool size: `" + maximumPoolSize + "`.");
        }
        if (timeout <= 0) {
            LOG.error("Create AutoReconnectRpcBroadcastClient failed. Timeout could not be equal or less than 0. Hosts: `" + Arrays.toString(hosts)
                    + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout
                    + "`. Compression threshold: `" + compressionThreshold + "`. Heartbeat period: `" + heartbeatPeriod + "`. Maximum pool size: `" + maximumPoolSize + "`.");
            throw new IllegalArgumentException("Create AutoReconnectRpcBroadcastClient failed. Timeout could not be equal or less than 0. Hosts: `" + Arrays.toString(hosts)
                    + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout
                    + "`. Compression threshold: `" + compressionThreshold + "`. Heartbeat period: `" + heartbeatPeriod + "`. Maximum pool size: `" + maximumPoolSize + "`.");
        }
        if (compressionThreshold <= 0) {
            LOG.error("Create AutoReconnectRpcBroadcastClient failed. CompressionThreshold could not be equal or less than 0. Hosts: `" + Arrays.toString(hosts)
                    + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout
                    + "`. Compression threshold: `" + compressionThreshold + "`. Heartbeat period: `" + heartbeatPeriod + "`. Maximum pool size: `" + maximumPoolSize + "`.");
            throw new IllegalArgumentException("Create AutoReconnectRpcBroadcastClient failed. CompressionThreshold could not be equal or less than 0. Hosts: `" + Arrays.toString(hosts)
                    + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout
                    + "`. Compression threshold: `" + compressionThreshold + "`. Heartbeat period: `" + heartbeatPeriod + "`. Maximum pool size: `" + maximumPoolSize + "`.");
        }
        if (maximumPoolSize <= 0) {
            LOG.error("Create AutoReconnectRpcBroadcastClient failed. Maximum pool size could not be equal or less than 0. Hosts: `" + Arrays.toString(hosts)
                    + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout
                    + "`. Compression threshold: `" + compressionThreshold + "`. Heartbeat period: `" + heartbeatPeriod + "`. Maximum pool size: `" + maximumPoolSize + "`.");
            throw new IllegalArgumentException("Create AutoReconnectRpcBroadcastClient failed. Maximum pool size could not be equal or less than 0. Hosts: `" + Arrays.toString(hosts)
                    + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout
                    + "`. Compression threshold: `" + compressionThreshold + "`. Heartbeat period: `" + heartbeatPeriod + "`. Maximum pool size: `" + maximumPoolSize + "`.");
        }
        this.hosts = hosts;
        this.configuration = configuration;
        this.timeout = timeout;
        this.compressionThreshold = compressionThreshold;
        this.heartbeatPeriod = heartbeatPeriod;
        this.rpcClientListener = rpcClientListener;
        this.executorService = new ThreadPoolExecutor(0, maximumPoolSize,
                60L, TimeUnit.SECONDS, new SynchronousQueue<>(), new NamedThreadFactory());
        ThreadPoolMonitor.register(ThreadPoolType.RPC_CLIENT, executorService);
        this.hostIndexMap = new HashMap<>();
        int activeClientCount = 0;
        for (int i = 0; i < hosts.length; i++) {
            hostIndexMap.put(hosts[i], i);
            boolean isSuccess = createRpcClient(-1, hosts[i]);
            if (isSuccess) {
                activeClientCount ++;
                RPC_CONNECTION_LOG.info("Add `{}` to AutoReconnectRpcBroadcastClient is success. Hosts: `{}`.", hosts[i], Arrays.toString(hosts));
            } else {
                RPC_CONNECTION_LOG.error("Add `{}` to AutoReconnectRpcBroadcastClient is failed. Hosts: `{}`.", hosts[i], Arrays.toString(hosts));
            }
        }
        if (activeClientCount == 0) {
            LOG.error("Create AutoReconnectRpcBroadcastClient failed. There is no available rpc server. Hosts: `" + Arrays.toString(hosts)
                    + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout
                    + "`. Compression threshold: `" + compressionThreshold + "`. Heartbeat period: `" + heartbeatPeriod + "`. Maximum pool size: `" + maximumPoolSize + "`.");
            throw new IllegalStateException("Create AutoReconnectRpcBroadcastClient failed. There is no available rpc server. Hosts: `" + Arrays.toString(hosts)
                    + "`. SocketConfiguration: `" + configuration + "`. Timeout: `" + timeout
                    + "`. Compression threshold: `" + compressionThreshold + "`. Heartbeat period: `" + heartbeatPeriod + "`. Maximum pool size: `" + maximumPoolSize + "`.");
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
            LOG.error("AutoReconnectRpcBroadcastClient has been closed. Hosts: `" + Arrays.toString(hosts) + "`. Method: `"
                    + method + "`. Arguments: `" + Arrays.toString(args) + "`. Timeout: `" + timeout + "`." );
            throw new IllegalStateException("AutoReconnectRpcBroadcastClient has been closed. Hosts: `" + Arrays.toString(hosts) + "`. Method: `"
                + method + "`. Arguments: `" + Arrays.toString(args) + "`. Timeout: `" + timeout + "`.");
        }
        if (hosts == null || hosts.length == 0) {
            LOG.error("Hosts could not be null or empty. Hosts: `" + Arrays.toString(hosts) + "`. Method: `"
                    + method + "`. Arguments: `" + Arrays.toString(args) + "`. Timeout: `" + timeout + "`.");
            throw new IllegalArgumentException("Hosts could not be null or empty. Hosts: `" + Arrays.toString(hosts) + "`. Method: `" + method
                    + "`. Arguments: `" + Arrays.toString(args) + "`. Timeout: `" + timeout + "`.");
        }
        Map<String, BroadcastResponse> responseMap = new HashMap<>();
        Map<String, Future<BroadcastResponse>> futureMap = new HashMap<>();
        for (String host : hosts) {
            Integer index = hostIndexMap.get(host);
            if (index != null) {
                RpcClient client = getRpcClient(index);
                if (client != null) {
                    try {
                        Future<BroadcastResponse> future = executorService.submit(new RpcExecuteTask(client, method, args, timeout));
                        futureMap.put(client.getHost(), future);
                    } catch (RejectedExecutionException e) {
                        ThreadPoolMonitor.addRejectedCount(ThreadPoolType.RPC_CLIENT);
                        LOG.error("Broadcast rpc execute failed: `too busy`. Host: `" + host + "`. Method: `" + method
                                + "`. Arguments: `" + Arrays.toString(args) + "`. Timeout: `" + timeout + "`.", e);
                    }
                } else {
                    BroadcastResponse response = new BroadcastResponse();
                    response.setHost(host);
                    response.setCode(BroadcastResponse.CODE_INVALID_HOST);
                    responseMap.put(host, response);
                    LOG.error("Broadcast rpc execute failed: `invalid host`. Host: `" + host + "`. Method: `" + method
                            + "`. Arguments: `" + Arrays.toString(args) + "`. Timeout: `" + timeout + "`.");
                }
            } else {
                BroadcastResponse response = new BroadcastResponse();
                response.setHost(host);
                response.setCode(BroadcastResponse.CODE_UNKNOWN_HOST);
                responseMap.put(host, response);
                LOG.error("Broadcast rpc execute failed: `unknown host`. Host: `" + host + "`. Method: `" + method
                        + "`. Arguments: `" + Arrays.toString(args) + "`. Timeout: `" + timeout + "`.");
            }
        }
        for (String host : futureMap.keySet()) {
            Future<BroadcastResponse> future = futureMap.get(host);
            try {
                BroadcastResponse response = future.get();
                responseMap.put(host, response);
                if (!response.isSuccess()) {
                    if (response.getException() != null) {
                        LOG.error("Broadcast rpc execute failed: `" + response.getException().getMessage() + "`. Host: `" + host + "`. Method: `" + method
                                + "`. Arguments: `" + Arrays.toString(args) + "`. Timeout: `" + timeout + "`.", response.getException());
                    } else {
                        LOG.error("Broadcast rpc execute failed: `error code [" + response.getCode() + "]`. Host: `" + host + "`. Method: `" + method
                                + "`. Arguments: `" + Arrays.toString(args) + "`. Timeout: `" + timeout + "`.");
                    }
                }
            } catch (Exception e) { //should not happen
                BroadcastResponse response = new BroadcastResponse();
                response.setHost(host);
                response.setCode(BroadcastResponse.CODE_ERROR);
                response.setException(e);
                responseMap.put(host, response);
                LOG.error("Broadcast rpc execute failed: `" + e.getMessage() + "`. Host: `" + host + "`. Method: `" + method
                        + "`. Arguments: `" + Arrays.toString(args) + "`. Timeout: `" + timeout + "`.", e);
            }
        }
        return responseMap;
    }

    @Override
    public synchronized void close() {
        if (state != BeanStatusEnum.CLOSED) {
            state = BeanStatusEnum.CLOSED;
            executorService.shutdown();
            for (RpcClient rpcClient : clientList) {
                if (rpcClient != null) {
                    try {
                        rpcClient.close();
                    } catch (Exception e) {
                        LOG.error("Close `" + rpcClient.getHost() + "` failed. Hosts: `" + Arrays.toString(hosts) + "`.", e);
                    }
                }
            }
            RPC_CONNECTION_LOG.info("AutoReconnectRpcBroadcastClient has been closed. Hosts: `{}`.", Arrays.toString(hosts));
        }
    }

    private RpcClient getRpcClient(int clientIndex) {
        RpcClient client = clientList.get(clientIndex);
        if (client != null) {
            if (!client.isActive()) {
                synchronized (removeInactiveRpcClientLock) {
                    if (client == clientList.get(clientIndex)) {
                        clientList.set(clientIndex, null);
                        RPC_CONNECTION_LOG.error("Remove `" + client.getHost() + "` from AutoReconnectRpcBroadcastClient. Hosts: `" + Arrays.toString(hosts) + "`.");
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
            client = new DirectRpcClient(host, configuration, timeout, compressionThreshold, heartbeatPeriod, rpcClientListener);
        } catch (Exception e) {
            LOG.error("Create DirectRpcClient for AutoReconnectRpcBroadcastClient failed. Host: `" + host + "`. Hosts: `"
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
                            RPC_CONNECTION_LOG.info("AutoReconnectRpcBroadcastClient rescue task has been started. Hosts: `{}`", Arrays.toString(hosts));
                            try {
                                int activeClientCount = 0;
                                while (state == BeanStatusEnum.NORMAL && activeClientCount < hosts.length) {
                                    activeClientCount = 0;
                                    for (int i = 0; i < hosts.length; i++) {
                                        if (clientList.get(i) == null) {
                                            boolean isSuccess = createRpcClient(i, hosts[i]);
                                            if (isSuccess) {
                                                activeClientCount ++;
                                                RPC_CONNECTION_LOG.info("Rescue `{}` to AutoReconnectRpcBroadcastClient success. Hosts: `{}`.", hosts[i], Arrays.toString(hosts));
                                            } else {
                                                RPC_CONNECTION_LOG.warn("Rescue `{}` to AutoReconnectRpcBroadcastClient failed. Hosts: `{}`.", hosts[i], Arrays.toString(hosts));
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
                                RPC_CONNECTION_LOG.info("AutoReconnectRpcBroadcastClient rescue task has been finished. Cost: {}ms. Hosts: `{}`",
                                        System.currentTimeMillis() - startTime, hosts);
                            } catch (Exception e) {
                                rescueOver();
                                RPC_CONNECTION_LOG.info("AutoReconnectRpcBroadcastClient rescue task executed failed: `{}`. Cost: {}ms. Hosts: `{}`",
                                        e.getMessage(), System.currentTimeMillis() - startTime, hosts);
                                LOG.error("AutoReconnectRpcBroadcastClient rescue task executed failed. Hosts: `" + Arrays.toString(hosts) + "`", e);
                            }
                        }

                        private void rescueOver() {
                            synchronized (rescueTaskLock) {
                                isRescueTaskRunning = false;
                            }
                        }

                    };
                    rescueThread.setName("AutoReconnectRpcBroadcastClient rescue task");
                    rescueThread.setDaemon(true);
                    rescueThread.start();
                    isRescueTaskRunning = true;
                }
            }
        }
    }

    private static class RpcExecuteTask implements Callable<BroadcastResponse> {

        private final RpcClient rpcClient;

        private final Method method;

        private final Object[] args;

        private final long timeout;

        private RpcExecuteTask(RpcClient rpcClient, Method method, Object[] args, long timeout) {
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
