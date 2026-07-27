## 1. 前置确认

- [ ] 1.1 确认 `integrate-tiny-engine-core` 已完成并提供 TinyEngine core 基础领域能力、通用契约和可被 AI 层引用的服务边界。
- [ ] 1.2 盘点 `fw-plus-ai` 现有 LangChain4j 模型 registry、AI Boot starter 聚合、AI Boot BOM 和多向量 store registry/factory 的可复用边界。
- [ ] 1.3 确认目标模块仍使用 Java 8 与 Spring Boot 2.7.18，不引入与既有 LangChain4j 0.35.0、Spring AI 1.0.1 体系冲突的主线版本。

## 2. 模块与依赖

- [ ] 2.1 在 `fw-plus-ai` 中新增 TinyEngine AI starter 及必要的自动配置模块结构。
- [ ] 2.2 将 TinyEngine AI starter 注册到 AI Boot starter 聚合 POM。
- [ ] 2.3 在 AI Boot BOM 中管理 TinyEngine AI starter artifact，版本风格与当前层级保持一致。
- [ ] 2.4 为 starter 添加 TinyEngine core 和既有 LangChain4j 模型/向量能力依赖，不把 Spring AI、Spring AI Alibaba、AgentScope2 Java 版作为必需依赖。

## 3. 配置与自动配置

- [ ] 3.1 定义 TinyEngine AI 配置属性，覆盖总开关、Provider 选择、Chat/RAG/Vector 能力级开关、模型 key、向量 store key、RAG 参数和缺失依赖策略。
- [ ] 3.2 实现自动配置条件，确保总开关关闭时不创建 TinyEngine AI Chat、RAG、知识库向量化和 Provider 运行时 Bean。
- [ ] 3.3 实现能力级条件，支持只启用 Chat 而不要求 EmbeddingModel 或向量存储。
- [ ] 3.4 实现 RAG 启用时的模型、Embedding 能力和向量 store 配置校验。

## 4. Provider SPI

- [ ] 4.1 定义 Java 8 兼容的 TinyEngine AI Provider SPI、能力枚举、Provider registry 和 unsupported capability 错误模型。
- [ ] 4.2 定义 TinyEngine Chat、Streaming Chat、RAG 检索、知识库写入、Embedding 和向量访问所需的请求/响应业务对象。
- [ ] 4.3 在 SPI 中预留 Spring AI、Spring AI Alibaba、AgentScope2 Java 版和业务自定义 Provider 的运行时标识与扩展点。
- [ ] 4.4 覆盖 Provider 选择逻辑：默认使用 LangChain4j，显式自定义 Provider 可替换默认运行时。

## 5. LangChain4j 默认 Provider

- [ ] 5.1 实现 LangChain4j Chat Provider，复用现有 LangChain4j 模型 registry 获取 Chat 模型。
- [ ] 5.2 实现 LangChain4j Streaming Chat Provider，返回可由 Web 层转换为 SSE 的流式响应。
- [ ] 5.3 映射 TinyEngine Chat 参数，包括 model、messages、tools、stream、max_tokens、stream_options、presence_penalty、response_format、enable_thinking、tool_choice、parallel_tool_calls、enable_search、search_options 和 frequency_penalty。
- [ ] 5.4 对 Provider 不支持的参数实现忽略、转换或明确 unsupported 参数错误策略。
- [ ] 5.5 对不存在的模型 key 返回包含 key、Provider 和能力类型的明确错误。

## 6. RAG 与知识库服务

- [ ] 6.1 实现 TinyEngine RAG 服务，支持检索文本、maxResults、minScore 和 collection 参数。
- [ ] 6.2 实现知识库向量化服务，支持文档加载、格式过滤、元数据写入、chunk-size/chunk-overlap 切分和批量向量写入。
- [ ] 6.3 通过既有向量 store registry 按 key 获取向量 store，避免将 Chroma 作为 TinyEngine AI starter 的唯一主路径。
- [ ] 6.4 支持按集合检索和跨集合检索，单集合失败时返回该集合的受控错误状态或空结果。
- [ ] 6.5 对不存在或不支持格式的文档返回包含加载、跳过和失败数量的诊断信息。

## 7. 失败与降级

- [ ] 7.1 实现默认 fail-fast 策略，Chat 或 RAG 启用但缺少必需模型、Provider 或向量 store 时启动失败。
- [ ] 7.2 实现显式 degrade 策略，注册降级实现并暴露能力不可用状态。
- [ ] 7.3 确保降级模式下 Chat 或 RAG 请求不会静默伪造成功，响应或异常包含不可用原因。

## 8. 兼容入口与样例

- [ ] 8.1 提供 TinyEngine AI Chat、RAG 搜索和知识库写入服务契约，供 Web 层或 TinyEngine core 复用。
- [ ] 8.2 如实现 REST 路由，添加独立 web enabled 开关和 path prefix 配置，避免与宿主 `/app-center/api/*` 路由冲突。
- [ ] 8.3 添加 sample 或文档，展示默认 LangChain4j Chat、Streaming Chat、RAG、向量 store 配置、禁用 AI 和缺失依赖策略。

## 9. 验证

- [ ] 9.1 添加自动配置条件测试，覆盖总开关关闭、仅 Chat 启用、RAG 启用缺依赖 fail-fast 和显式 degrade。
- [ ] 9.2 添加 Provider SPI 测试，覆盖默认 LangChain4j Provider、自定义 Provider 和 unsupported capability。
- [ ] 9.3 添加 RAG 服务测试，覆盖文档写入、格式过滤、按集合检索、跨集合检索和向量 store key 缺失错误。
- [ ] 9.4 运行相关 Maven Wrapper 编译或测试命令，确认 TinyEngine AI starter 及受影响 AI Boot 模块通过 Java 8 编译。
- [ ] 9.5 检查新增源码不包含 Java 9+ API，并确认未新增 Spring AI、Spring AI Alibaba、AgentScope2 Java 版硬依赖。
