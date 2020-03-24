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

package com.heimuheimu.naiverpc.monitor.client.prometheus;

import com.heimuheimu.naivemonitor.monitor.SocketMonitor;
import com.heimuheimu.naivemonitor.prometheus.PrometheusData;
import com.heimuheimu.naivemonitor.prometheus.PrometheusSample;
import com.heimuheimu.naivemonitor.prometheus.support.AbstractSocketPrometheusCollector;
import com.heimuheimu.naiverpc.monitor.client.RpcClientSocketMonitorFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * RPC 客户端 Socket 读、写信息采集器，采集时会返回以下数据：
 * <ul>
 *     <li>naiverpc_client_socket_read_count{name="$serverName",remoteAddress="$remoteAddress"} 相邻两次采集周期内 Socket 读取的次数</li>
 *     <li>naiverpc_client_socket_read_bytes{name="$serverName",remoteAddress="$remoteAddress"} 相邻两次采集周期内 Socket 读取的字节总数</li>
 *     <li>naiverpc_client_socket_max_read_bytes{name="$serverName",remoteAddress="$remoteAddress"} 相邻两次采集周期内单次 Socket 读取的最大字节数</li>
 *     <li>naiverpc_client_socket_write_count{name="$serverName",remoteAddress="$remoteAddress"} 相邻两次采集周期内 Socket 写入的次数</li>
 *     <li>naiverpc_client_socket_write_bytes{name="$serverName",remoteAddress="$remoteAddress"} 相邻两次采集周期内 Socket 写入的字节总数</li>
 *     <li>naiverpc_client_socket_max_write_bytes{name="$serverName",remoteAddress="$remoteAddress"} 相邻两次采集周期内单次 Socket 写入的最大字节数</li>
 * </ul>
 *
 * @author heimuheimu
 * @since 1.2
 */
public class RpcClientSocketPrometheusCollector extends AbstractSocketPrometheusCollector {

    /**
     * RPC 客户端使用的 Socket 读、写信息监控器列表，不会为 {@code null} 或空，仅允许在构造函数中修改
     */
    private final List<SocketMonitor> monitorList;

    /**
     * Socket 读、写信息监控器访问的 RPC 服务名称列表，与 {@link #monitorList} 一一对应， 不会为 {@code null} 或空，仅允许在构造函数中修改
     */
    private final List<String> nameList;

    /**
     * 构造一个 RpcClientSocketPrometheusCollector 实例。
     *
     * @param configurationList 配置信息列表，不允许为 {@code null} 或空
     * @throws IllegalArgumentException 如果 configurationList 为 {@code null} 或空，将会抛出此异常
     */
    public RpcClientSocketPrometheusCollector(List<RpcClientPrometheusCollectorConfiguration> configurationList) throws IllegalArgumentException {
        if (configurationList == null || configurationList.isEmpty()) {
            throw new IllegalArgumentException("Create `RpcClientSocketPrometheusCollector` failed: `configurationList could not be empty`.");
        }
        monitorList = new ArrayList<>();
        nameList = new ArrayList<>();
        for (RpcClientPrometheusCollectorConfiguration configuration : configurationList) {
            for (String host : configuration.getHostList()) {
                monitorList.add(RpcClientSocketMonitorFactory.get(host));
                nameList.add(configuration.getName());
            }
        }
    }

    @Override
    protected String getMetricPrefix() {
        return "naiverpc_client";
    }

    @Override
    protected List<SocketMonitor> getMonitorList() {
        return monitorList;
    }

    @Override
    protected String getMonitorId(SocketMonitor monitor, int index) {
        return String.valueOf(index);
    }

    @Override
    protected void afterAddSample(int monitorIndex, PrometheusData data, PrometheusSample sample) {
        sample.addSampleLabel("name", nameList.get(monitorIndex));
    }
}
