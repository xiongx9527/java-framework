package com.xiongx9527.fw.plus.ai.askdata.observability;

import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataMonitoringLogStore;

/**
 * AskData 观测事件发布 SPI。
 *
 * <p>每个已发布事件必须形成一条已脱敏的基础监控日志，并通过
 * {@link AskDataMonitoringLogStore#append(com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext,
 * AskDataMonitoringLogStore.MonitoringLog)} 按 {@code projectId + operatorId} 写入。LangSmith、Langfuse、
 * OpenTelemetry 等外部观测导出只能作为额外适配，不能替代基础日志存储；Core 不依赖任何具体观测厂商。</p>
 */
public interface AskDataEventPublisher {

	/**
	 * 发布一条已脱敏的 AskData 生命周期事件。
	 *
	 * <p>实现不得丢弃事件携带的项目和操作人上下文，也不得将其与其他项目或操作人的事件
	 * 合并。发布失败时的重试、降级或告警策略由适配层决定，但不得影响业务隔离约束。</p>
	 *
	 * @param event 已脱敏且携带隔离上下文的事件，不能为空
	 */
	void publish(AskDataEvent event);

	/**
	 * 创建将每个事件追加到基础监控日志存储的发布器。
	 *
	 * <p>该工厂是 Core 的强制观测路径：事件会先转换为 {@link AskDataMonitoringLog}，再转换为
	 * 存储 SPI 使用的记录，并使用事件原有的项目和操作人上下文追加。调用方如需导出外部观测系统，
	 * 应在此基础上组合额外发布器，不得跳过该存储发布器。</p>
	 *
	 * @param store 按项目和操作人隔离的监控日志存储，不能为空
	 * @return 将事件持久化为已脱敏监控日志的发布器
	 * @throws IllegalArgumentException 当存储为空时抛出
	 */
	static AskDataEventPublisher toStore(final AskDataMonitoringLogStore store) {
		return toStore(store, null);
	}

	/**
	 * 创建带本地诊断 fallback 的基础日志发布器。
	 *
	 * <p>主存储失败后只向 fallback 写入脱敏降级诊断，不复制可能含业务摘要的原事件。两级存储均失败时
	 * 也不会向业务流程抛出异常。</p>
	 */
	static AskDataEventPublisher toStore(final AskDataMonitoringLogStore store,
			final AskDataMonitoringLogStore fallbackStore) {
		if (store == null) {
			throw new IllegalArgumentException("store 不能为空");
		}
		return new AskDataEventPublisher() {
			@Override
			public void publish(AskDataEvent event) {
				if (event == null) {
					throw new IllegalArgumentException("event 不能为空");
				}
				try {
					store.append(event.getContext(), monitoringLog(event).toStorageLog());
				} catch (RuntimeException primaryFailure) {
					appendFallbackDiagnostic(fallbackStore, event);
				}
			}
		};
	}

	static AskDataMonitoringLog monitoringLog(AskDataEvent event) {
		return new AskDataMonitoringLog(event.getContext(), event.getTraceId(), event.getType(), event.getMessage(),
				event.getDatasourceKey(), event.getDenialCode(), event.getAuthorizationVersion(),
				event.getAuthorizationSource(), event.getCapabilityKey(), event.getPurposeKey(),
				event.getModelType() == null ? null : event.getModelType().name(),
				event.getModelKey(), event.getModelRoutePolicy() == null ? null : event.getModelRoutePolicy().name(),
				event.getModelState() == null ? null : event.getModelState().name(), event.getModelFailureCode(),
				event.getModelSwitchReason(), event.getActualModelKey(), event.getModelUsage(), event.getCorrelation(),
				event.getModelRouteReason());
	}

	static void appendFallbackDiagnostic(AskDataMonitoringLogStore fallbackStore, AskDataEvent failedEvent) {
		if (fallbackStore == null || failedEvent == null) {
			return;
		}
		try {
			String traceId = failedEvent.getTraceId();
			AskDataEventCorrelation original = failedEvent.getCorrelation();
			AskDataEventCorrelation correlation = null;
			if (original != null) {
				correlation = AskDataEventCorrelation.builder(original.getRequestId(), original.getConversationId())
						.subQuestionId(original.getSubQuestionId()).graphNodeId(original.getGraphNodeId())
						.agentKey(original.getAgentKey()).instanceId(original.getInstanceId())
						.parentEventId(original.getEventId()).build();
			}
			AskDataEvent diagnostic = correlation == null
					? new AskDataEvent(failedEvent.getContext(), traceId,
							AskDataEvent.Type.MONITORING_DELIVERY_DEGRADED, "AskData 监控存储降级")
					: AskDataEvent.correlated(failedEvent.getContext(), traceId,
							AskDataEvent.Type.MONITORING_DELIVERY_DEGRADED, "AskData 监控存储降级", correlation);
			fallbackStore.append(diagnostic.getContext(), monitoringLog(diagnostic).toStorageLog());
		} catch (RuntimeException fallbackFailure) {
			// 本地诊断失败不能改变 AskData 业务结果。
		}
	}
}
