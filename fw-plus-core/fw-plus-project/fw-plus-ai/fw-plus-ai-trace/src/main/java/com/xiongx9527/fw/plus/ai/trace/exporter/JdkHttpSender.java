package com.xiongx9527.fw.plus.ai.trace.exporter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** 基于 JDK HttpURLConnection 的 HttpSender 实现，不引入额外 HTTP 依赖。 */
public final class JdkHttpSender implements HttpSender {
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    public JdkHttpSender() { this(5000, 10000); }
    public JdkHttpSender(int connectTimeoutMillis, int readTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }
    @Override
    public void post(String url, Map<String, String> headers, String body) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(connectTimeoutMillis);
        conn.setReadTimeout(readTimeoutMillis);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
        }
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) { os.write(data); }
        int code = conn.getResponseCode();
        InputStream in = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (in != null) { byte[] buffer = new byte[1024]; while (in.read(buffer) != -1) {} in.close(); }
        if (code >= 400) { throw new IOException("trace export failed: HTTP " + code + " -> " + url); }
    }
}
