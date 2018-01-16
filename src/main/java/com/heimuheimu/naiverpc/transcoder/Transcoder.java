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

package com.heimuheimu.naiverpc.transcoder;

/**
 * Java 对象与字节数组转换器
 * <p>该接口的实现必须保证线程安全</p>
 *
 * @author heimuheimu
 */
public interface Transcoder {

    /**
     * 将 Java 对象编码成字节数组，并返回长度为 2 的二维数组
     * <ul>
     *     <li>索引 0 的为字节编码信息数组，长度为2，第 1 个字节为序列化类型信息， 第 2 个字节为压缩类型信息</li>
     *     <li>索引 1 的为 value 字节数组，长度不固定</li>
     * </ul>
     *
     * @param value Java 对象
     * @return 长度为 2 的二维数组
     * @throws Exception 编码过程中发生错误
     */
    byte[][] encode(Object value) throws Exception;

    /**
     * 将字节数组还原成 Java 对象并返回
     *
     * @param encodedValue 需要解码的字节数组
     * @param serializationType 序列化类型
     * @param compressionType 压缩类型
     * @return Java 对象
     * @throws Exception 解码过程中发生错误
     */
    <T> T decode(byte[] encodedValue, byte serializationType, byte compressionType) throws Exception;
}
