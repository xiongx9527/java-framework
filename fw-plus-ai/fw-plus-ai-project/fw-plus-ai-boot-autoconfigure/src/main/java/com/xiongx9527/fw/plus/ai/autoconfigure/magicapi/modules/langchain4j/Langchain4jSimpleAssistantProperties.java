package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.EphemeralChatMemoryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code simpleAssistant} 按 key 一键装配的配置属性。
 *
 * <pre>{@code
 * fw.plus.magicapi.langchain4j.simple-assistant:
 *   enabled: true                # 注入开关（缺省开启）
 *   use-default-key: false       # 分类型专属 key 未配置时是否回落默认 key（缺省 false：该类型跳过）
 *   use-default-keys:            # 分类型是否回落默认 key（未配置的类型回落 use-default-key）
 *     chatModel: true
 *   key: my-assistant            # 默认 key：所有资源类型共用（use-default-key / use-default-keys 命中时生效）
 *   keys:                        # 分类型专属 key（每个类型建议都配置）
 *     chatModel: qwen-main
 *     streamingModel: qwen-stream
 *     tokenizer: openai
 *     scoringModel: jina
 *   memory-max-messages: 20      # 进程内记忆窗口（条）；配置 tokenizer 后改按 token 截断
 *   max-tokens: 1000             # TokenWindowChatMemory 的 token 上限（需配置 tokenizer）
 *   rerank-min-score: 0.6        # rerank 生效下限（需配置 scoringModel）
 *   rerank-max-results: 5        # rerank 保留上限（需配置 scoringModel；不配保留全部）
 *   similarity-weight: 0.3       # 相似度分权重（与 semantic-weight 成对配置启用加权混合重排）
 *   semantic-weight: 0.7         # 语义分权重（加权混合重排：总分 = 相似度×本组权重 + 语义分×semantic-weight）
 * }</pre>
 */
@ConfigurationProperties(prefix = "fw.plus.magicapi.langchain4j.simple-assistant")
public class Langchain4jSimpleAssistantProperties {

	/** 注入开关：关闭后 simpleAssistant 不装配 */
	private boolean enabled = true;

	/** 是否回落默认 key：分类型专属 key 未配置时，取 {@link #key} 作为该类型的资源 key；缺省 false（未配置专属 key 的类型跳过） */
	private boolean useDefaultKey = false;

	/** langchain4jModule 是否必须依赖 SimpleAssistant：true 时 SimpleAssistant 缺席会导致启动失败；缺省 false（软依赖，缺席为 null） */
	private boolean simpleAssistantRequired = false;

	/** 分类型是否回落默认 key（键为类型名，值同 {@link #useDefaultKey} 语义）；未配置的类型回落 {@link #useDefaultKey} */
	private Map<String, Boolean> useDefaultKeys = new HashMap<>();

	/** 默认 key：所有资源类型共用 */
	private String key;

	/** 分类型专属 key：类型名（chatModel / streamingModel / languageModel /
	 * streamingLanguageModel / tokenizer / scoringModel / skill）→ 资源 key
	 * （skill 支持逗号分隔多个），未配置的类型回落 {@link #key}。 */
	private Map<String, String> keys = new HashMap<>();

	/** 进程内记忆窗口（条，每 memoryId 一份）；null 时用 AssistantBuilder 默认（20） */
	private Integer memoryMaxMessages;

	/** TokenWindowChatMemory 的 token 上限（仅在配置了 tokenizer 类型 key 时生效）；null 时用默认（1000） */
	private Integer maxTokens;

	/** rerank 生效下限：低于该分数的检索条目丢弃（仅在配置了 scoring-model 类型 key 时生效） */
	private Double rerankMinScore;

	/** rerank 保留上限：重排后最多保留的条目数（仅在配置了 scoring-model 类型 key 时生效；null 保留全部） */
	private Integer rerankMaxResults;

	/** 相似度分权重（0~1，与 {@link #semanticWeight} 成对配置时启用加权混合重排；null 不启用） */
	private Double similarityWeight;

	/** 语义分权重（0~1，加权混合重排：总分 = 相似度分 × similarityWeight + 语义分 × semanticWeight） */
	private Double semanticWeight;

	/** Ephemeral 记忆配置：AI 持久化策略、长度限制、RAG/工具往返处理等（见 {@link EphemeralChatMemoryProperties}） */
	private EphemeralChatMemoryProperties ephemeral = new EphemeralChatMemoryProperties();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isUseDefaultKey() {
		return useDefaultKey;
	}

	public void setUseDefaultKey(boolean useDefaultKey) {
		this.useDefaultKey = useDefaultKey;
	}

	public boolean isSimpleAssistantRequired() {
		return simpleAssistantRequired;
	}

	public void setSimpleAssistantRequired(boolean simpleAssistantRequired) {
		this.simpleAssistantRequired = simpleAssistantRequired;
	}

	public Map<String, Boolean> getUseDefaultKeys() {
		return useDefaultKeys;
	}

	public void setUseDefaultKeys(Map<String, Boolean> useDefaultKeys) {
		this.useDefaultKeys = useDefaultKeys;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Map<String, String> getKeys() {
		return keys;
	}

	public void setKeys(Map<String, String> keys) {
		this.keys = keys;
	}

	public Integer getMemoryMaxMessages() {
		return memoryMaxMessages;
	}

	public void setMemoryMaxMessages(Integer memoryMaxMessages) {
		this.memoryMaxMessages = memoryMaxMessages;
	}

	public Integer getMaxTokens() {
		return maxTokens;
	}

	public void setMaxTokens(Integer maxTokens) {
		this.maxTokens = maxTokens;
	}

	public Double getRerankMinScore() {
		return rerankMinScore;
	}

	public void setRerankMinScore(Double rerankMinScore) {
		this.rerankMinScore = rerankMinScore;
	}

	public Integer getRerankMaxResults() {
		return rerankMaxResults;
	}

	public void setRerankMaxResults(Integer rerankMaxResults) {
		this.rerankMaxResults = rerankMaxResults;
	}

	public Double getSimilarityWeight() {
		return similarityWeight;
	}

	public void setSimilarityWeight(Double similarityWeight) {
		this.similarityWeight = similarityWeight;
	}

	public Double getSemanticWeight() {
		return semanticWeight;
	}

	public void setSemanticWeight(Double semanticWeight) {
		this.semanticWeight = semanticWeight;
	}

	public EphemeralChatMemoryProperties getEphemeral() {
		return ephemeral;
	}

	public void setEphemeral(EphemeralChatMemoryProperties ephemeral) {
		this.ephemeral = ephemeral;
	}
}
