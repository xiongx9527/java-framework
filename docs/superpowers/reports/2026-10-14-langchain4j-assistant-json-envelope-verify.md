# Verify 报告：langchain4j-assistant-json-envelope

- **change**：`langchain4j-assistant-json-envelope`（Comet Classic，`workflow=full`、`classic_profile=full`、`verify_mode=full`）
- **分支**：`dev`（`bound_branch=dev`，未 push）
- **基线 `base_ref`**：`e98ba52df64a716833afb0f9a1f3ace8069d153e`
- **提交**：
  - `503d846f9` 结构化输出工具类与手写规则断言
  - `8b41f0594` 通用聊天信封（一个入口 + N 份业务契约）并接管既有流式入口
  - `cbee5c197` Verify 修复轮（I-1 单次覆盖入口 + 接管边界文档同步 + 加固）
  - `1bbf857aa` Verify 第二轮修复（接管白名单措辞补全 + 计划文档修正 + 兜底轮次覆盖补充）
- **验证次数**：本报告为第 3 次 verify 的最终版本（第 1 次 NEEDS-FIX → `verify_failures=1`；第 2 次 PASS 但留 3 项文档偏差与 1 项覆盖缺口 → 用户指示修复 → `verify_failures=2`；本轮为修复后的第 3 次 verify）

## 1. 结论

**PASS**（无 CRITICAL / 无 IMPORTANT；第 2 次复核见第 9 节、第 3 次复核见第 12 节）。

- 第 1 次审查的 `I-1`（IMPORTANT）已修复并有产品入口级测试覆盖（复核者用 `javap` 字节码独立确认透传链）；`W-1` 按用户选定方案（精确化 spec 措辞 + 同步文档）处理；`W-2`/`W-3`/`W-4`/`S-1`/`S-2` 全部处置并经独立核对。
- 第 2 次复核发现的 3 项文档偏差 + 1 项覆盖缺口（第 8 节第 7–10 条）已由第 12 节的第 6 组修复轮全部处置并经第 3 次独立复核确认。
- `git diff e98ba52df..HEAD` = **64 A / 3 M / 0 D**（其中 62 A 为产品/测试/文档/流程产物，另 2 A 为第 12 节修复轮的 build 证据日志 `.comet/checks/*.log`）：零签名改动、零删除、零库层新增依赖的性质保持。
- 模块 835 例、样例 15 例全绿（第 3 次复核独立复跑一致）；build 证据（14/14 检查）与 verify 命令检查一致。
- **无遗留 CRITICAL / IMPORTANT / WARNING**；第 8 节其余条目为设计取舍或环境限制说明，不阻塞归档。

## 2. 本轮过程

1. Verify 第 1 次：独立集成审查 → **NEEDS-FIX**（1 IMPORTANT + 4 WARNING + 2 SUGGESTION + 0 CRITICAL）。
2. 按 `comet-verify` 失败路径：报告失败项 → `comet state transition <change> verify-fail` → 回 Build 修复（`check_epoch` 4 → 5）。
3. Build 修复轮：追加 tasks **6.1–6.4**（`--assign-ids` 分配 ID 后逐项 `task-complete`，21/21 完成）→ 修复 → 录制 build 证据 → `comet guard build --apply` **14/14 PASS** → 提交 `cbee5c197`。
4. Verify 第 2 次：入口验证 4/4 PASS → verify 命令检查（`exit=0`、`reused=true`、`tier=full`）→ 针对性独立复核（只读，独立会话）→ 结论 PASS，但发现 3 项文档偏差 + 1 项覆盖缺口。
5. 用户指示「修复未完成的问题」→ 按 `comet-verify` 失败路径 `comet state transition ... verify-fail`（`verify_failures` 2）→ 回 Build 追加 tasks 第 7 组（7.1–7.3，24/24 完成）→ 修复 R-1–R-4 → 录制 build 证据 → `comet guard build --apply` **14/14 PASS** → 提交 `1bbf857aa`。
6. Verify 第 3 次（本轮）：入口验证 4/4 PASS → verify 命令检查 → 针对性独立复核（只读）→ **PASS**（无 CRITICAL/IMPORTANT），见第 12 节。

## 3. 第 1 次审查项与处置

