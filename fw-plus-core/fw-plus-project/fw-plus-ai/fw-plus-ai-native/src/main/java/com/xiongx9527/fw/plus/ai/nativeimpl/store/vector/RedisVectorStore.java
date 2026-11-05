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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.search.IndexDefinition;
import redis.clients.jedis.search.IndexOptions;
import redis.clients.jedis.search.Query;
import redis.clients.jedis.search.Schema;
import redis.clients.jedis.search.SearchResult;

/**
 * 真实原子能力：Redis Stack（RediSearch）向量存储后端。实现 base 的 VectorStorePort。
 *
 * <p>每个集合对应一个 RediSearch 索引（HASH 类型），键前缀 {@code doc:<collection>:}，向量字段用 HNSW +
 * FLOAT32 + COSINE。写入时把向量编码为小端 float32 blob 存入 hash 字段 {@code vector}，检索用
 * {@code *=>[KNN $K @vector $BLOB AS vector_score]}，distance 转成 {@code 1-distance} 作为相关度（截断到 [0,1]）。
 * 元数据过滤与候选 id 收敛在客户端完成。</p>
 *
 * <p>Redis 不暴露索引维度，{@link #collectionDimension(String)} 恒返回 null；维度由 RediSearch 在写入时校验。</p>
 */
public class RedisVectorStore implements VectorStorePort {
    private static final Logger log = LoggerFactory.getLogger(RedisVectorStore.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final String defaultCollection;
    private final UnifiedJedis jedis;
    /** 已尝试注册进 RediSearch 索引的元数据字段（m_<key>），避免重复 FT.ALTER。 */
    private final Set<String> knownFields = ConcurrentHashMap.newKeySet();

    public RedisVectorStore(String baseUrl, String password, String defaultCollection) {
        this(baseUrl, null, password, defaultCollection, 0, 5000, 10000);
    }

    public RedisVectorStore(String baseUrl, String username, String password, String defaultCollection,
                            int database, int connectTimeoutMillis, int readTimeoutMillis) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("redis baseUrl is required");
        }
        this.defaultCollection = defaultCollection == null ? "default" : defaultCollection;
        String hostPort = stripScheme(baseUrl.trim());
        int slash = hostPort.indexOf('/');
        if (slash >= 0) {
            hostPort = hostPort.substring(0, slash);
        }
        String host = hostPort;
        int port = 6379;
        int colon = hostPort.lastIndexOf(':');
        if (colon > 0) {
            host = hostPort.substring(0, colon);
            try {
                port = Integer.parseInt(hostPort.substring(colon + 1));
            } catch (NumberFormatException ignored) {
                port = 6379;
            }
        }
        DefaultJedisClientConfig.Builder cb = DefaultJedisClientConfig.builder()
                .connectionTimeoutMillis(connectTimeoutMillis)
                .socketTimeoutMillis(readTimeoutMillis)
                .database(database);
        if (username != null && !username.isEmpty()) {
            cb.user(username);
        }
        if (password != null && !password.isEmpty()) {
            cb.password(password);
        }
        this.jedis = new UnifiedJedis(new HostAndPort(host, port), cb.build());
    }

    @Override
    public String name() {
        return "redis";
    }

    @Override
    public StoreType type() {
        return StoreType.VECTOR;
    }

    @Override
    public boolean configured() {
        return jedis != null;
    }

    @Override
    public boolean health() {
        try {
            return "PONG".equals(jedis.ping());
        } catch (RuntimeException ex) {
            return false;
        }
    }

    @Override
    public String defaultCollection() {
        return defaultCollection;
    }

    @Override
    public void ensureCollection(String collection, int dimension) {
        Map<String, Object> attrs = new LinkedHashMap<String, Object>();
        attrs.put("TYPE", "FLOAT32");
        attrs.put("DIM", dimension);
        attrs.put("DISTANCE_METRIC", "COSINE");
        Schema schema = new Schema()
                .addTextField("content", 1.0)
                .addTextField("metadata", 1.0)
                .addTextField("pk", 1.0)
                .addVectorField("vector", Schema.VectorField.VectorAlgo.HNSW, attrs);
        IndexDefinition def = new IndexDefinition(IndexDefinition.Type.HASH)
                .setPrefixes(prefix(collection));
        IndexOptions options = IndexOptions.defaultOptions().setDefinition(def);
        try {
            jedis.ftCreate(indexName(collection), options, schema);
        } catch (RuntimeException ex) {
            String msg = ex.getMessage();
            if (msg != null && msg.toLowerCase().contains("already exists")) {
                return;
            }
            throw new IllegalStateException("redis ensureCollection failed: " + msg, ex);
        }
    }

    @Override
    public void upsert(String collection, String id, double[] vector, Map<String, Object> metadata) {
        if (collection == null || id == null || vector == null) {
            return;
        }
        Map<byte[], byte[]> fields = new LinkedHashMap<byte[], byte[]>();
        Object content = metadata == null ? null : metadata.get("content");
        fields.put(bytes("content"), bytes(content == null ? "" : String.valueOf(content)));
        fields.put(bytes("metadata"), bytes(writeJson(metadata)));
        fields.put(bytes("pk"), bytes(id));
        // 改造存储：把标量 metadata 逐键存为独立 HASH 字段 m_<key>，并注册进 RediSearch 索引，
        // 使检索时的 Filter 能下推到 RediSearch 在库内执行
        if (metadata != null) {
            for (Map.Entry<String, Object> e : metadata.entrySet()) {
                Object value = e.getValue();
                if (value == null
                        || !(value instanceof String)
                        && !(value instanceof Number)
                        && !(value instanceof Boolean)) {
                    continue;
                }
                String fieldName = "m_" + e.getKey();
                ftAlterAdd(collection, fieldName);
                fields.put(bytes(fieldName), bytes(String.valueOf(value)));
            }
        }
        fields.put(bytes("vector"), toBlob(vector));
        try {
            jedis.hset(bytes(prefix(collection) + id), fields);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("redis upsert failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void delete(String collection, String id) {
        if (collection == null || id == null) {
            return;
        }
        try {
            jedis.del(prefix(collection) + id);
        } catch (RuntimeException ex) {
            log.warn("redis delete failed: {}", ex.getMessage());
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
            jedis.ftDropIndex(indexName(collection));
        } catch (RuntimeException ex) {
            log.warn("redis dropCollection failed: {}", ex.getMessage());
        }
    }

    @Override
    public void deleteByMetadata(String collection, String key, Object value) {
        String term = (value instanceof Number || value instanceof Boolean)
                ? String.valueOf(value)
                : "\"" + String.valueOf(value).replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        String expr = "@m_" + key + ":(" + term + ")";
        Query query = new Query(expr).returnFields("pk").limit(0, 10000).dialect(2);
        try {
            SearchResult sr = jedis.ftSearch(indexName(collection), query);
            for (redis.clients.jedis.search.Document d : sr.getDocuments()) {
                String pk = str(d.get("pk"));
                if (pk != null) {
                    jedis.del(prefix(collection) + pk);
                }
            }
        } catch (RuntimeException ex) {
            log.warn("redis deleteByMetadata failed: {}", ex.getMessage());
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
        int fetch = (filter == Filter.NO_OP && candidatesEmpty)
                ? topK : Math.max(topK * 10, 200);
        // 过滤下推：Filter 翻译为 RediSearch 表达式；候选 id 收敛下推到 @pk
        StringBuilder prefix = new StringBuilder();
        if (candidates != null) {
            for (String cid : candidates) {
                if (cid == null) {
                    continue;
                }
                if (prefix.length() == 0) {
                    prefix.append("@pk:(");
                } else {
                    prefix.append("|");
                }
                prefix.append(redisEscape(cid));
            }
            if (prefix.length() > 0) {
                prefix.append(")");
            }
        }
        String redisFilter = FilterExpressions.toRedis(filter);
        if (redisFilter != null) {
            if (prefix.length() > 0) {
                prefix.append(" ");
            }
            prefix.append(redisFilter);
        }
        String base = prefix.length() > 0 ? prefix.toString() : "*";
        String queryStr = base + "=>[KNN $K @vector $BLOB AS vector_score]";
        Query query = new Query(queryStr)
                .addParam("K", fetch)
                .addParam("BLOB", toBlob(request.getVector()))
                .returnFields("content", "metadata", "pk", "vector_score")
                .limit(0, fetch)
                .dialect(2);
        String index = indexName(request.getCollection());
        try {
            return collect(jedis.ftSearch(index, query), request, topK);
        } catch (RuntimeException ex) {
            // 后端不支持该过滤（字段未索引等）：退回无过滤查询，交给客户端 matches() 后过滤
            if (prefix.length() > 0) {
                Query fallback = new Query("*=>[KNN $K @vector $BLOB AS vector_score]")
                        .addParam("K", fetch)
                        .addParam("BLOB", toBlob(request.getVector()))
                        .returnFields("content", "metadata", "pk", "vector_score")
                        .limit(0, fetch)
                        .dialect(2);
                try {
                    return collect(jedis.ftSearch(index, fallback), request, topK);
                } catch (RuntimeException ex2) {
                    log.warn("redis search failed: {}", ex2.getMessage());
                    return VectorSearchResult.empty();
                }
            }
            log.warn("redis search failed: {}", ex.getMessage());
            return VectorSearchResult.empty();
        }
    }

    private VectorSearchResult collect(SearchResult sr, VectorSearchRequest request, int topK) {
        List<ScoredDocument> hits = new ArrayList<ScoredDocument>();
        for (redis.clients.jedis.search.Document d : sr.getDocuments()) {
            double distance = toDouble(d.get("vector_score"), 1.0);
            double score = Math.max(0.0, Math.min(1.0, 1.0 - distance));
            String pk = str(d.get("pk"));
            Map<String, Object> meta = parseMetadata(str(d.get("metadata")));
            Document doc = Document.builder().id(pk).content(metaText(meta)).metadata(meta).build();
            hits.add(new ScoredDocument(doc, score));
        }
        List<ScoredDocument> filtered = applyFilter(hits, request);
        if (filtered.size() > topK) {
            filtered = filtered.subList(0, topK);
        }
        return VectorSearchResult.of(filtered);
    }

    private void ftAlterAdd(String collection, String fieldName) {
        if (knownFields.contains(fieldName)) {
            return;
        }
        try {
            jedis.ftAlter(indexName(collection), new Schema().addTextField(fieldName, 1.0));
        } catch (RuntimeException ignored) {
            // 索引暂不支持 FT.ALTER 或字段已存在：忽略，检索时退回客户端后过滤
        }
        knownFields.add(fieldName);
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

    private String writeJson(Map<String, Object> metadata) {
        try {
            return mapper.writeValueAsString(metadata == null ? Collections.emptyMap() : metadata);
        } catch (Exception ex) {
            return "{}";
        }
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
        } catch (Exception ignored) {
            // metadata 非合法 JSON 时退化为空
        }
        return meta;
    }

    private static String indexName(String collection) {
        return "idx:" + collection;
    }

    private static String prefix(String collection) {
        return "doc:" + collection + ":";
    }

    private static String stripScheme(String baseUrl) {
        if (baseUrl.startsWith("redis://")) {
            return baseUrl.substring("redis://".length());
        }
        if (baseUrl.startsWith("rediss://")) {
            return baseUrl.substring("rediss://".length());
        }
        return baseUrl;
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] toBlob(double[] vector) {
        ByteBuffer bb = ByteBuffer.allocate(vector.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (double v : vector) {
            bb.putFloat((float) v);
        }
        return bb.array();
    }

    private static String str(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof byte[]) {
            return new String((byte[]) value, StandardCharsets.UTF_8);
        }
        return String.valueOf(value);
    }

    private static String redisEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace(" ", "\\ ");
    }

    private static double toDouble(Object value, double fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(str(value));
        } catch (NumberFormatException ex) {
            return fallback;
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
}
