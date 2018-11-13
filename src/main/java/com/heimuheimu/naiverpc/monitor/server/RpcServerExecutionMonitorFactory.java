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

package com.heimuheimu.naiverpc.monitor.server;

import com.heimuheimu.naivemonitor.monitor.ExecutionMonitor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RPC 服务端使用的操作执行信息监控工厂类。
 *
 * @author heimuheimu
 */
public class RpcServerExecutionMonitorFactory {

    private RpcServerExecutionMonitorFactory() {
        //private constructor
    }

    private static final ConcurrentHashMap<Integer, ExecutionMonitor> SERVER_EXECUTION_MONITOR_MAP = new ConcurrentHashMap<>();

    private static final Object lock = new Object();

    /**
     * RPC 服务端操作执行错误码：调用方法抛出异常
     */
    public static final int ERROR_CODE_INVOCATION_ERROR = -3;

    /**
     * RPC 服务端操作执行错误码：慢执行
     */
    public static final int ERROR_CODE_SLOW_EXECUTION = -4;

    /**
     * 根据 RPC 服务端监听端口，获得对应的操作执行信息监控器，该方法不会返回 {@code null}。
     *
     * @param listenPort RPC 服务端监听端口
     * @return RPC 服务端监听端口对应的操作执行信息监控器，该方法不会返回 {@code null}
     */
    public static ExecutionMonitor get(int listenPort) {
        ExecutionMonitor monitor = SERVER_EXECUTION_MONITOR_MAP.get(listenPort);
        if (monitor == null) {
            synchronized (lock) {
                monitor = SERVER_EXECUTION_MONITOR_MAP.get(listenPort);
                if (monitor == null) {
                    monitor = new ExecutionMonitor();
                    SERVER_EXECUTION_MONITOR_MAP.put(listenPort, monitor);
                }
            }
        }
        return monitor;
    }

    /**
     * 获得当前 RPC 服务端使用的操作执行信息监控工厂管理的所有操作执行信息监控列表。
     *
     * @return 当前操作执行信息监控工厂管理的所有操作执行信息监控列表
     */
    public static List<ExecutionMonitor> getAll() {
        return new ArrayList<>(SERVER_EXECUTION_MONITOR_MAP.values());
    }
}
