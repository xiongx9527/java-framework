package com.xiongx9527.fw.plus.ai.base.model;

/**
 * 大语言模型端口。
 *
 * <p><b>流式与非流式是同一端口的两条通道</b>：</p>
 * <ul>
 *   <li>{@link #stream(LlmRequest, LlmStreamHandler)} —— 增量回调，唯一的抽象方法</li>
 *   <li>{@link #complete(LlmRequest)} —— 一次性返回，默认由流式通道聚合而来</li>
 * </ul>
 *
 * <p>两条通道都会向 {@link LlmRequest#getListeners()} 派发同一组事件
 * （{@link LlmListener}），所以日志 / 计费 / 追踪只需实现一次。推荐 adapter 继承
 * {@link AbstractLlmPort}：它把事件派发统一收口，adapter 只实现「怎么发一次上游请求」。</p>
 *
 * <p>失败语义：{@link #stream} 不抛异常，错误通过 {@link LlmStreamHandler#onError(String)}
 * 与 {@link LlmListener#onError(String)} 交付（流式回调发生在 IO 线程上，抛异常会丢上下文）；
 * {@link #complete} 在聚合到错误时抛 {@link LlmException}，便于上层统一 try/catch 与重试。</p>
 */
public interface LlmPort {

    /**
     * 流式补全：按增量回调 {@code handler}，必然以 {@code onComplete} 或 {@code onError} 收尾。
     */
    void stream(LlmRequest request, LlmStreamHandler handler);

    /**
     * 非流式补全：聚合流式通道的正文、思考过程与用量，一次性返回。
     *
     * @throws LlmException 流式通道回调了 {@code onError}
     */
    default LlmResponse complete(LlmRequest request) {
        if (request == null) {
            throw new LlmException("llm request is required");
        }
        final StringBuilder content = new StringBuilder();
        final StringBuilder reasoning = new StringBuilder();
        final LlmUsage[] usage = {LlmUsage.ZERO};
        final String[] error = {null};
        stream(request, new LlmStreamHandler() {
            @Override
            public void onToken(String delta, String reasoningDelta) {
                if (delta != null) {
                    content.append(delta);
                }
                if (reasoningDelta != null) {
                    reasoning.append(reasoningDelta);
                }
            }

            @Override
            public void onComplete() {
                // 非流式调用方不关心结束事件
            }

            @Override
            public void onError(String message) {
                error[0] = message;
            }

            @Override
            public void onUsageDetail(long inputTokens, long outputTokens, long totalTokens) {
                usage[0] = new LlmUsage(inputTokens, outputTokens, totalTokens);
            }
        });
        if (error[0] != null) {
            throw new LlmException(error[0]);
        }
        return LlmResponse.builder()
                .content(content.length() == 0 ? null : content.toString())
                .reasoningContent(reasoning.length() == 0 ? null : reasoning.toString())
                .usage(usage[0])
                .build();
    }

    /** 单轮 system + user 的非流式补全快捷方式。 */
    default LlmResponse complete(String systemPrompt, String userPrompt) {
        return complete(LlmRequest.systemUser(systemPrompt, userPrompt));
    }
}
