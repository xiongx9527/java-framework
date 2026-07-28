# Comet Design Handoff

- Change: rework-ai-askdata-core-starter
- Phase: design
- Mode: compact
- Context hash: ccb2798351a11b3d76f0515ad65f52e0c03828fd2ccd32c0dcc61f0e2c2f4822

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/rework-ai-askdata-core-starter/proposal.md

- Source: openspec/changes/rework-ai-askdata-core-starter/proposal.md
- Lines: 1-48
- SHA256: e985cdc2e27194c3ae2e6d6e242375fca393eb0abc1b58c9d89c942d281a95a5

```md
## Why

当前 `fw-plus-ai-askdata-spring-boot-starter` 已具备基础 Text-to-SQL 流程，但无法作为 SQLBot 后端重构的稳定底座：模型适配主线不够收敛，Doris 场景的方言与安全边界不足，问数会话也缺少可运营的审计、恢复与集群运行契约。

本批次的实施顺序固定为先实现 Vue3 组件化前端控制台、前端 API SDK、Mock 服务和页面级交互原型，再实现后端 Starter 并按前端契约对齐 API、事件和数据库设计，最后进行前后端联调验收。SQLBot 开源项目、SQLBot v1 文档和其中企业级能力是优先级最高的功能基线；Canner/WrenAI 的企业级 GenBI 能力也必须覆盖，但当产品语义或命名与 SQLBot 重叠时，以 SQLBot 为主，吸收 WrenAI 的语义模型、dry-plan、推荐和部署治理等增强能力。

本 change 将核心问数能力收敛到 Java 8，保留当前 Starter 的 LangChain4j 封装作为默认适配，但不把 LangChain4j 固定为唯一运行时；Spring AI、Spring AI Alibaba、AgentScope Java 2.0+ 和业务自定义运行时都必须能通过 Core SPI、能力插件或 Spring Bean 替换。若 Spring AI、Spring AI Alibaba 或 AgentScope Java 2.0+ 当前官方基线无法在 Java 8 / Spring Boot 2.7.18 中直接运行，文档 SHALL 明确标记不兼容原因、验证版本、不能内嵌的依赖边界，并给出 Java 17 独立适配服务的 HTTP/MCP/A2A/消息等对接方式。LangGraph Java 仅作为可选的多 Agent 图编排适配，为后续项目治理、训练检索、API/MCP、企业能力、控制台与联调验收提供稳定后端底座。

## What Changes

- 保持并完善当前封装的 LangChain4j 模型注册与自动配置，以 LangChain4j 作为默认聊天模型、embedding、RAG 和工具调用适配；Starter 默认模型与向量厂商为 Ollama，但 Core 不依赖 LangChain4j、Spring AI、Spring AI Alibaba、AgentScope Java、Agents-Flex 或 Python 运行时类型。
- 支持按项目、数据源、Agent 任务与请求动态路由模型；为主模型的超时、限流、熔断和不可用定义可配置候选模型链与兜底模型，记录完整切换审计，且不将模型调用失败误判为 SQL 执行成功。
- 建立双基线功能追踪矩阵：SQLBot v1 全量功能矩阵为硬验收主线，覆盖开源与企业级后端能力；WrenAI 企业级 GenBI 增强矩阵为必须覆盖的补强基线。重复能力 SHALL 按 SQLBot 的项目、权限、训练、嵌入、MCP、系统和企业配置语义落地，WrenAI 的 MDL/语义层、上下文富化、dry-plan、结构化错误、推荐和部署治理能力作为后端增强实现。
- 在 `fw-plus-core/fw-plus-project/fw-plus-ai` 新增独立 `fw-plus-ai-askdata` 模块，提供无 Spring 依赖的 AskData 领域对象、服务接口和 SPI；在现有 `fw-plus-ai-langchain4j` 提供 LangChain4j 适配，使业务方能够重新实现模型、向量检索、编排、元数据、检索、权限、存储、SQL 组装/执行、审计和对外接口。
- 增加可选 LangGraph Java 编排适配，复用既有 `AskDataFlowExecutor`、`AskDataAgentDispatcher`、检查点和会话存储接口，实现可检查点、可恢复的多 Agent 执行图；未启用适配时保留既有顺序执行行为。
- 扩展元数据、Prompt、连接器与方言规则，建立 SQLBot 和 WrenAI 已支持数据源的统一能力矩阵；MySQL、Oracle、PostgreSQL、SQL Server、ClickHouse、达梦、Apache Doris、AWS Redshift、Elasticsearch、Kingbase、StarRocks、Apache Hive、Excel/CSV、BigQuery、DuckDB、Snowflake 等均 SHALL 纳入支持和验证计划，任一未实现项必须在矩阵中标记阻塞或后续项，不得被 Doris 优先验证掩盖。
- 增加类似 MyBatis 拦截器链的 SQL 插件扩展：业务插件可按阶段组装受控 SQL 片段、绑定参数、附加租户或数据范围条件、改写查询和映射结果；最终 SQL 仍必须经过统一的权限、只读、限行、超时和审计管线，避免生成 SQL 越权或造成资源失控。
- 定义自动上下文压缩、可持久化检查点、幂等恢复和集群共享运行状态的核心契约；默认实现保持轻量且可替换，生产集群通过外部共享存储实现状态一致性。
- 定义以项目为前提、以已认证操作人为第二层隔离的对话、长期记忆和推荐契约，支持事实、偏好、已确认问数和语义线索的存储、召回、过期与遗忘；所有会话必须绑定 `projectId + operatorId`，推荐在当前项目候选集中按操作人排序，后端和策略均可由业务插件扩展。
- 支持一次请求中的复合提问与批量提问：将问题拆分为可追溯子任务，按依赖关系和受限并发执行，并聚合每个子问题的结果、错误、模型路由与 SQL 审计。
- 所有状态、长期记忆与向量能力支持内存、JDBC、Redis、向量库或业务自定义的多个可组合存储实现；默认内存实现仅面向单机临时运行，向量也提供内存模式，集群不得静默使用本地存储。
- 业务应用可通过公开 SPI、能力插件或 Spring Bean 覆盖任一默认能力，也可完整重新实现 AskData 服务、Agent 编排和全部状态存储；Agent 运行状态支持多种存储方式，单机默认内存，集群必须使用共享实现。
- 文档 SHALL 详细说明如何从默认 LangChain4j/Ollama 替换为 Spring AI、Spring AI Alibaba、AgentScope Java 2.0+ 或业务自定义运行时，覆盖依赖、配置、能力 key、Bean/SPI 覆盖、模型/向量/工具/RAG/Agent/状态/观测映射、限制、回滚和验证用例；若运行时只支持 Java 17+ 或 Spring Boot 3.x+，必须写清楚不兼容的官方基线、验证结论、禁止作为 Java 8 Starter 传递依赖的原因，以及 Java 17 侧车/独立适配服务的部署、鉴权、上下文传递、错误映射、审计和回滚方案。
- 定义不绑定观测厂商的 Agent 运行事件、关联标识和发布 SPI，为后续监控、告警以及 LangSmith、Langfuse 和业务平台导出提供一致数据源。
- 每次对话必须记录完整监控日志，覆盖创建、提问、子问题、模型路由、Agent 节点、SQL、安全校验、恢复、完成与失败；日志按项目、操作人和会话关联，默认只对已授权管理员开放查询，并采用可替换、默认内存的异步存储。
- 所有第三方集成、存储、模型、向量库、SQL 插件、图编排与推荐策略 SHALL 通过版本化 SPI 和能力注册表装配，禁止将具体实现、供应商类型或业务 SQL 规则写死在 Core 契约中。
- 后端源码 SHALL 使用详细中文注释解释公开接口、SPI 扩展点、能力注册、模型路由、SQL 安全、权限隔离、状态恢复、集群存储、监控日志和前后端联调契约；注释用于说明业务语义、边界和风险，不得写成空泛的逐行翻译。
- 提供可验收的详细 Starter 文档及已实现功能清单：架构与模块、依赖、完整配置、Ollama、模型路由、存储矩阵、插件 SDK、SQL 安全、多 Agent、多提问、长期记忆、管理员监控、集群、覆盖扩展、迁移、故障恢复和排障示例；清单只列经验证的实现并标记限制。`openspec/changes/rework-ai-askdata-core-starter/feature-list.md` SHALL 作为 SQLBot 与 WrenAI 全功能清单和后续追踪矩阵的设计期事实源；项目管理、成员权限、训练检索、REST/MCP 管理 API、企业集成、前端控制台与前后端联调按 Vue3 先行、后端对齐、最终联调的顺序在后续 change 闭环。
- SQLBot 文档中出现的所有外部系统对接能力 SHALL 全部支持并进入追踪矩阵，当前至少包括模型接入/Ollama、MCP Server、嵌入式对接、DataEase 接入、X-Pack 登录认证、X-Pack 平台对接、X-Pack 参数配置、企业微信、钉钉、飞书、MaxKB、Dify、Coze、n8n 等对接方式；任一能力未实现或未验证时必须标记阻塞/后续，不得用泛化接口替代。
- 文档 SHALL 包含详细设计文档、技术流程图、业务流程图、数据库设计文档和测试用例文档。流程图必须覆盖业务问数闭环、管理员治理闭环、训练/语义模型闭环、模型路由与兜底、多 Agent 执行、SQL 安全、状态恢复、监控审计、模型接入、MCP Server、嵌入式集成、DataEase 集成、企业微信/钉钉/飞书平台对接、登录认证、参数配置和前后端联调；数据库设计文档必须覆盖后端 Starter 所需的项目、成员权限、数据源、语义模型/训练、会话、长期记忆、推荐、SQL 审计、监控日志、Agent 状态、模型/API Key、MCP/API、嵌入令牌、DataEase 对接、平台对接、登录认证、企业配置和扩展插件表结构；测试用例文档必须覆盖单元、集成、契约、故障恢复、安全、MCP/嵌入/DataEase/企业平台对接和前后端联调用例。
- 保持 Java 服务 API 与自动配置方式兼容 Spring Boot 2.7.18 和 Java 8。

## Capabilities

### New Capabilities

- `askdata-core-contracts`: 在 Core AI 层提供稳定、可二次开发的 AskData Java 领域模型、服务接口和扩展 SPI。
- `askdata-agent-observability-contracts`: 在 Core AI 层提供可二次开发的 Agent 运行事件和观测发布 SPI。

### Modified Capabilities

- `askdata-query-engine`: 收敛为可插拔 Java AI 运行时主线，默认适配当前 LangChain4j 封装，新增可选 LangGraph Java、多 Agent 和后续 Spring AI、Spring AI Alibaba、AgentScope Java 2.0+ 适配边界，并强化 SQLBot/WrenAI 数据源方言矩阵、自动上下文压缩、故障恢复、集群运行、SQL 安全执行和可审计会话的行为要求。

## Impact

- 影响 `fw-plus-core` 中新增的 `fw-plus-ai-askdata`、现有 `fw-plus-ai-langchain4j`，以及 `fw-plus-ai-boot-autoconfigure` 中的 `askdata` 包、其 `META-INF/spring.factories` 注册和现有 AskData 测试。
- 影响 `fw-plus-ai-askdata-spring-boot-starter` 的传递依赖边界及 `fw-plus-ai-boot-dependencies` 的 BOM 管理。
- 为后续项目治理、训练检索、REST/MCP、Vue3 控制台和运行时适配 change 提供稳定的 Java 接口与扩展点；Core 契约不直接依赖 LangChain4j、Spring AI、Spring AI Alibaba、AgentScope Java、Agents-Flex、Python 运行时或 Vue3 控制台类型。

```

