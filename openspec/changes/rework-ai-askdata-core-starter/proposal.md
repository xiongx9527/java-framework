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
