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
import java.util.concurrent.TimeUnit;

/**
 * RPC 服务调用客户端监听器
 *
 * @author heimuheimu
 */
public interface RpcClientListener {

    /**
     * 大于该执行时间的 RPC 服务调用将会被定义为慢查，单位：纳秒
     */
    long SLOW_EXECUTION_THRESHOLD = TimeUnit.NANOSECONDS.convert(50, TimeUnit.MILLISECONDS);

    /**
     * 当 RPC 调用出现类未找到异常时，将触发该监听事件
     *
     * @param client RPC 服务调用客户端
     * @param method RPC 服务调用的方法
     * @param args RPC 服务调用使用的参数数组
     */
    void onClassNotFound(RpcClient client, Method method, Object[] args);

    /**
     * 当 RPC 调用出现方法未找到异常时，将触发该监听事件
     *
     * @param client RPC 服务调用客户端
     * @param method RPC 服务调用的方法
     * @param args RPC 服务调用使用的参数数组
     */
    void onNoSuchMethod(RpcClient client, Method method, Object[] args);

    /**
     * 当 RPC 调用出现非法参数异常时，将触发该监听事件
     *
     * @param client RPC 服务调用客户端
     * @param method RPC 服务调用的方法
     * @param args RPC 服务调用使用的参数数组
     */
    void onIllegalArgument(RpcClient client, Method method, Object[] args);

    /**
     * 当 RPC 服务提供端执行过程中发生异常时，将触发该监听事件
     *
     * @param client RPC 服务调用客户端
     * @param method RPC 服务调用的方法
     * @param args RPC 服务调用使用的参数数组
     * @param errorMessage 异常信息
     */
    void onInvocationTargetError(RpcClient client, Method method, Object[] args, String errorMessage);

    /**
     * 当 RPC 调用发生超时异常时，将触发该监听事件
     *
     * @param client RPC 服务调用客户端
     * @param method RPC 服务调用的方法
     * @param args RPC 服务调用使用的参数数组
     */
    void onTimeout(RpcClient client, Method method, Object[] args);

    /**
     * 当 RPC 调用发生预期外异常时，将触发该监听事件
     *
     * @param client RPC 服务调用客户端
     * @param method RPC 服务调用的方法
     * @param args RPC 服务调用使用的参数数组
     */
    void onError(RpcClient client, Method method, Object[] args);

    /**
     * 当 RPC 服务提供端过于繁忙，无法执行该调用时，将触发该监听事件
     *
     * @param client RPC 服务调用客户端
     * @param method RPC 服务调用的方法
     * @param args RPC 服务调用使用的参数数组
     */
    void onTooBusy(RpcClient client, Method method, Object[] args);

    /**
     * 当 RPC 服务调用客户端已经关闭，无法执行指定的 RPC 调用，将触发该监听事件
     *
     * @param client RPC 服务调用客户端
     * @param method RPC 服务调用的方法
     * @param args RPC 服务调用使用的参数数组
     */
    void onClosed(RpcClient client, Method method, Object[] args);

    /**
     * 当 RPC 调用执行时间大于 {@link #SLOW_EXECUTION_THRESHOLD} 时，将触发该监听事件
     *
     * @param client RPC 服务调用客户端
     * @param method RPC 服务调用的方法
     * @param args RPC 服务调用使用的参数数组
     * @param executedNanoTime RPC 调用执行时间，单位：纳秒
     */
    void onSlowExecution(RpcClient client, Method method, Object[] args, long executedNanoTime);

}
