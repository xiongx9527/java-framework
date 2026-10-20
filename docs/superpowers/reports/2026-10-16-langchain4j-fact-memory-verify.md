# 验证报告：langchain4j-fact-memory

- 日期：2026-10-16
- 变更：`langchain4j-fact-memory`（LangChain4j 跨会话事实记忆）
- 验证模式：**full**（自动规模评估：16 任务 / 1 delta spec / 54 变更文件）
- 实现提交：`91567fae3`（基线 `0d140ec13`）
- 语言：zh-CN

## 1. 检查项总览（full 模式 7 项）

| # | 检查项 | 结果 | 证据 |
| --- | --- | --- | --- |
| 1 | tasks.md 全部任务已完成 `[x]` | **PASS** | OpenSpec `instructions apply` → `state: all_done`，progress 16/16，remaining 0 |
| 2 | 实现符合 change `design.md` 高层设计决策 | **PASS** | F1–F7 逐项落地（见 §2） |
| 3 | 实现符合 Design Doc | **PASS** | 配置键 14 项与实现字段逐一相符（见 §3）；组件清单、失败回退、回滚语义一致 |
| 4 | 能力规格场景全部通过 | **PASS** | delta spec 5 个 Requirement 全场景映射到测试（见 §4） |
| 5 | proposal.md 目标已满足 | **PASS** | 独立事实通道 / 4·3·4 策略全实现 / 注入顺序 / `session.facts.*` / 依赖前提（见 §5） |
| 6 | delta spec 与 design doc 无矛盾 | **PASS** | Build 阶段无 spec 增量修改，无未记录偏差 |
| 7 | `docs/superpowers/specs/` 关联设计文档可定位 | **PASS** | `2026-10-16-langchain4j-fact-memory-design.md` 存在且被 change `design.md` 以 `canonical_spec: openspec` 指向 |

## 2. `design.md` 高层决策符合性（F1–F7）

| 决策 | 实现落点 | 核对结论 |
| --- | --- | --- |
| F1 独立事实通道（独立 mem0 实例、`infer=true`、按 `user_id`、不带会话 `run_id`） | `Mem0FactStore`（`addDialogue` 写入 `infer=true`；直存 `infer=false`；`run_id` 仅出现于 javadoc 说明"不带"）；`FactMemoryConfiguration` 独立 base-url 装配 | 符合 |
| F2 抽取策略 4 全实现 | `FactExtractors` REGISTRY（mem0/llm/rule/hybrid）+ `resolveOrThrow` 装配期快速失败 | 符合 |
| F3 写入模式 3 全实现 | `FactWriteMode`（per-turn/batched/session-end）+ `FactTurnBuffer` + `FactWriteScheduler` + `FactMaintenance` | 符合 |
| F4 召回策略 4 全实现 | `FactRetrievers` REGISTRY（semantic/keyword/hybrid/rerank） | 符合 |
| F5 注入位置：事实在会话摘要之前 | `Langchain4jModuleConfiguration#applySessionMemory` 先 `FactMemoryAssembler.apply` 后 `SummaryMemoryAssembler.apply`；装配测试含 `InOrder` 断言 | 符合 |
| F6 失败隔离：读/写失败不中断对话 | `FactContextProvider` / `FactMaintenance` `catch (Throwable)` 仅日志降级；失败测试覆盖 | 符合 |
| F7 REGISTRY 形态（名字→内置、FQCN→反射兜底） | 与既有 `Summarizers.REGISTRY` 同形态；未知名装配期报错 | 符合 |

## 3. Design Doc 一致性（`docs/superpowers/specs/2026-10-16-langchain4j-fact-memory-design.md`）

- **§6 配置键 14 项逐字段核对**：`enabled=false`、`base-url`、`api-key=""`、`connect-timeout=5s`、`read-timeout=10s`、`extractor=mem0`、`write-mode=per-turn`、`batch-size=5`、`retriever=semantic`、`top-k=5`、`max-facts-tokens=500`、`extract-async=true`、`extract-model=""`、`rerank-model=""` —— 与 `Langchain4jAssistantProperties.Session.Facts` 字段及默认值逐一相符；`validate()`（batch-size/top-k/max-facts-tokens/策略名）已实现。
- **§3 组件清单**：`facts` 包 13 个类（FactStore/Mem0FactStore/FactExtractor(s)/FactRetriever(s)/FactTokens/FactTurnBuffer/FactWriteMode/FactWriteScheduler/FactMaintenance/FactContextProvider/FactMemoryAssembler）与设计一致。
- **§7 失败回退**：未知名策略装配期报错（fail-fast）、抽取/召回异常仅日志、无显式用户段跳过（一次性 WARN）、异步队列满丢弃、无 `extract-model` 时 llm 跳过 / hybrid 退化 rule —— 均有实现与测试。
- **§9 回滚**：`enabled=false` 为默认且不动 `ChatMemory` builder（`FactMemoryAssemblerTest` 断言 `verifyNoInteractions`）。

