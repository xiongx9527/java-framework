package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataErrorCode;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRequest;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRoutePolicy;
import com.xiongx9527.fw.plus.ai.langchain4j.model.DefaultLangChain4jModelRegistry;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelRegistry;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelType;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

public class LangChain4jAskDataModelRegistryTest {

	@Test
	public void shouldUseOllamaDefaultLogicalKeysForChatAndEmbeddingModels() {
		AskDataProperties properties = new AskDataProperties();
		Map<String, ChatLanguageModel> chatModels = new HashMap<String, ChatLanguageModel>();
		chatModels.put("qwen3:32b", chatModel("ollama-chat"));
		Map<String, EmbeddingModel> embeddingModels = new HashMap<String, EmbeddingModel>();
		embeddingModels.put("bge-m3", embeddingModel(0.1F, 0.2F));

		AskDataModelRegistry registry = registry(properties, modelRegistry(chatModels, embeddingModels));

		assertEquals("qwen3:32b", registry.getDefaultChatModelKey());
		assertEquals("bge-m3", registry.getDefaultEmbeddingModelKey());
		assertEquals("ollama-chat", registry.getChatModel(null).generate("question").getText());
		assertEquals(java.util.Arrays.asList(0.1F, 0.2F), registry.getEmbeddingModel(null).embed("question"));
	}

	@Test
	public void shouldSelectRequestLevelChatModelWhenModelKeyExists() {
		AskDataProperties properties = new AskDataProperties();
		properties.getModel().setDefaultChatModel("default");
		Map<String, ChatLanguageModel> models = new HashMap<String, ChatLanguageModel>();
		models.put("default", chatModel("default-sql"));
		models.put("fast", chatModel("fast-sql"));

		AskDataModelRegistry registry = registry(properties, modelRegistry(models, new HashMap<String, EmbeddingModel>()));

		assertEquals("fast-sql", registry.getChatModel("fast").generate("question").getText());
	}

	@Test
	public void shouldSelectDefaultChatModelWhenRequestModelKeyIsBlank() {
		AskDataProperties properties = new AskDataProperties();
		properties.getModel().setDefaultChatModel("default");
		Map<String, ChatLanguageModel> models = new HashMap<String, ChatLanguageModel>();
		models.put("default", chatModel("default-sql"));

		AskDataModelRegistry registry = registry(properties, modelRegistry(models, new HashMap<String, EmbeddingModel>()));

		assertEquals("default", registry.getDefaultChatModelKey());
		assertEquals("default-sql", registry.getChatModel("  ").generate("question").getText());
	}

	@Test
	public void shouldFallbackToNextCandidateAfterInfrastructureFailureAndKeepAuditRecords() {
		AskDataProperties properties = new AskDataProperties();
		properties.getModel().setDefaultChatModel("fallback");
		Map<String, ChatLanguageModel> models = new HashMap<String, ChatLanguageModel>();
		models.put("primary", failingChatModel(new RuntimeException(new TimeoutException("ollama timeout"))));
		models.put("fallback", chatModel("fallback-response"));
		LangChain4jAskDataModelRegistry registry = (LangChain4jAskDataModelRegistry) registry(properties,
				modelRegistry(models, new HashMap<String, EmbeddingModel>()));

		AskDataModelResponse response = registry.generate(request("primary", "fallback"), "question");

		assertEquals("fallback-response", response.getText());
		assertEquals("fallback", response.getModelKey());
		assertEquals(2, registry.getRouteAuditRecords().size());
		assertEquals("primary", registry.getRouteAuditRecords().get(0).getModelKey());
		assertEquals(AskDataModelRoutePolicy.REQUEST_MODEL_KEY,
				registry.getRouteAuditRecords().get(0).getRoutePolicy());
		assertEquals("MODEL_INFRASTRUCTURE_FAILURE",
				registry.getRouteAuditRecords().get(0).getErrorCode());
		assertEquals("fallback", registry.getRouteAuditRecords().get(1).getModelKey());
		assertEquals(AskDataModelRoutePolicy.DEFAULT_KEY,
				registry.getRouteAuditRecords().get(1).getRoutePolicy());
	}

