## Context

`fw-plus-ai-askdata-spring-boot-starter` 已在 AI 聚合器和 BOM 中发布，自动配置模块已提供 LangChain4j 模型注册、顺序流程执行器、Agent 派发、上下文压缩和内存检查点。本 change 已在 `fw-plus-core` 新增独立 `fw-plus-ai-askdata` 模块，领域类型、运行时 SPI、SQL 插件链、状态存储、观测事件、发布契约和 Remote Runtime 边界均迁入 Java 8 兼容 Core 契约；AI 自动配置层负责桥接默认 LangChain4j/Ollama、JDBC SQL 执行器、向量检索、监控日志、集群拒绝本地状态和业务可覆盖 Bean。

该 change 必须保持 Spring Boot 2.7.18 与 Java 8 兼容，并作为项目管理、训练检索、REST/MCP、企业配置和控制台业务接入的基础。`https://gitee.com/fit2cloud-feizhiyun/SQLBot` 与 `https://sqlbot.org/docs/v1/` 是整个批次的优先功能验收基线：每项开源和企业级功能都必须映射到 batch change、实现模块、文档和验证证据，全部契约、默认实现和验证矩阵闭环前不得宣称功能完整；真实外部租户连通必须另记联调证据。Canner/WrenAI 的企业级 GenBI 能力也是必须覆盖的补强基线：MDL 语义模型、上下文富化、dry-plan、结构化错误、值画像、评测、推荐、Agent 生成/部署仪表盘和多数据源治理均进入追踪矩阵；与 SQLBot 重叠时采用 SQLBot 的产品边界和命名，吸收 WrenAI 的底层设计优势。

本批次按“Vue3 组件化前端控制台 → 后端 Starter → 前后端联调”的顺序推进。前端先实现可复用组件、页面级交互、API SDK 与 Mock 服务；后端随后按前端契约实现真实 API、事件、数据库表、流程图、设计文档和测试用例，并在最终联调阶段闭环 SQLBot/WrenAI 功能矩阵。

### SQLBot 与 WrenAI 功能清单基线

`feature-list.md` 是本 change 的 SQLBot 与 WrenAI 全功能清单事实源，也是后续正式文档追踪矩阵的设计期入口。该文件 SHALL 随本 change 实现和接入验证持续维护，逐项映射来源、归属 change、实现模块、公开 API/配置、存储、文档、测试、状态和限制；任何需真实租户、外部凭据或业务 UI 接入验证的能力必须保留在矩阵中并标记状态，不得从清单中删除或用泛化接口掩盖。

SQLBot 功能清单 SHALL 作为优先验收主线维护，详见 `openspec/changes/rework-ai-askdata-core-starter/feature-list.md`。当前摘要至少包含：产品介绍/工作原理、安装部署和初始化、命令行工具、工作空间/项目、数据源概览、Excel/CSV、MySQL、Oracle、PostgreSQL、SQL Server、ClickHouse、达梦、Apache Doris、AWS Redshift、Elasticsearch、Kingbase、StarRocks、Apache Hive 数据源配置；智能问数、问数上下文、SQL 生成、结果解释、图表/图片渲染、仪表板、小助手应用、基础/高级小助手、权限配置、术语配置、AI 模型配置、模型接入、Ollama 部署模型接入；训练检索、SQL 示例、Prompt/自定义提示词、项目成员和数据权限；MCP Server、嵌入式对接、DataEase 接入、Web/弹窗/页面嵌入、API 对接、MaxKB/Dify/Coze/n8n 等外部 Agent 平台调用；X-Pack 登录认证、平台对接、企业微信、钉钉、飞书、扫码登录、免登、组织用户同步、参数配置、外观设置、操作日志和企业级审计。SQLBot 文档后续新增的任何对接或功能页面都 SHALL 进入该矩阵。

WrenAI 功能清单 SHALL 作为必须补齐的 GenBI 增强线维护，详见 `openspec/changes/rework-ai-askdata-core-starter/feature-list.md`。当前摘要至少包含：自然语言问数、Text-to-SQL、Text-to-Chart、AI 摘要/报告、GenBI Insights、多语言提问、可嵌入 API；MDL 语义层、模型、列、嵌套列、关系、指标、视图、缓存视图、部署 manifest、语义版本和 deploy log；Athena/Trino、Redshift、BigQuery、DuckDB、PostgreSQL、MySQL、SQL Server、ClickHouse、Oracle、Trino、Snowflake 数据源；OpenAI、Azure OpenAI、DeepSeek、Gemini/Google AI Studio、Vertex AI、Bedrock、Anthropic、Groq、Ollama、Databricks 模型接入；语义准备/索引、语义描述、关系推荐、问题推荐、历史问题、instructions 索引和检索、SQL pair 部署、ask feedback、evaluation、dry-plan、dry-run、metadata constraints、validate、model substitute、preview data、API 文档、任务轮询、dashboard 生成/调度和遥测。与 SQLBot 重复的问数、推荐、语义模型、仪表板、API 和数据源能力 SHALL 标记“SQLBot 优先”，并说明采用 SQLBot 产品语义还是 WrenAI 底层增强。

