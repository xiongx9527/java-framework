# Task 1: Core Model 模块契约报告

## 实现内容

- 新增 `fw-plus-ai-langchain4j-model` Maven 模块，并注册到 LangChain4j 聚合 POM。
- 在 Core BOM 中新增 `fw-plus-ai-langchain4j-model` 依赖管理。
- 新增 `LangChain4jModelDefinition` JavaBean，包含 key、provider、modelType、enabled、baseUrl、apiKey、modelName 与默认空 `LinkedHashMap` properties。
- 新增 `LangChain4jModelType` 六个模型类型枚举值。
- 新增 `LangChain4jModel` 包装对象、provider adapter、factory、registry、exception 公共契约。
- 未实现 `DefaultLangChain4jModelFactory`、`DefaultLangChain4jModelRegistry`，未引入 provider SDK、Spring Boot 自动配置或 Magic model 插件逻辑。

## RED

命令：

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model -am -Dtest=LangChain4jModelDefinitionTest test
```

结果：失败，未进入 Maven 项目解析或测试编译。

失败摘要：当前 worktree 缺失 `fw-plus-core/.mvn/wrapper/maven-wrapper.properties` 和 `fw-plus-core/.mvn/wrapper/maven-wrapper.jar`。Wrapper 下载 JAR 时因目标目录不存在失败，随后报 `java.lang.ClassNotFoundException: org.apache.maven.wrapper.MavenWrapperMain`。因此无法观察到预期的“新模块和类不存在”失败。

## GREEN

命令：

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model -am -Dtest=LangChain4jModelDefinitionTest test
```

结果：失败，失败原因与 RED 相同，Wrapper 未启动，无法验证测试 GREEN。

## 变更文件

- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/pom.xml`
- `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/pom.xml`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/model/LangChain4jModelDefinition.java`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/model/LangChain4jModelType.java`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/model/LangChain4jModel.java`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/model/LangChain4jModelProviderAdapter.java`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/model/LangChain4jModelFactory.java`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/model/LangChain4jModelRegistry.java`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/model/LangChain4jModelException.java`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/test/java/com/xiongx9527/fw/plus/ai/langchain4j/model/LangChain4jModelDefinitionTest.java`

## 自查结果

- `git diff --check` 通过。
- 三个受影响 POM 经 `xmllint --noout` 校验通过。
- 新模块未包含 `DefaultLangChain4jModelFactory`、`DefaultLangChain4jModelRegistry` 或 Java 9+ API。
- 模块仅声明 `langchain4j-core` 与测试依赖，未声明任何具体 provider SDK。

## 风险信号自评

高：Maven Wrapper 文件不在当前 worktree，指定测试命令无法启动，测试 GREEN 未得到验证。代码契约与 POM 结构已完成静态检查，但必须在恢复 `fw-plus-core/.mvn/wrapper/` 后重新运行指定命令。

## Review Fix

### 修复内容

- `LangChain4jModelType` 补齐 LangChain4j 0.35.0 core 中真实存在的八类模型接口：`CHAT`、`STREAMING_CHAT`、`EMBEDDING`、`IMAGE`、`LANGUAGE`、`STREAMING_LANGUAGE`、`MODERATION`、`SCORING`。
- 未增加 `AUDIO`、`VIDEO`、`PDF`；它们属于 Chat 多模态内容类型，不是 0.35.0 的独立模型接口。
- 未引用高版本的 `ChatModel` 或 `StreamingChatModel`。当前 core 契约继续使用 `Object` 包装模型实例，兼容 0.35.0 的 `ChatLanguageModel` 与 `StreamingChatLanguageModel`。
- `LangChain4jModelDefinitionTest` 已从 JUnit 4 迁移至 JUnit Jupiter，继续使用现有 `spring-boot-starter-test`，未引入 `junit:junit`。

### RED

命令：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/pom.xml -Dtest=LangChain4jModelDefinitionTest test
```

结果：失败。测试编译阶段无法解析 `org.junit.Test` 和 `org.junit.Assert`，证明模块的测试依赖仅提供 JUnit Jupiter，JUnit 4 导入不兼容。

### GREEN

命令：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/pom.xml -Dtest=LangChain4jModelDefinitionTest test
```

结果：通过。`LangChain4jModelDefinitionTest` 共运行 2 个测试，`Failures: 0`、`Errors: 0`、`Skipped: 0`。

### 环境限制

- Wrapper 命令 `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model -am -Dtest=LangChain4jModelDefinitionTest test` 仍无法启动：当前 worktree 缺失 `fw-plus-core/.mvn/wrapper/maven-wrapper.properties` 与 `maven-wrapper.jar`，最终报 `ClassNotFoundException: org.apache.maven.wrapper.MavenWrapperMain`。
- 聚合 `-am` 构建受基线中 cassandra、infinispan、neo4j 模块目录缺失影响；该问题不在本 Task 修复范围内，未修改这些模块。GREEN 以本次系统 Maven focused 命令为准。
