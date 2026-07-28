# fw-plus-ai-langchain4j-store-embeding-spring-boot-starter

统一的 LangChain4j 向量存储 Starter，用于在 Spring Boot 2.7 / Java 8 项目中按配置创建多个 `EmbeddingStore<TextSegment>`，并通过 `EmbeddingStoreRegistry` 统一获取。

## 引入依赖

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-langchain4j-store-embeding-spring-boot-starter</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
```

Starter 只作为依赖入口，不包含业务代码。自动配置位于：

```text
fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/store/embeding
```

## 默认依赖策略

本 starter 默认保持轻量，只直接依赖：

- `fw-plus-ai-boot-autoconfigure`
- `fw-plus-ai-langchain4j-store-embeding`

不会默认传递引入 Milvus、Qdrant、Weaviate、Elasticsearch、Redis、Azure、Oracle、Tablestore 等具体向量数据库实现包，避免业务应用只引 starter 就带上大量厂商 SDK。

如果业务要使用具体 provider，请在应用侧额外引入对应 LangChain4j 依赖。starter 的 `pom.xml` 中保留了被注释的依赖清单，可按需复制或取消注释到业务工程中。

例如使用 Milvus：

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-milvus</artifactId>
</dependency>
```

例如使用 Qdrant：

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-qdrant</artifactId>
</dependency>
```

## 最小可用示例

只想先跑通 starter，可以先使用内存向量存储：

```yaml
fw-plus:
  ai:
    langchain4j:
      embedding-store:
        default-store: memory
        stores:
          memory:
            provider: in-memory
```

启动后可直接注入默认 store：

```java
@Autowired
private EmbeddingStoreRegistry embeddingStoreRegistry;

public EmbeddingStore<TextSegment> store() {
    return embeddingStoreRegistry.getDefault();
}
```

## 基础配置

```yaml
fw-plus:
  ai:
    langchain4j:
      embedding-store:
        enabled: true
        default-store: memory
        stores:
          memory:
            provider: in-memory
          knowledge:
            provider: milvus
            dimension: 768
            properties:
              uri: http://127.0.0.1:19530
              collectionName: knowledge
              apiKey: token
          faq:
            provider: qdrant
            properties:
              host: 127.0.0.1
              port: 6334
              collectionName: faq
              useTls: false
```

说明：

- `enabled`：是否启用统一向量存储自动配置，默认启用。
- `default-store`：默认向量存储 key，可不填。
- `stores`：多向量存储配置，map key 会作为 store key。
- `provider`：向量存储类型。
- `dimension`：通用维度字段，会自动映射到支持 `dimension(...)` 的 builder。
- `metadataKeys`：通用元数据 key，会自动映射到支持 `metadataKeys(...)` 的 builder。
- `properties`：传给具体 LangChain4j store builder 的属性；属性名与 builder 方法名保持一致。

## 获取向量存储

```java
@Autowired
private EmbeddingStoreRegistry embeddingStoreRegistry;

public EmbeddingStore<TextSegment> defaultStore() {
    return embeddingStoreRegistry.getDefault();
}

