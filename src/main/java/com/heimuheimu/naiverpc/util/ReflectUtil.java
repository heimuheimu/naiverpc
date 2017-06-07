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
package com.heimuheimu.naiverpc.util;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 *
 * @author heimuheimu
 */
public class ReflectUtil {

	public static String getMethodUniqueName(Method method) {
		Class<?>[] parameterTypes = method.getParameterTypes();
		if (parameterTypes.length == 0) {
			return method.getName();
		} else {
			StringBuilder buffer = new StringBuilder();
			buffer.append(method.getName()).append("#");
			for (Class<?> parameterType : parameterTypes) {
				buffer.append(parameterType.getName()).append(",");
			}
			return buffer.toString();
		}
		
	}
	
}
