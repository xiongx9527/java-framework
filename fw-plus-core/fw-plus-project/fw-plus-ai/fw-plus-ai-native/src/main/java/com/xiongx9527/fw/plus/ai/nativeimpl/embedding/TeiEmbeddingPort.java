package com.xiongx9527.fw.plus.ai.nativeimpl.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.base.model.EmbeddingPort;
import com.xiongx9527.fw.plus.ai.base.model.ModelInfo;
import com.xiongx9527.fw.plus.ai.base.model.ModelType;
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
 * 真实原子能力：HuggingFace Text Embeddings Inference（TEI）向量化客户端（/embed）。
 * 用 okhttp3 直连，不引入额外模型 SDK。实现 base 的 EmbeddingPort。
 *
 * <p>TEI 原生请求体为 {@code {"inputs":[...],"normalize":false}}，响应为 {@code {"embeddings":[[...]]}}；
 * 模型元信息经 {@code /info} 获取。该端口覆盖自托管的 HF 向量服务（含 HuggingFace 厂商路由）。</p>
 */
public class TeiEmbeddingPort implements EmbeddingPort {
    private static final Logger log = LoggerFactory.getLogger(TeiEmbeddingPort.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient client;
    private final String baseUrl;
    private final String apiKey;
    private final String defaultModel;
    private volatile int dimension;

    public TeiEmbeddingPort(String baseUrl, String apiKey, String defaultModel) {
        this(baseUrl, apiKey, defaultModel, null, null);
    }

    public TeiEmbeddingPort(String baseUrl, String apiKey, String defaultModel,
            Integer connectTimeoutMillis, Integer readTimeoutMillis) {
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
        if (texts == null || texts.isEmpty()) return new ArrayList<double[]>();
        try {
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("inputs", new ArrayList<String>(texts));
            payload.put("normalize", false);
            Request.Builder rb = new Request.Builder().url(baseUrl + "/embed")
                    .post(RequestBody.create(JSON, mapper.writeValueAsString(payload)));
            if (apiKey != null && !apiKey.isEmpty()) rb.addHeader("Authorization", "Bearer " + apiKey);
            try (Response r = client.newCall(rb.build()).execute()) {
                String resp = bodyOf(r);
                if (!r.isSuccessful()) { log.warn("tei embedding http {}: {}", r.code(), resp); return emptyList(texts.size()); }
                JsonNode root = mapper.readTree(resp);
                JsonNode embeddings = root.path("embeddings");
                List<double[]> out = new ArrayList<double[]>();
                if (embeddings.isArray()) {
                    for (JsonNode item : embeddings) out.add(toVector(item));
                }
                if (dimension == 0 && !out.isEmpty()) dimension = out.get(0).length;
                while (out.size() < texts.size()) out.add(null);
                return out;
            }
        } catch (Exception ex) {
            log.warn("tei embedding failed: {}", ex.getMessage());
            return emptyList(texts.size());
        }
    }

    @Override
    public int dimension() { return dimension; }

    /**
     * 枚举模型：经 TEI {@code /info} 读取模型标识；该端点不可用（如未开启）时返回空列表。
     */
    @Override
    public List<ModelInfo> listModels() {
        List<ModelInfo> out = new ArrayList<ModelInfo>();
        String infoBaseUrl = baseUrl.endsWith("/v1")
                ? baseUrl.substring(0, baseUrl.length() - 3) : baseUrl;
        Request.Builder rb = new Request.Builder().url(infoBaseUrl + "/info").get();
        if (apiKey != null && !apiKey.isEmpty()) rb.addHeader("Authorization", "Bearer " + apiKey);
        try (Response r = client.newCall(rb.build()).execute()) {
            if (!r.isSuccessful()) { log.warn("tei info http {}", r.code()); return out; }
            JsonNode info = mapper.readTree(bodyOf(r));
            if (!info.path("model_type").has("embedding")) return out;
            String id = info.path("model_id").asText(null);
            if (id == null || id.trim().isEmpty()) id = info.path("served_model_name").asText(null);
            if (id != null && !id.trim().isEmpty()) {
                out.add(ModelInfo.builder().id(id).name(id).type(ModelType.EMBEDDING)
                        .dimension(id.equals(defaultModel) ? dimension : 0).build());
            }
        } catch (IOException ex) {
            log.warn("tei info failed: {}", ex.getMessage());
        }
        return out;
    }

    @Override
    public ModelValidation validateModel(String model) {
        if (model == null || model.trim().isEmpty()) {
            return ModelValidation.invalid(model, "model id is required", 0L);
        }
        long start = System.currentTimeMillis();
        List<double[]> vectors = embedAll(Collections.singletonList("ping"));
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
