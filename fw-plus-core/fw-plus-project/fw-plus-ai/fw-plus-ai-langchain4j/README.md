# fw-plus-ai-langchain4j Java 8 兼容覆盖说明

## 背景

fw-plus-core 的基线是 Java 8。对 LangChain4j 来说，`0.35.x` 通常是兼顾功能和 Java 8 兼容性的较高可用版本；但本目录下部分扩展模块的 `0.35` 版本源码已经使用了 Java 8 不支持的类或方法。

为保持项目整体 Java 8 兼容性，本目录中的 provider 模块采用与 LangChain4j 原模块相同的包名和类名重写关键类，用本工程内的源码覆盖 `0.35` 中不兼容 Java 8 的实现。
下表中的 `0.35` provider覆盖模块只面向Java 8编译或运行环境；Java 17及以上环境应优先使用对应LangChain4j `0.35`原始artifact。

## 当前已覆盖的类包

以下模块属于 LangChain4j `0.35` Java 8 兼容覆盖模块。覆盖方式是保留上游相同的 `dev.langchain4j.*` 包名和类名，在本工程重新编译出 Java 8 兼容版本。

| 本地模块 | 覆盖的上游 artifact | 覆盖包 | 覆盖类 |
| --- | --- | --- | --- |
| `fw-plus-ai-langchain4j-anthropic` | `langchain4j-anthropic-spring-boot-starter` | `dev.langchain4j.anthropic.spring` | `AutoConfig`, `ChatModelProperties`, `Properties` |
| `fw-plus-ai-langchain4j-azure-ai-search` | `langchain4j-azure-ai-search-spring-boot-starter` | `dev.langchain4j.azure.aisearch.spring` | `AutoConfig`, `Properties` |
| `fw-plus-ai-langchain4j-azure-open-ai` | `langchain4j-azure-open-ai-spring-boot-starter` | `dev.langchain4j.azure.openai.spring` | `AutoConfig`, `ChatModelProperties`, `EmbeddingModelProperties`, `ImageModelProperties`, `Properties` |
| `fw-plus-ai-langchain4j-cassandra` | `langchain4j-cassandra` | `dev.langchain4j.store.embedding.cassandra`, `dev.langchain4j.store.embedding.astradb`, `dev.langchain4j.store.memory.chat.cassandra` | `CassandraEmbeddingStore`, `AstraDbEmbeddingStore`, `CassandraChatMemoryStore` |
| `fw-plus-ai-langchain4j-core` | `langchain4j-spring-boot-starter` | `dev.langchain4j.spring`, `dev.langchain4j.rag.spring`, `dev.langchain4j.service.spring` | `LangChain4jAutoConfig`, `RagAutoConfig`, `RagProperties`, `RetrievalProperties`, `AiServicesAutoConfig`, `AiServiceScannerProcessor`, `ClassPathAiServiceScanner`, `AiServiceWiringMode`, `AiServiceFactory`, `AiService` |
| `fw-plus-ai-langchain4j-dashscope` | `langchain4j-dashscope-spring-boot-starter` | `dev.langchain4j.dashscope.spring` | `AutoConfig`, `ChatModelProperties`, `EmbeddingModelProperties`, `LanguageModelProperties`, `Properties`, `TokenizerProperties` |
| `fw-plus-ai-langchain4j-elasticsearch` | `langchain4j-elasticsearch-spring-boot-starter` | `dev.langchain4j.store.embedding.elasticsearch.spring` | `ElasticsearchClientHelper`, `ElasticsearchEmbeddingStoreAutoConfiguration`, `ElasticsearchEmbeddingStoreProperties` |
| `fw-plus-ai-langchain4j-github-models` | `langchain4j-github-models-spring-boot-starter` | `dev.langchain4j.model.githubmodels.spring` | `AutoConfig`, `ChatModelProperties`, `EmbeddingModelProperties`, `Properties` |
| `fw-plus-ai-langchain4j-infinispan` | `langchain4j-infinispan` | `dev.langchain4j.store.embedding.infinispan` | `InfinispanEmbeddingStore`, `InfinispanStoreConfiguration`, `LangchainSchemaCreator`, `LangChainInfinispanItem`, `LangChainMetadata`, `LangChainMetadataMarshaller`, `LangChainItemMarshaller` |
| `fw-plus-ai-langchain4j-milvus` | `langchain4j-milvus-spring-boot-starter` | `dev.langchain4j.store.embedding.milvus.spring` | `MilvusEmbeddingStoreAutoConfiguration`, `MilvusEmbeddingStoreProperties` |
| `fw-plus-ai-langchain4j-nebula` | 无（fw-plus 自研扩展，见下方说明） | `dev.langchain4j.store.embedding.nebula`, `dev.langchain4j.store.graph.nebula`, `dev.langchain4j.rag.content.retriever.nebula` | `NebulaEmbeddingStore`, `NebulaEmbeddingUtils`, `NebulaGraph`, `NebulaUtils`, `NebulaException`, `NebulaContentRetriever` |
| `fw-plus-ai-langchain4j-neo4j` | `langchain4j-neo4j` | `dev.langchain4j.store.embedding.neo4j`, `dev.langchain4j.store.graph.neo4j`, `dev.langchain4j.rag.content.retriever.neo4j` | `Neo4jEmbeddingStore`, `Neo4jEmbeddingUtils`, `Neo4jGraph`, `Neo4jException`, `Neo4jContentRetriever` |
| `fw-plus-ai-langchain4j-ollama` | `langchain4j-ollama-spring-boot-starter` | `dev.langchain4j.ollama.spring` | `AutoConfig`, `ChatModelProperties`, `EmbeddingModelProperties`, `LanguageModelProperties`, `Properties` |
| `fw-plus-ai-langchain4j-openai` | `langchain4j-open-ai-spring-boot-starter` | `dev.langchain4j.openai.spring` | `AutoConfig`, `ChatModelProperties`, `EmbeddingModelProperties`, `ImageModelProperties`, `LanguageModelProperties`, `ModerationModelProperties`, `Properties`, `ProxyProperties` |
| `fw-plus-ai-langchain4j-qianfan` | `langchain4j-qianfan-spring-boot-starter` | `dev.langchain4j.qianfan.spring` | `AutoConfig`, `ChatModelProperties`, `EmbeddingModelProperties`, `LanguageModelProperties`, `Properties` |
| `fw-plus-ai-langchain4j-reactor` | `langchain4j-reactor` | `dev.langchain4j.reactor` | `TokenStreamToFluxAdapter` |
| `fw-plus-ai-langchain4j-redis` | `langchain4j-redis-spring-boot-starter` | `dev.langchain4j.store.embedding.redis.spring` | `RedisEmbeddingStoreAutoConfiguration`, `RedisEmbeddingStoreProperties` |
| `fw-plus-ai-langchain4j-vertex-ai-gemini` | `langchain4j-vertex-ai-gemini` | `dev.langchain4j.vertexai.spring` | `AutoConfig`, `ChatModelProperties`, `Properties` |
| `fw-plus-ai-langchain4j-voyage-ai` | `langchain4j-voyage-ai-spring-boot-starter` | `dev.langchain4j.voyageai.spring` | `AutoConfig`, `EmbeddingModelProperties`, `Properties`, `ScoringModelProperties` |

