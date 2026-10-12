# nl2sql-core 完整目标规格（Shape 产物）

> **文档角色**：本文档是 nl2sql change 下 capability `nl2sql-core` 的完整目标规格，承接 `brief.md` 的「验收示例 → 问数核心（nl2sql-core）」小节与 D5/D6/D9/D12/D15/D16/D23/D24/D28/D30/D34/D34a/D40/D41/D44/D45 决策。实现后本 capability 的行为契约以本文为准；不重复物理定稿可使其退化。

问数核心（nl2sql-core）交付「编排即助手」形态的问数引擎：用户以自然语言提问，系统经意图识别 → 指代改写 → 表匹配 RAG → SQL 生成（ReAct 自愈）→ 只读校验 → 数据权限回调 → 执行 → 解读/图表配置 的链路产出结果。实现使用 `langchain4j-agentic:1.20.0-beta30`（SequenceAgent 主链 + ConditionalAgent 意图分支 + LoopAgent/ErrorHandler 自愈）与**自建执行层**（Druid 连接池 + 轻量 SqlExecutor + 方言适配，不依赖 magic-api 引擎），SQL 生成自愈环节采用 **ReAct agent**（只读工具自主查证修正）。编排包装为 `SimpleAssistant` 同构实例（D30），`chat(message)` 与 `streamToSse(...)` 复用现有助手通道；每个问数实例对应一个独立问数助手实例（数据源/向量库/LLM/权限策略各自绑定，D8/D30）。

## 1. 能力范围

- 主流程：意图识别（查数/闲聊/非数据）→ 闲聊分支 / 查数分支。
- 查数分支：多轮指代改写 → 关键词提取与术语扩展 → 表匹配 RAG（向量分数 + 关键词融合，α 加权，D6）→ SQL 生成（ReAct 自愈，D5）→ JSON 契约解析 → 只读校验 → 数据权限回调 → 行数限制注入 → （确认模式：SQL 预览/EXPLAIN 预检）→ 执行 → 结果解读与图表配置。
- 跨轮协议：JSON 输出契约、regenerate 自愈、人工调整 SQL 重执行（D44）、行数限制（默认 1000，禁入子查询）、会话上下文裁剪、限流配额、多实例隔离、OpenTelemetry 全链路观测。
- 不负责：管理端界面（nl2sql-admin）、数据权限策略后台（nl2sql-security，仅衔接其回调 SPI）、用户端 UI（nl2sql-user-ui）、图表渲染实现（nl2sql-chart-dashboard）、MCP/Excel/嵌入（nl2sql-integrations）——但本 capability 的编排产物须以可复用助手语义供这些 capability 调用。

## 2. 编排装配

- agentic 使用收口到核心编排模块少量类（隔离层）：`Nl2SqlOrchestrator`（主链 SequenceAgent）、`IntentRouter`（ConditionalAgent）、`ChitchatAgent`（闲聊分支）、`SqlGenerationReactAgent`（ReAct 自愈）、`Nl2SqlRegenerateLoop`（LoopAgent/ErrorHandler）。必要时可退化为手写编排（不变量）。
- 主链为 SequenceAgent 结构，节点：`IntentRecognizeNode` → `ChitchatOrQueryBranch(ConditionalAgent)` → 查数分支内 `RewriteQueryNode` → `MatchTablesNode` → `GenerateSqlNode(ReAct)` → `ValidateNode` → `SecurityCallbackNode` → `ConfirmGateNode` → `ExecuteNode` → `InterpretNode`。
- 每个问数实例持有独立装配：数据源（Druid 连接池自建，D23）、向量库客户端 + EmbeddingModel（复用框架现有 embedding-model/embedding-store 资源 key，D10）、ChatModel（langchain4j 工厂自建，D24）、权限回调、闲聊配置、方言、模型参数。实例间无共享可变状态（多实例隔离）。
- 配置来源为共享配置存储（D24），API 服务按管理端配置自建实例（D27：配置变更 → 重建实例 → 旧实例关闭）。
- ChatModel/EmbeddingModel 仅经 langchain4j 工厂构建，不依赖 magic-api；连接池与 SqlExecutor 不依赖 magic-api 引擎（D23）。

