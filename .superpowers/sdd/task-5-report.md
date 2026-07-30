# Task 5 实现报告：AskData 向量检索中立化

## 实现内容

- 新增 `AiEmbeddingStoreAskDataVectorRetriever`：通过 `AskDataModelRegistry` 的中立嵌入模型生成向量，使用 `AiEmbeddingStoreRegistry` 解析 `fw.plus.ai.askdata.vector.store` 指定的存储，并将 `AiEmbeddingSearchResult` 中的文档文本和元数据转换为 `AskDataRetrievedDocument`。
- 空问题、空嵌入结果和空搜索结果返回空列表；缺少默认注册表或指定存储时抛出包含“默认向量存储”或存储 key 的清晰诊断。
- `AskDataAutoConfiguration#askDataVectorRetriever` 仅以 `AiEmbeddingStoreRegistry` 和 `AskDataModelRegistry` 作为默认路径输入；移除了默认 LangChain4j 内存 store 及 `ContentRetriever`、`EmbeddingStore<TextSegment>`、`EmbeddingModel` 注入。
- 若中立 registry 或模型 registry 不存在，默认 retriever 保持为空组合；用户自定义 `AskDataVectorRetriever` 仍由 `@ConditionalOnMissingBean` 优先。

## RED / GREEN 证据

- RED 1：`JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml test -Dtest=AiEmbeddingStoreAskDataVectorRetrieverTest,AskDataAutoConfigurationTest`
  - 预期失败：新中立 retriever 类不存在，自动配置仍是三个 LangChain4j provider 参数。
- GREEN 1：同一模块的四个指定测试类运行通过，25 项测试通过。
- RED 2：`JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml test -Dtest=AiEmbeddingStoreAskDataVectorRetrieverTest`
  - 预期失败：缺失 registry 的诊断未包含“默认向量存储”。
- GREEN 2 / 最终：`JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml test -Dtest=AiEmbeddingStoreAskDataVectorRetrieverTest,AskDataVectorRetrieverTest,EmbeddingStoreAskDataVectorRetrieverTest,AskDataAutoConfigurationTest`
  - 27 项测试通过，0 failures，0 errors，0 skipped。

## 变更文件

- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/vector/AiEmbeddingStoreAskDataVectorRetriever.java`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/autoconfigure/AskDataAutoConfiguration.java`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/vector/AiEmbeddingStoreAskDataVectorRetrieverTest.java`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/autoconfigure/AskDataAutoConfigurationTest.java`

## 自审结果

- `git diff --check` 通过。
- 默认向量自动配置和新 retriever 源码均不存在 `dev.langchain4j` 引用。
- 测试覆盖中立结果映射、空问题、缺失指定 store、缺失默认 registry、自定义 retriever 覆盖，以及旧 LangChain4j store/model bean 不参与默认路径。
- 生产代码使用 Java 8 语法与 API。

## 风险信号

- Maven 有既存的重复 dependencyManagement 警告：`fw-plus-ai-model` 与 `fw-plus-ai-store-embedding`；本任务未修改 BOM。
- 测试运行时有既存的多个 SLF4J binding 警告；测试均通过。

## 顾虑

- `fw.plus.ai.askdata.vector.store` 现在是中立 registry 的存储 key；部署方需要确保该 key 与 `AiEmbeddingStoreRegistry` 中已注册的 key 一致。Task 6 的 TinyEngine 知识库真实业务逻辑不在本任务范围内。
