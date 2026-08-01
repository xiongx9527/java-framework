## REMOVED Requirements

### Requirement: 多向量存储 starter
**Reason**: `fw-plus-ai-langchain4j-store-embeding` 与 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter` 将被统一 LangChain4j adapter 替代，不能继续作为兼容坐标存在。

**Migration**: 应用改为引入统一 LangChain4j adapter starter，并使用 `fw-plus.ai.embedding-store` 中立配置。多个 adapter 同时存在时必须显式设置 `framework=langchain4j`。

### Requirement: 支持多个向量实例
**Reason**: 多实例能力已由中立 `AiEmbeddingStoreRegistry` 承担，不应继续通过旧原生 registry 暴露。

**Migration**: 通过中立 registry 按稳定 key 获取 store。

### Requirement: 支持默认向量存储
**Reason**: 默认 store 由中立配置与 registry 统一校验。

**Migration**: 使用中立 `default-store`，不要引用旧 `EmbeddingStoreRegistry`。

### Requirement: provider adapter 可扩展
**Reason**: provider 构造逻辑迁入统一 LangChain4j adapter。

**Migration**: 使用中立 SPI 和 adapter 内部 provider 实现，不在 Magic-API 或公共自动配置中构造原生 store。

## ADDED Requirements

### Requirement: 旧 LangChain4j store starter 坐标已移除
系统 SHALL 不再发布 `fw-plus-ai-langchain4j-store-embeding` 和 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter` 作为可用坐标；LangChain4j 向量存储能力 SHALL 由统一 LangChain4j adapter 和中立 `fw-plus.ai.embedding-store` 配置承担。

#### Scenario: 应用迁移到统一 adapter starter
- **WHEN** Boot 2.7 / Java 8 应用需要 LangChain4j 向量 store 实现
- **THEN** 应用 SHALL 引入 `fw-plus-ai-langchain4j-adapter-spring-boot-starter`。
- **AND** 不得继续引入已删除的 store artifact。

#### Scenario: 多实例和默认 store 由中立 registry 承担
- **WHEN** 应用配置多个向量 store 或默认 store
- **THEN** 系统 SHALL 通过中立 `AiEmbeddingStoreRegistry` 按 key 或默认 key 返回 store 门面。
- **AND** LangChain4j 原生 `EmbeddingStore<TextSegment>` 不得跨越中立边界。
