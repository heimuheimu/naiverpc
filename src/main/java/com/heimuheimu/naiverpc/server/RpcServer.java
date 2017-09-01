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
 * RPC 服务提供者，通过指定的监听端口与 RPC 服务调用客户端建立连接，为其提供 RPC 服务
 *
 * @author heimuheimu
 * @ThreadSafe
 */
public class RpcServer implements Closeable {

    private static final Logger RPC_CONNECTION_LOG = LoggerFactory.getLogger("NAIVERPC_CONNECTION_LOG");

    private static final Logger LOG = LoggerFactory.getLogger(RpcServer.class);

    /**
     * 已连接的 RPC 服务调用客户端数据交互管道列表
     */
    private final CopyOnWriteArrayList<RpcChannel> activeRpcChannelList = new CopyOnWriteArrayList<>();

    /**
     * RPC 数据包交互管道事件监听器
     */
    private final RpcChannelListener rpcChannelListener = new RpcChannelListenerImpl();

    /**
     * 当前实例所处状态
     */
    private volatile BeanStatusEnum state = BeanStatusEnum.UNINITIALIZED;

    /**
     * 监听端口
     */
    private final int port;

    /**
     * 与 RPC 服务调用客户端建立的数据交互管道所使用的 Socket 配置信息
     */
    private final SocketConfiguration socketConfiguration;

    /**
     * 远程调用执行器
     */
    private final RpcExecutor rpcExecutor;

    private RpcServerTask rpcServerTask;

    /**
     * 构造一个 RPC 服务提供者，监听端口为 4182，用于执行 RPC 服务调用请求的线程池最大数量为 200， 最小压缩字节数为 64 KB
     */
    public RpcServer() {
        this(4182, null, 200, 64 * 1024, null);
    }

    /**
     * 构造一个 RPC 服务提供者
     *
     * @param port 监听端口
     * @param socketConfiguration Socket 配置信息，允许为 {@code null}，如果传 {@code null}，将会使用 {@link SocketConfiguration#DEFAULT} 配置信息
     * @param maximumPoolSize 用于执行 RPC 服务调用请求的线程池最大数量
     * @param compressionThreshold 最小压缩字节数，当 数据包 body 字节数小于或等于该值，不进行压缩，不能小于等于0
     * @param rpcExecuteListener RPC 服务执行监听器
     */
    public RpcServer(int port, SocketConfiguration socketConfiguration, int maximumPoolSize, int compressionThreshold,
                     RpcExecuteListener rpcExecuteListener) {
        this.port = port;
        this.socketConfiguration = socketConfiguration;
        this.rpcExecutor = new AsyncJdkRpcExecutor(port, maximumPoolSize, compressionThreshold, rpcExecuteListener);
    }

    /**
     * 注册一个 RPC 服务实例，注册完成后，该实例所实现的接口方法就可以通过 RPC 的形式提供给 RPC 调用方调用
     * <p>注意：注册的实例必须继承 1 个或 1 个以上个数的接口，RPC 服务通常以接口的方式提供给 RPC 调用方调用</p>
     *
     * @param service 注册的 RPC 服务实例
     * @throws IllegalArgumentException 如果注册的 RPC 服务实例未继承任何接口
     */
    public void register(Object service) throws IllegalArgumentException {
        rpcExecutor.register(service);
    }

    /**
     * 执行下线操作，当前 RPC 服务提供者不再接受新的 RPC 客户端连接请求，并给已建立的客户端发送离线消息，
     * 收到离线消息的客户端将不再发送新的消息请求，并在 1 分钟后关闭。
     * <p>该方法通常在应用关闭前调用，正在执行中的请求不会因为 RPC 客户端突然关闭导致失败</p>
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
                    LOG.error("Offline RpcServer failed. Unexpected error. Port: `" + port + "`.", e);
                }
            }
        }
    }

    /**
     * 执行 RPC 服务提供者初始化操作，仅在初始化完成后，才能提供服务
     */
    public synchronized void init() {
        if (state == BeanStatusEnum.UNINITIALIZED) {
            long startTime = System.currentTimeMillis();
            state = BeanStatusEnum.NORMAL;
            try {
                rpcServerTask = new RpcServerTask();
                rpcServerTask.setName("[RpcServerTask]:" + port);
                rpcServerTask.start();
                RPC_CONNECTION_LOG.info("RpcServer has been initialized. Cost: `{}ms`. Port: `{}`. SocketConfiguration: `{}`.",
                        (System.currentTimeMillis() - startTime), port, socketConfiguration);
            } catch (Exception e) {
                LOG.error("Initialize RpcServer failed. Unexpected error. Port: `" + port + "`.", e);
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
                LOG.error("Close RpcServer failed. Unexpected error. Port: `" + port + "`.", e);
            }
        }
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
                    RpcChannel rpcChannel = new RpcChannel(socket, -1, rpcChannelListener);
                    rpcChannel.init();
                    if (rpcChannel.isActive()) {
                        activeRpcChannelList.add(rpcChannel);
                    }
                } catch (SocketException e) {
                    //do nothing
                } catch (Exception e) {
                    LOG.error("Accept RpcChannel failed. Port: `" + port + "`.", e);
                }
            }
        }

        private void close() throws IOException {
            this.stopSignal = true;
            serverSocket.close();
        }

    }

    private class RpcChannelListenerImpl extends RpcChannelListenerSkeleton {

        @Override
        public void onReceiveRpcPacket(RpcChannel channel, RpcPacket packet) {
            if (packet.isRequestPacket() && packet.getOpcode() == OperationCode.REMOTE_PROCEDURE_CALL) {
                rpcExecutor.execute(channel, packet);
            } else {
                LOG.error("Unrecognized rpc packet. Port: `{}`. Invalid packet: `{}`.", port, packet);
            }
        }

        @Override
        public void onClosed(RpcChannel channel) {
            activeRpcChannelList.remove(channel);
        }

    }

}
