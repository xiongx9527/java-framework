# askdata-query-engine Specification

## Purpose
TBD - created by archiving change add-askdata-spring-boot-starter. Update Purpose after archive.
## Requirements

### Requirement: Replaceable AskData flow steps
系统 SHALL 提供稳定步骤 key、类型化步骤上下文和结果，使选表、SQL 生成、SQL 校验、SQL 执行与结果解释能够由默认步骤、业务 Agent 或子 Agent 实现。

#### Scenario: Replace SQL generation only
- **WHEN** 应用为 `generate-sql` 提供唯一自定义 Agent
- **THEN** Flow SHALL 使用其类型化 SQL 输出并继续默认校验和执行，不得再次调用默认模型生成 SQL

#### Scenario: Reject duplicate step replacements
- **WHEN** 多个 Agent 替换同一稳定步骤 key 且没有唯一可解析优先级
- **THEN** Starter SHALL 在启动期失败并列出冲突实现

#### Scenario: Preserve an unmodified default step
- **WHEN** 应用未提供某步骤的自定义 Agent
- **THEN** Flow SHALL 使用该步骤默认实现并保持现有单问题行为

### Requirement: Framework-neutral context compression selection
系统 SHALL 提供可替换、可降级的上下文压缩器。默认实现 SHALL 不依赖 AI 框架并保留恢复所需结构化字段；业务可选择 LangChain4j 语义摘要 Adapter。

#### Scenario: Use deterministic compression by default
- **WHEN** 会话超过阈值且业务未选择语义压缩 Adapter
- **THEN** 默认压缩器 SHALL 生成有界摘要，并保留项目、操作人、模型 key、数据源、SQL、步骤状态、错误和恢复标识

#### Scenario: Use LangChain4j semantic compression when selected
- **WHEN** 业务引入 LangChain4j Adapter 并选择语义压缩能力 key
- **THEN** 压缩器 SHALL 使用 LangChain4j 生成脱敏摘要，同时保留全部结构化安全字段

#### Scenario: Fall back after semantic compression failure
- **WHEN** 语义压缩超时、不可用或返回无效结果
- **THEN** 系统 SHALL 使用默认确定性压缩器继续保存检查点，发布降级事件且不得改变对话结果

### Requirement: Executable dynamic model routing
系统 SHALL 在默认 Flow 中按照请求模型 key、项目、数据源、Agent 任务和默认策略动态路由 Chat 与 Embedding 模型。只有可重试基础设施故障允许在幂等模型节点内切换；响应、检查点和审计 SHALL 记录实际模型 key、路由原因和尝试历史。

#### Scenario: Route through the Core router
- **WHEN** 请求、项目、数据源或 Agent 任务配置了模型策略
- **THEN** 默认 Flow SHALL 调用 Core Router，而不是只解析普通候选列表

#### Scenario: Record every model attempt
- **WHEN** 主模型成功、切换候选或候选耗尽
- **THEN** 结果、检查点和脱敏事件 SHALL 记录每次实际模型 key、路由层级、故障分类和最终选择

#### Scenario: Avoid replay after a side effect
- **WHEN** SQL、工具或其他副作用节点已经开始
- **THEN** Flow SHALL 不因后续失败重新执行模型节点或副作用

### Requirement: Executable multi-question scheduler
系统 SHALL 使用带稳定 ID、依赖、失败策略和超时的计划执行多问题。无依赖节点可在受限并发度内执行；有依赖节点只在前置达到可用终态后执行。

#### Scenario: Respect the concurrency budget
- **WHEN** 多个独立子问题同时可运行
- **THEN** 调度器 SHALL 不超过配置最大并发度，并为每个稳定 ID 保存独立状态

#### Scenario: Propagate dependency terminal states
- **WHEN** 前置子问题失败、超时或取消
- **THEN** 依赖节点 SHALL 按计划标记跳过、降级或失败，不得在依赖结果不可用时执行

#### Scenario: Return partial results by stable id
- **WHEN** 子问题具有成功、失败、超时、取消或未执行等不同终态
- **THEN** 聚合结果 SHALL 按稳定 ID 返回状态、原因、实际模型 key 和 SQL 审计，不得将未完成节点标记成功

#### Scenario: Cancel pending sub-questions
- **WHEN** 请求取消或整体超时
- **THEN** 调度器 SHALL 停止提交未开始节点、尝试取消可中断任务，并保留已完成结果和取消审计

### Requirement: Stage-complete SQL plugin execution
系统 SHALL 将 SQL 组装、参数绑定、方言改写、安全校验、执行准备、执行、结果映射和审计组织为阶段化管线。插件新增具名参数和安全输出 SHALL 进入后续阶段，最终安全边界不得被替代。

