---
change: langchain4j-assistant-json-envelope
design-doc: docs/openspec/changes/langchain4j-assistant-json-envelope/design.md
base-ref: e98ba52df64a716833afb0f9a1f3ace8069d153e
---

# 实施计划：通用聊天助手 JSON 信封

<!-- comet-task-authority: docs/openspec/changes/langchain4j-assistant-json-envelope/tasks.md -->

技术方案见 `design.md`（D1–D18 为决策依据）；行为契约见同目录 `specs/` 三份 delta spec。
本计划只记录范围、依赖、约束与验收，不预写实现。

## 全局约定

- **执行策略**：autonomous + **tdd**（每任务先 RED 后 GREEN，记录真实命令与结果）+ review_mode standard。
- **语言约束**：Java 8（禁 `List.of` / `Map.copyOf` / `Optional.orElseThrow()` 等 Java 9+ API）；不新增第三方依赖；源文件 UTF-8；缩进用 Tab。
- **零签名改动**：既有方法入参/出参一律不动，只允许新增方法/新增 setter/新增类；`fw-plus-core`、`fw-plus-boot`、`fw-plus-cloud` 零改动。
- **模块与包**：全部落在 `fw-plus-ai-boot-autoconfigure`；新代码在
  `src/main/java/com/xiongxiao9527/.../langchain4j/support/envelope/`
  （实际包名 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope`），测试镜像到 `src/test/java/...`。
- **命令**（层 POM 默认 `maven.test.skip=true`，必须显式打开）：
  - 定向 RED/GREEN：`cd fw-plus-ai && ./mvnw -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dmaven.test.skip=false -Dtest=<TestClass> -DfailIfNoTests=false`
  - 模块全量：`cd fw-plus-ai && ./mvnw -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dmaven.test.skip=false`
  - 供样例取新版本：`cd fw-plus-ai && ./mvnw -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure install -DskipTests`
  - 样例：`./fw-plus-core/mvnw -f fw-plus-sample/pom.xml -o -pl fw-plus-ai-samples/fw-plus-ai-structured-output-sample test -Dmaven.test.skip=false`
- **测试硬限制（已修订，见 6.4）**：`SseEmitter` 在 `streamTokenStreamToSse` 内部构造、无公开注入点，
  但**可经反射读取 `ResponseBodyEmitter.earlySendAttempts` 拿到真实 SSE 帧**（每组事件 = 前缀 String + 数据本体 + `"\n\n"`）。
  样例侧 `EnvelopeChannelSameShapeTest` 据此对真实帧逐键逐值断言；原「禁止尝试解析真实 SSE 帧、只用三处侧信道」的约定已作废。
- **既有基线**：模块现有 641 例测试必须保持全绿；开关关闭时既有行为逐字节不变。

## 第 1 组：结构化输出规则增强（无外部依赖，先做）

### 1.1 内置规则支持点号路径 <!-- comet-task-ref:803de1cb-01f2-41b7-9675-53c24c683e5b -->

- **范围**：`Rule.required/type/enumOf/range/regex` 支持 `order.buyer.name` 形式路径，错误文本含完整路径；
  顶层字段行为不变。
- **依赖**：无。
- **约束**：`Rules` 为包内工具类，点号路径的读取逻辑放包内（避免公开新 API）；缺字段仍由必填规则负责。
- **验收**：`StructuredOutputTest` 新增嵌套字段缺失/类型/枚举/范围/模式用例 + 顶层字段回归用例全绿；
  错误文本包含完整路径（如 `order.buyer.name`）。

### 1.2 从 schema 派生规则 `Rule.fromSchema` <!-- comet-task-ref:ce05b687-fd9f-42d4-838d-9e43349320cb -->

- **范围**：递归读取 schema 的 `properties` / `required` / `type` / `enum` / `minimum` / `maximum` /
  `pattern`，为嵌套字段生成点号路径规则；错误文本格式与内置规则一致。
- **依赖**：1.1（共用路径读取与错误文本格式）。
- **约束**：不能用 `JSONSchema.of(Type).toString()` 推断（实测不含 `required`），必须直接读 schema JSON；
  不支持 `anyOf`/`oneOf`/`allOf`（不进入派生，由手写规则承担）。
- **验收**：嵌套必填/枚举/范围/模式用例全绿；「派生规则与手写规则并用」时错误清单同时包含两者。

### 1.3 写明路径语义与派生限制 <!-- comet-task-ref:3999130e-984b-43dc-92ee-ffaee8d3816b -->

- **范围**：`Rule` / `Rules` 的 Javadoc 写明点号路径语义、派生能力边界与「schema 只报首错、全量清单由规则侧承担」。
- **依赖**：1.1、1.2。
- **验收**：`grep -rn "anyOf" <Rule.java Rules.java>` 能查到限制说明；模块全量测试仍全绿。

## 第 2 组：信封收敛件与配置

### 2.1 `ChatEnvelope` <!-- comet-task-ref:d3b8ec37-4307-4cef-aa9a-e13981ca5b72 -->

- **范围**：固定 5 键 `ok` / `business` / `v` / `result` / `text` 的值对象 + 静态工厂（结构化成功 / 闲聊 / 失败）
  + 转 `JSONObject`；强制 `ok=false ⇒ result=null`；`v` 为字符串、默认 `"1"`。
- **依赖**：无。
- **约束**：Java 8；不暴露可变内部状态；键名是常量而非散落字符串。
- **验收**：四条路径（结构化成功 / 失败 / 闲聊 / 空回复）单测断言键恒定与取值语义。

### 2.2 `EnvelopeAssembler` <!-- comet-task-ref:ac9de5c3-f0f7-4bee-9541-71a64fc0b203 -->

- **范围**：`assemble(...)` 生成信封、唯一闸门 `ensure()` 收敛任意输入；覆盖六类输入（合法 JSON、夹带解释文字、
  截断 JSON、纯文本、空回复 `[DONE]`、异常对象）。
- **依赖**：2.1。
- **约束**：`ensure()` 永不抛（含 `Throwable`）；空回复与 `[DONE]` 必须降级 `ok=false`；结构化成功时 `text` 为空串。
- **验收**：六类输入单测断言不抛异常且 `ok`/`result`/`text` 符合契约。

### 2.3 `Langchain4jAssistantProperties` <!-- comet-task-ref:adbe5cff-d410-4ec8-85cd-1226eb0e5a42 -->

- **范围**：`@ConfigurationProperties("fw.plus.ai.langchain4j.assistant")`，含帧形态开关（`false`）、结构化全局默认
  （`false`）、分类开关（`true`）、分类历史条数（3）、分类尝试次数（1）、结构化最大尝试次数（3）、会话窗口条数（20）；
  三层覆盖顺序 单次 > 业务 > 全局。
- **依赖**：无（可与 2.1/2.2 并行）。
- **约束**：启动期非法配置 fail-fast；运行期非法值保留旧值并告警；Javadoc 列出全部键名与默认值。
- **验收**：覆盖顺序与两类非法值处置单测全绿。

## 第 3 组：业务契约、注册表、分类与记忆

### 3.1 `BusinessContract` + `BusinessRegistry` <!-- comet-task-ref:41e73182-2d1b-45e8-b025-e92d27de7d59 -->

- **范围**：契约（名称、内层 schema、零到多条规则、结构化默认值、最大尝试次数、系统提示、契约版本 `v`）；
  注册表（注册 + 解析 + 兜底 `chat`）。
- **依赖**：2.3（契约默认值读取配置）。
- **约束**：重名注册立即失败（fail-fast）；未注册名称解析回落 `chat`，不抛异常。
- **验收**：注册成功 / 重名失败 / 未注册回落三条单测全绿。

### 3.2 分类驱动的路由 <!-- comet-task-ref:599c42c7-9898-465c-a1ec-8a380065ccae -->

- **范围**：入口只接收 `message` 与 `memoryId`；本轮业务完全取分类结果（每轮判定，不沿用上一轮）；
  未命中/失败回落 `chat`；同一会话共用一份历史，跨业务切换连续。
- **依赖**：3.1、3.3。
- **约束**：不引入分类结果缓存、不做会话-业务绑定；不提供显式指定业务的入参。
- **验收**：「相邻两轮话题不同各自判定」「切换后历史不清空」「未注册回落 `chat`」单测全绿。

### 3.3 `BusinessClassifier` <!-- comet-task-ref:f63bb1e4-fb13-44dc-855b-85df700705a3 -->

- **范围**：以已注册业务名做枚举、用 `StructuredOutput` 判定；输入 = 业务名清单 + 最近 N 条用户消息 + 本轮文本；
  默认不重试；开关关闭时全部落 `chat` 且不产生模型调用；分类调用不写记忆。
- **依赖**：3.1。
- **约束**：不复用 `LlmIntentClassifier`（枚举绑定，抽象不匹配）；模型调用经 `StructuredOutput.ChatFunction`
  注入以便假模型测试；`N=0` 表示只看本轮。
- **验收**：假模型覆盖「命中 / 未命中 / 追问轮次命中 / 开关关闭」四条路径。

### 3.4 内层结果语义与契约文档 <!-- comet-task-ref:4c7d930e-ca5d-4ded-8083-ada6845a9ea0 -->

- **范围**：`result` 顶层必须是对象（列表需包一层字段）；业务追问是 `ok=true` 的业务结果而非校验失败；
  缺字段按失败处理；`BusinessContract` Javadoc 写明业务侧如何声明与新增契约。
- **依赖**：3.1、2.2。
- **约束**：追问识别靠业务契约显式声明（规则/字段），不靠关键词猜测。
- **验收**：「列表包一层」「追问 ≠ 失败」「缺字段 ≠ 追问」单测全绿。

### 3.5 `EnvelopeSessionMemory` <!-- comet-task-ref:d35c5f9b-5122-42c4-9107-d1a945ad38d1 -->

- **范围**：自建 `ChatMemory`（`MessageWindowChatMemory` + 同一个 `ChatMemoryStore`，`id` = `memoryId`）提供
  「读历史快照 / 轮次结束写回用户原文与结果摘要（失败为失败说明）」。
- **依赖**：2.1。
- **约束**：与既有 `chat(...)` 共用同一个 store 才共享历史；重试零写入（不提供写接口给重试路径）。
- **验收**：写回内容、失败轮次写失败说明、历史不含校验错误原文三类断言全绿；
  并核实 `EphemeralChatMemory` 读路径能否看到直写消息，结论写入 Javadoc（不能则直接读 store 快照）。

## 第 4 组：入口接线

### 4.1 `EnvelopeRuntime` + 非流式入口 <!-- comet-task-ref:502fe31a-abc3-442d-9b75-dbd3a48b3a05 -->

- **范围**：编排（分类 → 取契约 → 结构化 / 非结构化分支 → 收敛）+ `EnvelopeAssistant.chat(memoryId, message)`；
  结构化轮次用直连 `ChatLanguageModel` + 消息拼接（系统提示 + 历史快照 + `schema 指令 + 本轮文本`），
  失败经 `StructuredOutput` 错误回喂重试且零记忆写入。
- **依赖**：2.2、3.1–3.5。
- **约束**：结构化轮次不使用 AiServices 的 tools/RAG/systemMessageProvider（设计明确取舍）；
  流式不做重试；失败轮次信封必须 `ok=false` 而非抛异常。
- **验收**：「首轮失败后重试成功」「达上限失败」「失败轮次 `ok=false`」单测全绿。

### 4.2 接管既有无 mapper 的 `streamToSse(...)` <!-- comet-task-ref:f88e2f3f-ce0b-4bfb-befb-d9824d56386b -->

- **范围**：`SimpleAssistant` 新增可选处理器字段与新增 setter；无 mapper 重载内前置分支（处理器为空走原路径）；
  开关开启时 `start` 帧 `data` = `{requestId, business, tokens}` 且分类完成后才发，结构化轮次不发 `token` 帧、
  `done` 帧为信封，非结构化轮次逐字下发、`done` 帧为信封。
- **依赖**：4.1。
- **约束**：**不改任何既有签名**；带 `doneDataMapper` / 带 `onStarted`/`onDone` 的重载、`streamToSseWithFilter`、
  `generateTextToSse` 一律不接管；`token` 帧始终纯文本。
- **验收**：「开关关闭时原样输出」「带 mapper/回调的重载不被接管」「withFilter/generateTextToSse 不被接管」
  「结构化轮次不发 token 帧」「流式不重试」单测全绿。

### 4.3 Spring 装配（嵌套配置类 + 安装器） <!-- comet-task-ref:c680b466-7bd0-4919-83dd-ebb75864b8f8 -->

- **范围**：既有 `LangChain4jSupportAutoConfiguration` 增加 `@EnableConfigurationProperties` 与嵌套配置类：
  注册 `EnvelopeRuntime` 与 `SmartInitializingSingleton` 安装器（`ObjectProvider<SimpleAssistant>` +
  `ObjectProvider<EnvelopeRuntime>`，两者都在才调用新增 setter，缺失静默跳过）。
- **依赖**：4.2。
- **约束**：不改既有 `@Bean` 方法签名；**不改** `spring.factories` 与 `AutoConfiguration.imports`
  （只有新增自动配置类才需要）；开关未开启时不安装处理器。
- **验收**：`ApplicationContextRunner` 断言「未开启不安装 / 开启后既有 Bean 被装上」；既有自动配置测试与
  `AutoConfigurationMetadataConsistencyTest` 全绿。

## 第 5 组：样例与集成验证

### 5.1 样例两假业务端到端 <!-- comet-task-ref:22363fcc-9fea-4751-8b52-6079087d3fe5 -->

- **范围**：样例模块新增两个假业务契约 + 假模型（同一 `RecordingChatModel` 同时承担分类与生成），
  离线零 Key 覆盖四条路径（命中且结构化成功 / 命中但校验失败 / 业务追问 / 未命中落闲聊）。
- **依赖**：4.3 且已 `install` 供样例取到新版本。
- **约束**：样例不依赖任何真实模型或网络；断言结构化轮次不发 `token` 帧、`start` 帧含 `tokens`。
- **验收**：样例模块 `test` 目标通过。

### 5.2 两条通道同形断言 <!-- comet-task-ref:78c61a0b-0c2b-438d-b86c-9731e3030b3f -->

- **范围**：同一轮次分别取流式 `done` 信封（`doneDataMapper`/`onDone` 侧信道）与非流式返回，比较键结构 /
  `ok` / `business` / `result` / `v`。
- **依赖**：5.1。
- **约束**：不解析真实 `SseEmitter` 帧（内部构造、无注入点），把该限制写进测试类注释。
- **验收**：同形断言通过。

### 5.3 回归与证据 <!-- comet-task-ref:c654c4e4-994c-49c2-bded-59382ec2317f -->

- **范围**：模块全量测试（既有 641 例 + 新增）全绿；`install` 后用新版本复跑样例。
- **依赖**：5.2。
- **验收**：两条命令都通过；随后录制 build 证据并跑 `comet guard langchain4j-assistant-json-envelope build --apply`。

## 第 6 组：Verify 修复轮（最终集成审查 NEEDS-FIX 后回 Build 修复）

> 依据：Verify 阶段独立集成审查结论 NEEDS-FIX（1 IMPORTANT + 4 WARNING + 2 SUGGESTION），
> `comet state transition ... verify-fail` 后回本阶段修复；失败计数 `verify_failures=1`（未到 3 次上限）。

### 6.1 单次调用层产品入口（I-1 必修） <!-- comet-task-ref:c3987a65-b179-4500-a21e-09da7adf9626 -->

- **范围**：`EnvelopeRuntime.decide(memoryId, message, Boolean)` 与 `EnvelopeAssistant.chat(memoryId, message, Boolean)`
  两个新增入口，把 `callOverride` 传进 `resolveEnabled`；现有 2 参重载委托 `null`，签名与行为不变。
- **依赖**：无。
- **约束**：零签名改动（只新增重载）；`contract == null` 时单次开启也不产出 result（没有契约即没有 schema）；
  单次覆盖只在非流式信封入口提供，流式接管路径形态由全局/契约决定（既有签名不可加参）。
- **验收**：TDD RED（新入口符号未解析）→ GREEN；`EnvelopeAssistantTest` 新增两例覆盖「契约开→单次关」与
  「契约关→单次开」，模块全量 `test` 全绿。

### 6.2 接管边界措辞精确化（W-1，用户已选定方案） <!-- comet-task-ref:69774a43-ed83-4adb-95f9-ec7b3f22d54c -->

- **范围**：delta spec 接管条款写明两个接管重载 + 明确排除自带 `context` 的重载并给理由；同步 D12 与
  `SimpleAssistant#envelopeRuntime` 的「不接管」清单。
