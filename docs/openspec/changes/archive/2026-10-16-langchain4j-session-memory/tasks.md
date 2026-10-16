# Tasks

> 详细设计与代码骨架见 `docs/superpowers/specs/2026-10-15-langchain4j-session-memory-design.md`。
> 单测命令：`cd fw-plus-ai && ./mvnw -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dmaven.test.skip=false`

## 1. 会话键与会话兜底

- [x] 1.1 `ChatMemoryIds` 新增 5 参 `build(...)` 重载（4 参版本与 `parseXxx` 一字不动）；验证：单测同时覆盖 5 段/4 段构造，并断言 `parseSegment` 对 5 段串解析出正确的前 4 段 <!-- comet-task:546696e1-26e1-4921-bce4-b4bbfdf2abe5 -->
- [x] 1.2 新增 `SessionIdResolver` 接口与 `daily` 默认实现（`ZoneId`/`DateTimeFormatter` 静态化，仅用 Java 8 API）；验证：单测覆盖按天格式（`dYYYYMMDD`）与“显式会话优先于兜底” <!-- comet-task:50af6d5e-6642-409e-9d62-c9b8d15aa8c3 -->
- [x] 1.3 `SimpleAssistant` 新增 `session(String)` 与内部 `effectiveMemoryId()`，`memoryId()` 保持不变；验证：单测断言 `memoryId()` 仍为 `t1:crm:bot:u1`，而 `effectiveMemoryId()` 为 5 段 <!-- comet-task:7027fe3f-30cd-4391-b1e6-b8787f6d7258 -->
- [x] 1.4 `Langchain4jAssistantProperties` 新增 `session.enabled` 与 `session.resolver` 配置项（默认值符合设计 D1/D2）；验证：属性绑定单测断言默认值与显式覆盖 <!-- comet-task:4c4b0a47-b2fa-4263-a149-3d66979e1d0b -->

## 2. 统一上下文抽象

- [x] 2.1 新增 `ContextProvider` 接口与 `ContextRequest`；验证：单测断言返回空内容的来源不产生任何上下文块 <!-- comet-task:50bf273b-5c99-43a5-9fcf-f945ea243eac -->
- [x] 2.2 新增 `ContextStrategy` 枚举（`MEMORY` / `ENRICHED` / `MEMORY_AND_ENRICHED`）与 `ContextInjector`；验证：单测覆盖三种组合下的来源集合 <!-- comet-task:93ad87e2-dbc8-47b5-bced-38b06f464d5f -->
- [x] 2.3 新增 `ContextAwareChatMemory` 装饰器（`add` 捕获本轮输入、`messages` 前置系统消息）；验证：单测断言注入的上下文位于最近消息之前，且最近 N 条原文仍按窗口保留 <!-- comet-task:0e4aaa66-d6eb-4447-81a3-09b492bfbbe8 -->
- [x] 2.4 来源失败隔离（单个 provider 异常按空来源处理并记日志）；验证：单测断言某个 provider 抛异常时本轮不中断、其余来源与窗口仍可用 <!-- comet-task:a4c76c18-555c-475f-8b3b-43763dcbda68 -->

## 3. 一级滚动会话摘要

- [x] 3.1 新增 `ai_chat_summary` 表结构与 `SummaryStore`（`session_key` 唯一，读写与 upsert）；验证：单测覆盖首次写入、更新与按会话读取 <!-- comet-task:431ab7ce-8250-4fe6-b57f-ce9d094b505f -->
- [x] 3.2 新增 `Summarizer` 接口与 `Summarizers.REGISTRY`（名字 → 内置、FQCN → 反射，照 `AiPersistenceStrategy.Registry` 形态）；验证：单测覆盖按名字命中内置、按 FQCN 反射构造、未知名报错 <!-- comet-task:6c699238-495b-461d-8036-f995793069e1 -->
- [x] 3.3 实现 5 个内置摘要策略（`refine` / `rebuild` / `map-reduce` / `segmented` / `structured`）；验证：每个策略各有一条单测，断言输入历史下产出非空摘要 <!-- comet-task:da0c6f10-7b7c-4ff3-9fe1-94248c662278 -->
- [x] 3.4 实现双阈值滚动逻辑（`trigger-tokens` 触发、`keep-tokens` 压缩、每 `rebuild-every` 次插 `rebuild`），token 计数复用 `MagicDynamicTokenizer`；验证：单测断言未达阈值不触发、超出后压缩至 `keep-tokens` 以内、达 `rebuild-every` 走一次全量重摘 <!-- comet-task:e3a4caf1-cc60-4339-acec-d81880a92947 -->
- [x] 3.5 新增 `SummaryContextProvider` 并接入注入链，含读时兜底与失败回退；验证：单测断言读路径可在写路径未及时更新时补齐摘要，且摘要生成失败时本轮退化为最近 N 条原文 <!-- comet-task:d62f8e50-de12-4f26-95ec-c50acc41e195 -->
- [x] 3.6 `Langchain4jAssistantProperties` 新增 `session.summary.*`（开关、策略、双阈值、`rebuild-every`、独立小模型等）；验证：属性绑定单测断言默认策略为 `refine`、`rebuild-every` 默认值生效 <!-- comet-task:5bfd7bc8-9038-4f20-a2f2-7aee7279fa9e -->

