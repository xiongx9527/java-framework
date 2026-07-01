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

import net.dreamlu.iot.mqtt.codec.MqttQoS;
import net.dreamlu.iot.mqtt.core.server.auth.IMqttServerSubscribeValidator;
import org.tio.core.ChannelContext;

/**
 * 订阅校验，请按照自己的需求和业务进行扩展
 *
 * @author L.cm
 */
//@Configuration(proxyBeanMethods = false)
public class MqttSubscribeValidator implements IMqttServerSubscribeValidator {

	/**
	 * 验证客户端订阅主题的合法性
	 *
	 * @param context Channel上下文对象，包含连接相关信息
	 * @param clientId 客户端唯一标识符
	 * @param topicFilter 主题过滤器，客户端要订阅的主题
	 * @param qoS 服务质量等级
	 * @return 验证结果，true表示订阅合法，false表示订阅不合法
	 */
	@Override
	public boolean isValid(ChannelContext context, String clientId, String topicFilter, MqttQoS qoS) {
		// 校验客户端订阅的 topic，校验成功返回 true，失败返回 false
		return true;
	}


}
