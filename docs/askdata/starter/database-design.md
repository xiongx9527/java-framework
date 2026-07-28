# AskData 逻辑数据库设计

本文定义持久化实现应采用的逻辑模型，不代表 Starter 已自动创建或管理这些表。所有业务表建议包含 `id`、`project_id`、`created_at`、`updated_at`、`created_by`、`version` 和逻辑删除/归档字段；涉及个人上下文的表还必须包含 `operator_id`。敏感字段使用密钥引用、密文或脱敏摘要，禁止明文保存数据库密码、API Key、会话令牌和完整敏感结果集。

| 逻辑表 | 用途与关键字段 | 分区/索引 | 扩展点 |
| --- | --- | --- | --- |
| `askdata_project` | 项目/工作空间边界；`project_id`、`name`、`status`、`owner_id`、`policy_version` | `project_id` 主键；名称唯一索引；可按创建月分区 | 项目配额、数据驻留区、品牌与默认模型策略。 |
| `askdata_project_member` | 成员、角色和授权关系；`project_id`、`operator_id`、`role_code`、`data_scope_ref`、`status` | `(project_id, operator_id)` 唯一；按项目查询索引 | 接入企业组织、细粒度对象/行/列权限。 |
| `askdata_datasource` | 数据源逻辑配置；`datasource_key`、`type`、`dialect`、`secret_ref`、`metadata_version`、`status` | `(project_id, datasource_key)` 唯一；类型/状态索引 | 连接器能力、只读策略、健康检查、密钥轮换。 |
| `askdata_metadata` | 表、列、类型、描述、样例摘要与权限标签；`datasource_key`、`object_type`、`object_name`、`column_name`、`metadata_version` | `(project_id, datasource_key, object_name, column_name)` 索引；按数据源分区 | 预览数据、约束、值画像、元数据同步任务。 |
| `askdata_mdl` | MDL 语义模型、指标、关系、视图与 manifest；`mdl_key`、`semantic_version`、`manifest_json`、`deploy_status` | `(project_id, mdl_key, semantic_version)` 唯一；生效版本索引 | 嵌套列、缓存视图、部署日志、dry-plan。 |
| `askdata_training_asset` | Prompt、术语、SQL pair、文档、指令和向量引用；`asset_type`、`content_ref`、`embedding_ref`、`semantic_version` | `(project_id, datasource_key, asset_type)` 索引；按资产类型分区 | 批量导入、评测集、生命周期/过期和向量重建。 |
| `askdata_conversation` | 会话主记录；`conversation_id`、`operator_id`、`datasource_key`、`semantic_version`、`status` | `(project_id, operator_id, updated_at)` 索引；按时间分区 | 会话摘要、归档、嵌入式来源、保留策略。 |
| `askdata_memory` | 长期记忆、确认结果与偏好；`operator_id`、`memory_type`、`content_ref`、`embedding_ref`、`expires_at` | `(project_id, operator_id, datasource_key, semantic_version)` 索引；过期索引 | 遗忘、授权撤销清理、向量召回、反馈权重。 |
| `askdata_recommendation` | 推荐候选、排序依据和反馈；`operator_id`、`recommendation_type`、`payload_ref`、`score`、`feedback` | `(project_id, operator_id, recommendation_type, status)` 索引 | 算法版本、曝光/点击、过滤原因与离线重算。 |
| `askdata_sql_audit` | SQL 插件、校验、执行和结果摘要；`request_id`、`conversation_id`、`datasource_key`、`plugin_chain`、`decision`、`error_code`、`row_count` | `(project_id, request_id)` 唯一；按时间和数据源索引/分区 | 加密 SQL 文本引用、审计导出、合规保留与审批。 |
| `askdata_monitoring_log` | 生命周期监控日志；`event_type`、`request_id`、`conversation_id`、`agent_node`、`model_key`、`duration_ms`、`sanitized_detail` | `(project_id, operator_id, conversation_id, occurred_at)` 索引；按时间分区 | Langfuse/LangSmith/OTel 异步导出、采样与冷热分层。 |
| `askdata_agent_state` | Agent 图、节点、检查点、租约与幂等状态；`request_id`、`node_id`、`state_version`、`lease_owner`、`idempotency_key`、`resume_ref` | `(project_id, request_id, node_id)` 唯一；租约到期索引 | 图编排、消息队列任务、恢复策略和跨实例抢占。 |
| `askdata_model_config` | 模型能力、路由策略和密钥引用；`model_key`、`provider`、`capability_type`、`route_scope`、`secret_ref`、`enabled` | `(project_id, model_key)` 唯一；默认/启用状态索引 | 候选链、配额、熔断、远程运行时协议版本。 |
| `askdata_mcp_api` | MCP、REST、RPC、嵌入令牌和调用方注册；`api_key`、`transport`、`schema_version`、`credential_ref`、`scope` | `(project_id, api_key)` 唯一；调用方/状态索引 | SSE、HTTP、gRPC、A2A、限流、DataEase/第三方映射。 |
| `askdata_enterprise_integration` | 企业身份、组织同步、SSO 和平台配置；`integration_type`、`tenant_ref`、`config_secret_ref`、`sync_state` | `(project_id, integration_type, tenant_ref)` 唯一 | 企业微信、钉钉、飞书、扫码/免登、组织映射与配置审计。 |
| `askdata_plugin_registry` | 能力插件注册与版本协商；`plugin_key`、`capability_type`、`contract_version`、`priority`、`config_namespace`、`status` | `(project_id, plugin_key, contract_version)` 唯一；能力类型索引 | 依赖声明、灰度、禁用、远程能力健康与冲突诊断。 |

## 约束与保留策略

- 项目隔离是所有查询的首个条件；个人记忆、推荐、会话、监控和审计还必须带 `operator_id`。
- 数据源密码、模型密钥和企业密钥仅保存外部密钥服务引用；展示层仅可见脱敏标识和轮换状态。
- SQL 审计和监控日志按合规要求分区、归档与清理；删除项目或撤销成员权限时，异步清理其记忆、推荐、token 和可识别日志。
- `askdata_agent_state` 的租约、版本和幂等键用于集群恢复。SQL 执行中状态不确定的记录只能人工/业务授权处置，不能标为可重放。
- Schema 演进使用显式迁移版本，MDL、检查点和 API schema 均需保留兼容版本或拒绝不兼容读取。

## 当前实现与持久化落地边界

当前默认 Starter 提供内存会话、检查点、Agent 状态、记忆、推荐、监控日志和路由诊断；上表是 JDBC、Redis、向量库和企业服务接入时的落地蓝图。落地持久化实现时须先完成存储 SPI 契约、项目/操作人隔离、租约、加密、备份恢复、容量压测和迁移回滚验证。
