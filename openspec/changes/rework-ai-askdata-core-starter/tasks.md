## 1. Core API and compatibility

- [x] 1.1 在 `fw-plus-ai` 聚合中新增 `fw-plus-ai-askdata` 模块，并定义 AskData 请求、结果、错误、项目上下文及 Java 8 兼容的公开服务接口。
- [ ] 1.2 在 `fw-plus-ai-askdata` 定义模型、向量检索、推荐、Agent、图编排、元数据、检索、权限、SQL、审计和接口发布 SPI。
- [x] 1.3 定义版本化能力描述符、能力提供者和注册表，支持模型、向量、SQL、存储和事件插件的启动期选择、排序、冲突和兼容性校验。
- [x] 1.4 定义不可变 SQL 命令、受控片段、具名参数及类似 MyBatis 的阶段化 SQL 拦截器 SPI，禁止插件直接绕过安全校验或取得裸连接。
- [ ] 1.5 在 `fw-plus-ai-askdata` 定义以 `projectId + operatorId` 为强制归属的会话、检查点、租约、幂等键和审计状态存储 SPI，并为已有自动配置类型提供兼容迁移层。
- [ ] 1.6 定义独立的 Agent 图、节点、子任务、计划、工具摘要、模型路由和步骤日志状态存储 SPI，支持与会话存储组合或独立选择。
- [x] 1.7 为 Core AskData 契约、SPI 替换、插件版本协商和无 Spring 依赖编译添加单元测试。
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

- [x] 7.0 将 `feature-list.md` 作为 SQLBot 与 WrenAI 全功能清单事实源维护，并在正式说明文档中生成可追踪矩阵；每项功能必须记录来源、归属 change、模块、接口/配置、存储、文档、测试、状态和限制。
- [x] 7.1 编写详细中文架构与模块文档，说明 Core、LangChain4j 适配、Starter、默认实现、业务覆盖边界和依赖关系。
- [x] 7.2 编写快速开始、最小 Ollama 示例和完整配置属性参考；每项属性注明默认值、范围、单机/集群限制、失败表现和安全注意事项。
- [x] 7.3 编写动态模型路由、候选/兜底、模型/向量/SQL/Agent/存储/观测插件 SDK、业务完整覆盖默认实现的开发文档和可编译示例。
- [x] 7.4 编写 Doris SQL 安全、多 Agent、多提问、长期记忆、项目/操作人隔离、管理员监控日志、LangSmith/Langfuse、恢复和集群运维文档。
- [x] 7.5 编写存储能力矩阵、迁移指南、故障恢复、排障、容量和脱敏建议，并验证文档中的 Java 与配置示例。
- [x] 7.6 维护 Starter 已实现功能清单，逐项标注模块、版本、公开配置/API、单机/集群支持、验证证据与限制，并将未实现或后续 change 能力单独列出。
- [x] 7.7 建立并持续维护 SQLBot v1 全功能追踪矩阵，将 SQLBot 开源项目和 v1 文档的每项功能映射至本批次 change、实现模块、文档和验证证据；至少覆盖部署/命令行、工作空间/项目、数据源、智能问数、仪表板、小助手、权限、术语、模型配置、模型接入、训练检索、SQL 示例、Prompt、MCP Server、嵌入式对接、DataEase 接入、X-Pack 登录认证、平台对接、企业微信、钉钉、飞书、参数配置、外观设置和操作日志；矩阵存在缺项时不得宣称功能完整。
- [x] 7.8 建立并持续维护 WrenAI 企业级 GenBI 全功能追踪矩阵，将自然语言问数、Text-to-SQL、Text-to-Chart、AI 摘要/报告、可嵌入 API、MDL 语义层、模型/列/关系/指标/视图、deploy manifest、数据源连接器、模型接入、语义索引、语义描述、关系推荐、问题推荐、instructions、SQL pair、ask feedback、evaluation、dry-plan、dry-run、metadata constraints、validate、model substitute、preview data、任务轮询、dashboard 和遥测映射至本批次 change、实现模块、文档和验证证据；与 SQLBot 重复时标记 SQLBot 优先。
- [x] 7.9 编写后端 Starter 详细设计文档，覆盖模块边界、包结构、领域对象、SPI 清单、默认实现、自动配置、异常模型、配置模型、兼容迁移、前端联调契约和实现顺序。
- [x] 7.10 编写业务流程图和技术流程图文档，使用 Mermaid 源码覆盖项目治理、数据源、训练/语义模型、问数、多提问、推荐、模型路由、多 Agent、SQL 安全、恢复、监控审计和前后端联调。
- [x] 7.11 编写数据库设计文档，覆盖项目、成员权限、数据源、元数据、语义模型/MDL、训练资产、会话、长期记忆、推荐、SQL 审计、监控日志、Agent 状态、模型/API Key、MCP/API、企业配置、操作日志和插件注册表结构。
- [x] 7.12 编写测试用例文档，覆盖单元、契约、自动配置、SQL 安全、存储一致性、模型路由/兜底、多 Agent/恢复、长期记忆/推荐权限、监控审计、文档示例和后续前后端联调用例。
- [x] 7.13 编写 AI 运行时替换文档，明确从默认 LangChain4j/Ollama 切换到 Spring AI、Spring AI Alibaba、AgentScope Java 2.0+ 或业务自定义运行时所需依赖、配置、能力 key、Bean/SPI 覆盖、限制和验证用例；若运行时只支持 Java 17+ 或 Spring Boot 3.x+，必须写清楚不兼容原因、官方版本基线、禁止内嵌 Java 8 Starter 的边界，以及 Java 17 独立适配服务通过 HTTP/SSE、MCP、A2A、gRPC 或消息队列对接的配置、鉴权、上下文、错误、审计、监控和回滚步骤。
- [ ] 7.14 编写 SQLBot 外部系统对接全集文档，覆盖模型接入/Ollama、MCP Server、嵌入式对接、DataEase 接入、MaxKB、Dify、Coze、n8n、X-Pack 登录认证、平台对接、企业微信、钉钉、飞书、扫码登录、免登、组织用户同步、参数配置、服务配置、工具参数、token/chat_id、工作空间/数据源列表、基础/高级/页面嵌入、凭证映射、跨域、AES-Key、审计和联调测试。
- [ ] 7.15 提供业务方重新实现 HTTP、RPC、消息和 MCP 接口的示例。
- [ ] 7.16 提供单问题、显式多问题和自然语言复合问题的调用、聚合结果和部分失败处理示例。
- [ ] 7.17 运行 Core、AI 自动配置和 Starter 的定向 Maven 测试，并记录 Java 8、集群状态、文档示例和测试用例覆盖结果；同时记录 Spring AI、Spring AI Alibaba、AgentScope Java 2.0+ 的 Java 8 内嵌兼容性验证结果，以及 Java 17 独立适配服务的契约联调结果。
