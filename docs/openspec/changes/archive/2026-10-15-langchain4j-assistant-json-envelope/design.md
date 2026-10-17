---
comet_change: langchain4j-assistant-json-envelope
role: technical-design
canonical_spec: openspec
archived-with: 2026-10-15-langchain4j-assistant-json-envelope
status: final
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

## Decisions

### D1. 包结构与职责边界

`support/envelope/` 与既有 `support/structured/` 平级：`structured/` 负责"拿到并校验结构化结果"，
`envelope/` 负责"把结果收敛成对外的信封"。两侧都不感知对方之外的业务语义。

替代方案：在 `AssistantBuilder` 内按条件分支拼装信封。否决——会把对外协议散进装配代码，难以单测。

### D2. 信封收敛件是唯一出口

所有路径（流式 `done` 帧、非流式返回）都经同一个 `assemble(...)` 产出信封；
任何输入都收敛为合法信封，永不抛。这一条是"成功失败同形"不变量唯一的落地点。

替代方案：各入口各自拼 JSON。否决——不变量会在多处漂移。

### D3. 键名与 `text` 语义

信封固定 5 键：`ok` / `business` / `v` / `result` / `text`。内层用 `result` 而不是 `data`，
避免出现 `data.data`。`text` 语义收敛为"无结构可用时的可展示文本"：闲聊轮次 = 模型原文，
失败轮次 = 兜底话术，结构化成功轮次 = 空串（人话由业务放进 `result`）。

`v` 是**字符串**，取值来自业务契约声明的版本（默认 `"1"`），兜底 `chat` 轮次取全局默认 `"1"`；
它只表达契约版本，不表达框架版本。

替代方案 A：框架从原文中剥离"人话 + 末尾 JSON"，把 JSON 放 `result`、人话放 `reply`（6 键）。
否决——需要切文本、提示词更长、解析更绕，且切分边界不稳定。
替代方案 B：`text` 恒为模型原文。否决——结构化轮次会把 JSON 原文重复暴露一次。

### D4. 结构化输出路线

提示词携带 schema 指令 + 宽容解析 + 应用侧双层校验 + 错误回喂重试。

替代方案：provider 原生 JSON Schema。0.35 侧不具备该能力，且会把本层绑死到具体厂商。
替代方案：function-calling 强约束。跨 provider 行为不一致，改造面更大。

### D5. 校验器选型

结构校验复用 `com.alibaba.fastjson2.schema.JSONSchema`（fastjson2 2.0.53 自带、Java 8、零新依赖），
业务语义与全量错误清单由自研 `Rule` 承担。

已知限制及对策：schema 校验只报第一个错 → 全量清单由 `Rule` 侧补齐；
`anyOf` / `oneOf` / `allOf` 不支持 → 这类约束不进 schema，改由 `Rule` 表达；
`of(Type).toString()` 不含 `required` → 规则派生必须显式读取 schema 的 `required` 与 `properties`，
不能依赖字符串化结果。

### D6. 路由：分类唯一决定

本轮 `business` 完全由模型分类决定，每轮判定一次，不沿用上一轮结果；入口只接收 `message` 与
`memoryId`，不接受调用方指定业务（也不提供每业务一个专用入口）。分类以已注册业务名作为枚举，
判定失败或名称未注册时落兜底 `chat`。分类开关默认**开启**：关闭即让所有轮次落 `chat`，
作为应急降级手段。

替代方案 A：显式传入 / 会话绑定参与路由。已否决——会把"该用哪个业务"的判断推回调用方，
与"一个通用入口"的目标相悖；将来若确实需要确定性入口，另开 change。
替代方案 B：新增"会话-业务"映射表并沿用判定。否决——路由会退化成粘性状态，用户换话题时不再自动切换，
且引入存储、过期与清理负担。

### D7. 两个开关分离

- 帧形态开关：只能全局配置，默认 `false`（关闭时 `done` 帧仍是原文文本，既有行为一字不变）；
  开启后会话内帧形态恒定。
- 结构化开关：全局默认（`false`）→ 业务契约 → 单次调用，覆盖顺序 单次 > 业务 > 全局。

替代方案：只用一个开关。否决——无法在保持信封帧形态的同时让某一轮不要 `result`。

