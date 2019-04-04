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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 提供 {@link com.heimuheimu.naiverpc.client.DirectRpcClientListener} 监听器的一个简单实现，将 RPC 调用错误和 RPC 调用过慢打印至不同的日志文件中。
 *
 * <h3>RPC 调用错误 Log4j 配置</h3>
 * <strong>注意：</strong> <code>${log.output.directory}</code> 为占位替换符，请自行定义。
 * <blockquote>
 * <pre>
 * log4j.logger.NAIVERPC_CLIENT_ERROR_EXECUTION_LOG=INFO, NAIVERPC_CLIENT_ERROR_EXECUTION_LOG
 * log4j.additivity.NAIVERPC_CLIENT_ERROR_EXECUTION_LOG=false
 * log4j.appender.NAIVERPC_CLIENT_ERROR_EXECUTION_LOG=org.apache.log4j.DailyRollingFileAppender
 * log4j.appender.NAIVERPC_CLIENT_ERROR_EXECUTION_LOG.file=${log.output.directory}/naiverpc/client_error.log
 * log4j.appender.NAIVERPC_CLIENT_ERROR_EXECUTION_LOG.encoding=UTF-8
 * log4j.appender.NAIVERPC_CLIENT_ERROR_EXECUTION_LOG.DatePattern=_yyyy-MM-dd
 * log4j.appender.NAIVERPC_CLIENT_ERROR_EXECUTION_LOG.layout=org.apache.log4j.PatternLayout
 * log4j.appender.NAIVERPC_CLIENT_ERROR_EXECUTION_LOG.layout.ConversionPattern=%d{ISO8601} %-5p : %m%n
 * </pre>
 * </blockquote>
 *
 * <h3>RPC 调用过慢 Log4j 配置</h3>
 * <strong>注意：</strong> <code>${log.output.directory}</code> 为占位替换符，请自行定义。
 * <blockquote>
 * <pre>
 * log4j.logger.NAIVERPC_CLIENT_SLOW_EXECUTION_LOG=INFO, NAIVERPC_CLIENT_SLOW_EXECUTION_LOG
 * log4j.additivity.NAIVERPC_CLIENT_SLOW_EXECUTION_LOG=false
 * log4j.appender.NAIVERPC_CLIENT_SLOW_EXECUTION_LOG=org.apache.log4j.DailyRollingFileAppender
 * log4j.appender.NAIVERPC_CLIENT_SLOW_EXECUTION_LOG.file=${log.output.directory}/naiverpc/client_slow_execution.log
 * log4j.appender.NAIVERPC_CLIENT_SLOW_EXECUTION_LOG.encoding=UTF-8
 * log4j.appender.NAIVERPC_CLIENT_SLOW_EXECUTION_LOG.DatePattern=_yyyy-MM-dd
 * log4j.appender.NAIVERPC_CLIENT_SLOW_EXECUTION_LOG.layout=org.apache.log4j.PatternLayout
 * log4j.appender.NAIVERPC_CLIENT_SLOW_EXECUTION_LOG.layout.ConversionPattern=%d{ISO8601} : %m%n
 * </pre>
 * </blockquote>
 *
 * <p><strong>说明：</strong>{@code SimpleDirectRpcClientListener} 类是线程安全的，可在多个线程中使用同一个实例。</p>
 *
 * @author heimuheimu
 */
public class SimpleDirectRpcClientListener extends DirectRpcClientListenerSkeleton {

    private static final Logger RPC_CLIENT_ERROR_EXECUTION_LOG = LoggerFactory.getLogger("NAIVERPC_CLIENT_ERROR_EXECUTION_LOG");

    private static final Logger RPC_CLIENT_SLOW_EXECUTION_LOG = LoggerFactory.getLogger("NAIVERPC_CLIENT_SLOW_EXECUTION_LOG");

    /**
     * 调用的 RPC 服务名称，方便在同一项目调用多个 RPC 服务时进行区分
     */
    private final String serviceName;

