# 配置参考

AskData Starter 使用 `fw.plus.ai.askdata` 前缀。配置只选择逻辑能力 key、执行预算和部署模式，不替代认证、对象/行列授权、Provider SDK、密钥托管、数据源或共享存储。模型、Embedding 和候选列表没有厂商默认值；业务不配置时应用可以启动，但首次需要该能力的请求会返回明确的未配置错误。

## 治理 Bean 配置原则

治理没有“生产全局放行”属性。未注册业务 `AskDataGovernanceProvider` 时，Starter 使用默认拒绝实现；生产环境必须显式注册业务治理 Bean，将宿主 IAM 的 `projectId + operatorId`、成员/角色、数据源、表列与数据域映射为 `AskDataAuthorizationSummary`、`AskDataAuthorizedObject` 和参数化 `AskDataRowPredicate`。

管理员日志查询也复用该 SPI。业务实现必须识别数据源 `askdata-monitoring-log` 和用途 `MONITORING_LOG_QUERY`，授权摘要须绑定请求主体、管理范围、授权版本和来源。普通操作人、归属不明或摘要不完整的请求默认拒绝。

## 基础与模型

| 属性 | 默认值 | 合法范围与用途 | 失败表现与覆盖方式 |
| --- | --- | --- | --- |
| `enabled` | `true` | `true/false`；控制 AskData 自动配置 | `false` 时不装配默认 Bean；业务可直接提供自己的 Core `AskDataService` |
| `model.provider` | 无 | 可选诊断标签，不参与模型/Adapter 选择 | 不会据此创建 Provider；业务应通过 `fw-plus.ai.model` 和 Adapter 定义真实能力 |
| `model.default-chat-model` | 无 | 非空中立 Chat key | 请求未显式给 key 且无默认值时模型解析失败；业务可配置 key 或提供 `AskDataModelRegistry` |
| `model.default-embedding-model` | 无 | 非空中立 Embedding key | 需要向量召回而无 key 时明确失败/空召回边界；业务可配置 key 或覆盖向量检索器 |
| `model.chat-candidates` | `[]` | 有序、去重的 Chat key 列表 | 未知 key 在解析/调用时失败；业务通过 `AiModelRegistry` 注册同名模型 |
| `model.embedding-candidates` | `[]` | 有序、去重的 Embedding key 列表 | 类型不匹配或未知 key 失败；不得把 Chat 模型当 Embedding 使用 |
| `model.fallback-enabled` | `true` | 仅控制可重试基础设施故障的候选切换 | 鉴权、策略、输入、预算和副作用错误不切换；可用业务 Router 覆盖策略 |
| `model.request-timeout-millis` | `30000` | `<=0` 交由 Adapter；正数为单次调用预算 | 超时按中立错误分类；Provider HTTP 连接/读取超时仍须单独配置 |
| `model.infrastructure-failure-timeout-millis` | `30000` | 正整数；旧兼容层故障分类阈值 | 不是网络客户端超时；业务 Adapter 可提供更准确错误分类 |
| `model.route-audit-record-capacity` | `100` | 正整数；单机诊断快照容量 | 非正数回落安全容量；完整审计应使用监控 Store |

模型路由优先级固定为请求 key、项目 key、数据源 key、Agent 任务 key、策略 key、显式 fallback 列表、默认 key；策略标签交给可替换 Router 判定，但不能携带凭据。同一 key 一次请求内不重复尝试，只有无副作用模型节点的可重试基础设施故障才进入下一候选。AskData Starter 不创建 LangChain4j、Ollama、OpenAI 或其他 Provider，也不读取 API Key。

## Flow、SQL 与会话

| 属性 | 默认值 | 合法范围与用途 | 失败表现与覆盖方式 |
| --- | --- | --- | --- |
| `flow.default-sub-agent-timeout-millis` | `30000` | 正整数毫秒 | 超时返回独立受控子任务终态；业务 Agent 可自行收紧 |
| `flow.multi-question-max-concurrency` | `4` | 大于 0 | 非正数绑定失败；可按实例容量调整 |
| `flow.multi-question-timeout-millis` | `30000` | 大于 0，覆盖整个 DAG | 超时节点标记 `TIMED_OUT/CANCELLED`，已完成结果保留 |
| `flow.graph-runtime` | `sequential` | `sequential` 或已注册 `RUNTIME_ADAPTER` key | 未知/版本不兼容 key 启动失败；业务可提供 `AskDataAgentGraphRuntime` Bean |
| `sql.execute-enabled` | `false` | 仅在只读数据源和治理就绪后开启 | `false` 时只生成/校验，不执行；业务可覆盖 `AskDataSqlExecutor` |
| `sql.default-max-rows` | `100` | 正整数物理行数上限 | 无效值使用安全默认值；业务可按数据源收紧，不能由插件扩大 |
| `sql.query-timeout-seconds` | `30` | `<=0` 不主动设置，生产建议正数 | 驱动超时返回稳定 SQL 错误；连接池仍须设置自身超时 |
| `sql.read-only-connection-required` | `true` | `true/false` | `true` 且连接非只读时执行失败；关闭只适合受控演示或已有外部只读强制 |
| `sql.enforce-limit` | `true` | `true/false` | 开启时无物理限行的 SQL 被拒绝/改写；生产不建议关闭 |
| `conversation.compression-threshold-chars` | `12000` | 正整数字符阈值 | 超阈值触发所选压缩器；业务可覆盖 Boot `AskDataContextCompressor` |
| `conversation.checkpoint-version` | `1` | 非空版本字符串 | 恢复版本不一致时 fail-closed；升级须配套迁移或拒绝旧 token |
| `conversation.max-checkpoints-per-conversation` | `100` | 正整数 | 超限淘汰最早检查点及 token；共享实现可自定义保留策略 |
| `conversation.compressor` | `deterministic` | `deterministic` 或已注册 `RUNTIME_ADAPTER` key | 未知 key 启动失败；语义压缩失败自动回落确定性压缩 |

