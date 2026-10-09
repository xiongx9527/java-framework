# 验证报告：streaming-thinking-events

- Change: `streaming-thinking-events`
- 分支: `springboot3`
- 阶段: verify（full）
- 验证日期: 2026-10-09
- 验证方式: full（8 tasks / 2 delta specs / 变更文件多）

## 概述

本 change 让 LangChain4j 流式对话把模型独立思考增量以既有 SSE `thinking` / `thinking-done` 事件发送，保持 `token` / `tool-call` / `tool-result` / `done` 协议不变；只有适配器真实映射独立思考的 provider 才暴露原生思考选项（Ollama 用 `think` + `return-thinking`），其余 provider 不造通用开关并在 README 记录限制。

本次验证在原 8/8 任务全部完成的基础上，补做了：
1. 修复 `fw-plus-ai-magicapi-sample/pom.xml` 对已删除 `fw-plus-ai-langchain4j-*` 模块的引用，解除 Comet build guard 长期阻塞（build 阶段 14/14 检查通过）。
2. 依赖 resolution、目标测试套件、控制台元数据一致性测试的实际运行。

## 验证证据

### Runtime 记录

- Build guard（`comet guard build --apply`）14/14 PASS，阶段推进到 verify；`check_epoch=3`。
- Verify check（`comet check run verify --local`）exit=0，`reused=false`（真实执行）：
  - `SimpleAssistantTest` 15 passed（含思考-转回答、思考-转工具、纯思考无回答、无思考、直接流式路径 5 个 thinking 场景）
  - `StreamingThinkingFactoryTest` 7 passed（Anthropic/Cohere/GoogleGenAi/Mistral/OpenAI/Watsonx/Zhipu 选项透传 + 未配置保持 SDK 默认）
  - `Langchain4jOllamaModelFactoryTest` 3 passed
  - `StreamingThinkingAdapterAuditTest` 3 passed
  - `ChatAssistantConfigTest` 3 passed（含 `streamingChatModelPassesThroughThinkingOptions` 断言 `think()`/`returnThinking`）
  - streaming-chat-model console 元数据测试 4 passed（metadata 与后端工厂 options 键一致、字段中英文文案、provider 短名展示名、index.js 注册）
- 日志：`docs/openspec/changes/streaming-thinking-events/.comet/checks/422522cf-6da3-4b72-a409-2c6872beb4cf.log`

### 回归对比（用户附近的既有基线，供核对未引入新回归）

- 基线（stash 基线 full-run）: 586 tests / 11 failures / 20 errors / 5 skipped
- 改后（含本次 POM 修复 full-run）: 576 tests / 10 failures / 20 errors / 5 skipped
- 差异：基线多出的 1 failure 为已删除 `GuardedToolExecutorTest` 的既有失败；`MultimodalAssistantBridgeTest` 1 failure + 3 errors 等为既存环境性失败，均与本次 change 无关。

## 验证检查项

### 1. tasks.md 全部任务已完成

- 8/8 任务全部 `[x]`，任务 ID 与计划映射 `ready`，`unmappedCount=0`。

### 2. 实现符合 design.md 高层设计决策

- 决策 1（以 `PartialThinking` 回调能力作为支持标准）：`streamTokenStreamToSse` 仅消费 `onPartialThinkingWithContext` 增量；README 能力表逐 provider 标注 SDK 版本与独立 thinking 回调状态，限制项明确（Azure/Vertex/LocalAI/OCI/Xinference/Qianfan/GitHub/OpenAI-Official 均标注不支持）。
- 决策 2（provider 配置遵循现有 options 与前端元数据边界）：Ollama 工厂仅 `o.has("think")`/`o.has("return-thinking")` 时透传，未配置保持 SDK 默认；providers.js 仅列出各 provider 真实字段。
- 决策 3（SSE 由单一 provider 无关桥接器处理）：`ThinkingSseTracker` + `streamTokenStreamToSse` 覆盖 AiServices TokenStream 与直接 StreamingChatModel 两条路径（`DirectTokenStream.onPartialThinking` 转发）；`thinking-done` 在 `thinking.finish()` 处先于 token/tool-call/done 发送。
- 决策 4（审核结果随控制台能力文档交付）：README 能力表交付。

### 3. 实现符合 Design Doc / 能力规格

两个 delta spec 的需求与场景核对：

**`langchain4j-streaming-thinking-events`（ADDED）**

| 场景 | 实现证据 | 测试证据 | 结果 |
|---|---|---|---|
| 推送增量思考与完整思考（thinking → answer） | `SimpleAssistant.java:897-910` `onPartialThinkingWithContext` → `thinking.delta()`；`onPartialResponseWithContext` 先 `thinking.finish()` 再 `EVENT_TOKEN` | `SimpleAssistantTest.streamToSseFinishesThinkingBeforeToolCall` / 增量用例 | ✅ |
| 思考后直接调用工具 | `SimpleAssistant.java:920-927` `beforeToolExecution` 先 `thinking.finish()` 再 `EVENT_TOOL_CALL`；`onToolExecuted` 发 `EVENT_TOOL_RESULT` | `streamToSseFinishesThinkingBeforeToolCall` | ✅ |
| 思考阶段结束时没有回答片段 | `SimpleAssistant.java:934-945` `onCompleteResponse` 先 `thinking.finish()` 再 `EVENT_DONE` | `streamToSseFinishesThinkingBeforeDoneWhenThereIsNoAnswerToken` | ✅ |
| 模型没有独立思考输出 | `ThinkingSseTracker.finish` 仅 `active` 时发事件；`delta` 才置 active | `directStreamingChatWithoutThinkingDoesNotEmitThinkingEvents` | ✅ |
| 直接流式模型回调思考增量 | `SimpleAssistant.java:372-489` `DirectTokenStream` 保存 thinking handler，`start()` 内 `onPartialThinking` 转发 | `directStreamingChatForwardsThinkingAndFinishesItBeforeAnswer` | ✅ |
| 思考配置导致 provider 请求失败 | `onError` → `EVENT_ERROR` + `completeWithError`（`SimpleAssistant.java:956-969`） | `streamToSseEmitsErrorEventWhenProviderFailsAfterThinking`（思考增量后抛错，只发 start/thinking/error，无伪造 thinking-done/done） | ✅ |

