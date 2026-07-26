# Task 4 Report: Provider Adapter 迁移与 SDK 隔离

## Status

BLOCKED

## RED

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml -Dtest=LangChain4jModelProviderAdapterTest,LangChain4jModelAutoConfigurationTest test
```

结果：`BUILD FAILURE`。JUnit Jupiter 测试在编译阶段按预期失败，原因是 `ReflectiveLangChain4jModelProviderAdapter` 尚不存在。

## GREEN

同一 focused 命令在实现后能够完成主代码和测试代码编译，但 Surefire 2.22.2 报告 `Tests run: 0`，随后以 `No tests were executed!` 失败，未获得 GREEN 证据。

依赖树证据显示：Jupiter API/Platform 为 `5.8.2/1.8.2`，但 Jupiter Engine 被解析为 `5.10.0`。该版本不兼容导致 JUnit Platform provider 无法执行 Jupiter 测试。

最终执行中 `git diff --check` 通过。

## 修改文件

- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model/LangChain4jModelAutoConfiguration.java`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model/provider/ProviderAdapterSupport.java`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model/provider/ReflectiveLangChain4jModelProviderAdapter.java`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model/provider/LangChain4jModelProviderAdapterTest.java`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model/LangChain4jModelAutoConfigurationTest.java`
- `.superpowers/sdd/task-4-report.md`

## 提交

`f01905602ca703012ccd2c785d60baff5626dde7` (`feat: add langchain4j model provider adapters`)

## 自检

- 未创建或修改任何 Magic-API/vector、starter、BOM、README、OpenSpec、plan、progress 文件。
- 测试仅使用 `org.junit.jupiter.api.Test` 和 `org.junit.jupiter.api.Assertions`。
- 未注册 Audio、Video、PDF、Moderation 或 Scoring provider adapter。
- 未引用高版本 `ChatModel` 或 `StreamingChatModel`。

## 风险信号

- 当前用户限定范围禁止修改 POM，因此无法对齐 Jupiter Engine 与 API/Platform 版本，无法让指定 focused 命令实际执行 Jupiter 测试。
- 当前提交缺少 GREEN 证据；需在允许调整测试依赖后重新执行 focused 命令。

## Fix

在 `fw-plus-ai-boot-autoconfigure` 模块内排除 `agents-flex-spring-boot-starter` 经 Groovy 引入的 `org.testng:testng`，并将 `junit-jupiter-api`、`junit-jupiter-engine` 和 `junit-jupiter-params` 显式固定为 Spring Boot 2.7.18 管理的 `${junit-jupiter.version}`（5.8.2）。未修改 Task 4 provider adapter、模型类型矩阵或测试语义。

RED（修复前）：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml -Dtest=LangChain4jModelProviderAdapterTest,LangChain4jModelAutoConfigurationTest -Dsurefire.printSummary=true test
```

关键输出：Surefire 2.22.2 选用 TestNG 后报告 `Tests run: 0`，并以 `No tests were executed!` 失败；排除 TestNG 后的中间复现为 `TestEngine with ID 'junit-jupiter' failed to discover tests`，dump 显示 `NoClassDefFoundError: org/junit/jupiter/api/io/CleanupMode`，对应 5.10.0 Engine 与 5.8.2 API/1.8.2 Platform 混用。

GREEN：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml -Dtest=LangChain4jModelProviderAdapterTest,LangChain4jModelAutoConfigurationTest -Dsurefire.printSummary=true test
```

关键输出：`LangChain4jModelProviderAdapterTest` 执行 3 个测试，`LangChain4jModelAutoConfigurationTest` 执行 3 个测试；汇总为 `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

`git diff --check`：通过。

## Fix Review

### RED

在新增 Qianfan 风格 fake builder 回归测试后，执行以下 focused 测试：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml -Dtest=LangChain4jModelProviderAdapterTest,LangChain4jModelAutoConfigurationTest -Dsurefire.printSummary=true test
```

结果：`BUILD FAILURE`。`shouldNotUseBaseUrlAsEndpointWhenEndpointIsNotConfigured` 断言失败：预期 `endpoint` 为 `null`，实际为 `http://localhost:11434/v1`，证明旧实现将 `baseUrl` 错误回退到 `endpoint`。

### GREEN

移除 `endpoint` 的 `baseUrl` fallback 后，使用同一命令验证通过：`Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

- `endpoint` 仅在 `properties.endpoint` 显式配置时写入；`baseUrl` 仍独立写入 builder。
- `LangChain4jModelAutoConfigurationTest` 逐一构造 brief 中的 provider/modelType 定义，已声明组合断言恰好一个 adapter 支持，未声明组合断言零个支持，覆盖 Qianfan IMAGE 和 Anthropic EMBEDDING 等禁止组合。
- 测试中仍出现 SLF4J 多绑定警告（`slf4j-simple` 与 `logback-classic`），为既存 classpath 噪声，未纳入本次修改。

`git diff --check`：通过。
