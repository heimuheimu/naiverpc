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

import com.heimuheimu.naiverpc.server.RpcServer;
import com.heimuheimu.naiverpc.spring.InterfaceFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.ArrayList;
import java.util.List;

/**
 * 自动扫描指定包下的所有和接口名字正则匹配规则匹配的接口，并在 Spring 工厂中寻找对应的实现类，将其注册为 RPC 服务实例
 *
 * @author heimuheimu
 */
@SuppressWarnings("unused")
public class AutoRpcServiceBeanRegister implements ApplicationContextAware, InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(AutoRpcServiceBeanRegister.class);

    private ApplicationContext applicationContext;

    /**
     * RPC 服务提供者，通过指定的监听端口与 RPC 服务调用客户端建立连接，为其提供 RPC 服务
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
     * @see #basePackages
     */
    private final String classNameRegex;

    public AutoRpcServiceBeanRegister(RpcServer rpcServer, String[] basePackages, String classNameRegex) {
        this.rpcServer = rpcServer;
        this.basePackages = basePackages;
        this.classNameRegex = classNameRegex;
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
        if (!candidates.isEmpty()) {
            for (Class<?> clz : candidates) {
                try {
                    Object rpcServiceBean = applicationContext.getBean(clz);
                    rpcServer.register(rpcServiceBean);
                    LOG.info("`{}` has been registered. Interface: `{}`", rpcServiceBean.getClass().getName(),
                            clz.getName());
                } catch(Exception e) {
                    LOG.error("`" + clz + "` register failed.", e);
                }
            }
        } else {
            LOG.error("There is no rpc service has been registered.");
        }
    }

}
