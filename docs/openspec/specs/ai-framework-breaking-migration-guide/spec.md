# ai-framework-breaking-migration-guide Specification

## Purpose
TBD - created by archiving change neutral-ai-adapter-autodiscovery. Update Purpose after archive.
## Requirements
### Requirement: 破坏性 AI framework 迁移文档
系统 SHALL 提供中文迁移文档，明确旧 LangChain4j model/store 坐标和旧配置已删除，并说明如何安装 adapter、配置 framework、诊断选择失败和回滚。

#### Scenario: 文档列出删除的 artifact 与替代关系
- **WHEN** 开发者从旧版本升级
- **THEN** 文档 SHALL 列出四个被删除 artifact、统一 LangChain4j adapter 及其 starter 的替代坐标。
- **AND** 文档 SHALL 明确新旧 artifact 不得混用。

#### Scenario: 文档说明自动发现和多 adapter 失败
- **WHEN** 开发者使用缺省 framework 或同时安装多个 adapter
- **THEN** 文档 SHALL 说明唯一候选自动选择、零候选和多候选启动失败，以及显式 framework 的修复示例。

#### Scenario: 文档限定 Java 17 / Boot 3 后续实现
- **WHEN** 开发者评估 Spring AI、Spring AI Alibaba 或 AgentScope 2 Java
- **THEN** 文档 SHALL 说明其 adapter 本次未实现，必须在独立 Java 17 / Spring Boot 3.x reactor 中实现。
- **AND** 文档 SHALL 说明 AgentScope 2 Java 使用进程内 SDK，业务决定是否采用其能力。

#### Scenario: Starter 文档覆盖破坏性边界
- **WHEN** 开发者查看中立 model/store starter 或统一 LangChain4j adapter starter
- **THEN** 文档 SHALL 明确旧 model/store 专用 artifact 和 starter 不应继续使用。
- **AND** 文档 SHALL 说明中立 starter 不携带具体 framework SDK，LangChain4j 由业务显式引入 `fw-plus-ai-langchain4j-adapter`，并由 `fw-plus-ai-boot-starter` 提供自动配置。
- **AND** 文档 SHALL 说明 Spring AI、Spring AI Alibaba 和 AgentScope 2 Java 当前不是已交付实现。
