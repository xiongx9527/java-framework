package com.xiongx9527.fw.plus.tinyengine.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonIgnoreProperties({"appId", "serviceId", "provider", "baseUrl", "apiKey", "modelName", "settings"})
public class AiModelInvocationConfig {

	private Integer appId;
	private Long serviceId;
	private String provider;
	private String baseUrl;
	private String apiKey;
	private String modelName;
	private Map<String, Object> settings = Collections.emptyMap();

	public Integer getAppId() { return appId; }
	public void setAppId(Integer appId) { this.appId = appId; }
	public Long getServiceId() { return serviceId; }
	public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
	public String getProvider() { return provider; }
	public void setProvider(String provider) { this.provider = provider; }
	public String getBaseUrl() { return baseUrl; }
	public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
	public String getApiKey() { return apiKey; }
	public void setApiKey(String apiKey) { this.apiKey = apiKey; }
	public String getModelName() { return modelName; }
	public void setModelName(String modelName) { this.modelName = modelName; }
	public Map<String, Object> getSettings() { return settings; }
	public void setSettings(Map<String, Object> settings) {
		this.settings = settings == null ? Collections.<String, Object>emptyMap()
				: Collections.unmodifiableMap(new LinkedHashMap<String, Object>(settings));
	}
}
