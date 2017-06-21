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
import com.heimuheimu.naiverpc.monitor.rpc.client.RpcClientMonitor;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于 Falcon 系统的 RPC 服务调用客户端监控数据上报
 *
 * @author heimuheimu
 */
@SuppressWarnings("unused")
public class FalconRpcClientReporter extends AbstractFalconReporter {

    private volatile long lastTpsCount = 0;

    private volatile long lastExecutionCount = 0;

    private volatile long lastTotalExecutionTime = 0;

    private volatile long lastTimeoutCount = 0;

    private volatile long lastErrorCount = 0;

    public FalconRpcClientReporter(String pushUrl) {
        super(pushUrl);
    }

    @Override
    protected List<FalconData> getPushDataList() {
        List<FalconData> dataList = new ArrayList<>();
        dataList.add(getClientTps());
        dataList.add(getClientPeakTps());
        dataList.add(getClientAverageExecutionTime());
        dataList.add(getClientTimeoutCount());
        dataList.add(getClientErrorCount());
        return dataList;
    }

    private FalconData getClientTps() {
        long tpsCount = RpcClientMonitor.getGlobalInfo().getTpsInfo().getCount();
        FalconData tpsData = create();
        tpsData.metric = "naiverpc_client_tps";
        tpsData.value = (tpsCount - lastTpsCount) / REPORT_INTERVAL_SECONDS;
        lastTpsCount = tpsCount;
        return tpsData;
    }

    private FalconData getClientPeakTps() {
        FalconData peakTpsData = create();
        peakTpsData.metric = "naiverpc_client_peak_tps";
        peakTpsData.value = RpcClientMonitor.getGlobalInfo().getTpsInfo().getPeakTps();
        return peakTpsData;
    }

    private FalconData getClientAverageExecutionTime() {
        ExecutionTimeInfo executionTimeInfo = RpcClientMonitor.getGlobalInfo().getExecutionTimeInfo();
        long executionCount = executionTimeInfo.getCount();
        long totalExecutionTime = executionTimeInfo.getTotalExecutionTime();
        FalconData avgExecTimeData = create();
        avgExecTimeData.metric = "naiverpc_client_avg_exec_time";
        if (executionCount > lastExecutionCount) {
            avgExecTimeData.value = (totalExecutionTime - lastTotalExecutionTime) / (executionCount - lastExecutionCount);
        } else {
            avgExecTimeData.value = 0;
        }
        lastExecutionCount = executionCount;
        lastTotalExecutionTime = totalExecutionTime;
        return avgExecTimeData;
    }

    private FalconData getClientTimeoutCount() {
        FalconData timeoutCountData = create();
        timeoutCountData.metric = "naiverpc_client_timeout";
        long timeoutCount = RpcClientMonitor.getGlobalInfo().getTimeout();
        timeoutCountData.value = timeoutCount - lastTimeoutCount;
        lastTimeoutCount = timeoutCount;
        return timeoutCountData;
    }

    private FalconData getClientErrorCount() {
        FalconData errorCountData = create();
        errorCountData.metric = "naiverpc_client_error";
        long errorCount = RpcClientMonitor.getGlobalInfo().getError();
        errorCountData.value = errorCount - lastErrorCount;
        lastErrorCount = errorCount;
        return errorCountData;
    }

}
