package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
 * 知识库检索服务（设计 §3.1/§3.2/§3.4 共用）。多路召回架构：
 *
 * <ul>
 *   <li>向量路：query 嵌入（embedding_model_active 保证与向量一致）→ 向量检索（metadata filter
 *       下推优先、失败时内存兜底）→ score 阈值；</li>
 *   <li>关键词路：segment content/keywords LIKE 命中（中文按 2-gram 切词），命中度 0~1 与向量
 *       score 同量纲；</li>
 *   <li>融合：两路按 segmentId 去重合并（向量命中保留原分，纯关键词命中带命中度分），hybrid
 *       开关（{@code fw.plus.ai.knowledge.hybrid-search}，默认开启）控制；</li>
 *   <li>economy 经济模式（indexing_technique=economy）：不入向量库，仅关键词单路；</li>
 *   <li>统一 rerank（scoring_model_key 启用时重排并写 rerank_score，score 保留向量分）→
 *       topK 截断 → hit_count 异步累加。启禁/归档全程以表为权威。</li>
 * </ul>
 */
public class KbSearchService {

	private final KbDatasetMapper datasetMapper;

	private final KbDocumentMapper documentMapper;

	private final KbSegmentMapper segmentMapper;

	private final ContentRetrieverContext retrieverContext;

	private final ObjectProvider<Langchain4jScoringModelModule> scoringModels;

	private final KbIndexingExecutor executor;

	private final KbKnowledgeState state;

	/** 混合检索开关（向量路 + 关键词路融合；关闭则仅向量路） */
	private final boolean hybridSearch;