**`langchain4j-model-datasource-plugins`（MODIFIED）**

| 场景 | 实现证据 | 测试证据 | 结果 |
|---|---|---|---|
| provider 原生选项启用思考输出（Ollama） | `Langchain4jOllamaModelFactory.java:62-67` `think`/`return-thinking` 条件透传；chat-sample `application.yaml` 流式配置 `think: true`/`return-thinking: true` | `Langchain4jOllamaModelFactoryTest` + `ChatAssistantConfigTest.streamingChatModelPassesThroughThinkingOptions`（反射断言 `returnThinking` 与 `think()`） | ✅ |
| provider 没有独立思考映射 | README 能力表限制列；不提供通用 thinking 开关 | console 元数据测试断言字段与后端一致 | ✅ |
| thinking 字段与后端能力一致 | providers.js 与工厂 options 键一一对应（如 ollama `think`/`return-thinking`） | `streaming-chat-model-console-metadata.test.cjs`「元数据表与后端工厂读取的 options 键一致」「字段都有中英文文案」 | ✅ |

### 4. 集成代码审查

安排独立审查者（reviewer）对最终 diff 执行集成审查（正确性/安全/边界）：

**审查结论：无 CRITICAL；1 个 IMPORTANT + 4 WARNING + 3 SUGGESTION。**

四条硬边界核对（thinking-done 顺序 / 线程安全 / 取消语义 / 无思考不发事件）均通过：
- `thinking-done` 先于对应 token（`SimpleAssistant.java:906-908`）/ tool-call（`:922-924`）/ done（`:938-939`）发送，`ThinkingSseTracker.finish` 仅 active 时发一次。
- 取消：`cancelled`（AtomicBoolean）在所有发送点检查（`:899/:905/:921/:929/:938/:941/:958`），`sendEvent` 兜底（`:1003`）；置位后不发事件，`onDone` 仍在 finally 保存聊天记录。
- 无思考不发伪造事件：仅 `onPartialThinkingWithContext` 收到的 `PartialThinking` 触发 `delta`（`:897-901`），从不从回答文本推测思考。
- 异常路径：`onError` 发 `error` 事件 + `completeWithError`（`:956-969`），不静默改写。

**审查发现的问题与修复（verify-fail → build 修复，commit `505ff24e4`）：**

| 级别 | 问题 | 修复 |
|---|---|---|
| IMPORTANT | spec 场景「思考配置导致 provider 请求失败」无测试覆盖：`FakeTokenStream` 从不触发 errorHandler，任务 4.1「协议兼容验证」缺环 | `FakeTokenStream` 支持思考增量后 `onError` 触发；新增 `streamToSseEmitsErrorEventWhenProviderFailsAfterThinking`：断言只发 start/thinking/error（不补发伪造 thinking-done/done），completeWithError 结束 |
| WARNING 1 | `ThinkingSseTracker` 无同步，依赖回调单线程串行 | 类注释声明非线程安全 + 依赖模型回调线程串行驱动的前提 |
| WARNING 2 | `generateTextToSse` Javadoc 声称推 thinking 但实现为纯 token（历史遗留） | 修正 Javadoc：声明该路径走 `StreamingResponseHandler` 无独立 thinking 回调，思考事件由 `streamToSse` 桥接器负责 |
| WARNING 5 | AiServices 路径思考→回答 token 中间态无专门用例（direct 路径已有） | 新增 `streamToSseFinishesThinkingBeforeAnswerToken` |
| WARNING 3/4 | ASM 审计对 lambda 化回调可能漏检（保守方向）；取消语义无单测 | 记录为已知边界，不阻塞归档 |
| SUGGESTION 1/2/3 | thinking-done data 形状断言、OpenAI 分支用例、i18n hint 文案 | 可选增强，本次未实施 |

### 5. 安全检查

- 无硬编码密钥新增（Ollama 样例 `api-key: ollama` 为本地占位，非真实凭据）。
- 无新增 unsafe 操作 / 反射仅存在于测试（`ChatAssistantConfigTest.field` 为单测反射读字段）。
- SSE 取消语义：`cancelled` 置位后不再发送事件；`handleRef` 捕获 `StreamingHandle` 用于断开取消。

## 结论

- tasks 8/8 完成；两个 delta spec 全部场景均有实现证据与测试覆盖。
- 目标测试 37 个 Java 用例（SimpleAssistantTest 17 + StreamingThinkingFactoryTest 7 + OllamaModelFactoryTest 3 + StreamingThinkingAdapterAuditTest 3 + ChatAssistantConfigTest 3，verify-fail 后新增 2 个 error/中间态用例）+ 4 个 console 元数据用例全绿；build guard 14/14；verify check exit=0。
- 集成审查无 CRITICAL；唯一 IMPORTANT（error 路径测试缺环）已回 build 修复并复验（commit `505ff24e4`），WARNING 1/2/5 一并修复；WARNING 3/4 记录为已知边界。
- 遗留已知项（非本 change 引入）：TDD RED 证据缺口；部分未变更模块的既有测试环境性失败；`fw-plus-sample` 聚合器依赖其它样例模块的既有状态（本次仅修 `fw-plus-ai-magicapi-sample` POM）。