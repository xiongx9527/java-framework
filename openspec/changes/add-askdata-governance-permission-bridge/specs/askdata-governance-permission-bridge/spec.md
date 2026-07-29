## ADDED Requirements

### Requirement: Governance authorization bridge
系统 SHALL 提供 Java 8 兼容、无 Spring 依赖的 AskData 治理授权桥接 SPI，用于解析当前 `projectId + operatorId` 在项目、数据源、表、列、行级谓词和管理员日志查询上的授权摘要。未提供业务治理实现时，生产默认实现 SHALL 拒绝真实数据访问和管理员监控日志查询。

#### Scenario: Resolve authorization before metadata exposure
- **WHEN** AskData 处理带有 `projectId + operatorId` 的问数请求
- **THEN** 系统 SHALL 在加载 Prompt 元数据前解析该操作人在当前项目和数据源范围内的授权摘要，并且只暴露授权表、授权列和允许的行级约束

#### Scenario: Reject missing governance implementation for protected data
- **WHEN** 应用未注册业务治理授权实现且请求访问真实数据源
- **THEN** Starter SHALL 返回明确的权限配置错误或拒绝结果，不得默认允许读取项目数据、元数据或监控日志

#### Scenario: Keep host IAM types outside Core
- **WHEN** 业务 IAM、JustAuth、Sa-Token、Spring Security 或宿主项目成员系统接入 AskData
- **THEN** 它们 SHALL 通过 Core 治理 SPI 映射为稳定授权摘要，Core 公共 API 不得暴露宿主框架类型或外部平台凭据

### Requirement: Project member and datasource authorization
系统 SHALL 支持业务应用按项目成员、角色、数据源和资源范围授权 AskData 请求。授权摘要 SHALL 包含项目、操作人、数据源、授权对象、授权版本和可审计来源。

#### Scenario: Allow an authorized datasource
- **WHEN** 当前操作人在项目中被授权访问某个数据源
- **THEN** AskData SHALL 允许后续元数据加载和 SQL 生成继续，并在审计摘要中记录授权来源和授权版本

#### Scenario: Reject an unauthorized datasource
- **WHEN** 当前操作人请求未授权数据源或已被撤销权限的数据源
- **THEN** AskData SHALL 在模型调用、SQL 生成或 SQL 执行前拒绝请求，并写入脱敏权限拒绝监控事件

#### Scenario: Prevent cross-project reuse
- **WHEN** 操作人持有一个项目中的数据源、会话、token 或授权摘要并尝试用于另一个项目
- **THEN** 系统 SHALL 拒绝访问，即使数据源 key、会话 ID 或外部用户 ID 相同

### Requirement: Administrator monitoring access bridge
系统 SHALL 使用治理授权桥接控制管理员监控日志查询。本项目管理员只能查询其授权项目；平台管理员跨项目查询必须由业务治理实现显式返回允许范围并记录审计。

#### Scenario: Project admin queries project logs
- **WHEN** 项目管理员查询本项目 AskData 监控日志
- **THEN** 系统 SHALL 返回该项目内脱敏日志，并保持项目过滤、授权来源和查询审计

#### Scenario: Platform admin queries allowed projects
- **WHEN** 平台管理员发起跨项目监控日志查询
- **THEN** 系统 SHALL 仅返回治理授权摘要中明确允许的项目范围，并记录该跨项目查询事件

#### Scenario: Ordinary operator is denied
- **WHEN** 普通操作人调用管理员监控日志查询能力
- **THEN** 系统 SHALL 拒绝请求，不得把管理员日志接口作为个人历史接口复用
