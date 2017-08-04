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
 * 广播 RPC 服务调用客户端，连接多台相同功能的 RPC 服务提供者，每个 RPC 调用都会同时发送至全部（多台） RPC 服务提供者，并返回每个服务的执行结果
 * <p>注意：实现类必须保证是线程安全的</p>
 *
 * @author heimuheimu
 */
public interface RpcBroadcastClient extends Closeable {

    /**
     * 获得当前广播 RPC 服务调用客户端使用的提供 RPC 服务的主机地址数组，该方法不会返回 {@code null}
     * <br>RPC 服务的主机地址由主机名和端口组成，":"符号分割，例如：localhost:4182
     *
     * @return 提供 RPC 服务的主机地址数组
     */
    String[] getHosts();

    /**
     * 广播执行调用远程服务操作，并返回执行结果，超时时间使用客户端默认的超时时间设置。
     * <p>执行结果 Map 的 Key 为提供 RPC 服务的主机地址，Value 为向该服务发起远程调用执行结果。
     * {@link #getHosts()} 中的所有提供 RPC 服务的主机地址均会作为 Key 在执行结果 Map 中出现。</p>
     * <p>注意：该方法不会返回 {@code null}</p>
     *
     * @param method 需要执行的远程服务
     * @param args 执行该远程服务所需的参数，如果没有参数则使用 {@code null} 或空数组
     * @return 执行结果 Map，Key 为提供 RPC 服务的主机地址，Value 为向服务发起远程调用的执行结果
     * @throws IllegalStateException 如果客户端处于不可服务状态，将抛出此异常
     */
    Map<String, BroadcastResponse> execute(Method method, Object[] args) throws IllegalStateException;

    /**
     * 广播执行调用远程服务操作，并返回执行结果。
     * <p>执行结果 Map 的 Key 为提供 RPC 服务的主机地址，Value 为向该服务发起远程调用执行结果。
     * {@link #getHosts()} 中的所有提供 RPC 服务的主机地址均会作为 Key 在执行结果 Map 中出现。</p>
     * <p>注意：该方法不会返回 {@code null}</p>
     *
     * @param method 需要执行的远程服务
     * @param args 执行该远程服务所需的参数，如果没有参数则使用 {@code null} 或空数组
     * @param timeout 超时时间，单位为毫秒，如果小于等于 0，则使用客户端默认的超时时间设置
     * @return 执行结果 Map，Key 为提供 RPC 服务的主机地址，Value 为向服务发起远程调用的执行结果
     * @throws IllegalStateException 如果客户端处于不可服务状态，将抛出此异常
     */
    Map<String, BroadcastResponse> execute(Method method, Object[] args, long timeout) throws IllegalStateException;

    /**
     * 向指定的提供 RPC 服务的主机地址数组广播执行调用远程服务操作，并返回执行结果，超时时间使用客户端默认的超时时间设置。
     * <p>执行结果 Map 的 Key 为提供 RPC 服务的主机地址，Value 为向该服务发起远程调用执行结果。
     * 指定的提供 RPC 服务的主机地址数组中的所有地址均会作为 Key 在执行结果 Map 中出现。</p>
     * <p>注意：指定的提供 RPC 服务的主机地址数组中的地址必须包含在 {@link #getHosts()} 中，
     * 否则该主机的 RPC 调用执行状态码为 {@link BroadcastResponse#CODE_UNKNOWN_HOST}</p>
     *
     * @param hosts 提供 RPC 服务的主机地址数组，数组中的地址必须包含在 {@link #getHosts()} 中，不允许为 {@code null} 或空数组
     * @param method 需要执行的远程服务
     * @param args 执行该远程服务所需的参数，如果没有参数则使用 {@code null} 或空数组
     * @return 执行结果 Map，Key 为提供 RPC 服务的主机地址，Value 为向服务发起远程调用的执行结果
     * @throws IllegalArgumentException 如果 hosts 为 {@code null} 或空数组，将抛出此异常
     * @throws IllegalStateException 如果客户端处于不可服务状态，将抛出此异常
     */
    Map<String, BroadcastResponse> execute(String[] hosts, Method method, Object[] args) throws IllegalArgumentException, IllegalStateException;

    /**
     * 向指定的提供 RPC 服务的主机地址数组广播执行调用远程服务操作，并返回执行结果。
     * <p>执行结果 Map 的 Key 为提供 RPC 服务的主机地址，Value 为向该服务发起远程调用执行结果。
     * 指定的提供 RPC 服务的主机地址数组中的所有地址均会作为 Key 在执行结果 Map 中出现。</p>
     * <p>注意：指定的提供 RPC 服务的主机地址数组中的地址必须包含在 {@link #getHosts()} 中，
     * 否则该主机的 RPC 调用执行状态码为 {@link BroadcastResponse#CODE_UNKNOWN_HOST}</p>
     *
     * @param hosts 提供 RPC 服务的主机地址数组，数组中的地址必须包含在 {@link #getHosts()} 中，不允许为 {@code null} 或空数组
     * @param method 需要执行的远程服务
     * @param args 执行该远程服务所需的参数，如果没有参数则使用 {@code null} 或空数组
     * @param timeout 超时时间，单位为毫秒，如果小于等于 0，则使用客户端默认的超时时间设置
     * @return 执行结果 Map，Key 为提供 RPC 服务的主机地址，Value 为向服务发起远程调用的执行结果
     * @throws IllegalArgumentException 如果 hosts 为 {@code null} 或空数组，将抛出此异常
     * @throws IllegalStateException 如果客户端处于不可服务状态，将抛出此异常
     */
    Map<String, BroadcastResponse> execute(String[] hosts, Method method, Object[] args, long timeout) throws IllegalArgumentException, IllegalStateException;

}
