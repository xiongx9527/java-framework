# Tasks

## 1. 结构化输出规则增强（规则带路径 + 从 schema 派生）

- [x] 1.1 为内置校验规则（必填 / 类型 / 枚举 / 数值范围 / 字符串模式）增加点号路径支持，错误文本包含完整路径；扩展 `StructuredOutputTest` 覆盖嵌套字段与顶层字段回归，`cd fw-plus-ai && ./mvnw -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dmaven.test.skip=false` 全绿 <!-- comet-task:803de1cb-01f2-41b7-9675-53c24c683e5b -->
- [x] 1.2 实现从 JSON Schema 派生校验规则（`Rule.fromSchema`）：递归读取 `properties` / `required` / 类型 / 枚举 / 数值范围 / 字符串模式，为嵌套字段生成点号路径规则；测试覆盖嵌套必填、枚举、范围、模式，以及“派生规则与手写规则并用”时错误清单同时包含两者，命令同上 <!-- comet-task:ce05b687-fd9f-42d4-838d-9e43349320cb -->
- [x] 1.3 在 `Rule` / `Rules` 的 Javadoc 写明点号路径语义与派生限制（不支持 `anyOf` / `oneOf` / `allOf`，schema 校验只报首个错误，全量清单由规则侧承担）；用 `grep -rn "anyOf" <文件>` 确认限制已写进文档 <!-- comet-task:3999130e-984b-43dc-92ee-ffaee8d3816b -->

## 2. 信封收敛件与配置

- [x] 2.1 新增 `ChatEnvelope`：固定 5 键 `ok` / `business` / `v` / `result` / `text`，成功与失败同形，强制 `ok=false ⇒ result=null`；`v` 为字符串、默认 `"1"`、可被业务契约覆盖；单测断言四个取值路径（结构化成功 / 失败 / 闲聊 / 空回复）的键恒定与取值语义 <!-- comet-task:d3b8ec37-4307-4cef-aa9a-e13981ca5b72 -->
- [x] 2.2 新增 `EnvelopeAssembler`：`assemble(...)` 与唯一闸门 `ensure()`，覆盖合法 JSON、夹带解释文字的 JSON、被截断的 JSON、纯文本、空回复（`[DONE]`）、异常对象六类输入；单测断言收敛过程不抛异常，且各路径 `ok` / `result` / `text` 取值符合契约 <!-- comet-task:ac9de5c3-f0f7-4bee-9541-71a64fc0b203 -->
- [x] 2.3 新增 `Langchain4jAssistantProperties`（前缀 `fw.plus.ai.langchain4j.assistant`）：帧形态开关（默认 `false`）、结构化全局默认（`false`）、分类开关（默认 `true`）、分类历史条数（默认 3）、分类尝试次数（默认 1）、结构化最大尝试次数（默认 3）、会话窗口条数（默认 20）；启动期非法配置 fail-fast，运行期非法配置保留旧值并告警；单测覆盖三层覆盖顺序（单次 > 业务 > 全局）与两类非法值处置，并在 Javadoc 写明全部配置键与默认值 <!-- comet-task:adbe5cff-d410-4ec8-85cd-1226eb0e5a42 -->

## 3. 业务契约、注册表、分类与记忆

- [x] 3.1 新增 `BusinessContract`（名称、内层 schema、零到多条规则、结构化默认值、最大尝试次数、系统提示、契约版本 `v`）与 `BusinessRegistry`：注册阶段同名冲突立即失败，解析未注册名称回落兜底 `chat`；单测覆盖注册成功、重名失败、未注册回落 <!-- comet-task:41e73182-2d1b-45e8-b025-e92d27de7d59 -->
- [x] 3.2 实现分类驱动的路由：入口只接收 `message` 与 `memoryId`，本轮业务完全取分类结果（每轮判定、不沿用上一轮），未命中或判定失败回落 `chat`；同一会话共用一份历史，跨业务切换保持连续；单测覆盖“相邻两轮话题不同各自判定”“切换后历史不清空”“未注册回落 `chat`” <!-- comet-task:599c42c7-9898-465c-a1ec-8a380065ccae -->
- [x] 3.3 实现 `BusinessClassifier`：以已注册业务名作为枚举、用 `StructuredOutput` 判定；输入含最近 N 条用户消息（可配，0 = 只看本轮）以使追问轮次仍能命中；默认每次判定不重试；分类开关关闭时所有轮次落 `chat` 且不产生分类调用；分类调用不写任何记忆；单测用假模型覆盖命中、未命中、追问轮次命中、开关关闭四条路径 <!-- comet-task:f63bb1e4-fb13-44dc-855b-85df700705a3 -->
- [x] 3.4 落实内层结果语义：`result` 顶层必须是对象（列表需包一层字段）、业务追问是 `ok=true` 的业务结果而非校验失败、缺字段按失败处理；单测覆盖“列表包一层”“追问 ≠ 失败”“缺字段 ≠ 追问”，并在 `BusinessContract` 的 Javadoc 写明契约声明方式（业务侧如何使用、如何新增一份契约） <!-- comet-task:4c7d930e-ca5d-4ded-8083-ada6845a9ea0 -->
- [x] 3.5 新增 `EnvelopeSessionMemory`：自建 `ChatMemory`（`MessageWindowChatMemory` + 同一个 `ChatMemoryStore`，`id` = `memoryId`）提供“读历史快照 / 轮次结束写回用户原文与结果摘要”；单测断言写回内容、失败轮次写失败说明、会话历史中不出现校验错误原文；并核实 `EphemeralChatMemory` 读路径能否看到直写消息（不能则直接读 store 快照，把结论写进 Javadoc） <!-- comet-task:d35c5f9b-5122-42c4-9107-d1a945ad38d1 -->

