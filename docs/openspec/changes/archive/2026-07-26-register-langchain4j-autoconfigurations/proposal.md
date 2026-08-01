## Why

`fw-plus-ai-langchain4j` 已经在 core 层复制并维护多个 LangChain4j Spring Boot starter 自动配置源码，但 fw-plus AI 自动配置聚合类只导入了 core 配置，导致 provider/vector-store starter 配置不会随 `LangChain4jAutoConfiguration` 被纳入。

## What Changes

- 在 `LangChain4jAutoConfiguration` 中导入 core 层 `fw-plus-ai-langchain4j` 下各 starter 对应的配置 wrapper。
- 为 `LangChain4jCoreAutoConfiguration` 以及每个 provider/vector-store wrapper 增加 `enabled` 启动条件。
- 保留各 LangChain4j 原始 `AutoConfig` / `EmbeddingStoreAutoConfiguration` 内部已有的模型、向量库 bean 条件。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `langchain4j-starter-modules`: 补充 fw-plus AI 聚合自动配置对本地 LangChain4j starter 配置类的导入与启动条件要求。

## Impact

- 影响 `fw-plus-ai-boot-autoconfigure` 的 LangChain4j 自动配置入口。
- 不新增外部依赖，不修改 public API，不改变 core 层 starter 源码复制策略。
