# langchain4j-embedding-store-starter Specification

## Purpose
TBD - created by archiving change unify-vector-magicapi-plugin. Update Purpose after archive.
## Requirements
### Requirement: 多向量存储 starter
系统 SHALL 提供 `fw-plus-ai-store-embedding-spring-boot-starter` 作为中立向量 store starter，并保留 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter` 作为默认 LangChain4j adapter 和兼容入口。

#### Scenario: 新 starter 进入 LangChain4j 聚合
- **WHEN** 检查 `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j` 聚合 POM
- **THEN** 它 SHALL 包含 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter` 子模块。

#### Scenario: starter 引入基础 store 模块
- **WHEN** 检查 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter` 的 POM
- **THEN** 它 SHALL 依赖 `fw-plus-ai-langchain4j-store-embeding`。
- **AND** 它 SHALL 使用 BOM 管理 LangChain4j 版本，不硬编码 provider 版本。

#### Scenario: 聚合和 BOM 注册中立 starter 与 LangChain4j starter
- **WHEN** 检查 Core AI 聚合和 Core BOM
- **THEN** 它们 SHALL 注册中立 store 模块/starter 与既有 LangChain4j store starter。
- **AND** 既有 LangChain4j starter artifact SHALL 继续可解析。

#### Scenario: 既有 LangChain4j store 配置保持行为
- **WHEN** 应用继续使用 LangChain4j 向量 store 配置并引入既有 starter
- **THEN** 系统 SHALL 通过 LangChain4j adapter 注册对应中立 store。
- **AND** 既有 provider、多实例、重复 key 和默认 store 校验行为 SHALL 保持。

#### Scenario: 原生 EmbeddingStore 不跨越中立边界
- **WHEN** 检查中立 starter、自动配置和 Magic-API vector 插件
- **THEN** 它们 SHALL 不要求 `EmbeddingStore<TextSegment>` 作为公开 Bean 参数或返回类型。
- **AND** 该原生类型只允许存在于 LangChain4j adapter 内部。

### Requirement: 支持多个向量实例
系统 SHALL 支持在同一应用中声明多个向量存储实例，并通过稳定 key 获取指定实例。

#### Scenario: 配置多个 provider
- **WHEN** 应用配置 Milvus、Qdrant、RedisStack、Weaviate 中任意多个向量存储实例
- **THEN** registry SHALL 为每个启用实例注册独立的 `EmbeddingStore<TextSegment>` 访问项。
- **AND** 不同 provider 或不同 key 的实例 SHALL 不互相覆盖。

#### Scenario: 同 provider 多实例
- **WHEN** 应用为同一 provider 配置多个不同 key
- **THEN** registry SHALL 能按 key 返回对应实例。
- **AND** 重复 key SHALL 在启动期失败并给出明确错误。

### Requirement: 支持默认向量存储
系统 SHALL 支持配置默认向量存储，并允许消费者在未传 key 时获取默认实例。

#### Scenario: 默认 key 存在
- **WHEN** 配置声明 `default-store` 指向已启用的 store key
- **THEN** registry SHALL 将该实例作为默认向量存储返回。

#### Scenario: 默认 key 不存在
- **WHEN** 配置声明的 `default-store` 不存在或对应实例未启用
- **THEN** 应用启动 SHALL 失败并提示默认向量存储配置无效。

### Requirement: provider adapter 可扩展
系统 SHALL 使用 Java 8 兼容的 provider adapter/factory 机制构建向量存储，避免把所有 provider 构造逻辑写死在 Magic-API 插件中。

#### Scenario: 已支持 provider 构建 store
- **WHEN** 某个 provider adapter 在 classpath 中可用且配置有效
- **THEN** starter SHALL 使用该 adapter 构建对应 `EmbeddingStore<TextSegment>`。

#### Scenario: provider adapter 缺失
- **WHEN** 配置引用了当前 classpath 不支持的 provider
- **THEN** 应用启动 SHALL 失败并提示缺失 provider adapter 或依赖。

