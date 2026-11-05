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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 真实原子能力：PostgreSQL + pgvector 向量存储后端（JDBC）。实现 base 的 VectorStorePort。
 *
 * <p>每个集合对应一张表：{@code id TEXT PRIMARY KEY, content TEXT, metadata JSONB, embedding vector(dim)}。
 * 写入用 {@code ON CONFLICT (id) DO UPDATE} 幂等覆盖，检索用 pgvector 的余弦距离运算符
 * {@code <=>}（0=完全一致），相关度取 {@code 1 - distance} 并截断到 [0,1]。元数据过滤与候选 id
 * 收敛在客户端完成。</p>
 *
 * <p>{@code baseUrl} 即 JDBC URL（如 {@code jdbc:postgresql://host:5432/db}）。</p>
 */
public class PgVectorStore implements VectorStorePort {
    private static final Logger log = LoggerFactory.getLogger(PgVectorStore.class);

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ignored) {
            // 由 DriverManager 的 SPI 兜底
        }
    }

    private final ObjectMapper mapper = new ObjectMapper();
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final String defaultCollection;
    private final int connectTimeoutMillis;

    public PgVectorStore(String jdbcUrl, String username, String password, String defaultCollection) {
        this(jdbcUrl, username, password, defaultCollection, 5000);
    }

    public PgVectorStore(String jdbcUrl, String username, String password, String defaultCollection,
                         int connectTimeoutMillis) {
        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("pgvector jdbcUrl is required");
        }
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.defaultCollection = defaultCollection == null ? "default" : defaultCollection;
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    @Override
    public String name() {
        return "pgvector";
    }

    @Override
    public StoreType type() {
        return StoreType.VECTOR;
    }

    @Override
    public boolean configured() {
        return jdbcUrl != null && !jdbcUrl.isEmpty();
    }

    @Override
    public boolean health() {
        try (Connection c = open(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT 1")) {
            return rs.next();
        } catch (SQLException ex) {
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
        try (Connection c = open(); Statement s = c.createStatement()) {
            try {
                s.execute("CREATE EXTENSION IF NOT EXISTS vector");
            } catch (SQLException ex) {
                log.warn("pgvector create extension failed: {}", ex.getMessage());
            }
            s.execute("CREATE TABLE IF NOT EXISTS " + table(collection)
                    + " (id TEXT PRIMARY KEY, content TEXT, metadata JSONB, embedding vector(" + dimension + "))");
        } catch (SQLException ex) {
            throw new IllegalStateException("pgvector ensureCollection failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Integer collectionDimension(String collection) {
        String sql = "SELECT format_type(a.atttypid, a.atttypmod) AS t FROM pg_attribute a "
                + "JOIN pg_class c ON c.oid = a.attrelid JOIN pg_namespace n ON n.oid = c.relnamespace "
                + "WHERE c.relname = ? AND a.attname = 'embedding' AND a.attnum > 0 AND NOT a.attisdropped";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, collection);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String type = rs.getString("t");
                return parseDim(type);
            }
        } catch (SQLException ex) {
            return null;
        }
    }

    @Override
    public void upsert(String collection, String id, double[] vector, Map<String, Object> metadata) {
        if (collection == null || id == null || vector == null) {
            return;
        }
        String sql = "INSERT INTO " + table(collection)
                + " (id, content, metadata, embedding) VALUES (?, ?, ?::jsonb, ?::vector) "
                + "ON CONFLICT (id) DO UPDATE SET content = EXCLUDED.content, "
                + "metadata = EXCLUDED.metadata, embedding = EXCLUDED.embedding";
        Object content = metadata == null ? null : metadata.get("content");
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, content == null ? "" : String.valueOf(content));
            ps.setString(3, writeJson(metadata));
            ps.setString(4, vectorLiteral(vector));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("pgvector upsert failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void delete(String collection, String id) {
        if (collection == null || id == null) {
            return;
        }
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(
                "DELETE FROM " + table(collection) + " WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.warn("pgvector delete failed: {}", ex.getMessage());
        }
    }

    @Override
    public void deleteBatch(String collection, Collection<String> ids) {
        if (collection == null || ids == null || ids.isEmpty()) {
            return;
        }
        StringBuilder in = new StringBuilder();
        List<String> values = new ArrayList<String>();
        for (String id : ids) {
            if (id != null) {
                if (in.length() > 0) {
                    in.append(',');
                }
                in.append('?');
                values.add(id);
            }
        }
        if (values.isEmpty()) {
            return;
        }
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(
                "DELETE FROM " + table(collection) + " WHERE id IN (" + in + ")")) {
            for (int i = 0; i < values.size(); i++) {
                ps.setString(i + 1, values.get(i));
            }
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.warn("pgvector deleteBatch failed: {}", ex.getMessage());
        }
    }

    @Override
    public void dropCollection(String collection) {
        try (Connection c = open(); Statement s = c.createStatement()) {
            s.execute("DROP TABLE IF EXISTS " + table(collection));
        } catch (SQLException ex) {
            log.warn("pgvector dropCollection failed: {}", ex.getMessage());
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
        String literal = vectorLiteral(request.getVector());
        String sql = "SELECT id, content, metadata, 1 - (embedding <=> ?::vector) AS score FROM "
                + table(request.getCollection()) + " ORDER BY embedding <=> ?::vector LIMIT ?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, literal);
            ps.setString(2, literal);
            ps.setInt(3, fetch);
            List<ScoredDocument> hits = new ArrayList<ScoredDocument>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double raw = rs.getDouble("score");
                    double score = Math.max(0.0, Math.min(1.0, raw));
                    String id = rs.getString("id");
                    Map<String, Object> meta = parseMetadata(rs.getString("metadata"));
                    String content = rs.getString("content");
                    if (content == null && meta.get("content") != null) {
                        content = String.valueOf(meta.get("content"));
                    }
                    Document doc = Document.builder().id(id).content(content).metadata(meta).build();
                    hits.add(new ScoredDocument(doc, score));
                }
            }
            List<ScoredDocument> filtered = applyFilter(hits, request);
            if (filtered.size() > topK) {
                filtered = filtered.subList(0, topK);
            }
            return VectorSearchResult.of(filtered);
        } catch (SQLException ex) {
            log.warn("pgvector search failed: {}", ex.getMessage());
            return VectorSearchResult.empty();
        }
    }

    private Connection open() throws SQLException {
        if (username == null && password == null) {
            return DriverManager.getConnection(jdbcUrl);
        }
        return DriverManager.getConnection(jdbcUrl, username, password);
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

    private static String table(String collection) {
        return "\"" + collection.replace("\"", "\"\"") + "\"";
    }

    private static String vectorLiteral(double[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }

    private static Integer parseDim(String type) {
        if (type == null) {
            return null;
        }
        int open = type.indexOf('(');
        int close = type.indexOf(')');
        if (open < 0 || close <= open) {
            return null;
        }
        try {
            return Integer.parseInt(type.substring(open + 1, close).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
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
