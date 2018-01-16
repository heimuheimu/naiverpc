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

package com.heimuheimu.naiverpc.client.broadcast;

/**
 * 广播 RPC 调用返回结果。
 *
 * <p><strong>说明：</strong>{@code BroadcastResponse} 类是非线程安全的，不允许多个线程使用同一个实例。</p>
 *
 * @author heimuheimu
 */
public class BroadcastResponse {

    /**
     * 状态码：执行成功
     */
    public static final int CODE_SUCCESS = 0;

    /**
     * 状态码：未知的提供 RPC 服务主机地址
     *
     * @see RpcBroadcastClient#getHosts()
     */
    public static final int CODE_UNKNOWN_HOST = -1;

    /**
     * 状态码：提供 RPC 服务主机地址不可用
     */
    public static final int CODE_INVALID_HOST = -2;

    /**
     * 状态码：RPC 调用失败
     */
    public static final int CODE_ERROR = -3;

    /**
     * 提供 RPC 服务的主机地址，由主机名和端口组成，":"符号分割，例如：localhost:4182
     */
    private String host = "";

    /**
     * RPC 调用结果状态码
     */
    private int code = CODE_SUCCESS;

    /**
     * RPC 调用返回结果，在下列几种情况下，该值将会为 {@code null}
     * <ol>
     *     <li>RPC 调用成功，方法返回类型定义为 {@code void}</li>
     *     <li>RPC 调用成功，方法返回值为 {@code null}</li>
     *     <li>RPC 调用失败</li>
     * </ol>
     */
    private Object result = null;

    /**
     * RPC 调用发生的异常信息，在下列几种情况下，该值将会为 {@code null}
     * <ol>
     *     <li>RPC 调用成功</li>
     *     <li>RPC 调用失败，并且执行状态码为 {@link #CODE_UNKNOWN_HOST}</li>
     *     <li>RPC 调用失败，并且执行状态码为 {@link #CODE_INVALID_HOST}</li>
     * </ol>
     */
    private Exception exception = null;

    /**
     * 获得提供 RPC 服务的主机地址，由主机名和端口组成，":"符号分割，例如：localhost:4182。
     *
     * @return 提供 RPC 服务的主机地址
     */
    public String getHost() {
        return host;
    }

    /**
     * 设置提供 RPC 服务的主机地址，由主机名和端口组成，":"符号分割，例如：localhost:4182。
     *
     * @param host 提供 RPC 服务的主机地址
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * 获得 RPC 调用结果状态码。
     *
     * @return RPC 调用结果状态码
     */
    public int getCode() {
        return code;
    }

    /**
     * 设置 RPC 调用结果状态码。
     *
     * @param code RPC 调用结果状态码
     */
    public void setCode(int code) {
        this.code = code;
    }

    /**
     * 获得 RPC 调用返回结果，可能返回 {@code null}。在下列几种情况下，该值将会为 {@code null}
     * <ol>
     *     <li>RPC 调用成功，方法返回类型定义为 {@code void}</li>
     *     <li>RPC 调用成功，方法返回值为 {@code null}</li>
     *     <li>RPC 调用失败</li>
     * </ol>
     *
     * @return RPC 调用返回结果，可能为 {@code null}
     */
    public Object getResult() {
        return result;
    }

    /**
     * 设置 RPC 调用返回结果，允许设置 {@code null}。
     *
     * @param result RPC 调用返回结果
     */
    public void setResult(Object result) {
        this.result = result;
    }

    /**
     * 获得 RPC 调用发生的异常信息，可能返回 {@code null}。在下列几种情况下，该值将会为 {@code null}
     * <ol>
     *     <li>RPC 调用成功</li>
     *     <li>RPC 调用失败，并且执行状态码为 {@link #CODE_UNKNOWN_HOST}</li>
     *     <li>RPC 调用失败，并且执行状态码为 {@link #CODE_INVALID_HOST}</li>
     * </ol>
     *
     * @return RPC 调用发生的异常信息，可能为 {@code null}
     */
    public Exception getException() {
        return exception;
    }

    /**
     * 设置 RPC 调用发生的异常信息，允许设置 {@code null}。
     *
     * @param exception RPC 调用发生的异常信息
     */
    public void setException(Exception exception) {
        this.exception = exception;
    }

    /**
     * 判断 RPC 调用是否成功。
     *
     * @return RPC 调用是否成功
     */
    public boolean isSuccess() {
        return code == CODE_SUCCESS;
    }

    @Override
    public String toString() {
        return "BroadcastResponse{" +
                "host='" + host + '\'' +
                ", code=" + code +
                ", result=" + result +
                ", exception=" + exception +
                '}';
    }

}
