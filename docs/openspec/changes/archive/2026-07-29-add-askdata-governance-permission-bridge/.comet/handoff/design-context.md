# Comet Design Handoff

- Change: add-askdata-governance-permission-bridge
- Phase: design
- Mode: compact
- Context hash: 5141e916eb61ebd7e3fc69ceaee88eff6b1af0bb43cdb782c628ce9053a598cf

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/add-askdata-governance-permission-bridge/proposal.md

- Source: openspec/changes/add-askdata-governance-permission-bridge/proposal.md
- Lines: 1-28
- SHA256: 4eca6a2b6e7b9b7ab370e2549c55bd4cae6d386f9c6742cd35b272578958624c

```md
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

```

## openspec/changes/add-askdata-governance-permission-bridge/design.md

- Source: openspec/changes/add-askdata-governance-permission-bridge/design.md
- Lines: 1-58
- SHA256: e971b9fde28ccfe939dc53860c76d5944c730a3e021adf2ed81cdedbbafec15a

```md
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
   Flow 在元数据加载前解析授权摘要，Prompt 只能看到过滤后的元数据；SQL 插件链只能收到授权摘要快照，插件可以收窄但不能扩大授权对象；最终 SQL 校验再次基于同一摘要检查表/列/系统 schema 和行级条件。

4. **管理员监控日志查询独立于个人历史。**
   普通操作人的对话历史以后可以由 API/MCP 或业务 UI 暴露，但管理员监控日志必须经过 `AskDataMonitoringLogAccessPolicy`/治理桥接授权，平台管理员跨项目查询必须由业务显式放行并审计。

5. **文档矩阵只提升治理相关状态。**
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
- 行级谓词采用 SQL 片段还是结构化表达式需要在实现时结合现有 `AskDataSqlCommand` 能力收敛；原则是插件不得拼接动态值，必须使用具名参数。

```

## openspec/changes/add-askdata-governance-permission-bridge/tasks.md

- Source: openspec/changes/add-askdata-governance-permission-bridge/tasks.md
- Lines: 1-36
- SHA256: fdfc4a0fc200da355d72b4b15eead08cfcde5a4d641485819b75d3704bf22605

```md
## 1. Core governance contracts

- [ ] 1.1 在 `fw-plus-ai-askdata` 新增治理授权桥接 SPI、授权摘要、授权对象、列级授权、行级谓词、管理员查询范围和权限拒绝错误模型，保持 Java 8 与无 Spring 依赖。
- [ ] 1.2 保留并兼容现有 `AskDataPermissionEvaluator`，实现旧接口到粗粒度能力判定的适配说明，避免把旧接口误认为完整行列权限来源。
- [ ] 1.3 为治理 SPI、默认拒绝语义、跨项目/跨操作人拒绝和授权摘要不可扩大规则补充 Core 单元测试。

## 2. Starter autoconfiguration bridge

- [ ] 2.1 在 AskData Starter 自动配置中装配默认拒绝治理桥接、业务 `@ConditionalOnMissingBean` 覆盖入口和开发测试桩示例。
- [ ] 2.2 将 `CoreAskDataServiceAdapter`、默认 Flow、元数据提供器和请求上下文接入治理授权摘要，未授权时在模型调用前返回受控失败。
- [ ] 2.3 为自动配置覆盖、默认拒绝、业务 Bean 优先和旧服务适配边界补充 Spring Boot 测试。

## 3. SQL authorization pipeline

- [ ] 3.1 将授权摘要传入元数据过滤、Prompt 构建、SQL 插件链和最终 SQL 校验，确保插件只能收窄不能扩大授权对象或行级条件。
- [ ] 3.2 使用受控 SQL 片段和具名参数表达行级谓词，不允许业务插件通过字符串拼接动态值。
- [ ] 3.3 覆盖未授权数据源、未授权表/列、插件清空授权对象、插件移除行级谓词、跨项目 resume/token 复用和最终 SQL 重校验测试。

## 4. Observability and admin access

- [ ] 4.1 将治理拒绝、授权版本、授权来源和 SQL 权限失败写入脱敏监控事件，并与 SQL 审计使用同一请求/会话关联标识。
- [ ] 4.2 将管理员监控日志查询接入治理授权桥接，区分本项目管理员、平台管理员允许范围和普通操作人拒绝。
- [ ] 4.3 覆盖项目管理员查询、平台管理员跨项目受限查询、普通操作人拒绝和查询审计测试。

## 5. Documentation and matrix updates

- [ ] 5.1 更新 `docs/askdata/starter` 中治理权限、SQL 安全、配置、存储和外部系统边界文档，说明业务 IAM 如何映射到 Core 治理桥接。
- [ ] 5.2 更新 SQLBot/WrenAI 功能矩阵中工作空间/成员/权限/数据源/对象授权/管理员日志相关状态，不宣称企业微信、钉钉、飞书、DataEase、Dify、Coze、n8n 已真实联通。
- [ ] 5.3 补充可编译业务治理桥接示例或测试用例，展示数据源授权、列过滤和行级谓词参数绑定。

## 6. Verification

- [ ] 6.1 运行 `openspec validate add-askdata-governance-permission-bridge --strict` 并修复所有 OpenSpec 问题。
- [ ] 6.2 运行 Core AskData 相关 Maven 测试，确认 Java 8 Core 契约不引入框架类型。
- [ ] 6.3 运行 AI AskData 自动配置定向测试，确认默认拒绝、业务覆盖、SQL 权限和管理员日志查询行为通过。
- [ ] 6.4 运行文档/矩阵状态扫描和 `git diff --check`，记录验证证据。

```

