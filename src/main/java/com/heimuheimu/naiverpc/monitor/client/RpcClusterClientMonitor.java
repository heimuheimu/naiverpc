/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 heimuheimu
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

package com.heimuheimu.naiverpc.monitor.client;

import java.util.concurrent.atomic.AtomicLong;

/**
 * RPC 集群客户端信息监控器。
 *
 * @author heimuheimu
 */
public class RpcClusterClientMonitor {

    private static final RpcClusterClientMonitor INSTANCE = new RpcClusterClientMonitor();

    /**
     * 获取不可用 RPC 客户端的次数
     */
    private final AtomicLong unavailableClientCount = new AtomicLong();

    private RpcClusterClientMonitor() {
        //private constructor
    }

    /**
     * 对 RPC 集群客户端获取到不可用 RPC 客户端的次数进行监控。
     */
    public void onUnavailable() {
        unavailableClientCount.incrementAndGet();
    }

    /**
     * 获得获取不可用 RPC 客户端的次数。
     *
     * @return 获取不可用客户端的次数
     */
    public long getUnavailableClientCount() {
        return unavailableClientCount.get();
    }

    /**
     * 获得 RPC 集群客户端信息监控器。
     *
     * @return RPC 集群客户端信息监控器
     */
    public static RpcClusterClientMonitor getInstance() {
        return INSTANCE;
    }
}
