package com.xiongx9527.fw.plus.ai.autoconfigure.model;

import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "fw-plus.ai.model")
public class AiModelProperties {

	private boolean enabled = true;
	private String defaultModel;
	private Map<String, Model> models = new LinkedHashMap<String, Model>();

	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }
	public String getDefaultModel() { return defaultModel; }
	public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }
	public Map<String, Model> getModels() { return models; }
	public void setModels(Map<String, Model> models) {
		this.models = models == null ? new LinkedHashMap<String, Model>() : models;
	}

	public static class Model extends AiModelDefinition {
		public Model() { setEnabled(true); }
	}
}
