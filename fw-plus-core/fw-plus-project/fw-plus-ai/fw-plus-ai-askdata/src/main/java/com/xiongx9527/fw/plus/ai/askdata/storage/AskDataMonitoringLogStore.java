package com.xiongx9527.fw.plus.ai.askdata.storage;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

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
	 * 监控日志的最小审计字段。
	 */
	class MonitoringLog {

		private final String traceId;
		private final String eventType;
		private final String message;

		public MonitoringLog(String traceId, String eventType, String message) {
			this.traceId = traceId;
			this.eventType = eventType;
			this.message = message;
		}

		public String getTraceId() {
			return traceId;
		}

		public String getEventType() {
			return eventType;
		}

		public String getMessage() {
			return message;
		}
	}
}
