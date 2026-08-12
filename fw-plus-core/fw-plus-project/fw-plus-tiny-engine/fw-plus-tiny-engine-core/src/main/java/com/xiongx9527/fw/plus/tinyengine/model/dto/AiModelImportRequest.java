package com.xiongx9527.fw.plus.tinyengine.model.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class AiModelImportRequest {

	private AppAiModelSettings settings;
	private Map<String, String> apiKeys = new LinkedHashMap<String, String>();

	public AppAiModelSettings getSettings() { return settings; }
	public void setSettings(AppAiModelSettings settings) { this.settings = settings; }
	public Map<String, String> getApiKeys() { return apiKeys; }
	public void setApiKeys(Map<String, String> apiKeys) { this.apiKeys = apiKeys; }
}
