/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 heimuheimu
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

package com.heimuheimu.naiverpc.facility.clients;

import com.heimuheimu.naivemonitor.alarm.NaiveServiceAlarm;
import com.heimuheimu.naivemonitor.alarm.ServiceAlarmMessageNotifier;
import com.heimuheimu.naivemonitor.alarm.ServiceContext;
import com.heimuheimu.naivemonitor.util.MonitorUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 该监听器可用于当 RPC 直连客户端列表发生 RPC 服务不可用或者从不可用状态恢复时，进行实时通知。
 *
 * <p><strong>说明：</strong>如果 RPC 直连客户端已经下线，则不会进行报警通知。
 *
 * <p><strong>说明：</strong> {@code NoticeableDirectRpcClientListListener} 类是线程安全的，可在多个线程中使用同一个实例。</p>
 *
 * @author heimuheimu
 * @see NaiveServiceAlarm
 */
public class NoticeableDirectRpcClientListListener implements DirectRpcClientListListener {

    /**
     * 使用 RPC 服务的项目名称
     */
    private final String project;

    /**
     * 使用 RPC 服务的主机名称
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
     * 已发送下线操作请求的 RPC 服务提供方主机地址 Map，Key 为主机名称和列表名称组合字符串，Value 固定为 1
     */
    private final ConcurrentHashMap<String, Integer> offlineHostMap = new ConcurrentHashMap<>();

    /**
     * 构造一个 RPC 直连客户端列表事件监听器，可在 RPC 服务不可用或者从不可用状态恢复时，进行实时通知。
     *
     * @param project 使用 RPC 服务的项目名称
     * @param serviceName 调用的 RPC 服务名称
     * @param notifierList 服务不可用或从不可用状态恢复的报警消息通知器列表，不允许 {@code null} 或空
     * @throws IllegalArgumentException 如果报警消息通知器列表为 {@code null} 或空时，抛出此异常
     */
    public NoticeableDirectRpcClientListListener(String project, String serviceName,
                                                 List<ServiceAlarmMessageNotifier> notifierList) throws IllegalArgumentException {
        this(project, serviceName, notifierList, null);
    }

    /**
     * 构造一个 RPC 直连客户端列表事件监听器，可在 RPC 服务不可用或者从不可用状态恢复时，进行实时通知。
     *
     * @param project 使用 RPC 服务的项目名称
     * @param serviceName 调用的 RPC 服务名称
     * @param notifierList 服务不可用或从不可用状态恢复的报警消息通知器列表，不允许 {@code null} 或空
     * @param hostAliasMap 别名 Map，Key 为机器名， Value 为别名，允许为 {@code null}
     * @throws IllegalArgumentException 如果消息通知器列表为 {@code null} 或空时，抛出此异常
     */
    public NoticeableDirectRpcClientListListener(String project, String serviceName, List<ServiceAlarmMessageNotifier> notifierList,
                                                 Map<String, String> hostAliasMap) throws IllegalArgumentException {
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
    public void onCreated(String listName, String host) {
        //do nothing
    }

    @Override
    public void onRecovered(String listName, String host) {
        if (offlineHostMap.remove(host + "_" + listName) == null) {
            naiveServiceAlarm.onRecovered(getServiceContext(listName, host));
        }
    }

    @Override
    public void onClosed(String listName, String host, boolean isOffline) {
        if (isOffline) {
            offlineHostMap.put(host + "_" + listName, 1);
        } else {
            naiveServiceAlarm.onCrashed(getServiceContext(listName, host));
        }
    }

    @Override
    public String toString() {
        return "NoticeableDirectRpcClientListListener{" +
                "project='" + project + '\'' +
                ", host='" + host + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", naiveServiceAlarm=" + naiveServiceAlarm +
                '}';
    }

    protected ServiceContext getServiceContext(String listName, String rpcServerHost) {
        ServiceContext serviceContext = new ServiceContext();
        serviceContext.setName("[" + listName +"] " + serviceName);
        serviceContext.setHost(host);
        serviceContext.setProject(project);
        serviceContext.setRemoteHost(rpcServerHost);
        return serviceContext;
    }
}