#### Scenario: Preserve plugin named parameters
- **WHEN** 业务插件附加授权 SQL 片段和具名参数
- **THEN** Flow SHALL 保留参数到执行阶段、拒绝冲突参数，并记录能力 key、阶段和脱敏摘要

#### Scenario: Map an authorized result
- **WHEN** 业务插件声明结果映射阶段
- **THEN** Flow SHALL 在数据库返回后按确定顺序执行映射，且不得向插件暴露超出当前授权范围的数据

#### Scenario: Revalidate every rewritten command
- **WHEN** 任一插件生成或改写 SQL
- **THEN** 最终命令 SHALL 重新接受表列权限、行级谓词、单语句、只读、限行和超时校验，失败时不得执行
### Requirement: AskData starter module
系统 SHALL 在 `fw-plus-ai-boot-starters` 聚合下提供 `fw-plus-ai-askdata-spring-boot-starter` 模块，并遵循现有 `fw-plus-ai` 的 Maven 父 POM、BOM 和 Spring Boot 2.7.18 自动配置风格。

#### Scenario: Starter is part of AI aggregation
- **WHEN** 开发者在 `fw-plus-ai` 中构建 `fw-plus-ai-boot-starters`
- **THEN** `fw-plus-ai-askdata-spring-boot-starter` SHALL 作为聚合模块参与构建

#### Scenario: Java 8 compatibility
- **WHEN** 项目使用 Java 8 编译 AskData starter
- **THEN** 代码 SHALL 不依赖 Java 9+ API 或 Spring Boot 3 特性

### Requirement: AskData service API
系统 SHALL 提供 Java 服务接口，用于接收自然语言问题、数据源上下文和可选执行配置，并返回结构化 AskData 结果。

#### Scenario: Generate SQL result
- **WHEN** 调用方提交包含问题和数据源标识的 AskData 请求
- **THEN** 服务 SHALL 返回包含生成 SQL、使用表名、推荐展示类型、执行步骤和状态的结果对象

#### Scenario: Return model generation error
- **WHEN** 模型返回内容无法解析为有效 SQL 结果
- **THEN** 服务 SHALL 返回失败状态和可诊断错误信息，而不是抛出不可控异常给调用方

### Requirement: Metadata provider abstraction
系统 SHALL 提供数据源元数据提供器抽象，用于向 AskData 引擎提供数据库方言、表结构、字段说明、样例值和业务术语。

#### Scenario: Build prompt with metadata
- **WHEN** AskData 服务处理自然语言问题
- **THEN** 服务 SHALL 通过元数据提供器获取可用 schema，并将其用于 Prompt 构建

#### Scenario: Replace metadata provider
- **WHEN** 应用自定义 `AskDataMetadataProvider` Bean
- **THEN** 自动配置 SHALL 使用自定义实现替代默认实现

### Requirement: Prompt and dialect rules
系统 SHALL 提供可配置的 Prompt 模板、数据源连接器和数据库方言规则，使生成 SQL 遵守目标数据库的标识符引用、分页限制和只读查询约束。SQLBot 与 WrenAI 已支持的数据源 SHALL 进入统一能力矩阵；任一需真实数据源或外部依赖验证项 SHALL 明确标记，不得以 Doris 或其他单一方言代表完整支持。

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
系统 SHALL 支持多个模型配置，并允许配置一个默认模型；AskData 请求可以显式指定模型 key，未指定时使用默认模型。模型与 Embedding Adapter SHALL 由业务选择。当前 Java 8 分支实现 LangChain4j Adapter，但 AskData Starter SHALL 不默认绑定 LangChain4j、Ollama 或其他 AI 框架；Spring AI、Spring AI Alibaba 和 AgentScope Adapter SHALL 由 Java 17 / Spring Boot 3 分支实现。

#### Scenario: Prefer LangChain4j model integration
- **WHEN** 业务应用显式引入 LangChain4j Adapter，并存在可用 LangChain4j 模型 Bean 或模型注册信息
- **THEN** AskData SHALL 通过中立模型注册表使用 LangChain4j 完成聊天模型和 Embedding 模型适配，且业务无需替换默认 Flow

#### Scenario: Optional fallback model adapters
- **WHEN** 业务显式提供其他兼容模型 Adapter、能力插件或 Remote Runtime key
- **THEN** AskData SHALL 通过统一 Core SPI 使用该能力，当前 Java 8 Starter 不得要求核心流程直接依赖具体 AI 框架

#### Scenario: Replace runtime with Spring AI
- **WHEN** Java 17 / Spring Boot 3 分支提供 Spring AI Adapter 并选择对应能力 key
- **THEN** AskData SHALL 通过 Core SPI 或 Remote Runtime 使用 Spring AI 的 ChatModel、EmbeddingModel、VectorStore、ChatClient、Advisor 或 tool calling 能力，Core API 不得暴露 Spring AI 类型

