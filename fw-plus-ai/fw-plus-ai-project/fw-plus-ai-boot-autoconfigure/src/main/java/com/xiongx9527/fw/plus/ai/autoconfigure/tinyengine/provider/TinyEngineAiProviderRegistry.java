package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.provider;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class TinyEngineAiProviderRegistry {

	private final Map<String, TinyEngineAiProvider> providers = new LinkedHashMap<String, TinyEngineAiProvider>();
	private final String defaultProviderKey;

	public TinyEngineAiProviderRegistry(Collection<TinyEngineAiProvider> providers, String configuredProviderKey) {
		if (providers != null) {
			for (TinyEngineAiProvider provider : providers) {
				if (provider != null && !isBlank(provider.getKey())) {
					this.providers.put(provider.getKey(), provider);
				}
			}
		}
		this.defaultProviderKey = resolveDefaultProviderKey(configuredProviderKey);
	}

	public String getDefaultProviderKey() {
		return defaultProviderKey;
	}

	public TinyEngineAiProvider getDefaultProvider() {
		return getProvider(defaultProviderKey);
	}

	public TinyEngineAiProvider getProvider(String key) {
		if (isBlank(key)) {
			if (providers.size() > 1) {
				throw new IllegalStateException("TinyEngine AI provider must be configured when multiple providers are available: "
						+ joinProviderKeys());
			}
			throw new IllegalStateException("TinyEngine AI provider is not configured");
		}
		TinyEngineAiProvider provider = providers.get(key);
		if (provider == null) {
			throw new IllegalStateException("TinyEngine AI provider is not available: " + key);
		}
		return provider;
	}

	public TinyEngineAiProvider requireCapability(TinyEngineAiCapability capability) {
		TinyEngineAiProvider provider = getDefaultProvider();
		if (!provider.supports(capability)) {
			throw new IllegalStateException("TinyEngine AI provider '" + provider.getKey()
					+ "' does not support capability '" + capability + "'");
		}
		return provider;
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}

	private String resolveDefaultProviderKey(String configuredProviderKey) {
		if (!isBlank(configuredProviderKey)) {
			return configuredProviderKey.trim();
		}
		if (providers.size() == 1) {
			return providers.keySet().iterator().next();
		}
		return null;
	}

	private String joinProviderKeys() {
		StringBuilder builder = new StringBuilder();
		for (String key : providers.keySet()) {
			if (builder.length() > 0) {
				builder.append(",");
			}
			builder.append(key);
		}
		return builder.toString();
	}
}