## openspec/changes/rework-ai-askdata-core-starter/design.md

- Source: openspec/changes/rework-ai-askdata-core-starter/design.md
- Lines: 1-195
- SHA256: 321134dd92f846ab60a68a88426e0e76853a1ff95edec169ee46af95e6e6b7fc

[TRUNCATED]

```md
## Context

`fw-plus-ai-askdata-spring-boot-starter` 已在 AI 聚合器和 BOM 中发布，自动配置模块已提供 LangChain4j 模型注册、顺序流程执行器、Agent 派发、上下文压缩和内存检查点。`fw-plus-core` 的 AI 聚合已提供 `fw-plus-ai-base`、`fw-plus-ai-mcp` 和 `fw-plus-ai-langchain4j` 边界，但尚无独立 AskData 模块，领域类型仍耦合在自动配置模块。系统仍缺少 SQLBot v1 所需的生产级编排、全量数据源/方言矩阵、安全恢复、外部系统对接和集群状态边界。

该 change 必须保持 Spring Boot 2.7.18 与 Java 8 兼容，并作为项目管理、训练检索、REST/MCP、企业配置和控制台后续 change 的基础。`https://gitee.com/fit2cloud-feizhiyun/SQLBot` 与 `https://sqlbot.org/docs/v1/` 是整个批次的优先功能验收基线：每项开源和企业级功能都必须映射到 batch change、实现模块、文档和验证证据，全部闭环前不得宣称功能完整。Canner/WrenAI 的企业级 GenBI 能力也是必须覆盖的补强基线：MDL 语义模型、上下文富化、dry-plan、结构化错误、值画像、评测、推荐、Agent 生成/部署仪表盘和多数据源治理均进入追踪矩阵；与 SQLBot 重叠时采用 SQLBot 的产品边界和命名，吸收 WrenAI 的底层设计优势。

