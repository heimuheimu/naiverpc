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

package com.heimuheimu.naiverpc.server.executors;

import com.heimuheimu.naivemonitor.monitor.ExecutionMonitor;
import com.heimuheimu.naivemonitor.monitor.ThreadPoolMonitor;
import com.heimuheimu.naiverpc.channel.RpcChannel;
import com.heimuheimu.naiverpc.constant.ResponseStatusCode;
import com.heimuheimu.naiverpc.message.RpcRequestMessage;
import com.heimuheimu.naiverpc.monitor.server.RpcServerCompressionMonitorFactory;
import com.heimuheimu.naiverpc.monitor.server.RpcServerExecutionMonitorFactory;
import com.heimuheimu.naiverpc.monitor.server.RpcServerThreadPoolMonitorFactory;
import com.heimuheimu.naiverpc.packet.RpcPacket;
import com.heimuheimu.naiverpc.packet.RpcPacketBuilder;
import com.heimuheimu.naiverpc.server.RpcExecutor;
import com.heimuheimu.naiverpc.server.RpcExecutorListener;
import com.heimuheimu.naiverpc.transcoder.SimpleTranscoder;
import com.heimuheimu.naiverpc.transcoder.Transcoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.*;

/**
 * 基于 JDK 反射类库实现的 {@link RpcExecutor}。
 *
 * <h3>监听器</h3>
 * <blockquote>
 * 当 RPC 执行时出现异常、执行过慢等事件时，均会触发 {@link RpcExecutorListener} 相应的事件进行通知。
 * </blockquote>
 *
 * <h3>数据监控</h3>
 * <blockquote>
 * 可通过 {@link RpcServerThreadPoolMonitorFactory} 获取 RPC 执行使用的线程池信息监控数据。<br>
 * 可通过 {@link RpcServerExecutionMonitorFactory} 获取 RPC 执行信息监控数据。
 * </blockquote>
 *
 * <p><strong>说明：</strong>{@code AsyncJdkRpcExecutor} 类是线程安全的，可在多个线程中使用同一个实例。</p>
 *
 * @author heimuheimu
 */
