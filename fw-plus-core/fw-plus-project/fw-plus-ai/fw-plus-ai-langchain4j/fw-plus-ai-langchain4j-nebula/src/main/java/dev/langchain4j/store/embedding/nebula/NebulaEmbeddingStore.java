package dev.langchain4j.store.embedding.nebula;

import com.vesoft.nebula.client.graph.SessionPool;
import com.vesoft.nebula.client.graph.data.Node;
import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.data.ValueWrapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.graph.nebula.NebulaException;
import dev.langchain4j.store.graph.nebula.NebulaUtils;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.Utils.isNullOrEmpty;
import static dev.langchain4j.internal.ValidationUtils.ensureBetween;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static dev.langchain4j.internal.ValidationUtils.ensureTrue;
import static dev.langchain4j.store.embedding.nebula.NebulaEmbeddingUtils.*;
import static dev.langchain4j.store.graph.nebula.NebulaUtils.execute;
import static dev.langchain4j.store.graph.nebula.NebulaUtils.rows;
import static dev.langchain4j.store.graph.nebula.NebulaUtils.sanitizeOrThrows;

/**
 * 用 NebulaGraph 的 tag 存向量的 {@link EmbeddingStore}，对应 Neo4j 的 {@code Neo4jEmbeddingStore}。
 *
 * <p>一个文档对应一个点：vid 就是文档 id，tag 属性为
 * {@code id}、{@code text}、{@code embedding}（JSON 数组文本）、{@code metadata}（JSON 对象文本）。
 *
 * <p>与 Neo4j 的差异：
 * <ul>
 *     <li>NebulaGraph 没有向量索引，{@link #findRelevant} 会扫描该 tag 的点（最多 {@code maxScanRows} 个），
 *         在 Java 侧计算余弦相似度后再排序取 topN，数据量很大时应改用专门的向量库。</li>
 *     <li>vid 使用 32 位不带横线的 UUID，图空间的 {@code vid_type} 需要能容纳该长度的字符串，
 *         例如 {@code FIXED_STRING(32)}。</li>
 * </ul>
 */
@Getter
public class NebulaEmbeddingStore implements EmbeddingStore<TextSegment> {

    private static final Logger log = LoggerFactory.getLogger(NebulaEmbeddingStore.class);

    /* NebulaGraph 连接设置 */
    private final SessionPool sessionPool;
    private final String spaceName;

    /* NebulaGraph schema 设置 */
    private final String tag;
    private final String idProperty;
    private final String textProperty;
    private final String embeddingProperty;
    private final String metadataProperty;

    /* 检索设置 */
    private final int dimension;
    private final int maxScanRows;
    private final long awaitSchemaTimeout;

    /**
     * 用图服务地址直接创建 {@link NebulaEmbeddingStore}
     *
     * @param addresses graphd 地址，多个用逗号分隔，例如 {@code 127.0.0.1:9669,127.0.0.1:9670}
     */
    public static class NebulaEmbeddingStoreBuilder {
        public NebulaEmbeddingStoreBuilder withBasicAuth(String addresses, String username, String password) {
            this.addresses = addresses;
            this.username = username;
            this.password = password;
            return this;
        }
    }

    /**
     * 创建 {@link NebulaEmbeddingStore}
     *
     * @param sessionPool       已绑定图空间的 {@link SessionPool}（二选一：sessionPool 或 withBasicAuth）
     * @param spaceName         图空间名（使用 withBasicAuth 时必填）
     * @param addresses         graphd 地址（使用 withBasicAuth 时必填）
     * @param username          用户名（使用 withBasicAuth 时必填）
     * @param password          密码（使用 withBasicAuth 时必填）
     * @param maxSessionSize    最大会话数
     * @param timeout           超时时间（毫秒）
     * @param tag               存储文档的 tag 名（默认 "Document"）
     * @param idProperty        id 属性名（默认 "id"）
     * @param textProperty      文本属性名（默认 "text"）
     * @param embeddingProperty 向量属性名（默认 "embedding"）
     * @param metadataProperty  metadata 属性名（默认 "metadata"）
     * @param dimension         向量维度，大于 0 时校验写入的向量长度
     * @param maxScanRows       检索时最多扫描的点数（默认 10000）
     * @param awaitSchemaTimeout 等待 tag 创建生效的超时时间（毫秒，默认 60000）
     */
    @Builder
    public NebulaEmbeddingStore(SessionPool sessionPool,
                                String spaceName,
                                String addresses,
                                String username,
                                String password,
                                Integer maxSessionSize,
                                Integer timeout,
                                String tag,
                                String idProperty,
                                String textProperty,
                                String embeddingProperty,
                                String metadataProperty,
                                int dimension,
                                Integer maxScanRows,
                                Long awaitSchemaTimeout) {

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

        this.tag = sanitizeOrThrows(getOrDefault(tag, DEFAULT_TAG), "tag");
        this.idProperty = sanitizeOrThrows(getOrDefault(idProperty, DEFAULT_ID_PROP), "idProperty");
        this.textProperty = sanitizeOrThrows(getOrDefault(textProperty, DEFAULT_TEXT_PROP), "textProperty");
        this.embeddingProperty = sanitizeOrThrows(getOrDefault(embeddingProperty, DEFAULT_EMBEDDING_PROP), "embeddingProperty");
        this.metadataProperty = sanitizeOrThrows(getOrDefault(metadataProperty, DEFAULT_METADATA_PROP), "metadataProperty");
        this.dimension = ensureBetween(dimension, 0, 4096, "dimension");
        this.maxScanRows = getOrDefault(maxScanRows, DEFAULT_MAX_SCAN_ROWS);
        this.awaitSchemaTimeout = getOrDefault(awaitSchemaTimeout, DEFAULT_AWAIT_SCHEMA_TIMEOUT);

        createSchema();
    }

