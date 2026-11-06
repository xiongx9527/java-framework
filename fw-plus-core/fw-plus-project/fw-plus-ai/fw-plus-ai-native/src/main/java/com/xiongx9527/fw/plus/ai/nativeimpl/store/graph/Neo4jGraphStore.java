package com.xiongx9527.fw.plus.ai.nativeimpl.store.graph;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.base.store.Store;
import com.xiongx9527.fw.plus.ai.base.store.StoreQuery;
import com.xiongx9527.fw.plus.ai.base.store.StoreRecord;
import com.xiongx9527.fw.plus.ai.base.store.StoreType;
import com.xiongx9527.fw.plus.ai.base.store.graph.GraphEdge;
import com.xiongx9527.fw.plus.ai.base.store.graph.GraphNode;
import com.xiongx9527.fw.plus.ai.base.store.graph.GraphStorePort;
import com.xiongx9527.fw.plus.ai.nativeimpl.http.OkHttp;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 真实原子能力：Neo4j 图存储后端（HTTP transactional Cypher API）。实现 base 的 GraphStorePort。
 *
 * <p>只依赖 JDK HttpURLConnection + Jackson，不引入 bolt 驱动。端点为
 * {@code POST /db/{database}/tx/commit}，鉴权用 {@code Authorization: Basic ...}。</p>
 *
 * <p>节点统一打 {@code __Node} 标签，稳定属性：id / label / content / __meta（metadata 的 JSON）/ vector。
 * 关系统一用 {@code __EDGE} 类型，把业务类型放在 r.type 属性里（Cypher 的关系类型不能参数化）。
 * metadata 里非 Neo4j 原生属性类型的值统一序列化成 __meta JSON，避免属性类型不兼容。</p>
 *
 * <p>除端口契约外，参照 InMemoryGraphStore 额外提供 addNode / addEdge，便于构建图谱。</p>
 */
public class Neo4jGraphStore implements GraphStorePort {
    private static final String NODE_LABEL = "__Node";
    private static final String EDGE_TYPE = "__EDGE";
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE =
            new TypeReference<LinkedHashMap<String, Object>>() {
            };
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;
    private final String username;
    private final String password;
    private final String database;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;

    public Neo4jGraphStore(String baseUrl) {
        this(baseUrl, null, null, "neo4j", 5000, 10000);
    }

    public Neo4jGraphStore(String baseUrl, String username, String password) {
        this(baseUrl, username, password, "neo4j", 5000, 10000);
    }

    public Neo4jGraphStore(String baseUrl, String username, String password, String database) {
        this(baseUrl, username, password, database, 5000, 10000);
    }

