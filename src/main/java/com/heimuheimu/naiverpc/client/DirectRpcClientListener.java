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

import java.lang.reflect.Method;

/**
 * {@link DirectRpcClient} 事件监听器，可监听 RPC 调用时出现的异常、超时、执行过慢、RPC 服务提供方繁忙、{@code DirectRpcClient} 已关闭等事件。
 *
 * <p>
 *     <strong>说明：</strong>监听器的实现类必须是线程安全的。应优先考虑继承 {@link DirectRpcClientListenerSkeleton} 骨架类进行实现，
 *     防止 {@code DirectRpcClientListener} 在后续版本增加监听事件时，带来的编译错误。
 * </p>
 *
 * @author heimuheimu
 */
public interface DirectRpcClientListener {

    /**
     * 当 RPC 调用出现类未找到异常时，将触发该监听事件。
     *
     * @param host 提供 RPC 服务的主机地址，由主机名和端口组成，":"符号分割，例如：localhost:4182
     * @param method RPC 调用的方法
     * @param args RPC 调用使用的参数数组，可能为 {@code null}
     */
    void onClassNotFound(String host, Method method, Object[] args);

    /**
     * 当 RPC 调用出现方法未找到异常时，将触发该监听事件。
     *
     * @param host 提供 RPC 服务的主机地址，由主机名和端口组成，":"符号分割，例如：localhost:4182
     * @param method RPC 调用的方法
     * @param args RPC 调用使用的参数数组，可能为 {@code null}
     */
    void onNoSuchMethod(String host, Method method, Object[] args);

    /**
     * 当 RPC 调用使用的参数不符合方法定义时，将触发该监听事件。
     *
     * @param host 提供 RPC 服务的主机地址，由主机名和端口组成，":"符号分割，例如：localhost:4182
     * @param method RPC 调用的方法
     * @param args RPC 调用使用的参数数组，可能为 {@code null}
     */
    void onIllegalArgument(String host, Method method, Object[] args);

    /**
     * 当 RPC 服务提供方在执行请求的方法过程中发生异常时，将触发该监听事件。
     *
     * @param host 提供 RPC 服务的主机地址，由主机名和端口组成，":"符号分割，例如：localhost:4182
     * @param method RPC 调用的方法
     * @param args RPC 调用使用的参数数组，可能为 {@code null}
     * @param errorMessage 异常信息
     */
    void onInvocationTargetError(String host, Method method, Object[] args, String errorMessage);

    /**
     * 当 RPC 调用超时后，将触发该监听事件。
     *
     * @param host 提供 RPC 服务的主机地址，由主机名和端口组成，":"符号分割，例如：localhost:4182
     * @param method RPC 调用的方法
     * @param args RPC 调用使用的参数数组，可能为 {@code null}
     */
    void onTimeout(String host, Method method, Object[] args);

    /**
     * 当 RPC 调用发生预期外异常时，将触发该监听事件。
     *
     * @param host 提供 RPC 服务的主机地址，由主机名和端口组成，":"符号分割，例如：localhost:4182
     * @param method RPC 调用的方法
     * @param args RPC 调用使用的参数数组，可能为 {@code null}
     */
    void onError(String host, Method method, Object[] args);

    /**
     * 当 RPC 服务提供方过于繁忙，无法执行该调用时，将触发该监听事件。
     *
     * @param host 提供 RPC 服务的主机地址，由主机名和端口组成，":"符号分割，例如：localhost:4182
     * @param method RPC 调用的方法
     * @param args RPC 调用使用的参数数组，可能为 {@code null}
     */
    void onTooBusy(String host, Method method, Object[] args);

    /**
     * 当 {@code DirectRpcClient} 已经关闭，无法执行该调用时，将触发该监听事件。
     *
     * @param host 提供 RPC 服务的主机地址，由主机名和端口组成，":"符号分割，例如：localhost:4182
     * @param method RPC 调用的方法
     * @param args RPC 调用使用的参数数组，可能为 {@code null}
     */
    void onClosed(String host, Method method, Object[] args);

    /**
     * 当 RPC 调用执行过慢时，将触发该监听事件。
     *
     * @param host 提供 RPC 服务的主机地址，由主机名和端口组成，":"符号分割，例如：localhost:4182
     * @param method RPC 调用的方法
     * @param args RPC 调用使用的参数数组，可能为 {@code null}
     * @param executedNanoTime RPC 调用执行时间，单位：纳秒
     */
    void onSlowExecution(String host, Method method, Object[] args, long executedNanoTime);
}
