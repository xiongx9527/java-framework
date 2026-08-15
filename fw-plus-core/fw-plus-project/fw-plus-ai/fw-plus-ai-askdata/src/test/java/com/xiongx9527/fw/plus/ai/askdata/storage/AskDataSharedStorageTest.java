package com.xiongx9527.fw.plus.ai.askdata.storage;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.memory.AskDataLongTermMemory;
import com.xiongx9527.fw.plus.ai.askdata.memory.AskDataMemoryEntry;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AskDataSharedStorageTest {

	@Test
	void 应通过共享客户端在新适配器实例中恢复检查点和状态() {
		MapBackedJdbcClient client = new MapBackedJdbcClient();
		AskDataSharedStoreAdapter beforeRestart = AskDataSharedStoreAdapter.jdbc(client);
		AskDataProjectContext context = new AskDataProjectContext("project-a", "operator-a");
		beforeRestart.checkpoints().save(context, "checkpoint-1",
				new AskDataCheckpointStore.Checkpoint(1L, "executing", AskDataCheckpointStore.SqlExecutionState.UNKNOWN));
		beforeRestart.agentStates().save(context, "agent-1", new AskDataAgentStateStore.AgentState("running", "executing"));
		beforeRestart.conversations().save(context, "conversation-1",
				new AskDataConversationStore.Conversation("question", "answer"));
		assertTrue(beforeRestart.idempotencyKeys().recordIfAbsent(context, "request-1",
				new AskDataIdempotencyKeyStore.IdempotencyRecord("completed", "checkpoint-1")));
		beforeRestart.auditStates().save(context, "audit-1",
				new AskDataAuditStateStore.AuditState("SUCCEEDED", "sql-executed", 1L));
		beforeRestart.monitoringLogs().append(context,
				new AskDataMonitoringLogStore.MonitoringLog("trace-1", "SQL_EXECUTED", "completed"));

		AskDataSharedStoreAdapter afterRestart = AskDataSharedStoreAdapter.jdbc(client);

		assertEquals(1L, afterRestart.checkpoints().load(context, "checkpoint-1").getVersion());
		assertEquals("running", afterRestart.agentStates().load(context, "agent-1").getStatus());
		assertEquals("answer", afterRestart.conversations().load(context, "conversation-1").getAnswer());
		assertEquals("checkpoint-1", afterRestart.idempotencyKeys().load(context, "request-1").getReferenceId());
		assertEquals(1L, afterRestart.auditStates().load(context, "audit-1").getSequence());
		assertEquals("trace-1", afterRestart.monitoringLogs().list(context).get(0).getTraceId());
	}

	@Test
	void 共享适配器应在租约冲突与版本回退时拒绝写入() {
		AskDataSharedStoreAdapter stores = AskDataSharedStoreAdapter.redis(new MapBackedRedisClient());
		AskDataProjectContext context = new AskDataProjectContext("project-a", "operator-a");

		assertTrue(stores.leases().tryAcquire(context, "agent-1", "node-1", 10000L));
		assertFalse(stores.leases().tryAcquire(context, "agent-1", "node-2", 10000L));
		assertTrue(stores.leases().release(context, "agent-1", "node-1"));
		assertTrue(stores.leases().tryAcquire(context, "agent-1", "node-2", 10000L));
		stores.checkpoints().save(context, "checkpoint-1",
				new AskDataCheckpointStore.Checkpoint(2L, "done", AskDataCheckpointStore.SqlExecutionState.SUCCEEDED));
		try {
			stores.checkpoints().save(context, "checkpoint-1",
					new AskDataCheckpointStore.Checkpoint(1L, "old", AskDataCheckpointStore.SqlExecutionState.SUCCEEDED));
		} catch (IllegalArgumentException expected) {
			return;
		}
		throw new AssertionError("应拒绝检查点版本回退");
	}

	@Test
	void 共享会话幂等审计与监控必须按项目和操作人隔离() {
		AskDataSharedStoreAdapter stores = AskDataSharedStoreAdapter.jdbc(new MapBackedJdbcClient());
		AskDataProjectContext first = new AskDataProjectContext("project-a", "operator-a");
		AskDataProjectContext second = new AskDataProjectContext("project-a", "operator-b");
		stores.conversations().save(first, "conversation-1",
				new AskDataConversationStore.Conversation("question", "answer"));
		stores.idempotencyKeys().recordIfAbsent(first, "request-1",
				new AskDataIdempotencyKeyStore.IdempotencyRecord("completed", "checkpoint-1"));
		stores.auditStates().save(first, "audit-1",
				new AskDataAuditStateStore.AuditState("SUCCEEDED", "completed", 2L));
		stores.monitoringLogs().append(first,
				new AskDataMonitoringLogStore.MonitoringLog("trace-a", "MODEL", "completed"));
		stores.monitoringLogs().append(second,
				new AskDataMonitoringLogStore.MonitoringLog("trace-b", "MODEL", "completed"));

		assertNull(stores.conversations().load(second, "conversation-1"));
		assertNull(stores.idempotencyKeys().load(second, "request-1"));
		assertNull(stores.auditStates().load(second, "audit-1"));
		assertEquals(1, stores.monitoringLogs().list(first).size());
		assertEquals(1, stores.monitoringLogs().list(second).size());
		assertEquals(2, stores.monitoringLogs().listProject("project-a").size());
	}

	@Test
	void 共享审计序列必须严格推进且幂等键只能首次写入() {
		AskDataSharedStoreAdapter stores = AskDataSharedStoreAdapter.redis(new MapBackedRedisClient());
		AskDataProjectContext context = new AskDataProjectContext("project-a", "operator-a");
		stores.auditStates().save(context, "audit-1",
				new AskDataAuditStateStore.AuditState("RUNNING", "started", 2L));

		assertThrows(IllegalArgumentException.class, () -> stores.auditStates().save(context, "audit-1",
				new AskDataAuditStateStore.AuditState("RUNNING", "old", 1L)));
		assertTrue(stores.idempotencyKeys().recordIfAbsent(context, "request-1",
				new AskDataIdempotencyKeyStore.IdempotencyRecord("running", "checkpoint-1")));
		assertFalse(stores.idempotencyKeys().recordIfAbsent(context, "request-1",
				new AskDataIdempotencyKeyStore.IdempotencyRecord("completed", "checkpoint-2")));
	}

	@Test
	void 共享适配器应跨实例保存隔离的长期记忆与遗忘审计() {
		MapBackedJdbcClient client = new MapBackedJdbcClient();
		AskDataProjectContext context = new AskDataProjectContext("project-a", "operator-a");
		AskDataLongTermMemory beforeRestart = AskDataSharedStoreAdapter.jdbc(client).longTermMemory();
		AskDataMemoryEntry entry = new AskDataMemoryEntry(context, "orders", "v1", "monthly revenue");
		beforeRestart.remember(entry);

		AskDataLongTermMemory afterRestart = AskDataSharedStoreAdapter.jdbc(client).longTermMemory();

		assertEquals(1, afterRestart.find(context, "orders", "v1").size());
		assertEquals("monthly revenue", afterRestart.find(context, "orders", "v1").get(0).getContent());
		assertTrue(afterRestart.forget(context, entry.getId(), "business request", System.currentTimeMillis()));
		assertTrue(afterRestart.find(context, "orders", "v1").isEmpty());
		assertEquals(entry.getId(), afterRestart.listForgetAudits(context).get(0).getMemoryId());
	}

	private static final class MapBackedJdbcClient extends MapBackedClient implements AskDataJdbcStorageClient {
	}

	private static final class MapBackedRedisClient extends MapBackedClient implements AskDataRedisStorageClient {
	}

	private static class MapBackedClient implements AskDataSharedStorageClient {

		private final ConcurrentMap<String, Object> values = new ConcurrentHashMap<String, Object>();

		@Override
		public Object get(String key) {
			return values.get(key);
		}

		@Override
		public boolean putIfAbsent(String key, Object value) {
			return values.putIfAbsent(key, value) == null;
		}

		@Override
		public boolean compareAndSet(String key, Object expectedValue, Object newValue) {
			if (expectedValue == null) {
				if (newValue == null) {
					return !values.containsKey(key);
				}
				return values.putIfAbsent(key, newValue) == null;
			}
			if (newValue == null) {
				return values.remove(key, expectedValue);
			}
			return values.replace(key, expectedValue, newValue);
		}

		@Override
		public void put(String key, Object value) {
			values.put(key, value);
		}
	}
}
