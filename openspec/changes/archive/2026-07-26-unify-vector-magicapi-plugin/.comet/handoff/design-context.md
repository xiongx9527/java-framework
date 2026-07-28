# Comet Design Handoff

- Change: unify-vector-magicapi-plugin
- Phase: design
- Mode: compact
- Context hash: 8baf27be52550d644995d8004c07a29c85579a8758c21546142baf60a6f48c2c

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/unify-vector-magicapi-plugin/proposal.md

- Source: openspec/changes/unify-vector-magicapi-plugin/proposal.md
- Lines: 1-30
- SHA256: 92c521290704278ec882c1678208d5689ac1d05381b81f962c75b8a2b640d948

```md
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

```

## openspec/changes/unify-vector-magicapi-plugin/design.md

- Source: openspec/changes/unify-vector-magicapi-plugin/design.md
- Lines: 1-66
- SHA256: fecf1db278baaea2d4f94a21ca56ba15f9cc6638f5635a0658f65954c7fe2ef1

```md
## Context

fw-plus 当前在 Core 层已经有 `fw-plus-ai-langchain4j-store-embeding` 基础模块，并已有 Redis、Milvus、Elasticsearch 等 LangChain4j provider 模块；AI 层 Magic-API 插件目录下则存在 Milvus、Qdrant、RedisStack、Weaviate 等分散插件。分散插件让脚本侧 API、starter 聚合、BOM 管理和后续 RAG/SQLBot/AskData 工作流集成都变复杂。

本 change 先建立 Core 侧多向量 store starter，再让 AI Magic-API 侧只暴露一个统一 vector 插件。约束是 Java 8、Spring Boot 2.7.x、LangChain4j 优先，不能引入 Spring AI Java 17/Spring Boot 3 路径。

## Goals / Non-Goals

**Goals:**

- 新增 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter`，让应用通过一个 starter 获得多向量 store 注册、默认 store 选择和按 key 获取能力。
- 支持多个向量 provider 和多个实例，例如同一应用同时配置 Milvus、Qdrant、RedisStack、Weaviate，或同一 provider 下多个 key。
- 在 AI Magic-API 侧统一为一个 vector 插件，脚本侧通过一个模块选择具体向量 store。
- 复用现有 LangChain4j provider 模块和 Magic-API 动态模块习惯，减少迁移成本。

**Non-Goals:**

- 不重写 LangChain4j 的底层向量库实现。
- 不在本 change 中引入 Java 17、Spring Boot 3 或 Spring AI starter 路径。
- 不改变现有模型管理语义；EmbeddingModel 仍复用当前 AI/LangChain4j 模块可提供的模型。
- 不设计完整 SQLBot/AskData 查询链路，只提供其后续 RAG 能使用的向量底座。

## Decisions

### 1. Core starter 提供 registry/factory，而不是只注册单个 EmbeddingStore bean

新增 starter 应提供类似 `EmbeddingStoreRegistry`、`EmbeddingStoreFactory` 的抽象。配置结构包含 `enabled`、`default-store` 和 `stores`，每个 store 记录 `provider`、`key`、连接参数、索引/collection/class/schema、dimension、metadataKeys 等 provider 专属字段。自动配置启动时构建所有启用的 store，并把默认 store 标记出来。

选择这个方案是因为用户明确要求 starter 支持多向量；如果只延续 LangChain4j provider starter 的单 bean 形态，多个 provider 或同 provider 多实例会互相覆盖，Magic-API 也无法稳定按 key 路由。

### 2. provider 通过 Java 8 兼容 adapter 扩展

在 `fw-plus-ai-langchain4j-store-embeding` 中补基础模型：store 定义、provider 枚举/字符串、registry 查询、默认选择、异常信息。各 provider 构建逻辑通过 adapter 接入，优先复用已有 Redis/Milvus/Elasticsearch 模块；Qdrant、Weaviate、RedisStack 等 Magic-API 既有能力需要迁入时，如果 Core 侧缺少对应 adapter，应补齐依赖和构建器封装。

adapter 不使用 Java 9+ API，不依赖 Spring AI；自动配置使用 Spring Boot 2.7 支持的注册机制。

### 3. Magic-API 只暴露一个 vector 插件

AI Magic-API starter 聚合中新增统一插件模块，例如 `fw-plus-ai-magicapi-plugin-vector`。脚本侧只导入一个模块，例如 `import vector`，通过 `vector['default']`、`vector['knowledge']` 或等价 API 获取具体向量 store。模块内部委托 Core registry/factory，不再在每个 provider 插件里重复构造连接。

