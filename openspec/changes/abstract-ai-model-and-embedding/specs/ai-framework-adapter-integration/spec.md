## ADDED Requirements

### Requirement: 框架 adapter 以独立可选 starter 集成
系统 SHALL 为 LangChain4j 提供完整默认 adapter/starter，并为 Spring AI、Spring AI Alibaba 和 AgentScope 2 Java 提供 Java 17 / Spring Boot 3.x 后续实施文档；本 change 不创建目标 Maven artifact、starter 或自动配置代码。

#### Scenario: 默认 LangChain4j 路径不引入其他 SDK
- **WHEN** 应用只引入既有 LangChain4j starter 或兼容 artifact
- **THEN** LangChain4j SHALL 作为默认完整实现完成模型和向量 store 装配。
- **AND** Spring AI、Spring AI Alibaba 和 AgentScope 2 Java SDK 不得被传递引入。

#### Scenario: Java 17 目标框架后续实现被明确隔离
- **WHEN** 开发者阅读 Spring AI、Spring AI Alibaba 或 AgentScope 2 Java 的后续实施文档
- **THEN** 文档 SHALL 要求未来 artifact 使用 Java 17 / Spring Boot 3.x 独立 parent。
- **AND** 文档 SHALL 要求其不进入 Java 8 默认 LangChain4j 路径的传递依赖图。

#### Scenario: 当前发布不宣称 Java 17 adapter 可启用
- **WHEN** 开发者阅读当前发布说明
- **THEN** 文档 SHALL 明确当前没有 Java 17 artifact、starter、自动配置或原生 model/store adapter。
- **AND** 文档 SHALL 给出后续自动配置拒绝半初始化 Bean 和提供诊断的实现要求。

### Requirement: 同一业务 key 为后续 adapter 切换保留稳定契约
系统 SHALL 让中立定义中的 `framework`、中立 registry key 和 Magic-API 访问 key 保持稳定，使后续真实 Spring AI、Spring AI Alibaba 或 AgentScope 2 Java adapter 可在不改变业务 key 的前提下接入。

#### Scenario: 真实目标 adapter 实现后保持业务 key
- **WHEN** 后续 change 实现 Spring AI、Spring AI Alibaba 或 AgentScope 2 Java 的真实进程内 SDK adapter
- **THEN** 中立定义 SHALL 允许以 `framework` 选择该 adapter。
- **AND** 既有模型/store key、`model[...]` 和 `vector[...]` 访问方式 SHALL 不要求改变。
