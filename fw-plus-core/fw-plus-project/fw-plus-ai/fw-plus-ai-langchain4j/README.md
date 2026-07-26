# fw-plus-ai-langchain4j Java 8 兼容覆盖说明

## 背景

fw-plus-core 的基线是 Java 8。对 LangChain4j 来说，`0.35.x` 通常是兼顾功能和 Java 8 兼容性的较高可用版本；但本目录下部分扩展模块的 `0.35` 版本源码已经使用了 Java 8 不支持的类或方法。

为保持项目整体 Java 8 兼容性，本目录中除 `fw-plus-ai-langchain4j-store-embeding` 之外的模块，采用与 LangChain4j 原模块相同的包名和类名重写关键类，用本工程内的源码覆盖 `0.35` 中不兼容 Java 8 的实现。

这些覆盖模块只面向 Java 8 编译或运行环境。Java 17 及以上环境应优先使用 LangChain4j `0.35` 原始 artifact，不需要使用本目录的 Java 8 覆盖模块。

## 当前已覆盖的类包

以下模块属于 LangChain4j `0.35` Java 8 兼容覆盖模块。覆盖方式是保留上游相同的 `dev.langchain4j.*` 包名和类名，在本工程重新编译出 Java 8 兼容版本。

| 本地模块 | 覆盖的上游 artifact | 覆盖包 | 覆盖类 |
| --- | --- | --- | --- |
| `fw-plus-ai-langchain4j-anthropic` | `langchain4j-anthropic-spring-boot-starter` | `dev.langchain4j.anthropic.spring` | `AutoConfig`, `ChatModelProperties`, `Properties` |
| `fw-plus-ai-langchain4j-azure-ai-search` | `langchain4j-azure-ai-search-spring-boot-starter` | `dev.langchain4j.azure.aisearch.spring` | `AutoConfig`, `Properties` |
| `fw-plus-ai-langchain4j-azure-open-ai` | `langchain4j-azure-open-ai-spring-boot-starter` | `dev.langchain4j.azure.openai.spring` | `AutoConfig`, `ChatModelProperties`, `EmbeddingModelProperties`, `ImageModelProperties`, `Properties` |
| `fw-plus-ai-langchain4j-cassandra` | `langchain4j-cassandra` | `dev.langchain4j.store.embedding.astradb`, `dev.langchain4j.store.embedding.cassandra`, `dev.langchain4j.store.memory.chat.cassandra` | `AstraDbEmbeddingStore`, `CassandraEmbeddingStore`, `CassandraEmbeddingStore.Builder`, `CassandraEmbeddingStore.BuilderAstra`, `CassandraChatMemoryStore`, `CassandraChatMemoryStore.Builder`, `CassandraChatMemoryStore.BuilderAstra` |
| `fw-plus-ai-langchain4j-core` | `langchain4j-spring-boot-starter` | `dev.langchain4j.spring`, `dev.langchain4j.service.spring`, `dev.langchain4j.rag.spring` | `LangChain4jAutoConfig`, `AiService`, `AiServiceFactory`, `AiServiceScannerProcessor`, `AiServiceWiringMode`, `AiServicesAutoConfig`, `ClassPathAiServiceScanner`, `RagAutoConfig`, `RagProperties`, `RetrievalProperties` |
| `fw-plus-ai-langchain4j-dashscope` | `langchain4j-dashscope-spring-boot-starter` | `dev.langchain4j.dashscope.spring` | `AutoConfig`, `ChatModelProperties`, `EmbeddingModelProperties`, `LanguageModelProperties`, `Properties`, `TokenizerProperties` |
| `fw-plus-ai-langchain4j-elasticsearch` | `langchain4j-elasticsearch-spring-boot-starter` | `dev.langchain4j.store.embedding.elasticsearch.spring` | `ElasticsearchClientHelper`, `ElasticsearchEmbeddingStoreAutoConfiguration`, `ElasticsearchEmbeddingStoreProperties` |
| `fw-plus-ai-langchain4j-github-models` | `langchain4j-github-models-spring-boot-starter` | `dev.langchain4j.model.githubmodels.spring` | `AutoConfig`, `ChatModelProperties`, `EmbeddingModelProperties`, `Properties` |
| `fw-plus-ai-langchain4j-infinispan` | `langchain4j-infinispan` | `dev.langchain4j.store.embedding.infinispan` | `InfinispanEmbeddingStore`, `InfinispanEmbeddingStore.Builder`, `InfinispanStoreConfiguration`, `LangChainInfinispanItem`, `LangChainItemMarshaller`, `LangChainMetadata`, `LangChainMetadataMarshaller`, `LangchainSchemaCreator` |
| `fw-plus-ai-langchain4j-milvus` | `langchain4j-milvus-spring-boot-starter` | `dev.langchain4j.store.embedding.milvus.spring` | `MilvusEmbeddingStoreAutoConfiguration`, `MilvusEmbeddingStoreProperties` |
| `fw-plus-ai-langchain4j-neo4j` | `langchain4j-neo4j` | `dev.langchain4j.rag.content.retriever.neo4j`, `dev.langchain4j.store.embedding.neo4j`, `dev.langchain4j.store.graph.neo4j` | `Neo4jContentRetriever`, `Neo4jContentRetriever.Neo4jContentRetrieverBuilder`, `Neo4jEmbeddingStore`, `Neo4jEmbeddingStore.Neo4jEmbeddingStoreBuilder`, `Neo4jEmbeddingUtils`, `Neo4jException`, `Neo4jGraph`, `Neo4jGraph.Neo4jGraphBuilder` |
| `fw-plus-ai-langchain4j-ollama` | `langchain4j-ollama-spring-boot-starter` | `dev.langchain4j.ollama.spring` | `AutoConfig`, `ChatModelProperties`, `EmbeddingModelProperties`, `LanguageModelProperties`, `Properties` |
| `fw-plus-ai-langchain4j-openai` | `langchain4j-open-ai-spring-boot-starter` | `dev.langchain4j.openai.spring` | `AutoConfig`, `ChatModelProperties`, `EmbeddingModelProperties`, `ImageModelProperties`, `LanguageModelProperties`, `ModerationModelProperties`, `Properties`, `ProxyProperties` |
| `fw-plus-ai-langchain4j-qianfan` | `langchain4j-qianfan-spring-boot-starter` | `dev.langchain4j.qianfan.spring` | `AutoConfig`, `ChatModelProperties`, `EmbeddingModelProperties`, `LanguageModelProperties`, `Properties` |
| `fw-plus-ai-langchain4j-reactor` | `langchain4j-reactor` | `dev.langchain4j.reactor` | `TokenStreamToFluxAdapter` |
| `fw-plus-ai-langchain4j-redis` | `langchain4j-redis-spring-boot-starter` | `dev.langchain4j.store.embedding.redis.spring` | `RedisEmbeddingStoreAutoConfiguration`, `RedisEmbeddingStoreProperties` |
| `fw-plus-ai-langchain4j-vertex-ai-gemini` | `langchain4j-vertex-ai-gemini` | `dev.langchain4j.vertexai.spring` | `AutoConfig`, `ChatModelProperties`, `Properties` |
| `fw-plus-ai-langchain4j-voyage-ai` | `langchain4j-voyage-ai-spring-boot-starter` | `dev.langchain4j.voyageai.spring` | `AutoConfig`, `EmbeddingModelProperties`, `Properties`, `ScoringModelProperties` |