| 编号 | 级别 | 问题 | 处置 | 证据 |
| --- | --- | --- | --- | --- |
| **I-1** | IMPORTANT | 结构化开关「单次调用」层无产品入口：`EnvelopeRuntime` 调 `resolveEnabled(null, contract.getStructured())`，且 `EnvelopeAssistant.chat(String, String)` 不收该入参 → 规格承诺的三层覆盖顺序对该层不可达 | 新增 `EnvelopeRuntime.decide(String, String, Boolean)` 与 `EnvelopeAssistant.chat(String, String, Boolean)`，既有 2 参重载委托 `null`（签名与行为不变） | `EnvelopeRuntime.java:110`、`EnvelopeRuntime.java:122`、`EnvelopeAssistant.java:88-89`；新增两例测试（`EnvelopeAssistantTest.java:71`、`:87`） |
| **W-1** | WARNING | 接管范围措辞偏宽：`streamToSse(memoryId, context, message)` 既不在白名单也不在「不接管」清单 | 用户决策点选定「精确化 spec 措辞并同步文档」：delta spec 逐项列名并写明排除理由、新增 Scenario；`design.md` D12 同步；`SimpleAssistant` Javadoc 与该方法 Javadoc 补充；补边界锁定断言 | `specs/langchain4j-chat-envelope/spec.md:113-119`、`:146-149`；`design.md:184-198`；`SimpleAssistant.java:773-779`、`:808-809`；`SimpleAssistantEnvelopeTest.java:218`；白名单第三项 `streamToSse(String)` 由第 12 节第 6 组补全（`spec.md:113-115`、`:141-144`） |
| **W-2** | WARNING | `EnvelopeSessionMemory` 共享 store 读改写非原子（既有特性） | Javadoc 写明「同一 `memoryId` 的轮次需调用方串行」及其成因与不可能内部消除的理由 | `EnvelopeSessionMemory.java:36-42` |
| **W-3** | WARNING | `SimpleAssistant.envelopeRuntime` 字段非 `volatile` | 字段加 `volatile` + 说明 | `SimpleAssistant.java:763` |
| **W-4** | WARNING | `design.md` 组件表仍写 `AssistantBuilder` 装配（实际为 `EnvelopeRuntimeInstaller` + 链式 setter） | 组件表改为真实装配方式并加注 | `design.md:314-327` |
| **S-1** | SUGGESTION | 无条件 `@Import` 使信封全关时仍需启动期 fail-fast（属有意） | 在 `proposal.md` 兼容性段落记为「开关关闭时唯一的行为前移」 | `proposal.md` Impact 段 |
| **S-2** | SUGGESTION | 建议在 proposal 兼容性段落标注 `start` 帧时机变化 | `proposal.md` 写明 `start` 帧 `data` 形态变化 + **改到分类完成后才发出** + `startData` 不再使用 | `proposal.md` Impact 段 |

> 附加同步（恢复阶段自查发现，非审查项）：`tasks.md` 5.2、`design.md` 测试策略「已知测试硬限制」、计划「测试硬限制」约定与 `SimpleAssistantEnvelopeTest` 类注释中「无法解析真实 SSE 帧」的过时结论，以及 `proposal.md` 风险节里未采用的「隔离记忆标识」对策，全部订正为实际做法（反射读真实帧；结构化轮次直连模型、重试零写入）。

## 4. 修复轮改动清单（`cbee5c197`，相对 `8b41f0594`）

- **实现（新增，不改既有签名）**
  - `EnvelopeRuntime.decide(String memoryId, String message, Boolean structuredOverride)`（`EnvelopeRuntime.java:110`）：把 `structuredOverride` 透传进 `properties.getStructured().resolveEnabled(...)`（`:122`）；`contract == null` 时恒为非结构化（无契约即无 schema）。
  - `EnvelopeAssistant.chat(String memoryId, String message, Boolean structuredOverride)`（`EnvelopeAssistant.java:88`）：委托 `runtime.decide(memoryId, message, structuredOverride)`；2 参重载（`:70`）委托 `null`。
  - `SimpleAssistant.envelopeRuntime` 加 `volatile`（`SimpleAssistant.java:763`）。
- **测试（新增 3 例）**
  - `EnvelopeAssistantTest#callOverrideOffSuppressesStructuredEvenWhenContractEnablesIt`（`:71`）：契约 `structured=true` + 本次 `FALSE` → `ok=true`、`business=order`、`result == null`、走既有通道（`caller.calls` 有 1 条）。
  - `EnvelopeAssistantTest#callOverrideOnForcesStructuredWhenContractOptsOut`（`:87`）：契约 `structured=false` + 本次 `TRUE` → `result.orderId == "R9"`、`text` 为空、既有通道调用 0 次。
  - `SimpleAssistantEnvelopeTest#contextOverloadIsNotTakenOver`（`:218`）：自带 `context` 的重载未被接管 —— `verify(provider).stream(MEMORY_ID, "你是一个诗人", ...)`（调用方 context 原样下发）+ `model.requests` 为空（连分类都不发生）。
- **文档**：delta spec 接管条款 + 新增 Scenario；`design.md` D12 / 组件表 / 配置表 / 测试策略；`proposal.md` 兼容性、行为前移、风险节；计划「测试硬限制」+ 新增第 6 组任务映射；`tasks.md` 5.2 措辞 + 新增 6.1–6.4。

## 5. 测试与检查证据（真实输出）

