package com.xiongx9527.fw.plus.ai.nl2sql;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.request.EmbeddingRequest;

/**
 * langchain4j 1.22.0 新 embedding API（EmbeddingRequest/EmbeddingResponse）的统一封装。
 */
public final class EmbeddingUtils {

	private EmbeddingUtils() {
	}

	/** 单文本向量化 */
	public static Embedding embed(EmbeddingModel model, String text) {
		return model.embed(EmbeddingRequest.builder().inputs(text).build()).embeddings().get(0);
	}
}