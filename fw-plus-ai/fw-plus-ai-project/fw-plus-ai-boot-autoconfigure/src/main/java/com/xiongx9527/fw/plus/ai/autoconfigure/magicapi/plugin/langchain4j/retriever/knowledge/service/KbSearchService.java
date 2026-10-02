package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jScoringModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverContext;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbDataset;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbDocument;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbSegment;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbDatasetMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbDocumentMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbSegmentMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.pipeline.KbIndexingExecutor;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import org.springframework.beans.factory.ObjectProvider;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库检索服务（设计 §3.1/§3.2/§3.4 共用）：query → 嵌入（embedding_model_active 保证与向量一致）
 * → 向量检索（metadata filter 下推优先、失败时检索侧内存兜底）→ 启禁/归档以表为权威 → score 阈值
 * → rerank → hit_count 异步累加。
 */
public class KbSearchService {

	private final KbDatasetMapper datasetMapper;

	private final KbDocumentMapper documentMapper;

	private final KbSegmentMapper segmentMapper;

	private final ContentRetrieverContext retrieverContext;

	private final ObjectProvider<Langchain4jScoringModelModule> scoringModels;

	private final KbIndexingExecutor executor;

	private final KbKnowledgeState state;

	public KbSearchService(KbDatasetMapper datasetMapper, KbDocumentMapper documentMapper,
			KbSegmentMapper segmentMapper, ContentRetrieverContext retrieverContext,
			ObjectProvider<Langchain4jScoringModelModule> scoringModels, KbIndexingExecutor executor,
			KbKnowledgeState state) {
		this.datasetMapper = datasetMapper;
		this.documentMapper = documentMapper;
		this.segmentMapper = segmentMapper;
		this.retrieverContext = retrieverContext;
		this.scoringModels = scoringModels;
		this.executor = executor;
		this.state = state;
	}

	/**
	 * 召回检索。
	 *
	 * @param topKOverride     覆盖数据集 retrieval_top_k；null 用数据集默认
	 * @param minScoreOverride 覆盖数据集 retrieval_score_threshold；null 用数据集默认
	 * @param metadataCondition Dify metadata_condition 格式：{logical_operator, conditions:[{name, value, comparison_operator?}]}
	 */
	public List<KbSearchHit> search(String datasetId, String query, Integer topKOverride,
			Double minScoreOverride, JSONObject metadataCondition) {
		state.checkReady();
		if (query == null || query.isEmpty()) {
			throw new IllegalArgumentException("检索 query 不能为空");
		}
		KbDataset dataset = requireDataset(datasetId);
		// 嵌入一致性（设计 §2.3）：检索嵌入用 embedding_model_active，保证与向量一致
		String effectiveModel = dataset.getEmbeddingModelActive() != null
				? dataset.getEmbeddingModelActive()
				: dataset.getEmbeddingModelKey();
		EmbeddingModel embeddingModel = retrieverContext.embeddingModel(effectiveModel);
		EmbeddingStore<TextSegment> store = retrieverContext.embeddingStore(dataset.getEmbeddingStoreKey());
		Embedding queryEmbedding = embeddingModel.embed(query).content();

		int topK = topKOverride != null && topKOverride > 0 ? topKOverride
				: dataset.getRetrievalTopK() != null ? dataset.getRetrievalTopK() : 4;
		double minScore = minScoreOverride != null ? minScoreOverride
				: dataset.getRetrievalScoreThreshold() != null
						? dataset.getRetrievalScoreThreshold().doubleValue() : 0.0;

		Filter metadataFilter = buildMetadataCondition(metadataCondition);
		List<EmbeddingMatch<TextSegment>> matches;
		if (metadataFilter != null) {
			try {
				matches = doSearch(store, queryEmbedding, topK, minScore, baseFilter().and(metadataFilter));
			} catch (Exception e) {
				// store 不支持 metadata filter 下推：多取后内存兜底（设计 §3.1）
				matches = doSearch(store, queryEmbedding, Math.max(topK * 3, 20), minScore, null);
			}
		} else {
			matches = doSearch(store, queryEmbedding, topK, minScore, null);
		}

		List<KbSearchHit> hits = assemble(matches, minScore);
		if (metadataFilter != null && matches.size() > hits.size()) {
			// 下推失败兜底场景：多取一轮补足 topK
			matches = doSearch(store, queryEmbedding, Math.max(topK * 3, 20), minScore, null);
			hits = assemble(matches, minScore);
		}
		List<KbSearchHit> ranked = rerank(dataset, query, hits);
		if (ranked.size() > topK) {
			ranked = new ArrayList<>(ranked.subList(0, topK));
		}
		accumulateHitCount(ranked);
		return ranked;
	}

