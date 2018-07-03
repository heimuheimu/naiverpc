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

package com.heimuheimu.naiverpc.client;

import com.heimuheimu.naivemonitor.monitor.ExecutionMonitor;
import com.heimuheimu.naiverpc.channel.RpcChannel;
import com.heimuheimu.naiverpc.constant.OperationCode;
import com.heimuheimu.naiverpc.constant.ResponseStatusCode;
import com.heimuheimu.naiverpc.exception.RpcException;
import com.heimuheimu.naiverpc.exception.TimeoutException;
import com.heimuheimu.naiverpc.exception.TooBusyException;
import com.heimuheimu.naiverpc.facility.UnusableServiceNotifier;
import com.heimuheimu.naiverpc.message.RpcRequestMessage;
import com.heimuheimu.naiverpc.monitor.client.RpcClientCompressionMonitorFactory;
import com.heimuheimu.naiverpc.monitor.client.RpcClientExecutionMonitorFactory;
import com.heimuheimu.naiverpc.net.BuildSocketException;
import com.heimuheimu.naiverpc.net.SocketConfiguration;
import com.heimuheimu.naiverpc.packet.RpcPacket;
import com.heimuheimu.naiverpc.packet.RpcPacketBuilder;
import com.heimuheimu.naiverpc.transcoder.SimpleTranscoder;
import com.heimuheimu.naiverpc.transcoder.Transcoder;
import com.heimuheimu.naiverpc.util.ByteUtil;
import com.heimuheimu.naiverpc.util.ReflectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RPC 服务调用方使用的直连客户端，通过 {@link #execute(Method, Object[])} 方法远程调用 RPC 服务提供方提供的服务。
 *
 * <p>
 *     当 {@code DirectRpcClient} 不再使用时，应调用 {@link #close()} 方法进行资源释放。
 * </p>
 *
 * <h3>监听器</h3>
 * <blockquote>
 * 当 RPC 调用出现异常、超时、执行过慢、RPC 服务提供方繁忙、{@code DirectRpcClient} 已关闭等错误时，均会触发 {@link DirectRpcClientListener} 相应的事件进行通知。
 * </blockquote>
 *
 * <h3>数据监控</h3>
 * <blockquote>
 * 可通过 {@link RpcClientCompressionMonitorFactory} 获取 RPC 服务调用方压缩信息监控数据。<br>
 * 可通过 {@link RpcClientExecutionMonitorFactory} 获取单个 RPC 服务提供方的 RPC 调用信息监控数据。
 * </blockquote>
 *
 * <p><strong>说明：</strong>{@code DirectRpcClient} 类是线程安全的，可在多个线程中使用同一个实例。</p>
 *
 * @author heimuheimu
 */
public class DirectRpcClient implements RpcClient {

    private static final Logger RPC_CONNECTION_LOG = LoggerFactory.getLogger("NAIVERPC_CONNECTION_LOG");

    private static final Logger LOG = LoggerFactory.getLogger(DirectRpcClient.class);

    /**
     * RPC 数据 ID 生成器
     */
    private final AtomicLong packetIdGenerator = new AtomicLong();

    /**
     * Key 为 RPC 请求数据 ID，Value 为该请求数据对应的 {@link CountDownLatch} 实例
     */
    private final ConcurrentHashMap<Long, CountDownLatch> latchMap = new ConcurrentHashMap<>();

    /**
     * Key 为 RPC 请求数据 ID，Value 为该请求数据对应的 RPC 响应数据
     */
    private final ConcurrentHashMap<Long, RpcPacket> resultMap = new ConcurrentHashMap<>();

    /**
     * 提供 RPC 服务的主机地址，由主机名和端口组成，":"符号分割，例如：localhost:4182
     */
    private final String host;

    /**
     * RPC 调用默认超时时间，单位：毫秒
     */
    private final int timeout;

    /**
     * Java 对象与字节数组转换器
     */
    private final Transcoder transcoder;

    /**
     * RPC 调用过慢最小时间，单位：纳秒，不能小于等于0，RPC 调用时间大于该值时，
     * 将会触发 {@link DirectRpcClientListener#onSlowExecution(String, Method, Object[], long)} 事件
     */
    private final long slowExecutionThreshold;

    /**
     * RPC 服务调用方 与 RPC 服务提供方进行数据交互的管道
     */
    private final RpcChannel rpcChannel;

    /**
     * {@code DirectRpcClient} 事件监听器封装类，捕获监听器执行错误
     */
    private final RpcClientListenerWrapper rpcClientListenerWrapper;

    /**
     * 当前 RPC 服务调用客户端使用的操作执行信息监控器
     */
    private final ExecutionMonitor executionMonitor;

    /**
     * 连续 {@link TimeoutException} 异常出现次数
     */
    private volatile long continuousTimeoutExceptionTimes = 0;

    /**
     * 最后一次出现 {@link TimeoutException} 异常的时间戳
     */
    private volatile long lastTimeoutExceptionTime = 0;

    /**
     * 构造一个 RPC 服务调用方使用的直连客户端，{@link Socket} 配置信息使用 {@link SocketConfiguration#DEFAULT}，RPC 调用超时时间设置为 5 秒，
     * 最小压缩字节数设置为 64 KB，RPC 调用过慢最小时间设置为 50 毫秒，心跳检测时间设置为 30 秒。
     *
     * @param host 提供 RPC 服务的主机地址，由主机名和端口组成，":"符号分割，例如：localhost:4182
     * @param clientListener {@code DirectRpcClient} 事件监听器，允许为 {@code null}
     * @param unusableServiceNotifier {@code DirectRpcClient} 不可用通知器，允许为 {@code null}
     * @throws IllegalArgumentException 如果提供 RPC 服务的主机地址不符合规则，将会抛出此异常
     * @throws BuildSocketException 如果创建 {@link Socket} 过程中发生错误，将会抛出此异常
     */
    public DirectRpcClient(String host, DirectRpcClientListener clientListener,
                           UnusableServiceNotifier<DirectRpcClient> unusableServiceNotifier)
            throws IllegalArgumentException, BuildSocketException {
        this(host, null, 5000, 64 * 1024, 50, 30, clientListener, unusableServiceNotifier);
    }

    /**
     * 构造一个 RPC 服务调用方使用的直连客户端。
     *
     * @param host 提供 RPC 服务的主机地址，由主机名和端口组成，":"符号分割，例如：localhost:4182
     * @param configuration {@link Socket} 配置信息，如果传 {@code null}，将会使用 {@link SocketConfiguration#DEFAULT} 配置信息
     * @param timeout RPC 调用超时时间，单位：毫秒，不能小于等于 0
     * @param compressionThreshold 最小压缩字节数，当数据 body 字节数小于或等于该值，不进行压缩，不能小于等于 0
     * @param slowExecutionThreshold RPC 调用过慢最小时间，单位：毫秒，不能小于等于 0，RPC 调用时间大于该值时，将会触发 {@link DirectRpcClientListener#onSlowExecution(String, Method, Object[], long)} 事件
     * @param heartbeatPeriod 心跳检测时间，单位：秒，在该周期时间内没有任何数据交互，将会发送一个心跳请求数据，如果该值小于等于 0，则不进行检测
     * @param clientListener {@code DirectRpcClient} 事件监听器，允许为 {@code null}
     * @param unusableServiceNotifier {@code DirectRpcClient} 不可用通知器，允许为 {@code null}
     * @throws IllegalArgumentException 如果 RPC 调用超时时间小于等于 0，将会抛出此异常
     * @throws IllegalArgumentException 如果最小压缩字节数小于等于 0，将会抛出此异常
     * @throws IllegalArgumentException 如果 RPC 调用过慢最小时间小于等于 0，将会抛出此异常
     * @throws IllegalArgumentException 如果提供 RPC 服务的主机地址不符合规则，将会抛出此异常
     * @throws BuildSocketException 如果创建 {@link Socket} 过程中发生错误，将会抛出此异常
     */
    public DirectRpcClient(String host, SocketConfiguration configuration, int timeout, int compressionThreshold,
                           int slowExecutionThreshold, int heartbeatPeriod, DirectRpcClientListener clientListener,
                           UnusableServiceNotifier<DirectRpcClient> unusableServiceNotifier)
            throws IllegalArgumentException, BuildSocketException {
        if (timeout <= 0) {
            LOG.error("Create DirectRpcClient failed: `timeout could not be equal or less than 0`. Host: `" + host + "`. SocketConfiguration: `"
                    + configuration + "`. Timeout: `" + timeout + "`. CompressionThreshold: `" + compressionThreshold +
                    "`. SlowExecutionThreshold: `" + slowExecutionThreshold + "`. HeartbeatPeriod: `" + heartbeatPeriod +
                    "`. DirectRpcClientListener: `" + clientListener + "`.");
            throw new IllegalArgumentException("Create DirectRpcClient failed: `timeout could not be equal or less than 0`. Host: `" + host + "`. SocketConfiguration: `"
                    + configuration + "`. Timeout: `" + timeout + "`. CompressionThreshold: `" + compressionThreshold +
                    "`. SlowExecutionThreshold: `" + slowExecutionThreshold + "`. HeartbeatPeriod: `" + heartbeatPeriod +
                    "`. DirectRpcClientListener: `" + clientListener + "`.");
        }
        if (compressionThreshold <= 0) {
            LOG.error("Create DirectRpcClient failed: `CompressionThreshold could not be equal or less than 0`. Host: `" + host + "`. SocketConfiguration: `"
                    + configuration + "`. Timeout: `" + timeout + "`. CompressionThreshold: `" + compressionThreshold +
                    "`. SlowExecutionThreshold: `" + slowExecutionThreshold + "`. HeartbeatPeriod: `" + heartbeatPeriod +
                    "`. DirectRpcClientListener: `" + clientListener + "`.");
            throw new IllegalArgumentException("Create DirectRpcClient failed: `CompressionThreshold could not be equal or less than 0`. Host: `" + host + "`. SocketConfiguration: `"
                    + configuration + "`. Timeout: `" + timeout + "`. CompressionThreshold: `" + compressionThreshold +
                    "`. SlowExecutionThreshold: `" + slowExecutionThreshold + "`. HeartbeatPeriod: `" + heartbeatPeriod +
                    "`. DirectRpcClientListener: `" + clientListener + "`.");
        }
        if (slowExecutionThreshold <= 0) {
            LOG.error("Create DirectRpcClient failed: `SlowExecutionThreshold could not be equal or less than 0`. Host: `" + host + "`. SocketConfiguration: `"
                    + configuration + "`. Timeout: `" + timeout + "`. CompressionThreshold: `" + compressionThreshold +
                    "`. SlowExecutionThreshold: `" + slowExecutionThreshold + "`. HeartbeatPeriod: `" + heartbeatPeriod +
                    "`. DirectRpcClientListener: `" + clientListener + "`.");
            throw new IllegalArgumentException("Create DirectRpcClient failed: `SlowExecutionThreshold could not be equal or less than 0`. Host: `" + host + "`. SocketConfiguration: `"
                    + configuration + "`. Timeout: `" + timeout + "`. CompressionThreshold: `" + compressionThreshold +
                    "`. SlowExecutionThreshold: `" + slowExecutionThreshold + "`. HeartbeatPeriod: `" + heartbeatPeriod +
                    "`. DirectRpcClientListener: `" + clientListener + "`.");
        }
        this.host = host;
        this.timeout = timeout;
        this.transcoder = new SimpleTranscoder(compressionThreshold, RpcClientCompressionMonitorFactory.get());
        //将毫秒转换为纳秒
        this.slowExecutionThreshold = TimeUnit.NANOSECONDS.convert(slowExecutionThreshold, TimeUnit.MILLISECONDS);
        this.executionMonitor = RpcClientExecutionMonitorFactory.get(host);
        this.rpcChannel = new RpcChannel(host, configuration, heartbeatPeriod, unusableChannel -> {
            if (unusableChannel.isClosed()) { // 释放所有等待 RPC 命令
                for (CountDownLatch latch : latchMap.values()) {
                    latch.countDown();
                }
            }
            if (unusableServiceNotifier != null) {
                unusableServiceNotifier.onClosed(this);
            }
        }, (targetChannel, receivedPacket) -> {
                if (receivedPacket.isResponsePacket() && receivedPacket.getOpcode() == OperationCode.REMOTE_PROCEDURE_CALL) {
                    long packetId = ByteUtil.readLong(receivedPacket.getHeader(), 8);
                    CountDownLatch latch = latchMap.remove(packetId);
                    if (latch != null) {
                        resultMap.put(packetId, receivedPacket);
                        latch.countDown();
                    }
                } else { //should not happen
                    LOG.error("Unrecognized rpc packet: `{}`", receivedPacket);
                }
        });
        this.rpcChannel.init();
        this.rpcClientListenerWrapper = new RpcClientListenerWrapper(clientListener);
    }

    @Override
    public Object execute(Method method, Object[] args) throws IllegalStateException, TimeoutException, TooBusyException, RpcException {
        return execute(method, args, timeout);
    }

    @Override
    public Object execute(Method method, Object[] args, long timeout) throws IllegalStateException, TimeoutException, TooBusyException, RpcException {
        long startTime = System.nanoTime();
        try {
            if (timeout <= 0) {
                LOG.error("RPC execute failed: `timeout could not be equal or less than 0`. Timeout: `" + timeout + "`. Method: `"
                        + method + "`. Arguments: `" + Arrays.toString(args) + "`. DirectRpcClient: `" + this + "`.");
                rpcClientListenerWrapper.onError(host, method, args);
                executionMonitor.onError(RpcClientExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR);
                throw new RpcException("RPC execute failed: `timeout could not be equal or less than 0`. Timeout: `" + timeout
                        + "`. Method: `" + method + "`. Arguments: `" + Arrays.toString(args) + "`. DirectRpcClient: `" + this + "`.");
            }
            validateArguments(startTime, method, args);
            RpcRequestMessage rpcRequestMessage = new RpcRequestMessage();
            rpcRequestMessage.setTargetClass(method.getDeclaringClass().getName());
            rpcRequestMessage.setMethodUniqueName(ReflectUtil.getMethodUniqueName(method));
            rpcRequestMessage.setArguments(args);
            if (!rpcChannel.isActive()) {
                LOG.error("RPC execute failed: `inactive rpc channel`. Timeout: `" + timeout + "`. Method: `"
                        + method + "`. Arguments: `" + Arrays.toString(args) + "`. DirectRpcClient: `" + this + "`.");
                rpcClientListenerWrapper.onClosed(host, method, args);
                executionMonitor.onError(RpcClientExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR);
                throw new IllegalStateException("RPC execute failed: `inactive rpc channel`. Timeout: `" + timeout + "`. Method: `"
                        + method + "`. Arguments: `" + Arrays.toString(args) + "`. DirectRpcClient: `" + this + "`.");
            }

            long packetId = packetIdGenerator.incrementAndGet();
            RpcPacket rpcPacket;
            try {
                rpcPacket = RpcPacketBuilder.buildRequestPacket(packetId, OperationCode.REMOTE_PROCEDURE_CALL, rpcRequestMessage, transcoder);
            } catch (Exception e) {
                LOG.error("RPC execute failed: `build RpcPacket failed`. Timeout: `" + timeout + "`. Method: `"
                        + method + "`. Arguments: `" + Arrays.toString(args) + "`. DirectRpcClient: `" + this + "`.", e);
                rpcClientListenerWrapper.onError(host, method, args);
                executionMonitor.onError(RpcClientExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR);
                throw new RpcException("RPC execute failed: `build RpcPacket failed`. Timeout: `" + timeout + "`. Method: `"
                        + method + "`. Arguments: `" + Arrays.toString(args) + "`. DirectRpcClient: `" + this + "`.", e);
            }
            CountDownLatch latch = new CountDownLatch(1);
            latchMap.put(packetId, latch);
            rpcChannel.send(rpcPacket);
            boolean latchFlag;
            try {
                latchFlag = latch.await(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                latchFlag = false; //never happened
            }
            if (latchFlag) {
                RpcPacket responsePacket = resultMap.remove(packetId);
                if (responsePacket != null) {
                    byte status = responsePacket.getResponseStatus();
                    if (status == ResponseStatusCode.SUCCESS) {
                        try {
                            return transcoder.decode(responsePacket.getBody(), responsePacket.getSerializationType(), responsePacket.getCompressionType());
                        } catch (Exception e) {
                            LOG.error("RPC execute failed: `decode response packet failed`. Timeout: `" + timeout + "`. Method: `"
                                    + method + "`. Arguments: `" + Arrays.toString(args) + "`. DirectRpcClient: `" + this + "`.", e);
                            rpcClientListenerWrapper.onError(host, method, args);
                            executionMonitor.onError(RpcClientExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR);
                            throw new RpcException("RPC execute failed: `decode response packet failed`. Timeout: `" + timeout + "`. Method: `"
                                    + method + "`. Arguments: `" + Arrays.toString(args) + "`. DirectRpcClient: `" + this + "`.", e);
                        }
                    } else if (status == ResponseStatusCode.TOO_BUSY) {
                        LOG.error("RPC execute failed: `too busy`. Timeout: `" + timeout + "`. Method: `"
                                + method + "`. Arguments: `" + Arrays.toString(args) + "`. DirectRpcClient: `" + this + "`.");
                        rpcClientListenerWrapper.onTooBusy(host, method, args);
                        executionMonitor.onError(RpcClientExecutionMonitorFactory.ERROR_CODE_TOO_BUSY);
                        throw new TooBusyException("RPC execute failed: `too busy`. Timeout: `" + timeout + "`. Method: `"
                                + method + "`. Arguments: `" + Arrays.toString(args) + "`. DirectRpcClient: `" + this + "`.");
                    } else {
                        String errorMessage;
                        switch (status) {
                            case ResponseStatusCode.INVOCATION_TARGET_ERROR:
                                try {
                                    errorMessage = transcoder.decode(responsePacket.getBody(), responsePacket.getSerializationType(), responsePacket.getCompressionType());
                                } catch (Exception e) {
                                    errorMessage = "decode error message failed";
                                }
                                rpcClientListenerWrapper.onInvocationTargetError(host, method, args, errorMessage);
                                break;
                            case ResponseStatusCode.CLASS_NOT_FOUND:
                                errorMessage = "class not found";
                                rpcClientListenerWrapper.onClassNotFound(host, method, args);
                                break;
                            case ResponseStatusCode.NO_SUCH_METHOD:
                                errorMessage = "no such method";
                                rpcClientListenerWrapper.onNoSuchMethod(host, method, args);
                                break;
                            case ResponseStatusCode.ILLEGAL_ARGUMENT:
                                errorMessage = "illegal argument";
                                rpcClientListenerWrapper.onIllegalArgument(host, method, args);
                                break;
                            case ResponseStatusCode.INTERNAL_ERROR:
                                errorMessage = "server internal error";
                                rpcClientListenerWrapper.onError(host, method, args);
                                break;
                            default:
                                errorMessage = "unrecognized response status code [" + status + "]";
                                rpcClientListenerWrapper.onError(host, method, args);
                        }
                        LOG.error("RPC execute failed: `" + errorMessage + "`. See the rpc server log for more information. Timeout: `"
                                + timeout + "`. Method: `" + method + "`. Arguments: `" + Arrays.toString(args)
                                + "`. DirectRpcClient: `" + this + "`.");
                        executionMonitor.onError(RpcClientExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR);
                        throw new RpcException("RPC execute failed: `" + errorMessage + "`. See the rpc server log for more information. Timeout: `"
                                + timeout + "`. Method: `" + method + "`. Arguments: `" + Arrays.toString(args)
                                + "`. DirectRpcClient: `" + this + "`.");
                    }
                } else {
                    LOG.error("RPC execute failed: `empty response packet`. Timeout: `" + timeout + "`. Method: `" + method
                            + "`. Arguments: `" + Arrays.toString(args) + "`. DirectRpcClient: `" + this + "`.");
                    rpcClientListenerWrapper.onError(host, method, args);
                    executionMonitor.onError(RpcClientExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR);
                    throw new RpcException("RPC execute failed: `empty response packet`. Timeout: `" + timeout + "`. Method: `" + method
                            + "`. Arguments: `" + Arrays.toString(args) + "`. DirectRpcClient: `" + this + "`.");
                }
            } else {
                latchMap.remove(packetId);
                LOG.error("RPC execute failed: `wait response timeout`. Timeout: `" + timeout + "`. Method: `" + method
                        + "`. Arguments: `" + Arrays.toString(args) + "`. DirectRpcClient: `" + this + "`.");
                //如果两次超时异常发生在 1s 以内，则认为是连续失败
                if (System.currentTimeMillis() - lastTimeoutExceptionTime < 1000) {
                    continuousTimeoutExceptionTimes ++;
                } else {
                    continuousTimeoutExceptionTimes = 1;
                }
                lastTimeoutExceptionTime = System.currentTimeMillis();
                //如果连续超时异常出现次数大于 50 次，认为当前连接出现异常，关闭当前连接
                if (continuousTimeoutExceptionTimes > 50) {
                    RPC_CONNECTION_LOG.error("DirectRpcClient need to be closed due to: `Too many timeout exceptions[{}]`. Host: `{}`.",
                            continuousTimeoutExceptionTimes, host);
                    close();
                }
                rpcClientListenerWrapper.onTimeout(host, method, args);
                executionMonitor.onError(RpcClientExecutionMonitorFactory.ERROR_CODE_TIMEOUT);
                throw new TimeoutException("RPC execute failed: `wait response timeout`. Timeout: `" + timeout + "`. Method: `"
                        + method + "`. Arguments: `" + Arrays.toString(args) + "`. DirectRpcClient: `" + this + "`.");
            }
        } finally {
            long executedNanoTime = System.nanoTime() - startTime;
            if (executedNanoTime > slowExecutionThreshold) {
                rpcClientListenerWrapper.onSlowExecution(host, method, args, executedNanoTime);
            }
            executionMonitor.onExecuted(startTime);
        }
    }

    @SuppressWarnings("unchecked")
    private void validateArguments(long startTime, Method method, Object[] args) throws RpcException {
        if (method == null) {
            LOG.error("RPC execute failed: `method could not be null`. Method: `null`. Arguments: `"
                    + Arrays.toString(args) + "`. DirectRpcClient: `" + this + "`.");
            rpcClientListenerWrapper.onError(host, null, args);
            executionMonitor.onError(RpcClientExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR);
            throw new RpcException("RPC execute failed: `method could not be null`. Method: `null`. Arguments: `"
                    + Arrays.toString(args) + "`. DirectRpcClient: `" + this + "`.");
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        int argsLength = args != null ? args.length : 0;
        if (parameterTypes.length != argsLength) {
            LOG.error("RPC execute failed: `wrong argument size`. Method: `" + method + "`. Arguments: `"
                    + Arrays.toString(args) + "`. DirectRpcClient: `" + this + "`.");
            rpcClientListenerWrapper.onError(host, method, args);
            executionMonitor.onError(RpcClientExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR);
            throw new RpcException("RPC execute failed: `wrong argument size`. Method: `" + method + "`. Arguments: `"
                    + Arrays.toString(args) + "`. DirectRpcClient: `" + this + "`.");
        }
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg != null && !(arg instanceof Serializable)) {
                    if (arg instanceof List) {
                        args[i] = new ArrayList((List)arg);
                        LOG.warn("RPC execute warning: `not serializable List`. Method: `" + method + "`. Arguments: `" + Arrays.toString(args) + ".");
                    } else if (arg instanceof Map) {
                        args[i] = new HashMap((Map)arg);
                        LOG.warn("RPC execute warning: `not serializable Map`. Method: `" + method + "`. Arguments: `" + Arrays.toString(args) + ".");
                    } else if (arg instanceof Set) {
                        args[i] = new HashSet((Set)arg);
                        LOG.warn("RPC execute warning: `not serializable Set`. Method: `" + method + "`. Arguments: `" + Arrays.toString(args) + ".");
                    } else {
                        LOG.error("RPC execute failed: `not serializable argument`. Method: `" + method + "`. Arguments: `"
                                + Arrays.toString(args) + "`. DirectRpcClient: `" + this + "`.");
                        rpcClientListenerWrapper.onError(host, method, args);
                        executionMonitor.onError(RpcClientExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR);
                        throw new RpcException("RPC execute failed: `not serializable argument`. Method: `" + method + "`. Arguments: `"
                                + Arrays.toString(args) + "`. DirectRpcClient: `" + this + "`.");
                    }
                }
            }
        }
    }

    @Override
    public void close() {
        rpcChannel.close();
    }

    /**
     * 判断是否已接收到 RPC 服务提供方发送的下线操作请求。
     *
     * @return 是否已接收到 RPC 服务提供方发送的下线操作请求
     */
    public boolean isOffline() {
        return rpcChannel.isOffline();
    }

    /**
     * 判断当前 {@code DirectRpcClient} 是否可用。
     *
     * @return 当前 {@code DirectRpcClient} 是否可用
     */
    public boolean isActive() {
        return rpcChannel.isActive();
    }

    /**
     * 获得 RPC 服务提供方的远程主机地址，由主机名和端口组成，":"符号分割，例如：localhost:4182。
     *
     * @return RPC 服务提供方的远程主机地址，由主机名和端口组成，":"符号分割，例如：localhost:4182
     */
    public String getHost() {
        return host;
    }

    @Override
    public String toString() {
        return "DirectRpcClient{" +
                "packetIdGenerator=" + packetIdGenerator +
                ", host='" + host + '\'' +
                ", timeout=" + timeout +
                ", slowExecutionThreshold=" + slowExecutionThreshold +
                ", rpcChannel=" + rpcChannel +
                ", rpcClientListenerWrapper=" + rpcClientListenerWrapper +
                ", continuousTimeoutExceptionTimes=" + continuousTimeoutExceptionTimes +
                ", lastTimeoutExceptionTime=" + lastTimeoutExceptionTime +
                '}';
    }

    /**
     * {@code DirectRpcClient} 事件监听器封装类，捕获监听器执行错误。
     */
    private static class RpcClientListenerWrapper implements DirectRpcClientListener {

        private final DirectRpcClientListener directRpcClientListener;

        private RpcClientListenerWrapper(DirectRpcClientListener directRpcClientListener) {
            this.directRpcClientListener = directRpcClientListener;
        }

        @Override
        public void onClassNotFound(String host, Method method, Object[] args) {
            if (directRpcClientListener != null) {
                try {
                    directRpcClientListener.onClassNotFound(host, method, args);
                } catch (Exception e) {
                    LOG.error("Call DirectRpcClientListener#onClassNotFound() failed. Host: `" + host + "`. Method: `"
                        + method + "`. Arguments: `" + Arrays.toString(args) + "`.", e);
                }
            }
        }

        @Override
        public void onNoSuchMethod(String host, Method method, Object[] args) {
            if (directRpcClientListener != null) {
                try {
                    directRpcClientListener.onNoSuchMethod(host, method, args);
                } catch (Exception e) {
                    LOG.error("Call DirectRpcClientListener#onNoSuchMethod() failed. Host: `" + host + "`. Method: `"
                            + method + "`. Arguments: `" + Arrays.toString(args) + "`.", e);
                }
            }
        }

        @Override
        public void onIllegalArgument(String host, Method method, Object[] args) {
            if (directRpcClientListener != null) {
                try {
                    directRpcClientListener.onIllegalArgument(host, method, args);
                } catch (Exception e) {
                    LOG.error("Call DirectRpcClientListener#onIllegalArgument() failed. Host: `" + host + "`. Method: `"
                            + method + "`. Arguments: `" + Arrays.toString(args) + "`.", e);
                }
            }
        }

        @Override
        public void onInvocationTargetError(String host, Method method, Object[] args, String errorMessage) {
            if (directRpcClientListener != null) {
                try {
                    directRpcClientListener.onInvocationTargetError(host, method, args, errorMessage);
                } catch (Exception e) {
                    LOG.error("Call DirectRpcClientListener#onInvocationTargetError() failed. Host: `" + host + "`. Method: `"
                            + method + "`. Arguments: `" + Arrays.toString(args) + "`. Error message: `" + errorMessage + "`.", e);
                }
            }
        }

        @Override
        public void onTimeout(String host, Method method, Object[] args) {
            if (directRpcClientListener != null) {
                try {
                    directRpcClientListener.onTimeout(host, method, args);
                } catch (Exception e) {
                    LOG.error("Call DirectRpcClientListener#onTimeout() failed. Host: `" + host + "`. Method: `"
                            + method + "`. Arguments: `" + Arrays.toString(args) + "`.", e);
                }
            }
        }

        @Override
        public void onError(String host, Method method, Object[] args) {
            if (directRpcClientListener != null) {
                try {
                    directRpcClientListener.onError(host, method, args);
                } catch (Exception e) {
                    LOG.error("Call DirectRpcClientListener#onError() failed. Host: `" + host + "`. Method: `"
                            + method + "`. Arguments: `" + Arrays.toString(args) + "`.", e);
                }
            }
        }

        @Override
        public void onTooBusy(String host, Method method, Object[] args) {
            if (directRpcClientListener != null) {
                try {
                    directRpcClientListener.onTooBusy(host, method, args);
                } catch (Exception e) {
                    LOG.error("Call DirectRpcClientListener#onTooBusy() failed. Host: `" + host + "`. Method: `"
                            + method + "`. Arguments: `" + Arrays.toString(args) + "`.", e);
                }
            }
        }

        @Override
        public void onClosed(String host, Method method, Object[] args) {
            if (directRpcClientListener != null) {
                try {
                    directRpcClientListener.onClosed(host, method, args);
                } catch (Exception e) {
                    LOG.error("Call DirectRpcClientListener#onClosed() failed. Host: `" + host + "`. Method: `"
                            + method + "`. Arguments: `" + Arrays.toString(args) + "`.", e);
                }
            }
        }

        @Override
        public void onSlowExecution(String host, Method method, Object[] args, long executedNanoTime) {
            if (directRpcClientListener != null) {
                try {
                    directRpcClientListener.onSlowExecution(host, method, args, executedNanoTime);
                } catch (Exception e) {
                    LOG.error("Call DirectRpcClientListener#onSlowExecution() failed. Host: `" + host + "`. Method: `"
                            + method + "`. Arguments: `" + Arrays.toString(args) + "`. ExecutedNanoTime: `" + executedNanoTime + "`.", e);
                }
            }
        }

    }

}
