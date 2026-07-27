## ADDED Requirements

### Requirement: 框架 adapter 以独立可选 starter 集成
系统 SHALL 为 LangChain4j 提供完整默认 adapter/starter，并为 Spring AI、Spring AI Alibaba 和 AgentScope 2 Java 提供 Java 17+ / Spring Boot 3+ 独立 Maven artifact 与可验证 starter 骨架；中立 starter 只依赖中立核心模块。

#### Scenario: 默认 LangChain4j 路径不引入其他 SDK
- **WHEN** 应用只引入既有 LangChain4j starter 或兼容 artifact
- **THEN** LangChain4j SHALL 作为默认完整实现完成模型和向量 store 装配。
- **AND** Spring AI、Spring AI Alibaba 和 AgentScope 2 Java SDK 不得被传递引入。

#### Scenario: Java 17 目标框架 starter 骨架隔离
- **WHEN** 构建 Spring AI、Spring AI Alibaba 或 AgentScope 2 Java 的首版 starter artifact
- **THEN** 它 SHALL 使用 Java 17+ / Spring Boot 3+ 独立 parent。
- **AND** 它 SHALL 不进入 Java 8 默认 LangChain4j 路径的传递依赖图。

#### Scenario: 首版目标 adapter 未实现时拒绝启用
- **WHEN** 应用尝试使用首版 Java 17 artifact 启用 Spring AI、Spring AI Alibaba 或 AgentScope 2 Java 的原生 model/store adapter
- **THEN** 条件自动配置 SHALL 不创建半初始化 Bean。
- **AND** 启动诊断 SHALL 指出该 artifact 当前仅提供 starter 骨架、真实 adapter 尚未实现及迁移文档入口。

### Requirement: 同一业务 key 为后续 adapter 切换保留稳定契约
系统 SHALL 让中立定义中的 `framework`、中立 registry key 和 Magic-API 访问 key 保持稳定，使后续真实 Spring AI、Spring AI Alibaba 或 AgentScope 2 Java adapter 可在不改变业务 key 的前提下接入。

#### Scenario: 真实目标 adapter 实现后保持业务 key
- **WHEN** 后续 change 实现 Spring AI、Spring AI Alibaba 或 AgentScope 2 Java 的真实进程内 SDK adapter
- **THEN** 中立定义 SHALL 允许以 `framework` 选择该 adapter。
- **AND** 既有模型/store key、`model[...]` 和 `vector[...]` 访问方式 SHALL 不要求改变。
