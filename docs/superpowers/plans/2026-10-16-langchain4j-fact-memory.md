---
change: langchain4j-fact-memory
design-doc: docs/superpowers/specs/2026-10-16-langchain4j-fact-memory-design.md
base-ref: 0d140ec1348ed25020c2722851ed30b5eecb3086
archived-with: 2026-10-16-langchain4j-fact-memory
---

# 实施计划：LangChain4j 跨会话事实记忆

<!-- comet-task-authority: docs/openspec/changes/langchain4j-fact-memory/tasks.md -->

技术方案、组件与类设计、mem0 契约、配置默认值与测试策略见
`docs/superpowers/specs/2026-10-16-langchain4j-fact-memory-design.md`（决策 D1–D13 为实施依据）；
行为契约见 `docs/openspec/changes/langchain4j-fact-memory/specs/langchain4j-fact-memory/spec.md`。
本计划只记录范围、依赖、约束与验收，不预写实现。

## 全局约定

- **执行策略**：autonomous + **tdd**（每任务先 RED 后 GREEN，记录真实命令与结果；RED 的失败原因必须是待实现行为）+ review_mode standard（风险任务安排独立审查；最终集成审查留给 Verify，不在 Build 重复）。
- **语言约束**：JDK 1.8（禁 `List.of` / `Map.copyOf` / `Optional.orElseThrow()` 等 Java 9+ API）；不新增第三方依赖；源文件 UTF-8；缩进用 Tab。
- **零签名改动**：既有方法签名一律不动（`ContextMaintenance` 只新增 3 参 `default` 方法；`Mem0RestClient` 只放宽访问修饰符至 public；`ContextAwareChatMemory` 私有方法参数扩展无兼容影响）。
- **模块与包**：全部落在 `fw-plus-ai-boot-autoconfigure`；新代码在
  `src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/support/memory/facts/`，
  测试镜像到 `src/test/java/.../support/memory/facts/`；装配改动在 `support/LangChain4jSupportAutoConfiguration.java`、
  `magicapi/modules/langchain4j/Langchain4jModuleConfiguration.java`；属性在 `support/envelope/Langchain4jAssistantProperties.java`。
- **命令**（层 POM 默认 `maven.test.skip=true`，必须显式打开）：
  - 定向 RED/GREEN：`cd fw-plus-ai && ./mvnw -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dmaven.test.skip=false -Dtest=<TestClass> -DfailIfNoTests=false`
  - 模块全量回归：`cd fw-plus-ai && ./mvnw -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dmaven.test.skip=false`
- **测试范式**：fake mem0 服务用 `com.sun.net.httpserver.HttpServer`（照 `Mem0ChatMemoryStoreTest`）；
  配置样例绑定照 `SessionMemoryConfigSampleTest`；集成照 `SessionMemoryIntegrationTest`。
- **契约校准**：mem0 OSS 无 `/v1` 前缀；`POST /search` 的字段名（top_k/limit）以 fake 服务契约测试固化，
  与部署实例实测不一致时以实测为准并回写设计文档。
- **既有基线**：模块现有测试必须保持全绿；`session.facts.enabled=false` 时行为与 change ① 逐字节一致（零注入、零写入、零外部调用）。

## 第 1 组：事实通道骨架

### 1.1 `FactStore` 接口与 `Mem0FactStore` <!-- comet-task-ref:a3ba4c0e-ab7c-4525-80b4-0835f5893848 -->

- **范围**：`FactStore`（`addDialogue` / `addFacts` / `search` / `listAll`）+ `Mem0FactStore`：抽取写入 `POST /memories`（`infer=true`，`user_id`=段[3]，**无** `run_id`）；直存逐条 `POST /memories`（`infer=false`）；检索 `POST /search`；列表 `GET /memories?user_id=`。
- **依赖**：无（可先行）。
- **约束**：复用 `Mem0RestClient`（仅放宽类与方法可见性，不改签名与行为）；认证 `X-API-Key`；超时默认 connect 5s / read 10s；消息为真实文本，禁止 `{"seq","msg"}` 包装串。
- **验收**：fake HttpServer 单测断言写入请求含 `infer=true` 且**不含** `run_id`；`search` 按 `user_id` 维度命中；直存逐条。

### 1.2 事实通道与消息通道物理隔离 <!-- comet-task-ref:88c7a697-5a37-493c-968e-73933788384a -->

- **范围**：同一 fake 服务上双通道并存；断言事实写入后按会话读取消息记忆不出现该事实、消息体解析（`{"seq","msg"}` 解包）不受影响。
- **依赖**：1.1。
- **约束**：略不影响消息通道既有契约（`infer=false` / `run_id`=memId / append-only / 4-5 段键）。
- **验收**：隔离单测全绿（事实写入 → `Mem0ChatMemoryStore.getMessages` 结果不变）。