## 待新增覆盖的类包

当前已确认的 LangChain4j `0.35` 非 Java 8 字节码 artifact 均已在本目录落地覆盖模块，暂无待新增项。

## Java 8 / Java 17 依赖版本对照

以下版本只针对本目录新增的 Java 8 覆盖模块。Java 8 环境使用本工程覆盖模块及其降级依赖；Java 17 及以上环境应恢复使用 LangChain4j `0.35.0` 原始 artifact 及上游依赖版本。

| 覆盖模块 | Java 8 覆盖依赖 | Java 17 及以上原始依赖 |
| --- | --- | --- |
| `fw-plus-ai-langchain4j-cassandra` | 不引入 `com.datastax.astra:astra-db-client`；纯 Cassandra 实现使用 `com.datastax.oss:java-driver-core:4.17.0` | 使用 `dev.langchain4j:langchain4j-cassandra:0.35.0`；上游依赖 `com.datastax.astra:astra-db-client:1.2.4`，其父 POM 管理 `com.datastax.oss:java-driver-core:4.17.0`，并直接声明 `com.fasterxml.jackson.core:jackson-core:2.16.1` |
| `fw-plus-ai-langchain4j-infinispan` | 使用 `org.infinispan:infinispan-bom:13.0.22.Final`，覆盖 `infinispan-api`、`infinispan-client-hotrod`、`infinispan-remote-query-client`、`infinispan-query-dsl` | 使用 `dev.langchain4j:langchain4j-infinispan:0.35.0`；上游父 POM 管理 `org.infinispan:infinispan-bom:15.0.0.Final`，对应 Infinispan 依赖均为 `15.0.0.Final` |
| `fw-plus-ai-langchain4j-neo4j` | 使用 `org.neo4j.driver:neo4j-java-driver:4.4.26`；不引入 `org.neo4j:neo4j-cypher-dsl-schema-name-support`，改为本地 Java 8 校验实现 | 使用 `dev.langchain4j:langchain4j-neo4j:0.35.0`；上游依赖 `org.neo4j.driver:neo4j-java-driver:5.15.0` 和 `org.neo4j:neo4j-cypher-dsl-schema-name-support:2023.7.0` |

`fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc` 是本工程自有 JDBC 适配模块，也面向 Java 8，并同样使用 `org.neo4j.driver:neo4j-java-driver:4.4.26`。它与 `fw-plus-ai-langchain4j-neo4j` 的 Java 8 覆盖依赖版本保持一致；但 Java 17 及以上使用 LangChain4j 原始 `langchain4j-neo4j` 时，仍应按上游版本恢复为 Neo4j driver `5.15.0` 和 schema name support `2023.7.0`。

## Cassandra / AstraDB 兼容边界

`langchain4j-cassandra` 上游 `0.35.0` 依赖 `com.datastax.astra:astra-db-client:1.2.4`。经 Maven Central 发布版本扫描，`astra-db-client` 从 `0.7.1` 到 `1.2.7` 均为 Java 11 字节码（class major 55），没有 Java 8 字节码版本。

因此本工程不重写 `astra-db-client`，也不在 Java 8 覆盖模块中引入它：

- `dev.langchain4j.store.embedding.cassandra.CassandraEmbeddingStore` 使用 `com.datastax.oss:java-driver-core:4.17.0` 重写为 Java 8 可编译的纯 Cassandra 版本。
- `dev.langchain4j.store.memory.chat.cassandra.CassandraChatMemoryStore` 使用 `com.datastax.oss:java-driver-core:4.17.0` 重写为 Java 8 可编译的纯 Cassandra 版本。
- `dev.langchain4j.store.embedding.astradb.AstraDbEmbeddingStore` 保留同名类作为 Java 8 占位入口，但 AstraDB 功能在 Java 8 下不可用，调用时会明确抛出 `UnsupportedOperationException`。
- `builderAstra()` 入口保留为占位入口，Java 8 下调用 `build()` 会明确抛出 `UnsupportedOperationException`。

如运行环境是 Java 17 或以上，请直接使用上游 `dev.langchain4j:langchain4j-cassandra:0.35.0` 和 `astra-db-client` 原始实现。

## Infinispan 兼容边界

`langchain4j-infinispan` 上游 `0.35.0` 使用 Infinispan `15.0.0.Final`，该版本为 Java 17 字节码（class major 61）。Java 8 覆盖模块改用仍支持 Java 8 的 `org.infinispan:13.0.22.Final` 依赖，并将上游 record 类改写为普通 Java 类。

