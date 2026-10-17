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
import org.dromara.mica.mqtt.codec.MqttQoS;
import org.dromara.mica.mqtt.core.server.auth.IMqttServerPublishPermission;
import org.apache.commons.lang3.StringUtils;
import net.dreamlu.mica.net.core.ChannelContext;

import java.util.Objects;

public class MqttPublishPermission implements IMqttServerPublishPermission {

	private final FwPlusMqttServerSecurityProperties securityProperties;

	public MqttPublishPermission() {
		this(new FwPlusMqttServerSecurityProperties());
	}

	public MqttPublishPermission(FwPlusMqttServerSecurityProperties securityProperties) {
		this.securityProperties = Objects.requireNonNull(securityProperties, "FwPlusMqttServerSecurityProperties must not be null");
	}

	@Override
	public boolean hasPermission(ChannelContext context, String clientId, String topic, MqttQoS qoS, boolean isRetain) {
		if (!this.securityProperties.isPublishAuthEnabled()) {
			return true;
		}
		return StringUtils.isNotBlank(clientId)
				&& StringUtils.isNotBlank(topic)
				&& !StringUtils.contains(topic, '+')
				&& !StringUtils.contains(topic, '#');
	}
}
