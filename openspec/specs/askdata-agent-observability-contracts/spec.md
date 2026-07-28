# askdata-agent-observability-contracts Specification

## Purpose
TBD - created by archiving change rework-ai-askdata-core-starter. Update Purpose after archive.
## Requirements
### Requirement: Vendor-neutral Agent observability contracts
系统 SHALL 在 `fw-plus-ai-askdata` 中定义不依赖 Spring、LangChain4j 或观测厂商 SDK 的不可变 Agent 运行事件、关联标识和事件发布 SPI。事件 SHALL 覆盖请求、多提问子任务、图节点、子 Agent、模型路由/兜底、工具、SQL 插件、压缩、长期记忆、检查点、恢复、降级和失败。

#### Scenario: Correlate an execution across agents and instances
- **WHEN** 一个 AskData 请求经过多个子问题、Agent、SQL 插件或集群实例恢复
- **THEN** 事件 SHALL 携带项目、请求、子问题、会话、图节点、Agent、实例和父事件关联标识，使业务监控能够重建执行链路

#### Scenario: Observe a model failover
- **WHEN** 主模型发生可切换故障并使用候选或兜底模型
- **THEN** 系统 SHALL 发布主模型、故障分类、候选顺序、最终模型 key 和结果状态，且不得包含原始 Prompt 或凭据

#### Scenario: Keep sensitive data out of default events
- **WHEN** 默认事件发布器记录模型、SQL、长期记忆或工具调用
- **THEN** 原始问题、Prompt、SQL 结果、参数值、记忆正文和凭据 SHALL 不被默认写入；诊断摘要 SHALL 经显式配置和脱敏处理

### Requirement: Mandatory per-conversation monitoring logs
系统 SHALL 为每个 AskData 对话写入监控日志，不得因未启用外部可观测平台而省略。日志事件 SHALL 至少覆盖对话创建、请求/多提问子任务、模型路由与兜底、Agent/图节点、SQL 组装/校验/执行、长期记忆召回、检查点/恢复、完成、取消和失败，并绑定 `projectId`、已认证 `operatorId` 与 `conversationId`。

#### Scenario: Write logs throughout a conversation
- **WHEN** 已认证操作人在项目中创建或继续 AskData 对话
- **THEN** 系统 SHALL 为该对话写入可按 `projectId + operatorId + conversationId` 关联的生命周期监控日志，且每个子问题、模型尝试和 Agent 节点能够关联回同一对话

#### Scenario: Use default in-memory monitoring logs on a single node
- **WHEN** 单机应用未配置监控日志存储能力
- **THEN** Starter SHALL 使用默认内存环形日志存储，并说明容量上限和进程重启后日志丢失的行为

#### Scenario: Replace monitoring log storage
- **WHEN** 业务应用选择 JDBC、Redis、消息系统或自定义监控日志存储能力
- **THEN** Starter SHALL 通过能力注册表写入该实现，且不要求更改 Agent、SQL 或会话实现

#### Scenario: Do not block conversation on monitoring failure
- **WHEN** 外部日志存储或导出器超时、不可用或写入失败
- **THEN** 系统 SHALL 尝试保留脱敏本地诊断并继续对话处理；外部监控失败不得改变模型、SQL、检查点或对话成功状态

### Requirement: Administrator-only monitoring log access
系统 SHALL 通过可替换的监控日志访问授权 SPI 控制日志查询。项目管理员 SHALL 能查询其授权项目中的全部对话监控日志；平台管理员跨项目查询 SHALL 需要业务显式授权；普通操作人默认 SHALL 无法查询监控日志。

#### Scenario: A project administrator inspects project conversations
- **WHEN** 项目管理员查询其已授权项目的监控日志
- **THEN** 系统 SHALL 返回该项目内各操作人的脱敏对话、请求、Agent、模型、SQL 和恢复链路，并保持项目边界过滤

#### Scenario: A platform administrator requests cross-project logs
- **WHEN** 平台管理员查询多个项目的监控日志
- **THEN** 系统 SHALL 仅在业务授权 SPI 明确允许的项目范围内返回记录，且审计该查询操作

#### Scenario: An ordinary operator requests monitoring logs
- **WHEN** 非管理员操作人请求监控日志查询
- **THEN** 系统 SHALL 拒绝该查询；业务若需暴露个人历史，必须通过独立对话历史接口实现，不得复用管理员监控日志接口

### Requirement: Optional observability exporters
系统 SHALL 允许通过版本化能力插件异步装配 LangSmith、Langfuse、OpenTelemetry 或业务自定义导出器，且导出失败不得影响 AskData 请求、SQL 安全状态或检查点状态。

#### Scenario: Export to LangSmith when explicitly enabled
- **WHEN** 应用引入兼容 LangSmith 导出器并显式启用该能力
- **THEN** 导出器 SHALL 将请求、图节点、子 Agent、模型 token 和成本元数据映射为可关联的父子 run 或 trace，并遵守脱敏配置

#### Scenario: Export to Langfuse when explicitly enabled
- **WHEN** 应用引入兼容 Langfuse 导出器并显式启用该能力
- **THEN** 导出器 SHALL 异步批量导出 trace、span 和 generation 事件，并保留项目、请求和子问题关联标识

#### Scenario: Isolate exporter failures
- **WHEN** 任一可选观测导出器超时、不可用或返回错误
- **THEN** 系统 SHALL 仅产生本地诊断事件或指标，且不得阻塞问数、SQL 执行、恢复或向其他导出器发布