public EmbeddingStore<TextSegment> knowledgeStore() {
    return embeddingStoreRegistry.get("knowledge");
}
```

也可以注入 `EmbeddingStoreFactory`，按 `EmbeddingStoreDefinition` 动态创建。

## 支持的 provider

当前按项目锁定的 LangChain4j `0.35.0` 实证支持以下 provider。除 `in-memory` 外，业务应用需要按需显式引入对应 LangChain4j 实现 jar。

| Provider | LangChain4j 类 | 应用侧依赖 | 说明 |
| --- | --- | --- | --- |
| `in-memory`, `inmemory`, `memory` | `dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore` | 随 core 能力可用 | 内存向量存储 |
| `redis`, `redisstack` | `dev.langchain4j.store.embedding.redis.RedisEmbeddingStore` | `dev.langchain4j:langchain4j-redis` | 专用兼容 adapter |
| `milvus` | `dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore` | `dev.langchain4j:langchain4j-milvus` | 专用兼容 adapter |
| `elasticsearch` | `dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore` | `dev.langchain4j:langchain4j-elasticsearch` | 专用兼容 adapter |
| `qdrant` | `dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore` | `dev.langchain4j:langchain4j-qdrant` | 专用兼容 adapter |
| `weaviate` | `dev.langchain4j.store.embedding.weaviate.WeaviateEmbeddingStore` | `dev.langchain4j:langchain4j-weaviate` | 专用兼容 adapter |
| `azure-ai-search` | `dev.langchain4j.store.embedding.azure.search.AzureAiSearchEmbeddingStore` | `dev.langchain4j:langchain4j-azure-ai-search` | 专用兼容 adapter |
| `azure-cosmos-mongo-vcore` | `dev.langchain4j.store.embedding.azure.cosmos.mongo.vcore.AzureCosmosDbMongoVCoreEmbeddingStore` | `dev.langchain4j:langchain4j-azure-cosmos-mongo-vcore` | 独立反射 adapter |
| `azure-cosmos-nosql` | `dev.langchain4j.store.embedding.azure.cosmos.nosql.AzureCosmosDbNoSqlEmbeddingStore` | `dev.langchain4j:langchain4j-azure-cosmos-nosql` | 专用兼容 adapter |
| `cassandra` | `dev.langchain4j.store.embedding.cassandra.CassandraEmbeddingStore` | `dev.langchain4j:langchain4j-cassandra` | 独立反射 adapter |
| `astra-db` | `dev.langchain4j.store.embedding.astradb.AstraDbEmbeddingStore` | 随 `dev.langchain4j:langchain4j-cassandra` 提供 | 独立反射 adapter |
| `chroma` | `dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore` | `dev.langchain4j:langchain4j-chroma` | 独立反射 adapter |
| `couchbase` | `dev.langchain4j.store.embedding.couchbase.CouchbaseEmbeddingStore` | `dev.langchain4j:langchain4j-couchbase` | 独立反射 adapter |
| `infinispan` | `dev.langchain4j.store.embedding.infinispan.InfinispanEmbeddingStore` | `dev.langchain4j:langchain4j-infinispan` | Java 17 字节码，业务侧按需引入 |
| `mongodb-atlas`, `mongodb` | `dev.langchain4j.store.embedding.mongodb.MongoDbEmbeddingStore` | `dev.langchain4j:langchain4j-mongodb-atlas` | 独立反射 adapter |
| `neo4j` | `dev.langchain4j.store.embedding.neo4j.Neo4jEmbeddingStore` | `dev.langchain4j:langchain4j-neo4j` | Java 17 字节码，业务侧按需引入 |
| `opensearch` | `dev.langchain4j.store.embedding.opensearch.OpenSearchEmbeddingStore` | `dev.langchain4j:langchain4j-opensearch` | 独立反射 adapter |
| `oracle` | `dev.langchain4j.store.embedding.oracle.OracleEmbeddingStore` | `dev.langchain4j:langchain4j-oracle` | 专用兼容 adapter |
| `pgvector`, `postgres`, `postgresql` | `dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore` | `dev.langchain4j:langchain4j-pgvector` | 独立反射 adapter |
| `pinecone` | `dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore` | `dev.langchain4j:langchain4j-pinecone` | 独立反射 adapter |
| `tablestore` | `dev.langchain4j.store.embedding.tablestore.TablestoreEmbeddingStore` | `dev.langchain4j:langchain4j-tablestore` | 专用兼容 adapter |
| `vearch` | `dev.langchain4j.store.embedding.vearch.VearchEmbeddingStore` | `dev.langchain4j:langchain4j-vearch` | 独立反射 adapter |
| `vespa` | `dev.langchain4j.store.embedding.vespa.VespaEmbeddingStore` | `dev.langchain4j:langchain4j-vespa` | 独立反射 adapter |

`MariaDbEmbeddingStore` 属于 LangChain4j 较新版本能力；当前项目使用的 `0.35.0` 中没有 `dev.langchain4j:langchain4j-mariadb:0.35.0`，升级 LangChain4j 后再补该 provider。

`LangChain4jEmbeddingStoreProviderAdapter` 是内部反射创建工具，标准 provider 由上表中的独立 adapter 注册，避免多个 adapter 同时匹配同一个 provider。

注意：`langchain4j-infinispan:0.35.0` 和 `langchain4j-neo4j:0.35.0` 的 store class 是 Java 17 字节码。当前框架保持 Java 8 编译，因此这两个 provider 使用反射专用 adapter，且 Java 8 starter 不默认引入这两个 jar；后续 Java 17 应用需要使用时，在业务侧显式引入对应 LangChain4j 依赖。

## Java 17 provider

以下 provider 依赖的 LangChain4j `0.35.0` jar 是 Java 17 字节码，不适合放进 Java 8 starter 的默认依赖：

- `infinispan`：`dev.langchain4j:langchain4j-infinispan:0.35.0`
- `neo4j`：`dev.langchain4j:langchain4j-neo4j:0.35.0`

如果应用运行在 Java 17，并且需要使用这两个 provider，请在业务应用侧显式引入：

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-infinispan</artifactId>
    <version>0.35.0</version>
</dependency>

<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-neo4j</artifactId>
    <version>0.35.0</version>
</dependency>
```

Java 17 应用配置示例：

```yaml
fw-plus:
  ai:
    langchain4j:
      embedding-store:
        stores:
          graph:
            provider: neo4j
            dimension: 768
            properties:
              uri: bolt://127.0.0.1:7687
              username: neo4j
              password: password
              label: Document
              indexName: document_embedding_idx
          cache:
            provider: infinispan
            dimension: 768
            properties:
              cacheName: embeddings
```

Java 8 应用不要显式引入上述两个 jar，否则 Maven/IDE 可能报 `class file has wrong version 61.0, should be 52.0`。

