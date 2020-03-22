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

import com.heimuheimu.naivemonitor.monitor.SocketMonitor;
import com.heimuheimu.naivemonitor.prometheus.PrometheusData;
import com.heimuheimu.naivemonitor.prometheus.PrometheusSample;
import com.heimuheimu.naivemonitor.prometheus.support.AbstractSocketPrometheusCollector;
import com.heimuheimu.naiverpc.monitor.server.RpcServerSocketMonitorFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * RPC 服务端 Socket 读、写信息采集器，采集时会返回以下数据：
 * <ul>
 *     <li>naiverpc_server_socket_read_count{name="$serverName",port="$listenPort",remoteAddress="$remoteAddress"} 相邻两次采集周期内 Socket 读取的次数</li>
 *     <li>naiverpc_server_socket_read_bytes{name="$serverName",port="$listenPort",remoteAddress="$remoteAddress"} 相邻两次采集周期内 Socket 读取的字节总数</li>
 *     <li>naiverpc_server_socket_max_read_bytes{name="$serverName",port="$listenPort",remoteAddress="$remoteAddress"} 相邻两次采集周期内单次 Socket 读取的最大字节数</li>
 *     <li>naiverpc_server_socket_write_count{name="$serverName",port="$listenPort",remoteAddress="$remoteAddress"} 相邻两次采集周期内 Socket 写入的次数</li>
 *     <li>naiverpc_server_socket_write_bytes{name="$serverName",port="$listenPort",remoteAddress="$remoteAddress"} 相邻两次采集周期内 Socket 写入的字节总数</li>
 *     <li>naiverpc_server_socket_max_write_bytes{name="$serverName",port="$listenPort",remoteAddress="$remoteAddress"} 相邻两次采集周期内单次 Socket 写入的最大字节数</li>
 * </ul>
 *
 * @author heimuheimu
 * @since 1.2
 */
public class RpcServerSocketPrometheusCollector extends AbstractSocketPrometheusCollector {

    /**
     * RPC 服务端信息采集器配置列表
     */
    private final List<RpcServerPrometheusCollectorConfiguration> configurationList;

    /**
     * 每个 RPC 服务端信息采集器配置对应的监控器最大索引(不包含)列表，与 {@link #configurationList} 一一对应，
     * 每次调用 {@link #getMonitorList()} 方法后都会重新计算。
     * 
     * @see #getMonitorList()
     * @see #getConfigurationByMonitorIndex(int) 
     */
    private final CopyOnWriteArrayList<Integer> monitorEndIndexListForConfiguration = new CopyOnWriteArrayList<>();

    /**
     * 构造一个 RpcServerSocketPrometheusCollector 实例。
     *
     * @param configurationList 配置信息列表，不允许为 {@code null} 或空
     * @throws IllegalArgumentException 如果 configurationList 为 {@code null} 或空，将会抛出此异常
     */
    public RpcServerSocketPrometheusCollector(List<RpcServerPrometheusCollectorConfiguration> configurationList) {
        if (configurationList == null || configurationList.isEmpty()) {
            throw new IllegalArgumentException("Create `RpcServerSocketPrometheusCollector` failed: `configurationList could not be empty`.");
        }
        this.configurationList = configurationList;
    }

    @Override
    protected String getMetricPrefix() {
        return "naiverpc_server";
    }

    @Override
    protected List<SocketMonitor> getMonitorList() {
        monitorEndIndexListForConfiguration.clear();
        List<SocketMonitor> monitorList = new ArrayList<>();
        for (RpcServerPrometheusCollectorConfiguration configuration : configurationList) {
            int listenPort = configuration.getPort();
            List<SocketMonitor> subSocketMonitorList = RpcServerSocketMonitorFactory.get(listenPort);
            monitorList.addAll(subSocketMonitorList);
            monitorEndIndexListForConfiguration.add(monitorList.size());
        }
        return monitorList;
    }

    @Override
    protected String getMonitorId(SocketMonitor monitor, int index) {
        return monitor.getHost();
    }

    @Override
    protected void afterAddSample(int monitorIndex, PrometheusData data, PrometheusSample sample) {
        RpcServerPrometheusCollectorConfiguration configuration = getConfigurationByMonitorIndex(monitorIndex);
        sample.addSampleLabel("name", configuration != null ? configuration.getName() : "unknown")
                .addSampleLabel("port", configuration != null ? String.valueOf(configuration.getPort()) : "-1");
    }

    /**
     * 根据监控器索引获得对应的 RPC 服务端信息采集器配置。
     *
     * <p><strong>注意：</strong>如果调用 {@link #getMonitorList()} 方法后，未及时遍历所有监控器，又再次调用 {@link #getMonitorList()} 方法，
     * 有可能会返回不正确的配置信息或返回 {@code null}。</p>
     *
     * @param monitorIndex 监控器索引
     * @return RPC 服务端信息采集器配置，可能为 {@code null}
     */
    private RpcServerPrometheusCollectorConfiguration getConfigurationByMonitorIndex(int monitorIndex) {
        for (int i = 0; i < monitorEndIndexListForConfiguration.size(); i++) {
            int monitorEndIndex = monitorEndIndexListForConfiguration.get(i);
            if (monitorIndex < monitorEndIndex) {
                return configurationList.get(i);
            }
        }
        return null;
    }
}