## 4. 装配与使用文档

- [x] 4.1 在 `AssistantBuilder` / 自动配置中装配 `ContextAwareChatMemory` 与 provider 链（摘要来源按 `ContextStrategy` 启用）；验证：装配层单测断言启用 `MEMORY_AND_ENRICHED` 时注入链包含摘要来源、`MEMORY` 时不含 <!-- comet-task:e18676fb-040a-4c39-8be4-32c3d96e4224 -->
- [x] 4.2 补充使用文档（会话键、`SessionIdResolver`、`ContextStrategy`、摘要配置与回滚方式），并同步设计文档中的配置说明；验证：按文档写出的配置样例可被属性绑定单测加载 <!-- comet-task:5df8576a-ee87-4737-8f50-f4e23381e165 -->

## 5. 集成验证

- [x] 5.1 集成验证：同一用户两个会话并行对话，断言彼此记忆互不可见；验证：集成测试断言跨会话读取不到对方消息 <!-- comet-task:b7622bc2-0db7-42dd-8cbf-44dacac63ab0 -->
- [x] 5.2 集成验证：长会话累计超出 `trigger-tokens` 后，断言上下文包含会话摘要且不超过 `keep-tokens`；验证：集成测试断言摘要段存在且原始消息未被删除 <!-- comet-task:d596bfc6-c0d2-48aa-b838-a3fb5a824190 -->
- [x] 5.3 回归验证：既有 `memoryId()` 4 段契约与 `ChatMemoryIds.parseSegment` 既有回退语义不变；验证：`./mvnw -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test` 全绿 <!-- comet-task:43bd72a9-6087-43c6-8c09-cc471372eb61 -->

## 6. 独立复审修复（4 个 blocker）

> 来源：fresh-context 复审（4 blocker + 9 non-blocker）。四项均需在 verify 阶段对照。

- [x] 6.1 对话链路改用会话键：`SimpleAssistant` 6 个入口（`retrieve` / `chat(String)` / `stream(String)` / `chat(List)` / `stream(List)` / `streamToSse`）改走 `effectiveMemoryId()`，否则摘要与注入按 4 段锚点串会话；验证：`SimpleAssistantDimensionsTest` 断言单参会话键为 5 段、且切换 `session(...)` 后键不同、未设维度时仍为 `default`
- [x] 6.2 配置接线：`Langchain4jModuleConfiguration.simpleAssistant` 经 `ObjectProvider` 读 `Langchain4jAssistantProperties` / `SummaryStore` / `SummaryModel` / `SessionIdResolver`，交 `SummaryMemoryAssembler.apply(...)` 落到 `AssistantBuilder`（缺属性类/来源静默跳过）；验证：`Langchain4jSessionMemoryWiringTest` 断言 `session.summary.*` 真正驱动摘要写入、`SummaryStore` 默认 Bean 存在且可被自定义 Bean 覆盖
- [x] 6.3 生产摘要模型与默认存储：新增 `ChatLanguageModelSummaryModel`（模型 key/FQCN 解析链，留空即抽取式回退、不调 LLM），`LangChain4jSupportAutoConfiguration` 注册 `@ConditionalOnMissingBean` 的 `InMemorySummaryStore` 默认 Bean；验证：`SummaryMemoryAssemblerTest` 覆盖「自定义 Bean 优先 / 配置解析 / 留空不装模型」
- [x] 6.4 写时维护与水位：`ContextAwareChatMemory.add(AiMessage)` 用 **写前快照** 触发维护（避免窗口驱逐丢历史）、读时兜底仅在本轮未写时补一次、维护异常记日志不中断、`clear()` 重置；`SummaryRecord` 水位改为身份锚点 `lastCovered`（`ChatMessageIdentities`）避免同批历史反复精炼；验证：`ContextAwareChatMemoryTest` 覆盖「写后读不重复维护 / 兜底只跑一次 / 维护抛异常不破对话」，`RollingSummaryEngineTest` 覆盖水位与 `rebuild-every`
- [x] 6.5 文档同步：`usage.md` 补「未设维度退化为 `default`」「摘要模型/存储解析链」「`last_covered` 水位」「维护时机与第二轮生效口径」；设计稿 §3.2 / §5 / §7 / §11 同步

## Workflow follow-up

- 归档前按项目评审要求完成 verify 阶段。
- 归档后由后续 change `langchain4j-fact-memory` 复用本次建立的上下文注入抽象。