    /*
    Methods with `@Override`
    */

    @Override
    public String add(Embedding embedding) {
        String id = randomId();
        add(id, embedding);
        return id;
    }

    @Override
    public void add(String id, Embedding embedding) {
        addInternal(id, embedding, null);
    }

    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        String id = randomId();
        addInternal(id, embedding, textSegment);
        return id;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        return addAll(embeddings, null);
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> embedded) {
        List<String> ids = embeddings.stream()
                .map(ignored -> randomId())
                .collect(Collectors.toList());
        addAllInternal(ids, embeddings, embedded);
        return ids;
    }

    @Override
    public List<EmbeddingMatch<TextSegment>> findRelevant(Embedding referenceEmbedding, int maxResults, double minScore) {

        List<Float> reference = referenceEmbedding.vectorAsList();
        String query = String.format("MATCH (v:%s) RETURN v LIMIT %d", this.tag, this.maxScanRows);

        List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
        for (ResultSet.Record record : rows(this.sessionPool, query)) {
            Node node = toNode(record);
            if (node == null) {
                continue;
            }
            Map<String, ValueWrapper> properties = properties(this, node);
            List<Float> vector = parseEmbedding(text(properties, this.embeddingProperty));
            if (vector.isEmpty()) {
                continue;
            }
            double score = cosineSimilarity(reference, vector);
            if (score < minScore) {
                continue;
            }
            matches.add(toEmbeddingMatch(this, properties, score));
        }

        Collections.sort(matches, (left, right) -> Double.compare(right.score(), left.score()));
        return matches.stream().limit(maxResults).collect(Collectors.toList());
    }

    /*
    Private methods
    */

    private void addInternal(String id, Embedding embedding, TextSegment embedded) {
        addAllInternal(Collections.singletonList(id), Collections.singletonList(embedding),
                embedded == null ? null : Collections.singletonList(embedded));
    }

    private void addAllInternal(List<String> ids, List<Embedding> embeddings, List<TextSegment> embedded) {
        if (isNullOrEmpty(ids) || isNullOrEmpty(embeddings)) {
            log.info("[do not add empty embeddings to nebula]");
            return;
        }
        ensureTrue(ids.size() == embeddings.size(), "ids size is not equal to embeddings size");
        ensureTrue(embedded == null || embeddings.size() == embedded.size(), "embeddings size is not equal to embedded size");

        for (int i = 0; i < ids.size(); i++) {
            Embedding embedding = embeddings.get(i);
            if (this.dimension > 0) {
                ensureTrue(embedding.vector().length == this.dimension,
                        "embedding dimension is not equal to the configured dimension");
            }
            TextSegment segment = embedded == null ? null : embedded.get(i);
            execute(this.sessionPool, toUpsertStatement(this, ids.get(i), embedding, segment));
        }
    }

    private void createSchema() {
        String createTag = String.format("CREATE TAG IF NOT EXISTS %s(%s string, %s string, %s string, %s string)",
                this.tag, this.idProperty, this.textProperty, this.embeddingProperty, this.metadataProperty);
        execute(this.sessionPool, createTag);
        awaitTag();
    }

    /**
     * NebulaGraph 的 DDL 需要心跳同步到 storaged，建 tag 后立刻写入会报 tag 不存在，这里轮询等待其可见。
     */
    private void awaitTag() {
        long deadline = System.currentTimeMillis() + this.awaitSchemaTimeout;
        while (true) {
            if (tagExists()) {
                return;
            }
            if (System.currentTimeMillis() >= deadline) {
                throw new NebulaException(String.format(
                        "The tag `%s` is still not visible in the space `%s` after %d ms, please retry later",
                        this.tag, this.spaceName, this.awaitSchemaTimeout));
            }
            try {
                Thread.sleep(AWAIT_SCHEMA_POLL_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new NebulaException("Interrupted while waiting for the NebulaGraph tag: " + this.tag, e);
            }
        }
    }

    private boolean tagExists() {
        try {
            ResultSet resultSet = execute(this.sessionPool, "DESCRIBE TAG " + this.tag);
            return resultSet.rowsSize() > 0;
        } catch (NebulaException e) {
            return false;
        }
    }

    private Node toNode(ResultSet.Record record) {
        if (record.size() == 0) {
            return null;
        }
        ValueWrapper value = record.get(0);
        if (value == null || value.isEmpty() || !value.isVertex()) {
            return null;
        }
        try {
            return value.asNode();
        } catch (Exception e) {
            throw new NebulaException("Error reading the NebulaGraph vertex: " + value, e);
        }
    }

    /**
     * NebulaGraph 的 vid 长度由图空间的 {@code vid_type} 决定，默认使用 32 位不带横线的 UUID。
     */
    private String randomId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
