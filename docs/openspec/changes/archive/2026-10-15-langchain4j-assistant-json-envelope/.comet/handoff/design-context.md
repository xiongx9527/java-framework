# Comet Design Handoff

- Change: langchain4j-assistant-json-envelope
- Phase: design
- Mode: compact
- Context hash: e2f75b027ff64a72c116f710c3b41aeffeb39650f41aa819e04ddbbf5f60b8cb

Generated-by: comet-handoff.sh
Task hash policy: task-content-v1. Read tasks.md for live completion; excerpts are design-time context.

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## docs/openspec/changes/langchain4j-assistant-json-envelope/proposal.md

- Source: docs/openspec/changes/langchain4j-assistant-json-envelope/proposal.md
- Lines: 1-94
- SHA256: d22b8d31edc5ddcf58614d64b925f28fa7371038bc0c6b450921cb13d3f969ef

[TRUNCATED]

```md
# Proposal

## Why

fw-plus 的 LangChain4j 集成目前只有一种对话输出形态：把模型原始文本按 SSE 帧吐出，`token`/`done`
帧承载的是纯文本。调用方（Magic-API 脚本或前端）拿到的是自由文本，既无法直接渲染成卡片或表单，
也无法在业务失败时得到一致的、机器可读的结果。

结果是每个业务各自重写一遍提示词、解析与容错：模型输出夹带解释文字、JSON 被截断、字段缺失或类型不符时，
调用方只能拿到一段文本，前端无法判断「这轮是成功还是失败、失败该重试还是该显示兜底话术」。
同一类问题在每个业务里反复出现，且没有收敛点。

同时，仓库里已有一批未提交的结构化输出工具类与样例（`StructuredOutput` 等，20 例测试全绿）。
本 change 将其一并纳入，并补上缺失的嵌套路径校验能力，避免同一能力出现两套实现。

## What Changes

- 新增统一**聊天信封**（envelope）：`done` 帧的 `data` 从纯文本字符串变为统一 JSON 信封对象，
  非流式返回**同一份信封**，两条通道同形（5 键：`ok` / `business` / `v` / `result` / `text`）。
- 信封收敛件是唯一闸门且**永不抛异常**：正常 JSON、夹带解释文字的 JSON、被截断的 JSON、纯闲聊、
  空回复（`[DONE]`）全部收敛成合法信封；失败轮次统一 `ok=false` 且 `result=null`。
- 新增**业务契约**与**业务注册表**：每份契约声明名称、内层 JSON Schema、校验规则与是否结构化；
  内层 `result` 的结构由业务契约决定，框架不在外层引入任何业务语义字段（`needMore`、`missing`、
  `intent`、`orderId` 等一律进 `result`）。
- 新增**路由**：入口只接收 `message` 与 `memoryId`，本轮 `business` 完全由模型分类决定，
  每轮判定一次、不沿用上一轮结果；判定失败或名称未注册时兜底 `business="chat"`。
  分类开关默认开启，关闭即让所有轮次落 `chat`（应急降级）。
- 新增两个开关：帧形态开关（只能全局，默认关，关闭时现有行为一字不变）与结构化开关
  （业务级 + 单次调用级，覆盖顺序：单次调用 > 业务契约 > 全局默认 false）。
- 结构化输出工具类补齐**点号路径**校验规则（错误文本带路径，便于回喂）与**从 JSON Schema
  派生规则**（`Rule.fromSchema`），使每份业务契约的内层规则不必手写。
- 新增样例模块（两个假业务 + 假模型），无 API Key 离线可跑，覆盖分类命中成功/失败、业务追问、
  分类未命中四条路径。
- 既有未提交产出（`StructuredOutput` / `Rule` / `Rules` / `StructuredOutputException` 及 20 例测试、
  结构化输出样例模块）并入本 change 一起交付。

`token` 帧**保持纯文本**，`start` / `error` 帧形态不变，既有方法签名一个都不动。

## Capabilities

### New Capabilities

- `langchain4j-structured-output`: 大模型结构化输出的指令生成、宽容解析、双层校验（JSON Schema +
  轻量规则）与错误回喂重试；含嵌套路径校验规则与从 JSON Schema 派生规则。
- `langchain4j-chat-envelope`: 统一聊天信封的键位、取值语义（含 `text` 与 `ok=false ⇒ result=null`）、
  降级规则（永不抛、空回复降级）与两个开关的配置契约。
- `langchain4j-assistant-business-routing`: 业务契约与注册表、分类驱动的业务判定、对话历史共用
  与结构化调用的入口形态。

### Modified Capabilities

无。本次不改变任何既有 capability 的 requirement（`langchain4j-script-module`、
`langchain4j-model-datasource-plugins`、`magicapi-langchain4j-tool-provider` 等均不动）。

## Impact

- 代码（新增/增强，均在 `fw-plus-ai-project/fw-plus-ai-boot-autoconfigure`）：
  - 增强 `.../langchain4j/support/structured/`（`Rule` 点号路径、`Rule.fromSchema`）；
  - 新增 `.../langchain4j/support/envelope/`（信封与业务契约路由）。
- 调用面：SSE `done` 帧与非流式返回的形态，**仅在帧形态开关开启后生效**；关闭时零影响。
- 样例：`fw-plus-sample/fw-plus-ai-samples/` 新增样例模块，并在聚合 POM 注册。
- 依赖：不新增第三方依赖（复用 fastjson2 `JSONSchema`，2.0.53 自带、Java 8 可用）。
- 兼容性：帧形态开关默认关，既有 Magic-API 脚本与调用方无需改动。
- 不在范围内：`fw-plus-core`、`fw-plus-boot`、`fw-plus-cloud` 三个库层零改动。

## 非目标

- 不做 magic-script 专用形态：不注册 `@MagicModule`、不新增 `POST /chat/json`、
  不给现有类新增 `chatJson(...)`。
- 不改任何既有方法签名（入参、出参一个都不动）。
- 不做写操作闭环：不定义 action 回传协议、不做按钮白名单与状态机（`result` 里业务可以自带展示字段）。
- 不做业务管理界面；业务数量按少量（3~5 个）设计，注册方式为配置 + 代码注册。
- 不引入 provider 原生 JSON Schema（如 OpenAI `response_format`）。
- 不为「同一会话内切换帧形态」提供支持：开关一旦开启，会话内帧形态恒定。
- 不提供显式指定业务的入口，也不提供每业务一个专用入口：`business` 只由模型分类决定。

## 关键未知项与风险

- 重试污染记忆：重试会经 `EphemeralChatMemory` + `PERSIST_NON_TOOL_FINAL_ANSWER` 把校验错误原文
  沉淀进该 `memoryId` 的历史。对策：重试使用隔离的记忆标识，错误只经提示词回喂，不写入会话历史。

```