## openspec/changes/add-askdata-governance-permission-bridge/specs/askdata-agent-observability-contracts/spec.md

- Source: openspec/changes/add-askdata-governance-permission-bridge/specs/askdata-agent-observability-contracts/spec.md
- Lines: 1-16
- SHA256: c6a3e52047ba8749208e8e27f0b056f9ec62b659422764b2812538c8ed6f1536

```md
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

```

## openspec/changes/add-askdata-governance-permission-bridge/specs/askdata-core-contracts/spec.md

- Source: openspec/changes/add-askdata-governance-permission-bridge/specs/askdata-core-contracts/spec.md
- Lines: 1-16
- SHA256: 06d4b266d2590746419bf6f58e933d27aeae4ff65e217bebd5f6102b3d5f1b3e

```md
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

```

## openspec/changes/add-askdata-governance-permission-bridge/specs/askdata-governance-permission-bridge/spec.md

- Source: openspec/changes/add-askdata-governance-permission-bridge/specs/askdata-governance-permission-bridge/spec.md
- Lines: 1-46
- SHA256: 00a3310a35508259e1cb66bdc738d32a367b6b41866c0174a2d5f3ac7a3f78ca

```md
## ADDED Requirements

### Requirement: Governance authorization bridge
系统 SHALL 提供 Java 8 兼容、无 Spring 依赖的 AskData 治理授权桥接 SPI，用于解析当前 `projectId + operatorId` 在项目、数据源、表、列、行级谓词和管理员日志查询上的授权摘要。未提供业务治理实现时，生产默认实现 SHALL 拒绝真实数据访问和管理员监控日志查询。

#### Scenario: Resolve authorization before metadata exposure
- **WHEN** AskData 处理带有 `projectId + operatorId` 的问数请求
- **THEN** 系统 SHALL 在加载 Prompt 元数据前解析该操作人在当前项目和数据源范围内的授权摘要，并且只暴露授权表、授权列和允许的行级约束

#### Scenario: Reject missing governance implementation for protected data
- **WHEN** 应用未注册业务治理授权实现且请求访问真实数据源
- **THEN** Starter SHALL 返回明确的权限配置错误或拒绝结果，不得默认允许读取项目数据、元数据或监控日志

#### Scenario: Keep host IAM types outside Core
- **WHEN** 业务 IAM、JustAuth、Sa-Token、Spring Security 或宿主项目成员系统接入 AskData
- **THEN** 它们 SHALL 通过 Core 治理 SPI 映射为稳定授权摘要，Core 公共 API 不得暴露宿主框架类型或外部平台凭据

### Requirement: Project member and datasource authorization
系统 SHALL 支持业务应用按项目成员、角色、数据源和资源范围授权 AskData 请求。授权摘要 SHALL 包含项目、操作人、数据源、授权对象、授权版本和可审计来源。

#### Scenario: Allow an authorized datasource
- **WHEN** 当前操作人在项目中被授权访问某个数据源
- **THEN** AskData SHALL 允许后续元数据加载和 SQL 生成继续，并在审计摘要中记录授权来源和授权版本

#### Scenario: Reject an unauthorized datasource
- **WHEN** 当前操作人请求未授权数据源或已被撤销权限的数据源
- **THEN** AskData SHALL 在模型调用、SQL 生成或 SQL 执行前拒绝请求，并写入脱敏权限拒绝监控事件

#### Scenario: Prevent cross-project reuse
- **WHEN** 操作人持有一个项目中的数据源、会话、token 或授权摘要并尝试用于另一个项目
- **THEN** 系统 SHALL 拒绝访问，即使数据源 key、会话 ID 或外部用户 ID 相同

### Requirement: Administrator monitoring access bridge
系统 SHALL 使用治理授权桥接控制管理员监控日志查询。本项目管理员只能查询其授权项目；平台管理员跨项目查询必须由业务治理实现显式返回允许范围并记录审计。

#### Scenario: Project admin queries project logs
- **WHEN** 项目管理员查询本项目 AskData 监控日志
- **THEN** 系统 SHALL 返回该项目内脱敏日志，并保持项目过滤、授权来源和查询审计

#### Scenario: Platform admin queries allowed projects
- **WHEN** 平台管理员发起跨项目监控日志查询
- **THEN** 系统 SHALL 仅返回治理授权摘要中明确允许的项目范围，并记录该跨项目查询事件

#### Scenario: Ordinary operator is denied
- **WHEN** 普通操作人调用管理员监控日志查询能力
- **THEN** 系统 SHALL 拒绝请求，不得把管理员日志接口作为个人历史接口复用

```

