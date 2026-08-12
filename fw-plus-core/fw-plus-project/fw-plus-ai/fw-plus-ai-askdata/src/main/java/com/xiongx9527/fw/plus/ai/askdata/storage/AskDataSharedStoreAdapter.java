package com.xiongx9527.fw.plus.ai.askdata.storage;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.memory.AskDataLongTermMemory;
import com.xiongx9527.fw.plus.ai.askdata.memory.AskDataMemoryEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 通过共享客户端实现 Core 存储 SPI 的无驱动适配器。
 *
 * <p>所有 key 均包含 {@code projectId + operatorId}，JDBC 或 Redis 客户端只负责持久化不可变值和提供原子操作。
 * Adapter 不持有本地恢复状态，因此使用同一共享客户端创建的新实例可以继续读取会话、检查点和执行状态。</p>
 */
public final class AskDataSharedStoreAdapter {

	private static final int MAX_MONITORING_LOGS_PER_SCOPE = 1000;

	private final AskDataSharedStorageClient client;

	private AskDataSharedStoreAdapter(AskDataSharedStorageClient client) {
		if (client == null) {
			throw new IllegalArgumentException("sharedStorageClient 不能为空");
		}
		this.client = client;
	}

	public static AskDataSharedStoreAdapter jdbc(AskDataJdbcStorageClient client) {
		return new AskDataSharedStoreAdapter(client);
	}

	public static AskDataSharedStoreAdapter redis(AskDataRedisStorageClient client) {
		return new AskDataSharedStoreAdapter(client);
	}

	public AskDataConversationStore conversations() {
		return new AskDataConversationStore() {
			@Override
			public void save(AskDataProjectContext context, String conversationId, Conversation conversation) {
				client.put(key(context, "conversation", conversationId), require(conversation, "conversation"));
			}

			@Override
			public Conversation load(AskDataProjectContext context, String conversationId) {
				return (Conversation) client.get(key(context, "conversation", conversationId));
			}
		};
	}

	public AskDataCheckpointStore checkpoints() {
		return new AskDataCheckpointStore() {
			@Override
			public Checkpoint save(AskDataProjectContext context, String checkpointId, Checkpoint checkpoint) {
				Checkpoint checked = require(checkpoint, "checkpoint");
				String storageKey = key(context, "checkpoint", checkpointId);
				for (;;) {
					Checkpoint previous = (Checkpoint) client.get(storageKey);
					if (previous != null && !checked.isVersionAdvancedFrom(previous)) {
						throw new IllegalArgumentException("checkpoint version 必须严格推进");
					}
					if (client.compareAndSet(storageKey, previous, checked)) {
						return checked;
					}
				}
			}

			@Override
			public Checkpoint load(AskDataProjectContext context, String checkpointId) {
				return (Checkpoint) client.get(key(context, "checkpoint", checkpointId));
			}
		};
	}

	public AskDataAgentStateStore agentStates() {
		return new AskDataAgentStateStore() {
			@Override
			public void save(AskDataProjectContext context, String agentId, AgentState state) {
				client.put(key(context, "agent", agentId), require(state, "agentState"));
			}

			@Override
			public AgentState load(AskDataProjectContext context, String agentId) {
				return (AgentState) client.get(key(context, "agent", agentId));
			}
		};
	}

	public AskDataLeaseStore leases() {
		return new AskDataLeaseStore() {
			@Override
			public boolean tryAcquire(AskDataProjectContext context, String resourceId, String ownerId,
					long leaseDurationMillis) {
				if (leaseDurationMillis < 1) {
					throw new IllegalArgumentException("leaseDurationMillis 必须大于 0");
				}
				String storageKey = key(context, "lease", resourceId);
				String owner = requireId(ownerId);
				long expiresAt = System.currentTimeMillis() + leaseDurationMillis;
				for (;;) {
					Lease previous = (Lease) client.get(storageKey);
					if (previous != null && previous.expiresAt > System.currentTimeMillis()
							&& !owner.equals(previous.ownerId)) {
						return false;
					}
					if (client.compareAndSet(storageKey, previous, new Lease(owner, expiresAt))) {
						return true;
					}
				}
			}

			@Override
			public boolean release(AskDataProjectContext context, String resourceId, String ownerId) {
				String storageKey = key(context, "lease", resourceId);
				Lease previous = (Lease) client.get(storageKey);
				return previous != null && previous.ownerId.equals(requireId(ownerId))
						&& client.compareAndSet(storageKey, previous, null);
			}
		};
	}

	public AskDataIdempotencyKeyStore idempotencyKeys() {
		return new AskDataIdempotencyKeyStore() {
			@Override
			public boolean recordIfAbsent(AskDataProjectContext context, String idempotencyKey,
					IdempotencyRecord record) {
				return client.putIfAbsent(key(context, "idempotency", idempotencyKey),
						require(record, "idempotencyRecord"));
			}

			@Override
			public IdempotencyRecord load(AskDataProjectContext context, String idempotencyKey) {
				return (IdempotencyRecord) client.get(key(context, "idempotency", idempotencyKey));
			}
		};
	}