> `fw-plus-ai-langchain4j-nebula` 不是上游覆盖模块。LangChain4j 没有 nebula artifact，该模块是 fw-plus 自研扩展，
> 只沿用 `dev.langchain4j.*` 包名，并与 `fw-plus-ai-langchain4j-neo4j` 保持类结构对称（EmbeddingStore / Graph / ContentRetriever）。

与 Neo4j 模块的行为差异：

- NebulaGraph 没有向量索引，`NebulaEmbeddingStore` 的 `findRelevant(...)` 会扫描该 tag 的点（上限 `maxScanRows`，默认 10000），
  在 Java 侧计算余弦相似度后排序取 topN；向量以 JSON 数组文本存成 tag 的 string 属性。数据量大时应改用专门的向量库。
- NebulaGraph 的 DDL 需要心跳同步，`CREATE TAG` 之后会轮询 `DESCRIBE TAG` 等待生效（超时 `awaitSchemaTimeout`，默认 60 秒）。
- 文档 id 即点 vid，默认生成 32 位不带横线的 UUID，图空间的 `vid_type` 需要容纳该长度，例如 `FIXED_STRING(32)`。
- Nebula 的 schema 归属于图空间，`withBasicAuth(...)` 必须同时指定 `spaceName`；`NebulaGraph` 只能采样数据推断边的起点/终点 tag。

## Java 8 / Java 17 依赖版本对照

以下版本只针对本目录中做了依赖降级的 Java 8 覆盖模块。Java 8 环境使用本工程覆盖模块及其降级依赖；Java 17 及以上环境应恢复使用 LangChain4j `0.35.0` 原始 artifact 及上游依赖版本。其余覆盖模块没有显式降级依赖，其传递依赖版本统一由 `langchain4j-bom` / Spring Boot BOM 解析。

