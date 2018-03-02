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

package com.heimuheimu.naiverpc.client.cluster;

import com.heimuheimu.naivemonitor.alarm.NaiveServiceAlarm;
import com.heimuheimu.naivemonitor.alarm.ServiceAlarmMessageNotifier;
import com.heimuheimu.naivemonitor.alarm.ServiceContext;
import com.heimuheimu.naivemonitor.util.MonitorUtil;
import com.heimuheimu.naiverpc.client.DirectRpcClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code NoticeableRpcClusterClientListener} 监听集群客户端 {@link RpcClusterClient} 中 {@link DirectRpcClient} 的关闭和恢复事件，
 * 可在上述事件发生时，通过报警消息通知器进行实时通知。
 *
 * <br><strong>注意：</strong>接收到 RPC 服务提供方发送的下线操作请求导致的关闭不会进行通知。
 *
 * <p><strong>说明：</strong>{@code NoticeableRpcClusterClientListener} 类是线程安全的，可在多个线程中使用同一个实例。</p>
 *
 * @author heimuheimu
 * @see NaiveServiceAlarm
 */
public class NoticeableRpcClusterClientListener extends RpcClusterClientListenerSkeleton {

    /**
     * 使用 {@code RpcClusterClient} 的项目名称
     */
    private final String project;

    /**
     * 使用 {@code RpcClusterClient} 的主机名称
     */
    private final String host;

    /**
     * 调用的 RPC 服务名称，方便在同一项目调用多个 RPC 服务时进行区分
     */
    private final String serviceName;

    /**
     * 服务不可用报警器
     */
    private final NaiveServiceAlarm naiveServiceAlarm;

    /**
     * 已发送下线操作请求的 RPC 服务提供方主机地址 {@code Map}
     */
    private final ConcurrentHashMap<String, Integer> offlineHostMap = new ConcurrentHashMap<>();

    /**
     * 构造一个对 {@link DirectRpcClient} 的关闭和恢复事件进行实时通知的 {@link RpcClusterClient} 事件监听器。
     *
     * @param project 使用 {@code RpcClusterClient} 的项目名称
     * @param notifierList 报警消息通知器列表，不允许 {@code null} 或空
     * @throws IllegalArgumentException 如果报警消息通知器列表为 {@code null} 或空时，抛出此异常
     */
    public NoticeableRpcClusterClientListener(String project, List<ServiceAlarmMessageNotifier> notifierList)
            throws IllegalArgumentException {
        this (project, null, notifierList, null);
    }

    /**
     * 构造一个对 {@link DirectRpcClient} 的关闭和恢复事件进行实时通知的 {@link RpcClusterClient} 事件监听器。
     *
     * @param project 使用 {@code RpcClusterClient} 的项目名称
     * @param serviceName 调用的 RPC 服务名称，方便在同一项目调用多个 RPC 服务时进行区分，允许为 {@code null} 或者空字符串
     * @param notifierList 报警消息通知器列表，不允许 {@code null} 或空
     * @throws IllegalArgumentException 如果报警消息通知器列表为 {@code null} 或空时，抛出此异常
     */
    public NoticeableRpcClusterClientListener(String project, String serviceName,
            List<ServiceAlarmMessageNotifier> notifierList) throws IllegalArgumentException {
        this (project, serviceName, notifierList, null);
    }

    /**
     * 构造一个对 {@link DirectRpcClient} 的关闭和恢复事件进行实时通知的 {@link RpcClusterClient} 事件监听器。
     *
     * @param project 使用 {@code RpcClusterClient} 的项目名称
     * @param serviceName 调用的 RPC 服务名称，方便在同一项目调用多个 RPC 服务时进行区分，允许为 {@code null} 或者空字符串
     * @param notifierList 报警消息通知器列表，不允许 {@code null} 或空
     * @param hostAliasMap 别名 Map，Key 为机器名， Value 为别名，允许为 {@code null}
     * @throws IllegalArgumentException 如果报警消息通知器列表为 {@code null} 或空时，抛出此异常
     */
    public NoticeableRpcClusterClientListener(String project, String serviceName,
            List<ServiceAlarmMessageNotifier> notifierList, Map<String, String> hostAliasMap) throws IllegalArgumentException {
        this.project = project;
        this.serviceName = serviceName;
        this.naiveServiceAlarm = new NaiveServiceAlarm(notifierList);
        String host = MonitorUtil.getLocalHostName();
        if (hostAliasMap != null && hostAliasMap.containsKey(host)) {
            this.host = hostAliasMap.get(host);
        } else {
            this.host = host;
        }
    }

    @Override
    public void onRecovered(String host) {
        if (offlineHostMap.remove(host) == null) {
            naiveServiceAlarm.onRecovered(getServiceContext(host));
        }
    }

    @Override
    public void onClosed(String host, boolean isOffline) {
        if (isOffline) {
            offlineHostMap.put(host, 1);
        } else {
            naiveServiceAlarm.onCrashed(getServiceContext(host));
        }
    }

    /**
     * 根据提供 RPC 服务的主机地址构造一个服务及服务所在的运行环境信息。
     *
     * @param rpcServerHost 提供 RPC 服务的主机地址
     * @return 服务及服务所在的运行环境信息
     */
    protected ServiceContext getServiceContext(String rpcServerHost) {
        ServiceContext serviceContext = new ServiceContext();
        if (serviceName != null && !serviceName.isEmpty()) {
            serviceContext.setName("[RpcClusterClient] " + serviceName);
        } else {
            serviceContext.setName("RpcClusterClient");
        }
        serviceContext.setHost(host);
        serviceContext.setProject(project);
        serviceContext.setRemoteHost(rpcServerHost);
        return serviceContext;
    }

    @Override
    public String toString() {
        return "NoticeableRpcClusterClientListener{" +
                "project='" + project + '\'' +
                ", host='" + host + '\'' +
                ", serviceName='" + serviceName + '\'' +
                '}';
    }
}
