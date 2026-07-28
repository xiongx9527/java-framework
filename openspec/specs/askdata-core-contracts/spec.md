# askdata-core-contracts Specification

## Purpose
TBD - created by archiving change rework-ai-askdata-core-starter. Update Purpose after archive.
## Requirements
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

#### Scenario: Add another AI runtime adapter without changing core
- **WHEN** 业务或后续模块需要接入 Spring AI、Spring AI Alibaba、AgentScope Java 2.0+ 或其他 AI 运行时
- **THEN** 新适配 SHALL 通过 Core AskData SPI、能力注册表和可选依赖完成，不得要求修改 Core 领域对象或默认 SQL 安全管线

### Requirement: Single-node and clustered state storage contracts
系统 SHALL 在 Core AskData API 中定义项目状态、会话、检查点、租约、幂等键和审计的存储 SPI，并支持默认内存、JDBC、Redis、LangChain4j 向量存储注册表和业务自定义实现。不同状态类别 SHALL 能通过能力 key 组合多个存储实现。所有对话及其派生检查点、恢复 token、压缩上下文和审计记录 SHALL 以 `projectId + operatorId` 为强制归属，其中项目是第一层分区前提，`operatorId` 必须来自已认证操作人上下文。

#### Scenario: Store a conversation within a project and person
- **WHEN** 已认证主体在指定项目中创建或继续 AskData 对话
- **THEN** 系统 SHALL 将会话、消息、检查点、恢复 token、长期记忆关联和审计记录同时绑定到该 `projectId` 与已认证的 `operatorId`

#### Scenario: Reject an incomplete conversation scope
- **WHEN** 创建、读取、恢复或写入对话时缺少 `projectId` 或已认证 `operatorId`
- **THEN** 系统 SHALL 拒绝该操作，不得创建全局、匿名或仅按人归属的默认会话

#### Scenario: Prevent cross-project or cross-person conversation access
- **WHEN** 操作人尝试在其他项目或以其他操作人身份读取、继续或恢复会话
- **THEN** 系统 SHALL 拒绝访问；即使会话 ID、恢复 token 或数据源 key 有效也不得绕过 `projectId + operatorId` 归属校验

#### Scenario: Use default memory storage on a single node
- **WHEN** 单机应用未配置某个状态类别的外部存储能力
- **THEN** Starter SHALL 使用该类别的默认内存实现，并明确其重启后不保留、不得用于集群的限制

#### Scenario: Compose multiple storage implementations
- **WHEN** 应用为会话、租约、审计、长期记忆或向量检索配置不同能力 key
- **THEN** Starter SHALL 按状态类别组合这些实现，例如使用 JDBC 会话、Redis 租约和持久化向量检索，而不要求它们来自同一存储产品

### Requirement: Multi-store Agent runtime state
系统 SHALL 定义独立的 Agent 运行状态存储 SPI，持久化图状态、节点/子任务状态、计划、工具调用摘要、模型路由尝试、步骤日志、恢复位置和失败策略结果。该 SPI SHALL 支持默认内存、JDBC、Redis 和业务自定义实现，并能与会话/检查点使用同一或不同的物理存储。

#### Scenario: Use default in-memory Agent state on a single node
- **WHEN** 单机应用未配置 Agent 状态存储能力
- **THEN** Starter SHALL 使用默认内存实现保存当前进程内的 Agent 状态，并明确该状态在重启后丢失

#### Scenario: Persist Agent state in an external store
- **WHEN** 应用为 Agent 状态选择 JDBC、Redis 或业务自定义能力
- **THEN** 系统 SHALL 通过该能力保存和读取 Agent 节点、子任务及恢复状态，而无需改变 Agent 编排接口

#### Scenario: Reject local Agent state in a cluster
- **WHEN** 应用以集群模式运行且 Agent 状态能力为默认内存或其他本地实现
- **THEN** Starter SHALL 在启动期拒绝该配置，并要求具备共享状态、租约和幂等语义的实现

#### Scenario: Run with in-memory state on a single node
- **WHEN** 应用以单机模式启动且未配置外部状态存储
- **THEN** Starter SHALL 使用内存实现提供临时会话、检查点和审计状态

#### Scenario: Persist state through JDBC
- **WHEN** 应用配置 JDBC 状态存储
- **THEN** 系统 SHALL 以事务性方式持久化项目状态、会话、检查点和审计，并允许单机或多个实例读取同一状态

#### Scenario: Coordinate clustered state through Redis
- **WHEN** 应用以集群模式启动并配置 Redis 状态存储
- **THEN** 系统 SHALL 使用 Redis 保存热状态、恢复租约和幂等键，使任一实例能够按契约恢复可重入流程

#### Scenario: Use registered vector storage
- **WHEN** 训练检索功能需要持久化向量知识
- **THEN** AskData SHALL 通过现有 LangChain4j 向量存储注册表选择已注册的向量存储，而不在 Core API 中绑定具体向量数据库

