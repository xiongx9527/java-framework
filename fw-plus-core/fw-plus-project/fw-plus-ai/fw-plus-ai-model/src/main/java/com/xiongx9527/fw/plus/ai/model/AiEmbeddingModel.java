package com.xiongx9527.fw.plus.ai.model;

public interface AiEmbeddingModel extends AiModel {

	AiEmbeddingResult embed(AiEmbeddingRequest request);
}
