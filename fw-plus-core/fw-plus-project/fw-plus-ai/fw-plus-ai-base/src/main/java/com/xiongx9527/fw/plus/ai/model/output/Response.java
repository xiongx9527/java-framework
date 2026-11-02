package com.xiongx9527.fw.plus.ai.model.output;

import java.util.Collections;
import java.util.Map;

/**
 * 模型响应的通用载体。
 *
 * <p><b>作用</b>：把「业务载荷 + 用量 + 终止原因 + 厂商附加元数据」打包返回，
 * 让上层用统一形态消费不同厂商 / 不同模态（文本、向量、打分、审核）的结果。</p>
 *
 * <p><b>出处</b>：逐字镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.model.output.Response}。
 * Spring AI 的等价概念分散在 {@code ChatResponse} / {@code EmbeddingResponse} 中。</p>
 *
 * <p><b>实现约定</b>：不可变；{@link #metadata()} 永不返回 {@code null}（无元数据时返回空 Map）。</p>
 *
 * @param <T> 业务载荷类型
 */
public class Response<T> {

    private final T content;
    private final TokenUsage tokenUsage;
    private final FinishReason finishReason;
    private final Map<String, Object> metadata;

    /**
     * 只带业务载荷构造。
     *
     * @param content 业务载荷
     */
    public Response(T content) {
        this(content, null, null, null);
    }

    /**
     * 带用量与终止原因构造。
     *
     * @param content      业务载荷
     * @param tokenUsage   令牌用量，可为 {@code null}
     * @param finishReason 终止原因，可为 {@code null}
     */
    public Response(T content, TokenUsage tokenUsage, FinishReason finishReason) {
        this(content, tokenUsage, finishReason, null);
    }

    /**
     * 全量构造。
     *
     * @param content      业务载荷
     * @param tokenUsage   令牌用量，可为 {@code null}
     * @param finishReason 终止原因，可为 {@code null}
     * @param metadata     厂商附加元数据，可为 {@code null}
     */
    public Response(T content, TokenUsage tokenUsage, FinishReason finishReason,
                    Map<String, Object> metadata) {
        this.content = content;
        this.tokenUsage = tokenUsage;
        this.finishReason = finishReason;
        this.metadata = metadata == null ? Collections.<String, Object>emptyMap() : metadata;
    }

    /**
     * 返回业务载荷。
     *
     * @return 业务载荷
     */
    public T content() {
        return content;
    }

    /**
     * 返回令牌用量。
     *
     * @return 令牌用量；未知时为 {@code null}
     */
    public TokenUsage tokenUsage() {
        return tokenUsage;
    }

    /**
     * 返回终止原因。
     *
     * @return 终止原因；不适用时为 {@code null}
     */
    public FinishReason finishReason() {
        return finishReason;
    }

    /**
     * 返回厂商附加元数据。
     *
     * @return 元数据 Map，非 {@code null}
     */
    public Map<String, Object> metadata() {
        return metadata;
    }

    /**
     * 只带业务载荷创建响应。
     *
     * @param content 业务载荷
     * @param <T>     载荷类型
     * @return 响应对象
     */
    public static <T> Response<T> from(T content) {
        return new Response<>(content);
    }

    /**
     * 带用量创建响应。
     *
     * @param content    业务载荷
     * @param tokenUsage 令牌用量
     * @param <T>        载荷类型
     * @return 响应对象
     */
    public static <T> Response<T> from(T content, TokenUsage tokenUsage) {
        return new Response<>(content, tokenUsage, null);
    }

    /**
     * 带用量与终止原因创建响应。
     *
     * @param content      业务载荷
     * @param tokenUsage   令牌用量
     * @param finishReason 终止原因
     * @param <T>          载荷类型
     * @return 响应对象
     */
    public static <T> Response<T> from(T content, TokenUsage tokenUsage, FinishReason finishReason) {
        return new Response<>(content, tokenUsage, finishReason);
    }

    /**
     * 全量创建响应。
     *
     * @param content      业务载荷
     * @param tokenUsage   令牌用量
     * @param finishReason 终止原因
     * @param metadata     附加元数据
     * @param <T>          载荷类型
     * @return 响应对象
     */
    public static <T> Response<T> from(T content, TokenUsage tokenUsage, FinishReason finishReason,
                                       Map<String, Object> metadata) {
        return new Response<>(content, tokenUsage, finishReason, metadata);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Response)) {
            return false;
        }
        Response<?> that = (Response<?>) o;
        return java.util.Objects.equals(content, that.content)
                && java.util.Objects.equals(tokenUsage, that.tokenUsage)
                && finishReason == that.finishReason
                && java.util.Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(content, tokenUsage, finishReason, metadata);
    }

    @Override
    public String toString() {
        return "Response{content=" + content + ", tokenUsage=" + tokenUsage
                + ", finishReason=" + finishReason + ", metadata=" + metadata + "}";
    }
}
