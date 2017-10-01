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
import com.heimuheimu.naiverpc.channel.RpcChannelListenerSkeleton;
import com.heimuheimu.naiverpc.constant.OperationCode;
import com.heimuheimu.naiverpc.constant.ResponseStatusCode;
import com.heimuheimu.naiverpc.exception.RpcException;
import com.heimuheimu.naiverpc.exception.TimeoutException;
import com.heimuheimu.naiverpc.exception.TooBusyException;
import com.heimuheimu.naiverpc.message.RpcRequestMessage;
import com.heimuheimu.naiverpc.monitor.client.RpcClientCompressionMonitorFactory;
import com.heimuheimu.naiverpc.monitor.client.RpcClientExecutionMonitorFactory;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RPC 服务调用直连客户端
 *
 * <p><strong>说明：</strong>{@code DirectRpcClient} 类是线程安全的，可在多个线程中使用同一个实例。</p>
 *
 * @author heimuheimu
 */
public class DirectRpcClient implements RpcClient {

    private static final Logger RPC_CONNECTION_LOG = LoggerFactory.getLogger("NAIVERPC_CONNECTION_LOG");

    private static final Logger LOG = LoggerFactory.getLogger(DirectRpcClient.class);

    /**
     * RPC 数据包 ID 生成器
     */
    private final AtomicLong packetIdGenerator = new AtomicLong();

    /**
     * Key 为 RPC 请求数据包 ID，Value 为该请求数据包对应的 {@link CountDownLatch} 实例
     */
    private final ConcurrentHashMap<Long, CountDownLatch> latchMap = new ConcurrentHashMap<>();

    /**
     * Key 为 RPC 请求数据包 ID，Value 为该请求数据包对应的 RPC 响应数据包
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
     * RPC 服务调用方 与 RPC 服务提供方进行数据交互的管道
     */
    private final RpcChannel rpcChannel;

    /**
     * Java 对象与字节数组转换器
     */
    private final Transcoder transcoder;

    /**
     * RPC 服务调用客户端监听器封装类，捕获监听器执行错误
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
     * 构造一个 RPC 服务调用直连客户端
     * <p>该客户端的 RPC 服务调用超时时间设置为 5 秒，最小压缩字节数设置为 64 KB，心跳检测时间为 30 秒</p>
     * @param host 提供 RPC 服务的主机地址，由主机名和端口组成，":"符号分割，例如：localhost:4182
     * @throws IllegalArgumentException 如果提供 RPC 服务的主机地址不符合规则，将会抛出此异常
     * @throws RuntimeException 如果创建 {@link RpcChannel} 过程中发生错误，将会抛出此异常
     */
    public DirectRpcClient(String host) throws RuntimeException {
        this(host, null, 5000, 64 * 1024, 30, null);
    }

