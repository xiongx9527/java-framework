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

**框架提供（开箱即用）**：问数核心编排（agentic 流水线/自检提示词/行数限制/多方言/regenerate 自愈）、管理端 magic 插件（全部 toolbars 管理页 + 资源链）、数据权限注入器（SPI 调用方 + 执行前强制注入 + 提示词约束）、用户端 UI、**对外 API 服务（REST/SSE 对话 API，Servlet/WebFlux 双栈；同时对外提供 MCP Server 接口）**、对话记录存储、图表/dashboard/Excel 导入。

- 用户端 UI 含会话管理：会话列表/重命名/删除/历史回看（记录库数据）
- **对外 API 对齐 SQLBot 对外接口能力全量**（用户明确 2026-10-07）：SQLBot `/api/v1` 中**业务消费类**接口能力（问数对话与记录、数据源查询/执行、表关系、推荐问题、看板、MCP、API-Key、术语、数据训练）在本框架对外 API 中全部提供；**平台账号类接口（用户/工作区/助手/模型/系统参数）不提供**（沿用既有非目标「不做用户管理」）
- **图表服务**（用户明确 2026-10-07）：问数结果由 LLM 自动产出图表配置（类型/轴，柱/条/线/饼/表），前端据此渲染；同时提供服务端图表图片渲染（对齐 SQLBot `g2-ssr` 的能力），供导出/非流式场景产出 PNG
- **图表 + 数据分页列表**（用户明确 2026-10-07）：问数结果同时提供图表与数据列表；数据列表采用**后端服务分页**（API 接受 page/size，返回当前页数据与总行数）
- **数据导出补全**（用户明确 2026-10-07）：问数结果导出（Excel/CSV + 图表图片 PNG）、术语/训练示例的模板导入导出、数据源 schema 导入导出
- **SQL 人工调整**（用户明确 2026-10-07）：用户可对已生成 SQL 手动修改后重新执行（不限确认模式），调整后仍过只读校验与数据权限回调

**业务侧仅需（按需、可选）**：
- SQL 回调（数据权限自定义的唯一业务点）：注册回调则框架把原始 SQL + 用户标识交业务转换后执行；**不注册则 API 服务开箱即用**（原样执行，只读校验 + 行数限制仍生效）
- 认证授权直接使用框架内置的 Sa-Token 授权服务端（可配置），业务无需自建认证

**运营配置（非代码）**：控制台配置数据源/模型/表结构登记/术语/示例/权限策略与回调。

首版**全功能对齐 SQLBot**（来源：SQLBot 仓库 github.com/dataease/SQLBot，gitee 镜像 fit2cloud-feizhiyun/SQLBot，本地克隆 /tmp/sqlbot；模块级来源覆盖，逐条细化在 Spec 编写时进行）：

## Source coverage