	@Test
	public void shouldUseConfiguredChatCandidatesForDefaultRouteAndFallback() {
		AskDataProperties properties = new AskDataProperties();
		properties.getModel().setProvider("ollama");
		properties.getModel().setChatCandidates(java.util.Arrays.asList("primary", "fallback"));
		Map<String, ChatLanguageModel> models = new HashMap<String, ChatLanguageModel>();
		models.put("primary", failingChatModel(new RuntimeException(new TimeoutException("ollama timeout"))));
		models.put("fallback", chatModel("fallback-response"));
		LangChain4jAskDataModelRegistry registry = (LangChain4jAskDataModelRegistry) registry(properties,
				modelRegistry(models, new HashMap<String, EmbeddingModel>()));

		AskDataModelResponse response = registry.generate(new AskDataProjectContext("project-9527", "operator-9527"),
				"question");

		assertEquals("fallback", response.getModelKey());
		assertEquals("ollama", registry.getRouteAuditRecords().get(0).getProvider());
	}

	@Test
	public void shouldNotFallbackWhenDisabledAfterTimeoutFailure() {
		AskDataProperties properties = new AskDataProperties();
		properties.getModel().setFallbackEnabled(false);
		properties.getModel().setChatCandidates(java.util.Arrays.asList("primary", "fallback"));
		Map<String, ChatLanguageModel> models = new HashMap<String, ChatLanguageModel>();
		models.put("primary", failingChatModel(new RuntimeException(new TimeoutException("ollama timeout"))));
		models.put("fallback", chatModel("fallback-response"));
		LangChain4jAskDataModelRegistry registry = (LangChain4jAskDataModelRegistry) registry(properties,
				modelRegistry(models, new HashMap<String, EmbeddingModel>()));

		try {
			registry.generate(new AskDataProjectContext("project-9527", "operator-9527"), "question");
			fail("关闭兜底后超时错误不能切换模型");
		} catch (AskDataModelException ex) {
			assertEquals("MODEL_INFRASTRUCTURE_FAILURE", ex.getErrorCode());
		}
		assertEquals(1, registry.getRouteAuditRecords().size());
	}

	@Test
	public void shouldNotFallbackAfterOrdinaryRuntimeException() {
		AskDataProperties properties = new AskDataProperties();
		properties.getModel().setChatCandidates(java.util.Arrays.asList("primary", "fallback"));
		Map<String, ChatLanguageModel> models = new HashMap<String, ChatLanguageModel>();
		models.put("primary", failingChatModel(new IllegalStateException("unexpected failure")));
		models.put("fallback", chatModel("fallback-response"));
		LangChain4jAskDataModelRegistry registry = (LangChain4jAskDataModelRegistry) registry(properties,
				modelRegistry(models, new HashMap<String, EmbeddingModel>()));

		try {
			registry.generate(new AskDataProjectContext("project-9527", "operator-9527"), "question");
			fail("普通运行时错误不能切换模型");
		} catch (AskDataModelException ex) {
			assertEquals("MODEL_REQUEST_REJECTED", ex.getErrorCode());
		}
		assertEquals(1, registry.getRouteAuditRecords().size());
	}

	@Test
	public void shouldKeepOnlyConfiguredNumberOfRouteAuditRecords() {
		AskDataProperties properties = new AskDataProperties();
		properties.getModel().setRouteAuditRecordCapacity(1);
		properties.getModel().setChatCandidates(java.util.Arrays.asList("first", "second"));
		Map<String, ChatLanguageModel> models = new HashMap<String, ChatLanguageModel>();
		models.put("first", chatModel("first-response"));
		models.put("second", chatModel("second-response"));
		LangChain4jAskDataModelRegistry registry = (LangChain4jAskDataModelRegistry) registry(properties,
				modelRegistry(models, new HashMap<String, EmbeddingModel>()));

		registry.generate(new AskDataProjectContext("project-1", "operator-1"), "question");
		properties.getModel().setChatCandidates(java.util.Arrays.asList("second"));
		registry.generate(new AskDataProjectContext("project-2", "operator-2"), "question");

		assertEquals(1, registry.getRouteAuditRecords().size());
		assertEquals("second", registry.getRouteAuditRecords().get(0).getModelKey());
	}