#### Scenario: Replace runtime with Spring AI Alibaba
- **WHEN** Java 17 / Spring Boot 3 分支提供 Spring AI Alibaba Adapter 并选择对应能力 key
- **THEN** AskData SHALL 通过 Core SPI 或 Remote Runtime 使用阿里生态模型、Workflow、多 Agent、DataAgent、MCP/A2A 能力，并继续执行统一权限、SQL、检查点和监控管线

#### Scenario: Replace runtime with AgentScope Java 2.0+
- **WHEN** Java 17 / Spring Boot 3 分支提供 AgentScope Adapter 并选择对应能力 key
- **THEN** AskData SHALL 通过 Core SPI 或 Remote Runtime 使用 HarnessAgent、分布式状态、模型注册、权限、记忆压缩、tracing、MCP/A2A/AG-UI 和多 Agent 能力

#### Scenario: Use Ollama as the starter default vendor
- **WHEN** 业务显式引入 LangChain4j Adapter Starter、选择 Ollama Provider 且未覆盖该 Adapter 的厂商配置
- **THEN** LangChain4j Adapter Starter MAY 使用 Ollama 作为其局部默认厂商；公共 AskData Starter SHALL 不创建或传递默认 Ollama 配置

#### Scenario: Keep Ollama out of Core contracts
- **WHEN** 业务应用仅依赖 `fw-plus-ai-askdata` Core API
- **THEN** Core API SHALL 不暴露 Ollama、LangChain4j 或其他厂商类型，业务 SHALL 能通过能力 SPI 或业务 Bean 选择任意兼容 Adapter

#### Scenario: Keep Spring AI out of Java 8 starter runtime
- **WHEN** 构建当前 Java 8 AskData Starter
- **THEN** 模块 SHALL 不引入 Spring AI 运行时依赖；Spring AI SHALL 由 Java 17 / Spring Boot 3 Adapter 或 Remote Runtime SPI 接入

#### Scenario: Preserve current starter model registration
- **WHEN** 业务显式引入当前 fw-plus LangChain4j Adapter Starter 并配置已有模型注册信息
- **THEN** AskData SHALL 无需重复模型配置即可发现并使用该聊天模型和 embedding 模型

#### Scenario: Keep optional runtimes out of core contracts
- **WHEN** 构建当前 Java 8 AskData Core 契约
- **THEN** 模块 SHALL 不暴露 LangChain4j、Spring AI、Spring AI Alibaba、AgentScope、Python 或其他 AI 框架类型；可选依赖 SHALL 位于 Adapter 模块或业务应用

#### Scenario: Document incompatible AI runtimes
- **WHEN** Spring AI、Spring AI Alibaba 或 AgentScope 的官方基线要求 Java 17+ 或 Spring Boot 3.x+
- **THEN** 文档 SHALL 记录验证版本、当前分支不实现的原因、Java 17 分支模块边界、Remote Runtime 方式和回滚策略

#### Scenario: Bridge a Java 17 runtime service
- **WHEN** Java 8 应用需要调用 Java 17 / Spring Boot 3 Adapter 能力
- **THEN** AskData SHALL 通过 Remote Runtime 传递项目、操作人、会话、能力 key、路由上下文、脱敏输入、流式事件、错误和审计关联，SQL 执行 SHALL 回到 Java 8 统一安全管线

#### Scenario: Preserve contracts for future Java 17 adapters
- **WHEN** Java 17 / Spring Boot 3 分支新增 Spring AI、Spring AI Alibaba 或 AgentScope Adapter
- **THEN** Adapter SHALL 复用当前模型、Embedding、能力注册、权限、检查点和观测契约，不得要求修改 Java 8 Core 公共签名

#### Scenario: Reject incompatible embedded dependencies
- **WHEN** 某个可选运行时无法通过 Java 8 / Spring Boot 2.7.18 编译与最小启动验证
- **THEN** 该依赖 SHALL 不进入当前 Core、自动配置或 Starter，并提示改用 Java 17 Adapter、业务 SPI 或 Remote Runtime

#### Scenario: Use default model
- **WHEN** AskData 请求未指定模型 key 且业务已配置默认模型
- **THEN** AskData 服务 SHALL 使用默认模型生成 SQL

#### Scenario: Use requested model
- **WHEN** AskData 请求指定有效模型 key
- **THEN** AskData 服务 SHALL 使用该模型配置生成 SQL，而不是默认模型

#### Scenario: Requested model missing
- **WHEN** AskData 请求指定不存在的模型 key
- **THEN** AskData 服务 SHALL 返回模型配置错误，不得静默回退到默认模型

#### Scenario: No default model configured
- **WHEN** AskData 请求未指定模型 key 且业务没有配置默认模型
- **THEN** AskData 服务 SHALL 返回明确错误，说明缺少默认模型配置

