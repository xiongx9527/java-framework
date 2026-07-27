package com.xiongx9527.fw.plus.ai.askdata.storage;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

import java.util.List;

/**
 * AskData 监控日志存储 SPI。
 *
 * <p>实现必须按项目和操作人共同隔离日志。内存实现仅适用于单节点部署；集群部署必须使用共享存储实现。</p>
 */
public interface AskDataMonitoringLogStore {

	void append(AskDataProjectContext context, MonitoringLog log);

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
