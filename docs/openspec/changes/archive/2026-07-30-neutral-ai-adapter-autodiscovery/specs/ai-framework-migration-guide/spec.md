## REMOVED Requirements

### Requirement: 保持旧 LangChain4j 配置的迁移兼容
**Reason**: 旧 `fw-plus.ai.langchain4j.model` 与 `fw-plus.ai.langchain4j.embedding-store` 配置及其四个依赖坐标将被明确删除；继续静默读取会掩盖不兼容升级。

**Migration**: 将配置迁移到 `fw-plus.ai.model` 和 `fw-plus.ai.embedding-store`，显式安装所需 adapter，并在多个 adapter 可用时配置 framework。需要无代码回滚时使用包含旧模块的上一发布版本。

## ADDED Requirements

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
