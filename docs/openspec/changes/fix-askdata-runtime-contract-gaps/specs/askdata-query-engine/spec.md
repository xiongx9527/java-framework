## MODIFIED Requirements

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
- **THEN** AskData SHALL 无需重复模型配置即可发现并使用对应 Chat 和 Embedding 模型

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

## ADDED Requirements

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
