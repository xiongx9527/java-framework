package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TinyflowKnowledgeProviderRegistry {

	private final Map<String, TinyflowKnowledgeProvider> providers;

	public TinyflowKnowledgeProviderRegistry(Collection<TinyflowKnowledgeProvider> candidates) {
		Map<String, TinyflowKnowledgeProvider> registered = new LinkedHashMap<String, TinyflowKnowledgeProvider>();
		if (candidates != null) {
			for (TinyflowKnowledgeProvider provider : candidates) {
				if (provider == null) {
					throw new TinyflowException("Tinyflow knowledge provider must not be null");
				}
				String key = requireText(provider.getKey(), "Tinyflow knowledge provider key must not be blank");
				if (registered.containsKey(key)) {
					throw new TinyflowException("Duplicate Tinyflow knowledge provider key: " + key);
				}
				if (provider.capabilities() == null) {
					throw new TinyflowException("Tinyflow knowledge provider capabilities must not be null: " + key);
				}
				registered.put(key, provider);
			}
		}
		this.providers = Collections.unmodifiableMap(registered);
	}

	public TinyflowKnowledgeProvider get(String key) {
		return key == null ? null : providers.get(key.trim());
	}

	public TinyflowKnowledgeProvider require(String key) {
		TinyflowKnowledgeProvider provider = get(key);
		if (provider == null) {
			throw new TinyflowException("Tinyflow knowledge provider not found: " + key);
		}
		return provider;
	}

	public boolean supportsManagement(String key) {
		TinyflowKnowledgeProvider provider = require(key);
		return provider instanceof TinyflowKnowledgeManagementProvider
				&& provider.capabilities().supportsManagement();
	}

	public Map<String, TinyflowKnowledgeProvider> getProviders() {
		return providers;
	}

	private static String requireText(String value, String message) {
		if (value == null || value.trim().length() == 0) {
			throw new TinyflowException(message);
		}
		return value.trim();
	}
}