## Goals / Non-Goals

**Goals:**

- 默认使用当前 Starter 的 LangChain4j 封装完成模型发现、聊天、embedding、RAG 与工具调用；同时把 Spring AI、Spring AI Alibaba 和 AgentScope Java 2.0+ 作为可替换运行时适配方向纳入 Core SPI 与文档。
- 在 `fw-plus-ai` 下新增 `fw-plus-ai-askdata` 模块，提供无 Spring Boot 依赖的 AskData API、请求/结果、项目边界和值对象及全部业务扩展 SPI；业务应用 SHALL 能实现这些接口替换 Starter 默认行为。
- 使用 Java 版 LangGraph 适配层编排多 Agent、检查点、故障恢复和流式执行；未启用时保留默认顺序流程。
- 对 SQLBot 和 WrenAI 已支持的数据源提供统一连接器/方言矩阵、只读 SQL 校验、物理限行、超时、审计与不确定执行保护；Doris 不再代表支持范围，只是其中一个必须验证的数据源。
- 提供自动上下文压缩、可替换共享状态存储和集群安全的恢复协议。
- 提供不绑定观测厂商的 Agent 事件契约，覆盖请求、图节点、子 Agent、模型、工具、恢复和失败。
- 为每项配置、Java SPI、恢复状态和集群部署提供可执行文档与测试场景。
- 交付详细中文设计文档、数据库设计文档、业务流程图、技术流程图和测试用例文档，并将它们作为后端 Starter 的验收证据来源。

**Non-Goals:**

- 本 change 不实现项目成员权限、训练资产、REST/MCP 管理接口、企业配置或控制台页面。
- 本 change 不直接交付 Vue3 控制台代码；前端控制台由 `rework-ai-askdata-console` 先行实现。本 change 必须按前端组件/API SDK 形成的契约输出后端 API、数据库、流程图、测试用例和联调契约。
- 不将 LangChain4j、Spring AI、Spring AI Alibaba、AgentScope Java、Agents-Flex、Python 或 Java 17+ API 泄露到 Core AskData 契约；默认 Starter 可带 LangChain4j 适配，其他运行时通过可选适配模块或业务实现接入。
- 不要求业务应用继承 Starter 内部实现或修改自动配置才能实现 AskData 接口。
- 不把内存实现当作集群生产状态存储。

## Decisions

### 分层模型与编排

在 `fw-plus-ai` 下新增 `fw-plus-ai-askdata` 模块，定义 `AskDataService`、请求/结果、项目上下文以及模型、图编排、Agent、元数据、检索、权限、会话、检查点、SQL、审计和接口发布 SPI。该模块仅依赖 Java 8 和已有 Core 公共能力，不依赖 Spring、LangChain4j 或任意存储客户端。

`fw-plus-ai-langchain4j` 实现 Core 模型和检索 SPI；Starter 保留 `LangChain4jAskDataModelRegistry` 和自动配置作为默认装配层。LangChain4j 只是默认适配，不是固定架构。Core 只认 AskData 自有模型、向量、工具、记忆、Agent 和图编排 SPI，不暴露 LangChain4j、Spring AI、Spring AI Alibaba 或 AgentScope Java 类型。业务方可以通过 Core SPI、能力插件或同类型 Spring Bean 完整替换默认行为。

运行时适配分为四类：默认 LangChain4j 适配负责当前 fw-plus 兼容和 Ollama 默认模型；Spring AI 适配负责对接 Spring AI 的 ChatModel、EmbeddingModel、VectorStore、ChatClient、Advisor 和 tool calling 抽象；Spring AI Alibaba 适配负责对接阿里生态模型、DataAgent、Workflow、MCP/A2A 和多 Agent 能力；AgentScope Java 2.0+ 适配负责对接生产级 HarnessAgent、分布式状态、模型注册、工具权限、记忆压缩、tracing、MCP/A2A/AG-UI 和多 Agent 运行能力。除默认 LangChain4j 外，其余适配均为可选依赖，必须通过 BOM 兼容性、Java 8/Spring Boot 2.7.18 可用性和最小集成测试后才能进入默认 Starter 传递依赖；不兼容时只保留 SPI 与业务自定义接入文档。