	public AskDataAuditStateStore auditStates() {
		return new AskDataAuditStateStore() {
			@Override
			public void save(AskDataProjectContext context, String auditId, AuditState state) {
				AuditState checked = require(state, "auditState");
				String storageKey = key(context, "audit", auditId);
				for (;;) {
					AuditState previous = (AuditState) client.get(storageKey);
					if (previous != null && checked.getSequence() <= previous.getSequence()) {
						throw new IllegalArgumentException("audit sequence 必须严格推进");
					}
					if (client.compareAndSet(storageKey, previous, checked)) {
						return;
					}
				}
			}

			@Override
			public AuditState load(AskDataProjectContext context, String auditId) {
				return (AuditState) client.get(key(context, "audit", auditId));
			}
		};
	}

	public AskDataMonitoringLogStore monitoringLogs() {
		return new AskDataMonitoringLogStore() {
			@Override
			public void append(AskDataProjectContext context, MonitoringLog log) {
				MonitoringLog checked = require(log, "monitoringLog").ownedBy(context);
				registerMonitoringScope(context);
				String storageKey = key(context, "monitoring", "logs");
				for (;;) {
					MonitoringLogBatch previous = (MonitoringLogBatch) client.get(storageKey);
					List<MonitoringLog> logs = previous == null ? new ArrayList<MonitoringLog>()
							: new ArrayList<MonitoringLog>(previous.logs);
					if (logs.size() >= MAX_MONITORING_LOGS_PER_SCOPE) {
						logs.remove(0);
					}
					logs.add(checked);
					if (client.compareAndSet(storageKey, previous, new MonitoringLogBatch(logs))) {
						return;
					}
				}
			}

			@Override
			public List<MonitoringLog> list(AskDataProjectContext context) {
				MonitoringLogBatch batch = (MonitoringLogBatch) client.get(key(context, "monitoring", "logs"));
				return batch == null ? Collections.<MonitoringLog>emptyList()
						: new ArrayList<MonitoringLog>(batch.logs);
			}

			@Override
			public List<MonitoringLog> listProject(String projectId) {
				String checkedProjectId = requireId(projectId);
				MonitoringScopeIndex index = (MonitoringScopeIndex) client.get(projectIndexKey(checkedProjectId));
				if (index == null) {
					return Collections.emptyList();
				}
				List<MonitoringLog> result = new ArrayList<MonitoringLog>();
				for (String operatorId : index.operatorIds) {
					result.addAll(list(new AskDataProjectContext(checkedProjectId, operatorId)));
				}
				return result;
			}
		};
	}

	/** 通过同一 JDBC/Redis 客户端保存可跨实例恢复的长期记忆。 */
	public AskDataLongTermMemory longTermMemory() {
		return new AskDataLongTermMemory() {
			@Override
			public void remember(AskDataMemoryEntry entry) {
				AskDataMemoryEntry checked = require(entry, "memoryEntry");
				String storageKey = key(checked.getContext(), "long-term-memory", "entries");
				for (;;) {
					MemoryBatch previous = (MemoryBatch) client.get(storageKey);
					List<AskDataMemoryEntry> entries = previous == null ? new ArrayList<AskDataMemoryEntry>()
							: new ArrayList<AskDataMemoryEntry>(previous.entries);
					entries.add(checked);
					if (client.compareAndSet(storageKey, previous, new MemoryBatch(entries))) return;
				}
			}

			@Override
			public List<AskDataMemoryEntry> find(AskDataProjectContext context, String datasourceKey,
					String semanticVersion) {
				String checkedDatasource = requireId(datasourceKey);
				String checkedVersion = requireId(semanticVersion);
				MemoryBatch batch = (MemoryBatch) client.get(key(context, "long-term-memory", "entries"));
				if (batch == null) return Collections.emptyList();
				List<AskDataMemoryEntry> result = new ArrayList<AskDataMemoryEntry>();
				for (AskDataMemoryEntry entry : batch.entries) {
					if (checkedDatasource.equals(entry.getDatasourceKey())
							&& checkedVersion.equals(entry.getSemanticVersion())) result.add(entry);
				}
				return Collections.unmodifiableList(result);
			}

			@Override
			public boolean forget(AskDataProjectContext context, String memoryId, String reason,
					long forgottenAtEpochMillis) {
				if (forgottenAtEpochMillis < 1L) throw new IllegalArgumentException("forgottenAtEpochMillis 必须大于 0");
				String checkedId = requireId(memoryId);
				String checkedReason = requireId(reason);
				String storageKey = key(context, "long-term-memory", "entries");
				for (;;) {
					MemoryBatch previous = (MemoryBatch) client.get(storageKey);
					if (previous == null) return false;
					List<AskDataMemoryEntry> entries = new ArrayList<AskDataMemoryEntry>(previous.entries);
					boolean removed = false;
					for (int index = 0; index < entries.size(); index++) {
						if (checkedId.equals(entries.get(index).getId())) {
							entries.remove(index);
							removed = true;
							break;
						}
					}
					if (!removed) return false;
					if (client.compareAndSet(storageKey, previous, new MemoryBatch(entries))) {
						appendForgetAudit(context, new AskDataLongTermMemory.ForgetAudit(
								context, checkedId, checkedReason, forgottenAtEpochMillis));
						return true;
					}
				}
			}

			@Override
			public List<AskDataLongTermMemory.ForgetAudit> listForgetAudits(AskDataProjectContext context) {
				ForgetAuditBatch batch = (ForgetAuditBatch) client.get(
						key(context, "long-term-memory", "forget-audits"));
				return batch == null ? Collections.<AskDataLongTermMemory.ForgetAudit>emptyList()
						: batch.audits;
			}
		};
	}

