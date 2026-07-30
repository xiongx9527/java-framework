---
comet_change: add-tiny-engine-ai-starter
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-30-add-tiny-engine-ai-starter
status: final
---

# TinyEngine AI Starter 与 AskData 中立 AI 依赖设计

## 背景与边界

TinyEngine 参考实现把 Chat、SSE Chat、RAG、知识库向量化和向量存储访问放在业务应用内，且实现细节直接依赖 OpenAI 兼容 HTTP 调用、LangChain4j ONNX EmbeddingModel、Chroma EmbeddingStore、TextSegment metadata 等运行时类型。fw-plus 需要把这些能力沉淀为可复用 starter，但不能让 TinyEngine AI 再绑定旧 LangChain4j 专用模型和向量 store 契约。

AskData 也纳入当前设计。它已经有 Core 契约和 starter，且 starter 已使用统一 LangChain4j adapter；但默认自动配置、模型注册和向量检索仍存在 `ChatLanguageModel`、`EmbeddingModel`、`EmbeddingStore<TextSegment>`、`ContentRetriever` 等 LangChain4j 原生主路径。当前 change 需要把 AskData 默认模型和向量能力对齐到 `fw-plus-ai-model` 与 `fw-plus-ai-store-embedding`。

当前主线已经具备 `fw-plus-ai-model`、`fw-plus-ai-store-embedding` 和 LangChain4j adapter/starter。TinyEngine AI starter 与 AskData 的第一版对齐都以这两个中立模块作为主路径：模型从 `AiModelRegistry` 获取，向量库从 `AiEmbeddingStoreRegistry` 获取。LangChain4j 是默认完整 provider，但只存在于 adapter/provider 层。

## 架构设计

TinyEngine AI starter 新增一层 TinyEngine 业务服务契约，核心 Bean 包括 `TinyEngineAiChatService`、`TinyEngineRagService`、`TinyEngineKnowledgeBaseService` 和 `TinyEngineAiProviderRegistry`。这些服务对 Web 层和 sample 暴露 TinyEngine 请求/响应对象，不暴露 LangChain4j、Spring AI、AgentScope 或其他原生框架类型。

Provider SPI 负责把 TinyEngine 业务请求转给具体运行时。第一版提供 `langchain4j` 默认 Provider，内部通过中立 `AiModelRegistry` 获取 Chat、Streaming Chat、Embedding 模型门面，通过 `AiEmbeddingStoreRegistry` 获取中立向量 store 门面。后续 Spring AI、Spring AI Alibaba、AgentScope2 Java 版或业务自定义 Provider 通过独立 starter 或业务 Bean 注册，不进入首版必需 classpath。

AskData 保留 `AskDataModelRegistry`、`AskDataVectorRetriever` 等业务边界，但默认实现改成中立能力来源。默认模型 registry 从 `AiModelRegistry` 解析 Chat 与 Embedding 模型门面；默认向量 retriever 从 `AiEmbeddingStoreRegistry` 获取中立 store 门面。业务提供自定义 `AskDataModelRegistry` 或 `AskDataVectorRetriever` 时，自动配置让位。

配置前缀使用 TinyEngine Boot 已采用的风格，建议为 `fw.plus.tiny-engine.ai`。配置包含总开关、Provider 选择、Chat/RAG/Vector 能力级开关、模型 key、向量 store key、RAG chunk 参数、collection、缺失依赖策略和可选 Web 路由开关。总开关关闭时不创建 TinyEngine AI 运行时 Bean；能力关闭时不要求该能力的下游依赖。AskData 继续使用 `fw.plus.ai.askdata`，但模型与向量 key 语义指向中立 registry。

## 数据流

Chat 数据流从 Web 层或业务代码进入 `TinyEngineAiChatService`，服务根据请求的 stream 标记选择 Chat 或 Streaming Chat 能力，并通过 Provider registry 找到当前 Provider。Provider 根据配置 key 从 `AiModelRegistry` 获取中立模型门面，完成参数映射后返回 TinyEngine Chat 响应或流式片段。未支持的参数必须按配置策略忽略、转换或返回明确 unsupported 参数错误。

RAG 数据流由 `TinyEngineRagService` 接收 query、maxResults、minScore 和 collection。服务先校验 RAG 能力、Embedding 模型和向量 store 可用性，再通过中立 store 门面执行检索并返回文本片段、分数和 metadata。跨集合检索按集合隔离执行，单集合失败时在该集合结果中返回受控错误或空结果，不影响其他集合。

AskData 模型调用继续从 `DefaultAskDataFlowExecutor` 使用 `AskDataModelRegistry` 进入。默认 registry 内部通过 `AiModelRegistry` 解析模型 key 和能力类型，再适配为 AskData 的 `AskDataChatModel` / `AskDataEmbeddingModel`。AskData 向量检索从 `AskDataVectorRetriever` 进入，默认 retriever 使用 `AiEmbeddingStoreRegistry` 和中立查询/匹配结果类型返回 `AskDataRetrievedDocument`。

