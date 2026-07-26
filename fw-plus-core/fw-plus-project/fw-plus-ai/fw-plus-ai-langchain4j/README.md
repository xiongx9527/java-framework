# fw-plus-ai-langchain4j Java 8 兼容覆盖说明

## 背景

fw-plus-core 的基线是 Java 8。对 LangChain4j 来说，`0.35.x` 通常是兼顾功能和 Java 8 兼容性的较高可用版本；但本目录下部分扩展模块的 `0.35` 版本源码已经使用了 Java 8 不支持的类或方法。

为保持项目整体 Java 8 兼容性，本目录中除 `fw-plus-ai-langchain4j-store-embeding` 之外的模块，采用与 LangChain4j 原模块相同的包名和类名重写关键类，用本工程内的源码覆盖 `0.35` 中不兼容 Java 8 的实现。

## 当前已覆盖的类包

以下模块属于 LangChain4j `0.35` Java 8 兼容覆盖模块。覆盖方式是保留上游相同的 `dev.langchain4j.*` 包名和类名，在本工程重新编译出 Java 8 兼容版本。

| 本地模块 | 覆盖的上游 artifact | 覆盖包 | 覆盖类 |
| --- | --- | --- | --- |
| `fw-plus-ai-langchain4j-anthropic` | `langchain4j-anthropic-spring-boot-starter` | `dev.langchain4j.anthropic.spring` | `AutoConfig`, `ChatModelProperties`, `Properties` |
| `fw-plus-ai-langchain4j-azure-ai-search` | `langchain4j-azure-ai-search-spring-boot-starter` | `dev.langchain4j.azure.aisearch.spring` | `AutoConfig`, `Properties` |
| `fw-plus-ai-langchain4j-azure-open-ai` | `langchain4j-azure-open-ai-spring-boot-starter` | `dev.langchain4j.azure.openai.spring` | `AutoConfig`, `ChatModelProperties`, `EmbeddingModelProperties`, `ImageModelProperties`, `Properties` |
| `fw-plus-ai-langchain4j-core` | `langchain4j-spring-boot-starter` | `dev.langchain4j.spring`, `dev.langchain4j.service.spring`, `dev.langchain4j.rag.spring` | `LangChain4jAutoConfig`, `AiService`, `AiServiceFactory`, `AiServiceScannerProcessor`, `AiServiceWiringMode`, `AiServicesAutoConfig`, `ClassPathAiServiceScanner`, `RagAutoConfig`, `RagProperties`, `RetrievalProperties` |
| `fw-plus-ai-langchain4j-dashscope` | `langchain4j-dashscope-spring-boot-starter` | `dev.langchain4j.dashscope.spring` | `AutoConfig`, `ChatModelProperties`, `EmbeddingModelProperties`, `LanguageModelProperties`, `Properties`, `TokenizerProperties` |
| `fw-plus-ai-langchain4j-elasticsearch` | `langchain4j-elasticsearch-spring-boot-starter` | `dev.langchain4j.store.embedding.elasticsearch.spring` | `ElasticsearchClientHelper`, `ElasticsearchEmbeddingStoreAutoConfiguration`, `ElasticsearchEmbeddingStoreProperties` |
| `fw-plus-ai-langchain4j-github-models` | `langchain4j-github-models-spring-boot-starter` | `dev.langchain4j.model.githubmodels.spring` | `AutoConfig`, `ChatModelProperties`, `EmbeddingModelProperties`, `Properties` |
| `fw-plus-ai-langchain4j-milvus` | `langchain4j-milvus-spring-boot-starter` | `dev.langchain4j.store.embedding.milvus.spring` | `MilvusEmbeddingStoreAutoConfiguration`, `MilvusEmbeddingStoreProperties` |
| `fw-plus-ai-langchain4j-ollama` | `langchain4j-ollama-spring-boot-starter` | `dev.langchain4j.ollama.spring` | `AutoConfig`, `ChatModelProperties`, `EmbeddingModelProperties`, `LanguageModelProperties`, `Properties` |
| `fw-plus-ai-langchain4j-openai` | `langchain4j-open-ai-spring-boot-starter` | `dev.langchain4j.openai.spring` | `AutoConfig`, `ChatModelProperties`, `EmbeddingModelProperties`, `ImageModelProperties`, `LanguageModelProperties`, `ModerationModelProperties`, `Properties`, `ProxyProperties` |
| `fw-plus-ai-langchain4j-qianfan` | `langchain4j-qianfan-spring-boot-starter` | `dev.langchain4j.qianfan.spring` | `AutoConfig`, `ChatModelProperties`, `EmbeddingModelProperties`, `LanguageModelProperties`, `Properties` |
| `fw-plus-ai-langchain4j-reactor` | `langchain4j-reactor` | `dev.langchain4j.reactor` | `TokenStreamToFluxAdapter` |
| `fw-plus-ai-langchain4j-redis` | `langchain4j-redis-spring-boot-starter` | `dev.langchain4j.store.embedding.redis.spring` | `RedisEmbeddingStoreAutoConfiguration`, `RedisEmbeddingStoreProperties` |
| `fw-plus-ai-langchain4j-vertex-ai-gemini` | `langchain4j-vertex-ai-gemini` | `dev.langchain4j.vertexai.spring` | `AutoConfig`, `ChatModelProperties`, `Properties` |
| `fw-plus-ai-langchain4j-voyage-ai` | `langchain4j-voyage-ai-spring-boot-starter` | `dev.langchain4j.voyageai.spring` | `AutoConfig`, `EmbeddingModelProperties`, `Properties`, `ScoringModelProperties` |

