## ADDED Requirements

### Requirement: Complete vendor-neutral lifecycle event emission
系统 SHALL 在 `fw-plus-ai-askdata` 中定义不依赖 Spring、AI 框架或观测厂商 SDK 的不可变运行事件、关联标识和事件发布 SPI。事件 SHALL 覆盖请求、多问题计划与子任务、模型路由与每次尝试、Agent/图节点、工具、SQL 插件/校验/执行、压缩、长期记忆、检查点、恢复、取消、降级、完成和失败。

#### Scenario: Correlate the complete execution
- **WHEN** 一个请求经过多个子问题、模型尝试、Agent、SQL 插件或跨实例恢复
- **THEN** 事件 SHALL 携带项目、操作人、请求、子问题、会话、图节点、Agent、模型、实例和父事件关联标识，使监控能够重建执行链路

#### Scenario: Observe model attempts
- **WHEN** Flow 选择主模型、切换候选或耗尽候选
- **THEN** 系统 SHALL 为每次尝试发布模型 key、路由层级、故障分类、切换原因和最终状态，且不得包含原始 Prompt 或凭据

#### Scenario: Observe all stable flow stages
- **WHEN** 问题计划、Agent、SQL、记忆、压缩或检查点阶段开始、完成、失败或取消
- **THEN** 系统 SHALL 发布对应脱敏事件，且每个事件能够关联到同一会话和子问题

### Requirement: Non-blocking mandatory monitoring delivery
系统 SHALL 为每个 AskData 对话写入脱敏监控日志，并提供固定容量的默认本地环形存储。业务可替换 JDBC、Redis、消息或其他外部存储，但任何监控写入或导出失败不得改变模型、SQL、检查点或对话结果。

#### Scenario: Write logs throughout a conversation
- **WHEN** 已认证操作人创建、继续或取消 AskData 对话
- **THEN** 系统 SHALL 写入可按 `projectId + operatorId + conversationId` 关联的完整生命周期日志，每个子问题、模型尝试和 Agent 节点均可回溯

#### Scenario: Fall back after external store failure
- **WHEN** 外部监控存储超时、不可用或写入失败
- **THEN** 发布器 SHALL 尝试把脱敏诊断写入本地环形日志并继续业务处理，外部失败不得向 Flow 抛出

#### Scenario: Isolate local fallback failure
- **WHEN** 本地环形日志也无法写入
- **THEN** 系统 SHALL 只产生受控内部诊断并继续业务处理，不得修改对话成功、SQL 状态或检查点状态

#### Scenario: Replace monitoring storage through the capability registry
- **WHEN** 业务配置外部监控存储能力 key
- **THEN** Starter SHALL 通过唯一 Core 能力注册表选择该实现，未知、重复或版本不兼容 SHALL 在启动期失败

### Requirement: Independently isolated observability exporters
系统 SHALL 允许通过版本化能力插件异步装配观测导出器。基础日志存储和每个外部导出器 SHALL 分别隔离失败，一个发布器的失败不得阻塞其他发布器或 AskData 业务流程。

#### Scenario: Isolate every exporter
- **WHEN** 任一导出器超时、不可用或返回错误
- **THEN** 系统 SHALL 记录脱敏本地诊断并继续向其他导出器发布，且不得阻塞问数、SQL 执行或恢复
