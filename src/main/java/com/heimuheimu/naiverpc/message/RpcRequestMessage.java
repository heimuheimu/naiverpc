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

import java.io.*;
import java.util.Arrays;

/**
 * @author heimuheimu
 */
public class RpcRequestMessage implements Externalizable {

    private static final long serialVersionUID = -5126505809026496877L;

    private String targetClass = "";

    private String methodUniqueName = "";

    private Object[] arguments = null;

    public String getTargetClass() {
        return targetClass;
    }

    public void setTargetClass(String targetClass) {
        this.targetClass = targetClass;
    }

    public String getMethodUniqueName() {
        return methodUniqueName;
    }

    public void setMethodUniqueName(String methodUniqueName) {
        this.methodUniqueName = methodUniqueName;
    }

    public Object[] getArguments() {
        return arguments;
    }

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
