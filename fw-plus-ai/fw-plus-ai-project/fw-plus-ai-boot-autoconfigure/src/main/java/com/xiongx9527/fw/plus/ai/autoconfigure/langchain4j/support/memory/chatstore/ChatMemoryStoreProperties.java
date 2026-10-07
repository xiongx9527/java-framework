package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * ChatMemoryStore 存储配置项。
 *
 * <p>{@code type} 决定注入哪种存储后端：{@code in-memory}（默认）/ {@code jdbc} /
 * {@code redis} / {@code mongodb} / {@code elasticsearch}。后者的客户端 Bean
 * （JdbcTemplate / StringRedisTemplate / MongoTemplate / ElasticsearchClient 或 RestClient）
 * 由使用方环境提供。</p>
 */
@ConfigurationProperties(ChatMemoryStoreProperties.PREFIX)
public class ChatMemoryStoreProperties {

    public static final String PREFIX = "fw.plus.ai.langchain4j.chat-memory";

    public static final String TYPE_IN_MEMORY = "in-memory";

    public static final String TYPE_JDBC = "jdbc";

    public static final String TYPE_REDIS = "redis";

    public static final String TYPE_MONGODB = "mongodb";

    public static final String TYPE_ELASTICSEARCH = "elasticsearch";

    /** 存储后端类型，默认进程内存储 */
    private String type = TYPE_IN_MEMORY;

    /** memoryId 不含业务类型前缀时使用的默认业务类型 */
    private String defaultBizType = ChatMemoryIds.DEFAULT_BIZ_TYPE;

    /** 默认租户 ID（{@link ChatMemoryTenantResolver} 未注册或解析为空时使用） */
    private String defaultTenantId = ChatMemoryStoreDefaults.DEFAULT_TENANT_ID;

    private final Jdbc jdbc = new Jdbc();

    private final Redis redis = new Redis();

    private final Mongo mongo = new Mongo();

    private final Elasticsearch elasticsearch = new Elasticsearch();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDefaultBizType() {
        return defaultBizType;
    }

    public void setDefaultBizType(String defaultBizType) {
        this.defaultBizType = defaultBizType;
    }

    public String getDefaultTenantId() {
        return defaultTenantId;
    }

    public void setDefaultTenantId(String defaultTenantId) {
        this.defaultTenantId = defaultTenantId;
    }

    public Jdbc getJdbc() {
        return jdbc;
    }

    public Redis getRedis() {
        return redis;
    }

    public Mongo getMongo() {
        return mongo;
    }

    public Elasticsearch getElasticsearch() {
        return elasticsearch;
    }

    /** JDBC 存储配置 */
    public static class Jdbc {

        /** 消息表名（仅允许字母、数字、下划线），每条消息一行 */
        private String tableName = "ai_chat_memory_message";

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }
    }

    /** Redis 存储配置 */
    public static class Redis {

        /** 键前缀，实际 key 为 {@code keyPrefix + memoryId} */
        private String keyPrefix = RedisChatMemoryStore.DEFAULT_KEY_PREFIX;

        /** 过期时间，如 30m / 12h / 7d；缺省永不过期 */
        private Duration ttl;

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }
    }

    /** MongoDB 存储配置 */
    public static class Mongo {

        /** 集合名，每条消息一篇文档 */
        private String collectionName = MongoChatMemoryStore.DEFAULT_COLLECTION_NAME;

        public String getCollectionName() {
            return collectionName;
        }

        public void setCollectionName(String collectionName) {
            this.collectionName = collectionName;
        }
    }

    /** Elasticsearch 存储配置 */
    public static class Elasticsearch {

        /** 索引名，每条消息一篇文档 */
        private String indexName = ElasticsearchChatMemoryStore.DEFAULT_INDEX_NAME;

        public String getIndexName() {
            return indexName;
        }

        public void setIndexName(String indexName) {
            this.indexName = indexName;
        }
    }
}
