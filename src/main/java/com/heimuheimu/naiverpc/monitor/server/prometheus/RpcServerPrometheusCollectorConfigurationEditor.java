/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 heimuheimu
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

package com.heimuheimu.naiverpc.monitor.server.prometheus;

import java.beans.PropertyEditorSupport;

/**
 * RpcServerPrometheusCollectorConfiguration 类型转换器，支持在 Spring 配置文件中通过字符串形式配置 RpcServerPrometheusCollectorConfiguration，
 * 字符串格式为：port, name
 *
 * @author heimuheimu
 * @since 1.2
 */
public class RpcServerPrometheusCollectorConfigurationEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("Fails to parse `RpcServerPrometheusCollectorConfiguration`: `text could not be null or empty`.");
        }
        String[] parts = text.split(",");
        if (parts.length == 2) {
            try {
                setValue(new RpcServerPrometheusCollectorConfiguration(Integer.parseInt(parts[0].trim()), parts[1].trim()));
            } catch (Exception e) {
                throw new IllegalArgumentException("Fails to parse `RpcServerPrometheusCollectorConfiguration`: `invalid text`. `text`:`"
                        + text + "`.", e);
            }
        } else {
            throw new IllegalArgumentException("Fails to parse `RpcServerPrometheusCollectorConfiguration`: `invalid text`. `text`:`"
                    + text + "`.");
        }
    }

    @Override
    public String getAsText() {
        RpcServerPrometheusCollectorConfiguration configuration = (RpcServerPrometheusCollectorConfiguration) getValue();
        return configuration == null ? "" : configuration.getPort() + ", " + configuration.getName();
    }
}
