package com.xiongx9527.fw.plus.ai.askdata.model;

import com.xiongx9527.fw.plus.ai.askdata.AskDataErrorCode;
import com.xiongx9527.fw.plus.ai.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.LangChain4jModelInfo;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LangChain4jAskDataModelRegistry implements AskDataModelRegistry {

	private final AskDataProperties properties;
	private final Map<String, ChatLanguageModel> chatModels;
	private final Map<String, EmbeddingModel> embeddingModels;

	public LangChain4jAskDataModelRegistry(AskDataProperties properties, LangChain4jModelInfo modelInfo) {
		this.properties = properties;
		this.chatModels = modelInfo == null || modelInfo.getChatLanguageModelMap() == null
				? Collections.<String, ChatLanguageModel>emptyMap() : modelInfo.getChatLanguageModelMap();
		this.embeddingModels = modelInfo == null || modelInfo.getEmbeddingModelMap() == null
				? Collections.<String, EmbeddingModel>emptyMap() : modelInfo.getEmbeddingModelMap();
	}

	@Override
	public AskDataChatModel getChatModel(String modelKey) {
		final String resolvedKey = resolveModelKey(modelKey, getDefaultChatModelKey());
		final ChatLanguageModel chatLanguageModel = chatModels.get(resolvedKey);
		if (chatLanguageModel == null) {
			throw modelNotFound("聊天", resolvedKey);
		}
		return new AskDataChatModel() {
			@Override
			public AskDataModelResponse generate(String prompt) {
				return new AskDataModelResponse(chatLanguageModel.generate(prompt), null, resolvedKey);
			}
		};
	}

	@Override
	public AskDataEmbeddingModel getEmbeddingModel(String modelKey) {
		final String resolvedKey = resolveModelKey(modelKey, getDefaultEmbeddingModelKey());
		final EmbeddingModel embeddingModel = embeddingModels.get(resolvedKey);
		if (embeddingModel == null) {
			throw modelNotFound("向量", resolvedKey);
		}
		return new AskDataEmbeddingModel() {
			@Override
			public List<Float> embed(String text) {
				return embeddingModel.embed(text).content().vectorAsList();
			}
		};
	}

	@Override
	public String getDefaultChatModelKey() {
		return properties.getModel().getDefaultChatModel();
	}

	@Override
	public String getDefaultEmbeddingModelKey() {
		return properties.getModel().getDefaultEmbeddingModel();
	}

	private String resolveModelKey(String modelKey, String defaultModelKey) {
		String resolvedKey = hasText(modelKey) ? modelKey : defaultModelKey;
		if (!hasText(resolvedKey)) {
			throw new AskDataModelException(AskDataErrorCode.DEFAULT_MODEL_NOT_CONFIGURED, "未配置默认模型");
		}
		return resolvedKey;
	}

	private AskDataModelException modelNotFound(String modelType, String modelKey) {
		return new AskDataModelException(AskDataErrorCode.MODEL_NOT_FOUND,
				"未找到" + modelType + "模型: " + modelKey);
	}

	private boolean hasText(String value) {
		return value != null && value.trim().length() > 0;
	}
}
