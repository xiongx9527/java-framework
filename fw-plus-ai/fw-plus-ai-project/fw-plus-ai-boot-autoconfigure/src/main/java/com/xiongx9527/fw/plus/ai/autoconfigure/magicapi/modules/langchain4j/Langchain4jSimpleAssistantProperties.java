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
 *   key: my-assistant            # 默认 key：所有资源类型共用
 *   keys:                        # 分类型专属 key（缺省回落默认 key）
 *     chatModel: qwen-main
 *     streamingModel: qwen-stream
 *     chatMemoryStore: redis-store
 *     toolProvider: weather-tools
 *     contentRetriever: kb-retriever
 *     contentInjector: prompt-injector
 *     systemMessage: prompt/assistant
 *     tokenizer: openai
 *     scoringModel: jina
 * }</pre>
 */
@ConfigurationProperties(prefix = "fw.plus.magicapi.langchain4j.simple-assistant")
public class Langchain4jSimpleAssistantProperties {

	/** 注入开关：关闭后 simpleAssistant 拒绝装配 */
	private boolean enabled = true;

	/** 默认 key：所有资源类型共用 */
	private String key;

	/** 分类型专属 key：类型名（chatModel / streamingModel / chatMemoryStore / toolProvider /
	 * contentRetriever / contentInjector / systemMessage / tokenizer / scoringModel）→ 资源 key，
	 * 未配置的类型回落 {@link #key}。 */
	private Map<String, String> keys = new HashMap<>();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
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
}