### Requirement: SQL safety validation
系统 SHALL 在执行前校验生成 SQL，只允许单条只读查询语句，并拒绝包含危险操作、多语句、未授权对象或资源超限风险的 SQL。

#### Scenario: Reject non-query SQL
- **WHEN** 模型生成 `INSERT`、`UPDATE`、`DELETE`、`DROP`、`ALTER`、`TRUNCATE` 或其他非查询语句
- **THEN** AskData 服务 SHALL 拒绝执行并返回安全校验失败结果

#### Scenario: Reject multi statement SQL
- **WHEN** 模型生成包含多个 SQL 语句的内容
- **THEN** AskData 服务 SHALL 拒绝执行并返回安全校验失败结果

#### Scenario: Reject unauthorized metadata access
- **WHEN** SQL 引用了当前项目或数据源元数据未授权的表、列或系统 schema
- **THEN** AskData 服务 SHALL 拒绝执行并返回包含被拒绝对象的安全校验结果

#### Scenario: Apply physical limit and timeout
- **WHEN** SQL 通过安全校验并允许执行
- **THEN** 执行器 SHALL 以物理行数上限和查询超时执行 SQL，且返回受限执行的审计信息

### Requirement: Multi-agent flow extension
系统 SHALL 提供轻量的多 Agent 与子 Agent 协作抽象，使 AskData 流程中的选表、生成 SQL、SQL 校验、SQL 执行和结果解释可以由主 Agent、独立步骤、独立 Agent 或子 Agent 实现，并由流程执行器串联。系统 SHALL 提供可选 Java LangGraph 图编排适配，但默认流程不得强制依赖它。

#### Scenario: Execute default flow through steps
- **WHEN** 应用使用默认 Agent、流程执行器和顺序编排模式
- **THEN** AskData 服务 SHALL 使用默认步骤顺序完成元数据获取、SQL 生成、SQL 校验和可选 SQL 执行

#### Scenario: Replace one agent step
- **WHEN** 应用提供自定义 SQL 生成 Agent Bean
- **THEN** AskData 流程 SHALL 使用自定义 SQL 生成 Agent，并继续复用默认校验和执行步骤

#### Scenario: Dispatch sub agents
- **WHEN** 某个 AskData Agent 需要把表召回、术语召回或 SQL 修复拆给子 Agent 执行
- **THEN** 系统 SHALL 通过子 Agent 派发接口执行子任务，并将子任务结果归并到当前步骤上下文

#### Scenario: Execute through Java LangGraph
- **WHEN** 应用启用 AskData Java LangGraph 适配且存在经过 Java 8 验证的兼容构件
- **THEN** 系统 SHALL 将主 Agent、子 Agent、检查点和失败分支编排为可观察的状态图

#### Scenario: Optional LangGraph4j JDK8 graph adapter
- **WHEN** 当前 Java 8 AskData starter 需要外部图编排适配，且应用提供经过 Java 8 验证的 LangGraph4j 或 Java LangGraph 兼容适配构件
- **THEN** AskData SHALL 通过可选 adapter 对接该图编排能力；未提供兼容构件时 SHALL 保持默认顺序流程可用

#### Scenario: Fall back when graph adapter is unavailable
- **WHEN** 应用未启用 LangGraph 适配或兼容构件不在类路径中
- **THEN** 系统 SHALL 回退到默认顺序流程，并记录图编排未启用的诊断信息

#### Scenario: Sub agent failure is isolated
- **WHEN** 子 Agent 执行失败或超时
- **THEN** 系统 SHALL 将失败记录到步骤日志，并按该子任务的失败策略继续、降级或终止主流程

#### Scenario: Keep framework independence
- **WHEN** 应用只引入 AskData starter
- **THEN** 系统 SHALL 不要求必须引入 Java LangGraph、LangGraph4j、Agents-Flex、Tinyflow 或其他特定工作流引擎才能运行默认问数流程

### Requirement: Context compression and resumable execution
系统 SHALL 提供可压缩、可持久化的 AskData 会话上下文，使多轮问数、多 Agent 长流程和子 Agent 执行可以在上下文增长时压缩历史信息，并在应用异常退出、实例迁移或重启后从最近稳定检查点继续操作。系统 SHALL 支持内存、JDBC、Redis、向量存储注册表和业务自定义状态实现；集群部署 SHALL 使用共享存储和幂等租约，而非本地内存状态。

#### Scenario: Compress long conversation context
- **WHEN** AskData 会话上下文超过配置的压缩阈值
- **THEN** 系统 SHALL 使用基于 LangChain4j 的上下文压缩器生成摘要，并保留当前步骤所需的结构化字段、模型 key、项目 key、数据源 key、SQL、步骤状态和错误信息

#### Scenario: Recall long-term memory before SQL generation
- **WHEN** AskData 在生成 SQL 前存在当前项目和主体可访问的有效长期记忆
- **THEN** 系统 SHALL 按配置的数量、置信度和权限条件召回记忆摘要，并将其连同来源和语义版本加入受控上下文

