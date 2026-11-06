package com.xiongx9527.fw.plus.ai.nativeimpl.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.base.model.EmbeddingPort;
import com.xiongx9527.fw.plus.ai.base.model.ModelValidation;
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
 * 真实原子能力：Cohere 向量化客户端（/v1/embed）。
 * 用 okhttp3 直连，不引入额外模型 SDK。实现 base 的 EmbeddingPort。
 *
 * <p>Cohere v3+ 模型要求请求携带 {@code input_type}；本端口默认 {@code search_document}，
 * 可在 {@link EmbeddingModelConfig#getProperties()} 以 {@code inputType} 覆盖（如 {@code search_query}）。
 * 响应取 {@code embeddings.float}（请求显式声明 {@code embedding_types=["float"]}）。</p>
 */
public class CohereEmbeddingPort implements EmbeddingPort {
    private static final Logger log = LoggerFactory.getLogger(CohereEmbeddingPort.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient client;
    private final String baseUrl;
    private final String apiKey;
    private final String defaultModel;
    private volatile int dimension;

    public CohereEmbeddingPort(String baseUrl, String apiKey, String defaultModel) {
        this(baseUrl, apiKey, defaultModel, null, null, null);
    }

    public CohereEmbeddingPort(String baseUrl, String apiKey, String defaultModel,
            Integer connectTimeoutMillis, Integer readTimeoutMillis) {
        this(baseUrl, apiKey, defaultModel, connectTimeoutMillis, readTimeoutMillis, null);
    }

    public CohereEmbeddingPort(String baseUrl, String apiKey, String defaultModel,
            Integer connectTimeoutMillis, Integer readTimeoutMillis, Map<String, Object> properties) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) throw new IllegalArgumentException("embedding baseUrl is required");
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.apiKey = apiKey;
        this.defaultModel = defaultModel;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(connectTimeoutMillis != null ? connectTimeoutMillis : 8_000L, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutMillis != null ? readTimeoutMillis : 30_000L, TimeUnit.MILLISECONDS)
                .build();
    }

    @Override
    public double[] embed(String text) {
        if (text == null) return null;
        List<double[]> all = embedAll(Collections.singletonList(text));
        return all.isEmpty() ? null : all.get(0);
    }

    @Override
    public List<double[]> embedAll(List<String> texts) {
        return embedWith(defaultModel, texts);
    }

    private List<double[]> embedWith(String model, List<String> texts) {
        if (texts == null || texts.isEmpty()) return new ArrayList<double[]>();
        try {
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("model", model);
            payload.put("texts", new ArrayList<String>(texts));
            payload.put("input_type", "search_document");
            payload.put("embedding_types", Collections.singletonList("float"));
            Request.Builder rb = new Request.Builder().url(baseUrl + "/v1/embed")
                    .post(RequestBody.create(JSON, mapper.writeValueAsString(payload)));
            if (apiKey != null && !apiKey.isEmpty()) rb.addHeader("Authorization", "Bearer " + apiKey);
            try (Response r = client.newCall(rb.build()).execute()) {
                String resp = bodyOf(r);
                if (!r.isSuccessful()) { log.warn("cohere embedding http {}: {}", r.code(), resp); return emptyList(texts.size()); }
                JsonNode root = mapper.readTree(resp);
                JsonNode embeddings = root.path("embeddings");
                List<double[]> out = new ArrayList<double[]>();
                if (embeddings.has("float") && embeddings.get("float").isArray()) {
                    for (JsonNode item : embeddings.get("float")) out.add(toVector(item));
                } else if (embeddings.isArray()) {
                    // 部分端点未声明 embedding_types 时直接返回二维数组
                    for (JsonNode item : embeddings) out.add(toVector(item));
                }
                if (dimension == 0 && !out.isEmpty()) dimension = out.get(0).length;
                while (out.size() < texts.size()) out.add(null);
                return out;
            }
        } catch (Exception ex) {
            log.warn("cohere embedding failed: {}", ex.getMessage());
            return emptyList(texts.size());
        }
    }

    @Override
    public int dimension() { return dimension; }

    /**
     * 模型校验：用该模型真实编码一条探针文本，能拿到非空向量即视为可用。
     */
    @Override
    public ModelValidation validateModel(String model) {
        if (model == null || model.trim().isEmpty()) {
            return ModelValidation.invalid(model, "model id is required", 0L);
        }
        long start = System.currentTimeMillis();
        List<double[]> vectors = embedWith(model, Collections.singletonList("ping"));
        long elapsed = System.currentTimeMillis() - start;
        double[] vector = vectors == null || vectors.isEmpty() ? null : vectors.get(0);
        if (vector == null || vector.length == 0) {
            return ModelValidation.invalid(model, "embedding probe returned no vector", elapsed);
        }
        if (model.equals(defaultModel)) dimension = vector.length;
        return ModelValidation.valid(model, elapsed);
    }

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
