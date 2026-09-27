# fw-plus-ai

## 当前能力

本发布线只保留 LangChain4j Java 8 兼容覆盖模块与 Boot 自动配置：

- `fw-plus-ai-langchain4j`、`fw-plus-ai-langchain4j-*`：LangChain4j `0.35.0` 的 Java 8 兼容 provider 覆盖模块，保持与上游相同的 `dev.langchain4j.*` 包名与类名。
- `fw-plus-ai-boot-autoconfigure`：按 classpath 条件装配 provider 自动配置（总开关 `fw.plus.ai.langchain4j.enabled`），并提供 MCP 服务端、MCP 集群与 Magic-API MCP 插件能力。
- `fw-plus-ai-magicapi-plugin-mcp`：Magic-API MCP 插件。
- 聚合入口：`fw-plus-ai-boot-starter`、`fw-plus-ai-mcp-spring-boot-starter`。

各 provider 自动配置的条件与属性以 `fw-plus-ai-boot-autoconfigure` 中 `LangChain4j*AutoConfiguration` 源码为准。

## 已移除

以下构件与对应能力已从本工程删除，不保留 POM 别名、旧配置前缀或兼容兜底：

| 已删除坐标 | 原用途 |
| --- | --- |
| `fw-plus-ai-model` | 中立模型 API（`AiModel` / `AiChatModel` / `AiEmbeddingModel` 等） |
| `fw-plus-ai-store-embedding` | 中立向量存储 API（`AiEmbeddingStore` 等） |
| `fw-plus-ai-langchain4j-adapter` | 中立 SPI 的 LangChain4j 适配层 |
| `fw-plus-ai-model-spring-boot-starter`、`fw-plus-ai-store-embedding-spring-boot-starter` | 中立 API 的 Boot starter |
| `fw-plus-ai-magicapi-plugin-model`、`fw-plus-ai-magicapi-plugin-vector` | Magic-API model / vector 数据源插件 |

随同移除的还有中立配置前缀 `fw-plus.ai.model` / `fw-plus.ai.embedding-store`、`autoconfigure.model`、`autoconfigure.store.embedding`、`autoconfigure.langchain4j.adapter`、`autoconfigure.magicapi.plugin.model`、`autoconfigure.magicapi.plugin.vector` 等包与相关自动配置。需要直接使用 LangChain4j 时，请按所选 provider 引入对应 SDK，并依赖 `fw-plus-ai-boot-autoconfigure` 完成装配。

Spring AI、Spring AI Alibaba、AgentScope 2 等适配是后续 Java 17 / Spring Boot 3 工作，不是本发布线构件。