旧 Milvus、Qdrant、RedisStack、Weaviate 插件不再作为并列的一等插件扩展；实现时可保留兼容别名或迁移适配，确保旧配置能有明确的错误提示或过渡路径。

### 4. BOM 和聚合 POM 按层管理

Core 层新增 starter jar 进入 `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j` 聚合，并进入 `fw-plus-core/fw-plus-project/fw-plus-dependencies` dependencyManagement。AI 层统一 Magic-API vector 插件进入 AI boot starter 聚合和 AI boot dependencies。原分散插件模块移除、保留兼容空壳或标记废弃的具体策略在实现时根据编译影响选择，但最终对外应只推荐一个 vector 插件。

## Risks / Trade-offs

- [Risk] Qdrant/Weaviate 目前主要存在于 AI Magic-API 插件侧，Core LangChain4j 聚合未必已有对应模块。→ Mitigation: provider adapter 先设计为可插拔，迁移时补齐缺失 provider 依赖或把构建逻辑沉到统一 store starter。
- [Risk] 旧脚本直接 `import milvus`、`import qdrant` 等可能受影响。→ Mitigation: 提供兼容别名、迁移文档或启动期明确提示，默认新能力只暴露 `vector`。
- [Risk] 多实例配置错误时可能导致默认 store 不明确。→ Mitigation: 启动期校验 `default-store` 必须存在；无配置时不强制注册默认 store，避免误连本地服务。
- [Risk] provider 依赖全部打进一个 starter 会增大依赖体积。→ Mitigation: starter 只聚合当前框架需要的 provider adapter，并允许缺失 provider 依赖时报清晰错误；必要时把具体 provider 依赖保留为可选。

## Migration Plan

1. 新增 Core starter 和多向量 registry/factory，先通过单元测试覆盖默认选择、按 key 获取、重复 key、未知 provider 和缺失默认 store。
2. 接入 Redis/Milvus 等现有 provider adapter，再迁移 Qdrant、Weaviate、RedisStack 构建逻辑。
3. 新增统一 AI Magic-API vector 插件，并把 starter 聚合从多个 provider 插件收敛到一个插件。
4. 为旧插件入口提供兼容策略：保留只依赖统一插件的薄壳、别名映射，或在文档中标记迁移到 `vector`。
5. 分别验证 Core LangChain4j reactor 和 AI boot autoconfigure/starter reactor。

## Open Questions

- 旧脚本模块名是否必须长期兼容，还是只需要一次性迁移到 `vector`。
- 统一配置前缀最终使用 `fw.plus.ai.langchain4j.embedding-store` 还是沿用 `langchain4j.embedding-store` 风格。
- RedisStack 是继续复用现有 Redis datasource 配置，还是与其它向量库一样完整进入统一 store 配置。

```

## openspec/changes/unify-vector-magicapi-plugin/tasks.md

- Source: openspec/changes/unify-vector-magicapi-plugin/tasks.md
- Lines: 1-30
- SHA256: 28bd74b5b16116ab99ed04f0efd1a62182dc03362dcc80e77946989423f97001

```md
## 1. Core 多向量 starter

