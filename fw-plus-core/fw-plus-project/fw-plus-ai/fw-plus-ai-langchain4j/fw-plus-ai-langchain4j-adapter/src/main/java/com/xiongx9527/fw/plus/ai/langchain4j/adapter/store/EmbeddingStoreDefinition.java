package com.xiongx9527.fw.plus.ai.langchain4j.adapter.store;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EmbeddingStoreDefinition {

    private String key;

    private String provider;

    private boolean enabled = true;

    private Integer dimension;

    private List<String> metadataKeys = new ArrayList<String>();

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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getDimension() {
        return dimension;
    }

    public void setDimension(Integer dimension) {
        this.dimension = dimension;
    }

    public List<String> getMetadataKeys() {
        return metadataKeys;
    }

    public void setMetadataKeys(List<String> metadataKeys) {
        this.metadataKeys = metadataKeys;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}
