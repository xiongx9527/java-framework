# Spring Boot 3 分支升级调整清单

> 合并到 `springboot3` 分支（langchain4j 0.35.0 → 1.x）后需要调整的事项汇总。
> fw-plus-ai 层当前锁定 langchain4j 0.35.0（0.x → 1.x 为破坏性升级，统一在 Spring Boot 3 分支处理）。

## 1. knowledge 元数据 Filter：contains 下推原生支持

- **现状（0.35.0）**：`MetadataFilterBuilder` 无 `containsString`；`KbSearchService` 的 `contains`/`not contains`
  操作符使用自定义 `ContainsStringFilter`（knowledge.service 包内），各向量库的 filter 翻译器不识别该类型，
  下推会失败并自动回落"多取 3 倍 + 内存过滤"（语义正确，性能次优）。
- **调整（1.x）**：1.x 原生提供 `ContainsString`（`MetadataFilterBuilder.containsString(...)`），
  将 `contains` / `not contains`（`Filter.not(containsString)`）切换为原生实现，contains 即可随
  `EmbeddingSearchRequest.filter` 真正下推到向量库；删除自定义 Filter。
- **涉及位置**：`KbSearchService.buildMetadataCondition` 与自定义 Filter 类。
- **is empty / is not empty**：两版均无原生 `IsEmpty`，继续以空串（""）近似表达，行为不变。

## 2. KbTokenCounter：字符近似 → TokenCountEstimator

- **现状（0.35.0）**：`KbTokenCounter.estimate` 用 CJK/拉丁混合字符近似估 token，不引入 Tokenizer 词表依赖。
- **调整（1.x）**：替换为 `TokenCountEstimator`，只改 `KbTokenCounter` 单一封装点（设计 D9 升级兼容约束）。

## 3. 流式工具调用缺口

- **现状（0.35.0）**：Ollama 流式无 tools 重载；OpenAI 兼容 `/v1` 同帧 `tool_calls` 被丢弃，
  流式场景下工具调用能力受限（`/stream` 报 response cannot be null 的已知问题）。
- **调整（1.x）**：1.x 流式 API 重构后按新版 `ChatModel`/`StreamingChatModel` 的 tools 支持重新接通。

## 4. Flow.Publisher 流式取消

- **现状（0.35.0）**：无流式取消/底层请求 SPI，订阅取消无法透传到模型请求。
- **调整（1.x）**：langchain4j 1.20.2+ 提供流式取消 SPI 后，在流式链路实现真取消。

## 5. 检索元数据条件按请求动态传入

- **现状（0.35.0）**：AiServices 到 ContentRetriever 的运行期通道只有 Query（text + chatMemoryId），
  没有"每次请求向检索器传参"的内置 API，RAG 助手的 metadataCondition 只能构建期固定——每个条件
  组合（如每个租户）需单独构建一个 assistant 实例缓存复用，同一实例不支持每次对话变更条件
  （ThreadLocal / memoryId 编码租户 / `@V` 均已被否，见会话记录）。
- **调整（1.x）**：确认 1.x 是否提供按请求透传检索参数的 SPI（如 `ChatRequestParameters` 或
  AiServices 的 per-call 扩展）；若有，`SimpleAssistant.chat` 增加 per-call metadataCondition
  通道，工厂改为单实例 + 每请求条件，去掉按条件组合多实例缓存。
- **过渡备选（0.35.0 可用，暂未采用）**：`Query.metadata().chatMemoryId()` 已暴露当前会话
  memoryId，检索器可按 memoryId 查"会话 → 条件"映射动态解析（与 ChatMemoryStore 同构，
  非编码进 memoryId、非 ThreadLocal）；业务侧在会话建立时登记租户条件即可单实例服务全部会话。
