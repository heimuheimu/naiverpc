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

import com.heimuheimu.naivemonitor.monitor.ExecutionMonitor;
import com.heimuheimu.naivemonitor.prometheus.PrometheusData;
import com.heimuheimu.naivemonitor.prometheus.PrometheusSample;
import com.heimuheimu.naivemonitor.prometheus.support.AbstractExecutionPrometheusCollector;
import com.heimuheimu.naiverpc.monitor.server.RpcServerExecutionMonitorFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RPC 服务端执行信息采集器，采集时会返回以下数据：
 * <ul>
 *     <li>naiverpc_server_exec_count{name="$serverName",port="$listenPort"} 相邻两次采集周期内 RPC 方法执行次数</li>
 *     <li>naiverpc_server_exec_peak_tps_count{name="$serverName",port="$listenPort"} 相邻两次采集周期内每秒最大 RPC 方法执行次数</li>
 *     <li>naiverpc_server_avg_exec_time_millisecond{name="$serverName",port="$listenPort"} 相邻两次采集周期内单次 RPC 方法平均执行时间，单位：毫秒</li>
 *     <li>naiverpc_server_max_exec_time_millisecond{name="$serverName",port="$listenPort"} 相邻两次采集周期内单次 RPC 方法最大执行时间，单位：毫秒</li>
 *     <li>naiverpc_server_exec_error_count{errorCode="$errorCode",errorType="$errorType",name="$serverName",port="$listenPort"} 相邻两次采集周期内特定类型 RPC 方法执行失败次数</li>
 * </ul>
 *
 * @author heimuheimu
 * @since 1.2
 */
public class RpcServerExecutionPrometheusCollector extends AbstractExecutionPrometheusCollector {

    /**
     * RPC 服务端信息采集器配置列表
     */
    private final List<RpcServerPrometheusCollectorConfiguration> configurationList;

    /**
     * 构造一个 RpcServerExecutionPrometheusCollector 实例。
     *
     * @param configurationList 配置信息列表，不允许为 {@code null} 或空
     * @throws IllegalArgumentException 如果 configurationList 为 {@code null} 或空，将会抛出此异常
     */
    public RpcServerExecutionPrometheusCollector(List<RpcServerPrometheusCollectorConfiguration> configurationList) throws IllegalArgumentException {
        if (configurationList == null || configurationList.isEmpty()) {
            throw new IllegalArgumentException("Create `RpcServerExecutionPrometheusCollector` failed: `configurationList could not be empty`.");
        }
        this.configurationList = configurationList;
    }

    @Override
    protected String getMetricPrefix() {
        return "naiverpc_server";
    }

    @Override
    protected Map<Integer, String> getErrorTypeMap() {
        Map<Integer, String> errorTypeMap = new HashMap<>();
        errorTypeMap.put(RpcServerExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR, "InvocationError");
        errorTypeMap.put(RpcServerExecutionMonitorFactory.ERROR_CODE_SLOW_EXECUTION, "SlowExecution");
        return errorTypeMap;
    }

    @Override
    protected List<ExecutionMonitor> getMonitorList() {
        List<ExecutionMonitor> monitorList = new ArrayList<>();
        for (RpcServerPrometheusCollectorConfiguration configuration : configurationList) {
            monitorList.add(RpcServerExecutionMonitorFactory.get(configuration.getPort()));
        }
        return monitorList;
    }

    @Override
    protected String getMonitorId(ExecutionMonitor monitor, int index) {
        return String.valueOf(index);
    }

    @Override
    protected void afterAddSample(int monitorIndex, PrometheusData data, PrometheusSample sample) {
        RpcServerPrometheusCollectorConfiguration configuration = configurationList.get(monitorIndex);
        sample.addSampleLabel("name", configuration.getName())
                .addSampleLabel("port", String.valueOf(configuration.getPort()));
    }
}
