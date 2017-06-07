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

import com.heimuheimu.naiverpc.transcoder.compression.CompressionService;
import com.heimuheimu.naiverpc.transcoder.compression.CompressionType;
import com.heimuheimu.naiverpc.transcoder.compression.LZFCompressionService;
import com.heimuheimu.naiverpc.transcoder.serialization.JavaSerializationService;
import com.heimuheimu.naiverpc.transcoder.serialization.SerializationService;
import com.heimuheimu.naiverpc.transcoder.serialization.SerializationType;

/**
 * Java 对象与字节数组转换器
 *
 * @author heimuheimu
 * @ThreadSafe
 */
public class SimpleTranscoder implements Transcoder {

    /**
     * LZF 压缩、解压实现
     */
    private final CompressionService lzfCompressionService = new LZFCompressionService();

    /**
     * Java 自带的序列化、反序列化实现
     */
    private final SerializationService javaSerializationService = new JavaSerializationService();

    /**
     * 当 Value 字节数小于或等于该值，不进行压缩
     */
    private final int compressionThreshold;

    public SimpleTranscoder(int compressionThreshold) {
        this.compressionThreshold = compressionThreshold;
    }

    @Override
    public byte[][] encode(Object value) throws Exception {
        byte[][] result = new byte[2][];
        byte[] flags = new byte[2];
        //使用 Java 自带的序列化方式
        flags[0] = SerializationType.JAVA;
        byte[] valueBytes = javaSerializationService.encode(value);
        if (valueBytes.length > compressionThreshold) {
            //使用 LZF 压缩算法
            valueBytes = lzfCompressionService.compress(valueBytes);
            flags[1] = CompressionType.LZF;
        } else {
            flags[1] = CompressionType.NONE;
        }
        result[0] = flags;
        result[1] = valueBytes;
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T decode(byte[] encodedValue, byte serializationType, byte compressionType) throws Exception {
        switch (compressionType) {
            case CompressionType.NONE:
                break;
            case CompressionType.LZF:
                encodedValue = lzfCompressionService.decompress(encodedValue);
                break;
            default:
                throw new UnsupportedOperationException("Invalid compression type: `" + compressionType + "`.");
        }
        switch (serializationType) {
            case SerializationType.JAVA:
                return (T) javaSerializationService.decode(encodedValue);
            default:
                throw new UnsupportedOperationException("Invalid serialization type: `" + serializationType + "`.");
        }
    }

}
