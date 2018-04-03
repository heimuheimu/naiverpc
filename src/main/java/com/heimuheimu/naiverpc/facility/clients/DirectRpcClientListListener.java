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

package com.heimuheimu.naiverpc.facility.clients;

/**
 * RPC 直连客户端列表事件监听器，可监听集群列表中 {@code DirectRpcClient} 的创建、关闭、恢复等事件。
 *
 * <p>
 *     <strong>说明：</strong>监听器的实现类必须是线程安全的。
 * </p>
 *
 * @author heimuheimu
 */
public interface DirectRpcClientListListener {

    /**
     * 当 {@code DirectRpcClient} 在 {@code DirectRpcClientList} 初始化过程被创建成功时，将会触发此事件。
     *
     * @param listName RPC 直连客户端列表名称
     * @param host 提供 RPC 服务的主机地址数组，由主机名和端口组成，":"符号分割，例如：localhost:4182
     */
    void onCreated(String listName, String host);

    /**
     * 当 {@code DirectRpcClient} 恢复时，将会触发此事件。
     *
     * @param listName RPC 直连客户端列表名称
     * @param host 提供 RPC 服务的主机地址数组，由主机名和端口组成，":"符号分割，例如：localhost:4182
     */
    void onRecovered(String listName, String host);

    /**
     * 当 {@code DirectRpcClient} 关闭时，将会触发此事件。
     *
     * @param listName RPC 直连客户端列表名称
     * @param host 提供 RPC 服务的主机地址数组，由主机名和端口组成，":"符号分割，例如：localhost:4182
     * @param isOffline 是否为接收到 RPC 服务提供方发送的下线操作请求导致的关闭
     */
    void onClosed(String listName, String host, boolean isOffline);
}
