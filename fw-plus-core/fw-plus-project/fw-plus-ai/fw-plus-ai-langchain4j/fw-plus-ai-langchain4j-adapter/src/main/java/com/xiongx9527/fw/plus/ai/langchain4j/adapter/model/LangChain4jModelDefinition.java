package com.xiongx9527.fw.plus.ai.langchain4j.adapter.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class LangChain4jModelDefinition {

    private String key;

    private String provider;

    private LangChain4jModelType modelType;

    private boolean enabled;

    private String baseUrl;

    private String apiKey;

    private String modelName;

    private Map<String, Object> properties = new LinkedHashMap<String, Object>();

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public LangChain4jModelType getModelType() {
        return modelType;
    }

    public void setModelType(LangChain4jModelType modelType) {
        this.modelType = modelType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}
