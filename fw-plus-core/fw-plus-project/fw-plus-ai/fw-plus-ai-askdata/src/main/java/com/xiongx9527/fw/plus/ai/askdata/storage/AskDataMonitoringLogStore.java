package com.xiongx9527.fw.plus.ai.askdata.storage;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelUsage;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEventCorrelation;

import java.util.Collections;
import java.util.List;

/**
 * AskData 监控日志存储 SPI。
 *
 * <p>实现必须按项目和操作人共同隔离日志。内存实现仅适用于单节点部署；集群部署必须使用共享存储实现。</p>
 */
public interface AskDataMonitoringLogStore {

	/**
	 * 追加一条监控日志。
	 *
	 * <p>实现必须按 {@code projectId + operatorId} 隔离日志序列，禁止跨项目或跨操作人混入日志。实现可设置容量上限，
	 * 但在拒绝无效日志时不得删除已有有效日志。</p>
	 *
	 * <p>默认内存实现仅适用于单机进程，不能汇聚集群节点日志；集群部署必须接入共享日志存储实现。</p>
	 */
	void append(AskDataProjectContext context, MonitoringLog log);

	/**
	 * 获取当前项目和操作人的监控日志快照。
	 *
	 * <p>结果只包含 {@code projectId + operatorId} 隔离范围内的日志；不存在时返回空列表，不得读取其他项目或操作人的
	 * 日志。返回值应作为查询快照使用，调用方不得依赖默认内存实现提供跨节点共享或全局日志聚合。</p>
	 */
	List<MonitoringLog> list(AskDataProjectContext context);

	/**
	 * 获取项目内所有操作人的日志快照。管理员查询实现必须显式覆盖该方法。
	 */
	default List<MonitoringLog> listProject(String projectId) {
		return Collections.emptyList();
	}

	/**
	 * 监控日志的最小审计字段。
	 */
	class MonitoringLog {

		private final AskDataProjectContext context;
		private final String traceId;
		private final String eventType;
		private final String message;
		private final String datasourceKey;
		private final String denialCode;
		private final String authorizationVersion;
		private final String authorizationSource;
		private final String capabilityKey;
		private final String purposeKey;
		private final String modelType;
		private final String modelKey;
		private final String modelRoutePolicy;
		private final String modelRouteReason;
		private final String modelState;
		private final String modelFailureCode;
		private final String modelSwitchReason;
		private final String actualModelKey;
		private final AskDataModelUsage modelUsage;
		private final AskDataEventCorrelation correlation;

		public MonitoringLog(String traceId, String eventType, String message) {
			this(null, traceId, eventType, message, null, null, null, null, null);
		}

		public MonitoringLog(AskDataProjectContext context, String traceId, String eventType, String message) {
			this(context, traceId, eventType, message, null, null, null, null, null);
		}

		public MonitoringLog(AskDataProjectContext context, String traceId, String eventType, String message,
				String datasourceKey, String denialCode, String authorizationVersion, String authorizationSource) {
			this(context, traceId, eventType, message, datasourceKey, denialCode, authorizationVersion,
					authorizationSource, null);
		}

		public MonitoringLog(AskDataProjectContext context, String traceId, String eventType, String message,
				String datasourceKey, String denialCode, String authorizationVersion, String authorizationSource,
				String capabilityKey, String purposeKey) {
			this(context, traceId, eventType, message, datasourceKey, denialCode, authorizationVersion,
					authorizationSource, capabilityKey, purposeKey, null, null, null, null, null, null, null,
					AskDataModelUsage.unknown(), null);
		}

		public MonitoringLog(AskDataProjectContext context, String traceId, String eventType, String message,
				String datasourceKey, String denialCode, String authorizationVersion, String authorizationSource,
				String modelKey) {
			this(context, traceId, eventType, message, datasourceKey, denialCode, authorizationVersion,
					authorizationSource, null, modelKey, null, null, null, null, null);
		}

		public MonitoringLog(AskDataProjectContext context, String traceId, String eventType, String message,
				String datasourceKey, String denialCode, String authorizationVersion, String authorizationSource,
				String modelKey, String modelRoutePolicy, String modelState, String modelFailureCode,
				String modelSwitchReason, String actualModelKey) {
			this(context, traceId, eventType, message, datasourceKey, denialCode, authorizationVersion,
					authorizationSource, null, modelKey, modelRoutePolicy, modelState, modelFailureCode,
					modelSwitchReason, actualModelKey);
		}

		public MonitoringLog(AskDataProjectContext context, String traceId, String eventType, String message,
				String datasourceKey, String denialCode, String authorizationVersion, String authorizationSource,
				String modelType, String modelKey, String modelRoutePolicy, String modelState, String modelFailureCode,
				String modelSwitchReason, String actualModelKey) {
			this(context, traceId, eventType, message, datasourceKey, denialCode, authorizationVersion,
					authorizationSource, modelType, modelKey, modelRoutePolicy, modelState, modelFailureCode,
					modelSwitchReason, actualModelKey, null);
		}

