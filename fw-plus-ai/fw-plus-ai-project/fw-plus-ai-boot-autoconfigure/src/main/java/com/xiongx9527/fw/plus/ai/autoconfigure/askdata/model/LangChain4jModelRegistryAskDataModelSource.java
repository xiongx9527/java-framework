package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model;

import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelRegistry;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.LinkedHashMap;
import java.util.Map;

public class LangChain4jModelRegistryAskDataModelSource implements LangChain4jAskDataModelSource {

	private final Map<String, ChatLanguageModel> chatModels = new LinkedHashMap<String, ChatLanguageModel>();
	private final Map<String, EmbeddingModel> embeddingModels = new LinkedHashMap<String, EmbeddingModel>();

	public LangChain4jModelRegistryAskDataModelSource(LangChain4jModelRegistry modelRegistry) {
		for (String key : modelRegistry.keys()) {
			LangChain4jModel model = modelRegistry.get(key);
			if (model != null && model.getModel() instanceof ChatLanguageModel) {
				chatModels.put(key, (ChatLanguageModel) model.getModel());
			}
			if (model != null && model.getModel() instanceof EmbeddingModel) {
				embeddingModels.put(key, (EmbeddingModel) model.getModel());
			}
		}
	}

	@Override
	public Map<String, ChatLanguageModel> getChatModels() {
		return chatModels;
	}

	@Override
	public Map<String, EmbeddingModel> getEmbeddingModels() {
		return embeddingModels;
	}
}
