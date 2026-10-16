# Comet Design Handoff

- Change: langchain4j-session-memory
- Phase: design
- Mode: compact
- Context hash: 957a89dfc7486ee97cf0c29e6cb355c831def955936571a24de9541429324754

Generated-by: comet-handoff.sh
Task hash policy: task-content-v1. Read tasks.md for live completion; excerpts are design-time context.

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## docs/openspec/changes/langchain4j-session-memory/proposal.md

- Source: docs/openspec/changes/langchain4j-session-memory/proposal.md
- Lines: 1-42
- SHA256: 1e45e780cf932eadf7c67c254b9d74ea0ba7ea2b91d25b8579ae8447f353cd57

```md
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

```

## docs/openspec/changes/langchain4j-session-memory/design.md

- Source: docs/openspec/changes/langchain4j-session-memory/design.md
- Lines: 1-89
- SHA256: b4948677c25d4a8da0f13e2d1aa539e6516d7cb9d12271214311f42db0701c0f

[TRUNCATED]

```md
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

```

Full source: docs/openspec/changes/langchain4j-session-memory/design.md

## docs/openspec/changes/langchain4j-session-memory/tasks.md

- Source: docs/openspec/changes/langchain4j-session-memory/tasks.md
- Lines: 1-43
- SHA256: 70514cc07c5fd1a8545b6950bf286c8eef6294f198fe8198aab24f152b8bab1f

```md
# Tasks

> 详细设计与代码骨架见 `docs/superpowers/specs/2026-10-15-langchain4j-session-memory-design.md`。
> 单测命令：`cd fw-plus-ai && ./mvnw -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dmaven.test.skip=false`

## 1. 会话键与会话兜底

- [ ] 1.1 `ChatMemoryIds` 新增 5 参 `build(...)` 重载（4 参版本与 `parseXxx` 一字不动）；验证：单测同时覆盖 5 段/4 段构造，并断言 `parseSegment` 对 5 段串解析出正确的前 4 段
- [ ] 1.2 新增 `SessionIdResolver` 接口与 `daily` 默认实现（`ZoneId`/`DateTimeFormatter` 静态化，仅用 Java 8 API）；验证：单测覆盖按天格式（`dYYYYMMDD`）与“显式会话优先于兜底”
- [ ] 1.3 `SimpleAssistant` 新增 `session(String)` 与内部 `effectiveMemoryId()`，`memoryId()` 保持不变；验证：单测断言 `memoryId()` 仍为 `t1:crm:bot:u1`，而 `effectiveMemoryId()` 为 5 段
- [ ] 1.4 `Langchain4jAssistantProperties` 新增 `session.enabled` 与 `session.resolver` 配置项（默认值符合设计 D1/D2）；验证：属性绑定单测断言默认值与显式覆盖

## 2. 统一上下文抽象

- [ ] 2.1 新增 `ContextProvider` 接口与 `ContextRequest`；验证：单测断言返回空内容的来源不产生任何上下文块
- [ ] 2.2 新增 `ContextStrategy` 枚举（`MEMORY` / `ENRICHED` / `MEMORY_AND_ENRICHED`）与 `ContextInjector`；验证：单测覆盖三种组合下的来源集合
- [ ] 2.3 新增 `ContextAwareChatMemory` 装饰器（`add` 捕获本轮输入、`messages` 前置系统消息）；验证：单测断言注入的上下文位于最近消息之前，且最近 N 条原文仍按窗口保留
- [ ] 2.4 来源失败隔离（单个 provider 异常按空来源处理并记日志）；验证：单测断言某个 provider 抛异常时本轮不中断、其余来源与窗口仍可用

## 3. 一级滚动会话摘要

- [ ] 3.1 新增 `ai_chat_summary` 表结构与 `SummaryStore`（`session_key` 唯一，读写与 upsert）；验证：单测覆盖首次写入、更新与按会话读取
- [ ] 3.2 新增 `Summarizer` 接口与 `Summarizers.REGISTRY`（名字 → 内置、FQCN → 反射，照 `AiPersistenceStrategy.Registry` 形态）；验证：单测覆盖按名字命中内置、按 FQCN 反射构造、未知名报错
- [ ] 3.3 实现 5 个内置摘要策略（`refine` / `rebuild` / `map-reduce` / `segmented` / `structured`）；验证：每个策略各有一条单测，断言输入历史下产出非空摘要
- [ ] 3.4 实现双阈值滚动逻辑（`trigger-tokens` 触发、`keep-tokens` 压缩、每 `rebuild-every` 次插 `rebuild`），token 计数复用 `MagicDynamicTokenizer`；验证：单测断言未达阈值不触发、超出后压缩至 `keep-tokens` 以内、达 `rebuild-every` 走一次全量重摘
- [ ] 3.5 新增 `SummaryContextProvider` 并接入注入链，含读时兜底与失败回退；验证：单测断言读路径可在写路径未及时更新时补齐摘要，且摘要生成失败时本轮退化为最近 N 条原文
- [ ] 3.6 `Langchain4jAssistantProperties` 新增 `session.summary.*`（开关、策略、双阈值、`rebuild-every`、独立小模型等）；验证：属性绑定单测断言默认策略为 `refine`、`rebuild-every` 默认值生效

## 4. 装配与使用文档

- [ ] 4.1 在 `AssistantBuilder` / 自动配置中装配 `ContextAwareChatMemory` 与 provider 链（摘要来源按 `ContextStrategy` 启用）；验证：装配层单测断言启用 `MEMORY_AND_ENRICHED` 时注入链包含摘要来源、`MEMORY` 时不含
- [ ] 4.2 补充使用文档（会话键、`SessionIdResolver`、`ContextStrategy`、摘要配置与回滚方式），并同步设计文档中的配置说明；验证：按文档写出的配置样例可被属性绑定单测加载

## 5. 集成验证

- [ ] 5.1 集成验证：同一用户两个会话并行对话，断言彼此记忆互不可见；验证：集成测试断言跨会话读取不到对方消息
- [ ] 5.2 集成验证：长会话累计超出 `trigger-tokens` 后，断言上下文包含会话摘要且不超过 `keep-tokens`；验证：集成测试断言摘要段存在且原始消息未被删除
- [ ] 5.3 回归验证：既有 `memoryId()` 4 段契约与 `ChatMemoryIds.parseSegment` 既有回退语义不变；验证：`./mvnw -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test` 全绿

## Workflow follow-up

- 归档前按项目评审要求完成 verify 阶段。
- 归档后由后续 change `langchain4j-fact-memory` 复用本次建立的上下文注入抽象。

```

