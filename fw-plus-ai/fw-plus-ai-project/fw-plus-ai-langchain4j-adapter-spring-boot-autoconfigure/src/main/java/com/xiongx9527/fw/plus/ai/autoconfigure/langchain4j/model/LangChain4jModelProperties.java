package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.model;

import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jModelType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LangChain4jModelProperties {

    private boolean enabled = true;

    private String defaultModel;

    private Map<String, Model> models = new LinkedHashMap<String, Model>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public Map<String, Model> getModels() {
        return models;
    }

    public void setModels(Map<String, Model> models) {
        this.models = models;
    }

    public static class Model {

        private String provider;

        private LangChain4jModelType modelType;

        private boolean enabled = true;

        private String baseUrl;

        private String apiKey;

        private List<String> modelName = new ArrayList<String>();

        private String defaultModelName;

        private Map<String, ModelConfig> modelConfigs = new LinkedHashMap<String, ModelConfig>();

        private Map<String, Object> properties = new LinkedHashMap<String, Object>();

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

        public List<String> getModelName() {
            return modelName;
        }

        public void setModelName(List<String> modelName) {
            this.modelName = modelName == null ? new ArrayList<String>() : new ArrayList<String>(modelName);
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, Object> properties) {
            this.properties = properties;
        }

        public String getDefaultModelName() {
            return defaultModelName;
        }

        public void setDefaultModelName(String defaultModelName) {
            this.defaultModelName = defaultModelName;
        }

        public Map<String, ModelConfig> getModelConfigs() {
            return modelConfigs;
        }

        public void setModelConfigs(Map<String, ModelConfig> modelConfigs) {
            this.modelConfigs = modelConfigs == null ? new LinkedHashMap<String, ModelConfig>() : modelConfigs;
        }

        public List<String> modelNames() {
            if (modelName == null) {
                return Collections.emptyList();
            }
            return modelName;
        }
    }

    public static class ModelConfig {

        private Boolean enabled;

        private String provider;

        private LangChain4jModelType modelType;

        private String baseUrl;

        private String apiKey;

        private Map<String, Object> properties = new LinkedHashMap<String, Object>();

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
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

        public Map<String, Object> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, Object> properties) {
            this.properties = properties;
        }
    }
}
