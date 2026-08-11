package com.xiongx9527.fw.plus.ai.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AiChatRequest {

	private final String input;

	private final Map<String, Object> parameters;

	private final long timeoutMillis;

	public AiChatRequest(String input, Map<String, Object> parameters, long timeoutMillis) {
		if (input == null) {
			throw new IllegalArgumentException("input cannot be null");
		}
		if (timeoutMillis < 0L) {
			throw new IllegalArgumentException("timeoutMillis cannot be negative");
		}
		this.input = input;
		this.parameters = immutableParameters(parameters);
		this.timeoutMillis = timeoutMillis;
	}

	public String getInput() {
		return input;
	}

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public long getTimeoutMillis() {
		return timeoutMillis;
	}

	private static Map<String, Object> immutableParameters(Map<String, Object> parameters) {
		if (parameters == null || parameters.isEmpty()) {
			return Collections.emptyMap();
		}
		return Collections.unmodifiableMap(new LinkedHashMap<String, Object>(parameters));
	}
}