| 检查 | 命令 | 结果 |
| --- | --- | --- |
| TDD RED | `cd fw-plus-ai && ./mvnw -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dmaven.test.skip=false -Dtest=EnvelopeAssistantTest -DfailIfNoTests=false` | **COMPILATION ERROR**：`无法将类 ... EnvelopeAssistant 中的方法 chat 应用到给定类型`（第 77/92 行，新入口尚未实现）→ RED 失败原因正是待实现行为 |
| TDD GREEN（定向） | 同上命令（去掉 `-Dtest` 限定 `EnvelopeAssistantTest`） | `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS` |
| 模块全量 | `cd fw-plus-ai && ./mvnw -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dmaven.test.skip=false` | `Tests run: 834, Failures: 0, Errors: 0, Skipped: 5` — `BUILD SUCCESS`（修复轮前为 831；+3 = 上述 3 例） |
| 模块 + 样例（一次 reactor，build 证据命令） | `./mvnw -o -pl fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure,fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-structured-output-sample test -Dmaven.test.skip=false` | 模块 `Tests run: 834, Failures: 0, Errors: 0, Skipped: 5`；样例 `Tests run: 15, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS` |
| 样例分项 | 同上 | `StructuredOutputSampleTest` 4 + `EnvelopeAssistantSampleTest` 8 + `EnvelopeChannelSameShapeTest` 3 = 15 |
| build 命令检查 | `comet check run langchain4j-assistant-json-envelope build --local -- <上表 reactor 命令>` | `exit=0; reused=false; tier=full`（日志 `.comet/checks/c3e8ef3a-....log`） |
| build 守卫 | `comet guard langchain4j-assistant-json-envelope build --apply` | **14/14 PASS**（含 `tasks.md all tasks checked`、`plan task mapping is valid`、`Build passes`）→ `phase=verify`、`check_epoch=5` |
| verify 入口验证 | `comet state check langchain4j-assistant-json-envelope verify --json` | `checks: {"passed":4,"total":4,"blocked":false}`、`issues: []`、`missingFields: []` |
| verify 命令检查 | `comet check run langchain4j-assistant-json-envelope verify --local -- <同一 reactor 命令>` | `exit=0; reused=true; tier=full`（复用同一输入面的 build 证据） |
| **第 12 节（本轮）定向** | `cd fw-plus-ai && ./mvnw -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dmaven.test.skip=false -Dtest=EnvelopeAssistantTest -DfailIfNoTests=false` | `Tests run: 11, Failures: 0`（10 + R-4 新增 1，覆盖既有短路语义无 RED） |
| **第 12 节（本轮）模块 + 样例（build 证据命令）** | `./mvnw -o -pl fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure,fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-structured-output-sample test -Dmaven.test.skip=false` | 模块 `Tests run: 835, Failures: 0, Errors: 0, Skipped: 5`；样例 `Tests run: 15, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS` |
| **第 12 节（本轮）build 命令检查** | `comet check run langchain4j-assistant-json-envelope build --local -- <上表 reactor 命令>` | `exit=0; reused=false; tier=full`（日志 `.comet/checks/95240c8c-....log`） |
| **第 12 节（本轮）build 守卫** | `comet guard langchain4j-assistant-json-envelope build --apply` | **14/14 PASS** → `phase=verify`、`check_epoch=7` |
| **第 12 节（本轮）verify 入口 + 命令检查** | `comet state check ... verify` + `comet check run ... verify --local -- <同一 reactor 命令>` | 入口 `passed:4/4, issues:[]`；命令检查 `exit=0; tier=full` |

## 6. Requirement / Scenario 映射（实测计数）

三份 delta spec 合计 **21 Requirement / 61 Scenario**（第 12 节 R-1 新增 1 个 Scenario 后实测 `grep -c`）：

- `langchain4j-assistant-business-routing`：7 R / 21 S
- `langchain4j-chat-envelope`：8 R / 27 S
- `langchain4j-structured-output`：6 R / 13 S

