package com.xiongx9527.fw.plus.ai.model.embedding.response;

import com.xiongx9527.fw.plus.ai.model.output.TokenUsage;

import java.util.Objects;

/**
 * 嵌入响应元数据。
 *
 * <p><b>作用</b>：把「模型名 + 令牌用量」从业务载荷（向量）中分离出来，
 * 便于统一记录计费与审计信息。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.embedding.response.EmbeddingResponseMetadata}。</p>
 *
 * @author xiongxiao
 */
public class EmbeddingResponseMetadata {

    private final String modelName;
    private final TokenUsage tokenUsage;

    /**
     * 由构建器构造。
     *
     * @param builder 构建器
     */
    protected EmbeddingResponseMetadata(Builder<?> builder) {
        this.modelName = builder.modelName;
        this.tokenUsage = builder.tokenUsage;
    }

    /**
     * 模型名。
     *
     * @return 模型名；厂商未返回时为 {@code null}
     */
    public String modelName() {
        return modelName;
    }

    /**
     * 令牌用量。
     *
     * @return 令牌用量；厂商未返回时为 {@code null}
     */
    public TokenUsage tokenUsage() {
        return tokenUsage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EmbeddingResponseMetadata)) {
            return false;
        }
        EmbeddingResponseMetadata that = (EmbeddingResponseMetadata) o;
        return Objects.equals(modelName, that.modelName) && Objects.equals(tokenUsage, that.tokenUsage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelName, tokenUsage);
    }

    @Override
    public String toString() {
        return "EmbeddingResponseMetadata{modelName='" + modelName + "', tokenUsage=" + tokenUsage + "}";
    }

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    @SuppressWarnings("rawtypes")
    public static Builder<?> builder() {
        return new Builder();
    }

    /**
     * 构建器。
     *
     * @param <T> 具体构建器类型
     */
    public static class Builder<T extends Builder<T>> {

        private String modelName;
        private TokenUsage tokenUsage;

        /** 构造空构建器。 */
        public Builder() {
        }

        /**
         * 设置模型名。
         *
         * @param modelName 模型名
         * @return 本构建器
         */
        public T modelName(String modelName) {
            this.modelName = modelName;
            return self();
        }

        /**
         * 设置令牌用量。
         *
         * @param tokenUsage 令牌用量
         * @return 本构建器
         */
        public T tokenUsage(TokenUsage tokenUsage) {
            this.tokenUsage = tokenUsage;
            return self();
        }

        /**
         * 返回具体构建器类型。
         *
         * @return 本构建器
         */
        @SuppressWarnings("unchecked")
        protected T self() {
            return (T) this;
        }

        /**
         * 构建元数据。
         *
         * @return 元数据对象
         */
        public EmbeddingResponseMetadata build() {
            return new EmbeddingResponseMetadata(this);
        }
    }
}
