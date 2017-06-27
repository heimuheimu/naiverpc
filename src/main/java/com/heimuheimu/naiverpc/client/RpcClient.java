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
 * RPC 服务调用客户端
 * <p>实现类需保证实现是线程安全的</p>
 *
 * @author heimuheimu
 * @ThreadSafe
 */
public interface RpcClient extends Closeable {

    /**
     * 执行调用远程服务操作，并返回执行结果，超时时间使用客户端默认的超时时间设置。
     *
     * @param method 需要执行的远程服务
     * @param args 执行该远程服务所需的参数
     * @return 执行结果
     * @throws IllegalStateException 如果客户端处于不可服务状态，将抛出此异常
     * @throws TimeoutException 如果执行超时，将抛出此异常
     * @throws TooBusyException 如果远程服务繁忙，无法执行当前调用请求，将抛出此异常
     * @throws RpcException 执行过程中遇到错误，将抛出此异常
     */
    Object execute(Method method, Object[] args) throws IllegalStateException, TimeoutException, TooBusyException, RpcException;

    /**
     * 执行调用远程服务操作，并返回执行结果
     *
     * @param method 需要执行的远程服务
     * @param args 执行该远程服务所需的参数
     * @param timeout 超时时间，单位为毫秒，如果小于等于 0，则使用客户端默认的超时时间设置
     * @return 执行结果
     * @throws IllegalStateException 如果客户端处于不可服务状态，将抛出此异常
     * @throws TimeoutException 如果执行超时，将抛出此异常
     * @throws TooBusyException 如果远程服务繁忙，无法执行当前调用请求，将抛出此异常
     * @throws RpcException 执行过程中遇到错误，将抛出此异常
     */
    Object execute(Method method, Object[] args, long timeout) throws IllegalStateException, TimeoutException, TooBusyException, RpcException;

    /**
     * 判断当前客户端是否处于可用状态
     *
     * @return 当前客户端是否处于可用状态
     */
    boolean isActive();

    /**
     * 获得当前客户端所连的 RPC 服务提供者主机地址
     * <p>注意：客户端可能支持多 RPC 服务提供者主机地址，输出格式由实现类自行定义</p>
     *
     * @return 当前客户端所连的 RPC 服务提供者主机地址
     */
    String getHost();

}