## 3. 主流程

### 3.1 意图识别

- ConditionalAgent 将用户输入分类为三类：`query`（数据查询）、`chitchat`（闲聊）、`non-data`（与数据无关但非闲聊，如无法归属数据的请求）。判定可用轻量 LLM prompt 或规则 + LLM 兜底，分类结果随审计记录。
- `non-data`：不进入 SQL 生成，返回明确提示（无法回答数据问题），不污染问数质量统计。
- `chitchat`：进入闲聊分支（§3.2）。
- `query`：进入查数分支（§3.3 起）。

### 3.2 闲聊分支（D34/D34a）

- 实例配置 `chitchat-enabled=true`：走通用对话 agent，自然语言回复，**不查库、不生成 SQL、不携带任何 SQL 工具**（安全边界不变）；闲聊回复标记类型 `chitchat`，不计入问数成功率统计。
- `chitchat-enabled=false`：返回固定拒答文案（无法回答数据问题的提示）。
- 闲聊分支可选装配：多个 ContentRetriever 经框架既有 `AggregatingContentRetriever` 聚合挂载（企业知识库 RAG）；多个 ToolProvider（本地 magic 工具 + 远程 MCP 的 McpToolProvider）经框架既有 `AggregatingToolProvider` 聚合。未配置时为纯兜底回复。
- 闲聊分支能力集为实例显式配置项；SQL 生成/执行永远只在查数分支。

### 3.3 多轮指代改写

- 多轮对话中出现指代（如「那 3 月份呢」）时，经 `CompressingQueryTransformer` 式上下文压缩改写：结合会话历史将当前问句改写为独立可理解的完整查询，作为后续节点输入；改写结果入审计，不改变用户可见语义。
- ReAct 自愈重生成轮必须携带「上一次错误信息 + 完整对话上下文」（regenerate 语义）。

### 3.4 表匹配 RAG（D6/D10）

- 关键词提取 → 术语映射（术语库将用户口语映射为标准词，扩展关键词与注入提示词）→ 向量召回（EmbeddingModel + 实例绑定的向量库）+ 关键词匹配的融合评分：向量分数与关键词分数按 α 加权产出多级评分，取 Top-N 表。
- 输出为表元数据列表（表名、注释、列名、列类型、业务注释），供 SQL 生成提示词限定「只使用匹配到的表」。
- 向量存储复用框架资源体系，按实例绑定；表结构登记由管理端向量化写入（本 capability 只消费）。

### 3.5 SQL 生成（ReAct 自愈，D5）

- SQL 生成环节为 ReAct agent：为 LLM 配只读工具白名单 `get_table_schema` / `execute_readonly_sql` / `validate_sql`，LLM 自主多步查表结构、试执行只读 SQL、观察报错、修正 SQL，直到产出最终 SQL 或达到**轮数上限**。
- 轮数上限与工具白名单为不变量；全部工具只读；每次工具调用轨迹完整入审计日志。
- 达到轮数上限：如实报告失败与已尝试轨迹，不编造结果。
- 生成提示词体系对齐 SQLBot `template.yaml`：process_check 自检（生成时多步自检）、行数限制零容忍、多表时字段限定、方言示例注入（§4）、校准示例 few-shot 注入、数据权限提示注入（减少回调改写量）、自定义提示词覆盖/追加。
- 提示词/方言示例带版本与启停（版本化在 admin 管理，core 按当前生效版本渲染）。

### 3.6 输出契约与校验

- LLM 最终输出必须为约定 JSON 结构（§5）。解析失败或空 SQL：明确报错并携带原始内容，不编造结果、不落库快照。
- 只读校验：最外层 SQL 必须为 SELECT/查询类；拦截 DML/DDL/多语句注入（`;` 串联、`into outfile` 等按方言特征拦截）；非法即拒绝执行并报错。

