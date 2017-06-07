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

package com.heimuheimu.naiverpc.packet;

import com.heimuheimu.naiverpc.constant.OperationCode;

import java.util.Arrays;

/**
 * RPC 数据包，通过 {@link com.heimuheimu.naiverpc.channel.RpcChannel} 进行传输
 * <p>数据包由 24 字节头部信息，以及变长内容信息组成，内容信息长度允许为 0</p>
 * 24 字节头部信息定义如下：
 * <ul>
 *     <li>第 1 个字节为 <b>magic byte</b>，请求数据包常量值为 41，响应数据包常量值为 42</li>
 *     <li>第 2 个字节为 <b>opcode</b>，数据包所对应的操作代码，具体定义参考：{@link OperationCode}</li>
 *     <li>第 3 个字节为 <b>body serialization type</b>，Body 采用的序列化协议</li>
 *     <li>第 4 个字节为 <b>body compression type</b>，Body 采用的压缩格式</li>
 *     <li>第 5 到 8 个字节为 <b>body length</b>，Body 长度</li>
 *     <li>第 9 到 16 个字节为 <b>packet id</b> ，在同一个通信管道内发送的请求数据包，该 ID 值需唯一</li>
 *     <li>第 17 个字节为 <b>reponse status</b>，响应状态码，0 代表成功，错误码则由具体的操作自行定义，该字节仅在响应数据包中有意义</li>
 *     <li>第 18 到 24 字节为保留字节，由具体的操作自行定义</li>
 * </ul>
 *
 * @author heimuheimu
 */
public class RpcPacket {

    /**
     * RPC 请求数据包 <b>Magic Byte</b> 值
     */
    public static final byte REQUEST_MAGIC_BYTE = 41;

    /**
     * RPC 响应数据包 <b>Magic Byte</b> 值
     */
    public static final byte RESPONSE_MAGIC_BYTE = 42;

    /**
     * RPC 数据包头部信息，固定长度 24 字节
     */
    private final byte[] header;

    /**
     * RPC 数据包内容信息，长度允许为 0
     */
    private final byte[] body;

    /**
     * 构造一个 RPC 数据包，头部信息长度必须为 24 字节，内容信息允许为 {@code null} 或者空数组
     *
     * @param header 头部信息，长度必须为 24 字节
     * @param body 内容信息，允许为 {@code null} 或者空数组
     * @throws IllegalArgumentException 头部信息长度不为 24 字节
     * @throws IllegalArgumentException Magic byte 不为 41 或 42
     */
    public RpcPacket(byte[] header, byte[] body) throws IllegalArgumentException {
        if (header == null || header.length != 24) {
            throw new IllegalArgumentException("Header length must be 24 bytes: `" + Arrays.toString(header) + "`.");
        }
        if (header[0] != REQUEST_MAGIC_BYTE && header[0] != RESPONSE_MAGIC_BYTE) {
            throw new IllegalArgumentException("Invalid magic byte: `" + header[0] + "`. Header: `" + Arrays.toString(header) + "`.");
        }
        this.header = header;
        if (body != null) {
            this.body = body;
        } else {
            this.body = new byte[0];
        }
    }

    /**
     * 是否为 RPC 请求数据包
     *
     * @return 是否为 RPC 请求数据包
     */
    public boolean isRequestPacket() {
        return header[0] == REQUEST_MAGIC_BYTE;
    }

    /**
     * 是否为 RPC 响应数据包
     *
     * @return 是否为 RPC 响应数据包
     */
    public boolean isResponsePacket() {
        return header[0] == RESPONSE_MAGIC_BYTE;
    }

    /**
     * 获得 RPC 数据包头部信息，固定长度 24 字节
     *
     * @return RPC 数据包头部信息，固定长度 24 字节
     */
    public byte[] getHeader() {
        return header;
    }

    /**
     * 获得 RPC 数据包内容信息，可能为空数组，不会返回 {@code null}
     *
     * @return RPC 数据包内容信息，可能为空数组，不会返回 {@code null}
     */
    public byte[] getBody() {
        return body;
    }

    /**
     * 获得数据包所对应的操作代码
     *
     * @return 数据包所对应的操作代码
     */
    public byte getOpcode() {
        return header[1];
    }

    /**
     * 获得 Body 采用的序列化协议
     *
     * @return Body 采用的序列化协议
     */
    public byte getSerializationType() {
        return header[2];
    }

    /**
     * 获得 Body 采用的压缩格式
     *
     * @return Body 采用的压缩格式
     */
    public byte getCompressionType() {
        return header[3];
    }

    /**
     * 获得响应状态码，0 代表成功，错误码则由具体的操作自行定义
     *
     * @return 响应状态码，0 代表成功
     */
    public byte getResponseStatus() {
        return header[16];
    }

    @Override
    public String toString() {
        return "RpcPacket{" +
                "header=" + Arrays.toString(header) +
                ", body=" + Arrays.toString(body) +
                '}';
    }

}
