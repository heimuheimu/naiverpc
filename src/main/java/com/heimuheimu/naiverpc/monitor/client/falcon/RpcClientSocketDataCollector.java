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

import com.heimuheimu.naivemonitor.falcon.support.AbstractSocketDataCollector;
import com.heimuheimu.naivemonitor.monitor.SocketMonitor;
import com.heimuheimu.naiverpc.constant.FalconDataCollectorConstant;
import com.heimuheimu.naiverpc.monitor.client.RpcClientSocketMonitorFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * RPC 客户端使用的 Socket 信息采集器
 *
 * @author heimuheimu
 */
public class RpcClientSocketDataCollector extends AbstractSocketDataCollector {

    private final String collectorName;

    private final List<SocketMonitor> socketMonitorList;

    /**
     * 构造一个 RPC 客户端使用的 Socket 信息采集器，将会采集 RPC 客户端使用的所有 Socket 信息
     */
    public RpcClientSocketDataCollector() {
        this.collectorName = "client";
        this.socketMonitorList = null;
    }

    /**
     * 构造一个 RPC 客户端使用的 Socket 信息采集器，仅采集指定连接地址的 Socket 信息
     *
     * @param groupName 采集的 Socket 组名称，Collector 的 name 为 client_${groupName}
     * @param hosts 需要监控的连接地址列表，以 "," 进行分割，例如："localhost:4182,localhost:4183,localhost:4184..."
     */
    public RpcClientSocketDataCollector(String groupName, String hosts) {
        this(groupName, hosts.split(","));
    }

    /**
     * 构造一个 RPC 客户端使用的 Socket 信息采集器，仅采集指定连接地址的 Socket 信息
     *
     * @param groupName 采集的 Socket 组名称，Collector 的 name 为 client_${groupName}
     * @param hosts 需要监控的连接地址列表
     */
    public RpcClientSocketDataCollector(String groupName, String[] hosts) {
        this.collectorName = "client_" + groupName;
        this.socketMonitorList = new ArrayList<>();
        for (String host : hosts) {
            socketMonitorList.add(RpcClientSocketMonitorFactory.get(host));
        }
    }

    @Override
    protected List<SocketMonitor> getSocketMonitorList() {
        if (socketMonitorList != null) {
            return socketMonitorList;
        } else {
            return RpcClientSocketMonitorFactory.getAll();
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
    public int getPeriod() {
        return FalconDataCollectorConstant.REPORT_PERIOD;
    }
}
