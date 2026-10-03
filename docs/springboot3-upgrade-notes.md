# Spring Boot 3 分支升级调整清单

> 合并到 `springboot3` 分支（langchain4j 0.35.0 → 1.20.2）后需要调整的事项汇总。
> **状态（2026-10-02）**：四项均已完成。项 1、2 随本次调整落地；项 3、4 在 1.20.2 迁移中已接通。
> **状态（2026-10-03）**：合并 `origin/dev`（ad66c5c20，知识库元数据字段管理/ES 全文检索等）带回的旧 API 已适配（见第 5 节）；
> 元数据过滤改为 Assistant 接口显式参数方案（见第 6 节）。fw-plus-ai-boot-autoconfigure 474 个测试全部通过。
> **待办**：fw-plus-boot-autoconfigure 仍有 Boot2→3 遗留编译错误（见第 7 节）。

## 1. knowledge 元数据 Filter：contains 下推原生支持 ✅

- **现状（0.35.0）**：`MetadataFilterBuilder` 无 `containsString`；`KbSearchService` 的 `contains`/`not contains`
  操作符使用自定义 `ContainsStringFilter`（knowledge.service 包内），各向量库的 filter 翻译器不识别该类型，
  下推会失败并自动回落"多取 3 倍 + 内存过滤"（语义正确，性能次优）。
- **调整（1.x）**：1.x 原生提供 `ContainsString`（`MetadataFilterBuilder.containsString(...)`），
  将 `contains` / `not contains`（`Filter.not(containsString)`）切换为原生实现，contains 即可随
  `EmbeddingSearchRequest.filter` 真正下推到向量库；删除自定义 Filter。
- **涉及位置**：`KbSearchService.buildMetadataCondition` 与自定义 Filter 类。
- **is empty / is not empty**：两版均无原生 `IsEmpty`，继续以空串（""）近似表达，行为不变。

## 2. KbTokenCounter：字符近似 → TokenCountEstimator ✅

- **现状（0.35.0）**：`KbTokenCounter.estimate` 用 CJK/拉丁混合字符近似估 token，不引入 Tokenizer 词表依赖。
- **调整（1.x）**：`KbTokenCounter` 实现 `TokenCountEstimator` 接口（core 无默认实现，各提供商实现
  绑定自家 API 不通用），估算体保留字符近似算法；`estimate(String)` 入口不变，调用方零改动；
  后续如需精确计数只改本类（如换用 `OpenAiTokenCountEstimator`）。

## 3. 流式工具调用缺口 ✅（1.20.2 迁移中完成）

- **现状（0.35.0）**：Ollama 流式无 tools 重载；OpenAI 兼容 `/v1` 同帧 `tool_calls` 被丢弃，
  流式场景下工具调用能力受限（`/stream` 报 response cannot be null 的已知问题）。
- **调整（1.x）**：1.x 流式 API 重构后按新版 `ChatModel`/`StreamingChatModel` 的 tools 支持重新接通。
- **落地情况**：工具循环由 1.20.2 AiServices 驱动，`SimpleAssistant.doStreamToSse` 经 TokenStream 的
  `beforeToolExecution` / `onToolExecuted` 转发 `tool-call` / `tool-result` SSE 事件；
  OpenAI 兼容帧的 `tool_calls` 拼接由 1.x SDK 内部处理，框架侧无残留代码。

## 4. Flow.Publisher 流式取消 ✅（1.20.2 迁移中完成）

- **现状（0.35.0）**：无流式取消/底层请求 SPI，订阅取消无法透传到模型请求。
- **调整（1.x）**：langchain4j 1.20.2+ 提供流式取消 SPI 后，在流式链路实现真取消。
- **落地情况**：`SimpleAssistant.doStreamToSse` 捕获 WithContext 回调中的 `StreamingHandle`
  （`captureHandle`），SSE `onCompletion/onError/onTimeout` 时 `handle.cancel()` 真取消模型流式调用。
  文本补全路径（`doGenerateTextToSse`）沿用旧 `StreamingResponseHandler`，无取消句柄，仅停止消费。

## 5. dev 合并（ad66c5c20）旧 API 适配 ✅（2026-10-03）

dev 分支知识库新代码基于 0.35.0 基线开发，合并后适配如下：

- `KnowledgeAssistantFactory`（后被删除，见第 6 节）/ `KnowledgeContentRetriever`：
  `ChatLanguageModel` → `ChatModel`，`AiServices.chatLanguageModel()` → `chatModel()`，`generate(String)` → `chat(String)`。
