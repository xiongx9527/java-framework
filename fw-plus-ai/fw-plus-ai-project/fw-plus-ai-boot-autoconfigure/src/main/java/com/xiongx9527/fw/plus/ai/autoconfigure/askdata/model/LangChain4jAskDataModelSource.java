package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.Map;

public interface LangChain4jAskDataModelSource {

	Map<String, ChatLanguageModel> getChatModels();

	Map<String, EmbeddingModel> getEmbeddingModels();
}
