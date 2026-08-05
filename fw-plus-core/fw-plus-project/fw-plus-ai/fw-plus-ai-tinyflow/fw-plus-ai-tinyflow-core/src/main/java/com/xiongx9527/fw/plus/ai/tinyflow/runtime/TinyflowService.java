package com.xiongx9527.fw.plus.ai.tinyflow.runtime;

import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowExecutionResult;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflow;

import java.util.Map;

public interface TinyflowService {

	void validate(TinyflowWorkflow workflow);

	void register(TinyflowWorkflow workflow);

	void remove(String workflowId);

	TinyflowExecutionResult execute(String workflowId, Map<String, Object> variables);

	TinyflowAsyncResult executeAsync(String workflowId, Map<String, Object> variables);

	TinyflowExecutionResult resume(String instanceId, Map<String, Object> variables);

	TinyflowAsyncResult resumeAsync(String instanceId, Map<String, Object> variables);
}