### 3.7 数据权限回调衔接

- 校验通过的 SQL 交由实例配置的权限回调（SPI 本地 bean 或 HTTP 回调双实现可切换，D25）：回调接收原始 SQL + UserContext（用户标识透传，角色/部门由业务解释）+ 表元数据，返转换后 SQL。
- 未注册回调：SQL 原样执行，只读校验与行数限制仍生效。
- fail-closed：回调抛异常或返回空/非 SELECT → 拒绝执行并明确报错。
- 回调改写后的 SQL 仍须通过只读复检（SELECT + 注入特征）后才进入执行；远程回调启用加密开关时按 D19 非对称加密 + 签名传递，日志不输出完整 SQL。

### 3.8 SQL 预览确认 / EXPLAIN 预检 / 人工调整（D31/D44）

- 实例配置为**确认执行模式**：生成的 SQL 先返回给用户展示（可编辑），用户确认后才执行；**自动执行模式**不经此闸门直接执行。agentic HumanInTheLoop 原语承载该等待。
- 确认执行模式下用户确认执行前，先做 EXPLAIN 预检（`EXPLAIN` 解析执行成本）：命中全表扫描等高风险特征时按实例配置「警告并允许」或「拒绝执行」；EXPLAIN 解析失败同样按实例配置处理。
- 未确认的 SQL 不得执行。
- **SQL 人工调整**（不限确认模式）：用户对已生成（含自动执行模式已执行成功的）SQL 手动修改并提交 → 系统对调整后 SQL 仍执行只读校验 + 数据权限回调 → 通过则执行并返回结果 → 保存为一条新记录（标记人工调整）；调整为非 SELECT/非法 SQL 时拒绝执行并明确报错。

### 3.9 执行（自建执行层，D23/D16/D45）

- 基于管理端配置的数据源信息自建 Druid 连接池；轻量 `SqlExecutor`（参数绑定执行、结果集映射、超时与最大行约束）；方言判断经方言适配器（参考 magicapi DialectAdapter 思路，不依赖其运行时）。
- 行数限制：生成 SQL 未含行数限制时最外层自动约束（默认 1000），且**子查询内不含行数限制**（限制只下推到最外层）。
- 记录库（独立记录数据源，D15，自动建表）保存：SQL、受上限约束的结果集快照（默认 ≤1000 行）、总行数、图表配置、解读、耗时/token 统计。不存超出上限的全量结果集（不变量）。
- 结果数据列表后端分页：记录快照上按 page/size 返回当前页与总行数（D45）。

### 3.10 解读与图表配置

- 执行成功后 LLM 产出自然语言解读 + 图表配置（类型/轴：柱/条/线/饼/表）+ 推荐问题（可选）。解读基于结果摘要，不编造数据；结果为空时如实说明。
- 多方言：按实例绑定数据源类型应用对应方言提示词示例（MySQL/PG/Oracle/SQLServer/Redshift/ClickHouse/Doris/Hive/DM/Kingbase/ES，D9）；MySQL 真库端到端验收，其余以提示词渲染 + 语法解析单测验证（D16）。

## 4. JSON 契约

- SQL 生成最终输出 JSON：
  ```json
  {
    "sql": "SELECT ... LIMIT 1000",
    "tables": ["orders"],
    "chart": {"type": "bar|line|pie|table|column", "x": "...", "y": ["..."]},
    "analysis": "自然语言解读",
    "brief": "一句话结论"
  }
  ```
- 意图识别节点输出：`{"intent": "query|chitchat|non-data", "reason": "..."}`。
- regenerate 请求携带 `regenerate_record_id`：框架回溯该记录之前的对话与错误，重生成携带上一错误与完整上下文。
- 解析失败/空 SQL/非 JSON：本节点报错，链路终止并返回含原始内容的明确错误，不产出结果。
- SSE 流式事件与 `SimpleAssistant.streamToSse` 同构（过程节点进度 + 最终数据/解读/图表配置）。

