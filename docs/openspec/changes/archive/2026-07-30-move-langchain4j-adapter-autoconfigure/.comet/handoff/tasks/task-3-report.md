# Task 3 Report

## RED

命令：
```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am -DfailIfNoTests=false -Dtest=AiEmbeddingStoreAutoConfigurationMetadataTest,LangChain4jAutoConfigurationRegistrationTest,LangChain4jAdapterAutoConfigurationTest test
```
结果：失败，3 个断言失败，原因是 `spring.factories` 尚未注册 `LangChain4jAdapterAutoConfiguration`，且尚无 imports 资源；元数据加载测试也未发现该配置。

## GREEN

简报命令：
```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am -DfailIfNoTests=false -Dtest=AiEmbeddingStoreAutoConfigurationMetadataTest,LangChain4jAutoConfigurationRegistrationTest,LangChain4jAdapterAutoConfigurationTest#shouldStartWhenOptionalEmbeddingStoreProviderSdksAreMissing+shouldDeclareClassConditionsOnDirectLinkedEmbeddingStoreProviderBeans test
```
结果：BUILD SUCCESS，10 个测试零失败。完整三测试命令复跑结果：BUILD SUCCESS，14 个测试零失败。

## Assertions

- 元数据加载上下文：`openAiChatModelProviderAdapter` 存在；`AiModelAdapter` 为 1；`AiEmbeddingStoreAdapter` 为 1。
- 两个资源中 `LangChain4jAdapterAutoConfiguration` 各出现一次。
- 缺 SDK：无 startup failure；6 类可选 embedding-store provider Bean 未注册；in-memory provider 保留；`AiEmbeddingStoreAdapter` 为 1。

## Commit And Files

- 提交：`dbbdaa83 test(ai): preserve langchain4j dual registration`
- 文件：`spring.factories`、`AutoConfiguration.imports`、`AiEmbeddingStoreAutoConfigurationMetadataTest`、`LangChain4jAutoConfigurationRegistrationTest`、`LangChain4jAdapterAutoConfigurationTest`。

## Risk Signals

- 测试环境报告多个 SLF4J binding（`slf4j-simple` 与 `logback-classic`）；本任务测试均通过，未修改依赖范围。

## Concerns

- 无。
