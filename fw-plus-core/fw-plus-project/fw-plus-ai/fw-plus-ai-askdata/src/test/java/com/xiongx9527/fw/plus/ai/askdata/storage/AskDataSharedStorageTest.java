package com.xiongx9527.fw.plus.ai.askdata.storage;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

		AskDataSharedStoreAdapter afterRestart = AskDataSharedStoreAdapter.jdbc(client);

		assertEquals(1L, afterRestart.checkpoints().load(context, "checkpoint-1").getVersion());
		assertEquals("running", afterRestart.agentStates().load(context, "agent-1").getStatus());
	}

	@Test
	void 共享适配器应在租约冲突与版本回退时拒绝写入() {
		AskDataSharedStoreAdapter stores = AskDataSharedStoreAdapter.redis(new MapBackedRedisClient());
		AskDataProjectContext context = new AskDataProjectContext("project-a", "operator-a");

		assertTrue(stores.leases().tryAcquire(context, "agent-1", "node-1", 10000L));
		assertFalse(stores.leases().tryAcquire(context, "agent-1", "node-2", 10000L));
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
				return values.putIfAbsent(key, newValue) == null;
			}
			return values.replace(key, expectedValue, newValue);
		}

		@Override
		public void put(String key, Object value) {
			values.put(key, value);
		}
	}
}
