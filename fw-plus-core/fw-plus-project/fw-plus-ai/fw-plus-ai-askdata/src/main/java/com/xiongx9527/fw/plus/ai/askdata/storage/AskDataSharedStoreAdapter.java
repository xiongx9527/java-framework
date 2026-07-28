package com.xiongx9527.fw.plus.ai.askdata.storage;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

/** 通过共享客户端实现 Core 存储 SPI 的无驱动适配器。 */
public final class AskDataSharedStoreAdapter {

	private final AskDataSharedStorageClient client;

	private AskDataSharedStoreAdapter(AskDataSharedStorageClient client) {
		if (client == null) { throw new IllegalArgumentException("sharedStorageClient 不能为空"); }
		this.client = client;
	}

	public static AskDataSharedStoreAdapter jdbc(AskDataJdbcStorageClient client) { return new AskDataSharedStoreAdapter(client); }
	public static AskDataSharedStoreAdapter redis(AskDataRedisStorageClient client) { return new AskDataSharedStoreAdapter(client); }

	public AskDataCheckpointStore checkpoints() { return new AskDataCheckpointStore() {
		@Override public Checkpoint save(AskDataProjectContext context, String checkpointId, Checkpoint checkpoint) {
			String key = key(context, "checkpoint", checkpointId);
			for (;;) {
				Checkpoint previous = (Checkpoint) client.get(key);
				if (previous != null && !checkpoint.isVersionAdvancedFrom(previous)) { throw new IllegalArgumentException("checkpoint version 必须严格推进"); }
				if (client.compareAndSet(key, previous, checkpoint)) { return checkpoint; }
			}
		}
		@Override public Checkpoint load(AskDataProjectContext context, String checkpointId) { return (Checkpoint) client.get(key(context, "checkpoint", checkpointId)); }
	}; }

	public AskDataAgentStateStore agentStates() { return new AskDataAgentStateStore() {
		@Override public void save(AskDataProjectContext context, String agentId, AgentState state) { client.put(key(context, "agent", agentId), require(state, "agentState")); }
		@Override public AgentState load(AskDataProjectContext context, String agentId) { return (AgentState) client.get(key(context, "agent", agentId)); }
	}; }

	public AskDataLeaseStore leases() { return new AskDataLeaseStore() {
		@Override public boolean tryAcquire(AskDataProjectContext context, String resourceId, String ownerId, long leaseDurationMillis) {
			if (leaseDurationMillis < 1) { throw new IllegalArgumentException("leaseDurationMillis 必须大于 0"); }
			String key = key(context, "lease", resourceId); String owner = requireId(ownerId); long expiresAt = System.currentTimeMillis() + leaseDurationMillis;
			for (;;) {
				Lease previous = (Lease) client.get(key);
				if (previous != null && previous.expiresAt > System.currentTimeMillis() && !owner.equals(previous.ownerId)) { return false; }
				if (client.compareAndSet(key, previous, new Lease(owner, expiresAt))) { return true; }
			}
		}
		@Override public boolean release(AskDataProjectContext context, String resourceId, String ownerId) {
			String key = key(context, "lease", resourceId); Lease previous = (Lease) client.get(key);
			return previous != null && previous.ownerId.equals(requireId(ownerId)) && client.compareAndSet(key, previous, null);
		}
	}; }

	private String key(AskDataProjectContext context, String type, String id) {
		if (context == null) { throw new IllegalArgumentException("context 不能为空"); }
		return "askdata:" + type + ':' + requireId(context.getProjectId()) + ':' + requireId(context.getOperatorId()) + ':' + requireId(id);
	}
	private static String requireId(String value) { if (value == null || value.trim().length() == 0) { throw new IllegalArgumentException("id 不能为空白"); } return value; }
	private static <T> T require(T value, String name) { if (value == null) { throw new IllegalArgumentException(name + " 不能为空"); } return value; }
	private static final class Lease { private final String ownerId; private final long expiresAt; private Lease(String ownerId, long expiresAt) { this.ownerId = ownerId; this.expiresAt = expiresAt; } }
}
