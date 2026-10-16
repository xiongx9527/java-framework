package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.store;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * agentic scope 存储配置项（{@code fw.plus.ai.langchain4j.agentic.scope-store}）。
 *
 * <p>{@code type} 决定装配哪种 {@code AgenticScopeStore} 并安装到进程级
 * {@code AgenticScopePersister}（安装在本配置装配完成后自动完成）：</p>
 * <ul>
 *     <li>{@code in-memory}（缺省）：{@link InMemoryAgenticScopeStore}，进程内持有 scope 引用；</li>
 *     <li>{@code mongo}：{@link MongoAgenticScopeStore}，JSON 落 MongoDB，跨进程 / 跨重启恢复；</li>
 *     <li>{@code none}：不装配、不安装，保持上游「无存储」行为（scope 只活在 registry 内存中）。</li>
 * </ul>
 *
 * <p>mongo 后端需使用方自行引入 {@code org.mongodb:mongodb-driver-sync}（本框架以 optional 声明，不传递）。</p>
 */
@ConfigurationProperties(AgenticScopeStoreProperties.PREFIX)
public class AgenticScopeStoreProperties {

    /** 配置前缀。 */
    public static final String PREFIX = "fw.plus.ai.langchain4j.agentic.scope-store";

    /** 进程内实现（缺省）。 */
    public static final String TYPE_IN_MEMORY = "in-memory";

    /** MongoDB 持久化实现。 */
    public static final String TYPE_MONGO = "mongo";

    /** 显式关闭：不装配、不安装。 */
    public static final String TYPE_NONE = "none";

    /** 存储后端类型（in-memory / mongo / none）。 */
    private String type = TYPE_IN_MEMORY;

    /** MongoDB 后端参数（type=mongo 时生效）。 */
    private final Mongo mongo = new Mongo();

    /** 反序列化白名单（从 JSON 恢复 scope 时放行业务类型）。 */
    private final Serialization serialization = new Serialization();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Mongo getMongo() {
        return mongo;
    }

    public Serialization getSerialization() {
        return serialization;
    }

    /** MongoDB 后端参数。 */
    public static class Mongo {

        /** 连接串。 */
        private String connectionString = "mongodb://localhost:27017";

        /** 库名。 */
        private String database = MongoAgenticScopeStore.DEFAULT_DATABASE;

        /** 集合名。 */
        private String collection = MongoAgenticScopeStore.DEFAULT_COLLECTION;

        /** 文档 TTL（如 {@code 30d}）；不配置则永久保留。 */
        private Duration ttl;

        public String getConnectionString() {
            return connectionString;
        }

        public void setConnectionString(String connectionString) {
            this.connectionString = connectionString;
        }

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }

        public String getCollection() {
            return collection;
        }

        public void setCollection(String collection) {
            this.collection = collection;
        }

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }
    }

    /** 反序列化白名单；注册为进程级永久生效（仅持久化后端需要）。 */
    public static class Serialization {

        /** 放行的包前缀列表（如 {@code com.example.domain.}）。 */
        private List<String> allowPackagePrefixes = new ArrayList<>();

        /** 放行的具体类型列表。 */
        private List<Class<?>> allowTypes = new ArrayList<>();

        public List<String> getAllowPackagePrefixes() {
            return allowPackagePrefixes;
        }

        public void setAllowPackagePrefixes(List<String> allowPackagePrefixes) {
            this.allowPackagePrefixes = allowPackagePrefixes;
        }

        public List<Class<?>> getAllowTypes() {
            return allowTypes;
        }

        public void setAllowTypes(List<Class<?>> allowTypes) {
            this.allowTypes = allowTypes;
        }
    }
}