兼容性基线 SHALL 以官方文档、BOM、最小 Maven 编译和 Spring Boot 启动测试共同确认。当前已知 Spring AI 主线与 Spring AI Alibaba 官方示例和要求主要面向 Java 17 与 Spring Boot 3.x/更高版本，AgentScope Java 2.0+ 也要求 JDK 17+；因此在未证明 Java 8 / Spring Boot 2.7.18 可直接运行前，这些运行时不得作为 `fw-plus-ai-askdata-spring-boot-starter` 的传递依赖，也不得把其 Java 类型暴露到 Core 契约。正式文档必须逐项记录验证版本、官方基线、失败现象、可用替代路径和是否允许内嵌。

Java 17 运行时通过独立适配服务接入：在单独进程中使用 Spring Boot 3.x/Java 17 或 AgentScope Java 2.0+ 实现模型、embedding、工具调用、RAG、Workflow、多 Agent、状态和 tracing 能力；Java 8 Starter 仅通过 AskData Remote Runtime SPI 调用该服务。协议可以选择 HTTP/SSE、MCP Server、A2A、gRPC 或消息队列，但必须传递 `projectId`、`operatorId`、`conversationId`、`requestId`、能力 key、模型路由上下文、脱敏 Prompt 摘要和审计关联 ID；返回结果必须映射为 AskData 自有 DTO、错误码、模型尝试记录、token 用量、工具调用摘要和监控事件。Java 17 服务不得直接访问业务数据库或绕过 Java 8 Starter 的 SQL 安全、权限、审计、检查点和管理员监控日志；需要执行 SQL 时只返回受控 SQL 命令或计划，由 Java 8 侧统一校验和执行。

运行时替换文档 SHALL 逐项说明：需要引入的依赖或可选适配模块、配置前缀、如何声明能力 key、如何覆盖聊天模型、embedding、向量存储、工具调用、RAG、Agent 编排、状态存储和观测导出，如何从默认 LangChain4j/Ollama 切换到 Spring AI、Spring AI Alibaba 或 AgentScope Java 2.0+，以及哪些能力仍由 AskData Core 负责统一校验。对于无法在 Java 8 / Spring Boot 2.7.18 内嵌运行的 Spring AI、Spring AI Alibaba 或 AgentScope Java 版本，文档 SHALL 提供 Java 17 独立服务工程结构、依赖 BOM、配置样例、远程能力注册、鉴权签名、超时/重试/熔断、流式事件、错误映射、监控链路、版本兼容矩阵、回滚和联调测试用例。任何运行时替换都不得绕过项目/操作人隔离、SQL 只读安全、审计、检查点和管理员监控日志。

Starter 的默认模型和向量厂商为 Ollama：默认聊天逻辑 key 映射到 Ollama 的 `qwen3:32b`，默认 embedding 逻辑 key 映射到 Ollama 的 `bge-m3`；生产可按部署实际模型标签配置覆盖，主模型候选和兜底也可指向 Ollama 中的其他模型。该默认仅存在于自动配置层，Core 只认逻辑能力 key，不暴露或依赖 Ollama 类型；业务应用可用 LangChain4j 适配、SPI、能力插件或 Spring Bean 将任一逻辑 key 替换为其他厂商。

模型选择由不绑定 LangChain4j 的 `AskDataModelRouter` 和版本化路由策略完成。路由输入包含项目、数据源、Agent 任务、请求显式模型 key、能力标签及脱敏后的运行负载；策略依序解析请求指定、项目/数据源覆盖、任务策略和默认模型。每个模型配置可声明有序候选模型和兜底模型。主模型发生可判定的连接失败、超时、限流、熔断打开或供应商不可用时，路由器才尝试尚未调用过的候选；模型返回的业务/安全拒绝、输入不兼容或预算不足不得隐式切换。路由决策、每次尝试、失败分类和最终模型 key 必须进入检查点与观测事件。

模型兜底仅覆盖幂等的模型推理节点，不覆盖已经开始的 SQL、工具或其他外部副作用节点。任一候选返回可解析输出后，后续 SQL 仍从零开始经过同一授权和安全管线；全部候选失败时返回可诊断的模型不可用错误，不得伪造结果或回退为未配置模型。

新增可选 `AskDataGraphOrchestrator` SPI 及 Java LangGraph 适配实现。启用且类路径存在兼容构件时，它把元数据、检索、Prompt、SQL 生成、校验、执行、解释和子 Agent 合并表示为状态图节点；未启用或依赖缺失时由 `DefaultAskDataFlowExecutor` 与 `SequentialAskDataAgentDispatcher` 执行相同的逻辑顺序。

替代方案是在核心执行器中直接依赖 LangGraph Java。该方案会把可选依赖和 Java 版本风险传播给所有使用方，因此不采用。BOM 只管理经 Java 8 编译验证的 LangGraph Java 构件；不兼容的版本不得进入 Starter。

### 一个 Starter 与业务二次开发