知识库写入数据流由 `TinyEngineKnowledgeBaseService` 接收文档路径、文档集 ID 和 collection。服务负责文档存在性与格式过滤，按配置 chunk-size/chunk-overlap 切分内容，写入 source、format、timestamp、collection 等 metadata，再调用 Provider 的 Embedding 能力和中立向量 store 门面写入向量数据。

## 失败与降级

默认策略是 fail-fast：Chat 或 RAG 能力开启但缺少 Provider、模型 key、Embedding 能力或向量 store key 时，应用启动失败，错误包含 key、Provider 和能力类型，且不得泄漏凭证。

用户显式配置 degrade 时，starter 注册受控降级实现，并暴露能力不可用状态。Chat、RAG 或 AskData 检索请求命中不可用能力时，返回明确错误或空结果加不可用原因，不静默伪造成功。

AskData 若业务显式提供自定义 `AskDataModelRegistry` 或 `AskDataVectorRetriever`，默认中立实现让位。若保留 LangChain4j 原生 `ContentRetriever` 或旧原生 store 桥接，必须受 optional classpath 和 bean 条件隔离，且不能进入 starter 必需依赖边界。

## 依赖边界

TinyEngine AI starter 必须依赖 TinyEngine core、`fw-plus-ai-model`、`fw-plus-ai-store-embedding` 和必要中立 starter。LangChain4j 默认实现通过 `fw-plus-ai-langchain4j-adapter-spring-boot-starter` 或等价 adapter 进入。

AskData starter 可以保留统一 LangChain4j adapter 作为默认运行时来源，但默认模型和向量边界必须面向 `AiModelRegistry` 和 `AiEmbeddingStoreRegistry`。AskData starter 与 TinyEngine AI starter 都不得把旧 `fw-plus-ai-langchain4j-model`、旧 `fw-plus-ai-langchain4j-store-embeding`、旧 LangChain4j model/store starter、Spring AI、Spring AI Alibaba、AgentScope2 Java 版或 provider SDK 作为必需主路径依赖。

## 测试策略

TinyEngine 自动配置测试覆盖总开关关闭、仅 Chat 启用、RAG 缺模型/store fail-fast、显式 degrade、自定义 Provider 替换默认 Provider。

TinyEngine Provider SPI 测试覆盖能力声明、unsupported capability、模型 key 不存在、LangChain4j 默认 Provider 通过中立 registry 获取 Chat/Streaming/Embedding/Vector 能力。

AskData 中立边界测试覆盖默认 `AskDataModelRegistry` 从 `AiModelRegistry` 获取 Chat/Embedding 能力、默认 `AskDataVectorRetriever` 从 `AiEmbeddingStoreRegistry` 获取 store、adapter 缺失、模型/store key 缺失、业务 Bean 覆盖，以及可选 LangChain4j 原生桥接的 classpath 条件隔离。

RAG 和知识库测试覆盖文档写入、格式过滤、metadata 写入、按集合检索、跨集合检索、单集合失败隔离和向量 store key 缺失错误。

依赖边界测试扫描 TinyEngine AI starter 和 AskData starter 依赖树和 POM，确认不包含旧 LangChain4j 专用 model/store artifact、旧 starter 坐标、Spring AI、Spring AI Alibaba、AgentScope2 Java 版或直接 provider SDK 主路径依赖。

Java 8 验证编译 TinyEngine AI starter、AskData starter 和受影响 AI Boot 模块，并扫描新增源码不得使用 `List.of()`、`Map.of()`、`String.isBlank()`、`InputStream.readAllBytes()`、`java.net.http.HttpClient` 等 Java 9+ API。

## 实现顺序

1. 先确认 TinyEngine core、AI 中立模块、AskData 当前模型/向量耦合点和 LangChain4j adapter/starter 的最终 artifactId、包名和配置前缀。
2. 新增 TinyEngine AI starter、配置属性和自动配置条件，先落禁用和仅 Chat 启用场景。
3. 定义 TinyEngine AI 请求/响应对象、Provider SPI、Provider registry 和错误模型。
4. 实现 LangChain4j 默认 Provider，通过中立 registry 获取模型和向量 store。
5. 将 AskData 默认模型和向量检索主路径迁移到 `AiModelRegistry` 与 `AiEmbeddingStoreRegistry`，保留业务覆盖点和可选兼容桥。
6. 实现 TinyEngine RAG 和知识库向量化服务，补齐 fail-fast/degrade 语义。
7. 增加 sample 或文档，并运行 Maven 与依赖边界验证。
