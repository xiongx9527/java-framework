/*
 * Copyright (c) 2019-2029, Dreamlu 卢春梦 (596392912@qq.com & dreamlu.net).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.auth;

import net.dreamlu.mica.net.http.common.HttpRequest;
import net.dreamlu.mica.net.http.common.HttpResponse;
import net.dreamlu.mica.net.http.common.router.HttpFilter;
import net.dreamlu.mica.net.http.common.router.HttpFilterChain;

/**
 * 自定义 mqtt http 接口认证，请按照自己的需求和业务进行扩展。
 *
 * <p>mica-mqtt 2.6.x 起 http 接口过滤器改为标准责任链形式：认证不通过时直接返回
 * 失败响应（如 {@code Result.fail(request, ResultCode.E103)}），通过时调用
 * {@code chain.doFilter(request)} 继续执行过滤器链。</p>
 *
 * @author L.cm
 */
//@Configuration(proxyBeanMethods = false)
public class MqttHttpAuthFilter implements HttpFilter {

	/**
	 * 过滤器方法，用于对HTTP请求进行过滤处理
	 *
	 * @param request HTTP请求对象
	 * @param chain   过滤器链
	 * @return HTTP响应；认证不通过时直接返回失败响应
	 * @throws Exception 处理过程中可能抛出的异常
	 */
	@Override
	public HttpResponse doFilter(HttpRequest request, HttpFilterChain chain) throws Exception {
		// 自行实现逻辑：
		// 认证不通过时返回: Result.fail(request, ResultCode.E103);
		return chain.doFilter(request);
	}

}
