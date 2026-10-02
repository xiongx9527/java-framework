# 目标

在 fw-plus 框架中实现智能问数（NL2SQL / ChatBI）能力，功能设计**完全参照 SQLBot**（dataease，Python + langchain 实现；仅对齐功能设计，Java 重新实现，不复制其代码——SQLBot 为类 GPLv3 许可）。

用户以自然语言提问 → 系统路由数据源、匹配相关表 → LLM 生成 SQL → 数据库执行 → 返回数据、图表与解读。

技术选型（已确认）：
- 编排：`langchain4j-agentic:1.20.0-beta30`（BOM 已管理 beta 版本线）：SequenceAgent 流水线、LoopAgent/ErrorHandler 自愈循环、ConditionalAgent 意图分支
- 生成/解读：ChatModel（复用框架 27 家 provider 体系）
- 表匹配 RAG：EmbeddingModel + 向量库（复用框架资源体系）
- SQL 执行：**自建执行层**（基于管理端配置的数据源信息建 Druid 连接池 + 轻量 SqlExecutor + 方言判断；不依赖 magicapi 引擎）
- 过滤解析辅助：`langchain4j-embedding-store-filter-parser-sql:1.20.2-beta30`（已装 .m2）

# 范围

## 框架/业务边界（用户 2026-10-01 确认：框架提供能力，业务实现接口）

**框架提供（开箱即用）**：问数核心编排（agentic 流水线/自检提示词/行数限制/多方言/regenerate 自愈）、管理端 magic 插件（全部 toolbars 管理页 + 资源链）、数据权限注入器（SPI 调用方 + 执行前强制注入 + 提示词约束）、用户端 UI 与 SSE 对话 API（Servlet/WebFlux 双栈）、对话记录存储、图表/dashboard/MCP/Excel 导入。

**业务实现（接口/SPI）**：
- 数据权限策略 SPI：`当前用户 → 可见数据条件`（形态参照 UserRowPermissionProvider，不绑 Sa-Token 内部实现）——组织架构/数据归属在业务侧，框架无法代答
- 认证接入：引 fw-plus-auth-satoken starter 并按业务配置登录体系

**运营配置（非代码）**：控制台配置数据源/模型/表结构登记/术语/示例/权限规则。

首版**全功能对齐 SQLBot**（来源：SQLBot 仓库 github.com/dataease/SQLBot，gitee 镜像 fit2cloud-feizhiyun/SQLBot，本地克隆 /tmp/sqlbot；模块级来源覆盖，逐条细化在 Spec 编写时进行）：

## Source coverage

| 来源条目与位置 | 读取状态 | 需要保留的内容 | Spec 位置 | 验收 ID | 覆盖状态 | 理由或替代关系 |
| --- | --- | --- | --- | --- | --- | --- |
| S1: backend/apps/chat/task/llm.py 问数流水线（关键词提取→术语扩展→表匹配→生成→校验→执行→解读） | complete | 全链路编排（agentic 实现）与 JSON 输出契约 | 待 Spec | 待定 | needs-clarification | — |
| S2: backend/templates/template.yaml 提示词工程（process_check 11 步自检、行数限制零容忍、多表字段限定） | complete | 生成时自检提示词体系（Java 模板重写） | 待 Spec | 待定 | needs-clarification | — |
| S3: backend/templates/sql_examples/（11 种方言 bad/good few-shot） | complete | 方言示例库（Java 模板重写） | 待 Spec | 待定 | needs-clarification | — |
| S4: backend/apps/terminology 术语库 | complete | 术语→标准词映射，扩展关键词与注入提示词 | 待 Spec | 待定 | needs-clarification | — |
| S5: backend/apps/data_training 数据训练（人工保存正确 SQL 作 few-shot） | complete | 校准示例管理 + 注入 | 待 Spec | 待定 | needs-clarification | — |
| S6: backend/apps/datasource + ai_model（数据源/模型管理） | complete | 管理端维护数据源与模型配置 → 共享配置存储 → API 服务自建连接池与模型实例（langchain4j 工厂） | 待 Spec（共享配置存储 + 自建实例） | 待定 | needs-clarification | 架构调整：API 服务与 magicapi 解耦（D23/D24） |
| S7: 数据权限（用户明确：SQL 查询数据权限，可自定义策略；**回调模式**——框架给业务原始 SQL，业务转换后返回） | complete | SQL 回调 SPI：框架将原始 SQL + UserContext + 表元数据交给业务回调，业务转换后返回；框架执行前只读复检 + fail-closed | 待 Spec | 待定 | needs-clarification | 用户明确要求，采用回调模式（SQL 改写权归业务） |
| S8: 前端（frontend/：chat 对话、ds 管理、dashboard、work 训练/术语） | complete | 管理端 magic 插件 + 用户端独立 UI（D7） | 待 Spec | 待定 | needs-clarification | — |
| S9: regenerate 重新生成（错误提示前置 + 保留对话上下文自愈） | complete | 自愈机制 | 待 Spec | 待定 | needs-clarification | — |
| S10: MCP 集成 / 嵌入集成（企微/钉钉/飞书）/ Excel 导入 / dashboard | complete | 首版纳入（全功能对齐） | 待 Spec | 待定 | needs-clarification | — |
| S11: 多方言（sql_examples 11 种：MySQL/PG/Oracle/SQLServer/Redshift/ClickHouse/Doris/Hive/DM/Kingbase/ES） | complete | 首版多方言同步（提示词示例 + 执行层方言适配） | 待 Spec | 待定 | needs-clarification | — |

