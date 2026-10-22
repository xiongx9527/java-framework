package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.rag;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.xiongx9527.fw.plus.dify.dataset.types.RetrievalModel;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Dify 知识库检索（RAG {@code ContentRetriever}）配置。
 *
 * <p>{@code enabled=true} 时自动装配 {@link DifyContentRetriever}，调用 dify
 * {@code POST /datasets/{dataset_id}/retrieve} 知识库检索接口；
 * base-url / api-key / dataset-ids 在装配时校验，缺失即启动快速失败。</p>
 */
@ConfigurationProperties(DifyRagProperties.PREFIX)
public class DifyRagProperties {

	public static final String PREFIX = "fw.plus.ai.langchain4j.dify-rag";

	/** 开关：true 时装配 {@link DifyContentRetriever} */
	private boolean enabled = false;

	/** Dify 服务地址（如 http://127.0.0.1:8080；可含可不含 /v1，见 {@link #toBaseUrl()}） */
	private String baseUrl;

	/** 知识库 API Key */
	private String apiKey;

	/** 知识库 ID 列表（支持多个：逐库检索后按分数降序合并） */
	private List<String> datasetIds = new ArrayList<>();

	/** 每库召回条数，同时作为多库合并后的全局截断上限 */
	private int topK = 3;

	/** 检索方法：semantic_search / hybrid_search / full_text_search / keyword_search（空 → semantic_search；dify 必填） */
	private String searchMethod;

	/** 是否开启召回分数限制（空 → false；dify 必填） */
	private Boolean scoreThresholdEnabled;

	/** 召回分数限制 */
	private Double scoreThreshold;

	/** 是否开启 rerank（空 → false；dify 必填） */
	private Boolean rerankingEnable;

	/** 元数据过滤配置（chatMemoryId 四段 → dify metadata_filtering_conditions） */
	private final MetaFields metaFields = new MetaFields();

	/** 多库并行检索线程池配置 */
	private final Pool pool = new Pool();

	public MetaFields getMetaFields() {
		return metaFields;
	}

