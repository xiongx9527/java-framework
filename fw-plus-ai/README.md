# fw-plus-ai

## Current capabilities

This release line keeps the LangChain4j Java 8 compatibility overlays and the Boot auto-configuration (including the AgentScope 2 conditional assembly):

- `fw-plus-ai-langchain4j`, `fw-plus-ai-langchain4j-*`: Java 8 compatible provider overlays for LangChain4j `0.35.0`, preserving the upstream `dev.langchain4j.*` package and class names.
- `fw-plus-ai-boot-autoconfigure`: classpath-conditional provider auto-configurations (master switch `fw.plus.ai.langchain4j.enabled`), plus MCP server, MCP clustering and the Magic-API MCP plugin. Also supports injecting a `SimpleAssistant` bean from configuration (`fw.plus.magicapi.langchain4j.simple-assistant.*`: `enabled` switch, `key` default key, `keys.<type>` type-specific keys; resources are wrapped in MagicDynamic dynamic proxies that resolve on each call, so magic instance hot-reloads/removals never leave stale references).
- `fw-plus-ai-magicapi-plugin-mcp-server`: Magic-API MCP plugin.
- Magic-API plugin (langchain4j / mcp) web endpoints support both Servlet and WebFlux: in WebFlux applications (`spring.main.web-application-type=reactive`) they are assembled by the Boot-layer reactive engine, with endpoint paths identical to the Servlet stack. Known limitation: the magic-editor workbench WebSocket live debugging is unavailable under WebFlux (the rest of the console works normally).
- AgentScope Java 2 (`2.0.4`): `fw-plus-ai-boot-autoconfigure` provides the ported `AgentscopeAutoConfiguration` (official `agentscope-spring-boot-starter` v2.0.4 source, Apache-2.0, in the `autoconfigure.agentscope` package) that assembles `Memory` / `Toolkit` / `ReActAgent` beans when `io.agentscope.core.ReActAgent` is on the classpath and `agentscope.agent.enabled=true`. Configuration prefixes keep the official `agentscope.agent.*` / `agentscope.model.*`. Model beans are supplied by the app (e.g. `agentscope-extensions-model-ollama`'s `OllamaChatModel`) since this distribution line does not ship the official model starters. Dependencies are pinned in `fw-plus-ai-boot-dependencies` (reactor-core 3.8.2 / jackson 2.21.1 / opentelemetry 1.61.0) so AgentScope 2.0.4 runs on Spring Boot 3.5.8. Sample: `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-agentscope-sample`.
- Aggregators: `fw-plus-ai-boot-starter`, `fw-plus-ai-mcp-server-spring-boot-starter`.

Refer to the `LangChain4j*AutoConfiguration` sources in `fw-plus-ai-boot-autoconfigure` for each provider's conditions and properties.

## Removed

The following artifacts and their capabilities were removed from this repository. No POM aliases, old configuration prefixes, or compatibility fallbacks remain:

| Removed artifact | Former purpose |
| --- | --- |
| `fw-plus-ai-model` | Framework-neutral model API (`AiModel` / `AiChatModel` / `AiEmbeddingModel`, ...) |
| `fw-plus-ai-store-embedding` | Framework-neutral embedding store API (`AiEmbeddingStore`, ...) |
| `fw-plus-ai-langchain4j-adapter` | LangChain4j adapter layer for the neutral SPI |
| `fw-plus-ai-model-spring-boot-starter`, `fw-plus-ai-store-embedding-spring-boot-starter` | Boot starters for the neutral APIs |
| `fw-plus-ai-magicapi-plugin-model`, `fw-plus-ai-magicapi-plugin-vector` | Magic-API model / vector datasource plugins |

The neutral configuration prefixes `fw-plus.ai.model` / `fw-plus.ai.embedding-store`, the `autoconfigure.model`, `autoconfigure.store.embedding`, `autoconfigure.langchain4j.adapter`, `autoconfigure.magicapi.plugin.model` and `autoconfigure.magicapi.plugin.vector` packages, and their auto-configurations were removed as well. To use LangChain4j directly, add the provider SDK you need and depend on `fw-plus-ai-boot-autoconfigure` for assembly.

AgentScope 2 is no longer future work: it ships in this Java 17 / Spring Boot 3 release line via the ported `AgentscopeAutoConfiguration` described above (sample: `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-agentscope-sample`). Spring AI / Spring AI Alibaba remain out of scope of this distribution line's auto-configuration.