### D8. 对话历史共用，重试零写入

同一会话共用一份对话历史（`memoryId` 不变），跨业务切换保持连续。结构化轮次**不经过 AiServices**，
因此重试轮次天然不写任何记忆：错误只经提示词回喂，不存在"隔离 memoryId + 用完删除"的清理负担与泄漏风险
（比 Open 阶段记录的方案更简单，保证不变）。轮次结束后主动写回"本轮用户原文 + 一条结果摘要
（失败轮次为失败说明）"，使后续轮次能看到本轮结果。

替代方案：按业务命名空间隔离历史（`order:session-123`）。已否决——路由只由分类决定且每轮判定，
同一会话必然跨业务切换，按业务隔离会让用户换话题时"失忆"。
替代方案：重试复用业务 `memoryId`。已否决——`EphemeralChatMemory` +
`PERSIST_NON_TOOL_FINAL_ANSWER` 会把错误原文沉淀进会话历史，污染后续轮次。

### D9. 接线方式

仿 `AssistantBuilder` 提供纯 Java 入口（脚本 `new` 即用），不默认注册 Spring Bean；
确需 Bean 时只在 `Langchain4jModuleConfiguration` 增加一个 `@Bean` 方法，不改既有 Bean。

替代方案：把信封逻辑做成新的自动配置类。否决——需要同时改两个注册文件并同步一致性测试，收益不足。

### D10. 规则从 schema 派生

`Rule.fromSchema(...)` 递归遍历 schema 的 `properties` / `required` / 类型 / 枚举 / 数值范围 /
字符串模式，为嵌套字段生成点号路径规则，并复用与内置规则相同的错误文本格式（含路径），
使错误可直接回喂给模型。

### D11. 两条通道同源，入口形态不对称

非流式与流式共用 `assemble(...)` 与同一个编排件。结构化业务走非流式生成（可重试），
闲聊与长文本走流式（出字快）；结构化轮次不发 `token` 帧，用户在 `done` 帧一次拿到结果。

入口形态不对称是既有签名的硬约束：既有 `chat(...)` 返回 `String`，无法承载信封，
因此**非流式信封必须是新入口**（新类 `EnvelopeAssistant.chat(memoryId, message)`），
而**流式信封可以复用既有无 mapper 的流式入口**（`done` 帧的 `data` 是 `Object`，本就承载任意对象）。
不为了让两者形态对称去改既有签名。

### D12. 帧形态开关的作用面

开关开启后，接管范围限定为**既有的、调用方未自带 `done` 数据映射与完成回调的流式对话入口**，
白名单三个重载：`streamToSse(String message)`（内部委托第二项，因此同样被接管）、
`streamToSse(memoryId, message)`、`(memoryId, message, startData)`。

不接管（保持现状，白名单之外一律不动）：带 `doneDataMapper` 的重载、带 `onStarted`/`onDone` 的重载（调用方已显式声明
"由我决定 done 内容"）、`streamToSse(memoryId, context, message)`（`context` 是调用方显式给出的系统提示定制，
接管后会被契约/全局系统提示取代，等于静默丢弃调用方语义）、
`streamToSseWithFilter(...)`（检索过滤语义不同，先不与之纠缠）、
`generateTextToSse(...)`（显式直连补全）、既有非流式 `chat(...)`（返回 `String`，见 D11）。

> 白名单与排除项在 Verify 集成审查（W-1）后由一句话措辞改为逐项列名：原措辞"未自带 mapping/回调的流式对话入口"
> 按字面会把自带 `context` 的重载也算进去，与实现不一致。第 2 次针对性复核又发现该次列名**漏了单参重载**
> `streamToSse(String)`——它经委托同样被接管（`SimpleAssistantEnvelopeTest#switchOnTakesOverSingleArgOverload`
> 已断言、`SimpleAssistant` Javadoc 也已按接管描述），现补全（tasks 7.1）。行为始终未变，只是把已实现边界写明。

实现方式：`SimpleAssistant` 增加一个**可选的信封处理器字段**（`null` = 未启用）与一个新增的 setter，
在无 mapper 的 `streamToSse` 重载内部前置分支：处理器存在 → 交编排件；否则原样走 `doStreamToSse(...)`。
既有签名与既有代码路径都不变，开关关闭时行为与今天逐字节一致。

