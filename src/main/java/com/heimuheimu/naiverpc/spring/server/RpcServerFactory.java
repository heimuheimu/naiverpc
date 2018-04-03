/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 heimuheimu
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

package com.heimuheimu.naiverpc.spring.server;

import com.heimuheimu.naiverpc.net.SocketConfiguration;
import com.heimuheimu.naiverpc.server.RpcExecutor;
import com.heimuheimu.naiverpc.server.RpcExecutorListener;
import com.heimuheimu.naiverpc.server.RpcServer;
import org.springframework.beans.factory.FactoryBean;

import java.net.Socket;

/**
 * {@link RpcServer} Spring 工厂类，兼容 Spring 4.0 以下版本不支持 lambda 语法问题。
 *
 * @author heimuheimu
 */
public class RpcServerFactory implements FactoryBean<RpcServer> {

    private final RpcServer rpcServer;

    /**
     * 构造一个 {@link RpcServer} Spring 工厂类，用于创建 {@link RpcServer} 实例。
     *
     * @param rpcExecutorListener {@link RpcExecutor} 事件监听器，允许为 {@code null}
     * @see RpcServer#RpcServer(RpcExecutorListener)
     */
    public RpcServerFactory(RpcExecutorListener rpcExecutorListener) {
        this.rpcServer = new RpcServer(rpcExecutorListener);
    }

    /**
     * 构造一个 {@link RpcServer} Spring 工厂类，用于创建 {@link RpcServer} 实例。
     *
     * @param port {@code RpcServer} 开启的 {@code Socket} 监听端口，不能小于等于 0
     * @param rpcExecutorListener 创建 {@code AsyncJdkRpcExecutor} 使用的 {@link RpcExecutor} 事件监听器，允许为 {@code null}
     * @throws IllegalArgumentException 如果 {@code RpcServer} 开启的 {@code Socket} 监听端口小于等于 0，将会抛出此异常
     */
    public RpcServerFactory(int port, RpcExecutorListener rpcExecutorListener) throws IllegalArgumentException {
        this.rpcServer = new RpcServer(port, rpcExecutorListener);
    }

    /**
     * 构造一个 {@link RpcServer} Spring 工厂类，用于创建 {@link RpcServer} 实例。
     *
     * @param port {@code RpcServer} 开启的 {@code Socket} 监听端口，不能小于等于 0
     * @param socketConfiguration 创建 {@code RpcChannel} 使用的 {@link Socket} 配置信息，允许为 {@code null}
     * @param compressionThreshold 创建 {@code AsyncJdkRpcExecutor} 使用的最小压缩字节数，不能小于等于 0
     * @param slowExecutionThreshold 创建 {@code AsyncJdkRpcExecutor} 使用的 RPC 执行过慢最小时间，单位：毫秒，不能小于等于 0
     * @param rpcExecutorListener 创建 {@code AsyncJdkRpcExecutor} 使用的 {@link RpcExecutor} 事件监听器，允许为 {@code null}
     * @param maximumPoolSize 创建 {@code AsyncJdkRpcExecutor} 使用的线程池最大数量，不能小于等于 0
     * @throws IllegalArgumentException 如果 {@code RpcServer} 开启的 {@code Socket} 监听端口小于等于 0，将会抛出此异常
     * @throws IllegalArgumentException 如果创建 {@code AsyncJdkRpcExecutor} 使用的最小压缩字节数小于等于 0，将会抛出此异常
     * @throws IllegalArgumentException 如果创建 {@code AsyncJdkRpcExecutor} 使用的 RPC 执行过慢最小时间小于等于 0，将会抛出此异常
     * @throws IllegalArgumentException 如果创建 {@code AsyncJdkRpcExecutor} 使用的线程池最大数量小于等于 0，将会抛出此异常
     */
    public RpcServerFactory(int port, SocketConfiguration socketConfiguration, int compressionThreshold, int slowExecutionThreshold,
                            RpcExecutorListener rpcExecutorListener, int maximumPoolSize) throws IllegalArgumentException {
        this.rpcServer = new RpcServer(port, socketConfiguration, compressionThreshold, slowExecutionThreshold,
                rpcExecutorListener, maximumPoolSize);
    }

    @Override
    public RpcServer getObject() throws Exception {
        return rpcServer;
    }

    @Override
    public Class<?> getObjectType() {
        return RpcServer.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public void init() {
        this.rpcServer.init();
    }

    public void close() {
        this.rpcServer.close();
    }
}