Full source: docs/openspec/changes/langchain4j-assistant-json-envelope/proposal.md

## docs/openspec/changes/langchain4j-assistant-json-envelope/design.md

- Source: docs/openspec/changes/langchain4j-assistant-json-envelope/design.md
- Lines: 1-393
- SHA256: 45eaada21e717d2f36c532f15a6a92857080274e356c6e4be89da0b2805c4742

[TRUNCATED]

```md
---
comet_change: langchain4j-assistant-json-envelope
role: technical-design
canonical_spec: openspec
---

# Design

## Context

动机与范围见 `proposal.md`；行为契约见 `specs/` 下三份 delta spec。本文件记录实现路径所需的现状与约束。

现有实现与接缝（读源码核实，非推测）：

- `.../langchain4j/support/assistant/SimpleAssistant.java` 封装 AiServices：`streamToSse(...)` 有 7 个重载，
  其中带 `Function<String,Object> doneDataMapper` 的重载（L792）与带 `onStarted`/`onDone` 的重载（L802）
  已能让 `done` 帧承载任意对象；无 mapper 的重载（L762/L766/L771/L776/L781）直接把模型原文文本放进 `done`。
- SSE 事件常量在 `SimpleAssistant`（L67-82）：`start`（data=requestId）、`token`（String 片段）、`done`
  （完整文本；模型空回复时为 `DONE_FALLBACK = "[DONE]"`）、`error`（错误信息字符串）。`doneDataMapper`
  的入参是**模型完整文本**，返回值进 `done` 帧。
- `done` 帧放对象可行：`SseEmitter.event().data(Object)` 不指定 mediaType，
  `AbstractHttpMessageConverter.canWrite(clazz, null)` 为 true，String 转换器只接管 `String`，其余由 Jackson 写出 JSON。
- 记忆按 `memoryId` 隔离：`AssistantBuilder.resolveChatMemoryProvider()`（L616-650）为每个 `memoryId` 建
  一份 `MessageWindowChatMemory`/`TokenWindowChatMemory`（`.id(memoryId)`），store 默认 `InMemoryChatMemoryStore`，
  外层包 `EphemeralChatMemory`；默认持久化策略 `AiPersistenceStrategy.PERSIST_NON_TOOL_FINAL_ANSWER`
  （`.../support/memory/AiPersistenceStrategy.java:153-179`）会把上一轮的非工具 AI 回复沉淀进 store——
  这是"重试污染历史"的根因。
- 不写记忆的直连路径已存在：`SimpleAssistant.generate(String)`/`generate(List<ChatMessage>)`
  → `ChatLanguageModel.generate(...)`（L548-561），不经 AiServices、不读写 ChatMemory；
  `generateText(...)`/`generateTextToSse(...)` 走 `LanguageModel`/`StreamingLanguageModel`（直连补全）。
- 记忆读写的组件口径：`ChatMemory` 提供 `messages()`/`add(...)`/`clear()`；`ChatMemoryStore` 提供
  `getMessages`/`updateMessages`/`deleteMessages`，且 `LangChain4jSupportAutoConfiguration` 已注册
  `ChatMemoryStore` Bean（`in-memory` 默认 / `mem0`），非流式与流式两侧可用同一个 store 共用历史。
- 配置接线样板现成：`Langchain4jModuleConfiguration` 的 `@EnableConfigurationProperties` +
  `@ConditionalOnProperty` + `@Bean @ConditionalOnMissingBean`；`LangChain4jSupportAutoConfiguration`
  （support 层，前缀 `fw.plus.ai.langchain4j`，`enabled` 默认 true）已 `@Import` 多个嵌套配置类。
  只有**新增自动配置类**才需要同时改 `META-INF/spring.factories` 与
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
  （有 `AutoConfigurationMetadataConsistencyTest` 强制两者一致）；**新增 `@Bean`/嵌套配置类不需要改注册文件**。
- 版本约束：本层锁定 langchain4j 0.35.0，接口是 `dev.langchain4j.model.chat.ChatLanguageModel` 与
  `StreamingChatLanguageModel`（`ChatModel` 是 1.x 才有的名字，此处不存在）。测试假模型只需实现
  `generate(List<ChatMessage>)` / `generate(List<ChatMessage>, StreamingResponseHandler<AiMessage>)`。
- 既有测试基建：假模型惯用**测试类内部私有静态类**（`SimpleAssistantTest.RecordingChatModel` 按调用序号
  回放脚本回复）；SSE 断言靠 `onStarted`/`onDone` 回调侧信道，**没有**记录型 `SseEmitter`，`SseEmitter`
  在 `streamTokenStreamToSse` 内部 `new SseEmitter(0L)` 构造，无注入点。
- 既有意图分类：`.../support/intent/LlmIntentClassifier.java` 是泛型 `Enum` 绑定的独立实现
  （自建 systemPrompt + 正则解析 + 置信度阈值），与本次的"运行时注册的业务名"抽象不同。
- 前置产出：`.../langchain4j/support/structured/`（`StructuredOutput`、`Rule`、`Rules`、
  `StructuredOutputException` 及 20 例测试，尚未提交）已实现"指令 → 宽容解析 → 双层校验 → 错误回喂重试"，
  其中 `StructuredOutput.chat(ChatFunction, userText, maxAttempts)` 已是**回调式重试回路**，
  不依赖 ChatMemory，可直接承接"零写入重试"。

约束：

- Java 8（不得使用 Java 9+ API）；不新增第三方依赖。
- 既有方法签名零改动（新增方法/新增 setter 允许）；`fw-plus-core`、`fw-plus-boot`、`fw-plus-cloud` 零改动。
- 流式 mapper 重载固定使用 `DEFAULT_CONTEXT`，无法承载"每轮变化的系统提示"。
- `EphemeralChatMemory` + `PERSIST_NON_TOOL_FINAL_ANSWER` 会把参与过的错误原文写进该 `memoryId` 的历史。

## Goals / Non-Goals

**Goals:**

- 一个通用入口 + N 份业务契约；外层统一 5 键信封，内层 `result` 由业务自定，成功与失败同形。
- 接线零侵入：帧形态开关默认关时，既有输出与调用方行为一字不变。
- 收敛件是唯一闸门且永不抛异常：任何输入（含截断、空回复、异常对象）都收敛为合法信封。
- 校验能力可扩展：内层契约各不同，规则由 schema 派生而不是手写。
- 跨轮次连续：结构化轮次与闲聊轮次共用同一份会话历史。

**Non-Goals:**

- 不引入 provider 原生 JSON Schema（如 OpenAI `response_format`）；本层不感知具体模型厂商。
- 不做写操作回传闭环（action 协议、按钮白名单、状态机）。
- 不做业务管理界面；业务按少量（3~5 个）设计，注册方式为配置 + 代码注册。
- 不支持同一会话内切换帧形态。
- 不提供显式指定业务的入口（含每业务一个专用入口）：`business` 只由分类决定。
- 不为既有 `chat(...)` 入口增加结构化重载（`chatJson(...)` 之类一律不做）。
- 不接管自带 `done` 数据映射/回调的流式入口、带检索过滤的流式入口、`generateTextToSse(...)`
  与既有非流式入口。
- 结构化轮次不使用 tools / RAG / `systemMessageProvider`（该系统提示由契约或装配期配置提供）。

```

