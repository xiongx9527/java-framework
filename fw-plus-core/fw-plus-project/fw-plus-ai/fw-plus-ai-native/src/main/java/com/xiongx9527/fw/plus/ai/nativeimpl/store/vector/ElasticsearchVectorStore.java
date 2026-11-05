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
import com.xiongx9527.fw.plus.ai.nativeimpl.http.JdkHttp;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 真实原子能力：Elasticsearch 8.x 向量存储后端（REST）。实现 base 的 VectorStorePort。
 *
 * <p>索引以 dense_vector（cosine）承载向量，metadata 作为 object 字段原样保存；检索走
 * {@code _search} 的 knn 查询。ES 的 cosine 相似度分数已归一化到 (0,1]，越大越相关，直接作为
 * 相关度返回。元数据过滤与候选 id 收敛在客户端完成（与 Qdrant / Milvus 一致）。</p>
 *
 * <p>鉴权：优先 basic（username/password），否则 apiKey（含空格按原样作为 Authorization，否则加
 * Bearer 前缀）。</p>
 */
public class ElasticsearchVectorStore implements VectorStorePort {
    private static final Logger log = LoggerFactory.getLogger(ElasticsearchVectorStore.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;
    private final String apiKey;
    private final String username;
    private final String password;
    private final String defaultCollection;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;

    public ElasticsearchVectorStore(String baseUrl) {
        this(baseUrl, null, null, null, "default", 5000, 10000);
    }

    public ElasticsearchVectorStore(String baseUrl, String apiKey, String defaultCollection) {
        this(baseUrl, apiKey, null, null, defaultCollection, 5000, 10000);
    }

    public ElasticsearchVectorStore(String baseUrl, String apiKey, String defaultCollection,
                                    int connectTimeoutMillis, int readTimeoutMillis) {
        this(baseUrl, apiKey, null, null, defaultCollection, connectTimeoutMillis, readTimeoutMillis);
    }

    public ElasticsearchVectorStore(String baseUrl, String apiKey, String username, String password,
                                    String defaultCollection, int connectTimeoutMillis, int readTimeoutMillis) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("elasticsearch baseUrl is required");
        }
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.apiKey = apiKey;
        this.username = username;
        this.password = password;
        this.defaultCollection = defaultCollection == null ? "default" : defaultCollection;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    @Override
    public String name() {
        return "elasticsearch";
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
            return call("GET", baseUrl + "/_cluster/health", null, false).getCode() == 200;
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
        Map<String, Object> vectorProps = new LinkedHashMap<String, Object>();
        vectorProps.put("type", "dense_vector");
        vectorProps.put("dims", dimension);
        vectorProps.put("similarity", "cosine");
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("vector", vectorProps);
        properties.put("metadata", Collections.singletonMap("type", "object"));
        Map<String, Object> mappings = new LinkedHashMap<String, Object>();
        mappings.put("properties", properties);
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("mappings", mappings);
        try {
            call("PUT", baseUrl + "/" + collection, body, true);
        } catch (IOException ex) {
            String msg = ex.getMessage();
            if (msg != null && msg.contains("resource_already_exists_exception")) {
                return;
            }
            throw new IllegalStateException("elasticsearch ensureCollection failed: " + msg, ex);
        }
    }

    @Override
    public Integer collectionDimension(String collection) {
        try {
            JdkHttp.Response r = call("GET", baseUrl + "/" + collection, null, false);
            if (r.getCode() != 200) {
                return null;
            }
            JsonNode root = mapper.readTree(r.getBody());
            // 真实 ES 返回 {index: {mappings: ...}} 嵌套结构；部分桩返回扁平 {mappings: ...}，两者都兼容
            JsonNode mappings = root.path(collection).path("mappings");
            if (!mappings.isObject()) {
                mappings = root.path("mappings");
            }
            JsonNode dim = mappings.path("properties").path("vector").path("dims");
            return dim.isNumber() ? dim.asInt() : null;
        } catch (IOException ex) {
            return null;
        }
    }

