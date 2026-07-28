package com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "fw-plus.mqtt.server.security")
public class FwPlusMqttServerSecurityProperties {

	private boolean publishAuthEnabled = true;

	private boolean subscribeAuthEnabled = true;

	private boolean allowAnonymousWhenAuthDisabled = true;
}
