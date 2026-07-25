package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model;

public interface AskDataModelRegistry {

	AskDataChatModel getChatModel(String modelKey);

	AskDataEmbeddingModel getEmbeddingModel(String modelKey);

	String getDefaultChatModelKey();

	String getDefaultEmbeddingModelKey();
}
