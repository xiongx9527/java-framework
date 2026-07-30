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
- **THEN** TinyEngine AI Chat 服务 SHALL 返回可被 Web 层转换为 SSE 的流式响应
- **AND** 流式响应 SHALL 不要求业务方直接使用 OpenAI 兼容 HTTP Client

#### Scenario: LangChain4j Provider 复用中立模型 registry
- **WHEN** TinyEngine AI 配置引用 `AiModelRegistry` 中的模型 key
- **THEN** Provider SHALL 从中立 registry 获取对应 Chat、Streaming Chat 或 Embedding 模型门面
- **AND** 配置引用不存在的 key 时 SHALL 在启动期或首次使用时返回包含 key 和能力类型的明确错误

### Requirement: Provider SPI 可表达多运行时边界
系统 SHALL 定义 Java 8 兼容的 TinyEngine AI Provider SPI，使不同 AI 运行时可以声明自身能力并接入 TinyEngine AI 服务契约。

#### Scenario: SPI 声明能力集合
- **WHEN** Provider 实现注册到 TinyEngine AI Provider registry
- **THEN** Provider SHALL 声明支持的能力集合
- **AND** 能力集合 SHALL 至少覆盖 Chat、Streaming Chat、RAG Retrieval、Knowledge Indexing、Embedding、Vector Store 和 Agent Runtime 边界

#### Scenario: 首版未引入后续 Provider 硬依赖
- **WHEN** 检查 TinyEngine AI starter 的必需依赖
- **THEN** starter SHALL 不强制依赖 Spring AI、Spring AI Alibaba 或 AgentScope2 Java 版 artifact
- **AND** 这些运行时 SHALL 只能通过后续独立 Provider starter 或业务自定义 Provider 接入

#### Scenario: 自定义 Provider 可替换默认运行时
- **WHEN** 应用注册业务自定义 Provider 且配置选择该 Provider
- **THEN** TinyEngine AI 服务 SHALL 使用该 Provider 处理其声明支持的能力
- **AND** 该 Provider 不支持请求能力时 SHALL 返回明确的 unsupported capability 错误

### Requirement: RAG 和知识库能力
系统 SHALL 提供 TinyEngine RAG 服务和知识库向量化服务，用于文档加载、切分、Embedding、向量写入、按集合检索和跨集合检索。

#### Scenario: 文档写入知识库
- **WHEN** 应用提交支持格式的文档路径、文档集 ID 和目标集合
- **THEN** 知识库服务 SHALL 加载文档、写入 source、format、timestamp、collection 等元数据，并按配置 chunk-size 与 chunk-overlap 切分
- **AND** 服务 SHALL 调用 Provider 的 Embedding 能力和中立向量存储能力写入向量数据

#### Scenario: 不支持或不存在的文档
- **WHEN** 文档路径不存在或格式不在配置支持列表内
- **THEN** 知识库服务 SHALL 跳过该文档或返回受控错误
- **AND** 响应或异常 SHALL 包含已加载、已跳过和失败数量中的关键诊断信息

#### Scenario: 按集合检索
- **WHEN** 应用提交检索文本、maxResults、minScore 和 collection
- **THEN** RAG 服务 SHALL 返回匹配的文本片段、分数和元数据
- **AND** 指定 collection 时结果 SHALL 只包含该集合的数据

#### Scenario: 跨集合检索
- **WHEN** 应用请求跨集合检索
- **THEN** RAG 服务 SHALL 按已配置集合分别执行检索
- **AND** 某个集合检索失败时 SHALL 在该集合返回空结果或受控错误状态，不影响其他集合结果

### Requirement: 缺失依赖失败和降级语义
系统 SHALL 为模型、EmbeddingModel、向量存储和 Provider 缺失提供明确失败或受控降级行为。

#### Scenario: 默认 fail-fast
- **WHEN** Chat 或 RAG 能力启用但缺少必需模型、Provider 或向量 store 配置
- **THEN** 系统 SHALL 默认在启动期失败
- **AND** 错误信息 SHALL 包含缺失的配置 key、Provider 名称和能力类型

#### Scenario: 显式降级
- **WHEN** 应用显式配置缺失依赖策略为 degrade
- **THEN** 系统 SHALL 注册降级实现并暴露能力不可用状态
- **AND** Chat 或 RAG 请求命中不可用能力时 SHALL 返回明确错误或空结果加不可用原因，而不是静默伪造成功

### Requirement: TinyEngine AI API 兼容入口
系统 SHALL 为 TinyEngine AI Chat 和搜索场景提供可被 Web 层复用的服务契约，并允许后续暴露与 TinyEngine 参考实现兼容的 REST 路由。

#### Scenario: 服务契约支持参考 Chat 参数
- **WHEN** Web 层把 TinyEngine Chat 请求交给 Chat 服务
- **THEN** 服务 SHALL 能表达参考实现中的 model、messages、tools、stream、max_tokens、stream_options、presence_penalty、response_format、enable_thinking、tool_choice、parallel_tool_calls、enable_search、search_options 和 frequency_penalty 等参数
- **AND** Provider 不支持的参数 SHALL 被忽略、转换或返回明确 unsupported 参数错误

#### Scenario: REST 路由受开关控制
- **WHEN** 实现选择自动暴露 TinyEngine AI REST 路由
- **THEN** 路由 SHALL 受独立 web enabled 开关和 path prefix 配置控制
- **AND** 默认行为 SHALL 避免与宿主应用已有 `/app-center/api/ai/chat`、`/app-center/api/chat/completions` 或 `/app-center/api/ai/search` 路由冲突