# 非目标

- 不复制 SQLBot 代码（类 GPLv3 许可约束，仅对齐功能设计）
- 不自建模型接入层（复用框架现有 chat/embedding model 资源链）
- 数据源连接池与向量存储由 API 服务按管理端配置自建实例；模型实例（ChatModel/EmbeddingModel）按共享配置存储中的模型配置用 langchain4j 工厂自建（模型构建为纯 langchain4j API，不依赖 magicapi）
- 不搬移 SQLBot 内部权限体系（对接框架自身 Sa-Token / 数据权限体系）
- **不做用户管理**（用户确认 2026-10-01）：不建用户注册/管理功能，认证直接对接业务已有 Sa-Token 登录态
- **角色、用户体系完全由业务决定**（用户确认 2026-10-01）：框架不定义用户/角色/部门模型；数据权限策略的「生效对象」不写死维度——策略 SPI 将当前用户身份标识透传给业务实现，由业务返回可见数据条件，策略规则中引用的主体由业务解释

# 验收示例

## 问数核心（nl2sql-core）

- Scenario: 正常问数——WHEN 问数实例已配置（数据源+向量库+LLM）且表结构已登记，用户提问"上月订单总额" THEN 返回正确的 SQL 执行结果数据与自然语言解读
- Scenario: 非查数据意图——WHEN 用户提问与数据无关（如闲聊天气） THEN 不生成 SQL，返回无法回答数据问题的提示
- Scenario: JSON 契约失败——WHEN LLM 返回非法 JSON 或空 SQL THEN 明确报错（含原始内容），不编造结果
- Scenario: regenerate 自愈——WHEN 执行失败后触发重新生成 THEN 新一轮携带上一次错误信息与完整上下文
- Scenario: ReAct 自主修复——WHEN SQL 生成自愈环节启动 THEN ReAct agent 可调用 get_table_schema / execute_readonly_sql / validate_sql 工具自主查证与修正，最终 SQL 通过校验；达到轮数上限则如实报告失败与已尝试轨迹
- Scenario: 行数限制——WHEN 生成的 SQL 未含行数限制 THEN 最外层自动约束（默认 1000），且子查询内不含限制
- Scenario: 多方言——WHEN 数据源为 Oracle/达梦等非 MySQL 库 THEN 提示词应用对应方言示例，生成 SQL 符合该方言语法
- Scenario: 多实例隔离——WHEN 配置两个问数实例绑定不同数据源/模型 THEN 各自问数互不影响
- Scenario: 会话上下文裁剪——WHEN 多轮对话超过历史轮数上限 THEN 早轮历史按策略裁剪/摘要，生成仍正常
- Scenario: 限流配额——WHEN 用户问数次数达到配置的配额上限 THEN 返回明确的配额提示，不再调用 LLM

## 管理端插件（nl2sql-admin）

- Scenario: 表结构管理——WHEN 登记表/列及业务注释并触发向量化 THEN 表结构进入向量库可供表匹配
- Scenario: 术语库——WHEN 配置术语映射后问数 THEN 生成过程使用标准词
- Scenario: 数据训练——WHEN 人工保存正确 SQL 示例后提相似问题 THEN 生成命中该示例
- Scenario: 自定义提示词——WHEN 配置自定义提示词 THEN 生成时覆盖/追加默认规则
- Scenario: 提示词版本化——WHEN 提示词/方言示例修改后 THEN 可查看历史版本并回滚
- Scenario: 管理端操作审计——WHEN 管理端变更配置（表结构/策略/提示词等） THEN 记录操作人与变更前后快照，可查询

