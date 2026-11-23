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
 *   <li>{@link Style#COHERE} —— 遵循本项目对外统一规范（docs/rerank-unify-spec.md §3），兼容 Cohere / Jina：
 *       请求 {@code {model, query, documents, top_n, return_documents}}，端点 {@code {前缀}/v1/rerank}；
 *       响应 {@code {results:[{index, relevance_score}]}}，兼容 score/relevance、output.results 变体。</li>
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
        /** 本项目对外统一规范（§3）：{model,query,documents,top_n,return_documents} → {results:[{index,relevance_score}]}，端点 {前缀}/v1/rerank。 */
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
        this.style = style == null ? Style.COHERE : style;
        // endpoint 自动补 rerank 路径：调用方常只给服务根（如 http://host:19529），
        // 若路径里没有 /rerank 段则按 style 补齐（旧约定 COHERE→/v1/rerank、TEI→/rerank）。
        // 已给全路径（含 /rerank/v1、/rerank/tei、/v1/rerank、/rerank 等）则原样保留。
        String ep = endpoint.trim();
        while (ep.endsWith("/")) {
            ep = ep.substring(0, ep.length() - 1);
        }
        java.net.URI uri = java.net.URI.create(ep);
        String path = uri.getPath() == null ? "" : uri.getPath();
        if (!path.contains("/rerank")) {
            if (this.style == Style.TEI) {
                // TEI 官方规范：固定 /rerank
                ep = ep + "/rerank";
            } else if (ep.endsWith("/v1")) {
                // base 已带 /v1：补 /rerank 得 /v1/rerank（旧写法，避免 /v1/v1/rerank）
                ep = ep + "/rerank";
            } else {
                // COHERE 统一规范端点：缺前缀时补 /rerank/v1/rerank
                ep = ep + "/rerank/v1/rerank";
            }
        }
        this.endpoint = ep;
        this.apiKey = apiKey;
        this.model = model;
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
            JsonNode value;
            if (style == Style.TEI) {
                value = item.get("score");
            } else {
                // §3.5 厂商变体：relevance_score 为主，兼容 score / relevance 字段名
                value = item.get("relevance_score");
                if (value == null || !value.isNumber()) value = item.get("score");
                if (value == null || !value.isNumber()) value = item.get("relevance");
            }
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
            // §3 规范字段：是否回带原文（框架只要分数，固定 false 省带宽）
            body.put("return_documents", false);
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
            JsonNode results;
            if (style == Style.TEI) {
                results = root;
            } else {
                // §3 主形状 results[]；兼容 §3.5 厂商变体 output.results[]
                results = root.path("results");
                if (results == null || !results.isArray()) results = root.path("output").path("results");
            }
            return results != null && results.isArray() ? results : null;
        } catch (IOException ex) {
            log.warn("rerank failed, fallback to zero scores: {}", ex.getMessage());
            return null;
        }
    }
}
