package com.xiongx9527.fw.plus.ai.base.model;

import com.xiongx9527.fw.plus.ai.base.intent.IntentEvent;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalEvent;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionRequest;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionResult;

/**
 * 流式补全回调：承载一次 AI 调用的完整过程事件。
 *
 * <p>事件顺序（典型编排，具体由 {@code AiServices} 决定）：
 * <pre>
 *   onIntent(START)            // 意图分类开始（候选数）
 *   onIntent(END)  x N         // 每个命中意图一次（含排名 / 分值）
 *   onRetrieval(START)         // 检索发起（含查询）
 *   onRetrieval(END)           // 检索完成（含命中 + 注入上下文）
 *   onContextInject(START/END) // 检索上下文 / 技能目录 / 记忆 被注入
 *   onToken(...) x N           // 模型增量正文 / 思考过程
 *   onToolCall(call)           // 模型决定调用某工具
 *   onToolResponse(result)     // 该工具执行完成（成功 / 失败）
 *   ...（工具环可能重复 onToolCall/onToolResponse）...
 *   onUsage(...) / onUsageDetail(...)
 *   onComplete() | onError(...)
 * </pre>
 *
 * <p>除 {@link #onToken}、{@link #onComplete}、{@link #onError} 外，其余均为 {@code default} 空实现，
 * 调用方按需重写；一次调用必然以 {@code onComplete()} 或 {@code onError(String)} 之一收尾，
 * 实现方不得两者都回调。token 类回调可能任意线程触发，实现方自行保证线程安全。</p>
 *
 * <p>每个事件除基础签名外，另提供「带 {@link StreamEventContext} 上下文」的重载（对应 LangChain4j 1.x
 * 各 {@code *Context} 重载），默认委托回基础签名；需要回调内取消流或跨事件传递状态时重写。
 * 模型下发的、未映射到结构化事件的原始帧走 {@link #onUnmappedRawEvent(Object)}。</p>
 */
public interface LlmStreamHandler {

    /**
     * 收到一段增量内容。
     *
     * @param content          正文增量，可能为 {@code null}
     * @param reasoningContent 思考过程增量，可能为 {@code null}
     */
    void onToken(String content, String reasoningContent);

    /** 流正常结束（聚合完成，无错误）。 */
    void onComplete();

    /** 流出错（含模型未配置、网络失败、上游非 2xx、工具环不可恢复失败）。 */
    void onError(String message);

    /**
     * 收到一个完整的工具调用（开始）。
     *
     * <p>上游是增量下发 {@code tool_calls} 的（id / name / arguments 分片到达），
     * adapter 负责拼装完成后回调一次；同一轮 / 同一工具环内可能有多个调用。</p>
     */
    default void onToolCall(ToolExecutionRequest call) {
    }

    /**
     * 工具调用的参数增量（上游分片下发 id / name / arguments 时）。
     *
     * <p>对齐 LangChain4j 1.x {@code StreamingChatResponseHandler.onPartialToolCall}；
     * adapter 拼装完成后仍应回调一次 {@link #onToolCall}。</p>
     */
    default void onPartialToolCall(String id, String name, String argumentsDelta) {
    }

    /**
     * 一个完整工具调用拼装完成，与 {@link #onToolCall} 语义相同，
     * 供需要区分「增量 / 完整」阶段的监听器使用。
     */
    default void onCompleteToolCall(ToolExecutionRequest call) {
    }

    /**
     * 工具执行完成（结束），与 {@link #onToolCall} 配对。
     *
     * <p>{@code result} 的 {@link ToolExecutionResult#isError()} 标明成败；工具环内每执行一次工具回调一次。</p>
     */
    default void onToolResponse(ToolExecutionResult result) {
    }

    /**
     * 意图分类事件（开始 / 结束）。
     *
     * <p>{@code event.getPhase() == START} 时 {@link IntentEvent#getTotal()} 为候选意图数；
     * {@code END} 时每个命中意图上报一次，含 {@link IntentEvent#getRank()} 与 {@link IntentEvent#getScore()}。</p>
     */
    default void onIntent(IntentEvent event) {
    }

    /**
     * 检索事件（开始 / 结束）。
     *
     * <p>{@code START} 携带查询（{@link RetrievalEvent#getQuery()}）；
     * {@code END} 携带命中列表（{@link RetrievalEvent#getHits()}）与注入到对话的上下文（{@link RetrievalEvent#getContent()}）。</p>
     */
    default void onRetrieval(RetrievalEvent event) {
    }

    /**
     * 上下文注入事件（开始 / 结束）。
     *
     * <p>{@link ContextInjectEvent#getKind()} 标明来源（检索 / 技能 / 记忆 / 系统）；
     * {@code START} 表示即将注入，{@code END} 表示已注入完成。</p>
     */
    default void onContextInject(ContextInjectEvent event) {
    }

    /** 流末上报总 token 数；默认折算为 {@link #onUsageDetail(long, long, long)}。 */
    default void onUsage(long totalTokens) {
        onUsageDetail(0L, 0L, totalTokens);
    }

    /** 流末上报 token 明细；上游未上报分项时传 {@code 0}。默认忽略。 */
    default void onUsageDetail(long inputTokens, long outputTokens, long totalTokens) {
        // 只关心文本流的调用方无需实现
    }

    /**
     * 收到模型下发的、未映射到上述结构化事件的原始事件。
     *
     * <p>对齐 LangChain4j 1.x {@code StreamingChatResponseHandler.onUnmappedRawEvent(Object)}；
     * 例如供应商私有的增量状态、心跳或调试帧。默认忽略；需要落盘或透传的监听器重写本方法。
     * {@code rawEvent} 的具体类型由 adapter 决定，调用方不应假设其结构。</p>
     */
    default void onUnmappedRawEvent(Object rawEvent) {
    }

    /**
     * {@link #onToken} 的带上下文重载；默认委托回 {@link #onToken}。
     *
     * @see StreamEventContext
     */
    default void onToken(String content, String reasoningContent, StreamEventContext context) {
        onToken(content, reasoningContent);
    }

    /**
     * {@link #onPartialToolCall} 的带上下文重载；默认委托回 {@link #onPartialToolCall}。
     */
    default void onPartialToolCall(String id, String name, String argumentsDelta, StreamEventContext context) {
        onPartialToolCall(id, name, argumentsDelta);
    }

    /**
     * {@link #onCompleteToolCall} 的带上下文重载；默认委托回 {@link #onCompleteToolCall}。
     */
    default void onCompleteToolCall(ToolExecutionRequest call, StreamEventContext context) {
        onCompleteToolCall(call);
    }

    /**
     * {@link #onToolCall} 的带上下文重载；默认委托回 {@link #onToolCall}。
     */
    default void onToolCall(ToolExecutionRequest call, StreamEventContext context) {
        onToolCall(call);
    }

    /**
     * {@link #onToolResponse} 的带上下文重载；默认委托回 {@link #onToolResponse}。
     */
    default void onToolResponse(ToolExecutionResult result, StreamEventContext context) {
        onToolResponse(result);
    }
}