#### Scenario: Reject local state in clustered mode
- **WHEN** 应用声明集群模式但状态存储只支持本地内存
- **THEN** Starter SHALL 在启动时拒绝该配置并说明需要共享状态存储

#### Scenario: Replace state storage in a business application
- **WHEN** 业务应用实现并注册 Core AskData 状态存储 SPI
- **THEN** Starter SHALL 使用该实现处理对应状态类型，而不要求修改 Starter 内部代码

### Requirement: Permission-aware long-term memory contracts
系统 SHALL 定义独立于检查点的长期记忆 SPI，支持保存、召回、查询、过期和遗忘经过脱敏的用户偏好、业务事实、已确认问数、语义线索和结果解释摘要。项目是记忆的前提分区，每条个人记忆 SHALL 包含 `projectId`、从认证上下文取得的 `operatorId`、数据源、权限范围、来源、语义版本、置信度和保留策略；默认实现 SHALL 为单机内存。

#### Scenario: Recall authorized long-term memory
- **WHEN** AskData 在当前项目、主体和数据源范围内处理新问题
- **THEN** 系统 SHALL 仅召回仍有效且当前主体有权访问的记忆，并将其作为受控模型上下文而不是未校验 SQL

#### Scenario: Store a confirmed question-and-SQL memory
- **WHEN** 用户或业务流程确认某个问数结果可复用
- **THEN** 记忆 SPI SHALL 存储经过脱敏的自然语言、语义线索、SQL 摘要、授权范围和语义版本，供后续已授权请求检索

#### Scenario: Forget or expire memory
- **WHEN** 记忆达到保留期、所属权限被撤销、用户请求删除或语义版本不再兼容
- **THEN** 记忆实现 SHALL 使该条目不再被召回，并保留满足审计策略的删除或过期记录

#### Scenario: Replace the long-term memory backend
- **WHEN** 业务应用注册向量、JDBC、Redis 或其他符合长期记忆 SPI 的实现
- **THEN** Starter SHALL 通过能力注册表使用该实现，且不要求 Core 引入具体向量数据库或存储客户端

### Requirement: Person-centric recommendation contracts
系统 SHALL 定义推荐请求、候选过滤、操作人画像和排序 SPI。个性化推荐请求 SHALL 同时包含当前 `projectId` 与从认证上下文取得的 `operatorId`；项目、数据源和权限首先限制候选集，个人偏好、长期记忆、反馈和已确认问数 SHALL 只用于当前操作人在该项目内的排序。

#### Scenario: Rank recommendations for the current person
- **WHEN** 当前操作人在已授权项目内请求推荐问题、指标或数据对象
- **THEN** 推荐实现 SHALL 先按项目、数据源和权限过滤候选，再使用该操作人自己的偏好、反馈和长期记忆排序，并记录操作人与候选来源

#### Scenario: Prevent cross-person recommendation leakage
- **WHEN** 两个不同操作人访问同一项目、租户或数据源
- **THEN** 系统 SHALL 不使用一方的个人偏好、问数历史、反馈或排序特征为另一方生成个性化推荐

#### Scenario: Handle an incomplete recommendation scope
- **WHEN** 推荐请求缺少项目或已认证操作人身份
- **THEN** 系统 SHALL 拒绝个性化推荐；只有显式启用的通用推荐策略可以返回不依赖个人行为的结果

### Requirement: In-memory vector mode
系统 SHALL 通过向量能力插件提供 `memory` 向量存储模式，作为未配置外部向量存储时的默认单机开发/临时检索实现；Core 契约不得暴露具体向量数据库客户端类型。

#### Scenario: Use the default in-memory vector store
- **WHEN** 单机应用启用向量检索但未选择外部向量能力
- **THEN** Starter SHALL 选择 `memory` 向量能力完成当前进程内的写入和召回，并在文档与诊断中说明重启后丢失

#### Scenario: Reject in-memory vectors in a cluster
- **WHEN** 应用以集群模式启用需要共享知识或长期记忆的向量检索，且选择 `memory` 向量能力
- **THEN** Starter SHALL 在启动期拒绝该组合并要求选择共享向量存储或业务自定义实现

### Requirement: Multi-question planning contracts
系统 SHALL 支持单问题、显式问题列表和自然语言复合问题，并定义子问题计划、稳定子问题 ID、依赖关系、执行预算和聚合结果的 Core 领域模型与替换 SPI。

#### Scenario: Submit explicit multiple questions
- **WHEN** 调用方在同一 AskData 请求中提供多个问题
- **THEN** Core API SHALL 为每个问题创建可追溯子问题，并保持父请求、项目、主体和审计关联关系

#### Scenario: Replace compound-question planning
- **WHEN** 业务应用提供自定义问题拆分或依赖规划实现
- **THEN** Starter SHALL 使用该实现创建子问题计划，且不要求业务应用重写模型、SQL 安全或结果聚合流程