## 反射 adapter 属性规则

独立反射 adapter 会按以下顺序创建 store：

1. 查找 LangChain4j store class 的静态 `builder()` 方法。
2. 将 `properties` 中的每个 key 映射到同名 builder 方法。
3. 自动补充 `dimension` 和 `metadataKeys` 到 builder，前提是 `properties` 没有显式配置同名字段。
4. 调用 builder 的 `build()` 方法。
5. 如果 store 没有 `builder()`，则优先使用无参构造；如需有参构造，配置 `properties.constructorArgs`。

支持的简单类型转换：

- `String`
- `Integer` / `int`
- `Long` / `long`
- `Double` / `double`
- `Float` / `float`
- `Boolean` / `boolean`
- `Enum`
- `Collection`
- `Duration`

实际生产配置建议优先使用 LangChain4j store 原生 builder 支持的字段。

`constructorArgs` 只适合程序化创建 `EmbeddingStoreDefinition` 时传入真实对象参数。对于构造函数需要 SDK client 对象的 store，例如 `TablestoreEmbeddingStore(SyncClient, int)`，不建议只靠 YAML 字符串配置；更稳妥的方式是注册一个自定义 `EmbeddingStoreProviderAdapter` Bean，在 adapter 内部从 Spring 容器取得 SDK client 并创建 store。

部分专用 adapter 支持通过 bean 名称引用复杂对象：

- `azure-cosmos-nosql`：`cosmosClientBean`，可选 `cosmosVectorEmbeddingPolicyBean`、`cosmosVectorIndexesBean`、`containerPropertiesBean`
- `azure-ai-search`：可选 `tokenCredentialBean`、`indexBean`、`filterMapperBean`
- `oracle`：`dataSourceBean`
- `tablestore`：`clientBean`，可选 `metadataSchemaListBean`

## Magic-API 插件

统一向量插件通过 `fw-plus-ai-magicapi-plugin-vector` 暴露向量能力，并复用本 starter/自动配置创建的 `EmbeddingStoreRegistry`。

Spring Boot 配置里的 `default-store` 是 Magic-API 侧的默认向量数据源。也就是说，脚本里不带 key 调用时，使用的是 `application.yml` 中配置的默认 store：

如业务侧需要 Magic-API 向量插件，同时引入：

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-magicapi-plugin-vector</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
```

脚本示例：

```js
import vector;

// 使用 Spring Boot 配置的默认向量库
var defaultStore = vector.getEmbeddingStore();

// 使用指定 key 的向量库；优先运行时 vector-datasource，找不到再回退 Spring Boot 配置
var knowledgeStore = vector['knowledge'].getEmbeddingStore();
```

Spring Boot 默认数据源配置示例：

```yaml
fw-plus:
  ai:
    langchain4j:
      embedding-store:
        default-store: knowledge
        stores:
          knowledge:
            provider: in-memory
```

Magic-API 控制台中新增的 `vector-datasource` 会保存为 `vector-datasource/*.json`，用于运行时追加或覆盖指定 key 的向量库；它不会改变 `vector.getEmbeddingStore()` 使用的 Spring Boot 默认数据源。

`vector-datasource/faq.json` 示例：

```json
{
  "name": "FAQ 向量库",
  "key": "faq",
  "provider": "qdrant",
  "dimension": 768,
  "properties": {
    "host": "127.0.0.1",
    "port": 6334,
    "collectionName": "faq",
    "useTls": false
  }
}
```

数据源测试接口：

```text
POST /vector/datasource/test
```

请求体示例：

```json
{
  "key": "knowledge",
  "provider": "milvus",
  "dimension": 768,
  "properties": {
    "uri": "http://127.0.0.1:19530",
    "collectionName": "knowledge"
  }
}
```

## 扩展自定义 provider

当某个 LangChain4j store 需要外部 SDK client、复杂认证对象，或 YAML 不能直接表达构造参数时，可以注册自定义 `EmbeddingStoreProviderAdapter` Bean：

```java
@Bean
public EmbeddingStoreProviderAdapter customEmbeddingStoreProviderAdapter() {
    return new EmbeddingStoreProviderAdapter() {
        @Override
        public boolean supports(String provider) {
            return "custom".equalsIgnoreCase(provider);
        }

        @Override
        public EmbeddingStore<TextSegment> create(EmbeddingStoreDefinition definition) {
            return createCustomStore(definition);
        }
    };
}
```

配置中即可使用：

```yaml
fw-plus:
  ai:
    langchain4j:
      embedding-store:
        stores:
          customStore:
            provider: custom
            properties:
              endpoint: http://127.0.0.1:8080
```

## 兼容性

- Java 8
- Spring Boot 2.7.x
- LangChain4j 0.35.0
- 自动配置使用 `META-INF/spring.factories`
- 当前 starter 面向 Spring Boot 2.7 / Java 8，不提供 Spring Boot 3 的 `AutoConfiguration.imports`
