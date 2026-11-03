package com.xiongx9527.fw.plus.ai.base.tool;

/**
 * 工具调用环的输出槽：最终答案增量、工具事件、错误。
 *
 * <p>只新增一种信息帧——工具中间过程用 {@code onToolEvent} 表达，不混入答案增量，
 * 因此流式与非流式、文本与思考过程都能走同一套回调。</p>
 */
public interface ToolCallingSink {

    /** 最终答案增量（思考过程单独传第二个参数）。 */
    default void onDelta(String content, String reasoning) {
    }

    /** 工具事件：{@code phase} 为 {@code start} / {@code end}。 */
    default void onToolEvent(String name, String phase) {
    }

    /** 工具执行结果（与 {@code onToolEvent(name, "end")} 配对，但携带结构化结果）。 */
    default void onToolResponse(ToolExecutionResult result) {
    }

    /** 出错（不阻塞的情况下，例如工具失败但最终仍给出答案）。 */
    default void onError(String message) {
    }
}
