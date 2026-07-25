## Why

AskData 核心能力需要在 Magic-API 脚本中直接可用，使现有 Magic-API 应用可以通过脚本快速接入智能问数，而不必自行编写 Java Controller 或服务适配层。

本 change 在 `add-askdata-spring-boot-starter` 的基础上新增 Magic-API AI 插件，只做脚本侧暴露与自动配置，保持核心 Text-to-SQL 逻辑由 AskData Starter 承担。

## What Changes

- 新增 `fw-plus-ai-magicapi-plugin-askdata` 模块，作为 `fw-plus-ai-magicapi-plugin` 聚合的子模块。
- 新增 Magic-API 自动配置，遵循现有 `MagicAPILangchain4jConfiguration` 等插件模式。
- 新增 `@MagicModule("askdata")` 脚本模块，使 Magic-API 脚本可以 `import askdata`。
- 暴露脚本方法，用于提交自然语言问题、指定数据源上下文、控制是否执行 SQL，并返回结构化 AskData 结果。
- 参考 `magic-s3.vue` 和现有向量插件，新增 AskData 向量管理模块，用于管理智能问数可用的表结构、术语、训练样例或向量集合引用。
- 新增向量管理后端 Controller 和资源存储，保持 Magic-API 插件已有资源管理风格。
- 提供插件启用配置，使用 `magic-api.plugin.askdata.enabled` 作为 Magic-API 插件开关。
- 依赖 `fw-plus-ai-askdata-spring-boot-starter`，不重复实现核心问数、多 agent 流程、SQL 校验或 SQL 执行逻辑。

## Capabilities

### New Capabilities

- `askdata-magicapi-plugin`: 将 AskData 智能问数能力暴露为 Magic-API 插件，覆盖插件注册、脚本 API、向量管理、配置开关和结果返回。

### Modified Capabilities

- 无。

## Impact

- 影响 Maven 模块：
  - `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/pom.xml`
  - 新增 `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-askdata/`
- 影响自动配置：
  - 新增 `MagicAPIAskDataConfiguration`，注册插件信息和 `AskDataModule` Bean。
  - 需要加入 Spring Boot 2.7 兼容的自动配置注册。
- 影响 Magic-API 脚本能力：
  - 新增 `import askdata` 模块名。
  - 新增脚本侧问数方法和结果模型转换。
- 影响 Magic-API 控制台：
  - 新增 AskData 向量管理组件。
  - 新增插件前端 service、i18n 和图标资源，风格参考 S3 插件。
- 依赖前置 change：
  - `add-askdata-spring-boot-starter` 必须先提供稳定的 `AskDataService` 与请求/结果模型。
