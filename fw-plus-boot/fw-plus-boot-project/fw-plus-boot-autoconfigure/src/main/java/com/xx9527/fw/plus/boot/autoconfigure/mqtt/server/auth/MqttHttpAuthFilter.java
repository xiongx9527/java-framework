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

package com.xx9527.fw.plus.boot.autoconfigure.mqtt.server.auth;

import net.dreamlu.iot.mqtt.core.server.http.api.code.ResultCode;
import net.dreamlu.iot.mqtt.core.server.http.api.result.Result;
import net.dreamlu.iot.mqtt.core.server.http.handler.HttpFilter;
import net.dreamlu.iot.mqtt.core.server.http.handler.MqttHttpRoutes;
import org.springframework.beans.factory.InitializingBean;
import org.tio.http.common.HttpRequest;
import org.tio.http.common.HttpResponse;

/**
 * 自定义 mqtt http 接口认证，请按照自己的需求和业务进行扩展
 *
 * @author L.cm
 */
//@Configuration(proxyBeanMethods = false)
public class MqttHttpAuthFilter implements HttpFilter, InitializingBean {

	/**
	 * 过滤器方法，用于对HTTP请求进行过滤处理
	 * @param request HTTP请求对象
	 * @return true表示通过过滤，false表示不通过过滤
	 * @throws Exception 处理过程中可能抛出的异常
	 */
	@Override
	public boolean filter(HttpRequest request) throws Exception {
		// 自行实现逻辑
		return true;
	}

	/**
	 * 生成过滤器处理失败时的HTTP响应
	 * @param request HTTP请求对象
	 * @return 认证失败的HTTP响应结果
	 */
	@Override
	public HttpResponse response(HttpRequest request) {
		// 认证不通过时的响应
		return Result.fail(request, ResultCode.E103);
	}

	/**
	 * Bean初始化完成后的回调方法，用于将当前过滤器添加到MQTT HTTP路由中
	 * @throws Exception 初始化过程中可能抛出的异常
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		MqttHttpRoutes.addFilter(this);
	}

}
