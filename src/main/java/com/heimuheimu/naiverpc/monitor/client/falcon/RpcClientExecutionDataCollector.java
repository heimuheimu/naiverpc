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

package com.heimuheimu.naiverpc.monitor.client.falcon;

import com.heimuheimu.naivemonitor.falcon.support.AbstractExecutionDataCollector;
import com.heimuheimu.naivemonitor.monitor.ExecutionMonitor;
import com.heimuheimu.naiverpc.monitor.FalconReporterConstant;
import com.heimuheimu.naiverpc.monitor.client.RpcClientExecutionMonitorFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RPC 客户端使用的执行信息采集器
 *
 * @author heimuheimu
 */
public class RpcClientExecutionDataCollector extends AbstractExecutionDataCollector {

    private static final Map<Integer, String> ERROR_METRIC_SUFFIX_MAP;

    static {
        ERROR_METRIC_SUFFIX_MAP = new HashMap<>();
        ERROR_METRIC_SUFFIX_MAP.put(RpcClientExecutionMonitorFactory.ERROR_CODE_TOO_BUSY, "_too_busy");
        ERROR_METRIC_SUFFIX_MAP.put(RpcClientExecutionMonitorFactory.ERROR_CODE_TIMEOUT, "_timeout");
        ERROR_METRIC_SUFFIX_MAP.put(RpcClientExecutionMonitorFactory.ERROR_CODE_TIMEOUT, "_error");
    }

    private final String collectorName;

    private final List<ExecutionMonitor> executionMonitorList;

    /**
     * 构造一个 RPC 客户端使用的执行信息采集器，将会采集所有 RPC 客户端的执行信息信息
     */
    public RpcClientExecutionDataCollector() {
        this.collectorName = "client";
        this.executionMonitorList = null;
    }

    /**
     * 构造一个 RPC 客户端使用的执行信息采集器，仅采集指定连接地址的客户端列表执行信息
     *
     * @param groupName 采集的 Socket 组名称，Collector 的 name 为 client_${groupName}
     * @param hosts 需要监控的连接地址列表，以 "," 进行分割，例如："localhost:4182,localhost:4183,localhost:4184..."
     */
    public RpcClientExecutionDataCollector(String groupName, String hosts) {
        this(groupName, hosts.split(","));
    }

    /**
     * 构造一个 RPC 客户端使用的 Socket 信息采集器，仅采集指定连接地址的 Socket 信息
     *
     * @param groupName 采集的 Socket 组名称，Collector 的 name 为 client_${groupName}
     * @param hosts 需要监控的连接地址列表
     */
    public RpcClientExecutionDataCollector(String groupName, String[] hosts) {
        this.collectorName = "client_" + groupName;
        this.executionMonitorList = new ArrayList<>();
        for (String host : hosts) {
            executionMonitorList.add(RpcClientExecutionMonitorFactory.get(host));
        }
    }

    @Override
    protected List<ExecutionMonitor> getExecutionMonitorList() {
        if (executionMonitorList != null) {
            return executionMonitorList;
        } else {
            return RpcClientExecutionMonitorFactory.getAll();
        }
    }

    @Override
    protected String getModuleName() {
        return FalconReporterConstant.MODULE_NAME;
    }

    @Override
    protected String getCollectorName() {
        return collectorName;
    }

    @Override
    protected Map<Integer, String> getErrorMetricSuffixMap() {
        return ERROR_METRIC_SUFFIX_MAP;
    }

    @Override
    public int getPeriod() {
        return FalconReporterConstant.REPORT_PERIOD;
    }
}
