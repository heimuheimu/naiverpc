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

package com.heimuheimu.naiverpc.channel;

import com.heimuheimu.naiverpc.constant.BeanStatusEnum;
import com.heimuheimu.naiverpc.constant.OperationCode;
import com.heimuheimu.naiverpc.constant.ResponseStatusCode;
import com.heimuheimu.naiverpc.monitor.socket.SocketMonitor;
import com.heimuheimu.naiverpc.net.SocketBuilder;
import com.heimuheimu.naiverpc.net.SocketConfiguration;
import com.heimuheimu.naiverpc.packet.RpcPacket;
import com.heimuheimu.naiverpc.packet.RpcPacketBuilder;
import com.heimuheimu.naiverpc.packet.RpcPacketReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * RPC 服务调用者 与 RPC 服务提供者进行数据交互的管道，数据载体为 {@link RpcPacket}
 * <p>当前实现是线程安全的</p>
 *
 * @author heimuheimu
 * @ThreadSafe
 */
public class RpcChannel implements Closeable {

    private static final Logger RPC_CONNECTION_LOG = LoggerFactory.getLogger("NAIVERPC_CONNECTION_LOG");

    private static final Logger LOG = LoggerFactory.getLogger(RpcChannel.class);

    /**
     * 等待发送的 RPC 命令队列
     */
    private final LinkedBlockingQueue<RpcPacket> rpcPacketQueue = new LinkedBlockingQueue<>();

    /**
     * 远程主机地址，由主机名和端口组成，":"符号分割，例如：localhost:9610
     */
    private final String host;

    /**
     * 与远程主机地址 {@link #host} 建立的 Socket 连接
     */
    private final Socket socket;

    /**
     * 心跳检测时间，单位：秒，在该周期时间内当前管道如果没有任何数据交互，将会发送一个心跳请求数据包
     */
    private final int heartbeatPeriod;

    /**
     * 当前 RPC 数据包交互管道事件监听器
     */
    private final RpcChannelListener rpcChannelListener;

    /**
     * 当前实例所处状态
     */
    private volatile BeanStatusEnum state = BeanStatusEnum.UNINITIALIZED;

    /**
     * 是否已接收到对方的离线请求消息
     */
    private volatile boolean isOffline = false;

    /**
     * RPC 数据包发送线程
     */
    private WriteTask writeTask;

    /**
     * 创建一个 RPC 服务调用者 与 RPC 服务提供者进行数据交互的管道，数据载体为 {@link RpcPacket}
     * <p><b>注意：</b>管道必须执行初始化操作过后才可正常使用</p>
     *
     * @param host 远程主机地址，由主机名和端口组成，":"符号分割，例如：localhost:9610
     * @param configuration {@link Socket} 配置信息，如果传 {@code null}，将会使用 {@link SocketConfiguration#DEFAULT} 配置信息
     * @param heartbeatPeriod 心跳检测时间，单位：秒，在该周期时间内当前管道如果没有任何数据交互，将会发送一个心跳请求数据包，如果该值小于等于 0，则不进行检测
     * @param rpcChannelListener RPC 数据包交互管道事件监听器
     * @throws IllegalArgumentException 如果远程主机地址不符合规则，将会抛出此异常
     * @throws RuntimeException 如果创建 {@link Socket} 过程中发生错误，将会抛出此异常
     * @see #init()
     */
    public RpcChannel(String host, SocketConfiguration configuration, int heartbeatPeriod, RpcChannelListener rpcChannelListener) throws RuntimeException {
        this.host = host;
        this.socket = SocketBuilder.create(host, configuration);
        this.heartbeatPeriod = heartbeatPeriod;
        this.rpcChannelListener = rpcChannelListener;
    }

