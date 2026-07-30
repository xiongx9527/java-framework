## ADDED Requirements

### Requirement: Governance denial observability
系统 SHALL 对治理授权解析、权限拒绝、授权版本、管理员日志查询和跨项目拒绝发布脱敏监控事件。事件 SHALL 携带项目、操作人、数据源、能力、拒绝原因和授权来源摘要，但不得包含凭据、原始 SQL 参数值或外部平台密钥。

#### Scenario: Record denied datasource access
- **WHEN** 操作人请求未授权数据源、表或列
- **THEN** 系统 SHALL 写入权限拒绝监控事件，并能按项目、操作人和会话关联到该次失败请求

#### Scenario: Record administrator log query
- **WHEN** 项目管理员或平台管理员查询 AskData 监控日志
- **THEN** 系统 SHALL 记录查询主体、授权项目范围、过滤条件摘要和结果数量，不得记录敏感日志正文或凭据

#### Scenario: Correlate governance decisions with SQL audit
- **WHEN** SQL 最终校验因授权对象或行级谓词不满足而失败
- **THEN** 监控事件和 SQL 审计 SHALL 使用相同请求、会话和授权版本标识，便于定位权限配置问题
