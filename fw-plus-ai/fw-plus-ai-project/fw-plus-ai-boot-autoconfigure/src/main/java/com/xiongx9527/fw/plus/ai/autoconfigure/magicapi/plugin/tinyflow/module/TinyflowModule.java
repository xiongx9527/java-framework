package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.module;

import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;
import com.xiongx9527.fw.plus.ai.tinyflow.runtime.TinyflowService;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowExecutionResult;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflow;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflowRepository;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

import java.util.Map;

@MagicModule("tinyflow")
public final class TinyflowModule implements DynamicAttribute<TinyflowResourceModule, TinyflowResourceModule>,
		DynamicModule<TinyflowModule> {

	private final TinyflowWorkflowRepository workflowRepository;
	private final TinyflowService tinyflowService;

	public TinyflowModule(TinyflowWorkflowRepository workflowRepository, TinyflowService tinyflowService) {
		if (workflowRepository == null) {
			throw new IllegalArgumentException("workflowRepository must not be null");
		}
		if (tinyflowService == null) {
			throw new IllegalArgumentException("tinyflowService must not be null");
		}
		this.workflowRepository = workflowRepository;
		this.tinyflowService = tinyflowService;
	}

	@Override
	public TinyflowResourceModule getDynamicAttribute(String path) {
		return getDynamicModule(path);
	}

	public TinyflowResourceModule getDynamicModule(String path) {
		TinyflowWorkflow workflow = workflowRepository.findByPath(path);
		if (workflow == null) {
			throw new TinyflowException("Tinyflow workflow not found: " + path);
		}
		return new TinyflowResourceModule(workflow.getId(), tinyflowService);
	}

	@Override
	public TinyflowModule getDynamicModule(MagicScriptContext context) {
		return this;
	}

	public Object resume(String instanceId, Map<String, Object> variables) {
		TinyflowExecutionResult result = tinyflowService.resume(instanceId, variables);
		if (result != null && result.getOutputs().size() == 1 && result.getOutputs().containsKey("result")) {
			return result.getOutputs().get("result");
		}
		return result;
	}

	public Object resumeAsync(String instanceId, Map<String, Object> variables) {
		return tinyflowService.resumeAsync(instanceId, variables);
	}
}
