# langchain4j-embedding-store-starter Specification

## Purpose
TBD - created by archiving change unify-vector-magicapi-plugin. Update Purpose after archive.
## Requirements
### Requirement: 旧 LangChain4j store starter 坐标已移除
系统 SHALL 不再发布 `fw-plus-ai-langchain4j-store-embeding` 和 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter` 作为可用坐标；LangChain4j 向量存储能力 SHALL 由统一 LangChain4j adapter 和中立 `fw-plus.ai.embedding-store` 配置承担。

#### Scenario: 应用迁移到统一 adapter starter
- **WHEN** Boot 2.7 / Java 8 应用需要 LangChain4j 向量 store 实现
- **THEN** 应用 SHALL 引入 `fw-plus-ai-store-embedding-spring-boot-starter` 与 `fw-plus-ai-langchain4j-adapter`，并由 `fw-plus-ai-boot-starter` 提供自动配置。
- **AND** 不得继续引入已删除的 store artifact。

#### Scenario: 多实例和默认 store 由中立 registry 承担
- **WHEN** 应用配置多个向量 store 或默认 store
- **THEN** 系统 SHALL 通过中立 `AiEmbeddingStoreRegistry` 按 key 或默认 key 返回 store 门面。
- **AND** LangChain4j 原生 `EmbeddingStore<TextSegment>` 不得跨越中立边界。
