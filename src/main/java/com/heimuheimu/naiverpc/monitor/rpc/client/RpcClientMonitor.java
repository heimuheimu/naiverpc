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

package com.heimuheimu.naiverpc.monitor.rpc.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RPC 服务调用客户端信息统计
 *
 * @author heimuheimu
 */
public class RpcClientMonitor {

    private static final Logger LOG = LoggerFactory.getLogger(RpcClientMonitor.class);

    private static final RpcClientInfo GLOBAL_INFO = new RpcClientInfo("");

    private static final ConcurrentHashMap<String, RpcClientInfo> RPC_CLIENT_INFO_MAP = new ConcurrentHashMap<>();

    private static final Object lock = new Object();

    private RpcClientMonitor() {
        //private constructor
    }

    /**
     * 增加一个 RPC 服务调用成功信息统计
     *
     * @param host 提供 RPC 服务的主机地址，由主机名和端口组成，":" 符号分割，例如：localhost:4182
     * @param startTime 调用开始时间(nanoTime)
     */
    public static void addSuccess(String host, long startTime) {
        add(host, startTime, RpcResponse.SUCCESS);
    }

    /**
     * 增加一个 RPC 服务调用超时信息统计
     *
     * @param host 提供 RPC 服务的主机地址，由主机名和端口组成，":" 符号分割，例如：localhost:4182
     * @param startTime 调用开始时间(nanoTime)
     */
    public static void addTimeout(String host, long startTime) {
        add(host, startTime, RpcResponse.TIMEOUT);
    }

    /**
     * 增加一个 RPC 服务调用失败信息统计
     *
     * @param host 提供 RPC 服务的主机地址，由主机名和端口组成，":" 符号分割，例如：localhost:4182
     * @param startTime 调用开始时间(nanoTime)
     */
    public static void addError(String host, long startTime) {
        add(host, startTime, RpcResponse.ERROR);
    }

    /**
     * 获得全局 RPC 服务调用客户端统计信息
     *
     * @return 全局 RPC 服务调用客户端统计信息
     */
    public static RpcClientInfo getGlobalInfo() {
        return GLOBAL_INFO;
    }

    /**
     * 获得 RPC 服务调用客户端统计信息 Map，Key 为提供 RPC 服务的主机地址，Value 为该地址对应的 RPC 服务调用客户端统计信息
     * <p>注意：全局 RPC 服务调用客户端统计信息的 Key 为空字符串</p>
     *
     * @return RPC 服务调用客户端统计信息 Map
     */
    @SuppressWarnings("unused")
    public static Map<String, RpcClientInfo> get() {
        HashMap<String, RpcClientInfo> rpcClientInfoHashMap = new HashMap<>(RPC_CLIENT_INFO_MAP);
        rpcClientInfoHashMap.put("", GLOBAL_INFO);
        return rpcClientInfoHashMap;
    }

    /**
     * 新增一个 RPC 服务调用统计
     *
     * @param host 提供 RPC 服务的主机地址，由主机名和端口组成，":" 符号分割，例如：localhost:4182
     * @param startTime 调用开始时间(nanoTime)
     * @param rpcResponse RPC 服务调用结果
     */
    private static void add(String host, long startTime, RpcResponse rpcResponse) {
        try {
            GLOBAL_INFO.add(startTime, rpcResponse);
            RpcClientInfo rpcClientInfo = get(host);
            rpcClientInfo.add(startTime, rpcResponse);
        } catch (Exception e) {
            //should not happen
            LOG.error("Unexpected error. Host: `" + host + "`. Start time: `" + startTime
                    + "`. RpcResponse: `" + rpcResponse + ".", e);
        }
    }

    private static RpcClientInfo get(String host) {
        RpcClientInfo rpcClientInfo = RPC_CLIENT_INFO_MAP.get(host);
        if (rpcClientInfo == null) {
            synchronized (lock) {
                rpcClientInfo = RPC_CLIENT_INFO_MAP.get(host);
                //noinspection Java8MapApi
                if (rpcClientInfo == null) {
                    rpcClientInfo = new RpcClientInfo(host);
                    RPC_CLIENT_INFO_MAP.put(host, rpcClientInfo);
                }
            }
        }
        return rpcClientInfo;
    }

}