- `KbMetadataFieldService` / `KbDocumentService`：MP 3.5.16 `selectCount` 返回 `Long`，`Integer` 接收处同步改 `Long`。
- `KnowledgeIndexingServiceTest`：`KbIndexingService` 构造补 `EsSegmentSearchClient` 参数（主类判空，测试传 null）。
- `SimpleAssistantTest` fake 适配 1.x：
  - `RecordingChatModel` 覆写 `doChat(ChatRequest)`（1.x `chat(List)` 是 default 且内部转 ChatRequest
    走 `doChat`，覆写 `chat(List)` 不在调用链上）；
  - `fakeStream` 补覆写 `onPartialResponseWithContext` / `onPartialThinkingWithContext` /
    `beforeToolExecution`（WithContext 版本是 default 方法，直接抛 not implemented）。
- `SpringBoot3DependencyBaselineTest` 基线值更新：langchain4j 1.20.2、community 1.20.0-beta30。
- `FilteringContentRetriever.scoreOf` / `WeightedScoringContentAggregator.weighted`：
  1.x `EmbeddingStoreContentRetriever` 写入 metadata 的 `score` 是 `Double`，`getString` 读 Double 会抛异常，
  改用 `getValue` + Number/String 双兼容。

## 6. 元数据过滤：Assistant 接口显式参数方案 ✅（2026-10-03）

- 演进：RetrievalFilterHolder（ThreadLocal，7a318009b）→ KnowledgeAssistantFactory（构建期固定条件，
  d06a3829a）→ **Assistant 显式参数方案**（当前）。
- 方案：`SimpleAssistant` 内嵌 `FilterableRetriever` 接口（`retrieve(message, metadataCondition)`），
  `KnowledgeContentRetriever` 实现之（新条件与构建期固定条件 AND 叠加）；
  `AssistantBuilder.build()` 自动识别 `contentRetriever instanceof FilterableRetriever` 并接线。
- 接口（`metadataCondition` 为 Dify metadata_condition 格式 JSON 字符串，仅本次调用生效）：
  - `retrieve(memoryId, metadataCondition, message)` —— 带过滤只检索；
  - `chatWithFilter(memoryId, metadataCondition, message)` —— 带过滤检索后拼上下文直连生成
    （不经 AiServices 检索增强链，避免重复召回）。
- 不引入 ThreadLocal/Holder：流式等跨线程场景不适用隐式传参，显式参数语义清晰。

## 7. 待办：fw-plus-boot-autoconfigure 编译错误（Boot2→3 遗留）

fw-plus-boot 层 `fw-plus-boot-autoconfigure` 仍有约 90 处编译错误，非 dev 合并引入：

- **ws 插件 + web/ws**（占绝大多数）：`WsDataSourceInfo` / `WsSubscribeInfo` / `WsSubscribeScript` /
  `AbstractWebSocketHandler` / `MagicWsController` / 两个 MagicResourceStorage —— 大量"找不到符号"，
  疑似 dev/springboot2 侧新增代码未随 Boot3 基线适配（javax→jakarta 或 Lombok/继承签名变化）。
- **swagger 插件** `MagicSwaggerConfiguration`：springdoc 1.x→2.x 包名冲突
  （`org.springdoc.core.GroupedOpenApi` 等 5 个类 2.x 已移动包，与 `org.springdoc.core.properties` 双 import 冲突）。
- **config** `ConfigRefresher`：6 处"找不到符号"。

## 8. 待办：ai-boot-autoconfigure 12 个测试适配（1.x / spring-ai 1.1.8 升级连带）

主代码已全部编译通过、474 个测试中 462 个通过；剩余 12 个失败均为测试未跟随新版本结构（2026-10-03）：

- `Langchain4jOpenAiModelFactoryTest`（3）：反射断言 `OpenAiChatModel` 的 `temperature`/`topP` 等字段，
  1.20.2 已把参数移入 `OpenAiChatRequestParameters`，需改断言目标。
- `Langchain4jModelFactoryRegistryTest`（2）：BOM 加入 `langchain4j-github-models` 后 provider 注册清单变化。
- `MagicDynamicLangchain4jDataSourcesTest`（1）：数据源清单断言（同 github-models 新增所致）。
- `Langchain4jDatasourceControllerTest`（2）、`MagicAPIWebFluxPluginAutoConfigurationTest`（3）、
  `MagicApiMcpPluginAutoConfigurationTest`（1）：spring-ai 1.1.8 / BOM 管理方式变更后的装配断言。
- 已随本轮修复：`SpringBoot3DependencyBaselineTest`（版本与 BOM 管理断言对齐 1.20.2/1.20.0-beta30/
  spring-ai 1.1.8/community-bom 集中管理）、`MagicApiToolProviderTest`（1.x 校验 userMessage 非空）、
  `ContentRetrieverProviderRegistryTest` / `FilteringContentRetriever`（score 读取 Double/String 双兼容）。
