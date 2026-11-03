package com.xiongx9527.fw.plus.ai.base.model;

/**
 * 流式补全回调。
 *
 * <p>一次流式调用必然以 {@link #onComplete()} 或 {@link #onError(String)} 之一收尾，
 * 实现方不得两者都回调。token 回调可能在任意线程触发，实现方自行保证线程安全。</p>
 */
public interface LlmStreamHandler {

    /**
     * 收到一段增量内容。
     *
     * @param content          正文增量，可能为 {@code null}
     * @param reasoningContent 思考过程增量，可能为 {@code null}
     */
    void onToken(String content, String reasoningContent);

    /** 流正常结束。 */
    void onComplete();

    /** 流出错（含模型未配置、网络失败、上游非 2xx）。 */
    void onError(String message);

    /** 流末上报总 token 数；默认折算为 {@link #onUsageDetail(long, long, long)}。 */
    default void onUsage(long totalTokens) {
        onUsageDetail(0L, 0L, totalTokens);
    }

    /** 流末上报 token 明细；上游未上报分项时传 {@code 0}。默认忽略。 */
    default void onUsageDetail(long inputTokens, long outputTokens, long totalTokens) {
        // 只关心文本流的调用方无需实现
    }
}
