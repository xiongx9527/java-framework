## Why

TinyEngine 当前 AI 能力散落在业务应用内：Chat 通过 OpenAI 兼容 HTTP 接口直连，RAG 通过 LangChain4j ONNX EmbeddingModel、Chroma EmbeddingStore 与本地 StorageService 组合完成。fw-plus-ai 需要提供一个可复用的 TinyEngine AI 扩展 starter，把 Chat、RAG、向量存储、模型配置和运行时 Provider 边界沉淀为框架能力，让接入方不再复制业务项目里的控制器、配置和降级逻辑。

第一版应复用仓库既有 LangChain4j 0.35.0 和 Spring Boot 2.7.18 体系，默认 Provider 使用 LangChain4j；Spring AI、Spring AI Alibaba、AgentScope2 Java 版和业务自定义运行时只在 Provider SPI 层预留接入边界，避免首版引入新的硬依赖或 BOM 冲突。

## What Changes

- 新增 TinyEngine AI 扩展 starter 能力，面向 TinyEngine AI Chat、RAG 检索、知识库向量化、向量存储访问和模型配置提供统一接入入口。
- 定义 TinyEngine AI Provider SPI，表达 Chat、Streaming Chat、RAG、Embedding、Vector Store 和 Agent Runtime 等能力边界。
- 第一版提供 LangChain4j 默认 Provider，复用现有 `fw-plus-ai` / `fw-plus-core` 中的 LangChain4j 模型和向量存储能力。
- AI 能力必须可通过配置独立启用或关闭；缺少模型、EmbeddingModel 或向量库配置时必须有明确失败或受控降级行为。
- 为 Spring AI、Spring AI Alibaba、AgentScope2 Java 版和业务自定义 Provider 保留 SPI 与配置枚举边界，但不把它们作为首版硬依赖。
- 不重构整个 fw-plus-ai 模型抽象，不升级主线 AI 依赖版本，不修改与 TinyEngine core 适配无关的模块。

## Capabilities

### New Capabilities
- `tiny-engine-ai-starter`: TinyEngine AI 扩展 starter、默认 LangChain4j Provider、Provider SPI、Chat/RAG/向量存储配置和启停降级契约。

### Modified Capabilities

无。现有 LangChain4j 模型、starter 与向量存储能力作为依赖复用，本 change 不改变它们的既有需求契约。

## Impact

- 影响模块：`fw-plus-ai/fw-plus-ai-project` 下 AI Boot starter、AI 自动配置和可能新增的 TinyEngine AI starter 模块；必要时只补充与依赖管理相关的 AI Boot BOM 条目。
- 依赖关系：依赖 `integrate-tiny-engine-core` 提供 TinyEngine core 基础适配；默认运行时依赖既有 LangChain4j 0.35.0 能力。
- 运行时行为：接入方引入 starter 后可通过配置启用 TinyEngine AI；Chat 默认走 LangChain4j Provider；RAG 需要可用模型与向量存储配置，缺失时按能力开关和失败策略返回明确状态。
- 兼容性：保持 Java 8 与 Spring Boot 2.7.18 兼容；不得使用 Java 9+ API；不得将 Spring AI、Spring AI Alibaba 或 AgentScope2 Java 版加入首版必需 classpath。
