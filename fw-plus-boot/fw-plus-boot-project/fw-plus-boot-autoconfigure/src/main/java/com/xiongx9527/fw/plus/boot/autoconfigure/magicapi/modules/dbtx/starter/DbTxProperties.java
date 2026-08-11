package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "magic-api.dbtx")
public class DbTxProperties {

	private boolean enabled = true;

	private String defaultCoordinator = "local";

	private boolean localEnabled = true;

	private boolean seataEnabled = false;

	private int timeoutMs = 60000;

	private String namePrefix = "magic-api-dbtx";

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getDefaultCoordinator() {
		return defaultCoordinator;
	}

	public void setDefaultCoordinator(String defaultCoordinator) {
		this.defaultCoordinator = defaultCoordinator;
	}

	public boolean isLocalEnabled() {
		return localEnabled;
	}

	public void setLocalEnabled(boolean localEnabled) {
		this.localEnabled = localEnabled;
	}

	public boolean isSeataEnabled() {
		return seataEnabled;
	}

	public void setSeataEnabled(boolean seataEnabled) {
		this.seataEnabled = seataEnabled;
	}

	public int getTimeoutMs() {
		return timeoutMs;
	}

	public void setTimeoutMs(int timeoutMs) {
		this.timeoutMs = timeoutMs;
	}

	public String getNamePrefix() {
		return namePrefix;
	}

	public void setNamePrefix(String namePrefix) {
		this.namePrefix = namePrefix;
	}
}