    /**
     * 构造一个提供日志输出的 RPC 服务调用客户端事件监听器。
     *
     * @param serviceName 调用的 RPC 服务名称，方便在同一项目调用多个 RPC 服务时进行区分
     */
    public SimpleDirectRpcClientListener(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public void onClassNotFound(String host, Method method, Object[] args) {
        RPC_CLIENT_ERROR_EXECUTION_LOG.error("`Service`:`{}`, `Host`:`{}`, `Method`:`{}`, `Arguments`:`{}`, `Error`:`Class not found`",
                serviceName, host, method, args); // lgtm [java/print-array]
    }

    @Override
    public void onNoSuchMethod(String host, Method method, Object[] args) {
        RPC_CLIENT_ERROR_EXECUTION_LOG.error("`Service`:`{}`, `Host`:`{}`, `Method`:`{}`, `Arguments`:`{}`, `Error`:`No such method`",
                serviceName, host, method, args); // lgtm [java/print-array]
    }

    @Override
    public void onIllegalArgument(String host, Method method, Object[] args) {
        RPC_CLIENT_ERROR_EXECUTION_LOG.error("`Service`:`{}`, `Host`:`{}`, `Method`:`{}`, `Arguments`:`{}`, `Error`:`Illegal argument`",
                serviceName, host, method, args); // lgtm [java/print-array]
    }

    @Override
    public void onInvocationTargetError(String host, Method method, Object[] args, String errorMessage) {
        RPC_CLIENT_ERROR_EXECUTION_LOG.error("`Service`:`{}`, `Host`:`{}`, `Method`:`{}`, `Arguments`:`{}`, `Error`:`[Invocation target error] {}`",
                serviceName, host, method, args, errorMessage); // lgtm [java/print-array]
    }

    @Override
    public void onTimeout(String host, Method method, Object[] args) {
        RPC_CLIENT_ERROR_EXECUTION_LOG.error("`Service`:`{}`, `Host`:`{}`, `Method`:`{}`, `Arguments`:`{}`, `Error`:`Timeout`",
                serviceName, host, method, args); // lgtm [java/print-array]
    }

    @Override
    public void onError(String host, Method method, Object[] args) {
        RPC_CLIENT_ERROR_EXECUTION_LOG.error("`Service`:`{}`, `Host`:`{}`, `Method`:`{}`, `Arguments`:`{}`, `Error`:`Unexpected error`",
                serviceName, host, method, args); // lgtm [java/print-array]
    }

    @Override
    public void onTooBusy(String host, Method method, Object[] args) {
        RPC_CLIENT_ERROR_EXECUTION_LOG.error("`Service`:`{}`, `Host`:`{}`, `Method`:`{}`, `Arguments`:`{}`, `Error`:`Too busy`",
                serviceName, host, method, args); // lgtm [java/print-array]
    }

    @Override
    public void onClosed(String host, Method method, Object[] args) {
        RPC_CLIENT_ERROR_EXECUTION_LOG.error("`Service`:`{}`, `Host`:`{}`, `Method`:`{}`, `Arguments`:`{}`, `Error`:`Closed client`",
                serviceName, host, method, args); // lgtm [java/print-array]
    }

    @Override
    public void onSlowExecution(String host, Method method, Object[] args, long executedNanoTime) {
        RPC_CLIENT_SLOW_EXECUTION_LOG.error("`Cost`:`{}ns ({}ms)`, `Service`:`{}`, `Host`:`{}`, `Method`:`{}`, `Arguments`:`{}`",
                executedNanoTime, TimeUnit.MILLISECONDS.convert(executedNanoTime, TimeUnit.NANOSECONDS), serviceName,
                host, method, args); // lgtm [java/print-array]
    }

    @Override
    public String toString() {
        return "SimpleDirectRpcClientListener{" +
                "serviceName='" + serviceName + '\'' +
                '}';
    }
}