	@Test
	public void shouldNotSwitchModelAfterNonRetryableFailure() {
		AskDataProperties properties = new AskDataProperties();
		properties.getModel().setDefaultChatModel("fallback");
		Map<String, ChatLanguageModel> models = new HashMap<String, ChatLanguageModel>();
		models.put("primary", failingChatModel(new IllegalArgumentException("请求参数不合法")));
		models.put("fallback", chatModel("fallback-response"));
		LangChain4jAskDataModelRegistry registry = (LangChain4jAskDataModelRegistry) registry(properties,
				modelRegistry(models, new HashMap<String, EmbeddingModel>()));

		try {
			registry.generate(request("primary", "fallback"), "question");
			fail("不可重试错误不能切换到候选模型");
		} catch (AskDataModelException ex) {
			assertEquals("MODEL_REQUEST_REJECTED", ex.getErrorCode());
		}
		assertEquals(1, registry.getRouteAuditRecords().size());
		assertFalse(registry.getRouteAuditRecords().get(0).isRetryable());
	}

	@Test
	public void shouldRejectMissingRequestLevelChatModel() {
		AskDataProperties properties = new AskDataProperties();
		properties.getModel().setDefaultChatModel("default");
		AskDataModelRegistry registry = registry(properties,
				modelRegistry(new HashMap<String, ChatLanguageModel>(), new HashMap<String, EmbeddingModel>()));

		try {
			registry.getChatModel("missing");
			fail("缺失的请求模型必须抛出异常");
		} catch (AskDataModelException ex) {
			assertEquals(AskDataErrorCode.MODEL_NOT_FOUND, ex.getErrorCode());
		}
	}

	@Test
	public void shouldRejectMissingDefaultChatModel() {
		AskDataProperties properties = new AskDataProperties();
		properties.getModel().setDefaultChatModel(null);
		AskDataModelRegistry registry = new LangChain4jAskDataModelRegistry(properties,
				Collections.<String, ChatLanguageModel>emptyMap(),
				Collections.<String, EmbeddingModel>emptyMap(),
				Collections.<LangChain4jAskDataModelSource>emptyList());

		try {
			registry.getChatModel(null);
			fail("未配置默认模型必须抛出异常");
		} catch (AskDataModelException ex) {
			assertEquals(AskDataErrorCode.DEFAULT_MODEL_NOT_CONFIGURED, ex.getErrorCode());
		}
	}

	@Test
	public void shouldAdaptEmbeddingModelToFloatList() {
		AskDataProperties properties = new AskDataProperties();
		properties.getModel().setDefaultEmbeddingModel("embedding-default");
		Map<String, EmbeddingModel> models = new HashMap<String, EmbeddingModel>();
		models.put("embedding-default", embeddingModel(0.1F, 0.2F));

		AskDataModelRegistry registry = registry(properties, modelRegistry(new HashMap<String, ChatLanguageModel>(), models));

		assertEquals("embedding-default", registry.getDefaultEmbeddingModelKey());
		assertEquals(java.util.Arrays.asList(0.1F, 0.2F), registry.getEmbeddingModel(null).embed("question"));
	}

	@Test
	public void shouldUseConfiguredEmbeddingCandidateWhenModelKeyIsBlank() {
		AskDataProperties properties = new AskDataProperties();
		properties.getModel().setDefaultEmbeddingModel("embedding-default");
		properties.getModel().setEmbeddingCandidates(java.util.Arrays.asList("embedding-priority",
				"embedding-default"));
		Map<String, EmbeddingModel> models = new HashMap<String, EmbeddingModel>();
		models.put("embedding-priority", embeddingModel(0.3F, 0.5F));
		models.put("embedding-default", embeddingModel(0.1F, 0.2F));

		AskDataModelRegistry registry = registry(properties,
				modelRegistry(new HashMap<String, ChatLanguageModel>(), models));

		assertEquals(java.util.Arrays.asList(0.3F, 0.5F), registry.getEmbeddingModel(null).embed("question"));
	}

