# Comet Design Handoff

- Change: streaming-thinking-events
- Phase: design
- Mode: compact
- Context hash: 76587e45129aa5ee297585ea8c9d93ad552758018770b697b9e4635333c7798e

Generated-by: comet-handoff.sh
Task hash policy: task-content-v1. Read tasks.md for live completion; excerpts are design-time context.

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## docs/openspec/changes/streaming-thinking-events/proposal.md

- Source: docs/openspec/changes/streaming-thinking-events/proposal.md
- Lines: 1-31
- SHA256: 6fe045b71c229bace725de892903b7945fa825b1e8e6ae5473a7ac047c1ba072

```md
# 提案：跨提供商流式思考事件

## Why

`SimpleAssistant` 已将 LangChain4j 的独立思考回调映射为 SSE `thinking` / `thinking-done`，但 Ollama 样例流只收到检索、工具和回答事件：Ollama 流式模型没有启用生成思考及回传思考片段所需的选项。Magic-API 动态模型也由独立 provider 工厂构建，缺少对应配置；其他 provider 的思考能力尚未逐一确认，调用方无法判断哪些模型可以向前端提供真实思考事件。

## What Changes

- 修复 Ollama 样例与 Magic-API 动态模型的 thinking 配置透传；在样例流式配置中显式启用 Ollama 的生成与回传选项。
- 审核所有已注册的流式聊天 provider，只有当 LangChain4j 适配器能提供独立思考增量时，才暴露其真实 provider-specific 配置并纳入回归测试；无法映射独立思考内容的 provider 明确记录限制。
- 为 Magic-API 流式模型控制台增加与后端工厂一致的思考选项元数据和文案。
- 验证 SSE 思考回调以现有 `thinking` / `thinking-done` 事件发出；保持 `token`、`tool-call`、`tool-result` 等事件名和数据形状不变。
- 不从普通回答文本推测思考内容，不为所有 provider 添加虚假的统一开关。

## Capabilities

### New Capabilities

- `langchain4j-streaming-thinking-events`：定义 SimpleAssistant 流式对话对独立思考片段的 SSE 契约、事件顺序以及 provider 不支持时的行为。

### Modified Capabilities

- `langchain4j-model-datasource-plugins`：扩展 Ollama 及经能力核实支持思考回调的 streaming-chat-model provider options 与控制台字段契约；配置缺省时保持 SDK 默认值。

## Impact

- AI autoconfigure：`SimpleAssistant` 的 SSE 回调测试、全部 18 个流式 provider 工厂与能力测试。
- Magic-API 流式聊天模型插件：provider 字段元数据、中英文文案、元数据一致性测试及 README 限制说明。
- AI chat sample：Ollama 配置属性、流式模型构建、YAML 与配置测试。
- OpenSpec：新增流式思考事件规格，更新 `langchain4j-model-datasource-plugins` delta spec。
- 不更改 LangChain4j 依赖、第三方实现或 core 层 provider 源码；不更改现有 SSE 消费协议。

```

## docs/openspec/changes/streaming-thinking-events/design.md

- Source: docs/openspec/changes/streaming-thinking-events/design.md
- Lines: 1-81
- SHA256: 07ff3b5eedb7689f843b1bae552875e2a904653787f876bd62d02fa37e2a8752

[TRUNCATED]