本批次按“Vue3 组件化前端控制台 → 后端 Starter → 前后端联调”的顺序推进。前端先实现可复用组件、页面级交互、API SDK 与 Mock 服务；后端随后按前端契约实现真实 API、事件、数据库表、流程图、设计文档和测试用例，并在最终联调阶段闭环 SQLBot/WrenAI 功能矩阵。

### SQLBot 与 WrenAI 功能清单基线

`feature-list.md` 是本 change 的 SQLBot 与 WrenAI 全功能清单事实源，也是后续正式文档追踪矩阵的设计期入口。该文件 SHALL 随后续实现持续维护，逐项映射来源、归属 change、实现模块、公开 API/配置、存储、文档、测试、状态和限制；任何未实现、未验证或需后续 change 的能力必须保留在矩阵中并标记状态，不得从清单中删除或用泛化接口掩盖。

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

```

Full source: openspec/changes/rework-ai-askdata-core-starter/design.md

## openspec/changes/rework-ai-askdata-core-starter/tasks.md

- Source: openspec/changes/rework-ai-askdata-core-starter/tasks.md
- Lines: 1-79
- SHA256: f0e32911178a73cccfd34ca59dc53a39d225dce80ca4da0a1cb629eaa5af5fd3

```md
## 1. Core API and compatibility

- [ ] 1.1 在 `fw-plus-ai` 聚合中新增 `fw-plus-ai-askdata` 模块，并定义 AskData 请求、结果、错误、项目上下文及 Java 8 兼容的公开服务接口。
- [ ] 1.2 在 `fw-plus-ai-askdata` 定义模型、向量检索、推荐、Agent、图编排、元数据、检索、权限、SQL、审计和接口发布 SPI。
- [ ] 1.3 定义版本化能力描述符、能力提供者和注册表，支持模型、向量、SQL、存储和事件插件的启动期选择、排序、冲突和兼容性校验。
- [ ] 1.4 定义不可变 SQL 命令、受控片段、具名参数及类似 MyBatis 的阶段化 SQL 拦截器 SPI，禁止插件直接绕过安全校验或取得裸连接。
- [ ] 1.5 在 `fw-plus-ai-askdata` 定义以 `projectId + operatorId` 为强制归属的会话、检查点、租约、幂等键和审计状态存储 SPI，并为已有自动配置类型提供兼容迁移层。
- [ ] 1.6 定义独立的 Agent 图、节点、子任务、计划、工具摘要、模型路由和步骤日志状态存储 SPI，支持与会话存储组合或独立选择。
- [ ] 1.7 为 Core AskData 契约、SPI 替换、插件版本协商和无 Spring 依赖编译添加单元测试。
- [ ] 1.8 定义多提问请求、子问题计划、依赖关系、聚合结果和问题拆分 SPI，保持单问题请求兼容。
- [ ] 1.9 定义以认证操作人身份为强制边界的推荐请求、候选过滤、画像读取和排序 SPI，禁止跨操作人复用个人偏好、历史或排序特征。
- [ ] 1.10 为公开接口、SPI、默认实现、安全边界、状态恢复和联调契约补充详细中文注释，解释业务语义、扩展方式、调用约束和禁止行为。
- [ ] 1.11 定义可替换 AI 运行时适配契约，覆盖 LangChain4j 默认适配、Spring AI、Spring AI Alibaba 和 AgentScope Java 2.0+ 的模型、向量、工具、RAG、Agent、状态和观测映射边界。
- [ ] 1.12 定义 Remote Runtime SPI，用于在 Spring AI、Spring AI Alibaba 或 AgentScope Java 2.0+ 无法内嵌 Java 8 / Spring Boot 2.7.18 时，对接 Java 17 独立适配服务，并覆盖鉴权、上下文传递、流式事件、错误映射、审计和回滚。

## 2. Storage implementations

- [ ] 2.1 实现默认单机内存状态存储，并覆盖按项目和操作人隔离的会话、检查点、压缩上下文和审计状态测试。
- [ ] 2.2 实现 JDBC 状态存储，提供按项目和操作人隔离、事务、版本校验和重启恢复测试。
- [ ] 2.3 实现 Redis 状态存储，提供按项目和操作人隔离的集群租约、幂等键、实例迁移和租约冲突测试。
- [ ] 2.4 在 Starter 配置中区分单机与集群模式，拒绝集群使用本地内存状态，并提供业务自定义存储装配入口。
- [ ] 2.5 通过现有 `EmbeddingStoreRegistry` 接入训练检索的向量存储选择，提供默认内存向量模式和可选共享向量库模式，不在 Core 绑定具体向量数据库。
- [ ] 2.6 实现长期记忆 SPI 的默认内存、JDBC/Redis/向量或业务适配入口，覆盖 store、recall、fetch、过期、遗忘、项目/操作人/数据源隔离、权限撤销和按操作人推荐画像隔离测试。
- [ ] 2.7 实现 Agent 状态的默认单机内存、JDBC、Redis 和业务自定义存储适配，覆盖节点/子任务恢复、跨实例租约与集群拒绝内存状态测试。