#### Scenario: Exclude stale or unauthorized long-term memory
- **WHEN** 长期记忆已过期、语义版本不兼容、跨项目/主体/数据源，或当前权限不允许访问
- **THEN** 系统 SHALL 排除该记忆且不得将其提供给模型、SQL 插件或结果解释步骤

#### Scenario: Write checkpoint after stable step
- **WHEN** AskData 流程完成元数据获取、SQL 生成、SQL 校验、SQL 执行或子 Agent 归并等稳定步骤
- **THEN** 系统 SHALL 通过会话存储写入检查点，记录会话 ID、恢复 token、请求指纹、图版本、已完成步骤和可诊断步骤日志

#### Scenario: Resume after application restart
- **WHEN** 应用重启后调用方携带有效 `conversationId` 或 `resumeToken` 继续 AskData 请求
- **THEN** 系统 SHALL 从最近稳定检查点恢复步骤上下文，并从未完成的后续步骤继续执行

#### Scenario: Resume in another cluster instance
- **WHEN** 原执行实例不可用且其他实例取得同一恢复 token 的有效租约
- **THEN** 新实例 SHALL 从共享检查点继续可重入节点，不得使用原实例的本地内存状态

#### Scenario: Run a durable single-node flow
- **WHEN** 单机应用配置 JDBC 状态存储
- **THEN** 系统 SHALL 在进程重启后从持久化检查点恢复可重入步骤

#### Scenario: Reject incompatible clustered storage
- **WHEN** 应用声明集群模式且只配置内存状态存储
- **THEN** 系统 SHALL 在启动时失败并说明集群需要共享状态、租约和幂等存储

#### Scenario: Do not replay uncertain SQL execution
- **WHEN** 检查点显示 SQL 节点处于执行中但没有已完成结果
- **THEN** 系统 SHALL 返回执行状态不确定的恢复错误，不得自动再次执行该 SQL

#### Scenario: Reject unsafe or incompatible resume
- **WHEN** 检查点缺失、版本不兼容、恢复状态不完整、租约不可取得，或上一步 SQL 未通过安全校验
- **THEN** 系统 SHALL 返回明确恢复错误，不得静默重新执行不确定步骤

### Requirement: Read-only SQL execution
系统 SHALL 提供只读 SQL 执行器，在安全校验通过且请求允许执行时执行 SQL，并返回结构化行列数据。

#### Scenario: Execute valid query
- **WHEN** SQL 安全校验通过且执行开关启用
- **THEN** 执行器 SHALL 执行查询并返回字段列表、数据行和行数

#### Scenario: Skip execution
- **WHEN** 请求或配置禁用 SQL 执行
- **THEN** AskData 服务 SHALL 返回生成 SQL 和步骤信息，但不连接数据库执行查询

### Requirement: Step logging extension
系统 SHALL 提供步骤日志扩展点，记录 AskData 请求的关键阶段，包括元数据获取、Prompt 构建、模型生成、SQL 校验和 SQL 执行。

#### Scenario: Capture successful flow steps
- **WHEN** AskData 请求成功完成
- **THEN** 结果或日志回调 SHALL 包含每个关键阶段的状态和摘要信息

#### Scenario: Capture failed flow step
- **WHEN** AskData 请求在某个阶段失败
- **THEN** 结果或日志回调 SHALL 标记失败阶段并包含错误摘要

### Requirement: Dynamic model routing and resilient fallback
系统 SHALL 支持按照请求显式模型 key、项目、数据源、Agent 任务和策略标签动态路由聊天模型与 embedding 模型。模型路由、候选链、熔断和兜底策略 SHALL 由可替换 Core SPI 表达，且不得将某个模型供应商或实例写死在流程中。

#### Scenario: Route a model dynamically
- **WHEN** 请求未显式指定模型 key，且项目、数据源或当前 Agent 任务配置了有效模型策略
- **THEN** AskData 服务 SHALL 依照已定义优先级选择对应模型，并在响应、检查点和审计中记录最终模型 key 与路由原因

#### Scenario: Switch after a primary model failure
- **WHEN** 主模型发生连接失败、超时、限流、熔断打开或供应商不可用等可判定的基础设施故障
- **THEN** 路由器 SHALL 按配置顺序且不重复地尝试候选模型，并记录每次尝试、故障分类和切换原因

#### Scenario: Use an explicit fallback model
- **WHEN** 主模型和候选模型均因可切换基础设施故障不可用，且策略配置了可用兜底模型
- **THEN** 系统 SHALL 使用该兜底模型完成当前幂等模型节点，并标记响应与审计为兜底路径

