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

package com.heimuheimu.naiverpc.monitor.client;

import com.heimuheimu.naivemonitor.monitor.CompressionMonitor;

/**
 * RPC 客户端使用的压缩信息监控器工厂类。
 *
 * @author heimuheimu
 */
public class RpcClientCompressionMonitorFactory {

    private RpcClientCompressionMonitorFactory() {
        //private constructor
    }

    private static final CompressionMonitor CLIENT_COMPRESSION_MONITOR = new CompressionMonitor();

    /**
     * 获得 RPC 客户端使用的压缩信息监控器。
     *
     * @return 压缩信息监控器
     */
    public static CompressionMonitor get() {
        return CLIENT_COMPRESSION_MONITOR;
    }

}
