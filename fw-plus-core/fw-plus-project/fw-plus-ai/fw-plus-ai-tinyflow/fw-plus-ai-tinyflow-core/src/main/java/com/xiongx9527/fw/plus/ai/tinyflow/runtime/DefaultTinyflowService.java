package com.xiongx9527.fw.plus.ai.tinyflow.runtime;

import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowExecutionResult;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflow;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflowRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflowValidator;
import dev.tinyflow.core.chain.ChainDefinition;
import dev.tinyflow.core.chain.ChainDefinitionNotFoundException;
import dev.tinyflow.core.chain.ChainState;
import dev.tinyflow.core.chain.ChainStateNotFoundException;
import dev.tinyflow.core.chain.ChainSuspendException;
import dev.tinyflow.core.chain.repository.ChainStateRepository;
import dev.tinyflow.core.chain.runtime.ChainExecutor;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DefaultTinyflowService implements TinyflowService {

	private static final int WORKFLOW_LOCK_COUNT = 64;

	private final TinyflowWorkflowValidator validator;
	private final TinyflowWorkflowRepository workflowRepository;
	private final TinyflowChainDefinitionRepository definitionRepository;
	private final ChainExecutor chainExecutor;
	private final Duration executionTimeout;
	private final Object[] workflowLocks = createWorkflowLocks();

	public DefaultTinyflowService(TinyflowWorkflowValidator validator,
			TinyflowWorkflowRepository workflowRepository,
			TinyflowChainDefinitionRepository definitionRepository,
			ChainExecutor chainExecutor, Duration executionTimeout) {
		if (validator == null) {
			throw new IllegalArgumentException("validator must not be null");
		}
		if (workflowRepository == null) {
			throw new IllegalArgumentException("workflowRepository must not be null");
		}
		if (definitionRepository == null) {
			throw new IllegalArgumentException("definitionRepository must not be null");
		}
		if (chainExecutor == null) {
			throw new IllegalArgumentException("chainExecutor must not be null");
		}
		if (executionTimeout == null || executionTimeout.isZero() || executionTimeout.isNegative()) {
			throw new IllegalArgumentException("executionTimeout must be positive");
		}
		this.validator = validator;
		this.workflowRepository = workflowRepository;
		this.definitionRepository = definitionRepository;
		this.chainExecutor = chainExecutor;
		this.executionTimeout = executionTimeout;
	}

	@Override
	public void validate(TinyflowWorkflow workflow) {
		validateDefinition(workflow);
	}

	@Override
	public void register(TinyflowWorkflow workflow) {
		ChainDefinition definition = validateDefinition(workflow);
		synchronized (workflowLock(workflow.getId())) {
			workflowRepository.save(workflow);
			definitionRepository.register(definition);
		}
	}

	@Override
	public void remove(String workflowId) {
		String normalizedId = requireId(workflowId, "workflowId");
		synchronized (workflowLock(normalizedId)) {
			workflowRepository.delete(normalizedId);
			definitionRepository.remove(normalizedId);
		}
	}

	@Override
	public TinyflowExecutionResult execute(String workflowId, Map<String, Object> variables) {
		TinyflowExecutionRequest request = request(workflowId, variables);
		requireWorkflow(request.getTargetId());
		rejectInterruptedWait("workflow", request.getTargetId());
		try {
			Map<String, Object> outputs = chainExecutor.execute(request.getTargetId(), request.getVariables(),
					Collections.<String, Object>emptyMap(), executionTimeout.toNanos(), TimeUnit.NANOSECONDS);
			return completed(null, outputs);
		} catch (ChainSuspendException error) {
			return suspended(error);
		} catch (RuntimeException error) {
			throw executionFailure("workflow", request.getTargetId(), error);
		}
	}

	@Override
	public TinyflowAsyncResult executeAsync(String workflowId, Map<String, Object> variables) {
		TinyflowExecutionRequest request = request(workflowId, variables);
		requireWorkflow(request.getTargetId());
		try {
			String instanceId = chainExecutor.executeAsync(request.getTargetId(), request.getVariables());
			return new TinyflowAsyncResult(instanceId);
		} catch (RuntimeException error) {
			throw executionFailure("workflow", request.getTargetId(), error);
		}
	}

	@Override
	public TinyflowExecutionResult resume(String instanceId, Map<String, Object> variables) {
		TinyflowExecutionRequest request = request(instanceId, variables);
		requireExecutionState(request.getTargetId());
		rejectInterruptedWait("instance", request.getTargetId());
		try {
			Map<String, Object> outputs = chainExecutor.resume(request.getTargetId(), request.getVariables(),
					Collections.<String, Object>emptyMap(), executionTimeout.toNanos(), TimeUnit.NANOSECONDS);
			return completed(request.getTargetId(), outputs);
		} catch (ChainSuspendException error) {
			return suspended(error);
		} catch (RuntimeException error) {
			throw executionFailure("instance", request.getTargetId(), error);
		}
	}

	@Override
	public TinyflowAsyncResult resumeAsync(String instanceId, Map<String, Object> variables) {
		TinyflowExecutionRequest request = request(instanceId, variables);
		requireExecutionState(request.getTargetId());
		try {
			chainExecutor.resumeAsync(request.getTargetId(), request.getVariables());
			return new TinyflowAsyncResult(request.getTargetId());
		} catch (RuntimeException error) {
			throw executionFailure("instance", request.getTargetId(), error);
		}
	}

	private TinyflowExecutionRequest request(String targetId, Map<String, Object> variables) {
		try {
			return new TinyflowExecutionRequest(targetId, variables);
		} catch (IllegalArgumentException error) {
			throw new TinyflowException(error.getMessage(), error);
		}
	}

	private ChainDefinition validateDefinition(TinyflowWorkflow workflow) {
		if (workflow == null) {
			throw new IllegalArgumentException("workflow must not be null");
		}
		return validator.validate(workflow.getId(), workflow.getJson());
	}

	private void requireWorkflow(String workflowId) {
		if (workflowRepository.findById(workflowId) == null
				|| definitionRepository.getChainDefinitionById(workflowId) == null) {
			throw new TinyflowException("Tinyflow workflow not found: " + workflowId);
		}
	}

	private void requireExecutionState(String instanceId) {
		ChainStateRepository repository = chainExecutor.getChainStateRepository();
		ChainState state = repository instanceof TinyflowChainStateRepository
				? ((TinyflowChainStateRepository) repository).find(instanceId)
				: repository.load(instanceId);
		if (state == null || state.getChainDefinitionId() == null
				|| state.getChainDefinitionId().trim().length() == 0) {
			throw new TinyflowException("Tinyflow execution instance not found: " + instanceId);
		}
	}

	private TinyflowExecutionResult completed(String instanceId, Map<String, Object> outputs) {
		return new TinyflowExecutionResult(instanceId, cleanOutputs(outputs), false);
	}

	private TinyflowExecutionResult suspended(ChainSuspendException error) {
		return new TinyflowExecutionResult(error.getChainStateId(), Collections.<String, Object>emptyMap(), true);
	}

	private Map<String, Object> cleanOutputs(Map<String, Object> outputs) {
		if (outputs == null || outputs.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<String, Object> cleaned = new LinkedHashMap<String, Object>(outputs);
		chainExecutor.clearDefaultStates(cleaned);
		return cleaned;
	}

	private TinyflowException executionFailure(String targetType, String targetId, RuntimeException error) {
		if (error instanceof TinyflowException) {
			return (TinyflowException) error;
		}
		if (causedBy(error, TimeoutException.class)) {
			return new TinyflowException("Tinyflow execution timed out for " + targetType + ": " + targetId, error);
		}
		if (causedBy(error, InterruptedException.class)) {
			Thread.currentThread().interrupt();
			return new TinyflowException("Tinyflow execution interrupted for " + targetType + ": " + targetId, error);
		}
		if (error instanceof ChainDefinitionNotFoundException || error instanceof ChainStateNotFoundException) {
			return new TinyflowException("Tinyflow " + targetType + " not found: " + targetId, error);
		}
		return new TinyflowException("Tinyflow execution failed for " + targetType + ": " + targetId, error);
	}

	private void rejectInterruptedWait(String targetType, String targetId) {
		if (Thread.currentThread().isInterrupted()) {
			throw new TinyflowException("Tinyflow execution interrupted for " + targetType + ": " + targetId,
					new InterruptedException("Thread interrupted before Tinyflow execution wait"));
		}
	}

	private boolean causedBy(Throwable error, Class<? extends Throwable> type) {
		Throwable current = error;
		while (current != null) {
			if (type.isInstance(current)) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	private String requireId(String id, String fieldName) {
		if (id == null || id.trim().length() == 0) {
			throw new TinyflowException(fieldName + " must not be blank");
		}
		return id.trim();
	}

	private Object workflowLock(String workflowId) {
		return workflowLocks[workflowId.hashCode() & (workflowLocks.length - 1)];
	}

	private static Object[] createWorkflowLocks() {
		Object[] locks = new Object[WORKFLOW_LOCK_COUNT];
		for (int index = 0; index < locks.length; index++) {
			locks[index] = new Object();
		}
		return locks;
	}
}
