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

import com.heimuheimu.naiverpc.transcoder.Transcoder;
import com.heimuheimu.naiverpc.util.ByteUtil;

/**
 * RPC 数据构造工具类，提供静态方法来构造请求数据、响应数据。
 *
 * @author heimuheimu
 */
public class RpcPacketBuilder {

    private RpcPacketBuilder() {
        //prevent create instance
    }

    /**
     * 生成一个 body 长度为 0 的 RPC 请求数据。
     *
     * @param packetId RPC 数据 ID
     * @param opcode 操作代码
     * @return RPC 请求数据
     */
    public static RpcPacket buildRequestPacket(long packetId, byte opcode) {
        byte[] header = new byte[24];
        header[0] = RpcPacket.REQUEST_MAGIC_BYTE;
        header[1] = opcode;
        ByteUtil.writeLong(packetId, header, 8);
        return new RpcPacket(header, null);
    }

    /**
     * 生成一个 RPC 请求数据。
     *
     * @param packetId RPC 数据 ID
     * @param opcode 操作代码
     * @param bodyObject body 内容
     * @param transcoder 将 body 内容转换为字节数组的转换器
     * @return RPC 请求数据
     * @throws Exception 如果在 body 内容转换为字节数组过程中发生错误，将会抛出此异常
     */
    public static RpcPacket buildRequestPacket(long packetId, byte opcode, Object bodyObject, Transcoder transcoder) throws Exception {
        byte[][] encodedBytes = transcoder.encode(bodyObject);
        byte[] header = new byte[24];
        header[0] = RpcPacket.REQUEST_MAGIC_BYTE;
        header[1] = opcode;
        header[2] = encodedBytes[0][0];
        header[3] = encodedBytes[0][1];
        ByteUtil.writeInt(encodedBytes[1].length, header, 4);
        ByteUtil.writeLong(packetId, header, 8);
        return new RpcPacket(header, encodedBytes[1]);
    }

    /**
     * 生成一个与 RPC 请求数据对应的响应数据，该响应数据的 body 长度为 0，响应数据的操作代码、 RPC 数据 ID 与 RPC 请求数据一致。
     *
     * @param requestRpcPacket RPC 请求数据
     * @param status 响应状态码，0 代表成功，错误码则由具体的操作自行定义
     * @return RPC 响应数据
     */
    public static RpcPacket buildResponsePacket(RpcPacket requestRpcPacket, byte status) {
        byte[] header = new byte[24];
        header[0] = RpcPacket.RESPONSE_MAGIC_BYTE;
        header[1] = requestRpcPacket.getOpcode();
        System.arraycopy(requestRpcPacket.getHeader(), 8, header, 8, 8);
        header[16] = status;
        return new RpcPacket(header, null);
    }

    /**
     * 生成一个与 RPC 请求数据对应的响应数据，该响应数据的操作代码、 RPC 数据 ID 与 RPC 请求数据一致。
     *
     * @param requestRpcPacket RPC 请求数据
     * @param status 响应状态码，0 代表成功，错误码则由具体的操作自行定义
     * @param bodyObject body 内容
     * @param transcoder 将 body 内容转换为字节数组的转换器
     * @return RPC 响应数据
     * @throws Exception 如果在 body 内容转换为字节数组过程中发生错误，将会抛出此异常
     */
    public static RpcPacket buildResponsePacket(RpcPacket requestRpcPacket, byte status, Object bodyObject, Transcoder transcoder) throws Exception {
        byte[][] encodedBytes = transcoder.encode(bodyObject);
        byte[] header = new byte[24];
        header[0] = RpcPacket.RESPONSE_MAGIC_BYTE;
        header[1] = requestRpcPacket.getOpcode();
        header[2] = encodedBytes[0][0];
        header[3] = encodedBytes[0][1];
        ByteUtil.writeInt(encodedBytes[1].length, header, 4);
        System.arraycopy(requestRpcPacket.getHeader(), 8, header, 8, 8);
        header[16] = status;
        return new RpcPacket(header, encodedBytes[1]);
    }

}
