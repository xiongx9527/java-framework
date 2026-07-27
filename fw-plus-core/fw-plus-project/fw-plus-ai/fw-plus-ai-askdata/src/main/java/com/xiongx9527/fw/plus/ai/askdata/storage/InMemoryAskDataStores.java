package com.xiongx9527.fw.plus.ai.askdata.storage;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * AskData 的单节点内存存储集合。
 *
 * <p>该实现不共享进程间数据，只能用于单节点部署。集群模式必须注入共享存储 SPI 实现，调用
 * {@link #create(boolean)} 并传入 {@code true} 会直接拒绝创建该内存实现。</p>
 */
public final class InMemoryAskDataStores {

	private static final int MAX_MONITORING_LOGS_PER_SCOPE = 1000;

	private final ConcurrentMap<ScopeKey, ConcurrentMap<String, AskDataConversationStore.Conversation>> conversations = new ConcurrentHashMap<ScopeKey, ConcurrentMap<String, AskDataConversationStore.Conversation>>();
	private final ConcurrentMap<ScopeKey, ConcurrentMap<String, AskDataCheckpointStore.Checkpoint>> checkpoints = new ConcurrentHashMap<ScopeKey, ConcurrentMap<String, AskDataCheckpointStore.Checkpoint>>();
	private final ConcurrentMap<ScopeKey, ConcurrentMap<String, AskDataAgentStateStore.AgentState>> agentStates = new ConcurrentHashMap<ScopeKey, ConcurrentMap<String, AskDataAgentStateStore.AgentState>>();
	private final ConcurrentMap<ScopeKey, List<AskDataMonitoringLogStore.MonitoringLog>> monitoringLogs = new ConcurrentHashMap<ScopeKey, List<AskDataMonitoringLogStore.MonitoringLog>>();

	private final AskDataConversationStore conversationStore = new AskDataConversationStore() {
		@Override
		public void save(AskDataProjectContext context, String conversationId, Conversation conversation) {
			mapFor(conversations, context).put(requireId(conversationId), requireValue(conversation, "conversation"));
		}

		@Override
		public Conversation load(AskDataProjectContext context, String conversationId) {
			ConcurrentMap<String, Conversation> values = conversations.get(scope(context));
			return values == null ? null : values.get(requireId(conversationId));
		}
	};

	private final AskDataCheckpointStore checkpointStore = new AskDataCheckpointStore() {
		@Override
		public Checkpoint save(AskDataProjectContext context, String checkpointId, final Checkpoint checkpoint) {
			final Checkpoint checkedCheckpoint = requireValue(checkpoint, "checkpoint");
			return mapFor(checkpoints, context).compute(requireId(checkpointId), (id, previous) -> {
				if (previous != null && !checkedCheckpoint.isVersionAdvancedFrom(previous)) {
					throw new IllegalArgumentException("checkpoint version 必须严格推进");
				}
				return checkedCheckpoint;
			});
		}

		@Override
		public Checkpoint load(AskDataProjectContext context, String checkpointId) {
			ConcurrentMap<String, Checkpoint> values = checkpoints.get(scope(context));
			return values == null ? null : values.get(requireId(checkpointId));
		}
	};

	private final AskDataAgentStateStore agentStateStore = new AskDataAgentStateStore() {
		@Override
		public void save(AskDataProjectContext context, String agentId, AgentState state) {
			mapFor(agentStates, context).put(requireId(agentId), requireValue(state, "agentState"));
		}

		@Override
		public AgentState load(AskDataProjectContext context, String agentId) {
			ConcurrentMap<String, AgentState> values = agentStates.get(scope(context));
			return values == null ? null : values.get(requireId(agentId));
		}
	};

	private final AskDataMonitoringLogStore monitoringLogStore = new AskDataMonitoringLogStore() {
		@Override
		public void append(AskDataProjectContext context, MonitoringLog log) {
			MonitoringLog checkedLog = requireValue(log, "monitoringLog");
			ScopeKey key = scope(context);
			List<MonitoringLog> logs = monitoringLogs.get(key);
			if (logs == null) {
				List<MonitoringLog> created = Collections.synchronizedList(new ArrayList<MonitoringLog>());
				List<MonitoringLog> existing = monitoringLogs.putIfAbsent(key, created);
				logs = existing == null ? created : existing;
			}
			synchronized (logs) {
				if (logs.size() >= MAX_MONITORING_LOGS_PER_SCOPE) {
					logs.remove(0);
				}
				logs.add(checkedLog);
			}
		}

		@Override
		public List<MonitoringLog> list(AskDataProjectContext context) {
			List<MonitoringLog> logs = monitoringLogs.get(scope(context));
			if (logs == null) {
				return Collections.emptyList();
			}
			synchronized (logs) {
				return new ArrayList<MonitoringLog>(logs);
			}
		}
	};

	private InMemoryAskDataStores() {
	}

	public static InMemoryAskDataStores singleNode() {
		return create(false);
	}

	public static InMemoryAskDataStores create(boolean clusterMode) {
		if (clusterMode) {
			throw new IllegalStateException("集群模式不能使用内存 AskData 存储");
		}
		return new InMemoryAskDataStores();
	}

	public AskDataConversationStore conversations() {
		return conversationStore;
	}

	public AskDataCheckpointStore checkpoints() {
		return checkpointStore;
	}

	public AskDataAgentStateStore agentStates() {
		return agentStateStore;
	}

	public AskDataMonitoringLogStore monitoringLogs() {
		return monitoringLogStore;
	}

	private static <T> ConcurrentMap<String, T> mapFor(ConcurrentMap<ScopeKey, ConcurrentMap<String, T>> storage,
			AskDataProjectContext context) {
		ScopeKey key = scope(context);
		ConcurrentMap<String, T> values = storage.get(key);
		if (values == null) {
			ConcurrentMap<String, T> created = new ConcurrentHashMap<String, T>();
			ConcurrentMap<String, T> existing = storage.putIfAbsent(key, created);
			values = existing == null ? created : existing;
		}
		return values;
	}

	private static ScopeKey scope(AskDataProjectContext context) {
		if (context == null) {
			throw new IllegalArgumentException("context 不能为空");
		}
		return new ScopeKey(context.getProjectId(), context.getOperatorId());
	}

	private static String requireId(String value) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException("id 不能为空白");
		}
		return value;
	}

	private static <T> T requireValue(T value, String name) {
		if (value == null) {
			throw new IllegalArgumentException(name + " 不能为空");
		}
		return value;
	}

	private static final class ScopeKey {

		private final String projectId;
		private final String operatorId;

		private ScopeKey(String projectId, String operatorId) {
			this.projectId = projectId;
			this.operatorId = operatorId;
		}

		@Override
		public boolean equals(Object object) {
			if (this == object) {
				return true;
			}
			if (!(object instanceof ScopeKey)) {
				return false;
			}
			ScopeKey other = (ScopeKey) object;
			return projectId.equals(other.projectId) && operatorId.equals(other.operatorId);
		}

		@Override
		public int hashCode() {
			return 31 * projectId.hashCode() + operatorId.hashCode();
		}
	}
}
