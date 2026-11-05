package com.xiongx9527.fw.plus.ai.base.model;

import com.xiongx9527.fw.plus.ai.base.event.AiListener;

/**
 * 大语言调用事件监听器。
 *
 * <p><b>流式与非流式都会触发同一组事件</b>：请求开始、增量 token、用量、结束、错误。
 * 两条通道的事件序列一致，因此日志、计费、链路追踪等横切逻辑只需要写一份：</p>
 *
 * <pre>
 *   onRequest → onToken* → onResponse → onUsage → onComplete
 *   onRequest → onToken* → onResponse → onUsage → onError
 * </pre>
 *
 * <p>所有方法都有空实现，实现方只覆写关心的那一个。监听器在调用线程（含流式 IO 线程）上
 * 同步执行，必须自行保证线程安全，并且不得抛出异常——抛出会被吞掉，不影响主链路。</p>
 */
public interface LlmListener extends AiListener {

    /** 请求开始（参数已确定，尚未发起上游调用）。 */
    default void onRequest(LlmRequest request) {
    }

    /** 收到一段增量内容；非流式通道在结束时一次回调完整内容。 */
    default void onToken(String content, String reasoningContent) {
    }

    /**
     * 完整响应就绪（流式在聚合完成后、非流式在返回时触发）。
     *
     * <p>用于需要整段响应（而非逐 token）的横切逻辑，如整响应审计、按响应内容路由。</p>
     */
    default void onResponse(LlmResponse response) {
    }

    /** 用量上报；上游未上报时为 {@link LlmUsage#ZERO}。 */
    default void onUsage(LlmUsage usage) {
    }

    /** 正常结束（流式与非流式都触发）。 */
    default void onComplete() {
    }

    /**
     * 流结束原因（STOP / LENGTH / TOOL_EXECUTION / CONTENT_FILTER / OTHER）；
     * 对齐 {@link LlmStreamHandler#onFinish(String)} 的监听器侧镜像。
     * 上游不支持时不上报，{@code null}。默认忽略。
     */
    default void onFinish(String finishReason) {
    }

    /**
     * 模型下发的、未映射到结构化事件的原始事件；
     * 对齐 {@link LlmStreamHandler#onUnmappedRawEvent(Object)} 的监听器侧镜像。
     * 默认忽略（落盘 / 透传的监听器按需重写）。
     */
    default void onUnmappedRawEvent(Object rawEvent) {
    }

    /** 带上下文的请求事件；默认转发到 {@link #onRequest(LlmRequest)}。 */
    default void onRequest(LlmRequestContext context) {
        onRequest(context == null ? null : context.request());
    }

    /** 带上下文的响应事件；默认转发到 {@link #onResponse(LlmResponse)}。 */
    default void onResponse(LlmResponseContext context) {
        onResponse(context == null ? null : context.response());
    }

    /** 带上下文的错误事件；默认转发到 {@code onError(String)}。 */
    default void onError(LlmErrorContext context) {
        onError(context == null ? null : context.message());
    }

}
