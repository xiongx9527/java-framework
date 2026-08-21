# AskData 逻辑数据库设计

本文定义业务持久化实现应采用的逻辑模型，不代表 Starter 会自动建表。所有主键建议使用不含业务语义的 `id`，并带 `created_at`、`updated_at`、`created_by`、`version` 与归档标记；下表只列领域关键字段。所有租户查询先约束 `project_id`，个人状态再约束 `operator_id`。密码、API Key、token、连接串和敏感正文只存密钥/对象引用或密文，列表与日志仅展示脱敏摘要。

迁移版本以 `AD-001` 起步；业务落地时可用 Flyway/Liquibase 映射为本地版本，但必须保留下表的隔离、唯一性和并发语义。JDBC 状态 Adapter 不负责自动迁移。

| 逻辑表 | 主键与关键字段 | 索引与隔离 | 脱敏与保留 | 单机/集群与迁移 |
| --- | --- | --- | --- | --- |
| `askdata_project` | PK `project_id`；`name,status,owner_id,policy_version,residency_region` | UQ `(name)` 或宿主租户内名称；IDX `(owner_id,status)`；隔离根为 `project_id` | 名称按业务敏感级别展示；注销后先冻结，配置审计按合规期保留 | 单机/集群均放权威关系库；`AD-001`，策略版本使用乐观锁 |
| `askdata_project_member` | PK `id`；`project_id,operator_id,role_code,data_scope_ref,status,authorization_version` | UQ `(project_id,operator_id)`；IDX `(project_id,role_code,status)` | 不保存外部身份凭据；成员撤销后删除 token/个人推荐，关系审计按合规期保留 | 集群读取同一 IAM 同步结果；`AD-001`，变更递增授权版本 |
| `askdata_datasource` | PK `id`；`project_id,datasource_key,type,dialect,secret_ref,metadata_version,status` | UQ `(project_id,datasource_key)`；IDX `(project_id,type,status)` | 仅存 `secret_ref` 和脱敏端点；健康日志短期保留，删除时吊销密钥 | 集群共享注册表和连接健康状态；`AD-001`，凭据轮换不改逻辑 key |
| `askdata_metadata` | PK `id`；`project_id,datasource_key,object_type,schema_name,table_name,column_name,data_type,description_ref,metadata_version,permission_tag` | UQ `(project_id,datasource_key,metadata_version,schema_name,table_name,column_name)`；IDX `(project_id,datasource_key,metadata_version)` | 样例值只存脱敏摘要/对象引用；历史版本按恢复与审计窗口保留 | 大规模可按项目/数据源分区；`AD-002`，发布新版本后异步回收旧画像 |
| `askdata_mdl` | PK `id`；`project_id,mdl_key,semantic_version,manifest_ref,manifest_hash,deploy_status,deployed_at` | UQ `(project_id,mdl_key,semantic_version)`；IDX `(project_id,mdl_key,deploy_status,deployed_at)` | manifest 中凭据必须外置；失败日志脱敏，至少保留可回滚版本 | 集群使用同一生效指针；`AD-002`，deploy 以版本 CAS 切换 |
| `askdata_training_asset` | PK `id`；`project_id,datasource_key,asset_type,content_ref,embedding_ref,semantic_version,authorization_version,status,expires_at` | IDX `(project_id,datasource_key,asset_type,status)`、`(project_id,expires_at)` | Prompt/SQL pair/文档正文外置加密；撤权、过期或遗忘请求触发正文和向量清理 | 向量写入需幂等；`AD-002`，embedding 模型变化生成新版本而非原地覆盖 |
| `askdata_conversation` | PK `conversation_id`；`project_id,operator_id,datasource_key,authorization_version,semantic_version,status,summary_ref,last_checkpoint_version` | IDX `(project_id,operator_id,updated_at)`、`(project_id,status)`；所有读写带项目和操作人 | 原始消息/摘要外置加密；按业务会话保留期归档，撤权后禁止恢复 | 单机可内存，集群必须共享；`AD-003`，版本不兼容时迁移或拒绝恢复 |
| `askdata_memory` | PK `id`；`project_id,operator_id,datasource_key,memory_type,content_ref,sql_summary_ref,embedding_ref,source,confidence,retention_policy,expires_at,authorization_version` | IDX `(project_id,operator_id,datasource_key,memory_type,expires_at)` | 正文/SQL 仅存加密引用；支持按 ID、作用域、过期和撤权遗忘并写删除审计 | 集群共享或外部向量库；`AD-003`，删除操作幂等且同步清理 embedding |
| `askdata_recommendation` | PK `id`；`project_id,operator_id,datasource_key,recommendation_type,payload_ref,source,relevance_score,preference_score,feedback_score,memory_score,status,authorization_version` | IDX `(project_id,operator_id,datasource_key,status)`、`(project_id,updated_at)` | payload 外置/脱敏；曝光反馈按产品期限保留，个人画像撤销后删除 | 集群排序使用同一授权版本；`AD-003`，通用候选个人分值必须为空 |
| `askdata_sql_audit` | PK `id`；`project_id,operator_id,request_id,conversation_id,datasource_key,authorization_version,plugin_chain,decision,error_code,sql_ref,parameter_names,row_count,duration_ms` | UQ `(project_id,request_id,id)`；IDX `(project_id,datasource_key,created_at)`、`(project_id,conversation_id)` | SQL 可加密外置；不存参数值、凭据和原始结果；按合规期 WORM/归档 | 集群集中写入或消息汇聚；`AD-004`，审计失败不得伪造执行成功 |
| `askdata_monitoring_log` | PK `id`；`project_id,operator_id,event_id,parent_event_id,request_id,sub_question_id,conversation_id,graph_node_id,agent_key,model_key,instance_id,event_type,duration_ms,sanitized_detail,authorization_version` | UQ `(project_id,event_id)`；IDX `(project_id,operator_id,conversation_id,occurred_at)`、`(project_id,request_id)` | 仅脱敏字段；热日志短期、冷归档按运维策略，管理员查询另写审计 | 单机可环形内存，集群必须共享；`AD-004`，按月分区并支持重放到 exporter |
| `askdata_agent_state` | PK `id`；`project_id,operator_id,request_id,conversation_id,node_id,state_version,status,plan_ref,tool_summary_ref,model_attempts_ref,resume_ref,lease_owner,lease_expires_at,idempotency_key` | UQ `(project_id,operator_id,request_id,node_id,state_version)`；IDX `(project_id,lease_expires_at)`、UQ `(project_id,idempotency_key)` | 只存恢复所需摘要，工具参数/模型正文不落表；终态后按恢复窗口清理 | 单机可内存；集群要求 CAS、TTL 和共享租约；`AD-004`，SQL 不确定状态禁止标可重放 |
| `askdata_model_config` | PK `id`；`project_id,model_key,framework,provider,capability_type,route_scope,secret_ref,properties_ref,enabled,config_version` | UQ `(project_id,model_key,config_version)`；IDX `(project_id,capability_type,enabled)` | API Key 只存 `secret_ref`，properties 加密且日志不展开 | 集群共享配置并原子发布；`AD-005`，无默认 Provider，回滚按完整版本 |
| `askdata_mcp_api` | PK `id`；`project_id,api_key,transport,schema_version,credential_ref,caller_ref,scope,status,expires_at` | UQ `(project_id,api_key)`；IDX `(project_id,caller_ref,status)` | token/appSecret/AES-Key 只存引用或哈希；过期和轮换后立即失效 | 集群需共享限流与 token 状态；`AD-005`，协议版本不兼容 fail-closed |
| `askdata_enterprise_integration` | PK `id`；`project_id,integration_type,tenant_ref,config_secret_ref,sync_cursor_ref,sync_state,last_synced_at` | UQ `(project_id,integration_type,tenant_ref)`；IDX `(project_id,sync_state)` | AppSecret、回调 token 等只存密钥引用；配置变更永久审计，失败 payload 脱敏 | 由业务 IAM/JustAuth 集群实现；`AD-005`，AskData 只消费同步后的身份授权 |
| `askdata_operation_log` | PK `id`；`project_id,operator_id,operation_type,target_type,target_key,request_id,result,change_summary,authorization_source,occurred_at` | IDX `(project_id,operator_id,occurred_at)`、`(project_id,target_type,target_key)` | 不保存前后完整密钥/配置，只存字段级脱敏变更摘要；按合规期归档 | 集群统一写入且防篡改；`AD-006`，与运行监控和 SQL 审计分表 |
| `askdata_plugin_registry` | PK `id`；`project_id,plugin_key,capability_type,contract_version,min_core_version,max_core_version,priority,config_namespace,implementation,status` | UQ `(project_id,plugin_key,capability_type,contract_version)`；IDX `(project_id,capability_type,status,priority)` | 配置命名空间不含配置值；实现异常只记录脱敏类名/key | 运行时内存注册表可由发布配置生成；`AD-006`，重复 key 或版本冲突启动失败 |

