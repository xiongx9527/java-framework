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
系统 SHALL 在 LangChain4j 聚合和相关 BOM 中注册 `fw-plus-ai-langchain4j-adapter` 及其 adapter starter，并删除四个旧 model/store artifact 的模块、坐标和传递依赖。

#### Scenario: 聚合和 BOM 不再发布旧坐标
- **WHEN** 检查 Core AI 聚合、LangChain4j 聚合和 Core/AI Boot BOM
- **THEN** 它们不得列出四个已删除 artifact。
- **AND** 它们 SHALL 管理统一 adapter 及其 starter。

#### Scenario: 默认 LangChain4j 仍可显式安装
- **WHEN** Boot 2.7 / Java 8 应用引入统一 LangChain4j adapter starter
- **THEN** 它 SHALL 获得模型和向量 store 的 LangChain4j SPI 实现。
- **AND** 它不得传递 Spring AI、Spring AI Alibaba 或 AgentScope 2 Java SDK。

