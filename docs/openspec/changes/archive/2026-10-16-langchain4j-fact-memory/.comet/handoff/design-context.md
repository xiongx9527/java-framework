# Comet Design Handoff

- Change: langchain4j-fact-memory
- Phase: design
- Mode: compact
- Context hash: 4e5b10fca3c8b3c1532ebdc9321f2d311240c88afaf02dfd0c32558550556396

Generated-by: comet-handoff.sh
Task hash policy: task-content-v1. Read tasks.md for live completion; excerpts are design-time context.

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## docs/openspec/changes/langchain4j-fact-memory/proposal.md

- Source: docs/openspec/changes/langchain4j-fact-memory/proposal.md
- Lines: 1-35
- SHA256: b24bafdb9e2bf317ba88516846fa3c515d33874f1d217976bf85626a9db659a2

```md
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

```

## docs/openspec/changes/langchain4j-fact-memory/design.md

- Source: docs/openspec/changes/langchain4j-fact-memory/design.md
- Lines: 1-86
- SHA256: db0f86ad23a49fc70b1217d1b78828eee3a95f23f9dcfd5a8408d90bd0be25ac

[TRUNCATED]

```md
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

```

Full source: docs/openspec/changes/langchain4j-fact-memory/design.md

## docs/openspec/changes/langchain4j-fact-memory/tasks.md

- Source: docs/openspec/changes/langchain4j-fact-memory/tasks.md
- Lines: 1-41
- SHA256: 42f3bd7294a4777cdae548f7b22048912fd9b5b009b3b383fc0dcbade55eda26

```md
# Tasks

> 详细设计与代码骨架见 `docs/superpowers/specs/2026-10-15-langchain4j-session-memory-design.md`（§7/§8/§9）。
> 实施前提：`langchain4j-session-memory` 已合入基线（本 change 依赖其 `ContextProvider` / `ContextInjector` / 5 段键）。
> 单测命令：`cd fw-plus-ai && ./mvnw -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dmaven.test.skip=false`

## 1. 事实通道骨架

- [ ] 1.1 新增 `FactStore`（独立 mem0 实例/集合，`infer=true`，按 `user_id` 划分、不带会话 `run_id`，含 `add` / `search`）；验证：单测断言写入请求不含会话键、读取按用户维度命中 <!-- comet-task:a3ba4c0e-ab7c-4525-80b4-0835f5893848 -->
- [ ] 1.2 验证事实通道与消息通道物理隔离；验证：单测断言事实写入后按会话读取消息记忆不出现该事实，且消息体解析不受影响 <!-- comet-task:88c7a697-5a37-493c-968e-73933788384a -->
- [ ] 1.3 `Langchain4jAssistantProperties` 新增 `session.facts.*`（`enabled`、`extract-strategy`、`write-mode`、`recall-strategy`、`top-k`、`model` 等）；验证：属性绑定单测断言默认值（`mem0` / `per-turn` / `semantic`）与显式覆盖 <!-- comet-task:31497d5c-eb79-4046-86f3-cc88d40c70f6 -->

## 2. 事实抽取与写入

- [ ] 2.1 新增 `FactExtractor` 接口与 `FactExtractors.REGISTRY`（名字 → 内置、FQCN → 反射，与 `Summarizers.REGISTRY` 一致）；验证：单测覆盖按名字命中、按 FQCN 反射构造、未知名报错 <!-- comet-task:f7a67e76-2e0b-4e59-ad88-24f79ef236a5 -->
- [ ] 2.2 实现 4 个抽取策略（`mem0` / `llm` / `rule` / `hybrid`）；验证：每个策略各有一条单测，其中 `rule` 策略在不依赖外部服务时可独立跑通 <!-- comet-task:eac14b4e-9e71-4130-882f-eee70849bf61 -->
- [ ] 2.3 实现 3 种写入模式（`per-turn` / `batched` / `session-end`）；验证：单测断言各模式的触发时机（逐轮每次、批量达阈值、仅会话结束） <!-- comet-task:2f9cd1f8-4471-4f40-b586-3d89bf9bad26 -->
- [ ] 2.4 抽取与写入的失败隔离；验证：单测断言抽取服务抛异常时本轮对话不中断且仅记录失败 <!-- comet-task:dd093cdf-263d-46f7-9506-747f6ad7b8d5 -->

## 3. 事实召回与注入

- [ ] 3.1 新增 `FactRetriever` 接口与 `FactRetrievers.REGISTRY`；验证：单测覆盖注册与查找 <!-- comet-task:c603bb3e-2377-43fb-beed-c80a678409f7 -->
- [ ] 3.2 实现 4 个召回策略（`semantic` / `keyword` / `hybrid` / `rerank`）；验证：每个策略各有一条单测，断言按查询返回相关事实并遵守 `top-k` <!-- comet-task:491e3f5d-eae5-4dca-b4d7-193449eb792b -->
- [ ] 3.3 新增 `FactContextProvider` 并接入第 1 项的注入链，位置在会话摘要之前；验证：单测断言最终顺序为 人设 → 事实 → 摘要 → 最近 N 条原文 <!-- comet-task:fdc9d35e-69a6-4168-a4da-c5f4287bdefa -->
- [ ] 3.4 召回无命中与召回失败的隔离；验证：单测断言无命中时事实段为空、失败时其余来源与窗口仍可用 <!-- comet-task:b0ca761c-1da8-4a23-a06b-ebb49cb203a5 -->

## 4. 装配与使用文档

- [ ] 4.1 在装配层按 `facts.enabled` 注册 `FactContextProvider`；验证：装配层单测断言关闭时不注入事实段、开启时注入 <!-- comet-task:40ca75fb-5ad4-4793-b97c-7b82b54b9705 -->
- [ ] 4.2 补充使用文档（事实通道与 mem0 服务端前提、三种抽取/写入/召回策略的配置、无 LLM 时退化到 `rule`、回滚方式）；验证：按文档写出的配置样例可被属性绑定单测加载 <!-- comet-task:2e023087-fca5-4c65-a3cd-84209df62d9f -->

## 5. 集成验证

- [ ] 5.1 集成验证：会话 s1 沉淀事实后，在会话 s2 的上下文中出现该事实；验证：集成测试断言 s2 召回命中 <!-- comet-task:d4328723-6d1c-44d3-b50f-5a6e87706293 -->
- [ ] 5.2 集成验证：事实写入不污染消息记忆；验证：集成测试断言按会话读取 `getMessages` 仍只含消息、解析正常 <!-- comet-task:38637fa9-2928-40ca-9c69-d67e05bc9f5f -->
- [ ] 5.3 回归验证：事实能力关闭或依赖不可用时不影响第 1 项的会话记忆与摘要；验证：`./mvnw -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test` 全绿 <!-- comet-task:2e99e2c0-5897-45cb-b140-08241c31ca01 -->

## Workflow follow-up

- 实施前确认 `langchain4j-session-memory` 已合入基线，否则先将本分支 rebase 到含该项的提交。
- 归档前按项目评审要求完成 verify 阶段。

```

