package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.store;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.UpdateResult;
import dev.langchain4j.agentic.scope.AgenticScopeKey;
import dev.langchain4j.agentic.scope.AgenticScopeSerializer;
import dev.langchain4j.agentic.scope.AgenticScopeStore;
import dev.langchain4j.agentic.scope.DefaultAgenticScope;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * MongoDB {@link AgenticScopeStore} 实现：scope 经 {@link AgenticScopeSerializer} 序列化为 JSON 落库，
 * 支持跨进程 / 跨重启按 {@code (agentId, memoryId)} 恢复。形态对齐 AgentScope Java 的
 * {@code MongoAgentStateStore}：外部注入或自建 client、结构化复合 {@code _id}、可选 TTL、
 * {@link #close()} 只关闭自建 client。
 *
 * <p>文档结构（集合内每个 scope 一条）：</p>
 * <pre>{@code
 * {
 *   "_id":        { "agent": "<agentId>", "memory": "<memoryId 字符串化>" },
 *   "scope_json": "<AgenticScopeSerializer.toJson 产物>",
 *   "_updated_at": <Date>   // 仅配置 ttl 时参与 TTL 索引
 * }
 * }</pre>
 *
 * <p><b>白名单</b>：scope 状态中的业务类型必须在反序列化前注册
 * （{@link Builder#allowPackagePrefix(String...)} / {@link Builder#allowType(Class[])}），
 * 注册进程级永久生效；未注册类型在 {@link #load} 时抛
 * {@code UnserializableAgenticScopeException}。Spring 环境走
 * {@code fw.plus.ai.langchain4j.agentic.scope-store.serialization.*} 配置。</p>
 */
public class MongoAgenticScopeStore implements AgenticScopeStore, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(MongoAgenticScopeStore.class);

    /** 默认库名。 */
    public static final String DEFAULT_DATABASE = "fw_plus";

    /** 默认集合名。 */
    public static final String DEFAULT_COLLECTION = "fw_plus_agentic_scope";

    /** {@code _id} 内存放 agentId 的字段。 */
    static final String FIELD_AGENT = "agent";

    /** {@code _id} 内存放 memoryId（字符串化）的字段。 */
    static final String FIELD_MEMORY = "memory";

    /** scope JSON 正文所在字段。 */
    static final String FIELD_SCOPE_JSON = "scope_json";

    /** 写入时间字段（TTL 索引键）。 */
    static final String FIELD_UPDATED_AT = "_updated_at";

    private final MongoClient client;

    private final boolean ownsClient;

    private final MongoCollection<Document> collection;

    private final Duration ttl;

    MongoAgenticScopeStore(MongoClient client, boolean ownsClient, String database, String collection, Duration ttl) {
        this.client = client;
        this.ownsClient = ownsClient;
        this.collection = client.getDatabase(database).getCollection(collection, Document.class);
        this.ttl = ttl;
        if (ttl != null) {
            ensureTtlIndex();
        }
    }

    /** 创建构建器。 */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean save(AgenticScopeKey key, DefaultAgenticScope agenticScope) {
        UpdateResult result = collection.replaceOne(Filters.eq("_id", keyDocument(key)),
                scopeToDocument(key, agenticScope), new ReplaceOptions().upsert(true));
        return result.wasAcknowledged();
    }

    @Override
    public Optional<DefaultAgenticScope> load(AgenticScopeKey key) {
        Document document = collection.find(Filters.eq("_id", keyDocument(key))).first();
        return document == null ? Optional.empty() : Optional.of(documentToScope(document));
    }

    @Override
    public boolean delete(AgenticScopeKey key) {
        return collection.deleteOne(Filters.eq("_id", keyDocument(key))).getDeletedCount() > 0;
    }

    @Override
    public Set<AgenticScopeKey> getAllKeys() {
        Set<AgenticScopeKey> keys = new HashSet<>();
        for (Document document : collection.find().projection(Projections.include("_id"))) {
            keys.add(keyFromDocument(document.get("_id", Document.class)));
        }
        return keys;
    }

    /** 只关闭自建（{@link Builder#connectionString} 路径）的 client；外部注入的 client 由调用方负责。 */
    @Override
    public void close() {
        if (ownsClient) {
            client.close();
        }
    }

    // ------------------------------------------------------------------
    // 内部读写（package-private：同包测试直接断言 JSON 往返）
    // ------------------------------------------------------------------

    static Document scopeToDocument(AgenticScopeKey key, DefaultAgenticScope agenticScope) {
        return new Document("_id", keyDocument(key))
                .append(FIELD_SCOPE_JSON, AgenticScopeSerializer.toJson(agenticScope))
                .append(FIELD_UPDATED_AT, new Date());
    }

    static DefaultAgenticScope documentToScope(Document document) {
        return AgenticScopeSerializer.fromJson(document.getString(FIELD_SCOPE_JSON));
    }

    /** 复合 {@code _id}：{@code {agent: agentId, memory: String.valueOf(memoryId)}}（memoryId 归一为字符串）。 */
    static Document keyDocument(AgenticScopeKey key) {
        return new Document(FIELD_AGENT, key.agentId()).append(FIELD_MEMORY, String.valueOf(key.memoryId()));
    }

    static AgenticScopeKey keyFromDocument(Document id) {
        return new AgenticScopeKey(id.getString(FIELD_AGENT), id.getString(FIELD_MEMORY));
    }

    /** 测试观察用：本 store 绑定的集合。 */
    MongoCollection<Document> collection() {
        return collection;
    }

    private void ensureTtlIndex() {
        String indexName = FIELD_UPDATED_AT + "_1";
        IndexOptions options = new IndexOptions().expireAfter(ttl.getSeconds(), TimeUnit.SECONDS);
        try {
            collection.createIndex(Indexes.ascending(FIELD_UPDATED_AT), options);
        } catch (MongoCommandException e) {
            if (e.getErrorCode() == 85 || e.getErrorCode() == 86) {
                // IndexOptionsConflict / IndexKeySpecsConflict：已有索引的 TTL 与当前配置不一致，重建。
                LOG.info("重建 agentic scope TTL 索引 {}（已有索引与 ttl={} 不一致）", indexName, ttl);
                collection.dropIndex(indexName);
                collection.createIndex(Indexes.ascending(FIELD_UPDATED_AT), options);
            } else {
                throw e;
            }
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey(
                    "fw.ai.MongoAgenticScopeStore.fieldRequired", name));
        }
        return value.trim();
    }

    /** 构建器：{@code mongoClient} 与 {@code connectionString} 二选一；ttl / 白名单可选。 */
    public static class Builder {

        private MongoClient mongoClient;

        private String connectionString;

        private String database = DEFAULT_DATABASE;

        private String collection = DEFAULT_COLLECTION;

        private Duration ttl;

        private final List<String> allowPackagePrefixes = new ArrayList<>();

        private final List<Class<?>> allowTypes = new ArrayList<>();

        private Builder() {
        }

        /** 复用外部持有的 client（{@link MongoAgenticScopeStore#close()} 不会关闭它）。 */
        public Builder mongoClient(MongoClient mongoClient) {
            this.mongoClient = mongoClient;
            return this;
        }

        /** 由本 store 自建 client（close() 时关闭），与 {@link #mongoClient} 二选一。 */
        public Builder connectionString(String connectionString) {
            this.connectionString = connectionString;
            return this;
        }

        /** 库名，默认 {@value MongoAgenticScopeStore#DEFAULT_DATABASE}。 */
        public Builder database(String database) {
            this.database = database;
            return this;
        }

        /** 集合名，默认 {@value MongoAgenticScopeStore#DEFAULT_COLLECTION}。 */
        public Builder collection(String collection) {
            this.collection = collection;
            return this;
        }

        /** 文档 TTL（服务端自动过期）；不设置则永久保留。配置时构建即创建 TTL 索引（需可连通）。 */
        public Builder ttl(Duration ttl) {
            this.ttl = ttl;
            return this;
        }

        /** 反序列化白名单：放行包前缀（进程级永久生效）。 */
        public Builder allowPackagePrefix(String... packagePrefixes) {
            if (packagePrefixes != null) {
                this.allowPackagePrefixes.addAll(Arrays.asList(packagePrefixes));
            }
            return this;
        }

        /** 反序列化白名单：放行具体类型（进程级永久生效）。 */
        public Builder allowType(Class<?>... types) {
            if (types != null) {
                this.allowTypes.addAll(Arrays.asList(types));
            }
            return this;
        }

        public MongoAgenticScopeStore build() {
            boolean hasClient = mongoClient != null;
            boolean hasConnectionString = connectionString != null && !connectionString.trim().isEmpty();
            if (hasClient == hasConnectionString) {
                throw new IllegalArgumentException(hasClient
                        ? I18nMessageResolver.resolveKey(
                                "fw.ai.MongoAgenticScopeStore.clientAndConnectionStringExclusive")
                        : I18nMessageResolver.resolveKey(
                                "fw.ai.MongoAgenticScopeStore.clientOrConnectionStringRequired"));
            }
            String resolvedDatabase = requireText(database, "database");
            String resolvedCollection = requireText(collection, "collection");
            if (ttl != null && ttl.getSeconds() < 1) {
                throw new IllegalArgumentException(I18nMessageResolver.resolveKey(
                        "fw.ai.MongoAgenticScopeStore.ttlTooSmall", ttl));
            }
            allowPackagePrefixes.stream()
                    .filter(prefix -> prefix != null && !prefix.trim().isEmpty())
                    .forEach(AgenticScopeSerializer::allowDeserializationPackagePrefix);
            allowTypes.stream().filter(Objects::nonNull).forEach(AgenticScopeSerializer::allowDeserializationType);
            MongoClient client = hasClient ? mongoClient : MongoClients.create(connectionString.trim());
            return new MongoAgenticScopeStore(client, !hasClient, resolvedDatabase, resolvedCollection, ttl);
        }
    }
}