## 3. Pluggable AI runtime and graph orchestration

- [ ] 3.1 将当前 fw-plus LangChain4j 模型注册表适配到 Core AskData 模型、embedding、检索和工具调用 SPI，并提供默认 Ollama 聊天/向量适配和可覆盖的逻辑模型 key。
- [ ] 3.2 实现按项目、数据源、Agent 任务和请求动态选择模型的路由策略，以及模型候选链、熔断、健康状态、单点故障切换和兜底模型策略。
- [ ] 3.3 记录模型路由、尝试、故障分类和最终选择到检查点与 Agent 事件；覆盖候选耗尽、禁止切换和恢复后的幂等行为测试。
- [ ] 3.4 定义可选 AskData Java LangGraph 编排 SPI、状态映射和图版本契约。
- [ ] 3.5 在确认 Java 8 兼容的 LangGraph Java 构件后，实现多 Agent 图、检查点、流式步骤事件和失败分支适配。
- [ ] 3.6 定义 Spring AI 适配文档和可选适配入口，覆盖 ChatModel、EmbeddingModel、VectorStore、ChatClient、Advisor、tool calling 与 AskData SPI 的替换方法。
- [ ] 3.7 定义 Spring AI Alibaba 适配文档和可选适配入口，覆盖阿里生态模型、Workflow、多 Agent、DataAgent、MCP/A2A 和 AskData SPI 的替换方法。
- [ ] 3.8 定义 AgentScope Java 2.0+ 适配文档和可选适配入口，覆盖 HarnessAgent、DistributedStore、AgentStateStore、模型注册、权限引擎、记忆压缩、tracing、MCP/A2A/AG-UI 和 AskData SPI 的替换方法。
- [ ] 3.9 保留默认顺序流程并覆盖未启用或缺少图编排/运行时适配依赖时的回退测试。

## 4. Query safety and recovery

- [ ] 4.1 建立 SQLBot/WrenAI 数据源与方言能力矩阵，覆盖 MySQL、Oracle、PostgreSQL、SQL Server、ClickHouse、达梦、Apache Doris、AWS Redshift、Elasticsearch、Kingbase、StarRocks、Apache Hive、Excel/CSV、BigQuery、DuckDB、Snowflake 等连接器，并标明实现、验证和后续状态。
- [ ] 4.2 实现 SQL 组装、参数绑定、租户/数据范围补充、方言改写、结果映射和审计补充的阶段化插件管线，并覆盖确定性排序与失败隔离。
- [ ] 4.3 强化单语句只读 SQL 校验、物理限行、查询超时、只读执行和审计结果；确保全部插件产出的最终 SQL 重新校验。
- [ ] 4.4 实现基于 LangChain4j 的自动上下文压缩和权限感知的长期记忆召回，并保留恢复所需的结构化安全字段。
- [ ] 4.5 实现稳定节点检查点、跨实例恢复和执行中 SQL 不重放保护。
- [ ] 4.6 实现复合问题拆分、受限并发、依赖调度、部分失败聚合、取消/超时和按子问题恢复保护。

## 5. Agent observability contracts

- [ ] 5.1 定义不绑定厂商的 Agent 运行事件、关联标识和异步发布 SPI，覆盖多提问、子 Agent、模型路由/兜底、SQL 插件、压缩、长期记忆、检查点、恢复和失败。
- [ ] 5.2 实现可选 LangSmith、Langfuse/OpenTelemetry 和业务导出器适配，覆盖脱敏、批量异步导出、导出故障隔离和跨实例链路关联测试。
- [ ] 5.3 实现每次对话强制监控日志的生命周期事件和按 `projectId + operatorId + conversationId` 查询能力，提供默认单机内存、JDBC、Redis、消息或业务自定义日志存储适配。
- [ ] 5.4 定义监控日志访问授权 SPI，覆盖项目管理员查看本项目全量日志、经业务授权的平台管理员跨项目查询和普通操作人默认拒绝查询的测试。

## 6. Single starter integration

- [ ] 6.1 调整 AskData Starter POM、BOM 和自动配置，使其传递引入 Core 契约、LangChain4j 适配和默认实现。
- [ ] 6.2 使用 `@ConditionalOnMissingBean` 和能力注册表装配所有默认 SPI，使业务应用可替换、组合模型、向量、SQL 插件、Agent 状态或完整 `AskDataService`/Agent 编排。
- [ ] 6.3 保持 Spring Boot 2.7.18、Java 8 和现有 AskData 请求/结果兼容，并更新回归测试。

## 7. Documentation and verification

