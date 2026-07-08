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
import net.dreamlu.iot.mqtt.codec.MqttQoS;
import net.dreamlu.iot.mqtt.core.server.auth.IMqttServerSubscribeValidator;
import org.apache.commons.lang3.StringUtils;
import org.tio.core.ChannelContext;

import java.util.Objects;

public class MqttSubscribeValidator implements IMqttServerSubscribeValidator {

	private final FwPlusMqttServerSecurityProperties securityProperties;

	public MqttSubscribeValidator() {
		this(new FwPlusMqttServerSecurityProperties());
	}

	public MqttSubscribeValidator(FwPlusMqttServerSecurityProperties securityProperties) {
		this.securityProperties = Objects.requireNonNull(securityProperties, "FwPlusMqttServerSecurityProperties must not be null");
	}

	@Override
	public boolean isValid(ChannelContext context, String clientId, String topicFilter, MqttQoS qoS) {
		if (!this.securityProperties.isSubscribeAuthEnabled()) {
			return true;
		}
		return StringUtils.isNotBlank(clientId)
				&& StringUtils.isNotBlank(topicFilter)
				&& isValidTopicFilter(topicFilter);
	}

	private boolean isValidTopicFilter(String topicFilter) {
		String[] levels = topicFilter.split("/", -1);
		for (int i = 0; i < levels.length; i++) {
			String level = levels[i];
			if (StringUtils.contains(level, '#')) {
				if (!"#".equals(level) || i != levels.length - 1) {
					return false;
				}
			}
			if (StringUtils.contains(level, '+') && !"+".equals(level)) {
				return false;
			}
		}
		return true;
	}
}
