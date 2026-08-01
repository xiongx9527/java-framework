## Why

`rework-ai-askdata-core-starter` 已经交付 AskData Core/Starter 的 SPI、安全管线和默认单机实现，但真实 SQLBot/WrenAI 产品能力仍依赖业务治理层把项目成员、数据源、表、列、行级权限和审计归属接入到默认问数流程。现在需要先补齐这层后端桥接，否则 API/MCP、训练检索和外部系统联调只能停留在契约或文档覆盖，无法证明授权数据边界在运行时生效。

## What Changes

- 新增 AskData 治理权限桥接能力，提供项目、成员、数据源授权、对象授权、行列权限摘要和管理员查询授权的 Java 8 兼容 Core SPI。
- 在 Starter 自动配置中装配默认拒绝/空实现，并允许业务通过 `@ConditionalOnMissingBean` 覆盖治理权限、元数据过滤和监控日志访问策略。
- 将旧问数流程适配器、元数据提供、SQL 拦截/执行和监控日志查询接入项目与操作人授权摘要，避免仅传递 `projectId + operatorId` 但不执行业务权限。
- 为跨项目、跨操作人、未授权数据源、未授权表/列、行级谓词注入、管理员日志查询和权限撤销补充定向测试。
- 更新 AskData Starter 文档和 SQLBot/WrenAI 矩阵，把治理权限相关条目从“契约覆盖/业务适配覆盖”推进到“默认拒绝 + 业务桥接可验证”状态；外部企业 IAM 真实连通仍不纳入本 change。

## Capabilities

### New Capabilities
- `askdata-governance-permission-bridge`: 覆盖 AskData 项目成员、数据源/对象授权、行列权限摘要、管理员监控日志授权和治理层业务适配桥接。

### Modified Capabilities
- `askdata-core-contracts`: 增加治理权限桥接 SPI、默认拒绝语义和业务覆盖约束，并要求 Core/Starter 对外流程不能只传递上下文而跳过授权判定。
- `askdata-query-engine`: 增加强制使用授权对象和行列权限摘要的 SQL 生成/插件/执行边界，确保最终 SQL 校验前后的权限信息不可被插件清空或扩大。
- `askdata-agent-observability-contracts`: 增加管理员监控日志查询授权和权限拒绝事件记录要求，区分普通操作人、本项目管理员和平台管理员。

## Impact

- 影响 `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata` 中的安全、元数据、SQL、观测和上下文 SPI。
- 影响 `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure` 中 AskData 自动配置、旧流程适配、元数据提供、SQL 拦截链和监控日志访问策略。
- 影响 `docs/askdata/starter`、`openspec/specs/askdata-*` 和归档清单派生的功能矩阵。
- 不引入 Java 9+ API，不把 Spring/JustAuth/业务 IAM 类型放入 Core 公共接口，不实现企业微信、钉钉、飞书、DataEase、Dify、Coze、n8n 或控制台 UI 的真实外部联调。
