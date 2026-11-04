package com.xiongx9527.fw.plus.ai.trace.exporter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.base.model.LlmUsage;
import com.xiongx9527.fw.plus.ai.base.trace.Span;
import com.xiongx9527.fw.plus.ai.base.trace.SpanKind;
import com.xiongx9527.fw.plus.ai.base.trace.TraceContext;
import com.xiongx9527.fw.plus.ai.base.trace.TracePort;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Langfuse 导出器：把中性 Span 翻译成 Langfuse ingestion 报文并上报。
 * 协议：POST {baseUrl}/api/public/ingestion，Basic 鉴权（publicKey:secretKey）。
 * 每次 end 发一条 batch，内含 trace.create 与 observation（LLM/EMBEDDING/RERANK 落成 generation，其余落成 span）。
 */
public final class LangfuseTracePort implements TracePort {
    private static final Logger log = LoggerFactory.getLogger(LangfuseTracePort.class);
    private final String baseUrl;
    private final String authHeader;
    private final HttpSender sender;
    private final ObjectMapper mapper = new ObjectMapper();

    public LangfuseTracePort(String baseUrl, String publicKey, String secretKey) {
        this(baseUrl, publicKey, secretKey, new JdkHttpSender());
    }
    public LangfuseTracePort(String baseUrl, String publicKey, String secretKey, HttpSender sender) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) throw new IllegalArgumentException("langfuse baseUrl is required");
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString((publicKey + ":" + secretKey).getBytes(StandardCharsets.UTF_8));
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
        String now = Iso8601.fromEpochMillis(span.getEndEpochMillis());
        List<Map<String, Object>> batch = new ArrayList<Map<String, Object>>();
        Map<String, Object> traceBody = new LinkedHashMap<String, Object>();
        traceBody.put("id", Uuids.normalize(span.getTraceId()));
        traceBody.put("name", span.getName());
        if (!span.getAttributes().isEmpty()) traceBody.put("metadata", new LinkedHashMap<String, Object>(span.getAttributes()));
        batch.add(event(UUID.randomUUID().toString(), "trace.create", now, traceBody));
        boolean generation = isGeneration(span.getKind());
        Map<String, Object> obs = new LinkedHashMap<String, Object>();
        obs.put("id", Uuids.normalize(span.getSpanId()));
        obs.put("traceId", Uuids.normalize(span.getTraceId()));
        if (span.getParentSpanId() != null) obs.put("parentObservationId", Uuids.normalize(span.getParentSpanId()));
        obs.put("name", span.getName());
        obs.put("type", generation ? "GENERATION" : "SPAN");
        obs.put("startTime", Iso8601.fromEpochMillis(span.getStartEpochMillis()));
        obs.put("endTime", Iso8601.fromEpochMillis(span.getEndEpochMillis()));
        if (span.getInput() != null) obs.put("input", span.getInput());
        if (span.getOutput() != null) obs.put("output", span.getOutput());
        if (span.getModelId() != null) obs.put("model", span.getModelId());
        if (generation && span.getUsage() != null) {
            LlmUsage usage = span.getUsage();
            Map<String, Object> usageMap = new LinkedHashMap<String, Object>();
            usageMap.put("input", usage.getInputTokens());
            usageMap.put("output", usage.getOutputTokens());
            usageMap.put("total", usage.getTotalTokens());
            obs.put("usage", usageMap);
        }
        if (span.getError() != null) { obs.put("level", "ERROR"); obs.put("statusMessage", span.getError()); }
        if (!span.getAttributes().isEmpty()) obs.put("metadata", new LinkedHashMap<String, Object>(span.getAttributes()));
        batch.add(event(UUID.randomUUID().toString(), generation ? "generation.create" : "span.create", now, obs));
        Map<String, Object> envelope = new LinkedHashMap<String, Object>();
        envelope.put("batch", batch);
        post(envelope);
    }
    private Map<String, Object> event(String id, String type, String ts, Map<String, Object> body) {
        Map<String, Object> e = new LinkedHashMap<String, Object>();
        e.put("id", id); e.put("type", type); e.put("timestamp", ts); e.put("body", body);
        return e;
    }
    private void post(Map<String, Object> envelope) {
        try {
            String json = mapper.writeValueAsString(envelope);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put("Authorization", authHeader);
            sender.post(baseUrl + "/api/public/ingestion", headers, json);
        } catch (Exception ex) { log.warn("langfuse export failed: {}", ex.getMessage()); }
    }
    private static boolean isGeneration(SpanKind kind) {
        return kind == SpanKind.LLM || kind == SpanKind.EMBEDDING || kind == SpanKind.RERANK;
    }
}
