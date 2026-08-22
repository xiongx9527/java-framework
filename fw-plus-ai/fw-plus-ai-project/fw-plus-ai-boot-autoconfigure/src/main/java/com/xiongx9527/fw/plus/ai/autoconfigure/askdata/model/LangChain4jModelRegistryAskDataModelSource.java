package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model;

import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jAiModel;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jChatModelAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.LinkedHashMap;
import java.util.Map;

public class LangChain4jModelRegistryAskDataModelSource implements LangChain4jAskDataModelSource {

	private final Map<String, ChatModel> chatModels = new LinkedHashMap<String, ChatModel>();
	private final Map<String, EmbeddingModel> embeddingModels = new LinkedHashMap<String, EmbeddingModel>();

	public LangChain4jModelRegistryAskDataModelSource(AiModelRegistry modelRegistry) {
		for (String key : modelRegistry.keys()) {
			AiModel model = modelRegistry.get(key);
			if (!(model instanceof LangChain4jAiModel)) {
				continue;
			}
			LangChain4jModel nativeModel = ((LangChain4jAiModel) model).getNativeModel();
			ChatModel chatModel = nativeModel == null ? null
					: LangChain4jChatModelAdapter.asChatModel(nativeModel.getModel());
			if (chatModel != null) {
				chatModels.put(key, chatModel);
			}
			if (nativeModel != null && nativeModel.getModel() instanceof EmbeddingModel) {
				embeddingModels.put(key, (EmbeddingModel) nativeModel.getModel());
			}
		}
	}

	@Override
	public Map<String, ChatModel> getChatModels() {
		return chatModels;
	}

	@Override
	public Map<String, EmbeddingModel> getEmbeddingModels() {
		return embeddingModels;
	}
}
