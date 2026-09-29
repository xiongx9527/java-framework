package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.provider;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverContext;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.azure.search.AzureAiSearchContentRetriever;
import dev.langchain4j.rag.content.retriever.azure.search.AzureAiSearchQueryType;

/**
 * Azure AI Search 检索：{@code AzureAiSearchContentRetriever}，向量 / 全文 / 混合检索。
 *
 * <p>options：{@code endpoint}、{@code api-key}、{@code index-name}、{@code embedding-model-key}（必填），
 * {@code dimensions}（建索引时必填，缺省 0）、{@code query-type}（vector/full-text/hybrid/hybrid-with-reranking）、
 * {@code max-results}、{@code min-score}、{@code create-or-update-index}。</p>
 */
public class AzureAiSearchContentRetrieverProvider implements ContentRetrieverProvider {

	@Override
	public String provider() {
		return "azure-ai-search";
	}

	@Override
	public ContentRetriever build(Langchain4jOptionsReader o, ContentRetrieverContext context) {
		return AzureAiSearchContentRetriever.builder()
				.endpoint(o.required("endpoint"))
				.apiKey(o.required("api-key"))
				.indexName(o.required("index-name"))
				.embeddingModel(context.embeddingModel(o.required("embedding-model-key")))
				.dimensions(o.integer("dimensions", 0))
				.maxResults(o.integer("max-results", 5))
				.minScore(o.dbl("min-score", 0.0))
				.queryType(o.enumValue("query-type", AzureAiSearchQueryType.class, AzureAiSearchQueryType.VECTOR))
				.createOrUpdateIndex(o.bool("create-or-update-index", false))
				.build();
	}
}