#### Scenario: Do not switch on a non-retryable model result
- **WHEN** 模型因内容策略、输入不兼容、请求预算不足或返回明确业务拒绝而失败
- **THEN** 系统 SHALL 返回该错误，不得隐式切换到候选或兜底模型

#### Scenario: Exhaust all model candidates
- **WHEN** 所有配置的主模型、候选模型和兜底模型均不可用
- **THEN** 系统 SHALL 返回可诊断的模型不可用错误，并保留已尝试模型与故障分类；不得伪造结果或回退到未配置模型

#### Scenario: Keep SQL execution outside model failover
- **WHEN** SQL、工具或其他外部副作用节点已经开始执行
- **THEN** 模型路由器 SHALL 不因该节点失败重新调用模型或重放该节点，且系统 SHALL 遵守 SQL 执行状态不确定保护

### Requirement: Multi-question execution and aggregation
系统 SHALL 支持执行显式多问题和拆分后的复合问题；无依赖子问题可在受限并发度内执行，存在依赖的子问题 SHALL 按计划顺序执行。每个子问题 SHALL 独立遵守权限、模型路由、记忆、SQL 安全、超时、检查点与审计约束。

#### Scenario: Execute independent questions concurrently
- **WHEN** 一个多提问计划包含相互独立的子问题，且并发预算允许
- **THEN** 系统 SHALL 在配置的最大并发度内调度这些子问题，并为每个子问题记录独立的模型、SQL 和执行状态

#### Scenario: Respect question dependencies
- **WHEN** 子问题计划声明一个问题依赖另一个问题的结果或完成状态
- **THEN** 系统 SHALL 在依赖问题达到可用终态前不执行依赖问题，并在依赖失败时按计划记录跳过、降级或失败结果

#### Scenario: Return partial multi-question results
- **WHEN** 多提问请求中的部分子问题成功、失败、超时或被取消
- **THEN** 系统 SHALL 返回按子问题 ID 对齐的结构化聚合结果和失败原因，不得将未完成子问题标记为成功

### Requirement: Extensible SQL composition and execution pipeline
系统 SHALL 将 SQL 生成后的组装、参数绑定、方言改写、安全校验、执行准备、执行和结果映射组织为阶段化可扩展管线。SQL 插件 SHALL 通过 Core 能力注册表按确定性顺序参与，并且不得替代最终安全边界。

#### Scenario: Compose SQL with a business plugin
- **WHEN** 业务应用为当前项目注册 SQL 组装插件
- **THEN** 流程 SHALL 允许该插件在其声明阶段附加已授权 SQL 片段或具名参数，并将能力 key、改写摘要和参数元数据写入执行审计

#### Scenario: Apply a dialect or SQL rewrite plugin
- **WHEN** 数据源方言或业务规则需要对查询进行改写
- **THEN** 插件 SHALL 在安全校验前的受控阶段改写不可变 SQL 命令，并使改写后的结果重新接受完整 SQL 校验

#### Scenario: Bind dynamic business values safely
- **WHEN** 业务插件向 SQL 增加租户、组织、时间范围或其他动态条件
- **THEN** 执行器 SHALL 通过具名参数绑定传递动态值，且审计 SHALL 不记录未脱敏的敏感参数值

#### Scenario: Reject an unsafe plugin output
- **WHEN** 任一 SQL 插件输出多语句、写操作、未授权对象、无法绑定的参数或不符合方言规则的 SQL
- **THEN** 系统 SHALL 拒绝执行并记录插件能力 key 与失败阶段，不得因为插件被信任而跳过校验

#### Scenario: Map results without replacing execution safeguards
- **WHEN** 业务应用注册 SQL 结果映射插件
- **THEN** 插件 SHALL 在受限执行完成后映射结构化结果，且不得改变已记录的执行状态、行数上限、超时结论或审计事实

### Requirement: AskData core operational documentation
系统 SHALL 随 Starter 提供详细中文文档，覆盖架构与模块、依赖、快速开始、Ollama 默认厂商、完整配置属性、Java API、能力注册、业务覆盖、模型路由和兜底、向量、SQL 插件、Doris 安全、多 Agent、多提问、长期记忆、项目/操作人隔离、管理员监控、LangSmith/Langfuse、上下文压缩、恢复、单机/集群存储矩阵、迁移和排障。

#### Scenario: Configure single-instance flow
- **WHEN** 开发者仅配置 LangChain4j 模型和 JDBC 数据源
- **THEN** 文档 SHALL 提供可运行的顺序问数示例和安全限制说明

#### Scenario: Configure clustered recoverable flow
- **WHEN** 运维人员启用共享会话存储和 Java LangGraph 图编排
- **THEN** 文档 SHALL 说明租约、检查点、恢复、SQL 不重放约束和故障排查步骤

