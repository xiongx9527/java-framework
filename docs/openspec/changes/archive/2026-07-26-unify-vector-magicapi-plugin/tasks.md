## 1. Core 多向量 starter

- [x] 1.1 新建 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter` 模块，并加入 `fw-plus-ai-langchain4j` 聚合 POM。
- [x] 1.2 将新 starter 加入 `fw-plus-core/fw-plus-project/fw-plus-dependencies` dependencyManagement，版本使用 `${project.version}`。
- [x] 1.3 在 `fw-plus-ai-langchain4j-store-embeding` 中补齐 Java 8 兼容的多向量配置模型、store definition、registry 和默认 store 选择能力。
- [x] 1.4 在 starter 中实现 Spring Boot 2.7 自动配置，支持 `enabled`、`default-store`、`stores` 多实例配置和启动期校验。
- [x] 1.5 添加单元测试覆盖多 provider、多 key、同 provider 多实例、默认 store、重复 key、未知 provider 和缺失默认 store。

## 2. Provider adapter 接入

- [x] 2.1 抽象 provider adapter/factory 接口，避免 Magic-API 插件直接维护 provider 连接构建逻辑。
- [x] 2.2 接入已有 Redis、Milvus、Elasticsearch provider 模块构建能力。
- [x] 2.3 为 Qdrant、Weaviate、RedisStack 补齐必要依赖或 adapter，并复用 LangChain4j 0.35.0 兼容 API。
- [x] 2.4 确认所有 provider adapter 不引入 Java 9+ API，也不依赖 Spring AI Java 17/Spring Boot 3 路径。

## 3. AI Magic-API 统一 vector 插件

- [x] 3.1 新建统一 AI Magic-API vector 插件模块，并加入 `fw-plus-ai-magicapi-plugin` 聚合 POM。
- [x] 3.2 在 `fw-plus-ai-boot-autoconfigure` 的 Magic-API plugin 包下实现统一 `vector` DynamicModule/配置入口。
- [x] 3.3 让统一 vector 插件通过 Core registry/factory 获取 `EmbeddingStore<TextSegment>`，支持按 key 和默认 store 访问。
- [x] 3.4 迁移 Milvus、Qdrant、RedisStack、Weaviate 现有插件能力，移除或废弃重复 provider 构建逻辑。
- [x] 3.5 将统一 vector 插件加入 AI boot dependencies，并调整旧分散插件的 BOM/聚合/兼容策略。

## 4. 兼容、文档与验证

- [x] 4.1 明确旧脚本入口迁移策略：兼容别名、薄壳委托或移除，并给出清晰错误/迁移提示。
- [x] 4.2 补充配置示例，覆盖多个向量 store、默认 store、同 provider 多实例和脚本侧 `vector` 访问方式。
- [x] 4.3 使用 Maven Wrapper 验证 Core LangChain4j 相关 reactor 构建。
- [x] 4.4 使用 Maven Wrapper 验证 AI boot autoconfigure 与 Magic-API plugin starter 相关 reactor 构建。
- [x] 4.5 运行 `openspec validate unify-vector-magicapi-plugin --strict` 并修复所有 OpenSpec 校验问题。
