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

import com.heimuheimu.naivemonitor.monitor.SocketMonitor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RPC 服务端使用的 Socket 信息监控工厂类
 *
 * @author heimuheimu
 */
public class RpcServerSocketMonitorFactory {

    private RpcServerSocketMonitorFactory() {
        //private constructor
    }

    private static final ConcurrentHashMap<String, SocketMonitor> SERVER_SOCKET_MONITOR_MAP = new ConcurrentHashMap<>();

    private static final Object lock = new Object();

    /**
     * 根据 Socket 连接目标地址获得对应的 Socket 信息监控器，该方法不会返回 {@code null}
     *
     * @param listenPort 本地监听端口
     * @param remoteHostName Socket 连接目标主机名
     * @return Socket 连接目标地址获得对应的 Socket 信息监控器，该方法不会返回 {@code null}
     */
    public static SocketMonitor get(int listenPort, String remoteHostName) {
        String key = getKey(listenPort, remoteHostName);
        SocketMonitor monitor = SERVER_SOCKET_MONITOR_MAP.get(key);
        if (monitor == null) {
            synchronized (lock) {
                monitor = SERVER_SOCKET_MONITOR_MAP.get(key);
                if (monitor == null) {
                    monitor = new SocketMonitor(remoteHostName);
                    SERVER_SOCKET_MONITOR_MAP.put(key, monitor);
                }
            }
        }
        return monitor;
    }

    /**
     * 获取该本地监听端口下所有的 Socket 信息监控器列表，该方法不会返回 {@code null}
     *
     * @param listenPort 本地监听端口
     * @return 该本地监听端口下所有的 Socket 信息监控器列表，该方法不会返回 {@code null}
     */
    public static List<SocketMonitor> get(int listenPort) {
        List<SocketMonitor> socketMonitorList = new ArrayList<>();
        for (String key : SERVER_SOCKET_MONITOR_MAP.keySet()) {
            if (key.startsWith(listenPort + ":")) {
                socketMonitorList.add(SERVER_SOCKET_MONITOR_MAP.get(key));
            }
        }
        return socketMonitorList;
    }

    /**
     * 获得当前 Socket 信息监控工厂管理的所有 Socket 信息监控列表
     *
     * @return 当前 Socket 信息监控工厂管理的所有 Socket 信息监控列表
     */
    public static List<SocketMonitor> getAll() {
        return new ArrayList<>(SERVER_SOCKET_MONITOR_MAP.values());
    }

    private static String getKey(int listenPort, String remoteHostName) {
        return listenPort + ":" + remoteHostName;
    }
}
