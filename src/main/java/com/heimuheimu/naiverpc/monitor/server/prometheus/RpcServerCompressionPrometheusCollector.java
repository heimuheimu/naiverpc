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

import com.heimuheimu.naivemonitor.monitor.CompressionMonitor;
import com.heimuheimu.naivemonitor.prometheus.PrometheusData;
import com.heimuheimu.naivemonitor.prometheus.PrometheusSample;
import com.heimuheimu.naivemonitor.prometheus.support.AbstractCompressionPrometheusCollector;
import com.heimuheimu.naiverpc.monitor.server.RpcServerCompressionMonitorFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * RPC 压缩操作信息采集器，采集时会返回以下数据：
 * <ul>
 *     <li>naiverpc_server_compression_count{name="$serverName",port="$listenPort"} 相邻两次采集周期内已执行的压缩次数</li>
 *     <li>naiverpc_server_compression_reduce_bytes{name="$serverName",port="$listenPort"} 相邻两次采集周期内通过压缩节省的字节总数</li>
 * </ul>
 *
 * @author heimuheimu
 * @since 1.2
 */
public class RpcServerCompressionPrometheusCollector extends AbstractCompressionPrometheusCollector {

    /**
     * RPC 服务端信息采集器配置列表
     */
    private final List<RpcServerPrometheusCollectorConfiguration> configurationList;

    /**
     * 构造一个 RpcServerCompressionPrometheusCollector 实例。
     *
     * @param configurationList 配置信息列表，不允许为 {@code null} 或空
     * @throws IllegalArgumentException 如果 configurationList 为 {@code null} 或空，将会抛出此异常
     */
    public RpcServerCompressionPrometheusCollector(List<RpcServerPrometheusCollectorConfiguration> configurationList) throws IllegalArgumentException {
        if (configurationList == null || configurationList.isEmpty()) {
            throw new IllegalArgumentException("Create `RpcServerCompressionPrometheusCollector` failed: `configurationList could not be empty`.");
        }
        this.configurationList = configurationList;
    }

    @Override
    protected String getMetricPrefix() {
        return "naiverpc_server";
    }

    @Override
    protected List<CompressionMonitor> getMonitorList() {
        List<CompressionMonitor> monitorList = new ArrayList<>();
        for (RpcServerPrometheusCollectorConfiguration configuration : configurationList) {
            monitorList.add(RpcServerCompressionMonitorFactory.get(configuration.getPort()));
        }
        return monitorList;
    }

    @Override
    protected String getMonitorId(CompressionMonitor monitor, int index) {
        return String.valueOf(index);
    }

    @Override
    protected void afterAddSample(int monitorIndex, PrometheusData data, PrometheusSample sample) {
        RpcServerPrometheusCollectorConfiguration configuration = configurationList.get(monitorIndex);
        sample.addSampleLabel("name", configuration.getName())
                .addSampleLabel("port", String.valueOf(configuration.getPort()));
    }
}
