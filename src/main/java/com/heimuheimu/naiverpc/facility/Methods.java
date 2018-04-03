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

package com.heimuheimu.naiverpc.facility;

import com.heimuheimu.naiverpc.util.LogBuildUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 对 {@link Runnable#run()} 方法的执行进行封装，捕获异常，统一错误日志输出格式。
 *
 * @author heimuheimu
 */
public class Methods {

    private static final Logger LOGGER = LoggerFactory.getLogger(Methods.class);

    /**
     * 当 {@code target} 不为 {@code null} 时，执行 {@code runnable#run()} 方法，捕获方法执行异常，进行错误日志打印。
     *
     * <p><strong>说明：</strong>该方法不会抛出任何异常。</p>
     *
     * @param methodName 方法名称，用于错误日志打印
     * @param parameterMap 方法参数 {@code Map}，用于错误日志打印
     * @param target {@code target} 不为 {@code null} 时，{@code runnable#run()} 才会被执行
     * @param runnable {@code target} 不为 {@code null} 时，需要执行的方法
     */
    public static void invokeIfNotNull(String methodName, Map<String, Object> parameterMap, Object target, Runnable runnable) {
        if (target != null) {
            try {
                runnable.run();
            } catch (Exception e) {
                LOGGER.error(LogBuildUtil.buildMethodExecuteFailedLog(methodName, e.getMessage(), parameterMap), e);
            }
        }
    }
}
