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
		validateRagDependencies(properties, modelRegistryProvider.getIfAvailable(), storeRegistryProvider.getIfAvailable());
		return new DefaultTinyEngineRagService();
	}

	@Bean
	@ConditionalOnMissingBean(TinyEngineKnowledgeBaseService.class)
	@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.ai.rag", name = "enabled", havingValue = "true")
	public TinyEngineKnowledgeBaseService tinyEngineKnowledgeBaseService() {
		return new DefaultTinyEngineKnowledgeBaseService();
	}

	private void validateRagDependencies(TinyEngineAiProperties properties, AiModelRegistry modelRegistry,
			AiEmbeddingStoreRegistry storeRegistry) {
		if (isFailFast(properties) && (modelRegistry == null || storeRegistry == null)) {
			throw new IllegalStateException("TinyEngine AI RAG requires both AiModelRegistry and AiEmbeddingStoreRegistry");
		}
	}

	private boolean isFailFast(TinyEngineAiProperties properties) {
		String policy = properties.getMissingDependencyPolicy();
		return policy == null || "fail-fast".equalsIgnoreCase(policy.trim());
	}

	private static final class DefaultTinyEngineAiChatService implements TinyEngineAiChatService {
	}

	private static final class DefaultTinyEngineRagService implements TinyEngineRagService {
	}

	private static final class DefaultTinyEngineKnowledgeBaseService implements TinyEngineKnowledgeBaseService {
	}
}
