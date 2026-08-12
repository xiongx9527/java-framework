package com.xiongx9527.fw.plus.tinyengine.model.dto;

import com.xiongx9527.fw.plus.tinyengine.model.entity.AiModelConfig;
import com.xiongx9527.fw.plus.tinyengine.model.entity.AiModelServiceConfig;

import java.util.ArrayList;
import java.util.List;

public class AiModelServiceDto {

	private Integer id;
	private String serviceKey;
	private String provider;
	private String label;
	private String baseUrl;
	private Boolean allowEmptyApiKey;
	private Boolean enabled;
	private Integer sortOrder;
	private boolean hasApiKey;
	private String apiKeyMask;
	private List<AiModelConfig> models = new ArrayList<AiModelConfig>();

	public static AiModelServiceDto from(AiModelServiceConfig service) {
		AiModelServiceDto dto = new AiModelServiceDto();
		dto.setId(service.getId());
		dto.setServiceKey(service.getServiceKey());
		dto.setProvider(service.getProvider());
		dto.setLabel(service.getLabel());
		dto.setBaseUrl(service.getBaseUrl());
		dto.setAllowEmptyApiKey(service.getAllowEmptyApiKey());
		dto.setEnabled(service.getEnabled());
		dto.setSortOrder(service.getSortOrder());
		dto.setHasApiKey(service.getApiKey() != null && !service.getApiKey().isEmpty());
		dto.setApiKeyMask(dto.isHasApiKey() ? "********" : null);
		return dto;
	}

	public Integer getId() { return id; }
	public void setId(Integer id) { this.id = id; }
	public String getServiceKey() { return serviceKey; }
	public void setServiceKey(String serviceKey) { this.serviceKey = serviceKey; }
	public String getProvider() { return provider; }
	public void setProvider(String provider) { this.provider = provider; }
	public String getLabel() { return label; }
	public void setLabel(String label) { this.label = label; }
	public String getBaseUrl() { return baseUrl; }
	public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
	public Boolean getAllowEmptyApiKey() { return allowEmptyApiKey; }
	public void setAllowEmptyApiKey(Boolean allowEmptyApiKey) { this.allowEmptyApiKey = allowEmptyApiKey; }
	public Boolean getEnabled() { return enabled; }
	public void setEnabled(Boolean enabled) { this.enabled = enabled; }
	public Integer getSortOrder() { return sortOrder; }
	public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
	public boolean isHasApiKey() { return hasApiKey; }
	public void setHasApiKey(boolean hasApiKey) { this.hasApiKey = hasApiKey; }
	public String getApiKeyMask() { return apiKeyMask; }
	public void setApiKeyMask(String apiKeyMask) { this.apiKeyMask = apiKeyMask; }
	public List<AiModelConfig> getModels() { return models; }
	public void setModels(List<AiModelConfig> models) { this.models = models; }
}