默认压缩器不调用模型，只保存有界摘要和恢复安全字段。LangChain4j 语义压缩是业务可选 Adapter：业务创建 `LangChain4jAskDataContextCompressor("langchain4j-summary", chatModel)` 并把它作为 `AskDataCapabilityProvider` 注册后，才可设置 `conversation.compressor=langchain4j-summary`。

## 分类状态、向量与观测

| 属性 | 默认值 | 合法范围与用途 | 失败表现与覆盖方式 |
| --- | --- | --- | --- |
| `state.cluster-mode` | `false` | `true/false` | `true` 时任何仍选择本地 `memory` 的必需状态/向量能力都会启动失败 |
| `state.store` | `memory` | 所有分类 Store 的兼容回退 key | 非 `memory` 且分类项仍为默认时继承该 key；未知 key 启动失败 |
| `state.conversation-store` | `memory` | 会话 Store key | 可选 `jdbc`、`redis` 或业务 STORE key/Bean |
| `state.checkpoint-store` | `memory` | 检查点 Store key | 共享实现须保持版本、作用域和恢复 token 一致性 |
| `state.agent-state-store` | `memory` | Agent 图/节点/计划/工具/模型尝试 Store key | 集群不能使用本地实现 |
| `state.lease-store` | `memory` | 恢复租约 Store key | 集群共享实现必须具备 TTL 和原子获取/释放语义 |
| `state.idempotency-store` | `memory` | 父子请求幂等 Store key | 子问题使用 `<parentKey>:<questionId>`；冲突不能重复副作用 |
| `state.audit-store` | `memory` | 运行终态审计 Store key | sequence 必须单调递增；失败不能伪造成功 |
| `state.monitoring-store` | `memory` | 强制生命周期日志 Store key | 外部 Store 失败回落本地脱敏诊断，不改变业务结果 |
| `state.long-term-memory-store` | `memory` | 长期记忆 Store key；内置 `jdbc`/`redis` 复用共享客户端 | 选择 `jdbc`/`redis` 时必须存在且只能存在一个对应共享 client Bean；未知 key、缺失或重复 client 启动失败；业务也可直接提供 `AskDataLongTermMemory` Bean |
| `state.monitoring-log-capacity` | `1000` | 正整数、每个项目/操作人作用域 | 超限淘汰最早日志；不是合规审计持久化方案 |
| `vector.store` | `memory` | Core `VECTOR` 能力 key | 集群选择 `memory` 启动失败；未知 key 或类型错误明确失败 |
| `vector.default-top-k` | `5` | 大于 0 | 应按语料、上下文和延迟压测；授权 filters 无法落实时拒绝检索 |
| `observability.enabled` | `false` | `true/false` | 开启后解析 `exporters`；没有 Transport/Provider 时未知 key 启动失败 |
| `observability.exporters` | `[]` | `langsmith`、`langfuse`、`otel`/`opentelemetry` 或业务 exporter key | 重复/未知/版本不兼容 key 启动失败；运行时单个导出失败被隔离 |

## 内置能力 key 与前置条件

