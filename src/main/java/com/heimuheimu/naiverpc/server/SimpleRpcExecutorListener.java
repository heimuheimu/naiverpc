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

package com.heimuheimu.naiverpc.server;

import com.heimuheimu.naiverpc.message.RpcRequestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

/**
 * 提供 {@link com.heimuheimu.naiverpc.server.RpcExecutorListener} 监听器的一个简单实现，将 RPC 执行错误和 RPC 执行过慢打印至不同的日志文件中。
 *
 * <h3>RPC 执行错误 Log4j 配置</h3>
 * <strong>注意：</strong> <code>${log.output.directory}</code> 为占位替换符，请自行定义。
 * <blockquote>
 * <pre>
 * log4j.logger.NAIVERPC_SERVER_ERROR_EXECUTION_LOG=INFO, NAIVERPC_SERVER_ERROR_EXECUTION_LOG
 * log4j.additivity.NAIVERPC_SERVER_ERROR_EXECUTION_LOG=false
 * log4j.appender.NAIVERPC_SERVER_ERROR_EXECUTION_LOG=org.apache.log4j.DailyRollingFileAppender
 * log4j.appender.NAIVERPC_SERVER_ERROR_EXECUTION_LOG.file=${log.output.directory}/naiverpc/server_error.log
 * log4j.appender.NAIVERPC_SERVER_ERROR_EXECUTION_LOG.encoding=UTF-8
 * log4j.appender.NAIVERPC_SERVER_ERROR_EXECUTION_LOG.DatePattern=_yyyy-MM-dd
 * log4j.appender.NAIVERPC_SERVER_ERROR_EXECUTION_LOG.layout=org.apache.log4j.PatternLayout
 * log4j.appender.NAIVERPC_SERVER_ERROR_EXECUTION_LOG.layout.ConversionPattern=%d{ISO8601} %-5p : %m%n
 * </pre>
 * </blockquote>
 *
 * <h3>RPC 执行过慢 Log4j 配置</h3>
 * <strong>注意：</strong> <code>${log.output.directory}</code> 为占位替换符，请自行定义。
 * <blockquote>
 * <pre>
 * log4j.logger.NAIVERPC_SERVER_SLOW_EXECUTION_LOG=INFO, NAIVERPC_SERVER_SLOW_EXECUTION_LOG
 * log4j.additivity.NAIVERPC_SERVER_SLOW_EXECUTION_LOG=false
 * log4j.appender.NAIVERPC_SERVER_SLOW_EXECUTION_LOG=org.apache.log4j.DailyRollingFileAppender
 * log4j.appender.NAIVERPC_SERVER_SLOW_EXECUTION_LOG.file=${log.output.directory}/naiverpc/server_slow_execution.log
 * log4j.appender.NAIVERPC_SERVER_SLOW_EXECUTION_LOG.encoding=UTF-8
 * log4j.appender.NAIVERPC_SERVER_SLOW_EXECUTION_LOG.DatePattern=_yyyy-MM-dd
 * log4j.appender.NAIVERPC_SERVER_SLOW_EXECUTION_LOG.layout=org.apache.log4j.PatternLayout
 * log4j.appender.NAIVERPC_SERVER_SLOW_EXECUTION_LOG.layout.ConversionPattern=%d{ISO8601} : %m%n
 * </pre>
 * </blockquote>
 *
 * <p><strong>说明：</strong>{@code SimpleRpcExecutorListener} 类是线程安全的，可在多个线程中使用同一个实例。</p>
 *
 * @author heimuheimu
 */
public class SimpleRpcExecutorListener extends RpcExecutorListenerSkeleton {

    private static final Logger RPC_SERVER_ERROR_EXECUTION_LOG = LoggerFactory.getLogger("NAIVERPC_SERVER_ERROR_EXECUTION_LOG");

    private static final Logger RPC_SERVER_SLOW_EXECUTION_LOG = LoggerFactory.getLogger("NAIVERPC_SERVER_SLOW_EXECUTION_LOG");

    @Override
    public void onClassNotFound(RpcRequestMessage rpcRequestMessage) {
        RPC_SERVER_ERROR_EXECUTION_LOG.error("`Error`:`Class not found`, `RpcRequestMessage`:`{}`", rpcRequestMessage);
    }

    @Override
    public void onNoSuchMethod(RpcRequestMessage rpcRequestMessage) {
        RPC_SERVER_ERROR_EXECUTION_LOG.error("`Error`:`No such method`, `RpcRequestMessage`:`{}`", rpcRequestMessage);
    }

    @Override
    public void onIllegalArgument(RpcRequestMessage rpcRequestMessage) {
        RPC_SERVER_ERROR_EXECUTION_LOG.error("`Error`:`Illegal argument`, `RpcRequestMessage`:`{}`", rpcRequestMessage);
    }

    @Override
    public void onInvocationTargetError(RpcRequestMessage rpcRequestMessage, InvocationTargetException exception) {
        RPC_SERVER_ERROR_EXECUTION_LOG.error("`Error`:`Invocation target error`, `RpcRequestMessage`:`{}`", rpcRequestMessage);
    }

    @Override
    public void onSlowExecution(RpcRequestMessage rpcRequestMessage, long executedNanoTime) {
        RPC_SERVER_SLOW_EXECUTION_LOG.error("`Cost`:`{}ns ({}ms)`, `RpcRequestMessage`:`{}`", executedNanoTime,
                TimeUnit.MILLISECONDS.convert(executedNanoTime, TimeUnit.NANOSECONDS), rpcRequestMessage);
    }

    @Override
    public String toString() {
        return "SimpleRpcExecutorListener{}";
    }
}