| 来源条目与位置 | 读取状态 | 需要保留的内容 | Spec 位置 | 验收 ID | 覆盖状态 | 理由或替代关系 |
| --- | --- | --- | --- | --- | --- | --- |
| S1: backend/apps/chat/task/llm.py 问数流水线（关键词提取→术语扩展→表匹配→生成→校验→执行→解读） | complete | 全链路编排（agentic 实现）与 JSON 输出契约 | specs/nl2sql-core/spec.md §3 | A1–A16 | covered | — |
| S2: backend/templates/template.yaml 提示词工程（process_check 11 步自检、行数限制零容忍、多表字段限定） | complete | 生成时自检提示词体系（Java 模板重写） | specs/nl2sql-core/spec.md §5 | A1、A13 | covered | — |
| S3: backend/templates/sql_examples/（11 种方言 bad/good few-shot） | complete | 方言示例库（Java 模板重写） | specs/nl2sql-core/spec.md §3.10、§5 | A13 | covered | — |
| S4: backend/apps/terminology 术语库 | complete | 术语→标准词映射，扩展关键词与注入提示词 | specs/nl2sql-admin/spec.md §4 | A18 | covered | — |
| S5: backend/apps/data_training 数据训练（人工保存正确 SQL 作 few-shot） | complete | 校准示例管理 + 注入 | specs/nl2sql-admin/spec.md §5 | A19 | covered | — |
| S6: backend/apps/datasource + ai_model（数据源/模型管理） | complete | 管理端维护数据源与模型配置 → 共享配置存储 → API 服务自建连接池与模型实例（langchain4j 工厂） | specs/nl2sql-admin/spec.md §1、§12 | A17 | covered | 架构调整：API 服务与 magicapi 解耦（D23/D24） |
| S7: 数据权限（用户明确：SQL 查询数据权限，可自定义策略；**回调模式**——框架给业务原始 SQL，业务转换后返回） | complete | SQL 回调 SPI：框架将原始 SQL + UserContext + 表元数据交给业务回调，业务转换后返回；框架执行前只读复检 + fail-closed | specs/nl2sql-security/spec.md §1–§3 | A29、A30、A31 | covered | 用户明确要求，采用回调模式（SQL 改写权归业务） |
| S8: 前端（frontend/：chat 对话、ds 管理、dashboard、work 训练/术语） | complete | 管理端 magic 插件 + 用户端独立 UI（D7） | specs/nl2sql-admin/spec.md §1、specs/nl2sql-user-ui/spec.md | A17、A36 | covered | — |
| S9: regenerate 重新生成（错误提示前置 + 保留对话上下文自愈） | complete | 自愈机制 | specs/nl2sql-core/spec.md §3.5 | A4、A5 | covered | — |
| S10: MCP 集成 / 嵌入集成（企微/钉钉/飞书）/ Excel 导入 / dashboard | complete | 首版纳入（全功能对齐） | specs/nl2sql-integrations/spec.md、specs/nl2sql-chart-dashboard/spec.md | A41、A42、A44 | covered | — |
| S11: 多方言（sql_examples 11 种：MySQL/PG/Oracle/SQLServer/Redshift/ClickHouse/Doris/Hive/DM/Kingbase/ES） | complete | 首版多方言同步（提示词示例 + 执行层方言适配） | specs/nl2sql-core/spec.md §3.10 | A13 | covered | — |
| S12: 图表生成与服务端出图（backend/apps/chat/task/llm.py `chart_sys_question` 出图表配置；`g2-ssr/` `@antv/g2-ssr` 出 PNG，端口 3000；frontend `charts/{Bar,Column,Line,Pie,Table}.ts` 渲染） | complete | LLM 自动图表配置 + 前端渲染 + 服务端出图 | specs/nl2sql-chart-dashboard/spec.md §2–§4 | A38、A39 | covered | 用户 2026-10-07 明确纳入（D42） |
| S13: 导出族（`apps/chat/api/chat.py` record/{id}/excel/export；`apps/terminology`、`apps/data_training` 的 export/template/uploadExcel；`apps/datasource` exportDsSchema/{id} + uploadDsSchema/{id}） | complete | 结果导出 + 术语/示例模板导入导出 + 数据源 schema 导入导出 | specs/nl2sql-admin/spec.md §9 | A24、A25、A26 | covered | 用户 2026-10-07 明确纳入（D43） |
| S14: SQL 展示与人工调整 + 结果数据呈现（frontend `SQLComponent.vue` 只读高亮；`api/chat.ts` record/{id}/{data,predict_data,analysis,predict}；`charts/Table.ts` `@antv/s2` TableSheet） | complete | SQL 人工调整后重执行 + 结果数据列表（后端服务分页） | specs/nl2sql-core/spec.md §3.8、specs/nl2sql-chart-dashboard/spec.md §5 | A6、A7、A40 | covered | 用户 2026-10-07 明确纳入（D44/D45） |

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
- Scenario: SQL 预览确认——WHEN 实例配置为确认执行模式 THEN 生成的 SQL 先展示给用户（可编辑），用户确认后才执行；自动执行模式不受影响
- Scenario: SQL 人工调整——WHEN 用户对已生成（含自动执行模式）的 SQL 手动修改并提交 THEN 系统执行调整后的 SQL（仍过只读校验与数据权限回调），返回新结果并保存为记录；调整后为非 SELECT/非法 SQL 时拒绝执行
- Scenario: EXPLAIN 预检——WHEN 确认模式下用户确认执行 THEN 先 EXPLAIN 检查执行成本，命中全表扫描等高风险特征时警告并可拒绝
- Scenario: 多轮指代改写——WHEN 多轮对话中出现指代（如"那 3 月份呢"） THEN 经上下文改写后正确理解指代并生成 SQL
- Scenario: 闲聊兜底——WHEN 开启闲聊兜底（chitchat-enabled）且用户闲聊 THEN 通用对话 agent 自然回复（不查库不生成 SQL）；WHEN 关闭该开关 THEN 返回无法回答数据问题的提示
- Scenario: 闲聊分支 RAG/MCP——WHEN 问数实例的闲聊分支配置了 contentRetriever/MCP 工具 THEN 闲聊时可检索知识库/调用外部工具回答；未配置则纯兜底回复
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
- Scenario: 结果反馈——WHEN 用户对问数结果点赞/点踩 THEN 反馈入待改进池（点踩自动收集问题与生成的 SQL），管理员可据此补术语/校准示例
- Scenario: 结果导出——WHEN 用户对问数结果导出 THEN 生成 Excel/CSV 文件下载，并可同时导出图表图片（PNG）
- Scenario: 术语/示例模板导入导出——WHEN 管理员导出术语库/训练示例 THEN 得到可按模板回填再导入的 Excel 文件；导入后按最新配置生效
- Scenario: 数据源 schema 导入导出——WHEN 管理员导出某数据源的 schema 配置 THEN 可在另一实例导入，复用表结构登记与本地注释
- Scenario: schema 变更检测——WHEN 触发变更检测（定时/手动） THEN 对比业务库实际 schema 与已登记表结构，差异（新表/新列/类型变更）提醒管理员补登记/重向量化
- Scenario: 成本看板——WHEN 管理员查看成本看板 THEN 按用户/实例/时间段展示 token 消耗与问数次数统计

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

