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
import com.heimuheimu.naiverpc.constant.FalconDataCollectorConstant;
import com.heimuheimu.naiverpc.monitor.client.RpcClientExecutionMonitorFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RPC 客户端使用的执行信息 Falcon 监控数据采集器。该采集器采集周期为 30 秒，每次采集将会返回以下数据项：
 *
 * <ul>
 *     <li>naiverpc_client_{groupName}_too_busy/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 RPC 服务繁忙的错误次数</li>
 *     <li>naiverpc_client_{groupName}_timeout/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 RPC 调用发生超时的错误次数</li>
 *     <li>naiverpc_client_{groupName}_error/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 RPC 调用发生异常的错误次数</li>
 *     <li>naiverpc_client_{groupName}_tps/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内每秒平均执行次数</li>
 *     <li>naiverpc_client_{groupName}_peak_tps/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内每秒最大执行次数</li>
 *     <li>naiverpc_client_{groupName}_avg_exec_time/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内单次 RPC 调用平均执行时间</li>
 *     <li>naiverpc_client_{groupName}_max_exec_time/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内单次 RPC 调用最大执行时间</li>
 * </ul>
 *
 * @author heimuheimu
 */
public class RpcClientExecutionDataCollector extends AbstractExecutionDataCollector {

    private static final Map<Integer, String> ERROR_METRIC_SUFFIX_MAP;

    static {
        ERROR_METRIC_SUFFIX_MAP = new HashMap<>();
        ERROR_METRIC_SUFFIX_MAP.put(RpcClientExecutionMonitorFactory.ERROR_CODE_TOO_BUSY, "_too_busy");
        ERROR_METRIC_SUFFIX_MAP.put(RpcClientExecutionMonitorFactory.ERROR_CODE_TIMEOUT, "_timeout");
        ERROR_METRIC_SUFFIX_MAP.put(RpcClientExecutionMonitorFactory.ERROR_CODE_INVOCATION_ERROR, "_error");
    }

    private final String collectorName;

    private final List<ExecutionMonitor> executionMonitorList;

    /**
     * 构造一个 RPC 客户端使用的执行信息采集器，将会采集所有 RPC 客户端的执行信息信息。
     */
    public RpcClientExecutionDataCollector() {
        this.collectorName = "client";
        this.executionMonitorList = null;
    }

    /**
     * 构造一个 RPC 客户端使用的执行信息采集器，仅采集指定连接地址的客户端列表执行信息。
     *
     * @param groupName 采集的 Socket 组名称，Collector 的 name 为 client_${groupName}
     * @param hosts 需要监控的连接地址列表，以 "," 进行分割，例如："localhost:4182,localhost:4183,localhost:4184..."
     */
    public RpcClientExecutionDataCollector(String groupName, String hosts) {
        this(groupName, hosts.split(","));
    }

    /**
     * 构造一个 RPC 客户端使用的 Socket 信息采集器，仅采集指定连接地址的 Socket 信息。
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
        return FalconDataCollectorConstant.MODULE_NAME;
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
        return FalconDataCollectorConstant.REPORT_PERIOD;
    }
}
