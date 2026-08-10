package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.module;

import com.xiongx9527.fw.plus.ai.tinyflow.runtime.TinyflowAsyncResult;
import com.xiongx9527.fw.plus.ai.tinyflow.runtime.TinyflowService;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowExecutionResult;

import java.util.Map;

public final class TinyflowResourceModule {

	private final String workflowId;
	private final TinyflowService tinyflowService;

	TinyflowResourceModule(String workflowId, TinyflowService tinyflowService) {
		this.workflowId = workflowId;
		this.tinyflowService = tinyflowService;
	}

	public Object execute(Map<String, Object> variables) {
		return result(tinyflowService.execute(workflowId, variables));
	}

	public TinyflowAsyncResult executeAsync(Map<String, Object> variables) {
		return tinyflowService.executeAsync(workflowId, variables);
	}

	private Object result(TinyflowExecutionResult execution) {
		if (execution != null && execution.getOutputs().size() == 1 && execution.getOutputs().containsKey("result")) {
			return execution.getOutputs().get("result");
		}
		return execution;
	}
}