    @Override
    public void upsert(String collection, String id, double[] vector, Map<String, Object> metadata) {
        if (collection == null || id == null || vector == null) {
            return;
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("vector", vector);
        body.put("metadata", metadata == null ? Collections.emptyMap() : metadata);
        try {
            checkDim(collection, vector.length);
            // refresh=wait_for：保证写后即可检索（向量库常见“写完立刻查”语义），代价是等待下一次刷新
            call("PUT", baseUrl + "/" + collection + "/_doc/" + id + "?refresh=wait_for", body, true);
        } catch (IOException ex) {
            throw new IllegalStateException("elasticsearch upsert failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void delete(String collection, String id) {
        if (collection == null || id == null) {
            return;
        }
        try {
            call("DELETE", baseUrl + "/" + collection + "/_doc/" + id, null, true);
        } catch (IOException ex) {
            log.warn("elasticsearch delete failed: {}", ex.getMessage());
        }
    }

    @Override
    public void deleteBatch(String collection, Collection<String> ids) {
        if (collection == null || ids == null) {
            return;
        }
        for (String id : ids) {
            if (id != null) {
                delete(collection, id);
            }
        }
    }

    @Override
    public void deleteByMetadata(String collection, String key, Object value) {
        Map<String, Object> esFilter = FilterExpressions.toElasticsearch(
                Filter.builder().eq(key, value).build());
        if (esFilter == null) {
            throw new UnsupportedOperationException(
                    "deleteByMetadata is not supported for this filter on elasticsearch");
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("query", esFilter);
        try {
            call("POST", baseUrl + "/" + collection + "/_delete_by_query", body, true);
        } catch (IOException ex) {
            log.warn("elasticsearch deleteByMetadata failed: {}", ex.getMessage());
        }
    }

    @Override
    public void dropCollection(String collection) {
        try {
            call("DELETE", baseUrl + "/" + collection, null, true);
        } catch (IOException ex) {
            log.warn("elasticsearch dropCollection failed: {}", ex.getMessage());
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
        Map<String, Object> esFilter = FilterExpressions.toElasticsearch(filter);
        int fetch;
        if (esFilter != null && candidatesEmpty) {
            fetch = topK;
        } else if (filter == Filter.NO_OP && candidatesEmpty) {
            fetch = topK;
        } else {
            fetch = Math.max(topK * 10, 200);
        }
        Map<String, Object> knn = new LinkedHashMap<String, Object>();
        knn.put("field", "vector");
        knn.put("query_vector", request.getVector());
        knn.put("k", fetch);
        knn.put("num_candidates", Math.max(fetch * 10, 200));
        if (esFilter != null) {
            knn.put("filter", Collections.singletonList(esFilter));
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("size", fetch);
        body.put("knn", knn);
        try {
            JdkHttp.Response r = call("POST", baseUrl + "/" + request.getCollection() + "/_search", body, true);
            JsonNode hitsNode = mapper.readTree(r.getBody()).path("hits").path("hits");
            if (!hitsNode.isArray()) {
                return VectorSearchResult.empty();
            }
            List<ScoredDocument> hits = new ArrayList<ScoredDocument>();
            for (JsonNode n : hitsNode) {
                double score = n.has("_score") ? n.get("_score").asDouble() : 0.0;
                String id = n.path("_id").asText(null);
                JsonNode source = n.path("_source");
                JsonNode metaNode = source.path("metadata");
                Map<String, Object> meta = new LinkedHashMap<String, Object>();
                if (metaNode.isObject()) {
                    for (java.util.Iterator<Map.Entry<String, JsonNode>> it = metaNode.fields(); it.hasNext(); ) {
                        Map.Entry<String, JsonNode> e = it.next();
                        meta.put(e.getKey(), nodeValue(e.getValue()));
                    }
                }
                Document doc = Document.builder().id(id).content(metaText(meta)).metadata(meta).build();
                hits.add(new ScoredDocument(doc, score));
            }
            List<ScoredDocument> filtered = applyFilter(hits, request);
            if (filtered.size() > topK) {
                filtered = filtered.subList(0, topK);
            }
            return VectorSearchResult.of(filtered);
        } catch (IOException ex) {
            log.warn("elasticsearch search failed: {}", ex.getMessage());
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

    private JdkHttp.Response call(String method, String url, Object body, boolean requireOk) throws IOException {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        if (username != null && !username.isEmpty()) {
            headers.put("Authorization", JdkHttp.basicAuth(username, password));
        } else if (apiKey != null && !apiKey.isEmpty()) {
            headers.put("Authorization", apiKey.indexOf(' ') > 0 ? apiKey : "Bearer " + apiKey);
        }
        byte[] data = body == null ? null : mapper.writeValueAsBytes(body);
        JdkHttp.Response response = JdkHttp.send(method, url, data, headers,
                connectTimeoutMillis, readTimeoutMillis);
        return requireOk ? JdkHttp.requireOk(response) : response;
    }
}
