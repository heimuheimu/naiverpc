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

import com.heimuheimu.naivemonitor.monitor.SocketMonitor;
import com.heimuheimu.naiverpc.util.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * {@link RpcPacket} 读取器，从指定的输入流中读取 RPC 数据。
 *
 * <p><strong>说明：</strong>{@code RpcPacketReader} 类是非线程安全的，不允许多个线程使用同一个实例。</p>
 *
 * @author heimuheimu
 */
public class RpcPacketReader {
    
    private final static Logger LOGGER = LoggerFactory.getLogger(RpcPacketReader.class);

    /**
     * 字节读取数量监控器
     */
    private final SocketMonitor socketMonitor;

    /**
     * 用于读取 RPC 数据的输入流
     */
    private final InputStream inputStream;

    /**
     * 构造一个 {@link RpcPacket} 读取器，从指定的输入流中读取 RPC 数据。
     *
     * @param socketMonitor 字节读取数量信息监控器，不允许为 {@code null}
     * @param inputStream 用于读取 RPC 数据的输入流，不允许为 {@code null}
     * @throws NullPointerException 如果 {@code socketMonitor} 为 {@code null}，将抛出此异常
     * @throws NullPointerException 如果 {@code inputStream} 为 {@code null}，将抛出此异常
     */
    public RpcPacketReader(SocketMonitor socketMonitor, InputStream inputStream) throws NullPointerException {
        if (socketMonitor == null) {
            LOGGER.error("Create RpcPacketReader failed: `SocketMonitor could not be null`.");
            throw new NullPointerException("Create RpcPacketReader failed: `SocketMonitor could not be null`.");
        }
        if (inputStream == null) {
            LOGGER.error("Create RpcPacketReader failed: `InputStream could not be null`.");
            throw new NullPointerException("Create RpcPacketReader failed: `InputStream could not be null`.");
        }
        this.socketMonitor = socketMonitor;
        this.inputStream = inputStream;
    }

    /**
     * 从输入流中读取 {@link RpcPacket}，如果输入流被关闭，则返回 {@code null}。
     *
     * @return 读取的 RPC 数据，如果输入流被关闭，则返回 {@code null}
     * @throws IOException 如果读取 RPC 数据发生 IO 错误，将抛出此异常
     */
    public RpcPacket read() throws IOException {
        int headerPos = 0;
        byte[] header = new byte[24];
        byte[] body = null;
        while (headerPos < 24) {
            int readBytes = inputStream.read(header, headerPos, 24 - headerPos);
            socketMonitor.onRead(readBytes);
            if (readBytes >= 0) {
                headerPos += readBytes;
            } else {
                //流已经关闭，返回null
                return null;
            }
        }
        if (header[0] != RpcPacket.RESPONSE_MAGIC_BYTE &&
                header[0] != RpcPacket.REQUEST_MAGIC_BYTE) {
            throw new IOException("Invalid magic byte: `" + header[0] + "`. Host: `" + socketMonitor.getHost()
                    + "`. Header: `" + Arrays.toString(header) + "`.");
        }
        int bodyLength = ByteUtil.readInt(header, 4);
        if (bodyLength > 0) {
            body = new byte[bodyLength];
            int bodyPos = 0;
            while (bodyPos < bodyLength) {
                int readBytes = inputStream.read(body, bodyPos, bodyLength - bodyPos);
                socketMonitor.onRead(readBytes);
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
