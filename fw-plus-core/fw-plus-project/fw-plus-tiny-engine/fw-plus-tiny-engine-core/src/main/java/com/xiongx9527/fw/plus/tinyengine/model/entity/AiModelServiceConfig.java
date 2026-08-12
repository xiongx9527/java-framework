package com.xiongx9527.fw.plus.tinyengine.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiongx9527.fw.plus.common.mate.annotation.FieldEncrypt;

@TableName("t_ai_model_service")
public class AiModelServiceConfig extends AiModelBaseEntity {

	private Integer appId;
	private String serviceKey;
	private String provider;
	private String label;
	private String baseUrl;
	@FieldEncrypt
	private String apiKey;
	private Boolean allowEmptyApiKey;
	private Boolean enabled;
	private Integer sortOrder;

	public Integer getAppId() { return appId; }
	public void setAppId(Integer appId) { this.appId = appId; }
	public String getServiceKey() { return serviceKey; }
	public void setServiceKey(String serviceKey) { this.serviceKey = serviceKey; }
	public String getProvider() { return provider; }
	public void setProvider(String provider) { this.provider = provider; }
	public String getLabel() { return label; }
	public void setLabel(String label) { this.label = label; }
	public String getBaseUrl() { return baseUrl; }
	public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
	public String getApiKey() { return apiKey; }
	public void setApiKey(String apiKey) { this.apiKey = apiKey; }
	public Boolean getAllowEmptyApiKey() { return allowEmptyApiKey; }
	public void setAllowEmptyApiKey(Boolean allowEmptyApiKey) { this.allowEmptyApiKey = allowEmptyApiKey; }
	public Boolean getEnabled() { return enabled; }
	public void setEnabled(Boolean enabled) { this.enabled = enabled; }
	public Integer getSortOrder() { return sortOrder; }
	public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
