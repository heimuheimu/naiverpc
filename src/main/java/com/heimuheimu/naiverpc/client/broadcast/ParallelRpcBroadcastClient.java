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
import com.heimuheimu.naiverpc.facility.clients.DirectRpcClientList;
import com.heimuheimu.naiverpc.facility.clients.DirectRpcClientListListener;
import com.heimuheimu.naiverpc.monitor.client.RpcClientThreadPoolMonitorFactory;
import com.heimuheimu.naiverpc.net.SocketConfiguration;
import com.heimuheimu.naiverpc.util.LogBuildUtil;
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
 * <p>当 {@code ParallelRpcBroadcastClient} 中的 {@code DirectRpcClient} 被创建、关闭、恢复后，均会触发 {@link DirectRpcClientListener} 相应的事件进行通知。</p>
 * <p>当 RPC 调用成功或失败时，均会触发 {@link RpcBroadcastClientListener} 相应的事件进行通知。</p>
 * </blockquote>
 *
 * <h3>数据监控</h3>
 * <blockquote>
 * 可通过 {@link RpcClientThreadPoolMonitorFactory} 获取并行执行 RPC 调用请求使用的线程池监控数据。
 * </blockquote>
 *
 * <p><strong>说明：</strong> {@code ParallelRpcBroadcastClient} 类是线程安全的，可在多个线程中使用同一个实例。</p>
 *
 * @author heimuheimu
 */
public class ParallelRpcBroadcastClient implements RpcBroadcastClient {

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
     * RPC 直连客户端列表
     */
    private final DirectRpcClientList directRpcClientList;

    /**
     * {@code RpcBroadcastClient} 事件监听器，允许为 {@code null}
     */
    private final RpcBroadcastClientListener rpcBroadcastClientListener;

    /**
     * 并行执行 RPC 调用请求使用的线程池
     */
    private final ThreadPoolExecutor executorService;

    /**
     * 构造一个 RPC 服务调用方使用的广播客户端，并行执行 RPC 调用请求使用的线程池大小为 500，创建 {@code DirectRpcClient} 时，
     * {@link Socket} 配置信息使用 {@link SocketConfiguration#DEFAULT}，RPC 调用超时时间设置为 5 秒，最小压缩字节数设置为 64 KB，
     * RPC 调用过慢最小时间设置为 50 毫秒，心跳检测时间设置为 30 秒。
     *
     * @param hosts 提供 RPC 服务的主机地址数组，由主机名和端口组成，":"符号分割，例如：localhost:4182，不允许为 {@code null} 或空数组
     * @param directRpcClientListener 创建 {@code DirectRpcClient} 使用的 {@code DirectRpcClient} 事件监听器，允许为 {@code null}
     * @param directRpcClientListListener {@link DirectRpcClientList} 事件监听器，允许为 {@code null}
     * @param rpcBroadcastClientListener {@code RpcBroadcastClient} 事件监听器，允许为 {@code null}
     * @throws IllegalStateException 如果所有提供 RPC 服务的主机地址都不可用，将会抛出此异常
     * @see DirectRpcClientList
     */
    public ParallelRpcBroadcastClient(String[] hosts, DirectRpcClientListener directRpcClientListener,
                                      DirectRpcClientListListener directRpcClientListListener,
                                      RpcBroadcastClientListener rpcBroadcastClientListener) throws IllegalStateException {
        this(hosts, null, 5000, 64 * 1024, 50, 30, directRpcClientListener, directRpcClientListListener, rpcBroadcastClientListener, 500);
    }

