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

import com.heimuheimu.naiverpc.exception.RpcException;
import com.heimuheimu.naiverpc.exception.TimeoutException;
import com.heimuheimu.naiverpc.exception.TooBusyException;

import java.io.Closeable;
import java.lang.reflect.Method;

/**
 * RPC 服务调用方使用的客户端，通过 {@link #execute(Method, Object[])} 方法远程调用 RPC 服务提供方提供的服务。
 *
 * <p>
 *     <strong>说明：</strong> {@code RpcClient} 的实现类必须是线程安全的。
 * </p>
 *
 * @author heimuheimu
 */
public interface RpcClient extends Closeable {

    /**
     * 向 RPC 服务提供方发起调用请求，并返回执行结果，超时时间使用 {@code RpcClient} 实现类默认的超时时间设置。
     *
     * @param method RPC 调用的方法
     * @param args RPC 调用使用的参数数组，如果没有参数则使用 {@code null} 或空数组
     * @return 执行结果
     * @throws IllegalStateException 如果 {@code RpcClient} 处于不可服务状态，将抛出此异常
     * @throws TimeoutException 如果 RPC 调用超时，将抛出此异常
     * @throws TooBusyException 如果当 RPC 服务提供方过于繁忙，无法执行该调用请求，将抛出此异常
     * @throws RpcException 如果 RPC 调用过程中遇到错误，将抛出此异常
     */
    Object execute(Method method, Object[] args) throws IllegalStateException, TimeoutException, TooBusyException, RpcException;

    /**
     * 向 RPC 服务提供方发起调用请求，并返回执行结果。
     *
     * @param method RPC 调用的方法
     * @param args RPC 调用使用的参数数组，如果没有参数则使用 {@code null} 或空数组
     * @param timeout RPC 调用超时时间，单位为毫秒，不允许小于等于 0
     * @return 执行结果
     * @throws IllegalStateException 如果 {@code RpcClient} 处于不可服务状态，将抛出此异常
     * @throws TimeoutException 如果 RPC 调用超时，将抛出此异常
     * @throws TooBusyException 如果当 RPC 服务提供方过于繁忙，无法执行该调用请求，将抛出此异常
     * @throws RpcException 如果 RPC 调用过程中遇到错误，将抛出此异常
     */
    Object execute(Method method, Object[] args, long timeout) throws IllegalStateException, TimeoutException, TooBusyException, RpcException;
}
