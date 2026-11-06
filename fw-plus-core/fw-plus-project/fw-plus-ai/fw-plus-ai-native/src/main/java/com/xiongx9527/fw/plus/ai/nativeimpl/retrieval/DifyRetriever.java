package com.xiongx9527.fw.plus.ai.nativeimpl.retrieval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.base.retrieval.DocumentRetriever;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retrieved;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery;
import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Dify 知识库检索 API 的文档检索器（native 实现）。
 *
 * <p>委托 Dify {@code POST /datasets/{dataset_id}/retrieve}，把召回的 segment 映射为框架统一的
 * {@link Document} / {@link Retrieved}，可直接作为 {@code RetrievalStrategies} 语义 / 混合链路里的
 * 检索器使用。</p>
 *
 * <p>支持跨多知识库 fan-out 检索：依次查询 {@link DifyRetrieverConfig#resolvedDatasetIds()} 中的每个
 * 知识库，再按分数降序融合、阈值过滤、topK 截断。Dify 的 score 与向量余弦同量纲（0~1），因此
 * 与词法检索混合加权无需额外归一化。</p>
 */
public class DifyRetriever implements DocumentRetriever {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DifyRetrieverConfig config;

    private final OkHttpClient httpClient;

    public DifyRetriever(DifyRetrieverConfig config) {
        if (config == null || !config.configured()) {
            throw new IllegalArgumentException(
                    "DifyRetriever requires server, apiKey and at least one datasetId");
        }
        this.config = config;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(config.getConnectTimeoutMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(config.getReadTimeoutMillis(), TimeUnit.MILLISECONDS)
                .writeTimeout(config.getReadTimeoutMillis(), TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    @Override
    public List<Retrieved<Document>> retrieve(RetrievalQuery query) {
        List<Retrieved<Document>> result = new ArrayList<Retrieved<Document>>();
        if (query == null || query.getText() == null || query.getText().trim().isEmpty()) {
            return result;
        }
        String jsonBody;
        try {
            jsonBody = MAPPER.writeValueAsString(config.requestBody(query.getText()));
        } catch (IOException e) {
            throw new DifyRetrievalException("failed to serialize Dify retrieve request", e);
        }
        for (String datasetId : config.resolvedDatasetIds()) {
            result.addAll(retrieveOne(datasetId, jsonBody));
        }
        // 多库融合：按分数降序
        result.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        // 阈值过滤（对齐 Dify / ThresholdRetriever：<=0 不过滤）
        double threshold = query.getScoreThreshold();
        if (threshold > 0.0) {
            List<Retrieved<Document>> filtered = new ArrayList<Retrieved<Document>>();
            for (Retrieved<Document> r : result) {
                if (r.getScore() >= threshold) {
                    filtered.add(r);
                }
            }
            result = filtered;
        }
        // topK 截断
        int topK = query.getTopK();
        if (topK > 0 && result.size() > topK) {
            result = new ArrayList<Retrieved<Document>>(result.subList(0, topK));
        }
        return result;
    }

    private List<Retrieved<Document>> retrieveOne(String datasetId, String jsonBody) {
        String url = buildUrl(config.getServer(), datasetId);
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + config.getApiKey())
                .post(RequestBody.create(JSON, jsonBody))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : null;
            if (!response.isSuccessful() || body == null) {
                throw new DifyRetrievalException(
                        "Dify retrieve failed: HTTP " + response.code() + " for dataset " + datasetId);
            }
            return parse(body, datasetId);
        } catch (IOException e) {
            throw new DifyRetrievalException("Dify retrieve request error for dataset " + datasetId, e);
        }
    }

    private List<Retrieved<Document>> parse(String json, String datasetId) {
        List<Retrieved<Document>> hits = new ArrayList<Retrieved<Document>>();
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode records = root.path("records");
            if (!records.isArray()) {
                return hits;
            }
            int index = 0;
            for (JsonNode rec : records) {
                JsonNode segment = rec.path("segment");
                double score = rec.path("score").asDouble(0.0);
                String id = segment.path("id").asText();
                if (id == null || id.isEmpty()) {
                    id = datasetId + "-" + index;
                }
                String content = segment.path("content").asText();
                if (content == null || content.isEmpty()) {
                    // Q&A 模式下正文在 answer 字段
                    content = segment.path("answer").asText();
                }
                Map<String, Object> metadata = new LinkedHashMap<String, Object>();
                if (!segment.path("document_id").isMissingNode()) {
                    metadata.put("document_id", segment.path("document_id").asText());
                }
                if (!segment.path("position").isMissingNode()) {
                    metadata.put("position", segment.path("position").asLong());
                }
                JsonNode keywords = segment.path("keywords");
                if (keywords.isArray()) {
                    List<String> kw = new ArrayList<String>();
                    for (JsonNode k : keywords) {
                        kw.add(k.asText());
                    }
                    metadata.put("keywords", kw);
                }
                JsonNode segMeta = segment.path("metadata");
                if (segMeta.isObject()) {
                    Iterator<Map.Entry<String, JsonNode>> it = segMeta.fields();
                    while (it.hasNext()) {
                        Map.Entry<String, JsonNode> e = it.next();
                        metadata.put(e.getKey(), e.getValue().asText());
                    }
                }
                metadata.put("dataset_id", datasetId);
                Document doc = Document.builder().id(id).content(content).metadata(metadata).build();
                hits.add(Retrieved.of(doc, score, datasetId));
                index++;
            }
        } catch (IOException e) {
            throw new DifyRetrievalException("failed to parse Dify retrieve response", e);
        }
        return hits;
    }

    private static String buildUrl(String server, String datasetId) {
        String base = server == null ? "" : server.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/datasets/" + datasetId + "/retrieve";
    }
}
