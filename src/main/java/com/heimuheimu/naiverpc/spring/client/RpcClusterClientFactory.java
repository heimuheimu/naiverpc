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

package com.heimuheimu.naiverpc.spring.client;

import com.heimuheimu.naiverpc.client.DirectRpcClientListener;
import com.heimuheimu.naiverpc.client.cluster.RpcClusterClient;
import com.heimuheimu.naiverpc.facility.clients.DirectRpcClientList;
import com.heimuheimu.naiverpc.facility.clients.DirectRpcClientListListener;
import com.heimuheimu.naiverpc.net.SocketConfiguration;
import org.springframework.beans.factory.FactoryBean;

import java.net.Socket;

/**
 * {@link RpcClusterClient} Spring 工厂类，兼容 Spring 4.0 以下版本不支持 lambda 语法问题。
 *
 * @author heimuheimu
 */
public class RpcClusterClientFactory implements FactoryBean<RpcClusterClient> {

    private final RpcClusterClient clusterClient;

    /**
     * 构造一个 {@link RpcClusterClient} Spring 工厂类，用于创建 {@link RpcClusterClient} 实例。
     *
     * @param hosts 提供 RPC 服务的主机地址数组，由主机名和端口组成，":"符号分割，例如：localhost:4182，不允许为 {@code null} 或空数组
     * @param directRpcClientListener 创建 {@code DirectRpcClient} 使用的 {@code DirectRpcClient} 事件监听器，允许为 {@code null}
     * @param directRpcClientListListener {@link DirectRpcClientList} 事件监听器，允许为 {@code null}
     * @throws IllegalStateException 如果所有提供 RPC 服务的主机地址都不可用，将会抛出此异常
     * @see RpcClusterClient#RpcClusterClient(String[], DirectRpcClientListener, DirectRpcClientListListener)
     */
    public RpcClusterClientFactory(String[] hosts, DirectRpcClientListener directRpcClientListener,
                                   DirectRpcClientListListener directRpcClientListListener) throws IllegalStateException {
        this.clusterClient = new RpcClusterClient(hosts, directRpcClientListener, directRpcClientListListener);
    }

    /**
     * 构造一个 {@link RpcClusterClient} Spring 工厂类，用于创建 {@link RpcClusterClient} 实例。
     *
     * @param hosts 提供 RPC 服务的主机地址数组，由主机名和端口组成，":"符号分割，例如：localhost:4182，不允许为 {@code null} 或空数组
     * @param configuration 创建 {@code DirectRpcClient} 使用的 {@link Socket} 配置信息，允许为 {@code null}
     * @param timeout 创建 {@code DirectRpcClient} 使用的 RPC 调用超时时间，单位：毫秒，不能小于等于 0
     * @param compressionThreshold 创建 {@code DirectRpcClient} 使用的最小压缩字节数，不能小于等于 0
     * @param slowExecutionThreshold 创建 {@code DirectRpcClient} 使用的 RPC 调用过慢最小时间，单位：毫秒，不能小于等于 0
     * @param heartbeatPeriod 创建 {@code DirectRpcClient} 使用的心跳检测时间，单位：秒，如果该值小于等于 0，则不进行检测
     * @param directRpcClientListener 创建 {@code DirectRpcClient} 使用的 {@code DirectRpcClient} 事件监听器，允许为 {@code null}
     * @param directRpcClientListListener {@link DirectRpcClientList} 事件监听器，允许为 {@code null}
     * @throws IllegalStateException  如果所有提供 RPC 服务的主机地址都不可用，将会抛出此异常
     * @see RpcClusterClient#RpcClusterClient(String[], SocketConfiguration, int, int, int, int, DirectRpcClientListener, DirectRpcClientListListener)
     */
    public RpcClusterClientFactory(String[] hosts, SocketConfiguration configuration, int timeout, int compressionThreshold,
                                   int slowExecutionThreshold, int heartbeatPeriod, DirectRpcClientListener directRpcClientListener,
                                   DirectRpcClientListListener directRpcClientListListener) throws IllegalStateException {
        this.clusterClient = new RpcClusterClient(hosts, configuration, timeout, compressionThreshold, slowExecutionThreshold,
                heartbeatPeriod, directRpcClientListener, directRpcClientListListener);
    }

    @Override
    public RpcClusterClient getObject() throws Exception {
        return clusterClient;
    }

    @Override
    public Class<?> getObjectType() {
        return RpcClusterClient.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public void close() {
        this.clusterClient.close();
    }
}
