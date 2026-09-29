package dev.langchain4j.store.graph.nebula;

import com.vesoft.nebula.client.graph.SessionPool;
import com.vesoft.nebula.client.graph.data.Node;
import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.data.ValueWrapper;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static dev.langchain4j.store.graph.nebula.NebulaUtils.execute;
import static dev.langchain4j.store.graph.nebula.NebulaUtils.rows;
import static dev.langchain4j.store.graph.nebula.NebulaUtils.sanitizeOrThrows;
import static dev.langchain4j.store.graph.nebula.NebulaUtils.toText;

/**
 * NebulaGraph 图存储，负责读取图 schema 和执行 nGQL，对应 Neo4j 的 {@code Neo4jGraph}。
 *
 * <p>与 Neo4j 的差异：
 * <ul>
 *     <li>Nebula 的 schema 归属于图空间（{@code spaceName}），读取 schema 前必须先绑定图空间。</li>
 *     <li>Nebula schema 不记录边的起点/终点 tag，因此关系只能通过采样数据推断。</li>
 * </ul>
 */
public class NebulaGraph implements AutoCloseable {

    private static final String TAG_NAMES_QUERY = "SHOW TAGS";
    private static final String EDGE_NAMES_QUERY = "SHOW EDGES";

    /** 推断边起点/终点 tag 时，每种边类型的采样条数 */
    private static final int RELATIONSHIP_SAMPLE_LIMIT = 100;

    private final SessionPool sessionPool;

    @Getter
    private final String spaceName;

    @Getter
    private String schema;

    /**
     * 用图服务地址直接创建 {@link NebulaGraph}。
     *
     * @param addresses graphd 地址，多个用逗号分隔，例如 {@code 127.0.0.1:9669,127.0.0.1:9670}
     */
    public static class NebulaGraphBuilder {
        public NebulaGraphBuilder withBasicAuth(String addresses, String username, String password) {
            this.addresses = addresses;
            this.username = username;
            this.password = password;
            return this;
        }
    }

    /**
     * 创建 {@link NebulaGraph}
     *
     * @param sessionPool    已绑定图空间的 {@link SessionPool}（二选一：sessionPool 或 withBasicAuth）
     * @param spaceName      图空间名（使用 withBasicAuth 时必填）
     * @param addresses      graphd 地址（使用 withBasicAuth 时必填）
     * @param username       用户名（使用 withBasicAuth 时必填）
     * @param password       密码（使用 withBasicAuth 时必填）
     * @param maxSessionSize 最大会话数
     * @param timeout        超时时间（毫秒）
     */
    @Builder
    public NebulaGraph(SessionPool sessionPool,
                       String spaceName,
                       String addresses,
                       String username,
                       String password,
                       Integer maxSessionSize,
                       Integer timeout) {

        if (sessionPool != null) {
            this.sessionPool = sessionPool;
            this.spaceName = spaceName;
        } else {
            this.spaceName = ensureNotNull(spaceName, "spaceName");
            this.sessionPool = NebulaUtils.createSessionPool(
                    ensureNotNull(addresses, "addresses"),
                    ensureNotNull(username, "username"),
                    getOrDefault(password, ""),
                    this.spaceName,
                    getOrDefault(maxSessionSize, 0),
                    getOrDefault(timeout, -1));
        }
        refreshSchema();
    }

    public ResultSet executeWrite(String nGql) {

        return execute(this.sessionPool, nGql);
    }

    public List<ResultSet.Record> executeRead(String nGql) {

        return rows(this.sessionPool, nGql);
    }

    public void refreshSchema() {

        List<String> tagNames = schemaNames(TAG_NAMES_QUERY);
        List<String> edgeNames = schemaNames(EDGE_NAMES_QUERY);

        List<String> tagProperties = formatSchemaProperties(tagNames, "TAG");
        List<String> edgeProperties = formatSchemaProperties(edgeNames, "EDGE");
        List<String> relationships = formatRelationships(edgeNames);

        this.schema = "Node properties are the following:\n"
                + String.join("\n", tagProperties) + "\n\n"
                + "Relationship properties are the following:\n"
                + String.join("\n", edgeProperties) + "\n\n"
                + "The relationships are the following:\n"
                + String.join("\n", relationships);
    }

    private List<String> schemaNames(String query) {

        List<String> names = new ArrayList<>();
        for (ResultSet.Record record : rows(this.sessionPool, query)) {
            String name = toText(record.get(0));
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return names;
    }

    private List<String> formatSchemaProperties(List<String> schemaNames, String schemaType) {

        List<String> properties = new ArrayList<>();
        for (String schemaName : schemaNames) {
            String describeQuery = String.format("DESCRIBE %s %s", schemaType, sanitizeOrThrows(schemaName, schemaType));
            properties.add(schemaName + " " + formatProperties(rows(this.sessionPool, describeQuery)));
        }
        return properties;
    }

    private String formatProperties(List<ResultSet.Record> records) {

        List<String> properties = new ArrayList<>();
        for (ResultSet.Record record : records) {
            String name = toText(record.get(0));
            if (name.isEmpty()) {
                continue;
            }
            String type = record.size() > 1 ? toText(record.get(1)) : "";
            properties.add(name + ":" + type);
        }
        return properties.stream().collect(Collectors.joining(", ", "{", "}"));
    }

    /**
     * Nebula schema 不记录边的起点/终点 tag，只能采样图数据推断。
     */
    private List<String> formatRelationships(List<String> edgeNames) {

        Set<String> relationships = new LinkedHashSet<>();
        for (String edgeName : edgeNames) {
            String sanitizedEdgeName = sanitizeOrThrows(edgeName, "edgeName");
            String query = String.format("MATCH (a)-[e:%s]->(b) RETURN a, b LIMIT %d",
                    sanitizedEdgeName, RELATIONSHIP_SAMPLE_LIMIT);
            for (ResultSet.Record record : rows(this.sessionPool, query)) {
                if (record.size() < 2) {
                    continue;
                }
                String start = firstTagName(record.get(0));
                String end = firstTagName(record.get(1));
                if (start == null || end == null) {
                    continue;
                }
                relationships.add(String.format("(:%s)-[:%s]->(:%s)", start, edgeName, end));
            }
        }
        return new ArrayList<>(relationships);
    }

    private String firstTagName(ValueWrapper value) {

        if (value == null || value.isEmpty() || !value.isVertex()) {
            return null;
        }
        try {
            Node node = value.asNode();
            List<String> tagNames = node.tagNames();
            if (tagNames == null || tagNames.isEmpty()) {
                return null;
            }
            return tagNames.get(0);
        } catch (Exception e) {
            throw new NebulaException("Error reading the tags of the NebulaGraph vertex: " + value, e);
        }
    }

    @Override
    public void close() {

        this.sessionPool.close();
    }
}
