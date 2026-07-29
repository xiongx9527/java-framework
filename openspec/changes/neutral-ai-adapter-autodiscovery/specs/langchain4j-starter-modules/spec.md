## REMOVED Requirements

### Requirement: 暴露指定 LangChain4j starter 本地模块
**Reason**: 该要求继续把已删除的 LangChain4j store starter 作为聚合与 BOM 的公开模块，和统一 adapter 方案冲突。

**Migration**: LangChain4j 聚合保留 provider/vector-store 专用模块，但删除旧 model/store artifact；新增统一 `fw-plus-ai-langchain4j-adapter` 与其 starter，且仅该 adapter 层依赖 LangChain4j。

## ADDED Requirements

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