| delta spec / Requirement | 实现 | 代表性测试（真实用例名） |
| --- | --- | --- |
| structured-output / 结构声明同时驱动指令与本地校验 | `StructuredOutput`（`instruction`/`parseReply`/`ofType`） | `StructuredOutputTest#instructionCarriesSchemaAndJsonOnlyRule`、`#ofTypeGeneratesSchemaFromPojo`、`#promptWithoutErrorsIsInstructionPlusUserText` |
| structured-output / 宽容解析模型输出 | 同上（围栏/前后话剥离） | `#stripsJsonFence`、`#stripsSurroundingTalk`、`#parsesBareJson`、`#rejectsNonJson`、`#rejectsTopLevelArray`、`#staticParseReplyMatchesInstanceParse` |
| structured-output / 双层校验与全量错误清单 | `StructuredOutput.chat(...)`（Schema + `Rule`） | `#reportsMissingRequiredField`、`#reportsSchemaTypeViolation`、`#collectsAllRuleErrors`、`#rulesCatchWhatSchemaAllows`、`#ruleFailureIsReportedNotThrown` |
| structured-output / 错误回喂重试 | 同上（`maxAttempts`） | `#retriesOnSchemaViolation`、`#retriesWithPreviousErrorsFedBack`、`#promptWithErrorsFeedsBackPreviousFailure`、`#throwsWithLastReplyAfterExhaustingAttempts` |
| structured-output / 支持嵌套路径的校验规则 | `Rule`/`Rules` 点号路径 | `#dotPathRequiredReportsFullPathWhenLeafMissing`、`#dotPathRequiredReportsFullPathWhenIntermediateMissing`、`#dotPathTypeChecksNestedValue`、`#dotPathEnumChecksNestedValue`、`#dotPathRangeChecksNestedValue`、`#dotPathRegexChecksNestedValue`、`#topLevelRulesKeepWorkingAfterPathSupport` |
| structured-output / 从结构声明派生校验规则 | `Rule.fromSchema` | `#derivedRulesCoverNestedRequired`、`#derivedRequiredAcceptsExplicitNull`、`#derivedRequiredDeduplicatesNames`、`#derivedRulesCoexistWithManualRules`、`#derivedDecimalBoundsAreNotTruncated`、`#fromSchemaRejectsNonObjectTopLevel` |
| chat-envelope / 信封键位恒定 | `ChatEnvelope` | `ChatEnvelopeTest#allPathsShareTheSameFiveKeys`、`#jsonStringFormKeepsAllFiveKeysOnFailure`、`#toJsonObjectReturnsFreshCopy` |
| chat-envelope / 成功与失败的取值语义 | `ChatEnvelope` 静态工厂 + `EnvelopeAssembler.ensure` | `ChatEnvelopeTest#structuredSuccessCarriesResultAndEmptyText`、`#failedEnvelopeSerializesWithNullResultKey`、`#emptyReplyIsRepresentedByFailureEnvelope`、`InnerResultSemanticsTest#businessAndVersionPassThroughAllPaths`（经汇编器） |
| chat-envelope / 收敛过程永不抛异常 | `EnvelopeAssembler` | `EnvelopeAssemblerTest#everyInputConvergesToFiveKeysAndNeverThrows`、`#structuredRoundFailsOnTruncatedJson`、`#chatRoundFailsOnEmptyReply`、`#failureTextNeverLeaksExceptionDetails`、`#nullRoundModeFailsInsteadOfGuessingStructured` |
| chat-envelope / 帧协议保持不变 | `SimpleAssistant` 接管分支 + `EnvelopeRuntime`（token 纯文本、`start` 帧 3 键、结构化不发 token） | `SimpleAssistantEnvelopeTest#structuredRoundStreamsNoTokensAndDoesNotRetry`、`#switchOffKeepsOriginalBehaviour`、`EnvelopeRuntimeTest#startDataCarriesRequestIdBusinessAndTokenFlag`、样例 `EnvelopeAssistantSampleTest#structuredRoundStreamsNoTokens`、`#structuredSuccessPath` |
| chat-envelope / 流式与非流式同形 | 两条通道共用 `EnvelopeRuntime` + `ChatEnvelope` | 样例 `EnvelopeChannelSameShapeTest`（3 例，**反射读真实 SSE 帧**逐键逐值比较）、`EnvelopeAssistantTest#chatRoundDelegatesToExistingChannelAndWrapsText` |
| chat-envelope / 帧形态开关只可全局配置且默认关闭（接管白名单） | `SimpleAssistant` 两个无 mapper 重载的前置分支 | `SimpleAssistantEnvelopeTest#switchOnTakesOverSingleArgOverload`、`#switchOnTakesOverStartDataOverload`、`#switchOnTakesOverNoMapperOverloadForChatRound`、`#doneDataMapperOverloadIsNotTakenOver`、`#callbackOverloadsAreNotTakenOver`、`#contextOverloadIsNotTakenOver`、`#streamToSseWithFilterIsNotTakenOver`、`#generateTextToSseIsNotTakenOver`；`AssistantEnvelopeConfigurationTest#switchOffRegistersNothingAndInstallsNothing` |
| chat-envelope / 结构化开关与三层覆盖顺序 | `Langchain4jAssistantProperties.Structured#resolveEnabled` + `EnvelopeRuntime.decide` + `EnvelopeAssistant.chat` | `Langchain4jAssistantPropertiesTest#overrideOrderIsCallThenContractThenGlobal`、`EnvelopeRuntimeTest#decideKeepsBusinessButNotStructuredWhenContractOptsOut`、`EnvelopeAssistantTest#callOverrideOffSuppressesStructuredEvenWhenContractEnablesIt`、`#callOverrideOnForcesStructuredWhenContractOptsOut` |
| chat-envelope / 非法配置的处置 | `Langchain4jAssistantProperties.validate()`（启动 fail-fast / 运行期告警） | `Langchain4jAssistantPropertiesTest#startupFailsFastOnRangeViolation`、`#startupFailsFastOnUnrecognizableToggleValue`、`#runtimeIllegalValueKeepsOldValueAndWarns`、`#validatePointsAtTheOffendingKey`；`AssistantEnvelopeConfigurationTest#switchOnWithoutChatModelFailsFast`、`#switchOnWithoutMemoryStoreFailsFast` |
| business-routing / 业务契约声明的内容 | `BusinessContract`/`BusinessRegistry` | `BusinessRegistryTest#registersAndResolvesContract`、`#rejectsDuplicateNameAndPointsAtIt`、`#contractFailsFastOnIllegalSchema`、`#contractRejectsNonPositiveMaxAttempts`、`#declaredRulesAreAddedOnTopOfDerivedOnes` |
| business-routing / 业务解析与兜底 | `BusinessRegistry`（未注册回落 `chat`） | `BusinessRegistryTest#unregisteredNameFallsBackToChat`、`BusinessRouterTest#unregisteredBusinessFallsBackToChat`、`EnvelopeRuntimeTest#nullRegistryMeansEveryRoundFallsBack` |
| business-routing / 业务只由模型分类决定 | `EnvelopeAssistant`（无业务入参）+ 每轮 `decide` | `EnvelopeRuntimeTest#decideIsPerTurnNotSticky`、`#decideClassifiesToRegisteredBusiness`、`BusinessRouterTest#doesNotCarryOverPreviousTurnResult`、`#adjacentTurnsWithDifferentTopicsAreJudgedSeparately`、`#topicSwitchingBackStillRoutesCorrectly` |
| business-routing / 对话历史共用 | `EnvelopeSessionMemory`（同一 `ChatMemoryStore`） | `EnvelopeSessionMemoryTest#existingChatTurnIsVisibleToTheNextStructuredRound`、`#ephemeralChatMemoryReadPathSeesStructuredWriteBack`、`#structuredWriteBackIsVisibleToExistingChatMemory`、`BusinessRouterTest#historyIsSharedAcrossBusinessSwitch`、`#historyIsSharedAcrossThreeTurns` |
| business-routing / 模型分类判定 | `BusinessClassifier`（枚举 = 已注册业务名） | `BusinessClassifierTest#hitsRegisteredBusiness`、`#promptCarriesRegisteredNamesAndCurrentMessage`、`#followUpTurnHitsBusinessViaRecentUserMessages`、`#unregisteredResultFallsBackToChat`、`#thrownErrorFallsBackToChat`、`#configuredAttemptsAreUsedWhenAskedToRetry`、`#historyLengthIsLimitedByConfig` |
| business-routing / 内层结果形态与追问语义 | `BusinessContract.schema`/`Rule` + 汇编器 | `InnerResultSemanticsTest#listResultIsWrappedInAField`、`#bareArrayIsRejectedBecauseResultMustBeAnObject`、`#followUpIsASuccessfulBusinessResult`、`#followUpIsNotGuessedByKeywords`、`#missingRequiredFieldIsAFailureNotAFollowUp`、`#declaredRulesAndDerivedRulesBothApply`、`#validationErrorsNeverLeakIntoTheEnvelope` |
| business-routing / 两种调用形态 | `EnvelopeAssistant.chat`（非流式可重试）/ 流式不重试 | `EnvelopeRuntimeTest#structuredRoundRepeatsHistoryInRetryPrompt`、`#structuredRoundFailsAfterContractMaxAttempts`、`#structuredRoundOnceDoesNotRetry`、样例 `EnvelopeAssistantSampleTest#structuredSuccessPath`、`#validationFailurePathRetriesThenFails`、`#followUpPathKeepsBusinessAcrossTurns`、`#unmatchedFallsBackToChat`、`#chatRoundStreamsTokens` |

