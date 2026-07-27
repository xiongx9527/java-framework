# AI 框架兼容矩阵

## 当前构建基线

| 线路 | Java / Spring 基线 | artifact / 父 POM | 当前状态 | 依赖边界 |
| --- | --- | --- | --- | --- |
| 中立模型与向量存储契约 | Java 8 / Spring Boot 2.7.18 | `com.xiongx9527.fw.plus:fw-plus-ai-model`、`com.xiongx9527.fw.plus:fw-plus-ai-store-embedding` | 本变更登记到 Core BOM；实现模块由后续任务创建 | 不得公开或传递依赖 LangChain4j、Spring AI、Spring AI Alibaba、AgentScope 2 Java 类型 |
| 默认 LangChain4j | Java 8 / Spring Boot 2.7.18 | 现有 `fw-plus-ai-langchain4j-*` 模块与 AI Boot starter | 已有默认实现；本变更后续任务负责接入中立契约 | LangChain4j 仅存在于默认适配层；旧 artifact、配置、Magic-API key 保持兼容 |
| Spring AI | Java 17+ / Spring Boot 3+（计划目标） | 计划中的 `fw-plus-ai-j17` 独立 parent 与 starter | 待 Task 6 在 Java 17 独立 reactor 做 SDK 依赖树与编译探针；当前不可执行 | 不得进入 Java 8 Core reactor 或 Core BOM；其 SDK 版本仅在 Java 17 parent 管理 |
| Spring AI Alibaba | Java 17+ / Spring Boot 3+（计划目标） | 计划中的 `fw-plus-ai-j17` 独立 parent 与 starter | 待 Task 6 在 Java 17 独立 reactor 做 SDK 依赖树与编译探针；当前不可执行 | 不得进入 Java 8 Core reactor 或 Core BOM；其 SDK 版本仅在 Java 17 parent 管理 |
| AgentScope 2 Java | Java 17+ / Spring Boot 3+（计划目标） | 计划中的 `fw-plus-ai-j17` 独立 parent 与 starter | 待 Task 6 在 Java 17 独立 reactor 做 SDK 依赖树与编译探针；当前不可执行 | 按进程内 SDK 适配；不得进入 Java 8 Core reactor 或 Core BOM；其 SDK 版本仅在 Java 17 parent 管理 |

## 隔离规则

1. Core BOM 只管理 Java 8 可用的内部中立 artifact；不得写入 Spring Boot 3、Spring AI、Spring AI Alibaba 或 AgentScope 2 Java 的依赖或版本。
2. Java 17 适配线路必须在 `fw-plus-ai-j17` 独立 Maven reactor 中构建，并在该 parent 中管理 Spring Boot 3 与目标 SDK 的版本。
3. Java 17 starter 在未提供真实 model/store adapter 前必须给出明确诊断和迁移文档入口；不得创建半初始化 Bean，也不得静默回退到 LangChain4j。

## 验证计划

当前仅验证 Core BOM 中立 artifact 的版本管理和 Java 8 reactor。中立模块的 `install` 只在 Task 2/Task 3 创建模块后执行；在此之前不得以不存在的 artifact 作为构建成功依据。

### 当前已验证的 Java 8 Core 命令

以下命令在现有 Java 8 Core reactor 中可成功运行，用于验证 Core 构建基线、Core BOM 以及 effective POM 与声明依赖的隔离；`rg` 必须无输出且以零状态退出：

```bash
./fw-plus-core/mvnw -f fw-plus-core/pom.xml validate
./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-dependencies validate
./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-dependencies help:effective-pom -Doutput=target/fw-plus-dependencies-effective-pom.xml
! rg -n -i 'spring-ai|spring-ai-alibaba|agentscope' fw-plus-core/fw-plus-project/fw-plus-dependencies/target/fw-plus-dependencies-effective-pom.xml
! rg -n -i --glob 'pom.xml' 'spring-ai|spring-ai-alibaba|agentscope' fw-plus-core/fw-plus-project
```

### Task 6 Java 17 计划命令

以下命令仅是 Task 6 创建 Java 17 独立 reactor 后的 SDK 依赖树与编译探针。在 `fw-plus-ai-j17` reactor 创建前，因其 `pom.xml` 尚不存在而不能成功运行；这不代表真实 SDK adapter 已实现。

```bash
./fw-plus-core/mvnw -f fw-plus-ai-j17/pom.xml dependency:tree test
```

Task 6 完成 Java 17 parent 后，还必须保留其依赖树与编译探针输出，并确认 Java 8 Core effective POM 中不存在 Spring Boot 3、Spring AI、Spring AI Alibaba 或 AgentScope 2 Java 坐标。
