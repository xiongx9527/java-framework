package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataErrorCode;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.LangChain4jModelInfo;
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
		LangChain4jModelInfo modelInfo = new LangChain4jModelInfo();
		Map<String, ChatLanguageModel> models = new HashMap<String, ChatLanguageModel>();
		models.put("default", chatModel("default-sql"));
		models.put("fast", chatModel("fast-sql"));
		modelInfo.setChatLanguageModelMap(models);

		AskDataModelRegistry registry = new LangChain4jAskDataModelRegistry(properties, modelInfo);

		assertEquals("fast-sql", registry.getChatModel("fast").generate("question").getText());
	}

	@Test
	public void shouldSelectDefaultChatModelWhenRequestModelKeyIsBlank() {
		AskDataProperties properties = new AskDataProperties();
		properties.getModel().setDefaultChatModel("default");
		LangChain4jModelInfo modelInfo = new LangChain4jModelInfo();
		Map<String, ChatLanguageModel> models = new HashMap<String, ChatLanguageModel>();
		models.put("default", chatModel("default-sql"));
		modelInfo.setChatLanguageModelMap(models);

		AskDataModelRegistry registry = new LangChain4jAskDataModelRegistry(properties, modelInfo);

		assertEquals("default", registry.getDefaultChatModelKey());
		assertEquals("default-sql", registry.getChatModel("  ").generate("question").getText());
	}

	@Test
	public void shouldRejectMissingRequestLevelChatModel() {
		AskDataProperties properties = new AskDataProperties();
		properties.getModel().setDefaultChatModel("default");
		LangChain4jModelInfo modelInfo = new LangChain4jModelInfo();
		modelInfo.setChatLanguageModelMap(new HashMap<String, ChatLanguageModel>());
		AskDataModelRegistry registry = new LangChain4jAskDataModelRegistry(properties, modelInfo);

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
		LangChain4jModelInfo modelInfo = new LangChain4jModelInfo();
		Map<String, EmbeddingModel> models = new HashMap<String, EmbeddingModel>();
		models.put("embedding-default", embeddingModel(0.1F, 0.2F));
		modelInfo.setEmbeddingModelMap(models);

		AskDataModelRegistry registry = new LangChain4jAskDataModelRegistry(properties, modelInfo);

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
}