Full source: docs/openspec/changes/langchain4j-assistant-json-envelope/design.md

## docs/openspec/changes/langchain4j-assistant-json-envelope/tasks.md

- Source: docs/openspec/changes/langchain4j-assistant-json-envelope/tasks.md
- Lines: 1-39
- SHA256: 5724df22480db3294260fb10a955d29d1340387b6784296c7ceca7f5bda04a55

```md
# Tasks

## 1. 结构化输出规则增强（规则带路径 + 从 schema 派生）

- [ ] 1.1 为内置校验规则（必填 / 类型 / 枚举 / 数值范围 / 字符串模式）增加点号路径支持，错误文本包含完整路径；扩展 `StructuredOutputTest` 覆盖嵌套字段与顶层字段回归，`cd fw-plus-ai && ./mvnw -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dmaven.test.skip=false` 全绿
- [ ] 1.2 实现从 JSON Schema 派生校验规则（`Rule.fromSchema`）：递归读取 `properties` / `required` / 类型 / 枚举 / 数值范围 / 字符串模式，为嵌套字段生成点号路径规则；测试覆盖嵌套必填、枚举、范围、模式，以及“派生规则与手写规则并用”时错误清单同时包含两者，命令同上
- [ ] 1.3 在 `Rule` / `Rules` 的 Javadoc 写明点号路径语义与派生限制（不支持 `anyOf` / `oneOf` / `allOf`，schema 校验只报首个错误，全量清单由规则侧承担）；用 `grep -rn "anyOf" <文件>` 确认限制已写进文档

## 2. 信封收敛件与配置

- [ ] 2.1 新增 `ChatEnvelope`：固定 5 键 `ok` / `business` / `v` / `result` / `text`，成功与失败同形，强制 `ok=false ⇒ result=null`；`v` 为字符串、默认 `"1"`、可被业务契约覆盖；单测断言四个取值路径（结构化成功 / 失败 / 闲聊 / 空回复）的键恒定与取值语义
- [ ] 2.2 新增 `EnvelopeAssembler`：`assemble(...)` 与唯一闸门 `ensure()`，覆盖合法 JSON、夹带解释文字的 JSON、被截断的 JSON、纯文本、空回复（`[DONE]`）、异常对象六类输入；单测断言收敛过程不抛异常，且各路径 `ok` / `result` / `text` 取值符合契约
- [ ] 2.3 新增 `Langchain4jAssistantProperties`（前缀 `fw.plus.ai.langchain4j.assistant`）：帧形态开关（默认 `false`）、结构化全局默认（`false`）、分类开关（默认 `true`）、分类历史条数（默认 3）、分类尝试次数（默认 1）、结构化最大尝试次数（默认 3）、会话窗口条数（默认 20）；启动期非法配置 fail-fast，运行期非法配置保留旧值并告警；单测覆盖三层覆盖顺序（单次 > 业务 > 全局）与两类非法值处置，并在 Javadoc 写明全部配置键与默认值

## 3. 业务契约、注册表、分类与记忆

- [ ] 3.1 新增 `BusinessContract`（名称、内层 schema、零到多条规则、结构化默认值、最大尝试次数、系统提示、契约版本 `v`）与 `BusinessRegistry`：注册阶段同名冲突立即失败，解析未注册名称回落兜底 `chat`；单测覆盖注册成功、重名失败、未注册回落
- [ ] 3.2 实现分类驱动的路由：入口只接收 `message` 与 `memoryId`，本轮业务完全取分类结果（每轮判定、不沿用上一轮），未命中或判定失败回落 `chat`；同一会话共用一份历史，跨业务切换保持连续；单测覆盖“相邻两轮话题不同各自判定”“切换后历史不清空”“未注册回落 `chat`”
- [ ] 3.3 实现 `BusinessClassifier`：以已注册业务名作为枚举、用 `StructuredOutput` 判定；输入含最近 N 条用户消息（可配，0 = 只看本轮）以使追问轮次仍能命中；默认每次判定不重试；分类开关关闭时所有轮次落 `chat` 且不产生分类调用；分类调用不写任何记忆；单测用假模型覆盖命中、未命中、追问轮次命中、开关关闭四条路径
- [ ] 3.4 落实内层结果语义：`result` 顶层必须是对象（列表需包一层字段）、业务追问是 `ok=true` 的业务结果而非校验失败、缺字段按失败处理；单测覆盖“列表包一层”“追问 ≠ 失败”“缺字段 ≠ 追问”，并在 `BusinessContract` 的 Javadoc 写明契约声明方式（业务侧如何使用、如何新增一份契约）
- [ ] 3.5 新增 `EnvelopeSessionMemory`：自建 `ChatMemory`（`MessageWindowChatMemory` + 同一个 `ChatMemoryStore`，`id` = `memoryId`）提供“读历史快照 / 轮次结束写回用户原文与结果摘要”；单测断言写回内容、失败轮次写失败说明、会话历史中不出现校验错误原文；并核实 `EphemeralChatMemory` 读路径能否看到直写消息（不能则直接读 store 快照，把结论写进 Javadoc）

## 4. 入口接线（编排、非流式入口、既有流式入口接管）

- [ ] 4.1 新增 `EnvelopeRuntime` 编排件（分类 → 取契约 → 结构化/非结构化分支 → 收敛）与 `EnvelopeAssistant.chat(memoryId, message)` 非流式入口：结构化轮次用直连 `ChatLanguageModel` + 消息拼接（系统提示 + 历史快照 + `schema 指令 + 本轮文本`），失败经 `StructuredOutput` 的错误回喂重试且零记忆写入；单测覆盖首轮失败后重试成功、达上限失败、失败轮次信封为 `ok=false`
- [ ] 4.2 接管既有无 mapper 的 `streamToSse(...)` 重载：新增可选处理器字段与新增 setter，在无 mapper 重载内前置分支（处理器为空则原路径不变）；开关开启时 `start` 帧 `data` 为 `{requestId, business, tokens}` 且分类完成后才发，结构化轮次不发 `token` 帧、`done` 帧为信封，非结构化轮次逐字下发、`done` 帧为信封；单测断言开关关闭时原样输出、带 `doneDataMapper` 与带回调的重载及 `streamToSseWithFilter` / `generateTextToSse` 不被接管、`token` 帧始终纯文本、流式不重试
- [ ] 4.3 在既有 `LangChain4jSupportAutoConfiguration` 增加嵌套配置类：注册 `EnvelopeRuntime` 与一个 `SmartInitializingSingleton` 安装器（注入 `ObjectProvider<SimpleAssistant>` + `ObjectProvider<EnvelopeRuntime>`，两者都在才调用新增 setter，缺失则静默跳过）；不改任何既有 `@Bean` 方法签名、不改 `spring.factories` / `AutoConfiguration.imports`；用 `ApplicationContextRunner` 断言开关未开启时不安装处理器、开启时既有 `SimpleAssistant` Bean 被装上且既有自动配置测试与一致性测试仍全绿

## 5. 样例与集成验证

- [ ] 5.1 样例模块新增两个假业务契约与假模型（同一 `RecordingChatModel` 同时承担分类与生成），离线（零 API Key）覆盖四条路径：分类命中且结构化成功、分类命中但校验失败、业务追问、分类未命中落闲聊；断言结构化轮次不发 `token` 帧、`start` 帧含 `tokens`；`./fw-plus-core/mvnw -f fw-plus-sample/pom.xml -o -pl fw-plus-ai-samples/fw-plus-ai-structured-output-sample test -Dmaven.test.skip=false` 通过
- [ ] 5.2 断言同一轮次的两条通道同形：流式 `done` 帧的信封（经 `doneDataMapper`/`onDone` 侧信道取回）与非流式入口返回的信封在键结构、`ok`、`business`、`result`、`v` 上一致；不尝试解析真实 `SseEmitter` 帧（内部构造、无注入点），把该限制写进测试类注释
- [ ] 5.3 跑该模块全量测试确认既有 641 例未受影响（尤其开关关闭时的既有行为），并执行 `cd fw-plus-ai && ./mvnw -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure install -DskipTests` 使样例取到新版本后复跑样例

## Workflow follow-up

- 进入 Verify 阶段验证实现与产物一致，并记录验证报告。
- 归档本 change，把三份 delta spec 合入 `docs/openspec/specs/` 对应 capability。
- 按用户确认的提交拆分落库（结构化输出工具类与规则增强；信封、编排、接管与样例）。

```

