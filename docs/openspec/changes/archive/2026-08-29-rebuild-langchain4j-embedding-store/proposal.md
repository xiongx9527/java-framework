## Why

LangChain4j Embedding Store 当前同时维护中立 `AiEmbeddingStore*` 与 LangChain4j legacy Definition/Factory/Registry 两套体系，并通过双向转换连接；AutoConfigure 层还为 23 个厂商各维护一个公开 Adapter 类，叠加两层通用反射转发。该结构重复、依赖注入顺序不确定且难以维护，需要以中立 Store 体系为唯一外部抽象重新构建整条链路。

## What Changes

- 让 LangChain4j Store Adapter 直接消费 `AiEmbeddingStoreDefinition` 并产出 `AiEmbeddingStore`，移除 neutral/legacy 双向转换。
- **BREAKING**：删除 LangChain4j legacy `EmbeddingStoreDefinition`、`EmbeddingStoreProperties`、`EmbeddingStoreFactory`、`EmbeddingStoreRegistry`、`DefaultEmbeddingStoreRegistry`、`EmbeddingStoreException` 及旧 `EmbeddingStoreProviderAdapter`。
- 新建职责准确的 `LangChain4jEmbeddingStoreProviderAdapter` 扩展 seam，直接接收中立 Definition；内置 Adapter 严格检测零个、一个和多个匹配，多个匹配不再按 Spring 注入顺序选择第一个。
- 将 `adapter.store` 中 23 个逐厂商公开实现类删除，改为 23 个具名 Bean 方法声明 `ProviderSpec`，由一个通用反射 Adapter 执行创建规则。
- 以声明规则完整表达厂商必填属性、任一必填组、alias/fallback、Spring Bean 引用、builder setter、构造器和多参数方法调用，不为复杂厂商保留兼容壳或专用公开类。
- 保持现有 `fw-plus.ai.embedding-store` 配置前缀、provider key/alias、具名厂商 Bean 覆盖点和 `AiEmbeddingStore` 运行行为。
- 删除 Elasticsearch、Redis、Milvus 上游原生 Store 自动配置旁路；Azure AI Search 仅保留 ContentRetriever 自动配置并删除原生 Store Bean/配置字段。
- 同步迁移 MagicAPI、Tinyflow、自动配置、测试和 Maven 依赖；Model 链不在本次范围内。

## Capabilities

### New Capabilities

- `langchain4j-embedding-store-adapter`: 定义 LangChain4j 如何通过中立 Store Definition、确定性 Provider Adapter 选择和声明式厂商创建规则提供 Embedding Store。

### Modified Capabilities

无。当前主规格中尚无 Embedding Store capability。

## Impact

影响 Core 的 `fw-plus-ai-langchain4j-adapter`、中立 `fw-plus-ai-store-embedding` 的集成测试，以及 AI Boot AutoConfigure 中 Store、MagicAPI、Tinyflow 装配与测试。删除的 public legacy FQCN 和逐厂商实现类不提供兼容类型；调用方必须迁移至 `AiEmbeddingStoreDefinition`、`AiEmbeddingStoreFactory`、`AiEmbeddingStoreRegistry`、`AiEmbeddingStoreException` 与新的 LangChain4j Provider Adapter seam。Model 相关类型、配置和行为不变。