## 4. 入口接线（编排、非流式入口、既有流式入口接管）

- [x] 4.1 新增 `EnvelopeRuntime` 编排件（分类 → 取契约 → 结构化/非结构化分支 → 收敛）与 `EnvelopeAssistant.chat(memoryId, message)` 非流式入口：结构化轮次用直连 `ChatLanguageModel` + 消息拼接（系统提示 + 历史快照 + `schema 指令 + 本轮文本`），失败经 `StructuredOutput` 的错误回喂重试且零记忆写入；单测覆盖首轮失败后重试成功、达上限失败、失败轮次信封为 `ok=false` <!-- comet-task:502fe31a-abc3-442d-9b75-dbd3a48b3a05 -->
- [x] 4.2 接管既有无 mapper 的 `streamToSse(...)` 重载：新增可选处理器字段与新增 setter，在无 mapper 重载内前置分支（处理器为空则原路径不变）；开关开启时 `start` 帧 `data` 为 `{requestId, business, tokens}` 且分类完成后才发，结构化轮次不发 `token` 帧、`done` 帧为信封，非结构化轮次逐字下发、`done` 帧为信封；单测断言开关关闭时原样输出、带 `doneDataMapper` 与带回调的重载及 `streamToSseWithFilter` / `generateTextToSse` 不被接管、`token` 帧始终纯文本、流式不重试 <!-- comet-task:f88e2f3f-ce0b-4bfb-befb-d9824d56386b -->
- [x] 4.3 在既有 `LangChain4jSupportAutoConfiguration` 增加嵌套配置类：注册 `EnvelopeRuntime` 与一个 `SmartInitializingSingleton` 安装器（注入 `ObjectProvider<SimpleAssistant>` + `ObjectProvider<EnvelopeRuntime>`，两者都在才调用新增 setter，缺失则静默跳过）；不改任何既有 `@Bean` 方法签名、不改 `spring.factories` / `AutoConfiguration.imports`；用 `ApplicationContextRunner` 断言开关未开启时不安装处理器、开启时既有 `SimpleAssistant` Bean 被装上且既有自动配置测试与一致性测试仍全绿 <!-- comet-task:c680b466-7bd0-4919-83dd-ebb75864b8f8 -->

## 5. 样例与集成验证

- [x] 5.1 样例模块新增两个假业务契约与假模型（同一 `RecordingChatModel` 同时承担分类与生成），离线（零 API Key）覆盖四条路径：分类命中且结构化成功、分类命中但校验失败、业务追问、分类未命中落闲聊；断言结构化轮次不发 `token` 帧、`start` 帧含 `tokens`；`./fw-plus-core/mvnw -f fw-plus-sample/pom.xml -o -pl fw-plus-ai-samples/fw-plus-ai-structured-output-sample test -Dmaven.test.skip=false` 通过 <!-- comet-task:22363fcc-9fea-4751-8b52-6079087d3fe5 -->
- [x] 5.2 断言同一轮次的两条通道同形：流式 `done` 帧的信封与非流式入口返回的信封在键结构、`ok`、`business`、`result`、`v` 上一致；实际做法强于初版计划——反射读取 `ResponseBodyEmitter.earlySendAttempts` 取**真实 SSE 帧**逐键逐值比较，初版"不解析真实帧、只用侧信道"的约定已由 Build 阶段独立审查（M1）证伪并作废（措辞于 Verify 修复轮 6.4 同步） <!-- comet-task:78c61a0b-0c2b-438d-b86c-9731e3030b3f -->
- [x] 5.3 跑该模块全量测试确认既有 641 例未受影响（尤其开关关闭时的既有行为），并执行 `cd fw-plus-ai && ./mvnw -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure install -DskipTests` 使样例取到新版本后复跑样例 <!-- comet-task:c654c4e4-994c-49c2-bded-59382ec2317f -->

## 6. Verify 修复轮（最终集成审查 NEEDS-FIX：1 IMPORTANT + 4 WARNING + 2 SUGGESTION）

