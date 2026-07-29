package com.xiongx9527.fw.plus.ai.askdata.storage;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AskDataStoreContractTest {

	@Test
	void 应按项目和操作人隔离所有存储内容() {
		InMemoryAskDataStores stores = InMemoryAskDataStores.singleNode();
		AskDataProjectContext firstContext = new AskDataProjectContext("project-a", "operator-a");
		AskDataProjectContext secondContext = new AskDataProjectContext("project-a", "operator-b");

		stores.conversations().save(firstContext, "conversation-1",
				new AskDataConversationStore.Conversation("question", "answer"));
		stores.agentStates().save(firstContext, "agent-1",
				new AskDataAgentStateStore.AgentState("running", "planning"));
		stores.monitoringLogs().append(firstContext,
				new AskDataMonitoringLogStore.MonitoringLog("trace-1", "MODEL", "completed"));
		stores.checkpoints().save(firstContext, "checkpoint-1",
				new AskDataCheckpointStore.Checkpoint(1L, "pending", AskDataCheckpointStore.SqlExecutionState.NOT_EXECUTED));

		assertEquals("answer", stores.conversations().load(firstContext, "conversation-1").getAnswer());
		assertNull(stores.conversations().load(secondContext, "conversation-1"));
		assertEquals("planning", stores.agentStates().load(firstContext, "agent-1").getStage());
		assertNull(stores.agentStates().load(secondContext, "agent-1"));
		assertEquals(1, stores.monitoringLogs().list(firstContext).size());
		assertTrue(stores.monitoringLogs().list(secondContext).isEmpty());
		assertEquals(1L, stores.checkpoints().load(firstContext, "checkpoint-1").getVersion());
		assertNull(stores.checkpoints().load(secondContext, "checkpoint-1"));
	}

	@Test
	void 同一操作人在不同项目下不得读取彼此会话() {
		InMemoryAskDataStores stores = InMemoryAskDataStores.singleNode();
		AskDataProjectContext firstProjectContext = new AskDataProjectContext("project-a", "operator-a");
		AskDataProjectContext secondProjectContext = new AskDataProjectContext("project-b", "operator-a");

		stores.conversations().save(firstProjectContext, "conversation-1",
				new AskDataConversationStore.Conversation("question", "answer"));

		assertEquals("answer", stores.conversations().load(firstProjectContext, "conversation-1").getAnswer());
		assertNull(stores.conversations().load(secondProjectContext, "conversation-1"));
	}

	@Test
	void 集群模式必须拒绝内存实现() {
		IllegalStateException exception = assertThrows(IllegalStateException.class,
				new org.junit.jupiter.api.function.Executable() {
					@Override
					public void execute() {
						InMemoryAskDataStores.create(true);
					}
				});

		assertTrue(exception.getMessage().contains("集群"));
	}

	@Test
	void 检查点保存时必须推进版本() {
		InMemoryAskDataStores stores = InMemoryAskDataStores.singleNode();
		AskDataProjectContext context = new AskDataProjectContext("project-a", "operator-a");

		AskDataCheckpointStore.Checkpoint first = stores.checkpoints().save(context, "checkpoint-1",
				new AskDataCheckpointStore.Checkpoint(1L, "planning", AskDataCheckpointStore.SqlExecutionState.NOT_EXECUTED));
		AskDataCheckpointStore.Checkpoint second = stores.checkpoints().save(context, "checkpoint-1",
				new AskDataCheckpointStore.Checkpoint(2L, "completed", AskDataCheckpointStore.SqlExecutionState.SUCCEEDED));

		assertTrue(second.isVersionAdvancedFrom(first));
		assertFalse(first.isVersionAdvancedFrom(second));
		assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
			@Override
			public void execute() {
				stores.checkpoints().save(context, "checkpoint-1",
						new AskDataCheckpointStore.Checkpoint(2L, "duplicate", AskDataCheckpointStore.SqlExecutionState.SUCCEEDED));
			}
		});
	}

	@Test
	void SQL执行状态不确定时不得标记为可重放() {
		AskDataCheckpointStore.Checkpoint checkpoint = new AskDataCheckpointStore.Checkpoint(1L, "executing",
				AskDataCheckpointStore.SqlExecutionState.UNKNOWN);

		assertFalse(checkpoint.isReplayable());
		assertEquals(AskDataCheckpointStore.SqlExecutionState.UNKNOWN, checkpoint.getSqlExecutionState());
	}

	@Test
	void 监控日志应有内存容量上限() {
		InMemoryAskDataStores stores = InMemoryAskDataStores.singleNode();
		AskDataProjectContext context = new AskDataProjectContext("project-a", "operator-a");

		for (int index = 0; index <= 1000; index++) {
			stores.monitoringLogs().append(context,
					new AskDataMonitoringLogStore.MonitoringLog("trace-" + index, "MODEL", "completed"));
		}

		assertEquals(1000, stores.monitoringLogs().list(context).size());
		assertEquals("trace-1", stores.monitoringLogs().list(context).get(0).getTraceId());
	}

	@Test
	void 监控日志应携带不可变归属并支持项目范围读取() {
		InMemoryAskDataStores stores = InMemoryAskDataStores.singleNode();
		AskDataProjectContext first = new AskDataProjectContext("project-a", "operator-a");
		AskDataProjectContext second = new AskDataProjectContext("project-a", "operator-b");
		AskDataProjectContext otherProject = new AskDataProjectContext("project-b", "operator-c");
		stores.monitoringLogs().append(first,
				new AskDataMonitoringLogStore.MonitoringLog(first, "trace-a", "MODEL", "completed"));
		stores.monitoringLogs().append(second,
				new AskDataMonitoringLogStore.MonitoringLog(second, "trace-b", "MODEL", "completed"));
		stores.monitoringLogs().append(otherProject,
				new AskDataMonitoringLogStore.MonitoringLog(otherProject, "trace-c", "MODEL", "completed"));

		java.util.List<AskDataMonitoringLogStore.MonitoringLog> logs =
				stores.monitoringLogs().listProject("project-a");

		assertEquals(2, logs.size());
		assertEquals("project-a", logs.get(0).getContext().getProjectId());
		assertTrue(!logs.get(0).getContext().getOperatorId().equals(logs.get(1).getContext().getOperatorId()));
	}

	@Test
	void 日志满容量后追加空值不得淘汰已有日志() {
		InMemoryAskDataStores stores = InMemoryAskDataStores.singleNode();
		AskDataProjectContext context = new AskDataProjectContext("project-a", "operator-a");

		for (int index = 0; index < 1000; index++) {
			stores.monitoringLogs().append(context,
					new AskDataMonitoringLogStore.MonitoringLog("trace-" + index, "MODEL", "completed"));
		}

		assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
			@Override
			public void execute() {
				stores.monitoringLogs().append(context, null);
			}
		});
		assertEquals(1000, stores.monitoringLogs().list(context).size());
		assertEquals("trace-0", stores.monitoringLogs().list(context).get(0).getTraceId());
		assertEquals("trace-999", stores.monitoringLogs().list(context).get(999).getTraceId());
	}

	@Test
	void 租约幂等键和审计状态应按项目和操作人隔离() {
		InMemoryAskDataStores stores = InMemoryAskDataStores.singleNode();
		AskDataProjectContext first = new AskDataProjectContext("project-a", "operator-a");
		AskDataProjectContext second = new AskDataProjectContext("project-b", "operator-a");

		assertTrue(stores.leases().tryAcquire(first, "agent-1", "node-1", 10000L));
		assertFalse(stores.leases().tryAcquire(first, "agent-1", "node-2", 10000L));
		assertTrue(stores.leases().tryAcquire(second, "agent-1", "node-2", 10000L));
		assertTrue(stores.idempotencyKeys().recordIfAbsent(first, "request-1",
				new AskDataIdempotencyKeyStore.IdempotencyRecord("completed", "checkpoint-1")));
		assertFalse(stores.idempotencyKeys().recordIfAbsent(first, "request-1",
				new AskDataIdempotencyKeyStore.IdempotencyRecord("completed", "checkpoint-2")));
		assertNull(stores.idempotencyKeys().load(second, "request-1"));
		stores.auditStates().save(first, "audit-1",
				new AskDataAuditStateStore.AuditState("SUCCEEDED", "sql-executed", 2L));
		assertEquals("SUCCEEDED", stores.auditStates().load(first, "audit-1").getStatus());
		assertNull(stores.auditStates().load(second, "audit-1"));
	}

	@Test
	void Agent状态应保留压缩上下文和执行明细() {
		AskDataAgentStateStore.AgentState state = new AskDataAgentStateStore.AgentState("running", "executing",
				"压缩后的上下文", AskDataAgentStateStore.ContextCompressionStatus.COMPRESSED,
				Collections.singletonList(new AskDataAgentStateStore.NodeState("node-1", "running")),
				Collections.singletonList(new AskDataAgentStateStore.SubTaskState("task-1", "query", "pending")),
				Collections.singletonList("先生成 SQL"),
				Collections.singletonList(new AskDataAgentStateStore.ToolSummary("metadata", "success", "2 tables")),
				Collections.singletonList(new AskDataAgentStateStore.ModelRouteAttempt("primary", "failed", "timeout")),
				Arrays.asList(new AskDataAgentStateStore.StepLog("plan", "success", "ok"),
						new AskDataAgentStateStore.StepLog("execute", "running", "started")));

		assertEquals(AskDataAgentStateStore.ContextCompressionStatus.COMPRESSED, state.getContextCompressionStatus());
		assertEquals("压缩后的上下文", state.getCompressedContext());
		assertEquals("node-1", state.getNodes().get(0).getNodeId());
		assertEquals("primary", state.getModelRouteAttempts().get(0).getModelKey());
		assertEquals(2, state.getStepLogs().size());
	}
}
