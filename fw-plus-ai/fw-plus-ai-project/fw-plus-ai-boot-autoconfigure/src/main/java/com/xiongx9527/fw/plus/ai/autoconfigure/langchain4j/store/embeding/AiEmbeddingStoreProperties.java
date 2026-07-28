package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding;

import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreDefinition;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "fw-plus.ai.embedding-store")
public class AiEmbeddingStoreProperties {

	private boolean enabled = true;
	private String defaultStore;
	private Map<String, Store> stores = new LinkedHashMap<String, Store>();

	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }
	public String getDefaultStore() { return defaultStore; }
	public void setDefaultStore(String defaultStore) { this.defaultStore = defaultStore; }
	public Map<String, Store> getStores() { return stores; }
	public void setStores(Map<String, Store> stores) {
		this.stores = stores == null ? new LinkedHashMap<String, Store>() : stores;
	}

	public static class Store extends AiEmbeddingStoreDefinition {
		public Store() { setEnabled(true); }
	}
}