	public Pool getPool() {
		return pool;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public List<String> getDatasetIds() {
		return datasetIds;
	}

	public void setDatasetIds(List<String> datasetIds) {
		this.datasetIds = datasetIds;
	}

	public int getTopK() {
		return topK;
	}

	public void setTopK(int topK) {
		this.topK = topK;
	}

	public String getSearchMethod() {
		return searchMethod;
	}

	public void setSearchMethod(String searchMethod) {
		this.searchMethod = searchMethod;
	}

	public Boolean getScoreThresholdEnabled() {
		return scoreThresholdEnabled;
	}

	public void setScoreThresholdEnabled(Boolean scoreThresholdEnabled) {
		this.scoreThresholdEnabled = scoreThresholdEnabled;
	}

	public Double getScoreThreshold() {
		return scoreThreshold;
	}

	public void setScoreThreshold(Double scoreThreshold) {
		this.scoreThreshold = scoreThreshold;
	}

	public Boolean getRerankingEnable() {
		return rerankingEnable;
	}

	public void setRerankingEnable(Boolean rerankingEnable) {
		this.rerankingEnable = rerankingEnable;
	}

	/**
	 * 归一化的服务地址：去掉尾斜杠并保证 {@code /v1} 前缀。
	 *
	 * <p>dify SDK 的 {@code server} 语义为含 {@code /v1} 的根地址（如
	 * {@code https://api.dify.ai/v1}），自部署常只配到域名端口；不补全时请求会落到
	 * Web 控制台页面而非数据面接口。</p>
	 */
	public String toBaseUrl() {
		String trimmed = baseUrl == null ? "" : baseUrl.trim();
		while (trimmed.endsWith("/")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed.endsWith("/v1") ? trimmed : trimmed + "/v1";
	}

	/**
	 * 构造 dify {@link RetrievalModel}；{@code search-method} 非法取值在此快速失败。
	 *
	 * <p>dify 端 pydantic 的 {@code search_method} / {@code reranking_enable} /
	 * {@code score_threshold_enabled} 均为无默认值的必填字段，未配置时补安全默认
	 * （semantic_search / false / false），避免请求 400。</p>
	 */
	public RetrievalModel toRetrievalModel() {
		RetrievalModel.RetrievalModelBuilder builder = RetrievalModel.builder()
				.topK(topK)
				.scoreThresholdEnabled(Boolean.TRUE.equals(scoreThresholdEnabled))
				.rerankingEnable(Boolean.TRUE.equals(rerankingEnable));
		if (searchMethod == null || searchMethod.trim().isEmpty()) {
			builder.searchMethod(RetrievalModel.SearchMethod.semantic_search);
		} else {
			try {
				builder.searchMethod(RetrievalModel.SearchMethod.valueOf(searchMethod.trim()));
			} catch (IllegalArgumentException e) {
				throw new IllegalStateException(I18nMessageResolver.resolveKey(
						"fw.ai.DifyRagProperties.searchMethodInvalid", PREFIX, searchMethod));
			}
		}
		builder.scoreThreshold(scoreThreshold);
		return builder.build();
	}

	/**
	 * 元数据过滤：把会话 {@code chatMemoryId} 四段
	 * （{@code {tenantId}:{appId}:{agentId}:{userId}}）转成 dify
	 * {@code retrieval_model.metadata_filtering_conditions}。
	 *
	 * <p>参与哪些段（{@code include}）与各段在知识库元数据中的字段名（{@code *-field}）
	 * 均可配置；知识库文档需按字段约定打好元数据标签后才可开启。</p>
	 */
	public static class MetaFields {

		/** 总开关：知识库文档元数据打标完成后再开启，否则过滤会导致检索恒空 */
		private boolean enabled = false;

		/** 参与过滤的段（tenant / app / agent / user），决定四段中哪些进入条件 */
		private List<String> include = Arrays.asList("tenant", "app", "agent", "user");

		/** 段[0] tenantId 在知识库元数据中的字段名（空 → 该段不参与） */
		private String tenantField = "tenant_id";

		/** 段[1] appId 在知识库元数据中的字段名（空 → 该段不参与） */
		private String appField = "app_id";

		/** 段[2] agentId 在知识库元数据中的字段名（空 → 该段不参与） */
		private String agentField = "agent_id";

		/** 段[3] userId 在知识库元数据中的字段名（空 → 该段不参与） */
		private String userField = "user_id";

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public List<String> getInclude() {
			return include;
		}

		public void setInclude(List<String> include) {
			this.include = include;
		}

		public String getTenantField() {
			return tenantField;
		}

		public void setTenantField(String tenantField) {
			this.tenantField = tenantField;
		}

		public String getAppField() {
			return appField;
		}

		public void setAppField(String appField) {
			this.appField = appField;
		}

		public String getAgentField() {
			return agentField;
		}

		public void setAgentField(String agentField) {
			this.agentField = agentField;
		}

		public String getUserField() {
			return userField;
		}

		public void setUserField(String userField) {
			this.userField = userField;
		}

		/**
		 * 校验 {@code include} 合法段名（仅在开关开启时）：拼错的段名若等到检索期
		 * 才发现，会被降级逻辑吞掉导致「过滤静默不生效」，故在启动装配期快速失败。
		 */
		public void validate() {
			if (!enabled) {
				return;
			}
			Set<String> allowed = new HashSet<>(Arrays.asList("tenant", "app", "agent", "user"));
			List<String> items = include == null ? Collections.<String>emptyList() : include;
			for (String item : items) {
				if (item == null || !allowed.contains(item.trim())) {
					throw new IllegalStateException(I18nMessageResolver.resolveKey(
							"fw.ai.DifyRagProperties.metaFieldInvalid", PREFIX, item));
				}
			}
		}
	}

	/**
	 * 多知识库并行检索的线程池（单知识库不建池、同步直查）。
	 */
	public static class Pool {

		/** 线程数；&lt;=0 时按知识库数量创建（配小可限并发，任务自动排队） */
		private int size = 0;

		/** 线程名前缀（排障用），默认 dify-content-retriever-N */
		private String threadNamePrefix = "dify-content-retriever";

		public int getSize() {
			return size;
		}

		public void setSize(int size) {
			this.size = size;
		}

		public String getThreadNamePrefix() {
			return threadNamePrefix;
		}

		public void setThreadNamePrefix(String threadNamePrefix) {
			this.threadNamePrefix = threadNamePrefix;
		}
	}
}
