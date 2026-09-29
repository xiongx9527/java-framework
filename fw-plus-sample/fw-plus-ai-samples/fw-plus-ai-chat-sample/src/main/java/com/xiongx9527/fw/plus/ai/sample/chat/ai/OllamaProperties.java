package com.xiongx9527.fw.plus.ai.sample.chat.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 样例自建模型所需的 Ollama 配置，前缀与框架侧保持一致：{@value #PREFIX}。
 *
 * <p>存在两个原因：</p>
 * <ul>
 *   <li>样例要自己构造模型 Bean 以便挂载 {@link ChatContextLogger}，不能依赖
 *       {@code dev.langchain4j.ollama.spring.AutoConfig} 产出的 Bean；</li>
 *   <li>框架侧对应的 {@code ChatModelProperties} 是包私有且用 lombok 生成访问器，
 *       样例跨包无法复用。</li>
 * </ul>
 *
 * <p>字段与框架侧对齐，yaml 键名完全兼容——恢复默认的 {@code fw.plus.ai.langchain4j.ollama.enabled}
 * 并去掉样例自建的模型 Bean，即可切回框架装配。</p>
 */
@ConfigurationProperties(prefix = OllamaProperties.PREFIX)
public class OllamaProperties {

	public static final String PREFIX = "langchain4j.ollama";

	private ModelProperties chatModel = new ModelProperties();
	private ModelProperties streamingChatModel = new ModelProperties();

	public ModelProperties getChatModel() {
		return chatModel;
	}

	public void setChatModel(ModelProperties chatModel) {
		this.chatModel = chatModel;
	}

	public ModelProperties getStreamingChatModel() {
		return streamingChatModel;
	}

	public void setStreamingChatModel(ModelProperties streamingChatModel) {
		this.streamingChatModel = streamingChatModel;
	}

	/** 单个模型的连接与生成参数，同步与流式各一份。 */
	public static class ModelProperties {

		private String baseUrl;
		private String modelName;
		private Double temperature;
		private Integer topK;
		private Double topP;
		private Double repeatPenalty;
		private Integer seed;
		private Integer numPredict;
		private List<String> stop;
		private String format;
		private Duration timeout;
		private Integer maxRetries;
		private Map<String, String> customHeaders;
		private Boolean logRequests;
		private Boolean logResponses;

		public String getBaseUrl() {
			return baseUrl;
		}

		public void setBaseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
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

		public Integer getTopK() {
			return topK;
		}

		public void setTopK(Integer topK) {
			this.topK = topK;
		}

		public Double getTopP() {
			return topP;
		}

		public void setTopP(Double topP) {
			this.topP = topP;
		}

		public Double getRepeatPenalty() {
			return repeatPenalty;
		}

		public void setRepeatPenalty(Double repeatPenalty) {
			this.repeatPenalty = repeatPenalty;
		}

		public Integer getSeed() {
			return seed;
		}

		public void setSeed(Integer seed) {
			this.seed = seed;
		}

		public Integer getNumPredict() {
			return numPredict;
		}

		public void setNumPredict(Integer numPredict) {
			this.numPredict = numPredict;
		}

		public List<String> getStop() {
			return stop;
		}

		public void setStop(List<String> stop) {
			this.stop = stop;
		}

		public String getFormat() {
			return format;
		}

		public void setFormat(String format) {
			this.format = format;
		}

		public Duration getTimeout() {
			return timeout;
		}

		public void setTimeout(Duration timeout) {
			this.timeout = timeout;
		}

		public Integer getMaxRetries() {
			return maxRetries;
		}

		public void setMaxRetries(Integer maxRetries) {
			this.maxRetries = maxRetries;
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
