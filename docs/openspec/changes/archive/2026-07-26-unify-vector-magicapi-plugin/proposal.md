## Why

当前 LangChain4j 向量存储能力按 provider 分散在多个模块和 Magic-API 插件中，脚本侧需要分别引入 Milvus、Qdrant、RedisStack、Weaviate 等插件，配置与使用方式不统一。后续 SQLBot/AskData、RAG、多 Agent 和工作流能力都需要稳定的向量底座，因此需要先在 Core 侧提供支持多向量实例的 starter，再把 Magic-API 向量能力收敛为一个统一插件。

## What Changes

- 新增 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter`，基于现有 `fw-plus-ai-langchain4j-store-embeding` 提供统一自动配置入口。
- starter 支持多个向量存储实例，按 `provider + key` 或全局唯一 `key` 管理，并支持配置默认向量存储。
- starter 提供向量存储 registry/factory 能力，让业务代码和 Magic-API 插件可以按 key 获取 `EmbeddingStore<TextSegment>`。
- 统一 AI Magic-API 向量插件，脚本侧只暴露一个 vector 模块，再通过 key/provider 选择 Milvus、Qdrant、RedisStack、Weaviate 等具体向量库。
- 将现有分散的 Milvus、Qdrant、RedisStack、Weaviate Magic-API 插件能力迁入统一向量插件，并保留必要的兼容/迁移策略。
- 保持 Java 8 和 Spring Boot 2.7.x 兼容，不引入 Spring AI Java 17/Spring Boot 3 依赖路径。

## Capabilities

### New Capabilities

- `langchain4j-embedding-store-starter`: Core LangChain4j 向量存储 starter，提供多 provider、多实例、默认向量库和 registry/factory 能力。
- `magicapi-unified-vector-plugin`: AI Magic-API 统一向量插件，提供单一脚本模块和统一配置入口，聚合 Milvus、Qdrant、RedisStack、Weaviate 等向量库能力。

### Modified Capabilities

- `langchain4j-starter-modules`: 新增的向量 store starter 需要进入现有 LangChain4j starter/module/BOM 管理范围。

## Impact

- 影响 Core 层 `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j` 聚合、BOM 与 `fw-plus-ai-langchain4j-store-embeding` 基础模块。
- 影响 AI 层 `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin` 下现有分散向量插件。
- 影响 AI Magic-API plugin starter 聚合 POM 与依赖管理。
- 对 Magic-API 脚本侧的向量模块名称和使用方式有迁移影响；旧入口是否保留为兼容别名需在设计中明确。
