## REMOVED Requirements

### Requirement: 提供 core 侧 LangChain4j 模型抽象模块
**Reason**: `fw-plus-ai-langchain4j-model` 会继续暴露框架专属的公开坐标和兼容入口，违反统一 adapter 与中立 starter 的依赖边界。

**Migration**: 删除该 artifact；应用改为显式引入 `fw-plus-ai-langchain4j-adapter` 及其 Spring Boot starter。模型定义改用 `fw-plus.ai.model` 中立配置，并在多个 adapter 存在时显式设置 `framework=langchain4j`。

### Requirement: 模型工厂按 provider adapter 创建模型
**Reason**: 该能力随旧模型模块一同迁入统一 LangChain4j adapter。

**Migration**: 使用中立 `AiModelFactory` 与统一 adapter 的 SPI 实现，不再引用 `LangChain4jModelRegistry` 或旧包装类型。

### Requirement: 提供 Spring Boot model starter 和自动配置
**Reason**: 旧 model starter 已被移除，避免中立 starter 隐式传递 LangChain4j。

**Migration**: 使用统一 adapter starter；其框架专属自动配置只在 LangChain4j SDK 和 adapter 同时存在时装配。

## ADDED Requirements

### Requirement: 旧 LangChain4j model 模块和 starter 坐标已移除
系统 SHALL 不再发布 `fw-plus-ai-langchain4j-model` 和 `fw-plus-ai-langchain4j-model-spring-boot-starter` 作为可用坐标；LangChain4j 模型能力 SHALL 由统一 LangChain4j adapter 和中立 `fw-plus.ai.model` 配置承担。

#### Scenario: 应用迁移到统一 adapter starter
- **WHEN** Boot 2.7 / Java 8 应用需要 LangChain4j 模型实现
- **THEN** 应用 SHALL 引入 `fw-plus-ai-langchain4j-adapter-spring-boot-starter`。
- **AND** 不得继续引入已删除的 model artifact。

#### Scenario: 模型创建由中立 factory 和 adapter 承担
- **WHEN** 应用声明模型定义并选择 LangChain4j framework
- **THEN** 中立 `AiModelFactory` SHALL 使用统一 LangChain4j adapter 创建模型门面。
- **AND** 调用方不得引用旧 `LangChain4jModelRegistry` 公开坐标。
