# 配置参考

配置前缀为 `fw.plus.ai.askdata`。未指定的属性使用下表默认值。配置决定默认适配层行为，不替代宿主应用的认证、数据源权限、密钥托管和共享存储治理。

## 治理 Bean 配置原则

治理没有“生产全局放行”属性。未注册业务 `AskDataGovernanceProvider` 时，Starter 以默认拒绝实现返回 `ASKDATA_GOVERNANCE_DENIED`；生产环境必须显式注册业务治理 Bean，将宿主 IAM 的 `projectId + operatorId`、成员/角色、数据源、表列与数据域映射为 `AskDataAuthorizationSummary`、`AskDataAuthorizedObject` 和 `AskDataRowPredicate`。

开发和测试环境也应显式注册只覆盖测试数据源、表、列和参数化行级范围的 Bean，而不是通过 YAML 打开全局访问。例如测试 Bean 可仅授权 `demo.orders` 的 `id`、`amount`，并返回 `region_id = :regionId` 与 `AskDataSqlParameter.of("regionId", "test-region")`。这类 Bean 不能进入生产配置。

管理员监控日志读取需要同时注册 `AskDataMonitoringLogAccessAuthorizer` 与 `AskDataMonitoringLogAccessPolicy`。默认授权器恒拒绝，默认策略只允许已获宿主确认的同项目管理员读取；业务授权器应核验请求人、目标 scope 和角色声明，业务策略可在此基础上进一步收窄。成功查询将写入脱敏审计，普通操作人和未被授权的跨项目管理员均不能依靠属性或请求参数取得日志。

## 基础、模型与流程

| 属性 | 默认值 | 说明与限制 |
| --- | --- | --- |
| `enabled` | `true` | 启用 AskData 默认装配；业务自定义 Bean 仍优先。 |
| `model.provider` | `ollama` | 路由审计中的提供方诊断标签，不决定 Core 路由，也不注入厂商 SDK。 |
| `model.default-chat-model` | `qwen3:32b` | 默认聊天逻辑 key，应存在于已注册 LangChain4j 模型中。 |
| `model.default-embedding-model` | `bge-m3` | 默认向量逻辑 key，与聊天模型独立注册。 |
| `model.chat-candidates` | `[qwen3:32b]` | 聊天候选顺序；上层将其与请求、项目、数据源、任务策略合成为路由请求。 |
| `model.embedding-candidates` | `[bge-m3]` | 向量候选顺序。 |
| `model.fallback-enabled` | `true` | 只允许基础设施类可重试故障选择后继候选。 |
| `model.infrastructure-failure-timeout-millis` | `30000` | 适配层识别超时类故障的阈值；HTTP 客户端还须独立设置连接和读取超时。 |
| `model.route-audit-record-capacity` | `100` | 单机内存路由诊断快照上限，不是完整审计存储。 |
| `flow.default-sub-agent-timeout-millis` | `30000` | 默认子任务时间预算；超时后由流程返回受控结果。 |

模型路由固定优先级为：请求显式 key、项目 key、数据源 key、智能体任务 key、默认 key。相同 key 不会在一次基础设施失败后重复命中；策略拒绝、参数错误和预算错误不会触发降级。

## SQL、会话与恢复

| 属性 | 默认值 | 说明与安全要求 |
| --- | --- | --- |
| `sql.execute-enabled` | `false` | 默认禁止执行 SQL。开启前必须提供只读数据源、对象授权和审计保留策略。 |
| `sql.default-max-rows` | `100` | JDBC 默认物理返回行数上限；业务可按数据源收紧。 |
| `sql.query-timeout-seconds` | `30` | JDBC `PreparedStatement` 查询超时。小于等于 0 时不主动设置，生产环境不建议关闭。 |
| `sql.read-only-connection-required` | `true` | Starter 自动装配的 JDBC 执行器要求连接 `isReadOnly=true`；业务若关闭该检查，必须用只读账号和外部网关策略兜底。 |
| `sql.enforce-limit` | `true` | 最终校验器强制物理限行；关闭前应进行风险评审。 |
| `conversation.compression-threshold-chars` | `12000` | 达到阈值时由默认上下文压缩器处理。压缩结果不得越过项目和操作人边界。 |
| `conversation.checkpoint-version` | `1` | 检查点结构版本；变更时应拒绝不兼容恢复或执行迁移。 |
| `conversation.max-checkpoints-per-conversation` | `100` | 单会话内存检查点上限，超出后淘汰最早检查点及恢复令牌。 |

恢复仅可从已完成且可重入的节点开始。SQL 执行中断或状态不确定时不自动重放；权限、语义版本、SQL 校验状态或租约不兼容时必须拒绝恢复并返回可诊断错误。

## 状态、监控与向量

| 属性 | 默认值 | 单机/集群说明 |
| --- | --- | --- |
| `state.cluster-mode` | `false` | `true` 时禁止使用默认内存状态。 |
| `state.store` | `memory` | 仅单机临时状态；集群启用且仍为 `memory` 时启动期拒绝。JDBC、Redis、业务共享实现必须通过 SPI 提供。 |
| `state.monitoring-log-capacity` | `1000` | 每个项目与操作人范围内默认内存监控日志上限；仅保存脱敏摘要。 |
| `vector.default-top-k` | `5` | 默认召回数量，应结合语料规模、模型上下文与延迟压测调整。 |
| `vector.store` | `memory` | 仅单机开发或临时检索；重启即丢失，不得在集群中静默使用。 |

强制监控日志覆盖会话、请求、问题规划、模型路由/降级、Agent 节点、SQL 插件/校验/执行、检查点/恢复及完成/失败。默认访问策略只允许宿主系统已授权的同项目管理员查询；还必须由默认拒绝的管理员授权器确认管理员资格。跨项目查询必须由业务授权器和访问策略同时显式授予。

## 单机示例

```yaml
fw:
  plus:
    ai:
      askdata:
        state:
          cluster-mode: false
          store: memory
          monitoring-log-capacity: 1000
        vector:
          store: memory
          default-top-k: 5
```

该模式适用于本地开发、演示和单实例临时运行。进程重启会使默认会话、检查点、Agent 状态、记忆、推荐和向量内容丢失。

## 集群接入要求

```yaml
fw:
  plus:
    ai:
      askdata:
        state:
          cluster-mode: true
          store: jdbc
        vector:
          store: business-vector
```

上述键表达部署意图，当前 Starter 不将 JDBC、Redis 或向量服务伪装为已自动装配的实现。业务必须注册对应共享存储 SPI，并完成租约、幂等键、恢复版本、容量、备份与故障切换验证；否则应保持单机模式而非绕过启动期保护。

## 运维建议

- 使用逻辑模型 key 管理密钥和厂商差异，日志仅记录 key、路由层级、稳定错误码和可重试标识。
- 监控日志、SQL 审计和模型供应商诊断日志分层保存，禁止记录提示词全文、SQL 参数、会话令牌、裸连接或 API Key。
- 将 `default-max-rows`、`query-timeout-seconds` 和 `top-k` 作为容量预算的一部分，根据真实查询和模型延迟压测调整。
- 启用 SQL 执行前，用最小权限只读账号进行端到端校验，确认只读连接检查、最终校验、限行、超时和审计均已生效。
