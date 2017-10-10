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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * RPC 服务提供方通常以接口的形式提供其所支持的服务，{@code RpcProxyFactory} 可为这类接口生成一个代理实现，
 * 接口的方法执行均通过 {@link RpcClient#execute(Method, Object[])} 来发起 RPC 调用，并返回结果。
 *
 * <p><strong>说明：</strong>{@code RpcProxyFactory} 类是线程安全的，可在多个线程中使用同一个实例。</p>
 *
 * @author heimuheimu
 */
public class RpcProxyFactory {

    /**
     * 根据接口生成一个 RPC 调用代理，接口的方法执行均通过 {@link RpcClient#execute(Method, Object[])} 来发起 RPC 调用，并返回结果。
     *
     * @param clz 需要生成 RPC 调用代理的接口 Class
     * @param rpcClient RPC 服务调用方使用的客户端
     * @return RPC 调用代理
     *
     */
    @SuppressWarnings("unchecked")
    public static <T> T build(Class<T> clz, RpcClient rpcClient) {
        return (T) Proxy.newProxyInstance(clz.getClassLoader(), new Class<?>[]{clz}, new RpcInvocationHandler(rpcClient));
    }

    private static class RpcInvocationHandler implements InvocationHandler {

        private final RpcClient rpcClient;

        private RpcInvocationHandler(RpcClient rpcClient) {
            this.rpcClient = rpcClient;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return rpcClient.execute(method, args);
        }

    }

}
