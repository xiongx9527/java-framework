## ADDED Requirements

### Requirement: Typed AI and AskData core contracts
系统 SHALL 在 `fw-plus-ai-model` 提供厂商无关的模型定义、注册和类型化 Chat/Embedding 执行契约，在 `fw-plus-ai-store-embedding` 提供厂商无关的向量存储契约，并在 `fw-plus-ai-askdata` 提供 Java 8 兼容、无 Spring Boot 与具体 AI 框架依赖的 AskData 领域模型、服务接口和业务 SPI。

#### Scenario: Use typed model contracts without an AI framework
- **WHEN** 业务模块只依赖 `fw-plus-ai-model`
- **THEN** 模块 SHALL 能定义、注册并调用类型化 Chat/Embedding 模型，且公开签名不得暴露 LangChain4j、Spring AI、Spring AI Alibaba、AgentScope 或 Spring 类型

#### Scenario: Preserve model identity and execution metadata
- **WHEN** Adapter 完成 Chat 或 Embedding 调用
- **THEN** 结果 SHALL 提供实际模型 key、厂商无关的结束或错误语义及可用的用量摘要，且请求 SHALL 支持超时和不可变模型参数

#### Scenario: Keep embedding generation separate from storage
- **WHEN** AskData 将文本向量化并查询向量存储
- **THEN** Embedding 模型 SHALL 只负责向量生成，Embedding Store SHALL 只负责向量保存与检索，二者通过中立向量值对象组合而不暴露框架 SDK 类型

#### Scenario: Preserve authorized embedding filters
- **WHEN** AskData 检索请求携带项目、数据源或业务授权过滤条件
- **THEN** 中立 Store 查询 SHALL 传递不可变过滤条件，Adapter 不支持该过滤能力时 SHALL 明确拒绝或由上层完成可证明安全的过滤，不得静默忽略

### Requirement: Executable Core SPI replacement boundary
系统 SHALL 在 Core AskData API 中定义模型、Embedding、向量检索、推荐、图编排、Agent、元数据、检索、项目权限、会话、检查点、SQL、审计和接口发布的替换 SPI，默认 Starter Flow SHALL 直接消费这些 Core SPI。

#### Scenario: Replace a single Core capability
- **WHEN** 业务应用提供某个 Core AskData SPI 的实现
- **THEN** Starter SHALL 使用该实现替代对应默认实现，且不得因内部同名接口或旧 Bean 仍存在而绕过业务实现

#### Scenario: Bridge a legacy starter implementation
- **WHEN** 应用仍提供旧自动配置层的模型、元数据或会话接口
- **THEN** Starter SHALL 通过单向兼容 Adapter 接入 Core SPI，并提供迁移诊断；Core SPI 实现不得反向依赖旧接口

#### Scenario: Replace the complete business service
- **WHEN** 业务应用实现并注册 Core `AskDataService`
- **THEN** Starter SHALL 保留该业务实现作为对外问数服务，而不创建冲突的默认服务

#### Scenario: Replace the complete Agent implementation
- **WHEN** 业务应用实现并注册 Core Agent 派发/图编排、状态存储和必要服务接口
- **THEN** Starter SHALL 使用该业务实现接管 Agent 运行与恢复，而不要求继承或修改 Starter 内部类

### Requirement: Executable capability registry wiring
系统 SHALL 提供不依赖 Spring 的版本化能力描述符、能力提供者和唯一能力注册表。模型、Embedding、向量存储、SQL、状态、图编排和事件发布 Adapter SHALL 通过该机制声明能力类型、稳定 key、支持的契约版本区间、优先级和实现专属配置命名空间，Starter SHALL 在启动期完成发现和校验。

#### Scenario: Register a framework model adapter
- **WHEN** 应用引入 LangChain4j 或后续 Java 17 运行时 Adapter 并配置稳定能力 key
- **THEN** Starter SHALL 在不修改 Core 或默认 Flow 的前提下注册并按 key 选择该能力

#### Scenario: Reject ambiguous capability selection
- **WHEN** 同一 key 存在重复实现、配置引用未知 key 或契约版本不兼容
- **THEN** Starter SHALL 在启动期返回包含能力类型、key、实现和版本的明确诊断，且不得静默选择或回退

### Requirement: Stage-complete SQL composition plugins
系统 SHALL 定义不可变 SQL 命令、受控结构片段、具名参数、执行上下文和阶段化 SQL 拦截器 SPI，使业务方能够参与 SQL 组装、参数绑定、改写、执行准备、结果映射和审计补充。

#### Scenario: Preserve plugin parameters
- **WHEN** SQL 插件返回改写 SQL 和新增的租户、组织、时间范围或其他具名参数
- **THEN** 插件链 SHALL 将参数传递到后续校验和执行阶段，拒绝参数名冲突，且不得记录未脱敏参数值

#### Scenario: Execute plugins by declared stage
- **WHEN** 多个插件声明组装、改写、执行准备、结果映射或审计阶段
- **THEN** 管线 SHALL 按阶段、显式优先级和稳定 key 生成确定顺序，并在最终执行前重新完成权限与 SQL 安全校验

#### Scenario: Prevent plugins from expanding authorization
- **WHEN** 插件修改 SQL 命令中的授权摘要、表列范围或安全上下文
- **THEN** 管线 SHALL 只允许保持或收窄授权，任何扩大或无法证明安全的变化 SHALL 被拒绝

### Requirement: Business-selected LangChain4j adapters
系统 SHALL 在当前 Java 8 分支提供 LangChain4j 对中立模型、Embedding、向量存储和 Core AskData SPI 的 Adapter；Adapter SHALL 由业务显式引入，不得成为 AskData Starter 的强制传递依赖。

#### Scenario: Use LangChain4j when selected by the business
- **WHEN** 业务应用引入 LangChain4j Adapter、所需 Provider SDK 并配置 `framework=langchain4j`
- **THEN** Starter SHALL 将模型和 Embedding 能力注册到中立注册表并供 AskData Core Flow 使用

#### Scenario: Start without a concrete AI framework
- **WHEN** 应用只引入 AskData Starter 且尚未选择模型 Adapter
- **THEN** 应用上下文 SHALL 能启动；实际请求模型能力时 SHALL 返回明确的未配置诊断，不得隐式创建 Ollama 或其他 Provider

#### Scenario: Keep Java 17 adapters out of the Java 8 branch
- **WHEN** 构建当前 Java 8 / Spring Boot 2.7.18 分支
- **THEN** Spring AI、Spring AI Alibaba 和 AgentScope 运行时依赖 SHALL 不进入 Core、自动配置或 Starter；其 Adapter SHALL 在 Java 17 / Spring Boot 3 分支实现并复用当前契约或 Remote Runtime 边界
