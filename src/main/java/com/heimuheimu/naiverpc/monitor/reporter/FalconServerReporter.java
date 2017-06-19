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

package com.heimuheimu.naiverpc.monitor.reporter;

import com.heimuheimu.naiverpc.monitor.ExecutionTimeInfo;
import com.heimuheimu.naiverpc.monitor.rpc.server.RpcExecuteMonitor;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于 Falcon 系统的 RPC 服务提供者监控数据上报
 *
 * @author heimuheimu
 */
@SuppressWarnings("unused")
public class FalconServerReporter extends AbstractFalconReporter {

    private volatile long lastTpsCount = 0;

    private volatile long lastExecutionCount = 0;

    private volatile long lastTotalExecutionTime = 0;

    private volatile long lastErrorCount = 0;

    public FalconServerReporter(String pushUrl) {
        super(pushUrl);
    }

    @Override
    protected List<FalconData> getPushDataList() {
        List<FalconData> dataList = new ArrayList<>();
        dataList.add(getServerTps());
        dataList.add(getServerPeakTps());
        dataList.add(getServerAverageExecutionTime());
        dataList.add(getServerErrorCount());
        return dataList;
    }

    private FalconData getServerTps() {
        long tpsCount = RpcExecuteMonitor.get().getTpsInfo().getCount();
        FalconData tpsData = create();
        tpsData.metric = "naiverpc_server_tps";
        tpsData.value = (tpsCount - lastTpsCount) / REPORT_INTERVAL_SECONDS;
        lastTpsCount = tpsCount;
        return tpsData;
    }

    private FalconData getServerPeakTps() {
        FalconData peakTpsData = create();
        peakTpsData.metric = "naiverpc_server_peak_tps";
        peakTpsData.value = RpcExecuteMonitor.get().getTpsInfo().getPeakTps();
        return peakTpsData;
    }

    private FalconData getServerAverageExecutionTime() {
        ExecutionTimeInfo executionTimeInfo = RpcExecuteMonitor.get().getExecutionTimeInfo();
        long executionCount = executionTimeInfo.getCount();
        long totalExecutionTime = executionTimeInfo.getTotalExecutionTime();
        FalconData avgExecTimeData = create();
        avgExecTimeData.metric = "naiverpc_server_avg_exec_time";
        avgExecTimeData.value = (totalExecutionTime - lastTotalExecutionTime) / (executionCount - lastExecutionCount);
        lastExecutionCount = executionCount;
        lastTotalExecutionTime = totalExecutionTime;
        return avgExecTimeData;
    }

    private FalconData getServerErrorCount() {
        FalconData errorCountData = create();
        errorCountData.metric = "naiverpc_server_error";
        long errorCount = RpcExecuteMonitor.get().getError();
        errorCountData.value = errorCount - lastErrorCount;
        lastErrorCount = errorCount;
        return errorCountData;
    }

}