## docs/openspec/changes/langchain4j-assistant-json-envelope/.openspec.yaml

- Source: docs/openspec/changes/langchain4j-assistant-json-envelope/.openspec.yaml
- Lines: 1-2
- SHA256: cd54fe01555559ffd5cf6f7f1d1b06209492e5ba9147cdc3800201c09e2291d0

```md
schema: spec-driven
created: 2026-10-14

```

## docs/openspec/changes/langchain4j-assistant-json-envelope/specs/langchain4j-assistant-business-routing/spec.md

- Source: docs/openspec/changes/langchain4j-assistant-json-envelope/specs/langchain4j-assistant-business-routing/spec.md
- Lines: 1-151
- SHA256: 9c96fffb9a3eb168082f17aa552f9a0d3ead5e84f715afdcd4584e8a56049bc1

[TRUNCATED]

```md
# Spec Delta

## Purpose

让一个通用对话入口自己判断当前该用哪份业务契约：入口只接收用户消息，本轮业务完全由模型分类决定，
使多业务共用同一入口而不互相污染。

## ADDED Requirements

### Requirement: 业务契约声明的内容

每份业务契约 SHALL 声明：业务名称（即信封中 `business` 的取值）、内层结果的 JSON Schema、
零到多条校验规则、以及本业务是否默认产出结构化结果。业务名称 SHALL 在注册表中唯一。

#### Scenario: 契约被完整声明

- **WHEN** 注册一份业务契约
- **THEN** 该业务名称、内层 schema、校验规则与结构化默认值都被系统掌握

#### Scenario: 业务名称重复

- **WHEN** 注册两个同名业务契约
- **THEN** 系统在注册阶段即失败并指出冲突的业务名称，不静默覆盖

### Requirement: 业务解析与兜底

系统 SHALL 按名称解析业务契约。名称未注册时 SHALL 回落到兜底业务（`business="chat"`）而不是报错，
使通用入口在任何输入下都能给出结果。

#### Scenario: 解析到已注册业务

- **WHEN** 本轮解析出的业务名称已注册
- **THEN** 使用该契约的内层 schema 与校验规则

#### Scenario: 解析到未注册业务

- **WHEN** 本轮解析出的业务名称未注册
- **THEN** 本轮按兜底 `chat` 处理，信封仍为合法信封

### Requirement: 业务只由模型分类决定

本轮 `business` SHALL 完全由模型分类决定。入口 SHALL 只接收用户消息与会话标识，
SHALL NOT 接受调用方指定业务的入参。系统 SHALL NOT 沿用上一轮的判定结果作为本轮业务。

#### Scenario: 入口不接受业务入参

- **WHEN** 调用方只给出消息与会话标识
- **THEN** 本轮业务由分类结果决定，不出现“未指定业务”导致的失败

#### Scenario: 每轮重新判定

- **WHEN** 同一会话的相邻两轮话题不同
- **THEN** 两轮各自按本轮分类结果选择业务，不沿用上一轮结果

#### Scenario: 话题切回后仍然正确

- **WHEN** 用户先聊业务 A、再聊业务 B、之后又聊回业务 A
- **THEN** 每轮都按本轮分类结果处理，无需任何“重置”操作

### Requirement: 对话历史共用

同一会话 SHALL 共用一份对话历史，SHALL NOT 因本轮业务不同而隔离或重建；跨业务切换 SHALL 保持对话连续。

#### Scenario: 跨业务切换保持连续

- **WHEN** 同一会话先与业务 A 对话、再与业务 B 对话
- **THEN** 业务 B 的轮次能看到该会话此前的消息

#### Scenario: 历史不因业务切换而清空

- **WHEN** 分类在一轮中给出与上一轮不同的业务
- **THEN** 该会话的历史消息保持不变

#### Scenario: 结构化轮次写回可追溯的内容

- **WHEN** 结构化轮次结束
- **THEN** 该会话历史新增本轮用户原文，以及一条结果摘要（失败轮次为失败说明）
- **AND** 该会话历史中不出现校验错误原文

### Requirement: 模型分类判定

```

