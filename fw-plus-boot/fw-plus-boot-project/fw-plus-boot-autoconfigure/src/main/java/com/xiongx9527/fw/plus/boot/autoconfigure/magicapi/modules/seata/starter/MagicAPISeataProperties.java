package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.seata.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "magic-api.plugin.seata")
public class MagicAPISeataProperties {

	private boolean enabled = true;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