    /**
     * 构造一个 RPC 服务调用直连客户端
     *
     * @param host 提供 RPC 服务的主机地址，由主机名和端口组成，":"符号分割，例如：localhost:4182
     * @param configuration Socket 配置信息，允许为 {@code null}，如果传 {@code null}，将会使用 {@link SocketConfiguration#DEFAULT} 配置信息
     * @param timeout RPC 服务调用超时时间，单位：毫秒，不能小于等于0
     * @param compressionThreshold 最小压缩字节数，当 数据包 body 字节数小于或等于该值，不进行压缩，不能小于等于0
     * @param heartbeatPeriod 心跳检测时间，单位：秒，在该周期时间内当前管道如果没有任何数据交互，将会发送一个心跳请求数据包，如果该值小于等于 0，则不进行检测
     * @param clientListener RPC 服务调用客户端监听器，允许为 {@code null}
     * @throws IllegalArgumentException 如果 timeout 小于等于0
     * @throws IllegalArgumentException 如果 compressionThreshold 小于等于0
     * @throws IllegalArgumentException 如果提供 RPC 服务的主机地址不符合规则，将会抛出此异常
     * @throws RuntimeException 如果创建 {@link RpcChannel} 过程中发生错误，将会抛出此异常
     */
    public DirectRpcClient(String host, SocketConfiguration configuration, int timeout, int compressionThreshold,
                           int heartbeatPeriod, RpcClientListener clientListener)
        throws RuntimeException {
        if (timeout <= 0) {
            LOG.error("Create DirectRpcClient failed. Timeout could not be equal or less than 0. Host: `" + host + "`. Configuration: `"
                    + configuration + "`. Timeout: `" + timeout + "`. CompressionThreshold: `" + compressionThreshold + "`. ClientListener: `"
                    + clientListener + "`.");
            throw new IllegalArgumentException("Create DirectRpcClient failed. Timeout could not be equal or less than 0. Host: `" + host + "`. Configuration: `"
                    + configuration + "`. Timeout: `" + timeout + "`. CompressionThreshold: `" + compressionThreshold + "`. ClientListener: `"
                    + clientListener + "`.");
        }
        if (compressionThreshold <= 0) {
            LOG.error("Create DirectRpcClient failed. CompressionThreshold could not be equal or less than 0. Host: `" + host + "`. Configuration: `"
                    + configuration + "`. Timeout: `" + timeout + "`. CompressionThreshold: `" + compressionThreshold + "`. ClientListener: `"
                    + clientListener + "`.");
            throw new IllegalArgumentException("Create DirectRpcClient failed. CompressionThreshold could not be equal or less than 0. Host: `" + host + "`. Configuration: `"
                    + configuration + "`. Timeout: `" + timeout + "`. CompressionThreshold: `" + compressionThreshold + "`. ClientListener: `"
                    + clientListener + "`.");
        }
        this.host = host;
        this.timeout = timeout;
        this.transcoder = new SimpleTranscoder(compressionThreshold, RpcClientCompressionMonitorFactory.get());
        this.executionMonitor = RpcClientExecutionMonitorFactory.get(host);
        this.rpcChannel = new RpcChannel(host, configuration, heartbeatPeriod, new RpcChannelListenerSkeleton() {

            @Override
            public void onReceiveRpcPacket(RpcChannel channel, RpcPacket packet) {
                if (packet.isResponsePacket() && packet.getOpcode() == OperationCode.REMOTE_PROCEDURE_CALL) {
                    long packetId = ByteUtil.readLong(packet.getHeader(), 8);
                    CountDownLatch latch = latchMap.remove(packetId);
                    if (latch != null) {
                        resultMap.put(packetId, packet);
                        latch.countDown();
                    }
                } else {
                    LOG.error("Unrecognized rpc packet: `{}`", packet);
                }
            }

            @Override
            public void onClosed(RpcChannel channel) {
                for (CountDownLatch latch : latchMap.values()) {
                    latch.countDown();
                }
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
                LOG.error("Timeout could not be equal or less than 0. Method: `" + method + "`. Arguments: `" + Arrays.toString(args) + "`.");
                rpcClientListenerWrapper.onError(this, method, args);
                executionMonitor.onError(RpcClientExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR);
                throw new RpcException("Timeout could not be equal or less than 0. Method: `" + method + "`. Arguments: `" + Arrays.toString(args) + "`.");
            }
            validateArguments(startTime, method, args);
            RpcRequestMessage rpcRequestMessage = new RpcRequestMessage();
            rpcRequestMessage.setTargetClass(method.getDeclaringClass().getName());
            rpcRequestMessage.setMethodUniqueName(ReflectUtil.getMethodUniqueName(method));
            rpcRequestMessage.setArguments(args);
            if (!rpcChannel.isActive()) {
                LOG.error("Inactive rpc channel. Host: `" + host + "`. RpcRequestMessage: `" + rpcRequestMessage + "`.");
                rpcClientListenerWrapper.onClosed(this, method, args);
                executionMonitor.onError(RpcClientExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR);
                throw new IllegalStateException("Inactive rpc channel. Host: `" + host + "`. RpcRequestMessage: `" + rpcRequestMessage + "`.");
            }

            long packetId = packetIdGenerator.incrementAndGet();
            RpcPacket rpcPacket;
            try {
                rpcPacket = RpcPacketBuilder.buildRequestPacket(packetId, OperationCode.REMOTE_PROCEDURE_CALL, rpcRequestMessage, transcoder);
            } catch (Exception e) {
                LOG.error("Build RpcPacket failed. Host: `" + host + "`. RpcRequestMessage: `" + rpcRequestMessage + "`.", e);
                rpcClientListenerWrapper.onError(this, method, args);
                executionMonitor.onError(RpcClientExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR);
                throw new RpcException("Build RpcPacket failed. Host: `" + host + "`. RpcRequestMessage: `" + rpcRequestMessage + "`.", e);
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
                            LOG.error("Decode response packet failed. Host: `" + host + "`. RpcRequestMessage: `" + rpcRequestMessage + "`.", e);
                            rpcClientListenerWrapper.onError(this, method, args);
                            executionMonitor.onError(RpcClientExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR);
                            throw new RpcException("Decode response packet failed. Host: `" + host + "`. RpcRequestMessage: `" + rpcRequestMessage + "`.", e);
                        }
                    } else if (status == ResponseStatusCode.TOO_BUSY) {
                        LOG.error("`Too busy`. Host: `" + host + "`. RpcRequestMessage: `" + rpcRequestMessage + "`. See the rpc server log for more information.");
                        rpcClientListenerWrapper.onTooBusy(this, method, args);
                        executionMonitor.onError(RpcClientExecutionMonitorFactory.ERROR_CODE_TOO_BUSY);
                        throw new TooBusyException("`Too busy`. Host: `" + host + "`. RpcRequestMessage: `" + rpcRequestMessage + "`. See the rpc server log for more information.");
                    } else {
                        String errorMessage;
                        switch (status) {
                            case ResponseStatusCode.INVOCATION_TARGET_ERROR:
                                try {
                                    errorMessage = "Invocation target error: " + transcoder.decode(responsePacket.getBody(),
                                            responsePacket.getSerializationType(), responsePacket.getCompressionType());
                                } catch (Exception e) {
                                    errorMessage = "Invocation target error: Decode error message failed";
                                }
                                rpcClientListenerWrapper.onInvocationTargetError(this, method, args, errorMessage);
                                break;
                            case ResponseStatusCode.CLASS_NOT_FOUND:
                                errorMessage = "Class not found";
                                rpcClientListenerWrapper.onClassNotFound(this, method, args);
                                break;
                            case ResponseStatusCode.NO_SUCH_METHOD:
                                errorMessage = "No such method";
                                rpcClientListenerWrapper.onNoSuchMethod(this, method, args);
                                break;
                            case ResponseStatusCode.ILLEGAL_ARGUMENT:
                                errorMessage = "Illegal argument";
                                rpcClientListenerWrapper.onIllegalArgument(this, method, args);
                                break;
                            case ResponseStatusCode.INTERNAL_ERROR:
                                errorMessage = "Server internal error";
                                rpcClientListenerWrapper.onError(this, method, args);
                                break;
                            default:
                                errorMessage = "Unrecognized response status code: " + status;
                                rpcClientListenerWrapper.onError(this, method, args);
                        }
                        LOG.error("`" + errorMessage + "`. Host: `" + host + "`. RpcRequestMessage: `" + rpcRequestMessage + "`. See the rpc server log for more information.");
                        executionMonitor.onError(RpcClientExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR);
                        throw new RpcException("`" + errorMessage + "`. Host: `" + host + "`. RpcRequestMessage: `" + rpcRequestMessage + "`. See the rpc server log for more information.");
                    }
                } else {
                    LOG.error("Empty response packet. Host: `" + host + "`. RpcRequestMessage: `" + rpcRequestMessage + "`.");
                    rpcClientListenerWrapper.onError(this, method, args);
                    executionMonitor.onError(RpcClientExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR);
                    throw new RpcException("Empty response packet. Maybe DirectRpcClient has been closed. Host: `" + host + "`. RpcRequestMessage: `" + rpcRequestMessage + "`.");
                }
            } else {
                latchMap.remove(packetId);
                LOG.error("Wait rpc execute response timeout: `" + timeout + "ms`. Host: `"
                        + host + "`. Method: `" + method + "`. Arguments: `" + Arrays.toString(args) + "`.");
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
                rpcClientListenerWrapper.onTimeout(this, method, args);
                executionMonitor.onError(RpcClientExecutionMonitorFactory.ERROR_CODE_TIMEOUT);
                throw new TimeoutException("Wait rpc execute response timeout: `" + timeout + "ms`. Host: `"
                        + host + "`. Method: `" + method + "`. Arguments: `" + Arrays.toString(args) + "`.");
            }
        } finally {
            long executedNanoTime = System.nanoTime() - startTime;
            if (executedNanoTime > RpcClientListener.SLOW_EXECUTION_THRESHOLD) {
                rpcClientListenerWrapper.onSlowExecution(this, method, args, executedNanoTime);
            }
            executionMonitor.onExecuted(startTime);
        }
    }

    @SuppressWarnings("unchecked")
    private void validateArguments(long startTime, Method method, Object[] args) throws RpcException {
        if (method == null) {
            LOG.error("Method could not be null. Method: `null`. Arguments: `" + Arrays.toString(args) + ".");
            rpcClientListenerWrapper.onError(this, null, args);
            executionMonitor.onError(RpcClientExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR);
            throw new RpcException("Method could not be null. Method: `null`. Arguments: `" + Arrays.toString(args) + ".");
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        int argsLength = args != null ? args.length : 0;
        if (parameterTypes.length != argsLength) {
            LOG.error("Illegal argument: wrong argument size. Method: `" + method + "`. Arguments: `" + Arrays.toString(args) + ".");
            rpcClientListenerWrapper.onError(this, method, args);
            executionMonitor.onError(RpcClientExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR);
            throw new RpcException("Illegal argument: wrong argument size. Method: `" + method + "`. Arguments: `" + Arrays.toString(args) + ".");
        }
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg != null && !(arg instanceof Serializable)) {
                    if (arg instanceof List) {
                        args[i] = new ArrayList((List)arg);
                        LOG.warn("Illegal argument: Not serializable List. Method: `" + method + "`. Arguments: `" + Arrays.toString(args) + ".");
                    } else if (arg instanceof Map) {
                        args[i] = new HashMap((Map)arg);
                        LOG.warn("Illegal argument: Not serializable Map. Method: `" + method + "`. Arguments: `" + Arrays.toString(args) + ".");
                    } else if (arg instanceof Set) {
                        args[i] = new HashSet((Set)arg);
                        LOG.warn("Illegal argument: Not serializable Set. Method: `" + method + "`. Arguments: `" + Arrays.toString(args) + ".");
                    } else {
                        LOG.error("Illegal argument: Not serializable argument. Method: `" + method + "`. Arguments: `" + Arrays.toString(args) + ".");
                        rpcClientListenerWrapper.onError(this, method, args);
                        executionMonitor.onError(RpcClientExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR);
                        throw new RpcException("Illegal argument: Not serializable argument. Method: `" + method + "`. Arguments: `" + Arrays.toString(args) + ".");
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
     * 判断 RPC 服务提供方是否已发送下线请求
     *
     * @return RPC 服务提供方是否已发送下线请求
     */
    public boolean isOffline() {
        return rpcChannel.isOffline();
    }

    /**
     * 判断当前 RPC 服务调用客户端是否可用
     *
     * @return 当前 RPC 服务调用客户端是否可用
     */
    public boolean isActive() {
        return rpcChannel.isActive();
    }

    /**
     * 获得 RPC 服务提供方的远程主机地址
     *
     * @return RPC 服务提供方的远程主机地址
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
                '}';
    }

    /**
     * RPC 服务调用客户端监听器封装类，捕获监听器执行错误
     */
    private class RpcClientListenerWrapper implements RpcClientListener {

        private final RpcClientListener rpcClientListener;

        private RpcClientListenerWrapper(RpcClientListener rpcClientListener) {
            this.rpcClientListener = rpcClientListener;
        }

        @Override
        public void onClassNotFound(RpcClient client, Method method, Object[] args) {
            if (rpcClientListener != null) {
                try {
                    rpcClientListener.onClassNotFound(client, method, args);
                } catch (Exception e) {
                    LOG.error("Call RpcClientListener#onClassNotFound() failed. Client: `" + client + "`. Method: `"
                        + method + "`. Arguments: `" + Arrays.toString(args) + "`.");
                }
            }
        }

        @Override
        public void onNoSuchMethod(RpcClient client, Method method, Object[] args) {
            if (rpcClientListener != null) {
                try {
                    rpcClientListener.onNoSuchMethod(client, method, args);
                } catch (Exception e) {
                    LOG.error("Call RpcClientListener#onNoSuchMethod() failed. Client: `" + client + "`. Method: `"
                            + method + "`. Arguments: `" + Arrays.toString(args) + "`.");
                }
            }
        }

        @Override
        public void onIllegalArgument(RpcClient client, Method method, Object[] args) {
            if (rpcClientListener != null) {
                try {
                    rpcClientListener.onIllegalArgument(client, method, args);
                } catch (Exception e) {
                    LOG.error("Call RpcClientListener#onIllegalArgument() failed. Client: `" + client + "`. Method: `"
                            + method + "`. Arguments: `" + Arrays.toString(args) + "`.");
                }
            }
        }

        @Override
        public void onInvocationTargetError(RpcClient client, Method method, Object[] args, String errorMessage) {
            if (rpcClientListener != null) {
                try {
                    rpcClientListener.onInvocationTargetError(client, method, args, errorMessage);
                } catch (Exception e) {
                    LOG.error("Call RpcClientListener#onInvocationTargetError() failed. Client: `" + client + "`. Method: `"
                            + method + "`. Arguments: `" + Arrays.toString(args) + "`. Error message: `" + errorMessage + "`.");
                }
            }
        }

        @Override
        public void onTimeout(RpcClient client, Method method, Object[] args) {
            if (rpcClientListener != null) {
                try {
                    rpcClientListener.onTimeout(client, method, args);
                } catch (Exception e) {
                    LOG.error("Call RpcClientListener#onTimeout() failed. Client: `" + client + "`. Method: `"
                            + method + "`. Arguments: `" + Arrays.toString(args) + "`.");
                }
            }
        }

        @Override
        public void onError(RpcClient client, Method method, Object[] args) {
            if (rpcClientListener != null) {
                try {
                    rpcClientListener.onError(client, method, args);
                } catch (Exception e) {
                    LOG.error("Call RpcClientListener#onError() failed. Client: `" + client + "`. Method: `"
                            + method + "`. Arguments: `" + Arrays.toString(args) + "`.");
                }
            }
        }

        @Override
        public void onTooBusy(RpcClient client, Method method, Object[] args) {
            if (rpcClientListener != null) {
                try {
                    rpcClientListener.onTooBusy(client, method, args);
                } catch (Exception e) {
                    LOG.error("Call RpcClientListener#onTooBusy() failed. Client: `" + client + "`. Method: `"
                            + method + "`. Arguments: `" + Arrays.toString(args) + "`.");
                }
            }
        }

        @Override
        public void onClosed(RpcClient client, Method method, Object[] args) {
            if (rpcClientListener != null) {
                try {
                    rpcClientListener.onClosed(client, method, args);
                } catch (Exception e) {
                    LOG.error("Call RpcClientListener#onClosed() failed. Client: `" + client + "`. Method: `"
                            + method + "`. Arguments: `" + Arrays.toString(args) + "`.");
                }
            }
        }

        @Override
        public void onSlowExecution(RpcClient client, Method method, Object[] args, long executedNanoTime) {
            if (rpcClientListener != null) {
                try {
                    rpcClientListener.onSlowExecution(client, method, args, executedNanoTime);
                } catch (Exception e) {
                    LOG.error("Call RpcClientListener#onSlowExecution() failed. Client: `" + client + "`. Method: `"
                            + method + "`. Arguments: `" + Arrays.toString(args) + "`. ExecutedNanoTime: `" + executedNanoTime + "`.");
                }
            }
        }

    }

}
