package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model;

import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jAiModel;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.LinkedHashMap;
import java.util.Map;

public class LangChain4jModelRegistryAskDataModelSource implements LangChain4jAskDataModelSource {

	private final Map<String, ChatLanguageModel> chatModels = new LinkedHashMap<String, ChatLanguageModel>();
	private final Map<String, EmbeddingModel> embeddingModels = new LinkedHashMap<String, EmbeddingModel>();

	public LangChain4jModelRegistryAskDataModelSource(AiModelRegistry modelRegistry) {
		for (String key : modelRegistry.keys()) {
			AiModel model = modelRegistry.get(key);
			if (!(model instanceof LangChain4jAiModel)) {
				continue;
			}
			LangChain4jModel nativeModel = ((LangChain4jAiModel) model).getNativeModel();
			if (nativeModel != null && nativeModel.getModel() instanceof ChatLanguageModel) {
				chatModels.put(key, (ChatLanguageModel) nativeModel.getModel());
			}
			if (nativeModel != null && nativeModel.getModel() instanceof EmbeddingModel) {
				embeddingModels.put(key, (EmbeddingModel) nativeModel.getModel());
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