替代方案：只让新入口产信封、既有入口永不接管。否决（用户决策）——`streamToSse(...)` 的 `done`
帧本就是 `Object`，接管它才是"一个入口"；且新增一个"信封版流式入口"会让既有脚本必须迁移。

### D13. `start` 帧携带轮次形态

开关开启时 `start` 帧的 `data` 为对象 `{requestId, business, tokens}`，且**在业务判定完成之后**发出，
使前端立刻知道该轮"等结果卡片"还是"等逐字输出"。关闭时 `start` 帧保持现状（`data` 为 requestId 字符串）。

代价与处置：首帧因此延后一次轻量分类调用（每轮必然发生，分类与生成本就串行，不额外增加总轮次耗时）；
`start` 帧 `data` 形态变化属 opt-in 影响面，靠"默认关 + 会话内恒定"兜住，并在文档写明前端解析建议
（`data` 可能是字符串或对象，按类型分支）。

替代方案：`start` 帧保持 requestId 字符串、`business` 只在 `done` 帧出现。已否决（用户决策）——
前端无法预知本轮形态，只能干等 `done`。

### D14. 结构化轮次的记忆读写

envelope 层自建一个 `ChatMemory`（`MessageWindowChatMemory` + 同一个 `ChatMemoryStore`，`id` 即 `memoryId`），
与既有 `chat(...)` 走 EphemeralChatMemory 的历史**落在同一个 store**，因此天然共用一份历史。

- **读**：取 `messages()` 作为历史快照，拼进结构化轮次与分类调用的消息序列。
- **写**：轮次结束后写回"本轮用户原文 + 一条结果摘要（失败为失败说明）"。
- **重试**：不写任何记忆（见 D8）。

结构化轮次的消息序列：`[SystemMessage(契约 systemHint 或全局默认)] + 历史快照(最近 K 条) +
[UserMessage(schema 指令 + 本轮原文)]`，经 `SimpleAssistant.generate(List<ChatMessage>)` 直连发送。
重试时替换最后一条用户消息为 `schema 指令 + 错误清单 + 本轮原文`。

明确取舍：该轮不使用 AiServices 提供的 tools / RAG / `systemMessageProvider`；系统提示由契约提供。
这样换来的是一致性——重试与历史写入完全由本层掌握，不依赖 `EphemeralChatMemory` 的隐式持久化策略。

需要实现期核实的一处：`EphemeralChatMemory` 读取路径能否看到"直写 store"的消息。若不能，改为直接读
store 快照（读路径不依赖 EphemeralChatMemory 的视图）。

替代方案：结构化轮次全程走 AiServices（读写交给框架）。已否决——重试与业务历史同 `memoryId`，
直接违反 spec「重试不污染业务历史」。

### D15. 分类器实现

新增 `BusinessClassifier`，基于既有 `StructuredOutput`：schema 为 `{"business": <已注册业务名枚举>}`，
判定失败/未命中/开关关 → `chat`。**不复用** `LlmIntentClassifier`——后者泛型绑定 Java 枚举、
自带正则解析与置信度阈值，而业务名是运行时注册的字符串，抽象不匹配；强行复用会把动态业务塞进枚举接口。
两者共用 `StructuredOutput` 作内核，因此没有"第二套结构化输出实现"。

模型调用解耦：分类与结构化生成都通过 `StructuredOutput.ChatFunction`/`SimpleAssistant.generate(...)`
注入，便于用假模型离线测试。

### D16. 分类的输入、尝试次数与降级

- 输入 = 分类系统提示（已注册业务名清单）+ 最近 `N` 条**用户**消息（默认 3，可配，`0` = 只看本轮）+
  本轮文本。追问轮次（不含业务信号）因此仍能命中业务。
- 每轮一次判定，**默认不重试**（`classifier.max-attempts` 默认 1）：分类失败即落 `chat`，
  不让一次抖动升级成额外成本；需要更稳可调大。
- 分类开关默认开启，关闭 → 所有轮次落 `chat` 且不产生分类调用。
- 分类调用**不写任何记忆**（直连 `ChatLanguageModel`），分类提示与业务历史互不污染。

