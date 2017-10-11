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

import java.lang.reflect.InvocationTargetException;

/**
 * {@code RpcExecutor} 事件监听器，可监听 RPC 执行时出现的异常、执行过慢等事件。
 *
 * <p>
 *     <strong>说明：</strong>监听器的实现类必须是线程安全的。应优先考虑继承 {@link RpcExecutorListenerSkeleton} 骨架类进行实现，
 *     防止 {@code RpcExecutorListener} 在后续版本增加监听事件时，带来的编译错误。
 * </p>
 *
 * @author heimuheimu
 */
public interface RpcExecutorListener {

    /**
     * 当 RPC 执行遇到目标类未找到异常时，将触发该监听事件。
     *
     * @param rpcRequestMessage RPC 服务调用请求消息
     */
    void onClassNotFound(RpcRequestMessage rpcRequestMessage);

    /**
     * 当 RPC 执行遇到目标方法未找到异常时，将触发该监听事件。
     *
     * @param rpcRequestMessage RPC 服务调用请求消息
     */
    void onNoSuchMethod(RpcRequestMessage rpcRequestMessage);

    /**
     * 当 RPC 执行遇到非法参数异常时，将触发该监听事件。
     *
     * @param rpcRequestMessage RPC 服务调用请求消息
     */
    void onIllegalArgument(RpcRequestMessage rpcRequestMessage);

    /**
     * 当 RPC 执行的方法抛出异常时，将触发该监听事件。
     *
     * @param rpcRequestMessage RPC 服务调用请求消息
     * @param exception RPC 执行的方法抛出的异常
     */
    void onInvocationTargetError(RpcRequestMessage rpcRequestMessage, InvocationTargetException exception);

    /**
     * 当 RPC 执行过慢时，将触发该监听事件。
     *
     * @param rpcRequestMessage RPC 服务调用请求消息
     * @param executedNanoTime RPC 执行时间，单位：纳秒
     */
    void onSlowExecution(RpcRequestMessage rpcRequestMessage, long executedNanoTime);

}