## docs/openspec/changes/langchain4j-session-memory/.openspec.yaml

- Source: docs/openspec/changes/langchain4j-session-memory/.openspec.yaml
- Lines: 1-2
- SHA256: c5936a7e8ac90455175dd905c9e8fb9d33cf82476be795f24133b51ef95c3d96

```md
schema: spec-driven
created: 2026-10-15

```

## docs/openspec/changes/langchain4j-session-memory/specs/langchain4j-context-injection/spec.md

- Source: docs/openspec/changes/langchain4j-session-memory/specs/langchain4j-context-injection/spec.md
- Lines: 1-53
- SHA256: 3148ec6e33b00bad367b3fc05496ddeb17e68206918d4aa11ecbb61a164e8f8d

```md
# Spec Delta

## Purpose

定义助手在调用模型前组装上下文的统一来源抽象与注入行为，使摘要、事实等上下文来源可插拔，并按可配置策略组合进模型输入。

## ADDED Requirements

### Requirement: 上下文来源可插拔

系统 SHALL 以有序的可插拔上下文来源集合组装模型输入，每个来源为本轮产出可选的文本块；某来源无内容时 SHALL 被跳过。

#### Scenario: 来源无内容时跳过

- **WHEN** 某个上下文来源对本轮返回空内容
- **THEN** 该来源 SHALL 不产生任何上下文块，且不影响其它来源

#### Scenario: 新增来源无需改动注入逻辑

- **WHEN** 注册一个新的上下文来源
- **THEN** 上下文组装流程 SHALL 无需修改即可将其纳入

### Requirement: 统一注入点与消息窗口正交

系统 SHALL 把组装出的上下文作为前置系统消息注入，SHALL NOT 改变最近消息窗口的语义。

#### Scenario: 注入不改变窗口

- **WHEN** 上下文注入已启用
- **THEN** 注入的上下文 SHALL 出现在最近消息之前，最近的若干条原文 SHALL 仍按窗口保留

### Requirement: 上下文组合策略可配置

系统 SHALL 支持按配置选择上下文组合方式，至少包含仅最近消息、仅增强来源、以及两者同时启用三种。

#### Scenario: 选择同时启用

- **WHEN** 组合策略配置为同时启用最近消息与增强来源
- **THEN** 系统 SHALL 同时提供最近消息窗口与所有已启用的附加来源

#### Scenario: 选择仅最近消息

- **WHEN** 组合策略配置为仅最近消息
- **THEN** 系统 SHALL NOT 注入任何附加来源的上下文

### Requirement: 来源失败相互隔离

单个上下文来源失败 SHALL 被视为空来源，SHALL NOT 中断本轮对话。

#### Scenario: 某来源解析失败

- **WHEN** 某个上下文来源在解析时抛出异常
- **THEN** 系统 SHALL 忽略该来源并继续，其余来源与最近消息窗口 SHALL 仍正常可用

```

