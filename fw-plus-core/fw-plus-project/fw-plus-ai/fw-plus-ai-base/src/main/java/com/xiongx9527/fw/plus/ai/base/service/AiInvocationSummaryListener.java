package com.xiongx9527.fw.plus.ai.base.service;

/**
 * 一次 AI 服务调用结束后的全量摘要监听。
 *
 * <p>与 {@link AiObservationListener} 的区别：后者按 start / end / error 分三次回调，
 * 本接口只在收口时给一份含 {@link AiCallObservation#getEvents() 完整事件序列} 的快照
 * （对齐 LangChain4j 1.23 的 {@code AiServiceInvocationSummaryListener}）。
 * 监听器异常会被吞掉，不影响主链路。</p>
 */
public interface AiInvocationSummaryListener {

    void onSummary(AiCallObservation summary);
}
