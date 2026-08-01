## MODIFIED Requirements

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

#### Scenario: Starter 目录内可直接阅读迁移说明
- **WHEN** 开发者只打开 AI model、embedding store 或 LangChain4j adapter starter 目录
- **THEN** 对应 README SHALL 说明当前默认 LangChain4j 实现、`fw-plus.ai.model` 和 `fw-plus.ai.embedding-store` 中立配置入口。
- **AND** README SHALL 说明后续切换到 Spring AI、Spring AI Alibaba 或 AgentScope 2 Java 需要 Java 17 / Spring Boot 3.x adapter starter。
- **AND** README SHALL 说明回滚到默认 LangChain4j 的依赖和 `framework` 配置步骤。
