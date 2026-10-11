# fw-plus-ai

## 当前能力

本发布线保留 LangChain4j Java 8 兼容覆盖模块与 Boot 自动配置（含 AgentScope 2 条件装配）：

- `fw-plus-ai-langchain4j`、`fw-plus-ai-langchain4j-*`：LangChain4j `0.35.0` 的 Java 8 兼容 provider 覆盖模块，保持与上游相同的 `dev.langchain4j.*` 包名与类名。
- `fw-plus-ai-boot-autoconfigure`：按 classpath 条件装配 provider 自动配置（总开关 `fw.plus.ai.langchain4j.enabled`），并提供 MCP 服务端、MCP 集群与 Magic-API MCP 插件能力；支持按配置注入 `SimpleAssistant` bean（`fw.plus.magicapi.langchain4j.simple-assistant.*`：`enabled` 开关、`key` 默认 key、`keys.<类型>` 分类型专属 key，资源以 MagicDynamic 动态代理实时解析，防 magic 实例热更新/注销后失效）。
- `fw-plus-ai-magicapi-plugin-mcp-server`：Magic-API MCP 插件。
- Magic-API 插件（langchain4j / mcp）的 web 端点支持 Servlet / WebFlux 双栈：WebFlux 应用（`spring.main.web-application-type=reactive`）下由 Boot 层 reactive 引擎装配，端点路径与 Servlet 栈一致。已知限制：WebFlux 下 magic-editor 工作台 WebSocket 在线调试不可用（控制台其余功能正常）。
- AgentScope Java 2（`2.0.4`）：`fw-plus-ai-boot-autoconfigure` 提供移植的 `AgentscopeAutoConfiguration`（官方 `agentscope-spring-boot-starter` v2.0.4 源码，Apache-2.0，位于 `autoconfigure.agentscope` 包），当 classpath 出现 `io.agentscope.core.ReActAgent` 且 `agentscope.agent.enabled=true` 时装配 `Memory` / `Toolkit` / `ReActAgent`。配置前缀保留官方 `agentscope.agent.*` / `agentscope.model.*`；Model bean 由使用方提供（如 `agentscope-extensions-model-ollama` 的 `OllamaChatModel`），本发布线不引入官方各模型 starter。依赖版本由 `fw-plus-ai-boot-dependencies` 钉住（reactor-core 3.8.2 / jackson 2.21.1 / opentelemetry 1.61.0），使 AgentScope 2.0.4 可运行于 Spring Boot 3.5.8。样例：`fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-agentscope-sample`。
- 聚合入口：`fw-plus-ai-boot-starter`、`fw-plus-ai-mcp-server-spring-boot-starter`。

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

AgentScope 2 已不再是后续工作：它已随移植的 `AgentscopeAutoConfiguration`（见上文）进入本 Java 17 / Spring Boot 3 发布线（样例：`fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-agentscope-sample`）。Spring AI / Spring AI Alibaba 仍不在本发布线自动配置范围内。
