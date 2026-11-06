package com.xiongx9527.fw.plus.ai.nativeimpl.store.vector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.base.store.StoreType;
import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import com.xiongx9527.fw.plus.ai.base.store.vector.Filter;
import com.xiongx9527.fw.plus.ai.base.store.vector.ScoredDocument;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchRequest;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchResult;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorStorePort;
import com.xiongx9527.fw.plus.ai.nativeimpl.http.OkHttp;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 真实原子能力：Qdrant 向量存储后端（REST API）。实现 base 的 VectorStorePort。
 * 覆盖 ensureCollection / upsert / search / delete / dropCollection / health；
 * 元数据过滤与候选 id 收敛在客户端完成；base 的 {@link Filter} 已提供只读条件结构，可按需下推为 Qdrant 过滤表达式。
 * apiKey 非空时通过 api-key 头下发，用于本地/云端开启了鉴权的实例。
 */
public class QdrantVectorStore implements VectorStorePort {
    private static final Logger log = LoggerFactory.getLogger(QdrantVectorStore.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;
    private final String apiKey;
    private final String defaultCollection;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final PointIdStrategy pointIdStrategy;

    public QdrantVectorStore(String baseUrl) {
        this(baseUrl, null, "default", 5000, 10000, PointIdStrategy.UUID);
    }

    public QdrantVectorStore(String baseUrl, String apiKey, String defaultCollection) {
        this(baseUrl, apiKey, defaultCollection, 5000, 10000, PointIdStrategy.UUID);
    }

    public QdrantVectorStore(String baseUrl, String apiKey, String defaultCollection,
                             int connectTimeoutMillis, int readTimeoutMillis) {
        this(baseUrl, apiKey, defaultCollection, connectTimeoutMillis, readTimeoutMillis, PointIdStrategy.UUID);
    }

    public QdrantVectorStore(String baseUrl, String apiKey, String defaultCollection,
                             int connectTimeoutMillis, int readTimeoutMillis, PointIdStrategy pointIdStrategy) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("qdrant baseUrl is required");
        }
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.apiKey = apiKey;
        this.defaultCollection = defaultCollection;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.pointIdStrategy = pointIdStrategy != null ? pointIdStrategy : PointIdStrategy.UUID;
    }

    @Override
    public String name() {
        return "qdrant";
    }

    @Override
    public StoreType type() {
        return StoreType.VECTOR;
    }

    @Override
    public boolean configured() {
        return baseUrl != null && !baseUrl.isEmpty();
    }

    @Override
    public boolean health() {
        try {
            return call("GET", baseUrl + "/healthz", null, false).getCode() == 200;
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public String defaultCollection() {
        return defaultCollection;
    }

    @Override
    public void ensureCollection(String collection, int dimension) {
        Integer existing = collectionDimension(collection);
        if (existing != null) {
            if (existing != dimension) {
                throw new VectorStorePort.DimensionMismatchException(
                        "collection " + collection + " expects dim " + existing + " but got " + dimension);
            }
            return;
        }
        Map<String, Object> vectors = new LinkedHashMap<String, Object>();
        vectors.put("size", dimension);
        vectors.put("distance", "Cosine");
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("vectors", vectors);
        try {
            call("PUT", baseUrl + "/collections/" + collection, body, true);
        } catch (IOException ex) {
            String msg = ex.getMessage();
            if (msg != null && msg.contains("already exists")) {
                return;
            }
            throw new IllegalStateException("qdrant ensureCollection failed: " + msg, ex);
        }
    }

    @Override
    public Integer collectionDimension(String collection) {
        try {
            OkHttp.Response r = call("GET", baseUrl + "/collections/" + collection, null, false);
            if (r.getCode() != 200) {
                return null;
            }
            JsonNode cfg = mapper.readTree(r.getBody()).path("result").path("config").path("params").path("vectors");
            if (cfg.has("size")) {
                return cfg.get("size").asInt();
            }
            return null;
        } catch (IOException ex) {
            return null;
        }
    }

    @Override
    public void upsert(String collection, String id, double[] vector, Map<String, Object> metadata) {
        if (collection == null || id == null || vector == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        if (metadata != null) {
            payload.putAll(metadata);
        }
        payload.put("pk", id);
        Map<String, Object> point = new LinkedHashMap<String, Object>();
        point.put("id", toPointId(id));
        point.put("vector", vector);
        point.put("payload", payload);
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("points", Collections.singletonList(point));
        try {
            checkDim(collection, vector.length);
            call("PUT", baseUrl + "/collections/" + collection + "/points", body, true);
        } catch (IOException ex) {
            throw new IllegalStateException("qdrant upsert failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void delete(String collection, String id) {
        if (collection == null || id == null) {
            return;
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("points", Collections.singletonList(toPointId(id)));
        try {
            call("POST", baseUrl + "/collections/" + collection + "/points/delete", body, true);
        } catch (IOException ex) {
            log.warn("qdrant delete failed: {}", ex.getMessage());
        }
    }

    @Override
    public void deleteBatch(String collection, Collection<String> ids) {
        if (collection == null || ids == null) {
            return;
        }
        List<Object> list = new ArrayList<Object>();
        for (String id : ids) {
            if (id != null) {
                list.add(toPointId(id));
            }
        }
        if (list.isEmpty()) {
            return;
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("points", list);
        try {
            call("POST", baseUrl + "/collections/" + collection + "/points/delete", body, true);
        } catch (IOException ex) {
            log.warn("qdrant deleteBatch failed: {}", ex.getMessage());
        }
    }

    @Override
    public void deleteByMetadata(String collection, String key, Object value) {
        Map<String, Object> qdrantFilter = FilterExpressions.toQdrant(
                Filter.builder().eq(key, value).build());
        if (qdrantFilter == null) {
            throw new UnsupportedOperationException(
                    "deleteByMetadata is not supported for this filter on qdrant");
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("filter", qdrantFilter);
        try {
            call("POST", baseUrl + "/collections/" + collection + "/points/delete", body, true);
        } catch (IOException ex) {
            log.warn("qdrant deleteByMetadata failed: {}", ex.getMessage());
        }
    }

    @Override
    public void dropCollection(String collection) {
        try {
            call("DELETE", baseUrl + "/collections/" + collection, null, true);
        } catch (IOException ex) {
            log.warn("qdrant dropCollection failed: {}", ex.getMessage());
        }
    }

    @Override
    public VectorSearchResult search(VectorSearchRequest request) {
        if (request == null || request.getVector() == null) {
            return VectorSearchResult.empty();
        }
        int topK = request.getTopK() > 0 ? request.getTopK() : 10;
        Filter filter = request.getFilter();
        Collection<String> candidates = request.getCandidateIds();
        boolean candidatesEmpty = candidates == null || candidates.isEmpty();
        Map<String, Object> qdrantFilter = FilterExpressions.toQdrant(filter);
        int fetch;
        if (qdrantFilter != null && candidatesEmpty) {
            fetch = topK;
        } else if (filter == Filter.NO_OP && candidatesEmpty) {
            fetch = topK;
        } else {
            fetch = Math.max(topK * 10, 200);
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("vector", request.getVector());
        body.put("limit", fetch);
        body.put("with_payload", true);
        if (qdrantFilter != null) {
            body.put("filter", qdrantFilter);
        }
        try {
            OkHttp.Response r = call("POST",
                    baseUrl + "/collections/" + request.getCollection() + "/points/search", body, true);
            JsonNode arr = mapper.readTree(r.getBody()).get("result");
            if (arr == null || !arr.isArray()) {
                return VectorSearchResult.empty();
            }
            List<ScoredDocument> hits = new ArrayList<ScoredDocument>();
            for (JsonNode n : arr) {
                double score = n.has("score") ? n.get("score").asDouble() : 0.0;
                JsonNode payload = n.get("payload");
                String pk = payload != null && payload.has("pk")
                        ? payload.get("pk").asText() : stringId(n.get("id"));
                Map<String, Object> meta = new LinkedHashMap<String, Object>();
                if (payload != null) {
                    for (java.util.Iterator<Map.Entry<String, JsonNode>> it = payload.fields(); it.hasNext(); ) {
                        Map.Entry<String, JsonNode> e = it.next();
                        if (!"pk".equals(e.getKey())) {
                            meta.put(e.getKey(), nodeValue(e.getValue()));
                        }
                    }
                }
                Document doc = Document.builder().id(pk).content(metaText(meta)).metadata(meta).build();
                hits.add(new ScoredDocument(doc, score));
            }
            List<ScoredDocument> filtered = applyFilter(hits, request);
            if (filtered.size() > topK) {
                filtered = filtered.subList(0, topK);
            }
            return VectorSearchResult.of(filtered);
        } catch (IOException ex) {
            log.warn("qdrant search failed: {}", ex.getMessage());
            return VectorSearchResult.empty();
        }
    }

    private List<ScoredDocument> applyFilter(List<ScoredDocument> hits, VectorSearchRequest request) {
        Filter filter = request.getFilter();
        Collection<String> candidates = request.getCandidateIds();
        List<ScoredDocument> out = new ArrayList<ScoredDocument>();
        for (ScoredDocument h : hits) {
            if (candidates != null && !candidates.isEmpty()
                    && !candidates.contains(h.getDocument().getId())) {
                continue;
            }
            if (filter != Filter.NO_OP && !filter.matches(h.getDocument().getMetadata())) {
                continue;
            }
            out.add(h);
        }
        return out;
    }

    private void checkDim(String collection, int dim) {
        Integer existing = collectionDimension(collection);
        if (existing != null && existing != dim) {
            throw new VectorStorePort.DimensionMismatchException(
                    "collection " + collection + " expects dim " + existing + " but got " + dim);
        }
    }

    private static String metaText(Map<String, Object> meta) {
        Object c = meta == null ? null : meta.get("content");
        return c == null ? null : String.valueOf(c);
    }

    private static Object nodeValue(JsonNode n) {
        if (n == null) {
            return null;
        }
        if (n.isNumber()) {
            return n.numberValue();
        }
        if (n.isBoolean()) {
            return n.booleanValue();
        }
        return n.asText();
    }

    private static String stringId(JsonNode id) {
        if (id == null) {
            return null;
        }
        if (id.isTextual()) {
            return id.asText();
        }
        if (id.has("uuid")) {
            return id.get("uuid").asText();
        }
        return id.asText();
    }

    private Object toPointId(String id) {
        if (pointIdStrategy == PointIdStrategy.RAW) {
            return id;
        }
        try {
            UUID.fromString(id);
            return id;
        } catch (IllegalArgumentException ignored) {
            return UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8)).toString();
        }
    }

    private OkHttp.Response call(String method, String url, Object body, boolean requireOk) throws IOException {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.put("api-key", apiKey);
        }
        byte[] data = body == null ? null : mapper.writeValueAsBytes(body);
        OkHttp.Response response = OkHttp.send(method, url, data, headers,
                connectTimeoutMillis, readTimeoutMillis);
        return requireOk ? OkHttp.requireOk(response) : response;
    }
}
