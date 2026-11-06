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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 真实原子能力：Nebula Graph 图存储后端（nebula-http-gateway 的 nGQL REST API）。实现 base 的 GraphStorePort。
 *
 * <p>只依赖 JDK HttpURLConnection + Jackson，不引入 nebula-java 驱动。通过 nebula-http-gateway（v2.x）的会话式 API：
 * 先 {@code POST /api/db/connect} 拿到 {@code common-nsid} cookie，后续 {@code POST /api/db/exec} 携带该 cookie 执行 nGQL；
 * 网关再无状态转发到 Nebula 集群的 graphd。网关地址即构造参数 baseUrl（如 http://localhost:18080），
 * graphd 地址（默认 {@code graphd:9669}，docker network 内可达）仅在 connect 时告知网关。</p>
 *
 * <p>节点统一用 {@code Entity} tag（Nebula 保留以 {@code __} 开头的标识符，故不用 {@code __Node}），属性：
 * id / label / content / meta（metadata 的 JSON 字符串）/ vector（向量 JSON 字符串）。关系统一用 {@code Rel} 边类型，
 * 业务类型放在 e.type 属性里。metadata 统一序列化成 meta JSON 字符串，规避 Nebula 属性类型不兼容。
 * 语义与 Neo4jGraphStore 对齐，便于在同构检索链路里互换。</p>
 *
 * <p>除端口契约外，参照 InMemoryGraphStore 额外提供 addNode / addEdge，便于构建图谱。</p>
 */
public class NebulaGraphStore implements GraphStorePort {
    private static final String TAG = "Entity";
    private static final String EDGE = "Rel";
    private static final String NEBULA_USER = "root";
    private static final String NEBULA_PWD = "nebula";
    /** schema 异步生效的重试次数（每次间隔 1s）。 */
    private static final int SCHEMA_RETRY_TIMES = 60;

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE =
            new TypeReference<LinkedHashMap<String, Object>>() {
            };
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;
    private final String graphdAddress;
    private final int graphdPort;
    private final String username;
    private final String password;
    private final String space;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private String nsid;
    /** space/tag/edge 已确认可用（Nebula 的 schema 异步生效，确认后缓存避免每次 execute 重复等待）。 */
    private volatile boolean schemaReady;

    public NebulaGraphStore(String baseUrl) {
        this(baseUrl, null, null, "ai_graph", "graphd", 9669, 5000, 10000);
    }

    public NebulaGraphStore(String baseUrl, String username, String password) {
        this(baseUrl, username, password, "ai_graph", "graphd", 9669, 5000, 10000);
    }

    public NebulaGraphStore(String baseUrl, String username, String password, String space) {
        this(baseUrl, username, password, space, "graphd", 9669, 5000, 10000);
    }

    public NebulaGraphStore(String baseUrl, String username, String password, String space,
                           int connectTimeoutMillis, int readTimeoutMillis) {
        this(baseUrl, username, password, space, "graphd", 9669, connectTimeoutMillis, readTimeoutMillis);
    }

    /** 显式指定 graphd 可达地址（默认 docker network 内的 graphd:9669）。 */
    public NebulaGraphStore(String baseUrl, String graphdAddress, int graphdPort) {
        this(baseUrl, null, null, "ai_graph", graphdAddress, graphdPort, 5000, 10000);
    }

