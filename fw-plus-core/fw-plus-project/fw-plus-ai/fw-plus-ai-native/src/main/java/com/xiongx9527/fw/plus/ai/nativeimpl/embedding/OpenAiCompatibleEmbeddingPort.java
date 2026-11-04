package com.xiongx9527.fw.plus.ai.nativeimpl.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.base.model.EmbeddingPort;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 真实原子能力：OpenAI 兼容向量化客户端（/v1/embeddings）。
 * 用 okhttp3 直连，不引入额外模型 SDK。实现 base 的 EmbeddingPort。
 * 覆盖 OpenAI、Ollama(embed)、Qwen(DashScope 兼容端点) 等同族协议。
 */
public class OpenAiCompatibleEmbeddingPort implements EmbeddingPort {
    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleEmbeddingPort.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient client;
    private final String baseUrl;
    private final String apiKey;
    private final String defaultModel;
    private volatile int dimension;
    public OpenAiCompatibleEmbeddingPort(String baseUrl, String apiKey, String defaultModel) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) throw new IllegalArgumentException("embedding baseUrl is required");
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.apiKey = apiKey;
        this.defaultModel = defaultModel;
        this.client = new OkHttpClient.Builder().connectTimeout(8_000, TimeUnit.MILLISECONDS).readTimeout(30_000, TimeUnit.MILLISECONDS).build();
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
            Request.Builder rb = new Request.Builder().url(baseUrl + "/embeddings").post(RequestBody.create(JSON, mapper.writeValueAsString(payload)));
            if (apiKey != null && !apiKey.isEmpty()) rb.addHeader("Authorization", "Bearer " + apiKey);
            try (Response r = client.newCall(rb.build()).execute()) {
                String resp = bodyOf(r);
                if (!r.isSuccessful()) { log.warn("embedding http {}: {}", r.code(), resp); return emptyList(texts.size()); }
                JsonNode root = mapper.readTree(resp);
                JsonNode data = root.get("data");
                List<double[]> out = new ArrayList<double[]>();
                if (data != null && data.isArray()) {
                    for (JsonNode item : data) out.add(toVector(item.get("embedding")));
                }
                if (dimension == 0 && !out.isEmpty()) dimension = out.get(0).length;
                while (out.size() < texts.size()) out.add(null);
                return out;
            }
        } catch (Exception ex) {
            log.warn("embedding failed: {}", ex.getMessage());
            return emptyList(texts.size());
        }
    }
    @Override
    public int dimension() { return dimension; }
    private static String bodyOf(Response r) throws IOException {
        try (ResponseBody b = r.body()) { return b == null ? "" : b.string(); }
    }
    private double[] toVector(JsonNode node) {
        if (node == null || !node.isArray()) return new double[0];
        double[] v = new double[node.size()];
        for (int i = 0; i < node.size(); i++) v[i] = node.get(i).asDouble();
        return v;
    }
    private static List<double[]> emptyList(int n) {
        List<double[]> out = new ArrayList<double[]>();
        for (int i = 0; i < n; i++) out.add(null);
        return out;
    }
}
