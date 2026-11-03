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
 * <p>{@code candidateIds} 是「只在给定 id 集合内检索」的限定条件（业务权限或候选集已在
 * 上游收敛时使用），与 {@code filter}（元数据等值过滤）互补：前者按主键收敛，后者按属性收敛。</p>
 */
public final class VectorSearchRequest {

    private final String collection;

    private final double[] vector;

    private final int topK;

    private final Set<String> candidateIds;

    private final Map<String, Object> filter;

    private VectorSearchRequest(Builder builder) {
        this.collection = builder.collection;
        this.vector = builder.vector;
        this.topK = builder.topK;
        this.candidateIds = Collections.unmodifiableSet(new LinkedHashSet<String>(builder.candidateIds));
        this.filter = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.filter));
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

    /** 元数据等值过滤；空表表示不过滤。 */
    public Map<String, Object> getFilter() {
        return filter;
    }

    public static final class Builder {

        private final Set<String> candidateIds = new LinkedHashSet<String>();

        private final Map<String, Object> filter = new LinkedHashMap<String, Object>();

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

        public Builder filter(Map<String, Object> value) {
            this.filter.clear();
            if (value != null) {
                this.filter.putAll(value);
            }
            return this;
        }

        public Builder filter(String key, Object value) {
            if (key != null) {
                this.filter.put(key, value);
            }
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
