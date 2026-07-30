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

- [Risk] LangChain4j 0.35.0 与较新 Spring AI/AgentScope2 API 术语不同，SPI 过度贴近某一方会导致后续难接入。→ Mitigation: SPI 使用 TinyEngine 业务对象和能力枚举，Provider adapter 内转换运行时类型。
- [Risk] 参考实现存在 Java 9+ API，直接迁移会破坏 core/AI 的 Java 8 编译约束。→ Mitigation: 任务中明确 Java 8 API 替代检查，并在实现验证中编译目标模块。
- [Risk] RAG 降级如果返回空结果，用户可能误判为没有知识命中。→ Mitigation: 默认 fail-fast；degrade 必须暴露能力状态和明确不可用原因。
- [Risk] AskData 仍有多个 LangChain4j 原生兼容入口，直接删除可能破坏已有业务 Bean。→ Mitigation: 默认主路径切中立 registry；旧原生入口仅作为可选兼容桥并加 classpath/bean 条件，不进入 starter 必需依赖边界。
- [Risk] 中立模型/store 门面可能无法覆盖 TinyEngine 或 AskData 的全部参数。→ Mitigation: TinyEngine SPI 和 AskData 业务 registry 保留业务参数，Provider 对 unsupported 参数执行忽略、转换或明确错误策略；如确需扩展中立 AI 抽象，后续单独 change 处理。
- [Risk] 后续实现误接旧 LangChain4j 专用 artifact。→ Mitigation: 任务中加入依赖边界检查，扫描旧 artifactId、旧 starter 坐标和直接 provider SDK 依赖。

## Migration Plan

1. 在实现阶段确认 `integrate-tiny-engine-core` 已提供 TinyEngine core 基础领域能力、通用契约和可被 AI 层引用的服务边界。
2. 确认 AskData 当前 `AiModelRegistry` source、`AskDataModelRegistry`、`AskDataVectorRetriever` 和 starter 依赖边界。
3. 新增 TinyEngine AI starter 与自动配置，默认关闭或按 `enabled` 控制，避免影响现有应用。
4. 添加 LangChain4j 默认 Provider，通过 `AiModelRegistry` 和 `AiEmbeddingStoreRegistry` 复用中立模型和向量 store；对缺失模型或向量库执行 fail-fast/degrade 策略。
5. 将 AskData 默认模型和向量检索主路径迁移到 `AiModelRegistry` 与 `AiEmbeddingStoreRegistry`，保留可选 LangChain4j 兼容桥。
6. 在 sample 或文档中演示 TinyEngine AI 与 AskData 如何使用当前中立 AI 配置、默认 LangChain4j adapter、禁用 AI 和缺失依赖策略。
7. 回滚时移除 TinyEngine AI starter 依赖或关闭 `fw.plus.tiny-engine.ai.enabled`；AskData 可回退到业务自定义 `AskDataModelRegistry` / `AskDataVectorRetriever` Bean，不影响既有 Core 契约。

## Open Questions

- TinyEngine core 最终配置前缀是否固定为 `fw.plus.tiny-engine`，AI starter 是否应完全复用该前缀。
- 首版是否需要自动暴露与参考实现一致的 REST 路由，还是只提供服务 Bean 和 sample。
- API Key 加解密是否复用现有 fw-plus 安全/加密能力，还是由接入方在 Provider 请求上下文中自行注入明文凭据。
- AskData 是否保留 LangChain4j 原生 `ContentRetriever` 作为显式兼容入口，还是只保留业务自定义 `AskDataVectorRetriever` 覆盖点。
