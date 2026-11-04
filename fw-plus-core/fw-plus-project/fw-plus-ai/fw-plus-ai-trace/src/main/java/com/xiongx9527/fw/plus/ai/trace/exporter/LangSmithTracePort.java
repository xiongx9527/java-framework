package com.xiongx9527.fw.plus.ai.trace.exporter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.base.model.LlmUsage;
import com.xiongx9527.fw.plus.ai.base.trace.Span;
import com.xiongx9527.fw.plus.ai.base.trace.SpanKind;
import com.xiongx9527.fw.plus.ai.base.trace.TraceContext;
import com.xiongx9527.fw.plus.ai.base.trace.TracePort;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LangSmith 导出器：把中性 Span 翻译成 LangSmith run 报文并上报。
 * 协议：POST {baseUrl}/api/v1/runs，请求头 x-api-key 鉴权。
 * SpanKind 映射到 LangSmith 的 run_type（LLM/EMBEDDING/RETRIEVAL/TOOL/CHAIN 等）。
 */
public final class LangSmithTracePort implements TracePort {
    private static final Logger log = LoggerFactory.getLogger(LangSmithTracePort.class);
    private final String baseUrl;
    private final String apiKey;
    private final HttpSender sender;
    private final ObjectMapper mapper = new ObjectMapper();
    public LangSmithTracePort(String baseUrl, String apiKey) { this(baseUrl, apiKey, new JdkHttpSender()); }
    public LangSmithTracePort(String baseUrl, String apiKey, HttpSender sender) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) throw new IllegalArgumentException("langsmith baseUrl is required");
        if (apiKey == null || apiKey.trim().isEmpty()) throw new IllegalArgumentException("langsmith apiKey is required");
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.apiKey = apiKey;
        this.sender = sender == null ? new JdkHttpSender() : sender;
    }
    @Override
    public Span.Builder start(String name, SpanKind kind) {
        String traceId = TraceContext.traceId();
        if (traceId == null) traceId = UUID.randomUUID().toString();
        Span.Builder builder = Span.start(traceId, name, kind);
        String parent = TraceContext.spanId();
        if (parent != null) builder.parent(parent);
        return builder;
    }
    @Override
    public void end(Span.Builder span) {
        if (span == null) return;
        export(span.build());
    }
    @Override
    public void flush() {}
    private void export(Span span) {
        Map<String, Object> run = new LinkedHashMap<String, Object>();
        run.put("id", Uuids.normalize(span.getSpanId()));
        run.put("trace_id", Uuids.normalize(span.getTraceId()));
        if (span.getParentSpanId() != null) run.put("parent_run_id", Uuids.normalize(span.getParentSpanId()));
        run.put("name", span.getName());
        run.put("run_type", runType(span.getKind()));
        run.put("start_time", Iso8601.fromEpochMillis(span.getStartEpochMillis()));
        run.put("end_time", Iso8601.fromEpochMillis(span.getEndEpochMillis()));
        if (span.getInput() != null) run.put("inputs", Collections.singletonMap("content", span.getInput()));
        if (span.getOutput() != null) run.put("outputs", Collections.singletonMap("content", span.getOutput()));
        if (span.getError() != null) run.put("error", span.getError());
        Map<String, Object> extra = new LinkedHashMap<String, Object>();
        if (!span.getAttributes().isEmpty()) extra.put("metadata", new LinkedHashMap<String, Object>(span.getAttributes()));
        if (span.getUsage() != null) {
            LlmUsage usage = span.getUsage();
            Map<String, Object> tokens = new LinkedHashMap<String, Object>();
            tokens.put("prompt", usage.getInputTokens());
            tokens.put("completion", usage.getOutputTokens());
            tokens.put("total", usage.getTotalTokens());
            extra.put("tokens", tokens);
        }
        if (!extra.isEmpty()) run.put("extra", extra);
        post(run);
    }
    private void post(Map<String, Object> run) {
        try {
            String json = mapper.writeValueAsString(run);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put("x-api-key", apiKey);
            sender.post(baseUrl + "/api/v1/runs", headers, json);
        } catch (Exception ex) { log.warn("langsmith export failed: {}", ex.getMessage()); }
    }
    private static String runType(SpanKind kind) {
        if (kind == null) return "chain";
        switch (kind) {
            case LLM: case MODERATION: case RERANK: return "llm";
            case EMBEDDING: return "embedding";
            case RETRIEVAL: return "retriever";
            case TOOL: case MEMORY: return "tool";
            case CHAIN: return "chain";
            case INTERNAL: default: return "chain";
        }
    }
}