## docs/openspec/changes/langchain4j-fact-memory/.openspec.yaml

- Source: docs/openspec/changes/langchain4j-fact-memory/.openspec.yaml
- Lines: 1-2
- SHA256: c5936a7e8ac90455175dd905c9e8fb9d33cf82476be795f24133b51ef95c3d96

```md
schema: spec-driven
created: 2026-10-15

```

## docs/openspec/changes/langchain4j-fact-memory/specs/langchain4j-fact-memory/spec.md

- Source: docs/openspec/changes/langchain4j-fact-memory/specs/langchain4j-fact-memory/spec.md
- Lines: 1-77
- SHA256: bcc187a19b6af7a4946b44706ce73753a88c1454aa0f8b6593e561bb0d1fb9f8

```md
# Spec Delta

## Purpose

定义跨会话的事实记忆行为：把对话中沉淀的稳定事实按用户维度保存，并在后续任意会话中作为独立上下文段召回，使助手在换会话后仍记得用户。

## ADDED Requirements

### Requirement: 事实通道与消息通道物理隔离

事实记忆 SHALL 保存在独立于消息记忆的通道中，以用户维度（跨会话）划分作用域，SHALL NOT 以会话为键，SHALL NOT 复用消息记忆的消息体格式。

#### Scenario: 事实写入不污染消息解析

- **WHEN** 一条事实被写入事实通道
- **THEN** 按会话读取消息记忆时 SHALL NOT 出现该事实，消息记忆的消息解析 SHALL 不受影响

#### Scenario: 事实跨会话可见

- **WHEN** 用户在会话 s1 中沉淀了一条事实，随后在会话 s2 中对话
- **THEN** 该事实 SHALL 可在 s2 中被召回并注入上下文

### Requirement: 事实抽取策略可插拔

系统 SHALL 支持按配置选择事实抽取策略，至少包含：由记忆服务内置推断、由模型抽取、规则抽取，以及二者混合。

#### Scenario: 按配置切换抽取策略

- **WHEN** 配置指定了某个受支持的事实抽取策略
- **THEN** 系统 SHALL 使用该策略从本轮对话中抽取事实

#### Scenario: 无可沉淀事实时静默

- **WHEN** 本轮对话未产生可沉淀事实
- **THEN** 系统 SHALL NOT 写入任何事实，且 SHALL NOT 影响本轮对话

### Requirement: 事实写入模式可配置

系统 SHALL 支持按配置选择事实写入时机，至少包含：逐轮写入、批量写入、会话结束时写入。

#### Scenario: 选择逐轮写入

- **WHEN** 写入模式配置为逐轮写入
- **THEN** 每轮对话结束后 SHALL 触发一次事实抽取与写入

#### Scenario: 选择会话结束时写入

- **WHEN** 写入模式配置为会话结束时写入
- **THEN** 系统 SHALL 仅在会话结束时集中抽取并写入事实，会话过程中 SHALL NOT 重复写入

#### Scenario: 会话结束的判定

- **WHEN** 写入模式配置为会话结束时写入，且同一用户在后续请求中使用了新的会话标识
- **THEN** 系统 SHALL 判定上一会话已结束，并集中抽取、写入其事实

### Requirement: 事实召回策略可插拔

系统 SHALL 支持按配置选择事实召回策略，至少包含：语义召回、关键词召回、混合召回、重排召回。

#### Scenario: 按本轮输入召回事实

- **WHEN** 某一轮请求需要注入事实上下文
- **THEN** 系统 SHALL 以本轮输入为查询，按配置的召回策略取回相关事实

#### Scenario: 无命中时跳过

- **WHEN** 事实召回没有命中任何事实
- **THEN** 事实段 SHALL 为空，其它上下文来源与最近消息窗口 SHALL 不受影响

### Requirement: 事实能力失败不影响主对话

事实的抽取、写入或召回失败 SHALL 被隔离，SHALL NOT 中断本轮对话。

#### Scenario: 事实依赖服务不可用

- **WHEN** 事实抽取或召回所依赖的服务不可用或超时
- **THEN** 系统 SHALL 记录失败并继续本轮对话，消息记忆与会话摘要 SHALL 仍正常可用

```
