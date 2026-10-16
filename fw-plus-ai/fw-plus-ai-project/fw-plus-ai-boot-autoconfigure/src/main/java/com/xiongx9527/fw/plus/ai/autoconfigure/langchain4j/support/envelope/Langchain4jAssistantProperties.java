package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 助手（信封 + 结构化 + 分类 + 会话）配置项，前缀 {@code fw.plus.ai.langchain4j.assistant}。
 *
 * <pre>{@code
 * fw.plus.ai.langchain4j.assistant:
 *   envelope:
 *     enabled: false                     # 帧形态开关（默认关）；开启后既有无 mapper 流式入口的 done 帧承载信封
 *   structured:
 *     default-enabled: false             # 结构化全局默认（默认关）；覆盖顺序：单次调用 > 业务契约 > 全局默认
 *     max-attempts: 3                    # 结构化重试上限（含首次），必须 >= 1
 *   classifier:
 *     enabled: true                      # 分类开关（默认开）；关闭后所有轮次落兜底 chat 且不产生分类调用
 *     history-user-messages: 3           # 分类输入附带的最近用户消息条数，必须 >= 0（0 = 只看本轮）
 *     max-attempts: 1                    # 分类判定尝试次数（含首次），必须 >= 1（默认不重试）
 *   session:
 *     window-size: 20                    # 会话窗口条数（读快照/写回用），必须 >= 1
 *     facts:
 *       enabled: false                   # 跨会话事实通道开关（默认关）；开启时 base-url 必填、缺失即启动失败
 *       base-url: http://localhost:8000  # 独立 mem0 实例（OSS 无 /v1 前缀；网关要求时自行带前缀）
 *       api-key:                         # X-API-Key 认证（留空不发送）
 *       connect-timeout: 5s              # mem0 HTTP 连接超时（默认 5s）
 *       read-timeout: 10s                # mem0 HTTP 读取超时（默认 10s）
 *       extractor: mem0                  # mem0 | llm | rule | hybrid
 *       write-mode: per-turn             # per-turn | batched | session-end
 *       batch-size: 5                    # batched 攒满轮数（>= 1）
 *       retriever: semantic              # semantic | keyword | hybrid | rerank
 *       top-k: 5                         # 召回条数上限（>= 1）
 *       max-facts-tokens: 500            # 事实段 token 软上限（>= 1；4 字符/token 折算）
 *       extract-async: true              # 抽取默认异步；false = 请求线程内联
 *       extract-model:                   # llm / hybrid 抽取模型 key 或 ChatLanguageModel FQCN（缺省：llm 跳过、hybrid 纯规则）
 *       rerank-model:                    # retriever=rerank 的重排模型 key 或 ScoringModel FQCN（缺省：本地关键词重叠重排）
 * }</pre>
 *
 * <p><b>非法配置的处置</b>：启动阶段（{@link #validate()}，由 {@link InitializingBean} 在绑定完成后自动调用）
 * 发现非法配置立即抛 {@link IllegalStateException} 并指出具体配置项，让应用启动失败；
 * 运行阶段（{@code trySetXxx(...)}，供配置中心推送 / 脚本热更新）遇到非法值保留旧值、记录告警并返回
 * {@code false}，不中断既有服务。</p>
 */
@Slf4j
@ConfigurationProperties(prefix = Langchain4jAssistantProperties.PREFIX)
public class Langchain4jAssistantProperties implements InitializingBean {

	/** 配置前缀 */
	public static final String PREFIX = "fw.plus.ai.langchain4j.assistant";

	private final Envelope envelope = new Envelope();

	private final Structured structured = new Structured();

	private final Classifier classifier = new Classifier();

	private final Session session = new Session();

	@Override
	public void afterPropertiesSet() {
		validate();
	}

	/** 校验全部配置项；非法立即失败并指出配置项（启动期调用） */
	public void validate() {
		envelope.validate();
		structured.validate();
		classifier.validate();
		session.validate();
	}

	public Envelope getEnvelope() {
		return envelope;
	}

	public Structured getStructured() {
		return structured;
	}

	public Classifier getClassifier() {
		return classifier;
	}

	public Session getSession() {
		return session;
	}

	// -------------------------------------------------- 内部工具

	/** 非法配置异常：带上完整配置项与取值 */
	static IllegalStateException illegal(String key, Object value, String reason) {
		return new IllegalStateException("非法配置 " + PREFIX + "." + key + "=" + value + "：" + reason);
	}

	/** 运行期非法值告警：沿用旧值，不中断服务 */
	static void warnIllegal(String key, Object value, String reason) {
		log.warn("忽略非法配置 {}={}：{}（沿用旧值）", PREFIX + "." + key, value, reason);
	}

	// -------------------------------------------------- 帧形态开关

	/** 帧形态：{@code done} 帧承载信封还是模型原文 */
	public static class Envelope {

		/** 帧形态开关（默认 {@code false}：既有输出与调用方行为一字不变） */
		private boolean enabled = false;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public void validate() {
			// boolean 无非法取值；不可识别的取值由 Spring 绑定阶段拦下（fail-fast 并指出配置项）
		}
	}

	// -------------------------------------------------- 结构化开关

	/** 结构化输出相关：全局默认与重试上限 */
	public static class Structured {

		/** 全局默认是否产出结构化结果（默认 {@code false}） */
		private boolean defaultEnabled = false;

		/** 重试上限（含首次，默认 {@value #DEFAULT_MAX_ATTEMPTS}） */
		private int maxAttempts = DEFAULT_MAX_ATTEMPTS;

		/** 重试上限默认值 */
		public static final int DEFAULT_MAX_ATTEMPTS = 3;

		/**
		 * 三层覆盖顺序解析：单次调用 > 业务契约 > 全局默认。
		 *
		 * @param callOverride     单次调用显式声明；{@code null} 表示未声明
		 * @param contractOverride 业务契约声明；{@code null} 表示未声明
		 * @return 本轮是否产出结构化结果
		 */
		public boolean resolveEnabled(Boolean callOverride, Boolean contractOverride) {
			if (callOverride != null) {
				return callOverride;
			}
			if (contractOverride != null) {
				return contractOverride;
			}
			return defaultEnabled;
		}

		public boolean isDefaultEnabled() {
			return defaultEnabled;
		}

		public void setDefaultEnabled(boolean defaultEnabled) {
			this.defaultEnabled = defaultEnabled;
		}

		public int getMaxAttempts() {
			return maxAttempts;
		}

		public void setMaxAttempts(int maxAttempts) {
			this.maxAttempts = maxAttempts;
		}

		public void validate() {
			if (maxAttempts < 1) {
				throw illegal("structured.max-attempts", maxAttempts, "结构化重试上限必须 >= 1（含首次）");
			}
		}

		/** 运行期热更新：非法值保留旧值并告警 */
		public boolean trySetMaxAttempts(int value) {
			if (value < 1) {
				warnIllegal("structured.max-attempts", value, "结构化重试上限必须 >= 1（含首次）");
				return false;
			}
			this.maxAttempts = value;
			return true;
		}
	}

	// -------------------------------------------------- 分类

	/** 业务分类相关配置 */
	public static class Classifier {

		/** 分类开关（默认 {@code true}） */
		private boolean enabled = true;

		/** 分类输入附带的最近用户消息条数（默认 {@value #DEFAULT_HISTORY_USER_MESSAGES}；0 = 只看本轮） */
		private int historyUserMessages = DEFAULT_HISTORY_USER_MESSAGES;

		/** 判定尝试次数（含首次，默认 {@value #DEFAULT_MAX_ATTEMPTS}：不重试） */
		private int maxAttempts = DEFAULT_MAX_ATTEMPTS;

		/** 分类历史条数默认值 */
		public static final int DEFAULT_HISTORY_USER_MESSAGES = 3;

		/** 分类尝试次数默认值（不重试） */
		public static final int DEFAULT_MAX_ATTEMPTS = 1;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public int getHistoryUserMessages() {
			return historyUserMessages;
		}

		public void setHistoryUserMessages(int historyUserMessages) {
			this.historyUserMessages = historyUserMessages;
		}

		public int getMaxAttempts() {
			return maxAttempts;
		}

		public void setMaxAttempts(int maxAttempts) {
			this.maxAttempts = maxAttempts;
		}

		public void validate() {
			if (historyUserMessages < 0) {
				throw illegal("classifier.history-user-messages", historyUserMessages, "分类历史条数必须 >= 0（0 = 只看本轮）");
			}
			if (maxAttempts < 1) {
				throw illegal("classifier.max-attempts", maxAttempts, "分类尝试次数必须 >= 1（含首次）");
			}
		}

		/** 运行期热更新：非法值保留旧值并告警 */
		public boolean trySetHistoryUserMessages(int value) {
			if (value < 0) {
				warnIllegal("classifier.history-user-messages", value, "分类历史条数必须 >= 0（0 = 只看本轮）");
				return false;
			}
			this.historyUserMessages = value;
			return true;
		}

		/** 运行期热更新：非法值保留旧值并告警 */
		public boolean trySetMaxAttempts(int value) {
			if (value < 1) {
				warnIllegal("classifier.max-attempts", value, "分类尝试次数必须 >= 1（含首次）");
				return false;
			}
			this.maxAttempts = value;
			return true;
		}
	}

	// -------------------------------------------------- 会话

	/** 会话历史相关配置 */
	public static class Session {

		/**
		 * 会话窗口条数（默认 {@value #DEFAULT_WINDOW_SIZE}）。
		 *
		 * <p>只作用于信封编排侧的读历史/写回（{@link EnvelopeSessionMemory}）；既有 AiServices 通道的窗口
		 * 由自身配置决定（{@code AssistantBuilder.memory(...)}）。两者默认相同（20）且共用同一个
		 * {@code ChatMemoryStore} 与 {@code memoryId}，但只调其中一个会让同一会话在两条通道上出现
		 * 不同的窗口长度——要么成对配置，要么保持默认。</p>
		 */
		private int windowSize = DEFAULT_WINDOW_SIZE;

		/** 会话窗口条数默认值 */
		public static final int DEFAULT_WINDOW_SIZE = 20;

		/** 会话能力总开关（默认 {@code true}）；关闭后不进行会话兜底与摘要维护 */
		private boolean enabled = true;

		/** 会话标识解析器（默认 {@value #DEFAULT_SESSION_ID_RESOLVER}：按天兜底） */
		private String sessionIdResolver = DEFAULT_SESSION_ID_RESOLVER;

		/** 会话标识解析器默认值 */
		public static final String DEFAULT_SESSION_ID_RESOLVER = "daily";

		/** 空闲超时分钟数（默认 {@value #DEFAULT_IDLE_TIMEOUT_MINUTES}）；{@code idle-timeout} 解析器用 */
		private int idleTimeoutMinutes = DEFAULT_IDLE_TIMEOUT_MINUTES;

		/** 空闲超时默认值（分钟） */
		public static final int DEFAULT_IDLE_TIMEOUT_MINUTES = 30;

		/** 上下文组合策略（默认 {@value #DEFAULT_CONTEXT_STRATEGY}） */
		private String contextStrategy = DEFAULT_CONTEXT_STRATEGY;

		/** 上下文组合策略默认值 */
		public static final String DEFAULT_CONTEXT_STRATEGY = "memory-and-enriched";

		/** 一级会话摘要配置 */
		private final Summary summary = new Summary();

		/** 跨会话事实记忆配置（独立事实通道，默认关闭） */
		private final Facts facts = new Facts();

		public int getWindowSize() {
			return windowSize;
		}

		public void setWindowSize(int windowSize) {
			this.windowSize = windowSize;
		}

		public String getSessionIdResolver() {
			return sessionIdResolver;
		}

		public void setSessionIdResolver(String sessionIdResolver) {
			this.sessionIdResolver = sessionIdResolver;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public int getIdleTimeoutMinutes() {
			return idleTimeoutMinutes;
		}

		public void setIdleTimeoutMinutes(int idleTimeoutMinutes) {
			this.idleTimeoutMinutes = idleTimeoutMinutes;
		}

		public String getContextStrategy() {
			return contextStrategy;
		}

		public void setContextStrategy(String contextStrategy) {
			this.contextStrategy = contextStrategy;
		}

		public Summary getSummary() {
			return summary;
		}

		public Facts getFacts() {
			return facts;
		}

		public void validate() {
			if (windowSize < 1) {
				throw illegal("session.window-size", windowSize, "会话窗口条数必须 >= 1");
			}
			if (idleTimeoutMinutes < 1) {
				throw illegal("session.idle-timeout-minutes", idleTimeoutMinutes, "空闲超时分钟数必须 >= 1");
			}
			summary.validate();
			facts.validate();
		}

		/** 运行期热更新：非法值保留旧值并告警 */
		public boolean trySetWindowSize(int value) {
			if (value < 1) {
				warnIllegal("session.window-size", value, "会话窗口条数必须 >= 1");
				return false;
			}
			this.windowSize = value;
			return true;
		}

		// -------------------------------------------------- 会话摘要

		/** 一级滚动会话摘要配置 */
		public static class Summary {

			/** 摘要开关（默认 {@code true}） */
			private boolean enabled = true;

			/** 摘要策略（默认 {@value #DEFAULT_STRATEGY}：增量精炼） */
			private String strategy = DEFAULT_STRATEGY;

			/** 摘要形态（默认 {@code hybrid}：结构化硬约束 + 自由文本主线） */
			private String form = "hybrid";

			/** 触发阈值（默认 {@value #DEFAULT_TRIGGER_TOKENS}）：历史累计超过才触发摘要 */
			private int triggerTokens = DEFAULT_TRIGGER_TOKENS;

			/** 压缩目标（默认 {@value #DEFAULT_KEEP_TOKENS}）：摘要压缩到该 token 以内 */
			private int keepTokens = DEFAULT_KEEP_TOKENS;

			/** 摘要正文 token 上限（默认 {@value #DEFAULT_MAX_SUMMARY_TOKENS}） */
			private int maxSummaryTokens = DEFAULT_MAX_SUMMARY_TOKENS;

			/** 每连续多少次增量精炼插入一次全量重摘（默认 {@value #DEFAULT_REBUILD_EVERY}） */
			private int rebuildEvery = DEFAULT_REBUILD_EVERY;

			/** 摘要用便宜小模型标识（模型 key 或 FQCN）；留空表示抽取式回退（不调 LLM） */
			private String summarizeModel;

			/** 摘要策略默认值 */
			public static final String DEFAULT_STRATEGY = "refine";

			/** 触发阈值默认值 */
			public static final int DEFAULT_TRIGGER_TOKENS = 4000;

			/** 压缩目标默认值 */
			public static final int DEFAULT_KEEP_TOKENS = 3000;

			/** 摘要正文 token 上限默认值 */
			public static final int DEFAULT_MAX_SUMMARY_TOKENS = 800;

			/** 全量重摘间隔默认值 */
			public static final int DEFAULT_REBUILD_EVERY = 10;

			public boolean isEnabled() {
				return enabled;
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

			public String getStrategy() {
				return strategy;
			}

			public void setStrategy(String strategy) {
				this.strategy = strategy;
			}

			public String getForm() {
				return form;
			}

			public void setForm(String form) {
				this.form = form;
			}

			public int getTriggerTokens() {
				return triggerTokens;
			}

			public void setTriggerTokens(int triggerTokens) {
				this.triggerTokens = triggerTokens;
			}

			public int getKeepTokens() {
				return keepTokens;
			}

			public void setKeepTokens(int keepTokens) {
				this.keepTokens = keepTokens;
			}

			public int getMaxSummaryTokens() {
				return maxSummaryTokens;
			}

			public void setMaxSummaryTokens(int maxSummaryTokens) {
				this.maxSummaryTokens = maxSummaryTokens;
			}

			public int getRebuildEvery() {
				return rebuildEvery;
			}

			public void setRebuildEvery(int rebuildEvery) {
				this.rebuildEvery = rebuildEvery;
			}

			public String getSummarizeModel() {
				return summarizeModel;
			}

			public void setSummarizeModel(String summarizeModel) {
				this.summarizeModel = summarizeModel;
			}

			public void validate() {
				if (triggerTokens < 1) {
					throw illegal("session.summary.trigger-tokens", triggerTokens, "摘要触发阈值必须 >= 1");
				}
				if (keepTokens < 1) {
					throw illegal("session.summary.keep-tokens", keepTokens, "摘要压缩目标必须 >= 1");
				}
				if (maxSummaryTokens < 1) {
					throw illegal("session.summary.max-summary-tokens", maxSummaryTokens, "摘要正文上限必须 >= 1");
				}
				if (rebuildEvery < 0) {
					throw illegal("session.summary.rebuild-every", rebuildEvery, "全量重摘间隔必须 >= 0（0 = 不重摘）");
				}
			}
		}

		// -------------------------------------------------- 跨会话事实记忆

		/** 跨会话事实记忆配置（独立事实通道；默认关闭，开启需独立 mem0 服务端） */
		public static class Facts {

			/** 事实通道开关（默认 {@code false}：不注入、不写入，行为与会话记忆基线一致） */
			private boolean enabled = false;

			/** 独立 mem0 实例地址（OSS 无 {@code /v1} 前缀；{@code enabled=true} 时必填，缺失即启动失败） */
			private String baseUrl;

			/** API Key（以 {@code X-API-Key} 头发送；留空不发送） */
			private String apiKey = "";

			/** 连接超时（默认 {@value #DEFAULT_CONNECT_TIMEOUT_SECONDS} 秒，与消息通道一致） */
			private Duration connectTimeout = Duration.ofSeconds(DEFAULT_CONNECT_TIMEOUT_SECONDS);

			/** 读取超时（默认 {@value #DEFAULT_READ_TIMEOUT_SECONDS} 秒，与消息通道一致） */
			private Duration readTimeout = Duration.ofSeconds(DEFAULT_READ_TIMEOUT_SECONDS);

			/** 抽取策略（默认 {@value #DEFAULT_EXTRACTOR}）：{@code mem0} | {@code llm} | {@code rule} | {@code hybrid} */
			private String extractor = DEFAULT_EXTRACTOR;

			/** 写入模式（默认 {@value #DEFAULT_WRITE_MODE}）：{@code per-turn} | {@code batched} | {@code session-end} */
			private String writeMode = DEFAULT_WRITE_MODE;

			/** 批量写入阈值（默认 {@value #DEFAULT_BATCH_SIZE} 轮；仅 {@code batched} 模式使用） */
			private int batchSize = DEFAULT_BATCH_SIZE;

			/** 召回策略（默认 {@value #DEFAULT_RETRIEVER}）：{@code semantic} | {@code keyword} | {@code hybrid} | {@code rerank} */
			private String retriever = DEFAULT_RETRIEVER;

			/** 召回条数上限（默认 {@value #DEFAULT_TOP_K}） */
			private int topK = DEFAULT_TOP_K;

			/** 事实段 token 软上限（默认 {@value #DEFAULT_MAX_FACTS_TOKENS}；4 字符/token 折算） */
			private int maxFactsTokens = DEFAULT_MAX_FACTS_TOKENS;

			/** 抽取是否异步（默认 {@code true}：后台线程执行不阻塞本轮；{@code false} = 请求线程内联） */
			private boolean extractAsync = true;

			/** 抽取模型（{@code llm}/{@code hybrid} 用：模型 key 或 ChatLanguageModel FQCN；缺省时 llm 跳过、hybrid 退化为 rule） */
			private String extractModel;

			/** 重排模型（{@code rerank} 用：模型 key 或 ScoringModel FQCN；缺省时用本地关键词重叠重排） */
			private String rerankModel;

			/** 连接超时默认秒数 */
			public static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 5;

			/** 读取超时默认秒数 */
			public static final int DEFAULT_READ_TIMEOUT_SECONDS = 10;

			/** 抽取策略默认值 */
			public static final String DEFAULT_EXTRACTOR = "mem0";

			/** 写入模式默认值 */
			public static final String DEFAULT_WRITE_MODE = "per-turn";

			/** 批量写入阈值默认值 */
			public static final int DEFAULT_BATCH_SIZE = 5;

			/** 召回策略默认值 */
			public static final String DEFAULT_RETRIEVER = "semantic";

			/** 召回条数上限默认值 */
			public static final int DEFAULT_TOP_K = 5;

			/** 事实段 token 上限默认值 */
			public static final int DEFAULT_MAX_FACTS_TOKENS = 500;

			/** 受支持的抽取策略名 */
			static final Set<String> SUPPORTED_EXTRACTORS = Collections.unmodifiableSet(
					new HashSet<String>(Arrays.asList("mem0", "llm", "rule", "hybrid")));

			/** 受支持的写入模式名 */
			static final Set<String> SUPPORTED_WRITE_MODES = Collections.unmodifiableSet(
					new HashSet<String>(Arrays.asList("per-turn", "batched", "session-end")));

			/** 受支持的召回策略名 */
			static final Set<String> SUPPORTED_RETRIEVERS = Collections.unmodifiableSet(
					new HashSet<String>(Arrays.asList("semantic", "keyword", "hybrid", "rerank")));

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

			public Duration getConnectTimeout() {
				return connectTimeout;
			}

			public void setConnectTimeout(Duration connectTimeout) {
				this.connectTimeout = connectTimeout;
			}

			public Duration getReadTimeout() {
				return readTimeout;
			}

			public void setReadTimeout(Duration readTimeout) {
				this.readTimeout = readTimeout;
			}

			public String getExtractor() {
				return extractor;
			}

			public void setExtractor(String extractor) {
				this.extractor = extractor;
			}

			public String getWriteMode() {
				return writeMode;
			}

			public void setWriteMode(String writeMode) {
				this.writeMode = writeMode;
			}

			public int getBatchSize() {
				return batchSize;
			}

			public void setBatchSize(int batchSize) {
				this.batchSize = batchSize;
			}

			public String getRetriever() {
				return retriever;
			}

			public void setRetriever(String retriever) {
				this.retriever = retriever;
			}

			public int getTopK() {
				return topK;
			}

			public void setTopK(int topK) {
				this.topK = topK;
			}

			public int getMaxFactsTokens() {
				return maxFactsTokens;
			}

			public void setMaxFactsTokens(int maxFactsTokens) {
				this.maxFactsTokens = maxFactsTokens;
			}

			public boolean isExtractAsync() {
				return extractAsync;
			}

			public void setExtractAsync(boolean extractAsync) {
				this.extractAsync = extractAsync;
			}

			public String getExtractModel() {
				return extractModel;
			}

			public void setExtractModel(String extractModel) {
				this.extractModel = extractModel;
			}

			public String getRerankModel() {
				return rerankModel;
			}

			public void setRerankModel(String rerankModel) {
				this.rerankModel = rerankModel;
			}

			public void validate() {
				if (batchSize < 1) {
					throw illegal("session.facts.batch-size", batchSize, "批量写入阈值必须 >= 1");
				}
				if (topK < 1) {
					throw illegal("session.facts.top-k", topK, "召回条数上限必须 >= 1");
				}
				if (maxFactsTokens < 1) {
					throw illegal("session.facts.max-facts-tokens", maxFactsTokens, "事实段 token 上限必须 >= 1");
				}
				if (!SUPPORTED_EXTRACTORS.contains(extractor)) {
					throw illegal("session.facts.extractor", extractor, "抽取策略只支持 mem0/llm/rule/hybrid");
				}
				if (!SUPPORTED_WRITE_MODES.contains(writeMode)) {
					throw illegal("session.facts.write-mode", writeMode, "写入模式只支持 per-turn/batched/session-end");
				}
				if (!SUPPORTED_RETRIEVERS.contains(retriever)) {
					throw illegal("session.facts.retriever", retriever, "召回策略只支持 semantic/keyword/hybrid/rerank");
				}
				if (connectTimeout == null || connectTimeout.isZero() || connectTimeout.isNegative()) {
					throw illegal("session.facts.connect-timeout", connectTimeout, "连接超时必须为正时长");
				}
				if (readTimeout == null || readTimeout.isZero() || readTimeout.isNegative()) {
					throw illegal("session.facts.read-timeout", readTimeout, "读取超时必须为正时长");
				}
			}
		}
	}
}