| key | 类型 | 用途与默认 | 前置条件 | 失败表现 |
| --- | --- | --- | --- | --- |
| `memory` | `VECTOR` | 单机内存向量写入/召回；`vector.store` 默认 | `cluster-mode=false` | 集群启动拒绝；重启数据丢失 |
| `memory` | 分类默认 Store | 会话、检查点、Agent、租约、幂等、审计、监控、长期记忆 | `cluster-mode=false` | 集群启动拒绝；各分类可独立覆盖 |
| `jdbc` | `STORE` | 由一个 `AskDataJdbcStorageClient` 组合会话、检查点、Agent、租约、幂等、审计、监控与长期记忆 | 容器中恰好一个 client Bean；业务负责 schema、事务、序列化与凭据 | 0 个或多个 client、未知 key、版本不兼容均启动失败；客户端不满足原子 `putIfAbsent/compareAndSet` 契约时运行失败 |
| `redis` | `STORE` | 由一个 `AskDataRedisStorageClient` 组合热状态、租约、幂等及长期记忆等分类 Store | 容器中恰好一个 client Bean；业务负责 Lua/WATCH 原子性、TTL、序列化与凭据 | 0 个或多个 client、未知 key、版本不兼容均启动失败；非原子 GET/SET 实现不符合契约 |
| `sequential` | 图运行时默认 | Core 顺序图执行，不依赖外部图库 | 无 | 仅提供进程内执行；集群恢复仍依赖共享 Store |
| 业务 graph key | `RUNTIME_ADAPTER` | `LangGraph4jAskDataGraphAdapter` 或业务图运行时 | 注册唯一 Provider 且 `isAvailable=true` | 缺失、重复、不可用或版本不兼容启动/执行失败 |
| `deterministic` | 压缩默认 | 无模型确定性摘要 | 无 | 不产生语义摘要，但保留恢复安全字段 |
| 业务 compressor key | `RUNTIME_ADAPTER` | LangChain4j 或其他语义压缩 | 唯一 Provider、可用 Chat 模型 | 语义调用失败时发布降级事件并回落确定性压缩 |
| `langsmith`、`langfuse`、`opentelemetry` | `OBSERVABILITY_EXPORTER` | 映射事件为 trace/span/generation 后交给中立 Transport | 一个业务 `AskDataObservationTransport` Bean | 未提供 Transport 时能力不存在；发送失败仅写脱敏降级诊断 |

## SPI 覆盖速查

| SPI | 用途 | 最小覆盖示例 | 必须保持的边界 |
| --- | --- | --- | --- |
| `AskDataGovernanceProvider` | 项目成员、对象、行列授权 | `@Bean AskDataGovernanceProvider governance()` | 摘要绑定项目、操作人、数据源、用途、版本和来源；默认拒绝 |
| `AskDataChatModel` / `AskDataEmbeddingModel` | 直接提供中立模型 | `@Bean AskDataChatModel businessChat()` | 不泄漏 SDK 类型/凭据，返回实际模型 key 和稳定错误 |
| `AskDataVectorStore` / `AskDataVectorRetriever` | 向量写入、授权召回 | `@Bean AskDataVectorStore businessVector()` | 必须落实授权 filters；不能静默忽略 |
| 分类 Store SPI | 会话、检查点、Agent、租约、幂等、审计、监控 | 分别声明 `@Bean AskDataCheckpointStore ...` | `projectId + operatorId` 隔离、并发版本和集群原子性 |
| `AskDataAgent` / `AskDataAgentGraphRuntime` | 稳定步骤或图执行替换 | `@Bean AskDataAgent sqlGenerator()` | 只能收窄授权；SQL/工具副作用不自动重放 |
| `AskDataSqlInterceptor` / `AskDataSqlValidator` | SQL 阶段插件和最终边界 | `@Bean AskDataSqlInterceptor tenantScope()` | 具名参数、确定顺序、治理与最终安全复检 |
| `AskDataEventPublisher` / `AskDataObservationTransport` | 本地日志外的事件消费与厂商传输 | `@Bean AskDataObservationTransport otlpGateway()` | 只处理脱敏记录，任何失败不得影响问数 |

直接提供某个 SPI Bean 时 `@ConditionalOnMissingBean` 默认实现让位；按 key 选择时应注册 `AskDataCapabilityProvider`。同一 key 重复、类型错误、契约版本不兼容或配置命名空间冲突均在启动期失败，不按 Bean 顺序猜测。

## 单机最小配置

```yaml
fw:
  plus:
    ai:
      askdata:
        state:
          cluster-mode: false
        sql:
          execute-enabled: false
```

未配置模型 key 时只完成上下文启动验证；真正问数前必须定义业务模型。默认内存会话、检查点、Agent 状态、记忆、推荐、监控和向量内容在进程重启后丢失。

## 集群分类组合

```yaml
fw:
  plus:
    ai:
      askdata:
        state:
          cluster-mode: true
          conversation-store: jdbc
          checkpoint-store: jdbc
          agent-state-store: jdbc
          lease-store: redis
          idempotency-store: redis
          audit-store: jdbc
          monitoring-store: jdbc
          long-term-memory-store: jdbc
        vector:
          store: business-vector
```

该配置还要求业务提供唯一 JDBC/Redis client Bean 和 `business-vector` 能力，并完成迁移、备份、租约 TTL、幂等、双节点恢复、容量、脱敏与故障切换验证。`long-term-memory-store=jdbc` 会调用同一个共享客户端完成 `remember/find/forget` 和遗忘审计；改为 `redis` 时语义相同。Starter 不自动创建表、Redis keyspace、连接、客户端或凭据，缺少对应 client Bean 会在启动期失败。