## 外部向量与 Redis 键模型

- 向量文档必须携带 `projectId`、`operatorId` 或业务共享标记、`datasourceKey`、`authorizationVersion`、语义版本和资产 ID。后端不能应用这些 filters 时拒绝查询。
- Redis key 建议使用 `askdata:{projectId}:{operatorId}:{category}:{id}`；租约和幂等写入必须使用原子 compare-and-set/TTL，不以本地锁替代。
- JDBC `AskDataSharedStoreAdapter` 需要业务实现 `AskDataJdbcStorageClient` 的原子读改写；Redis 对应实现需要业务保证脚本/事务语义。两者都不得把序列化对象中的凭据或正文写入诊断日志。

## 保留、删除与恢复

1. 项目注销先冻结请求，再吊销密钥和接口 token，最后按保留策略清理会话、记忆、推荐、向量与可识别日志。
2. 成员撤销立即阻止新请求和恢复；个人记忆、推荐与 token 异步删除，删除动作写 `askdata_operation_log`。
3. SQL 审计、操作日志和合规监控按法规期限归档；普通路由诊断和性能日志采用更短期限，不混用保留策略。
4. 备份恢复后先核对 schema、检查点、授权和语义版本，再开放流量。SQL 已发送但终态不明的状态只能由业务审批处置。
5. 每次迁移必须提供前向脚本、回滚或恢复方案、双节点读写验证、脱敏扫描和容量基线；不能仅依靠 ORM 自动更新生产 schema。

## 当前落地边界

当前 Starter 提供单机内存实现，以及把业务提供的 JDBC/Redis client 组合为分类 Store 的 Adapter；不会创建上述表、连接池、Redis、向量集合、密钥或迁移任务。生产落地必须由业务模块实现 schema、客户端、备份恢复、容量、数据驻留和合规审批，并通过 [测试用例](test-cases.md) 中的存储与恢复场景。
