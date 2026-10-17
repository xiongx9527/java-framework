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

import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.properties.FwPlusMqttServerSecurityProperties;
import org.dromara.mica.mqtt.core.server.auth.IMqttServerAuthHandler;
import org.dromara.mica.mqtt.spring.server.config.MqttServerProperties;
import org.apache.commons.lang3.StringUtils;
import net.dreamlu.mica.net.core.ChannelContext;

import java.util.Objects;

public class MqttAuthHandler implements IMqttServerAuthHandler {

	private final MqttServerProperties properties;

	private final FwPlusMqttServerSecurityProperties securityProperties;

	public MqttAuthHandler() {
		this(new MqttServerProperties(), new FwPlusMqttServerSecurityProperties());
	}

	public MqttAuthHandler(MqttServerProperties properties, FwPlusMqttServerSecurityProperties securityProperties) {
		this.properties = Objects.requireNonNull(properties, "MqttServerProperties must not be null");
		this.securityProperties = Objects.requireNonNull(securityProperties, "FwPlusMqttServerSecurityProperties must not be null");
	}

	@Override
	public boolean authenticate(ChannelContext context, String uniqueId, String clientId, String userName, String password) {
		if (StringUtils.isBlank(clientId)) {
			return false;
		}
		MqttServerProperties.MqttAuth mqttAuth = this.properties.getAuth();
		if (mqttAuth == null || !mqttAuth.isEnable()) {
			return this.securityProperties.isAllowAnonymousWhenAuthDisabled();
		}
		return StringUtils.equals(mqttAuth.getUsername(), userName)
				&& StringUtils.equals(mqttAuth.getPassword(), password);
	}
}
