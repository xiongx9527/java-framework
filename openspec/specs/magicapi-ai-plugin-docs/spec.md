# magicapi-ai-plugin-docs Specification

## Purpose
规范 AI Magic-API model/vector 插件模块说明文档的最低覆盖范围，确保使用者能从模块 README 直接了解依赖、启用条件、动态资源、脚本入口、测试接口和版本边界。
## Requirements
### Requirement: Model 插件模块说明文档
系统 SHALL 在 `fw-plus-ai-magicapi-plugin-model` 模块目录提供 README，说明 Magic-API model 插件的接入方式和使用边界。

#### Scenario: 文档覆盖 model 插件基础信息
- **WHEN** 用户查看 `fw-plus-ai-magicapi-plugin-model/README.md`
- **THEN** 文档说明模块依赖 `fw-plus-ai-langchain4j-model-spring-boot-starter` 和 `LangChain4jModelRegistry`。
- **AND** 文档说明自动配置开关为 `magic-api.plugin.model.enabled`。
- **AND** 文档说明 Magic 模块名为 `model`，控制台插件文件为 `magic-model.1.0.0.iife.js`。
- **AND** 文档说明动态资源类型为 `model-datasource`。

#### Scenario: 文档覆盖 model 配置与脚本用法
- **WHEN** 用户查看 model 插件 README 的示例
- **THEN** 文档提供 Spring Boot 模型 registry 配置示例。
- **AND** 文档提供 `model-datasource` JSON 示例。
- **AND** 文档说明 `model.getModel()`、`model.getModelObject()` 和 `model['<key>']` 的脚本访问方式。
- **AND** 文档列出 `/model/datasource/list` 和 `/model/datasource/test` 接口。

#### Scenario: 文档覆盖默认模型与多模型配置
- **WHEN** 用户查看 model 插件 README 的多模型说明
- **THEN** 文档说明同一个配置 key 下多个模型名通过 `model-name` 列表配置。
- **AND** 文档说明每个模型名自己的参数通过 `model-configs.<modelName>.properties` 配置。
- **AND** 文档说明 `<配置 key>` 默认别名和 `default-model-name` 的关系。

### Requirement: Vector 插件模块说明文档
系统 SHALL 在 `fw-plus-ai-magicapi-plugin-vector` 模块目录提供 README，说明 Magic-API vector 插件的接入方式和使用边界。

#### Scenario: 文档覆盖 vector 插件基础信息
- **WHEN** 用户查看 `fw-plus-ai-magicapi-plugin-vector/README.md`
- **THEN** 文档说明模块依赖 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter` 和 `EmbeddingStoreRegistry`。
- **AND** 文档说明自动配置开关为 `magic-api.plugin.vector.enabled`。
- **AND** 文档说明 Magic 模块名为 `vector`，控制台插件文件为 `magic-vector.1.0.0.iife.js`。
- **AND** 文档说明动态资源类型为 `vector-datasource`。

#### Scenario: 文档覆盖 vector 配置与脚本用法
- **WHEN** 用户查看 vector 插件 README 的示例
- **THEN** 文档提供 Spring Boot embedding-store registry 配置示例。
- **AND** 文档提供 `vector-datasource` JSON 示例。
- **AND** 文档说明 `vector.getEmbeddingStore()` 和 `vector['<key>'].getEmbeddingStore()` 的脚本访问方式。
- **AND** 文档列出 `/vector/datasource/test` 接口。

### Requirement: 文档保持源码事实一致
系统 SHALL 让新增 README 与当前实现保持一致，不描述尚未实现的运行时代码能力。

#### Scenario: 文档不扩大脚本操作 API
- **WHEN** 文档展示 Magic 脚本调用
- **THEN** model 插件不声明未实现的高级模型操作封装。
- **AND** vector 插件不声明未实现的检索、写入、删除等快捷脚本方法。

#### Scenario: 文档说明 LangChain4j 版本边界
- **WHEN** 文档提到 Chat 和多模态模型类型
- **THEN** 文档说明当前项目以 LangChain4j 0.35.0 编译，Chat 类型使用 `ChatLanguageModel` 和 `StreamingChatLanguageModel`。
- **AND** 文档说明 `AUDIO`、`VIDEO`、`PDF` 是 fw-plus 扩展 modelType，需要自定义 provider adapter 承接厂商 SDK 或业务封装。
