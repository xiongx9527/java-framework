# langchain4j-starter-modules Specification

## Purpose
TBD - created by archiving change add-langchain4j-starter-modules. Update Purpose after archive.
## Requirements
### Requirement: 保持现有 LangChain4j 模块可构建
系统 SHALL 在新增 starter 模块时保留 LangChain4j 聚合下已有的 `core` 和 `openai` 模块。

#### Scenario: LangChain4j 聚合构建成功
- **WHEN** 构建 core 侧 LangChain4j Maven reactor
- **THEN** 现有模块和新增模块都能解析并编译成功。

### Requirement: LangChain4j 聚合暴露统一可选 adapter
系统 SHALL 在 LangChain4j 聚合和相关 BOM 中注册 `fw-plus-ai-langchain4j-adapter` 及其 adapter Starter，并删除四个旧 model/store artifact 以及 `fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure` 的模块、坐标和传递依赖。具体 LangChain4j Spring Boot 自动配置 SHALL 归属现有 `fw-plus-ai-boot-autoconfigure`，并保留在 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.*` 包下。

#### Scenario: 聚合和 BOM 不再发布旧坐标
- **WHEN** 检查 Core AI 聚合、AI Boot 聚合、LangChain4j 聚合和 Core/AI Boot BOM
- **THEN** 它们不得列出四个已删除 model/store artifact 或 `fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure`。
- **AND** 它们 SHALL 管理统一 Core Adapter，并通过 `fw-plus-ai-boot-starter` 暴露自动配置能力。

#### Scenario: 默认 LangChain4j 仍可显式安装
- **WHEN** Boot 2.7 / Java 8 应用引入 `fw-plus-ai-boot-starter` 与 `fw-plus-ai-langchain4j-adapter`
- **THEN** 它 SHALL 通过 `fw-plus-ai-boot-autoconfigure` 获得模型和向量 store 的 LangChain4j 条件自动配置。
- **AND** 它 SHALL 获得 `fw-plus-ai-langchain4j-adapter` 提供的中立 SPI 实现。
- **AND** 它不得传递 Spring AI、Spring AI Alibaba、AgentScope 2 Java SDK 或未显式允许的 LangChain4j provider SDK。

#### Scenario: 统一自动配置保留包结构和注册行为
- **WHEN** 检查 `fw-plus-ai-boot-autoconfigure` 的源码和 Spring Boot 自动配置元数据
- **THEN** LangChain4j 配置类 SHALL 保留在 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.*` 包下。
- **AND** `LangChain4jAdapterAutoConfiguration` SHALL 通过 Spring Boot 2.7 支持的元数据注册。
- **AND** 缺少可选 provider SDK 时对应 Bean SHALL 不注册且应用上下文 SHALL 可启动。
