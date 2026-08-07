package com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow.dify;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = TinyflowDifyExternalProperties.PREFIX)
public class TinyflowDifyExternalProperties implements InitializingBean {

	public static final String PREFIX = "fw.plus.ai.tinyflow.dify.external";

	private boolean enabled;
	private String basePath = "/external-knowledge";
	private Duration requestTimeout = Duration.ofSeconds(5L);
	private int maxTopK = 20;
	private int rateLimit = 60;

	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }
	public String getBasePath() { return basePath; }
	public void setBasePath(String basePath) { this.basePath = basePath; }
	public Duration getRequestTimeout() { return requestTimeout; }
	public void setRequestTimeout(Duration requestTimeout) { this.requestTimeout = requestTimeout; }
	public int getMaxTopK() { return maxTopK; }
	public void setMaxTopK(int maxTopK) { this.maxTopK = maxTopK; }
	public int getRateLimit() { return rateLimit; }
	public void setRateLimit(int rateLimit) { this.rateLimit = rateLimit; }

	@Override
	public void afterPropertiesSet() {
		if (basePath == null || !basePath.startsWith("/") || basePath.length() < 2) {
			throw new IllegalArgumentException("Tinyflow Dify external base path must be absolute");
		}
		if (requestTimeout == null || requestTimeout.isZero() || requestTimeout.isNegative()) {
			throw new IllegalArgumentException("Tinyflow Dify external request timeout must be positive");
		}
		if (maxTopK <= 0) {
			throw new IllegalArgumentException("Tinyflow Dify external max topK must be positive");
		}
		if (rateLimit <= 0) {
			throw new IllegalArgumentException("Tinyflow Dify external rate limit must be positive");
		}
	}
}
