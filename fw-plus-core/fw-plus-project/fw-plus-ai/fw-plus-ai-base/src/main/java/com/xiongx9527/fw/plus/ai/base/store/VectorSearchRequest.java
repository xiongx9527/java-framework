package com.xiongx9527.fw.plus.ai.base.store;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 向量检索请求。
 *
 * <p>收敛候选集有两条互补路径：</p>
 * <ul>
 *   <li>{@code candidateIds}——按主键（文档 id）收敛，业务权限或上游已收敛候选集时使用；</li>
 *   <li>{@code filter}——按元数据属性收敛（等值 / 范围 / 包含 / 组合），见 {@link Filter}。</li>
 * </ul>
 *
 * <p>两条路径可同时使用：先按主键收敛再按属性过滤，等价于「白名单 ∩ 属性」。</p>
 */
public final class VectorSearchRequest {

    private final String collection;

    private final double[] vector;

    private final int topK;

    private final Set<String> candidateIds;

    private final Filter filter;

    private VectorSearchRequest(Builder builder) {
        this.collection = builder.collection;
        this.vector = builder.vector;
        this.topK = builder.topK;
        this.candidateIds = Collections.unmodifiableSet(new LinkedHashSet<String>(builder.candidateIds));
        this.filter = builder.filter == null ? Filter.NO_OP : builder.filter;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getCollection() {
        return collection;
    }

    public double[] getVector() {
        return vector;
    }

    public int getTopK() {
        return topK;
    }

    /** 候选 id 限定；空集表示不限定。 */
    public Set<String> getCandidateIds() {
        return candidateIds;
    }

    /** 元数据过滤条件；{@link Filter#NO_OP} 表示不过滤。 */
    public Filter getFilter() {
        return filter;
    }

    public static final class Builder {

        private final Set<String> candidateIds = new LinkedHashSet<String>();

        private Filter filter = Filter.NO_OP;

        private String collection;

        private double[] vector;

        private int topK = 10;

        private Builder() {
        }

        public Builder collection(String value) {
            this.collection = value;
            return this;
        }

        public Builder vector(double[] value) {
            this.vector = value;
            return this;
        }

        public Builder topK(int value) {
            this.topK = value;
            return this;
        }

        public Builder candidateIds(Collection<String> value) {
            this.candidateIds.clear();
            if (value != null) {
                this.candidateIds.addAll(value);
            }
            return this;
        }

        public Builder filter(Filter value) {
            this.filter = value == null ? Filter.NO_OP : value;
            return this;
        }

        /** 便捷：单个元数据等值条件。 */
        public Builder filter(String key, Object value) {
            this.filter = Filter.builder().eq(key, value).build();
            return this;
        }

        /** 便捷：多个元数据等值条件（AND）。 */
        public Builder filter(Map<String, Object> values) {
            if (values == null || values.isEmpty()) {
                this.filter = Filter.NO_OP;
                return this;
            }
            Filter.Builder b = Filter.builder();
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                b.eq(entry.getKey(), entry.getValue());
            }
            this.filter = b.build();
            return this;
        }

        public VectorSearchRequest build() {
            if (vector == null) {
                throw new IllegalArgumentException("vector is required");
            }
            return new VectorSearchRequest(this);
        }
    }
}
