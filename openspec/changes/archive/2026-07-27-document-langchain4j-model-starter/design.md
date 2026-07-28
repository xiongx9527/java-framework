# 设计

本次保留既有模块结构，在模型 starter 文档和自动配置中补齐多模型配置语义。README 按使用路径组织：先说明 starter 的轻量依赖策略，再给出 Spring Boot 配置和 Java 获取方式，随后列出 model-type/provider 支持矩阵，最后补充 Magic-API 插件和版本兼容说明。

同一模型节点继续使用 `model-name` 表达一个或多个模型名。单值配置保持兼容；列表配置展开为 `<key>.<modelName>` 子模型，并把 `<key>` 注册成默认模型名的别名。每个模型名的差异化参数放在 `model-configs.<modelName>` 下，父级 `properties` 先作为共享默认值，子级 `properties` 后覆盖同名字段；子级也可覆盖 `provider`、`model-type`、`base-url` 和 `api-key`。

文档中的类型命名以当前项目锁定的 LangChain4j `0.35.0` 为准，明确 `CHAT` 对应 `ChatLanguageModel`、`STREAMING_CHAT` 对应 `StreamingChatLanguageModel`。同时单独说明高版本 `1.x` 改用 `ChatModel` / `StreamingChatModel`，以及 `AudioContent`、`VideoContent`、`PdfFileContent` 是 Chat 多模态输入内容，不是独立 `AudioModel` / `VideoModel` / `PdfModel`。

Magic-API 部分只描述新入口 `model` 和 `model-datasource`，并标注敏感字段脱敏规则，避免示例泄露真实密钥。