Full source: docs/openspec/changes/langchain4j-assistant-json-envelope/specs/langchain4j-assistant-business-routing/spec.md

## docs/openspec/changes/langchain4j-assistant-json-envelope/specs/langchain4j-chat-envelope/spec.md

- Source: docs/openspec/changes/langchain4j-assistant-json-envelope/specs/langchain4j-chat-envelope/spec.md
- Lines: 1-181
- SHA256: 462c72c1108bbb9a677ef26ae8881508a64af799a8a08d65077c4aa06362c899

[TRUNCATED]

```md
# Spec Delta

## Purpose

定义统一聊天信封的形态与取值语义，使流式与非流式两条对话通道在成功和失败时都输出同一份可编程判断的
JSON 结果，让前端与调用方不必再解析自由文本。

## ADDED Requirements

### Requirement: 信封键位恒定

信封 SHALL 恰好包含 5 个键：`ok`、`business`、`v`、`result`、`text`。成功与失败 SHALL 同形，
任何情况下都不得因为失败而缺少键。

#### Scenario: 成功轮次的键

- **WHEN** 某个业务轮次成功产出结构化结果
- **THEN** 信封包含全部 5 个键

#### Scenario: 失败轮次的键

- **WHEN** 某个轮次因为校验失败、解析失败或模型空回复而失败
- **THEN** 信封仍包含全部 5 个键，与成功轮次的键完全一致

### Requirement: 成功与失败的取值语义

`ok=true` 时 `result` SHALL 为业务结构化结果；`ok=false` 时 `result` SHALL 为 `null`。
`text` SHALL 表示“无结构可用时的可展示文本”：闲聊轮次为模型原文，失败轮次为兜底话术，
结构化成功轮次 SHALL 为空串。外层 SHALL NOT 出现任何业务语义字段。

#### Scenario: 结构化成功

- **WHEN** 业务轮次成功产出结构化结果
- **THEN** `ok=true`、`result` 为业务对象、`text` 为空串

#### Scenario: 失败轮次

- **WHEN** 轮次以失败结束
- **THEN** `ok=false`、`result=null`、`text` 为兜底话术

#### Scenario: 纯闲聊轮次

- **WHEN** 轮次没有对应的业务契约（兜底 `chat`）
- **THEN** `ok=true`、`result=null`、`text` 为模型原文

#### Scenario: 业务语义字段不外泄

- **WHEN** 业务结果中包含追问标记、缺失字段列表、意图或订单号等业务语义
- **THEN** 这些字段只出现在 `result` 内部，信封外层不出现

### Requirement: 收敛过程永不抛异常

任何输入——合法 JSON、夹带解释文字的 JSON、被截断的 JSON、纯文本闲聊、空回复——SHALL 被收敛为合法信封，
收敛过程 SHALL NOT 抛出异常。空回复 SHALL 降级为 `ok=false`，不得被视为成功。

#### Scenario: JSON 被截断

- **WHEN** 模型输出到一半被截断，JSON 不完整
- **THEN** 得到 `ok=false` 的合法信封，调用方不收到解析异常

#### Scenario: 模型空回复

- **WHEN** 模型没有返回任何内容
- **THEN** 得到 `ok=false` 的合法信封，`result=null`

#### Scenario: 收敛件收到异常对象

- **WHEN** 上游把异常信息传给收敛件
- **THEN** 收敛件把异常信息降级为 `ok=false` 的信封，不把异常向外抛出

### Requirement: 帧协议保持不变

`token` 帧 SHALL 保持纯文本。承载信封的 SHALL 只有 `done` 帧的 `data`。`error` 帧的形态 SHALL 与现状一致，
不新增传输概念。`start` 帧在开关关闭时 SHALL 保持现状（`data` 为 requestId 字符串）；开关开启时 `start`
帧的 `data` SHALL 为对象，包含 `requestId`、本轮 `business` 与本轮是否逐字输出（`tokens`），且 SHALL 在
本轮业务判定完成之后发出。

#### Scenario: 结构化轮次的 token 帧

- **WHEN** 帧形态开关开启时处理结构化轮次

```

