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

import com.heimuheimu.naiverpc.monitor.ExecutionTimeInfo;
import com.heimuheimu.naiverpc.monitor.TpsInfo;

import java.util.concurrent.atomic.AtomicLong;

/**
 * RPC 服务执行统计信息
 * <p>当前实现是线程安全的</p>
 *
 * @author heimuheimu
 * @ThreadSafe
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class RpcExecuteInfo {

    /**
     * RPC 服务执行 TPS 统计信息
     */
    private final TpsInfo tpsInfo = new TpsInfo();

    /**
     * RPC 服务执行时间统计信息
     */
    private final ExecutionTimeInfo executionTimeInfo = new ExecutionTimeInfo();

    /**
     * RPC 服务执行成功次数
     */
    private final AtomicLong success = new AtomicLong();

    /**
     * RPC 服务执行失败次数
     */
    private final AtomicLong error = new AtomicLong();

    /**
     * 新增一个 RPC 服务执行成功统计
     *
     * @param startTime 调用开始时间(nanoTime)
     */
    public void addSuccess(long startTime) {
        tpsInfo.add();
        executionTimeInfo.add(startTime);
        success.incrementAndGet();
    }

    /**
     * 新增一个 RPC 服务执行失败统计
     *
     * @param startTime 调用开始时间(nanoTime)
     */
    public void addError(long startTime) {
        tpsInfo.add();
        executionTimeInfo.add(startTime);
        error.incrementAndGet();
    }

    /**
     * 获得 RPC 服务执行 TPS 统计信息
     *
     * @return RPC 服务执行 TPS 统计信息
     */
    public TpsInfo getTpsInfo() {
        return tpsInfo;
    }

    /**
     * 获得 RPC 服务执行时间统计信息
     *
     * @return RPC 服务执行时间统计信息
     */
    public ExecutionTimeInfo getExecutionTimeInfo() {
        return executionTimeInfo;
    }

    /**
     * 获得 RPC 服务执行成功次数
     *
     * @return RPC 服务执行成功次数
     */
    public long getSuccess() {
        return success.get();
    }

    /**
     * 获得 RPC 服务执行失败次数
     *
     * @return RPC 服务执行失败次数
     */
    public long getError() {
        return error.get();
    }

    @Override
    public String toString() {
        return "RpcExecuteInfo{" +
                "tpsInfo=" + tpsInfo +
                ", executionTimeInfo=" + executionTimeInfo +
                ", success=" + success +
                ", error=" + error +
                '}';
    }

}
