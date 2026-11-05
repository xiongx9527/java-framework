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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 真实原子能力：Weaviate 向量存储后端（REST + GraphQL）。实现 base 的 VectorStorePort。
 *
 * <p>Weaviate 的“集合”对应一个 class（首字母必须大写），创建时 vectorizer=none，向量由调用方在写入时
 * 提供。metadata 无法动态建属性，故整体序列化为 JSON 字符串存入 text 属性 {@code metadata}，原始 id 存
 * {@code pk} 属性；检索走 GraphQL {@code nearVector}，用 {@code _additional.certainty}（0~1）作为相关度。
 * 元数据过滤与候选 id 收敛在客户端完成。</p>
 *
 * <p>Weaviate 不暴露向量维度，{@link #collectionDimension(String)} 恒返回 null；维度由后端在写入时校验。</p>
 */
public class WeaviateVectorStore implements VectorStorePort {
    private static final Logger log = LoggerFactory.getLogger(WeaviateVectorStore.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;
    private final String apiKey;
    private final String username;
    private final String password;
    private final String defaultCollection;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    /** 已确保存在过的 class#property，避免每次 upsert 都打 schema 接口。 */
    private final Set<String> knownProps = ConcurrentHashMap.newKeySet();

    public WeaviateVectorStore(String baseUrl) {
        this(baseUrl, null, null, null, "default", 5000, 10000);
    }

    public WeaviateVectorStore(String baseUrl, String apiKey, String defaultCollection) {
        this(baseUrl, apiKey, null, null, defaultCollection, 5000, 10000);
    }

    public WeaviateVectorStore(String baseUrl, String apiKey, String defaultCollection,
                               int connectTimeoutMillis, int readTimeoutMillis) {
        this(baseUrl, apiKey, null, null, defaultCollection, connectTimeoutMillis, readTimeoutMillis);
    }

    public WeaviateVectorStore(String baseUrl, String apiKey, String username, String password,
                               String defaultCollection, int connectTimeoutMillis, int readTimeoutMillis) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("weaviate baseUrl is required");
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
        return "weaviate";
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
            return call("GET", baseUrl + "/v1/meta", null, false).getCode() == 200;
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
        // Weaviate 不暴露维度；仅在 class 缺失时创建，维度由写入时校验
        if (classExists(collection)) {
            return;
        }
        List<Map<String, Object>> properties = new ArrayList<Map<String, Object>>();
        properties.add(textProperty("content"));
        properties.add(textProperty("metadata"));
        properties.add(textProperty("pk"));
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("class", className(collection));
        body.put("vectorizer", "none");
        body.put("properties", properties);
        try {
            call("POST", baseUrl + "/v1/schema", body, true);
        } catch (IOException ex) {
            String msg = ex.getMessage();
            if (msg != null && msg.contains("already exists")) {
                return;
            }
            throw new IllegalStateException("weaviate ensureCollection failed: " + msg, ex);
        }
    }

    @Override
    public Integer collectionDimension(String collection) {
        return null;
    }

    @Override
    public void upsert(String collection, String id, double[] vector, Map<String, Object> metadata) {
        if (collection == null || id == null || vector == null) {
            return;
        }
        Map<String, Object> props = new LinkedHashMap<String, Object>();
        Object content = metadata == null ? null : metadata.get("content");
        props.put("content", content == null ? "" : String.valueOf(content));
        props.put("metadata", writeJson(metadata));
        props.put("pk", id);
        // 改造存储：把标量 metadata 逐键注册为 class 属性并以原生类型存储，
        // 使检索时的 Filter 能下推到 Weaviate 的 where 在库内执行（不再只能客户端后过滤）
        String cls = className(collection);
        if (metadata != null) {
            for (Map.Entry<String, Object> e : metadata.entrySet()) {
                String key = e.getKey();
                Object value = e.getValue();
                if (value == null || "content".equals(key) || "pk".equals(key)) {
                    continue;
                }
                ensureProperty(cls, key, weaviateType(value));
                props.put(key, value);
            }
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("class", className(collection));
        body.put("id", uuidOf(id));
        body.put("vector", vector);
        body.put("properties", props);
        try {
            call("POST", baseUrl + "/v1/objects", body, true);
        } catch (IOException ex) {
            throw new IllegalStateException("weaviate upsert failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void delete(String collection, String id) {
        if (collection == null || id == null) {
            return;
        }
        try {
            call("DELETE", baseUrl + "/v1/objects/" + className(collection) + "/" + uuidOf(id), null, true);
        } catch (IOException ex) {
            log.warn("weaviate delete failed: {}", ex.getMessage());
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
    public void dropCollection(String collection) {
        try {
            call("DELETE", baseUrl + "/v1/schema/" + className(collection), null, true);
        } catch (IOException ex) {
            log.warn("weaviate dropCollection failed: {}", ex.getMessage());
        }
    }

    @Override
    public void deleteByMetadata(String collection, String key, Object value) {
        Map<String, Object> where = FilterExpressions.toWeaviate(
                Filter.builder().eq(key, value).build());
        if (where == null) {
            throw new UnsupportedOperationException(
                    "deleteByMetadata is not supported for this filter on weaviate");
        }
        String cls = className(collection);
        String gql = "{ Delete { " + cls + "(where:" + graphqlWhere(where) + ") } }";
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("query", gql);
        try {
            call("POST", baseUrl + "/v1/graphql", body, true);
        } catch (IOException ex) {
            log.warn("weaviate deleteByMetadata failed: {}", ex.getMessage());
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
        String cls = className(request.getCollection());
        // 过滤下推：Filter 翻译成 Weaviate where；候选 id 收敛下推到 pk 的 ContainsAny
        Map<String, Object> where = FilterExpressions.toWeaviate(request.getFilter());
        Collection<String> candidates = request.getCandidateIds();
        if (candidates != null && !candidates.isEmpty()) {
            Map<String, Object> cw = new LinkedHashMap<String, Object>();
            cw.put("path", Collections.singletonList("pk"));
            cw.put("operator", "ContainsAny");
            cw.put("valueText", new ArrayList<String>(candidates));
            where = where == null ? cw : andWhere(where, cw);
        }
        StringBuilder q = new StringBuilder();
        q.append("{ Get { ").append(cls).append("(nearVector:{vector:[")
                .append(joinVector(request.getVector())).append("]}");
        if (where != null) {
            q.append(", where:").append(graphqlWhere(where));
        }
        q.append(", limit:").append(fetch)
                .append(") { content metadata pk _additional { id certainty } } } }");
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("query", q.toString());
        try {
            JdkHttp.Response r = call("POST", baseUrl + "/v1/graphql", body, true);
            JsonNode arr = mapper.readTree(r.getBody()).path("data").path("Get").path(cls);
            if (!arr.isArray()) {
                return VectorSearchResult.empty();
            }
            List<ScoredDocument> hits = new ArrayList<ScoredDocument>();
            for (JsonNode n : arr) {
                JsonNode add = n.path("_additional");
                double score = add.has("certainty") ? add.get("certainty").asDouble() : 0.0;
                String id = n.has("pk") ? n.get("pk").asText() : add.path("id").asText(null);
                Map<String, Object> meta = parseMetadata(n.path("metadata").asText(null));
                if (!meta.containsKey("content") && n.has("content")) {
                    meta.put("content", n.get("content").asText());
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
            log.warn("weaviate search failed: {}", ex.getMessage());
            return VectorSearchResult.empty();
        }
    }

    private boolean classExists(String collection) {
        try {
            return call("GET", baseUrl + "/v1/schema/" + className(collection), null, false).getCode() == 200;
        } catch (IOException ex) {
            return false;
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

    private Map<String, Object> parseMetadata(String json) {
        Map<String, Object> meta = new LinkedHashMap<String, Object>();
        if (json == null || json.isEmpty()) {
            return meta;
        }
        try {
            JsonNode node = mapper.readTree(json);
            if (node.isObject()) {
                for (java.util.Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
                    Map.Entry<String, JsonNode> e = it.next();
                    meta.put(e.getKey(), nodeValue(e.getValue()));
                }
            }
        } catch (IOException ignored) {
            // metadata 非合法 JSON 时退化为空，不阻断检索
        }
        return meta;
    }

    private void ensureProperty(String cls, String key, String dataType) {
        String sig = cls + "#" + key;
        if (knownProps.contains(sig)) {
            return;
        }
        Map<String, Object> p = new LinkedHashMap<String, Object>();
        p.put("name", key);
        p.put("dataType", Collections.singletonList(dataType));
        try {
            call("POST", baseUrl + "/v1/schema/" + cls + "/properties", p, false);
        } catch (IOException ignored) {
            // 已存在或网络异常：标记已处理，避免反复打 schema 接口
        }
        knownProps.add(sig);
    }

    private static String weaviateType(Object value) {
        if (value instanceof Boolean) {
            return "boolean";
        }
        if (value instanceof Number) {
            return "number";
        }
        return "text";
    }

    private static Map<String, Object> andWhere(Map<String, Object> a, Map<String, Object> b) {
        Map<String, Object> node = new LinkedHashMap<String, Object>();
        node.put("operator", "And");
        List<Map<String, Object>> ops = new ArrayList<Map<String, Object>>();
        ops.add(a);
        ops.add(b);
        node.put("operands", ops);
        return node;
    }

    private static String graphqlWhere(Map<String, Object> where) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("operator:").append(where.get("operator"));
        Object operands = where.get("operands");
        if (operands instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ops = (List<Map<String, Object>>) operands;
            sb.append(", operands:[");
            for (int i = 0; i < ops.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(graphqlWhere(ops.get(i)));
            }
            sb.append("]");
        } else {
            @SuppressWarnings("unchecked")
            List<String> path = (List<String>) where.get("path");
            sb.append(", path:[");
            for (int i = 0; i < path.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(path.get(i));
            }
            sb.append("]");
            if (where.containsKey("valueString")) {
                sb.append(", valueString:\"").append(escapeGql(String.valueOf(where.get("valueString"))))
                        .append("\"");
            }
            if (where.containsKey("valueNumber")) {
                sb.append(", valueNumber:").append(where.get("valueNumber"));
            }
            if (where.containsKey("valueBoolean")) {
                sb.append(", valueBoolean:").append(where.get("valueBoolean"));
            }
            if (where.containsKey("valueText")) {
                @SuppressWarnings("unchecked")
                List<String> vals = (List<String>) where.get("valueText");
                sb.append(", valueText:[");
                for (int i = 0; i < vals.size(); i++) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    sb.append("\"").append(escapeGql(vals.get(i))).append("\"");
                }
                sb.append("]");
            }
        }
        return sb.append("}").toString();
    }

    private static String escapeGql(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static Map<String, Object> textProperty(String name) {
        Map<String, Object> p = new LinkedHashMap<String, Object>();
        p.put("name", name);
        p.put("dataType", Collections.singletonList("text"));
        return p;
    }

    private String writeJson(Map<String, Object> metadata) {
        try {
            return mapper.writeValueAsString(metadata == null ? Collections.emptyMap() : metadata);
        } catch (IOException ex) {
            return "{}";
        }
    }

    private static String joinVector(double[] vector) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.toString();
    }

    private static String className(String collection) {
        if (collection == null || collection.isEmpty()) {
            return "Default";
        }
        return Character.toUpperCase(collection.charAt(0)) + collection.substring(1);
    }

    private static String uuidOf(String id) {
        try {
            return UUID.fromString(id).toString();
        } catch (IllegalArgumentException ignored) {
            return UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8)).toString();
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
            headers.put("Authorization", "Bearer " + apiKey);
        }
        byte[] data = body == null ? null : mapper.writeValueAsBytes(body);
        JdkHttp.Response response = JdkHttp.send(method, url, data, headers,
                connectTimeoutMillis, readTimeoutMillis);
        return requireOk ? JdkHttp.requireOk(response) : response;
    }
}
