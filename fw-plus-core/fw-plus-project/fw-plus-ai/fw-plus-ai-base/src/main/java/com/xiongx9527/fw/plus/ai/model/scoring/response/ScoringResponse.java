package com.xiongx9527.fw.plus.ai.model.scoring.response;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 相关性打分响应。
 *
 * <p><b>作用</b>：返回与请求文档顺序一致的相关性分数，并附带元数据。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.scoring.response.ScoringResponse}。</p>
 *
 * @author xiongxiao
 */
public class ScoringResponse {

    private final List<Double> scores;
    private final ScoringResponseMetadata metadata;
    private final String modelName;
    private final com.xiongx9527.fw.plus.ai.model.output.TokenUsage tokenUsage;

    private ScoringResponse(Builder builder) {
        this.scores = builder.scores == null
                ? Collections.<Double>emptyList()
                : Collections.unmodifiableList(builder.scores);
        this.metadata = builder.metadata == null ? ScoringResponseMetadata.builder().build() : builder.metadata;
        this.modelName = builder.modelName != null ? builder.modelName : this.metadata.modelName();
        this.tokenUsage = builder.tokenUsage != null ? builder.tokenUsage : this.metadata.tokenUsage();
    }

    /**
     * 相关性分数列表。
     *
     * @return 与请求文档顺序一致的分数，非 {@code null}
     */
    public List<Double> scores() {
        return scores;
    }

    /**
     * 响应元数据。
     *
     * @return 元数据，非 {@code null}
     */
    public ScoringResponseMetadata metadata() {
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
    public com.xiongx9527.fw.plus.ai.model.output.TokenUsage tokenUsage() {
        return tokenUsage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ScoringResponse)) {
            return false;
        }
        ScoringResponse that = (ScoringResponse) o;
        return Objects.equals(scores, that.scores) && Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scores, metadata);
    }

    @Override
    public String toString() {
        return "ScoringResponse{scores=" + scores + ", metadata=" + metadata + "}";
    }

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /** {@link ScoringResponse} 构建器。 */
    public static class Builder {

        private List<Double> scores;
        private ScoringResponseMetadata metadata;
        private String modelName;
        private com.xiongx9527.fw.plus.ai.model.output.TokenUsage tokenUsage;

        /** 构造空构建器。 */
        public Builder() {
        }

        /**
         * 设置相关性分数列表。
         *
         * @param scores 分数列表
         * @return 本构建器
         */
        public Builder scores(List<Double> scores) {
            this.scores = scores;
            return this;
        }

        /**
         * 设置响应元数据。
         *
         * @param metadata 元数据
         * @return 本构建器
         */
        public Builder metadata(ScoringResponseMetadata metadata) {
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
        public Builder tokenUsage(com.xiongx9527.fw.plus.ai.model.output.TokenUsage tokenUsage) {
            this.tokenUsage = tokenUsage;
            return this;
        }

        /**
         * 构建响应。
         *
         * @return 响应对象
         */
        public ScoringResponse build() {
            return new ScoringResponse(this);
        }
    }
}
