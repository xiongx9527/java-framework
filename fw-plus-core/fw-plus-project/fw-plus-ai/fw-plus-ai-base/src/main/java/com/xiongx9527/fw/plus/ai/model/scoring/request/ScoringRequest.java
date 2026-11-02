package com.xiongx9527.fw.plus.ai.model.scoring.request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 相关性打分请求。
 *
 * <p><b>作用</b>：一次提交「一个查询 + 多篇候选文档」，由模型给出每篇的相关性分数，
 * 用于 RAG 重排、候选过滤等场景。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.scoring.request.ScoringRequest}。</p>
 *
 * <p><b>实现约定</b>：{@link #documents()} 与 {@link #query()} 均不可为空。</p>
 *
 * @author xiongxiao
 */
public class ScoringRequest {

    private final List<String> documents;
    private final String query;
    private final ScoringRequestParameters parameters;
    private final String modelName;

    private ScoringRequest(Builder builder) {
        if (builder.documents == null || builder.documents.isEmpty()) {
            throw new IllegalArgumentException("documents must not be empty");
        }
        if (builder.query == null || builder.query.trim().isEmpty()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        this.documents = Collections.unmodifiableList(new ArrayList<String>(builder.documents));
        this.query = builder.query;
        this.parameters = builder.parameters == null ? ScoringRequestParameters.EMPTY : builder.parameters;
        this.modelName = builder.modelName;
    }

    /**
     * 候选文档列表。
     *
     * @return 只读文档列表，非 {@code null}
     */
    public List<String> documents() {
        return documents;
    }

    /**
     * 查询文本。
     *
     * @return 查询
     */
    public String query() {
        return query;
    }

    /**
     * 请求参数。
     *
     * @return 参数对象，非 {@code null}
     */
    public ScoringRequestParameters parameters() {
        return parameters;
    }

    /**
     * 模型名。
     *
     * @return 模型名；未设置时为 {@code null}
     */
    public String modelName() {
        return modelName != null ? modelName : parameters.modelName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ScoringRequest)) {
            return false;
        }
        ScoringRequest that = (ScoringRequest) o;
        return Objects.equals(documents, that.documents)
                && Objects.equals(query, that.query)
                && Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documents, query, parameters);
    }

    @Override
    public String toString() {
        return "ScoringRequest{documents=" + documents + ", query='" + query + "'}";
    }

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /** {@link ScoringRequest} 构建器。 */
    public static class Builder {

        private List<String> documents;
        private String query;
        private ScoringRequestParameters parameters;
        private String modelName;

        /** 构造空构建器。 */
        public Builder() {
        }

        /**
         * 设置候选文档列表。
         *
         * @param documents 文档列表
         * @return 本构建器
         */
        public Builder documents(List<String> documents) {
            this.documents = documents;
            return this;
        }

        /**
         * 设置查询文本。
         *
         * @param query 查询
         * @return 本构建器
         */
        public Builder query(String query) {
            this.query = query;
            return this;
        }

        /**
         * 设置请求参数。
         *
         * @param parameters 参数
         * @return 本构建器
         */
        public Builder parameters(ScoringRequestParameters parameters) {
            this.parameters = parameters;
            return this;
        }

        /**
         * 设置模型名（会并入参数）。
         *
         * @param modelName 模型名
         * @return 本构建器
         */
        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        /**
         * 构建请求。
         *
         * @return 请求对象
         */
        public ScoringRequest build() {
            return new ScoringRequest(this);
        }
    }
}