## 5. 提示词与方言

- 提示词模板对齐 SQLBot `template.yaml` 的角色体系（系统角色、自检 process_check、行数限制零容忍、多表字段限定、方言 few-shot 注入、术语扩展、数据权限提示、自定义追加），Java 侧模板化重写；版本化、启停由 admin 管理，core 渲染当前生效版本。
- 方言示例库对齐 SQLBot `sql_examples/` 11 种方言 bad/good few-shot，按实例数据源 `type` 选择对应示例集。
- 表匹配仅限匹配表出现，提示词显式限定「只使用下列表与字段」。

## 6. 行数限制、多实例、会话裁剪与限流

- 行数限制默认 1000（实例可配），强制最外层、禁入子查询；记录快照与分页均在该上限内。
- 多实例：每个问数实例独立绑定数据源/向量库/模型/权限策略/闲聊配置/记录库，实例间问数互不影响；按 key 区分引用（D30），配置变更触发实例重建（D27）。
- 会话上下文裁剪：历史轮数超上限时早轮按「按轮数窗口 + 可选摘要」策略裁剪后再参与提示词与指代改写，生成行为不受影响。
- 限流配额：用户问数次数达到实例/全局配额上限时返回明确配额提示，不再调用 LLM。
- 审计与可观测：全链路 OpenTelemetry（节点耗时/token/调用轨迹）；工具调用轨迹、SQL 全文（按日志级别脱敏，默认不输出完整 SQL）入审计日志互补。

## 7. 约束与不变量

- 生成 SQL 强制只读（DML/DDL/多语句注入被拦截）；强制外层行数限制（默认 1000，子查询内不含限制）。
- ReAct 循环必须有轮数上限与只读工具白名单；工具调用轨迹完整入审计（D5 不变量）。
- 闲聊分支不携带任何 SQL 工具；SQL 生成/执行仅在查数分支（安全边界，D34a）。
- 确认执行模式下未确认的 SQL 不得执行；EXPLAIN 预检失败（解析失败/高风险）按实例配置拒绝或警告。
- 回调 fail-closed：异常/空/非 SELECT 一律拒绝执行；回调改写后 SQL 仍过只读复检。
- 记录库不存超出行数上限的全量结果集；敏感配置（数据源密码/api-key/回调密钥）加密存储；日志默认不输出完整 SQL。
- langchain4j-agentic 为 beta 构件，接受 API 变化风险；agentic 使用收口隔离层，可退化为手写编排。
- Java 1.8 编译基线（禁 `List.of()`、`Map.copyOf()`、`Optional.orElseThrow()` 等 Java 9+ API）；包前缀 `com.xiongx9527.fw.plus.*`；源文件 UTF-8；不新增未确认 Maven 坐标；SQLBot 许可证约束——仅对齐功能设计，不复制其代码。

## 验收场景索引（对应 brief「问数核心（nl2sql-core）」）

- Scenario 正常问数 → §3.3–§3.10、§5
- Scenario 非查数据意图 → §3.1
- Scenario JSON 契约失败 → §4、§3.6
- Scenario regenerate 自愈 → §3.3、§3.5
- Scenario ReAct 自主修复 → §3.5、§7
- Scenario SQL 预览确认 → §3.8
- Scenario SQL 人工调整 → §3.8
- Scenario EXPLAIN 预检 → §3.8
- Scenario 多轮指代改写 → §3.3
- Scenario 闲聊兜底 → §3.2
- Scenario 闲聊分支 RAG/MCP → §3.2
- Scenario 行数限制 → §3.9、§6
- Scenario 多方言 → §3.10、§5
- Scenario 多实例隔离 → §6
- Scenario 会话上下文裁剪 → §6
- Scenario 限流配额 → §6
