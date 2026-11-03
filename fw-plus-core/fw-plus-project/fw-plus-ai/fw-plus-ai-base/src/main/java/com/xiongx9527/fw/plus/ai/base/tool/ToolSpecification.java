package com.xiongx9527.fw.plus.ai.base.tool;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工具规格：给模型看的声明（名字 + 说明 + 参数 JSON Schema）。
 *
 * <p>{@code parameters} 用 JSON Schema 的通用结构（type / properties / required），
 * 由 adapter 直接序列化进上游请求，base 不做 JSON 依赖。</p>
 */
public final class ToolSpecification {

    private final String name;

    private final String description;

    private final Map<String, Object> parameters;

    private final Map<String, Object> metadata;

    private ToolSpecification(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.parameters = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.parameters));
        this.metadata = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.metadata));
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public static final class Builder {

        private final Map<String, Object> parameters = new LinkedHashMap<String, Object>();

        private final Map<String, Object> metadata = new LinkedHashMap<String, Object>();

        private String name;

        private String description;

        private Builder() {
        }

        public Builder name(String value) {
            this.name = value;
            return this;
        }

        public Builder description(String value) {
            this.description = value;
            return this;
        }

        public Builder parameters(Map<String, Object> value) {
            this.parameters.clear();
            if (value != null) {
                this.parameters.putAll(value);
            }
            return this;
        }

        public Builder metadata(String key, Object value) {
            if (key != null) {
                this.metadata.put(key, value);
            }
            return this;
        }

        public ToolSpecification build() {
            return new ToolSpecification(this);
        }
    }
}