- [ ] 1.1 新建 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter` 模块，并加入 `fw-plus-ai-langchain4j` 聚合 POM。
- [ ] 1.2 将新 starter 加入 `fw-plus-core/fw-plus-project/fw-plus-dependencies` dependencyManagement，版本使用 `${project.version}`。
- [ ] 1.3 在 `fw-plus-ai-langchain4j-store-embeding` 中补齐 Java 8 兼容的多向量配置模型、store definition、registry 和默认 store 选择能力。
- [ ] 1.4 在 starter 中实现 Spring Boot 2.7 自动配置，支持 `enabled`、`default-store`、`stores` 多实例配置和启动期校验。
- [ ] 1.5 添加单元测试覆盖多 provider、多 key、同 provider 多实例、默认 store、重复 key、未知 provider 和缺失默认 store。

## 2. Provider adapter 接入

- [ ] 2.1 抽象 provider adapter/factory 接口，避免 Magic-API 插件直接维护 provider 连接构建逻辑。
- [ ] 2.2 接入已有 Redis、Milvus、Elasticsearch provider 模块构建能力。
- [ ] 2.3 为 Qdrant、Weaviate、RedisStack 补齐必要依赖或 adapter，并复用 LangChain4j 0.35.0 兼容 API。
- [ ] 2.4 确认所有 provider adapter 不引入 Java 9+ API，也不依赖 Spring AI Java 17/Spring Boot 3 路径。

## 3. AI Magic-API 统一 vector 插件

- [ ] 3.1 新建统一 AI Magic-API vector 插件模块，并加入 `fw-plus-ai-magicapi-plugin` 聚合 POM。
- [ ] 3.2 在 `fw-plus-ai-boot-autoconfigure` 的 Magic-API plugin 包下实现统一 `vector` DynamicModule/配置入口。
- [ ] 3.3 让统一 vector 插件通过 Core registry/factory 获取 `EmbeddingStore<TextSegment>`，支持按 key 和默认 store 访问。
- [ ] 3.4 迁移 Milvus、Qdrant、RedisStack、Weaviate 现有插件能力，移除或废弃重复 provider 构建逻辑。
- [ ] 3.5 将统一 vector 插件加入 AI boot dependencies，并调整旧分散插件的 BOM/聚合/兼容策略。

## 4. 兼容、文档与验证

- [ ] 4.1 明确旧脚本入口迁移策略：兼容别名、薄壳委托或移除，并给出清晰错误/迁移提示。
- [ ] 4.2 补充配置示例，覆盖多个向量 store、默认 store、同 provider 多实例和脚本侧 `vector` 访问方式。
- [ ] 4.3 使用 Maven Wrapper 验证 Core LangChain4j 相关 reactor 构建。
- [ ] 4.4 使用 Maven Wrapper 验证 AI boot autoconfigure 与 Magic-API plugin starter 相关 reactor 构建。
- [ ] 4.5 运行 `openspec validate unify-vector-magicapi-plugin --strict` 并修复所有 OpenSpec 校验问题。

```

## openspec/changes/unify-vector-magicapi-plugin/specs/langchain4j-embedding-store-starter/spec.md

- Source: openspec/changes/unify-vector-magicapi-plugin/specs/langchain4j-embedding-store-starter/spec.md
- Lines: 1-48
- SHA256: 3145ff59a85ad4d9542acbc338b79b8fae68ac43f55ad06626d8bed66a46e994

```md
## ADDED Requirements

### Requirement: 多向量存储 starter
系统 SHALL 提供 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter`，并基于 `fw-plus-ai-langchain4j-store-embeding` 注册统一的向量存储 registry/factory 能力。

#### Scenario: 新 starter 进入 LangChain4j 聚合
- **WHEN** 检查 `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j` 聚合 POM
- **THEN** 它 SHALL 包含 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter` 子模块。

#### Scenario: starter 引入基础 store 模块
- **WHEN** 检查 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter` 的 POM
- **THEN** 它 SHALL 依赖 `fw-plus-ai-langchain4j-store-embeding`。
- **AND** 它 SHALL 使用 BOM 管理 LangChain4j 版本，不硬编码 provider 版本。

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

```

## openspec/changes/unify-vector-magicapi-plugin/specs/langchain4j-starter-modules/spec.md

- Source: openspec/changes/unify-vector-magicapi-plugin/specs/langchain4j-starter-modules/spec.md
- Lines: 1-26
- SHA256: ba8ce02c084a250f26466ed5c7195b75f7b63e792005cbf967d83e765111c97e

```md
## MODIFIED Requirements

### Requirement: 暴露指定 LangChain4j starter 本地模块
系统 SHALL 在 `fw-plus-ai-langchain4j` 下为每个用户指定的 LangChain4j Spring Boot starter 暴露一个 fw-plus Maven 模块，并在模块内保留对应 starter 自动配置代码。系统还 SHALL 暴露 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter`，用于统一管理多向量 store starter 能力。

#### Scenario: 聚合 POM 注册指定 starter 模块
- **WHEN** 检查 `fw-plus-ai-langchain4j` 聚合 POM
- **THEN** 它列出 Anthropic、Ollama、Azure AI Search、Azure OpenAI、Voyage AI、GitHub Models、Vertex AI Gemini、Elasticsearch、Redis、Qianfan、Milvus、DashScope 对应的子模块。
- **AND** 它列出 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter` 子模块。

