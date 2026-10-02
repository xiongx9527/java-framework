package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSearchHit;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSearchService;
import org.ssssssss.magicapi.core.annotation.MagicModule;

import java.util.List;

/**
 * magic-script 检索入口（设计 §3.2）：spring bean 方式 {@code import knowledge} 调
 * {@code search(datasetId, query)}（M1，不做四件套资源）。
 */
@MagicModule("knowledge")
public class KnowledgeModule {

	private final KbSearchService searchService;

	public KnowledgeModule(KbSearchService searchService) {
		this.searchService = searchService;
	}

	/** 按数据集默认 topK/阈值检索。 */
	public List<KbSearchHit> search(String datasetId, String query) {
		return searchService.search(datasetId, query, null, null, null);
	}

	/** 覆盖 topK 与 score 阈值的检索。 */
	public List<KbSearchHit> search(String datasetId, String query, Integer topK, Double minScore) {
		return searchService.search(datasetId, query, topK, minScore, null);
	}
}