    /**
     * 创建一个 RPC 服务调用者 与 RPC 服务提供者进行数据交互的管道，数据载体为 {@link RpcPacket}
     * <p><b>注意：</b>管道必须执行初始化操作过后才可正常使用</p>
     *
     * @param socket 与远程主机地址建立的 Socket 连接，不允许为 {@code null}
     * @param heartbeatPeriod 心跳检测时间，单位：秒，在该周期时间内当前管道如果没有任何数据交互，将会发送一个心跳请求数据包，如果该值小于等于 0，则不进行检测
     * @param rpcChannelListener RPC 数据包交互管道事件监听器，允许为 {@code null}
     * @throws NullPointerException 如果 Socket 连接为 {@code null}，将会抛出此异常
     * @see #init()
     */
    public RpcChannel(Socket socket, int heartbeatPeriod, RpcChannelListener rpcChannelListener) throws NullPointerException {
        if (socket == null) {
            throw new NullPointerException("Socket could not be null.");
        }
        this.host = socket.getInetAddress().getCanonicalHostName() + ":" + socket.getPort();
        this.socket = socket;
        this.heartbeatPeriod = heartbeatPeriod;
        this.rpcChannelListener = rpcChannelListener;
    }

    /**
     * 发送一个 RPC 数据包
     *
     * @param rpcPacket RPC 数据包
     */
    public void send(RpcPacket rpcPacket) throws NullPointerException, IllegalStateException {
        if (rpcPacket == null) {
            throw new NullPointerException("RpcPacket could not be null. Host: `" + host + "`. Socket: `" + socket + "`.");
        }
        if (state == BeanStatusEnum.NORMAL && !isOffline) {
            rpcPacketQueue.add(rpcPacket);
        } else {
            throw new IllegalStateException("RpcChannel is not initialized or has been closed. State: `" + state +
                    "`. Offline: `" + isOffline + "`. Host: `" + host + "`. Socket: `" + socket + "`.");
        }
    }

    /**
     * 执行下线操作请求，远程主机接受到该请求后将不在当前管道发送新的 RPC 数据包，并在 1 分钟后关闭当前管道
     */
    public void offline() {
        rpcPacketQueue.add(RpcPacketBuilder.buildRequestPacket(0, OperationCode.OFFLINE));
        LOG.debug("Send offline request packet success. Host: `{}`.", host);
    }

    /**
     * 判断当前 RPC 数据包交互管道是否可用
     *
     * @return 当前 RPC 数据包交互管道是否可用
     */
    public boolean isActive() {
        return state == BeanStatusEnum.NORMAL && !isOffline;
    }

    /**
     * 执行初始化操作，如果该管道已经初始化完成，则方法不执行任何操作
     */
    public synchronized void init() {
        if (state == BeanStatusEnum.UNINITIALIZED) {
            try {
                if (socket.isConnected() && !socket.isClosed()) {
                    long startTime = System.currentTimeMillis();
                    state = BeanStatusEnum.NORMAL;
                    SocketConfiguration config = SocketBuilder.getConfig(socket);
                    String socketAddress = host + "/" + socket.getLocalPort();
                    writeTask = new WriteTask(config.getSendBufferSize());
                    writeTask.setName("[Write] " + socketAddress);
                    writeTask.start();

                    ReadTask readTask = new ReadTask();
                    readTask.setName("[Read] " + socketAddress);
                    readTask.start();
                    RPC_CONNECTION_LOG.info("RpcChannel has been initialized. Cost: {}ms. Host: `{}`. Local port: `{}`. Heartbeat period: `{}`. Config: `{}`.",
                            (System.currentTimeMillis() - startTime), host, socket.getLocalPort(), heartbeatPeriod, config);
                } else {
                    RPC_CONNECTION_LOG.error("Initialize RpcChannel failed. Socket is not connected or has been closed. Host: `{}`.", host);
                    close();
                }
            } catch (Exception e) {
                RPC_CONNECTION_LOG.error("Initialize RpcChannel failed. Unexpected error: `{}`. Host: `{}`. Heartbeat period: `{}`.", e.getMessage(), host, heartbeatPeriod);
                LOG.error("Initialize RpcChannel failed. Unexpected error. Host: `" + host + "`. Socket: `" + socket + "`.", e);
                close();
            }
        }
    }