	private void appendForgetAudit(AskDataProjectContext context, AskDataLongTermMemory.ForgetAudit audit) {
		String storageKey = key(context, "long-term-memory", "forget-audits");
		for (;;) {
			ForgetAuditBatch previous = (ForgetAuditBatch) client.get(storageKey);
			List<AskDataLongTermMemory.ForgetAudit> audits = previous == null
					? new ArrayList<AskDataLongTermMemory.ForgetAudit>()
					: new ArrayList<AskDataLongTermMemory.ForgetAudit>(previous.audits);
			audits.add(audit);
			if (client.compareAndSet(storageKey, previous, new ForgetAuditBatch(audits))) return;
		}
	}

	private void registerMonitoringScope(AskDataProjectContext context) {
		String storageKey = projectIndexKey(context.getProjectId());
		String operatorId = requireId(context.getOperatorId());
		for (;;) {
			MonitoringScopeIndex previous = (MonitoringScopeIndex) client.get(storageKey);
			Set<String> operatorIds = previous == null ? new LinkedHashSet<String>()
					: new LinkedHashSet<String>(previous.operatorIds);
			if (!operatorIds.add(operatorId)) {
				return;
			}
			if (client.compareAndSet(storageKey, previous, new MonitoringScopeIndex(operatorIds))) {
				return;
			}
		}
	}

	private String key(AskDataProjectContext context, String type, String id) {
		if (context == null) {
			throw new IllegalArgumentException("context 不能为空");
		}
		return "askdata:" + requireId(type) + ':' + encode(context.getProjectId()) + ':'
				+ encode(context.getOperatorId()) + ':' + encode(id);
	}

	private String projectIndexKey(String projectId) {
		return "askdata:monitoring-project:" + encode(projectId);
	}

	private static String encode(String value) {
		String checked = requireId(value);
		return checked.length() + ":" + checked;
	}

	private static String requireId(String value) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException("id 不能为空白");
		}
		return value.trim();
	}

	private static <T> T require(T value, String name) {
		if (value == null) {
			throw new IllegalArgumentException(name + " 不能为空");
		}
		return value;
	}

	private static final class Lease {
		private final String ownerId;
		private final long expiresAt;

		private Lease(String ownerId, long expiresAt) {
			this.ownerId = ownerId;
			this.expiresAt = expiresAt;
		}
	}

	private static final class MonitoringLogBatch {
		private final List<AskDataMonitoringLogStore.MonitoringLog> logs;

		private MonitoringLogBatch(List<AskDataMonitoringLogStore.MonitoringLog> logs) {
			this.logs = Collections.unmodifiableList(new ArrayList<AskDataMonitoringLogStore.MonitoringLog>(logs));
		}
	}

	private static final class MonitoringScopeIndex {
		private final List<String> operatorIds;

		private MonitoringScopeIndex(Set<String> operatorIds) {
			this.operatorIds = Collections.unmodifiableList(new ArrayList<String>(operatorIds));
		}
	}

	private static final class MemoryBatch {
		private final List<AskDataMemoryEntry> entries;
		private MemoryBatch(List<AskDataMemoryEntry> entries) {
			this.entries = Collections.unmodifiableList(new ArrayList<AskDataMemoryEntry>(entries));
		}
	}

	private static final class ForgetAuditBatch {
		private final List<AskDataLongTermMemory.ForgetAudit> audits;
		private ForgetAuditBatch(List<AskDataLongTermMemory.ForgetAudit> audits) {
			this.audits = Collections.unmodifiableList(
					new ArrayList<AskDataLongTermMemory.ForgetAudit>(audits));
		}
	}
}
