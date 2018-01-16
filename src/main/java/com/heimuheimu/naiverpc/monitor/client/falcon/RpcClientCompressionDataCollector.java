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

import com.heimuheimu.naivemonitor.falcon.support.AbstractCompressionDataCollector;
import com.heimuheimu.naivemonitor.monitor.CompressionMonitor;
import com.heimuheimu.naiverpc.constant.FalconDataCollectorConstant;
import com.heimuheimu.naiverpc.monitor.client.RpcClientCompressionMonitorFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * RPC 客户端使用的压缩信息采集器
 *
 * @author heimuheimu
 */
public class RpcClientCompressionDataCollector extends AbstractCompressionDataCollector {

    private final String collectorName;

    private final List<CompressionMonitor> compressionMonitorList;

    /**
     * 构造一个 RPC 客户端使用的压缩信息采集器，将会采集 RPC 客户端所有的压缩信息
     */
    public RpcClientCompressionDataCollector() {
        this.collectorName = "client";
        this.compressionMonitorList = new ArrayList<>();
        this.compressionMonitorList.add(RpcClientCompressionMonitorFactory.get());
    }

    @Override
    protected List<CompressionMonitor> getCompressionMonitorList() {
        return compressionMonitorList;
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
    public int getPeriod() {
        return FalconDataCollectorConstant.REPORT_PERIOD;
    }
}
