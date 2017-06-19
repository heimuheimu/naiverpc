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

package com.heimuheimu.naiverpc.monitor.rpc.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC 服务执行信息统计
 *
 * @author heimuheimu
 */
public class RpcExecuteMonitor {

    private static final Logger LOG = LoggerFactory.getLogger(RpcExecuteMonitor.class);

    private static final RpcExecuteInfo rpcExecuteInfo = new RpcExecuteInfo();

    private RpcExecuteMonitor() {
        //private constructor
    }

    /**
     * 新增一个 RPC 服务执行成功统计
     *
     * @param startTime 调用开始时间(nanoTime)
     */
    public static void addSuccess(long startTime) {
        try {
            rpcExecuteInfo.addSuccess(startTime);
        } catch (Exception e) {
            //should not happen
            LOG.error("Unexpected error. Start nano time: `" + startTime + "`.", e);
        }
    }

    /**
     * 新增一个 RPC 服务执行失败统计
     *
     * @param startTime 调用开始时间(nanoTime)
     */
    public static void addError(long startTime) {
        try {
            rpcExecuteInfo.addError(startTime);
        } catch (Exception e) {
            //should not happen
            LOG.error("Unexpected error. Start nano time: `" + startTime + "`.", e);
        }
    }

    /**
     * 获得 RPC 服务执行统计信息
     *
     * @return RPC 服务执行统计信息
     */
    @SuppressWarnings("unused")
    public static RpcExecuteInfo get() {
        return rpcExecuteInfo;
    }

}
