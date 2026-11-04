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
 * 真实原子能力：Milvus 向量存储后端（Milvus 2.5+/2.6 的 v2 RESTful API）。实现 base 的 VectorStorePort。
 *
 * <p>端点均为 {@code /v2/vectordb/...}：collections/create、collections/describe、
 * collections/drop、entities/upsert、entities/delete、entities/search，鉴权用
 * {@code Authorization: Bearer <token>}（token 形如 user:password）。</p>
 *
 * <p>create 用简化形态（collectionName + dimension + metricType），Milvus 会自动建索引、
 * 自动 load 并开启动态字段。base 的 id 是字符串，而简化形态主键 id 为 Int64，因此写入时把
 * 字符串 id 映射为稳定的 64 位代理键，并把原始 id 放进动态字段 pk，检索时据此还原。</p>
 *
 * <p>元数据过滤与候选 id 收敛在客户端完成（base 的 {@link Filter} 不暴露内部结构，无法翻译为 Milvus expr）。</p>
 */
public class MilvusVectorStore implements VectorStorePort {
    private static final Logger log = LoggerFactory.getLogger(MilvusVectorStore.class);
    private static final String API = "/v2/vectordb";
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;
    private final String token;
    private final String defaultCollection;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;

    public MilvusVectorStore(String baseUrl) {
        this(baseUrl, null, "default", 5000, 10000);
    }

    public MilvusVectorStore(String baseUrl, String token, String defaultCollection) {
        this(baseUrl, token, defaultCollection, 5000, 10000);
    }

