# langchain4j-model-module Specification

## Purpose
TBD - created by archiving change add-langchain4j-model-magicapi. Update Purpose after archive.
## Requirements
### Requirement: Magic-API 支持动态模型资源和脚本访问
系统 SHALL 为 Magic-API 提供新的 LangChain4j model 插件能力，使脚本可以通过 `model-datasource` 动态资源 key 获取模型，并与 Spring Boot registry 共存。

#### Scenario: Magic 插件注册模型动态资源
- **WHEN** Magic-API 启动且模型插件启用
- **THEN** 系统注册模型动态资源存储和动态注册器。
- **AND** 动态资源类型为 `model-datasource`。
- **AND** 动态资源可以被创建、销毁、列举和按 key 获取。

#### Scenario: Magic 脚本按 key 获取模型
- **WHEN** Magic 脚本通过 model 模块访问已注册的模型 key
- **THEN** 系统返回对应 LangChain4j 模型包装对象或受控访问对象。
- **AND** 该对象可以根据 modelType 暴露 Chat、Streaming Chat、Embedding、Image、Language、Streaming Language、Moderation、Scoring 或自定义扩展模型能力。

#### Scenario: Spring Boot 模型与 Magic 动态模型共存
- **WHEN** 同时存在 Spring Boot 配置模型和 Magic 动态创建模型
- **THEN** 脚本模块可以访问两类模型。
- **AND** 动态模型 key 优先级和冲突处理规则在代码中明确实现并通过测试覆盖。

#### Scenario: Magic 控制器测试模型定义
- **WHEN** 调用 Magic 模型插件的测试接口并提交合法模型定义
- **THEN** 控制器使用模型工厂验证模型可创建。
- **AND** 接口返回 Magic-API 风格的成功响应。

#### Scenario: Magic 控制器隐藏敏感字段
- **WHEN** Magic 模型插件返回模型资源列表或测试失败日志
- **THEN** 响应和日志不得明文暴露 apiKey、secretKey、accessToken 等敏感字段。

### Requirement: 替换旧 Magic LangChain4j 插件实现并保持非 Magic 行为兼容
系统 SHALL 在新增模型模块和 Magic model 插件能力时删除或替换旧 Magic-API LangChain4j 插件实现，同时保持已有 LangChain4j starter 模块和 `fw-plus-ai-langchain4j-store-embeding` 模块行为兼容。

#### Scenario: 向量存储模块行为不变
- **WHEN** 构建并运行现有 `fw-plus-ai-langchain4j-store-embeding` 相关测试
- **THEN** 现有 EmbeddingStoreFactory、EmbeddingStoreRegistry 和 Magic vector 插件测试继续通过。

#### Scenario: 旧 Magic LangChain4j 实现被删除或替换
- **WHEN** 检查 AI Boot 自动配置和 Magic 插件源码
- **THEN** 旧 `com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j` 下的重复模型工厂、动态数据源、资源存储、控制器和脚本模块实现已被删除或替换。
- **AND** Magic 插件自动配置导入新的 model 插件配置，而不是继续导入旧 LangChain4j 插件配置。

#### Scenario: 旧 Magic 资源和样例迁移到 model 插件
- **WHEN** 检查 Magic 前端插件资源、starter POM 和示例项目依赖
- **THEN** 它们使用新的 model 插件 artifact、`model-datasource` 资源类型和 model 脚本模块。
- **AND** 它们不再要求旧 `fw-plus-ai-magicapi-plugin-langchain4j` 作为运行入口。

#### Scenario: 分层 Maven 构建通过
- **WHEN** 按依赖顺序使用 Maven Wrapper 构建 core 新模型模块和 AI Boot 相关模块
- **THEN** 编译和相关测试均通过。

#### Scenario: 文档说明多模型名和单模型名配置
- **WHEN** 检查 README 或迁移文档
- **THEN** 它说明同一个模型 key 下多个模型名通过 `model-name` 列表配置。
- **AND** 它说明不要在该模型 key 下再嵌套一层 `models` 表达多个模型。
- **AND** 它说明每个模型名自己的参数通过 `model-configs.<modelName>.properties` 配置。

#### Scenario: 文档说明 LangChain4j 版本差异和多模态边界
- **WHEN** 检查 README 或迁移文档
- **THEN** 它说明当前实现以 LangChain4j 0.35.0 编译，Chat 类型使用 `ChatLanguageModel`，Streaming Chat 类型使用 `StreamingChatLanguageModel`。
- **AND** 它说明高版本 LangChain4j 使用 `ChatModel` / `StreamingChatModel`，这些类名不得在 0.35.0 编译代码中使用。
- **AND** 它说明 `ImageContent`、`AudioContent`、`VideoContent`、`PdfFileContent` 等是 Chat 多模态输入内容，不是 LangChain4j 0.35.0 或当前 1.x 的独立 `AudioModel` / `VideoModel` / `PdfModel` 统一接口。
- **AND** 它说明 `AUDIO`、`VIDEO`、`PDF` 是 fw-plus 为 Magic 资源和业务适配器保留的扩展 modelType，使用时需要自定义 `LangChain4jModelProviderAdapter` 接入厂商 SDK 或业务封装。

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
