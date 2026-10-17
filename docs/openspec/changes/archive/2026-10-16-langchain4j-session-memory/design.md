---
comet_change: langchain4j-session-memory
role: technical-design
canonical_spec: openspec
---

# Design

> 本文件只记录本次改动的高层技术决策与数据流。**唯一一份正式技术设计**见
> `docs/superpowers/specs/2026-10-15-langchain4j-session-memory-design.md`（含完整决策、代码骨架与生态核实结论），
> 本文不复制其内容。动机与范围见 `proposal.md`，行为契约见 `specs/`。

## Context

- 现状：助手记忆只有窗口能力（条数窗口默认 20，或配 tokenizer 后按 token 窗口），记忆键为 4 段、无会话边界，无摘要与事实能力。详见 `proposal.md` - Why。
- 生态约束：langchain4j 0.35.0 的 `dev.langchain4j.service` **不存在** `chatRequestTransformer` / `UserMessageTransformer`（已核实 jar），`langchain4j-agentic` 的注入机制不可照搬。
- 工程约束：JDK 1.8（禁 Java 9+ API）；不新增第三方依赖；Tab 缩进、UTF-8；现有方法签名一个不动。

## Goals / Non-Goals

**Goals:**

- 记忆键升级为 5 段会话级，并提供会话标识兜底，保证生效键恒为 5 段。
- 建立统一的上下文来源抽象与注入点，使来源可插拔、按策略组合。
- 落地一级滚动会话摘要，策略可插拔（5 种全实现）。

**Non-Goals（设计层面补充）:**

- 不实现跨会话事实通道（由后续 change `langchain4j-fact-memory` 承接）。
- 不做 2 级（用户级）摘要。
- 不修改 `ChatMemoryStore` 一族的消息存储语义。
- 不引入 `mem0` 的 `infer=true`（保持消息通道 `infer=false` 契约）。

## Decisions

**D1 键与会话维度分离**：记忆键升为 5 段 `{tenant}:{app}:{agent}:{user}:{session}`；但 `SimpleAssistant.memoryId()` **继续返回 4 段**，会话维度只经由新增的 `session(...)` 与内部 `effectiveMemoryId()` 生效。
_备选_：直接把 `memoryId()` 改成 5 段 —— 破坏既有公开契约与测试断言，弃。
_理由_：`ChatMemoryIds.parseSegment` 只取前 4 段且要求段数 ≥ 4，5 段串对既有解析天然正确，可零改动兼容。

**D2 会话兜底**：新增 `SessionIdResolver`（默认 `daily`，形如 `dYYYYMMDD`），缺失会话时补全，避免键退化为用户级而丢失隔离。
_备选_：无会话时回退 4 段 —— 隔离静默失效，弃。

**D3 注入点落在 ChatMemory 装饰器**：新增 `ContextAwareChatMemory`，在 `add` 捕获本轮用户输入、在 `messages` 前置系统消息。
_备选_：照 `langchain4j-agentic` 用 `chatRequestTransformer` —— 0.35.0 无此 API，弃。

**D4 抽象形态对齐 agentic、实现策略不同**：`ContextProvider`（有序来源）+ `ContextInjector`（统一注入）+ `ContextStrategy`（`MEMORY` / `ENRICHED` / `MEMORY_AND_ENRICHED`）。
_与 agentic 的关键差异_：agentic 的 `Summarizer` 每轮对整段 scope 对话**全量重摘**；本设计摘要在**写时滚动维护**、读时**直读**，不做每轮重摘。

**D5 摘要形态**：一级、锚定会话、写时为主 + 读时兜底、双阈值（`trigger-tokens` 触发 / `keep-tokens` 压缩）；默认 `refine` 增量精炼，并每 `rebuild-every` 次插入一次 `rebuild` 防漂移。

**D6 摘要存储独立**：新建 `ai_chat_summary` 表，`session_key` 唯一。摘要只压缩进上下文，**不删除原文**（原文仍留在消息存储，append-only）。

**D7 三通道物理隔离**：消息（mem0-A，`infer=false`）/ 摘要（`ai_chat_summary`）/ 事实（下一 change，独立 mem0 实例）。隔离是硬要求：事实若混入消息通道会污染 `unwrapMessageJson` 解析。

**D8 复用既有能力**：token 计数复用 `MagicDynamicTokenizer`（无需模型）；策略注册复用 `AiPersistenceStrategy.Registry` 的“名字 → 内置 / FQCN → 反射”形态。

### 数据流

```
用户输入
  └─ Prompt 组装
       ① 人设（System）
       ② 会话摘要（SummaryStore.get(sessionKey)）      ← 本 change
       ③ 最近 N 条原文（MessageWindowChatMemory）
       ④ 本轮输入
  └─ 模型回复
       └─ 写路径：消息落 mem0-A（run_id=5 段）→ 越阈值则更新摘要
```

## Risks / Trade-offs

- [摘要来不及在窗口驱逐前生成] → 写时触发为主 + 读时兜底补齐，双保险。
- [5 段键影响既有调用方] → 当前无存量记忆数据、无需迁移；`memoryId()` 契约不变，`parseXxx` 零改动。
- [摘要引入额外 LLM 成本/延迟] → 独立便宜小模型 + 阈值触发 + Refine 增量；失败即回退最近 N 条。
- [注入多来源推高 token] → 按 `ContextStrategy` 组合，摘要按 `keep-tokens` 压缩。

## Migration Plan

- **无数据迁移**：当前无存量记忆数据。
- `ai_chat_summary` 由既有存储初始化机制在首次使用时创建（无则建）。
- **回滚**：将 `ContextStrategy` 置为 `memory`（不注入附加来源）即回到纯窗口行为。

## Open Questions

以下为可安全后置的未知项，不改变本次规格、方案与任务拆解：

- 会话标识生成规则的取舍（按天 / 显式传入 / 业务会话主键）—— 默认按天，其余作为可配置项。
- 摘要与各上下文来源之间的 token 预算分配比例。
- 摘要与事实（后续 change）之间的 token 预算如何共同上限。
