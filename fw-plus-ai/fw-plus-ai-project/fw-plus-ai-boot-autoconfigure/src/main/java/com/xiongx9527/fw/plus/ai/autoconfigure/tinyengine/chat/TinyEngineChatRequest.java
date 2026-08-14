package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties({"baseUrl", "apiKey", "modelSettings"})
public class TinyEngineChatRequest {

	private Integer appId;
	private Long serviceId;
	private String provider;
	private String modelName;
	private String modelKey;
	private String model;
	private String baseUrl;
	private String apiKey;
	private Map<String, Object> modelSettings;
	private String message;
	private List<TinyEngineChatMessage> messages = new ArrayList<TinyEngineChatMessage>();
	private Boolean stream;
	private Integer maxTokens;
	private Double temperature;
	private Double topP;
	private Double presencePenalty;
	private Double frequencyPenalty;
	private String responseFormat;
	private Boolean enableThinking;
	private String toolChoice;
	private Boolean parallelToolCalls;
	private Boolean enableSearch;
	private Map<String, Object> streamOptions;
	private Map<String, Object> searchOptions;
	private List<Map<String, Object>> tools = new ArrayList<Map<String, Object>>();

	public Integer getAppId() {
		return appId;
	}

	public void setAppId(Integer appId) {
		this.appId = appId;
	}

	public Long getServiceId() {
		return serviceId;
	}

	public void setServiceId(Long serviceId) {
		this.serviceId = serviceId;
	}

	@JsonIgnore
	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	public String getModelKey() {
		return modelKey;
	}

	public void setModelKey(String modelKey) {
		this.modelKey = modelKey;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	@JsonIgnore
	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	@JsonIgnore
	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	@JsonIgnore
	public Map<String, Object> getModelSettings() {
		return modelSettings;
	}

	public void setModelSettings(Map<String, Object> modelSettings) {
		this.modelSettings = modelSettings;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public List<TinyEngineChatMessage> getMessages() {
		return messages;
	}

	public void setMessages(List<TinyEngineChatMessage> messages) {
		this.messages = messages == null ? new ArrayList<TinyEngineChatMessage>() : new ArrayList<TinyEngineChatMessage>(messages);
	}

	public Boolean getStream() {
		return stream;
	}

	public void setStream(Boolean stream) {
		this.stream = stream;
	}

	public Integer getMaxTokens() {
		return maxTokens;
	}

	@JsonAlias("max_tokens")
	public void setMaxTokens(Integer maxTokens) {
		this.maxTokens = maxTokens;
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

	@JsonAlias("top_p")
	public void setTopP(Double topP) {
		this.topP = topP;
	}

	public Double getPresencePenalty() {
		return presencePenalty;
	}

	@JsonAlias("presence_penalty")
	public void setPresencePenalty(Double presencePenalty) {
		this.presencePenalty = presencePenalty;
	}

	public Double getFrequencyPenalty() {
		return frequencyPenalty;
	}

	@JsonAlias("frequency_penalty")
	public void setFrequencyPenalty(Double frequencyPenalty) {
		this.frequencyPenalty = frequencyPenalty;
	}

	public String getResponseFormat() {
		return responseFormat;
	}

	public void setResponseFormat(String responseFormat) {
		this.responseFormat = responseFormat;
	}

	@JsonSetter("response_format")
	public void setOpenAiResponseFormat(Object responseFormat) {
		if (responseFormat instanceof Map) {
			Object type = ((Map<?, ?>) responseFormat).get("type");
			this.responseFormat = type == null ? null : String.valueOf(type);
		} else {
			this.responseFormat = responseFormat == null ? null : String.valueOf(responseFormat);
		}
	}

	public Boolean getEnableThinking() {
		return enableThinking;
	}

	@JsonAlias("enable_thinking")
	public void setEnableThinking(Boolean enableThinking) {
		this.enableThinking = enableThinking;
	}

	public String getToolChoice() {
		return toolChoice;
	}

	@JsonAlias("tool_choice")
	public void setToolChoice(String toolChoice) {
		this.toolChoice = toolChoice;
	}

	public Boolean getParallelToolCalls() {
		return parallelToolCalls;
	}

	@JsonAlias("parallel_tool_calls")
	public void setParallelToolCalls(Boolean parallelToolCalls) {
		this.parallelToolCalls = parallelToolCalls;
	}

	public Boolean getEnableSearch() {
		return enableSearch;
	}

	@JsonAlias("enable_search")
	public void setEnableSearch(Boolean enableSearch) {
		this.enableSearch = enableSearch;
	}

	public Map<String, Object> getStreamOptions() {
		return streamOptions;
	}

	@JsonAlias("stream_options")
	public void setStreamOptions(Map<String, Object> streamOptions) {
		this.streamOptions = streamOptions;
	}

	public Map<String, Object> getSearchOptions() {
		return searchOptions;
	}

	@JsonAlias("search_options")
	public void setSearchOptions(Map<String, Object> searchOptions) {
		this.searchOptions = searchOptions;
	}

	public List<Map<String, Object>> getTools() {
		return tools;
	}

	public void setTools(List<Map<String, Object>> tools) {
		this.tools = tools == null ? new ArrayList<Map<String, Object>>() : new ArrayList<Map<String, Object>>(tools);
	}
}
