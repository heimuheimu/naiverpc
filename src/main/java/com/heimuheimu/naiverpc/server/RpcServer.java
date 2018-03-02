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

package com.heimuheimu.naiverpc.server;

import com.heimuheimu.naiverpc.channel.RpcChannel;
import com.heimuheimu.naiverpc.channel.RpcChannelListener;
import com.heimuheimu.naiverpc.channel.RpcChannelListenerSkeleton;
import com.heimuheimu.naiverpc.constant.BeanStatusEnum;
import com.heimuheimu.naiverpc.constant.OperationCode;
import com.heimuheimu.naiverpc.net.SocketBuilder;
import com.heimuheimu.naiverpc.net.SocketConfiguration;
import com.heimuheimu.naiverpc.packet.RpcPacket;
import com.heimuheimu.naiverpc.server.executors.AsyncJdkRpcExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * RPC 服务提供方通过 {@code RpcServer} 对外提供 RPC 服务，提供的 RPC 服务需要调用 {@link #register(Object)} 方法完成注册。
 *
 * <p>
 *     {@code RpcServer} 实例应调用 {@link #init()} 方法，初始化成功后，才可对外提供 RPC 服务。
 *     <br>当 {@code RpcServer} 需要关闭时，应先调用 {@link #offline()} 执行下线操作，防止正在执行中的 RPC 调用失败，
 *     等待一段时间后，再调用 {@link #close()} 方法进行资源释放。
 * </p>
 *
 * <p><strong>说明：</strong>{@code RpcServer} 类是线程安全的，可在多个线程中使用同一个实例。</p>
 *
 * @author heimuheimu
 * @see AsyncJdkRpcExecutor
 */
public class RpcServer implements Closeable {

    private static final Logger RPC_CONNECTION_LOG = LoggerFactory.getLogger("NAIVERPC_CONNECTION_LOG");

    private static final Logger LOG = LoggerFactory.getLogger(RpcServer.class);

    /**
     * 已经与 RPC 调用方建立的 {@code RpcChannel} 列表
     */
    private final CopyOnWriteArrayList<RpcChannel> activeRpcChannelList = new CopyOnWriteArrayList<>();

    /**
     * {@code RpcServer} 使用的 {@code RpcChannel} 事件监听器
     */
    private final RpcChannelListener rpcChannelListener = new RpcChannelListenerImpl();

    /**
     * {@code RpcServer} 所处状态
     */
    private volatile BeanStatusEnum state = BeanStatusEnum.UNINITIALIZED;

    /**
     * {@code RpcServer} 开启的 {@code Socket} 监听端口
     */
    private final int port;

    /**
     * 创建 {@code RpcChannel} 使用的 {@code Socket} 配置信息
     */
    private final SocketConfiguration socketConfiguration;

    /**
     * RPC 调用方请求的 RPC 方法执行器
     */
    private final RpcExecutor rpcExecutor;

    /**
     * {@code RpcServer} 后台线程，用于监听 RPC 调用方发起的建立连接请求
     */
    private RpcServerTask rpcServerTask;

    /**
     * 构造一个 {@code RpcServer} 对外提供 RPC 服务，{@code Socket} 监听端口为 4182，{@link Socket} 配置信息使用 {@link SocketConfiguration#DEFAULT}，
     * 创建 {@code AsyncJdkRpcExecutor} 时，最小压缩字节数设置为 64 KB，RPC 执行过慢最小时间设置为 50 毫秒，使用的线程池最大数量为 500。
     *
     * @param rpcExecutorListener {@link RpcExecutor} 事件监听器，允许为 {@code null}
     * @see AsyncJdkRpcExecutor
     */
    public RpcServer(RpcExecutorListener rpcExecutorListener) {
        this(4182, rpcExecutorListener);
    }

    /**
     * 构造一个 {@code RpcServer} 对外提供 RPC 服务，{@link Socket} 配置信息使用 {@link SocketConfiguration#DEFAULT}，
     * 创建 {@code AsyncJdkRpcExecutor} 时，最小压缩字节数设置为 64 KB，RPC 执行过慢最小时间设置为 50 毫秒，使用的线程池最大数量为 500。
     *
     * @param port {@code RpcServer} 开启的 {@code Socket} 监听端口，不能小于等于 0
     * @param rpcExecutorListener 创建 {@code AsyncJdkRpcExecutor} 使用的 {@link RpcExecutor} 事件监听器，允许为 {@code null}
     * @throws IllegalArgumentException 如果 {@code RpcServer} 开启的 {@code Socket} 监听端口小于等于 0，将会抛出此异常
     */
    public RpcServer(int port, RpcExecutorListener rpcExecutorListener) throws IllegalArgumentException {
        this(port, null, 64 * 1024, 50, rpcExecutorListener, 500);
    }

    /**
     * 构造一个 {@code RpcServer} 对外提供 RPC 服务。
     *
     * @param port {@code RpcServer} 开启的 {@code Socket} 监听端口，不能小于等于 0
     * @param socketConfiguration 创建 {@code RpcChannel} 使用的 {@link Socket} 配置信息，允许为 {@code null}
     * @param compressionThreshold 创建 {@code AsyncJdkRpcExecutor} 使用的最小压缩字节数，不能小于等于 0
     * @param slowExecutionThreshold 创建 {@code AsyncJdkRpcExecutor} 使用的 RPC 执行过慢最小时间，单位：毫秒，不能小于等于 0
     * @param rpcExecutorListener 创建 {@code AsyncJdkRpcExecutor} 使用的 {@link RpcExecutor} 事件监听器，允许为 {@code null}
     * @param maximumPoolSize 创建 {@code AsyncJdkRpcExecutor} 使用的线程池最大数量，不能小于等于 0
     * @throws IllegalArgumentException 如果 {@code RpcServer} 开启的 {@code Socket} 监听端口小于等于 0，将会抛出此异常
     * @throws IllegalArgumentException 如果创建 {@code AsyncJdkRpcExecutor} 使用的最小压缩字节数小于等于 0，将会抛出此异常
     * @throws IllegalArgumentException 如果创建 {@code AsyncJdkRpcExecutor} 使用的 RPC 执行过慢最小时间小于等于 0，将会抛出此异常
     * @throws IllegalArgumentException 如果创建 {@code AsyncJdkRpcExecutor} 使用的线程池最大数量小于等于 0，将会抛出此异常
     * @see AsyncJdkRpcExecutor
     */
    public RpcServer(int port, SocketConfiguration socketConfiguration, int compressionThreshold, int slowExecutionThreshold,
                     RpcExecutorListener rpcExecutorListener, int maximumPoolSize) throws IllegalArgumentException {
        if (port <= 0) {
            LOG.error("Create RpcServer failed: `port could not be equal or less than 0`. Port: `" + port
                    + "`. SocketConfiguration: `" + socketConfiguration + "`. CompressionThreshold: `"
                    + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold + "`. RpcExecutorListener: `"
                    + rpcExecutorListener + "`. MaximumPoolSize: `" + maximumPoolSize + "`.");
            throw new IllegalArgumentException("Create RpcServer failed: `port could not be equal or less than 0`. Port: `" + port
                    + "`. SocketConfiguration: `" + socketConfiguration + "`. CompressionThreshold: `"
                    + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold + "`. RpcExecutorListener: `"
                    + rpcExecutorListener + "`. MaximumPoolSize: `" + maximumPoolSize + "`.");
        }
        if (compressionThreshold <= 0) {
            LOG.error("Create RpcServer failed: `compressionThreshold could not be equal or less than 0`. Port: `" + port
                    + "`. SocketConfiguration: `" + socketConfiguration + "`. CompressionThreshold: `"
                    + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold + "`. RpcExecutorListener: `"
                    + rpcExecutorListener + "`. MaximumPoolSize: `" + maximumPoolSize + "`.");
            throw new IllegalArgumentException("Create RpcServer failed: `compressionThreshold could not be equal or less than 0`. Port: `" + port
                    + "`. SocketConfiguration: `" + socketConfiguration + "`. CompressionThreshold: `"
                    + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold + "`. RpcExecutorListener: `"
                    + rpcExecutorListener + "`. MaximumPoolSize: `" + maximumPoolSize + "`.");
        }
        if (slowExecutionThreshold <= 0) {
            LOG.error("Create RpcServer failed: `slowExecutionThreshold could not be equal or less than 0`. Port: `" + port
                    + "`. SocketConfiguration: `" + socketConfiguration + "`. CompressionThreshold: `"
                    + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold + "`. RpcExecutorListener: `"
                    + rpcExecutorListener + "`. MaximumPoolSize: `" + maximumPoolSize + "`.");
            throw new IllegalArgumentException("Create RpcServer failed: `slowExecutionThreshold could not be equal or less than 0`. Port: `" + port
                    + "`. SocketConfiguration: `" + socketConfiguration + "`. CompressionThreshold: `"
                    + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold + "`. RpcExecutorListener: `"
                    + rpcExecutorListener + "`. MaximumPoolSize: `" + maximumPoolSize + "`.");
        }
        if (maximumPoolSize <= 0) {
            LOG.error("Create RpcServer failed: `maximumPoolSize could not be equal or less than 0`. Port: `" + port
                    + "`. SocketConfiguration: `" + socketConfiguration + "`. CompressionThreshold: `"
                    + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold + "`. RpcExecutorListener: `"
                    + rpcExecutorListener + "`. MaximumPoolSize: `" + maximumPoolSize + "`.");
            throw new IllegalArgumentException("Create RpcServer failed: `maximumPoolSize could not be equal or less than 0`. Port: `" + port
                    + "`. SocketConfiguration: `" + socketConfiguration + "`. CompressionThreshold: `"
                    + compressionThreshold + "`. SlowExecutionThreshold: `" + slowExecutionThreshold + "`. RpcExecutorListener: `"
                    + rpcExecutorListener + "`. MaximumPoolSize: `" + maximumPoolSize + "`.");
        }
        this.port = port;
        this.socketConfiguration = socketConfiguration;
        this.rpcExecutor = new AsyncJdkRpcExecutor(port, compressionThreshold, slowExecutionThreshold, rpcExecutorListener, maximumPoolSize);
    }

    /**
     * 在 {@code RpcServer} 注册一个 RPC 服务，服务注册完成后，才可对外提供服务。
     * <p><strong>注意：</strong> RPC 服务以接口的形式提供给调用方使用，注册的 RPC 服务必须继承至少 1 个接口。</p>
     *
     * @param service 需要对外提供的 RPC 服务
     * @throws IllegalArgumentException 如果注册的 RPC 服务未继承任何接口，将会抛出此异常
     */
    public void register(Object service) throws IllegalArgumentException {
        rpcExecutor.register(service);
    }

    /**
     * {@code RpcServer} 执行下线操作，防止正在执行中的 RPC 调用失败，在下线完成一段时间后，再调用 {@link #close()} 方法进行资源释放，该方法不会抛出任何异常。
     */
    public synchronized void offline() {
        long startTime = System.currentTimeMillis();
        if (state == BeanStatusEnum.NORMAL) {
            if (rpcServerTask != null) {
                try {
                    rpcServerTask.close();
                    ArrayList<RpcChannel> copyActiveChannelList = new ArrayList<>(activeRpcChannelList);
                    for (RpcChannel channel : copyActiveChannelList) {
                        channel.offline();
                    }
                    RPC_CONNECTION_LOG.info("RpcServer has been offline. Cost: `{}ms`. Port: `{}`. SocketConfiguration: `{}`.",
                            (System.currentTimeMillis() - startTime), port, socketConfiguration);
                } catch (Exception e) {
                    LOG.error("Offline RpcServer failed: `unexpected error`. Port: `" + port + "`.", e);
                }
            }
        }
    }

    /**
     * 执行 {@code RpcServer} 初始化操作，在初始化完成后，重复执行该方法不会产生任何效果。
     */
    public synchronized void init() {
        if (state == BeanStatusEnum.UNINITIALIZED) {
            long startTime = System.currentTimeMillis();
            state = BeanStatusEnum.NORMAL;
            try {
                rpcServerTask = new RpcServerTask();
                rpcServerTask.setName("naiverpc-server-" + port);
                rpcServerTask.start();
                RPC_CONNECTION_LOG.info("RpcServer has been initialized. Cost: `{}ms`. Port: `{}`. SocketConfiguration: `{}`.",
                        (System.currentTimeMillis() - startTime), port, socketConfiguration);
            } catch (Exception e) {
                LOG.error("Initialize RpcServer failed: `unexpected error`. Port: `" + port + "`.", e);
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
                if (rpcServerTask != null) {
                    rpcServerTask.close();
                }
                for (RpcChannel rpcChannel : activeRpcChannelList) {
                    rpcChannel.close();
                }
                RPC_CONNECTION_LOG.info("RpcServer has been closed. Cost: `{}ms`. Port: `{}`. SocketConfiguration: `{}`.",
                        (System.currentTimeMillis() - startTime), port, socketConfiguration);
            } catch (Exception e) {
                LOG.error("Close RpcServer failed: `unexpected error`. Port: `" + port + "`.", e);
            }
        }
    }

    @Override
    public String toString() {
        return "RpcServer{" +
                "state=" + state +
                ", port=" + port +
                ", socketConfiguration=" + socketConfiguration +
                ", rpcExecutor=" + rpcExecutor +
                '}';
    }

    private class RpcServerTask extends Thread {

        private volatile boolean stopSignal = false;

        private final ServerSocket serverSocket;

        private RpcServerTask() throws IOException {
            serverSocket = new ServerSocket(port);
        }

        @Override
        public void run() {
            while (!stopSignal) {
                try {
                    Socket socket = serverSocket.accept();
                    SocketBuilder.setConfig(socket, socketConfiguration);
                    RpcChannel rpcChannel = new RpcChannel(socket, rpcChannelListener);
                    rpcChannel.init();
                    if (rpcChannel.isActive()) {
                        activeRpcChannelList.add(rpcChannel);
                    }
                } catch (SocketException e) {
                    //do nothing
                } catch (Exception e) { //should not happen
                    LOG.error("Accept RpcChannel failed. Port: `" + port + "`.", e);
                }
            }
        }

        private void close() {
            this.stopSignal = true;
            try {
                serverSocket.close();
            } catch (Exception e) {
                LOG.error("Close ServerSocket failed. Port: `" + port + "`.", e);
            }
        }

    }

    private class RpcChannelListenerImpl extends RpcChannelListenerSkeleton {

        @Override
        public void onReceiveRpcPacket(RpcChannel channel, RpcPacket packet) {
            if (packet.isRequestPacket() && packet.getOpcode() == OperationCode.REMOTE_PROCEDURE_CALL) {
                rpcExecutor.execute(channel, packet);
            } else { //should not happen
                LOG.error("Unrecognized rpc packet. Port: `{}`. Invalid packet: `{}`.", port, packet);
            }
        }

        @Override
        public void onClosed(RpcChannel channel) {
            activeRpcChannelList.remove(channel);
        }

    }

}