## 待新增覆盖的类包

以下 LangChain4j `0.35` artifact 已确认不是 Java 8 字节码，但当前目录尚未落地对应本地覆盖模块：

| 待新增本地模块 | 需覆盖的上游 artifact | 需覆盖包 | 需覆盖类 |
| --- | --- | --- | --- |
| `fw-plus-ai-langchain4j-cassandra` | `langchain4j-cassandra` | `dev.langchain4j.store.embedding.astradb`, `dev.langchain4j.store.embedding.cassandra`, `dev.langchain4j.store.memory.chat.cassandra` | `AstraDbEmbeddingStore`, `CassandraEmbeddingStore`, `CassandraEmbeddingStore.Builder`, `CassandraEmbeddingStore.BuilderAstra`, `CassandraChatMemoryStore`, `CassandraChatMemoryStore.Builder`, `CassandraChatMemoryStore.BuilderAstra` |
| `fw-plus-ai-langchain4j-infinispan` | `langchain4j-infinispan` | `dev.langchain4j.store.embedding.infinispan` | `InfinispanEmbeddingStore`, `InfinispanEmbeddingStore.Builder`, `InfinispanStoreConfiguration`, `LangChainInfinispanItem`, `LangChainItemMarshaller`, `LangChainMetadata`, `LangChainMetadataMarshaller`, `LangchainSchemaCreator` |
| `fw-plus-ai-langchain4j-neo4j` | `langchain4j-neo4j` | `dev.langchain4j.rag.content.retriever.neo4j`, `dev.langchain4j.store.embedding.neo4j`, `dev.langchain4j.store.graph.neo4j` | `Neo4jContentRetriever`, `Neo4jContentRetriever.Neo4jContentRetrieverBuilder`, `Neo4jEmbeddingStore`, `Neo4jEmbeddingStore.Neo4jEmbeddingStoreBuilder`, `Neo4jEmbeddingUtils`, `Neo4jException`, `Neo4jGraph`, `Neo4jGraph.Neo4jGraphBuilder` |

`fw-plus-ai-langchain4j-store-embeding` 是本工程自有的向量存储抽象与适配扩展，不属于上游 LangChain4j 类覆盖范围。

## 覆盖方式

这些模块中的源码保留上游 LangChain4j 的原始包名，例如：

- `dev.langchain4j.spring.*`
- `dev.langchain4j.openai.spring.*`
- `dev.langchain4j.store.embedding.*.spring.*`

构建时，本工程会产出同坐标或配套坐标的 Java 8 兼容类，供上层 `fw-plus-ai-boot-autoconfigure` 和 starter 依赖使用。这样可以避免运行时加载到使用高版本 Java API 的上游实现。

## 维护规则

1. 修改这些模块时，必须继续保持 Java 8 兼容，不要使用 Java 9+ API，例如 `List.of()`、`Map.of()`、`Optional.or()`、`Stream.toList()` 等。
2. 同步 LangChain4j `0.35` 或后续版本时，应先对比上游同名类的行为差异，再只迁移必要逻辑，避免直接复制会破坏 Java 8 兼容的代码。
3. 包名、类名、Spring Bean 名称和配置属性前缀应尽量与上游保持一致，确保自动配置和 starter 行为可替换。
4. 若新增 LangChain4j 扩展覆盖模块，应同步更新本说明和上层 `LangChain4jAutoConfiguration` 的导入及启动条件。
5. 若某个上游模块重新恢复 Java 8 兼容，也需要经过编译、自动配置测试和依赖冲突检查后，才能移除本地覆盖。

## 验证建议

修改本目录的兼容覆盖类后，至少验证：

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core
./mvnw clean install -pl fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j -DskipTests

cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-ai
./mvnw test -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure
```

当前工作区的 Maven Wrapper 若不可用，可按项目实际环境使用 Java 8 的 Maven 命令替代，但最终应确认 Java 8 下编译通过。
