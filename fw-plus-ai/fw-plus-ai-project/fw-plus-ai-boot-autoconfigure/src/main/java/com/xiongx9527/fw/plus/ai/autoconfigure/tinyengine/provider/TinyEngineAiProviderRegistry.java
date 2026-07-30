package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.provider;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class TinyEngineAiProviderRegistry {

	public static final String DEFAULT_PROVIDER_KEY = "langchain4j";

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
		this.defaultProviderKey = isBlank(configuredProviderKey) ? DEFAULT_PROVIDER_KEY : configuredProviderKey.trim();
	}

	public String getDefaultProviderKey() {
		return defaultProviderKey;
	}

	public TinyEngineAiProvider getDefaultProvider() {
		return getProvider(defaultProviderKey);
	}

	public TinyEngineAiProvider getProvider(String key) {
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
}
