package com.xiongx9527.fw.plus.ai.tinyflow.workflow;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TinyflowExecutionResult {

	private final String instanceId;
	private final Map<String, Object> outputs;
	private final boolean suspended;

	public TinyflowExecutionResult(String instanceId, Map<String, Object> outputs, boolean suspended) {
		this.instanceId = instanceId;
		this.outputs = outputs == null ? Collections.<String, Object>emptyMap()
				: Collections.unmodifiableMap(new LinkedHashMap<String, Object>(outputs));
		this.suspended = suspended;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public Map<String, Object> getOutputs() {
		return outputs;
	}

	public boolean isSuspended() {
		return suspended;
	}
}