最终对使用方只发布一个 `fw-plus-ai-askdata-spring-boot-starter`。它传递引入 Core 的 `fw-plus-ai-askdata` 契约、LangChain4j 适配和默认自动配置。业务应用可以只覆盖单个 SPI，也可以实现 `AskDataService` 后覆盖默认服务，并按自身 Web、RPC、MCP 或消息协议发布接口；Starter 不强制提供某一种业务 API。

全部默认实现都以 Core 接口和能力描述符对外暴露：业务应用可以覆盖模型路由、向量、长期记忆、SQL 管线、状态存储、Agent、图编排、观测或任何单项默认能力，也可以同时提供 `AskDataService`、`AskDataAgentDispatcher`/图编排器和存储能力以完全接管流程。自动配置只在缺少对应业务 Bean/能力时生效，不得要求继承 Starter 内部类或修改其源码。

业务接入体验 SHALL 以“最少配置可运行、按需覆盖可扩展”为目标。后端 Starter 必须提供最小 Ollama + JDBC 数据源示例、Spring Boot 自动配置属性、可直接注入的门面服务、统一错误模型、事件/审计回调和配置校验诊断；业务系统只需要接入认证上下文、项目/操作人获取、数据源元数据和可选权限 SPI 即可完成基础问数。深度接入时通过能力 key、SPI、Spring Bean、SQL 插件和后续前端组件/SDK 扩展，不要求复制默认流程、fork Starter 或理解内部编排细节。

前端控制台后续 SHALL 以组件库方式交付：项目/成员、数据源、语义模型/训练、问数会话、多提问、推荐、仪表盘/助手、监控审计、企业配置、嵌入/MCP 配置等均为可复用业务组件，并通过统一 TypeScript API SDK 与后端契约交互。业务应用可以整包使用控制台，也可以只嵌入部分组件并覆盖权限、路由、主题和 API 基址。

替代方案是为每项能力发布多个业务 Starter。该方案会增加依赖组合和版本兼容负担，违背单 Starter 目标，因此不采用。

### 扩展边界与能力注册

Core 的每个可变能力都以窄接口定义，并携带稳定的能力标识和契约版本：模型、向量检索、语义推荐、图编排、状态存储、权限、SQL 执行、事件发布和对外接口均不在 Core 中硬编码供应商、驱动或传输协议。Starter 通过 Spring 条件装配和配置 key 选择默认实现，业务应用通过 SPI、Spring Bean 或能力注册表新增、替换或组合实现。

配置只选择能力，不承载实现细节；实现专属参数由各适配器自己的配置对象管理。未知能力 key、版本不兼容或集群不满足的能力组合必须在启动时给出明确错误，而不是回退到隐式默认实现。

`AskDataCapabilityProvider` SHALL 返回带能力类型、稳定 key、支持的契约版本区间、优先级、配置命名空间和可选依赖说明的描述符；`AskDataCapabilityRegistry` 在启动期完成冲突检测和选择。模型供应商、向量库适配器、SQL 插件、存储和事件导出器均以此方式登记。Core 只传递 AskData 自有请求、结果和值对象，具体客户端由适配器私有持有；业务方可以新增实现而不修改 Core 或 Starter。

### 业务 SQL 组装与插件管线

AskData 不把 SQL 限定为“模型生成后直接 JDBC 执行”。Core 定义不可变的 `AskDataSqlCommand`、受控 SQL 片段和具名参数模型，以及类似 MyBatis 的有序 `AskDataSqlInterceptor` 链。插件按声明阶段参与：意图到模板/片段组装、参数绑定、最终安全校验前改写、执行前准备、执行后结果映射和审计补充；可用于业务固定查询、租户谓词、行列范围、指标口径、方言函数、脱敏和结果格式化。

插件不得直接获得裸连接或绕过 `AskDataSqlValidator`。动态值必须通过参数绑定表达，结构性 SQL 片段必须声明来源和可审计的能力 key；所有插件产出的最终 SQL 都重新执行单语句、只读、授权对象、方言、物理限行和超时校验。拦截器排序、短路/拒绝行为、失败策略和契约版本由注册表显式解析，歧义或不兼容配置在启动期失败。

### Agent 可观测契约与导出器

`fw-plus-ai-askdata` 定义带 `projectId`、`requestId`、`conversationId`、`graphNodeId`、`agentId`、`instanceId` 和时间戳的不可变 Agent 运行事件。事件覆盖接收、排队、节点开始/结束、模型和工具调用、token 用量、压缩、检查点、恢复、降级和失败。默认发布器可以为空实现，业务方或后续可观测 Starter 可以通过 SPI 输出指标、链路、日志或告警。

