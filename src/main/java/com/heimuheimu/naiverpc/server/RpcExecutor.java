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

package com.heimuheimu.naiverpc.server;

import com.heimuheimu.naiverpc.channel.RpcChannel;
import com.heimuheimu.naiverpc.packet.RpcPacket;

import java.io.Closeable;

/**
 * RPC 服务调用执行器
 *
 * @author heimuheimu
 */
public interface RpcExecutor extends Closeable {

    /**
     * 注册一个 RPC 服务实例，注册完成后，该实例所实现的接口方法就可以通过 RPC 的形式提供给 RPC 调用方调用
     * <p>注意：注册的实例必须继承 1 个或 1 个以上个数的接口，RPC 服务通常以接口的方式提供给 RPC 调用方调用</p>
     *
     * @param service 注册的 RPC 服务实例
     * @throws IllegalArgumentException 如果注册的 RPC 服务实例未继承任何接口
     */
    void register(Object service) throws IllegalArgumentException;

    /**
     * 执行 RPC 服务调用请求
     *
     * @param channel 发送 RPC 服务调用请求的客户端数据管道
     * @param packet RPC 服务调用请求数据包
     */
    void execute(RpcChannel channel, RpcPacket packet);

}
