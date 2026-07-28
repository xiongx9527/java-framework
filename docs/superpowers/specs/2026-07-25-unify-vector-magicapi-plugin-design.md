---
comet_change: unify-vector-magicapi-plugin
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-26-unify-vector-magicapi-plugin
status: final
---

# 统一向量 Store Starter 与 Magic-API Vector 插件技术设计

## 背景

fw-plus 当前在 Core 层已有 `fw-plus-ai-langchain4j-store-embeding` 基础模块，以及 Redis、Milvus、Elasticsearch 等 LangChain4j provider 模块；AI Magic-API 侧则存在 Milvus、Qdrant、RedisStack、Weaviate 等分散插件。分散插件会让脚本导入、动态数据源、starter 聚合、BOM 管理和后续 SQLBot/AskData/RAG/多 Agent 工作流不断重复对接向量库。

本设计采用 Core 多向量 registry/factory + provider adapter + AI Magic-API 单 vector 插件。Core 侧先提供稳定的多实例向量 store 底座，Magic-API 侧只做脚本入口和运行时委托。

## 目标

- 新增 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter`，支持多 provider、多 key、默认 store。
- 在 `fw-plus-ai-langchain4j-store-embeding` 中补齐通用配置模型、registry、factory 和 provider adapter 抽象。
- 将 AI Magic-API 向量能力收敛为一个 `vector` 插件。
- 让 Milvus、Qdrant、RedisStack、Weaviate 等能力通过统一 registry 获取，避免插件内重复构造 provider。
- 保持 Java 8、Spring Boot 2.7.x、LangChain4j 0.35.0 路线。

## 非目标

- 不重写 LangChain4j 各 provider 的底层实现。
- 不引入 Java 17、Spring Boot 3 或 Spring AI starter 路线。
- 不改变现有 EmbeddingModel/模型管理语义。
- 不在本 change 实现完整 SQLBot/AskData 查询链路。

## 架构

### Core 多向量底座

`fw-plus-ai-langchain4j-store-embeding` 承载跨 provider 的通用抽象：

- `EmbeddingStoreDefinition`：描述单个 store 的 key、provider、enabled、dimension、metadataKeys 和 provider 专属属性。
- `EmbeddingStoreRegistry`：按 key 获取 `EmbeddingStore<TextSegment>`，并提供默认 store 获取能力。
- `EmbeddingStoreFactory`：根据 definition 和可用 adapter 构建 store。
- `EmbeddingStoreProviderAdapter`：provider 扩展点，按 provider 名称判断是否支持并构建具体 store。
- `EmbeddingStoreException`：统一表达重复 key、未知 provider、缺失默认 store、provider 配置错误等启动或运行时错误。

新增 starter `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter` 负责 Spring Boot 自动配置：绑定 properties、加载 adapter、构建 registry、暴露默认 bean。配置应支持如下语义：

```yaml
fw-plus:
  ai:
    langchain4j:
      embedding-store:
        enabled: true
        default-store: knowledge
        stores:
          knowledge:
            provider: milvus
            dimension: 1536
            collection-name: knowledge_base
          local-cache:
            provider: redisstack
            index-name: kb_vector
            dimension: 1536
