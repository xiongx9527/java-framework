## ADDED Requirements

### Requirement: AskData Magic-API plugin module
系统 SHALL 在 `fw-plus-ai-magicapi-plugin` 聚合下提供 `fw-plus-ai-magicapi-plugin-askdata` 子模块，并遵循现有 AI Magic-API 插件的 Maven 和自动配置风格。

#### Scenario: Plugin module participates in aggregation
- **WHEN** 开发者构建 `fw-plus-ai-magicapi-plugin` 聚合模块
- **THEN** `fw-plus-ai-magicapi-plugin-askdata` SHALL 作为子模块参与构建

#### Scenario: Starter dependency is reused
- **WHEN** 插件模块编译
- **THEN** 插件 SHALL 依赖 `fw-plus-ai-askdata-spring-boot-starter` 提供的 AskData 服务接口，而不是重复实现核心问数逻辑

### Requirement: Magic-API plugin registration
系统 SHALL 通过 Spring Boot 自动配置注册 AskData Magic-API 插件，并支持 `magic-api.plugin.askdata.enabled` 开关。

#### Scenario: Register plugin when enabled
- **WHEN** `magic-api.plugin.askdata.enabled` 为 `true` 或未显式关闭
- **THEN** 系统 SHALL 注册 AskData 插件配置和脚本模块 Bean

#### Scenario: Disable plugin by property
- **WHEN** `magic-api.plugin.askdata.enabled=false`
- **THEN** 系统 SHALL 不注册 AskData Magic-API 插件模块

### Requirement: AskData script module
系统 SHALL 提供 `@MagicModule("askdata")` 脚本模块，使 Magic-API 脚本可以通过 `import askdata` 调用智能问数能力。

#### Scenario: Import askdata module
- **WHEN** Magic-API 脚本执行 `import askdata`
- **THEN** 脚本 SHALL 获得 AskData 模块对象

#### Scenario: Ask question from script
- **WHEN** 脚本调用 `askdata.ask(request)` 且 request 包含自然语言问题和数据源上下文
- **THEN** 插件 SHALL 调用 `AskDataService` 并返回结构化结果

### Requirement: Script request conversion
系统 SHALL 将 Magic-API 脚本传入的 Map/JSON 参数转换为 AskData 请求模型，并对缺失或非法参数返回明确错误。

#### Scenario: Missing question
- **WHEN** 脚本调用 AskData 方法但未提供问题内容
- **THEN** 插件 SHALL 返回参数错误，说明缺少 question 字段

#### Scenario: Optional execution flag
- **WHEN** 脚本传入 `execute=false`
- **THEN** 插件 SHALL 调用 AskData 服务生成 SQL 但不执行 SQL

### Requirement: Script result shape
系统 SHALL 向 Magic-API 脚本返回稳定的结果结构，包含成功状态、SQL、数据、步骤日志和错误信息。

#### Scenario: Successful result
- **WHEN** AskData 服务返回成功结果
- **THEN** 脚本结果 SHALL 包含 `success=true`、`sql`、`data` 和 `steps` 等字段

#### Scenario: Failed result
- **WHEN** AskData 服务返回失败结果
- **THEN** 脚本结果 SHALL 包含 `success=false` 和 `error` 字段

### Requirement: Existing AI modules remain source of model and vector capabilities
系统 SHALL 通过 AskData Starter 和现有 `fw-plus-ai` 模型/向量模块获得模型管理和向量召回能力，插件自身不得新增平行的模型管理或向量库配置体系。

#### Scenario: Use existing model configuration
- **WHEN** 脚本请求指定模型 key 或使用默认模型
- **THEN** 插件 SHALL 将请求交由 AskData Starter 的模型适配层处理

#### Scenario: Pass requested model key
- **WHEN** 脚本请求包含 `modelKey`
- **THEN** 插件 SHALL 将该模型 key 传递给 AskData 请求模型

#### Scenario: Omit model key
- **WHEN** 脚本请求不包含 `modelKey`
- **THEN** 插件 SHALL 不自行选择模型，而是让 AskData Starter 使用默认模型

#### Scenario: Use existing vector modules
- **WHEN** AskData 需要表结构、术语或训练样例向量召回
- **THEN** 插件 SHALL 通过 AskData Starter 复用现有向量模块能力，而不是直接维护新的向量客户端

### Requirement: AskData vector management console
系统 SHALL 提供 AskData 向量管理控制台模块，参考 `magic-s3.vue` 和 S3 插件前端结构管理智能问数召回资源。

#### Scenario: Register vector management UI
- **WHEN** AskData Magic-API 插件启用
- **THEN** Magic-API 控制台 SHALL 可以加载 AskData 向量管理入口、图标、i18n 和组件资源

#### Scenario: Manage retrieval resources
- **WHEN** 用户在 AskData 向量管理界面维护表结构、术语、训练样例或向量集合引用
- **THEN** 系统 SHALL 将配置保存到 AskData 向量管理资源存储，并可供 AskData 服务召回使用

#### Scenario: No project management UI
- **WHEN** AskData 插件前端加载
- **THEN** 系统 SHALL 不提供项目列表、项目成员或项目权限管理界面

### Requirement: AskData vector management backend
系统 SHALL 提供向量管理后端接口，用于查询、保存、测试和删除 AskData 向量召回配置，并遵循 Magic-API Controller 权限风格。

#### Scenario: List vector resources
- **WHEN** 控制台请求 AskData 向量资源列表
- **THEN** Controller SHALL 返回当前已配置的向量召回资源

#### Scenario: Test vector resource
- **WHEN** 控制台提交向量资源测试请求
- **THEN** Controller SHALL 调用现有 `fw-plus-ai` 向量模块或 AskData Starter 适配层验证配置可用性
