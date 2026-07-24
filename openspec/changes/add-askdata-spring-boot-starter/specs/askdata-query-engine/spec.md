## ADDED Requirements

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
系统 SHALL 提供可配置的 Prompt 模板和数据库方言规则，使生成 SQL 遵守目标数据库的标识符引用、分页限制和只读查询约束。

#### Scenario: Apply query limit policy
- **WHEN** 请求未显式指定返回数量且默认查询限制启用
- **THEN** Prompt 或 SQL 校验阶段 SHALL 要求生成 SQL 包含默认行数限制

#### Scenario: Dialect-specific identifier rules
- **WHEN** 数据源方言为 MySQL、PostgreSQL 或 ClickHouse
- **THEN** Prompt 构建 SHALL 使用该方言对应的标识符引用和 limit 规则

### Requirement: Multiple model configuration
系统 SHALL 支持多个模型配置，并允许配置一个默认模型；AskData 请求可以显式指定模型 key，未指定时使用默认模型。

#### Scenario: Prefer LangChain4j model integration
- **WHEN** 应用中存在可用 LangChain4j 模型 Bean 或 LangChain4j 模型注册信息
- **THEN** AskData 默认模型注册表 SHALL 优先使用 LangChain4j 完成聊天模型和 embedding 模型适配

#### Scenario: Optional fallback model adapters
- **WHEN** LangChain4j 无法满足某个 AskData 模型能力且应用显式提供其他模型适配 Bean
- **THEN** AskData SHALL 按 Spring AI（仅未来 Spring Boot 3 / Java 17 版本）、Agents-Flex、其他实现的顺序通过统一模型接口使用适配 Bean，当前 Java 8 Starter 不得要求核心流程直接依赖 Spring AI、Agents-Flex、EasyAi 或其他框架

#### Scenario: Keep Spring AI out of Java 8 starter runtime
- **WHEN** 构建当前 Java 8 AskData starter
- **THEN** 模块 SHALL 不引入 Spring AI 运行时依赖；Spring AI 只作为后续 Spring Boot 3 / Java 17 版本的适配方向记录

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

### Requirement: SQL safety validation
系统 SHALL 在执行前校验生成 SQL，只允许只读查询语句，并拒绝包含危险操作或多语句执行风险的 SQL。

#### Scenario: Reject non-query SQL
- **WHEN** 模型生成 `INSERT`、`UPDATE`、`DELETE`、`DROP`、`ALTER`、`TRUNCATE` 或其他非查询语句
- **THEN** AskData 服务 SHALL 拒绝执行并返回安全校验失败结果

#### Scenario: Reject multi statement SQL
- **WHEN** 模型生成包含多个 SQL 语句的内容
- **THEN** AskData 服务 SHALL 拒绝执行并返回安全校验失败结果

### Requirement: Multi-agent flow extension
系统 SHALL 提供轻量的多 agent 与子 agent 协作抽象，使 AskData 流程中的选表、生成 SQL、SQL 校验、SQL 执行和结果解释可以由主 Agent、独立步骤、独立 Agent 或子 Agent 实现，并由流程执行器串联。

#### Scenario: Execute default flow through steps
- **WHEN** 应用未提供自定义 Agent 或流程执行器
- **THEN** AskData 服务 SHALL 使用默认步骤顺序完成元数据获取、SQL 生成、SQL 校验和可选 SQL 执行

#### Scenario: Replace one agent step
- **WHEN** 应用提供自定义 SQL 生成 Agent Bean
- **THEN** AskData 流程 SHALL 使用自定义 SQL 生成 Agent，并继续复用默认校验和执行步骤

#### Scenario: Dispatch sub agents
- **WHEN** 某个 AskData Agent 需要把表召回、术语召回或 SQL 修复拆给子 Agent 执行
- **THEN** 系统 SHALL 通过子 Agent 派发接口执行子任务，并将子任务结果归并到当前步骤上下文

#### Scenario: Sub agent failure is isolated
- **WHEN** 子 Agent 执行失败或超时
- **THEN** 系统 SHALL 将失败记录到步骤日志，并按该子任务的失败策略继续、降级或终止主流程

#### Scenario: Keep framework independence
- **WHEN** 应用只引入 AskData starter
- **THEN** 系统 SHALL 不要求必须引入 Agents-Flex、Tinyflow 或其他特定工作流引擎才能运行默认问数流程

### Requirement: Context compression and resumable execution
系统 SHALL 提供可压缩、可持久化的 AskData 会话上下文，使多轮问数、多 agent 长流程和子 agent 执行可以在上下文增长时压缩历史信息，并在应用异常退出或重启后从最近稳定检查点继续操作。

#### Scenario: Compress long conversation context
- **WHEN** AskData 会话上下文超过配置的压缩阈值
- **THEN** 系统 SHALL 使用上下文压缩器生成摘要，并保留当前步骤所需的结构化字段、模型 key、数据源 key、SQL、步骤状态和错误信息

#### Scenario: Write checkpoint after stable step
- **WHEN** AskData 流程完成元数据获取、SQL 生成、SQL 校验、SQL 执行或子 Agent 归并等稳定步骤
- **THEN** 系统 SHALL 通过会话存储写入检查点，记录会话 ID、恢复 token、已完成步骤和可诊断步骤日志

#### Scenario: Resume after application restart
- **WHEN** 应用重启后调用方携带有效 `conversationId` 或 `resumeToken` 继续 AskData 请求
- **THEN** 系统 SHALL 从最近稳定检查点恢复步骤上下文，并从未完成的后续步骤继续执行

#### Scenario: Reject unsafe or incompatible resume
- **WHEN** 检查点缺失、版本不兼容、恢复状态不完整，或上一步 SQL 未通过安全校验
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
