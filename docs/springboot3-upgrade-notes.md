# Spring Boot 3 分支升级调整清单

> 合并到 `springboot3` 分支（langchain4j 0.35.0 → 1.20.2）后需要调整的事项汇总。
> **状态（2026-10-02）**：四项均已完成。项 1、2 随本次调整落地；项 3、4 在 1.20.2 迁移中已接通。

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
