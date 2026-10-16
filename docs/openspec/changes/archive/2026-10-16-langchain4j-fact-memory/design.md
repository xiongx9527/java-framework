---
comet_change: langchain4j-fact-memory
role: technical-design
canonical_spec: openspec
---

# Design

> 本文件只记录本次改动的高层技术决策与数据流。**唯一一份正式技术设计**见
> `docs/superpowers/specs/2026-10-16-langchain4j-fact-memory-design.md`（实现方案、组件与类设计、mem0 契约、配置默认值、测试策略），
> 本文不复制其内容。动机与范围见 `proposal.md`，行为契约见 `specs/`；批次高层设计见
> `docs/superpowers/specs/2026-10-15-langchain4j-session-memory-design.md` §7/§8/§9。

## Context

- 现状：消息通道 `Mem0ChatMemoryStore` 以 `infer=false` 使用 mem0，`run_id` = 记忆键，消息体被包装为 `{"seq","msg"}` 串；这是不可更改的既有契约。
- 依赖：第 1 个拆分项 `langchain4j-session-memory` 建立的 5 段会话键与统一上下文注入抽象（`ContextProvider` / `ContextInjector` / `ContextStrategy`）。本 change 在此基础上新增一个"事实来源"。
- 实施前提：`langchain4j-session-memory` 须先合入基线；否则本分支需先 rebase 到包含该项的提交。
- 工程约束：JDK 1.8（禁 Java 9+ API）；不新增第三方依赖；Tab 缩进、UTF-8；现有方法签名一个不动。

## Goals / Non-Goals

**Goals:**

- 建立独立于消息记忆的跨会话事实通道（用户维度）。
- 事实抽取、写入时机、召回三处策略均可插拔（分别 4 / 3 / 4 种）。
- 事实作为独立上下文段，经第 1 项的统一注入点注入，顺序在会话摘要之前。

**Non-Goals（设计层面补充）:**

- 不修改消息通道 `Mem0ChatMemoryStore` 的 `infer=false` 契约、消息体格式与 append-only 语义。
- 不复用消息通道的 `{"seq","msg"}` 包装串作为抽取输入。
- 不做向量库选型扩展（复用既有 mem0 通道）。
- 不做 2 级（用户级）摘要；不引入新的第三方依赖。

## Decisions

**F1 独立事实通道**：新增 `FactStore`，使用**独立于消息通道的 mem0 实例/集合**，`infer=true`，按 `user_id`（跨会话）划分作用域，**不带会话 `run_id`**。
_备选_：复用消息通道 mem0 实例并把 `infer` 打开 —— 会污染 `unwrapMessageJson` 解析、且违反消息通道 `infer=false` 契约，弃。

**F2 抽取策略（4 种全实现）**：`mem0`（默认，服务端内置推断）/ `llm`（本项目模型抽取）/ `rule`（正则+关键词，无外部依赖）/ `hybrid`（rule 兜底 + mem0/llm）。
_理由_：`mem0` 依赖服务端 LLM，`rule` 保证无 LLM 环境下仍可用。

**F3 写入模式（3 种全实现）**：`per-turn`（默认，轮末）/ `batched`（累积到阈值或间隔）/ `session-end`（会话结束时集中）。

**F4 召回策略（4 种全实现）**：`semantic`（默认，向量检索）/ `keyword`（关键词）/ `hybrid`（向量+关键词融合）/ `rerank`（召回后重排）。

**F5 注入位置**：`FactContextProvider` 实现第 1 项的 `ContextProvider`，在注入链中排在**会话摘要之前**，最终顺序为 ① 人设 ② 事实 ③ 会话摘要 ④ 最近 N 条原文 ⑤ 本轮输入。

**F6 失败隔离**：抽取/写入/召回的异常统一按"空结果 + 日志"处理，绝不中断本轮对话。

**F7 策略注册复用既有形态**：`FactExtractors.REGISTRY` / `FactRetrievers.REGISTRY` 沿用 `AiPersistenceStrategy.Registry` 的"名字 → 内置、FQCN → 反射"约定，并与第 1 项的 `Summarizers.REGISTRY` 保持一致的写法。

### 数据流

```
读（每轮）：
  本轮输入 ──► FactRetriever.retrieve(user_id, query)
                   └─► FactContextProvider ──► ContextInjector（注入链第 ② 段）
                                        └─► 人设 → 事实 → 摘要 → 最近 N 条 → 本轮

写：
  轮末 / 批量阈值 / 会话结束
      └─► FactExtractor.extract(本轮消息) ──► FactStore.add(user_id, facts)
```

## Risks / Trade-offs

- [事实抽取依赖服务端 LLM] → 提供 `rule` 策略作为无 LLM 环境兜底，可配置切换。
- [事实注入推高 token] → 限制召回条数与单条长度，配合第 1 项的 token 预算。
- [异步写入带来时序不确定] → 默认全异步（`extract-async=true`，2026-10-16 用户确认）；时序不确定以文档声明兜底，失败仅记日志；需要即时可见可显式改同步。
- [与消息通道抢同一 mem0 实例] → 明确使用独立实例/集合，避免互相污染。

## Migration Plan

- **无数据迁移**：事实通道冷启动为空，首次抽取后逐步积累。
- **回滚**：关闭 `facts.enabled` 即完全不注入、不写入事实，回到第 1 项的行为。
- **实施顺序**：先确认 `langchain4j-session-memory` 已合入基线，再实施本 change。

## Open Questions

原三项未决项已于 2026-10-16（Comet Classic Step 1c 用户确认）决议，详见正式设计 §2 决策记录 D8–D12：

- 事实召回查询串 = **本轮用户输入**（不拼接历史）。
- `top-k` = **5**；事实段 token 预算 = **`max-facts-tokens: 500`**。
- 抽取默认**全异步**（`extract-async: true`；可显式置 `false` 改为请求线程内联）。