    public MilvusVectorStore(String baseUrl, String token, String defaultCollection,
                             int connectTimeoutMillis, int readTimeoutMillis) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("milvus baseUrl is required");
        }
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.token = token;
        this.defaultCollection = defaultCollection;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    @Override
    public String name() {
        return "milvus";
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
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("collectionName", collection);
        body.put("dimension", dimension);
        body.put("metricType", "COSINE");
        try {
            dataOf(call("POST", baseUrl + API + "/collections/create", body, true));
        } catch (IOException ex) {
            String msg = ex.getMessage();
            if (msg != null && msg.contains("exist")) {
                return;
            }
            throw new IllegalStateException("milvus ensureCollection failed: " + msg, ex);
        }
    }

    @Override
    public Integer collectionDimension(String collection) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("collectionName", collection);
        try {
            JdkHttp.Response r = call("POST", baseUrl + API + "/collections/describe", body, false);
            if (r.getCode() != 200) {
                return null;
            }
            JsonNode root = mapper.readTree(r.getBody());
            if (root.has("code") && root.get("code").asInt() != 0) {
                return null;
            }
            JsonNode fields = root.path("data").path("fields");
            if (!fields.isArray()) {
                fields = root.path("fields");
            }
            for (JsonNode f : fields) {
                if ("vector".equals(f.path("name").asText())) {
                    JsonNode dim = f.path("params").path("dim");
                    if (dim.isNumber()) {
                        return dim.asInt();
                    }
                    if (dim.isTextual()) {
                        try {
                            return Integer.parseInt(dim.asText());
                        } catch (NumberFormatException ignored) {
                            return null;
                        }
                    }
                }
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
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("id", surrogate(id));
        item.put("vector", vector);
        item.put("pk", id);
        if (metadata != null) {
            for (Map.Entry<String, Object> e : metadata.entrySet()) {
                if (!"id".equals(e.getKey()) && !"vector".equals(e.getKey()) && !"pk".equals(e.getKey())) {
                    item.put(e.getKey(), e.getValue());
                }
            }
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("collectionName", collection);
        body.put("data", Collections.singletonList(item));
        try {
            checkDim(collection, vector.length);
            dataOf(call("POST", baseUrl + API + "/entities/upsert", body, true));
        } catch (IOException ex) {
            throw new IllegalStateException("milvus upsert failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void delete(String collection, String id) {
        if (collection == null || id == null) {
            return;
        }
        deleteByIds(collection, Collections.singletonList(id));
    }

    @Override
    public void deleteBatch(String collection, Collection<String> ids) {
        if (collection == null || ids == null) {
            return;
        }
        List<String> list = new ArrayList<String>();
        for (String id : ids) {
            if (id != null) {
                list.add(id);
            }
        }
        if (list.isEmpty()) {
            return;
        }
        deleteByIds(collection, list);
    }

    private void deleteByIds(String collection, List<String> ids) {
        StringBuilder expr = new StringBuilder("id in [");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                expr.append(",");
            }
            expr.append(surrogate(ids.get(i)));
        }
        expr.append("]");
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("collectionName", collection);
        body.put("filter", expr.toString());
        try {
            dataOf(call("POST", baseUrl + API + "/entities/delete", body, true));
        } catch (IOException ex) {
            log.warn("milvus delete failed: {}", ex.getMessage());
        }
    }

    @Override
    public void dropCollection(String collection) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("collectionName", collection);
        try {
            dataOf(call("POST", baseUrl + API + "/collections/drop", body, true));
        } catch (IOException ex) {
            log.warn("milvus dropCollection failed: {}", ex.getMessage());
        }
    }

    @Override
    public VectorSearchResult search(VectorSearchRequest request) {
        if (request == null || request.getVector() == null) {
            return VectorSearchResult.empty();
        }
        int topK = request.getTopK() > 0 ? request.getTopK() : 10;
        int fetch = (request.getFilter() == Filter.NO_OP
                && (request.getCandidateIds() == null || request.getCandidateIds().isEmpty()))
                ? topK : Math.max(topK * 10, 200);
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("collectionName", request.getCollection());
        body.put("data", Collections.singletonList(request.getVector()));
        body.put("limit", fetch);
        body.put("outputFields", Collections.singletonList("*"));
        try {
            JsonNode arr = dataOf(call("POST", baseUrl + API + "/entities/search", body, true));
            if (arr == null || !arr.isArray()) {
                return VectorSearchResult.empty();
            }
            List<ScoredDocument> hits = new ArrayList<ScoredDocument>();
            for (JsonNode n : arr) {
                double score = n.has("distance") ? n.get("distance").asDouble() : 0.0;
                String pk = n.has("pk") ? n.get("pk").asText() : stringId(n.get("id"));
                Map<String, Object> meta = new LinkedHashMap<String, Object>();
                for (java.util.Iterator<Map.Entry<String, JsonNode>> it = n.fields(); it.hasNext(); ) {
                    Map.Entry<String, JsonNode> e = it.next();
                    if ("id".equals(e.getKey()) || "distance".equals(e.getKey())
                            || "vector".equals(e.getKey()) || "pk".equals(e.getKey())) {
                        continue;
                    }
                    meta.put(e.getKey(), nodeValue(e.getValue()));
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
            log.warn("milvus search failed: {}", ex.getMessage());
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

    /** 解析 v2 响应：非 0 code 视为失败，成功时返回 data 节点（无 data 时返回根节点）。 */
    private JsonNode dataOf(JdkHttp.Response response) throws IOException {
        JsonNode root = mapper.readTree(response.getBody());
        if (root.has("code") && root.get("code").asInt() != 0) {
            throw new IOException("milvus code " + root.get("code").asInt()
                    + ": " + root.path("message").asText());
        }
        return root.has("data") ? root.get("data") : root;
    }

    private static String stringId(JsonNode id) {
        return id == null ? null : id.asText();
    }

    private static long surrogate(String id) {
        return UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8)).getMostSignificantBits();
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
        headers.put("Request-Timeout", "10");
        if (token != null && !token.isEmpty()) {
            headers.put("Authorization", "Bearer " + token);
        }
        byte[] data = body == null ? null : mapper.writeValueAsBytes(body);
        JdkHttp.Response response = JdkHttp.send(method, url, data, headers,
                connectTimeoutMillis, readTimeoutMillis);
        return requireOk ? JdkHttp.requireOk(response) : response;
    }
}
