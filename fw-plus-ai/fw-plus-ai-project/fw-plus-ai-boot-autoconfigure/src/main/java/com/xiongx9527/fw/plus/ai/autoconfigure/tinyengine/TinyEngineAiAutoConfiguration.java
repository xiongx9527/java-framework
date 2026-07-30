package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineAiChatService;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineKnowledgeBaseService;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineRagService;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(TinyEngineAiProperties.class)
public class TinyEngineAiAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(TinyEngineAiChatService.class)
	@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.ai.chat", name = "enabled", havingValue = "true")
	public TinyEngineAiChatService tinyEngineAiChatService() {
		return new DefaultTinyEngineAiChatService();
	}

	@Bean
	@ConditionalOnMissingBean(TinyEngineRagService.class)
	@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.ai.rag", name = "enabled", havingValue = "true")
	public TinyEngineRagService tinyEngineRagService(TinyEngineAiProperties properties,
			ObjectProvider<AiModelRegistry> modelRegistryProvider,
			ObjectProvider<AiEmbeddingStoreRegistry> storeRegistryProvider) {
		String unavailableReason = ragUnavailableReason(properties, modelRegistryProvider.getIfAvailable(),
				storeRegistryProvider.getIfAvailable());
		if (unavailableReason != null) {
			if (isDegrade(properties)) {
				return new UnavailableTinyEngineRagService(unavailableReason);
			}
			throw new IllegalStateException(unavailableReason);
		}
		return new DefaultTinyEngineRagService();
	}

	@Bean
	@ConditionalOnMissingBean(TinyEngineKnowledgeBaseService.class)
	@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.ai.rag", name = "enabled", havingValue = "true")
	public TinyEngineKnowledgeBaseService tinyEngineKnowledgeBaseService() {
		return new DefaultTinyEngineKnowledgeBaseService();
	}

	private String ragUnavailableReason(TinyEngineAiProperties properties, AiModelRegistry modelRegistry,
			AiEmbeddingStoreRegistry storeRegistry) {
		validatePolicy(properties);
		if (!containsModel(modelRegistry, modelKey(properties, modelRegistry))
				|| !containsStore(storeRegistry, storeKey(properties, storeRegistry))) {
			return "TinyEngine AI RAG requires both AiModelRegistry and AiEmbeddingStoreRegistry; "
					+ "requires an available embedding model and embedding store";
		}
		return null;
	}

	private void validatePolicy(TinyEngineAiProperties properties) {
		String policy = properties.getMissingDependencyPolicy();
		if (policy == null || "fail-fast".equalsIgnoreCase(policy.trim()) || "degrade".equalsIgnoreCase(policy.trim())) {
			return;
		}
		throw new IllegalStateException("Unsupported TinyEngine AI missing dependency policy: " + policy);
	}

	private boolean isDegrade(TinyEngineAiProperties properties) {
		return "degrade".equalsIgnoreCase(properties.getMissingDependencyPolicy());
	}

	private boolean containsModel(AiModelRegistry registry, String key) {
		return registry != null && !isBlank(key) && registry.contains(key);
	}

	private boolean containsStore(AiEmbeddingStoreRegistry registry, String key) {
		return registry != null && !isBlank(key) && registry.contains(key);
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
	}

	private static final class DefaultTinyEngineRagService implements TinyEngineRagService {
		@Override
		public boolean isAvailable() {
			return true;
		}

		@Override
		public String getUnavailableReason() {
			return null;
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
	}

	private static final class DefaultTinyEngineKnowledgeBaseService implements TinyEngineKnowledgeBaseService {
	}
}
