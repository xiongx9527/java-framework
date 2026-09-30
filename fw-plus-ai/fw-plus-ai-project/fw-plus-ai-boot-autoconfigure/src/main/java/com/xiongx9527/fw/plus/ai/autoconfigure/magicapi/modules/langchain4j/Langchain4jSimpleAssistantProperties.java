package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j;

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
 *     chatMemoryStore: redis-store
 *     toolProvider: weather-tools
 *     contentRetriever: kb-retriever
 *     contentInjector: prompt-injector
 *     systemMessage: prompt/assistant
 *     tokenizer: openai
 *     scoringModel: jina
 *   memory-max-messages: 20      # 进程内记忆窗口（条）；配置 tokenizer 后改按 token 截断
 *   max-tokens: 1000             # TokenWindowChatMemory 的 token 上限（需配置 tokenizer）
 *   rerank-min-score: 0.6        # rerank 生效下限（需配置 scoringModel）
 * }</pre>
 */
@ConfigurationProperties(prefix = "fw.plus.magicapi.langchain4j.simple-assistant")
public class Langchain4jSimpleAssistantProperties {

	/** 注入开关：关闭后 simpleAssistant 不装配 */
	private boolean enabled = true;

	/** 是否回落默认 key：分类型专属 key 未配置时，取 {@link #key} 作为该类型的资源 key；缺省 false（未配置专属 key 的类型跳过） */
	private boolean useDefaultKey = false;

	/** 分类型是否回落默认 key（键为类型名，值同 {@link #useDefaultKey} 语义）；未配置的类型回落 {@link #useDefaultKey} */
	private Map<String, Boolean> useDefaultKeys = new HashMap<>();

	/** 默认 key：所有资源类型共用 */
	private String key;

	/** 分类型专属 key：类型名（chatModel / streamingModel / chatMemoryStore / toolProvider /
	 * contentRetriever / contentInjector / systemMessage / tokenizer / scoringModel）→ 资源 key，
	 * 未配置的类型回落 {@link #key}。 */
	private Map<String, String> keys = new HashMap<>();

	/** 进程内记忆窗口（条，每 memoryId 一份）；null 时用 AssistantBuilder 默认（20） */
	private Integer memoryMaxMessages;

	/** TokenWindowChatMemory 的 token 上限（仅在配置了 tokenizer 类型 key 时生效）；null 时用默认（1000） */
	private Integer maxTokens;

	/** rerank 生效下限：低于该分数的检索条目丢弃（仅在配置了 scoring-model 类型 key 时生效） */
	private Double rerankMinScore;

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
}
