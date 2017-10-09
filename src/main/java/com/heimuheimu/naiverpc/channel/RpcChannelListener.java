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

package com.heimuheimu.naiverpc.channel;

import com.heimuheimu.naiverpc.packet.RpcPacket;

/**
 * {@link RpcChannel} 事件监听器，可监听 RPC 数据接收事件，以及 {@code RpcChannel} 关闭事件。
 *
 * <p>
 *     <strong>说明：</strong>监听器的实现类必须是线程安全的。应优先考虑继承 {@link RpcChannelListenerSkeleton} 骨架类进行实现，
 *     防止 {@link RpcChannelListener} 在后续版本增加监听事件时，带来的编译错误。
 * </p>
 *
 * @author heimuheimu
 */
public interface RpcChannelListener {

    /**
     * 当管道接收到一个 RPC 数据时，将会触发此事件。
     * <p>
     *   <strong>注意：</strong>心跳检测数据和下线请求数据不会触发此事件。
     * </p>
     *
     * @param channel 接收到 RPC 数据的管道
     * @param packet 接收到的 RPC 数据
     */
    void onReceiveRpcPacket(RpcChannel channel, RpcPacket packet);

    /**
     * 当 RPC 数据通信管道关闭后，将会触发此事件，同一个管道仅触发一次。
     *
     * @param channel 已关闭的 RPC 数据通信管道
     */
    void onClosed(RpcChannel channel);
}