    public NebulaGraphStore(String baseUrl, String username, String password, String space,
                           String graphdAddress, int graphdPort,
                           int connectTimeoutMillis, int readTimeoutMillis) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("nebula gateway baseUrl is required");
        }
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.username = username;
        this.password = password;
        this.space = space == null || space.isEmpty() ? "ai_graph" : space;
        this.graphdAddress = graphdAddress == null || graphdAddress.isEmpty() ? "graphd" : graphdAddress;
        this.graphdPort = graphdPort <= 0 ? 9669 : graphdPort;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    @Override
    public String name() {
        return "nebula";
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
            execRaw("SHOW SPACES;");
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    public void addNode(GraphNode node) {
        if (node == null || node.getId() == null) {
            return;
        }
        save(StoreRecord.builder().id(node.getId()).type(node.getLabel())
                .metadata(node.getMetadata()).vector(node.getVector()).build());
    }

    public void addEdge(GraphEdge edge) {
        if (edge == null || edge.getFromId() == null || edge.getToId() == null) {
            return;
        }
        String gql = "USE " + space + "; UPSERT EDGE " + q(edge.getFromId()) + " -> " + q(edge.getToId())
                + " OF " + EDGE + " SET " + EDGE + ".type=" + q(edge.getType() == null ? "" : edge.getType())
                + ", " + EDGE + ".weight=" + edge.getWeight() + ";";
        try {
            execute(gql);
        } catch (IOException ex) {
            throw new IllegalStateException("nebula addEdge failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<GraphEdge> neighbors(String nodeId, Set<String> edgeTypes) {
        if (nodeId == null) {
            return new ArrayList<GraphEdge>();
        }
        List<String> types = edgeTypes == null ? Collections.<String>emptyList()
                : new ArrayList<String>(edgeTypes);
        StringBuilder filter = new StringBuilder();
        if (types.isEmpty()) {
            filter.append("true");
        } else {
            filter.append("false");
            for (String t : types) {
                filter.append(" OR e.type == ").append(q(t));
            }
        }
        String gql = "USE " + space + "; "
                + "MATCH (n:" + TAG + ")-[e:" + EDGE + "]->(m:" + TAG + ") "
                + "WHERE id(n) == " + q(nodeId) + " AND (" + filter + ") "
                + "RETURN id(m) AS toId, e.type AS type, e.weight AS weight;";
        List<GraphEdge> out = new ArrayList<GraphEdge>();
        try {
            for (Map<String, JsonNode> row : execute(gql)) {
                out.add(GraphEdge.builder()
                        .fromId(nodeId)
                        .toId(text(row.get("toId")))
                        .type(text(row.get("type")))
                        .weight(number(row.get("weight"), 1.0))
                        .build());
            }
        } catch (IOException ex) {
            throw new IllegalStateException("nebula neighbors failed: " + ex.getMessage(), ex);
        }
        return out;
    }

    @Override
    public List<GraphNode> nodes(Collection<String> ids) {
        List<GraphNode> out = new ArrayList<GraphNode>();
        if (ids == null || ids.isEmpty()) {
            return out;
        }
        StringBuilder inClause = new StringBuilder("[");
        boolean first = true;
        for (String id : ids) {
            if (!first) {
                inClause.append(", ");
            }
            inClause.append(q(id));
            first = false;
        }
        inClause.append("]");
        String gql = "USE " + space + "; "
                + "MATCH (n:" + TAG + ") WHERE id(n) IN " + inClause + " "
                + "RETURN id(n) AS id, " + props() + ";";
        try {
            for (Map<String, JsonNode> row : execute(gql)) {
                out.add(toNode(row));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("nebula nodes failed: " + ex.getMessage(), ex);
        }
        return out;
    }

    @Override
    public List<GraphNode> searchNodes(String text, int topK) {
        List<GraphNode> out = new ArrayList<GraphNode>();
        String needle = text == null ? "" : text;
        String gql = "USE " + space + "; "
                + "MATCH (n:" + TAG + ") "
                + "WHERE " + prop("label") + " CONTAINS " + q(needle)
                + " OR " + prop("content") + " CONTAINS " + q(needle) + " "
                + "RETURN id(n) AS id, " + props() + " "
                + "LIMIT " + (topK > 0 ? topK : 100) + ";";
        try {
            for (Map<String, JsonNode> row : execute(gql)) {
                out.add(toNode(row));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("nebula searchNodes failed: " + ex.getMessage(), ex);
        }
        return out;
    }

    @Override
    public void save(StoreRecord record) {
        if (record == null || record.getId() == null) {
            return;
        }
        merge(record);
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
        merge(record);
    }

    private void merge(StoreRecord record) {
        String gql = "USE " + space + "; "
                + "UPSERT VERTEX " + q(record.getId()) + " "
                + "SET " + TAG + ".id=" + q(record.getId())
                + ", " + TAG + ".label=" + q(record.getType() == null ? "" : record.getType())
                + ", " + TAG + ".content=" + q(record.getContent() == null ? "" : record.getContent())
                + ", " + TAG + ".meta=" + q(metadataJson(record.getMetadata()))
                + ", " + TAG + ".vector=" + q(vectorJson(record.getVector())) + ";";
        try {
            execute(gql);
        } catch (IOException ex) {
            throw new IllegalStateException("nebula save failed: " + ex.getMessage(), ex);
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
        // Nebula 的 DELETE VERTEX 只接受逗号分隔的 id（不接受 ["a","b"] 列表字面量）
        StringBuilder idList = new StringBuilder();
        boolean first = true;
        for (String id : ids) {
            if (!first) {
                idList.append(", ");
            }
            idList.append(q(id));
            first = false;
        }
        String gql = "USE " + space + "; DELETE VERTEX " + idList + " WITH EDGE;";
        try {
            execute(gql);
            return ids.size();
        } catch (IOException ex) {
            throw new IllegalStateException("nebula deleteAll failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<StoreRecord> search(StoreQuery query) {
        if (query == null) {
            return new ArrayList<StoreRecord>();
        }
        String needle = query.getText() == null ? "" : query.getText();
        String gql = "USE " + space + "; "
                + "MATCH (n:" + TAG + ") "
                + "WHERE " + prop("label") + " CONTAINS " + q(needle)
                + " OR " + prop("content") + " CONTAINS " + q(needle) + " "
                + "RETURN id(n) AS id, " + props() + " "
                + "LIMIT " + (query.getTopK() > 0 ? query.getTopK() : 10) + ";";
        List<StoreRecord> out = new ArrayList<StoreRecord>();
        try {
            for (Map<String, JsonNode> row : execute(gql)) {
                StoreRecord candidate = toRecord(toNode(row));
                if (matchesFilter(candidate.getMetadata(), query.getFilter())) {
                    out.add(candidate);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("nebula search failed: " + ex.getMessage(), ex);
        }
        return out;
    }

    @Override
    public void clear() {
        try {
            // DELETE VERTEX 不支持子查询/列表字面量，先取出 id 再按逗号分隔删除
            List<String> ids = new ArrayList<String>();
            for (Map<String, JsonNode> row : execute("USE " + space + "; MATCH (n:" + TAG + ") RETURN id(n) AS id;")) {
                String id = text(row.get("id"));
                if (id != null) {
                    ids.add(id);
                }
            }
            if (!ids.isEmpty()) {
                deleteAll(ids);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("nebula clear failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public long count() {
        try {
            List<Map<String, JsonNode>> rows = execute("USE " + space + "; MATCH (n:" + TAG + ") RETURN count(n) AS c;");
            return rows.isEmpty() ? -1L : (long) number(rows.get(0).get("c"), -1.0);
        } catch (IOException ex) {
            return -1L;
        }
    }

    /**
     * 先建 space（含 vid_type），再建 tag/edge，并等待 schema 真正可用。
     *
     * <p>Nebula 的 space / tag / edge 都是异步生效：DDL 返回成功不代表已可写入，紧随的 UPSERT 可能报
     * {@code No schema found}。因此这里在 DDL 后额外 {@code DESCRIBE TAG} 轮询确认，成功后置
     * {@link #schemaReady} 缓存，避免每次 execute 重复等待。</p>
     */
    private void ensureSchema() throws IOException {
        if (schemaReady) {
            return;
        }
        try {
            execRaw("CREATE SPACE IF NOT EXISTS " + space + " (vid_type=FIXED_STRING(256));");
        } catch (IOException ignored) {
            // space 已存在时可能返回已存在错误，忽略
        }
        String ddl = "USE " + space + "; "
                + "CREATE TAG IF NOT EXISTS " + TAG
                + "(id string, label string, content string, meta string, vector string); "
                + "CREATE EDGE IF NOT EXISTS " + EDGE + "(type string, weight double);";
        IOException last = null;
        for (int i = 0; i < 60; i++) {
            try {
                execRaw(ddl);
                // schema 异步生效，确认 tag 可查后再返回
                execRaw("USE " + space + "; DESCRIBE TAG " + TAG + ";");
                schemaReady = true;
                return;
            } catch (IOException ex) {
                last = ex;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        if (last != null) {
            throw last;
        }
    }

    /**
     * 执行 nGQL，并对「schema 尚未在 graphd 侧生效」做重试。
     *
     * <p>Nebula 的 schema 是异步广播的：{@code DESCRIBE TAG} 已能查到 tag，紧随的 MATCH 仍可能报
     * {@code Unknown tag}。这里捕获这类错误，重建 schema 并重试，避免调用方在刚建库时偶发失败。</p>
     */
    private List<Map<String, JsonNode>> execute(String gql) throws IOException {
        IOException last = null;
        for (int i = 0; i < SCHEMA_RETRY_TIMES; i++) {
            ensureSchema();
            try {
                return toRows(execRaw(gql));
            } catch (IOException ex) {
                if (!schemaNotReady(ex)) {
                    throw ex;
                }
                schemaReady = false;
                last = ex;
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw last == null ? new IOException("nebula schema not ready: " + gql) : last;
    }

    private static boolean schemaNotReady(IOException ex) {
        String message = ex.getMessage();
        if (message == null) {
            return false;
        }
        return message.contains("Unknown tag") || message.contains("No schema found");
    }

    private JsonNode execRaw(String gql) throws IOException {
        ensureConnected();
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("gql", gql);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Cookie", "common-nsid=" + nsid);
        OkHttp.Response response = OkHttp.send("POST", baseUrl + "/api/db/exec",
                mapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8), headers,
                connectTimeoutMillis, readTimeoutMillis);
        JsonNode root = mapper.readTree(response.getBody());
        int code = root.path("code").asInt(-1);
        if (code != 0) {
            throw new IOException("nebula nGQL error (code=" + code + "): " + root.path("message").asText());
        }
        return root;
    }

    private void ensureConnected() throws IOException {
        if (nsid != null) {
            return;
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("username", username == null ? NEBULA_USER : username);
        body.put("password", password == null ? NEBULA_PWD : password);
        body.put("address", graphdAddress);
        body.put("port", graphdPort);
        OkHttp.Response response = OkHttp.send("POST", baseUrl + "/api/db/connect",
                mapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8), null,
                connectTimeoutMillis, readTimeoutMillis);
        // gateway 会返回多个 Set-Cookie 头（common-nsid / Path / SameSite / Secure），必须逐条扫描
        for (String cookie : response.getHeaders("Set-Cookie")) {
            for (String part : cookie.split(";")) {
                String p = part.trim();
                if (p.startsWith("common-nsid=")) {
                    nsid = p.substring("common-nsid=".length());
                    break;
                }
            }
            if (nsid != null) {
                break;
            }
        }
        if (nsid == null) {
            throw new IOException("nebula gateway connect failed: no common-nsid cookie; body=" + response.getBody());
        }
    }

    private List<Map<String, JsonNode>> toRows(JsonNode root) {
        List<Map<String, JsonNode>> rows = new ArrayList<Map<String, JsonNode>>();
        JsonNode data = root.path("data");
        if (data == null || data.isNull()) {
            return rows;
        }
        JsonNode headers = data.path("headers");
        JsonNode tables = data.path("tables");
        if (!tables.isArray()) {
            return rows;
        }
        for (JsonNode rowObj : tables) {
            Map<String, JsonNode> mapped = new LinkedHashMap<String, JsonNode>();
            if (headers.isArray()) {
                for (JsonNode h : headers) {
                    String name = h.asText();
                    mapped.put(name, rowObj.path(name));
                }
            } else {
                rowObj.fields().forEachRemaining(e -> mapped.put(e.getKey(), e.getValue()));
            }
            rows.add(mapped);
        }
        return rows;
    }

    private String metadataJson(Map<String, Object> metadata) {
        try {
            return mapper.writeValueAsString(metadata == null ? Collections.emptyMap() : metadata);
        } catch (IOException ex) {
            return "{}";
        }
    }

    private String vectorJson(double[] vector) {
        if (vector == null) {
            return "[]";
        }
        try {
            return mapper.writeValueAsString(vector);
        } catch (IOException ex) {
            return "[]";
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
        if (content != null && !content.isNull() && !content.asText().isEmpty()) {
            meta.put("content", content.asText());
        }
        GraphNode.Builder builder = GraphNode.builder()
                .id(text(row.get("id")))
                .label(text(row.get("label")))
                .metadata(meta);
        JsonNode vector = row.get("vector");
        if (vector != null && vector.isTextual() && !vector.asText().isEmpty()) {
            try {
                JsonNode arr = mapper.readTree(vector.asText());
                if (arr.isArray()) {
                    double[] v = new double[arr.size()];
                    for (int i = 0; i < v.length; i++) {
                        v[i] = arr.get(i).asDouble();
                    }
                    builder.vector(v);
                }
            } catch (IOException ignored) {
                // 向量解析失败则忽略
            }
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

    /** 把字符串包成 nGQL 双引号字面量，转义内部双引号。 */
    private static final char DQ = (char) 34;
    private static final char BS = (char) 92;

    private static String q(String value) {
        String v = value == null ? "" : value;
        StringBuilder sb = new StringBuilder();
        sb.append(DQ);
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (c == DQ || c == BS) {
                sb.append(BS);
            }
            sb.append(c);
        }
        sb.append(DQ);
        return sb.toString();
    }

    /**
     * MATCH 里访问 tag 属性必须写成 {@code n.<tag>.<prop>}：Nebula 3.x 对未限定的 {@code n.prop}
     * 不报错而是返回 NULL，容易让检索静默失效。
     */
    private static String prop(String name) {
        return "n." + TAG + "." + name;
    }

    /** RETURN 子句里的全部节点属性（同样需要 tag 限定）。 */
    private static String props() {
        return prop("label") + " AS label, " + prop("content") + " AS content, "
                + prop("meta") + " AS meta, " + prop("vector") + " AS vector";
    }

    private static String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private static double number(JsonNode node, double fallback) {
        return node == null || !node.isNumber() ? fallback : node.asDouble();
    }
}