Full source: docs/openspec/changes/langchain4j-assistant-json-envelope/specs/langchain4j-chat-envelope/spec.md

## docs/openspec/changes/langchain4j-assistant-json-envelope/specs/langchain4j-structured-output/spec.md

- Source: docs/openspec/changes/langchain4j-assistant-json-envelope/specs/langchain4j-structured-output/spec.md
- Lines: 1-104
- SHA256: 766cc2cfa93aed48e72404eba6cf6db933d50356162d063c43587efd1c869e05

[TRUNCATED]

```md
# Spec Delta

## Purpose

让调用方以一份结构声明（JSON Schema 或 Java 类型）加若干校验规则作为契约，从大模型取得结构化结果；
输出不符合契约时自动把错误回喂给模型并重试，最终给出调用方可编程判断的成功或失败。

## ADDED Requirements

### Requirement: 结构声明同时驱动指令与本地校验

调用方 SHALL 能以 JSON Schema 文本或 Java 类型声明期望的输出结构，并可附加零到多条校验规则。
系统 SHALL 由同一份声明生成给模型的指令与本地校验依据，二者不得由两处配置分别给出。

#### Scenario: 用 JSON Schema 声明结构

- **WHEN** 调用方给出一份 JSON Schema
- **THEN** 发给模型的提示词包含该结构要求
- **AND** 模型返回后按同一份 schema 做本地校验

#### Scenario: 用 Java 类型声明结构

- **WHEN** 调用方给出一个 Java 类型而不是 schema 文本
- **THEN** 系统按该类型的属性生成等价的指令与本地校验依据

### Requirement: 宽容解析模型输出

模型输出夹带解释文字、Markdown 代码块标记或其他前后缀文本时，系统 SHALL 仍提取其中的 JSON 完成解析。
无法提取到可用 JSON 时 SHALL 判为失败，不得把解析异常抛给调用方。

#### Scenario: 输出含解释文字

- **WHEN** 模型返回“好的，结果如下：{...}”这类文本
- **THEN** 系统提取其中的 JSON 并正常解析

#### Scenario: 输出无法解析

- **WHEN** 模型返回的内容中不含可用 JSON
- **THEN** 该次尝试判为失败并进入重试或最终失败，调用方不收到解析异常

### Requirement: 双层校验与全量错误清单

系统 SHALL 先做 JSON Schema 结构校验，再执行调用方提供的校验规则。Schema 校验 SHALL 报告其发现的不符合项，
校验规则 SHALL 全部执行并汇总错误清单，不得因首条错误而提前停止。

#### Scenario: 结构不符合

- **WHEN** 模型返回的 JSON 缺少必填字段或字段类型不符
- **THEN** 该次尝试判为失败并产生结构类错误条目

#### Scenario: 多条规则同时失败

- **WHEN** 校验规则中有两条以上同时不成立
- **THEN** 错误清单包含全部这些条目的错误文本

### Requirement: 错误回喂重试

校验失败时系统 SHALL 把错误清单作为下一轮提示的一部分重新请求模型。最大尝试次数 SHALL 可配置，
未配置时 SHALL 为 3 次。重试 SHALL NOT 把错误原文写入业务会话的历史消息。

#### Scenario: 首轮失败后重试成功

- **WHEN** 首轮输出不符合契约，且模型在被提示错误后给出了符合契约的输出
- **THEN** 调用方得到成功结果，不感知中间失败轮次

#### Scenario: 达到最大尝试次数

- **WHEN** 连续 3 次尝试都不符合契约
- **THEN** 系统以失败结束，并给出可供排查的错误信息

#### Scenario: 重试不污染业务历史

- **WHEN** 同一业务会话内发生多轮重试
- **THEN** 该会话的历史消息中不出现校验错误原文

### Requirement: 支持嵌套路径的校验规则

内置校验规则 SHALL 覆盖必填、类型、枚举、数值范围与字符串模式五类，并 SHALL 接受点号路径（如 `a.b.c`）
定位嵌套字段；不通过时的错误文本 SHALL 包含该路径。


```

Full source: docs/openspec/changes/langchain4j-assistant-json-envelope/specs/langchain4j-structured-output/spec.md
