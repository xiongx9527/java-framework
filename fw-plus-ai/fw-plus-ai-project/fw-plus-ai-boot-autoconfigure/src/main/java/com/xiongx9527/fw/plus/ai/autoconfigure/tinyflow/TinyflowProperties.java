package com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "fw.plus.ai.tinyflow")
public class TinyflowProperties {

	private boolean enabled = true;
	private Duration executionTimeout = Duration.ofMinutes(5);

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Duration getExecutionTimeout() {
		return executionTimeout;
	}

	public void setExecutionTimeout(Duration executionTimeout) {
		this.executionTimeout = executionTimeout;
	}
}
