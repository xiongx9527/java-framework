package com.xiongx9527.fw.plus.ai.nativeimpl.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * 轻量 HTTP/JSON 传输：native 各后端（Qdrant / Milvus / Neo4j / rerank）共用。
 * 只依赖 JDK HttpURLConnection，不引入大型 SDK；调用方负责序列化与报文解析。
 */
public final class JdkHttp {

    private JdkHttp() {
    }

    /** 一次请求的响应快照。 */
    public static final class Response {
        private final int code;
        private final String body;
        private final Map<String, java.util.List<String>> headers;

        public Response(int code, String body, Map<String, java.util.List<String>> headers) {
            this.code = code;
            this.body = body;
            this.headers = headers == null
                    ? java.util.Collections.<String, java.util.List<String>>emptyMap() : headers;
        }

        public int getCode() {
            return code;
        }

        public String getBody() {
            return body;
        }

        /** 取响应头的首个值（键名不区分大小写）；不存在时返回 null。 */
        public String getHeader(String name) {
            java.util.List<String> values = getHeaders(name);
            return values.isEmpty() ? null : values.get(0);
        }

        /**
         * 取响应头的全部值（键名不区分大小写）。
         * 一个响应可能出现多个同名头，典型如多次 Set-Cookie；调用方需要逐条解析时使用本方法。
         */
        public java.util.List<String> getHeaders(String name) {
            if (name == null) {
                return java.util.Collections.emptyList();
            }
            String key = name.toLowerCase(java.util.Locale.ROOT);
            for (Map.Entry<String, java.util.List<String>> entry : headers.entrySet()) {
                if (entry.getKey().toLowerCase(java.util.Locale.ROOT).equals(key)) {
                    return entry.getValue();
                }
            }
            return java.util.Collections.emptyList();
        }
    }

    public static Response send(String method, String url, byte[] body,
                                Map<String, String> headers,
                                int connectTimeoutMillis, int readTimeoutMillis) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(connectTimeoutMillis);
        conn.setReadTimeout(readTimeoutMillis);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        if (body != null) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }
        }
        int code = conn.getResponseCode();
        String resp = readAll(code >= 400 ? conn.getErrorStream() : conn.getInputStream());
        Map<String, java.util.List<String>> headerMap =
                new java.util.LinkedHashMap<String, java.util.List<String>>();
        for (Map.Entry<String, java.util.List<String>> entry : conn.getHeaderFields().entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            headerMap.put(entry.getKey(),
                    java.util.Collections.unmodifiableList(new java.util.ArrayList<String>(entry.getValue())));
        }
        return new Response(code, resp, headerMap);
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

    private static String readAll(InputStream in) throws IOException {
        if (in == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) {
            sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
