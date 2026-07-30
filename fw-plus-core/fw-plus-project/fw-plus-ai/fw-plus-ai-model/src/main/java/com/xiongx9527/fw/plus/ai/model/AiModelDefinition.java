package com.xiongx9527.fw.plus.ai.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class AiModelDefinition {

    private String key;

    private String framework;

    private String provider;

    private AiModelType type;

    private boolean enabled;

    private Map<String, Object> properties = new LinkedHashMap<String, Object>();

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getFramework() {
        return framework;
    }

    public void setFramework(String framework) {
        this.framework = framework;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public AiModelType getType() {
        return type;
    }

    public void setType(AiModelType type) {
        this.type = type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties == null ? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(properties);
    }
}
