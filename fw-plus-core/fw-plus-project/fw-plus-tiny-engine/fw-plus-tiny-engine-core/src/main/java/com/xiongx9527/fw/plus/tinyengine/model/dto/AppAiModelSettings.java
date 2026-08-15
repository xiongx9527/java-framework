package com.xiongx9527.fw.plus.tinyengine.model.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AppAiModelSettings {

	private List<AiModelServiceDto> services = new ArrayList<AiModelServiceDto>();
	private String defaultModel;
	private String quickModel;
	private String chatMode;
	private Boolean enableThinking;
	private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

	public static AppAiModelSettings copyOf(Object value) {
		AppAiModelSettings copy = new AppAiModelSettings();
		if (value instanceof AppAiModelSettings) {
			AppAiModelSettings source = (AppAiModelSettings) value;
			copy.setDefaultModel(source.getDefaultModel());
			copy.setQuickModel(source.getQuickModel());
			copy.setChatMode(source.getChatMode());
			copy.setEnableThinking(source.getEnableThinking());
			copy.getAdditionalProperties().putAll(source.getAdditionalProperties());
		} else if (value instanceof Map) {
			Map<?, ?> source = (Map<?, ?>) value;
			copy.setDefaultModel(stringValue(source.get("defaultModel")));
			copy.setQuickModel(stringValue(source.get("quickModel")));
			copy.setChatMode(stringValue(source.get("chatMode")));
			Object thinking = source.get("enableThinking");
			if (thinking instanceof Boolean) {
				copy.setEnableThinking((Boolean) thinking);
			}
			for (Map.Entry<?, ?> entry : source.entrySet()) {
				String key = String.valueOf(entry.getKey());
				if (!"services".equals(key) && !"defaultModel".equals(key) && !"quickModel".equals(key)
						&& !"chatMode".equals(key) && !"enableThinking".equals(key)) {
					copy.setAdditionalProperty(key, entry.getValue());
				}
			}
		}
		return copy;
	}

	private static String stringValue(Object value) {
		return value == null ? null : String.valueOf(value);
	}

	public List<AiModelServiceDto> getServices() { return services; }
	public void setServices(List<AiModelServiceDto> services) { this.services = services; }
	public String getDefaultModel() { return defaultModel; }
	public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }
	public String getQuickModel() { return quickModel; }
	public void setQuickModel(String quickModel) { this.quickModel = quickModel; }
	public String getChatMode() { return chatMode; }
	public void setChatMode(String chatMode) { this.chatMode = chatMode; }
	public Boolean getEnableThinking() { return enableThinking; }
	public void setEnableThinking(Boolean enableThinking) { this.enableThinking = enableThinking; }
	@JsonAnyGetter
	public Map<String, Object> getAdditionalProperties() { return additionalProperties; }
	@JsonAnySetter
	public void setAdditionalProperty(String name, Object value) { additionalProperties.put(name, value); }
}
