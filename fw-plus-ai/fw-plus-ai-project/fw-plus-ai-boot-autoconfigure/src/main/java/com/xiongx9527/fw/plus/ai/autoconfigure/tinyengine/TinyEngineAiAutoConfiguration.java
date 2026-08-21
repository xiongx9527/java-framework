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
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official.DefaultTinyEngineOpenAiCompatibleGateway;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official.DefaultTinyEngineOfficialRagGateway;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official.Sm4TinyEngineAiCredentialCipher;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official.TinyEngineAiCredentialCipher;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official.TinyEngineOpenAiCompatibleGateway;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official.TinyEngineOfficialRagGateway;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.provider.TinyEngineAiCapability;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.provider.TinyEngineAiProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.provider.TinyEngineAiProviderRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.TinyEngineAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webflux.TinyEngineReactiveExecutor;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreRegistry;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelInvocationConfig;
import com.xiongx9527.fw.plus.tinyengine.service.AppAiModelConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@AutoConfigureAfter(TinyEngineAutoConfiguration.class)
@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(TinyEngineAiProperties.class)
public class TinyEngineAiAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
	@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.ai.web", name = "enabled", havingValue = "true")
	public TinyEngineAiController tinyEngineAiController(ObjectProvider<TinyEngineAiChatService> chatServiceProvider,
			ObjectProvider<TinyEngineRagService> ragServiceProvider) {
		return new TinyEngineAiController(chatService(chatServiceProvider), ragService(ragServiceProvider));
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
	@ConditionalOnBean(TinyEngineReactiveExecutor.class)
	@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.ai.web", name = "enabled", havingValue = "true")
	public TinyEngineAiWebFluxController tinyEngineAiWebFluxController(
			ObjectProvider<TinyEngineAiChatService> chatServiceProvider,
			ObjectProvider<TinyEngineRagService> ragServiceProvider,
			TinyEngineReactiveExecutor executor) {
		return new TinyEngineAiWebFluxController(chatService(chatServiceProvider), ragService(ragServiceProvider), executor);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.ai.official", name = "enabled", havingValue = "true", matchIfMissing = true)
	public TinyEngineAiCredentialCipher tinyEngineAiCredentialCipher(TinyEngineAiProperties properties) {
		return Sm4TinyEngineAiCredentialCipher.from(properties.getOfficial());
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.ai.official", name = "enabled", havingValue = "true", matchIfMissing = true)
	public TinyEngineOpenAiCompatibleGateway tinyEngineOpenAiCompatibleGateway(TinyEngineAiProperties properties,
			ObjectProvider<ObjectMapper> objectMapperProvider) {
		ObjectMapper objectMapper = objectMapperProvider.getIfAvailable();
		return new DefaultTinyEngineOpenAiCompatibleGateway(properties, objectMapper == null ? new ObjectMapper() : objectMapper);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.ai.official", name = "enabled", havingValue = "true", matchIfMissing = true)
	public TinyEngineOfficialRagGateway tinyEngineOfficialRagGateway(
			ObjectProvider<TinyEngineRagService> ragServiceProvider,
			ObjectProvider<ObjectMapper> objectMapperProvider) {
		ObjectMapper objectMapper = objectMapperProvider.getIfAvailable();
		return new DefaultTinyEngineOfficialRagGateway(ragService(ragServiceProvider),
				objectMapper == null ? new ObjectMapper() : objectMapper);
	}

	@Bean
	@ConditionalOnMissingBean(TinyEngineAiChatService.class)
	@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.ai.chat", name = "enabled", havingValue = "true")
	public TinyEngineAiChatService tinyEngineAiChatService(TinyEngineAiProviderRegistry providerRegistry,
			TinyEngineAiProperties properties, ObjectProvider<AppAiModelConfigService> configServiceProvider) {
		String missingDependencyPolicy = normalizeMissingDependencyPolicy(properties.getMissingDependencyPolicy());
		String unavailableReason = chatUnavailableReason(providerRegistry);
		if (unavailableReason != null) {
			if (isDegrade(missingDependencyPolicy)) {
				return new UnavailableTinyEngineAiChatService(unavailableReason);
			}
			throw new IllegalStateException(unavailableReason);
		}
		return new DefaultTinyEngineAiChatService(providerRegistry, configServiceProvider.getIfAvailable());
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

	private TinyEngineAiChatService chatService(ObjectProvider<TinyEngineAiChatService> provider) {
		TinyEngineAiChatService service = provider.getIfAvailable();
		return service == null
				? new UnavailableTinyEngineAiChatService("TinyEngine AI chat is disabled")
				: service;
	}

	private TinyEngineRagService ragService(ObjectProvider<TinyEngineRagService> provider) {
		TinyEngineRagService service = provider.getIfAvailable();
		return service == null
				? new UnavailableTinyEngineRagService("TinyEngine AI RAG is disabled")
				: service;
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
		private final AppAiModelConfigService configService;

		private DefaultTinyEngineAiChatService(TinyEngineAiProviderRegistry providerRegistry,
				AppAiModelConfigService configService) {
			this.providerRegistry = providerRegistry;
			this.configService = configService;
		}

		@Override
		public TinyEngineChatResponse chat(TinyEngineChatRequest request) {
			AiModelInvocationConfig invocation = resolveInvocation(request);
			return provider(invocation, TinyEngineAiCapability.CHAT).chat(providerRequest(request, invocation));
		}

		@Override
		public List<TinyEngineChatStreamChunk> stream(TinyEngineChatRequest request) {
			AiModelInvocationConfig invocation = resolveInvocation(request);
			return provider(invocation, TinyEngineAiCapability.STREAMING_CHAT).stream(providerRequest(request, invocation));
		}

		private AiModelInvocationConfig resolveInvocation(TinyEngineChatRequest request) {
			if (request == null || request.getAppId() == null || request.getServiceId() == null
					|| isBlank(request.getModelName())) {
				throw new IllegalArgumentException("appId, serviceId and modelName are required");
			}
			if (configService == null) {
				throw new IllegalStateException("TinyEngine AI application model configuration service is unavailable");
			}
			return configService.resolveInvocation(request.getAppId(), request.getServiceId(), request.getModelName());
		}

		private TinyEngineAiProvider provider(AiModelInvocationConfig invocation, TinyEngineAiCapability capability) {
			TinyEngineAiProvider provider = providerRegistry.requireCapability(capability);
			if (!provider.supports(capability)) {
				throw new IllegalStateException("TinyEngine AI provider '" + provider.getKey()
						+ "' does not support capability '" + capability + "'");
			}
			return provider;
		}

		private TinyEngineChatRequest providerRequest(TinyEngineChatRequest request, AiModelInvocationConfig invocation) {
			TinyEngineChatRequest target = new TinyEngineChatRequest();
			target.setAppId(request.getAppId());
			target.setServiceId(request.getServiceId());
			target.setProvider(invocation.getProvider());
			target.setModelName(invocation.getModelName());
			target.setModelKey(invocation.getModelName());
			target.setModel(invocation.getModelName());
			target.setBaseUrl(invocation.getBaseUrl());
			target.setApiKey(invocation.getApiKey());
			target.setModelSettings(invocation.getSettings());
			target.setMessage(request.getMessage());
			target.setMessages(request.getMessages());
			target.setStream(request.getStream());
			target.setMaxTokens(request.getMaxTokens());
			target.setTemperature(request.getTemperature());
			target.setTopP(request.getTopP());
			target.setPresencePenalty(request.getPresencePenalty());
			target.setFrequencyPenalty(request.getFrequencyPenalty());
			target.setResponseFormat(request.getResponseFormat());
			target.setEnableThinking(request.getEnableThinking());
			target.setToolChoice(request.getToolChoice());
			target.setParallelToolCalls(request.getParallelToolCalls());
			target.setEnableSearch(request.getEnableSearch());
			target.setStreamOptions(request.getStreamOptions());
			target.setSearchOptions(request.getSearchOptions());
			target.setTools(request.getTools());
			return target;
		}

		private boolean isBlank(String value) {
			return value == null || value.trim().length() == 0;
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