#### Scenario: 子模块复制指定 starter 代码
- **WHEN** 检查任一新增 starter 子模块 POM
- **THEN** 它不直接依赖对应的 `dev.langchain4j` Spring Boot starter artifact。
- **AND** 它包含从 LangChain4j `0.35.0` starter jar 包复制的 `src/main/java` 自动配置源码。
- **AND** 它声明对应的底层 `dev.langchain4j` provider/vector-store artifact，版本由 core 层 BOM 统一管理。

#### Scenario: 子模块不自动注册 AutoConfiguration imports
- **WHEN** 检查任一新增 starter 子模块资源
- **THEN** 它不包含 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。
- **AND** 它不包含 `META-INF/spring/org.springframework.boot.autoconfig.AutoConfiguration.imports`。

#### Scenario: core BOM 管理新增 jar 模块
- **WHEN** 检查 `fw-plus-core/fw-plus-project/fw-plus-dependencies` 的 dependencyManagement
- **THEN** 它包含 Anthropic、Ollama、Azure AI Search、Azure OpenAI、Voyage AI、GitHub Models、Vertex AI Gemini、Elasticsearch、Redis、Qianfan、Milvus、DashScope 对应的 `fw-plus-ai-langchain4j-*` jar 模块。
- **AND** 它包含 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter` jar 模块。
- **AND** 这些模块使用 `${project.version}` 作为版本。

```

## openspec/changes/unify-vector-magicapi-plugin/specs/magicapi-unified-vector-plugin/spec.md

- Source: openspec/changes/unify-vector-magicapi-plugin/specs/magicapi-unified-vector-plugin/spec.md
- Lines: 1-42
- SHA256: 578e9c5b3e516009b500ba5cb2aba4cc9342a61cac9b3eeb01853cd916bad6db

```md
## ADDED Requirements

### Requirement: 单一 Magic-API 向量插件
系统 SHALL 在 AI Magic-API 侧提供一个统一 vector 插件，用一个脚本模块聚合 Milvus、Qdrant、RedisStack、Weaviate 等向量存储能力。

#### Scenario: 只暴露统一 vector 插件
- **WHEN** 检查 AI Magic-API plugin starter 聚合
- **THEN** 它 SHALL 提供一个统一向量插件模块。
- **AND** Milvus、Qdrant、RedisStack、Weaviate 不 SHALL 作为推荐的一等独立向量插件继续暴露。

#### Scenario: 脚本侧通过 key 选择 store
- **WHEN** Magic-API 脚本使用统一 vector 模块访问 `vector['knowledge']` 或等价 key 选择方式
- **THEN** 插件 SHALL 返回 key 对应的向量存储操作对象。

#### Scenario: 脚本侧访问默认 store
- **WHEN** Magic-API 脚本未显式指定向量 store key
- **THEN** 插件 SHALL 使用 starter registry 中配置的默认向量存储。

### Requirement: 统一插件复用 Core 多向量 registry
系统 SHALL 让统一 Magic-API vector 插件委托 Core LangChain4j 多向量 registry/factory 获取向量存储实例。

#### Scenario: 插件不重复构造 provider 连接
- **WHEN** 插件需要访问 Milvus、Qdrant、RedisStack、Weaviate 等向量库
- **THEN** 插件 SHALL 通过 Core registry/factory 获取 `EmbeddingStore<TextSegment>`。
- **AND** 插件自身不 SHALL 复制每个 provider 的连接构建逻辑。

#### Scenario: provider 配置异常
- **WHEN** 脚本访问的向量 store key 未注册或 provider 配置无效
- **THEN** 插件 SHALL 返回明确的 Magic-API 运行时错误，包含缺失 key 或 provider 名称。

### Requirement: 旧向量插件迁移策略
系统 SHALL 为现有 Milvus、Qdrant、RedisStack、Weaviate Magic-API 插件提供明确迁移策略，确保用户知道应迁移到统一 vector 插件。

#### Scenario: 旧插件入口被保留为兼容别名
- **WHEN** 实现选择保留旧插件入口
- **THEN** 旧入口 SHALL 委托统一 vector 插件或统一 registry。
- **AND** 旧入口 SHALL 避免继续维护独立 provider 构建逻辑。

#### Scenario: 旧插件入口被移除
- **WHEN** 实现选择移除旧插件入口
- **THEN** 聚合 POM 和 BOM SHALL 不再推荐独立 provider 插件。
- **AND** 迁移说明 SHALL 指向统一 vector 插件和按 key 访问方式。

```
