package com.xiongx9527.fw.plus.ai;

import com.xiongx9527.fw.plus.ai.model.output.Response;

/**
 * 流式响应回调。
 *
 * <p><b>作用</b>：用于 {@code StreamingChatModel} / {@code StreamingLanguageModel} 的增量输出回调，
 * 把模型逐 token 的片段、最终响应与异常回传给调用方。</p>
 *
 * <p><b>出处</b>：抽象自 langchain4j 1.23.0 的 {@code dev.langchain4j.model.StreamingResponseHandler<T>}
 * （{@code onPartialResponse} / {@code onCompleteResponse} / {@code onError}），此处方法名收敛为
 * {@code onNext} / {@code onComplete} / {@code onError}；Spring AI 等价 {@code Flux} 的
 * {@code onNext/onComplete/onError} 语义。</p>
 *
 * <p><b>调用顺序约定</b>：正常情况下先多次 {@link #onNext(String)}，最后调用一次
 * {@link #onComplete(Response)}；任一步出错则调用一次 {@link #onError(Throwable)}，之后不再回调。
 * 实现应保证回调在同一调用上下文内串行发生。</p>
 *
 * @param <T> 最终响应的内容类型（如 {@code AiMessage}、{@code String}）
 */
public interface StreamingResponseHandler<T> {

    /**
     * 收到一个增量文本片段。
     *
     * @param token 本次增量片段（非完整文本）；可能为空串
     */
    void onNext(String token);

    /**
     * 流式输出正常结束。
     *
     * @param response 最终完整响应（含内容、用量、结束原因等）；实现方不应传入 {@code null}
     */
    void onComplete(Response<T> response);

    /**
     * 流式输出发生错误。
     *
     * @param error 错误原因；实现方不应传入 {@code null}
     */
    void onError(Throwable error);
}