- [ ] 7.0 将 `feature-list.md` 作为 SQLBot 与 WrenAI 全功能清单事实源维护，并在正式说明文档中生成可追踪矩阵；每项功能必须记录来源、归属 change、模块、接口/配置、存储、文档、测试、状态和限制。
- [ ] 7.1 编写详细中文架构与模块文档，说明 Core、LangChain4j 适配、Starter、默认实现、业务覆盖边界和依赖关系。
- [ ] 7.2 编写快速开始、最小 Ollama 示例和完整配置属性参考；每项属性注明默认值、范围、单机/集群限制、失败表现和安全注意事项。
- [ ] 7.3 编写动态模型路由、候选/兜底、模型/向量/SQL/Agent/存储/观测插件 SDK、业务完整覆盖默认实现的开发文档和可编译示例。
- [ ] 7.4 编写 Doris SQL 安全、多 Agent、多提问、长期记忆、项目/操作人隔离、管理员监控日志、LangSmith/Langfuse、恢复和集群运维文档。
- [ ] 7.5 编写存储能力矩阵、迁移指南、故障恢复、排障、容量和脱敏建议，并验证文档中的 Java 与配置示例。
- [ ] 7.6 维护 Starter 已实现功能清单，逐项标注模块、版本、公开配置/API、单机/集群支持、验证证据与限制，并将未实现或后续 change 能力单独列出。
- [ ] 7.7 建立并持续维护 SQLBot v1 全功能追踪矩阵，将 SQLBot 开源项目和 v1 文档的每项功能映射至本批次 change、实现模块、文档和验证证据；至少覆盖部署/命令行、工作空间/项目、数据源、智能问数、仪表板、小助手、权限、术语、模型配置、模型接入、训练检索、SQL 示例、Prompt、MCP Server、嵌入式对接、DataEase 接入、X-Pack 登录认证、平台对接、企业微信、钉钉、飞书、参数配置、外观设置和操作日志；矩阵存在缺项时不得宣称功能完整。
- [ ] 7.8 建立并持续维护 WrenAI 企业级 GenBI 全功能追踪矩阵，将自然语言问数、Text-to-SQL、Text-to-Chart、AI 摘要/报告、可嵌入 API、MDL 语义层、模型/列/关系/指标/视图、deploy manifest、数据源连接器、模型接入、语义索引、语义描述、关系推荐、问题推荐、instructions、SQL pair、ask feedback、evaluation、dry-plan、dry-run、metadata constraints、validate、model substitute、preview data、任务轮询、dashboard 和遥测映射至本批次 change、实现模块、文档和验证证据；与 SQLBot 重复时标记 SQLBot 优先。
- [ ] 7.9 编写后端 Starter 详细设计文档，覆盖模块边界、包结构、领域对象、SPI 清单、默认实现、自动配置、异常模型、配置模型、兼容迁移、前端联调契约和实现顺序。
- [ ] 7.10 编写业务流程图和技术流程图文档，使用 Mermaid 源码覆盖项目治理、数据源、训练/语义模型、问数、多提问、推荐、模型路由、多 Agent、SQL 安全、恢复、监控审计和前后端联调。
- [ ] 7.11 编写数据库设计文档，覆盖项目、成员权限、数据源、元数据、语义模型/MDL、训练资产、会话、长期记忆、推荐、SQL 审计、监控日志、Agent 状态、模型/API Key、MCP/API、企业配置、操作日志和插件注册表结构。
- [ ] 7.12 编写测试用例文档，覆盖单元、契约、自动配置、SQL 安全、存储一致性、模型路由/兜底、多 Agent/恢复、长期记忆/推荐权限、监控审计、文档示例和后续前后端联调用例。
- [ ] 7.13 编写 AI 运行时替换文档，明确从默认 LangChain4j/Ollama 切换到 Spring AI、Spring AI Alibaba、AgentScope Java 2.0+ 或业务自定义运行时所需依赖、配置、能力 key、Bean/SPI 覆盖、限制和验证用例；若运行时只支持 Java 17+ 或 Spring Boot 3.x+，必须写清楚不兼容原因、官方版本基线、禁止内嵌 Java 8 Starter 的边界，以及 Java 17 独立适配服务通过 HTTP/SSE、MCP、A2A、gRPC 或消息队列对接的配置、鉴权、上下文、错误、审计、监控和回滚步骤。
- [ ] 7.14 编写 SQLBot 外部系统对接全集文档，覆盖模型接入/Ollama、MCP Server、嵌入式对接、DataEase 接入、MaxKB、Dify、Coze、n8n、X-Pack 登录认证、平台对接、企业微信、钉钉、飞书、扫码登录、免登、组织用户同步、参数配置、服务配置、工具参数、token/chat_id、工作空间/数据源列表、基础/高级/页面嵌入、凭证映射、跨域、AES-Key、审计和联调测试。
- [ ] 7.15 提供业务方重新实现 HTTP、RPC、消息和 MCP 接口的示例。
- [ ] 7.16 提供单问题、显式多问题和自然语言复合问题的调用、聚合结果和部分失败处理示例。
- [ ] 7.17 运行 Core、AI 自动配置和 Starter 的定向 Maven 测试，并记录 Java 8、集群状态、文档示例和测试用例覆盖结果；同时记录 Spring AI、Spring AI Alibaba、AgentScope Java 2.0+ 的 Java 8 内嵌兼容性验证结果，以及 Java 17 独立适配服务的契约联调结果。

```

## openspec/changes/rework-ai-askdata-core-starter/specs/askdata-agent-observability-contracts/spec.md

- Source: openspec/changes/rework-ai-askdata-core-starter/specs/askdata-agent-observability-contracts/spec.md
- Lines: 1-65
- SHA256: c9563f4fcd0344841bff0a21c5e44c00dc1d7b3c7e4d30f64c8a16a056cf411a

```md
## ADDED Requirements

### Requirement: Vendor-neutral Agent observability contracts
系统 SHALL 在 `fw-plus-ai-askdata` 中定义不依赖 Spring、LangChain4j 或观测厂商 SDK 的不可变 Agent 运行事件、关联标识和事件发布 SPI。事件 SHALL 覆盖请求、多提问子任务、图节点、子 Agent、模型路由/兜底、工具、SQL 插件、压缩、长期记忆、检查点、恢复、降级和失败。

#### Scenario: Correlate an execution across agents and instances
- **WHEN** 一个 AskData 请求经过多个子问题、Agent、SQL 插件或集群实例恢复
- **THEN** 事件 SHALL 携带项目、请求、子问题、会话、图节点、Agent、实例和父事件关联标识，使业务监控能够重建执行链路

#### Scenario: Observe a model failover
- **WHEN** 主模型发生可切换故障并使用候选或兜底模型
- **THEN** 系统 SHALL 发布主模型、故障分类、候选顺序、最终模型 key 和结果状态，且不得包含原始 Prompt 或凭据

#### Scenario: Keep sensitive data out of default events
- **WHEN** 默认事件发布器记录模型、SQL、长期记忆或工具调用
- **THEN** 原始问题、Prompt、SQL 结果、参数值、记忆正文和凭据 SHALL 不被默认写入；诊断摘要 SHALL 经显式配置和脱敏处理

