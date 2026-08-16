package com.xiongx9527.fw.plus.ai.tinyflow.model;

import dev.langchain4j.model.chat.ChatModel;

import java.util.List;

public interface TinyflowModelProvider {

	List<TinyflowModelDescriptor> listChatModels();

	List<TinyflowModelDescriptor> listEmbeddingModels();

	ChatModel getChatModel(String key);

	TinyflowEmbeddingModel getEmbeddingModel(String key);
}
