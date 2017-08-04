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
import com.heimuheimu.naiverpc.monitor.socket.SocketMonitor;
import com.heimuheimu.naiverpc.monitor.thread.ThreadPoolMonitor;
import com.heimuheimu.naiverpc.monitor.thread.ThreadPoolType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    private volatile long lastTooBusyCount = 0;

    private volatile long lastErrorCount = 0;

    private volatile long lastReadBytes = 0;

    private volatile long lastWriteBytes = 0;

    private volatile long lastRejectedCount = 0;

    public FalconRpcClientReporter(String pushUrl) {
        super(pushUrl);
    }

    public FalconRpcClientReporter(String pushUrl, Map<String, String> endpointAliasMap) {
        super(pushUrl, endpointAliasMap);
    }

    @Override
    protected List<FalconData> getPushDataList() {
        List<FalconData> dataList = new ArrayList<>();
        dataList.add(getClientTps());
        dataList.add(getClientPeakTps());
        dataList.add(getClientAverageExecutionTime());
        dataList.add(getClientMaxExecutionTime());
        dataList.add(getClientTimeoutCount());
        dataList.add(getClientTooBusyCount());
        dataList.add(getClientErrorCount());
        dataList.add(getClientReadBytes());
        dataList.add(getClientWriteBytes());
        dataList.add(getActiveCount());
        dataList.add(getPoolSize());
        dataList.add(getPeakPoolSize());
        dataList.add(getCorePoolSize());
        dataList.add(getMaximumPoolSize());
        dataList.add(getRejectedCount());
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

    private FalconData getClientMaxExecutionTime() {
        ExecutionTimeInfo executionTimeInfo = RpcClientMonitor.getGlobalInfo().getExecutionTimeInfo();
        FalconData data = create();
        data.metric = "naiverpc_client_max_exec_time";
        data.value = executionTimeInfo.getMaxExecutionTime();
        executionTimeInfo.resetMaxExecutionTime();
        return data;
    }

    private FalconData getClientTimeoutCount() {
        FalconData timeoutCountData = create();
        timeoutCountData.metric = "naiverpc_client_timeout";
        long timeoutCount = RpcClientMonitor.getGlobalInfo().getTimeout();
        timeoutCountData.value = timeoutCount - lastTimeoutCount;
        lastTimeoutCount = timeoutCount;
        return timeoutCountData;
    }

    private FalconData getClientTooBusyCount() {
        FalconData tooBusyCountData = create();
        tooBusyCountData.metric = "naiverpc_client_too_busy";
        long tooBusyCount = RpcClientMonitor.getGlobalInfo().getTooBusy();
        tooBusyCountData.value = tooBusyCount - lastTooBusyCount;
        lastTooBusyCount = tooBusyCount;
        return tooBusyCountData;
    }

    private FalconData getClientErrorCount() {
        FalconData errorCountData = create();
        errorCountData.metric = "naiverpc_client_error";
        long errorCount = RpcClientMonitor.getGlobalInfo().getError();
        errorCountData.value = errorCount - lastErrorCount;
        lastErrorCount = errorCount;
        return errorCountData;
    }

    private FalconData getClientReadBytes() {
        FalconData readBytesData = create();
        readBytesData.metric = "naiverpc_client_read_bytes";
        long readBytes = SocketMonitor.getGlobalInfo().getReadSize().getSize();
        readBytesData.value = readBytes - lastReadBytes;
        lastReadBytes = readBytes;
        return readBytesData;
    }

    private FalconData getClientWriteBytes() {
        FalconData writeBytesData = create();
        writeBytesData.metric = "naiverpc_client_write_bytes";
        long writeBytes = SocketMonitor.getGlobalInfo().getWriteSize().getSize();
        writeBytesData.value = writeBytes - lastWriteBytes;
        lastWriteBytes = writeBytes;
        return writeBytesData;
    }

    private FalconData getActiveCount() {
        FalconData data = create();
        data.metric = "naiverpc_client_threadPool_active_count";
        data.value = ThreadPoolMonitor.getActiveCount(ThreadPoolType.RPC_CLIENT);
        return data;
    }

    private FalconData getPoolSize() {
        FalconData data = create();
        data.metric = "naiverpc_client_threadPool_pool_size";
        data.value = ThreadPoolMonitor.getPoolSize(ThreadPoolType.RPC_CLIENT);
        return data;
    }

    private FalconData getPeakPoolSize() {
        FalconData data = create();
        data.metric = "naiverpc_client_threadPool_peak_pool_size";
        data.value = ThreadPoolMonitor.getPeakPoolSize(ThreadPoolType.RPC_CLIENT);
        return data;
    }

    private FalconData getCorePoolSize() {
        FalconData data = create();
        data.metric = "naiverpc_client_threadPool_core_pool_size";
        data.value = ThreadPoolMonitor.getCorePoolSize(ThreadPoolType.RPC_CLIENT);
        return data;
    }

    private FalconData getMaximumPoolSize() {
        FalconData data = create();
        data.metric = "naiverpc_client_threadPool_maximum_pool_size";
        data.value = ThreadPoolMonitor.getMaximumPoolSize(ThreadPoolType.RPC_CLIENT);
        return data;
    }

    private FalconData getRejectedCount() {
        FalconData data = create();
        data.metric = "naiverpc_client_threadPool_rejected_count";
        long rejectedCount = ThreadPoolMonitor.getRejectedCount(ThreadPoolType.RPC_CLIENT);
        data.value = rejectedCount - lastRejectedCount;
        lastRejectedCount = rejectedCount;
        return data;
    }

}