	@Test
	public void shouldMergeDirectSpringBeansWithModelRegistryModels() {
		AskDataProperties properties = new AskDataProperties();
		properties.getModel().setDefaultChatModel("direct");
		properties.getModel().setDefaultEmbeddingModel("managed");
		Map<String, ChatLanguageModel> directChatModels = new HashMap<String, ChatLanguageModel>();
		directChatModels.put("direct", chatModel("direct-response"));
		Map<String, EmbeddingModel> managedEmbeddingModels = new HashMap<String, EmbeddingModel>();
		managedEmbeddingModels.put("managed", embeddingModel(0.4F, 0.6F));

		AskDataModelRegistry registry = new LangChain4jAskDataModelRegistry(properties, directChatModels,
				Collections.<String, EmbeddingModel>emptyMap(), Collections.<LangChain4jAskDataModelSource>singletonList(
						new LangChain4jModelRegistryAskDataModelSource(modelRegistry(
								Collections.<String, ChatLanguageModel>emptyMap(), managedEmbeddingModels))));

		assertEquals("direct-response", registry.getChatModel(null).generate("question").getText());
		assertEquals(java.util.Arrays.asList(0.4F, 0.6F), registry.getEmbeddingModel(null).embed("question"));
	}

	private ChatLanguageModel chatModel(final String response) {
		return (ChatLanguageModel) Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class<?>[] {ChatLanguageModel.class}, new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) {
						if ("generate".equals(method.getName()) && method.getReturnType() == String.class) {
							return response;
						}
						throw new UnsupportedOperationException(method.toString());
					}
				});
	}

	private ChatLanguageModel failingChatModel(final RuntimeException exception) {
		return (ChatLanguageModel) Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class<?>[] {ChatLanguageModel.class}, new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) {
						if ("generate".equals(method.getName())) {
							throw exception;
						}
						throw new UnsupportedOperationException(method.toString());
					}
				});
	}

	private EmbeddingModel embeddingModel(final float... values) {
		return (EmbeddingModel) Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class<?>[] {EmbeddingModel.class}, new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) {
						if ("embed".equals(method.getName())) {
							return Response.from(Embedding.from(values));
						}
						throw new UnsupportedOperationException(method.toString());
					}
				});
	}

	private LangChain4jModelRegistry modelRegistry(Map<String, ChatLanguageModel> chatModels,
			Map<String, EmbeddingModel> embeddingModels) {
		Map<String, LangChain4jModel> models = new HashMap<String, LangChain4jModel>();
		for (Map.Entry<String, ChatLanguageModel> entry : chatModels.entrySet()) {
			models.put(entry.getKey(), new LangChain4jModel("test", LangChain4jModelType.CHAT, entry.getValue()));
		}
		for (Map.Entry<String, EmbeddingModel> entry : embeddingModels.entrySet()) {
			models.put(entry.getKey(), new LangChain4jModel("test", LangChain4jModelType.EMBEDDING, entry.getValue()));
		}
		return new DefaultLangChain4jModelRegistry(models, null);
	}

	private AskDataModelRegistry registry(AskDataProperties properties, LangChain4jModelRegistry modelRegistry) {
		return new LangChain4jAskDataModelRegistry(properties,
				Collections.<String, ChatLanguageModel>emptyMap(),
				Collections.<String, EmbeddingModel>emptyMap(),
				Collections.<LangChain4jAskDataModelSource>singletonList(
						new LangChain4jModelRegistryAskDataModelSource(modelRegistry)));
	}

	private AskDataModelRequest request(String requestModelKey, String defaultKey) {
		return new AskDataModelRequest(new AskDataProjectContext("project-9527", "operator-9527"), requestModelKey,
				null, null, null, defaultKey);
	}
}
