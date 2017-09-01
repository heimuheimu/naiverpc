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

package com.heimuheimu.naiverpc.client;

import java.lang.reflect.Method;

/**
 * RPC 服务调用客户端监听器抽象实现类，继承该类的监听器，仅需重载自己所关心的事件，
 * 可防止 {@link RpcClientListener} 在后续版本增加方法时，需重新调整监听器实现类。
 *
 * @author heimuheimu
 */
public abstract class RpcClientListenerSkeleton implements RpcClientListener {

    @Override
    public void onClassNotFound(RpcClient client, Method method, Object[] args) {
        //do nothing
    }

    @Override
    public void onNoSuchMethod(RpcClient client, Method method, Object[] args) {
        //do nothing
    }

    @Override
    public void onIllegalArgument(RpcClient client, Method method, Object[] args) {
        //do nothing
    }

    @Override
    public void onInvocationTargetError(RpcClient client, Method method, Object[] args, String errorMessage) {
        //do nothing
    }

    @Override
    public void onTimeout(RpcClient client, Method method, Object[] args) {
        //do nothing
    }

    @Override
    public void onError(RpcClient client, Method method, Object[] args) {
        //do nothing
    }

    @Override
    public void onTooBusy(RpcClient client, Method method, Object[] args) {
        //do nothing
    }

    @Override
    public void onClosed(RpcClient client, Method method, Object[] args) {
        //do nothing
    }

    @Override
    public void onSlowExecution(RpcClient client, Method method, Object[] args, long executedNanoTime) {
        //do nothing
    }

}
