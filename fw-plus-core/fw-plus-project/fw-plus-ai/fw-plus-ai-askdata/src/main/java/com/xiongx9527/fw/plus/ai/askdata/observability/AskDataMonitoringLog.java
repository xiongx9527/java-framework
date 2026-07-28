package com.xiongx9527.fw.plus.ai.askdata.observability;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

/**
 * 供管理员审计和监控查询的已脱敏 AskData 日志。
 *
 * <p>日志把项目作为第一分区、操作人作为第二分区。存储 SPI 和查询适配层必须使用这两个
 * 标识进行过滤，禁止通过仅按项目或仅按操作人查询而突破隔离边界。</p>
 */
public class AskDataMonitoringLog {

	private final AskDataProjectContext context;
	private final String traceId;
	private final AskDataEvent.Type eventType;
	private final String message;

	/**
	 * 创建一条已脱敏的监控日志。
	 *
	 * @param context 项目和操作人隔离上下文，不能为空
	 * @param traceId 调用链追踪标识，可为空
	 * @param eventType 对应的生命周期事件类型，不能为空
	 * @param message 已脱敏日志摘要，可为空
	 * @throws IllegalArgumentException 当上下文或事件类型为空时抛出
	 */
	public AskDataMonitoringLog(AskDataProjectContext context, String traceId, AskDataEvent.Type eventType,
			String message) {
		if (context == null) {
			throw new IllegalArgumentException("context 不能为空");
		}
		if (eventType == null) {
			throw new IllegalArgumentException("eventType 不能为空");
		}
		this.context = context;
		this.traceId = traceId;
		this.eventType = eventType;
		this.message = message;
	}

	/**
	 * 获取日志的项目和操作人隔离上下文。
	 *
	 * @return 不为空的隔离上下文
	 */
	public AskDataProjectContext getContext() {
		return context;
	}

	/**
	 * 获取调用链追踪标识。
	 *
	 * @return 调用方提供的追踪标识，可为空
	 */
	public String getTraceId() {
		return traceId;
	}

	/**
	 * 获取日志的生命周期事件类型。
	 *
	 * @return 不为空的事件类型
	 */
	public AskDataEvent.Type getEventType() {
		return eventType;
	}

	/**
	 * 获取已脱敏的日志摘要。
	 *
	 * @return 已脱敏摘要，可为空
	 */
	public String getMessage() {
		return message;
	}
}