监控日志不是可选的空发布器：每个对话必须产生 `ConversationStarted`、请求/子问题、模型路由、Agent 节点、SQL 校验/执行、检查点/恢复以及完成/失败等生命周期事件。事件均绑定 `projectId + operatorId + conversationId`，并可继续关联请求、子问题、图节点和实例。默认 `AskDataMonitoringLogStore` 为单机内存环形存储；业务可按能力 key 改用 JDBC、Redis、消息系统或自定义实现。读取授权由 `AskDataMonitoringLogAccessPolicy` SPI 决定：项目管理员可查本项目全部日志，跨项目平台管理员必须有业务显式授权，普通操作人默认没有监控日志查询权限。事件发布和外部导出异步失败隔离，脱敏后的本地监控日志仍须保留，不得阻塞或改变对话结果。

LangSmith 和 Langfuse 只作为可选导出契约与适配方向：Core 事件、脱敏字段、批量异步、失败隔离和关联标识由 AskData 契约定义；真实 LangSmith/Langfuse SDK、OpenTelemetry Collector、租户凭据和外部链路连通由业务观测适配或后续 Starter 接入验证。导出失败只允许产生本地诊断事件，不得阻断 AskData 请求。

原始问题、Prompt、SQL 结果和凭据不得默认写入事件；需要诊断时只允许受配置控制的脱敏摘要。集群实例通过相同关联标识上报事件，使监控层可重建一次图执行链路。

### 可恢复状态图与集群一致性

每个稳定节点写入 `AskDataCheckpoint`：包含请求指纹、项目/数据源边界、图版本、节点状态、压缩上下文、SQL 校验结果、恢复 token 和审计关联 ID。恢复仅从已完成且可重入节点继续。

集群部署要求实现 `AskDataConversationStore` 的共享持久化版本，并以请求指纹和恢复 token 实现幂等租约。SQL 执行节点在提交前写入 `EXECUTING`，完成后写入结果摘要；实例故障时状态仍为 `EXECUTING` 的节点必须返回“执行状态不确定”，不得自动重放。内存存储仅用于单实例开发。

Core 层定义状态存储能力矩阵：内存实现是所有状态类别的默认实现，仅用于单机临时会话、检查点、审计和长期记忆；JDBC 实现用于单机或集群的事务性项目配置、会话、检查点与审计；Redis 实现用于集群的热状态、租约、幂等键和检查点协调；向量知识通过既有 LangChain4j `EmbeddingStoreRegistry` 选择已注册的向量存储。向量适配同时提供 `memory` 能力，用内存 embedding store 支持开发和单机临时检索。不同状态类别可通过能力 key 组合使用多个存储实现，例如 JDBC 会话加 Redis 租约加持久化向量库。状态 SPI 必须允许业务方提供自定义实现。集群模式发现本地内存状态或内存向量实现时必须在启动期失败，不得静默降级。

Agent 运行状态由独立 `AskDataAgentStateStore` 管理，至少包含图状态、节点/子任务状态、计划、工具调用摘要、模型路由尝试、步骤日志、恢复位置与失败策略结果。它与会话/检查点接口可复用同一物理存储，但按能力 key 独立选择：单机未配置时默认内存；业务可选择 JDBC、Redis 或自定义实现；集群要求共享实现和租约/幂等语义，禁止把 Agent 状态静默保留在某个实例内存中。

替代方案是由 LangGraph 本身保存全部运行状态。该方案无法保证与当前 AskData API、项目隔离和 SQL 不重放约束一致，因此 LangGraph 状态通过 AskData 检查点 SPI 持久化。

### 多 Agent、压缩与降级

主 Agent 按图调用元数据、术语/样例检索、选表、SQL 生成、校验、执行和结果解释子 Agent。每个子 Agent 必须声明超时、重试上限和失败策略；失败只能记录为可诊断步骤结果，策略决定继续、降级或终止。

当图状态超过阈值时，`AskDataContextCompressor` 使用默认 LangChain4j 聊天模型生成摘要，同时保留项目 key、数据源 key、模型 key、方言、授权过滤条件、SQL、安全校验、已完成节点和错误摘要。压缩前后的结构化字段必须可用于恢复。

### 长期记忆与权限边界

`AskDataLongTermMemory` 以独立 SPI 提供 `store`、`recall`、`fetch`、`expire` 和 `forget` 语义，记录项目、数据源、已认证操作人、会话、语义版本、授权范围、来源、置信度、保留策略和脱敏内容。项目是所有记忆和会话的前提分区，操作人是项目内的第二层分区；因此任何个人记忆均以 `projectId + operatorId` 归属。它用于保存用户偏好、业务术语、经确认的 NL-SQL、结果解释摘要和可复用语义线索，不替代检查点，也不把完整原始 Prompt、SQL 结果或凭据作为默认记忆内容。

