## 1. RED 证据

- [x] 1.1 新增 Store 架构契约测试，证明 legacy Store 类型仍存在、AutoConfigure 厂商主类超过目标数量，并实际运行确认 RED
- [x] 1.2 新增 Provider Adapter 多匹配测试，证明当前 first-match 行为未拒绝冲突，并实际运行确认 RED

## 2. Core Store 链重建

- [x] 2.1 新建 `LangChain4jEmbeddingStoreProviderAdapter`，直接消费 `AiEmbeddingStoreDefinition`，并以 Interface 测试验证完整 Definition 可用于支持判断和创建
- [x] 2.2 实现 `DefaultLangChain4jEmbeddingStoreAdapter` 的 framework/type 校验、相同实例去重和零/一/多 Provider 候选判定，并通过定向测试
- [x] 2.3 保持 `LangChain4jAiEmbeddingStore` 中立 CRUD/search 行为并迁移原生 Store 包装入口，通过显式 id、filter、add/remove/search 测试
- [x] 2.4 删除 8 个 LangChain4j legacy Definition/Properties/Factory/Registry/Exception/Provider 类型及其测试，并通过全仓 legacy FQCN 搜索

## 3. 声明式 Provider 创建器

- [x] 3.1 实现不可变 `LangChain4jEmbeddingStoreProviderSpec` 及其不变量校验，通过重复 alias、无创建计划、规则冲突和不可变性测试
- [x] 3.2 重写 `ProviderAdapterSupport`，支持不初始化类的可用性检查、属性/fallback/Bean 解析、基础类型/枚举/Duration/Collection 转换和确定性重载选择，并通过机制级测试
- [x] 3.3 实现具体 `ReflectiveLangChain4jEmbeddingStoreProviderAdapter`，按 Spec 校验、选择创建计划、调用 builder/constructor/method 并翻译中立异常，通过 builder、构造器、Bean 引用和歧义测试

## 4. 内置 Provider 迁移

- [x] 4.1 将 12 个纯映射 Provider 迁移为具名 Spec Bean，保持 provider alias 和 Bean 名，并通过参数化契约测试
- [x] 4.2 将 Milvus、Qdrant、Elasticsearch、Azure AI Search、Azure Cosmos NoSQL、Neo4j、Redis 和 Infinispan 规则迁移为 Spec Bean，通过必填、fallback、Bean alias 和类型转换测试
- [x] 4.3 将 Tablestore、Oracle 和 Weaviate 的条件构造器、多参数重载和长构造器规则迁移为 Spec Bean，通过厂商专项测试
- [x] 4.4 删除 25 个旧厂商/通用/转发 Provider Adapter 类，确认 `adapter.store` 主源码仅保留四个目标类型

## 5. 装配与调用方迁移

- [x] 5.1 重写 `LangChain4jEmbeddingStoreAdapterAutoConfiguration`，保留 23 个具名覆盖点并创建唯一 framework Adapter，通过 Bean 名、回退和可选 SDK 测试
- [x] 5.2 迁移 Spring Store 自动配置、MagicAPI 动态向量资源和 Tinyflow 测试/fixture 到新 Core seam，通过相关集成测试
- [x] 5.3 修正文档中的 legacy 配置前缀与 public 类型迁移指引，并通过全文搜索确认只保留明确历史说明
- [x] 5.4 删除原生 Store 自动配置旁路，清理 Maven 依赖和测试引用，确认 Model 源码无差异且 Store legacy FQCN 在生产/测试代码中为零

## 6. 验证与提交

- [x] 6.1 运行原架构断言并确认 GREEN：Core Store 仅三个目标类型、AutoConfigure Store 仅四个目标类型、所有删除项不存在
- [x] 6.2 运行 Core `fw-plus-ai-langchain4j-adapter` 与 `fw-plus-ai-store-embedding` 定向测试并确认通过
- [x] 6.3 运行 `fw-plus-ai-boot-autoconfigure` 完整测试和 AI Reactor `verify -am`，确认 Java 8 构建通过
- [x] 6.4 执行旧 FQCN、敏感值、diff 格式和 OpenSpec strict 校验，并完成两路独立代码审查
- [x] 6.5 仅暂存当前 change 的实现路径，核对 staged diff 后创建实现提交
