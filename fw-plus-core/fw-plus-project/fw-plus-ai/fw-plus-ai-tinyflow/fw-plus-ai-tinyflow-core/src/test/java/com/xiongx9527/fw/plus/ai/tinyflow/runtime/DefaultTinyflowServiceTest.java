package com.xiongx9527.fw.plus.ai.tinyflow.runtime;

import com.alibaba.fastjson.JSONObject;
import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.InMemoryTinyflowWorkflowRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowExecutionResult;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflow;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflowValidator;
import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.chain.ChainConsts;
import dev.tinyflow.core.chain.ChainState;
import dev.tinyflow.core.chain.ChainStatus;
import dev.tinyflow.core.chain.repository.InMemoryChainStateRepository;
import dev.tinyflow.core.chain.repository.InMemoryNodeStateRepository;
import dev.tinyflow.core.chain.runtime.ChainExecutor;
import dev.tinyflow.core.node.BaseNode;
import dev.tinyflow.core.parser.BaseNodeParser;
import dev.tinyflow.core.parser.ChainParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class DefaultTinyflowServiceTest {

	private static final String SIMPLE_WORKFLOW = "{\"nodes\":["
			+ "{\"id\":\"start\",\"type\":\"startNode\"},"
			+ "{\"id\":\"template\",\"type\":\"templateNode\",\"data\":{\"template\":\"Hello #(name)\","
			+ "\"parameters\":[{\"id\":\"name\",\"name\":\"name\",\"refType\":\"input\","
			+ "\"dataType\":\"String\"}]}},"
			+ "{\"id\":\"end\",\"type\":\"endNode\",\"data\":{\"outputDefs\":["
			+ "{\"id\":\"greeting\",\"name\":\"greeting\",\"ref\":\"template.output\","
			+ "\"refType\":\"ref\",\"dataType\":\"String\"}]}}],"
			+ "\"edges\":["
			+ "{\"id\":\"e1\",\"source\":\"start\",\"target\":\"template\"},"
			+ "{\"id\":\"e2\",\"source\":\"template\",\"target\":\"end\"}]}";
	private static final String SUSPENDING_WORKFLOW = "{\"nodes\":["
			+ "{\"id\":\"start\",\"type\":\"startNode\"},"
			+ "{\"id\":\"approval\",\"type\":\"confirmNode\",\"data\":{\"message\":\"Approve?\"}},"
			+ "{\"id\":\"end\",\"type\":\"endNode\"}],"
			+ "\"edges\":["
			+ "{\"id\":\"e1\",\"source\":\"start\",\"target\":\"approval\"},"
			+ "{\"id\":\"e2\",\"source\":\"approval\",\"target\":\"end\"}]}";
	private static final String BLOCKING_WORKFLOW = "{\"nodes\":["
			+ "{\"id\":\"start\",\"type\":\"startNode\"},"
			+ "{\"id\":\"slow\",\"type\":\"blockingNode\"},"
			+ "{\"id\":\"end\",\"type\":\"endNode\"}],"
			+ "\"edges\":["
			+ "{\"id\":\"e1\",\"source\":\"start\",\"target\":\"slow\"},"
			+ "{\"id\":\"e2\",\"source\":\"slow\",\"target\":\"end\"}]}";

	private CountDownLatch blockingGate;
	private InMemoryTinyflowWorkflowRepository workflowRepository;
	private InMemoryChainDefinitionRepository definitionRepository;
	private InMemoryChainStateRepository stateRepository;
	private ChainExecutor chainExecutor;
	private TinyflowWorkflowValidator validator;
	private TinyflowService service;

	@Before
	public void setUp() {
		blockingGate = new CountDownLatch(1);
		ChainParser parser = ChainParser.builder()
				.addParser("blockingNode", new BlockingNodeParser(blockingGate))
				.build();
		validator = new TinyflowWorkflowValidator(parser);
		workflowRepository = new InMemoryTinyflowWorkflowRepository();
		definitionRepository = new InMemoryChainDefinitionRepository();
		stateRepository = new InMemoryChainStateRepository();
		chainExecutor = new ChainExecutor(definitionRepository, stateRepository,
				new InMemoryNodeStateRepository());
		service = new DefaultTinyflowService(validator, workflowRepository, definitionRepository,
				chainExecutor, Duration.ofSeconds(2));
	}

	@After
	public void tearDown() {
		blockingGate.countDown();
		Thread.interrupted();
	}

	@Test
	public void shouldExecuteSavedStartTemplateEndWorkflowAndCleanInternalState() {
		service.register(workflow("flow-1", SIMPLE_WORKFLOW));

		TinyflowExecutionResult result = service.execute("flow-1", Collections.<String, Object>singletonMap("name", "Tinyflow"));

		assertFalse(result.isSuspended());
		assertEquals(Collections.<String, Object>singletonMap("greeting", "Hello Tinyflow"), result.getOutputs());
		assertFalse(result.getOutputs().containsKey(ChainConsts.SCHEDULE_NEXT_NODE_DISABLED_KEY));
		assertFalse(result.getOutputs().containsKey(ChainConsts.NODE_STATE_STATUS_KEY));
		assertFalse(result.getOutputs().containsKey(ChainConsts.CHAIN_STATE_STATUS_KEY));
		assertFalse(result.getOutputs().containsKey(ChainConsts.CHAIN_STATE_MESSAGE_KEY));
		assertThrows(UnsupportedOperationException.class, () -> result.getOutputs().put("leak", new ChainState()));
	}

	@Test
	public void shouldReturnInstanceIdForAsyncExecution() throws Exception {
		service.register(workflow("flow-async", SIMPLE_WORKFLOW));

		TinyflowAsyncResult started = service.executeAsync("flow-async",
				Collections.<String, Object>singletonMap("name", "Async"));

		assertNotNull(started.getInstanceId());
		assertFalse(started.getInstanceId().trim().isEmpty());
		awaitStatus(started.getInstanceId(), ChainStatus.SUCCEEDED);
	}

	@Test
	public void shouldReturnSuspendedResultAndResumeWithSameInstanceId() {
		service.register(workflow("flow-suspend", SUSPENDING_WORKFLOW));

		TinyflowExecutionResult suspended = service.execute("flow-suspend", Collections.<String, Object>emptyMap());
		TinyflowExecutionResult resumed = service.resume(suspended.getInstanceId(),
				Collections.<String, Object>singletonMap("confirm__approval", "yes"));

		assertTrue(suspended.isSuspended());
		assertNotNull(suspended.getInstanceId());
		assertEquals(suspended.getInstanceId(), resumed.getInstanceId());
		assertFalse(resumed.isSuspended());
	}

	@Test
	public void shouldResumeSuspendedChainAsynchronously() throws Exception {
		service.register(workflow("flow-resume-async", SUSPENDING_WORKFLOW));
		TinyflowAsyncResult started = service.executeAsync("flow-resume-async", Collections.<String, Object>emptyMap());
		awaitStatus(started.getInstanceId(), ChainStatus.SUSPEND);

		TinyflowAsyncResult resumed = service.resumeAsync(started.getInstanceId(),
				Collections.<String, Object>singletonMap("confirm__approval", "yes"));

		assertEquals(started.getInstanceId(), resumed.getInstanceId());
		awaitStatus(started.getInstanceId(), ChainStatus.SUCCEEDED);
	}

	@Test
	public void shouldFailWithStableErrorWhenExecutionTimesOut() {
		service.register(workflow("slow-flow", BLOCKING_WORKFLOW));
		TinyflowService shortTimeoutService = new DefaultTinyflowService(validator, workflowRepository,
				definitionRepository, chainExecutor, Duration.ofMillis(30));

		TinyflowException error = assertThrows(TinyflowException.class,
				() -> shortTimeoutService.execute("slow-flow", Collections.<String, Object>emptyMap()));

		assertTrue(error.getMessage().contains("timed out"));
	}

	@Test
	public void shouldRestoreInterruptFlagWhenWaitingIsInterrupted() {
		service.register(workflow("interrupt-flow", BLOCKING_WORKFLOW));
		Thread.currentThread().interrupt();

		TinyflowException error = assertThrows(TinyflowException.class,
				() -> service.execute("interrupt-flow", Collections.<String, Object>emptyMap()));

		assertTrue(error.getMessage().contains("interrupted"));
		assertTrue(Thread.currentThread().isInterrupted());
	}

	@Test
	public void shouldRejectMissingWorkflowAndRemovedWorkflow() {
		TinyflowException missing = assertThrows(TinyflowException.class,
				() -> service.execute("missing-flow", Collections.<String, Object>emptyMap()));
		assertTrue(missing.getMessage().contains("missing-flow"));

		service.register(workflow("flow-remove", SIMPLE_WORKFLOW));
		assertNotNull(workflowRepository.findById("flow-remove"));
		assertNotNull(definitionRepository.getChainDefinitionById("flow-remove"));

		service.remove("flow-remove");

		assertNull(workflowRepository.findById("flow-remove"));
		assertNull(definitionRepository.getChainDefinitionById("flow-remove"));
		TinyflowException removed = assertThrows(TinyflowException.class,
				() -> service.execute("flow-remove", Collections.<String, Object>emptyMap()));
		assertTrue(removed.getMessage().contains("flow-remove"));
	}

	@Test
	public void shouldKeepRegisteredWorkflowWhenReplacementIsInvalid() {
		service.register(workflow("flow-stable", SIMPLE_WORKFLOW));
		TinyflowWorkflow saved = workflowRepository.findById("flow-stable");

		assertThrows(TinyflowException.class,
				() -> service.register(workflow("flow-stable", "{\"nodes\":[],\"edges\":[]}")));

		assertSame(saved, workflowRepository.findById("flow-stable"));
		TinyflowExecutionResult result = service.execute("flow-stable",
				Collections.<String, Object>singletonMap("name", "Still valid"));
		assertEquals("Hello Still valid", result.getOutputs().get("greeting"));
	}

	private TinyflowWorkflow workflow(String id, String json) {
		return new TinyflowWorkflow(id, "test/" + id, id, json);
	}

	private void awaitStatus(String instanceId, ChainStatus expected) throws Exception {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
		ChainStatus actual = null;
		while (System.nanoTime() < deadline) {
			actual = stateRepository.load(instanceId).getStatus();
			if (actual == expected) {
				return;
			}
			Thread.sleep(10L);
		}
		assertEquals(expected, actual);
	}

	private static final class BlockingNodeParser extends BaseNodeParser<BlockingNode> {

		private final CountDownLatch gate;

		private BlockingNodeParser(CountDownLatch gate) {
			this.gate = gate;
		}

		@Override
		protected BlockingNode doParse(JSONObject root, JSONObject data, JSONObject chain) {
			return new BlockingNode(gate);
		}
	}

	private static final class BlockingNode extends BaseNode {

		private final CountDownLatch gate;

		private BlockingNode(CountDownLatch gate) {
			this.gate = gate;
		}

		@Override
		public Map<String, Object> execute(Chain chain) {
			try {
				gate.await(2, TimeUnit.SECONDS);
			} catch (InterruptedException error) {
				Thread.currentThread().interrupt();
			}
			return Collections.emptyMap();
		}
	}
}
