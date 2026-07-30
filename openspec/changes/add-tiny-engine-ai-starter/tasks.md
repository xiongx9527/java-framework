## 1. 前置确认

- [x] 1.1 确认 `integrate-tiny-engine-core` 已完成并提供 TinyEngine core 基础领域能力、通用契约和可被 AI 层引用的服务边界。
- [x] 1.2 盘点 `fw-plus-ai` 现有 `fw-plus-ai-model`、`fw-plus-ai-store-embedding`、中立 starter、AI Boot starter 聚合、AI Boot BOM 和 LangChain4j adapter/starter 的可复用边界。
- [x] 1.3 盘点 AskData 当前模型与向量耦合点，包括 `AskDataAutoConfiguration`、`LangChain4jAskDataModelRegistry`、`LangChain4jAskDataModelSource`、`EmbeddingStoreAskDataVectorRetriever` 和 AskData starter POM。
- [x] 1.4 确认目标模块仍使用 Java 8 与 Spring Boot 2.7.18，不引入与既有 LangChain4j 0.35.0、Spring AI 1.0.1 体系冲突的主线版本。

## 2. 模块与依赖

- [x] 2.1 在 `fw-plus-ai` 中新增 TinyEngine AI starter 及必要的自动配置模块结构。
- [x] 2.2 将 TinyEngine AI starter 注册到 AI Boot starter 聚合 POM。
- [x] 2.3 在 AI Boot BOM 中管理 TinyEngine AI starter artifact，版本风格与当前层级保持一致。
- [x] 2.4 为 TinyEngine AI starter 添加 TinyEngine core、`fw-plus-ai-model`、`fw-plus-ai-store-embedding` 和必要中立 starter 依赖，不把 Spring AI、Spring AI Alibaba、AgentScope2 Java 版作为必需依赖。
- [x] 2.5 调整 AskData starter 依赖边界，确认默认 AI 能力来自中立模型/向量模块和统一 LangChain4j adapter，不依赖旧 LangChain4j model/store starter 坐标。
- [x] 2.6 确认 TinyEngine AI starter 和 AskData starter 均不依赖旧 `fw-plus-ai-langchain4j-model`、旧 `fw-plus-ai-langchain4j-store-embeding` 或旧 LangChain4j model/store starter 作为主路径。

## 3. 配置与自动配置

- [x] 3.1 定义 TinyEngine AI 配置属性，覆盖总开关、Provider 选择、Chat/RAG/Vector 能力级开关、中立模型 key、中立向量 store key、RAG 参数和缺失依赖策略。
- [x] 3.2 实现自动配置条件，确保总开关关闭时不创建 TinyEngine AI Chat、RAG、知识库向量化和 Provider 运行时 Bean。
- [x] 3.3 实现能力级条件，支持只启用 Chat 而不要求 EmbeddingModel 或向量存储。
- [x] 3.4 实现 RAG 启用时的模型、Embedding 能力和中立向量 store 配置校验。
- [x] 3.5 调整 AskData 默认自动配置，使 `AskDataModelRegistry` 和 `AskDataVectorRetriever` 默认从 `AiModelRegistry` / `AiEmbeddingStoreRegistry` 获取能力，并保留业务 Bean 覆盖点。

## 4. Provider SPI

- [x] 4.1 定义 Java 8 兼容的 TinyEngine AI Provider SPI、能力枚举、Provider registry 和 unsupported capability 错误模型。
- [x] 4.2 定义 TinyEngine Chat、Streaming Chat、RAG 检索、知识库写入、Embedding 和向量访问所需的请求/响应业务对象。
- [x] 4.3 在 SPI 中预留 Spring AI、Spring AI Alibaba、AgentScope2 Java 版和业务自定义 Provider 的运行时标识与扩展点。
- [x] 4.4 覆盖 Provider 选择逻辑：默认使用 LangChain4j adapter，显式自定义 Provider 可替换默认运行时。

## 5. LangChain4j 默认 Provider 与 AskData 模型