### Requirement: Mandatory per-conversation monitoring logs
系统 SHALL 为每个 AskData 对话写入监控日志，不得因未启用外部可观测平台而省略。日志事件 SHALL 至少覆盖对话创建、请求/多提问子任务、模型路由与兜底、Agent/图节点、SQL 组装/校验/执行、长期记忆召回、检查点/恢复、完成、取消和失败，并绑定 `projectId`、已认证 `operatorId` 与 `conversationId`。

#### Scenario: Write logs throughout a conversation
- **WHEN** 已认证操作人在项目中创建或继续 AskData 对话
- **THEN** 系统 SHALL 为该对话写入可按 `projectId + operatorId + conversationId` 关联的生命周期监控日志，且每个子问题、模型尝试和 Agent 节点能够关联回同一对话

#### Scenario: Use default in-memory monitoring logs on a single node
- **WHEN** 单机应用未配置监控日志存储能力
- **THEN** Starter SHALL 使用默认内存环形日志存储，并说明容量上限和进程重启后日志丢失的行为

#### Scenario: Replace monitoring log storage
- **WHEN** 业务应用选择 JDBC、Redis、消息系统或自定义监控日志存储能力
- **THEN** Starter SHALL 通过能力注册表写入该实现，且不要求更改 Agent、SQL 或会话实现

#### Scenario: Do not block conversation on monitoring failure
- **WHEN** 外部日志存储或导出器超时、不可用或写入失败
- **THEN** 系统 SHALL 尝试保留脱敏本地诊断并继续对话处理；外部监控失败不得改变模型、SQL、检查点或对话成功状态

### Requirement: Administrator-only monitoring log access
系统 SHALL 通过可替换的监控日志访问授权 SPI 控制日志查询。项目管理员 SHALL 能查询其授权项目中的全部对话监控日志；平台管理员跨项目查询 SHALL 需要业务显式授权；普通操作人默认 SHALL 无法查询监控日志。

#### Scenario: A project administrator inspects project conversations
- **WHEN** 项目管理员查询其已授权项目的监控日志
- **THEN** 系统 SHALL 返回该项目内各操作人的脱敏对话、请求、Agent、模型、SQL 和恢复链路，并保持项目边界过滤

#### Scenario: A platform administrator requests cross-project logs
- **WHEN** 平台管理员查询多个项目的监控日志
- **THEN** 系统 SHALL 仅在业务授权 SPI 明确允许的项目范围内返回记录，且审计该查询操作

#### Scenario: An ordinary operator requests monitoring logs
- **WHEN** 非管理员操作人请求监控日志查询
- **THEN** 系统 SHALL 拒绝该查询；业务若需暴露个人历史，必须通过独立对话历史接口实现，不得复用管理员监控日志接口

### Requirement: Optional observability exporters
系统 SHALL 允许通过版本化能力插件异步装配 LangSmith、Langfuse、OpenTelemetry 或业务自定义导出器，且导出失败不得影响 AskData 请求、SQL 安全状态或检查点状态。

#### Scenario: Export to LangSmith when explicitly enabled
- **WHEN** 应用引入兼容 LangSmith 导出器并显式启用该能力
- **THEN** 导出器 SHALL 将请求、图节点、子 Agent、模型 token 和成本元数据映射为可关联的父子 run 或 trace，并遵守脱敏配置

#### Scenario: Export to Langfuse when explicitly enabled
- **WHEN** 应用引入兼容 Langfuse 导出器并显式启用该能力
- **THEN** 导出器 SHALL 异步批量导出 trace、span 和 generation 事件，并保留项目、请求和子问题关联标识

#### Scenario: Isolate exporter failures
- **WHEN** 任一可选观测导出器超时、不可用或返回错误
- **THEN** 系统 SHALL 仅产生本地诊断事件或指标，且不得阻塞问数、SQL 执行、恢复或向其他导出器发布

```

## openspec/changes/rework-ai-askdata-core-starter/specs/askdata-core-contracts/spec.md

- Source: openspec/changes/rework-ai-askdata-core-starter/specs/askdata-core-contracts/spec.md
- Lines: 1-202
- SHA256: 3fdf6377ce4894d93b64c76c580a8a076fdc957ad75685f328ef4ec1bb956215

[TRUNCATED]

```md
## ADDED Requirements

### Requirement: Core AskData contracts
系统 SHALL 在 `fw-plus-core/fw-plus-project/fw-plus-ai` 下新增 `fw-plus-ai-askdata` 模块，提供 Java 8 兼容、无 Spring Boot 依赖的 AskData 领域模型、服务接口与 SPI。

#### Scenario: Use core AskData API without Spring Boot
- **WHEN** 业务模块只依赖 `fw-plus-ai-askdata`
- **THEN** 模块 SHALL 能编译并使用 AskData 请求、结果、错误、项目上下文和扩展接口，且不需要引入 Spring Boot、LangChain4j 或数据库客户端

#### Scenario: Keep one public starter for default integration
- **WHEN** Spring Boot 应用引入 `fw-plus-ai-askdata-spring-boot-starter`
- **THEN** Starter SHALL 传递提供 Core AskData 契约与默认实现，应用不需要组合多个 AskData Starter

### Requirement: Business replacement SPI
系统 SHALL 在 Core AskData API 中定义模型、向量检索、推荐、图编排、Agent、元数据、检索、项目权限、会话、检查点、SQL、审计和接口发布的替换 SPI。

#### Scenario: Replace a single capability
- **WHEN** 业务应用提供某个 Core AskData SPI 的实现
- **THEN** Starter SHALL 使用该实现替代对应默认实现，且不要求业务应用复制其他默认流程代码

#### Scenario: Replace the complete business service
- **WHEN** 业务应用实现并注册 Core `AskDataService`
- **THEN** Starter SHALL 保留该业务实现作为对外问数服务，而不创建冲突的默认服务