- **依赖**：无。
- **约束**：行为不变；措辞精确化与已批准的 D12 白名单一致，不改变接管范围本身。
- **验收**：spec / design / 代码注释三处口径一致，无新增行为变化。

### 6.3 并发与装配加固（W-2/W-3） <!-- comet-task-ref:17e8edcd-8848-4c2d-8c1c-f52bb0e5b081 -->

- **范围**：`envelopeRuntime` 字段加 `volatile`；`EnvelopeSessionMemory` Javadoc 注明同一 `memoryId` 轮次需串行。
- **依赖**：无。
- **验收**：编译与既有测试全绿（加固本身不可断言，说明写入注释）。

### 6.4 文档同步（W-4/S-1/S-2 + 过时表述） <!-- comet-task-ref:ddaf372b-4b92-4c8d-8a7a-6e637865a114 -->

- **范围**：design 组件表 `AssistantBuilder` 行改为实际装配方式；design 测试策略「拿不到真实帧」段落、
  本计划「测试硬限制」约定与 `tasks.md` 5.2 措辞同步为反射读真实帧方案；design/proposal 补记
  `start` 帧时序变化与开关关闭时配置校验仍前移的说明。
- **依赖**：6.1–6.3。
- **验收**：文档与实现/规格口径一致，构建证据重跑通过。



- **回退路径**：帧形态开关默认关，关闭即回到现状，无需回滚代码；应急可只关分类开关（全部落 `chat`）。
- **高不确定点**：`EphemeralChatMemory` 读路径的可见性（任务 3.5 核实，兜底为直接读 store 快照）。
- **提交策略**：guard 通过后再提交；按「结构化输出工具类与规则增强」「信封、编排、接管与样例」两段拆分，
  既有未提交产出随本 change 一并交付。