> 说明：上表为 Requirement 级映射（21 行）。第 1 次审查已给出 Scenario 级逐条映射并在第 2 节记录的唯一缺口 `I-1`；本轮修复后，受影响的 Scenario（`单次调用覆盖业务契约`、`自带 context 的流式入口`）已各自有专测，其余 Scenario 的映射与实现均未变动（修复轮只新增重载/注释，未改既有行为），第 9 节的针对性复核再次核对了一致性。

## 7. 约束核对

| 约束 | 核对方式 | 结果 |
| --- | --- | --- |
| 零签名改动（既有入参/出参一个都不动） | `git diff e98ba52df..HEAD --name-status` 分类统计 | **64 A / 3 M / 0 D**（含 2 个修复轮证据日志）；`M` 仅 `SimpleAssistant.java`、`LangChain4jSupportAutoConfiguration.java`、`fw-plus-sample/fw-plus-ai-samples/pom.xml` |
| 修复轮未破坏上述性质 | `git diff 8b41f0594..cbee5c197 -- '*SimpleAssistant.java'` | 仅有 `private EnvelopeRuntime` → `private volatile EnvelopeRuntime` 与 Javadoc 变化，无签名变化 |
| 不新增 `@MagicModule` / magic-script 专用形态 / `POST /chat/json` | 新增文件清单与 API 面复核 | 未新增；入口为编程式 `EnvelopeAssistant` |
| Java 8 语法 | 对全部非 `test` 的**新增文件**扫描 `List.of(`/`Map.of(`/`Set.of(`/`.copyOf(`/`orElseThrow()` | 无命中 |
| 库层无新增第三方依赖 | 三个 `M` 文件 diff 复核 | 库层无依赖变化；样例 `pom.xml` 仅新增 `langchain4j`、`spring-boot-starter-test`（样例侧） |
| delta spec 与既有 capability 不冲突 | `docs/openspec/specs/` 无同名 capability | 三份均为 `## ADDED`，无 `MODIFIED` |
| 包名/编码/缩进 | 新增文件与既有文件对照 | `com.xiongx9527.fw.plus.*`、UTF-8、Tab |