```md
---
comet_change: streaming-thinking-events
role: technical-design
canonical_spec: openspec
---

# 设计：跨提供商流式思考事件

## Context

参见 `proposal.md` 的 Why 和本 change 的两个规格文件。当前 SSE 汇聚在 `SimpleAssistant.streamTokenStreamToSse`：LangChain4j `TokenStream` 的思考回调映射为 `thinking` 与 `thinking-done`，回答和工具回调分别映射为 `token`、`tool-call`、`tool-result`。已知 Ollama 1.20.2 适配器在 `returnThinking` 启用后才把 Ollama thinking 字段交给 LangChain4j 思考回调；Ollama 的 `think` 参数控制服务端生成思考。

Magic-API 的 provider 配置经 `Langchain4jOptionsReader` 读取，前端 streaming-chat-model 子插件 `providers.js` 有后端工厂字段一致性测试。当前注册了 18 个显式实现流式模型构建方法的 provider：Anthropic、Azure OpenAI、Bedrock、Cohere、GitHub Models、Google GenAI、LocalAI、OCI GenAI、Ollama、OpenAI、OpenAI Official、Qianfan、Qwen、Mistral AI、Vertex AI Gemini、Watsonx、Xinference、Zhipu AI。provider SDK 能力和版本并不一致；工厂存在不等同于适配器提供独立思考回调。

## Goals / Non-Goals

**Goals:**

- 完成上述 18 个 provider 的逐项能力判定，并在实现和控制台元数据中保持一致。
- 保持 provider 配置缺省时原有 SDK 默认行为；只对 Ollama 样例显式启用 thinking。
- 保证思考回调结束时先发 `thinking-done` 再发 `done`，避免只返回思考内容的流遗漏思考轮结束事件。
- 通过工厂、控制台元数据和 SSE 回归测试锁定端到端契约。

**Non-Goals:**

- 不新增 provider 无关的 `thinking.enabled` 开关。
- 不在 LangChain4j 未提供独立思考回调时解析私有原始响应或回答文本。
- 不改动同步/非流式调用的用户可见行为，除非某个已有 provider 工厂必须共享同一配置对象；不得因此为同步请求启用思考。

## Decisions

### 1. 以 `PartialThinking` 回调能力作为支持标准

对 18 个 `buildStreamingChatModel` 工厂逐一核对当前 LangChain4j 依赖版本中的公开 builder 参数，以及 SDK 响应处理器是否把独立思考增量映射到 `StreamingChatResponseHandler.onPartialThinking`。结果归入：

| 分类 | 实施行为 |
|---|---|
| 有公开启用选项，且映射为独立 thinking 回调 | 工厂透传 provider 专属选项；streaming-chat-model 控制台只增加该 provider 实际选项；测试断言选项透传及其能力说明 |
| 无需启用选项，但能映射独立 thinking 回调 | 不增造开关；记录 SDK 默认/模型触发条件；用可验证的适配器行为测试 |
| 适配器不映射独立 thinking 回调，或没有公开配置能力 | 不解析私有原始响应，不显示通用思考开关；README 能力表注明限制及已审核的 SDK 版本 |

不能仅凭 provider 厂商具有 reasoning 模型，就认定对应 LangChain4j provider 实现支持独立思考回调。即使适配器支持，模型也可能不返回 thinking；此时 SSE 不产生思考事件是合法行为。

**替代方案：** 给全部工厂增加 `thinking.enabled`。拒绝：它隐藏 provider 参数差异，不能让缺少响应映射的 SDK 发出 `PartialThinking`，并会制造所有 provider 均支持的错误预期。

### 2. provider 配置遵循现有 `options` 与前端元数据边界

有原生配置能力的选项继续用各 SDK 的真实参数名、类型及默认值，经 `Langchain4jOptionsReader` 从现有 datasource `options` 读取。Ollama 使用 `think` 与 `return-thinking`；Ollama 样例属性与 YAML 绑定这两个字段，只在流式模型配置中将其设为 `true`。Magic-API 流式模型的 Ollama 元数据同步显示这两个字段。

其他 provider 只在能力审核通过后才新增工厂读取项和对应 UI 字段。每个 provider 的字段集合由现有 Node 一致性测试从后端 `buildStreamingChatModel` 中读取项反向校验，且增加中英文文案断言。未配置的布尔值传 `null`，保持 SDK builder 默认语义。

**替代方案：** 改 datasource schema 或引入统一嵌套 thinking 子对象。拒绝：现有 provider 差异参数已经由 `options` 表达，无需迁移持久化结构；不同 provider 的选项名与数值含义并不一致。

### 3. SSE 继续由单一 provider 无关的桥接器处理

保留 `SimpleAssistant` 当前的 LangChain4j 回调链与 SSE payload。`onPartialThinkingWithContext` 收到片段时发 `thinking` 并累积文本；思考阶段在转入回答、工具调用或完成时发一次 `thinking-done`。在 `onCompleteResponse` 的完成路径中先 flush 尚未结束的思考轮，再发 `done`。回答、工具或 retrieved 回调行为不因 provider 能力分类而改变。

此桥接覆盖 AiServices TokenStream 与直接 StreamingChatModel 两条路径。后者当前的 `DirectTokenStream.onPartialThinkingWithContext` 是 no-op；将它扩展为保存 thinking handler，并在 `StreamingChatResponseHandler.onPartialThinking` 到达时转发公开的 `PartialThinking` 增量。锁定的 `langchain4j-core:1.20.2` 提供该 handler 回调；直接路径在回答开始时沿用共享 tracker 闭合 thinking，在完成回调中也先闭合 thinking 再发送 `done`。直接路径不新增工具执行能力。

单测使用可控 TokenStream 和 StreamingChatModel 替身分别验证两条路径的思考增量、思考完成与 `token` / `tool-call` / `done` 的先后次序，以及无思考输入不产生思考事件。provider 工厂测试独立验证 SDK 配置，避免把实时模型和外网服务引入单测。

**替代方案：** 由 Controller 或前端区分 provider 并订阅厂商专有事件。拒绝：SSE 桥接已有稳定事件名，provider 专有协议应止于模型适配器。

### 4. 审核结果随 streaming-chat-model 控制台能力文档交付

逐 provider 审核记录到流式模型插件 README，至少包含 provider、当前 LangChain4j 实现/版本、独立思考回调状态、可配置选项（若有）和限制。控制台元数据测试继续保证显示字段恰好等于工厂实际消费字段；文档能力列表用于解释“字段存在”与“某个部署模型必然输出思考”之间的差异。

## Risks / Trade-offs

- **SDK 版本或模型端点升级改变映射行为** → 用本仓库锁定的 SDK 依赖核对公开 API 与适配器源/字节码，并将版本和分类写入 README；升级依赖时能力表测试与审核需要复核。
- **服务端或模型不支持 thinking 选项而返回错误** → 默认不设置选项；只有样例明确启用。沿用现有 SSE error 路径，不把失败伪装成正常回答。
- **能力检查只覆盖工厂参数，未证明真实 provider 响应会产出思考** → 区分“适配器能映射”与“模型必然生成”；离线测试验证配置及本地响应映射能力，真实模型输出作为可选集成检查，不作为跨 provider 必须可重复的单测条件。
- **结束时思考轮未闭合** → 在 `onCompleteResponse` 前 flush 活跃思考轮，并通过只含 thinking 的流测试确保 `thinking-done` 先于 `done`。

## Migration Plan

无需迁移 datasource JSON 结构。既有配置未设置新增 provider options 时仍采用 LangChain4j 默认值。Ollama 样例显式启用 thinking；使用不支持 thinking 的 Ollama 模型时，用户可将流式配置的 `think` / `return-thinking` 设为 `false` 或删除配置。回滚时移除新增 builder 透传及样例开关，旧 SSE 事件保持兼容。

## Open Questions


```