#### Scenario: Replace the complete Agent implementation
- **WHEN** 业务应用实现并注册 Core Agent 派发/图编排、状态存储和必要服务接口
- **THEN** Starter SHALL 使用该业务实现接管 Agent 运行与恢复，而不要求继承或修改 Starter 内部类

#### Scenario: Publish a business-specific interface
- **WHEN** 业务应用通过 HTTP、RPC、消息或 MCP 发布 AskData 能力
- **THEN** 应用 SHALL 能仅依赖 Core AskData API 建模请求和结果，而不依赖 Starter 的内部实现类

#### Scenario: Explain extension contracts with Chinese comments
- **WHEN** 开发者阅读 Core AskData 公开接口、SPI、领域对象、默认实现或关键自动配置
- **THEN** 源码 SHALL 使用详细中文注释说明业务语义、扩展方式、调用约束、线程/集群/权限边界和禁止行为；注释不得只是重复方法名或逐行翻译代码

### Requirement: Versioned capability plugin registry
系统 SHALL 提供不依赖 Spring 的版本化能力描述符、能力提供者和能力注册表。模型、向量、SQL、状态、图编排和事件发布适配 SHALL 通过该机制声明能力类型、稳定 key、支持的契约版本区间、优先级和实现专属配置命名空间。

#### Scenario: Register a business model or vector plugin
- **WHEN** 业务应用提供满足 Core 契约版本的模型或向量能力提供者
- **THEN** Starter SHALL 在不修改 Core 或默认适配器的前提下发现、注册并按配置 key 选择该能力

#### Scenario: Resolve ordered SQL plugins deterministically
- **WHEN** 多个 SQL 插件适用于同一执行阶段
- **THEN** 注册表 SHALL 按显式优先级和稳定 key 生成确定性顺序；未声明可解析顺序的冲突 SHALL 导致启动失败

#### Scenario: Reject an incompatible or unknown capability
- **WHEN** 配置引用未知能力 key、重复冲突实现或不兼容契约版本
- **THEN** Starter SHALL 在启动期返回明确诊断，且不得静默回退到内置实现

### Requirement: Business SQL composition plugins
系统 SHALL 定义不可变 SQL 命令、受控结构片段、具名参数、执行上下文和阶段化 SQL 拦截器 SPI，使业务方能够以类似 MyBatis 插件的方式参与 SQL 组装、参数绑定、改写、执行准备、结果映射和审计补充。

#### Scenario: Add a business data-scope predicate
- **WHEN** 业务应用注册 SQL 插件以注入租户、组织或行级数据范围条件
- **THEN** 插件 SHALL 基于受控 SQL 命令和具名参数输出改写结果，且无需复制默认 SQL 生成或执行流程

#### Scenario: Compose a business-owned query
- **WHEN** 业务应用注册固定查询模板或业务 SQL 片段插件
- **THEN** AskData 流程 SHALL 能将已授权的业务结构片段和绑定参数合并到当前 SQL 命令，并保留来源能力 key 供审计

#### Scenario: Protect dynamic values from SQL concatenation
- **WHEN** SQL 插件补充用户、租户或业务动态值
- **THEN** 插件 SHALL 使用具名参数绑定；Core API 不得要求插件通过字符串拼接内嵌动态值

#### Scenario: Prevent plugins from bypassing query safety
- **WHEN** SQL 插件生成或改写 SQL
- **THEN** 插件 SHALL 不获得裸数据库连接，且最终 SQL SHALL 继续进入统一的权限、单语句、只读、限行、超时和审计管线

### Requirement: LangChain4j core adapters
系统 SHALL 在 `fw-plus-core` 的 `fw-plus-ai-langchain4j` 聚合下提供 LangChain4j 对 Core AskData 模型、embedding、检索和工具调用 SPI 的适配。

#### Scenario: Reuse current model registry
- **WHEN** 应用配置当前 fw-plus LangChain4j 模型注册表
- **THEN** AskData LangChain4j 适配 SHALL 将其暴露为 Core AskData 模型 SPI 实现

#### Scenario: Keep framework-specific types out of core contracts
- **WHEN** 检查 `fw-plus-ai-base` 的 AskData 公共 API
- **THEN** `fw-plus-ai-askdata` 的方法签名 SHALL 不暴露 `dev.langchain4j`、Spring AI、Spring AI Alibaba、AgentScope Java、Spring、LangGraph Java 或其他运行时框架类型

```

Full source: openspec/changes/rework-ai-askdata-core-starter/specs/askdata-core-contracts/spec.md

## openspec/changes/rework-ai-askdata-core-starter/specs/askdata-query-engine/spec.md

- Source: openspec/changes/rework-ai-askdata-core-starter/specs/askdata-query-engine/spec.md
- Lines: 1-310
- SHA256: 3ebd2000a7c2e9e7a9484b9e06c548481fd4d80aa5567c3ba3baa035e8d899f4

[TRUNCATED]

```md
## MODIFIED Requirements

### Requirement: Prompt, connector and dialect rules
系统 SHALL 提供可配置的 Prompt 模板、数据源连接器和数据库方言规则，使生成 SQL 遵守目标数据库的标识符引用、分页限制和只读查询约束。SQLBot 与 WrenAI 已支持的数据源 SHALL 进入统一能力矩阵；任一未实现或未验证项 SHALL 明确标记，不得以 Doris 或其他单一方言代表完整支持。

#### Scenario: Apply query limit policy
- **WHEN** 请求未显式指定返回数量且默认查询限制启用
- **THEN** Prompt 或 SQL 校验阶段 SHALL 要求生成 SQL 包含默认行数限制

#### Scenario: Dialect-specific identifier rules
- **WHEN** 数据源方言为 SQLBot 或 WrenAI 支持矩阵中的任一数据库
- **THEN** Prompt 构建 SHALL 使用该方言对应的标识符引用、schema、函数、类型、limit/offset 和只读规则

#### Scenario: Track all SQLBot and WrenAI data sources
- **WHEN** 团队验收数据源与方言支持范围
- **THEN** 文档 SHALL 列出 MySQL、Oracle、PostgreSQL、SQL Server、ClickHouse、达梦、Apache Doris、AWS Redshift、Elasticsearch、Kingbase、StarRocks、Apache Hive、Excel/CSV、BigQuery、DuckDB、Snowflake 等 SQLBot/WrenAI 数据源的连接器状态、方言规则、权限支持、dry-plan 支持、验证证据和限制

