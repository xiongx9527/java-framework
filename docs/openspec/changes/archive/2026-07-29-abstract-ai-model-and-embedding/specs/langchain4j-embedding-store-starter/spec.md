## MODIFIED Requirements

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