    /**
     * 构造一个 RPC 服务调用方使用的广播客户端。
     *
     * @param hosts 提供 RPC 服务的主机地址数组，由主机名和端口组成，":"符号分割，例如：localhost:4182，不允许为 {@code null} 或空数组
     * @param configuration 创建 {@code DirectRpcClient} 使用的 {@link Socket} 配置信息，允许为 {@code null}
     * @param timeout 创建 {@code DirectRpcClient} 使用的 RPC 调用超时时间，单位：毫秒，不能小于等于 0
     * @param compressionThreshold 创建 {@code DirectRpcClient} 使用的最小压缩字节数，不能小于等于 0
     * @param slowExecutionThreshold 创建 {@code DirectRpcClient} 使用的 RPC 调用过慢最小时间，单位：毫秒，不能小于等于 0
     * @param heartbeatPeriod heartbeatPeriod 创建 {@code DirectRpcClient} 使用的心跳检测时间，单位：秒，如果该值小于等于 0，则不进行检测
     * @param directRpcClientListener 创建 {@code DirectRpcClient} 使用的 {@code DirectRpcClient} 事件监听器，允许为 {@code null}
     * @param directRpcClientListListener {@link DirectRpcClientList} 事件监听器，允许为 {@code null}
     * @param rpcBroadcastClientListener {@code RpcBroadcastClient} 事件监听器，允许为 {@code null}
     * @param maximumPoolSize 并行执行 RPC 调用请求使用的线程池大小，如果小于等于 0，则为默认值 500
     * @throws IllegalStateException  如果所有提供 RPC 服务的主机地址都不可用，将会抛出此异常
     * @see DirectRpcClientList
     */
    public ParallelRpcBroadcastClient(String[] hosts, SocketConfiguration configuration, int timeout, int compressionThreshold,
                                      int slowExecutionThreshold, int heartbeatPeriod, DirectRpcClientListener directRpcClientListener,
                                      DirectRpcClientListListener directRpcClientListListener, RpcBroadcastClientListener rpcBroadcastClientListener,
                                      int maximumPoolSize) throws IllegalStateException {
        this.hosts = hosts;
        this.hostIndexMap = new HashMap<>();
        for (int i = 0; i < hosts.length; i++) {
            hostIndexMap.put(hosts[i], i);
        }

        this.directRpcClientList = new DirectRpcClientList("ParallelRpcBroadcastClient", hosts, configuration, timeout,
                compressionThreshold, slowExecutionThreshold, heartbeatPeriod, directRpcClientListener, directRpcClientListListener);
        this.rpcBroadcastClientListener = rpcBroadcastClientListener;

        maximumPoolSize = maximumPoolSize > 0 ? maximumPoolSize : 500;
        this.executorService = new ThreadPoolExecutor(0, maximumPoolSize,
                60L, TimeUnit.SECONDS, new SynchronousQueue<>(), new NamedThreadFactory());
        RpcClientThreadPoolMonitorFactory.get().register(executorService);
    }

    @Override
    public String[] getHosts() {
        return hosts;
    }

    @Override
    public Map<String, BroadcastResponse> execute(Method method, Object[] args) throws IllegalStateException {
        return execute(hosts, method, args, -1);
    }

    @Override
    public Map<String, BroadcastResponse> execute(Method method, Object[] args, long timeout) throws IllegalStateException {
        return execute(hosts, method, args, timeout);
    }

    @Override
    public Map<String, BroadcastResponse> execute(String[] hosts, Method method, Object[] args) throws IllegalStateException, IllegalArgumentException {
        return execute(hosts, method, args, -1);
    }

