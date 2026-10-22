# Proposal

## Why

第 1 个拆分项 `langchain4j-session-memory` 解决了"单个会话内记得住"，但用户的**跨会话**信息仍然丢失：`Mem0ChatMemoryStore` 以 `infer=false` 使用 mem0，只做逐条消息存储（`run_id` = 记忆键），且消息体被包装成 `{"seq","msg"}` 串。它既不能作为事实抽取的有效输入，也**必须**保持 `infer=false` 契约。结果是"换个会话就失忆"，助手无法记住用户稳定的偏好、画像与业务事实。

因此需要一条**独立于消息记忆**的事实通道：按用户维度沉淀事实，在任意后续会话中召回并注入上下文。

## What Changes

- **新增独立事实通道 `FactStore`**：独立 mem0 实例/集合，`infer=true`，按 `user_id`（跨会话）划分作用域，**不带会话 `run_id`**。
- **新增事实抽取 SPI** `FactExtractor` + `FactExtractors.REGISTRY`，**4 种策略全实现**：`mem0`（默认，服务端推断）/ `llm` / `rule` / `hybrid`。
- **新增事实写入模式** `FactWriteMode`，**3 种全实现**：`per-turn`（默认）/ `batched` / `session-end`。
- **新增事实召回 SPI** `FactRetriever` + `FactRetrievers.REGISTRY`，**4 种策略全实现**：`semantic`（默认）/ `keyword` / `hybrid` / `rerank`。
- **新增 `FactContextProvider`**：实现第 1 项建立的 `ContextProvider`，把事实作为独立上下文段注入，顺序为 ① 人设 ② 事实 ③ 会话摘要 ④ 最近 N 条原文 ⑤ 本轮输入。
- **新增配置** `langchain4j.assistant.session.facts.*`。
- **依赖关系**：本 change 依赖 `langchain4j-session-memory` 建立的统一上下文注入抽象与 5 段会话键，必须在其之后实施。

## Capabilities

### New Capabilities

- `langchain4j-fact-memory`: 跨会话事实记忆——独立事实通道、事实抽取/写入/召回的可插拔策略，以及经统一注入点进入上下文。

### Modified Capabilities

（无。第 1 项 `langchain4j-context-injection` 的"上下文来源可插拔"需求已覆盖新增事实来源，该 spec 的行为约定不发生变化；消息通道 `Mem0ChatMemoryStore` 的契约同样不变。）

## Impact

- **代码模块**：`fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure`（事实存储、抽取/召回策略、上下文提供者、属性与装配）。
- **外部依赖**：复用既有 mem0 通道，**不新增第三方依赖**；`mem0` 抽取策略需要一个带 LLM 的 mem0 服务端，缺失时应可退化为 `rule` 策略。
- **配置**：`Langchain4jAssistantProperties` 新增 `session.facts.*` 节点。
- **兼容性**：不修改消息通道 `Mem0ChatMemoryStore` 的 `infer=false` 契约与消息体格式；事实召回失败或未命中时静默跳过，不影响主对话。
- **实施顺序**：必须在 `langchain4j-session-memory` 合入基线后实施。
