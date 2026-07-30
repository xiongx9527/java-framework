# Comet Design Handoff

- Change: add-tiny-engine-ai-starter
- Phase: design
- Mode: compact
- Context hash: 1ea109414688fab81ec69a7032698450141c824f6c7121de501ce5a96cdd0445

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/add-tiny-engine-ai-starter/proposal.md

- Source: openspec/changes/add-tiny-engine-ai-starter/proposal.md
- Lines: 1-33
- SHA256: 55396b7bc082e65c7af04840089b6e6c196fc953ff14ec72956c4be110bfa06d

```md
## Why

TinyEngine 当前 AI 能力散落在业务应用内：Chat 通过 OpenAI 兼容 HTTP 接口直连，RAG 通过 LangChain4j ONNX EmbeddingModel、Chroma EmbeddingStore 与本地 StorageService 组合完成。fw-plus-ai 需要提供一个可复用的 TinyEngine AI 扩展 starter，把 Chat、RAG、向量存储、模型配置和运行时 Provider 边界沉淀为框架能力，让接入方不再复制业务项目里的控制器、配置和降级逻辑。

AskData 当前已经有 Core 契约和 starter，但 AI 依赖边界仍未彻底收口：starter 已使用统一 LangChain4j adapter，模型 source 可从 `AiModelRegistry` 读取，但自动配置、模型注册和向量检索仍直接注入或暴露 `ChatLanguageModel`、`EmbeddingModel`、`EmbeddingStore<TextSegment>`、`ContentRetriever` 等 LangChain4j 原生类型。当前设计需要把 AskData 也纳入同一轮中立 AI 模块对齐，避免 TinyEngine AI starter 新增后形成两套 AI 依赖边界。

`abstract-ai-model-and-embedding` 与后续中立 adapter autodiscovery 已把公共模型和向量存储能力整理为 Java 8 的 `fw-plus-ai-model`、`fw-plus-ai-store-embedding`，并把 LangChain4j 放到默认 adapter/starter。TinyEngine AI starter 和 AskData 都应直接使用这两个中立 registry 与门面；LangChain4j 仍是默认完整 provider，但不能重新成为业务契约或主路径依赖。

## What Changes

- 新增 TinyEngine AI 扩展 starter 能力，面向 TinyEngine AI Chat、RAG 检索、知识库向量化、向量存储访问和模型配置提供统一接入入口。
- 定义 TinyEngine AI Provider SPI，表达 Chat、Streaming Chat、RAG、Embedding、Vector Store 和 Agent Runtime 等能力边界。
- 第一版提供 LangChain4j 默认 Provider，但 Provider 通过 `fw-plus-ai-model` 的 `AiModelRegistry` 与 `fw-plus-ai-store-embedding` 的 `AiEmbeddingStoreRegistry` 获取模型和向量 store 门面。
- 补齐 AskData 对当前中立 AI 模块的依赖边界：AskData 模型注册、默认模型 source、向量检索和 starter 依赖检查 SHALL 面向 `AiModelRegistry`、`AiEmbeddingStoreRegistry` 和中立门面，而不是 LangChain4j 原生模型/store 类型。
- AI 能力必须可通过配置独立启用或关闭；缺少模型、Embedding 能力或向量库配置时必须有明确失败或受控降级行为。
- 为 Spring AI、Spring AI Alibaba、AgentScope2 Java 版和业务自定义 Provider 保留 SPI 与配置枚举边界，但不把它们作为首版硬依赖。
- 不重构整个 fw-plus-ai 模型抽象，不升级主线 AI 依赖版本，不修改与 TinyEngine core 适配无关的模块。

## Capabilities

### New Capabilities
- `tiny-engine-ai-starter`: TinyEngine AI 扩展 starter、默认 LangChain4j Provider、Provider SPI、Chat/RAG/向量存储配置和启停降级契约。

### Modified Capabilities

- `askdata-query-engine`: AskData starter 和自动配置 SHALL 使用当前中立 AI 模型/向量存储 API 作为主路径，保留 LangChain4j adapter 作为默认运行时实现但不暴露旧原生契约。

## Impact

- 影响模块：`fw-plus-ai/fw-plus-ai-project` 下 AI Boot starter、AI 自动配置、AskData starter 和可能新增的 TinyEngine AI starter 模块；必要时只补充与依赖管理相关的 AI Boot BOM 条目。
- 依赖关系：TinyEngine AI starter 依赖 `integrate-tiny-engine-core` 提供 TinyEngine core 基础适配；TinyEngine AI 与 AskData 的模型和向量存储主路径依赖当前中立 `fw-plus-ai-model`、`fw-plus-ai-store-embedding`，默认运行时由 LangChain4j adapter/starter 提供。
- 运行时行为：接入方引入 starter 后可通过配置启用 TinyEngine AI；Chat 默认走 LangChain4j Provider；RAG 需要可用中立模型与向量存储配置，缺失时按能力开关和失败策略返回明确状态。AskData 继续保留既有 SQLBot/Core 行为，但模型与向量来源切到中立 registry。
- 兼容性：保持 Java 8 与 Spring Boot 2.7.18 兼容；不得使用 Java 9+ API；不得将 Spring AI、Spring AI Alibaba 或 AgentScope2 Java 版加入首版必需 classpath；不得把旧 LangChain4j 专用 model/store artifact 作为 TinyEngine AI 或 AskData 主路径依赖。

```

