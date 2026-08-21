## Context

`rework-ai-askdata-core-starter` 已将 AskData 拆出 Core 契约、SQL 安全管线、监控日志、长期记忆、推荐和默认 LangChain4j Starter。当前缺口不是自然语言问数本身，而是生产业务治理层如何把成员、数据源、对象授权、行列权限和管理员日志访问接入默认流程。现有 `AskDataPermissionEvaluator` 只有通用 `capability/resource` 判定，旧 `CoreAskDataServiceAdapter` 也明确说明默认适配不应作为跨项目授权依据，因此需要增加更具体、可测试、默认拒绝的治理桥接。

本 change 先补后端安全底座，不接入控制台 UI、企业 IAM 真凭据或外部系统真实租户。API/MCP、训练检索、DataEase/Dify/Coze/n8n 和企业微信/钉钉/飞书可以在本桥接之上单独推进。

## Goals / Non-Goals

**Goals:**

- 在 Core 中定义项目成员、数据源访问、表/列授权、行级谓词、管理员日志查询和权限撤销相关的 Java 8 SPI 与领域对象。
- Starter 提供默认拒绝或空授权摘要实现，使未接入治理层时不会误开放业务数据。
- 默认问数流程在元数据加载、Prompt 构建、SQL 插件链、最终 SQL 校验、SQL 执行和监控日志查询中消费同一授权摘要。
- 保持 `projectId + operatorId` 为强制边界，并把授权失败写入脱敏监控事件和可诊断错误。
- 用单元/自动配置测试覆盖跨项目、跨人、无权限、行列过滤、插件恶意扩大授权对象和管理员日志查询。

**Non-Goals:**

- 不实现企业微信、钉钉、飞书、DataEase、Dify、Coze、n8n、MCP Server 或 Vue3 控制台真实联调。
- 不引入业务 IAM、JustAuth、Spring Security、Sa-Token、数据库迁移工具或 Java 17 运行时到 Core 公共 API。
- 不把所有 SQLBot/WrenAI API、训练资产、MDL 语义层或仪表板能力合并到本 change。

## Decisions

1. **新增强类型治理 SPI，而不是扩展字符串式 `AskDataPermissionEvaluator`。**
   现有接口适合最小能力判定，但无法表达数据源、表、列、行级谓词、授权版本和管理员查询范围。新增 `AskDataGovernanceProvider`、`AskDataAuthorizationSummary`、`AskDataAuthorizedObject` 等 Core 类型，旧接口保留兼容，并可由默认桥接适配到新摘要。

2. **默认实现采用拒绝优先。**
   对真实数据源、对象授权、管理员日志查询和跨项目访问，未接入业务治理层时默认拒绝；仅允许明确标记的开发空元数据或测试桩返回空结果。这样比“默认允许再等待业务覆盖”更符合 SQLBot/WrenAI 的企业权限语义。

3. **授权摘要贯穿流程而非临时回调。**
   Flow 在元数据加载前解析并校验绑定 project/operator/dataSource/purpose/scope 的授权摘要，Prompt 只能看到净化后的表列元数据；无法判定对象归属的术语、样例和 Prompt 片段默认清空。SQL 插件链收到授权摘要快照，收窄后的摘要传给后续插件；最终 SQL 校验再次基于该摘要检查表、列和行级条件。

4. **行级谓词由 Starter 确定性注入，最终命令负责参数。**
   受控谓词使用 JSqlParser 注入匹配表的 `SELECT`，与已有 `WHERE` 以括号化 `AND` 合并。请求 option 与谓词参数冲突时 fail-closed；执行器只消费最终 `AskDataSqlCommand` 中最终 SQL 实际引用的具名参数。列受限时无法解析或无法安全归属的 SQL 同样 fail-closed。

5. **管理员监控日志查询独立于个人历史。**
   管理员资格和项目范围由同一个 Core `AskDataGovernanceProvider` 以 `MONITORING_LOG_QUERY` 用途返回，`AskDataMonitoringLogAccessPolicy` 再按日志真实归属收窄。项目管理员可查本项目所有操作人，平台管理员只能查显式授权项目，普通用户拒绝；审计记录主体、范围、结果数量和授权来源。

6. **文档矩阵只提升治理相关状态。**
   本 change 只把工作空间/项目、成员/权限、数据源授权、对象/行列权限和管理员监控日志推进到可验证后端桥接。外部系统联调仍保持“业务集成契约覆盖”或后续 change 状态。

## Risks / Trade-offs

- [Risk] 强类型治理 SPI 会增加 Core 类型数量。→ Mitigation: 仅建模授权摘要、对象引用、行级谓词和查询范围，不把宿主 IAM 用户表或框架类型带入 Core。
- [Risk] 默认拒绝可能让旧样例在未配置治理桩时返回无权限。→ Mitigation: 文档提供开发模式测试 Bean 示例，并在错误码中明确缺少治理授权实现。
- [Risk] SQL 插件和元数据过滤可能重复校验。→ Mitigation: 摘要快照作为单一事实源，Prompt 阶段过滤是减少暴露面，最终 SQL 校验是强制安全边界。
- [Risk] 外部系统团队可能误以为本 change 已完成企业微信/DataEase 真实联调。→ Mitigation: proposal、docs 和矩阵明确本 change 不接触外部真实租户。

## Migration Plan

1. 新增 Core 治理 SPI 和默认拒绝实现，不移除现有 `AskDataPermissionEvaluator`。
2. Starter 自动配置优先使用业务提供的治理 Bean；缺省时返回拒绝摘要和明确诊断。
3. 默认 Flow、SQL 管线和监控日志查询接入授权摘要，并补充回归测试。
4. 更新文档、矩阵和示例，说明如何从业务 IAM/项目成员系统映射到 AskData 治理桥接。
5. 若业务应用暂未接入治理层，可显式注册开发用测试 Bean；生产配置不得通过属性打开全局放行。

## Open Questions

- 业务项目成员和数据源清单最终来自哪个宿主模块，目前只能通过 SPI 接入，不在本 change 固定。
