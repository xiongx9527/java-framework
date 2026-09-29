# fw-plus-ai

## Current capabilities

This release line keeps only the LangChain4j Java 8 compatibility overlays and the Boot auto-configuration:

- `fw-plus-ai-langchain4j`, `fw-plus-ai-langchain4j-*`: Java 8 compatible provider overlays for LangChain4j `0.35.0`, preserving the upstream `dev.langchain4j.*` package and class names.
- `fw-plus-ai-boot-autoconfigure`: classpath-conditional provider auto-configurations (master switch `fw.plus.ai.langchain4j.enabled`), plus MCP server, MCP clustering and the Magic-API MCP plugin.
- `fw-plus-ai-magicapi-plugin-mcp`: Magic-API MCP plugin.
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

Spring AI, Spring AI Alibaba, and AgentScope 2 adapters are future Java 17 / Spring Boot 3 work, not artifacts of this release line.