## openspec/changes/add-tiny-engine-ai-starter/design.md

- Source: openspec/changes/add-tiny-engine-ai-starter/design.md
- Lines: 1-104
- SHA256: 3e5edffb75488a26836fb6e245d6a9aae4e822fae4da7c69275b0d650c64e24f

[TRUNCATED]

```md
## Context

TinyEngine 参考实现里的 AI 能力目前直接放在业务应用内。`AiChatController` 暴露 `/app-center/api/ai/chat`、`/app-center/api/chat/completions`、`/app-center/api/encrypt-key` 和 `/app-center/api/ai/search`，其中 Chat 请求把 Authorization Bearer token 写回请求对象后委托 `AiChatV1Service`，流式响应返回 `text/event-stream`。`AiChatV1ServiceImpl` 直接使用 JDK HTTP Client 调用 OpenAI 兼容 `/chat/completions` 接口，手工拼接模型、messages、tools、stream、temperature、tool_choice、enable_search 等字段，并负责 EKEY_ API Key 解密。

RAG 侧由 `RAGConfig`、`VectorStoreConfig` 和 `StorageService` 组成。配置前缀为 `rag`，包含 chunkSize、chunkOverlap、maxResults、minScore、Chroma baseUrl、collectionName、ONNX model/tokenizer 路径、超时和批量参数。向量实现基于 LangChain4j 的 ONNX EmbeddingModel、Chroma EmbeddingStore、DocumentLoader、DocumentSplitter、EmbeddingSearchRequest 与 TextSegment 元数据，缺失模型或 Chroma 时使用 fallback 模型和 store。该实现对 TinyEngine 业务有价值，但不适合作为 fw-plus-ai 用户复制粘贴的应用代码。

AskData 当前不是完全没有接入当前 AI 模块，但边界仍不干净。`fw-plus-ai-askdata-spring-boot-starter` 已依赖统一 `fw-plus-ai-langchain4j-adapter-spring-boot-starter`，`LangChain4jModelAskDataAutoConfiguration` 已通过 `AiModelRegistry` 创建 model source；但 `AskDataAutoConfiguration` 仍直接 import 并注入 `ChatLanguageModel`、`EmbeddingModel`、`EmbeddingStore<TextSegment>`、`ContentRetriever`，`LangChain4jAskDataModelRegistry` 与 `EmbeddingStoreAskDataVectorRetriever` 也仍以 LangChain4j 原生类型作为主路径。当前 change 要把 AskData 一并对齐到中立 `fw-plus-ai-model` 和 `fw-plus-ai-store-embedding`。

fw-plus-ai 已有中立 `fw-plus-ai-model`、`fw-plus-ai-store-embedding`、中立 starter 和 LangChain4j adapter/starter。当前 change 应增加 TinyEngine AI starter，并补齐 AskData 中立 AI 依赖边界；不能回到旧 `LangChain4jModelRegistry` / `EmbeddingStoreRegistry` 主路径。实现必须保持 Java 8 与 Spring Boot 2.7.18 兼容，尤其不能照搬参考实现中的 `List.of()`、`String.isBlank()`、`InputStream.readAllBytes()`、`java.net.http.HttpClient` 等 Java 9+ API。

本 change 依赖 `integrate-tiny-engine-core`。TinyEngine core 应负责基础 domain、通用结果、异常、服务契约和资源等底层能力；AI 自动配置入口、Provider SPI 和 AI starter 装配由本 change 负责。

## Goals / Non-Goals

**Goals:**

- 在 `fw-plus-ai` 增加 TinyEngine AI 扩展 starter，支持接入方按配置启用 TinyEngine AI Chat、Streaming Chat、RAG 检索、知识库向量化和向量存储访问。
- 第一版默认 Provider 使用 LangChain4j，但通过 `AiModelRegistry`、`AiEmbeddingStoreRegistry` 和中立模型/store 门面获取底层能力。
- 补齐 AskData 对当前中立 AI 模块的依赖边界，使 AskData 模型、Embedding 和向量检索主路径使用 `fw-plus-ai-model` 与 `fw-plus-ai-store-embedding`。
- 定义 Java 8 兼容的 TinyEngine Provider SPI，让 LangChain4j、Spring AI、Spring AI Alibaba、AgentScope2 Java 版和业务自定义运行时可以表达清晰接入边界。
- 为模型、向量库或 RAG 依赖缺失提供可配置的失败或降级语义，避免静默返回不可诊断结果。
- 保持 TinyEngine AI starter 和 AskData starter 可以独立启用或关闭，不影响未接入这些能力的 fw-plus-ai 用户。

**Non-Goals:**

- 不把 Spring AI、Spring AI Alibaba、AgentScope2 Java 版作为首版硬依赖。
- 不重构整个 fw-plus-ai 模型抽象，不升级 LangChain4j、Spring AI 或 Spring Boot 主线版本。
- 不重新引入旧 `fw-plus-ai-langchain4j-model`、旧 `fw-plus-ai-langchain4j-store-embeding` 或旧 LangChain4j model/store starter 作为 TinyEngine AI 或 AskData 主路径依赖。
- 不重做 AskData SQLBot、治理、权限、观测、存储、任务恢复或 API/MCP 业务功能；本 change 只处理 AskData 与当前中立 AI 模型/向量模块的依赖边界。
- 不实现 TinyEngine 低代码核心能力；该部分由 `integrate-tiny-engine-core` 承接。
- 不把业务应用里的 OpenAI HTTP 直连代码原样搬入框架。
- 不提供完整 Agent 编排运行时；AgentScope2 Java 版仅作为 SPI 能力边界预留。

## Decisions

### Decision 1: 新增 TinyEngine AI starter，而非改造现有中立 AI starter

TinyEngine AI 的业务入口需要同时覆盖 Chat、SSE 流式 Chat、RAG、知识库文档处理、向量 store 选择和 Provider 选择。现有中立 AI specs 更偏底层模型与向量能力，直接改造会把 TinyEngine 业务语义扩散到基础模块。因此新增一个 TinyEngine AI starter，依赖现有中立底层能力，并在 TinyEngine 命名空间内组织配置和自动配置。

替代方案是修改 `ai-framework-neutral-model-abstraction` 或 `ai-framework-neutral-embedding-store-abstraction` 规格，让它们直接理解 TinyEngine 业务概念。该方案会扩大已有能力契约，且容易影响 Magic-API model/vector 插件和 AskData 用户，所以不采用。

### Decision 2: 默认 Provider 使用 LangChain4j，但主路径是中立 registry

仓库当前已经将模型与向量存储公共契约下沉到 `fw-plus-ai-model` 与 `fw-plus-ai-store-embedding`。TinyEngine 参考实现中的 RAG 实际使用 LangChain4j，因此第一版 Provider 仍使用 LangChain4j adapter 作为默认完整运行时；但 TinyEngine AI starter 的依赖和服务契约只面向 `AiModelRegistry`、`AiEmbeddingStoreRegistry` 与中立模型/store 门面。

旧 `LangChain4jModelRegistry`、旧 `EmbeddingStoreRegistry`、旧 LangChain4j model/store starter 只能作为历史迁移背景，不能作为新 TinyEngine AI starter 或 AskData 的主路径。Spring AI、Spring AI Alibaba、AgentScope2 Java 版的接入方式和版本边界可能与 Spring Boot 2.7.18、现有 BOM 或 Java 8 有冲突。第一版只定义 `TinyEngineAiProvider` / `TinyEngineAiRuntimeProvider` 一类 SPI，使这些 Provider 后续以独立 starter 或业务模块加入。

### Decision 3: AskData 模型注册改为中立模型主路径

AskData 保留 `AskDataModelRegistry` 作为业务边界，但默认实现应从 `AiModelRegistry` 解析 Chat 与 Embedding 模型门面，而不是直接收集 `Map<String, ChatLanguageModel>` 和 `Map<String, EmbeddingModel>`。LangChain4j adapter 可作为默认能力来源，但转换应封装在 adapter/source 内部，AskData 自动配置和业务接口不再直接暴露 LangChain4j 原生模型类型。

现有 `LangChain4jModelAskDataAutoConfiguration` 已经以 `AiModelRegistry` 为条件，这是可复用基础；实现阶段应进一步把 `LangChain4jAskDataModelRegistry` 重命名或替换为中立 `AiModelAskDataModelRegistry`，并保留兼容测试，确认缺少 adapter 时错误可诊断。

### Decision 4: AskData 向量检索改为中立向量 store 主路径

AskData 的默认 vector retriever 应优先通过 `AiEmbeddingStoreRegistry` 获取中立向量 store 门面，并通过 AskData embedding 模型或中立 store 查询 API 执行检索。`EmbeddingStoreAskDataVectorRetriever` 不应继续作为默认主路径绑定 `EmbeddingStore<TextSegment>` 和 `EmbeddingModel`。

若业务显式提供 LangChain4j `ContentRetriever` 或旧原生 `EmbeddingStore` Bean，可作为兼容扩展点保留，但必须受 `@ConditionalOnClass` / `@ConditionalOnBean` 隔离，且不能成为 starter 的必需依赖或默认依赖边界。

### Decision 5: Provider SPI 以能力声明和请求上下文为边界

TinyEngine Provider SPI 应至少表达 Chat、Streaming Chat、RAG Retrieval、Knowledge Indexing、Embedding、Vector Store 和 Agent Runtime。SPI 不应暴露 LangChain4j 专有类型作为唯一接口。默认 LangChain4j Provider 可在 adapter 内完成转换，业务接口使用 TinyEngine AI 请求、响应、文档、检索结果和能力描述对象。这样后续 Spring AI 或业务自定义 Provider 可以接入同一业务契约。

### Decision 6: 配置按 TinyEngine AI 命名空间组织，并保持启停独立

建议配置前缀使用 TinyEngine Boot 已采用风格，例如 `fw.plus.tiny-engine.ai`，并包含总开关、Provider、能力级开关、中立模型 key、中立向量 store key、RAG 参数和 `missing-dependency-policy`。能力关闭时不创建对应业务服务 Bean。能力开启但缺少必需配置时，默认 fail-fast；只有用户显式选择 degrade 时才注册受控降级实现。

### Decision 7: RAG 复用中立向量 store registry，避免绑定 Chroma 单一实现

参考实现固定使用 Chroma，同时通过 TextSegment metadata 保存 source、format、timestamp、collection，再在应用层按 collection 过滤。fw-plus-ai 已有 `AiEmbeddingStoreRegistry` 和中立 store 门面，TinyEngine AI starter 应通过 store key 选择中立向量 store，而不是新增 Chroma 专用主路径。

第一版可以提供文档加载、切分、Embedding 和存储编排服务，并允许配置默认集合、集合白名单和元数据字段。Chroma 只应作为 LangChain4j adapter 或 provider 配置结果。若用户未配置向量 store 且开启 RAG，按缺失依赖策略处理。

### Decision 8: API 层先提供服务契约，不强制复制 TinyEngine Controller 路由

参考实现的 Controller 路由可作为接入方或 sample 的兼容入口，但 starter 的核心价值是提供服务 Bean 和 Provider SPI。设计上优先交付 `TinyEngineAiChatService`、`TinyEngineRagService`、`TinyEngineKnowledgeBaseService` 等服务契约，由 TinyEngine core、Boot Web 层或 sample 再暴露兼容路由。

如果首版需要自动暴露 REST 路由，应受 `web.enabled` 和 path prefix 配置控制，且默认不得破坏宿主应用已有 Controller。

## Risks / Trade-offs

```