如运行环境是 Java 17 或以上，请直接使用上游 `dev.langchain4j:langchain4j-infinispan:0.35.0` 和 Infinispan 15 原始实现。

## Neo4j 兼容边界

`langchain4j-neo4j` 上游 `0.35.0` 使用 `org.neo4j.driver:neo4j-java-driver:5.15.0` 和 `neo4j-cypher-dsl-schema-name-support:2023.7.0`，两者均为 Java 17 字节码。Java 8 覆盖模块改用仍支持 Java 8 的 `org.neo4j.driver:neo4j-java-driver:4.4.26`，并将上游 Java 17 语法和 schema-name sanitizing 依赖改写为 Java 8 实现。

如运行环境是 Java 17 或以上，请直接使用上游 `dev.langchain4j:langchain4j-neo4j:0.35.0` 和 Neo4j 5 原始实现。

`fw-plus-ai-langchain4j-store-embeding` 是本工程自有的向量存储抽象与适配扩展，不属于上游 LangChain4j 类覆盖范围。

## 覆盖方式

这些模块中的源码保留上游 LangChain4j 的原始包名，例如：

- `dev.langchain4j.spring.*`
- `dev.langchain4j.openai.spring.*`
- `dev.langchain4j.store.embedding.*.spring.*`
- `dev.langchain4j.store.embedding.cassandra.*` / `dev.langchain4j.store.embedding.astradb.*`
- `dev.langchain4j.store.embedding.infinispan.*`
- `dev.langchain4j.store.embedding.neo4j.*` / `dev.langchain4j.store.graph.neo4j.*` / `dev.langchain4j.rag.content.retriever.neo4j.*`

构建时，本工程会产出配套坐标的 Java 8 兼容类，供上层 `fw-plus-ai-boot-autoconfigure` 和 starter 依赖使用。这样可以避免 Java 8 环境加载到使用高版本 Java API 或高版本字节码的上游实现。Java 17 及以上环境不需要这些覆盖模块，应使用 LangChain4j 原始 artifact。

## 维护规则

1. 修改这些模块时，必须继续保持 Java 8 兼容，不要使用 Java 9+ API，例如 `List.of()`、`Map.of()`、`Optional.or()`、`Stream.toList()` 等。
2. 同步 LangChain4j `0.35` 或后续版本时，应先对比上游同名类的行为差异，再只迁移必要逻辑，避免直接复制会破坏 Java 8 兼容的代码。
3. 包名、类名、Spring Bean 名称和配置属性前缀应尽量与上游保持一致，确保自动配置和 starter 行为可替换。
4. 若新增 LangChain4j 扩展覆盖模块，应同步更新本说明和上层 `LangChain4jAutoConfiguration` 的导入及启动条件。
5. 若某个上游模块重新恢复 Java 8 兼容，也需要经过编译、自动配置测试和依赖冲突检查后，才能移除本地覆盖。

## 模型 API 版本边界

本工程的模型扩展锁定 LangChain4j `0.35.0`。`CHAT` 使用
`ChatLanguageModel`，`STREAMING_CHAT` 使用 `StreamingChatLanguageModel`；LangChain4j
`1.x` 使用 `ChatModel` 和 `StreamingChatModel` 命名，当前版本不能引用这些类。

`TextContent`、`ImageContent`、`AudioContent`、`VideoContent` 和
`PdfFileContent` 均表示 Chat 请求可携带的多模态内容，而不是独立模型接口。因此
`modelType` 不新增 `AUDIO`、`VIDEO` 或 `PDF`，也不存在独立的 `AudioModel`、
`VideoModel` 或 `PdfModel`。

## 验证建议

修改本目录的兼容覆盖类后，至少验证：

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core
./mvnw clean install -pl fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j -DskipTests

cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-ai
./mvnw test -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure
```

当前工作区的 Maven Wrapper 若不可用，可按项目实际环境使用 Java 8 的 Maven 命令替代，但最终应确认 Java 8 下编译通过。
