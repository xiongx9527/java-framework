package com.xiongx9527.fw.plus.ai.base.trace;

import com.xiongx9527.fw.plus.ai.base.model.LlmUsage;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 一条链路节点（span）的可序列化快照。
 *
 * <p>中性结构：不绑定 Langfuse / LangSmith 的具体字段，由后端 exporter 翻译成各自的
 * generation / run 报文。{@code attributes} 承载平台无关的业务维度
 * （tenantId、toolName、collection 等）。</p>
 */
public final class Span {

    private final String traceId;

    private final String spanId;

    private final String parentSpanId;

    private final String name;

    private final SpanKind kind;

    private final long startEpochMillis;

    private final long endEpochMillis;

    private final SpanStatus status;

    private final String modelId;

    private final String input;

    private final String output;

    private final LlmUsage usage;

    private final String error;

    private final Map<String, Object> attributes;

    private Span(Builder b) {
        this.traceId = b.traceId;
        this.spanId = b.spanId;
        this.parentSpanId = b.parentSpanId;
        this.name = b.name;
        this.kind = b.kind;
        this.startEpochMillis = b.startEpochMillis;
        this.endEpochMillis = b.endEpochMillis <= 0 ? b.startEpochMillis : b.endEpochMillis;
        this.status = b.status;
        this.modelId = b.modelId;
        this.input = b.input;
        this.output = b.output;
        this.usage = b.usage;
        this.error = b.error;
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(b.attributes));
    }

    public String getTraceId() {
        return traceId;
    }

    public String getSpanId() {
        return spanId;
    }

    public String getParentSpanId() {
        return parentSpanId;
    }

    public String getName() {
        return name;
    }

    public SpanKind getKind() {
        return kind;
    }

    public long getStartEpochMillis() {
        return startEpochMillis;
    }

    public long getEndEpochMillis() {
        return endEpochMillis;
    }

    public long getDurationMillis() {
        return endEpochMillis - startEpochMillis;
    }

    public SpanStatus getStatus() {
        return status;
    }

    public String getModelId() {
        return modelId;
    }

    public String getInput() {
        return input;
    }

    public String getOutput() {
        return output;
    }

    public LlmUsage getUsage() {
        return usage;
    }

    public String getError() {
        return error;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /** 开启一个节点；spanId 自动生成。 */
    public static Builder start(String traceId, String name, SpanKind kind) {
        return new Builder(traceId == null ? UUID.randomUUID().toString() : traceId,
                UUID.randomUUID().toString(), name, kind == null ? SpanKind.INTERNAL : kind);
    }

    public static final class Builder {

        private final String traceId;

        private final String spanId;

        private String parentSpanId;

        private final String name;

        private final SpanKind kind;

        private final long startEpochMillis = System.currentTimeMillis();

        private long endEpochMillis;

        private SpanStatus status = SpanStatus.UNSET;

        private String modelId;

        private String input;

        private String output;

        private LlmUsage usage;

        private String error;

        private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();

        private Builder(String traceId, String spanId, String name, SpanKind kind) {
            this.traceId = traceId;
            this.spanId = spanId;
            this.name = name;
            this.kind = kind;
        }

        /** 本节点所属链路 id（可作为子节点的 traceId）。 */
        public String getTraceId() {
            return traceId;
        }

        /** 本节点 id（可作为子节点的 parentSpanId，也可绑定到 {@link TraceContext} 供下游串联）。 */
        public String getSpanId() {
            return spanId;
        }

        public Builder parent(String parentSpanId) {
            this.parentSpanId = parentSpanId;
            return this;
        }

        public Builder modelId(String value) {
            this.modelId = value;
            return this;
        }

        public Builder input(String value) {
            this.input = value;
            return this;
        }

        public Builder output(String value) {
            this.output = value;
            return this;
        }

        public Builder usage(LlmUsage value) {
            this.usage = value;
            return this;
        }

        public Builder status(SpanStatus value) {
            this.status = value == null ? SpanStatus.UNSET : value;
            return this;
        }

        public Builder error(String value) {
            this.error = value;
            this.status = SpanStatus.ERROR;
            return this;
        }

        public Builder attribute(String key, Object value) {
            if (key != null) {
                this.attributes.put(key, value);
            }
            return this;
        }

        public Builder end(long endEpochMillis) {
            this.endEpochMillis = endEpochMillis <= 0 ? System.currentTimeMillis() : endEpochMillis;
            if (this.status == SpanStatus.UNSET) {
                this.status = SpanStatus.OK;
            }
            return this;
        }

        public Span build() {
            return new Span(this);
        }
    }
}
