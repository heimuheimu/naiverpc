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

package com.heimuheimu.naiverpc.message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * RPC 服务调用请求消息。
 *
 * @author heimuheimu
 */
public class RpcRequestMessage implements Externalizable {

    private static final long serialVersionUID = -5126505809026496877L;

    /**
     * RPC 服务所在 Class 名称
     */
    private String targetClass = "";

    /**
     * RPC 服务 Method 名称，由方法名和参数类型组合构成
     *
     * @see com.heimuheimu.naiverpc.util.ReflectUtil#getMethodUniqueName(Method)
     */
    private String methodUniqueName = "";

    /**
     * RPC 服务调用所使用的参数数组
     */
    private Object[] arguments = null;

    /**
     * 获得 RPC 服务所在 Class 名称。
     *
     * @return RPC 服务所在 Class 名称
     */
    public String getTargetClass() {
        return targetClass;
    }

    /**
     * 设置 RPC 服务所在 Class 名称。
     *
     * @param targetClass RPC 服务所在 Class 名称
     */
    public void setTargetClass(String targetClass) {
        this.targetClass = targetClass;
    }

    /**
     * 获得 RPC 服务 Method 名称，由方法名和参数类型组合构成。
     *
     * @return RPC 服务 Method 名称，由方法名和参数类型组合构成
     * @see com.heimuheimu.naiverpc.util.ReflectUtil#getMethodUniqueName(Method)
     */
    public String getMethodUniqueName() {
        return methodUniqueName;
    }

    /**
     * 设置 RPC 服务 Method 名称，由方法名和参数类型组合构成。
     *
     * @param methodUniqueName RPC 服务 Method 名称，由方法名和参数类型组合构成
     * @see com.heimuheimu.naiverpc.util.ReflectUtil#getMethodUniqueName(Method)
     */
    public void setMethodUniqueName(String methodUniqueName) {
        this.methodUniqueName = methodUniqueName;
    }

    /**
     * 获得 RPC 服务调用所使用的参数数组。
     *
     * @return RPC 服务调用所使用的参数数组
     */
    public Object[] getArguments() {
        return arguments;
    }

    /**
     * 设置 RPC 服务调用所使用的参数数组。
     *
     * @param arguments RPC 服务调用所使用的参数数组
     */
    public void setArguments(Object[] arguments) {
        this.arguments = arguments;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(targetClass);
        out.writeUTF(methodUniqueName);
        out.writeObject(arguments);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
        targetClass = in.readUTF();
        methodUniqueName = in.readUTF();
        arguments = (Object[]) in.readObject();
    }

    @Override
    public String toString() {
        return "RpcRequestMessage{" +
                "targetClass='" + targetClass + '\'' +
                ", methodUniqueName='" + methodUniqueName + '\'' +
                ", arguments=" + Arrays.toString(arguments) +
                '}';
    }

}
