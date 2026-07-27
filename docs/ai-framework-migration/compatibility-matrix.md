# AI 框架兼容矩阵

## 当前构建基线

| 线路 | Java / Spring 基线 | artifact / 父 POM | 当前状态 | 依赖边界 |
| --- | --- | --- | --- | --- |
| 中立模型与向量存储契约 | Java 8 / Spring Boot 2.7.18 | `com.xiongx9527.fw.plus:fw-plus-ai-model`、`com.xiongx9527.fw.plus:fw-plus-ai-store-embedding` | 本变更登记到 Core BOM；实现模块由后续任务创建 | 不得公开或传递依赖 LangChain4j、Spring AI、Spring AI Alibaba、AgentScope 2 Java 类型 |
| 默认 LangChain4j | Java 8 / Spring Boot 2.7.18 | 现有 `fw-plus-ai-langchain4j-*` 模块与 AI Boot starter | 已有默认实现；本变更后续任务负责接入中立契约 | LangChain4j 仅存在于默认适配层；旧 artifact、配置、Magic-API key 保持兼容 |
| Spring AI | Java 17+ / Spring Boot 3+ | 计划中的 `fw-plus-ai-j17` 独立 parent 与 starter | 尚未创建，当前不可执行 | 不得进入 Java 8 Core reactor 或 Core BOM；其 SDK 版本仅在 Java 17 parent 管理 |
| Spring AI Alibaba | Java 17+ / Spring Boot 3+ | 计划中的 `fw-plus-ai-j17` 独立 parent 与 starter | 尚未创建，当前不可执行 | 不得进入 Java 8 Core reactor 或 Core BOM；其 SDK 版本仅在 Java 17 parent 管理 |
| AgentScope 2 Java | Java 17+ / Spring Boot 3+ | 计划中的 `fw-plus-ai-j17` 独立 parent 与 starter | 尚未创建，当前不可执行 | 按进程内 SDK 适配；不得进入 Java 8 Core reactor 或 Core BOM；其 SDK 版本仅在 Java 17 parent 管理 |

## 隔离规则

1. Core BOM 只管理 Java 8 可用的内部中立 artifact；不得写入 Spring Boot 3、Spring AI、Spring AI Alibaba 或 AgentScope 2 Java 的依赖或版本。
2. Java 17 适配线路必须在 `fw-plus-ai-j17` 独立 Maven reactor 中构建，并在该 parent 中管理 Spring Boot 3 与目标 SDK 的版本。
3. Java 17 starter 在未提供真实 model/store adapter 前必须给出明确诊断和迁移文档入口；不得创建半初始化 Bean，也不得静默回退到 LangChain4j。

## 验证计划

当前仅验证 Core BOM 中立 artifact 的版本管理和 Java 8 reactor；以下 Java 17 验证命令为后续创建 `fw-plus-ai-j17` 后的目标，当前不会执行成功：

```bash
./fw-plus-core/mvnw -f fw-plus-core/pom.xml validate
./fw-plus-core/mvnw -f fw-plus-ai-j17/pom.xml test
```

完成 Java 17 parent 后，还必须检查其依赖树，并确认 Java 8 Core effective POM 中不存在 Spring Boot 3、Spring AI、Spring AI Alibaba 或 AgentScope 2 Java 坐标。
