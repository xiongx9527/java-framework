package com.xiongx9527.fw.plus.ai.base.trace;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 基于 HTTP 的 {@link TracePort} 基类：把 Span 翻译成各观测平台的报文并 POST 出去。
 * 导出失败只记日志，绝不抛入调用链（避免影响模型调用）。
 *
 * <p>子类只需实现 {@link #toPayload(Span)} 与 {@link #ingestPath()}：前者把中性 Span 转成平台报文，
 * 后者给出 ingest 的相对路径；鉴权头等由构造时传入。放在 base 是通用传输能力，
 * 具体平台适配器（Langfuse / LangSmith）在 support 实现。</p>
 */
public abstract class HttpTracePort implements TracePort {

    protected final Logger log = Logger.getLogger(getClass().getName());
    protected final ObjectMapper mapper = new ObjectMapper();

    private final String baseUrl;
    private final Map<String, String> defaultHeaders;
    private final int connectTimeout;
    private final int readTimeout;

    protected HttpTracePort(String baseUrl, Map<String, String> defaultHeaders,
                           int connectTimeout, int readTimeout) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.defaultHeaders = defaultHeaders == null ? new HashMap<String, String>() : defaultHeaders;
        this.connectTimeout = connectTimeout > 0 ? connectTimeout : 3000;
        this.readTimeout = readTimeout > 0 ? readTimeout : 5000;
    }

    protected String baseUrl() {
        return baseUrl;
    }

    /** 子类把中性 Span 翻译成平台报文。 */
    abstract Object toPayload(Span span);

    /** 子类给出 ingest 的相对路径（如 Langfuse 的 /api/public/ingestion）。 */
    abstract String ingestPath();

    @Override
    public Span.Builder start(String name, SpanKind kind) {
        String traceId = TraceContext.traceId();
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
        }
        String parent = TraceContext.spanId();
        Span.Builder b = Span.start(traceId, name, kind);
        if (parent != null) {
            b.parent(parent);
        }
        return b;
    }

    @Override
    public void end(Span.Builder builder) {
        try {
            Span span = builder.build();
            Object payload = toPayload(span);
            post(ingestPath(), payload);
        } catch (Exception e) {
            log.log(Level.WARNING, "trace export failed: " + e.getMessage());
        }
    }

    @Override
    public void flush() {
        // 即时发送，无缓冲；子类如需批量可重写此方法。
    }

    protected void post(String path, Object body) throws Exception {
        byte[] bytes = mapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
        HttpURLConnection con = (HttpURLConnection) new URL(baseUrl + path).openConnection();
        con.setRequestMethod("POST");
        con.setConnectTimeout(connectTimeout);
        con.setReadTimeout(readTimeout);
        con.setDoOutput(true);
        con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        for (Map.Entry<String, String> e : defaultHeaders.entrySet()) {
            con.setRequestProperty(e.getKey(), e.getValue());
        }
        try (OutputStream os = con.getOutputStream()) {
            os.write(bytes);
        }
        int code = con.getResponseCode();
        if (code >= 400) {
            log.log(Level.WARNING, "trace export http " + code + " for " + path);
        }
        con.disconnect();
    }

    /** epoch 毫秒 -> ISO-8601 UTC（如 2026-11-10T09:00:00.000Z）。 */
    protected static String isoUtc(long epochMillis) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date(epochMillis));
    }

    private static String trimTrailingSlash(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        while (t.endsWith("/")) {
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }
}