流程在模型调用前由记忆检索策略先按项目、再按已认证操作人、数据源和当前权限范围召回有限、可追溯的条目；召回结果仅作为受控上下文，SQL 仍须重新生成和校验。长期记忆实现可复用默认内存、向量存储、JDBC、Redis 或业务存储，采用能力注册表选择；跨项目、跨操作人、跨租户、超期、权限撤销或语义版本不兼容的条目必须被过滤。业务方可定义保留期、人工确认门槛、删除与遗忘策略。

推荐能力以 `projectId` 为必填前提、从认证上下文获取的 `operatorId` 为项目内画像边界：先在当前项目、数据源和权限范围内构造候选问题、指标、语义对象或已确认问数，再只使用当前操作人在该项目内的偏好、反馈、长期记忆和历史确认行为排序。推荐响应必须携带项目、操作人、候选来源与过滤原因；缺少项目或操作人身份时只能返回不使用个人行为的显式通用推荐，且默认关闭。不同操作人的个人偏好、历史和排序特征不得参与彼此的推荐，即使他们位于同一项目或租户。

### 多提问分解与聚合

`AskDataRequest` 支持单问题、显式问题列表和自然语言复合问题。`AskDataQuestionPlanner` 将复合问题拆为带稳定子问题 ID、依赖关系、项目/数据源边界和执行预算的计划；业务方可以通过 SPI 替换拆分规则或直接提交计划。无依赖子问题可由图编排或顺序执行器在配置的最大并发度内并行处理，存在数据/结果依赖的子问题严格按依赖顺序执行。

每个子问题独立完成权限过滤、长期记忆召回、模型路由/兜底、SQL 插件、最终安全校验、执行和审计。聚合器按原始计划返回每个子问题的 SQL、结构化结果、错误或跳过原因，并保留父请求关联 ID；部分失败不会伪造聚合成功。多提问的取消、超时、资源预算和恢复状态都以子问题为粒度记录，执行中的 SQL 仍禁止自动重放。

### 数据源方言矩阵与安全执行

方言注册表不以 Doris 作为支持范围上限，而是建立 SQLBot 与 WrenAI 的统一数据源/方言能力矩阵。矩阵第一批至少覆盖 SQLBot 文档中的 MySQL、Oracle、PostgreSQL、SQL Server、ClickHouse、达梦、Apache Doris、AWS Redshift、Elasticsearch、Kingbase、StarRocks、Apache Hive、Excel/CSV，并覆盖 WrenAI 公开仓库中的 BigQuery、DuckDB、Snowflake、Redshift、SQL Server、ClickHouse、Oracle、PostgreSQL、MySQL 等连接器。重复项合并到同一个能力 key；同名但行为不同的连接器必须说明驱动、认证、分页、schema、函数、时间类型和限制差异。

每个数据源能力 SHALL 明确连接器状态、元数据同步能力、Prompt 方言规则、SQL 解析/校验规则、分页/limit 改写、只读执行方式、系统 schema 限制、表列/行权限支持、dry-plan 支持、结果类型映射、测试数据和验证证据。需真实数据源或外部依赖验证的项必须在矩阵中标记验证状态，不得用 Doris、MySQL 或任意单一数据源的通过结果代表全量支持。

校验器先解析单个只读语句，再执行元数据允许表校验、系统 schema 限制、物理限行改写和超时设置。执行器使用只读账户和查询级超时；任何校验失败均不得建立执行连接。业务 SQL 插件仅能在明确阶段基于受控命令对象追加内容，追加后的 SQL 必须重新进入完整校验管线。

### 外部系统对接契约

SQLBot 的 MCP Server、嵌入式对接和 DataEase 接入 SHALL 作为独立外部系统对接能力纳入批次。Core change 负责抽象后端契约和审计边界，API/MCP change 负责真实服务端接口，Console change 负责 Vue3 组件和嵌入式前端能力。

MCP Server 契约 SHALL 覆盖 SSE 和可选 Streamable HTTP 传输、服务地址/端口配置、图片/图表资源访问地址、工具清单和鉴权状态。内置工具至少包含 `mcp_start`、`mcp_question`、`mcp_ws_list` 和 `mcp_datasource_list`：`mcp_start` 返回 `access_token` 与 `chat_id`，`mcp_question` 在同一上下文内提交自然语言问题并返回 SQL、结构化结果、可视化结果与图表图片地址，列表工具按当前认证身份返回可访问工作空间和数据源。所有 MCP 调用必须绑定项目、操作人、会话、权限、审计和监控日志，并支持 MaxKB、Dify、Coze、n8n 等外部 Agent 平台的调用流程。