## openspec/changes/add-askdata-governance-permission-bridge/specs/askdata-query-engine/spec.md

- Source: openspec/changes/add-askdata-governance-permission-bridge/specs/askdata-query-engine/spec.md
- Lines: 1-20
- SHA256: 5630ed9376064db4fb0a1fabff44dd0444263eb837b70d191fed5b66ac509eb3

```md
## ADDED Requirements

### Requirement: Authorization-aware metadata and SQL pipeline
系统 SHALL 在元数据加载、Prompt 构建、SQL 插件链、最终 SQL 校验和只读执行阶段使用同一治理授权摘要。插件 MAY 收窄 SQL 文本或授权对象，但 MUST NOT 清空、扩大、替换项目/操作人上下文、参数、授权对象或行列权限摘要。

#### Scenario: Filter metadata by authorized objects
- **WHEN** AskData 为当前请求构建 Prompt 元数据
- **THEN** 元数据提供器 SHALL 只返回授权摘要允许的数据源、表、列、业务描述和样例信息，不得向模型暴露未授权对象

#### Scenario: Preserve authorization through SQL plugins
- **WHEN** SQL 插件链改写生成 SQL
- **THEN** 插件结果 SHALL 保留原始项目上下文、参数和授权摘要；任何扩大授权对象、移除行级谓词或替换操作人边界的结果 SHALL 被拒绝

#### Scenario: Revalidate SQL against authorization summary
- **WHEN** 所有 SQL 插件完成后准备执行最终 SQL
- **THEN** SQL 校验器 SHALL 基于授权摘要重新检查引用表、列、系统 schema、行级谓词和只读限制，未授权引用 SHALL 导致安全校验失败

#### Scenario: Inject row predicates with parameters
- **WHEN** 业务治理桥接提供行级数据范围条件
- **THEN** SQL 管线 SHALL 使用受控 SQL 片段和具名参数表达该条件，不得要求业务通过字符串拼接动态值

```