### D17. 配置与装配

配置前缀 `fw.plus.ai.langchain4j.assistant`（与 support 层顶层开关 `fw.plus.ai.langchain4j.enabled` 同族，
语义属于 support 层而非 magicapi 层）。属性类 `Langchain4jAssistantProperties`（嵌套 `envelope` /
`structured` / `classifier` 分组）通过既有 `LangChain4jSupportAutoConfiguration` 的
`@EnableConfigurationProperties` 注册，`EnvelopeRuntime` 与其安装器等 `@Bean` 放在该自动配置的嵌套配置类里
——**不改任何注册文件**（只有新增自动配置类才需要）。

把 runtime 装到既有 `SimpleAssistant` Bean 上用一个 `SmartInitializingSingleton` 安装器：
它注入 `ObjectProvider<SimpleAssistant>` 与 `ObjectProvider<EnvelopeRuntime>`，两者都在才调用新增 setter，
找不到就静默跳过。这样既保证 Spring 场景"开关一开、既有入口即生效"，也避免修改既有 `@Bean` 方法签名
（例如给 `simpleAssistant(...)` 加参数）。

替代方案：改 `simpleAssistant(...)` 的签名多注入一个参数。否决——违反"零签名改动"。
替代方案：新增自动配置类。否决——要改两个注册文件并同步一致性测试。

### D18. `business` 与结构化开关正交

`business` 只由分类决定，`result` 只由结构化开关（单次 > 业务契约 > 全局默认 `false`）决定。
因此存在"业务命中但结构化关"的合法组合：该轮 `business` 为契约名、`result=null`、
`text` 为逐字下发的正文。业务契约的 `structured` 默认值是"该业务默认产出结构化结果"，
不是"该业务是否被启用"。

### 数据流

```
调用方 ──(message, memoryId)──► 既有无 mapper 流式入口 / 新增非流式信封入口
                                   │
                     分类：已注册业务名清单 + 最近 N 条用户消息 + 本轮文本
                     （每轮一次；未命中/失败/开关关 → chat；不写记忆）
                                   │
                       start 帧 data = {requestId, business, tokens}
                                   │
        ┌──────────────────────────┴──────────────────────────┐
  结构化轮次（非流式生成）                             非结构化轮次（流式生成）
  tokens=false；契约 schema + 规则                      tokens=true；既有 AiServices 流式（有记忆）
  读会话历史快照 → 直连模型 → 宽容解析 → 双层校验         token 帧逐字下发
      失败 ─► 错误回喂重试（零记忆写入，最多 N 次）              │
  结束写回「用户原文 + 结果摘要」                              │
        └──────────────────────────┬──────────────────────────┘
                                   ▼
                     assemble(...) 收敛成 5 键信封（永不抛）
                                   │
                    done 帧 data / 非流式返回值
```

### 组件与文件清单

新增（`fw-plus-ai-boot-autoconfigure` 的 `.../langchain4j/support/envelope/`）：

| 文件 | 职责 |
| --- | --- |
| `ChatEnvelope` | 5 键值对象 + 静态工厂（结构化成功 / 闲聊 / 失败）+ 转 `JSONObject` |
| `EnvelopeAssembler` | `assemble(...)`、唯一闸门 `ensure()`：六类输入收敛，永不抛 |
| `BusinessContract` | 名称、内层 schema、规则、结构化默认值、最大尝试次数、系统提示、契约版本 `v` |
| `BusinessRegistry` | 注册（重名 fail-fast）与解析（未注册回落 `chat`） |
| `BusinessClassifier` | 业务名枚举判定（`StructuredOutput` 内核，输入含最近用户消息，失败落 `chat`） |
| `Langchain4jAssistantProperties` | `@ConfigurationProperties("fw.plus.ai.langchain4j.assistant")` |
| `EnvelopeSessionMemory` | 会话记忆读写口（同一 `ChatMemoryStore`；读快照、写回结果摘要） |
| `EnvelopeRuntime` | 编排：分类 → 契约 → 结构化/非结构化 → 收敛；供流式与非流式复用 |
| `EnvelopeAssistant` | 非流式信封入口：`ChatEnvelope chat(String memoryId, String message)`；单次调用层覆盖走重载 `chat(String, String, Boolean)` |

