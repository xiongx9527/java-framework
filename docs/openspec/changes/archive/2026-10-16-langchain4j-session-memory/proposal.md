# Proposal

## Why

当前 fw-plus-ai 的 LangChain4j 助手记忆只有**窗口**能力（按条数窗口，默认 20 条；或配置 tokenizer 后按 token 窗口），且记忆键为 **4 段** `{tenant}:{app}:{agent}:{user}`。这带来三个问题：

1. **没有会话边界**：同一用户的所有对话共用一份历史，新对话不是新对话。
2. **上下文超限无任何压缩手段**：长会话只能靠窗口丢弃旧消息，主线信息丢失。
3. **没有跨会话记忆**：`Mem0ChatMemoryStore` 以 `infer=false` 使用 mem0，仅当逐条消息存储，换会话即失忆。

生态核实结论：langchain4j 主包（0.35.0 → 1.22.0）**只有窗口记忆，无摘要记忆实现**；仅 `langchain4j-agentic`（beta）有 `SupervisorContextStrategy.SUMMARIZATION`，但绑死 `AgenticScope`、每轮全量重摘且非阈值触发；且 0.35.0 不存在 `chatRequestTransformer` / `UserMessageTransformer`，其注入机制不可照搬。

本 change 是设计文档《LangChain4j 会话记忆增强设计》的**第一个拆分项**：建立会话级记忆与统一上下文抽象，并落地一级滚动摘要。跨会话事实通道由后续 change `langchain4j-fact-memory` 承接。

## What Changes

- **新增 5 段会话键**：`{tenant}:{app}:{agent}:{user}:{session}`，`run_id` = 会话；新增 `ChatMemoryIds.build(...)` 5 参重载（4 参版本与 `parseXxx` 保持不动）。
- **新增 `SessionIdResolver`**：会话缺失时兜底（默认按天 `d20261015` 形态），保证 `effectiveMemoryId` 恒为 5 段。
- **`SimpleAssistant` 扩展**：新增 `session(String)` 与内部 `effectiveMemoryId()`；`memoryId()` **仍返回 4 段**，既有断言不破。
- **新增统一上下文抽象**：`ContextProvider`（可插拔来源）、`ContextInjector`（统一注入点，落在 ChatMemory 装饰器上）、`ContextStrategy`（组合策略枚举：`MEMORY` / `ENRICHED` / `MEMORY_AND_ENRICHED`）。
- **新增一级会话摘要**：`SummaryStore` + `ai_chat_summary` 表；`Summarizer` SPI + `Summarizers.REGISTRY`，**5 种策略全实现**（`refine` 默认 / `rebuild` / `map-reduce` / `segmented` / `structured`）；双阈值触发（`trigger-tokens` / `keep-tokens`），写时为主、读时兜底。
- **新增配置**：`langchain4j.assistant.session.*`（会话解析、上下文策略、摘要阈值与模型等）。
- **非破坏**：现有方法签名一个不动；`ChatMemoryStore` / `AbstractJsonChatMemoryStore` / `Mem0ChatMemoryStore` 的消息存储语义不变。

## Capabilities

### New Capabilities

- `langchain4j-session-memory`: 会话级记忆作用域——5 段记忆键、会话兜底解析，以及一级滚动会话摘要（含可插拔摘要策略与阈值触发）。
- `langchain4j-context-injection`: 助手上下文注入的统一抽象——可插拔上下文来源、统一注入点与来源组合策略。

### Modified Capabilities

（无。既有 spec 的行为约定不发生变化：`memoryId()` 仍 4 段，`langchain4j-chat-envelope` 等能力的需求描述不受影响。）

## Impact

- **代码模块**：`fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure`（记忆装配、助手、属性、自动配置）。
- **数据存储**：新增表 `ai_chat_summary`（会话摘要，`session_key` 唯一）。
- **配置**：`Langchain4jAssistantProperties` 新增 `session.*` 节点；默认关闭或保持现有行为，避免影响存量用法。
- **依赖**：不新增第三方依赖；token 计数复用既有 `MagicDynamicTokenizer`。
- **兼容性**：`ChatMemoryIds.parseXxx` 对 5 段串天然正确；4 段记忆键退化为用户级的风险由 `SessionIdResolver` 兜底。
