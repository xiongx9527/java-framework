# Task 2 修复报告：中立 AiModel 模块

Status: DONE_WITH_CONCERNS

## 范围与提交

- 实现提交：`dbdb54fc81294e6cfaf87b96f48d35b3ba0e68a0`（`fw-plus-ai-model: 补全 provider 与失败路径契约`）。
- 修改范围仅为 `fw-plus-ai-model/**` 与本报告；未修改 OpenSpec、聚合 POM、BOM、AI Boot 或 LangChain4j 模块。
- 旧报告中的 LangChain4j Task 2 证据与本任务无关，已从本报告移除；以下证据仅对应中立 `fw-plus-ai-model`。

## 修复内容

- `AiModel` 新增 `getProvider()`，门面现完整保留 key、framework、provider、type/capability 元数据；同步更新 fake 实现及断言。
- 新增 `DefaultAiModelFactoryTest`，覆盖无支持 adapter、`supports` 抛异常、`create` 抛异常、`create` 返回 null 四条失败路径。
- 四条路径均校验异常消息包含 `key=chat`、`framework=fake`、`provider=fake-provider`、`type=CHAT`；存在上游异常时校验原 cause 为同一对象，无上游异常时 cause 为 null。

## RED 证据

1. provider 契约 RED：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-model -Dtest=DefaultAiModelRegistryTest test
```

退出码 `1`；测试编译明确报告 `AiModel.getProvider()` 不存在及 fake 的 `@Override` 无法覆盖，符合缺失接口契约预期。

2. Factory 失败边界 RED：先新增四项契约测试，并按 TDD 要求撤回先于测试存在的未验证错误处理，再运行：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-model -Dtest=DefaultAiModelFactoryTest test
```

退出码 `1`，4/4 按预期失败：无支持 adapter 缺少上下文；`supports` 和 `create` 裸透传 `IllegalStateException`；null 返回未抛异常。

## GREEN 与安装证据

- provider GREEN：同一 `DefaultAiModelRegistryTest` 命令退出码 `0`，3/3 通过。
- Factory GREEN：同一 `DefaultAiModelFactoryTest` 命令退出码 `0`，4/4 通过。
- JDK 8 模块回归：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-model test
```

退出码 `0`，7/7 通过，失败、错误、跳过均为 0。

- 目标模块单独安装：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-model install -DskipTests
```

退出码 `0`。该结果仅证明目标模块可单独安装，不能替代 reactor 安装。

- 计划中的 reactor 安装复核：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-model -am install -DskipTests
```

本次退出码 `0`，8/8 reactor 模块通过，`fw-plus-common` 与 `fw-plus-ai-model` 均实际完成。审查指出的旧执行曾在未修改的 `fw-plus-common` Lombok 编译处失败且未到达目标模块；该历史失败不再被目标模块单独安装掩盖，也不冒充本次结果。

## 自审与风险

- `AiModel.class` 的 classfile major version 为 `52`；主源码未命中 LangChain4j、Spring AI 或 AgentScope 类型。
- `getProvider()` 是公共接口新增抽象方法：仓库内唯一实现为已同步的测试 fake，但外部或后续 adapter 实现必须同步实现，存在源码/二进制兼容风险，因此状态为 `DONE_WITH_CONCERNS`。
- reactor install 使用 `-DskipTests`，测试结论仅来自前述独立模块 test；两类证据未混用。
- 未修改任务范围之外的既有脏文件。
