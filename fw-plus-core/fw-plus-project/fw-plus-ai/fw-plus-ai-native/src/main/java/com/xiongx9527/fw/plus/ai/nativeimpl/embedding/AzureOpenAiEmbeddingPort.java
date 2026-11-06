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
 * 真实原子能力：Azure OpenAI 向量化客户端。
 * 用 okhttp3 直连，不引入额外模型 SDK。实现 base 的 EmbeddingPort。
 *
 * <p>Azure 与标准 OpenAI 不同：部署名（即模型名）出现在路径 {@code /openai/deployments/{deployment}/embeddings}，
 * 且必须带 {@code api-version} 查询参数；鉴权走 {@code api-key} 请求头（而非 {@code Authorization: Bearer}）。
 * api-version 可在 {@link EmbeddingModelConfig#getProperties()} 以 {@code apiVersion} 覆盖，默认 {@code 2023-05-15}。</p>
 */
public class AzureOpenAiEmbeddingPort implements EmbeddingPort {
    private static final Logger log = LoggerFactory.getLogger(AzureOpenAiEmbeddingPort.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String DEFAULT_API_VERSION = "2023-05-15";
    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient client;
    private final String baseUrl;
    private final String apiKey;
    private final String defaultDeployment;
    private final String apiVersion;
    private volatile int dimension;

    public AzureOpenAiEmbeddingPort(String baseUrl, String apiKey, String defaultDeployment) {
        this(baseUrl, apiKey, defaultDeployment, null, null, null);
    }

    public AzureOpenAiEmbeddingPort(String baseUrl, String apiKey, String defaultDeployment,
            Integer connectTimeoutMillis, Integer readTimeoutMillis) {
        this(baseUrl, apiKey, defaultDeployment, connectTimeoutMillis, readTimeoutMillis, null);
    }

    public AzureOpenAiEmbeddingPort(String baseUrl, String apiKey, String defaultDeployment,
            Integer connectTimeoutMillis, Integer readTimeoutMillis, Map<String, Object> properties) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) throw new IllegalArgumentException("embedding baseUrl is required");
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.apiKey = apiKey;
        this.defaultDeployment = defaultDeployment;
        this.apiVersion = properties != null && properties.get("apiVersion") instanceof String
                ? (String) properties.get("apiVersion") : DEFAULT_API_VERSION;
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
        return embedWith(defaultDeployment, texts);
    }

    private List<double[]> embedWith(String deployment, List<String> texts) {
        if (texts == null || texts.isEmpty()) return new ArrayList<double[]>();
        try {
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("input", new ArrayList<String>(texts));
            payload.put("model", deployment);
            String url = baseUrl + "/openai/deployments/" + deployment + "/embeddings?api-version=" + apiVersion;
            Request.Builder rb = new Request.Builder().url(url)
                    .post(RequestBody.create(JSON, mapper.writeValueAsString(payload)));
            if (apiKey != null && !apiKey.isEmpty()) rb.addHeader("api-key", apiKey);
            try (Response r = client.newCall(rb.build()).execute()) {
                String resp = bodyOf(r);
                if (!r.isSuccessful()) { log.warn("azure embedding http {}: {}", r.code(), resp); return emptyList(texts.size()); }
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
            log.warn("azure embedding failed: {}", ex.getMessage());
            return emptyList(texts.size());
        }
    }

    @Override
    public int dimension() { return dimension; }

    @Override
    public ModelValidation validateModel(String model) {
        if (model == null || model.trim().isEmpty()) {
            return ModelValidation.invalid(model, "deployment name is required", 0L);
        }
        long start = System.currentTimeMillis();
        List<double[]> vectors = embedWith(model, Collections.singletonList("ping"));
        long elapsed = System.currentTimeMillis() - start;
        double[] vector = vectors == null || vectors.isEmpty() ? null : vectors.get(0);
        if (vector == null || vector.length == 0) {
            return ModelValidation.invalid(model, "embedding probe returned no vector", elapsed);
        }
        if (model.equals(defaultDeployment)) dimension = vector.length;
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
