package com.xiongx9527.fw.plus.ai.askdata.observability;

/**
 * AskData 观测事件发布 SPI。
 *
 * <p>实现可以将事件转换为 {@code AskDataMonitoringLogStore} 的存储记录，或接入任意外部
 * 观测系统；Core 不依赖具体观测厂商。实现发布前必须确认事件摘要已经脱敏，并保持
 * {@code projectId + operatorId} 的隔离边界。</p>
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
}