		public MonitoringLog(AskDataProjectContext context, String traceId, String eventType, String message,
				String datasourceKey, String denialCode, String authorizationVersion, String authorizationSource,
				String modelType, String modelKey, String modelRoutePolicy, String modelState, String modelFailureCode,
				String modelSwitchReason, String actualModelKey, AskDataEventCorrelation correlation) {
			this(context, traceId, eventType, message, datasourceKey, denialCode, authorizationVersion,
					authorizationSource, null, null, modelType, modelKey, modelRoutePolicy, modelState,
					modelFailureCode, modelSwitchReason, actualModelKey, AskDataModelUsage.unknown(), correlation);
		}

		public MonitoringLog(AskDataProjectContext context, String traceId, String eventType, String message,
				String datasourceKey, String denialCode, String authorizationVersion, String authorizationSource,
				String capabilityKey, String purposeKey, String modelType, String modelKey, String modelRoutePolicy,
				String modelState, String modelFailureCode, String modelSwitchReason, String actualModelKey,
				AskDataModelUsage modelUsage, AskDataEventCorrelation correlation) {
			this(context, traceId, eventType, message, datasourceKey, denialCode, authorizationVersion,
					authorizationSource, capabilityKey, purposeKey, modelType, modelKey, modelRoutePolicy, modelState,
					modelFailureCode, modelSwitchReason, actualModelKey, modelUsage, correlation, null);
		}

		public MonitoringLog(AskDataProjectContext context, String traceId, String eventType, String message,
				String datasourceKey, String denialCode, String authorizationVersion, String authorizationSource,
				String capabilityKey, String purposeKey, String modelType, String modelKey, String modelRoutePolicy,
				String modelState, String modelFailureCode, String modelSwitchReason, String actualModelKey,
				AskDataModelUsage modelUsage, AskDataEventCorrelation correlation, String modelRouteReason) {
			this.context = context;
			this.traceId = traceId;
			this.eventType = eventType;
			this.message = message;
			this.datasourceKey = datasourceKey;
			this.denialCode = denialCode;
			this.authorizationVersion = authorizationVersion;
			this.authorizationSource = authorizationSource;
			this.capabilityKey = capabilityKey;
			this.purposeKey = purposeKey;
			this.modelType = modelType;
			this.modelKey = modelKey;
			this.modelRoutePolicy = modelRoutePolicy;
			this.modelRouteReason = modelRouteReason;
			this.modelState = modelState;
			this.modelFailureCode = modelFailureCode;
			this.modelSwitchReason = modelSwitchReason;
			this.actualModelKey = actualModelKey;
			this.modelUsage = modelUsage == null ? AskDataModelUsage.unknown() : modelUsage;
			this.correlation = correlation;
		}

		public String getTraceId() {
			return traceId;
		}

		public AskDataProjectContext getContext() {
			return context;
		}

		public MonitoringLog ownedBy(AskDataProjectContext owner) {
			if (owner == null) {
				throw new IllegalArgumentException("owner 不能为空");
			}
			if (context != null && (!context.getProjectId().equals(owner.getProjectId())
					|| !context.getOperatorId().equals(owner.getOperatorId()))) {
				throw new IllegalArgumentException("监控日志归属与存储上下文不一致");
			}
			return context == null ? new MonitoringLog(owner, traceId, eventType, message, datasourceKey,
					denialCode, authorizationVersion, authorizationSource, capabilityKey, purposeKey, modelType,
					modelKey, modelRoutePolicy, modelState, modelFailureCode, modelSwitchReason, actualModelKey,
					modelUsage, correlation, modelRouteReason) : this;
		}

		public String getEventType() {
			return eventType;
		}

		public String getMessage() {
			return message;
		}

		public String getDatasourceKey() { return datasourceKey; }
		public String getDenialCode() { return denialCode; }
		public String getAuthorizationVersion() { return authorizationVersion; }
		public String getAuthorizationSource() { return authorizationSource; }
		public String getCapabilityKey() { return capabilityKey; }
		public String getPurposeKey() { return purposeKey; }
		public String getModelType() { return modelType; }
		public String getModelKey() { return modelKey; }
		public String getModelRoutePolicy() { return modelRoutePolicy; }
		public String getModelRouteReason() { return modelRouteReason; }
		public String getModelState() { return modelState; }
		public String getModelFailureCode() { return modelFailureCode; }
		public String getModelSwitchReason() { return modelSwitchReason; }
		public String getActualModelKey() { return actualModelKey; }
		public AskDataModelUsage getModelUsage() { return modelUsage; }
		public AskDataEventCorrelation getCorrelation() { return correlation; }
	}
}
