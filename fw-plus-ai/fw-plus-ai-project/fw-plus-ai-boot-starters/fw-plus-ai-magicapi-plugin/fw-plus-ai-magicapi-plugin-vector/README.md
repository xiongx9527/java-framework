# fw-plus-ai-magicapi-plugin-vector

Magic-API 的 LangChain4j 向量存储插件，用于把 Spring Boot 中的 `EmbeddingStoreRegistry` 和 Magic 动态向量资源统一暴露给 MagicScript。业务脚本可以通过 `vector` 模块获取默认或指定 key 的 `EmbeddingStore<TextSegment>`。

本模块目录主要负责 starter 依赖和控制台前端资源打包，后端自动配置实现位于 `fw-plus-ai-boot-autoconfigure`。

## 引入依赖

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-magicapi-plugin-vector</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
```

模块依赖：

- `fw-plus-ai-boot-autoconfigure`：提供 Magic-API 插件后端配置、控制器、资源存储和动态注册。
- `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter`：提供 `EmbeddingStoreRegistry`、向量 store 工厂和 provider adapter。

具体向量数据库 SDK 不由本插件一次性全部引入。使用 Milvus、Qdrant、Weaviate、Redis、Elasticsearch、PGVector 等 provider 时，请在业务应用侧按需引入对应 LangChain4j 实现依赖。

## 启用条件

后端配置类为 `MagicAPIVectorConfiguration`，满足以下条件时生效：

- 当前应用是 Magic-API 所在的 Servlet Web 应用，并启用了 AI Magic-API 基础自动配置。
- classpath 中存在 `EmbeddingStoreRegistry`。
- Spring 容器中已经有 `EmbeddingStoreRegistry` Bean。
- `magic-api.plugin.vector.enabled=true` 或未配置该开关。

关闭插件：

```yaml
magic-api:
  plugin:
    vector:
      enabled: false
```

插件启动后会注册：

- Magic 模块：`vector`
- 动态资源类型：`vector-datasource`
- 控制台插件：`vector` / `MagicVector` / `magic-vector.1.0.0.iife.js`
- 控制器接口：`/vector/datasource/test`

## Spring Boot 向量存储配置

如果向量 store 由 Spring Boot 配置创建，使用 `fw-plus.ai.langchain4j.embedding-store` 前缀：

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
              apiKey: ${MILVUS_API_KEY:}
          faq:
            provider: qdrant
            dimension: 768
            properties:
              host: 127.0.0.1
              port: 6334
              collectionName: faq
              useTls: false
```

`default-store` 指向 registry key。Magic 脚本中不指定 key 时，会使用 registry 的默认向量存储。

## Magic 动态资源

Magic 控制台中新增的数据源类型是 `vector-datasource`，运行态资源通常位于 `vector-datasource/*.json`。示例：

```json
{
  "name": "知识库 Milvus",
  "key": "knowledge",
  "provider": "milvus",
  "enabled": true,
  "dimension": 768,
  "metadataKeys": ["tenantId", "docId"],
  "properties": {
    "uri": "http://127.0.0.1:19530",
    "collectionName": "knowledge",
    "apiKey": "token",
    "retrieveEmbeddingsOnSearch": false,
    "autoFlushOnInsert": true
  }
}
```

字段说明：

- `name`：控制台显示名称。
- `key`：数据源 key，必填且唯一，已有资源不允许修改 key。
- `provider`：向量 store provider，必填。
- `enabled`：是否注册运行态向量存储，默认启用。
- `dimension`：向量维度，按 provider 需要配置。
- `metadataKeys`：元数据字段列表，按 provider builder 支持情况透传。
- `properties`：透传给 LangChain4j store builder 或自定义 provider adapter 的扩展参数。

常见 `properties` 字段包括：`host`、`port`、`uri`、`collectionName`、`apiKey`、`token`、`username`、`password`、`databaseName`、`indexType`、`metricType`、`consistencyLevel`、`retrieveEmbeddingsOnSearch`、`autoFlushOnInsert`、`payloadTextKey`、`useTls`、`utls`、`scheme`、`className`、`useGrpcForInserts`、`securedGrpc`、`grpcPort`、`avoidDups`、`textFieldName`。

