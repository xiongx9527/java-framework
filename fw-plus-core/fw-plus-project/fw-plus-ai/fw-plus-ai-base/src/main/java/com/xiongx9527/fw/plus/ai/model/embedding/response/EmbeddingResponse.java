package com.xiongx9527.fw.plus.ai.model.embedding.response;

import com.xiongx9527.fw.plus.ai.data.embedding.Embedding;
import com.xiongx9527.fw.plus.ai.model.output.TokenUsage;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 嵌入响应。
 *
 * <p><b>作用</b>：返回与请求输入一一对应的向量列表，并附带模型名与令牌用量。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.embedding.response.EmbeddingResponse}。</p>
 *
 * <p><b>实现约定</b>：{@link #embeddings()} 顺序与请求输入顺序一致；
 * 元数据未设置时返回空元数据对象而非 {@code null}。</p>
 *
 * @author xiongxiao
 */
public class EmbeddingResponse {

    private final List<Embedding> embeddings;
    private final EmbeddingResponseMetadata metadata;
    private final String modelName;
    private final TokenUsage tokenUsage;

    /**
     * 由构建器构造。
     *
     * @param builder 构建器
     */
    protected EmbeddingResponse(Builder builder) {
        this.embeddings = builder.embeddings == null
                ? Collections.<Embedding>emptyList()
                : Collections.unmodifiableList(builder.embeddings);
        this.metadata = builder.metadata == null ? EmbeddingResponseMetadata.builder().build() : builder.metadata;
        this.modelName = builder.modelName != null ? builder.modelName : this.metadata.modelName();
        this.tokenUsage = builder.tokenUsage != null ? builder.tokenUsage : this.metadata.tokenUsage();
    }

    /**
     * 向量列表。
     *
     * @return 与请求输入顺序一致的向量列表，非 {@code null}
     */
    public List<Embedding> embeddings() {
        return embeddings;
    }

    /**
     * 响应元数据。
     *
     * @return 元数据对象，非 {@code null}
     */
    public EmbeddingResponseMetadata metadata() {
        return metadata;
    }

    /**
     * 模型名。
     *
     * @return 模型名；未知时为 {@code null}
     */
    public String modelName() {
        return modelName;
    }

    /**
     * 令牌用量。
     *
     * @return 令牌用量；未知时为 {@code null}
     */
    public TokenUsage tokenUsage() {
        return tokenUsage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EmbeddingResponse)) {
            return false;
        }
        EmbeddingResponse that = (EmbeddingResponse) o;
        return Objects.equals(embeddings, that.embeddings) && Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(embeddings, metadata);
    }

    @Override
    public String toString() {
        return "EmbeddingResponse{embeddings=" + embeddings.size() + ", metadata=" + metadata + "}";
    }

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /** {@link EmbeddingResponse} 构建器。 */
    public static class Builder {

        private List<Embedding> embeddings;
        private EmbeddingResponseMetadata metadata;
        private String modelName;
        private TokenUsage tokenUsage;

        /** 构造空构建器。 */
        public Builder() {
        }

        /**
         * 设置向量列表。
         *
         * @param embeddings 向量列表
         * @return 本构建器
         */
        public Builder embeddings(List<Embedding> embeddings) {
            this.embeddings = embeddings;
            return this;
        }

        /**
         * 设置响应元数据。
         *
         * @param metadata 元数据
         * @return 本构建器
         */
        public Builder metadata(EmbeddingResponseMetadata metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * 设置模型名。
         *
         * @param modelName 模型名
         * @return 本构建器
         */
        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        /**
         * 设置令牌用量。
         *
         * @param tokenUsage 令牌用量
         * @return 本构建器
         */
        public Builder tokenUsage(TokenUsage tokenUsage) {
            this.tokenUsage = tokenUsage;
            return this;
        }

        /**
         * 构建响应。
         *
         * @return 响应对象
         */
        public EmbeddingResponse build() {
            return new EmbeddingResponse(this);
        }
    }
}