- [x] 6.1 补齐结构化开关「单次调用」层的产品入口（审查 I-1，必修）：新增 `EnvelopeRuntime.decide(memoryId, message, Boolean structuredOverride)` 与 `EnvelopeAssistant.chat(memoryId, message, Boolean structuredOverride)`，把单次覆盖透传进 `resolveEnabled`（现有 2 参重载委托 `null`、行为不变），使「单次调用 > 业务契约 > 全局」三层在规格中承诺的覆盖顺序对产品入口可达；TDD 先 RED 后 GREEN，覆盖「契约开启时单次关闭 → 该次调用不产出 result」「契约关闭时单次开启 → 该次调用产出 result」两条；`cd fw-plus-ai && ./mvnw -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dmaven.test.skip=false` 全绿 <!-- comet-task:c3987a65-b179-4500-a21e-09da7adf9626 -->
- [x] 6.2 接管边界措辞精确化（审查 W-1，用户已选定方案）：delta spec `langchain4j-chat-envelope/spec.md` 的接管条款写明接管的两个既有重载与「调用方自带 context 的重载不接管」及理由，并补一条同名 Scenario；同步 `design.md` D12 与 `SimpleAssistant#envelopeRuntime` Javadoc 的「不接管」清单；补边界锁定断言 `SimpleAssistantEnvelopeTest#contextOverloadIsNotTakenOver`（当前实现已满足，无 RED） <!-- comet-task:69774a43-ed83-4adb-95f9-ec7b3f22d54c -->
- [x] 6.3 并发与装配加固（审查 W-2/W-3）：`SimpleAssistant.envelopeRuntime` 字段加 `volatile`；`EnvelopeSessionMemory` Javadoc 注明同一 `memoryId` 的轮次需调用方串行（底层 store 为读改写、非原子） <!-- comet-task:17e8edcd-8848-4c2d-8c1c-f52bb0e5b081 -->
- [x] 6.4 文档同步（审查 W-4/S-1/S-2 与恢复阶段发现的过时表述）：`design.md` 组件与文件清单中 `AssistantBuilder` 一行改为实际装配方式；`design.md` 测试策略「拿不到真实 SSE 帧」段落、计划「测试硬限制」约定与本文件 5.2 措辞同步为「反射读真实帧」方案；`design.md` 与 `proposal.md` 补记开关开启时 `start` 帧时序变化、开关关闭时助手配置仍绑定并校验的行为前移；`proposal.md` 风险节「隔离记忆标识」对策同步为实际采用的「直连模型、重试零写入」；`SimpleAssistantEnvelopeTest` 类注释中的过时结论一并订正 <!-- comet-task:ddaf372b-4b92-4c8d-8a7a-6e637865a114 -->

## 7. Verify 第二轮修复（第 2 次针对性复核发现的文档偏差与覆盖补充）

> 依据：第 2 次针对性独立复核结论 PASS（无 CRITICAL/IMPORTANT），但发现 3 项第 6 组修复轮自己引入的
> 文档瑕疵 + 1 项覆盖缺口；按用户指示"修复未完成的问题"回 Build 处置。`verify_failures=2`（上限 3）。

- [x] 7.1 接管白名单措辞补全（复核 WARNING #1，使 6.2 的"三处口径一致"验收真正达成）：delta spec `langchain4j-chat-envelope/spec.md` 与 `design.md` D12 的白名单由"两个重载"改为"三个重载"，补上单参入口 `streamToSse(String message)`（内部委托 `streamToSse(memoryId, message)`、实际同样被接管，测试 `switchOnTakesOverSingleArgOverload` 与 `SimpleAssistant` Javadoc 均已按接管描述），并新增同名 Scenario「单参重载委托后的形态」；行为不变 <!-- comet-task:2d267093-f796-4ebf-be48-d0c210b0c688 -->
- [x] 7.2 计划文件修正（复核 WARNING #2 + SUGGESTION #3）：`docs/superpowers/plans/2026-10-14-langchain4j-assistant-json-envelope.md` 5.2 正文由过时的「`doneDataMapper`/`onDone` 侧信道 + 不解析真实帧」改为与同文件顶部约定及 `tasks.md` 5.2 一致的「反射读 `ResponseBodyEmitter.earlySendAttempts` 真实帧」；恢复被误替换掉的 `## 风险与回退` 标题（其下三条 bullet 现悬空在 6.4 之下） <!-- comet-task:4cb1673d-9daa-411a-81e2-16984deda06f -->
- [x] 7.3 补覆盖缺口（复核 SUGGESTION #4）：新增一条用例证明「无契约注册（`contract == null`）+ 单次调用 `TRUE`」仍不产出 `result`（兜底 `chat` 轮次，没有契约就没有可校验的 schema），避免该 Javadoc 承诺仅靠单一布尔表达式保证；覆盖既有短路语义，无 RED <!-- comet-task:73d8bd88-d6c2-4b99-97c8-883d01f953eb -->

## Workflow follow-up

- 进入 Verify 阶段验证实现与产物一致，并记录验证报告。
- 归档本 change，把三份 delta spec 合入 `docs/openspec/specs/` 对应 capability。
- 按用户确认的提交拆分落库（结构化输出工具类与规则增强；信封、编排、接管与样例）。