public class AsyncJdkRpcExecutor implements RpcExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncJdkRpcExecutor.class);

    /**
     * RPC 执行线程池最大数量
     */
    private final int maximumPoolSize;

    /**
     * RPC 执行的线程池
     */
    private final ThreadPoolExecutor executorService;

    /**
     * Java 对象与字节数组转换器
     */
    private final Transcoder transcoder;

    /**
     * RPC 执行过慢最小时间，单位：纳秒，RPC 调用时间大于该值时，
     * 将会触发 {@link RpcExecutorListener#onSlowExecution(RpcRequestMessage, long)} 事件
     */
    private final long slowExecutionThreshold;

    /**
     * {@code RpcExecutor} 事件监听器
     */
    private final RpcExecutorListener rpcExecutorListener;

    /**
     * RPC 执行的线程池信息监控器
     */
    private final ThreadPoolMonitor threadPoolMonitor;

    /**
     * RPC 执行信息监控器
     */
    private final ExecutionMonitor executionMonitor;

    /**
     * 已在 {@code AsyncJdkRpcExecutor} 注册的 RPC 服务画像 {@code Map}，Key 为 RPC 服务接口名称，Value 为该接口对应的 RPC 服务画像
     */
    private final ConcurrentHashMap<String, RpcServiceDepiction> depictionMap = new ConcurrentHashMap<>();

    /**
     * 构造一个 {@code AsyncJdkRpcExecutor} ，用于执行 RPC 方法。
     *
     * @param listenPort 通过该执行器提供 RPC 服务的 {@code RpcServer} 监听端口
     * @param compressionThreshold 最小压缩字节数，当数据 body 字节数小于或等于该值，不进行压缩，不能小于等于 0
     * @param slowExecutionThreshold RPC 执行过慢最小时间，单位：纳秒，不能小于等于 0，RPC 调用时间大于该值时，将会触发 {@link RpcExecutorListener#onSlowExecution(RpcRequestMessage, long)} 事件
     * @param rpcExecutorListener {@code RpcExecutor} 事件监听器，允许为 {@code null}
     * @param maximumPoolSize RPC 执行线程池最大数量，不能小于等于 0
     * @throws IllegalArgumentException 如果创建 {@code AsyncJdkRpcExecutor} 使用的最小压缩字节数小于等于 0，将会抛出此异常
     * @throws IllegalArgumentException 如果创建 {@code AsyncJdkRpcExecutor} 使用的 RPC 执行过慢最小时间小于等于 0，将会抛出此异常
     * @throws IllegalArgumentException 如果创建 {@code AsyncJdkRpcExecutor} 使用的线程池最大数量小于等于 0，将会抛出此异常
     */
    public AsyncJdkRpcExecutor(int listenPort, int compressionThreshold, int slowExecutionThreshold,
            RpcExecutorListener rpcExecutorListener, int maximumPoolSize) throws IllegalArgumentException {
        if (compressionThreshold <= 0) {
            LOG.error("Create AsyncJdkRpcExecutor failed: `compressionThreshold could not be equal or less than 0`. Port: `" + listenPort
                    + "`. CompressionThreshold: `" + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold + "`. RpcExecutorListener: `"
                    + rpcExecutorListener + "`. MaximumPoolSize: `" + maximumPoolSize + "`.");
            throw new IllegalArgumentException("Create AsyncJdkRpcExecutor failed: `compressionThreshold could not be equal or less than 0`. Port: `" + listenPort
                    + "`. CompressionThreshold: `" + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold + "`. RpcExecutorListener: `"
                    + rpcExecutorListener + "`. MaximumPoolSize: `" + maximumPoolSize + "`.");
        }
        if (slowExecutionThreshold <= 0) {
            LOG.error("Create AsyncJdkRpcExecutor failed: `slowExecutionThreshold could not be equal or less than 0`. Port: `" + listenPort
                    + "`. CompressionThreshold: `" + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold + "`. RpcExecutorListener: `"
                    + rpcExecutorListener + "`. MaximumPoolSize: `" + maximumPoolSize + "`.");
            throw new IllegalArgumentException("Create AsyncJdkRpcExecutor failed: `slowExecutionThreshold could not be equal or less than 0`. Port: `" + listenPort
                    + "`. CompressionThreshold: `" + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold + "`. RpcExecutorListener: `"
                    + rpcExecutorListener + "`. MaximumPoolSize: `" + maximumPoolSize + "`.");
        }
        if (maximumPoolSize <= 0) {
            LOG.error("Create AsyncJdkRpcExecutor failed: `maximumPoolSize could not be equal or less than 0`. Port: `" + listenPort
                    + "`. CompressionThreshold: `" + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold + "`. RpcExecutorListener: `"
                    + rpcExecutorListener + "`. MaximumPoolSize: `" + maximumPoolSize + "`.");
            throw new IllegalArgumentException("Create AsyncJdkRpcExecutor failed: `maximumPoolSize could not be equal or less than 0`. Port: `" + listenPort
                    + "`. CompressionThreshold: `" + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold + "`. RpcExecutorListener: `"
                    + rpcExecutorListener + "`. MaximumPoolSize: `" + maximumPoolSize + "`.");
        }
        this.transcoder = new SimpleTranscoder(compressionThreshold, RpcServerCompressionMonitorFactory.get());
        //将毫秒转换为纳秒
        this.slowExecutionThreshold = TimeUnit.NANOSECONDS.convert(slowExecutionThreshold, TimeUnit.MILLISECONDS);
        this.rpcExecutorListener = rpcExecutorListener;
        //创建 RPC 服务执行线程池
        this.maximumPoolSize = maximumPoolSize;
        this.executorService = new ThreadPoolExecutor(0, maximumPoolSize,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new NamedThreadFactory());
        this.threadPoolMonitor = RpcServerThreadPoolMonitorFactory.get(listenPort);
        this.threadPoolMonitor.register(executorService);
        this.executionMonitor = RpcServerExecutionMonitorFactory.get(listenPort);
    }

    @Override
    public void register(Object service) throws IllegalArgumentException {
        RpcServiceDepiction depiction = new RpcServiceDepiction(service);
        Class<?>[] interfaces = depiction.getInterfaces();
        for(Class<?> proxyInterface : interfaces) {
            String interfaceName = proxyInterface.getName();
            RpcServiceDepiction existedDepiction = depictionMap.get(interfaceName);
            if (existedDepiction != null && existedDepiction.getTarget() != service) {
                LOG.error("`{}` is existed. It will be overridden. Previous target: `{}`. New target: `{}`.",
                        proxyInterface, existedDepiction.getTarget(), service);
            }
            depictionMap.put(interfaceName, depiction);
            LOG.info("`{}` has been registered.", proxyInterface);
        }
    }

    @Override
    public void execute(RpcChannel channel, RpcPacket packet) {
        try  {
            executorService.submit(new RpcTask(channel, packet));
        } catch (RejectedExecutionException e) {
            threadPoolMonitor.onRejected();
            LOG.error("AsyncJdkRpcExecutor is too busy. MaximumPoolSize: " + maximumPoolSize + ".", e);
            channel.send(RpcPacketBuilder.buildResponsePacket(packet, ResponseStatusCode.TOO_BUSY));
        }
    }

    @Override
    public void close() {
        executorService.shutdown();
    }

    @Override
    public String toString() {
        return "AsyncJdkRpcExecutor{" +
                "maximumPoolSize=" + maximumPoolSize +
                ", executorService=" + executorService +
                ", transcoder=" + transcoder +
                ", slowExecutionThreshold=" + slowExecutionThreshold +
                ", rpcExecutorListener=" + rpcExecutorListener +
                ", depictionMap=" + depictionMap +
                '}';
    }

    /**
     * RPC 执行任务
     */
    private class RpcTask implements Runnable {

        private final RpcChannel channel;

        private final RpcPacket packet;

        private RpcTask(RpcChannel channel, RpcPacket packet) {
            this.channel = channel;
            this.packet = packet;
        }

        @Override
        public void run() {
            RpcRequestMessage rpcRequestMessage = null;
            try {
                rpcRequestMessage = transcoder.decode(packet.getBody(), packet.getSerializationType(), packet.getCompressionType());
            } catch (Exception e) {
                LOG.error("Decode RpcRequestMessage failed: `invalid packet`. Packet: `" + packet + "`.", e);
                channel.send(RpcPacketBuilder.buildResponsePacket(packet, ResponseStatusCode.INTERNAL_ERROR));
            }
            if (rpcRequestMessage != null) {
                long startTime = System.nanoTime();
                try {
                    RpcServiceDepiction depiction = depictionMap.get(rpcRequestMessage.getTargetClass());
                    if (depiction != null) {
                        try {
                            Object v = depiction.execute(rpcRequestMessage.getMethodUniqueName(), rpcRequestMessage.getArguments());
                            channel.send(RpcPacketBuilder.buildResponsePacket(packet, ResponseStatusCode.SUCCESS, v, transcoder));
                        } catch (NoSuchMethodException e) {
                            LOG.error("Execute rpc method failed: `no such method`. RpcRequestMessage: `" + rpcRequestMessage + "`.", e);
                            channel.send(RpcPacketBuilder.buildResponsePacket(packet, ResponseStatusCode.NO_SUCH_METHOD));
                            if (rpcExecutorListener != null) {
                                try {
                                    rpcExecutorListener.onNoSuchMethod(rpcRequestMessage);
                                } catch (Exception e1) {
                                    LOG.error("Call RpcExecutorListener#onNoSuchMethod() failed. RpcRequestMessage: `" + rpcRequestMessage + "`.", e1);
                                }
                            }
                            executionMonitor.onError(RpcServerExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR);
                        } catch (IllegalAccessException e) { //should not happen
                            LOG.error("Execute rpc method failed: `illegal access`. RpcRequestMessage: `" + rpcRequestMessage + "`.", e);
                            channel.send(RpcPacketBuilder.buildResponsePacket(packet, ResponseStatusCode.INTERNAL_ERROR));
                            executionMonitor.onError(RpcServerExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR);
                        } catch (IllegalArgumentException e) {
                            LOG.error("Execute rpc method failed: `illegal argument`. RpcRequestMessage: `" + rpcRequestMessage + "`.", e);
                            channel.send(RpcPacketBuilder.buildResponsePacket(packet, ResponseStatusCode.ILLEGAL_ARGUMENT));
                            if (rpcExecutorListener != null) {
                                try {
                                    rpcExecutorListener.onIllegalArgument(rpcRequestMessage);
                                } catch (Exception e1) {
                                    LOG.error("Call RpcExecutorListener#onIllegalArgument() failed. RpcRequestMessage: `" + rpcRequestMessage + "`.", e1);
                                }
                            }
                            executionMonitor.onError(RpcServerExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR);
                        } catch (InvocationTargetException e) {
                            LOG.error("Execute rpc method failed: `invocation target error`. RpcRequestMessage: `" + rpcRequestMessage + "`.", e);
                            String errorMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                            channel.send(RpcPacketBuilder.buildResponsePacket(packet, ResponseStatusCode.INVOCATION_TARGET_ERROR, errorMessage, transcoder));
                            if (rpcExecutorListener != null) {
                                try {
                                    rpcExecutorListener.onInvocationTargetError(rpcRequestMessage, e);
                                } catch (Exception e1) {
                                    LOG.error("Call RpcExecutorListener#onInvocationTargetError() failed. RpcRequestMessage: `" + rpcRequestMessage + "`.", e1);
                                }
                            }
                            executionMonitor.onError(RpcServerExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR);
                        }
                    } else {
                        LOG.error("Execute rpc method failed: `class not found`. RpcRequestMessage: `" + rpcRequestMessage + "`.");
                        channel.send(RpcPacketBuilder.buildResponsePacket(packet, ResponseStatusCode.CLASS_NOT_FOUND));
                        if (rpcExecutorListener != null) {
                            try {
                                rpcExecutorListener.onClassNotFound(rpcRequestMessage);
                            } catch (Exception e) {
                                LOG.error("Call RpcExecutorListener#onClassNotFound() failed. RpcRequestMessage: `" + rpcRequestMessage + "`.", e);
                            }
                        }
                        executionMonitor.onError(RpcServerExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR);
                    }
                } catch (Exception e) {
                    LOG.error("Execute rpc method failed: `" + e.getMessage() + "`. RpcRequestMessage: `" + rpcRequestMessage + "`.", e);
                    channel.send(RpcPacketBuilder.buildResponsePacket(packet, ResponseStatusCode.INTERNAL_ERROR));
                    executionMonitor.onError(RpcServerExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR);
                } finally {
                    if (rpcExecutorListener != null) {
                        long executedNanoTime = System.nanoTime() - startTime;
                        if (executedNanoTime > slowExecutionThreshold) {
                            try {
                                rpcExecutorListener.onSlowExecution(rpcRequestMessage, executedNanoTime);
                            } catch (Exception e) {
                                LOG.error("Call RpcExecutorListener#onSlowExecution() failed. RpcRequestMessage: `" + rpcRequestMessage
                                        + "`. Executed nano time: `" + executedNanoTime + "`.", e);
                            }
                        }
                    }
                    executionMonitor.onExecuted(startTime);
                }
            }
        }

    }
}