对既有类的**新增式**改动（不动任何既有签名）：

| 位置 | 改动 |
| --- | --- |
| `SimpleAssistant` | 新增可选信封处理器字段（`volatile`）+ 新增链式 setter；无 mapper 的 `streamToSse` 重载内加一个前置分支 |
| `LangChain4jSupportAutoConfiguration` | 新增 `@EnableConfigurationProperties(Langchain4jAssistantProperties.class)`、嵌套配置类与 `EnvelopeRuntimeInstaller`（`SmartInitializingSingleton`，启动期调用 `SimpleAssistant#envelopeRuntime(...)` 完成装配） |
| `Rule` / `Rules` | 点号路径与 `fromSchema`（第 1 组任务） |

> 注（审查 W-4 同步）：装配**未经过 `AssistantBuilder`**（原计划里的那一行已删除）。纯 Java 场景由调用方
> 自行链式调用 `SimpleAssistant#envelopeRuntime(...)`；Spring 场景由上面的 `EnvelopeRuntimeInstaller` 在启动期完成。

### 配置项（键名以实现为准）

| 配置 | 层级 | 默认 | 说明 |
| --- | --- | --- | --- |
| 帧形态开关 | 全局 | `false` | 开启后接管既有无 mapper 流式入口；会话内恒定 |
| 结构化开关 | 全局 / 业务 / 单次 | `false` | 覆盖顺序：单次 > 业务 > 全局；单次层经 `EnvelopeAssistant.chat(memoryId, message, Boolean)` 提供（流式入口签名不可加参） |
| 分类开关 | 全局 | `true` | 每轮以已注册业务名做一次结构化判定；关闭则全部轮次落 `chat` |
| 分类历史条数 | 全局 | `3` | 分类输入附带的最近用户消息条数（`0` = 只看本轮） |
| 分类尝试次数 | 全局 | `1` | 分类判定尝试次数 |
| 最大尝试次数 | 全局 / 业务 / 单次 | `3` | 结构化重试上限 |
| 会话窗口条数 | 全局 | `20` | 读快照/写回所用的窗口大小 |
| 业务契约 | 配置 + 代码注册 | 空 | 名称、内层 schema、规则、结构化默认值、最大尝试次数、系统提示、版本 |

## 测试策略

**分层**：

1. **纯单测（无模型）**：
   - `EnvelopeAssembler`：六类输入（合法 JSON、夹带解释文字的 JSON、截断 JSON、纯文本、空回复/`[DONE]`、
     异常对象）都收敛且永不抛；四类路径（结构化成功 / 失败 / 闲聊 / 空回复）的键恒定与取值语义。
   - `Rule` / `Rules`：点号路径错误文本、`fromSchema` 的嵌套必填/枚举/范围/模式，以及派生规则与手写规则并用。
   - `BusinessRegistry`：重名 fail-fast、未注册回落 `chat`。
   - `BusinessClassifier`：命中、未命中、开关关闭三条路径（用 `StructuredOutput.ChatFunction` 注入假回复）。
2. **假模型集成（零 Key）**：`RecordingChatModel implements ChatLanguageModel` 按调用序号回放脚本
   （第 1 次 = 分类 JSON，第 2 次 = 结果 JSON，重试再追加），假 `StreamingChatLanguageModel` 实现
   `generate(List<ChatMessage>, StreamingResponseHandler<AiMessage>)` 发若干 token 后 `onComplete`。
   覆盖：开关关闭时既有 `streamToSse` 交互与输出不变、开关开启时接管并给出信封、结构化轮次不发 token 帧、
   流式不重试、分类命中/未命中、业务命中但结构化关、重试后历史不含错误原文。
3. **通道同形**：同一轮次分别取流式 `done` 帧（**反射读真实 SSE 帧**，见下）与非流式入口返回，
   逐键逐值比较键结构 / `ok` / `business` / `result` / `v`。
4. **样例端到端**：两假业务离线覆盖四条路径（分类命中成功后结构化、命中但校验失败、业务追问、未命中落闲聊）。
5. **回归**：模块既有 641 例全绿；开关关闭时既有行为断言不变。

