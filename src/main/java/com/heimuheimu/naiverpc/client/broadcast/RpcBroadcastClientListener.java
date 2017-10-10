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
 * {@link RpcBroadcastClient} 事件监听器，可监听广播客户端中 {@code DirectRpcClient} 的创建、关闭、恢复等事件以及 RPC 调用失败事件。
 *
 * <p>
 *     <strong>说明：</strong>监听器的实现类必须是线程安全的。应优先考虑继承 {@link RpcBroadcastClientListenerSkeleton} 骨架类进行实现，
 *     防止 {@code RpcBroadcastClientListener} 在后续版本增加监听事件时，带来的编译错误。
 * </p>
 *
 * @author heimuheimu
 */
public interface RpcBroadcastClientListener {

    /**
     * 当 {@code DirectRpcClient} 在 {@code RpcBroadcastClient} 初始化过程被创建成功时，将会触发此事件。
     *
     * @param host 创建成功的提供 RPC 服务的主机地址，由主机名和端口组成，":"符号分割，例如：localhost:4182
     */
    void onCreated(String host);

    /**
     * 当 {@code DirectRpcClient} 恢复时，将会触发此事件。
     *
     * @param host 已恢复的提供 RPC 服务的主机地址，由主机名和端口组成，":"符号分割，例如：localhost:4182
     */
    void onRecovered(String host);

    /**
     * 当 {@code DirectRpcClient} 关闭时，将会触发此事件。
     *
     * @param host 已关闭的提供 RPC 服务的主机地址，由主机名和端口组成，":"符号分割，例如：localhost:4182
     * @param isOffline 是否为接收到 RPC 服务提供方发送的下线操作请求导致的关闭
     */
    void onClosed(String host, boolean isOffline);

    /**
     * 当 RPC 调用失败时，将会触发此事件。
     *
     * @param host 提供 RPC 服务的主机地址
     * @param method RPC 调用的方法
     * @param args RPC 调用使用的参数数组，如果没有参数则使用 {@code null} 或空数组
     */
    void onFailedExecuted(String host, Method method, Object[] args);
}