## 4. delta spec 场景覆盖（ADDED Requirements）

| Requirement | 场景 | 实现与验证 |
| --- | --- | --- |
| 事实通道与消息通道物理隔离 | 事实写入不污染消息解析 | 独立存储 + `FactMessageIsolationTest`（3）+ `FactMemoryIntegrationTest#factWritesDoNotPolluteMessageMemory`（按会话读消息仅含消息、`GsonChatMessageJsonCodec` 往返正常） |
| 同上 | 事实跨会话可见 | `FactMemoryIntegrationTest#factSinksInSessionOneAndRecallsInSessionTwo`（s1 沉淀「我叫张三」→ s2 注入「【用户长期记忆】」段召回命中；变异验证：换无交集查询断言变红，证明断言敏感） |
| 事实抽取策略可插拔 | 按配置切换抽取策略 | `FactExtractorsTest` + `FactMemoryAssemblerTest`（未知策略 `IllegalArgumentException`） |
| 同上 | 无可沉淀事实时静默 | `rule` 抽取器未命中静默；`FactMaintenance` 空事实不写入 |
| 事实写入模式可配置 | 逐轮 / 批量 / 会话结束 | `FactMaintenanceTest` / `FactTurnBufferTest` / `FactWriteSchedulerTest`（会话切换 drain、工具中间态不触发） |
| 事实召回策略可插拔 | 按本轮输入召回事实 | `FactRetrieversTest` + `FactContextProviderTest`（4 策略、预算截断） |
| 同上 | 无命中时跳过 | `FactContextProviderTest`（空段、`appended` 标记防空标题块） |
| 事实能力失败不影响主对话 | 依赖不可用降级 | `FactMaintenanceFailureTest` + `FactContextProviderTest` 失败隔离用例 |

## 5. proposal.md 目标核对

- ✅ 独立 `FactStore` 通道（独立 mem0 实例、`infer=true`、`user_id` 作用域）
- ✅ 抽取 4 策略 / 写入 3 模式 / 召回 4 策略**全实现**（非占位）
- ✅ `FactContextProvider` 注入顺序：人设 → 事实 → 摘要 → 最近 N 条原文 → 本轮输入
- ✅ 配置 `session.facts.*`（默认关闭 opt-in；开启且无 base-url 启动快速失败）
- ✅ 实施于 change ①（session-memory）之上，零破坏既有方法签名、零新增第三方依赖

## 6. 集成代码审查（review_mode: standard）

独立审查者 D2（与实现者分离）对最终 diff `0d140ec13..91567fae3`（54 文件）执行集成审查，重点覆盖未被分段审查 A/B/C 覆盖的部分（`FactMemoryIntegrationTest`、`usage.md` 修订）与跨模块集成正确性（写→读链路、装配顺序、失败隔离、并发）。

- 结论：**APPROVE**
- 逐项符合性：
  - **A. 最终 diff 覆盖复核**：`FactMemoryIntegrationTest` 3 条测试断言真实有效且变异敏感（手工推演确认无交集查询使召回断言失败）；`usage.md` 两处修订（FQCN 解析口径、session.enabled 前提）与实现一致
  - **B. 需求符合性**：proposal 目标、design D1–D13 决策、spec 场景全覆盖；无「文档写了但代码没做」或「代码做了但文档没写」的遗漏
  - **C. 集成正确性**：写→读链路（`FactMaintenance` → `FactStore` → `FactContextProvider`）、装配顺序（先 fact 后 summary）、失败隔离（读/写/网络）、并发语义、既有链路兼容性均无问题
- 发现：**无 CRITICAL / IMPORTANT / SUGGESTION 级别问题**
- 独立验证：前一个审查者跑完全模块测试 991/991（0 failures / 0 errors / 5 skipped，surefire 18:30–18:31）；D2 抽查重跑 `FactMemoryIntegrationTest` 3/3 通过（0.772s）

## 7. Runtime 验证证据

- 命令（从项目根）：`./mvnw -o -pl fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dmaven.test.skip=false`
- 结果：`Tests run: 991, Failures: 0, Errors: 0, Skipped: 5`（5 为存量跳过，非本 change 引入）
- `comet check run langchain4j-fact-memory verify --local -- …` → `exit=0; reused=true; tier=full`（复用 Build 的 full 证据：argv/cwd/输入面一致）
- 日志：`docs/openspec/changes/langchain4j-fact-memory/.comet/checks/0cff78f2-02d3-47c0-99e5-454cdc4e21f5.log`

## 8. 结论

七项检查全部 PASS；无 CRITICAL / IMPORTANT 问题（集成审查结论见 §6）。`verify_result` 由阶段守卫 `comet guard langchain4j-fact-memory verify --apply` 写入。

> 归档前提：需用户明确授权（验证通过本身不代表已授权归档）。
