# Brainstorm Summary

- Change: langchain4j-assistant-json-envelope
- Date: 2026-10-14
- 状态：**已由用户在 Design Step 1c 正式确认**（2026-10-14）；下方「已确认选择」与「确认的技术方案」即为最终设计依据。

## 已确认选择（本轮 4 问，全部单选）

| 编号 | 问题 | 用户选择 |
| --- | --- | --- |
| Q1 | 帧形态开关（`envelope.enabled`）控制谁 | **控制既有入口**：开关开启后，既有无 mapper 的 `streamToSse(...)` 重载把 `done` 帧换成信封；带 mapper/回调的重载不被接管；关闭时一字不变 |
| Q2 | `start` 帧是否携带轮次形态 | **携带**：开关开启时 `start` 帧 `data` 改为对象 `{requestId, business, tokens}`，且分类完成后才发 |
| Q3 | 结构化轮次与会话历史的关系 | **读+写**：结构化轮次读写同一份会话记忆（指向同一个 `ChatMemoryStore`），轮次结束写回「用户原文 + 结果摘要」；重试不写任何记忆 |
| Q4 | 分类调用的输入 | **本轮文本 + 最近 N 条用户消息**（默认 3，可配，只读） |

## 确认的技术方案

### 通道与入口

- **流式信封**：接管既有无 mapper 的 `streamToSse(memoryId, message[, startData])`（内部加分支，**不改任何签名**）。开关关闭 → 完全走现状代码路径。
- **非流式信封**：新增 `EnvelopeAssistant.chat(memoryId, message)`（新类，不改既有 `chat(...)` 返回类型）。
- 不接管：带 `doneDataMapper` / `onStarted` / `onDone` 的重载（尊重"我要自己决定 done 内容"）、`streamToSseWithFilter(...)`（过滤检索语义不同）、`generateTextToSse(...)`（显式直连补全）。

### 每轮流程（流式与非流式共用同一编排）

1. **分类**（每轮一次，不沿用上轮）：直连 `ChatLanguageModel.generate(List<ChatMessage>)`，输入 = 分类系统提示（已注册业务名清单）+ 最近 N 条用户消息 + 本轮文本；用 `StructuredOutput` 校验 `{"business": <enum>}`；未命中/校验失败/开关关 → `chat`。
2. **取契约**：命中已注册业务 → 用其 schema/规则；未注册或 `chat` → 兜底。
3. **分支**：
   - **结构化轮次**（命中业务且结构化开关为真）：`start` 帧 `tokens=false` → 非流式结构化生成（直连 + 消息拼接：系统提示 + 历史快照 + `schema 指令 + 本轮文本`）→ 双层校验 → 失败回喂重试（最多 N 次，**零记忆写入**）→ `done` 帧信封。
   - **非结构化轮次**（`chat` 或命中但结构化关）：走既有 AiServices 流式（有记忆），`token` 帧逐字下发 → `done` 帧信封（`business` 仍为契约名、`result=null`、`text` 为正文）。
4. **收敛**：两条通道共用同一个 `assemble(...)`，产出 5 键信封，**永不抛**；异常/空回复/截断 → `ok=false`、`result=null`。

### 关键实现取舍

- **`business` 与结构化开关正交**：`business` 只由分类决定，`result` 只由结构化开关（单次 > 业务契约 > 全局默认 false）决定。
- **记忆读写口**：envelope 层自建 `ChatMemory`（`MessageWindowChatMemory` + 同一个 `ChatMemoryStore`），与既有 `chat(...)` 历史天然共用；结构化轮次读快照、轮次结束写回「用户原文 + 结果摘要」。
- **重试零写入**：结构化轮次不经 AiServices，因此重试天然不写记忆——比"隔离 memoryId + 用完删除"更简单且无泄漏（同一保证，更少机制）。
- **结构化轮次不走 AiServices**：该轮不使用 tools / RAG / `systemMessageProvider`；系统提示由业务契约或全局默认提供，schema 指令拼进用户消息。
- **分类器自己写**（`BusinessClassifier`）：既有 `LlmIntentClassifier` 绑定 Java 枚举，业务名是运行时注册的字符串，抽象不匹配；两者共用 `StructuredOutput` 作内核，不新增第二套引擎。
- **配置前缀**：`fw.plus.ai.langchain4j.assistant.*`；properties 与 runtime 挂在既有 `LangChain4jSupportAutoConfiguration`（新增 `@Bean` 与 `@EnableConfigurationProperties`，**无需改注册文件**）。
- **装配**：`SimpleAssistant` 新增一个可选处理器字段 + 新增 setter（additive），内部只在无 mapper 重载里分支；Spring 侧用一个 `SmartInitializingSingleton` 安装器把 runtime 装到既有 `SimpleAssistant` Bean 上，**不改任何既有方法签名**。

