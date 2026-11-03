package com.xiongx9527.fw.plus.ai.base.store;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通用检索文档：标识 + 文本内容 + 元数据 + 可选向量。
 *
 * <p>{@code id} 用字符串承载，业务侧的长整型主键由 adapter 或调用方自行转换，
 * 这样同一套契约既能装数据库行，也能装文件分片。</p>
 */
public final class Document {

    private final String id;

    private final String content;

    private final Map<String, Object> metadata;

    private final double[] vector;

    private Document(Builder builder) {
        this.id = builder.id;
        this.content = builder.content;
        this.metadata = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.metadata));
        this.vector = builder.vector;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Document of(String id, String content) {
        return builder().id(id).content(content).build();
    }

    public String getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public double[] getVector() {
        return vector;
    }

    public static final class Builder {

        private final Map<String, Object> metadata = new LinkedHashMap<String, Object>();

        private String id;

        private String content;

        private double[] vector;

        private Builder() {
        }

        public Builder id(String value) {
            this.id = value;
            return this;
        }

        public Builder content(String value) {
            this.content = value;
            return this;
        }

        public Builder metadata(Map<String, Object> value) {
            if (value != null) {
                this.metadata.putAll(value);
            }
            return this;
        }

        public Builder metadata(String key, Object value) {
            if (key != null) {
                this.metadata.put(key, value);
            }
            return this;
        }

        public Builder vector(double[] value) {
            this.vector = value;
            return this;
        }

        public Document build() {
            return new Document(this);
        }
    }
}
