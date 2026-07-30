package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TinyEngineChatRequest {

	private String modelKey;
	private String message;
	private List<TinyEngineChatMessage> messages = new ArrayList<TinyEngineChatMessage>();
	private Boolean stream;
	private Integer maxTokens;
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

	public String getModelKey() {
		return modelKey;
	}

	public void setModelKey(String modelKey) {
		this.modelKey = modelKey;
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

	public void setMaxTokens(Integer maxTokens) {
		this.maxTokens = maxTokens;
	}

	public Double getPresencePenalty() {
		return presencePenalty;
	}

	public void setPresencePenalty(Double presencePenalty) {
		this.presencePenalty = presencePenalty;
	}

	public Double getFrequencyPenalty() {
		return frequencyPenalty;
	}

	public void setFrequencyPenalty(Double frequencyPenalty) {
		this.frequencyPenalty = frequencyPenalty;
	}

	public String getResponseFormat() {
		return responseFormat;
	}

	public void setResponseFormat(String responseFormat) {
		this.responseFormat = responseFormat;
	}

	public Boolean getEnableThinking() {
		return enableThinking;
	}

	public void setEnableThinking(Boolean enableThinking) {
		this.enableThinking = enableThinking;
	}

	public String getToolChoice() {
		return toolChoice;
	}

	public void setToolChoice(String toolChoice) {
		this.toolChoice = toolChoice;
	}

	public Boolean getParallelToolCalls() {
		return parallelToolCalls;
	}

	public void setParallelToolCalls(Boolean parallelToolCalls) {
		this.parallelToolCalls = parallelToolCalls;
	}

	public Boolean getEnableSearch() {
		return enableSearch;
	}

	public void setEnableSearch(Boolean enableSearch) {
		this.enableSearch = enableSearch;
	}

	public Map<String, Object> getStreamOptions() {
		return streamOptions;
	}

	public void setStreamOptions(Map<String, Object> streamOptions) {
		this.streamOptions = streamOptions;
	}

	public Map<String, Object> getSearchOptions() {
		return searchOptions;
	}

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
