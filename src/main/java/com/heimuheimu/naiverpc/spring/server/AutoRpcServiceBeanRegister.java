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

package com.heimuheimu.naiverpc.spring.server;

import com.heimuheimu.naiverpc.exception.RpcException;
import com.heimuheimu.naiverpc.server.RpcServer;
import com.heimuheimu.naiverpc.spring.InterfaceFinder;
import com.heimuheimu.naiverpc.util.LogBuildUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * RPC 服务自动注册器。自动扫描指定包下的所有和接口名字正则匹配的接口，并在 Spring 工厂中寻找对应的实现类，将其注册为 RPC 服务实例。
 *
 * @author heimuheimu
 */
public class AutoRpcServiceBeanRegister implements ApplicationContextAware, InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(AutoRpcServiceBeanRegister.class);

    private ApplicationContext applicationContext;

    /**
     * RPC 服务提供方，通过指定的监听端口与 RPC 服务调用客户端建立连接，为其提供 RPC 服务
     */
    private final RpcServer rpcServer;

    /**
     * RPC 服务接口包名数组，该包下的所有和接口名字正则匹配规则匹配的接口（包含子包）在 Spring 工厂中的实现类，均会被注册成 RPC 服务
     * <p>如果该值为空，则不扫描任何包
     *
     * @see #classNameRegex
     */
    private final String[] basePackages;

    /**
     * 接口名字正则匹配规则，如果为空，则匹配所有名字
     *
     * @see #basePackages
     */
    private final String classNameRegex;

    /**
     * 在 RPC 服务全部注册完成后，是否执行 {@link RpcServer#init()} 方法
     */
    private final boolean initRpcServer;

    /**
     * 构造一个 RPC 服务自动注册器，并在服务全部注册完成后，自动执行 {@link RpcServer#init()} 方法。
     *
     * @param rpcServer RPC 服务提供方
     * @param basePackages RPC 服务接口包名数组
     * @param classNameRegex 接口名字正则匹配规则
     */
    public AutoRpcServiceBeanRegister(RpcServer rpcServer, String[] basePackages, String classNameRegex) {
        this(rpcServer, basePackages, classNameRegex, true);
    }

    /**
     * 构造一个 RPC 服务自动注册器。
     *
     * @param rpcServer RPC 服务提供方
     * @param basePackages RPC 服务接口包名数组
     * @param classNameRegex 接口名字正则匹配规则
     * @param initRpcServer 在 RPC 服务全部注册完成后，是否执行 {@link RpcServer#init()} 方法
     */
    public AutoRpcServiceBeanRegister(RpcServer rpcServer, String[] basePackages, String classNameRegex, boolean initRpcServer) {
        this.rpcServer = rpcServer;
        this.basePackages = basePackages;
        this.classNameRegex = classNameRegex;
        this.initRpcServer = initRpcServer;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        InterfaceFinder interfaceFinder = new InterfaceFinder();
        List<Class<?>> candidates = new ArrayList<>();
        for (String basePackage : basePackages) {
            candidates.addAll(interfaceFinder.find(basePackage, classNameRegex));
        }
        LOG.info("Base package: `{}`. Class name regex: `{}`. Candidates: `{}`.", basePackages, classNameRegex, candidates); // lgtm [java/print-array]
        int registeredServiceCount = 0;
        if (!candidates.isEmpty()) {
            for (Class<?> clz : candidates) {
                try {
                    Object rpcServiceBean = applicationContext.getBean(clz);
                    rpcServer.register(rpcServiceBean);
                    LOG.info("`{}` has been registered. Interface: `{}`", rpcServiceBean.getClass().getName(),
                            clz.getName());
                    registeredServiceCount ++;
                } catch(Exception e) {
                    LOG.error("`" + clz + "` register failed.", e);
                }
            }
        }

        if (registeredServiceCount == 0) {
            LinkedHashMap<String, Object> params = new LinkedHashMap<>();
            params.put("basePackages", basePackages);
            params.put("classNameRegex", classNameRegex);
            params.put("initRpcServer", initRpcServer);
            String errorMessage = "There is no rpc service has been registered." + LogBuildUtil.build(params);
            LOG.error(errorMessage);
            throw new RpcException(errorMessage);
        }
        if (initRpcServer) {
            rpcServer.init();
        }
    }

}
