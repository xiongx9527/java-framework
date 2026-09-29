package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.provider;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverContext;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;

/**
 * 向量库检索：{@code EmbeddingStoreContentRetriever}，按 embedding-store 与 embedding-model 两个 key 取依赖。
 *
 * <p>options：{@code embedding-store-key}（必填）、{@code embedding-model-key}（空则取 Spring 默认）、
 * {@code max-results}、{@code min-score}。</p>
 */
public class EmbeddingStoreContentRetrieverProvider implements ContentRetrieverProvider {

	@Override
	public String provider() {
		return "embedding-store";
	}

	@Override
	public ContentRetriever build(Langchain4jOptionsReader o, ContentRetrieverContext context) {
		EmbeddingStoreContentRetriever.EmbeddingStoreContentRetrieverBuilder builder = EmbeddingStoreContentRetriever
				.builder()
				.embeddingStore(context.embeddingStore(o.required("embedding-store-key")))
				.embeddingModel(context.embeddingModel(o.string("embedding-model-key", "")))
				.minScore(o.dbl("min-score", null));
		if (o.has("max-results")) {
			builder.maxResults(o.integer("max-results", null));
		}
		return builder.build();
	}
}