Full source: openspec/changes/add-tiny-engine-ai-starter/design.md

## openspec/changes/add-tiny-engine-ai-starter/tasks.md

- Source: openspec/changes/add-tiny-engine-ai-starter/tasks.md
- Lines: 1-73
- SHA256: 588a7abe09c61cc7f6f1126eb3416ecebe2547d8b9ac11dffb29b7669c29382b

```md
## 1. 前置确认

- [ ] 1.1 确认 `integrate-tiny-engine-core` 已完成并提供 TinyEngine core 基础领域能力、通用契约和可被 AI 层引用的服务边界。
- [ ] 1.2 盘点 `fw-plus-ai` 现有 `fw-plus-ai-model`、`fw-plus-ai-store-embedding`、中立 starter、AI Boot starter 聚合、AI Boot BOM 和 LangChain4j adapter/starter 的可复用边界。
- [ ] 1.3 盘点 AskData 当前模型与向量耦合点，包括 `AskDataAutoConfiguration`、`LangChain4jAskDataModelRegistry`、`LangChain4jAskDataModelSource`、`EmbeddingStoreAskDataVectorRetriever` 和 AskData starter POM。
- [ ] 1.4 确认目标模块仍使用 Java 8 与 Spring Boot 2.7.18，不引入与既有 LangChain4j 0.35.0、Spring AI 1.0.1 体系冲突的主线版本。

## 2. 模块与依赖

- [ ] 2.1 在 `fw-plus-ai` 中新增 TinyEngine AI starter 及必要的自动配置模块结构。
- [ ] 2.2 将 TinyEngine AI starter 注册到 AI Boot starter 聚合 POM。
- [ ] 2.3 在 AI Boot BOM 中管理 TinyEngine AI starter artifact，版本风格与当前层级保持一致。
- [ ] 2.4 为 TinyEngine AI starter 添加 TinyEngine core、`fw-plus-ai-model`、`fw-plus-ai-store-embedding` 和必要中立 starter 依赖，不把 Spring AI、Spring AI Alibaba、AgentScope2 Java 版作为必需依赖。
- [ ] 2.5 调整 AskData starter 依赖边界，确认默认 AI 能力来自中立模型/向量模块和统一 LangChain4j adapter，不依赖旧 LangChain4j model/store starter 坐标。
- [ ] 2.6 确认 TinyEngine AI starter 和 AskData starter 均不依赖旧 `fw-plus-ai-langchain4j-model`、旧 `fw-plus-ai-langchain4j-store-embeding` 或旧 LangChain4j model/store starter 作为主路径。

## 3. 配置与自动配置

- [ ] 3.1 定义 TinyEngine AI 配置属性，覆盖总开关、Provider 选择、Chat/RAG/Vector 能力级开关、中立模型 key、中立向量 store key、RAG 参数和缺失依赖策略。
- [ ] 3.2 实现自动配置条件，确保总开关关闭时不创建 TinyEngine AI Chat、RAG、知识库向量化和 Provider 运行时 Bean。
- [ ] 3.3 实现能力级条件，支持只启用 Chat 而不要求 EmbeddingModel 或向量存储。
- [ ] 3.4 实现 RAG 启用时的模型、Embedding 能力和中立向量 store 配置校验。
- [ ] 3.5 调整 AskData 默认自动配置，使 `AskDataModelRegistry` 和 `AskDataVectorRetriever` 默认从 `AiModelRegistry` / `AiEmbeddingStoreRegistry` 获取能力，并保留业务 Bean 覆盖点。

## 4. Provider SPI

- [ ] 4.1 定义 Java 8 兼容的 TinyEngine AI Provider SPI、能力枚举、Provider registry 和 unsupported capability 错误模型。
- [ ] 4.2 定义 TinyEngine Chat、Streaming Chat、RAG 检索、知识库写入、Embedding 和向量访问所需的请求/响应业务对象。
- [ ] 4.3 在 SPI 中预留 Spring AI、Spring AI Alibaba、AgentScope2 Java 版和业务自定义 Provider 的运行时标识与扩展点。
- [ ] 4.4 覆盖 Provider 选择逻辑：默认使用 LangChain4j adapter，显式自定义 Provider 可替换默认运行时。

## 5. LangChain4j 默认 Provider 与 AskData 模型

- [ ] 5.1 实现 LangChain4j Chat Provider，通过 `AiModelRegistry` 获取中立 Chat 模型门面。
- [ ] 5.2 实现 LangChain4j Streaming Chat Provider，通过 `AiModelRegistry` 获取中立 Streaming Chat 模型门面，并返回可由 Web 层转换为 SSE 的流式响应。
- [ ] 5.3 映射 TinyEngine Chat 参数，包括 model、messages、tools、stream、max_tokens、stream_options、presence_penalty、response_format、enable_thinking、tool_choice、parallel_tool_calls、enable_search、search_options 和 frequency_penalty。
- [ ] 5.4 对 Provider 不支持的参数实现忽略、转换或明确 unsupported 参数错误策略。
- [ ] 5.5 对不存在的模型 key 返回包含 key、Provider 和能力类型的明确错误。
- [ ] 5.6 将 AskData 默认模型 registry 改为中立模型主路径，避免默认实现直接收集 `ChatLanguageModel` / `EmbeddingModel`。
- [ ] 5.7 补充 AskData 模型 key 缺失、能力类型不匹配、adapter 缺失和业务自定义 `AskDataModelRegistry` 覆盖测试。

## 6. RAG、知识库服务与 AskData 向量检索

- [ ] 6.1 实现 TinyEngine RAG 服务，支持检索文本、maxResults、minScore 和 collection 参数。
- [ ] 6.2 实现知识库向量化服务，支持文档加载、格式过滤、元数据写入、chunk-size/chunk-overlap 切分和批量向量写入。
- [ ] 6.3 通过 `AiEmbeddingStoreRegistry` 按 key 获取中立向量 store，避免将 Chroma 或 LangChain4j 原生 store 作为 TinyEngine AI starter 的唯一主路径。
- [ ] 6.4 支持按集合检索和跨集合检索，单集合失败时返回该集合的受控错误状态或空结果。
- [ ] 6.5 对不存在或不支持格式的文档返回包含加载、跳过和失败数量的诊断信息。
- [ ] 6.6 将 AskData 默认 vector retriever 改为中立向量 store 主路径，避免默认实现直接绑定 `EmbeddingStore<TextSegment>`。
- [ ] 6.7 若保留 AskData LangChain4j 原生 `ContentRetriever` 或旧 store 桥接，确保其 optional classpath/bean 条件隔离且不进入 starter 必需依赖。

## 7. 失败与降级

- [ ] 7.1 实现默认 fail-fast 策略，Chat 或 RAG 启用但缺少必需模型、Provider 或向量 store 时启动失败。
- [ ] 7.2 实现显式 degrade 策略，注册降级实现并暴露能力不可用状态。
- [ ] 7.3 确保降级模式下 Chat、RAG 或 AskData 向量检索请求不会静默伪造成功，响应或异常包含不可用原因。

## 8. 兼容入口与样例

- [ ] 8.1 提供 TinyEngine AI Chat、RAG 搜索和知识库写入服务契约，供 Web 层或 TinyEngine core 复用。
- [ ] 8.2 如实现 REST 路由，添加独立 web enabled 开关和 path prefix 配置，避免与宿主 `/app-center/api/*` 路由冲突。
- [ ] 8.3 添加 sample 或文档，展示默认 LangChain4j adapter Chat、Streaming Chat、RAG、中立向量 store 配置、禁用 AI 和缺失依赖策略。
- [ ] 8.4 更新 AskData 文档或 starter README，说明 AskData 使用当前中立 AI 模型/向量配置和默认 LangChain4j adapter 的方式。

## 9. 验证

- [ ] 9.1 添加 TinyEngine AI 自动配置条件测试，覆盖总开关关闭、仅 Chat 启用、RAG 启用缺依赖 fail-fast 和显式 degrade。
- [ ] 9.2 添加 TinyEngine Provider SPI 测试，覆盖默认 LangChain4j Provider、自定义 Provider 和 unsupported capability。
- [ ] 9.3 添加 TinyEngine RAG 服务测试，覆盖文档写入、格式过滤、按集合检索、跨集合检索和向量 store key 缺失错误。
- [ ] 9.4 添加 AskData 中立模型/向量边界测试，覆盖 `AiModelRegistry`、`AiEmbeddingStoreRegistry`、adapter 缺失、业务 Bean 覆盖和旧原生桥接隔离。
- [ ] 9.5 运行相关 Maven Wrapper 编译或测试命令，确认 TinyEngine AI starter、AskData starter 及受影响 AI Boot 模块通过 Java 8 编译。
- [ ] 9.6 检查新增源码不包含 Java 9+ API，并确认未新增 Spring AI、Spring AI Alibaba、AgentScope2 Java 版硬依赖。
- [ ] 9.7 检查 TinyEngine AI starter 和 AskData starter 依赖树不包含旧 LangChain4j 专用 model/store artifact、旧 starter 坐标或直接 provider SDK 主路径依赖。

```

## openspec/changes/add-tiny-engine-ai-starter/specs/tiny-engine-ai-starter/spec.md

- Source: openspec/changes/add-tiny-engine-ai-starter/specs/tiny-engine-ai-starter/spec.md
- Lines: 1-154
- SHA256: 01047310cd17506f8c2ba8bcafb8f49ed17d5935c315e7b65a7814417fdf5f64

[TRUNCATED]

```md
## ADDED Requirements

### Requirement: TinyEngine AI starter 模块接入
系统 SHALL 在 `fw-plus-ai` 中提供 TinyEngine AI 扩展 starter，使应用可以通过引入该 starter 获得 TinyEngine AI Chat、RAG、知识库向量化和 Provider SPI 能力。

#### Scenario: starter 被 AI Boot 聚合管理
- **WHEN** 检查 AI Boot starter 聚合和 AI Boot BOM
- **THEN** 系统 SHALL 注册 TinyEngine AI starter 模块
- **AND** starter artifact SHALL 由 AI Boot BOM 使用 `${revision}` 或当前层级既有版本管理方式管理

#### Scenario: starter 依赖 TinyEngine core
- **WHEN** 检查 TinyEngine AI starter 的依赖
- **THEN** 它 SHALL 依赖 `integrate-tiny-engine-core` 产出的 TinyEngine core 能力
- **AND** 它 SHALL 不复制 TinyEngine core 的基础自动配置、结果模型或通用异常契约

#### Scenario: starter 使用中立 AI 主路径
- **WHEN** 检查 TinyEngine AI starter 的必需依赖和公开服务签名
- **THEN** 它 SHALL 使用 `fw-plus-ai-model` 与 `fw-plus-ai-store-embedding` 的中立 registry、门面和 adapter SPI 表达模型与向量存储能力
- **AND** 它 MUST NOT 将旧 `fw-plus-ai-langchain4j-model`、旧 `fw-plus-ai-langchain4j-store-embeding` 或旧 LangChain4j model/store starter 作为主路径依赖
- **AND** 它 MUST NOT 在 TinyEngine AI 业务契约中暴露 LangChain4j、Spring AI、Spring AI Alibaba 或 AgentScope2 Java 版原生类型

#### Scenario: starter 保持 Java 8 兼容
- **WHEN** 编译 TinyEngine AI starter 及其自动配置模块
- **THEN** 源码 SHALL 使用 Java 8 兼容 API
- **AND** 源码 MUST NOT 使用 `List.of()`、`Map.of()`、`String.isBlank()`、`InputStream.readAllBytes()`、`java.net.http.HttpClient` 等 Java 9+ API

### Requirement: AskData 使用当前中立 AI 模块
系统 SHALL 将 AskData starter 与自动配置的默认模型和向量能力主路径对齐到 `fw-plus-ai-model` 与 `fw-plus-ai-store-embedding`。

#### Scenario: AskData starter 依赖中立 AI 能力
- **WHEN** 检查 `fw-plus-ai-askdata-spring-boot-starter` 的 POM 和依赖树
- **THEN** starter SHALL 通过当前中立模型/向量模块和统一 LangChain4j adapter 获得默认 AI 能力
- **AND** starter MUST NOT 依赖旧 LangChain4j model/store starter 坐标
- **AND** starter MUST NOT 将 Spring AI、Spring AI Alibaba 或 AgentScope2 Java 版作为必需依赖

#### Scenario: AskData 模型注册使用中立 registry
- **WHEN** AskData 默认自动配置创建 `AskDataModelRegistry`
- **THEN** 默认实现 SHALL 从 `AiModelRegistry` 获取 Chat 与 Embedding 模型门面
- **AND** 默认自动配置 SHALL NOT 直接要求 `Map<String, ChatLanguageModel>` 或 `Map<String, EmbeddingModel>` 作为主路径输入
- **AND** 模型 key 缺失或能力不匹配时错误 SHALL 包含 key、能力类型和 provider 诊断信息

#### Scenario: AskData 向量检索使用中立 store registry
- **WHEN** AskData 默认自动配置创建 `AskDataVectorRetriever`
- **THEN** 默认实现 SHALL 从 `AiEmbeddingStoreRegistry` 获取中立向量 store 门面
- **AND** 默认实现 SHALL NOT 直接绑定 LangChain4j `EmbeddingStore<TextSegment>` 作为主路径
- **AND** 向量 store key 缺失、Embedding 能力缺失或查询失败时 SHALL 返回受控错误或空结果加不可用原因

#### Scenario: AskData 保留业务覆盖点
- **WHEN** 应用提供自定义 `AskDataModelRegistry` 或 `AskDataVectorRetriever` Bean
- **THEN** AskData 默认中立实现 SHALL 让位
- **AND** 兼容 LangChain4j 原生 `ContentRetriever` 或旧原生 store 的桥接若保留，SHALL 受 optional classpath 和 bean 条件隔离

### Requirement: AI 能力可独立启用和关闭
系统 SHALL 提供 TinyEngine AI 配置属性，使 Chat、RAG 和向量能力可以按总开关与能力级开关独立启用或关闭。

#### Scenario: 总开关关闭
- **WHEN** 应用配置 TinyEngine AI 总开关为关闭
- **THEN** 系统 SHALL 不创建 TinyEngine AI Chat、RAG、知识库向量化和 Provider 运行时 Bean
- **AND** 现有 fw-plus-ai 中立模型、向量 store、LangChain4j adapter、Spring AI、AskData 或 Magic-API AI 能力 SHALL 不受该开关影响

#### Scenario: 仅启用 Chat
- **WHEN** 应用启用 TinyEngine AI Chat 并关闭 RAG 和向量能力
- **THEN** 系统 SHALL 创建 Chat 服务和默认 Provider 绑定
- **AND** 系统 SHALL 不要求配置 EmbeddingModel 或向量存储

#### Scenario: 启用 RAG
- **WHEN** 应用启用 TinyEngine AI RAG
- **THEN** 系统 SHALL 校验可用的检索模型、Embedding 能力和中立向量存储配置
- **AND** 缺失必需依赖时 SHALL 按配置的缺失依赖策略 fail-fast 或 degrade

### Requirement: 默认 LangChain4j Provider
系统 SHALL 提供 LangChain4j 默认 Provider，用于首版 TinyEngine AI Chat、Streaming Chat、RAG、Embedding 和向量存储能力，但 TinyEngine AI starter 的业务契约 SHALL 面向中立 AI registry 和门面。

#### Scenario: 默认 Provider 提供非流式 Chat
- **WHEN** 应用引入 TinyEngine AI starter、安装 LangChain4j adapter starter 并配置可用的中立 Chat 模型
- **THEN** TinyEngine AI Chat 服务 SHALL 使用 LangChain4j Provider 返回非流式 Chat completion
- **AND** 请求 SHALL 支持模型 key、messages、tools、temperature、max tokens、response format、tool choice 和停止词等 TinyEngine Chat 参数

#### Scenario: 默认 Provider 提供流式 Chat
- **WHEN** TinyEngine Chat 请求声明 stream 为 true 且配置可用的中立 Streaming Chat 模型

```

Full source: openspec/changes/add-tiny-engine-ai-starter/specs/tiny-engine-ai-starter/spec.md
