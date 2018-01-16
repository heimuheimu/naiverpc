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
 * {@code RpcExecutor} 用于执行 RPC 调用方请求的 RPC 方法，并向调用方返回执行结果。
 *
 * <p><strong>说明：</strong>{@code RpcExecutor}的实现类必须是线程安全的。</p>
 *
 * @author heimuheimu
 */
public interface RpcExecutor extends Closeable {

    /**
     * 在 {@code RpcExecutor} 注册一个 RPC 服务，服务注册完成后才可执行。
     *
     * <p><strong>注意：</strong> RPC 服务以接口的形式提供给调用方使用，注册的 RPC 服务必须继承至少 1 个接口。</p>
     *
     * @param service 需要对外提供的 RPC 服务
     * @throws IllegalArgumentException 如果注册的 RPC 服务未继承任何接口，将会抛出此异常
     */
    void register(Object service) throws IllegalArgumentException;

    /**
     * 执行 RPC 服务，并将执行结果通过 {@code RpcChannel} 发送给调用方，该方法不会抛出任何异常。
     *
     * @param channel 发起 RPC 服务调用的 {@code RpcChannel}
     * @param packet RPC 服务调用请求数据
     */
    void execute(RpcChannel channel, RpcPacket packet);
}
