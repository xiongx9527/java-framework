package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataErrorCode;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelRegistry;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LangChain4jAskDataModelRegistry implements AskDataModelRegistry {

	private final AskDataProperties properties;
	private final Map<String, ChatLanguageModel> chatModels;
	private final Map<String, EmbeddingModel> embeddingModels;

	public LangChain4jAskDataModelRegistry(AskDataProperties properties, LangChain4jModelRegistry modelRegistry) {
		this.properties = properties;
		this.chatModels = chatModels(modelRegistry);
		this.embeddingModels = embeddingModels(modelRegistry);
	}

	private Map<String, ChatLanguageModel> chatModels(LangChain4jModelRegistry modelRegistry) {
		if (modelRegistry == null) {
			return Collections.emptyMap();
		}
		Map<String, ChatLanguageModel> models = new LinkedHashMap<String, ChatLanguageModel>();
		for (String key : modelRegistry.keys()) {
			LangChain4jModel model = modelRegistry.get(key);
			if (model != null && model.getModel() instanceof ChatLanguageModel) {
				models.put(key, (ChatLanguageModel) model.getModel());
			}
		}
		return models;
	}

	private Map<String, EmbeddingModel> embeddingModels(LangChain4jModelRegistry modelRegistry) {
		if (modelRegistry == null) {
			return Collections.emptyMap();
		}
		Map<String, EmbeddingModel> models = new LinkedHashMap<String, EmbeddingModel>();
		for (String key : modelRegistry.keys()) {
			LangChain4jModel model = modelRegistry.get(key);
			if (model != null && model.getModel() instanceof EmbeddingModel) {
				models.put(key, (EmbeddingModel) model.getModel());
			}
		}
		return models;
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
