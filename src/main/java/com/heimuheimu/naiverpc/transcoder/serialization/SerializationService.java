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

package com.heimuheimu.naiverpc.transcoder.serialization;

/**
 * 提供 Java 对象序列化、反序列化服务。
 *
 * <p><strong>说明：</strong> {@code SerializationService} 的实现类必须是线程安全的。</p>
 *
 * @author heimuheimu
 */
public interface SerializationService {

    /**
     * 执行 Java 对象序列化操作，将 Java 对象编码成字节数组后返回。
     *
     * <p><b>注意：</b>实现类需支持 {@code null} 的序列化操作</p>
     *
     * @param value 需要执行序列化操作的 Java 对象，允许为 {@code null}
     * @return 序列化后的字节数组
     * @throws Exception 序列化操作出错时，抛出此异常
     */
    byte[] encode(Object value) throws Exception;

    /**
     * 执行 Java 对象反序列化操作，将字节数组还原成 Java 对象后返回。
     *
     * @param encodedBytes 编码后的字节数组
     * @return 还原后的 Java 对象
     * @throws Exception 序列化操作出错时，抛出此异常
     */
    Object decode(byte[] encodedBytes) throws Exception;

}