**测试硬限制（已修订）**：`SseEmitter` 在 `streamTokenStreamToSse` 内部 `new SseEmitter(0L)`，无公开注入点。
初版据此写下"无法从外部解析真实帧、只用三处侧信道断言"的约定——**该结论已被 Build 阶段独立审查（M1）证伪并作废**：
反射读取 `ResponseBodyEmitter.earlySendAttempts`（私有 `LinkedHashSet`，有序）即可拿到真实帧，每组事件 3 个元素
（前缀 String + 数据本体 + `"\n\n"`）。样例侧 `EnvelopeChannelSameShapeTest` 因此改为对真实帧逐键逐值断言
（强于初版计划；本段落与计划约定、`tasks.md` 5.2 措辞由 Verify 修复轮一并同步）。三处侧信道断言仍保留在
模块侧 `SimpleAssistantEnvelopeTest`（它作帧来源与交互断言，不解析帧）。

## Risks / Trade-offs

- [`start` 帧 `data` 形态变化] → 仅开关开启时生效（opt-in）；默认关 + 会话内恒定；文档写明前端按类型分支解析。
- [每轮多一次分类调用] → 提示词极短、以已注册业务名做枚举、默认不重试；`classifier.history-user-messages`
  可调到 0 进一步降本；不引入判定结果缓存（缓存会破坏"每轮重新判定"）。
- [结构化轮次读不到手动直写 store 的消息] → 实现期核实 `EphemeralChatMemory` 读路径；不能则直接读 store 快照。
- [结构化轮次无 tools/RAG/系统提示提供者] → 明确取舍；系统提示由业务契约提供，schema 指令拼进用户消息。
- [纯 Java（非 Spring）接入时历史分叉] → 非流式入口必须与 `AssistantBuilder` 共用同一个 `ChatMemoryStore`；
  文档写明，Spring 场景由容器注入同一 store。
- [重试污染记忆] → 结构化轮次不经 AiServices、重试零写入（见 D8/D14）。
- [空回复被误判为成功] → 收敛件把空文本 / `[DONE]` 降级为 `ok=false`。
- [JSON 被截断] → 宽容解析；非流式可重试；流式闲聊直接降级为 `ok=false`。
- [schema 能力不足（只报首错、无 `anyOf`）] → 全量清单交给 `Rule`，复杂约束不进 schema。
- [分类误判] → 未命中或判定失败一律落 `chat`（闲聊不报错），并保留分类开关作为整体降级手段。
- [帧形态开关误开导致调用方不兼容] → 默认关 + 会话内恒定 + 启动期非法配置 fail-fast + 接管范围白名单（D12）。
- [业务契约演进破坏旧 `result`] → 用信封的 `v` 承载业务自定的契约版本，前端可据此降级。
- [新增文件量较大] → 先做不依赖任何待定项的部分（规则派生、收敛件），再接编排、接线与样例。

## Migration Plan

1. 落地顺序：① `Rule` 点号路径 + `fromSchema` → ② 信封收敛件与配置类 → ③ 契约/注册表/分类器/记忆读写
   → ④ 编排件 + 非流式入口 + 既有流式入口接管（含安装器） → ⑤ 样例两假业务端到端。
   第 ①② 步不依赖任何待定项。
2. 部署：帧形态开关默认关，先合入再按需开启；行为变化只在开启后出现；分类与接管范围随帧形态开关一起生效。
3. 回滚：关闭帧形态开关即回到现状，无需回滚代码；应急时也可只关分类开关（全部落 `chat`）。
4. 提交：既有未提交产出（结构化输出工具类与其样例、`fw-plus-ai-samples/pom.xml` 注册行）随本 change 一并交付。
5. 验证：模块测试全绿（含既有 641 例不受影响）+ 样例离线跑通四条路径 + 通道同形断言。

## Open Questions

- 业务契约的 `v` 何时递增、由谁维护：不改变 specs 与任务拆分，可按业务上线节奏后定。
- 分类提示词的具体文案与 `descriptions`（业务语义说明）是否需要：可按实测成功率后定。
- 结构化轮次的系统提示默认文案：Build 阶段敲定，不影响契约与接口。
- 是否把既有 `LlmIntentClassifier` 收敛为复用 `StructuredOutput`：属独立改动，本 change 不做。
