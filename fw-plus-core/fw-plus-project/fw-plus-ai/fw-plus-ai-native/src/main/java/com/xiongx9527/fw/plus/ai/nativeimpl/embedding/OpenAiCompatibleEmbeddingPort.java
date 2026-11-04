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
        return embedWith(defaultModel, texts);
    }

    private List<double[]> embedWith(String model, List<String> texts) {
        if (texts == null || texts.isEmpty()) return new ArrayList<double[]>();
        try {
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("model", model);
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

    /**
     * 枚举向量模型：GET {@code /models}，按常见向量模型名关键字过滤。
     *
     * <p>OpenAI 兼容的 {@code /models} 不标注模型能力，只能按 id 关键字判断；一个都匹配不上时
     * 返回空列表（表示「无法可靠枚举」），而不是把对话模型误报成向量模型。</p>
     */
    @Override
    public List<ModelInfo> listModels() {
        Request.Builder rb = new Request.Builder().url(baseUrl + "/models").get();
        if (apiKey != null && !apiKey.isEmpty()) rb.addHeader("Authorization", "Bearer " + apiKey);
        List<ModelInfo> out = new ArrayList<ModelInfo>();
        try (Response r = client.newCall(rb.build()).execute()) {
            if (!r.isSuccessful()) { log.warn("embedding list models http {}", r.code()); return out; }
            JsonNode data = mapper.readTree(bodyOf(r)).get("data");
            if (data == null || !data.isArray()) return out;
            for (JsonNode item : data) {
                String id = item.path("id").asText(null);
                if (id == null || !looksLikeEmbedding(id)) continue;
                out.add(ModelInfo.builder().id(id).name(id)
                        .ownedBy(item.hasNonNull("owned_by") ? item.get("owned_by").asText() : null)
                        .type(ModelType.EMBEDDING)
                        .dimension(id.equals(defaultModel) ? dimension : 0)
                        .build());
            }
        } catch (IOException ex) {
            log.warn("embedding list models failed: {}", ex.getMessage());
        }
        return out;
    }

    /**
     * 模型校验：用该模型真实编码一条探针文本，能拿到非空向量即视为可用。
     *
     * <p>这样地址、鉴权、模型名三者同时被验证；顺带记录维度，供集合维度校验使用。</p>
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

    private static boolean looksLikeEmbedding(String id) {
        String lower = id.toLowerCase(java.util.Locale.ROOT);
        return lower.contains("embed") || lower.contains("bge") || lower.contains("gte")
                || lower.contains("m3e") || lower.contains("e5-") || lower.contains("jina")
                || lower.contains("voyage");
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
