package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.provider;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSearchHit;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSearchService;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * knowledge 类型检索实例（设计 §3.2）：懒解析——检索时才按 dataset-id 解析数据集配置与嵌入/向量资源，
 * 注册顺序与数据集创建先后不影响构建。过滤链（enabled/archived/score 阈值）与 hit_count 累加
 * 统一由 {@link KbSearchService#search} 承担。
 */
public class KnowledgeContentRetriever implements ContentRetriever {

	private final ObjectProvider<KbSearchService> searchServices;

	private final String datasetId;

	private final Integer maxResults;

	private final Double minScore;

	public KnowledgeContentRetriever(ObjectProvider<KbSearchService> searchServices, String datasetId,
			Integer maxResults, Double minScore) {
		this.searchServices = searchServices;
		this.datasetId = datasetId;
		this.maxResults = maxResults;
		this.minScore = minScore;
	}

	@Override
	public List<Content> retrieve(Query query) {
		KbSearchService searchService = searchServices == null ? null : searchServices.getIfAvailable();
		if (searchService == null) {
			throw new IllegalStateException("知识库检索服务未装配，无法构建 knowledge 类型检索器");
		}
		List<KbSearchHit> hits = searchService.search(datasetId, query.text(), maxResults, minScore, null);
		List<Content> contents = new ArrayList<>(hits.size());
		for (KbSearchHit hit : hits) {
			Metadata metadata = new Metadata();
			metadata.put("dataset_id", hit.getDatasetId());
			metadata.put("document_id", hit.getDocumentId());
			metadata.put("segment_id", hit.getSegmentId());
			if (hit.getDocumentName() != null) {
				metadata.put("document_name", hit.getDocumentName());
			}
			if (hit.getPosition() > 0) {
				metadata.put("position", String.valueOf(hit.getPosition()));
			}
			if (hit.getMetadata() != null) {
				for (Map.Entry<String, Object> entry : hit.getMetadata().entrySet()) {
					if (entry.getKey() != null && entry.getValue() != null) {
						metadata.put(entry.getKey(), String.valueOf(entry.getValue()));
					}
				}
			}
			contents.add(Content.from(TextSegment.from(hit.getContent(), metadata)));
		}
		return contents;
	}
}
