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

import com.heimuheimu.naivemonitor.monitor.SocketMonitor;
import com.heimuheimu.naiverpc.constant.BeanStatusEnum;
import com.heimuheimu.naiverpc.constant.OperationCode;
import com.heimuheimu.naiverpc.constant.ResponseStatusCode;
import com.heimuheimu.naiverpc.monitor.client.RpcClientSocketMonitorFactory;
import com.heimuheimu.naiverpc.monitor.server.RpcServerSocketMonitorFactory;
import com.heimuheimu.naiverpc.net.BuildSocketException;
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
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * RPC 服务调用方 与 RPC 服务提供方进行数据通信的管道，使用 {@link RpcPacket} 作为数据载体。
 *
 * <p>
 *     {@code RpcChannel} 实例应调用 {@link #init()} 方法，初始化成功后，双方才可进行 RPC 数据通信。
 *     管道是否可以进行数据通信可通过 {@link #isActive()} 方法进行判断。当管道不再使用时，应调用 {@link #close()} 方法进行资源释放。
 * </p>
 *
 * <h3>监听器</h3>
 * <blockquote>
 * 当管道接收到 RPC 数据或管道被关闭时，均会触发 {@link RpcChannelListener} 相应的事件进行通知。
 * </blockquote>
 *
 * <h3>数据监控</h3>
 * <blockquote>
 * 可通过 {@link RpcClientSocketMonitorFactory} 获取 RPC 服务调用方 Socket 通信监控数据。<br>
 * 可通过 {@link RpcServerSocketMonitorFactory} 获取 RPC 服务提供方 Socket 通信监控数据。
 * </blockquote>
 *
 * <h3>连接信息日志 Log4j 配置</h3>
 * <strong>注意：</strong> <code>${log.output.directory}</code> 为占位替换符，请自行定义。
 * <blockquote>
 * <pre>
 * log4j.logger.NAIVERPC_CONNECTION_LOG=INFO, NAIVERPC_CONNECTION_LOG
 * log4j.additivity.NAIVERPC_CONNECTION_LOG=false
 * log4j.appender.NAIVERPC_CONNECTION_LOG=org.apache.log4j.DailyRollingFileAppender
 * log4j.appender.NAIVERPC_CONNECTION_LOG.file=${log.output.directory}/naiverpc/connection.log
 * log4j.appender.NAIVERPC_CONNECTION_LOG.encoding=UTF-8
 * log4j.appender.NAIVERPC_CONNECTION_LOG.DatePattern=_yyyy-MM-dd
 * log4j.appender.NAIVERPC_CONNECTION_LOG.layout=org.apache.log4j.PatternLayout
 * log4j.appender.NAIVERPC_CONNECTION_LOG.layout.ConversionPattern=%d{ISO8601} %-5p : %m%n
 * </pre>
 * </blockquote>
 *
 * <p><strong>说明：</strong>{@code RpcChannel} 类是线程安全的，可在多个线程中使用同一个实例。</p>
 *
 * @author heimuheimu
 */
public class RpcChannel implements Closeable {

    private static final Logger RPC_CONNECTION_LOG = LoggerFactory.getLogger("NAIVERPC_CONNECTION_LOG");

    private static final Logger LOG = LoggerFactory.getLogger(RpcChannel.class);

    private static final String MODE_CLIENT = "Client";

    private static final String MODE_SERVER = "Server";


    /**
     * 等待发送的 RPC 数据队列
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
     * 心跳检测时间，单位：秒，在该周期时间内当前管道如果没有任何数据通信，将会发送一个心跳请求数据包
     */
    private final int heartbeatPeriod;

    /**
     * RPC 数据通信管道事件监听器
     */
    private final RpcChannelListener rpcChannelListener;

    /**
     * RPC 数据通信管道使用的 Socket 信息监控器
     */
    private final SocketMonitor socketMonitor;

    /**
     * 当前 RPC 数据通信管道状态
     */
    private volatile BeanStatusEnum state = BeanStatusEnum.UNINITIALIZED;

    /**
     * 执行初始化、关闭等操作使用的私有锁
     */
    private final Object lock = new Object();

    /**
     * RPC 数据通信管道模式，RPC 服务调用方使用的模式为 client，RPC 服务提供方使用的模式为 server
     */
    private final String mode;

    /**
     * 是否已接收到 RPC 服务提供方的下线请求消息
     */
    private volatile boolean isOffline = false;

    /**
     * RPC 数据包发送线程
     */
    private WriteTask writeTask;

    /**
     * RPC 服务调用方创建一个与 RPC 服务提供方进行数据通信的管道。
     *
     * <p><b>注意：</b>管道必须执行 {@link #init()} 方法，完成初始化后才可正常使用。</p>
     *
     * @param host RPC 服务提供方主机地址，由主机名和端口组成，":"符号分割，例如：localhost:4182
     * @param configuration {@link Socket} 配置信息，如果传 {@code null}，将会使用 {@link SocketConfiguration#DEFAULT} 配置信息
     * @param heartbeatPeriod 心跳检测时间，单位：秒，在该周期时间内当前管道如果没有任何数据通信，将会发送一个心跳请求数据包，如果该值小于等于 0，则不进行检测
     * @param rpcChannelListener {@code RpcChannel} 事件监听器
     * @throws IllegalArgumentException 如果 RPC 服务提供方主机地址不符合规则，将会抛出此异常
     * @throws BuildSocketException 如果创建 {@link Socket} 过程中发生错误，将会抛出此异常
     * @see #init()
     */
    public RpcChannel(String host, SocketConfiguration configuration, int heartbeatPeriod, RpcChannelListener rpcChannelListener)
            throws IllegalArgumentException, BuildSocketException {
        this.host = host;
        this.socket = SocketBuilder.create(host, configuration);
        this.heartbeatPeriod = heartbeatPeriod;
        this.rpcChannelListener = rpcChannelListener;
        this.socketMonitor = RpcClientSocketMonitorFactory.get(host);
        this.mode = MODE_CLIENT;
    }

    /**
     * RPC 服务提供方创建一个与 RPC 服务调用方进行数据通信的管道。
     *
     * <p><b>注意：</b>管道必须执行 {@link #init()} 方法，完成初始化后才可正常使用。</p>
     *
     * @param socket 与 RPC 服务调用方建立的 {@code Socket} 连接，不允许为 {@code null}
     * @param rpcChannelListener RPC 数据通信管道事件监听器，允许为 {@code null}
     * @throws NullPointerException 如果 {@code Socket} 连接为 {@code null}，将会抛出此异常
     * @see #init()
     */
    public RpcChannel(Socket socket, RpcChannelListener rpcChannelListener) throws NullPointerException {
        if (socket == null) {
            LOG.error("[Server] Create RpcChannel failed: `socket could not be null`.");
            throw new NullPointerException("[Server] Create RpcChannel failed: `socket could not be null`.");
        }
        String remoteHostName = "unknown";
        InetAddress remoteInetAddress = socket.getInetAddress();
        if (remoteInetAddress != null) {
            remoteHostName = remoteInetAddress.getCanonicalHostName();
        }
        this.host = remoteHostName + ":" + socket.getPort();
        this.socket = socket;
        this.heartbeatPeriod = -1;
        this.rpcChannelListener = rpcChannelListener;
        this.socketMonitor = RpcServerSocketMonitorFactory.get(socket.getLocalPort(), remoteHostName);
        this.mode = MODE_SERVER;
    }

    /**
     * 执行 {@code RpcChannel} 初始化操作，在初始化完成后，重复执行该方法不会产生任何效果。管道是否可以进行数据通信可通过 {@link #isActive()} 方法进行判断。
     *
     * <p><strong>注意：</strong>该方法不会抛出任何异常，如果初始化失败，管道将被自动关闭，且不会触发 {@link RpcChannelListener#onClosed(RpcChannel)} 事件。</p>
     */
    public void init() {
        synchronized (lock) {
            if (state == BeanStatusEnum.UNINITIALIZED) {
                try {
                    if (socket.isConnected() && !socket.isClosed()) {
                        long startTime = System.currentTimeMillis();
                        state = BeanStatusEnum.NORMAL;
                        SocketConfiguration config = SocketBuilder.getConfig(socket);
                        String socketAddress = host + "/" + socket.getLocalPort();
                        writeTask = new WriteTask(config.getSendBufferSize());
                        writeTask.setName("naiverpc-" + mode + "-channel-write-" + socketAddress);
                        writeTask.start();

                        ReadTask readTask = new ReadTask();
                        readTask.setName("naiverpc-" + mode + "-channel-read-" + socketAddress);
                        readTask.start();
                        RPC_CONNECTION_LOG.info("[{}] RpcChannel has been initialized. Cost: `{}ms`. Host: `{}`. Local port: `{}`. Heartbeat period: `{}`. Config: `{}`.",
                                mode, (System.currentTimeMillis() - startTime), host, socket.getLocalPort(), heartbeatPeriod, config);
                    } else {
                        RPC_CONNECTION_LOG.error("[{}] Initialize RpcChannel failed: `socket is not connected or has been closed`. Host: `{}`.", mode, host);
                        close(false);
                    }
                } catch (Exception e) {
                    RPC_CONNECTION_LOG.error("[{}] Initialize RpcChannel failed: `{}`. Host: `{}`. Heartbeat period: `{}`.",
                            mode, e.getMessage(), host, heartbeatPeriod);
                    LOG.error("[" + mode + "] Initialize RpcChannel failed: `" + e.getMessage() + "`. Host: `" + host + "`. Socket: `" + socket + "`.", e);
                    close(false);
                }
            }
        }
    }

    /**
     * 执行 RPC 数据通信管道关闭操作，进行资源释放，在关闭完成后，重复执行该方法不会产生任何效果。
     *
     * <p><strong>注意：</strong>该方法不会抛出任何异常，{@link RpcChannelListener#onClosed(RpcChannel)} 事件只会被通知一次。</p>
     */
    @Override
    public void close() {
        close(true);
    }

    /**
     * 判断当前 RPC 数据通信管道是否可用。在以下情况，管道将被认为不可用：
     * <ul>
     *     <li>管道没有执行初始化操作，或已执行关闭操作。</li>
     *     <li>RPC 服务调用方管道接收到了 RPC 服务提供方发送的下线操作请求。</li>
     * </ul>
     *
     * @return 当前 RPC 数据通信管道是否可用
     */
    public boolean isActive() {
        return state == BeanStatusEnum.NORMAL && !isOffline;
    }


    /**
     * 发送一个 RPC 数据，数据发送前可通过 {@link #isActive()} 方法判断管道是否可用。
     *
     * @param rpcPacket RPC 数据
     * @throws NullPointerException 如果发送的 RPC 数据为 {@code null}，将抛出此异常
     * @throws IllegalStateException 如果当前管道不可用，将抛出此异常
     * @see #isActive()
     */
    public void send(RpcPacket rpcPacket) throws NullPointerException, IllegalStateException {
        if (rpcPacket == null) {
            LOG.error("[" + mode + "] RpcChannel send RpcPacket failed: `RpcPacket could not be null`. Host: `" + host + "`. Socket: `" + socket + "`.");
            throw new NullPointerException("[" + mode + "] RpcChannel send RpcPacket failed: `RpcPacket could not be null`. Host: `" + host + "`. Socket: `" + socket + "`.");
        }
        if (isActive()) {
            rpcPacketQueue.add(rpcPacket);
        } else {
            LOG.error("[" + mode + "] RpcChannel send RpcPacket failed: `channel is inactive`. State: `" + state +
                    "`. Offline: `" + isOffline + "`. Host: `" + host + "`. Socket: `" + socket + "`.");
            throw new IllegalStateException("[" + mode + "] RpcChannel send RpcPacket failed: `channel is inactive`. State: `" + state +
                    "`. Offline: `" + isOffline + "`. Host: `" + host + "`. Socket: `" + socket + "`.");
        }
    }

    /**
     * RPC 服务提供方给 RPC 服务调用方发送一个下线操作请求，调用方在收到该请求后将不再发送新的 RPC 数据，并在 1 分钟后关闭当前管道。
     *
     * <p><strong>说明：</strong>该方法由 RPC 服务提供方调用，RPC 服务调用方调用此方法不产生任何效果。该方法不会抛出任何异常。</p>
     */
    public void offline() {
        if (mode.equals(MODE_SERVER)) {
            rpcPacketQueue.add(RpcPacketBuilder.buildRequestPacket(0, OperationCode.OFFLINE));
        } else {
            LOG.warn("[" + mode + "] RpcChannel offline failed: `client rpc channel should not invoke #offline() method`. State: `" + state +
                    "`. Offline: `" + isOffline + "`. Host: `" + host + "`. Socket: `" + socket + "`.");
        }
    }

    /**
     * 判断是否已接收到 RPC 服务提供方发送的下线操作请求。
     *
     * <p><strong>说明：</strong>该方法由 RPC 服务调用方调用，RPC 服务提供方调用此方法将永远返回 {@code false}。该方法不会抛出任何异常。</p>
     *
     * @return 是否已接收到 RPC 服务提供方发送的下线操作请求
     */
    public boolean isOffline() {
        return isOffline;
    }

    private void close(boolean triggerOnClosedEvent) {
        synchronized (lock) {
            if (state != BeanStatusEnum.CLOSED) {
                long startTime = System.currentTimeMillis();
                state = BeanStatusEnum.CLOSED;
                try {
                    //关闭 Socket 连接
                    socket.close();
                    //停止 Write 线程
                    writeTask.stopSignal = true;
                    writeTask.interrupt();
                    RPC_CONNECTION_LOG.info("[{}] RpcChannel has been closed. Cost: `{}ms`. Host: `{}`. Heartbeat period: `{}`.",
                            mode, (System.currentTimeMillis() - startTime), host, heartbeatPeriod);
                } catch (Exception e) {
                    RPC_CONNECTION_LOG.error("[{}] Close RpcChannel failed: `{}`. Host: `{}`.", mode, e.getMessage(), host);
                    LOG.error("[" + mode + "] Close RpcChannel failed: `" + e.getMessage() + "`. Host: `" + host + "`. Socket: `" + socket + "`.", e);
                }
                if (triggerOnClosedEvent) {
                    if (rpcChannelListener != null) {
                        try {
                            rpcChannelListener.onClosed(this);
                        } catch (Exception e) {
                            LOG.error("[" + mode + "] Call RpcChannelListener#onClosed() failed. Host: `" + host + "`. Socket: `" + socket + "`.", e);
                        }
                    }
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
                ", mode='" + mode + '\'' +
                ", isOffline=" + isOffline +
                '}';
    }

    /**
     * RPC 数据发送线程
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
                                socketMonitor.onWritten(rpcPacketByteArray.length);
                            } else {
                                addToMergedPacket(rpcPacket);
                            }
                            outputStream.flush();
                        }
                    } else {
                        rpcPacketQueue.add(RpcPacketBuilder.buildRequestPacket(0, OperationCode.HEARTBEAT));
                        LOG.debug("[{}] Send heartbeat request packet success. Host: `{}`.", mode, host);
                    }
                }
            } catch (InterruptedException e) {
                //因当前管道关闭才会抛出此异常，不做任何处理
            } catch (Exception e) {
                RPC_CONNECTION_LOG.error("[{}-WriteTask] RpcChannel need to be closed due to: `{}`. Host: `{}`. Socket: `{}`.", mode, e.getMessage(), host, socket);
                LOG.error("[" + mode + "-WriteTask] RpcChannel need to be closed: `" + e.getMessage() + "`. Host: `" + host
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
            socketMonitor.onWritten(destPos);
            resetMergedPacket();
        }

        private void resetMergedPacket() {
            mergedPacketList.clear();
            mergedPacketSize = 0;
        }

    }

    /**
     * RPC 数据读取线程
     */
    private class ReadTask extends Thread {

        private final RpcPacketReader reader;

        private ReadTask () throws IOException {
            this.reader = new RpcPacketReader(socketMonitor, socket.getInputStream());
        }

        @Override
        public void run() {
            try {
                RpcPacket rpcPacket;
                while ((rpcPacket = reader.read()) != null) {
                    if (rpcPacket.getOpcode() == OperationCode.HEARTBEAT) {
                        if (rpcPacket.isRequestPacket()) {
                            rpcPacketQueue.add(RpcPacketBuilder.buildResponsePacket(rpcPacket, ResponseStatusCode.SUCCESS));
                            LOG.debug("[{}] Send heartbeat response packet success. Host: `{}`.", mode, host);
                        } else {
                            LOG.debug("[{}] Receive heartbeat response packet success. Host: `{}`.", mode, host);
                        }
                    } else if (rpcPacket.getOpcode() == OperationCode.OFFLINE) {
                        if (rpcPacket.isRequestPacket()) {
                            isOffline = true;
                            new Thread() {

                                @Override
                                public void run() {
                                    RPC_CONNECTION_LOG.info("[{}] RpcChannel receive offline packet, channel will be closed after 1 minute. {}", mode, RpcChannel.this);
                                    try {
                                        Thread.sleep(1000 * 60);
                                    } catch (InterruptedException ignored) {} //ignore exception
                                    close();
                                }

                            }.start();
                            rpcPacketQueue.add(RpcPacketBuilder.buildResponsePacket(rpcPacket, ResponseStatusCode.SUCCESS));
                            LOG.debug("[{}] Send offline response packet success. Host: `{}`.", mode, host);
                        } else {
                            LOG.debug("[{}] Receive offline response packet success. Host: `{}`.", mode, host);
                        }
                    } else {
                        if (rpcChannelListener != null) {
                            try {
                                rpcChannelListener.onReceiveRpcPacket(RpcChannel.this, rpcPacket);
                            } catch (Exception e) {
                                LOG.error("[" + mode + "] Call RpcChannelListener#onReceiveRpcPacket() failed. Host: `" + host + "`. Socket: `" + socket + "`.", e);
                            }
                        }
                    }
                }
                RPC_CONNECTION_LOG.info("[{}] End of the input stream has been reached, channel will be closed. Host: `{}`.", mode, host);
                close();
            } catch (SocketException e) {
                close(); //防止跟当前管道相关联的socket在外部关闭，再调用一次close方法
            } catch (Exception e) {
                RPC_CONNECTION_LOG.error("[{}-ReadTask] RpcChannel need to be closed due to: `{}`. Host: `{}`. Socket: `{}`", mode, e.getMessage(), host, socket);
                LOG.error("[" + mode + "-ReadTask] RpcChannel need to be closed due to: `" + e.getMessage() + "`. Host: `" + host
                        + "`. Socket: `" + socket + "`.", e);
                close();
            }
        }

    }

}