| 覆盖模块 | Java 8 覆盖依赖 | Java 17 及以上原始依赖 | 降级原因 |
| --- | --- | --- | --- |
| `fw-plus-ai-langchain4j-cassandra` | `com.datastax.oss:java-driver-core:4.17.0` | `com.datastax.astra:astra-db-client:1.2.4` | 上游 cassandra 模块 `maven.compiler.release=11`，`astra-db-client` 无法在 Java 8 运行；改用 `java-driver-core` 直连 Cassandra/Astra |
| `fw-plus-ai-langchain4j-infinispan` | `org.infinispan:*:13.0.22.Final` | `org.infinispan:*:15.0.0.Final` | 上游 infinispan 模块 `maven.compiler.release=17`，且 parent 属性 `infinispan.version=15.0.0.Final`；降回 13.x 以保持 Java 8 兼容 |

## 覆盖方式

这些模块中的源码保留上游 LangChain4j 的原始包名，例如：

- `dev.langchain4j.spring.*`（`dev.langchain4j.spring.LangChain4jAutoConfig`）
- `dev.langchain4j.rag.spring.*`（`RagAutoConfig`、`RagProperties`、`RetrievalProperties`）
- `dev.langchain4j.service.spring.*`（`AiServicesAutoConfig`、`AiServiceScannerProcessor`、`ClassPathAiServiceScanner`、`AiServiceFactory`、`AiService`、`AiServiceWiringMode`）
- `dev.langchain4j.openai.spring.*`
- `dev.langchain4j.store.embedding.*.spring.*`

构建时，本工程会产出配套坐标的 Java 8 兼容类，供上层 `fw-plus-ai-boot-autoconfigure` 和 starter 依赖使用。这样可以避免 Java 8 环境加载到使用高版本 Java API 或高版本字节码的上游实现。Java 17 及以上环境不需要这些覆盖模块，应使用 LangChain4j 原始 artifact。

## 维护规则

1. 修改这些模块时，必须继续保持 Java 8 兼容，不要使用 Java 9+ API，例如 `List.of()`、`Map.of()`、`Optional.or()`、`Stream.toList()` 等。
2. 同步 LangChain4j `0.35` 或后续版本时，应先对比上游同名类的行为差异，再只迁移必要逻辑，避免直接复制会破坏 Java 8 兼容的代码。
3. 包名、类名、Spring Bean 名称和配置属性前缀应尽量与上游保持一致，确保自动配置和 starter 行为可替换。
4. 若新增 LangChain4j 扩展覆盖模块，应同步更新本说明和 `fw-plus-ai-langchain4j-core` 中 `dev.langchain4j.spring.LangChain4jAutoConfig` 的导入及启动条件。
5. 若某个上游模块重新恢复 Java 8 兼容，也需要经过编译、自动配置测试和依赖冲突检查后，才能移除本地覆盖。

## 模型 API 版本边界

本工程的模型扩展锁定 LangChain4j `0.35.0`。`CHAT` 使用
`ChatLanguageModel`，`STREAMING_CHAT` 使用 `StreamingChatLanguageModel`；LangChain4j
`1.x` 使用 `ChatModel` 和 `StreamingChatModel` 命名，当前版本不能引用这些类。

`TextContent`、`ImageContent`、`AudioContent`、`VideoContent` 和
`PdfFileContent` 均表示 Chat 请求可携带的多模态内容，而不是独立模型接口。
`modelType` 保留 `AUDIO`、`VIDEO`、`PDF` 三类 fw-plus 扩展类型，便于 Magic
资源和业务适配器统一登记；但 LangChain4j `0.35.0` 和当前 `1.x` 文档没有
独立的 `AudioModel`、`VideoModel` 或 `PdfModel` 统一接口，默认自动配置不会
为这三类创建内置 provider。需要使用时在业务层自行封装厂商 SDK。

## 验证建议

修改本目录的兼容覆盖类后，至少验证：

```bash
cd fw-plus-core
./mvnw -f fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/pom.xml clean install

cd ../fw-plus-ai
./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-autoconfigure -am test
```

命令必须使用JDK 8。当前worktree的Maven Wrapper运行文件若缺失，可用Maven 3.6.3执行等价命令，但不能跳过unit、Failsafe IT、owner成品或生产classpath门禁。
