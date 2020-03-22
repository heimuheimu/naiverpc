/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 heimuheimu
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

package com.heimuheimu.naiverpc.monitor.server.prometheus;

/**
 * RPC 服务端信息 Prometheus 采集器配置。
 *
 * @author heimuheimu
 * @since 1.2
 */
public class RpcServerPrometheusCollectorConfiguration {

    /**
     * RPC 服务端监听端口
     */
    private final int port;

    /**
     * RPC 服务名称
     */
    private final String name;

    /**
     * 构造一个 RpcServerPrometheusCollectorConfiguration 实例。
     *
     * @param port RPC 服务端监听端口
     * @param name RPC 服务名称，不允许为 {@code null 或空}
     * @throws IllegalArgumentException 如果 name 为 {@code null} 或空，将会抛出此异常
     */
    public RpcServerPrometheusCollectorConfiguration(int port, String name) throws IllegalArgumentException {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Create `RpcServerPrometheusCollectorConfiguration` failed: `name could not be null or empty`.");
        }
        this.port = port;
        this.name = name;
    }

    /**
     * 获得 RPC 服务端监听端口。
     *
     * @return RPC 服务端监听端口
     */
    public int getPort() {
        return port;
    }

    /**
     * 获得 RPC 服务名称，不会为 {@code null} 或空。
     *
     * @return RPC 服务名称
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "RpcServerPrometheusCollectorConfiguration{" +
                "port=" + port +
                ", name='" + name + '\'' +
                '}';
    }
}
