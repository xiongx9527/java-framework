package com.xiongx9527.fw.plus.ai.nativeimpl.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.base.model.EmbeddingPort;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 真实原子能力：OpenAI 兼容向量化客户端（/v1/embeddings）。
 * 用 JDK HttpURLConnection 直连，不引入额外模型 SDK。实现 base 的 EmbeddingPort。
 */
public class OpenAiCompatibleEmbeddingPort implements EmbeddingPort {
    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleEmbeddingPort.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;
    private final String apiKey;
    private final String defaultModel;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private volatile int dimension;
    public OpenAiCompatibleEmbeddingPort(String baseUrl, String apiKey, String defaultModel) {
        this(baseUrl, apiKey, defaultModel, 8000, 30000);
    }
    public OpenAiCompatibleEmbeddingPort(String baseUrl, String apiKey, String defaultModel, int connectTimeoutMillis, int readTimeoutMillis) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) throw new IllegalArgumentException("embedding baseUrl is required");
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.apiKey = apiKey;
        this.defaultModel = defaultModel;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }
    @Override
    public double[] embed(String text) {
        if (text == null) return null;
        List<double[]> all = embedAll(Collections.singletonList(text));
        return all.isEmpty() ? null : all.get(0);
    }
    @Override
    public List<double[]> embedAll(List<String> texts) {
        if (texts == null || texts.isEmpty()) return new ArrayList<double[]>();
        try {
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("model", defaultModel);
            payload.put("input", new ArrayList<String>(texts));
            String body = mapper.writeValueAsString(payload);
            HttpURLConnection conn = open(baseUrl + "/embeddings");
            try (OutputStream os = conn.getOutputStream()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
            int code = conn.getResponseCode();
            String resp = code == 200 ? readAll(conn.getInputStream()) : readAll(conn.getErrorStream());
            if (code != 200) {
                log.warn("embedding http {}: {}", code, resp);
                return emptyList(texts.size());
            }
            JsonNode root = mapper.readTree(resp);
            JsonNode data = root.get("data");
            List<double[]> out = new ArrayList<double[]>();
            if (data != null && data.isArray()) {
                for (JsonNode item : data) {
                    out.add(toVector(item.get("embedding")));
                }
            }
            if (dimension == 0 && !out.isEmpty()) dimension = out.get(0).length;
            while (out.size() < texts.size()) out.add(null);
            return out;
        } catch (Exception ex) {
            log.warn("embedding failed: {}", ex.getMessage());
            return emptyList(texts.size());
        }
    }
    @Override
    public int dimension() {
        return dimension;
    }
    private HttpURLConnection open(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(connectTimeoutMillis);
        conn.setReadTimeout(readTimeoutMillis);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        if (apiKey != null && !apiKey.isEmpty()) conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        return conn;
    }
    private double[] toVector(JsonNode node) {
        if (node == null || !node.isArray()) return new double[0];
        double[] v = new double[node.size()];
        for (int i = 0; i < node.size(); i++) v[i] = node.get(i).asDouble();
        return v;
    }
    private static String readAll(InputStream in) throws IOException {
        if (in == null) return "";
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) sb.append((char) c);
        return sb.toString();
    }
    private static List<double[]> emptyList(int n) {
        List<double[]> out = new ArrayList<double[]>();
        for (int i = 0; i < n; i++) out.add(null);
        return out;
    }
}
