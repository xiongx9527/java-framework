# AI 框架兼容矩阵

## 当前构建基线

| 线路 | Java / Spring 基线 | artifact / 父 POM | 当前状态 | 依赖边界 |
| --- | --- | --- | --- | --- |
| 中立模型与向量存储契约 | Java 8 / Spring Boot 2.7.18 | `com.xiongx9527.fw.plus:fw-plus-ai-model`、`com.xiongx9527.fw.plus:fw-plus-ai-store-embedding` | 已在 Java 8 Core reactor 创建、测试并安装，且已登记到 Core BOM | 不得公开或传递依赖 LangChain4j、Spring AI、Spring AI Alibaba、AgentScope 2 Java 类型 |
| LangChain4j Adapter | Java 8 / Spring Boot 2.7.18 | `com.xiongx9527.fw.plus:fw-plus-ai-langchain4j-adapter` 与 `com.xiongx9527.fw.plus.ai:fw-plus-ai-langchain4j-adapter-spring-boot-starter` | 通过统一 Adapter 由业务显式安装；AI Boot 回归验证中立 `fw-plus.ai.model`、`fw-plus.ai.embedding-store` 绑定、显式 framework/唯一候选选择和 Magic-API facade | LangChain4j 仅存在于 Adapter 层；AskData Starter 不默认引入 Adapter、Provider 或 Ollama |
| Spring AI | Java 17 / Spring Boot 3.x 高版本线（计划目标） | 未来 `fw-plus-ai-j17` 独立 parent 与 `fw-plus-ai-spring-ai-j17-starter` | 当前仅有后续实施蓝图；无 artifact、starter、自动配置或真实 adapter | 不得进入 Java 8 Core reactor 或 Core BOM；其 SDK 版本仅在未来 Java 17 parent 管理 |
| Spring AI Alibaba | Java 17 / Spring Boot 3.x 高版本线（计划目标） | 未来 `fw-plus-ai-j17` 独立 parent 与 `fw-plus-ai-spring-ai-alibaba-j17-starter` | 当前仅有后续实施蓝图；无 artifact、starter、自动配置或真实 adapter | 不得进入 Java 8 Core reactor 或 Core BOM；Spring AI Alibaba 与 Spring AI 版本须在未来 Java 17 parent 成组管理 |
| AgentScope 2 Java | Java 17 / Spring Boot 3.x 高版本线（计划目标） | 未来 `fw-plus-ai-j17` 独立 parent 与 `fw-plus-ai-agentscope-j17-starter` | 当前仅有后续实施蓝图；无 artifact、starter、自动配置或真实 adapter | 按进程内 SDK 适配；不得进入 Java 8 Core reactor 或 Core BOM；其 SDK 版本仅在未来 Java 17 parent 管理 |

## 隔离规则

1. Core BOM 只管理 Java 8 可用的内部中立 artifact；不得写入 Spring Boot 3、Spring AI、Spring AI Alibaba 或 AgentScope 2 Java 的依赖或版本。
2. Java 17 适配线路是未来高版本线，实施时必须在 `fw-plus-ai-j17` 独立 Maven reactor 中以 Java 17 和 Spring Boot 3.x 构建，并在独立 parent 中管理目标 SDK 的版本；当前不得创建或引用这些坐标。
3. 未来 Java 17 starter 只有在 SDK 版本探针、真实 adapter 和条件装配测试完成后才能发布；缺 SDK、adapter、provider 或必要配置时不得创建半初始化 Bean，也不得静默回退到 LangChain4j。
4. Spring Boot 2.7 可以运行在 Java 17；本变更选择 Spring Boot 3.x 是为了 Spring AI、Spring AI Alibaba 与 AgentScope 2 Java 高版本生态的独立适配，不是 Java 17 对 Boot 2.x 的强制替换。
5. Java 17 adapter 必须绑定 Java 8 中立 `AiModelAdapter`/`AiEmbeddingStoreAdapter` SPI，原生 SDK 类型不得进入中立 artifact 的公开签名。

## 验证计划

当前验证 Java 8 Core reactor、Core BOM 中立 artifact 的版本管理，以及已创建的 `fw-plus-ai-model`、`fw-plus-ai-store-embedding` 的测试与安装；AI Boot 回归验证业务显式安装 LangChain4j Adapter、中立配置绑定、自动发现选择和 Magic-API facade。不得将这些 Java 8 验证结果延伸为 Java 17 SDK、`fw-plus-ai-j17` reactor 或目标 SDK Adapter 已实现的证据。

### 当前已验证的 Java 8 Core 命令

以下命令在现有 Java 8 Core reactor 中可成功运行，用于验证 Core 构建基线、Core BOM、已创建的中立模块、模块测试与安装，以及 effective POM 与声明依赖的隔离；`rg` 必须无输出且以零状态退出：

```bash
./fw-plus-core/mvnw -f fw-plus-core/pom.xml validate
./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-dependencies validate
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-model test
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding test
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-model -am install -DskipTests
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding -am install -DskipTests
./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-dependencies help:effective-pom -Doutput=target/fw-plus-dependencies-effective-pom.xml
! rg -n -i 'spring-ai|spring-ai-alibaba|agentscope' fw-plus-core/fw-plus-project/fw-plus-dependencies/target/fw-plus-dependencies-effective-pom.xml
! rg -n -i --glob 'pom.xml' 'spring-ai|spring-ai-alibaba|agentscope' fw-plus-core/fw-plus-project
```

### 当前已验证的 AI Boot 接入命令

以下命令验证业务显式选择的 LangChain4j Adapter 已接入中立契约，且中立前缀、显式 framework、唯一候选选择与 Magic-API `model[...]`、`vector[...]` facade 符合破坏性迁移边界：

```bash
./fw-plus-ai/mvnw -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am test \
  -Dtest=LangChain4jModelAutoConfigurationTest,EmbeddingStoreAutoConfigurationTest,LangChain4jAdapterAutoConfigurationTest,MagicAPIModelAutoConfigurationIntegrationTest,MagicAPIVectorAutoConfigurationIntegrationTest
```

### 未来 Java 17 实施命令

以下命令只是后续 change 创建 Java 17 独立 reactor 后的 SDK 依赖树、编译和测试模板。在 `fw-plus-ai-j17` reactor 创建前，因其 `pom.xml` 尚不存在而不能成功运行；文档中列出该命令不代表 artifact 或真实 SDK adapter 已实现。

```bash
./fw-plus-core/mvnw -f fw-plus-ai-j17/pom.xml dependency:tree test
```

后续实现 Java 17 parent 时，必须先分别执行 Spring AI、Spring AI Alibaba、AgentScope 2 Java 的 SDK 版本探针，保留依赖树与编译结果；再运行 Boot 3 条件装配、真实 adapter 契约与远端集成测试，并确认 Java 8 Core effective POM 中不存在 Spring Boot 3 或任一目标 SDK 坐标。

## 当前发布边界

本次变更已交付 Java 8 Core 中立 `fw-plus-ai-model`、`fw-plus-ai-store-embedding` 模块及业务显式选择的 LangChain4j Adapter，并完成测试与安装验证；同时交付 Java 17 / Boot 3.x 后续实施蓝图和迁移文档。当前仍不创建 `fw-plus-ai-j17`、其 Maven parent、Starter、自动配置或 Spring AI、Spring AI Alibaba、AgentScope 2 Java 的真实 Adapter。业务 key `model[...]`、`vector[...]` 在本发布线保持稳定；AskData Starter 不默认引入任何具体 AI 框架或 Provider。
