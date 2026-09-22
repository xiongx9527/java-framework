package com.xiongx9527.fw.plus.ai.mcp.transport;

/**
 * SSE 写入通道抽象：屏蔽 SseEmitter 与 reactor Sinks 差异
 *
 * @author xiongxiao
 * @date 2026-09-22
 */
public interface SseSink {

	/**
	 * 写入一条 SSE 事件
	 */
	void send(String eventId, String jsonData);

	/**
	 * 正常结束通道
	 */
	void complete();

	/**
	 * 以异常结束通道
	 */
	void completeWithError(Throwable error);
}
