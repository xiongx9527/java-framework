package com.xiongx9527.fw.plus.ai.base.model;

/**
 * 模型目录条目：由各端口 {@code listModels()} 返回，用于前端选型、配置校验与按能力归类。
 *
 * <p>只保留跨厂商通用的字段；厂商私有字段不进入契约，避免上层被单一协议绑定。
 * 未知数值用 {@code 0} 表示（与 {@link EmbeddingPort#dimension()} 的约定一致）。</p>
 */
public final class ModelInfo {

    private final String id;

    private final String name;

    private final String ownedBy;

    private final ModelType type;

    private final int contextLength;

    private final int dimension;

    private ModelInfo(Builder builder) {
        this.id = builder.id;
        this.name = builder.name == null ? builder.id : builder.name;
        this.ownedBy = builder.ownedBy;
        this.type = builder.type;
        this.contextLength = builder.contextLength;
        this.dimension = builder.dimension;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** 厂商侧模型标识，直接用于请求体的 {@code model} 字段。 */
    public String getId() {
        return id;
    }

    /** 展示名；厂商未提供时回落到 {@link #getId()}。 */
    public String getName() {
        return name;
    }

    /** 归属方（如 {@code openai}、{@code system}），未知时为 {@code null}。 */
    public String getOwnedBy() {
        return ownedBy;
    }

    public ModelType getType() {
        return type;
    }

    /** 上下文长度（token）；未知时为 {@code 0}。 */
    public int getContextLength() {
        return contextLength;
    }

    /** 向量维度（仅 {@link ModelType#EMBEDDING} 有意义）；未知时为 {@code 0}。 */
    public int getDimension() {
        return dimension;
    }

    @Override
    public String toString() {
        return "ModelInfo{id=" + id + ", type=" + type + ", contextLength=" + contextLength
                + ", dimension=" + dimension + "}";
    }

    public static final class Builder {

        private String id;

        private String name;

        private String ownedBy;

        private ModelType type;

        private int contextLength;

        private int dimension;

        public Builder id(String value) {
            this.id = value;
            return this;
        }

        public Builder name(String value) {
            this.name = value;
            return this;
        }

        public Builder ownedBy(String value) {
            this.ownedBy = value;
            return this;
        }

        public Builder type(ModelType value) {
            this.type = value;
            return this;
        }

        public Builder contextLength(int value) {
            this.contextLength = value;
            return this;
        }

        public Builder dimension(int value) {
            this.dimension = value;
            return this;
        }

        public ModelInfo build() {
            return new ModelInfo(this);
        }
    }
}