- [x] 5.1 实现 LangChain4j Chat Provider，通过 `AiModelRegistry` 获取中立 Chat 模型门面。
- [x] 5.2 实现 LangChain4j Streaming Chat Provider，通过 `AiModelRegistry` 获取中立 Streaming Chat 模型门面，并返回可由 Web 层转换为 SSE 的流式响应。
- [x] 5.3 映射 TinyEngine Chat 参数，包括 model、messages、tools、stream、max_tokens、stream_options、presence_penalty、response_format、enable_thinking、tool_choice、parallel_tool_calls、enable_search、search_options 和 frequency_penalty。
- [x] 5.4 对 Provider 不支持的参数实现忽略、转换或明确 unsupported 参数错误策略。
- [x] 5.5 对不存在的模型 key 返回包含 key、Provider 和能力类型的明确错误。
- [x] 5.6 将 AskData 默认模型 registry 改为中立模型主路径，避免默认实现直接收集 `ChatLanguageModel` / `EmbeddingModel`。
- [x] 5.7 补充 AskData 模型 key 缺失、能力类型不匹配、adapter 缺失和业务自定义 `AskDataModelRegistry` 覆盖测试。

## 6. RAG、知识库服务与 AskData 向量检索

- [x] 6.1 实现 TinyEngine RAG 服务，支持检索文本、maxResults、minScore 和 collection 参数。
- [x] 6.2 实现知识库向量化服务，支持文档加载、格式过滤、元数据写入、chunk-size/chunk-overlap 切分和批量向量写入。
- [x] 6.3 通过 `AiEmbeddingStoreRegistry` 按 key 获取中立向量 store，避免将 Chroma 或 LangChain4j 原生 store 作为 TinyEngine AI starter 的唯一主路径。
- [x] 6.4 支持按集合检索和跨集合检索，单集合失败时返回该集合的受控错误状态或空结果。
- [x] 6.5 对不存在或不支持格式的文档返回包含加载、跳过和失败数量的诊断信息。
- [x] 6.6 将 AskData 默认 vector retriever 改为中立向量 store 主路径，避免默认实现直接绑定 `EmbeddingStore<TextSegment>`。
- [x] 6.7 若保留 AskData LangChain4j 原生 `ContentRetriever` 或旧 store 桥接，确保其 optional classpath/bean 条件隔离且不进入 starter 必需依赖。

## 7. 失败与降级

- [x] 7.1 实现默认 fail-fast 策略，Chat 或 RAG 启用但缺少必需模型、Provider 或向量 store 时启动失败。
- [x] 7.2 实现显式 degrade 策略，注册降级实现并暴露能力不可用状态。
- [x] 7.3 确保降级模式下 Chat、RAG 或 AskData 向量检索请求不会静默伪造成功，响应或异常包含不可用原因。

## 8. 兼容入口与样例

- [x] 8.1 提供 TinyEngine AI Chat、RAG 搜索和知识库写入服务契约，供 Web 层或 TinyEngine core 复用。
- [x] 8.2 如实现 REST 路由，添加独立 web enabled 开关和 path prefix 配置，避免与宿主 `/app-center/api/*` 路由冲突。
- [x] 8.3 添加 sample 或文档，展示默认 LangChain4j adapter Chat、Streaming Chat、RAG、中立向量 store 配置、禁用 AI 和缺失依赖策略。
- [x] 8.4 更新 AskData 文档或 starter README，说明 AskData 使用当前中立 AI 模型/向量配置和默认 LangChain4j adapter 的方式。

## 9. 验证

- [x] 9.1 添加 TinyEngine AI 自动配置条件测试，覆盖总开关关闭、仅 Chat 启用、RAG 启用缺依赖 fail-fast 和显式 degrade。
- [x] 9.2 添加 TinyEngine Provider SPI 测试，覆盖默认 LangChain4j Provider、自定义 Provider 和 unsupported capability。
- [x] 9.3 添加 TinyEngine RAG 服务测试，覆盖文档写入、格式过滤、按集合检索、跨集合检索和向量 store key 缺失错误。
- [x] 9.4 添加 AskData 中立模型/向量边界测试，覆盖 `AiModelRegistry`、`AiEmbeddingStoreRegistry`、adapter 缺失、业务 Bean 覆盖和旧原生桥接隔离。
- [x] 9.5 运行相关 Maven Wrapper 编译或测试命令，确认 TinyEngine AI starter、AskData starter 及受影响 AI Boot 模块通过 Java 8 编译。
- [x] 9.6 检查新增源码不包含 Java 9+ API，并确认未新增 Spring AI、Spring AI Alibaba、AgentScope2 Java 版硬依赖。
- [x] 9.7 检查 TinyEngine AI starter 和 AskData starter 依赖树不包含旧 LangChain4j 专用 model/store artifact、旧 starter 坐标或直接 provider SDK 主路径依赖。
