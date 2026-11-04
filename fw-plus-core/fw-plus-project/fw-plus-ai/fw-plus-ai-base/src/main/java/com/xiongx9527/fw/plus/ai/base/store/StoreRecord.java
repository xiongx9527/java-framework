package com.xiongx9527.fw.plus.ai.base.store;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 存储记录：向量库与图库共用的中立数据载体。
 *
 * <p>{@code id} 为唯一标识；{@code type} 为实体类型（向量库的文档类别 / 图库的节点标签）；
 * {@code metadata} 承载属性；{@code content} 为文本负载；{@code vector} 为嵌入（向量库使用）；
 * {@code score} 仅用于召回结果，表示相似度。</p>
 */
public final class StoreRecord {

    private final String id;

    private final String type;

    private final Map<String, Object> metadata;

    private final String content;

    private final double[] vector;

    private final double score;

    private StoreRecord(Builder builder) {
        this.id = builder.id;
        this.type = builder.type;
        this.metadata = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.metadata));
        this.content = builder.content;
        this.vector = builder.vector;
        this.score = builder.score;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public String getContent() {
        return content;
    }

    public double[] getVector() {
        return vector;
    }

    public double getScore() {
        return score;
    }

    public static final class Builder {

        private final Map<String, Object> metadata = new LinkedHashMap<String, Object>();

        private String id;

        private String type;

        private String content;

        private double[] vector;

        private double score = -1d;

        private Builder() {
        }

        public Builder id(String value) {
            this.id = value;
            return this;
        }

        public Builder type(String value) {
            this.type = value;
            return this;
        }

        public Builder content(String value) {
            this.content = value;
            return this;
        }

        public Builder vector(double[] value) {
            this.vector = value;
            return this;
        }

        public Builder score(double value) {
            this.score = value;
            return this;
        }

        public Builder metadata(Map<String, Object> value) {
            if (value != null) {
                this.metadata.putAll(value);
            }
            return this;
        }

        public StoreRecord build() {
            return new StoreRecord(this);
        }
    }
}
