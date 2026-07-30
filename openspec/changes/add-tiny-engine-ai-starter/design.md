## Context

TinyEngine 参考实现里的 AI 能力目前直接放在业务应用内。`AiChatController` 暴露 `/app-center/api/ai/chat`、`/app-center/api/chat/completions`、`/app-center/api/encrypt-key` 和 `/app-center/api/ai/search`，其中 Chat 请求把 Authorization Bearer token 写回请求对象后委托 `AiChatV1Service`，流式响应返回 `text/event-stream`。`AiChatV1ServiceImpl` 直接使用 JDK HTTP Client 调用 OpenAI 兼容 `/chat/completions` 接口，手工拼接模型、messages、tools、stream、temperature、tool_choice、enable_search 等字段，并负责 EKEY_ API Key 解密。

RAG 侧由 `RAGConfig`、`VectorStoreConfig` 和 `StorageService` 组成。配置前缀为 `rag`，包含 chunkSize、chunkOverlap、maxResults、minScore、Chroma baseUrl、collectionName、ONNX model/tokenizer 路径、超时和批量参数。向量实现基于 LangChain4j 的 ONNX EmbeddingModel、Chroma EmbeddingStore、DocumentLoader、DocumentSplitter、EmbeddingSearchRequest 与 TextSegment 元数据，缺失模型或 Chroma 时使用 fallback 模型和 store。该实现对 TinyEngine 业务有价值，但不适合作为 fw-plus-ai 用户复制粘贴的应用代码。

fw-plus-ai 已有 LangChain4j 0.35.0、Spring AI 1.0.1、AI Boot starter、LangChain4j 模型抽象和多向量存储相关规格。当前 change 应增加 TinyEngine AI starter，而不是重写这些底层能力。实现必须保持 Java 8 与 Spring Boot 2.7.18 兼容，尤其不能照搬参考实现中的 `List.of()`、`String.isBlank()`、`InputStream.readAllBytes()`、`java.net.http.HttpClient` 等 Java 9+ API。

本 change 依赖 `integrate-tiny-engine-core`。TinyEngine core 应负责基础 domain、通用结果、异常、服务契约和资源等底层能力；AI 自动配置入口、Provider SPI 和 AI starter 装配由本 change 负责。

## Goals / Non-Goals

**Goals:**

- 在 `fw-plus-ai` 增加 TinyEngine AI 扩展 starter，支持接入方按配置启用 TinyEngine AI Chat、Streaming Chat、RAG 检索、知识库向量化和向量存储访问。
- 第一版默认 Provider 使用 LangChain4j，并复用现有 LangChain4j 模型 registry、模型配置和多向量存储 registry/factory 能力。
- 定义 Java 8 兼容的 Provider SPI，让 LangChain4j、Spring AI、Spring AI Alibaba、AgentScope2 Java 版和业务自定义运行时可以表达清晰接入边界。
- 为模型、向量库或 RAG 依赖缺失提供可配置的失败或降级语义，避免静默返回不可诊断结果。
- 保持 AI starter 可以独立启用或关闭，不影响未接入 TinyEngine AI 的 fw-plus-ai 用户。

**Non-Goals:**

- 不把 Spring AI、Spring AI Alibaba、AgentScope2 Java 版作为首版硬依赖。
- 不重构整个 fw-plus-ai 模型抽象，不升级 LangChain4j、Spring AI 或 Spring Boot 主线版本。
- 不实现 TinyEngine 低代码核心能力；该部分由 `integrate-tiny-engine-core` 承接。
- 不把业务应用里的 OpenAI HTTP 直连代码原样搬入框架。
- 不提供完整 Agent 编排运行时；AgentScope2 Java 版仅作为 SPI 能力边界预留。

## Decisions

### Decision 1: 新增 TinyEngine AI starter，而非改造现有 LangChain4j starter

TinyEngine AI 的业务入口需要同时覆盖 Chat、SSE 流式 Chat、RAG、知识库文档处理、向量 store 选择和 Provider 选择。现有 LangChain4j specs 更偏底层模型与向量能力，直接改造会把业务语义扩散到基础模块。因此新增一个 TinyEngine AI starter，依赖现有底层能力，并在 TinyEngine 命名空间内组织配置和自动配置。

替代方案是修改 `langchain4j-model-module` 或 `langchain4j-embedding-store-starter` 规格，让它们直接理解 TinyEngine 业务概念。该方案会扩大已有能力契约，且容易影响 Magic-API 模型和向量插件用户，所以不采用。

### Decision 2: 默认 Provider 使用 LangChain4j，其他运行时只作为 SPI 候选

仓库当前 BOM 与规格已经围绕 LangChain4j 0.35.0 建立模型、starter 和向量能力。TinyEngine 参考实现中的 RAG 也实际使用 LangChain4j。因此第一版 Provider 使用 LangChain4j，Chat 使用现有模型 registry 中的 Chat/Streaming Chat 能力，Embedding 和 Vector Store 使用现有向量 registry 或明确配置的 LangChain4j 对象。

Spring AI、Spring AI Alibaba、AgentScope2 Java 版的接入方式和版本边界可能与 Spring Boot 2.7.18、现有 BOM 或 Java 8 有冲突。第一版只定义 `TinyEngineAiProvider` / `TinyEngineAiRuntimeProvider` 一类 SPI，使这些 Provider 后续以独立 starter 或业务模块加入。

### Decision 3: Provider SPI 以能力声明和请求上下文为边界

Provider SPI 应至少表达以下能力：

