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

package com.heimuheimu.naiverpc.client.cluster;

/**
 * RPC 服务调用集群客户端事件监听器
 *
 * @author heimuheimu
 */
public interface RpcClusterClientListener {

    /**
     * 当 RPC 服务调用客户端在 RPC 服务调用集群客户端初始化过程被创建成功时，将会触发此事件
     *
     * @param host 创建成功的提供 RPC 服务的主机地址，由主机名和端口组成，":"符号分割，例如：localhost:4182
     */
    void onCreated(String host);

    /**
     * 当 RPC 服务调用客户端恢复时，将会触发此事件
     *
     * @param host 已恢复的提供 RPC 服务的主机地址，由主机名和端口组成，":"符号分割，例如：localhost:4182
     */
    void onRecovered(String host);

    /**
     * 当 RPC 服务调用客户端关闭时，将会触发此事件
     *
     * @param host 已关闭的提供 RPC 服务的主机地址，由主机名和端口组成，":"符号分割，例如：localhost:4182
     */
    void onClosed(String host);

}
