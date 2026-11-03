package com.xiongx9527.fw.plus.ai.support.retrieval.graph;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 图节点（GraphRAG 预留）。 */
public final class GraphNode {

    private final String id;

    private final String label;

    private final Map<String, Object> metadata;

    private final double[] vector;

    private GraphNode(Builder builder) {
        this.id = builder.id;
        this.label = builder.label;
        this.metadata = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.metadata));
        this.vector = builder.vector;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
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

        private String label;

        private double[] vector;

        private Builder() {
        }

        public Builder id(String value) {
            this.id = value;
            return this;
        }

        public Builder label(String value) {
            this.label = value;
            return this;
        }

        public Builder metadata(Map<String, Object> value) {
            if (value != null) {
                this.metadata.putAll(value);
            }
            return this;
        }

        public Builder vector(double[] value) {
            this.vector = value;
            return this;
        }

        public GraphNode build() {
            return new GraphNode(this);
        }
    }
}
