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

package com.heimuheimu.naiverpc.spring.client;

import com.heimuheimu.naiverpc.spring.InterfaceFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * 自动扫描指定包下的所有和接口名字正则匹配规则匹配的接口，生成接口对应的 RPC 远程调用代理实例，
 * 并将该代理实例在 Spring 工厂中进行注册
 *
 * @author heimuheimu
 */
public class AutoRpcProxyBeanRegister implements BeanDefinitionRegistryPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(AutoRpcProxyBeanRegister.class);

    private final String rpcClientBeanName;

    private final String[] basePackages;

    private final String classNameRegex;

    public AutoRpcProxyBeanRegister(String rpcClientBeanName, String[] basePackages, String classNameRegex) {
        this.rpcClientBeanName = rpcClientBeanName;
        this.basePackages = basePackages;
        this.classNameRegex = classNameRegex;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        InterfaceFinder interfaceFinder = new InterfaceFinder();
        List<Class<?>> candidates = new ArrayList<>();
        for (String basePackage : basePackages) {
            try {
                candidates.addAll(interfaceFinder.find(basePackage, classNameRegex));
            } catch (Exception e) {
                throw new FatalBeanException("Scan interface failed. BasePackage: `" + basePackage + "`. ClassNameRegex: `"
                    + classNameRegex + "`.", e);
            }
        }
        if (!candidates.isEmpty()) {
            for (Class<?> clz : candidates) {
                AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
                        .genericBeanDefinition(RpcProxyFactoryBean.class)
                        .addConstructorArgValue(clz)
                        .addConstructorArgReference(rpcClientBeanName)
                        .setScope(BeanDefinition.SCOPE_SINGLETON)
                        .getBeanDefinition();
                registry.registerBeanDefinition(clz.getSimpleName(), beanDefinition);
                LOG.info("`{}` proxy has been registered.", clz.getName());
            }
        } else {
            LOG.warn("There is no rpc proxy has been registered.");
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        //do nothing
    }

}
