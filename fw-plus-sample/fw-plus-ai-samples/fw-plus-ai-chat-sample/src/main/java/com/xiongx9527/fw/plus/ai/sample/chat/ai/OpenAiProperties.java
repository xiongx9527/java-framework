package com.xiongx9527.fw.plus.ai.sample.chat.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 流式模型所需的 OpenAI 配置，前缀与框架侧一致：{@value #PREFIX}。
 *
 * <p>样例的流式链路用的是 OpenAI 集成（指向 Ollama 的 OpenAI 兼容端点），所以配置挂在
 * {@code langchain4j.open-ai} 下而不是 {@code langchain4j.ollama}——后者是 Ollama 原生协议。</p>
 *
 * <p>字段与框架侧 {@code dev.langchain4j.openai.spring.ChatModelProperties} 的键名对齐（取用到的那部分），
 * yaml 键名完全兼容；样板自建 Bean 只为挂 {@link ChatContextLogger}，与 {@link OllamaProperties} 同一套路。</p>
 */
@ConfigurationProperties(prefix = OpenAiProperties.PREFIX)
public class OpenAiProperties {

	public static final String PREFIX = "langchain4j.open-ai";

	private StreamingChatModelProperties streamingChatModel = new StreamingChatModelProperties();

	public StreamingChatModelProperties getStreamingChatModel() {
		return streamingChatModel;
	}

	public void setStreamingChatModel(StreamingChatModelProperties streamingChatModel) {
		this.streamingChatModel = streamingChatModel;
	}

	/** 流式模型参数，键名与框架侧 OpenAI 属性一致。 */
	public static class StreamingChatModelProperties {

		private String baseUrl;
		private String apiKey;
		private String modelName;
		private Double temperature;
		private Double topP;
		private List<String> stop;
		private Integer maxTokens;
		private Integer seed;
		private Duration timeout;
		private Map<String, String> customHeaders;
		private Boolean logRequests;
		private Boolean logResponses;

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

		public String getModelName() {
			return modelName;
		}

		public void setModelName(String modelName) {
			this.modelName = modelName;
		}

		public Double getTemperature() {
			return temperature;
		}

		public void setTemperature(Double temperature) {
			this.temperature = temperature;
		}

		public Double getTopP() {
			return topP;
		}

		public void setTopP(Double topP) {
			this.topP = topP;
		}

		public List<String> getStop() {
			return stop;
		}

		public void setStop(List<String> stop) {
			this.stop = stop;
		}

		public Integer getMaxTokens() {
			return maxTokens;
		}

		public void setMaxTokens(Integer maxTokens) {
			this.maxTokens = maxTokens;
		}

		public Integer getSeed() {
			return seed;
		}

		public void setSeed(Integer seed) {
			this.seed = seed;
		}

		public Duration getTimeout() {
			return timeout;
		}

		public void setTimeout(Duration timeout) {
			this.timeout = timeout;
		}

		public Map<String, String> getCustomHeaders() {
			return customHeaders;
		}

		public void setCustomHeaders(Map<String, String> customHeaders) {
			this.customHeaders = customHeaders;
		}

		public Boolean getLogRequests() {
			return logRequests;
		}

		public void setLogRequests(Boolean logRequests) {
			this.logRequests = logRequests;
		}

		public Boolean getLogResponses() {
			return logResponses;
		}

		public void setLogResponses(Boolean logResponses) {
			this.logResponses = logResponses;
		}
	}
}