### Requirement: Multiple model configuration
系统 SHALL 支持多个模型配置，并允许配置一个默认模型；AskData 请求可以显式指定模型 key，未指定时使用默认模型。LangChain4j SHALL 是默认适配，不是固定运行时；Spring AI、Spring AI Alibaba、AgentScope Java 2.0+ 或业务自定义运行时 SHALL 能通过 Core SPI、能力插件或 Spring Bean 替换。

#### Scenario: Prefer LangChain4j model integration
- **WHEN** 应用中存在可用 LangChain4j 模型 Bean 或 LangChain4j 模型注册信息
- **THEN** AskData 默认模型注册表 SHALL 优先使用 LangChain4j 完成聊天模型和 embedding 模型适配

#### Scenario: Replace runtime with Spring AI
- **WHEN** 业务应用提供 Spring AI 适配能力并选择对应模型、embedding、向量存储或工具调用能力 key
- **THEN** AskData SHALL 通过 Core SPI 使用 Spring AI 的 ChatModel、EmbeddingModel、VectorStore、ChatClient、Advisor 或 tool calling 能力，而 Core API 不暴露 Spring AI 类型

#### Scenario: Replace runtime with Spring AI Alibaba
- **WHEN** 业务应用提供 Spring AI Alibaba 适配能力并选择对应运行时能力 key
- **THEN** AskData SHALL 能对接 Spring AI Alibaba 的模型、Workflow、多 Agent、DataAgent、MCP/A2A 能力，并继续通过 AskData 的项目权限、SQL 安全、审计、检查点和监控日志管线执行

#### Scenario: Replace runtime with AgentScope Java 2.0+
- **WHEN** 业务应用提供 AgentScope Java 2.0+ 适配能力并选择对应 Agent 运行时能力 key
- **THEN** AskData SHALL 能对接 AgentScope 的 HarnessAgent、分布式状态、模型注册、权限引擎、记忆压缩、tracing、MCP/A2A/AG-UI 和多 Agent 编排能力，并继续遵守 AskData Core 的统一安全与恢复契约

#### Scenario: Use Ollama as the starter default vendor
- **WHEN** 应用未覆盖 AskData 的模型和向量厂商配置
- **THEN** Starter SHALL 通过 LangChain4j 使用 Ollama 作为默认聊天模型与 embedding 模型厂商，并允许以配置覆盖 Ollama 地址、模型标签和逻辑模型 key

#### Scenario: Keep Ollama out of Core contracts
- **WHEN** 业务应用仅依赖 `fw-plus-ai-askdata` Core API
- **THEN** Core API SHALL 不暴露 Ollama、LangChain4j 或其他厂商类型；业务应用 SHALL 能使用能力 SPI 或 Spring Bean 替换 Starter 的 Ollama 默认适配

#### Scenario: Preserve current starter model registration
- **WHEN** 应用使用当前 fw-plus LangChain4j Starter 提供的模型注册信息
- **THEN** AskData SHALL 无需重复模型配置即可发现并使用该聊天模型和 embedding 模型

#### Scenario: Keep optional runtimes out of core contracts
- **WHEN** 构建当前 Java 8 AskData Core 契约
- **THEN** 模块 SHALL 不暴露 LangChain4j、Spring AI、Spring AI Alibaba、AgentScope Java、Agents-Flex、EasyAi、Python 或其他 AI 框架运行时类型；可选适配依赖 SHALL 位于适配模块或业务应用中

#### Scenario: Document incompatible AI runtimes
- **WHEN** Spring AI、Spring AI Alibaba 或 AgentScope Java 2.0+ 的官方文档、BOM、最小编译或启动验证表明其要求 Java 17+、Spring Boot 3.x+ 或其他不兼容当前基线的依赖
- **THEN** 文档 SHALL 明确记录官方基线、验证版本、失败现象、不能作为 Java 8 Starter 传递依赖的原因、可替代接入方式和回滚策略

#### Scenario: Bridge a Java 17 runtime service
- **WHEN** 业务方需要使用只能运行在 Java 17 或 Spring Boot 3.x+ 的 Spring AI、Spring AI Alibaba、AgentScope Java 2.0+ 能力
- **THEN** AskData SHALL 通过 Remote Runtime SPI 对接独立 Java 17 适配服务，并通过 HTTP/SSE、MCP、A2A、gRPC 或消息队列传递项目、操作人、会话、能力 key、模型路由上下文、脱敏输入、流式事件、错误和审计关联；SQL 执行仍 SHALL 回到 Java 8 Starter 的统一安全管线

#### Scenario: Reject incompatible embedded dependencies
- **WHEN** 某个可选运行时依赖无法通过 Java 8 / Spring Boot 2.7.18 的编译、依赖解析和最小启动测试
- **THEN** 该依赖 SHALL 不进入默认 Starter 传递依赖，不得污染 Core API，且启动诊断 SHALL 提示改用兼容版本、自定义 SPI 或 Java 17 独立适配服务

#### Scenario: Use default model
- **WHEN** AskData 请求未指定模型 key 且系统已配置默认模型
- **THEN** AskData 服务 SHALL 使用默认模型生成 SQL

#### Scenario: Use requested model
- **WHEN** AskData 请求指定有效模型 key
- **THEN** AskData 服务 SHALL 使用该模型配置生成 SQL，而不是默认模型

#### Scenario: Requested model missing
- **WHEN** AskData 请求指定不存在的模型 key
- **THEN** AskData 服务 SHALL 返回模型配置错误，不得静默回退到默认模型

#### Scenario: No default model configured
- **WHEN** AskData 请求未指定模型 key 且系统没有默认模型
- **THEN** AskData 服务 SHALL 返回明确错误，说明缺少默认模型配置


```

Full source: openspec/changes/rework-ai-askdata-core-starter/specs/askdata-query-engine/spec.md
