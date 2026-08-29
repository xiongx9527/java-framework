## Context

当前 `adapter` 根包混合两条独立适配链：

1. 通用 `AiModel` 与 LangChain4j native model 之间的模型适配链。
2. 通用 `AiEmbeddingStore` 与 LangChain4j `EmbeddingStore` 之间的向量存储适配链。

两组类型内部各自高内聚，且不存在必须保留在同一包中的跨组 package-private 调用。现状失败证据为结构检查发现根包仍有 21 个直属 Java 类型，且 `model`、`store` 两个子包尚未建立。

## Goals / Non-Goals

**Goals**

- 用两个职责明确的子包替代扁平根包。
- 保持类名、可见性、方法签名和运行行为不变。
- 更新生产代码和测试中的全部引用，确保相关模块可编译并通过测试。

**Non-Goals**

- 不重命名类型或方法。
- 不改变模型或 Embedding Store 的创建、注册、异常转换逻辑。
- 不提供旧包名兼容桥接类。

## Decisions

### 1. 使用 `model` 与 `store` 两个子包

`adapter.model` 收纳 12 个模型相关类型：`DefaultLangChain4jModelFactory`、`DefaultLangChain4jModelRegistry`、`LangChain4jAiChatModel`、`LangChain4jAiEmbeddingModel`、`LangChain4jAiModel`、`LangChain4jModel`、`LangChain4jModelDefinition`、`LangChain4jModelException`、`LangChain4jModelFactory`、`LangChain4jModelProviderAdapter`、`LangChain4jModelRegistry`、`LangChain4jModelType`。

`adapter.store` 收纳 9 个向量存储相关类型：`DefaultEmbeddingStoreFactory`、`DefaultEmbeddingStoreRegistry`、`EmbeddingStoreDefinition`、`EmbeddingStoreException`、`EmbeddingStoreFactory`、`EmbeddingStoreProperties`、`EmbeddingStoreProviderAdapter`、`EmbeddingStoreRegistry`、`LangChain4jAiEmbeddingStore`。

该命名直接反映现有领域术语，并与 Boot 自动配置中的 `model`、`store` 分区一致。

### 2. 同步移动测试包

6 个现有测试按照被测类型迁移到对应 `model` 或 `store` 包。这样可继续访问原有 package-private 构造器和辅助方法，而无需扩大生产类型可见性。

### 3. 不保留旧包兼容类型

用户已明确接受 package public API 变更并选择继续 Hotfix。保留旧包 façade 会造成重复 API 和额外维护面，因此仓库内调用方统一切换到新包。

## Risks / Trade-offs

- 外部消费者使用旧全限定类名时会编译失败；这是已确认接受的 breaking package migration。
- 文件数较多，但变更应严格限制为目录移动、package 声明和 import 更新；任何逻辑 diff 都视为范围漂移。

## Verification

- RED：根包结构脚本确认 21 个直属 Java 类型仍存在且两个子包缺失。
- GREEN：根包不再包含直属 Java 类型，`model` 与 `store` 中类型数量分别为 12 和 9；测试对应为 3 和 3。
- 搜索旧 package import/声明，确认不存在遗留引用。
- 运行 `fw-plus-ai-langchain4j-adapter` 模块测试。
- 安装 Core 依赖后运行 Boot AI 自动配置相关编译/测试，确认跨层 import 全部迁移。
