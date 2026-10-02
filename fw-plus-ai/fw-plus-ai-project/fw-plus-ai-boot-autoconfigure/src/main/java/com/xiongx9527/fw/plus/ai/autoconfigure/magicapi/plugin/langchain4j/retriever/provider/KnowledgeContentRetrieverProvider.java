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
 * （可覆盖数据集默认）、{@code document-id}（选填，限定在该文档范围内检索）、
 * {@code metadata-condition}（选填，Dify metadata_condition 格式的固定过滤条件 JSON）、
 * {@code metadata-filter-mode}（选填，none 默认 / auto——检索前由对话模型从用户消息与
 * 数据集字段定义自动生成元数据过滤条件，SimpleAssistant 等 RAG 链无需传参即可支持
 * “查 2024 年的文档”这类带过滤意图的问题）。
 * 多实例：每个 content-retriever 资源 key 指向不同数据集，互不影响。</p>
 */
public class KnowledgeContentRetrieverProvider implements ContentRetrieverProvider {

	private final ObjectProvider<KbSearchService> searchServices;

	/** 字段定义服务（metadata-filter-mode=auto 时拉取字段定义） */
	private final ObjectProvider<com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbMetadataFieldService> metadataFieldServices;

	public KnowledgeContentRetrieverProvider(ObjectProvider<KbSearchService> searchServices,
			ObjectProvider<com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbMetadataFieldService> metadataFieldServices) {
		this.searchServices = searchServices;
		this.metadataFieldServices = metadataFieldServices;
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
		String documentId = o.has("document-id") ? o.string("document-id", null) : null;
		String conditionJson = o.has("metadata-condition") ? o.string("metadata-condition", null) : null;
		String metadataFilterMode = o.has("metadata-filter-mode") ? o.string("metadata-filter-mode", "none") : "none";
		com.alibaba.fastjson2.JSONObject metadataCondition = null;
		if (conditionJson != null && !conditionJson.trim().isEmpty()) {
			try {
				metadataCondition = com.alibaba.fastjson2.JSON.parseObject(conditionJson);
			} catch (Exception e) {
				throw new IllegalArgumentException("metadata-condition 不是合法的 JSON 对象: " + e.getMessage());
			}
		}
		return new KnowledgeContentRetriever(searchServices, datasetId, maxResults, minScore, documentId,
				metadataCondition, metadataFilterMode, metadataFieldServices, context);
	}
}
