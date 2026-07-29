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
