package com.xiongx9527.fw.plus.ai.store.embedding;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class AiEmbeddingStoreDefinition {

    private String key;

    private String framework;

    private String provider;

    private AiEmbeddingStoreType type;

    private boolean enabled;

    private Map<String, Object> properties = Collections.emptyMap();

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

    public AiEmbeddingStoreType getType() {
        return type;
    }

    public void setType(AiEmbeddingStoreType type) {
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
        if (properties == null || properties.isEmpty()) {
            this.properties = Collections.emptyMap();
        } else {
            this.properties = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(properties));
        }
    }
}