Full source: docs/openspec/changes/streaming-thinking-events/design.md

## docs/openspec/changes/streaming-thinking-events/tasks.md

- Source: docs/openspec/changes/streaming-thinking-events/tasks.md
- Lines: 1-21
- SHA256: 25320c7bb726d5f92d1919fbc17279d1acac02986c17bbb0630fd7f7d6a1ebcc

```md
# Tasks

## 1. 核实 Provider 思考能力

- [ ] 1.1 审核全部 18 个流式模型工厂及其锁定版本 LangChain4j builder/响应映射，逐 provider 记录独立 `PartialThinking` 能力、原生配置、默认值和限制；通过源码/字节码核对及可执行适配器测试验证分类，并将结果交付到流式插件 README 能力表

## 2. Provider 工厂与控制台配置

- [ ] 2.1 为审核确认支持独立 thinking 回调的 provider 增加真实 provider-specific 选项透传，保持未配置时 SDK 默认行为；通过工厂单测验证 options 值、缺省值及 builder 调用
- [ ] 2.2 同步更新 streaming-chat-model provider 元数据、中英文 i18n 和配置说明，只呈现审核确认的原生选项；运行既有 Node 元数据一致性测试并验证每个 UI 字段均由对应工厂读取
- [ ] 2.3 为不支持独立 thinking 回调的 provider 保持无通用 thinking 开关，在 README 能力表注明依据及限制；通过元数据测试确认未引入猜测式配置项

## 3. SSE 桥接与 Ollama 示例

- [ ] 3.1 修正 `SimpleAssistant` 两条流式路径，使活跃思考轮在 `done` 之前恰好发送一次 `thinking-done`，并让 DirectTokenStream 转发公开的 `PartialThinking` 回调；以 TokenStream 与 StreamingChatModel 替身覆盖 thinking-only、thinking 转回答/工具、无 thinking 和事件顺序
- [ ] 3.2 在 Ollama streaming-chat-model 工厂透传 `think` 与 `return-thinking`，同步配置流式插件控制台字段和选项测试；确认未配置时不覆盖 Ollama SDK 默认值
- [ ] 3.3 在 AI Chat 样例中提供 Ollama 思考参数配置，并仅对流式模型显式启用所需参数；以样例配置测试验证参数绑定和 builder 透传，更新 README 中关于流式模型的说明

## 4. 集成验收

- [ ] 4.1 运行所有变更涉及的工厂、SSE、样例和控制台元数据测试，并验证既有 `thinking`、`thinking-done`、`token`、`tool-call`、`tool-result`、`done` SSE 协议及顺序保持兼容

```

