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
        private final Map<String, String> headers;

        public Response(int code, String body, Map<String, String> headers) {
            this.code = code;
            this.body = body;
            this.headers = headers == null ? java.util.Collections.<String, String>emptyMap() : headers;
        }

        public int getCode() {
            return code;
        }

        public String getBody() {
            return body;
        }

        /** 取响应头（键名不区分大小写）；不存在时返回 null。 */
        public String getHeader(String name) {
            if (name == null) {
                return null;
            }
            String key = name.toLowerCase(java.util.Locale.ROOT);
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getKey().toLowerCase(java.util.Locale.ROOT).equals(key)) {
                    return entry.getValue();
                }
            }
            return null;
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
        Map<String, String> headerMap = new java.util.LinkedHashMap<String, String>();
        for (Map.Entry<String, java.util.List<String>> entry : conn.getHeaderFields().entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            headerMap.put(entry.getKey(), entry.getValue().get(0));
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
