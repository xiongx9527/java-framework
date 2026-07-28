## Why

`fw-plus-ai-langchain4j-model-spring-boot-starter` 已具备统一模型注册能力，但 README 仍停留在最小示例。当前缺少 provider、model-type、Magic-API、LangChain4j 版本边界和多模态说明，使用者容易把 0.35 与高版本模型命名混用。同时，同一个配置节点下多个 `model-name` 需要支持每个模型名自己的参数覆盖。

## What Changes

- 扩充模型 starter README，补充模块定位、依赖策略、配置方式和 Java 获取方式。
- 支持 `model-name` 多值，并通过 `model-configs.<modelName>` 为单个模型名覆盖 `provider`、`model-type`、`base-url`、`api-key` 和 `properties`。
- 列出当前自动配置注册的 provider adapter 与 `LangChain4jModelType`。
- 说明 LangChain4j `0.35.0` 与高版本在 `ChatLanguageModel` / `ChatModel`、多模态内容类型上的差异。
- 说明 Magic-API `model` / `model-datasource` 的使用边界和敏感字段处理。

## Capabilities

### New Capabilities

- 同一模型配置节点可以把 `model-name` 配成列表。
- 每个 `model-name` 可以通过 `model-configs` 拥有自己的参数覆盖。

### Modified Capabilities

- 扩展 Spring Boot 属性绑定和模型注册逻辑。
- 保持旧的单 `model-name` 配置兼容。

## Impact

- 影响文件：模型 starter README、LangChain4j 模型自动配置、属性绑定类和相关测试。
- 不引入 Java 9+ API，不改变默认 provider SDK 依赖策略。
