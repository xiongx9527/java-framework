package com.xiongx9527.fw.plus.ai.tinyflow.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TinyflowExecutionRequest {

	private final String targetId;
	private final Map<String, Object> variables;

	public TinyflowExecutionRequest(String targetId, Map<String, Object> variables) {
		if (targetId == null || targetId.trim().length() == 0) {
			throw new IllegalArgumentException("targetId must not be blank");
		}
		this.targetId = targetId.trim();
		this.variables = variables == null ? Collections.<String, Object>emptyMap()
				: Collections.unmodifiableMap(new LinkedHashMap<String, Object>(variables));
	}

	public String getTargetId() {
		return targetId;
	}

	public Map<String, Object> getVariables() {
		return variables;
	}
}