### 1.3 `session.facts.*` 配置属性 <!-- comet-task-ref:31497d5c-eb79-4046-86f3-cc88d40c70f6 -->

- **范围**：`Langchain4jAssistantProperties.Session.Facts`：`enabled=false` / `base-url` / `api-key=""` / `connect-timeout=5s` / `read-timeout=10s` / `extractor=mem0` / `write-mode=per-turn` / `batch-size=5` / `retriever=semantic` / `top-k=5` / `max-facts-tokens=500` / `extract-async=true` / `extract-model=""` / `rerank-model=""` + `validate()`。
- **依赖**：无。
- **约束**：默认值以设计 §6 为准；非法值装配期 fail-fast；Javadoc 列全量键名与默认值。
- **验收**：属性绑定单测断言全部默认值与显式覆盖。

## 第 2 组：事实抽取与写入

### 2.1 `FactExtractor` 与 `FactExtractors.REGISTRY` <!-- comet-task-ref:f7a67e76-2e0b-4e59-ad88-24f79ef236a5 -->

- **范围**：`FactExtractor` 接口（`extract(userId, turnMessages)`）+ REGISTRY：名字 → 内置、FQCN → 反射（无参构造）、未知名报错；形态与 `Summarizers.REGISTRY` 一致。
- **依赖**：1.1（`FactStore`）。
- **约束**：Java 8 反射；内置由 `resolve(name, store, extractModel)` 现场构造。
- **验收**：按名字命中 / FQCN 反射构造 / 未知名报错 三条单测。

### 2.2 4 个抽取策略 <!-- comet-task-ref:eac14b4e-9e71-4130-882f-eee70849bf61 -->

- **范围**：`mem0`（委托 `addDialogue`）/ `llm`（模型 + 固定 prompt → JSON 数组解析 → `addFacts`）/ `rule`（偏好、姓名/称呼、联系方式、编号规则族）/ `hybrid`（规则候选 + 模型归并；模型缺失退化为 rule 行为）。
- **依赖**：2.1、1.1。
- **约束**：抽取输入为真实对话文本；`llm` 模型缺失或解析失败 → warn + 跳过；`rule` 无命中 → 静默；无可沉淀事实 → 零写入。
- **验收**：每策略一条单测；`rule` 零外部依赖独立跑通。

### 2.3 3 种写入模式 <!-- comet-task-ref:2f9cd1f8-4471-4f40-b586-3d89bf9bad26 -->

- **范围**：`FactWriteMode` + `FactTurnBuffer` + `FactMaintenance` 调度：`per-turn` 每轮抽；`batched` 达 `batch-size` 阈值 drain + 会话切换 drain；`session-end` 仅会话切换懒判定（同一用户出现新会话标识）时集中抽取。
- **依赖**：2.1、2.2、1.3。
- **约束**：轮末仅最终 AiMessage 触发（跳过 `hasToolExecutionRequests()` 中间态）；读时兜底（`turnMessage=null`）不触发；每会话缓冲并发安全（append 在请求线程、drain 可在异步线程）。
- **验收**：三模式触发时机单测（逐轮每次 / 批量达阈值 / 仅会话结束）+ 工具中间态不触发用例。

### 2.4 抽取与写入失败隔离 <!-- comet-task-ref:dd093cdf-263d-46f7-9506-747f6ad7b8d5 -->

- **范围**：抽取/写入异常仅日志、不中断本轮；异步队列（容量 128）拒绝时 warn + 丢弃；同步模式内联 try/catch；失败不产生用户可见影响。
- **依赖**：2.3。
- **约束**：兜底含 `Throwable`；`ContextAwareChatMemory.add` 流程不因事实失败而抛出。
- **验收**：抽取服务抛异常单测断言本轮对话不中断且仅记录失败。

## 第 3 组：事实召回与注入

### 3.1 `FactRetriever` 与 `FactRetrievers.REGISTRY` <!-- comet-task-ref:c603bb3e-2377-43fb-beed-c80a678409f7 -->

- **范围**：`FactRetriever` 接口（`retrieve(query, userId, topK)`）+ REGISTRY（同 2.1 形态）。
- **依赖**：1.1。
- **约束**：内置由 `resolve(name, store, rerankModel)` 现场构造。
- **验收**：注册与查找单测。

### 3.2 4 个召回策略 <!-- comet-task-ref:491e3f5d-eae5-4dca-b4d7-193449eb792b -->