## 8. 遗留与偏差（不阻塞归档）

1. **R1（既有能力限制，非本 change 引入）**：宿主若为 SSE 注册了 fastjson 消息转换器，`done` 帧的 `result` 键在序列化时会被丢弃（产品代码已按规格输出对象；`ChatEnvelope.toJsonString()` 强制保留 null 键）。本 change 不改；如需要，一行修法：`data(envelope.toJsonString())`。
2. **分类器提示词缺「都不匹配」出口**：当前提示词只列已注册业务名，未显式给出「都不匹配 → 不选任何业务」的选项；未命中仍由「返回未注册名 → 回落 `chat`」兜住（`BusinessClassifierTest#unregisteredResultFallsBackToChat`、`#unparsableReplyFallsBackToChat`）。
3. **`contract == null` + 单次 `TRUE` 无专测**：~~该组合恒为非结构化，由 `EnvelopeRuntime.java:121-122` 的单一布尔表达式（`contract != null && ...`）保证并在 Javadoc 声明；未加专测（加测需再走一轮 Build+证据重录）。~~ → **已关闭**：第 12 节 R-4 已补 `EnvelopeAssistantTest#callOverrideOnWithoutAnyContractStillProducesNoResult`。
4. **真实异步流式帧时序未做端到端验证**：样例侧同形断言使用同步回调的假流式模型；真实网络流式的帧间时序（`start` 帧延后一帧）未端到端压测。
5. **工作区遗留未提交文件**：`docs/openspec/changes/archive/2026-09-29-langchain4j-magicapi-model-plugins/.comet/delivery.json` 是上一个 change 归档时的 Comet 运行时文件，与本 change 无关；保持未提交、未修改归属，本 change 的提交未包含它。
6. **基线双比对限制（第 1 次审查已记录）**：未在基线提交编译 `target/classes` 做 `javap` 双向比对；零签名改动结论基于全仓 diff（含 `--diff-filter=D` 计数为 0）+ 源码签名 diff + HEAD `javap`。
7. **WARNING（第 2 次复核新增，W-1 残留）**：~~…spec/design 措辞过窄且字面为假，未达成 tasks 6.2 的"三处口径一致"。无运行时风险，纯措辞。~~ → **已关闭**（第 12 节 R-1）：白名单改为三个重载并补列 `streamToSse(String message)`，新增 Scenario。
8. **WARNING（第 2 次复核新增）**：~~计划文件 5.2 正文仍是过时约定，与同文件 `:27-29` 及 `tasks.md` 5.2 矛盾。~~ → **已关闭**（第 12 节 R-2）。
9. **SUGGESTION（第 2 次复核新增）**：~~计划文件的 `## 风险与回退` 标题被误替换，三条 bullet 悬空。~~ → **已关闭**（第 12 节 R-3）。
10. **SUGGESTION**：~~`contract == null` + 单次 `TRUE` 无专测。~~ → **已关闭**（第 12 节 R-4，同本节第 3 条）。

## 9. 第 2 次针对性独立复核结论

**结论：PASS**（无新增 CRITICAL / IMPORTANT）。复核方式：独立会话、**只读**（不修改任何仓库文件、不执行任何 `git add/commit/checkout/stash/clean`），覆盖修复 diff `8b41f0594..cbee5c197`、`I-1` 是否彻底解决、`W-1`…`S-2` 处置准确性、delta spec / design / 代码一致性、以及模块与样例回归。

**I-1 已彻底解决（复核者的独立证据）**

- 透传链：`EnvelopeRuntime.java:95-97`（2 参委托 `null`）→ `:110-125`（3 参，`:122` 把 `structuredOverride` 传入 `resolveEnabled`）；`EnvelopeAssistant.java:70-72`（2 参委托 `null`）→ `:88-89`（3 参 → `runtime.decide(memoryId, message, structuredOverride)`）；`Langchain4jAssistantProperties.java:128-136`（顺序 `call → contract → default`）。
- **字节码实证**（对已编译 `target/classes` 只读 `javap`）：3 参 `decide` 在偏移 87 处 `aload_3`（第三个 `Boolean`）→ `resolveEnabled(Boolean,Boolean)`；两个 2 参重载均以 `aconst_null` 进 3 参。
- **证伪性**（代码路径推理，未做变异实证）：若把 `decide` 的 override 改回恒 `null`，新增两例分别会因「走结构化轮 → 脚本用尽 → `ok=false`」与「非结构化 → `result==null` → NPE」而失败；两例都经真实产品入口 `EnvelopeAssistant.chat(memoryId, message, Boolean)`。
- `contract == null` 的短路语义与 Javadoc 声明一致（无专测，见第 8 节第 10 条）。