#### Scenario: Look up a configuration or plugin contract
- **WHEN** 开发者查阅任一配置项、能力 key 或公开扩展 SPI
- **THEN** 文档 SHALL 说明其用途、默认值、取值范围、单机/集群限制、最小示例、失败表现和可覆盖方式

#### Scenario: Follow a safe production runbook
- **WHEN** 运维人员配置模型切换、共享存储、管理员监控日志或恢复策略
- **THEN** 文档 SHALL 明确安全边界、禁止行为、脱敏要求、容量建议和故障恢复步骤，且示例不得包含真实凭据

#### Scenario: Inspect implemented functionality
- **WHEN** 开发者或运维人员查看 Starter 文档首页
- **THEN** 文档 SHALL 提供当前版本已实现功能清单；每项 SHALL 标注所属模块、公开配置/API、单机/集群支持、最小验证证据和已知限制

#### Scenario: Maintain a dedicated feature list document
- **WHEN** 团队在设计、实现或验收阶段追踪 SQLBot 与 WrenAI 功能覆盖
- **THEN** `openspec/changes/rework-ai-askdata-core-starter/feature-list.md` SHALL 作为设计期功能清单事实源存在，并 SHALL 被正式说明文档转换为追踪矩阵；矩阵字段至少包含 product、category、feature、source、priority、duplicatePolicy、change、module、apiOrConfig、storage、docs、tests、status 和 limitation

#### Scenario: Distinguish implementation from roadmap
- **WHEN** 某项能力需要真实租户、外部依赖或业务 UI 接入验证
- **THEN** 文档 SHALL 在“未包含/后续能力”中单独说明，不得将其列入已实现功能清单

#### Scenario: Trace every SQLBot v1 capability
- **WHEN** 团队验收 AskData 批次对 SQLBot v1 的功能覆盖
- **THEN** 文档 SHALL 提供以 `https://gitee.com/fit2cloud-feizhiyun/SQLBot` 和 `https://sqlbot.org/docs/v1/` 为基线的功能追踪矩阵，将每项功能映射到 batch change、实现模块、接口/配置、用户文档和验证证据；矩阵至少 SHALL 覆盖部署/命令行、工作空间/项目、数据源、智能问数、仪表板、小助手、权限、术语、模型配置、模型接入、训练检索、SQL 示例、Prompt、MCP Server、嵌入式对接、DataEase 接入、X-Pack 登录认证、平台对接、企业微信、钉钉、飞书、参数配置、外观设置和操作日志

#### Scenario: Prevent incomplete SQLBot coverage claims
- **WHEN** SQLBot v1 功能追踪矩阵中仍存在未归属、需外部验证或业务接入状态不明的功能
- **THEN** 项目 SHALL 标记该能力为阻塞或后续项，且不得宣称 SQLBot 功能已完整实现

#### Scenario: Trace WrenAI enterprise GenBI capabilities
- **WHEN** 团队验收 AskData 批次对 Canner/WrenAI 企业级 GenBI 能力的覆盖
- **THEN** 文档 SHALL 提供 WrenAI 功能追踪矩阵，将自然语言问数、Text-to-SQL、Text-to-Chart、AI 摘要/报告、可嵌入 API、MDL 语义层、模型/列/嵌套列/关系/指标/视图、deploy manifest、数据源连接器、模型接入、语义索引、语义描述、关系推荐、问题推荐、instructions、SQL pair、ask feedback、evaluation、dry-plan、dry-run、metadata constraints、validate、model substitute、preview data、任务轮询、dashboard 和遥测映射到 batch change、实现模块、接口/配置、用户文档和验证证据；与 SQLBot 重复的条目 SHALL 标记 SQLBot 优先

#### Scenario: Document Vue3-first delivery order
- **WHEN** 团队查看批次实施计划或 Starter 文档
- **THEN** 文档 SHALL 明确先实现 Vue3 组件化前端控制台、前端 API SDK、Mock 服务和页面级交互原型，再实现后端 Starter 并按前端契约对齐 API、事件和数据库设计，最后进行前后端联调

#### Scenario: Read detailed backend design documentation
- **WHEN** 开发者查看 AskData 后端 Starter 设计文档
- **THEN** 文档 SHALL 详细说明模块边界、包结构、领域对象、SPI 清单、默认实现、自动配置条件、异常模型、配置模型、兼容迁移、前端联调契约和实现顺序

#### Scenario: Inspect business and technical flow diagrams
- **WHEN** 开发者或业务方查看流程图文档
- **THEN** 文档 SHALL 提供业务流程图和技术流程图，覆盖项目治理、数据源接入、训练/语义模型、问数、多提问、推荐、模型路由、多 Agent、SQL 安全、状态恢复、监控审计和前后端联调

