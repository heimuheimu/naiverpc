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
 * RPC 数据包交互管道事件监听器
 * <p>
 *     <b>注意：</b>当需要实现 RPC 数据包交互管道事件监听器时，应优先考虑继承 {@link RpcChannelListenerSkeleton} 抽象类进行实现，
 *     防止 {@link RpcChannelListener} 在后续版本增加方法时，需重新调整监听器实现类。
 * </p>
 *
 * @author heimuheimu
 */
public interface RpcChannelListener {

    /**
     * 当管道接受到一个 RPC 数据包时，将会触发此事件
     *
     * @param channel 接收到该 RPC 数据包的管道
     * @param packet RPC 数据包
     */
    void onReceiveRpcPacket(RpcChannel channel, RpcPacket packet);

    /**
     * 当管道关闭后，将会触发此事件
     *
     * @param channel 已关闭的 RPC 数据包交互管道
     */
    void onClosed(RpcChannel channel);

}