## docs/openspec/changes/langchain4j-session-memory/specs/langchain4j-session-memory/spec.md

- Source: docs/openspec/changes/langchain4j-session-memory/specs/langchain4j-session-memory/spec.md
- Lines: 1-86
- SHA256: 3dbc9e7cfeb33599e0336075163777acb433f63b823dde358c665c64315841c7

[TRUNCATED]

```md
# Spec Delta

## Purpose

定义助手记忆的会话级作用域与一级滚动会话摘要行为，使同一用户的多次对话彼此隔离，并在长会话中保持主线信息不丢失。

## ADDED Requirements

### Requirement: 记忆键包含会话维度

助手聊天记忆 SHALL 以五段键 `{tenantId}:{appId}:{agentId}:{userId}:{sessionId}` 划分作用域，其中 `sessionId` 标识单次会话；记忆的读取与写入 SHALL 使用同一会话键。

#### Scenario: 同用户不同会话互相隔离

- **WHEN** 用户 u1 在会话 s1 中写入消息
- **THEN** 以 u1 与 s2 组成的键读取记忆时，SHALL NOT 返回 s1 的消息

#### Scenario: 既有四段键解析不受影响

- **WHEN** 记忆键为四段 `{tenantId}:{appId}:{agentId}:{userId}`
- **THEN** 系统 SHALL 仍能解析出 tenant、app、agent、user 四段并保持既有回退语义

### Requirement: 会话标识兜底

当调用方未提供 `sessionId` 时，系统 SHALL 通过可配置的会话解析器补全会话标识，默认按天生成，使最终生效的记忆键恒为五段。

#### Scenario: 未提供会话时按天兜底

- **WHEN** 调用方未提供 `sessionId`
- **THEN** 系统 SHALL 生成按天标识（形如 `dYYYYMMDD`）并以此构成五段记忆键

#### Scenario: 显式会话优先于兜底

- **WHEN** 调用方显式提供了 `sessionId`
- **THEN** 系统 SHALL 使用该值，SHALL NOT 用兜底规则覆盖它

### Requirement: 助手公开的四段记忆标识保持不变

`SimpleAssistant.memoryId()` SHALL 继续返回四段标识；会话维度 SHALL 通过新增能力暴露，SHALL NOT 改变该既有方法的返回契约。

#### Scenario: 既有断言保持

- **WHEN** 以 tenant=t1、app=crm、agent=bot、user=u1 构造助手并读取 `memoryId()`
- **THEN** 返回值 SHALL 等于 `t1:crm:bot:u1`

### Requirement: 一级会话摘要滚动维护

系统 SHALL 为每个会话至多维护一级滚动的会话摘要，覆盖范围限于已滚出最近消息窗口的历史；摘要 SHALL 在写入路径上抢在窗口驱逐之前更新，并在读取路径上兜底补齐。

#### Scenario: 超阈值触发摘要

- **WHEN** 某会话的历史累计超出 `trigger-tokens`
- **THEN** 系统 SHALL 生成或更新该会话摘要，并将其压缩到 `keep-tokens` 以内

#### Scenario: 摘要不删除原文

- **WHEN** 某会话摘要已生成
- **THEN** 该会话的原始消息 SHALL 仍完整保留在消息存储中，只有显式删除才移除

#### Scenario: 摘要失败时回退

- **WHEN** 摘要生成失败
- **THEN** 本轮上下文 SHALL 退化为最近若干条原文，主对话 SHALL NOT 中断

### Requirement: 摘要策略可插拔

系统 SHALL 支持按配置选择摘要策略，至少包含增量精炼、全量重摘、分段映射归并、逐段留存与结构化状态更新；默认 SHALL 为增量精炼，并按配置的间隔周期性执行一次全量重摘。

#### Scenario: 按配置切换摘要策略

- **WHEN** 配置指定了某个受支持的摘要策略
- **THEN** 系统 SHALL 使用该策略生成该会话摘要

#### Scenario: 增量与全量重摘交替

- **WHEN** 某会话连续执行增量精炼达到 `rebuild-every` 次数
- **THEN** 系统 SHALL 对该会话执行一次全量重摘

### Requirement: 摘要作用域与会话一致


```

Full source: docs/openspec/changes/langchain4j-session-memory/specs/langchain4j-session-memory/spec.md
