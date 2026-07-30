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

### Requirement: 文档说明破坏性升级与 Java 17 后续实施
系统 SHALL 用中文文档说明被删除 artifact、配置前缀、替代 adapter、自动发现失败诊断、Magic-API key 保持边界、测试命令和回滚限制，并提供 Java 17 / Spring Boot 3.x adapter 的实施蓝图。

#### Scenario: 文档提供可执行的 LangChain4j 升级路径
- **WHEN** 开发者从旧 LangChain4j model/store 模块升级
- **THEN** 文档 SHALL 给出旧依赖和配置到统一 adapter/中立配置的逐项映射。
- **AND** 文档 SHALL 说明 `model[...]`、`vector[...]` 的业务 key 可保持稳定但框架原生 API 不兼容。

#### Scenario: 文档隔离 Java 17 适配器实现
- **WHEN** 开发者计划接入 Spring AI、Spring AI Alibaba 或 AgentScope 2 Java
- **THEN** 文档 SHALL 指定独立 Java 17 / Boot 3.x reactor、BOM、`jakarta.*` 迁移和 adapter SPI 测试要求。
- **AND** 文档 SHALL 声明当前 Java 8 / Boot 2.7 release 不提供这些 adapter。
