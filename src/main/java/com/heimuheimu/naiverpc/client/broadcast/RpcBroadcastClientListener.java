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

import java.lang.reflect.Method;

/**
 * {@link RpcBroadcastClient} 事件监听器，可监听广播客户端中 {@code DirectRpcClient} 的 RPC 调用成功或失败事件。
 *
 * <p>
 *     <strong>说明：</strong>监听器的实现类必须是线程安全的。
 * </p>
 *
 * @author heimuheimu
 */
public interface RpcBroadcastClientListener {

    /**
     * 当 RPC 调用成功时，将会触发此事件。
     *
     * @param host 提供 RPC 服务的主机地址
     * @param method RPC 调用的方法
     * @param args RPC 调用使用的参数数组，如果没有参数则使用 {@code null} 或空数组
     */
    void onSuccess(String host, Method method, Object[] args);

    /**
     * 当 RPC 调用失败时，将会触发此事件。
     *
     * <p><strong>注意：</strong>如果因 {@code DirectRpcClient} 关闭导致的调用失败不会触发此事件。</p>
     *
     * @param host 提供 RPC 服务的主机地址
     * @param method RPC 调用的方法
     * @param args RPC 调用使用的参数数组，如果没有参数则使用 {@code null} 或空数组
     */
    void onFail(String host, Method method, Object[] args);
}