**W-1 到 S-2 的核对结果**

- `W-1`：四处（spec / design D12 / `envelopeRuntime` Javadoc / context 重载 Javadoc）+ 新增 Scenario + 边界测试均落地；`contextOverloadIsNotTakenOver`（`SimpleAssistantEnvelopeTest.java:217-227`）装在非空信封处理器上断言「分类不发生」→ 能证伪、不过弱。**残留一处过窄措辞** → 第 8 节第 7 条。
- `W-2`：复核者独立核对 `InMemoryChatMemoryStore`（`computeIfAbsent` 返回活列表 + `put` 写回）与 `MessageWindowChatMemory`（每次 `messages()` 重建窗口）确为非原子读改写，`EnvelopeSessionMemory.java:36-40` 的说明准确，未说错。
- `W-3`：`SimpleAssistant.java:763` 已为 `private volatile EnvelopeRuntime envelopeRuntime;`。
- `W-4`：`design.md:314-328` 与实现 `LangChain4jSupportAutoConfiguration.java:185-263`（`AssistantEnvelopeConfiguration` + `EnvelopeRuntimeInstaller.afterSingletonsInstantiated`）一致。
- `S-1`/`S-2`：`proposal.md:63-69` 的兼容性与"开关关闭时唯一的行为前移"与代码一致（`LangChain4jSupportAutoConfiguration.java:186` 无条件 `@EnableConfigurationProperties`、`Langchain4jAssistantProperties.java:32,45-56` 始终 `validate()`）；`start` 帧时序在 `SimpleAssistant.java:873-912` 得到印证。真实帧做法与样例 `EnvelopeChannelSameShapeTest.java:167-184`（`:169` 反射读 `earlySendAttempts`）一致。

**复核者实测（独立复跑，串行，无并发 Maven）**

| 命令 | 结果 |
| --- | --- |
| `cd fw-plus-ai && ./mvnw -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dmaven.test.skip=false` | `Tests run: 834, Failures: 0, Errors: 0, Skipped: 5` — `BUILD SUCCESS` |
| `./fw-plus-core/mvnw -o -f fw-plus-sample/pom.xml -pl fw-plus-ai-samples/fw-plus-ai-structured-output-sample test -Dmaven.test.skip=false` | `Tests run: 15, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS`（4 + 8 + 3） |

（复核者先执行了 `install -DskipTests` 以保证样例跑在 HEAD 源码上：`BUILD SUCCESS`；仅写 `~/.m2`，在仓库之外。）

**复核者未核实事项**：① 未做"改回恒 `null` 再跑测试"的变异实证（以字节码 + 路径推理代替）；② 真实网络异步流的帧间时序未端到端验证；③ 本报告第 6/7 节计数仅抽查；④ 复核开始时仓库内尚无本报告文件，原审查项原文以任务描述与后来写入的本报告第 3 节交叉确认。

**工作树证据**：复核结束后 `git status --short` 仅见 3 个先前既有的 `.comet` 运行时文件与本报告（`??`），无任何源码/文档被复核者改动。

## 10. 归档建议

- 三份 delta spec 合入 `docs/openspec/specs/` 对应 capability（`langchain4j-chat-envelope`、`langchain4j-assistant-business-routing`、`langchain4j-structured-output`，均为新增 capability）。
- 归档需用户明确授权（Comet 协议），授权前不执行 `comet archive`。

## 11. 待定：3 项文档级偏差的处置方式（用户决策点）

第 2 次针对性复核结论为 PASS（无 CRITICAL/IMPORTANT），因此不触发"自动回 Build"的强制路径；但复核发现 3 项**本轮修复轮自己引入的文档瑕疵**（第 8 节第 7–9 条），需要用户决定处置方式（Comet 决策点：是否接受 WARNING/SUGGESTION 级偏差、如何处理实现与 Spec 措辞的不一致）：

1. `spec.md:113-115` + `design.md:185-186` 白名单措辞漏列 `streamToSse(String)`（实现与测试实际接管，Javadoc 已列出）—— 若原样归档，该过窄措辞会合入 `docs/openspec/specs/`。
2. 计划文件 5.2 正文与同文件顶部/`tasks.md` 5.2 矛盾（过时约定未同步）。
3. 计划文件 `## 风险与回退` 标题被误替换、三条 bullet 悬空。

三种可选处置：**回 Build 修掉后再验证一次**（推荐；纯文档、零行为变化，需再走一次 build 证据录制 + 守卫）/ **只修第 2、3 项并接受第 1 项偏差**（第 1 项写入报告遗留）/ **全部接受并归档**（三项全部写入报告遗留）。本报告当前的 PASS 结论对三种处置都成立；差异只在归档产物的文档洁净度。

