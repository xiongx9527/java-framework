package com.xiongx9527.fw.plus.ai.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class AiEmbeddingRequest {

	private final List<String> inputs;

	private final Map<String, Object> parameters;

	private final long timeoutMillis;

	public AiEmbeddingRequest(List<String> inputs, Map<String, Object> parameters, long timeoutMillis) {
		if (inputs == null || inputs.isEmpty()) {
			throw new IllegalArgumentException("inputs cannot be empty");
		}
		List<String> inputCopy = new ArrayList<String>(inputs.size());
		for (String input : inputs) {
			if (input == null) {
				throw new IllegalArgumentException("inputs cannot contain null");
			}
			inputCopy.add(input);
		}
		if (timeoutMillis < 0L) {
			throw new IllegalArgumentException("timeoutMillis cannot be negative");
		}
		this.inputs = Collections.unmodifiableList(inputCopy);
		this.parameters = immutableParameters(parameters);
		this.timeoutMillis = timeoutMillis;
	}

	public List<String> getInputs() {
		return inputs;
	}

	public Map<String, Object> getParameters() {
		return AiModelImmutableValues.immutableMap(parameters);
	}

	public long getTimeoutMillis() {
		return timeoutMillis;
	}

	private static Map<String, Object> immutableParameters(Map<String, Object> parameters) {
		return AiModelImmutableValues.immutableMap(parameters);
	}
}
