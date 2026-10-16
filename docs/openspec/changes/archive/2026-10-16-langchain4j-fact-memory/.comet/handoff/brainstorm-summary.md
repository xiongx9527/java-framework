# Brainstorm Summary

- Change: langchain4j-fact-memory
- Date: 2026-10-16
- 状态：**已确认**（Comet Classic Step 1c 用户确认，2026-10-16）

## 确认的技术方案

- 新增 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts` 包：
  - `FactStore`（接口：addDialogue / addFacts / search / listAll）+ `Mem0FactStore`（默认实现，复用 `Mem0RestClient`——仅放宽访问修饰符，方法签名与行为不变）
  - `FactExtractor` + `FactExtractors.REGISTRY`：`mem0`（默认）/ `llm` / `rule` / `hybrid`，4 策略全实现，形态照 `Summarizers.REGISTRY`
  - `FactWriteMode`：`per-turn`（默认）/ `batched` / `session-end`，3 模式全实现（含会话轮次缓冲与懒 flush）
  - `FactRetriever` + `FactRetrievers.REGISTRY`：`semantic`（默认）/ `keyword` / `hybrid` / `rerank`，4 策略全实现
  - `FactContextProvider`（注入链第 ② 段，位于会话摘要之前）
  - `FactMaintenance`（轮末触发、按写入模式调度、异常隔离）+ `FactWriteScheduler`（异步执行）
  - `FactMemoryAssembler`（装配收口；在 `SummaryMemoryAssembler` 之前调用以固定顺序）
- `ContextMaintenance` 纯增量扩展：新增 3 参 default 方法（携带本轮新增消息）；`ContextAwareChatMemory` 写路径改调 3 参、读时兜底不触发抽取；既有签名零改动，`SummaryMaintenance` 行为不变。
- 写路径：轮末触发（最终 AiMessage，跳过工具调用中间态）；输入为本轮 [用户消息, AI 回复] 真实文本（禁止 `{"seq","msg"}` 包装串）；作用域仅显式用户段（段[3]）。
- 读路径：`FactContextProvider` 以本轮输入为查询；semantic=mem0 `POST /search`；keyword=`GET /memories` + 本地评分；hybrid=RRF 融合；rerank=ScoringModel（缺省退化为关键词重叠）；事实段上限 `max-facts-tokens=500`；顺序 人设→事实→摘要→最近 N 条→本轮输入。
- 装配：autoconfigure 注册默认 `Mem0FactStore` Bean（`session.facts.enabled=true` 且无自定义 Bean；base-url 缺失→启动快速失败）；`applySessionMemory` 中先装配事实、再装配摘要。
- mem0 契约（OSS 无 `/v1` 前缀）：抽取 `POST /memories`（`infer=true`、无 `run_id`）；直存 `POST /memories`（`infer=false`、逐条）；检索 `POST /search`（query/user_id/top_k，字段名实施时实测校准）；列表 `GET /memories?user_id=`。

## 用户确认的默认值（Q2 / Q3）

- `facts.enabled` 默认 **false**（opt-in；开启而 base-url 缺失时启动快速失败）。
- 抽取默认**全异步**（`extract-async: true`；后台 daemon 线程 + 有界队列，队列满丢弃并告警）；可显式置 false 改为请求线程内联。

## §12 未决项决议

1. 事实 query = 本轮用户输入（不拼接历史）。
2. `top-k`=5；事实段预算 500 tokens（摘要段 800，各自软上限、互不抢占）。
3. 抽取执行默认：全异步（见上）。
4. `ContextStrategy` / session 规则：沿用 change ① 已落地值（`memory-and-enriched` / `daily`）。

## 与既有文档的差异

- `merge-policy` 不在 v1 落地：mem0 `infer=true` 服务端自带合并/去重，直存路径由 mem0 哈希去重兜底；spec 与任务均未要求该项。

## 关键取舍与风险

- 事实通道依赖独立 mem0-B（含服务端 LLM）：无 LLM 环境退化为 `rule`（配置显式切换，不自动降级）。
- 钩子扩展为纯增量（default 方法 + 私有方法多传一个参数）：既有签名零改动；`SummaryMaintenance` 不受影响。
- mem0 OSS `POST /search` 字段名（top_k/limit 等）以真实部署实测校准，并固化进契约测试。
- 异步/缓冲为 best-effort：进程退出可能丢在途任务与内存缓冲（文档声明）；session-end 模式在会话切换时才懒写入。
- 读路径 token 增量受 `max-facts-tokens` 限制；写路径失败不产生任何用户可见影响。

## 测试策略

- 单测：fake HttpServer（复用 `Mem0ChatMemoryStoreTest` 范式）断言 `infer=true` / 无 `run_id` / 无包装串；4 抽取、3 模式、4 召回各单测；`rule` 与 `keyword` 零外部依赖跑通；失败隔离；顺序与 token 上限；属性默认值与配置样例。
- 集成：s1 沉淀 → s2 召回；事实不污染消息通道 `getMessages`；关闭 facts 回归。
- 回归命令：`cd fw-plus-ai && ./mvnw -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dmaven.test.skip=false`

## Spec Patch

已确认并回写：为「事实写入模式可配置」补充边界场景「会话结束的判定」——同一用户在后续请求中出现新会话标识即判定上一会话已结束并集中写入；会话进行中不写入。仅补充定义，不改 delta spec 结构。