## 数据权限（nl2sql-security）

- Scenario: 回调转换——WHEN 业务注册了 SQL 回调 THEN 框架把生成的原始 SQL + 用户标识交给回调，执行回调返回的 SQL
- Scenario: 无回调放行——WHEN 未注册回调 THEN 生成的 SQL 原样执行（只读校验仍生效）
- Scenario: fail-closed——WHEN 回调抛异常或返回空/非 SELECT SQL THEN 拒绝执行并明确报错
- Scenario: 租户隔离示例——WHEN 业务的回调按用户租户改写 SQL（附加 tenant_id 条件） THEN 越租户数据不可见
- Scenario: 策略管理——WHEN 管理端新增/修改/停用数据权限策略或回调函数 THEN 问数执行时按最新配置生效
- Scenario: SQL 传递安全——WHEN 回调为远程调用且加密开关启用 THEN SQL 载荷经非对称加密（管理端生成的密钥对）+ 签名传递；开关关闭时明文传递；用户端默认不展示原始 SQL 明文；日志不输出完整 SQL
- Scenario: 敏感配置加密——WHEN 管理端保存数据源密码/LLM api-key 等敏感字段 THEN 以加密形态写入共享配置存储，API 服务读取时解密

## 用户端 UI（nl2sql-user-ui）

- Scenario: 流式对话——WHEN 用户端发起问数 THEN SSE 流式返回过程与最终数据/解读
- Scenario: 图表切换——WHEN 返回结果后切换图表类型 THEN 按数据正确渲染表格/柱状/折线/饼图

## 图表与看板（nl2sql-chart-dashboard）

- Scenario: dashboard——WHEN 用户将多次问数结果保存到看板 THEN 看板按配置展示图表

## 集成（nl2sql-integrations）

- Scenario: MCP 问数——WHEN 通过 MCP 协议调用问数能力 THEN 返回与对话一致的问数结果
- Scenario: Excel 导入——WHEN 导入 Excel 数据 THEN 生成可查询表并可问数
- Scenario: API 对接——WHEN 业务方用自己的 UI 调用问数 API THEN 依据公开接口契约完成对话问数（含流式），不依赖我们的用户端 UI
- Scenario: UI 对接——WHEN 业务系统以嵌入/弹窗方式集成我们提供的用户端对话 UI THEN 对话功能在嵌入环境中可用

# 约束与不变量

- 生成的 SQL 强制只读（拦截 DML/DDL），强制外层行数限制（默认 1000，禁入子查询）
- langchain4j-agentic 为 beta 构件，接受 API 可能变化的风险
- 问数执行使用只读数据库账号兜底
- 遵循框架 Java 版本基线与 magicapi 自维护版本约束
- 共享配置存储的 DDL 由框架统一提供并带 schema 版本；分应用部署时版本不匹配明确报错
- 共享配置存储中的敏感字段（数据源密码、api-key、回调密钥）加密存储
- agentic 的使用收口到核心编排模块的少量类（隔离层），必要时可退化为手写编排
- ReAct 循环必须有轮数上限与工具白名单（全部只读），工具调用轨迹完整入审计日志
- 记录库只存 SQL + 结果摘要/前 N 行 + 总行数，不存全量结果集

# 决策