#### Scenario: Inspect database design documentation
- **WHEN** 开发者或 DBA 查看数据库设计文档
- **THEN** 文档 SHALL 列出项目、成员权限、数据源、元数据、语义模型/MDL、训练资产、会话、长期记忆、推荐、SQL 审计、监控日志、Agent 状态、模型/API Key、MCP/API、企业配置、操作日志和插件注册表结构，并说明字段、主键、索引、隔离字段、脱敏字段、保留策略、单机/集群建议和迁移版本

#### Scenario: Inspect test case documentation
- **WHEN** 团队准备实现或验收 AskData Starter
- **THEN** 文档 SHALL 提供测试用例清单，覆盖单元、契约、自动配置、SQL 安全、存储一致性、模型路由/兜底、多 Agent/恢复、长期记忆/推荐权限、监控审计、文档示例和后续前后端联调，并为每个用例记录前置条件、输入、期望结果、验证证据和关联 SQLBot/WrenAI 功能项

#### Scenario: Document runtime replacement steps
- **WHEN** 业务方计划从默认 LangChain4j/Ollama 替换为 Spring AI、Spring AI Alibaba、AgentScope Java 2.0+ 或自定义运行时
- **THEN** 文档 SHALL 提供依赖引入、配置前缀、能力 key、Bean/SPI 覆盖、模型/embedding/向量/工具/RAG/Agent/状态/观测映射、限制、回滚和验证用例；若目标运行时不兼容 Java 8 / Spring Boot 2.7.18，文档 SHALL 额外提供 Java 17 独立适配服务的工程结构、部署拓扑、协议选择、鉴权签名、上下文传递、超时/重试/熔断、流式返回、错误映射、监控审计、版本矩阵和联调测试

#### Scenario: Support SQLBot external integration capabilities
- **WHEN** 团队验收 SQLBot 对接其他系统能力
- **THEN** 文档与接口契约 SHALL 覆盖 SQLBot 文档中的全部外部系统对接能力，至少包括模型接入/Ollama、MCP Server、嵌入式对接、DataEase 接入、MaxKB、Dify、Coze、n8n、X-Pack 登录认证、平台对接、企业微信、钉钉、飞书、扫码登录、免登、组织用户同步和参数配置；MCP SHALL 覆盖 `mcp_start`、`mcp_question`、`mcp_ws_list`、`mcp_datasource_list`、SSE/Streamable HTTP、token/chat_id；嵌入 SHALL 覆盖基础/高级/页面嵌入、宿主凭证映射、跨域、appId/appSecret；DataEase SHALL 覆盖 `/de2api/sqlbot/datasource`、`x-de-token`、AES-Key、审计和联调测试
- **AND** 企业微信、钉钉和飞书登录、扫码、免登、回调和组织同步 SHALL 由业务 IAM/JustAuth 适配完成；AskData SHALL 只接收已认证和授权映射后的 `projectId + operatorId` 及授权摘要，不在 Starter 内自研这些企业平台协议

### Requirement: Authorization-aware metadata and SQL pipeline
系统 SHALL 在元数据加载、Prompt 构建、SQL 插件链、最终 SQL 校验和只读执行阶段使用同一治理授权摘要。插件 MAY 收窄 SQL 文本或授权对象，但 MUST NOT 清空、扩大、替换项目/操作人上下文、参数、授权对象或行列权限摘要。

#### Scenario: Filter metadata by authorized objects
- **WHEN** AskData 为当前请求构建 Prompt 元数据
- **THEN** Flow SHALL 不可覆盖地只保留授权摘要允许的数据源、表和列；无法判定对象归属的术语、样例和 Prompt 片段 SHALL 默认丢弃，不得向模型暴露未授权对象

#### Scenario: Preserve authorization through SQL plugins
- **WHEN** SQL 插件链改写生成 SQL
- **THEN** 插件结果 SHALL 保留原始项目上下文、参数和授权摘要；任何扩大授权对象、移除行级谓词或替换操作人边界的结果 SHALL 被拒绝

#### Scenario: Revalidate SQL against authorization summary
- **WHEN** 所有 SQL 插件完成后准备执行最终 SQL
- **THEN** SQL 校验器 SHALL 基于授权摘要重新检查引用表、列、系统 schema、行级谓词和只读限制，未授权引用 SHALL 导致安全校验失败

#### Scenario: Inject row predicates with parameters
- **WHEN** 业务治理桥接提供行级数据范围条件
- **THEN** SQL 管线 SHALL 将受控 SQL 片段注入匹配表查询、与已有条件以 `AND` 合并，并从最终命令绑定具名参数；同名参数冲突、解析失败或谓词无法安全落实时 SHALL fail-closed

#### Scenario: Reject unauthorized columns after model or plugin rewrite
- **WHEN** 模型或 SQL 插件生成只引用授权表但包含未授权列或受限通配列的 SQL
- **THEN** 最终校验 SHALL 解析列引用并拒绝该 SQL；受限列授权下无法安全解析或归属的 SQL SHALL fail-closed