    public Neo4jGraphStore(String baseUrl, String username, String password, String database,
                           int connectTimeoutMillis, int readTimeoutMillis) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("neo4j baseUrl is required");
        }
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.username = username;
        this.password = password;
        this.database = database == null || database.isEmpty() ? "neo4j" : database;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    @Override
    public String name() {
        return "neo4j";
    }

    @Override
    public StoreType type() {
        return StoreType.GRAPH;
    }

    @Override
    public boolean configured() {
        return baseUrl != null && !baseUrl.isEmpty();
    }

    @Override
    public boolean health() {
        try {
            return call("GET", baseUrl + "/", null).getCode() == 200;
        } catch (IOException ex) {
            return false;
        }
    }

    /** 写入 / 更新节点（按 id 幂等）。 */
    public void addNode(GraphNode node) {
        if (node == null || node.getId() == null) {
            return;
        }
        save(StoreRecord.builder().id(node.getId()).type(node.getLabel())
                .metadata(node.getMetadata()).vector(node.getVector()).build());
    }

    /** 写入 / 更新一条出边。 */
    public void addEdge(GraphEdge edge) {
        if (edge == null || edge.getFromId() == null || edge.getToId() == null) {
            return;
        }
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("from", edge.getFromId());
        params.put("to", edge.getToId());
        params.put("type", edge.getType() == null ? "" : edge.getType());
        params.put("weight", edge.getWeight());
        String cypher = "MATCH (a:" + NODE_LABEL + " {id:$from}), (b:" + NODE_LABEL + " {id:$to}) "
                + "MERGE (a)-[r:" + EDGE_TYPE + " {type:$type}]->(b) SET r.weight=$weight RETURN r";
        try {
            execute(cypher, params);
        } catch (IOException ex) {
            throw new IllegalStateException("neo4j addEdge failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<GraphEdge> neighbors(String nodeId, Set<String> edgeTypes) {
        if (nodeId == null) {
            return new ArrayList<GraphEdge>();
        }
        List<String> types = edgeTypes == null ? Collections.<String>emptyList()
                : new ArrayList<String>(edgeTypes);
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("id", nodeId);
        params.put("types", types);
        String cypher = "MATCH (n:" + NODE_LABEL + " {id:$id})-[r:" + EDGE_TYPE + "]->(m:" + NODE_LABEL + ") "
                + "WHERE size($types)=0 OR r.type IN $types "
                + "RETURN m.id AS toId, r.type AS type, coalesce(r.weight,1.0) AS weight";
        List<GraphEdge> out = new ArrayList<GraphEdge>();
        try {
            for (Map<String, JsonNode> row : execute(cypher, params)) {
                out.add(GraphEdge.builder()
                        .fromId(nodeId)
                        .toId(text(row.get("toId")))
                        .type(text(row.get("type")))
                        .weight(number(row.get("weight"), 1.0))
                        .build());
            }
        } catch (IOException ex) {
            throw new IllegalStateException("neo4j neighbors failed: " + ex.getMessage(), ex);
        }
        return out;
    }

    @Override
    public List<GraphNode> nodes(Collection<String> ids) {
        List<GraphNode> out = new ArrayList<GraphNode>();
        if (ids == null || ids.isEmpty()) {
            return out;
        }
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("ids", new ArrayList<String>(ids));
        String cypher = "MATCH (n:" + NODE_LABEL + ") WHERE n.id IN $ids "
                + "RETURN n.id AS id, n.label AS label, n.content AS content, n.__meta AS meta, n.vector AS vector";
        try {
            for (Map<String, JsonNode> row : execute(cypher, params)) {
                out.add(toNode(row));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("neo4j nodes failed: " + ex.getMessage(), ex);
        }
        return out;
    }

    @Override
    public List<GraphNode> searchNodes(String text, int topK) {
        List<GraphNode> out = new ArrayList<GraphNode>();
        String needle = text == null ? "" : text;
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("text", needle);
        params.put("limit", topK > 0 ? topK : 100);
        String cypher = "MATCH (n:" + NODE_LABEL + ") "
                + "WHERE n.label CONTAINS $text OR n.content CONTAINS $text OR n.__meta CONTAINS $text "
                + "RETURN n.id AS id, n.label AS label, n.content AS content, n.__meta AS meta, n.vector AS vector "
                + "LIMIT $limit";
        try {
            for (Map<String, JsonNode> row : execute(cypher, params)) {
                out.add(toNode(row));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("neo4j searchNodes failed: " + ex.getMessage(), ex);
        }
        return out;
    }

    @Override
    public void save(StoreRecord record) {
        if (record == null || record.getId() == null) {
            return;
        }
        merge(record, false);
    }

    @Override
    public void saveAll(Collection<StoreRecord> records) {
        if (records == null) {
            return;
        }
        for (StoreRecord record : records) {
            save(record);
        }
    }

    @Override
    public void update(StoreRecord record) {
        if (record == null || record.getId() == null) {
            throw new IllegalArgumentException("record id is required");
        }
        merge(record, true);
    }

    private void merge(StoreRecord record, boolean requireExisting) {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("id", record.getId());
        params.put("label", record.getType() == null ? "" : record.getType());
        params.put("content", record.getContent());
        params.put("meta", metadataJson(record.getMetadata()));
        params.put("vector", record.getVector());
        String cypher;
        if (requireExisting) {
            cypher = "MATCH (n:" + NODE_LABEL + " {id:$id}) "
                    + "SET n.label=$label, n.content=$content, n.__meta=$meta, n.vector=$vector "
                    + "RETURN n.id AS id";
        } else {
            cypher = "MERGE (n:" + NODE_LABEL + " {id:$id}) "
                    + "SET n.label=$label, n.content=$content, n.__meta=$meta, n.vector=$vector "
                    + "RETURN n.id AS id";
        }
        try {
            List<Map<String, JsonNode>> rows = execute(cypher, params);
            if (requireExisting && rows.isEmpty()) {
                throw new IllegalStateException("node does not exist: " + record.getId());
            }
        } catch (IOException ex) {
            throw new IllegalStateException("neo4j save failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public StoreRecord get(String id) {
        if (id == null) {
            return null;
        }
        List<StoreRecord> all = getAll(Collections.singletonList(id));
        return all.isEmpty() ? null : all.get(0);
    }

    @Override
    public List<StoreRecord> getAll(Collection<String> ids) {
        List<StoreRecord> out = new ArrayList<StoreRecord>();
        for (GraphNode node : nodes(ids)) {
            out.add(toRecord(node));
        }
        return out;
    }

    @Override
    public boolean delete(String id) {
        if (id == null) {
            return false;
        }
        return deleteAll(Collections.singletonList(id)) > 0;
    }

    @Override
    public long deleteAll(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0L;
        }
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("ids", new ArrayList<String>(ids));
        String cypher = "MATCH (n:" + NODE_LABEL + ") WHERE n.id IN $ids "
                + "DETACH DELETE n RETURN count(n) AS deleted";
        try {
            List<Map<String, JsonNode>> rows = execute(cypher, params);
            return rows.isEmpty() ? 0L : (long) number(rows.get(0).get("deleted"), 0.0);
        } catch (IOException ex) {
            throw new IllegalStateException("neo4j deleteAll failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<StoreRecord> search(StoreQuery query) {
        if (query == null) {
            return new ArrayList<StoreRecord>();
        }
        String needle = query.getText() == null ? "" : query.getText();
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("text", needle);
        params.put("limit", query.getTopK() > 0 ? query.getTopK() : 10);
        String cypher = "MATCH (n:" + NODE_LABEL + ") "
                + "WHERE n.label CONTAINS $text OR n.content CONTAINS $text OR n.__meta CONTAINS $text "
                + "RETURN n.id AS id, n.label AS label, n.content AS content, n.__meta AS meta, n.vector AS vector "
                + "LIMIT $limit";
        List<StoreRecord> out = new ArrayList<StoreRecord>();
        try {
            for (Map<String, JsonNode> row : execute(cypher, params)) {
                StoreRecord candidate = toRecord(toNode(row));
                if (matchesFilter(candidate.getMetadata(), query.getFilter())) {
                    out.add(candidate);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("neo4j search failed: " + ex.getMessage(), ex);
        }
        return out;
    }

    @Override
    public void clear() {
        try {
            execute("MATCH (n:" + NODE_LABEL + ") DETACH DELETE n RETURN count(n) AS deleted",
                    Collections.<String, Object>emptyMap());
        } catch (IOException ex) {
            throw new IllegalStateException("neo4j clear failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public long count() {
        try {
            List<Map<String, JsonNode>> rows = execute(
                    "MATCH (n:" + NODE_LABEL + ") RETURN count(n) AS c",
                    Collections.<String, Object>emptyMap());
            return rows.isEmpty() ? -1L : (long) number(rows.get(0).get("c"), -1.0);
        } catch (IOException ex) {
            return -1L;
        }
    }

    private List<Map<String, JsonNode>> execute(String cypher, Map<String, Object> params) throws IOException {
        Map<String, Object> statement = new LinkedHashMap<String, Object>();
        statement.put("statement", cypher);
        statement.put("parameters", params);
        statement.put("resultDataContents", Collections.singletonList("row"));
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("statements", Collections.singletonList(statement));
        OkHttp.Response response = OkHttp.requireOk(call("POST", txUrl(), body));
        JsonNode root = mapper.readTree(response.getBody());
        JsonNode errors = root.path("errors");
        if (errors.isArray() && errors.size() > 0) {
            throw new IOException("neo4j cypher error: " + errors.get(0).path("message").asText());
        }
        JsonNode result = root.path("results").path(0);
        JsonNode columns = result.path("columns");
        List<Map<String, JsonNode>> rows = new ArrayList<Map<String, JsonNode>>();
        for (JsonNode data : result.path("data")) {
            JsonNode row = data.path("row");
            Map<String, JsonNode> mapped = new LinkedHashMap<String, JsonNode>();
            for (int i = 0; i < columns.size() && i < row.size(); i++) {
                mapped.put(columns.get(i).asText(), row.get(i));
            }
            rows.add(mapped);
        }
        return rows;
    }

    private OkHttp.Response call(String method, String url, Object body) throws IOException {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        if (username != null && !username.isEmpty()) {
            headers.put("Authorization", OkHttp.basicAuth(username, password));
        }
        byte[] data = body == null ? null : mapper.writeValueAsBytes(body);
        return OkHttp.send(method, url, data, headers, connectTimeoutMillis, readTimeoutMillis);
    }

    private String txUrl() {
        return baseUrl + "/db/" + database + "/tx/commit";
    }

    private String metadataJson(Map<String, Object> metadata) {
        try {
            return mapper.writeValueAsString(metadata == null ? Collections.emptyMap() : metadata);
        } catch (IOException ex) {
            return "{}";
        }
    }

    private GraphNode toNode(Map<String, JsonNode> row) {
        Map<String, Object> meta = new LinkedHashMap<String, Object>();
        JsonNode metaNode = row.get("meta");
        if (metaNode != null && metaNode.isTextual() && !metaNode.asText().isEmpty()) {
            try {
                meta.putAll(mapper.readValue(metaNode.asText(), MAP_TYPE));
            } catch (IOException ignored) {
                // 保留空 metadata
            }
        }
        JsonNode content = row.get("content");
        if (content != null && !content.isNull()) {
            meta.put("content", content.asText());
        }
        GraphNode.Builder builder = GraphNode.builder()
                .id(text(row.get("id")))
                .label(text(row.get("label")))
                .metadata(meta);
        JsonNode vector = row.get("vector");
        if (vector != null && vector.isArray()) {
            double[] v = new double[vector.size()];
            for (int i = 0; i < v.length; i++) {
                v[i] = vector.get(i).asDouble();
            }
            builder.vector(v);
        }
        return builder.build();
    }

    private StoreRecord toRecord(GraphNode node) {
        StoreRecord.Builder builder = StoreRecord.builder()
                .id(node.getId())
                .type(node.getLabel())
                .metadata(node.getMetadata())
                .vector(node.getVector());
        Object content = node.getMetadata() == null ? null : node.getMetadata().get("content");
        if (content != null) {
            builder.content(String.valueOf(content));
        }
        return builder.build();
    }

    private static boolean matchesFilter(Map<String, Object> meta, Map<String, Object> filter) {
        if (filter == null || filter.isEmpty()) {
            return true;
        }
        for (Map.Entry<String, Object> e : filter.entrySet()) {
            Object actual = meta == null ? null : meta.get(e.getKey());
            if (actual == null ? e.getValue() != null : !actual.equals(e.getValue())) {
                return false;
            }
        }
        return true;
    }

    private static String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private static double number(JsonNode node, double fallback) {
        return node == null || !node.isNumber() ? fallback : node.asDouble();
    }
}
