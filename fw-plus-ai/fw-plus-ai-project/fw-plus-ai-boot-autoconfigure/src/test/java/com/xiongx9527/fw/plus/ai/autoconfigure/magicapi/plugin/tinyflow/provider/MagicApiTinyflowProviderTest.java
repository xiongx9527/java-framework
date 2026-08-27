package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.provider;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.tinyflow.MagicApiTinyflowLangChain4jAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.tinyflow.MagicApiTinyflowModelProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.MagicDynamicModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.model.MagicDynamicVectorDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.starter.MagicAPITinyflowConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow.TinyflowAutoConfiguration;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jModelRegistry;
import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jAiModel;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jModelType;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreRegistry;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreType;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagicApiTinyflowProviderTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MagicAPITinyflowConfiguration.class,
					MagicApiTinyflowLangChain4jAutoConfiguration.class, TinyflowAutoConfiguration.class));

	@Test
	void shouldMergeSpringModelRegistryAndExposeOnlyCapabilities() {
		Map<String, AiModel> models = new LinkedHashMap<String, AiModel>();
		models.put("chat-main", nativeModel("chat-main", AiModelType.CHAT, new dev.langchain4j.model.chat.ChatLanguageModel() {
			@Override public dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> generate(java.util.List<dev.langchain4j.data.message.ChatMessage> messages) { return null; }
		}));
		models.put("embedding-main", nativeModel("embedding-main", AiModelType.EMBEDDING, new dev.langchain4j.model.embedding.EmbeddingModel() {
			@Override public dev.langchain4j.model.output.Response<java.util.List<dev.langchain4j.data.embedding.Embedding>> embedAll(java.util.List<dev.langchain4j.data.segment.TextSegment> segments) { return null; }
		}));
		models.put("plain-chat", model("plain-chat", AiModelType.CHAT));
		MagicApiTinyflowModelProvider provider = new MagicApiTinyflowModelProvider(
				new MagicDynamicModelDataSource(new ModelRegistry(models), null, Collections.emptyList()));

		assertEquals("chat-main", provider.listChatModels().get(0).getKey());
		assertEquals("embedding-main", provider.listEmbeddingModels().get(0).getKey());
		assertEquals(1, provider.listChatModels().size());
		assertEquals(1, provider.listEmbeddingModels().size());
		assertSame(provider.getChatModel("chat-main"), provider.getChatModel(provider.listChatModels().get(0).getKey()));
		assertFalse(provider.listChatModels().stream().anyMatch(model -> "plain-chat".equals(model.getKey())));
		assertFalse(String.valueOf(provider.listChatModels()).contains("apiKey"));
	}

	@Test
	void shouldRegisterMagicDynamicModelsBeforeTheDefaultTinyflowProvider() {
		dev.langchain4j.model.chat.ChatLanguageModel chatModel = new dev.langchain4j.model.chat.ChatLanguageModel() {
			@Override public dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> generate(
					java.util.List<dev.langchain4j.data.message.ChatMessage> messages) { return null; }
		};
		dev.langchain4j.model.embedding.EmbeddingModel embeddingModel = new dev.langchain4j.model.embedding.EmbeddingModel() {
			@Override public dev.langchain4j.model.output.Response<java.util.List<dev.langchain4j.data.embedding.Embedding>> embedAll(
					java.util.List<dev.langchain4j.data.segment.TextSegment> segments) { return null; }
		};
		Map<String, AiModel> models = new LinkedHashMap<String, AiModel>();
		models.put("chat-main", nativeModel("chat-main", AiModelType.CHAT, chatModel));
		models.put("embedding-main", nativeModel("embedding-main", AiModelType.EMBEDDING, embeddingModel));
		MagicDynamicModelDataSource dataSource = new MagicDynamicModelDataSource(new ModelRegistry(models), null,
				Collections.emptyList());

		contextRunner.withBean(MagicResourceService.class, this::magicResourceService)
				.withBean(MagicDynamicModelDataSource.class, () -> dataSource)
				.run(context -> {
					TinyflowModelProvider provider = context.getBean(TinyflowModelProvider.class);
					assertTrue(provider instanceof MagicApiTinyflowModelProvider);
					assertEquals("chat-main", provider.listChatModels().get(0).getKey());
					assertEquals("embedding-main", provider.listEmbeddingModels().get(0).getKey());
					assertSame(chatModel, provider.getChatModel("chat-main"));
					assertTrue(provider.getEmbeddingModel("embedding-main") != null);
				});
	}

	@Test
	void shouldBackOffMagicTinyflowModelIntegrationWhenLangChain4jAdapterIsMissing() {
		contextRunner.withClassLoader(new FilteredClassLoader(LangChain4jModelRegistry.class))
				.withBean(MagicResourceService.class, this::magicResourceService)
				.withBean(MagicDynamicModelDataSource.class,
						() -> new MagicDynamicModelDataSource(new ModelRegistry(Collections.<String, AiModel>emptyMap()), null,
								Collections.emptyList()))
				.run(context -> {
					assertTrue(context.getStartupFailure() == null);
					assertTrue(context.getBeansOfType(TinyflowModelProvider.class).isEmpty());
				});
	}

	private AiModel nativeModel(String key, AiModelType type, Object nativeModel) {
		AiModelDefinition definition = new AiModelDefinition();
		definition.setKey(key);
		definition.setFramework("langchain4j");
		definition.setProvider("test");
		definition.setType(type);
		LangChain4jModelType nativeType = type == AiModelType.CHAT ? LangChain4jModelType.CHAT : LangChain4jModelType.EMBEDDING;
		return new LangChain4jAiModel(definition, new LangChain4jModel("test", nativeType, nativeModel));
	}

	private MagicResourceService magicResourceService() {
		return (MagicResourceService) Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class<?>[] { MagicResourceService.class }, (proxy, method, args) -> null);
	}

	@Test
	void shouldMergeSpringVectorRegistryWithoutExposingStoreConfiguration() {
		Map<String, AiEmbeddingStore> stores = new LinkedHashMap<String, AiEmbeddingStore>();
		stores.put("vector-main", store("vector-main"));
		MagicApiTinyflowVectorStoreProvider provider = new MagicApiTinyflowVectorStoreProvider(
				new MagicDynamicVectorDataSource(new VectorRegistry(stores), null, Collections.emptyList()));

		assertEquals("vector-main", provider.list().get(0).getKey());
		assertFalse(String.valueOf(provider.list()).contains("headers"));
	}

	private AiModel model(final String key, final AiModelType type) {
		return new AiModel() {
			@Override public String getKey() { return key; }
			@Override public String getFramework() { return "test"; }
			@Override public String getProvider() { return "test"; }
			@Override public AiModelType getType() { return type; }
		};
	}

	private AiEmbeddingStore store(final String key) {
		return new AiEmbeddingStore() {
			@Override public String getKey() { return key; }
			@Override public String getFramework() { return "test"; }
			@Override public String getProvider() { return "test"; }
			@Override public AiEmbeddingStoreType getType() { return AiEmbeddingStoreType.VECTOR; }
			@Override public java.util.List<String> addAll(java.util.List<com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingDocument> documents) { return Collections.emptyList(); }
			@Override public String add(com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingDocument document) { return document.getId(); }
			@Override public void removeAll(java.util.List<String> ids) { }
			@Override public void remove(String id) { }
			@Override public com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingSearchResult search(com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingQuery query) { return null; }
		};
	}

	private static final class ModelRegistry implements AiModelRegistry {
		private final Map<String, AiModel> models;
		private ModelRegistry(Map<String, AiModel> models) { this.models = models; }
		@Override public AiModel get(String key) { return models.get(key); }
		@Override public AiModel getDefault() { return models.get("chat-main"); }
		@Override public String getDefaultKey() { return "chat-main"; }
		@Override public boolean contains(String key) { return models.containsKey(key); }
		@Override public Set<String> keys() { return models.keySet(); }
	}

	private static final class VectorRegistry implements AiEmbeddingStoreRegistry {
		private final Map<String, AiEmbeddingStore> stores;
		private VectorRegistry(Map<String, AiEmbeddingStore> stores) { this.stores = stores; }
		@Override public AiEmbeddingStore get(String key) { return stores.get(key); }
		@Override public AiEmbeddingStore getDefault() { return stores.get("vector-main"); }
		@Override public String getDefaultKey() { return "vector-main"; }
		@Override public boolean contains(String key) { return stores.containsKey(key); }
		@Override public Set<String> keys() { return stores.keySet(); }
	}
}
