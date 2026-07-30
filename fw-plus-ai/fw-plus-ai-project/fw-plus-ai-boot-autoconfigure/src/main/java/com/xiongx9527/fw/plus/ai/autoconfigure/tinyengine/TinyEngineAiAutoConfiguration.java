package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineAiChatService;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineKnowledgeBaseService;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineRagService;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.DefaultTinyEngineKnowledgeBaseService;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.DefaultTinyEngineRagService;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineRagRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineRagSearchResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatResponse;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatStreamChunk;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.provider.LangChain4jTinyEngineAiProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.provider.TinyEngineAiCapability;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.provider.TinyEngineAiProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.provider.TinyEngineAiProviderRegistry;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(TinyEngineAiProperties.class)
public class TinyEngineAiAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(TinyEngineAiChatService.class)
	@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.ai.chat", name = "enabled", havingValue = "true")
	public TinyEngineAiChatService tinyEngineAiChatService(TinyEngineAiProviderRegistry providerRegistry,
			TinyEngineAiProperties properties) {
		String missingDependencyPolicy = normalizeMissingDependencyPolicy(properties.getMissingDependencyPolicy());
		String unavailableReason = chatUnavailableReason(providerRegistry);
		if (unavailableReason != null) {
			if (isDegrade(missingDependencyPolicy)) {
				return new UnavailableTinyEngineAiChatService(unavailableReason);
			}
			throw new IllegalStateException(unavailableReason);
		}
		return new DefaultTinyEngineAiChatService(providerRegistry);
	}

	@Bean
	@ConditionalOnMissingBean(TinyEngineAiProvider.class)
	public TinyEngineAiProvider langChain4jTinyEngineAiProvider(ObjectProvider<AiModelRegistry> modelRegistryProvider,
			TinyEngineAiProperties properties) {
		return new LangChain4jTinyEngineAiProvider(modelRegistryProvider.getIfAvailable(), properties);
	}

	@Bean
	@ConditionalOnMissingBean(TinyEngineAiProviderRegistry.class)
	public TinyEngineAiProviderRegistry tinyEngineAiProviderRegistry(List<TinyEngineAiProvider> providers,
			TinyEngineAiProperties properties) {
		return new TinyEngineAiProviderRegistry(providers, properties.getProvider());
	}

	@Bean
	@ConditionalOnMissingBean(TinyEngineRagService.class)
	@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.ai.rag", name = "enabled", havingValue = "true")
	public TinyEngineRagService tinyEngineRagService(TinyEngineAiProperties properties,
			ObjectProvider<AiModelRegistry> modelRegistryProvider,
			ObjectProvider<AiEmbeddingStoreRegistry> storeRegistryProvider) {
		String missingDependencyPolicy = normalizeMissingDependencyPolicy(properties.getMissingDependencyPolicy());
		String unavailableReason = ragUnavailableReason(properties, modelRegistryProvider.getIfAvailable(),
				storeRegistryProvider.getIfAvailable());
		if (unavailableReason != null) {
			if (isDegrade(missingDependencyPolicy)) {
				return new UnavailableTinyEngineRagService(unavailableReason);
			}
			throw new IllegalStateException(unavailableReason);
		}
		return new DefaultTinyEngineRagService(storeRegistryProvider.getIfAvailable(),
				storeKey(properties, storeRegistryProvider.getIfAvailable()), properties.getRag().getMaxResults(),
				properties.getRag().getMinScore());
	}

	@Bean
	@ConditionalOnMissingBean(TinyEngineKnowledgeBaseService.class)
	@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.ai.rag", name = "enabled", havingValue = "true")
	public TinyEngineKnowledgeBaseService tinyEngineKnowledgeBaseService(TinyEngineAiProperties properties,
			ObjectProvider<AiEmbeddingStoreRegistry> storeRegistryProvider) {
		AiEmbeddingStoreRegistry registry = storeRegistryProvider.getIfAvailable();
		return new DefaultTinyEngineKnowledgeBaseService(registry, storeKey(properties, registry),
				properties.getRag().getChunkSize(), properties.getRag().getChunkOverlap());
	}

	private String ragUnavailableReason(TinyEngineAiProperties properties, AiModelRegistry modelRegistry,
			AiEmbeddingStoreRegistry storeRegistry) {
		if (!containsModel(modelRegistry, modelKey(properties, modelRegistry))
				|| !containsStore(storeRegistry, storeKey(properties, storeRegistry))) {
			return "TinyEngine AI RAG requires both AiModelRegistry and AiEmbeddingStoreRegistry; "
					+ "requires an available embedding model and embedding store";
		}
		return null;
	}

	private String chatUnavailableReason(TinyEngineAiProviderRegistry providerRegistry) {
		try {
			providerRegistry.requireCapability(TinyEngineAiCapability.CHAT);
			return null;
		} catch (RuntimeException ex) {
			return "TinyEngine AI chat provider is unavailable: " + ex.getMessage();
		}
	}

	private String normalizeMissingDependencyPolicy(String policy) {
		String normalizedPolicy = policy == null ? null : policy.trim();
		if ("fail-fast".equalsIgnoreCase(normalizedPolicy) || "degrade".equalsIgnoreCase(normalizedPolicy)) {
			return normalizedPolicy;
		}
		throw new IllegalStateException("Unsupported TinyEngine AI missing dependency policy: " + policy);
	}

	private boolean isDegrade(String missingDependencyPolicy) {
		return "degrade".equalsIgnoreCase(missingDependencyPolicy);
	}

	private boolean containsModel(AiModelRegistry registry, String key) {
		return registry != null && !isBlank(key) && registry.contains(key) && registry.get(key) != null;
	}

	private boolean containsStore(AiEmbeddingStoreRegistry registry, String key) {
		return registry != null && !isBlank(key) && registry.contains(key) && registry.get(key) != null;
	}

	private String modelKey(TinyEngineAiProperties properties, AiModelRegistry registry) {
		String embeddingKey = properties.getModel().getEmbeddingKey();
		if (!isBlank(embeddingKey)) {
			return embeddingKey;
		}
		String chatKey = properties.getModel().getChatKey();
		return !isBlank(chatKey) ? chatKey : registry == null ? null : registry.getDefaultKey();
	}

	private String storeKey(TinyEngineAiProperties properties, AiEmbeddingStoreRegistry registry) {
		String vectorStoreKey = properties.getVector().getStoreKey();
		if (!isBlank(vectorStoreKey)) {
			return vectorStoreKey;
		}
		String configuredStoreKey = properties.getStore().getStoreKey();
		return !isBlank(configuredStoreKey) ? configuredStoreKey : registry == null ? null : registry.getDefaultKey();
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}

	private static final class DefaultTinyEngineAiChatService implements TinyEngineAiChatService {
		private final TinyEngineAiProviderRegistry providerRegistry;

		private DefaultTinyEngineAiChatService(TinyEngineAiProviderRegistry providerRegistry) {
			this.providerRegistry = providerRegistry;
		}

		@Override
		public TinyEngineChatResponse chat(TinyEngineChatRequest request) {
			return providerRegistry.requireCapability(TinyEngineAiCapability.CHAT).chat(request);
		}

		@Override
		public List<TinyEngineChatStreamChunk> stream(TinyEngineChatRequest request) {
			return providerRegistry.requireCapability(TinyEngineAiCapability.STREAMING_CHAT).stream(request);
		}
	}

	private static final class UnavailableTinyEngineAiChatService implements TinyEngineAiChatService {
		private final String unavailableReason;

		private UnavailableTinyEngineAiChatService(String unavailableReason) {
			this.unavailableReason = unavailableReason;
		}

		@Override
		public boolean isAvailable() {
			return false;
		}

		@Override
		public String getUnavailableReason() {
			return unavailableReason;
		}

		@Override
		public TinyEngineChatResponse chat(TinyEngineChatRequest request) {
			throw new IllegalStateException(unavailableReason);
		}

		@Override
		public List<TinyEngineChatStreamChunk> stream(TinyEngineChatRequest request) {
			throw new IllegalStateException(unavailableReason);
		}
	}

	private static final class UnavailableTinyEngineRagService implements TinyEngineRagService {
		private final String unavailableReason;

		private UnavailableTinyEngineRagService(String unavailableReason) {
			this.unavailableReason = unavailableReason;
		}

		@Override
		public boolean isAvailable() {
			return false;
		}

		@Override
		public String getUnavailableReason() {
			return unavailableReason;
		}

		@Override
		public TinyEngineRagSearchResult search(TinyEngineRagRequest request) {
			throw new IllegalStateException(unavailableReason);
		}
	}
}