- Scenario: LLM 图表推荐——WHEN 问数返回结果 THEN LLM 依据结果自动产出图表配置（类型/轴，柱/条/线/饼/表），前端据此渲染
- Scenario: 服务端出图——WHEN 需要图片形态（导出/非流式场景） THEN 服务端按图表配置渲染为 PNG（对齐 SQLBot g2-ssr 能力）
- Scenario: 数据分页列表——WHEN 用户查看问数结果数据列表 THEN 后端按 page/size 服务端分页返回当前页数据与总行数，翻页结果正确
- Scenario: dashboard——WHEN 用户将多次问数结果保存到看板 THEN 看板按配置展示图表

## 集成（nl2sql-integrations）

- Scenario: MCP 问数——WHEN MCP 客户端通过对外 API 服务的 MCP 接口调用问数能力 THEN 返回与对话通道一致的问数结果（复用同一问数核心与配置）
- Scenario: MCP 对外能力全覆盖——WHEN MCP 客户端通过 MCP 接口调用会话历史与图表数据能力 THEN 返回与 REST/SSE 通道一致的结果（会话列表/消息历史、问数结果图表数据）
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
- 记录库保存 SQL + 受行数限制上限约束的结果集快照（默认 ≤1000 行）+ 总行数，用于历史回看与后端分页；不存超出上限的全量结果集
- 问数编排全链路对接 OpenTelemetry（各节点耗时/token 用量/调用轨迹）
- 确认执行模式下未确认的 SQL 不得执行；EXPLAIN 预检失败（解析失败/高风险）按实例配置拒绝或警告
- 闲聊兜底 agent 不携带任何工具（不触 SQL 生成/执行），安全边界不变

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
- D30: **问数最终产物为特殊的 SimpleAssistant**（用户确认 2026-10-01）：nl2sql 编排包装为助手语义——`chat(message)` 背后是问数编排（非 AiServices），`streamToSse(...)` 流式跑编排；与普通助手同构，对话 API/SSE/用户端 UI 全部复用现有 SimpleAssistant 通道。实现上对应「编排即助手」形态在问数场景的落地（SimpleAssistant 支持编排本体或 Nl2SqlAssistant 独立类同接口语义，Build 时定），并**形成 SimpleAssistant 多实例**（用户明确 2026-10-01）：每个问数实例配置对应一个独立的问数助手实例（各绑定自己的数据源/向量库/LLM/权限策略），按 key 区分引用——与 chat-model 等资源的多实例模式一致
- D31: **安全与体验增强**（采纳建议 2026-10-01）：SQL 预览确认模式（可按实例配置自动/确认，agentic HumanInTheLoop 原语）+ EXPLAIN 预检（全表扫描拦截）+ 多轮指代改写（langchain4j CompressingQueryTransformer）+ 结果反馈闭环（点赞/点踩 → 待改进池驱动术语/示例运营）+ 结果导出 Excel/CSV（复用 EasyPoi）
- D32: **可观测性**（采纳建议）：问数编排全链路对接框架 OpenTelemetry 体系（节点耗时/token/调用轨迹），与审计日志互补
- D33: **交付物含使用文档**：管理端操作手册（表结构登记规范/术语库运营/权限策略配置指南）
- D34: **闲聊兜底回复**（采纳建议 2026-10-01）：ConditionalAgent 闲聊分支挂通用对话 agent（自然回复、不查库不生成 SQL），按实例配置 chitchat-enabled 开关（关闭回拒答模式）；闲聊标记 chitchat 类型不污染问数质量统计
- D34a: **闲聊分支可配 RAG/MCP**（用户明确 2026-10-01）：闲聊分支的通用对话 agent 支持按实例可选配置 contentRetriever（企业知识库 RAG）与 MCP 工具——使问数助手兼做通用企业助手；默认不配（纯兜底回复）；闲聊分支的能力集为显式配置项，问数编排（SQL 生成/执行）永远只在查数分支，安全边界不变。RAG 实现：可配置**多个 ContentRetriever**，经框架既有 `AggregatingContentRetriever` 聚合后挂载（与 AssistantBuilder 的 contentRetriever 累积挂载语义一致）；MCP/工具实现：可配置**多个 ToolProvider**（本地 magic 工具 + 远程 MCP Server 的 McpToolProvider 并存），经框架既有 `AggregatingToolProvider` 聚合后挂载（与 RAG 聚合对称）
- D35: **schema 变更检测**（采纳建议）：定时/手动对比业务库实际 schema 与已登记表结构，差异提醒管理员补登记/重向量化
- D36: **列级脱敏**（采纳建议，权限回调演进）：权限回调扩展列级规则，结果集序列化时统一脱敏（手机号/身份证等），与行级同一 SPI 演进
- D37: **成本看板**（采纳建议）：记录库 token 用量按用户/实例/时间段聚合，管理端展示
- D38: **交付物含 demo 数据库**（采纳建议）：内置演示数据（建表 SQL+样例数据+预登记表结构/术语/示例），开箱 5 分钟跑通首个问数，同时作为 MySQL 真库端到端验收的标准数据集
- D39: **Excel 导入自动登记**（采纳建议）：导入建表后自动生成表结构登记并触发向量化，导入的表立即可问数
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
- D40: **对外 API 必须提供 MCP 接口，且暴露全部对外能力**（用户明确 2026-10-07）：问数 API 服务除 REST/SSE 通道外，必须对外提供 MCP Server 接口，供 MCP 客户端 / AI Agent 以工具（tools/resources）形式调用对外 API 的**全部能力**——问数对话、会话历史、图表数据；复用框架 `fw-plus-ai-mcp-server` 与既有 SaTokenOAuth2 认证体系，MCP 通道与 REST/SSE 通道共享同一问数核心与共享配置；管理端能力不通过 MCP 暴露
- D41: **对外 API 对齐 SQLBot 全部对外接口能力（业务消费类）**（用户明确 2026-10-07）：以 SQLBot `/api/v1` 接口清单为基线，业务消费类能力全量提供——问数对话与记录、数据源查询/执行、表关系、推荐问题、看板、MCP、API-Key、术语、数据训练；**平台账号类接口（用户/工作区/助手/模型/系统参数）不提供**（用户 2026-10-07 确认，沿用非目标「不做用户管理」）；接口以 REST/SSE + MCP 两种形态交付
- D42: **图表服务**（用户明确 2026-10-07）：问数结果由 LLM 自动产出图表配置（类型/轴：柱/条/线/饼/表），前端按配置渲染（G2/S2 思路）；同时提供服务端图表图片渲染（对齐 SQLBot `g2-ssr` 的能力），供导出/非流式场景产出 PNG。**不引入 Node 出图服务**——服务端出图用 Java 侧渲染实现（Build 时定方案）
- D43: **数据导出补全**（用户明确 2026-10-07）：问数结果导出 Excel/CSV + 图表图片 PNG；术语库/训练示例支持按模板导入导出（Excel）；数据源 schema 支持导入导出（表结构登记与本地注释迁移）
- D44: **SQL 人工调整（改后重执行）**（用户明确 2026-10-07）：用户可对已生成 SQL 手动修改并重新执行，**不限确认模式**；调整后的 SQL 仍强制走只读校验与数据权限回调；执行成功保存为一条记录（体现人工调整）
- D45: **数据分页列表（后端服务分页）**（用户明确 2026-10-07）：问数结果数据列表 API 接受 page/size，**后端服务分页**，返回当前页数据与总行数；分页在受行数限制（默认 ≤1000 行）的结果集快照上进行——记录库保存该快照以支持历史回看与翻页（取代原「只存前 N 行」表述，仍不存超出上限的全量结果集）

# 待解决问题

（本轮无 [blocking] 用户问题；待补齐完整目标 Spec 与来源覆盖表后，重新准备最终 Shape 确认）

# 验证预期

- 单测覆盖：SQL 只读拦截、行数限制注入、权限条件注入、JSON 契约解析、表匹配评分、多方言提示词渲染
- 集成验证：配置真实数据源后端到端问数（依赖外部 LLM key，Verifier 可用 mock ChatModel 验证链路逻辑 + 用户人工验证真实问数效果）
- 前端插件构建产物校验（iife 进 jar）
- 用户端 UI 构建与流式对话联调
