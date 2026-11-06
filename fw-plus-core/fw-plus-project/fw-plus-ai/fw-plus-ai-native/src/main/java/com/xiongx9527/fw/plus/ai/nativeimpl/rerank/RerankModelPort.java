package com.xiongx9527.fw.plus.ai.nativeimpl.rerank;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.base.model.ModelInfo;
import com.xiongx9527.fw.plus.ai.base.model.ModelValidation;
import com.xiongx9527.fw.plus.ai.base.model.RerankPort;
import com.xiongx9527.fw.plus.ai.nativeimpl.http.OkHttp;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 真实原子能力：重排打分模型（RerankPort）。按 query 给候选文本重新打相关度分，返回与输入等长、0~1 的分数列表。
 *
 * <p>两家厂商的 REST 报文不同，用 {@link Style} 切换， endpoint 由调用方给全：</p>
 * <ul>
 *   <li>{@link Style#TEI} —— HuggingFace text-embeddings-inference 的 {@code /rerank}：
 *       请求 {@code {query, texts:[...]}}，响应为按分数降序的 {@code [{index, score}]}。</li>
 *   <li>{@link Style#COHERE} —— Cohere / Jina 风格（{@code /v1/rerank}、{@code /v2/rerank}）：
 *       请求 {@code {model, query, documents:[...], top_n}}，响应 {@code {results:[{index, relevance_score}]}}。</li>
 * </ul>
 *
 * <p>两者都返回「下标对应输入 documents」，实现把降序结果回填成与输入顺序对齐的分数数组；
 * 缺失下标按 0 处理。失败不抛异常，返回等长的 0 分数组，让上层检索链路可降级继续。</p>
 *
 * <p>其它私有重排协议直接 implements {@link RerankPort} 即可，不必改本类。</p>
 */
public class RerankModelPort implements RerankPort {
    private static final Logger log = LoggerFactory.getLogger(RerankModelPort.class);

    public enum Style {
        TEI,
        COHERE
    }

    private final ObjectMapper mapper = new ObjectMapper();
    private final String endpoint;
    private final String apiKey;
    private final String model;
    private final Style style;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;

    public RerankModelPort(String endpoint, Style style) {
        this(endpoint, null, null, style, 5000, 10000);
    }

    public RerankModelPort(String endpoint, String apiKey, String model, Style style) {
        this(endpoint, apiKey, model, style, 5000, 10000);
    }

    public RerankModelPort(String endpoint, String apiKey, String model, Style style,
                           int connectTimeoutMillis, int readTimeoutMillis) {
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("rerank endpoint is required");
        }
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.model = model;
        this.style = style == null ? Style.COHERE : style;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    @Override
    public List<Double> score(String query, List<String> documents) {
        return scoreWith(model, query, documents);
    }

    @Override
    public List<Double> score(String model, String query, List<String> documents) {
        return scoreWith(model, query, documents);
    }

    private List<Double> scoreWith(String model, String query, List<String> documents) {
        if (documents == null || documents.isEmpty()) {
            return new ArrayList<Double>();
        }
        List<Double> scores = new ArrayList<Double>(Collections.nCopies(documents.size(), 0.0));
        JsonNode results = request(model, query, documents);
        if (results == null) {
            return scores;
        }
        for (JsonNode item : results) {
            int index = item.path("index").asInt(-1);
            if (index < 0 || index >= scores.size()) {
                continue;
            }
            JsonNode value = style == Style.TEI
                    ? item.get("score") : item.get("relevance_score");
            if (value != null && value.isNumber()) {
                scores.set(index, value.asDouble());
            }
        }
        return scores;
    }

    /**
     * TEI 与 Cohere 重排 API 均未提供可用于此适配器的模型目录端点；
     * 当前配置的模型不等于服务端可枚举目录，因此返回空列表。
     */
    @Override
    public List<ModelInfo> listModels() {
        return Collections.emptyList();
    }

    /**
     * 模型校验：用该模型真实打一次分，返回结果非空即视为可用（地址、鉴权、模型名同时被验证）。
     */
    @Override
    public ModelValidation validateModel(String model) {
        if (model == null || model.trim().isEmpty()) {
            return ModelValidation.invalid(model, "model id is required", 0L);
        }
        long start = System.currentTimeMillis();
        JsonNode results = request(model, "ping", Collections.singletonList("ping"));
        long elapsed = System.currentTimeMillis() - start;
        if (results == null || results.size() == 0) {
            return ModelValidation.invalid(model, "rerank probe returned no score", elapsed);
        }
        return ModelValidation.valid(model, elapsed);
    }

    /** 发起一次打分请求，成功返回 results 数组，失败返回 {@code null}（由调用方决定降级方式）。 */
    private JsonNode request(String model, String query, List<String> documents) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("query", query == null ? "" : query);
        if (style == Style.TEI) {
            body.put("texts", documents);
            body.put("return_text", false);
        } else {
            if (model != null && !model.isEmpty()) {
                body.put("model", model);
            }
            body.put("documents", documents);
            body.put("top_n", documents.size());
        }
        try {
            byte[] data = mapper.writeValueAsBytes(body);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            if (apiKey != null && !apiKey.isEmpty()) {
                headers.put("Authorization", "Bearer " + apiKey);
            }
            OkHttp.Response response = OkHttp.requireOk(
                    OkHttp.send("POST", endpoint, data, headers, connectTimeoutMillis, readTimeoutMillis));
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode results = style == Style.TEI ? root : root.path("results");
            return results != null && results.isArray() ? results : null;
        } catch (IOException ex) {
            log.warn("rerank failed, fallback to zero scores: {}", ex.getMessage());
            return null;
        }
    }
}
