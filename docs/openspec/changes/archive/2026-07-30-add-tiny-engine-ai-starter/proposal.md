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
