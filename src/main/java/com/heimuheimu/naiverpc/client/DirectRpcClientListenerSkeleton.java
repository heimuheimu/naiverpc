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
 * {@link DirectRpcClient} 事件监听器骨架类，可防止 {@link DirectRpcClientListener} 在后续版本增加监听事件时，带来的编译错误。
 *
 * <p><strong>说明：</strong>监听器的实现类必须是线程安全的。</p>
 *
 * @author heimuheimu
 */
public abstract class DirectRpcClientListenerSkeleton implements DirectRpcClientListener {

    @Override
    public void onClassNotFound(String host, Method method, Object[] args) {
        //do noting
    }

    @Override
    public void onNoSuchMethod(String host, Method method, Object[] args) {
        //do noting
    }

    @Override
    public void onIllegalArgument(String host, Method method, Object[] args) {
        //do noting
    }

    @Override
    public void onInvocationTargetError(String host, Method method, Object[] args, String errorMessage) {
        //do noting
    }

    @Override
    public void onTimeout(String host, Method method, Object[] args) {
        //do noting
    }

    @Override
    public void onError(String host, Method method, Object[] args) {
        //do noting
    }

    @Override
    public void onTooBusy(String host, Method method, Object[] args) {
        //do noting
    }

    @Override
    public void onClosed(String host, Method method, Object[] args) {
        //do noting
    }

    @Override
    public void onSlowExecution(String host, Method method, Object[] args, long executedNanoTime) {
        //do noting
    }
}