## 关键取舍与风险

- `start` 帧 `data` 形态变化（仅开关开启时，opt-in）：既有前端若把 `data` 当 requestId 解析会受影响。
- 首帧延后一次轻量分类调用；每轮多一次模型调用 + 最近 3 条用户消息。
- `EphemeralChatMemory` 读路径能否看到直写 store 的消息，需实现期核实；不能则直接读 store 快照。
- 结构化轮次无 tools/RAG/systemMessageProvider（明确取舍）。
- **测试限制**：`SseEmitter` 内部 `new SseEmitter(0L)`，无注入点 → 无法断言真实帧。帧内容不变量收敛为「mapper 输出单测 + `onStarted`/`onDone` 回调 + 交互校验」，不解析真实帧。
- 纯 Java（非 Spring）接入时，非流式入口需与 `AssistantBuilder` 共用同一个 `ChatMemoryStore` 才能与既有 `chat(...)` 共享历史；否则历史分叉。

## 测试策略

- **单测**：`EnvelopeAssembler`（六类输入 + 永不抛 + 取值语义）、`Rule`/`Rules`（点号路径 + `fromSchema`，扩既有 20 例）、`BusinessRegistry`（重名 fail-fast / 未注册回落）、`BusinessClassifier`（命中 / 未命中 / 开关关）。
- **假模型集成**（零 Key）：`RecordingChatModel implements ChatLanguageModel`（脚本化「分类调用 → 生成调用」序列，重试再追加）+ 假 `StreamingChatLanguageModel`（实现 `generate(List, handler)`，发若干 token 后 `onComplete`）。
- **通道同形**：同一轮次分别取流式 mapper 输出与非流式入口返回，比较键结构 / `ok` / `business` / `result`。
- **样例端到端**：两假业务离线覆盖四条路径（命中成功 / 命中但校验失败 / 业务追问 / 未命中落闲聊）。
- **回归**：模块既有 641 例全绿；开关关闭时既有输出与交互断言不变。

## Spec Patch

用户确认后回写（只补验收场景/纠偏措辞，不改结构）：

1. `langchain4j-chat-envelope`｜「帧协议保持不变」→ 改为「`token` 帧保持纯文本；承载信封的只有 `done` 帧 `data`；**开关开启时 `start` 帧 `data` 为 `{requestId, business, tokens}`，关闭时保持现状**」，并补 scenario（结构化轮次 `start` 帧 `tokens=false`）。
2. `langchain4j-chat-envelope`｜「帧形态开关只可全局配置且默认关闭」→ 明确作用面：接管既有无 mapper 的 `streamToSse(...)`；带 mapper/回调的重载与 `streamToSseWithFilter(...)` 不被接管；补 scenario。
3. `langchain4j-chat-envelope`｜补 scenario：业务命中但结构化开关关 → 信封 `business` 为契约名、`result=null`、`text` 为正文、逐字流式。
4. `langchain4j-assistant-business-routing`｜「模型分类判定」补 scenario：分类输入含最近 N 条用户消息，追问轮次（不含业务信号）仍能命中业务。
5. `langchain4j-assistant-business-routing`｜「对话历史共用」补 scenario：结构化轮次结束后会话历史含本轮用户原文与结果摘要，且不含校验错误原文。
