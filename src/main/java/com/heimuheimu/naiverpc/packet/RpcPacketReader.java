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

import com.heimuheimu.naiverpc.monitor.socket.SocketMonitor;
import com.heimuheimu.naiverpc.util.ByteUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * RPC 数据包读取器
 * <p>该实现是非线程安全的</p>
 *
 * @author heimuheimu
 * @NotThreadSafe
 */
public class RpcPacketReader {

    private final String host;

    private final InputStream inputStream;

    public RpcPacketReader(String host, InputStream inputStream) {
        this.host = host;
        this.inputStream = inputStream;
    }

    public RpcPacket read() throws IOException {
        int headerPos = 0;
        byte[] header = new byte[24];
        byte[] body = null;
        while (headerPos < 24) {
            int readBytes = inputStream.read(header, headerPos, 24 - headerPos);
            SocketMonitor.addRead(host, readBytes);
            if (readBytes >= 0) {
                headerPos += readBytes;
            } else {
                //流已经关闭，返回null
                return null;
            }
        }
        if (header[0] != RpcPacket.RESPONSE_MAGIC_BYTE &&
                header[0] != RpcPacket.REQUEST_MAGIC_BYTE) {
            throw new IllegalStateException("Invalid magic byte: `" + header[0] + "`. Host: `" + host
                    + "`. Header: `" + Arrays.toString(header) + "`.");
        }
        int bodyLength = ByteUtil.readInt(header, 4);
        if (bodyLength > 0) {
            body = new byte[bodyLength];
            int bodyPos = 0;
            while (bodyPos < bodyLength) {
                int readBytes = inputStream.read(body, bodyPos, bodyLength - bodyPos);
                SocketMonitor.addRead(host, readBytes);
                if (readBytes >= 0) {
                    bodyPos += readBytes;
                } else {
                    //流已经关闭，返回null
                    return null;
                }
            }
        }
        return new RpcPacket(header, body);
    }

}
