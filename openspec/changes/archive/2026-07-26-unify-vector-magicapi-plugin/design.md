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