## docs/openspec/changes/streaming-thinking-events/.openspec.yaml

- Source: docs/openspec/changes/streaming-thinking-events/.openspec.yaml
- Lines: 1-2
- SHA256: ad734da2ba5f4c5a6979a8bbb6bad4db20fb21ec275ceca5f24842fbdfaf5bef

```md
schema: spec-driven
created: 2026-10-04

```

## docs/openspec/changes/streaming-thinking-events/specs/langchain4j-model-datasource-plugins/spec.md

- Source: docs/openspec/changes/streaming-thinking-events/specs/langchain4j-model-datasource-plugins/spec.md
- Lines: 1-27
- SHA256: bcff0663deba464b63a78627e391675495ce4c3021d2815eae4b7171a50a3f51

```md
# Spec Delta

## MODIFIED Requirements

### Requirement: 控制台前端资源与插件标识

插件 SHALL 声明插件标识并向前端提供控制台资源，使每个数据源类型在控制台有对应的配置组件；`options` 表单字段 SHALL 依据「数据源类型 × provider」的元数据渲染，因此不同 provider 呈现不同字段。对于已核实支持独立思考输出的 streaming-chat-model provider，其 provider 专属思考选项 SHALL 在控制台中可配置，并与后端工厂读取的选项一致；不支持独立思考输出的 provider SHALL 不展示伪造的通用思考选项。

#### Scenario: 插件资源可加载
- **WHEN** 插件前端产物存在于约定资源路径
- **THEN** 控制台加载该产物，并以插件标识注册该插件的页面能力

#### Scenario: 按类型与 provider 渲染表单
- **WHEN** 在控制台新建某类型数据源并切换 `provider`
- **THEN** `options` 表单字段随所选 provider 变化，只显示该 provider 声明的字段

#### Scenario: embedding-store 选择 embedding-model
- **WHEN** 在控制台新建 `langchain4j-embedding-store`
- **THEN** 存在一个可选择已配置 `langchain4j-embedding-model` 的 `embeddingModelKey` 输入项

#### Scenario: 前端产物缺失时降级
- **WHEN** 插件前端产物不在约定资源路径
- **THEN** 插件仅按基础标识注册，服务端装配仍正常工作

#### Scenario: thinking 字段与后端能力一致
- **WHEN** 在控制台配置 streaming-chat-model 的 provider
- **THEN** 只显示该 provider 经确认可用于独立思考输出的真实 provider 专属选项，且每个显示字段均由对应后端工厂读取

```

