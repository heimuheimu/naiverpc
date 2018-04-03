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

package com.heimuheimu.naiverpc.util;

import java.util.Arrays;
import java.util.Map;

/**
 * 日志信息输出构造器。
 *
 * @author heimuheimu
 */
public class LogBuildUtil {

    /**
     * 生成方法执行错误日志文本信息。
     *
     * @param methodName 方法名称
     * @param errorMessage 错误信息
     * @param parameterMap 方法执行参数 {@code Map}，允许为 {@code null}
     * @return 方法执行错误日志文本信息
     */
    public static String buildMethodExecuteFailedLog(String methodName, String errorMessage, Map<String, Object> parameterMap) {
        return "Execute `" + methodName + "` failed: `" + errorMessage +  "`." + LogBuildUtil.build(parameterMap);
    }

    /**
     * 根据 {@code Map} 信息构造一个供日志输出使用的文本信息。
     *
     * @param data {@code Map} 信息
     * @return 供日志输出使用的文本信息
     */
    public static String build(Map<String, Object> data) {
        StringBuilder buffer = new StringBuilder();
        if (data != null && !data.isEmpty()) {
            for (String key : data.keySet()) {
                buffer.append(" `").append(key).append("`:`");
                Object value = data.get(key);
                if (value != null && value.getClass().isArray()) {
                    if (value.getClass() == int[].class) {
                        buffer.append(Arrays.toString((int[]) value));
                    } else if (value.getClass() == long[].class) {
                        buffer.append(Arrays.toString((long[]) value));
                    } else if (value.getClass() == double[].class) {
                        buffer.append(Arrays.toString((double[]) value));
                    } else if (value.getClass() == byte[].class) {
                        buffer.append(Arrays.toString((byte[]) value));
                    } else if (value.getClass() == short[].class) {
                        buffer.append(Arrays.toString((short[]) value));
                    } else if (value.getClass() == boolean[].class) {
                        buffer.append(Arrays.toString((boolean[]) value));
                    } else if (value.getClass() == float[].class) {
                        buffer.append(Arrays.toString((float[]) value));
                    } else {
                        buffer.append(Arrays.toString((Object[]) value));
                    }
                } else {
                    buffer.append(value);
                }
                buffer.append("`.");
            }
        }
        return buffer.toString();
    }
}
