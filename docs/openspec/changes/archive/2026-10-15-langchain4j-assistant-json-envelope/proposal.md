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
- 兼容性：帧形态开关默认关，既有 Magic-API 脚本与调用方无需改动。开启后有两处可感知变化，均只在开关开启时生效：
  （1）`start` 帧 `data` 由 requestId 字符串变为对象 `{requestId, business, tokens}`，且**改到业务分类完成之后才发出**
  （比现状晚一帧），调用方传入的 `startData` 不再使用——前端应按类型分支解析 `data`；
  （2）结构化轮次不再逐字下发（没有 `token` 帧），结果在 `done` 帧一次给出。
- 行为前移（开关关闭时唯一的一处）：助手配置 `fw.plus.ai.langchain4j.assistant.*` 始终绑定并在启动期校验，
  因此即使信封开关关闭，非法的助手配置也会 fail-fast（与"启动期非法配置立即失败"的规格一致，属有意为之）。
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

- 重试污染记忆：若重试走业务 `memoryId`（`EphemeralChatMemory` + `PERSIST_NON_TOOL_FINAL_ANSWER`），
  校验错误原文会沉淀进该 `memoryId` 的历史。对策（以 Design D8/D14 为准，比本节初稿更简单）：
  结构化轮次**直连 `ChatLanguageModel`、不经 AiServices**，重试期间不写任何历史，错误只经提示词回喂；
  轮次结束才写回"用户原文 + 结果摘要"。本节初稿记的"重试使用隔离的记忆标识"未采用。
- 每轮分类的调用开销：路由只由分类决定后，每轮会多一次轻量模型调用（只判业务名）。对策：提示词极短、
  以已注册业务名做枚举约束，判定失败直接落 `chat`，不引入结果缓存（缓存会破坏“每轮重新判定”）。
- 空回复伪装成功：模型空回复时流式 mapper 收到的是 `[DONE]`，收敛件必须把它降级为 `ok=false`，
  不能当成"成功但没有 result"。
- 系统提示与 mapper 的组合限制：流式 mapper 重载固定使用默认上下文，无法承载"每轮变化的系统提示"。
  对策：固定系统提示走装配期配置，schema 指令拼进 `message`。
- 非法配置的处置：启动期非法配置 fail-fast；运行期坏配置保留旧值并告警，不中断服务。

## 拆分评估

本次不拆分为多个 change。理由是各部分是同一 capability 的实施顺序而非可独立交付的目标：
结构化输出增强（`Rule` 点号路径、`fromSchema`）是信封内层校验的直接前置依赖，路由与信封共享同一份
业务契约定义，样例依赖全部；若拆开，"成功与失败同形""永不抛"这类整体不变量会在两个 change 里各验一半，
收益低于两次 Open/Design/Build/Verify/Archive 的成本。
