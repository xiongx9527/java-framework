package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.es;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSettingService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.module.ElasticSearchModule;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库分段 ES 检索客户端：复用 fw-plus-boot magicapi elasticsearch 插件
 * （按知识库设置里的 {@code esKey} 指定 ES 数据源资源，空则取默认数据源）。
 * 索引名 {@code {esIndexPrefix}-segments}（默认 fw-kb-segments）。
 *
 * <p>负责分段全文索引的写入/删除/检索；混合检索关键词路在 ES 启用时优先走本客户端。</p>
 *
 * <p>{@code esKey} / {@code esIndexPrefix} / {@code esEnabled} 三项每次调用都从
 * {@link KbSettingService#current()} 现读，控制台改完立即生效——原实现把它们固化在构造
 * 参数里，只有重启才生效。注意 {@code esIndexPrefix} 改后旧索引下的数据不会自动迁移，
 * 需要调用方重新走一次同步。
 */
public class EsSegmentSearchClient {

	/** ES 数据源模块提供器 */
	private final ObjectProvider<ElasticSearchModule> esModules;

	/** 知识库全局设置（每次现读，实现 esKey / esIndexPrefix / esEnabled 热生效） */
	private final KbSettingService settings;

	/** ES 索引名后缀，固定为 -segments */
	private static final String INDEX_SUFFIX = "-segments";

	public EsSegmentSearchClient(ObjectProvider<ElasticSearchModule> esModules, KbSettingService settings) {
		this.esModules = esModules;
		this.settings = settings;
	}

	/** ES 全文检索开关；关闭时所有方法 no-op。每次现读设置。 */
	private boolean enabled() {
		return settings != null && settings.esEnabled();
	}

	/** 当前 ES 数据源 key（空则取默认数据源）。每次现读设置。 */
	private String esKey() {
		return settings == null ? null : settings.current().getEsKey();
	}

	/** 当前分段全文索引名。每次现读设置，前缀变化即换索引。 */
	private String index() {
		String prefix = settings == null ? null : settings.current().getEsIndexPrefix();
		return (StringUtils.hasText(prefix) ? prefix : KbSettingService.DEFAULT_ES_INDEX_PREFIX) + INDEX_SUFFIX;
	}

	private ElasticSearchModule resolve() {
		ElasticSearchModule module = esModules == null ? null : esModules.getIfAvailable();
		if (module == null) {
			throw new IllegalStateException("storage 检索 ES 路需要启用 fw-plus-boot 的 magicapi elasticsearch 插件"
					+ "（fw.plus.magicapi.elasticsearch.enabled）并配置 ES 数据源");
		}
		String key = esKey();
		return StringUtils.hasText(key) ? module.getDynamicAttribute(key) : module;
	}

	/** 启动时创建索引与 mapping（已存在时 ES 返回 resource_already_exists_exception，忽略）。 */
	public void ensureIndex() {
		if (!enabled()) {
			return;
		}
		try {
			Map<String, Object> mappings = createMappings();
			resolve().rest("/" + index()).put(mappings);
		} catch (Exception ignored) {
			// 已存在或暂时不可达：忽略，写入/检索时再暴露
		}
	}

	/** 写入/更新分段文档（按 segment_id 覆盖）。 */
	public void index(String datasetId, String documentId, String segmentId, String content, String keywords) {
		if (!enabled()) {
			return;
		}
		Map<String, Object> doc = new LinkedHashMap<>();
		doc.put("dataset_id", datasetId);
		doc.put("document_id", documentId);
		doc.put("segment_id", segmentId);
		doc.put("content", content == null ? "" : content);
		doc.put("keywords", keywords == null ? "" : keywords);
		try {
			resolve().index(index()).save(segmentId, doc);
		} catch (Exception e) {
			throw new IllegalStateException("写入 ES 分段索引失败: " + e.getMessage(), e);
		}
	}

	/** 删除分段文档。 */
	public void delete(String segmentId) {
		if (!enabled()) {
			return;
		}
		try {
			resolve().index(index()).delete(segmentId);
		} catch (Exception e) {
			throw new IllegalStateException("删除 ES 分段索引失败: " + segmentId, e);
		}
	}

	/** 按文档批量删除分段（delete_by_query）。 */
	public void deleteByDocument(String documentId) {
		if (!enabled()) {
			return;
		}
		try {
			Map<String, Object> body = java.util.Collections.singletonMap("query",
					java.util.Collections.singletonMap("term",
							java.util.Collections.singletonMap("document_id", documentId)));
			resolve().rest("/" + index() + "/_delete_by_query").post(body);
		} catch (Exception e) {
			throw new IllegalStateException("按文档删除 ES 分段索引失败: " + documentId, e);
		}
	}

	/** ES 命中（segmentId + 相关度分）。 */
	public static class EsHit {

		private final String segmentId;

		private final double score;

		public EsHit(String segmentId, double score) {
			this.segmentId = segmentId;
			this.score = score;
		}

		public String getSegmentId() {
			return segmentId;
		}

		public double getScore() {
			return score;
		}
	}

	/**
	 * 全文检索：multi_match(content/keywords) + dataset/document 过滤；
	 * 返回按 _score 降序的命中（segmentId + 原始相关度分，调用方归一化）。
	 */
	public List<EsHit> search(String datasetId, String query, String documentId, int limit) {
		if (!enabled()) {
			return new ArrayList<>();
		}
		try {
			List<Object> must = java.util.Collections.singletonList(
					java.util.Collections.singletonMap("multi_match",
							java.util.Collections.singletonMap("query", query)));
			List<Object> filter = new ArrayList<>();
			filter.add(java.util.Collections.singletonMap("term",
					java.util.Collections.singletonMap("dataset_id", datasetId)));
			if (StringUtils.hasText(documentId)) {
				filter.add(java.util.Collections.singletonMap("term",
						java.util.Collections.singletonMap("document_id", documentId)));
			}
			Map<String, Object> dsl = new LinkedHashMap<>();
			dsl.put("query", java.util.Collections.singletonMap("bool",
					new LinkedHashMap<String, Object>() {{
						put("must", must);
						put("filter", filter);
					}}));
			dsl.put("size", limit);

			Object result = resolve().index(index()).search(dsl);
			List<EsHit> hits = new ArrayList<>();
			if (result instanceof Map) {
				Object hitsObj = ((Map<?, ?>) result).get("hits");
				if (hitsObj instanceof Map) {
					Object list = ((Map<?, ?>) hitsObj).get("hits");
					if (list instanceof List) {
						for (Object item : (List<?>) list) {
							if (item instanceof Map) {
								Map<?, ?> hit = (Map<?, ?>) item;
								String segmentId = String.valueOf(hit.get("_id"));
								Object score = hit.get("_score");
								hits.add(new EsHit(segmentId,
										score instanceof Number ? ((Number) score).doubleValue() : 0));
							}
						}
					}
				}
			}
			return hits;
		} catch (Exception e) {
			throw new IllegalStateException("ES 全文检索失败: " + e.getMessage(), e);
		}
	}

	private static Map<String, Object> createMappings() {
		List<Map<String, Object>> properties = Arrays.asList(
				java.util.Collections.singletonMap("dataset_id",
						java.util.Collections.singletonMap("type", "keyword")),
				java.util.Collections.singletonMap("document_id",
						java.util.Collections.singletonMap("type", "keyword")),
				java.util.Collections.singletonMap("segment_id",
						java.util.Collections.singletonMap("type", "keyword")),
				java.util.Collections.singletonMap("content",
						java.util.Collections.singletonMap("type", "text")),
				java.util.Collections.singletonMap("keywords",
						java.util.Collections.singletonMap("type", "text")));
		return java.util.Collections.singletonMap("mappings",
				java.util.Collections.singletonMap("properties", properties));
	}
}
