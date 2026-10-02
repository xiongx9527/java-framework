package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.provider;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverContext;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverProvider;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSearchService;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import org.springframework.beans.factory.ObjectProvider;

/**
 * knowledge 类型检索器工厂（设计 §3.2）：短名 {@code knowledge}，作为
 * {@code ContentRetrieverProvider} bean 由 {@code ContentRetrieverProviderRegistry} 收集注册。
 *
 * <p>options：{@code dataset-id}（必填，指向 kb_dataset.id）、{@code max-results}、{@code min-score}
 * （可覆盖数据集默认）。多实例：每个 content-retriever 资源 key 指向不同数据集，互不影响。</p>
 */
public class KnowledgeContentRetrieverProvider implements ContentRetrieverProvider {

	private final ObjectProvider<KbSearchService> searchServices;

	public KnowledgeContentRetrieverProvider(ObjectProvider<KbSearchService> searchServices) {
		this.searchServices = searchServices;
	}

	@Override
	public String provider() {
		return "knowledge";
	}

	@Override
	public ContentRetriever build(Langchain4jOptionsReader o, ContentRetrieverContext context) {
		String datasetId = o.required("dataset-id");
		Integer maxResults = o.has("max-results") ? o.integer("max-results", null) : null;
		Double minScore = o.has("min-score") ? o.dbl("min-score", null) : null;
		return new KnowledgeContentRetriever(searchServices, datasetId, maxResults, minScore);
	}
}