- D1: 功能设计完全参照 SQLBot（用户 2026-10-01 明确）；许可证约束：不复制代码，Java 重新实现
- D2: SQL 查询数据权限采用**回调模式**（用户明确 2026-10-01）：框架将原始 SQL + UserContext（用户标识透传）+ 表元数据交给业务提供的回调，**业务转换 SQL 后返回**，框架执行前仅做只读复检（SELECT）+ fail-closed（回调异常/返回空则拒绝执行）；提示词层仍注入可配置的权限提示以减少回调改写量
- D3: **管理端整成一个 magic 插件**：表结构/术语库/数据训练/自定义提示词/数据权限策略等管理功能做成 magic 控制台插件资源（用户明确）
- D4: 问数实例需要配置三类资源（用户明确）：数据源、向量模型/向量库、大语言模型
- D5: 编排用 langchain4j-agentic（用户确认）；**SQL 生成自愈环节采用 ReAct agent**（用户确认 2026-10-01）：为 LLM 配只读工具（get_table_schema / execute_readonly_sql / validate_sql），LLM 自主多步查表结构、试执行、观察报错、修正 SQL，直到产出最终 SQL 或达到轮数上限；主链其余环节保持 SequenceAgent 结构，全程工具调用轨迹入审计；编排产物对 AssistantBuilder 的挂载能力（agenticToolProvider）为独立 change（agentic-basic），非本 change 范围。agentic-basic 范围（用户确认 2026-10-01）：编排原语 sequence/loop/conditional/parallel 四种 + 集成 langgraph4j（MIT，org.bsc.langgraph4j）提供 graph 编排（graphToolProvider 把编译图挂为工具）
- D6: 表匹配采用 SQLBot 的「向量分数 + 关键词融合评分」机制（α 加权、多级评分）
- D7: UI 分两端（用户明确）：**管理端 = 一个 magic 插件**；**用户端 = 单独的独立 UI**（不进 magic 控制台、不嵌第三方，独立问数对话界面，SSE 流式）
- D8: 问数插件**多实例**（用户明确）：每个插件实例可分别配置数据源、向量数据库、大语言模型；不同实例互不影响
- D9: 首版数据库方言**多方言同步**，对齐 SQLBot 11 种（MySQL/PG/Oracle/SQLServer/Redshift/ClickHouse/Doris/Hive/DM/Kingbase/ES），方言差异走「方言提示词示例库 + 执行层方言适配」（用户确认）
- D10: 表匹配 RAG 向量存储**复用现有资源链**（embedding-model + embedding-store 资源 key，实例级绑定）（用户确认）
- D11: 首版**全功能对齐 SQLBot**：问数核心链路、管理端（表结构/术语库/数据训练/自定义提示词）、图表展示、dashboard 看板、MCP 集成、Excel 导入、嵌入集成（企微/钉钉/飞书）＋数据权限自定义策略（用户确认）
- D12: **Supervisor Change 拆分**（用户确认草案）：6 子任务 `nl2sql-core`（问数核心编排，无依赖）/ `nl2sql-admin`（管理端插件，无依赖）/ `nl2sql-security`（数据权限，依赖 core）/ `nl2sql-user-ui`（用户端 UI，依赖 core）/ `nl2sql-chart-dashboard`（图表+看板，依赖 user-ui）/ `nl2sql-integrations`（MCP+Excel+嵌入，依赖 core）；推进方式 **A 多会话协作**（用户确认）
- D13: 管理端插件的实现**参照 s3 插件（fw-plus-magicapi-plugin-s3）及其管理端模式**（用户明确）；管理端 UI 采用 s3 插件的 **toolbars 页签模式**（`{id, title, icon, order, component, adminOnly: true, allowClose, fullscreen}` 全屏管理页）：表结构管理、术语库、数据训练、自定义提示词、**数据权限策略管理（独立功能模块：策略 CRUD、规则绑定、启停）**等均以此形态注册进 magic 控制台；问数实例等资源配置沿用 datasources 资源表单模式
- D14: 用户端认证**对接框架 Sa-Token**（用户确认）：登录态与权限上下文（用户/部门/角色）可直接供数据权限策略使用
- D15: 对话记录存储**独立记录库**（用户确认）：问数实例可配置「记录存储数据源」，自动建表，与业务库隔离
- D16: 多方言验收**分级**（用户确认）：MySQL 真库端到端验收；其余 10 种方言以「提示词渲染 + SQL 语法解析单测」验证，真库效果由用户后续自验
- D17: Excel 导入**导入到问数绑定的数据源**（用户确认）：自动建表写数据后即可问数（需业务库允许建表）
- D18: **提供业务可对接的后端服务**（用户确认 2026-10-01）：问数能力以 HTTP/SSE API 层交付（对话问数/会话历史/图表数据），用户端 UI 与业务系统均为其消费者；**问数 API 服务定位为 Sa-Token 的授权服务端**（用户明确 2026-10-01）：登录认证、token 签发与接口鉴权的闭环在问数服务端完成，认证授权方式可按 Sa-Token 标准配置（框架不另造认证机制），用户态与权限上下文从登录态获取；MCP 调用走框架 mcp-server 既有认证体系（SaTokenOAuth2）
- D21: **业务客户端的对接形态自行决定**（用户明确 2026-10-01）：框架只提供授权服务端与公开 API 契约，业务客户端（自有 UI、脚本、第三方系统）如何对接、如何集成 token、如何组织界面均由业务自行安排，框架不做约定
- D22: **API 服务的配置均通过管理 UI 配置**（用户明确 2026-10-01）：问数实例、数据权限策略与回调、加密开关与密钥对、认证授权相关可配置项等全部在管理端（magic 插件）配置，不依赖 application.yml 手工配置（框架缺省值除外）；配置即热生效（问数实例多实例动态重建语义）
- D20: **业务对接方式两类**（用户明确 2026-10-01）：**API 对接**（业务方用**自己的 UI** 调用问数 HTTP/SSE API，接口契约公开）与 **UI 对接**（业务系统直接集成我们提供的用户端对话 UI，嵌入/弹窗方式）——MCP 调用为第三条独立通道（见 D11 集成范围）
- D19: **SQL 传递安全**（用户明确 2026-10-01）：
  - 业务回调为远程调用（业务系统独立部署）时，SQL 载荷支持**非对称加密**（如 RSA/SM2 混合加密：框架用业务公钥加密载荷、业务私钥解密；响应可反向加密）+ 私钥签名防篡改。**加密由开关配置控制（可启用/关闭）**：启用时以应用层加密为安全保障（不要求 TLS，传输协议由部署环境自行决定）；关闭时明文传递，传输保护由部署环境自行负责。本地 SPI（Java bean）为进程内调用，无传输环节。**加密密钥对由管理端生成与管理**（生成/轮换/公钥分发均在管理端完成，私钥不回显）
  - 用户端 UI 默认**不展示原始 SQL 明文**（可配置开启）；SQL 相关内容在传输中以加密/脱敏形态出现
  - 日志中 SQL 全文按日志级别脱敏/控制（默认不输出完整 SQL）
  - 记录库中的 SQL 字段支持可选加密存储（实例配置）
