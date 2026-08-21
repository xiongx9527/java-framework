package com.xiongx9527.fw.plus.ai.tinyflow.runtime;

public final class TinyflowAsyncResult {

	private final String instanceId;

	public TinyflowAsyncResult(String instanceId) {
		if (instanceId == null || instanceId.trim().length() == 0) {
			throw new IllegalArgumentException("instanceId must not be blank");
		}
		this.instanceId = instanceId;
	}

	public String getInstanceId() {
		return instanceId;
	}
}