    @Override
    public Map<String, BroadcastResponse> execute(String[] hosts, Method method, Object[] args, long timeout) throws IllegalStateException, IllegalArgumentException {

        if (hosts == null || hosts.length == 0) {
            String errorMessage = buildMethodExecuteFailedLog(null, hosts, method, args, timeout, "hosts could not be null or empty");
            LOG.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        Map<String, BroadcastResponse> responseMap = new HashMap<>();
        Map<String, Future<BroadcastResponse>> futureMap = new HashMap<>();
        List<String> failedExecutedHostList = new ArrayList<>();
        for (String host : hosts) {
            Integer index = hostIndexMap.get(host);
            if (index != null) {
                DirectRpcClient client = directRpcClientList.get(index);
                if (client != null) {
                    try {
                        Future<BroadcastResponse> future = executorService.submit(new RpcExecuteTask(client, method, args, timeout));
                        futureMap.put(client.getHost(), future);
                    } catch (RejectedExecutionException e) {
                        RpcClientThreadPoolMonitorFactory.get().onRejected();
                        LOG.error(buildMethodExecuteFailedLog(host, hosts, method, args, timeout, "broadcast thread pool is too busy"), e);
                        BroadcastResponse response = new BroadcastResponse();
                        response.setHost(host);
                        response.setCode(BroadcastResponse.CODE_ERROR);
                        response.setException(e);
                        responseMap.put(host, response);
                        failedExecutedHostList.add(host);
                    }
                } else {
                    LOG.error(buildMethodExecuteFailedLog(host, hosts, method, args, timeout, "invalid host"));
                    BroadcastResponse response = new BroadcastResponse();
                    response.setHost(host);
                    response.setCode(BroadcastResponse.CODE_INVALID_HOST);
                    responseMap.put(host, response);
                }
            } else {
                LOG.error(buildMethodExecuteFailedLog(host, hosts, method, args, timeout, "unknown host"));
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
                if (response.isSuccess()) {
                    if (rpcBroadcastClientListener != null) {
                        try {
                            rpcBroadcastClientListener.onSuccess(host, method, args);
                        } catch (Exception e) {
                            String methodName = "RpcBroadcastClientListener#onSuccess(String host, Method method, Object[] args)";
                            Map<String, Object> parameterMap = buildMethodExecuteParameterMap(host, hosts, method, args, timeout);
                            LOG.error(LogBuildUtil.buildMethodExecuteFailedLog(methodName, e.getMessage(), parameterMap), e);
                        }
                    }
                } else {
                    if (response.getException() != null) {
                        LOG.error(buildMethodExecuteFailedLog(host, hosts, method, args, timeout, response.getException().getMessage()), response.getException());
                    } else {
                        LOG.error(buildMethodExecuteFailedLog(host, hosts, method, args, timeout, "error code [" + response.getCode() + "]"));
                    }
                    failedExecutedHostList.add(host);
                }
            } catch (Exception e) { //should not happen
                LOG.error(buildMethodExecuteFailedLog(host, hosts, method, args, timeout, e.getMessage()), e);
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
                    rpcBroadcastClientListener.onFail(failedExecutedHost, method, args);
                } catch (Exception e) {
                    String methodName = "RpcBroadcastClientListener#onFail(String host, Method method, Object[] args)";
                    Map<String, Object> parameterMap = buildMethodExecuteParameterMap(failedExecutedHost, hosts, method, args, timeout);
                    LOG.error(LogBuildUtil.buildMethodExecuteFailedLog(methodName, e.getMessage(), parameterMap), e);
                }
            }
        }
        return responseMap;
    }

    @Override
    public void close() {
        directRpcClientList.close();
    }

    @Override
    public String toString() {
        return "ParallelRpcBroadcastClient{" +
                "hosts=" + Arrays.toString(hosts) +
                ", hostIndexMap=" + hostIndexMap +
                ", directRpcClientList=" + directRpcClientList +
                ", rpcBroadcastClientListener=" + rpcBroadcastClientListener +
                ", executorService=" + executorService +
                '}';
    }

    private Map<String, Object> buildMethodExecuteParameterMap(String host, String[] hosts, Method method, Object[] args, long timeout) {
        LinkedHashMap<String, Object> parameterMap = new LinkedHashMap<>();
        if (host != null && !host.isEmpty()) {
            parameterMap.put("host", host);
        }
        parameterMap.put("method", method);
        parameterMap.put("args", args);
        parameterMap.put("timeout", timeout);
        parameterMap.put("hosts", hosts);
        return parameterMap;
    }

    private String buildMethodExecuteFailedLog(String host, String[] hosts, Method method, Object[] args, long timeout,
                                               String errorMessage) {
        String methodName = "ParallelRpcBroadcastClient#execute(String[] hosts, Method method, Object[] args, long timeout)";
        Map<String, Object> parameterMap = buildMethodExecuteParameterMap(host, hosts, method, args, timeout);
        return LogBuildUtil.buildMethodExecuteFailedLog(methodName, errorMessage, parameterMap);
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
                if (timeout > 0) {
                    response.setResult(rpcClient.execute(method, args, timeout));
                } else {
                    response.setResult(rpcClient.execute(method, args));
                }
            } catch (Exception e) {
                response.setCode(BroadcastResponse.CODE_ERROR);
                response.setException(e);
            }
            return response;
        }

    }

}