嵌入式对接契约 SHALL 覆盖小助手嵌入和页面嵌入，支持基础应用、高级应用、浮窗、全屏和区域页面模式。基础应用使用 SQLBot/AskData 自身数据源和游客/员工权限；高级应用支持宿主系统权限和宿主 API 数据源，后端不得保存宿主凭据，凭证通过受控参数、Header、localStorage/Cookie 映射或业务回调即时传递。页面嵌入必须由后端生成 token，避免泄露 app secret；`online`、`userFlag`、appId、account、跨域白名单、嵌入 token、应用模式和数据源范围都必须进入配置与审计。

DataEase 接入 SHALL 兼容 SQLBot 文档定义的高级应用方式：SQLBot 侧创建高级应用、配置跨域、固定调用 `/de2api/sqlbot/datasource` 获取 DataEase 数据源，凭证映射支持 `user.token`/`de_v2_user.token` 到 `x-de-token`，敏感数据支持 AES-Key 加密，DataEase 侧配置 SQLBot URL 与应用 ID 后在快捷入口打开问数能力。后端契约必须支持 DataEase 数据源、Excel/API 数据源代理地址、DataEase 权限上下文、数据集/表字段/权限 SQL 描述和对接日志；前端 Vue3 组件必须提供 DataEase 集成配置、校验和状态展示。

### 文档交付结构

文档是核心 Starter 的实现验收项，必须使用中文并随源码维护，至少拆分为：架构与模块依赖图；快速开始与最小 Ollama 配置；完整配置属性参考与逻辑能力 key；聊天/向量模型、动态路由、候选和兜底策略；单机内存、JDBC、Redis、向量存储和 Agent 状态的组合矩阵；项目/操作人对话隔离、长期记忆和推荐；模型、向量、SQL、存储、Agent、观测与接口发布插件 SDK；Doris 与 SQL 安全策略；多 Agent、多提问、流式与恢复；管理员监控日志、LangSmith/Langfuse；单机和集群部署；业务覆盖默认实现；从现有 AskData API 迁移；常见故障、恢复操作、容量/脱敏建议及可运行示例。

详细设计文档 SHALL 作为后端 Starter 的主设计入口维护，至少包含模块边界、包结构、核心领域对象、SPI 清单、默认实现、自动配置条件、扩展覆盖方式、异常模型、配置模型、兼容迁移、前端联调契约和实现顺序。所有公开接口、SPI、关键默认实现、安全边界、状态恢复和联调契约对应的 Java 源码必须写详细中文注释，说明业务含义、调用约束、扩展方式和禁止行为；注释应解释为什么有该边界，而不是重复代码表面语义。

流程图 SHALL 同时包含业务流程图和技术流程图，优先使用 Mermaid 源码随文档维护并在文档中渲染。业务流程图至少覆盖：项目创建/成员授权、数据源接入、元数据同步、语义模型/训练、问数会话、多问题拆分、推荐、仪表盘/助手、管理员监控、企业配置和审计闭环。技术流程图至少覆盖：请求入口、认证上下文、能力注册、模型路由、RAG/记忆召回、多 Agent 图节点、SQL 插件链、dry-plan、安全校验、执行、结果解释、检查点、故障恢复、监控导出和集群租约。

数据库设计文档 SHALL 覆盖后端 Starter 与后续控制台/API 所需的逻辑表模型，至少包含：项目、成员/角色/授权、数据源、数据集/表/列元数据、语义模型/MDL、指标/关系/视图、术语、SQL 样例、训练文档、向量索引引用、会话/消息/子问题、长期记忆、个性化推荐画像与反馈、SQL 审计、监控日志、Agent 图状态/节点状态/租约、模型配置/路由策略/API Key、MCP/API 发布、企业集成配置、操作日志和能力插件注册。每张表需要字段说明、主键/唯一键、索引、租户/项目/操作人隔离字段、脱敏字段、保留策略、单机/集群存储建议和迁移版本说明。

测试用例文档 SHALL 以验收矩阵驱动，按 Core 单元测试、SPI 契约测试、自动配置测试、SQL 安全测试、存储一致性测试、模型路由/兜底测试、多 Agent/恢复测试、长期记忆/推荐权限测试、监控审计测试、文档示例编译测试和后续前后端联调测试分类。每个用例必须包含前置条件、输入、期望结果、验证命令或人工验证证据、覆盖的 SQLBot/WrenAI 功能项和失败诊断。

每一项配置和扩展 SPI 必须包含用途、默认值、取值范围、单机/集群限制、最小示例和失败表现；安全、恢复、模型切换和存储选择必须给出明确的禁止行为。文档示例不得包含真实凭据，并在 CI 中执行或编译其 Java 配置片段。

文档首页必须维护“已实现功能清单”，以模块和版本为单位列出实际可用能力、公开配置/API、支持的单机/集群模式、最小验证证据与已知限制；每次实现任务完成后同步更新。仅完成契约但未完成真实外部联通、可选依赖未验证或需业务模块接入的能力不得标为默认可用，应在业务覆盖接口章节说明范围，避免将契约覆盖误报为外部系统已连通。