	private List<EmbeddingMatch<TextSegment>> doSearch(EmbeddingStore<TextSegment> store,
			Embedding queryEmbedding, int topK, double minScore, Filter filter) {
		EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
				.queryEmbedding(queryEmbedding)
				.maxResults(topK)
				.minScore(minScore)
				.filter(filter)
				.build();
		EmbeddingSearchResult<TextSegment> result = store.search(request);
		return result.matches();
	}

	/** 恒定过滤：向量 metadata 快照的 enabled/archived（最终仍以表为权威内存过滤）。 */
	private Filter baseFilter() {
		return new MetadataFilterBuilder("enabled").isEqualTo("1")
				.and(new MetadataFilterBuilder("archived").isEqualTo("0"));
	}

	private List<KbSearchHit> assemble(List<EmbeddingMatch<TextSegment>> matches, double minScore) {
		List<KbSearchHit> hits = new ArrayList<>();
		for (EmbeddingMatch<TextSegment> match : matches) {
			if (match.score() < minScore) {
				continue;
			}
			Map<String, Object> vectorMetadata = match.embedded() != null && match.embedded().metadata() != null
					? match.embedded().metadata().toMap()
					: Collections.<String, Object>emptyMap();
			Object segmentIdObj = vectorMetadata.get("segment_id");
			String segmentId = segmentIdObj == null ? null : String.valueOf(segmentIdObj);
			KbSegment segment = segmentId == null || segmentId.isEmpty() ? null : segmentMapper.selectById(segmentId);
			if (segment == null) {
				// 向量残留（分段已删）：跳过
				continue;
			}
			// 启禁/归档以表为准（向量 metadata 只是写入时快照）
			if (!"1".equals(segment.getEnabled())) {
				continue;
			}
			KbDocument document = documentMapper.selectById(segment.getDocumentId());
			if (document != null && "1".equals(document.getArchived())) {
				continue;
			}
			String parentContent = null;
			if (segment.getParentId() != null && !segment.getParentId().isEmpty()) {
				KbSegment parent = segmentMapper.selectById(segment.getParentId());
				parentContent = parent == null ? null : parent.getContent();
			}
			hits.add(KbSearchHit.of(segment, document == null ? null : document.getName(), match.score(),
					parentContent, document == null || document.getMetadata() == null
							? new LinkedHashMap<String, Object>()
							: parseMetadata(document.getMetadata())));
		}
		return hits;
	}

	private static Map<String, Object> parseMetadata(String metadataJson) {
		try {
			Map<String, Object> map = JSON.parseObject(metadataJson);
			return map == null ? new LinkedHashMap<String, Object>() : map;
		} catch (Exception e) {
			return new LinkedHashMap<>();
		}
	}

	/** Dify metadata_condition → langchain4j Filter（M1 支持 is / is not；and/or 组合）。 */
	Filter buildMetadataCondition(JSONObject condition) {
		if (condition == null || condition.isEmpty()) {
			return null;
		}
		boolean or = "or".equalsIgnoreCase(condition.getString("logical_operator"));
		Filter combined = null;
		List<Object> conditions = condition.getJSONArray("conditions");
		if (conditions == null) {
			return null;
		}
		for (Object itemObj : conditions) {
			JSONObject item = (JSONObject) itemObj;
			String name = item.getString("name");
			if (name == null || name.isEmpty()) {
				continue;
			}
			Object value = item.get("value");
			String valueStr = value == null ? null : String.valueOf(value);
			String operator = item.getString("comparison_operator");
			MetadataFilterBuilder builder = new MetadataFilterBuilder(name);
			Filter single = "is not".equalsIgnoreCase(operator) || "not_equal".equalsIgnoreCase(operator)
					? builder.isNotEqualTo(valueStr)
					: builder.isEqualTo(valueStr);
			combined = combined == null ? single : (or ? combined.or(single) : combined.and(single));
		}
		return combined;
	}

