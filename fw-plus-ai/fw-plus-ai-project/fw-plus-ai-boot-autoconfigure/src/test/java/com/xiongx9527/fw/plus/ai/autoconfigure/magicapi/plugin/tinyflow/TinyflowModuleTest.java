package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.module.TinyflowModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.service.MagicApiTinyflowWorkflowRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;
import com.xiongx9527.fw.plus.ai.tinyflow.runtime.TinyflowAsyncResult;
import com.xiongx9527.fw.plus.ai.tinyflow.runtime.TinyflowService;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowExecutionResult;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflow;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TinyflowModuleTest {

	@Test
	void shouldResolvePathAndExecuteSavedWorkflow() {
		MagicApiTinyflowWorkflowRepository repository = repository("flow-order", "customer-service/order-agent");
		RecordingService service = new RecordingService();
		TinyflowModule module = new TinyflowModule(repository, service);

		Object result = module.getDynamicModule("customer-service/order-agent")
				.execute(Collections.<String, Object>singletonMap("orderId", "A-1"));

		assertEquals("done", result);
		assertEquals("flow-order", service.executedWorkflowId);
		assertEquals("A-1", service.executedVariables.get("orderId"));
	}

	@Test
	void shouldRejectUnknownPathAndAmbiguousPathStably() {
		RecordingService service = new RecordingService();
		MagicApiTinyflowWorkflowRepository repository = repository("flow-a", "orders/create");
		TinyflowModule module = new TinyflowModule(repository, service);

		TinyflowException unknown = assertThrows(TinyflowException.class,
				() -> module.getDynamicModule("orders/missing"));
		assertEquals("Tinyflow workflow not found: orders/missing", unknown.getMessage());

		repository.save(new TinyflowWorkflow("flow-b", "orders/create", "duplicate", "{}"));
		TinyflowException ambiguous = assertThrows(TinyflowException.class,
				() -> module.getDynamicModule("orders/create"));
		assertEquals("Workflow path is ambiguous: orders/create, matching ids: [flow-a, flow-b]",
				ambiguous.getMessage());
	}

	@Test
	void shouldDelegateAsyncAndResumeToTheSameService() {
		RecordingService service = new RecordingService();
		TinyflowModule module = new TinyflowModule(repository("flow-order", "orders/create"), service);

		assertEquals("instance-1", module.getDynamicModule("orders/create")
				.executeAsync(Collections.<String, Object>emptyMap()).getInstanceId());
		assertEquals("done", module.resume("instance-1", Collections.<String, Object>emptyMap()));
		assertEquals("flow-order", service.asyncWorkflowId);
		assertEquals("instance-1", service.resumedInstanceId);
	}

	private MagicApiTinyflowWorkflowRepository repository(String id, String path) {
		MagicApiTinyflowWorkflowRepository repository = new MagicApiTinyflowWorkflowRepository();
		repository.save(new TinyflowWorkflow(id, path, id, "{}"));
		return repository;
	}

	private static final class RecordingService implements TinyflowService {
		private String executedWorkflowId;
		private Map<String, Object> executedVariables;
		private String asyncWorkflowId;
		private String resumedInstanceId;

		@Override public void validate(TinyflowWorkflow workflow) { }
		@Override public void register(TinyflowWorkflow workflow) { }
		@Override public void remove(String workflowId) { }

		@Override
		public TinyflowExecutionResult execute(String workflowId, Map<String, Object> variables) {
			executedWorkflowId = workflowId;
			executedVariables = variables;
			return result();
		}

		@Override
		public TinyflowAsyncResult executeAsync(String workflowId, Map<String, Object> variables) {
			asyncWorkflowId = workflowId;
			return new TinyflowAsyncResult("instance-1");
		}

		@Override
		public TinyflowExecutionResult resume(String instanceId, Map<String, Object> variables) {
			resumedInstanceId = instanceId;
			return result();
		}

		@Override public TinyflowAsyncResult resumeAsync(String instanceId, Map<String, Object> variables) {
			return new TinyflowAsyncResult(instanceId);
		}

		private TinyflowExecutionResult result() {
			return new TinyflowExecutionResult("instance-1", Collections.<String, Object>singletonMap("result", "done"), false);
		}
	}
}
