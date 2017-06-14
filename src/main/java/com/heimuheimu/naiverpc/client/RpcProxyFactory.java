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

import com.heimuheimu.naiverpc.client.RpcClient;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * RPC 远程调用代理工厂，可以为指定接口生成一个 RPC 远程调用代理实例
 *
 * @author heimuheimu
 */
public class RpcProxyFactory {

    /**
     * 根据指定接口生成一个 RPC 远程调用代理实例后返回
     *
     * @param clz 需要生成代理的接口 Class
     * @param rpcClient RPC 服务调用客户端，该代理通过此客户端进行 RPC 服务调用
     * @return 指定接口对应的 RPC 远程调用代理实例u
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