> **已处置（用户选定第 1 项）**：用户在验证过程中指示「修复未完成的问题」，等价于选择**回 Build 修掉 3 项后再验证一次**。已按协议 `comet state transition ... verify-fail`（`verify_failures` 1 → 2）回 Build，逐项修复（见第 12 节），并顺带补齐复核列出的第 4 项覆盖缺口。本节保留为决策历史。

## 12. 第 3 次 verify：第二轮修复轮的复核

### 12.1 本轮修复项（提交 `1bbf857aa`，父 `cbee5c197`）

| 编号 | 来源 | 问题 | 修复 | 证据 |
| --- | --- | --- | --- | --- |
| R-1 | 第 2 次复核 WARNING | `spec.md:113-115` + `design.md:185-186` 白名单写「两个重载 / 白名单之外一律不动」，但 `streamToSse(String message)` 经委托同样被接管 → 字面为假，tasks 6.2「三处口径一致」验收未达成 | delta spec 与 design D12 改为「白名单三个重载」并补列 `streamToSse(String message)`（注明内部委托第二项）；design 附变更注记；新增 Scenario「单参重载委托后的形态」 | `specs/langchain4j-chat-envelope/spec.md:113-115`、`:141-144`；`design.md:185-197`；对应断言 `SimpleAssistantEnvelopeTest.java:113` |
| R-2 | 第 2 次复核 WARNING | 计划文件 5.2 正文仍是「`doneDataMapper`/`onDone` 侧信道 + 不解析真实帧」的过时约定，与同文件顶部约定及 `tasks.md` 5.2 矛盾 | 5.2 正文改为真实帧方案（反射读 `ResponseBodyEmitter.earlySendAttempts`），并注明初版结论已被 Build 阶段 M1 审查证伪 | `docs/superpowers/plans/2026-10-14-langchain4j-assistant-json-envelope.md` 5.2 |
| R-3 | 第 2 次复核 SUGGESTION | 计划文件 `## 风险与回退` 标题被第 6 组标题误替换，三条 bullet 悬空 | 恢复标题，标题层级完整（`## 第 1 组` … `## 第 7 组`、`## 风险与回退`） | 同上文件尾 |
| R-4 | 第 2 次复核 SUGGESTION（覆盖缺口） | 「无契约注册（`contract == null`）+ 单次 `TRUE` 仍不产出 `result`」仅有单一布尔表达式保证，无专测 | 新增 `EnvelopeAssistantTest#callOverrideOnWithoutAnyContractStillProducesNoResult`（兜底 `chat` 轮次、`result` 为 null、`text` 为调用方回复）；覆盖既有短路语义，无 RED | `EnvelopeAssistantTest.java`（新增用例） |

- 本轮改动：`1bbf857aa` 5 文件、47 插入 / 21 删除（`tasks.md`、`design.md`、delta spec、计划文件、`EnvelopeAssistantTest.java`）；无产品代码改动。
- 回归：模块 **835 例**（+1，0 失败 / 0 错误 / 5 跳过）、样例 **15 例**（0 失败）。
- 流程：追加 tasks 第 7 组 **7.1–7.3**（`--assign-ids` 后逐项完成，**24/24**）→ 录制 build 证据（`exit=0; tier=full`）→ `comet guard build --apply` **14/14 PASS** → 提交 → Verify 入口验证 **4/4 PASS**（`issues: []`）→ 录 verify 命令检查（`exit=0; tier=full`）。

### 12.2 第 3 次独立复核结论（只读，独立会话）

**PASS**（无 CRITICAL / 无 IMPORTANT）。

- **A. 接管真值核对**：逐重载复核 `SimpleAssistant`，确认接管的恰好是三个重载——`streamToSse(String memoryId, String message, Object startData)`、`streamToSse(String memoryId, String message)`、`streamToSse(String message)`（经委托第二项）；不接管清单与 spec/design 完全一致，**无漏列、无错列**。
- **B. Scenario 对应**：新增 Scenario「单参重载委托后的形态」有测试断言 `SimpleAssistantEnvelopeTest.java:113`（`switchOnTakesOverSingleArgOverload`）。
- **C. 计划文件**：标题层级完整、5.2 正文与顶部约定及 `tasks.md` 一致，无自相矛盾。
- **D. 新增测试**：无假通过风险（`entry(...)` 未注册任何契约，`contract == null` 前置成立）、与既有测试不重复。
- **E. 独立复跑**：核实命令下模块 **835 例**、样例 **15 例**、0 失败，与报告一致。
- **F. 约束**：本轮未改任何既有方法签名、未引入 Java 9+ API、未新增第三方依赖。

### 12.3 本轮后的遗留

第 8 节原第 7–10 条（即 R-1–R-4）**全部关闭**。第 8 节其余条目为设计取舍/环境限制说明（R1 宿主 fastjson 转换器会在 `done` 帧丢 `result` 键、分类器提示词缺"都不匹配"出口、真实网络异步流帧间时序未端到端验证、`javap` 双向比对未做等），均已在第 8 节逐条写明归属与后续建议，不阻塞归档。