	public KbSearchService(KbDatasetMapper datasetMapper, KbDocumentMapper documentMapper,
			KbSegmentMapper segmentMapper, ContentRetrieverContext retrieverContext,
			ObjectProvider<Langchain4jScoringModelModule> scoringModels, KbIndexingExecutor executor,
			KbKnowledgeState state, boolean hybridSearch) {
		this.datasetMapper = datasetMapper;
		this.documentMapper = documentMapper;
		this.segmentMapper = segmentMapper;
		this.retrieverContext = retrieverContext;
		this.scoringModels = scoringModels;
		this.executor = executor;
		this.state = state;
		this.hybridSearch = hybridSearch;
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
		int topK = topKOverride != null && topKOverride > 0 ? topKOverride
				: dataset.getRetrievalTopK() != null ? dataset.getRetrievalTopK() : 4;
		double minScore = minScoreOverride != null ? minScoreOverride
				: dataset.getRetrievalScoreThreshold() != null
						? dataset.getRetrievalScoreThreshold().doubleValue() : 0.0;
		Filter metadataFilter = buildMetadataCondition(metadataCondition);

		// economy 经济模式：不入向量库，纯关键词单路
		if ("economy".equalsIgnoreCase(dataset.getIndexingTechnique())) {
			List<KbSearchHit> hits = economySearch(dataset, query, metadataFilter);
			if (hits.size() > topK) {
				hits = new ArrayList<>(hits.subList(0, topK));
			}
			accumulateHitCount(hits);
			return hits;
		}

		// 嵌入一致性（设计 §2.3）：检索嵌入用 embedding_model_active，保证与向量一致
		String effectiveModel = dataset.getEmbeddingModelActive() != null
				? dataset.getEmbeddingModelActive()
				: dataset.getEmbeddingModelKey();
		EmbeddingModel embeddingModel = retrieverContext.embeddingModel(effectiveModel);
		EmbeddingStore<TextSegment> store = retrieverContext.embeddingStore(dataset.getEmbeddingStoreKey());
		Embedding queryEmbedding = embeddingModel.embed(query).content();

		List<EmbeddingMatch<TextSegment>> matches;
		// 元数据条件优先下推向量库（查询期过滤）；store 不支持时才回落为取出后内存过滤
		boolean metadataPushedDown = false;
		if (metadataFilter != null) {
			try {
				matches = doSearch(store, queryEmbedding, topK, minScore, baseFilter().and(metadataFilter));
				metadataPushedDown = true;
			} catch (Exception e) {
				// store 不支持 metadata filter 下推：多取后内存过滤（语义与下推一致）
				matches = doSearch(store, queryEmbedding, Math.max(topK * 3, 20), minScore, null);
			}
		} else {
			matches = doSearch(store, queryEmbedding, topK, minScore, null);
		}

		Filter inMemoryMetadataFilter = metadataPushedDown ? null : metadataFilter;
		List<KbSearchHit> hits = assemble(matches, minScore, inMemoryMetadataFilter);
		if (inMemoryMetadataFilter != null && matches.size() > hits.size()) {
			// 兜底过滤后不足 topK：多取一轮补足
			matches = doSearch(store, queryEmbedding, Math.max(topK * 3, 20), minScore, null);
			hits = assemble(matches, minScore, inMemoryMetadataFilter);
		}

		// 多路召回融合：向量路之外补充关键词路（按 segmentId 去重，关键词命中带命中度分）
		if (hybridSearch) {
			hits = mergeKeywordRecall(dataset, query, hits, inMemoryMetadataFilter, Math.max(topK * 3, 20));
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

	private List<KbSearchHit> assemble(List<EmbeddingMatch<TextSegment>> matches, double minScore,
			Filter inMemoryMetadataFilter) {
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
			Map<String, Object> docMetadata = document == null || document.getMetadata() == null
					? new LinkedHashMap<String, Object>()
					: parseMetadata(document.getMetadata());
			// 元数据条件内存过滤（仅 store 不支持下推时；以最新文档 metadata 为准，而非向量快照）
			if (inMemoryMetadataFilter != null
					&& !inMemoryMetadataFilter.test(new dev.langchain4j.data.document.Metadata(docMetadata))) {
				continue;
			}
			String parentContent = null;
			if (segment.getParentId() != null && !segment.getParentId().isEmpty()) {
				KbSegment parent = segmentMapper.selectById(segment.getParentId());
				parentContent = parent == null ? null : parent.getContent();
			}
			hits.add(KbSearchHit.of(segment, document == null ? null : document.getName(), match.score(),
					parentContent, docMetadata));
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

	/**
	 * Dify metadata_condition → langchain4j Filter。操作符（Dify 兼容 + 通用别名）：
	 * is / not is、is not / not_equal、in / not in、gt / gte / lt / lte（数值优先，非数值按字符串比较）、
	 * contains / not contains（0.35.0 自定义 Filter，下推失败自动落内存过滤；1.x 原生 containsString）；
	 * is empty / is not empty 以空串近似（metadata 值用空串表示空）。logical_operator 支持 and/or。
	 */
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
			Filter single = buildSingleCondition(name, value, valueStr, operator);
			combined = combined == null ? single : (or ? combined.or(single) : combined.and(single));
		}
		return combined;
	}

	private Filter buildSingleCondition(String name, Object value, String valueStr, String operator) {
		MetadataFilterBuilder builder = new MetadataFilterBuilder(name);
		String op = operator == null ? "is" : operator.trim();
		switch (op.toLowerCase()) {
			case "is not":
			case "not_equal":
			case "not equal":
				return builder.isNotEqualTo(valueStr);
			case "in": {
				List<String> values = toValueList(value);
				return values.isEmpty() ? builder.isEqualTo(valueStr)
						: builder.isIn(values.toArray(new String[0]));
			}
			case "not in":
			case "not_in": {
				List<String> values = toValueList(value);
				return values.isEmpty() ? builder.isNotEqualTo(valueStr)
						: builder.isNotIn(values.toArray(new String[0]));
			}
			case "contains":
				return ContainsStringFilter.of(name, valueStr);
			case "not contains":
			case "not_contains":
				return Filter.not(ContainsStringFilter.of(name, valueStr));
			case "gt":
			case "greater_than":
				return numericAware(valueStr, v -> builder.isGreaterThan(v), builder::isGreaterThan);
			case "gte":
			case "greater_than_or_equal":
				return numericAware(valueStr, v -> builder.isGreaterThanOrEqualTo(v),
						builder::isGreaterThanOrEqualTo);
			case "lt":
			case "less_than":
				return numericAware(valueStr, v -> builder.isLessThan(v), builder::isLessThan);
			case "lte":
			case "less_than_or_equal":
				return numericAware(valueStr, v -> builder.isLessThanOrEqualTo(v),
						builder::isLessThanOrEqualTo);
			case "is empty":
			case "is_empty":
				// 空值以空串近似（metadata 值用空串表示空；0.35.0/1.x 均无原生 IsEmpty）
				return builder.isEqualTo("");
			case "is not empty":
			case "is_not_empty":
				return Filter.not(builder.isEqualTo(""));
			default:
				// 默认按相等（is / equal）
				return builder.isEqualTo(valueStr);
		}
	}

	private Filter numericAware(String valueStr,
			java.util.function.Function<Double, Filter> numeric,
			java.util.function.Function<String, Filter> textual) {
		if (valueStr != null) {
			try {
				return numeric.apply(Double.parseDouble(valueStr));
			} catch (NumberFormatException ignored) {
				// 非数值按字符串比较
			}
		}
		return textual.apply(valueStr);
	}

	private static List<String> toValueList(Object value) {
		List<String> values = new ArrayList<>();
		if (value instanceof List) {
			for (Object item : (List<?>) value) {
				if (item != null) {
					values.add(String.valueOf(item));
				}
			}
		} else if (value instanceof String && !((String) value).isEmpty()) {
			for (String item : ((String) value).split(",")) {
				if (!item.trim().isEmpty()) {
					values.add(item.trim());
				}
			}
		}
		return values;
	}

	/** rerank（scoring_model_key 非空时启用）；score 保留向量相似度分，rerank_score 由重排模型打分；失败回落向量 score 顺序。 */
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
				KbSearchHit rankedHit = KbSearchHit.of(segmentOf(hit), hit.getDocumentName(), hit.getScore(),
						hit.getParentContent(), hit.getMetadata());
				// score 保留向量相似度分，重排分单独放 rerank_score（两套量纲不混用）
				rankedHit.setRerankScore(entry.getValue());
				ranked.add(rankedHit);
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

	// ---------------- 关键词路（多路召回第二路 / economy 单路） ----------------

	/**
	 * 关键词路召回：segment content/keywords 对 query 词项 LIKE 命中（中文按 2-gram 切词，
	 * 空格分词的词项原样），命中度 = 命中词项数 / 词项总数（0~1，与向量 score 同量纲）。
	 */
	private List<KbSearchHit> economySearch(KbDataset dataset, String query, Filter metadataFilter) {
		List<KbSegment> segments = keywordRecallSegments(dataset.getId(), query);
		List<KbSearchHit> hits = new ArrayList<>();
		for (KbSegment segment : segments) {
			double score = keywordScore(query, segment);
			if (score <= 0) {
				continue;
			}
			KbDocument document = documentMapper.selectById(segment.getDocumentId());
			if (document == null || "1".equals(document.getArchived())) {
				continue;
			}
			Map<String, Object> docMetadata = parseMetadata(document.getMetadata());
			if (metadataFilter != null
					&& !metadataFilter.test(new dev.langchain4j.data.document.Metadata(docMetadata))) {
				continue;
			}
			hits.add(KbSearchHit.of(segment, document.getName(), score, null, docMetadata));
		}
		hits.sort((a, b) -> Double.compare(b.getScore() == null ? 0 : b.getScore(),
				a.getScore() == null ? 0 : a.getScore()));
		return hits;
	}

	/** 多路召回融合：向量路命中保留原分，关键词路新增命中带命中度分，按 segmentId 去重。 */
	private List<KbSearchHit> mergeKeywordRecall(KbDataset dataset, String query, List<KbSearchHit> vectorHits,
			Filter inMemoryMetadataFilter, int limit) {
		List<KbSegment> keywordSegments = keywordRecallSegments(dataset.getId(), query);
		if (keywordSegments.isEmpty()) {
			return vectorHits;
		}
		java.util.Set<String> vectorIds = new java.util.HashSet<>();
		for (KbSearchHit hit : vectorHits) {
			vectorIds.add(hit.getSegmentId());
		}
		List<KbSearchHit> merged = new ArrayList<>(vectorHits);
		for (KbSegment segment : keywordSegments) {
			if (vectorIds.contains(segment.getId())) {
				continue;
			}
			double score = keywordScore(query, segment);
			if (score <= 0) {
				continue;
			}
			KbDocument document = documentMapper.selectById(segment.getDocumentId());
			if (document == null || "1".equals(document.getArchived())) {
				continue;
			}
			Map<String, Object> docMetadata = parseMetadata(document.getMetadata());
			if (inMemoryMetadataFilter != null
					&& !inMemoryMetadataFilter.test(new dev.langchain4j.data.document.Metadata(docMetadata))) {
				continue;
			}
			String parentContent = null;
			if (segment.getParentId() != null && !segment.getParentId().isEmpty()) {
				KbSegment parent = segmentMapper.selectById(segment.getParentId());
				parentContent = parent == null ? null : parent.getContent();
			}
			merged.add(KbSearchHit.of(segment, document.getName(), score, parentContent, docMetadata));
		}
		return merged;
	}

	/** 关键词路 SQL 召回：content/keywords 对词项 LIKE（OR 连接），限制返回量。 */
	private List<KbSegment> keywordRecallSegments(String datasetId, String query) {
		List<String> terms = queryTerms(query);
		if (terms.isEmpty()) {
			return Collections.emptyList();
		}
		QueryWrapper<KbSegment> wrapper = new QueryWrapper<>();
		wrapper.eq("dataset_id", datasetId)
				.eq("enabled", "1")
				.eq("status", "completed")
				.and(w -> {
					for (int i = 0; i < terms.size(); i++) {
						String term = terms.get(i);
						if (i == 0) {
							w.and(x -> x.like("content", term).or().like("keywords", term));
						} else {
							w.or(x -> x.like("content", term).or().like("keywords", term));
						}
					}
				})
				.last("LIMIT 60");
		return segmentMapper.selectList(wrapper);
	}

	/** 命中度：命中词项数 / 词项总数（0~1）。 */
	private double keywordScore(String query, KbSegment segment) {
		List<String> terms = queryTerms(query);
		if (terms.isEmpty()) {
			return 0;
		}
		String content = segment.getContent() == null ? "" : segment.getContent();
		String keywords = segment.getKeywords() == null ? "" : segment.getKeywords();
		int hit = 0;
		for (String term : terms) {
			if (content.contains(term) || keywords.contains(term)) {
				hit++;
			}
		}
		return (double) hit / terms.size();
	}

	/**
	 * 检索词项切分：空格分词；含中文的长词按 2-gram 切（无分词器依赖的朴素匹配粒度）；
	 * 词项数上限 8，避免关键词路 SQL 过长。
	 */
	static List<String> queryTerms(String query) {
		if (query == null) {
			return Collections.emptyList();
		}
		java.util.LinkedHashSet<String> terms = new java.util.LinkedHashSet<>();
		for (String part : query.trim().split("\\s+")) {
			if (part.isEmpty()) {
				continue;
			}
			boolean hasCJK = false;
			for (int i = 0; i < part.length(); i++) {
				if (Character.UnicodeScript.of(part.charAt(i)) == Character.UnicodeScript.HAN) {
					hasCJK = true;
					break;
				}
			}
			if (!hasCJK) {
				terms.add(part.toLowerCase());
			} else if (part.length() <= 2) {
				terms.add(part);
			} else {
				for (int i = 0; i + 2 <= part.length(); i++) {
					terms.add(part.substring(i, i + 2));
				}
			}
		}
		List<String> result = new ArrayList<>(terms);
		return result.size() > 8 ? new ArrayList<>(result.subList(0, 8)) : result;
	}
}