- Chat：同步非流式 Chat completion。
- Streaming Chat：SSE 或流式 token 响应。
- RAG Retrieval：基于查询从知识库检索上下文。
- Knowledge Indexing：加载、切分、向量化并写入知识库。
- Embedding：文本向量化。
- Vector Store：按 key 或默认 store 访问向量库。
- Agent Runtime：保留 AgentScope2 Java 版或业务 agent runtime 边界。

SPI 不应暴露 LangChain4j 专有类型作为唯一接口。默认 LangChain4j Provider 可在 adapter 内完成转换，业务接口使用 TinyEngine AI 请求、响应、文档、检索结果和能力描述对象。这样后续 Spring AI 或业务自定义 Provider 可以接入同一业务契约。

### Decision 4: 配置按 TinyEngine AI 命名空间组织，并保持启停独立

建议配置前缀使用 TinyEngine core 下的统一命名空间，例如 `fw-plus.tiny-engine.ai`，并包含：

- `enabled`：总开关。
- `provider`：默认 `langchain4j`，允许声明 `spring-ai`、`spring-ai-alibaba`、`agentscope2`、`custom` 等枚举或字符串。
- `chat.enabled`、`rag.enabled`、`vector.enabled`：能力级开关。
- `model.chat-key`、`model.streaming-chat-key`、`model.embedding-key`：引用现有模型 registry。
- `vector.store-key`：引用现有向量 store registry。
- `rag.chunk-size`、`rag.chunk-overlap`、`rag.max-results`、`rag.min-score`、`rag.collections`、`rag.fallback-policy`：映射参考实现的 RAG 行为。
- `missing-dependency-policy`：缺少模型或向量库时选择 `fail-fast` 或 `degrade`。

能力关闭时不创建对应业务服务 Bean。能力开启但缺少必需配置时，默认应 fail-fast；只有用户显式选择 degrade 时才注册受控降级实现，并通过状态对象、异常或日志说明不可用原因。

### Decision 5: RAG 复用现有向量 registry，避免绑定 Chroma 单一实现

参考实现固定使用 Chroma，同时通过 TextSegment metadata 保存 source、format、timestamp、collection，再在应用层按 collection 过滤。fw-plus-ai 已有多向量 store registry/factory 规格，TinyEngine AI starter 应通过 store key 选择向量 store，而不是新增 Chroma 专用主路径。

第一版可以提供 LangChain4j 文档加载、切分、Embedding 和存储编排服务，并允许配置默认集合、集合白名单和元数据字段。Chroma 只应作为现有 LangChain4j store provider 的一种配置结果。若用户未配置向量 store 且开启 RAG，按缺失依赖策略处理。

### Decision 6: API 层先提供服务契约，不强制复制 TinyEngine Controller 路由

参考实现的 Controller 路由可作为接入方或 sample 的兼容入口，但 starter 的核心价值是提供服务 Bean 和 Provider SPI。设计上优先交付 `TinyEngineAiChatService`、`TinyEngineRagService`、`TinyEngineKnowledgeBaseService` 等服务契约，由 TinyEngine core 或 sample 再暴露兼容路由。

如果首版需要自动暴露 REST 路由，应受 `web.enabled` 和 path prefix 配置控制，且默认不得破坏宿主应用已有 Controller。

## Risks / Trade-offs

- [Risk] LangChain4j 0.35.0 与较新 Spring AI/AgentScope2 API 术语不同，SPI 过度贴近某一方会导致后续难接入。→ Mitigation: SPI 使用 TinyEngine 业务对象和能力枚举，Provider adapter 内转换运行时类型。
- [Risk] 参考实现存在 Java 9+ API，直接迁移会破坏 core/AI 的 Java 8 编译约束。→ Mitigation: 任务中明确 Java 8 API 替代检查，并在实现验证中编译目标模块。
- [Risk] RAG 降级如果返回空结果，用户可能误判为没有知识命中。→ Mitigation: 默认 fail-fast；degrade 必须暴露能力状态和明确不可用原因。
- [Risk] 新 starter 若默认自动创建 Controller，可能与宿主 TinyEngine 应用已有路由冲突。→ Mitigation: API 路由不作为核心默认行为；如实现必须暴露，使用独立开关和可配置 path。
- [Risk] 依赖 `integrate-tiny-engine-core` 的类型或配置命名尚未落地时，AI starter 任务会被阻塞。→ Mitigation: 实现前先确认 core change 已完成，并把 AI starter 与 core 的交互点收敛到少量接口。

## Migration Plan

1. 在实现阶段确认 `integrate-tiny-engine-core` 已提供 TinyEngine core 基础领域能力、通用契约和可被 AI 层引用的服务边界。
2. 新增 TinyEngine AI starter 与自动配置，默认关闭或按 `enabled` 控制，避免影响现有应用。
3. 添加 LangChain4j 默认 Provider，复用已有模型和向量 store registry；对缺失模型或向量库执行 fail-fast/degrade 策略。
4. 在 sample 中演示从参考实现迁移：Chat 请求、SSE 流式响应、RAG 检索、知识库写入和禁用 AI 的行为。
5. 回滚时移除 starter 依赖或关闭 `fw-plus.tiny-engine.ai.enabled`，不影响既有 fw-plus-ai LangChain4j 模块。

## Open Questions

- TinyEngine core 最终配置前缀是否固定为 `fw-plus.tiny-engine`，AI starter 是否应完全复用该前缀。
- 首版是否需要自动暴露与参考实现一致的 REST 路由，还是只提供服务 Bean 和 sample。
- API Key 加解密是否复用现有 fw-plus 安全/加密能力，还是由接入方在 Provider 请求上下文中自行注入明文凭据。
