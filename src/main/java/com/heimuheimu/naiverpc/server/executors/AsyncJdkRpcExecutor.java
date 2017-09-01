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
import com.heimuheimu.naiverpc.server.RpcExecuteListener;
import com.heimuheimu.naiverpc.server.RpcExecutor;
import com.heimuheimu.naiverpc.transcoder.SimpleTranscoder;
import com.heimuheimu.naiverpc.transcoder.Transcoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.*;

/**
 * 基于 JDK 反射类库实现的异步远程调用执行器，使用缓存线程池实现
 * <p>当前实现是线程安全的</p>
 *
 * @author heimuheimu
 * @ThreadSafe
 */
public class AsyncJdkRpcExecutor implements RpcExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncJdkRpcExecutor.class);

    private final int maximumPoolSize;

    private final ThreadPoolExecutor executorService;

    private final Transcoder transcoder;

    private final RpcExecuteListener rpcExecuteListener;

    private final ThreadPoolMonitor threadPoolMonitor;

    private final ExecutionMonitor executionMonitor;

    private final ConcurrentHashMap<String, RpcServiceDepiction> depictionMap = new ConcurrentHashMap<>();

    public AsyncJdkRpcExecutor(int listenPort, int maximumPoolSize, int compressionThreshold, RpcExecuteListener rpcExecuteListener) {
        this.maximumPoolSize = maximumPoolSize;
        executorService = new ThreadPoolExecutor(0, maximumPoolSize,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new NamedThreadFactory());
        transcoder = new SimpleTranscoder(compressionThreshold, RpcServerCompressionMonitorFactory.get());
        this.rpcExecuteListener = rpcExecuteListener;
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
            LOG.error("AsyncJdkRpcExecutor is too busy. MaximumPoolSize: {}.",  maximumPoolSize);
            channel.send(RpcPacketBuilder.buildResponsePacket(packet, ResponseStatusCode.TOO_BUSY));
        }
    }

    @Override
    public void close() {
        executorService.shutdown();
    }

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
                LOG.error("Decode RpcRequestMessage failed. Invalid packet: `" + packet + "`.", e);
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
                            executionMonitor.onExecutedSuccess(startTime);
                        } catch (NoSuchMethodException e) {
                            LOG.error("No such method. `{}`.", rpcRequestMessage);
                            channel.send(RpcPacketBuilder.buildResponsePacket(packet, ResponseStatusCode.NO_SUCH_METHOD));
                            if (rpcExecuteListener != null) {
                                try {
                                    rpcExecuteListener.onNoSuchMethod(rpcRequestMessage);
                                } catch (Exception e1) {
                                    LOG.error("Call RpcExecuteListener#onNoSuchMethod() failed. RpcRequestMessage: `" + rpcRequestMessage + "`.");
                                }
                            }
                            executionMonitor.onExecutedError(startTime, RpcServerExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR);
                        } catch (IllegalAccessException e) { //should not happen
                            LOG.error("Illegal access. `{}`.", rpcRequestMessage);
                            channel.send(RpcPacketBuilder.buildResponsePacket(packet, ResponseStatusCode.INTERNAL_ERROR));
                            executionMonitor.onExecutedError(startTime, RpcServerExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR);
                        } catch (IllegalArgumentException e) {
                            LOG.error("Illegal argument. `{}`.", rpcRequestMessage);
                            channel.send(RpcPacketBuilder.buildResponsePacket(packet, ResponseStatusCode.ILLEGAL_ARGUMENT));
                            if (rpcExecuteListener != null) {
                                try {
                                    rpcExecuteListener.onIllegalArgument(rpcRequestMessage);
                                } catch (Exception e1) {
                                    LOG.error("Call RpcExecuteListener#onIllegalArgument() failed. RpcRequestMessage: `" + rpcRequestMessage + "`.");
                                }
                            }
                            executionMonitor.onExecutedError(startTime, RpcServerExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR);
                        } catch (InvocationTargetException e) {
                            LOG.error("Invocation target error. `" + rpcRequestMessage + "`.", e);
                            channel.send(RpcPacketBuilder.buildResponsePacket(packet, ResponseStatusCode.INVOCATION_TARGET_ERROR, e.getCause().getMessage(), transcoder));
                            if (rpcExecuteListener != null) {
                                try {
                                    rpcExecuteListener.onInvocationTargetError(rpcRequestMessage, e);
                                } catch (Exception e1) {
                                    LOG.error("Call RpcExecuteListener#onInvocationTargetError() failed. RpcRequestMessage: `" + rpcRequestMessage + "`.");
                                }
                            }
                            executionMonitor.onExecutedError(startTime, RpcServerExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR);
                        }
                    } else {
                        LOG.error("Class not found. `{}`", rpcRequestMessage);
                        channel.send(RpcPacketBuilder.buildResponsePacket(packet, ResponseStatusCode.CLASS_NOT_FOUND));
                        if (rpcExecuteListener != null) {
                            try {
                                rpcExecuteListener.onClassNotFound(rpcRequestMessage);
                            } catch (Exception e) {
                                LOG.error("Call RpcExecuteListener#onClassNotFound() failed. RpcRequestMessage: `" + rpcRequestMessage + "`.");
                            }
                        }
                        executionMonitor.onExecutedError(startTime, RpcServerExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR);
                    }
                } catch (Exception e) {
                    LOG.error("Unexpected error. `" + rpcRequestMessage + "`.", e);
                    channel.send(RpcPacketBuilder.buildResponsePacket(packet, ResponseStatusCode.INTERNAL_ERROR));
                    executionMonitor.onExecutedError(startTime, RpcServerExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR);
                } finally {
                    if (rpcExecuteListener != null) {
                        long executedNanoTime = System.nanoTime() - startTime;
                        if (executedNanoTime > RpcExecuteListener.SLOW_EXECUTION_THRESHOLD) {
                            try {
                                rpcExecuteListener.onSlowExecution(rpcRequestMessage, executedNanoTime);
                            } catch (Exception e) {
                                LOG.error("Call RpcExecuteListener#onSlowExecution() failed. RpcRequestMessage: `" + rpcRequestMessage
                                        + "`. Executed nano time: `" + executedNanoTime + "`.");
                            }
                        }
                    }
                }
            }
        }

    }
}
