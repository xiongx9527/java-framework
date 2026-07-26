package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataErrorCode;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.langchain4j.model.DefaultLangChain4jModelRegistry;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelRegistry;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelType;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class LangChain4jAskDataModelRegistryTest {

	@Test
	public void shouldSelectRequestLevelChatModelWhenModelKeyExists() {
		AskDataProperties properties = new AskDataProperties();
		properties.getModel().setDefaultChatModel("default");
		Map<String, ChatLanguageModel> models = new HashMap<String, ChatLanguageModel>();
		models.put("default", chatModel("default-sql"));
		models.put("fast", chatModel("fast-sql"));

		AskDataModelRegistry registry = new LangChain4jAskDataModelRegistry(properties, modelRegistry(models, new HashMap<String, EmbeddingModel>()));

		assertEquals("fast-sql", registry.getChatModel("fast").generate("question").getText());
	}

	@Test
	public void shouldSelectDefaultChatModelWhenRequestModelKeyIsBlank() {
		AskDataProperties properties = new AskDataProperties();
		properties.getModel().setDefaultChatModel("default");
		Map<String, ChatLanguageModel> models = new HashMap<String, ChatLanguageModel>();
		models.put("default", chatModel("default-sql"));

		AskDataModelRegistry registry = new LangChain4jAskDataModelRegistry(properties, modelRegistry(models, new HashMap<String, EmbeddingModel>()));

		assertEquals("default", registry.getDefaultChatModelKey());
		assertEquals("default-sql", registry.getChatModel("  ").generate("question").getText());
	}

	@Test
	public void shouldRejectMissingRequestLevelChatModel() {
		AskDataProperties properties = new AskDataProperties();
		properties.getModel().setDefaultChatModel("default");
		AskDataModelRegistry registry = new LangChain4jAskDataModelRegistry(properties,
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
		AskDataModelRegistry registry = new LangChain4jAskDataModelRegistry(properties, null);

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

		AskDataModelRegistry registry = new LangChain4jAskDataModelRegistry(properties, modelRegistry(new HashMap<String, ChatLanguageModel>(), models));

		assertEquals("embedding-default", registry.getDefaultEmbeddingModelKey());
		assertEquals(java.util.Arrays.asList(0.1F, 0.2F), registry.getEmbeddingModel(null).embed("question"));
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
}
