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
import com.heimuheimu.naiverpc.client.RpcClient;
import com.heimuheimu.naiverpc.client.RpcClientListener;
import com.heimuheimu.naiverpc.constant.BeanStatusEnum;
import com.heimuheimu.naiverpc.exception.RpcException;
import com.heimuheimu.naiverpc.exception.TimeoutException;
import com.heimuheimu.naiverpc.exception.TooBusyException;
import com.heimuheimu.naiverpc.net.SocketConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * RPC 服务调用集群客户端，连接多台相同功能的 RPC 服务提供者
 *
 * @author heimuheimu
 * @ThreadSafe
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class RpcClusterClient implements RpcClient {

    private static final Logger RPC_CONNECTION_LOG = LoggerFactory.getLogger("NAIVERPC_CONNECTION_LOG");

    private static final Logger LOG = LoggerFactory.getLogger(RpcClusterClient.class);

    /**
     * 提供 RPC 服务的主机地址数组，由主机名和端口组成，":"符号分割，例如：localhost:4182
     */
    private final String[] hosts;

    /**
     * RPC 服务调用客户端最后一次从不可用状态中恢复的时间戳列表，该列表顺序、大小与 {@link #hosts} 一致
     * <p>
     *     如果 RPC 服务调用客户端一直保持可用，在列表中的值为 0
     * </p>
     */
    private final AtomicLongArray rescueTimeArray;

    /**
     * 记录已获取 RPC 服务调用客户端的次数，用于做负载均衡
     */
    private final AtomicLong count = new AtomicLong(0);

    /**
     * RPC 服务调用客户端列表，该列表顺序、大小与 {@link #hosts} 一致
     * <p>
     *     如果某个 RPC 服务调用客户端不可用，该客户端在列表中的值为 {@code null}
     * </p>
     */
    private final CopyOnWriteArrayList<RpcClient> clientList = new CopyOnWriteArrayList<>();

    /**
     * 当前可用的 RPC 服务调用客户端列表
     */
    private final CopyOnWriteArrayList<RpcClient> aliveClientList = new CopyOnWriteArrayList<>();

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
     * RPC 服务调用集群客户端事件监听器
     */
    private final RpcClusterClientListener rpcClusterClientListener;

    /**
     * RPC 服务调用客户端恢复任务是否运行
     */
    private boolean isRescueTaskRunning = false;

    /**
     * RPC 服务调用客户端恢复任务使用的私有锁
     */
    private final Object rescueTaskLock = new Object();

    /**
     * 当前 RPC 服务调用集群客户端
     */
    private volatile BeanStatusEnum state = BeanStatusEnum.NORMAL;

    /**
     * 构造一个 RPC 服务调用集群客户端
     * <p>该客户端的 RPC 服务调用超时时间设置为 5 秒，最小压缩字节数设置为 64 KB，心跳检测时间为 30 秒</p>
     *
     * @param hosts 提供 RPC 服务的主机地址数组，由主机名和端口组成，":"符号分割，例如：localhost:4182
     *              @throws IllegalArgumentException 如果 RPC 服务的主机地址数组为 {@code null} 或 空数组
     * @throws IllegalArgumentException 如果 RPC 服务的主机地址数组为 {@code null} 或 空数组
     */
    public RpcClusterClient(String[] hosts) {
        this(hosts, null, 5000, 64 * 1024, 30, null, null);
    }

    /**
     * 构造一个 RPC 服务调用集群客户端
     *
     * @param hosts 提供 RPC 服务的主机地址数组，由主机名和端口组成，":"符号分割，例如：localhost:4182
     * @param configuration 创建 RPC 服务调用客户端所使用的 Socket 配置信息
     * @param timeout RPC 服务调用默认超时时间，单位：毫秒
     * @param compressionThreshold 最小压缩字节数，当 Value 字节数小于或等于该值，不进行压缩，不能小于等于0
     * @param heartbeatPeriod 心跳检测时间，单位：秒，在该周期时间内单个 RPC 服务调用客户端使用的数据管道如果没有任何数据交互，将会发送一个心跳请求数据包，如果该值小于等于 0，则不进行检测
     * @param rpcClientListener RPC 服务调用客户端监听器，允许为 {@code null}
     * @param rpcClusterClientListener RPC 服务调用集群客户端事件监听器，允许为 {@code null}
     * @throws IllegalArgumentException 如果 RPC 服务的主机地址数组为 {@code null} 或 空数组
     * @throws IllegalStateException  如果在创建过程中所有提供 RPC 服务的主机都不可用
     */
    public RpcClusterClient(String[] hosts, SocketConfiguration configuration, int timeout, int compressionThreshold,
                            int heartbeatPeriod, RpcClientListener rpcClientListener, RpcClusterClientListener rpcClusterClientListener) {
        if (hosts == null || hosts.length == 0) {
            throw new IllegalArgumentException("Hosts could not be empty. Hosts: " + Arrays.toString(hosts)
                    + ". SocketConfiguration: " + configuration + ". Timeout: " + timeout
                    + ". Compression threshold: " + compressionThreshold);
        }
        this.hosts = hosts;
        this.rescueTimeArray = new AtomicLongArray(hosts.length);
        this.configuration = configuration;
        this.timeout = timeout;
        this.compressionThreshold = compressionThreshold;
        this.heartbeatPeriod = heartbeatPeriod;
        this.rpcClientListener = rpcClientListener;
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
                        rpcClusterClientListener.onClosed(host);
                    } catch (Exception e) {
                        LOG.error("Call RpcClusterClientListener#onClosed() failed. Host: `" + host + "`. Hosts: `" + Arrays.toString(hosts) + "`.", e);
                    }
                }
            }
        }
        if (aliveClientList.isEmpty()) {
            throw new IllegalStateException("There is no available rpc server. Hosts: `" + Arrays.toString(hosts) + "`");
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
            RpcClient client = getClient();
            if (client != null) {
                LOG.debug("Choose RpcClient success. Host: `{}`. Hosts: `{}`", client.getHost(), hosts);
                try {
                    return client.execute(method, args, timeout);
                } catch (TooBusyException ex) {
                    if (tooBusyRetryTimes > 0) {
                        --tooBusyRetryTimes;
                        LOG.error("RpcServer is too busy. Host: `{}`. Left retry times: `{}`. Hosts: `{}`.", client.getHost(), tooBusyRetryTimes, hosts);
                        return execute(method, args, timeout, tooBusyRetryTimes);
                    } else {
                        LOG.error("RpcServer is too busy. No more retry. Host: `{}`. Hosts: `{}`.", client.getHost(), hosts);
                        throw ex;
                    }
                }
            } else {
                throw new IllegalStateException("There is no available RpcClient. Method: `" + method + "`. Arguments: `"
                        + Arrays.toString(args) + "`. Timeout: `" + timeout + "`. Hosts: `" + Arrays.toString(hosts) + "`.");
            }
        } else {
            throw new IllegalStateException("RpcClusterClient has been closed. Method: `" + method + "`. Arguments: `"
                    + Arrays.toString(args) + "`. Timeout: `" + timeout + "`. Hosts: `" + Arrays.toString(hosts) + "`.");
        }
    }

    @Override
    public boolean isActive() {
        return !aliveClientList.isEmpty();
    }

    @Override
    public String getHost() {
        return Arrays.toString(hosts);
    }

    @Override
    public synchronized void close() {
        if (state != BeanStatusEnum.CLOSED) {
            state = BeanStatusEnum.CLOSED;
            for (RpcClient rpcClient : aliveClientList) {
                try {
                    rpcClient.close();
                } catch (Exception e) {
                    LOG.error("Close `" + rpcClient.getHost() + "` failed. Hosts: `" + Arrays.toString(hosts) + "`.", e);
                }
            }
            RPC_CONNECTION_LOG.info("RpcClusterClient has been closed. Hosts: `{}`.", Arrays.toString(hosts));
        }
    }

    private boolean createRpcClient(int clientIndex, String host) {
        DirectRpcClient client = null;
        try {
            client = new DirectRpcClient(host, configuration, timeout, compressionThreshold, heartbeatPeriod, rpcClientListener);
        } catch (Exception e) {
            LOG.error("Create DirectRpcClient for cluster failed. Host: `" + host + "`. Hosts: `" + Arrays.toString(hosts) + "`.", e);
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

    private RpcClient getClient() {
        int clientIndex = (int) (Math.abs(count.incrementAndGet()) % hosts.length);
        RpcClient client = clientList.get(clientIndex);
        if (client != null) {
            if (!client.isActive()) {
                boolean isRemoveSuccess= aliveClientList.remove(client);
                if (isRemoveSuccess) {
                    clientList.set(clientIndex, null);
                    if (rpcClusterClientListener != null) {
                        try {
                            rpcClusterClientListener.onClosed(client.getHost());
                        } catch (Exception e) {
                            LOG.error("Call RpcClusterClientListener#onClosed() failed. Host: `" + client.getHost() + "`. Hosts: `" + Arrays.toString(hosts) + "`.", e);
                        }
                    }
                    RPC_CONNECTION_LOG.error("Remove `" + client.getHost() + "` from cluster. Hosts: `" + Arrays.toString(hosts) + "`.");
                }
                client = null;
            } else if (isSkipThisRound(clientIndex)) {
                LOG.debug("RpcClient is skip this round: `{}`. Client index: `{}`. Hosts: `{}`.", client.getHost(), clientIndex, Arrays.toString(hosts));
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
                } catch (IndexOutOfBoundsException e) {
                    LOG.error("No available RpcClient due to IndexOutOfBoundsException. Index: `" + aliveClientIndex + "`. Size: `"
                            + aliveClientList.size() + "`. Hosts: `" + Arrays.toString(hosts) + "`.");
                }
            } else {
                LOG.error("There is no available RpcClient. Hosts: `" + Arrays.toString(hosts) + "`.");
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

    private void startRescueTask() {
        if (state == BeanStatusEnum.NORMAL) {
            synchronized (rescueTaskLock) {
                if (!isRescueTaskRunning) {
                    Thread rescueThread = new Thread() {

                        @Override
                        public void run() {
                            long startTime = System.currentTimeMillis();
                            RPC_CONNECTION_LOG.info("Rescue task has been started. Hosts: `{}`", Arrays.toString(hosts));
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
                                    Thread.sleep(5000); //delay 5000ms
                                }
                                rescueOver();
                                RPC_CONNECTION_LOG.info("Rescue task has been finished. Cost: {}ms. Hosts: `{}`",
                                        System.currentTimeMillis() - startTime, hosts);
                            } catch (Exception e) {
                                rescueOver();
                                RPC_CONNECTION_LOG.info("Rescue task executed failed: `{}`. Cost: {}ms. Hosts: `{}`",
                                        e.getMessage(), System.currentTimeMillis() - startTime, hosts);
                                LOG.error("Rescue task executed failed. Hosts: `" + Arrays.toString(hosts) + "`", e);
                            }
                        }

                        private void rescueOver() {
                            synchronized (rescueTaskLock) {
                                isRescueTaskRunning = false;
                            }
                        }

                    };
                    rescueThread.setName("RpcClusterClient rescue task");
                    rescueThread.setDaemon(true);
                    rescueThread.start();
                    isRescueTaskRunning = true;
                }
            }
        }
    }

}
