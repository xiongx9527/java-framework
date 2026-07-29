## ADDED Requirements

### Requirement: Governance bridge contracts
系统 SHALL 在 Core AskData API 中定义治理权限桥接 SPI 和领域对象，用于表达项目成员、数据源授权、授权对象、列级授权、行级谓词、授权版本、授权来源和管理员日志查询范围。该契约 SHALL 保持 Java 8 兼容且不依赖 Spring、JustAuth、Sa-Token、Spring Security 或宿主 IAM 类型。

#### Scenario: Business application provides governance bridge
- **WHEN** 业务应用注册符合 Core 契约的治理授权实现
- **THEN** Starter SHALL 使用该实现解析 AskData 请求、元数据加载、SQL 管线、日志查询和审计所需的授权摘要

#### Scenario: Default bridge denies protected access
- **WHEN** 应用未提供治理授权实现而请求真实数据源、授权元数据或管理员监控日志
- **THEN** 默认桥接 SHALL 返回拒绝摘要或权限配置错误，不得以空权限解释为全量允许

#### Scenario: Preserve existing permission evaluator compatibility
- **WHEN** 业务应用仍只提供旧的 `AskDataPermissionEvaluator`
- **THEN** Starter MAY 将其用于粗粒度能力判定，但 SHALL 不把它解释为表、列、行级和管理员跨项目授权的完整来源
