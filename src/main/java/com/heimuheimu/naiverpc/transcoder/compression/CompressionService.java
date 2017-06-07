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

package com.heimuheimu.naiverpc.transcoder.compression;

/**
 * 提供字节数组的压缩、解压服务
 * <p>实现类需保证实现是线程安全的</p>
 *
 * @author heimuheimu
 * @ThreadSafe
 */
public interface CompressionService {

    /**
     * 压缩字节数组，并返回压缩后的字节数组
     *
     * @param src 被压缩的字节数组
     * @return 已压缩的字节数组
     * @throws Exception 压缩操作出错时，抛出此异常
     */
    byte[] compress(byte[] src) throws Exception;

    /**
     * 解压字节数组，并返回解压后的字节数组
     *
     * @param compressedBytes 已压缩的字节数组
     * @return 解压后的字节数组
     * @throws Exception 解压操作出错时，抛出此异常
     */
    byte[] decompress(byte[] compressedBytes) throws Exception;

}