文档同时维护 SQLBot v1 功能追踪矩阵，以 `https://gitee.com/fit2cloud-feizhiyun/SQLBot` 和 `https://sqlbot.org/docs/v1/` 为基线。矩阵逐项映射 SQLBot 功能到本批次 change、实现模块、公开接口/配置、用户文档和自动化或人工验证证据；缺失项必须标记阻塞，不能以“后续可能支持”代替。核心 change 仅交付问数底座，其余 SQLBot 功能由治理、训练检索、API/MCP、企业、观测和控制台 change 分别闭环；所有矩阵项完成才是“功能完整”的验收条件。

文档同时维护 WrenAI 企业级 GenBI 功能追踪矩阵，以 Canner/WrenAI 的公开仓库为基线。矩阵逐项映射 MDL/语义模型、上下文富化、dry-plan、结构化错误、值画像、评测、推荐、Agent 生成/部署仪表盘、多数据源连接器和工作流/SDK 能力到本批次 change、实现模块、公开接口/配置、用户文档和验证证据。与 SQLBot 功能重复的条目必须标记“SQLBot 优先”，并说明采用 SQLBot 产品语义还是 WrenAI 底层增强。

## Risks / Trade-offs

- [LangGraph Java 构件与 Java 8 不兼容] → BOM 引入前执行 Java 8 编译和最小状态图集成测试；不通过时只保留 SPI，禁止作为 Starter 传递依赖。
- [Spring AI、Spring AI Alibaba 或 AgentScope Java 2.0+ 无法在 Java 8 / Spring Boot 2.7.18 内嵌运行] → 文档记录官方基线和验证失败证据；Java 8 Starter 只保留 Remote Runtime SPI，不传递不兼容依赖；通过 Java 17 独立适配服务对接模型、Agent 和观测能力。
- [共享状态存储不可用或产生网络分区] → 取得或续约租约失败时拒绝恢复；返回可诊断错误而非降级为本地恢复。
- [不同存储实现的行为不一致] → 以 Core 状态 SPI 的契约测试验证内存、JDBC、Redis 和业务自定义实现的版本、租约、幂等与状态转换语义。
- [上下文压缩遗漏授权或 SQL 状态] → 压缩结果保留结构化安全字段，并在恢复前重新执行权限与 SQL 校验。
- [Agent 重试导致重复外部动作] → SQL 节点不自动重放，工具节点使用幂等键，副作用状态记录在检查点中。
- [模型输出不稳定] → 严格 JSON/SQL 解析、有限重试、失败步骤日志和可配置的人工接管入口。
- [模型单点故障或候选模型产生语义漂移] → 按故障分类执行有限候选切换，记录模型路由事件；候选模型仍须经同一结构化输出和 SQL 安全校验，全部失败时返回明确错误。
- [插件绕过安全边界或插件顺序不确定] → 以不可变命令、参数绑定、阶段化拦截器和启动期排序/版本校验约束插件；所有最终 SQL 强制进入同一安全校验与审计管线。
- [长期记忆泄露过期或越权语义] → 所有 recall 以项目、主体、数据源和实时权限过滤；记忆包含保留期与语义版本，权限撤销、过期或不兼容时不参与上下文。
- [推荐跨人泄露偏好或行为] → 认证上下文中的 `operatorId` 是个性化排序的强制边界；项目/权限只过滤候选，跨操作人特征不读取、不聚合到个人推荐结果。

## Migration Plan

1. 保持现有自动配置和 Java API，新增配置项默认关闭图编排与共享状态存储。
2. 引入兼容的 LangGraph Java 可选适配和持久化会话存储实现，先以影子执行比对顺序流程与图流程的步骤结果。
3. 对 Spring AI、Spring AI Alibaba 和 AgentScope Java 2.0+ 分别执行兼容性验证；若无法在 Java 8 / Spring Boot 2.7.18 内嵌运行，则创建 Java 17 独立适配服务样例，通过 Remote Runtime SPI 完成模型、embedding、Agent、流式事件和错误映射联调。
4. 在 Doris 测试环境启用只读账户、查询超时、审计和共享检查点，验证实例中断后的恢复与不重放约束。
5. 发布配置与运维文档；生产切换失败时关闭图编排或远程运行时能力 key，并回退到本地 LangChain4j/Ollama 顺序流程，保留检查点供人工诊断。

## Open Questions

- 将在实现阶段锁定经 Java 8 编译验证的 Java LangGraph 构件版本和 BOM 坐标；该构件必须支持当前 Starter 的依赖树。
- 集群默认状态存储将优先复用现有 Redis Starter；关系型数据库实现作为同一 SPI 的后续可选实现。