    @Override
    public synchronized void close() {
        if (state != BeanStatusEnum.CLOSED) {
            long startTime = System.currentTimeMillis();
            state = BeanStatusEnum.CLOSED;
            try {
                //关闭Socket连接
                socket.close();
                //停止 Write 线程
                writeTask.stopSignal = true;
                writeTask.interrupt();
                RPC_CONNECTION_LOG.info("RpcChannel has been closed. Cost: {}ms. Host: `{}`. Heartbeat period: `{}`.",
                        (System.currentTimeMillis() - startTime), host, heartbeatPeriod);
            } catch (Exception e) {
                RPC_CONNECTION_LOG.error("Close RpcChannel failed. Unexpected error: `{}`. Host: `{}`.", e.getMessage(), host);
                LOG.error("Close RpcChannel failed. Unexpected error. Host: `" + host + "`. Socket: `" + socket + "`.", e);
            }
            if (rpcChannelListener != null) {
                try {
                    rpcChannelListener.onClosed(this);
                } catch (Exception e) {
                    LOG.error("Call RpcChannelListener#onClosed() failed. Host: `" + host + "`. Socket: `" + socket + "`.", e);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "RpcChannel{" +
                "host='" + host + '\'' +
                ", socket=" + socket +
                ", heartbeatPeriod=" + heartbeatPeriod +
                ", rpcChannelListener=" + rpcChannelListener +
                ", state=" + state +
                '}';
    }

    /**
     * RPC 数据包发送线程
     */
    private class WriteTask extends Thread {

        private final int sendBufferSize;

        private int mergedPacketSize = 0;

        private final ArrayList<RpcPacket> mergedPacketList = new ArrayList<>();

        private volatile boolean stopSignal = false;

        private WriteTask(Integer sendBufferSize) {
            this.sendBufferSize = sendBufferSize != null ? sendBufferSize : 32 * 1024;
        }

        @Override
        public void run() {
            try {
                OutputStream outputStream = socket.getOutputStream();
                RpcPacket rpcPacket;
                while (!stopSignal) {
                    if (heartbeatPeriod <= 0) {
                        rpcPacket = rpcPacketQueue.take();
                    } else {
                        rpcPacket = rpcPacketQueue.poll(heartbeatPeriod, TimeUnit.SECONDS);
                    }
                    if (rpcPacket != null) {
                        int packetLength = rpcPacket.getHeader().length + rpcPacket.getBody().length;
                        if ((packetLength + mergedPacketSize) < sendBufferSize) {
                            addToMergedPacket(rpcPacket);
                            if (rpcPacketQueue.size() == 0) {
                                sendMergedPacket(outputStream);
                                outputStream.flush();
                            }
                        } else {
                            sendMergedPacket(outputStream);
                            if (rpcPacketQueue.size() == 0) {
                                byte[] rpcPacketByteArray = new byte[rpcPacket.getHeader().length + rpcPacket.getBody().length];
                                System.arraycopy(rpcPacket.getHeader(), 0, rpcPacketByteArray, 0, rpcPacket.getHeader().length);
                                System.arraycopy(rpcPacket.getBody(), 0, rpcPacketByteArray, rpcPacket.getHeader().length, rpcPacket.getBody().length);
                                outputStream.write(rpcPacketByteArray);
                                SocketMonitor.addWrite(host, rpcPacketByteArray.length);
                            } else {
                                addToMergedPacket(rpcPacket);
                            }
                            outputStream.flush();
                        }
                    } else {
                        rpcPacketQueue.add(RpcPacketBuilder.buildRequestPacket(0, OperationCode.HEARTBEAT));
                        LOG.debug("Send heartbeat request packet success. Host: `{}`.", host);
                    }
                }
            } catch (InterruptedException e) {
                //因当前通道关闭才会抛出此异常，不做任何处理
            } catch (IOException e) {
                RPC_CONNECTION_LOG.error("[WriteTask] RpcChannel need to be closed due to: `IOException: {}`. Host: `{}`.", e.getMessage(), host);
                LOG.error("[WriteTask] RpcChannel need to be closed due to: `IoException`. Host: `" + host
                        + "`. Socket: `" + socket + "`.", e);
                close();
            } catch (Exception e) {
                RPC_CONNECTION_LOG.error("[WriteTask] RpcChannel need to be closed due to: `{}`. Host: `{}`.", e.getMessage(), host);
                LOG.error("[WriteTask] RpcChannel need to be closed: `Unexpected error`. Host: `" + host
                        + "`. Socket: `" + socket + "`.", e);
                close();
            }
        }

        private void addToMergedPacket(RpcPacket rpcPacket) {
            mergedPacketList.add(rpcPacket);
            mergedPacketSize = mergedPacketSize + rpcPacket.getHeader().length + rpcPacket.getBody().length;
        }

        private void sendMergedPacket(OutputStream outputStream) throws IOException {
            byte[] mergedPacket = new byte[mergedPacketSize];
            int destPos = 0;
            for(RpcPacket rpcPacket : mergedPacketList) {
                byte[] header = rpcPacket.getHeader();
                byte[] body = rpcPacket.getBody();
                System.arraycopy(header, 0, mergedPacket, destPos, header.length);
                destPos += header.length;
                System.arraycopy(body, 0, mergedPacket, destPos, body.length);
                destPos += body.length;
            }
            outputStream.write(mergedPacket, 0,  destPos);
            SocketMonitor.addWrite(host, destPos);
            resetMergedPacket();
        }

        private void resetMergedPacket() {
            mergedPacketList.clear();
            mergedPacketSize = 0;
        }

    }

    /**
     * RPC 数据包读取线程
     */
    private class ReadTask extends Thread {

        private final RpcPacketReader reader;

        private ReadTask () throws IOException {
            this.reader = new RpcPacketReader(host, socket.getInputStream());
        }

        @Override
        public void run() {
            try {
                RpcPacket rpcPacket;
                while ((rpcPacket = reader.read()) != null) {
                    if (rpcPacket.getOpcode() == OperationCode.HEARTBEAT) {
                        if (rpcPacket.isRequestPacket()) {
                            rpcPacketQueue.add(RpcPacketBuilder.buildResponsePacket(rpcPacket, ResponseStatusCode.SUCCESS));
                            LOG.debug("Send heartbeat response packet success. Host: `{}`.", host);
                        } else {
                            LOG.debug("Receive heartbeat response packet success. Host: `{}`.", host);
                        }
                    } else if (rpcPacket.getOpcode() == OperationCode.OFFLINE) {
                        if (rpcPacket.isRequestPacket()) {
                            isOffline = true;
                            //noinspection AnonymousHasLambdaAlternative
                            new Thread() {

                                @Override
                                public void run() {
                                    RPC_CONNECTION_LOG.info("Received offline packet. RpcChannel will be closed after 1 minute. {}", RpcChannel.this);
                                    try {
                                        Thread.sleep(1000 * 60);
                                    } catch (InterruptedException ignored) {} //ignore exception
                                    close();
                                }

                            }.start();
                            rpcPacketQueue.add(RpcPacketBuilder.buildResponsePacket(rpcPacket, ResponseStatusCode.SUCCESS));
                            LOG.debug("Send offline response packet success. Host: `{}`.", host);
                        } else {
                            LOG.debug("Receive offline response packet success. Host: `{}`.", host);
                        }
                    } else {
                        if (rpcChannelListener != null) {
                            try {
                                rpcChannelListener.onReceiveRpcPacket(RpcChannel.this, rpcPacket);
                            } catch (Exception e) {
                                LOG.error("Call RpcChannelListener#onReceiveRpcPacket() failed. Host: `" + host + "`. Socket: `" + socket + "`.", e);
                            }
                        }
                    }
                }
                RPC_CONNECTION_LOG.info("End of the input stream has been reached. Host: `{}`.", host);
                close();
            } catch (SocketException e) {
                close(); //防止跟当前通道相关联的socket在外部关闭，再调用一次close方法
            } catch (IOException e) {
                LOG.error("[ReadTask] RavenRawMessageChannel need to be closed due to: {}. {}", e.getMessage(), socket);
                close();
            } catch (Exception e) {
                LOG.error("[ReadTask] Unexpected error. " + socket, e);
                close();
            }
        }

    }

}