- D23: **问数 API 服务与 magicapi 解耦**（用户明确 2026-10-01）：API 服务为独立模块，不依赖 magicapi 引擎/脚本执行；**SQL 执行层自建**——基于管理端配置的数据源信息建 Druid 连接池 + 轻量 SqlExecutor（含方言判断，可参考 magicapi DialectAdapter 思路）
- D24: **配置共享存储；magic 只管管理端**（用户明确 2026-10-01）：管理端 magic 插件把配置（表结构/术语/训练示例/提示词/权限策略/问数实例/数据源与模型配置）写入共享配置存储（数据库表），API 服务从同一存储读取——管理端与 API 服务可同进程或分应用部署；ChatModel/EmbeddingModel 实例由 API 服务按共享配置用 langchain4j 工厂自建（纯 langchain4j API，不依赖 magicapi）
- D25: **配置机制**（采纳建议）：配置缓存 + 变更刷新（同进程事件通知/分应用版本号轮询或 Redis 广播）；回调定义统一契约，SPI（同进程）与 HTTP 回调（跨进程）双实现可切换；管理端操作审计（变更前后快照）
- D26: **API 服务交付形态**（采纳建议）：核心做成 starter（业务应用引入即得问数 API 服务）+ 可选独立 server 装配（演示/独立部署）；magic 插件为可选叠加的管理端
- D27: **外部资源实例统一自建管理器**（采纳建议）：连接池/ChatModel/EmbeddingModel/向量库客户端统一按配置自建，配置变更→重建实例→旧实例关闭（对齐 magic 插件多实例动态重建语义）
- D28: **分期交付**（采纳建议）：第一批 core+admin+security（可问数、有权限）；第二批 user-ui+chart；第三批 dashboard+integrations；多方言按成熟度分级（MySQL/PG/Oracle/达梦首批精调，其余提供示例框架随用随补）；提示词/示例带版本与启停

# 待解决问题

（当前无 [blocking] 项；待用户确认完整 Shape 后进入 Build）

# 验证预期

- 单测覆盖：SQL 只读拦截、行数限制注入、权限条件注入、JSON 契约解析、表匹配评分、多方言提示词渲染
- 集成验证：配置真实数据源后端到端问数（依赖外部 LLM key，Verifier 可用 mock ChatModel 验证链路逻辑 + 用户人工验证真实问数效果）
- 前端插件构建产物校验（iife 进 jar）
- 用户端 UI 构建与流式对话联调