	/** rerank（scoring_model_key 非空时启用）；失败回落向量 score 顺序。 */
	private List<KbSearchHit> rerank(KbDataset dataset, String query, List<KbSearchHit> hits) {
		if (dataset.getScoringModelKey() == null || dataset.getScoringModelKey().isEmpty() || hits.size() < 2) {
			return hits;
		}
		try {
			Langchain4jScoringModelModule module = scoringModels == null ? null : scoringModels.getIfAvailable();
			if (module == null) {
				return hits;
			}
			ScoringModel scoringModel = module.getDynamicAttribute(dataset.getScoringModelKey());
			List<TextSegment> segments = new ArrayList<>();
			for (KbSearchHit hit : hits) {
				segments.add(TextSegment.from(hit.getContent()));
			}
			List<Double> scores = scoringModel.scoreAll(segments, query).content();
			List<Map.Entry<KbSearchHit, Double>> paired = new ArrayList<>();
			for (int i = 0; i < hits.size(); i++) {
				Double score = scores != null && i < scores.size() && scores.get(i) != null
						? scores.get(i) : hits.get(i).getScore();
				paired.add(new AbstractMap.SimpleEntry<>(hits.get(i), score));
			}
			paired.sort(new Comparator<Map.Entry<KbSearchHit, Double>>() {
				@Override
				public int compare(Map.Entry<KbSearchHit, Double> a, Map.Entry<KbSearchHit, Double> b) {
					return Double.compare(b.getValue() == null ? 0 : b.getValue(),
							a.getValue() == null ? 0 : a.getValue());
				}
			});
			List<KbSearchHit> ranked = new ArrayList<>();
			for (Map.Entry<KbSearchHit, Double> entry : paired) {
				KbSearchHit hit = entry.getKey();
				ranked.add(KbSearchHit.of(segmentOf(hit), hit.getDocumentName(), entry.getValue(),
						hit.getParentContent(), hit.getMetadata()));
			}
			return ranked;
		} catch (Exception e) {
			return hits;
		}
	}

	private static KbSegment segmentOf(KbSearchHit hit) {
		KbSegment segment = new KbSegment();
		segment.setId(hit.getSegmentId());
		segment.setDatasetId(hit.getDatasetId());
		segment.setDocumentId(hit.getDocumentId());
		segment.setPosition(hit.getPosition());
		segment.setContent(hit.getContent());
		segment.setAnswer(hit.getAnswer());
		segment.setKeywords(hit.getKeywords());
		return segment;
	}

	private void accumulateHitCount(List<KbSearchHit> hits) {
		for (final KbSearchHit hit : hits) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						segmentMapper.incrHitCount(hit.getSegmentId());
					} catch (Exception ignored) {
						// 命中计数失败不影响检索结果
					}
				}
			});
		}
	}

	/** 数据集存在性查询（对外 API Dify 兼容路由用）。 */
	public KbDataset datasetOf(String datasetId) {
		return datasetId == null || datasetId.isEmpty() ? null : datasetMapper.selectById(datasetId);
	}

	private KbDataset requireDataset(String datasetId) {
		if (datasetId == null || datasetId.isEmpty()) {
			throw new IllegalArgumentException("数据集 id 不能为空");
		}
		KbDataset dataset = datasetMapper.selectById(datasetId);
		if (dataset == null) {
			throw new IllegalArgumentException("知识库数据集不存在: " + datasetId);
		}
		return dataset;
	}
}