## docs/openspec/changes/streaming-thinking-events/specs/langchain4j-streaming-thinking-events/spec.md

- Source: docs/openspec/changes/streaming-thinking-events/specs/langchain4j-streaming-thinking-events/spec.md
- Lines: 1-47
- SHA256: a2f779e9070663440558e68a09de9f9e96a6ef5e30e38a292adfd3beeb3398b6

```md
# Spec Delta

## Purpose

使 LangChain4j 流式对话能够把模型明确提供的独立思考片段传递给 SSE 客户端，同时保留回答、工具和结束事件的既有协议；未提供独立思考能力的模型不得被伪装成会发出思考内容。

## ADDED Requirements

### Requirement: 流式思考片段通过既有 SSE 事件发送

流式对话 SHALL 将模型适配器明确提供的思考增量作为 `thinking` SSE 事件发送，data 为该增量文本；思考阶段结束后 SHALL 发送一次 `thinking-done` 事件，data 为本轮完整思考文本。思考转入回答、工具调用或流完成时，思考完成事件 SHALL 先于对应的 `token`、`tool-call` 或 `done` 事件发送。没有独立思考增量时 SHALL 不发送思考事件，也 SHALL 不从普通回答文本推测思考内容。

#### Scenario: 推送增量思考与完整思考
- **WHEN** 流式模型产生一个或多个独立思考增量，随后开始输出回答
- **THEN** 客户端先收到对应的 `thinking` 增量事件，再收到包含完整思考文本的 `thinking-done`，然后收到回答 `token` 事件

#### Scenario: 思考后直接调用工具
- **WHEN** 流式模型产生独立思考增量后请求执行工具
- **THEN** 客户端先收到 `thinking` 与 `thinking-done`，再收到 `tool-call`；工具执行完成后仍收到 `tool-result`

#### Scenario: 思考阶段结束时没有回答片段
- **WHEN** 流式模型已产生独立思考增量并在 `done` 前结束该思考阶段，且没有回答或工具调用事件可作为阶段边界
- **THEN** 客户端在 `done` 前收到 `thinking-done`，其 data 为本轮完整思考文本

#### Scenario: 模型没有独立思考输出
- **WHEN** 模型只产生回答文本，或 provider 适配器没有提供独立思考增量
- **THEN** 客户端只收到既有回答与完成事件，不会收到伪造或从回答文本推测的 `thinking` 事件

#### Scenario: 直接流式模型回调思考增量
- **WHEN** SimpleAssistant 通过直接 StreamingChatModel 路径流式调用模型，且 LangChain4j 的 `StreamingChatResponseHandler` 提供独立思考增量
- **THEN** 客户端收到既有格式的 `thinking` 事件；思考转入回答或流完成时先收到一次 `thinking-done`，再收到对应的 `token` 或 `done`

#### Scenario: 思考配置导致 provider 请求失败
- **WHEN** 配置了 provider 专属思考选项，但当前模型或服务端拒绝该选项
- **THEN** 流式请求通过既有错误事件与错误结束路径向客户端报告失败，不静默改写为普通思考/回答文本

### Requirement: 思考能力依赖 provider 的独立输出支持

每个流式聊天 provider SHALL 仅在其 LangChain4j 适配器可以输出独立思考增量时，才将该能力映射至思考 SSE 事件。provider 专属思考选项 SHALL 按各自真实名称和类型配置；没有该选项的 provider SHALL 保持其模型默认行为。系统 SHALL 明确记录适配器不映射独立思考输出的 provider 限制。

#### Scenario: provider 原生选项启用思考输出
- **WHEN** provider 有公开思考选项，调用方按该 provider 配置启用思考，且模型实际返回独立思考内容
- **THEN** LangChain4j 适配器提供的思考增量按本能力的 SSE 契约发送给客户端

#### Scenario: provider 没有独立思考映射
- **WHEN** provider 只在原始协议中存在思考相关字段，但 LangChain4j 适配器没有将其映射为独立思考增量
- **THEN** 系统不从原始回答流进行猜测式解析，并在能力说明中声明该 provider 当前不能发出思考 SSE 事件

```
