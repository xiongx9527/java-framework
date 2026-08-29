package com.xiongx9527.fw.plus.ai.langchain4j.adapter.store;

import java.util.LinkedHashMap;
import java.util.Map;

public class EmbeddingStoreProperties {

    private boolean enabled = true;

    private String defaultStore;

    private Map<String, EmbeddingStoreDefinition> stores = new LinkedHashMap<String, EmbeddingStoreDefinition>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultStore() {
        return defaultStore;
    }

    public void setDefaultStore(String defaultStore) {
        this.defaultStore = defaultStore;
    }

    public Map<String, EmbeddingStoreDefinition> getStores() {
        return stores;
    }

    public void setStores(Map<String, EmbeddingStoreDefinition> stores) {
        this.stores = stores;
    }
}
