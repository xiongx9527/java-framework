package com.xiongx9527.fw.plus.ai.store.embedding;

import com.xiongx9527.fw.plus.ai.data.embedding.Embedding;
import com.xiongx9527.fw.plus.ai.store.embedding.filter.Filter;

import java.util.Objects;

/**
 * 向量检索请求。
 *
 * <p><b>作用</b>：把「用哪个向量、要几条、最低分多少、过滤条件是什么」收成一个不可变对象，
 * 避免 {@code search} 方法参数列表随能力扩展不断变长。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。逐成员镜像
 * {@code dev.langchain4j.store.embedding.EmbeddingSearchRequest}。</p>
 *
 * <p><b>实现约定</b>：{@link #queryEmbedding()} 必填；{@link #maxResults()} 默认 3，
 * {@link #minScore()} 默认 0（即不过滤）；{@link #filter()} 为 {@code null} 表示不加元数据过滤。</p>
 *
 * @author xiongxiao
 */
public class EmbeddingSearchRequest {

    /** 默认召回条数。 */
    private static final int DEFAULT_MAX_RESULTS = 3;

    private final String query;
    private final Embedding queryEmbedding;
    private final int maxResults;
    private final double minScore;
    private final Filter filter;

    /**
     * 兼容旧签名的构造。
     *
     * @param queryEmbedding 查询向量，不能为 {@code null}
     * @param maxResults     最大召回条数，{@code null} 时取默认值
     * @param minScore       最低相似度，{@code null} 时取默认值
     * @param filter         元数据过滤条件，可为 {@code null}
     */
    public EmbeddingSearchRequest(Embedding queryEmbedding, Integer maxResults, Double minScore, Filter filter) {
        this(null, queryEmbedding, maxResults, minScore, filter);
    }

    /**
     * 由构建器构造。
     *
     * @param builder 构建器
     */
    public EmbeddingSearchRequest(EmbeddingSearchRequestBuilder builder) {
        this(builder.query, builder.queryEmbedding, builder.maxResults, builder.minScore, builder.filter);
    }

    private EmbeddingSearchRequest(String query, Embedding queryEmbedding, Integer maxResults, Double minScore,
                                   Filter filter) {
        if (queryEmbedding == null) {
            throw new IllegalArgumentException("queryEmbedding must not be null");
        }
        this.query = query;
        this.queryEmbedding = queryEmbedding;
        this.maxResults = maxResults == null ? DEFAULT_MAX_RESULTS : maxResults;
        this.minScore = minScore == null ? 0.0d : minScore;
        this.filter = filter;
    }

    /**
     * 原始查询文本（可选，仅用于观测与调试）。
     *
     * @return 查询文本；未设置时为 {@code null}
     */
    public String query() {
        return query;
    }

    /**
     * 查询向量。
     *
     * @return 查询向量，非 {@code null}
     */
    public Embedding queryEmbedding() {
        return queryEmbedding;
    }

    /**
     * 最大召回条数。
     *
     * @return 条数，默认 3
     */
    public int maxResults() {
        return maxResults;
    }

    /**
     * 最低相似度阈值。
     *
     * @return 阈值，默认 0
     */
    public double minScore() {
        return minScore;
    }

    /**
     * 元数据过滤条件。
     *
     * @return 过滤条件；不过滤时为 {@code null}
     */
    public Filter filter() {
        return filter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EmbeddingSearchRequest)) {
            return false;
        }
        EmbeddingSearchRequest that = (EmbeddingSearchRequest) o;
        return maxResults == that.maxResults
                && Double.compare(minScore, that.minScore) == 0
                && Objects.equals(query, that.query)
                && Objects.equals(queryEmbedding, that.queryEmbedding)
                && Objects.equals(filter, that.filter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, queryEmbedding, maxResults, minScore, filter);
    }

    @Override
    public String toString() {
        return "EmbeddingSearchRequest{query='" + query + "', maxResults=" + maxResults
                + ", minScore=" + minScore + ", filter=" + filter + "}";
    }

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    public static EmbeddingSearchRequestBuilder builder() {
        return new EmbeddingSearchRequestBuilder();
    }

    /** {@link EmbeddingSearchRequest} 构建器。 */
    public static class EmbeddingSearchRequestBuilder {

        private String query;
        private Embedding queryEmbedding;
        private Integer maxResults;
        private Double minScore;
        private Filter filter;

        /** 构造空构建器。 */
        EmbeddingSearchRequestBuilder() {
        }

        /**
         * 设置原始查询文本。
         *
         * @param query 查询文本
         * @return 本构建器
         */
        public EmbeddingSearchRequestBuilder query(String query) {
            this.query = query;
            return this;
        }

        /**
         * 设置查询向量（必填）。
         *
         * @param queryEmbedding 查询向量
         * @return 本构建器
         */
        public EmbeddingSearchRequestBuilder queryEmbedding(Embedding queryEmbedding) {
            this.queryEmbedding = queryEmbedding;
            return this;
        }

        /**
         * 设置最大召回条数。
         *
         * @param maxResults 条数
         * @return 本构建器
         */
        public EmbeddingSearchRequestBuilder maxResults(Integer maxResults) {
            this.maxResults = maxResults;
            return this;
        }

        /**
         * 设置最低相似度阈值。
         *
         * @param minScore 阈值
         * @return 本构建器
         */
        public EmbeddingSearchRequestBuilder minScore(Double minScore) {
            this.minScore = minScore;
            return this;
        }

        /**
         * 设置元数据过滤条件。
         *
         * @param filter 过滤条件
         * @return 本构建器
         */
        public EmbeddingSearchRequestBuilder filter(Filter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * 构建请求。
         *
         * @return 请求对象
         */
        public EmbeddingSearchRequest build() {
            return new EmbeddingSearchRequest(this);
        }
    }
}
