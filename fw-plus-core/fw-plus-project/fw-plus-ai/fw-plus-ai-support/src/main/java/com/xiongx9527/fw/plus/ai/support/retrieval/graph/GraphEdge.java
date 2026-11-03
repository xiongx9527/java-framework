package com.xiongx9527.fw.plus.ai.support.retrieval.graph;

/** 图边（GraphRAG 预留）。 */
public final class GraphEdge {

    private final String fromId;

    private final String toId;

    private final String type;

    private final double weight;

    private GraphEdge(Builder builder) {
        this.fromId = builder.fromId;
        this.toId = builder.toId;
        this.type = builder.type;
        this.weight = builder.weight;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getFromId() {
        return fromId;
    }

    public String getToId() {
        return toId;
    }

    public String getType() {
        return type;
    }

    public double getWeight() {
        return weight;
    }

    public static final class Builder {

        private String fromId;

        private String toId;

        private String type;

        private double weight = 1.0;

        private Builder() {
        }

        public Builder fromId(String value) {
            this.fromId = value;
            return this;
        }

        public Builder toId(String value) {
            this.toId = value;
            return this;
        }

        public Builder type(String value) {
            this.type = value;
            return this;
        }

        public Builder weight(double value) {
            this.weight = value;
            return this;
        }

        public GraphEdge build() {
            return new GraphEdge(this);
        }
    }
}
