package com.xiongx9527.fw.plus.ai.nativeimpl.http;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 轻量 HTTP/JSON 传输：native 各后端（Qdrant / Milvus / Neo4j / rerank 等）共用。
 * 基于 okhttp3 实现（与 pom 注释「HTTP 走 okhttp3」一致），替代原 JdkHttp 的 HttpURLConnection 实现；
 * 对外 API 与原 JdkHttp 保持兼容：调用方负责序列化与报文解析。
 */
public final class OkHttp {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private OkHttp() {
    }

    /** 一次请求的响应快照。 */
    public static final class Response {
        private final int code;
        private final String body;
        private final Map<String, List<String>> headers;

        public Response(int code, String body, Map<String, List<String>> headers) {
            this.code = code;
            this.body = body;
            this.headers = headers == null
                    ? Collections.<String, List<String>>emptyMap() : headers;
        }

        public int getCode() {
            return code;
        }

        public String getBody() {
            return body;
        }

        /** 取响应头的首个值（键名不区分大小写）；不存在时返回 null。 */
        public String getHeader(String name) {
            List<String> values = getHeaders(name);
            return values.isEmpty() ? null : values.get(0);
        }

        /**
         * 取响应头的全部值（键名不区分大小写）。
         * 一个响应可能出现多个同名头，典型如多次 Set-Cookie；调用方需要逐条解析时使用本方法。
         */
        public List<String> getHeaders(String name) {
            if (name == null) {
                return Collections.emptyList();
            }
            String key = name.toLowerCase(Locale.ROOT);
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (entry.getKey().toLowerCase(Locale.ROOT).equals(key)) {
                    return entry.getValue();
                }
            }
            return Collections.emptyList();
        }
    }

    public static Response send(String method, String url, byte[] body,
                                Map<String, String> headers,
                                int connectTimeoutMillis, int readTimeoutMillis) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(connectTimeoutMillis, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
                .build();
        Request.Builder rb = new Request.Builder().url(url)
                .method(method, body != null ? RequestBody.create(JSON, body) : null);
        rb.header("Content-Type", "application/json; charset=UTF-8");
        rb.header("Accept", "application/json");
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                rb.header(entry.getKey(), entry.getValue());
            }
        }
        try (okhttp3.Response resp = client.newCall(rb.build()).execute()) {
            int code = resp.code();
            String respBody = resp.body() != null ? resp.body().string() : "";
            Map<String, List<String>> headerMap = new LinkedHashMap<String, List<String>>();
            Headers h = resp.headers();
            for (String name : h.names()) {
                headerMap.put(name, new ArrayList<String>(h.values(name)));
            }
            return new Response(code, respBody, headerMap);
        }
    }

    /** 4xx/5xx 视为失败，抛出带响应体的 IOException。 */
    public static Response requireOk(Response response) throws IOException {
        if (response.getCode() >= 400) {
            throw new IOException("http " + response.getCode() + ": " + response.getBody());
        }
        return response;
    }

    public static String basicAuth(String username, String password) {
        String raw = (username == null ? "" : username) + ":" + (password == null ? "" : password);
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