### 配置中心覆盖

可通过 `magic.api.vector.<key>.*` 覆盖运行态资源中的 `dimension`、`metadataKeys`、`properties` 和上述常见扩展属性。例如：

```yaml
magic:
  api:
    vector:
      knowledge:
        properties: '{"uri":"http://127.0.0.1:19530","collectionName":"knowledge"}'
        apiKey: token
```

配置中心存在 `properties` JSON 时，其内容会完整替代资源文件中的 `properties`；单独配置的常见扩展属性仍会覆盖该 JSON 中的同名值。

## MagicScript 用法

```javascript
import vector;

// 默认向量存储的中立 facade，来自 Spring Boot registry 的 default-store
var defaultStore = vector.getEmbeddingStore();

// 指定 Spring Boot registry 或 vector-datasource 中的 key
var knowledgeStore = vector['knowledge'].getEmbeddingStore();

// 返回 AiEmbeddingStore facade，只读取稳定元数据：key/framework/provider/type
```

`getEmbeddingStore()` 不再返回 LangChain4j `EmbeddingStore<TextSegment>`，因此旧脚本不能再直接调用
`add`、`search`、`delete` 等原生方法。这是有意的破坏性变更，用于保证后续 framework 切换时
`vector['key']` 的类型边界稳定。需要 LangChain4j 原生写入或检索能力时，应在 Java 应用层通过
`EmbeddingStoreRegistry`/LangChain4j adapter 调用，不能从 Magic-API facade 取得原生对象。未来 Spring AI、
Spring AI Alibaba 或 AgentScope 2 adapter 接入后，`vector['key']` 仍保持不变，`framework` 标识实际实现。
如需紧急回滚旧 MagicScript，请回退到本改造前的插件版本和旧 `EmbeddingStore` 契约。

动态资源与 Spring Boot registry 共存。按 key 获取时会先查 Magic 运行态 `vector-datasource`，再回退到 Spring Boot 的 `EmbeddingStoreRegistry`。默认 store 来自 registry 的 `default-store`。

## 控制器接口

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/vector/datasource/test` | POST | 接收 `EmbeddingStoreDefinition` JSON，校验 `key` 和 `provider` 后调用 `EmbeddingStoreFactory.create(...)` 验证能否创建。 |

测试接口成功返回 Magic-API 标准响应中的 `ok`；失败返回 code `-1` 和具体错误消息。

## 支持的 provider

控制台 provider 下拉包含这些值：

- `in-memory`
- `milvus`
- `qdrant`
- `weaviate`
- `redis`
- `elasticsearch`
- `azure-ai-search`
- `azure-cosmos-mongo-vcore`
- `azure-cosmos-nosql`
- `cassandra`
- `astra-db`
- `chroma`
- `couchbase`
- `mongodb-atlas`
- `opensearch`
- `oracle`
- `pgvector`
- `pinecone`
- `tablestore`
- `vearch`
- `vespa`
- `infinispan`
- `neo4j`

是否可用取决于业务应用是否引入对应 LangChain4j provider jar，以及运行 JDK 是否满足该 provider 要求。

## 前端资源

前端源码位于 `src/console`：

- 包名：`magic-vector`
- 构建：`npm run build`，底层为 `vite build`
- 测试：`npm test`，底层为 `node --test test/*.test.cjs`
- 输出：IIFE library，global name `MagicVector`，文件 `magic-vector.1.0.0.iife.js`

Maven 在 `prepare-package` 阶段执行 `npm install` 和 `npm run build`，并把 `src/console/dist` 复制到 `target/classes/magic-editor/plugins`。

## 注意事项

- 资源类型是 `vector-datasource`，统一通过 `vector` 模块按 key 访问。
- 当前脚本侧返回中立 `AiEmbeddingStore` facade，不声明检索、写入、删除等 SDK 专有 API。
- 老的按 provider 分散的 Magic-API 向量入口应迁移到统一 `vector-datasource` 和 `vector['key']` 访问方式。
- Java 8 应用不要默认引入 Java 17 字节码的 provider jar；需要时在业务应用侧按运行环境显式管理依赖。