- **范围**：`semantic`（`store.search`）/ `keyword`（`listAll(userId,200)` + 本地评分：ASCII 词 + CJK 二元组、覆盖率排序）/ `hybrid`（各取 2K → RRF k=60 融合、按文本去重）/ `rerank`（semantic 4K → `ScoringModel` 评分降序；未配模型 → 本地关键词重叠重排保底）。
- **依赖**：3.1、1.1。
- **约束**：遵守 `top-k`；实现内部尽量不抛（异常由 Provider 统一隔离）。
- **验收**：每策略一条单测断言相关性与 `top-k` 上限。

### 3.3 `FactContextProvider` 接入注入链 <!-- comet-task-ref:fdc9d35e-69a6-4168-a4da-c5f4287bdefa -->

- **范围**：查询=本轮输入；守卫（无显式用户段 / 空输入 → 空串）；渲染「【用户长期记忆】+ 条目」；`max-facts-tokens=500`（4 字符/token）软上限截断；装配顺序在会话摘要之前。
- **依赖**：3.2、3.1。
- **约束**：不改动既有 `ContextProvider` / `ContextInjector` 语义与消息窗口。
- **验收**：顺序单测断言 人设 → 事实 → 摘要 → 最近 N 条原文；预算截断与空命中用例。

### 3.4 召回无命中与失败隔离 <!-- comet-task-ref:b0ca761c-1da8-4a23-a06b-ebb49cb203a5 -->

- **范围**：无命中 → 事实段为空串；检索异常/超时 → warn + 空串；其余来源与窗口仍可用。
- **依赖**：3.3。
- **约束**：隔离边界在 Provider（含 `Throwable`）。
- **验收**：无命中与失败两条隔离单测。

## 第 4 组：装配与文档

### 4.1 装配层按 `facts.enabled` 注册 <!-- comet-task-ref:40ca75fb-5ad4-4793-b97c-7b82b54b9705 -->

- **范围**：`FactMemoryAssembler.apply(...)` + `Langchain4jModuleConfiguration#applySessionMemory`（**先事实、后摘要**）+ `LangChain4jSupportAutoConfiguration.FactMemoryConfiguration`（默认 `Mem0FactStore` Bean：`session.facts.enabled=true` + `@ConditionalOnMissingBean`；base-url 缺失 → 启动快速失败）+ 模型解析（`extract-model`/`rerank-model`：模型 key → `MagicDynamic*`；FQCN → 反射）。
- **依赖**：1–3 组全部。
- **约束**：`enabled=false` 零注册、零行为变化；`enabled=true` 但无 Bean → warn + 跳过；顺序固定。
- **验收**：装配单测断言关闭时不注入、开启时注入且位于摘要之前。

### 4.2 使用文档与配置样例测试 <!-- comet-task-ref:2e023087-fca5-4c65-a3cd-84209df62d9f -->

- **范围**：`docs/openspec/changes/langchain4j-fact-memory/usage.md`：事实通道前提（独立 mem0 服务端 + LLM；无 LLM 用 `rule`）、配置样例、三种策略族说明与默认值、`X-API-Key` 认证、作用域（仅显式用户段）、回滚方式；`FactMemoryConfigSampleTest` 按文档样例做属性绑定断言。
- **依赖**：1.3、4.1。
- **约束**：文档样例与测试共享（漂移即红）；zh-CN。
- **验收**：样例绑定单测通过；文档含完整配置键与默认值表。

## 第 5 组：集成与回归

### 5.1 跨会话召回集成 <!-- comet-task-ref:d4328723-6d1c-44d3-b50f-5a6e87706293 -->

- **范围**：fake 模型 + fake mem0：会话 s1 沉淀事实 → 会话 s2 的注入上下文出现该事实。
- **依赖**：4.1。
- **约束**：经 `ContextInjector` 端到端断言（非仅策略单测）。
- **验收**：集成测试断言 s2 召回命中。

### 5.2 事实不污染消息记忆（集成） <!-- comet-task-ref:38637fa9-2928-40ca-9c69-d67e05bc9f5f -->

- **范围**：事实沉淀后按会话读取 `getMessages` 仍只含消息、解析正常。
- **依赖**：4.1、1.2。
- **约束**：双通道同 fake 服务。
- **验收**：集成断言全绿。

### 5.3 回归：关闭/依赖不可用不影响会话记忆与摘要 <!-- comet-task-ref:2e99e2c0-5897-45cb-b140-08241c31ca01 -->

- **范围**：facts 关闭、mem0-B 不可用两种状态下，第 1 项会话记忆与摘要行为不变。
- **依赖**：全部。
- **约束**：不改变既有测试与行为。
- **验收**：`cd fw-plus-ai && ./mvnw -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dmaven.test.skip=false` 全绿（模块全量）。

## 收尾

- 全部任务勾选后录制 build 证据并运行 `comet guard langchain4j-fact-memory build --apply`；
  guard 通过后提交代码，进入 Verify（最终集成审查）；归档后合并回 `dev`。
