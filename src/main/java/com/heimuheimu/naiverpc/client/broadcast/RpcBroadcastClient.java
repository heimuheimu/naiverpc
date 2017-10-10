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

import java.io.Closeable;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * RPC 服务调用方使用的广播客户端，RPC 调用请求会发送至 {@code RpcBroadcastClient} 中的多个 RPC 服务提供方进行执行，并返回结果 {@code Map}，
 * {@code Map} 的 Key 为 RPC 服务提供方主机地址，Value 为 {@link BroadcastResponse}。
 *
 * <p><strong>说明：</strong> {@code RpcBroadcastClient } 的实现类必须是线程安全的。</p>
 *
 * @author heimuheimu
 */
public interface RpcBroadcastClient extends Closeable {

    /**
     * 获得当前 {@code RpcBroadcastClient} 使用的提供 RPC 服务的主机地址数组，该方法不会返回 {@code null}，
     * RPC 服务的主机地址由主机名和端口组成，":"符号分割，例如：localhost:4182。
     *
     * @return 提供 RPC 服务的主机地址数组
     */
    String[] getHosts();

    /**
     * 向所有 RPC 服务提供方发起调用请求，并返回结果 {@code Map}，{@code Map} 的 Key 为 RPC 服务提供方主机地址，Value 为 {@link BroadcastResponse}，
     * 超时时间使用 {@code RpcBroadcastClient} 实现类默认的超时时间设置。
     *
     * <p><strong>说明：</strong> {@link #getHosts()} 中的所有主机地址均会作为 Key 在结果 {@code Map} 中存在，该方法不会返回 {@code null}。</p>
     *
     * @param method RPC 调用的方法
     * @param args RPC 调用使用的参数数组，如果没有参数则使用 {@code null} 或空数组
     * @return 结果 {@code Map}，{@code Map} 的 Key 为 RPC 服务提供方主机地址，Value 为 {@link BroadcastResponse}
     * @throws IllegalStateException 如果 {@code RpcBroadcastClient} 处于不可服务状态，将抛出此异常
     */
    Map<String, BroadcastResponse> execute(Method method, Object[] args) throws IllegalStateException;

    /**
     * 向所有 RPC 服务提供方发起调用请求，并返回结果 {@code Map}，{@code Map} 的 Key 为 RPC 服务提供方主机地址，Value 为 {@link BroadcastResponse}。
     *
     * <p><strong>说明：</strong> {@link #getHosts()} 中的所有主机地址均会作为 Key 在结果 {@code Map} 中存在，该方法不会返回 {@code null}。</p>
     *
     * @param method RPC 调用的方法
     * @param args RPC 调用使用的参数数组，如果没有参数则使用 {@code null} 或空数组
     * @param timeout RPC 调用超时时间，单位为毫秒，不允许小于等于 0
     * @return 结果 {@code Map}，{@code Map} 的 Key 为 RPC 服务提供方主机地址，Value 为 {@link BroadcastResponse}
     * @throws IllegalStateException 如果 {@code RpcBroadcastClient} 处于不可服务状态，将抛出此异常
     */
    Map<String, BroadcastResponse> execute(Method method, Object[] args, long timeout) throws IllegalStateException;

    /**
     * 向指定的 RPC 服务提供方发起调用请求，并返回结果 {@code Map}，{@code Map} 的 Key 为 RPC 服务提供方主机地址，Value 为 {@link BroadcastResponse}，
     * 超时时间使用 {@code RpcBroadcastClient} 实现类默认的超时时间设置。
     *
     * <p><strong>说明：</strong> 指定的 RPC 服务提供方主机地址均会作为 Key 在结果 {@code Map} 中存在，该方法不会返回 {@code null}。</p>
     *
     * <p><strong>注意：</strong> 指定的 RPC 服务提供方主机地址必须包含在 {@link #getHosts()} 中，否则该主机的 RPC 调用执行状态码为 {@link BroadcastResponse#CODE_UNKNOWN_HOST}。</p>
     *
     * @param hosts RPC 服务提供方主机地址数组，地址必须包含在 {@link #getHosts()} 中，不允许为 {@code null} 或空数组
     * @param method RPC 调用的方法
     * @param args RPC 调用使用的参数数组，如果没有参数则使用 {@code null} 或空数组
     * @return 结果 {@code Map}，{@code Map} 的 Key 为 RPC 服务提供方主机地址，Value 为 {@link BroadcastResponse}
     * @throws IllegalArgumentException 如果 RPC 服务提供方主机地址数组为 {@code null} 或空数组，将抛出此异常
     * @throws IllegalStateException 如果 {@code RpcBroadcastClient} 处于不可服务状态，将抛出此异常
     */
    Map<String, BroadcastResponse> execute(String[] hosts, Method method, Object[] args) throws IllegalArgumentException, IllegalStateException;

    /**
     * 向指定的 RPC 服务提供方发起调用请求，并返回结果 {@code Map}，{@code Map} 的 Key 为 RPC 服务提供方主机地址，Value 为 {@link BroadcastResponse}。
     *
     * <p><strong>说明：</strong> 指定的 RPC 服务提供方主机地址均会作为 Key 在结果 {@code Map} 中存在，该方法不会返回 {@code null}。</p>
     *
     * <p><strong>注意：</strong> 指定的 RPC 服务提供方主机地址必须包含在 {@link #getHosts()} 中，否则该主机的 RPC 调用执行状态码为 {@link BroadcastResponse#CODE_UNKNOWN_HOST}。</p>
     *
     * @param hosts RPC 服务提供方主机地址数组，地址必须包含在 {@link #getHosts()} 中，不允许为 {@code null} 或空数组
     * @param method RPC 调用的方法
     * @param args RPC 调用使用的参数数组，如果没有参数则使用 {@code null} 或空数组
     * @param timeout RPC 调用超时时间，单位为毫秒，不允许小于等于 0
     * @return 执行结果 Map，Key 为提供 RPC 服务的主机地址，Value 为向服务发起远程调用的执行结果
     * @throws IllegalArgumentException 如果 RPC 服务提供方主机地址数组为 {@code null} 或空数组，将抛出此异常
     * @throws IllegalStateException 如果 {@code RpcBroadcastClient} 处于不可服务状态，将抛出此异常
     */
    Map<String, BroadcastResponse> execute(String[] hosts, Method method, Object[] args, long timeout) throws IllegalArgumentException, IllegalStateException;

}
