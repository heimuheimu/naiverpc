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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * RPC 服务调用方与 RPC 服务提供方进行数据通信的数据载体，由 24 字节头部信息，以及变长内容信息组成，内容信息长度允许为 0。
 *
 * <h3>24 字节头部信息定义</h3>
 * <blockquote>
 *     <table border summary="头部信息定义">
 *         <tr>
 *             <th>起始索引</th>
 *             <th>结束索引</th>
 *             <th>字节长度</th>
 *             <th>名称</th>
 *             <th>描述</th>
 *         </tr>
 *         <tr>
 *             <td>1</td>
 *             <td>1</td>
 *             <td>1</td>
 *             <td>Magic byte</td>
 *             <td>数据类型，请求数据值为 {@link #REQUEST_MAGIC_BYTE}，响应数据值为 {@link #RESPONSE_MAGIC_BYTE}</td>
 *         </tr>
 *         <tr>
 *             <td>2</td>
 *             <td>2</td>
 *             <td>1</td>
 *             <td>opcode</td>
 *             <td>操作代码，具体定义参考：{@link OperationCode}</td>
 *         </tr>
 *         <tr>
 *             <td>3</td>
 *             <td>3</td>
 *             <td>1</td>
 *             <td>body serialization type</td>
 *             <td>内容信息采用的序列化协议</td>
 *         </tr>
 *         <tr>
 *             <td>4</td>
 *             <td>4</td>
 *             <td>1</td>
 *             <td>body compression type</td>
 *             <td>内容信息采用的压缩格式</td>
 *         </tr>
 *         <tr>
 *             <td>5</td>
 *             <td>8</td>
 *             <td>4</td>
 *             <td>body length</td>
 *             <td>内容信息字节长度</td>
 *         </tr>
 *         <tr>
 *             <td>9</td>
 *             <td>16</td>
 *             <td>8</td>
 *             <td>packet id</td>
 *             <td>RPC 数据 ID，在同一通信管道内保证唯一</td>
 *         </tr>
 *         <tr>
 *             <td>17</td>
 *             <td>17</td>
 *             <td>1</td>
 *             <td>response status</td>
 *             <td>响应状态码，0 代表成功，错误码则由具体的操作自行定义，该字节仅在响应数据中有意义</td>
 *         </tr>
 *         <tr>
 *             <td>18</td>
 *             <td>24</td>
 *             <td>7</td>
 *             <td>reserved bytes</td>
 *             <td>预留字节，允许具体操作自行定义其含义</td>
 *         </tr>
 * </table>
 * </blockquote>
 *
 * <p><strong>说明：</strong>{@code RpcPacket} 类是线程安全的，可在多个线程中使用同一个实例。</p>
 *
 * @author heimuheimu
 */
public class RpcPacket {
    
    private final static Logger LOGGER = LoggerFactory.getLogger(RpcPacket.class);
    
    /**
     * RPC 请求数据 <b>Magic Byte</b> 值
     */
    public static final byte REQUEST_MAGIC_BYTE = 41;

    /**
     * RPC 响应数据 <b>Magic Byte</b> 值
     */
    public static final byte RESPONSE_MAGIC_BYTE = 42;

    /**
     * RPC 数据头部信息，固定长度 24 字节
     */
    private final byte[] header;

    /**
     * RPC 数据内容信息，长度允许为 0
     */
    private final byte[] body;

    /**
     * 构造一个 RPC 数据，头部信息长度必须为 24 字节，内容信息允许为 {@code null} 或者空数组。
     *
     * @param header 头部信息，长度必须为 24 字节
     * @param body 内容信息，允许为 {@code null} 或者空数组
     * @throws IllegalArgumentException 头部信息长度不为 24 字节
     * @throws IllegalArgumentException Magic byte 不为 {@link #REQUEST_MAGIC_BYTE} 或 {@link #RESPONSE_MAGIC_BYTE}
     */
    public RpcPacket(byte[] header, byte[] body) throws IllegalArgumentException {
        if (header == null || header.length != 24) {
            LOGGER.error("Create RpcPacket failed: `header length must be 24 bytes`. Header: `" + Arrays.toString(header) + "`.");
            throw new IllegalArgumentException("Create RpcPacket failed: `header length must be 24 bytes`. Header: `" + Arrays.toString(header) + "`.");
        }
        if (header[0] != REQUEST_MAGIC_BYTE && header[0] != RESPONSE_MAGIC_BYTE) {
            LOGGER.error("Create RpcPacket failed: `invalid magic byte [" + header[0] + "]`. Header: `" + Arrays.toString(header) + "`.");
            throw new IllegalArgumentException("Create RpcPacket failed: `invalid magic byte [" + header[0] + "]`. Header: `" + Arrays.toString(header) + "`.");
        }
        this.header = header;
        if (body != null) {
            this.body = body;
        } else {
            this.body = new byte[0];
        }
    }

    /**
     * 判断当前 RPC 数据是否为请求数据。
     *
     * @return 是否为请求数据
     */
    public boolean isRequestPacket() {
        return header[0] == REQUEST_MAGIC_BYTE;
    }

    /**
     * 判断当前 RPC 数据是否为响应数据。
     *
     * @return 是否为响应数据
     */
    public boolean isResponsePacket() {
        return header[0] == RESPONSE_MAGIC_BYTE;
    }

    /**
     * 获得 RPC 数据头部信息，字节数组长度为 24 字节。
     *
     * <p><strong>注意：</strong>为保证线程安全，获取后请勿修改头部信息。</p>
     *
     * @return RPC 数据头部信息
     */
    public byte[] getHeader() {
        return header;
    }

    /**
     * 获得 RPC 数据内容信息，该方法不会返回 {@code null}。
     *
     * <p><strong>注意：</strong>为保证线程安全，获取后请勿修改内容信息。</p>
     *
     * @return RPC 数据内容信息，不会返回 {@code null}
     */
    public byte[] getBody() {
        return body;
    }

    /**
     * 获得当前 RPC 数据所对应的操作代码，具体定义参考：{@link OperationCode}。
     *
     * @return 操作代码
     * @see OperationCode
     */
    public byte getOpcode() {
        return header[1];
    }

    /**
     * 获得当前 RPC 数据内容信息采用的序列化协议。
     *
     * @return 内容信息采用的序列化协议
     */
    public byte getSerializationType() {
        return header[2];
    }

    /**
     * 获得当前 RPC 数据内容信息采用的压缩格式。
     *
     * @return 内容信息采用的压缩格式
     */
    public byte getCompressionType() {
        return header[3];
    }

    /**
     * 获得当前 RPC 数据对应的响应状态码，0 代表成功，错误码则由具体的操作自行定义，该字节仅在响应数据中有意义。
     *
     * @return 响应状态码，0 代表成功，错误码则由具体的操作自行定义
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
