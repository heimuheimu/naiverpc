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

import com.heimuheimu.naivemonitor.monitor.ThreadPoolMonitor;
import com.heimuheimu.naivemonitor.prometheus.PrometheusData;
import com.heimuheimu.naivemonitor.prometheus.PrometheusSample;
import com.heimuheimu.naivemonitor.prometheus.support.AbstractThreadPoolPrometheusCollector;
import com.heimuheimu.naiverpc.monitor.client.RpcClientThreadPoolMonitorFactory;

import java.util.Collections;
import java.util.List;

/**
 * RPC 客户端使用的线程池信息采集器，采集时会返回以下数据：
 * <ul>
 *     <li>naiverpc_client_threadPool_reject_count 相邻两次采集周期内监控器中所有线程池拒绝执行的任务总数</li>
 *     <li>naiverpc_client_threadPool_active_count 采集时刻监控器中的所有线程池活跃线程数近似值总和</li>
 *     <li>naiverpc_client_threadPool_pool_size 采集时刻监控器中的所有线程池线程数总和</li>
 *     <li>naiverpc_client_threadPool_peak_pool_size 监控器中的所有线程池出现过的最大线程数总和</li>
 *     <li>naiverpc_client_threadPool_core_pool_size 监控器中的所有线程池配置的核心线程数总和</li>
 *     <li>naiverpc_client_threadPool_maximum_pool_size 监控器中的所有线程池配置的最大线程数总和</li>
 * </ul>
 *
 * @author heimuheimu
 * @since 1.2
 */
public class RpcClientThreadPoolPrometheusCollector extends AbstractThreadPoolPrometheusCollector {

    @Override
    protected String getMetricPrefix() {
        return "naiverpc_client";
    }

    @Override
    protected List<ThreadPoolMonitor> getMonitorList() {
        return Collections.singletonList(RpcClientThreadPoolMonitorFactory.get());
    }

    @Override
    protected String getMonitorId(ThreadPoolMonitor monitor, int index) {
        return String.valueOf(index);
    }

    @Override
    protected void afterAddSample(int monitorIndex, PrometheusData data, PrometheusSample sample) {
        // do nothing
    }
}