```

实际前缀可在实现时结合项目属性命名习惯确认，但必须具备 `enabled`、`default-store`、`stores` 三类语义。

### Provider adapter

provider 构建逻辑下沉到 Core adapter，Magic-API 插件不再直接维护每个 provider 的连接构建细节。优先接入已有 Redis、Milvus、Elasticsearch 模块；Qdrant、Weaviate、RedisStack 在迁移时补齐 adapter 或必要依赖。

adapter 必须满足：

- Java 8 兼容，不使用 `List.of()`、`Map.copyOf()` 等 Java 9+ API。
- 不依赖 Spring AI。
- provider 缺失或 classpath 不完整时给出清晰错误。
- 同 provider 多实例通过不同 key 注册，不以 bean name 单例覆盖。

### Magic-API 统一插件

AI Magic-API 侧新增统一插件模块，例如 `fw-plus-ai-magicapi-plugin-vector`。该模块注册一个 Magic-API 插件和一个脚本模块：

- 插件元信息使用单一名称，如 `Plugin("vector", "MagicVector", "magic-vector.1.0.0.iife.js")`。
- 脚本侧统一使用 `import vector`。
- `vector['knowledge']` 或等价方式按 key 选择 store。
- 未指定 key 时使用 Core registry 的默认 store。
- 未知 key、未配置默认 store、provider 构造失败时抛出明确 Magic-API 运行时错误。

旧 Milvus、Qdrant、RedisStack、Weaviate 插件入口不再作为推荐的一等插件继续演进。实现可以选择保留薄壳兼容入口，但薄壳只能委托统一 `vector` 插件或 Core registry，不能继续复制 provider 构建逻辑。

## Maven 与模块管理

Core 层：

- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/pom.xml` 加入 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter`。
- `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml` 管理新 starter jar，版本使用 `${project.version}`。
- provider 依赖版本继续由 LangChain4j BOM 或 Core BOM 管理。

AI 层：

- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/pom.xml` 加入统一 vector 插件。
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml` 管理统一 vector 插件。
- 旧分散插件根据兼容策略调整为移除、废弃或薄壳委托。

## 迁移策略

推荐实现顺序是先保留兼容入口，降低旧脚本一次性迁移风险：

1. 新增 `vector` 插件并完成新脚本路径。
2. 旧插件入口保留为 deprecated thin wrapper，内部委托 registry 或 `vector` 模块。
3. 文档和示例只展示 `vector`。
4. 后续归档或大版本再移除旧独立插件模块。

如果实现时发现旧插件模块保留会导致循环依赖或 Maven 聚合复杂度过高，可以改为移除旧模块，但必须提供启动期/文档层面的迁移提示。

## 错误处理

- 重复 key：启动失败，提示重复的 store key。
- `default-store` 不存在：启动失败，提示默认 store 配置无效。
- provider 未支持：启动失败，提示缺少 provider adapter 或依赖。
- 脚本访问未知 key：Magic-API 运行时错误，提示 key 未注册。
- 未配置默认 store 且脚本未传 key：Magic-API 运行时错误，提示需要指定 key 或配置默认 store。

## 测试策略

Core 单元测试：

- 多 provider 同时注册。
- 同 provider 多 key 注册。
- 默认 store 正确返回。
- 重复 key 启动失败。
- 未知 provider 启动失败。
- `default-store` 指向不存在 key 启动失败。
- provider adapter 缺失时错误信息清晰。

AI/Magic-API 测试：

- `vector['key']` 获取指定 store。
- 未指定 key 时使用默认 store。
- 未知 key 报明确错误。
- 旧入口若保留，确认只委托统一 registry，不重复构造 provider。

构建验证：

- 使用 `fw-plus-core/mvnw` 验证 Core LangChain4j 相关 reactor。
- 使用 `fw-plus-ai/mvnw` 验证 AI boot autoconfigure 和 Magic-API plugin starter 相关 reactor。
- 运行 `openspec validate unify-vector-magicapi-plugin --strict`。

## 风险与缓解

- Qdrant/Weaviate 目前主要在 AI Magic-API 插件侧，Core provider adapter 可能缺口较大。通过可插拔 adapter 和分 provider 小步迁移降低风险。
- 多 provider 依赖进入 starter 后依赖体积可能增加。实现时可让 provider 依赖保持可选，并在缺失时提供清晰错误。
- 旧脚本入口迁移可能影响现有资源。先提供 deprecated thin wrapper 或迁移提示，避免静默失败。
- 默认 store 配置错误会影响运行时脚本。启动期校验比首次脚本调用时报错更合适。
