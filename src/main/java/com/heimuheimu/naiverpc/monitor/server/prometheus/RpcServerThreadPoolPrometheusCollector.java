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

import com.heimuheimu.naivemonitor.monitor.ThreadPoolMonitor;
import com.heimuheimu.naivemonitor.prometheus.PrometheusData;
import com.heimuheimu.naivemonitor.prometheus.PrometheusSample;
import com.heimuheimu.naivemonitor.prometheus.support.AbstractThreadPoolPrometheusCollector;
import com.heimuheimu.naiverpc.monitor.server.RpcServerThreadPoolMonitorFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * RPC 服务端使用的线程池信息采集器，采集时会返回以下数据：
 * <ul>
 *     <li>naiverpc_server_threadPool_reject_count{name="$serverName",port="$listenPort"} 相邻两次采集周期内监控器中所有线程池拒绝执行的任务总数</li>
 *     <li>naiverpc_server_threadPool_active_count{name="$serverName",port="$listenPort"} 采集时刻监控器中的所有线程池活跃线程数近似值总和</li>
 *     <li>naiverpc_server_threadPool_pool_size{name="$serverName",port="$listenPort"} 采集时刻监控器中的所有线程池线程数总和</li>
 *     <li>naiverpc_server_threadPool_peak_pool_size{name="$serverName",port="$listenPort"} 监控器中的所有线程池出现过的最大线程数总和</li>
 *     <li>naiverpc_server_threadPool_core_pool_size{name="$serverName",port="$listenPort"} 监控器中的所有线程池配置的核心线程数总和</li>
 *     <li>naiverpc_server_threadPool_maximum_pool_size{name="$serverName",port="$listenPort"} 监控器中的所有线程池配置的最大线程数总和</li>
 * </ul>
 *
 * @author heimuheimu
 * @since 1.2
 */
public class RpcServerThreadPoolPrometheusCollector extends AbstractThreadPoolPrometheusCollector {

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
    public RpcServerThreadPoolPrometheusCollector(List<RpcServerPrometheusCollectorConfiguration> configurationList) throws IllegalArgumentException {
        if (configurationList == null || configurationList.isEmpty()) {
            throw new IllegalArgumentException("Create `RpcServerThreadPoolPrometheusCollector` failed: `configurationList could not be empty`.");
        }
        this.configurationList = configurationList;
    }

    @Override
    protected String getMetricPrefix() {
        return "naiverpc_server";
    }

    @Override
    protected List<ThreadPoolMonitor> getMonitorList() {
        List<ThreadPoolMonitor> monitorList = new ArrayList<>();
        for (RpcServerPrometheusCollectorConfiguration configuration : configurationList) {
            monitorList.add(RpcServerThreadPoolMonitorFactory.get(configuration.getPort()));
        }
        return monitorList;
    }

    @Override
    protected String getMonitorId(ThreadPoolMonitor monitor, int index) {
        return String.valueOf(index);
    }

    @Override
    protected void afterAddSample(int monitorIndex, PrometheusData data, PrometheusSample sample) {
        RpcServerPrometheusCollectorConfiguration configuration = configurationList.get(monitorIndex);
        sample.addSampleLabel("name", configuration.getName())
                .addSampleLabel("port", String.valueOf(configuration.getPort()));
    }
}
