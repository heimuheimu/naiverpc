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

import com.heimuheimu.naiverpc.monitor.ExecutionTimeInfo;
import com.heimuheimu.naiverpc.monitor.TpsInfo;

import java.util.concurrent.atomic.AtomicLong;

/**
 * RPC 服务调用客户端统计信息
 * <p>当前实现是线程安全的</p>
 *
 * @author heimuheimu
 * @ThreadSafe
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class RpcClientInfo {

    /**
     * 提供 RPC 服务的主机地址，由主机名和端口组成，":" 符号分割，例如：localhost:4182
     */
    private final String host;

    /**
     * RPC 服务调用 TPS 统计信息
     */
    private final TpsInfo tpsInfo = new TpsInfo();

    /**
     * RPC 服务调用执行时间统计信息
     */
    private final ExecutionTimeInfo executionTimeInfo = new ExecutionTimeInfo();

    /**
     * RPC 调用成功次数
     */
    private final AtomicLong success = new AtomicLong();

    /**
     * RPC 调用超时次数
     */
    private final AtomicLong timeout = new AtomicLong();

    /**
     * RPC 调用因远程主机繁忙被拒绝
     */
    private final AtomicLong tooBusy = new AtomicLong();

    /**
     * RPC 调用失败次数
     */
    private final AtomicLong error = new AtomicLong();

    /**
     * 构造一个 RPC 服务调用客户端统计信息
     *
     * @param host 提供 RPC 服务的主机地址，由主机名和端口组成，":" 符号分割，例如：localhost:4182
     */
    public RpcClientInfo(String host) {
        this.host = host;
    }

    /**
     * 新增一个 RPC 服务调用统计
     *
     * @param startTime 调用开始时间(nanoTime)
     * @param executeResult RPC 服务调用结果
     */
    public void add(long startTime, RpcResponse executeResult) {
        tpsInfo.add();
        executionTimeInfo.add(startTime);
        switch (executeResult) {
            case SUCCESS:
                success.incrementAndGet();
                break;
            case TIMEOUT:
                timeout.incrementAndGet();
                break;
            case TOO_BUSY:
                tooBusy.incrementAndGet();
                break;
            case ERROR:
                error.incrementAndGet();
                break;
            default:
                throw new IllegalArgumentException("Unsupported RpcResponse: `" + executeResult + "`.");
        }
    }

    /**
     * 获得提供 RPC 服务的主机地址，由主机名和端口组成，":" 符号分割，例如：localhost:4182
     *
     * @return 提供 RPC 服务的主机地址，由主机名和端口组成，":" 符号分割，例如：localhost:4182
     */
    public String getHost() {
        return host;
    }

    /**
     * 获得 RPC 服务调用 TPS 统计信息
     *
     * @return RPC 服务调用 TPS 统计信息
     */
    public TpsInfo getTpsInfo() {
        return tpsInfo;
    }

    /**
     * 获得 RPC 服务调用执行时间统计信息
     *
     * @return RPC 服务调用执行时间统计信息
     */
    public ExecutionTimeInfo getExecutionTimeInfo() {
        return executionTimeInfo;
    }

    /**
     * 获得 RPC 调用成功次数
     *
     * @return RPC 调用成功次数
     */
    public long getSuccess() {
        return success.get();
    }

    /**
     * 获得 RPC 调用超时次数
     *
     * @return RPC 调用超时次数
     */
    public long getTimeout() {
        return timeout.get();
    }

    /**
     * 获得 RPC 调用因远程主机繁忙被拒绝次数
     *
     * @return RPC 调用因远程主机繁忙被拒绝
     */
    public long getTooBusy() {
        return tooBusy.get();
    }

    /**
     * 获得 RPC 调用失败次数
     *
     * @return RPC 调用失败次数
     */
    public long getError() {
        return error.get();
    }

    @Override
    public String toString() {
        return "RpcClientInfo{" +
                "host='" + host + '\'' +
                ", tpsInfo=" + tpsInfo +
                ", executionTimeInfo=" + executionTimeInfo +
                ", success=" + success +
                ", timeout=" + timeout +
                ", tooBusy=" + tooBusy +
                ", error=" + error +
                '}';
    }

}
