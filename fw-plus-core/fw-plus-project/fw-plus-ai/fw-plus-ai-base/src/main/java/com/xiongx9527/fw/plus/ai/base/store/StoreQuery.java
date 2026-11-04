package com.xiongx9527.fw.plus.ai.base.store;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通用召回查询：图库按 {@link #getText()} 文本 + {@link #getFilter()} 元数据召回；向量库按 {@link #getVector()} 召回。
 *
 * <p>{@code filter} 为元数据等值过滤条件（key=属性名，value=期望值）；文本与向量至少提供其一。</p>
 */
public final class StoreQuery {

    private final String text;

    private final double[] vector;

    private final int topK;

    private final Map<String, Object> filter;

    private StoreQuery(Builder builder) {
        this.text = builder.text;
        this.vector = builder.vector;
        this.topK = builder.topK;
        this.filter = builder.filter == null ? null
                : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.filter));
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getText() {
        return text;
    }

    public double[] getVector() {
        return vector;
    }

    public int getTopK() {
        return topK;
    }

    public Map<String, Object> getFilter() {
        return filter;
    }

    public static final class Builder {

        private String text;

        private double[] vector;

        private int topK = 10;

        private Map<String, Object> filter;

        private Builder() {
        }

        public Builder text(String value) {
            this.text = value;
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

        public Builder filter(Map<String, Object> value) {
            this.filter = value;
            return this;
        }

        public StoreQuery build() {
            return new StoreQuery(this);
        }
    }
}
