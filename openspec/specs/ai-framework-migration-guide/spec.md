# ai-framework-migration-guide Specification

## Purpose
TBD - created by archiving change abstract-ai-model-and-embedding. Update Purpose after archive.
## Requirements
### Requirement: 提供从 LangChain4j 切换到目标框架的迁移指南
系统 SHALL 在项目文档中提供从 LangChain4j 切换到 Spring AI、Spring AI Alibaba 和 AgentScope 2 Java 的独立迁移章节。

#### Scenario: 文档覆盖完整切换步骤
- **WHEN** 开发者阅读任一目标框架的迁移章节
- **THEN** 文档 SHALL 说明 Maven artifact/BOM、JDK 与 Spring 基线、starter 选择、中立配置映射、Java registry/API 替换、Magic-API 资源与脚本影响、测试命令和回滚步骤。
- **AND** 文档 SHALL 明确列出目标 adapter 未支持的能力。

#### Scenario: 文档使用稳定业务 key 示例
- **WHEN** 文档展示从 LangChain4j 切换的配置示例
- **THEN** 示例 SHALL 以同一个模型或 store key 展示旧配置、框架无关配置和目标框架选择。
- **AND** 示例 SHALL 说明 `model[...]`、`vector[...]` 与既有动态资源 key 如何保持稳定。

### Requirement: 保持旧 LangChain4j 配置的迁移兼容
系统 SHALL 在兼容周期内继续读取 `fw-plus.ai.langchain4j.model` 和 `fw-plus.ai.langchain4j.embedding-store`。

#### Scenario: 仅存在旧配置
- **WHEN** 应用只提供旧 LangChain4j 配置
- **THEN** 系统 SHALL 将其映射为 `framework=langchain4j` 的中立定义并保持原有行为。

#### Scenario: 中立配置与旧配置存在相同 key
- **WHEN** 同一 key 同时存在中立配置和旧 LangChain4j 配置
- **THEN** 中立配置 SHALL 优先。
- **AND** 系统 SHALL 输出不含敏感字段的迁移提示。
