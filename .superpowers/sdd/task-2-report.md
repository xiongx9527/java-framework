# Task 2 实施报告

## 状态

完成。

## RED / GREEN 证据

### RED

1. 初始 wrapper 命令：

   ```bash
   cd fw-plus-core && ./mvnw -f fw-plus-project/pom.xml -pl fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter -Dtest=EmbeddingStoreProviderAdapterTest test
   ```

   结果：失败。`fw-plus-core/.mvn/wrapper/maven-wrapper.properties` 和 wrapper jar 缺失，无法启动 Maven Wrapper。

2. 以 JDK 8 的系统 Maven 直接运行新模块测试：

   ```bash
   cd fw-plus-core/fw-plus-project && JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/pom.xml -Dtest=EmbeddingStoreProviderAdapterTest test
   ```

   结果：失败。测试编译报告 `RedisStackEmbeddingStoreProviderAdapter`、`MilvusEmbeddingStoreProviderAdapter`、`ElasticsearchEmbeddingStoreProviderAdapter`、`QdrantEmbeddingStoreProviderAdapter`、`WeaviateEmbeddingStoreProviderAdapter` 均不存在。这是预期的实现前失败。

### GREEN

```bash
cd fw-plus-core/fw-plus-project && JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -q -f fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/pom.xml -Dtest=EmbeddingStoreProviderAdapterTest test
```

结果：通过，`Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`。

```bash
cd fw-plus-core && rg -n "List\\.of|Map\\.of|Set\\.of|copyOf|orElseThrow\\(\\)" fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/main/java
```

结果：无输出，未使用 Java 9+ API。

另执行了 `git diff --check`，结果通过。

## Wrapper 替代说明

简报给出的 wrapper 命令已先执行，但因工作树中的 `fw-plus-core/.mvn/wrapper/` 缺失而不能启动。随后尝试简报指定的 `mvn -f fw-plus-project/pom.xml -pl ...` 替代命令，Maven 不能选择该模块，因为 Task 2 明确禁止修改 `fw-plus-ai-langchain4j` 聚合 POM，而该新模块尚未注册。故使用 JDK 8 系统 Maven 并以 starter 自身 POM 运行同一测试类；Task 3 注册聚合后可恢复使用简报中的 `-pl` 命令。

## 修改文件

- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/pom.xml`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/autoconfigure/provider/ProviderAdapterSupport.java`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/autoconfigure/provider/RedisStackEmbeddingStoreProviderAdapter.java`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/autoconfigure/provider/MilvusEmbeddingStoreProviderAdapter.java`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/autoconfigure/provider/ElasticsearchEmbeddingStoreProviderAdapter.java`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/autoconfigure/provider/QdrantEmbeddingStoreProviderAdapter.java`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/autoconfigure/provider/WeaviateEmbeddingStoreProviderAdapter.java`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/autoconfigure/provider/EmbeddingStoreProviderAdapterTest.java`

## 实现摘要

- 支持 `redisstack`、`redis`、`milvus`、`elasticsearch`、`qdrant`、`weaviate`，均大小写不敏感。
- 仅将简报列出的显式通用配置键映射至 LangChain4j 0.35.0 builder。
- 配置缺失或 builder 失败统一包装为 `EmbeddingStoreException`，消息含 `provider=<provider>` 与 `key=<key>`。
- 对必填键在调用 builder 前校验，测试不会触发远程连接。实际 `RedisEmbeddingStore.builder().build()` 会执行 Redis Stack 的 `FT._LIST`，因此不以“成功构造”作为无网络单元测试断言。
- `langchain4j-weaviate:0.35.0` 已在 LangChain4j BOM 中存在，按继承版本引用，未硬编码 provider artifact 版本。
- LangChain4j Redis 0.35.0 依赖 Jedis 5.0.0；Spring Boot 2.7.18 BOM 会降至不兼容的 Jedis 3.8.0，starter 显式使用 Jedis 5.0.0。Milvus SDK 引入的 `log4j-slf4j-impl` 已排除，避免与 Spring Boot 的 `log4j-to-slf4j` 冲突。

## 提交哈希

- `762b0b8a393dc37f423db99a74d9380318c993ed` - `feat(vector): add embedding store provider adapters`

## 自审结果

- 未修改 OpenSpec、计划、规格、`.comet`、LangChain4j 聚合 POM、BOM、auto-configuration 或 Magic-API 插件。
- 未引入 Spring AI。
- 生产源码未使用 Java 9+ API。
- provider artifact 均从 LangChain4j BOM 继承版本。
- adapter 不做显式网络健康检查；不完整配置在本地校验并返回带上下文的异常。

## 风险信号清单

- 新 starter 尚未加入 Maven 聚合和 BOM，需由 Task 3 完成后才能通过父 POM 的 `-pl` 方式构建。
- Redis adapter 的 LangChain4j builder 在完整配置时会初始化 Redis Stack 索引并可能建立连接；本实现未新增网络验证，单元测试通过不完整配置确保不连接。
- Weaviate 0.35.0 builder 不提供 `grpcHost` setter，仅支持 host 与 `grpcPort`，故 `grpcHost` 不映射；其余简报显式键按可用 API 映射。
- Elasticsearch builder 在 LangChain4j 0.35.0 标记为 deprecated，编译会出现现有 API 的弃用提示。
