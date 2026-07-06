# Comet Design Handoff

- Change: fix-magic-ws-console-query
- Phase: design
- Mode: compact
- Context hash: 12617592dab8d06a893e41f86e396e9dace1247f3786394c18923ff7002c2eaa

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/fix-magic-ws-console-query/proposal.md

- Source: openspec/changes/fix-magic-ws-console-query/proposal.md
- Lines: 1-30
- SHA256: 0ae46022e643025102f93c3a6b3e0ac079ac785f6146e5f0790f7287f18f7138

```md
## Why

Magic WebSocket 插件的运行时主链路已经具备标准化基础，但控制台查询、脚本测试和代码提示仍存在遗留实现：管理端测试接口固定返回成功，前端 JavaClass 指向旧包名和不存在的 Producer 类型，脚本上下文变量也不完全一致。现在需要补齐这些能力，避免控制台显示“可测试/可提示”但实际无效，保证 WebSocket 插件从运行时到 MagicAPI 管理端都符合既有标准。

## What Changes

- 修复 `ws-subscribe` 控制台执行测试，使 `/ws/execute` 能按脚本 id 加载订阅脚本并使用模拟上下文执行。
- 修复 `ws-datasource` 控制台测试，使 `/ws/datasource/test` 至少执行真实字段校验、JSON 校验和 token 生成能力校验。
- 修复控制台代码提示配置，使用真实 `WsModule` 类名与模块方法，移除旧 `com.jugui` 包名、`WsProducer` 和 `extra=producer` 遗留逻辑。
- 统一 `ws-subscribe` 前后端资源标准，确保路径要求与后端 storage 一致。
- 统一脚本上下文变量兼容性，在业务、init、close 场景中提供一致的 `type` 与 `businessType` 访问方式。
- 补充关键单元测试，覆盖控制台测试接口、上下文变量兼容与前端配置静态风险。

## Capabilities

### New Capabilities

- 无。

### Modified Capabilities

- `standard-websocket-plugin`: 补强 MagicAPI WebSocket 插件的控制台测试、代码提示、资源元数据一致性和脚本上下文标准。

## Impact

- 后端：`fw-plus-boot-autoconfigure` 中 `magicapi/plugin/ws` 的控制器、处理器或相关服务。
- 前端：`fw-plus-magicapi-plugin-ws/src/console` 中数据源与订阅资源插件配置。
- 测试：WS 插件相关 JUnit 单元测试与必要的静态配置校验。
- API：不引入破坏性连接协议变更；`type`/`businessType` 以兼容方式补齐。
- 依赖：不新增外部依赖，不修改 MagicAPI、Spring Boot 或 JWT 版本。
```

## openspec/changes/fix-magic-ws-console-query/design.md

- Source: openspec/changes/fix-magic-ws-console-query/design.md
- Lines: 1-65
- SHA256: 3aa31ca414798d7c5c5125daab66ab9ba38e30acdbcaa83a232005c97459efea

```md
## Context

`standard-websocket-plugin` 已覆盖 WebSocket 握手校验、控制帧、业务消息、生命周期脚本和广播能力。当前缺口集中在 MagicAPI 控制台配套能力：`/ws/execute` 和 `/ws/datasource/test` 固定返回成功，导致管理端测试没有真实语义；前端代码提示仍引用旧包名和不存在的 Producer 类型；`ws-subscribe` 的前端路径要求与后端 storage 不一致；脚本上下文中 `init`、业务消息、`close` 的变量命名不统一。

本次变更跨后端控制器、运行时上下文和前端插件配置，但不改变客户端正式 WebSocket 连接协议。

## Goals / Non-Goals

**Goals:**

- 让 MagicAPI 控制台的 WS 数据源测试和订阅脚本测试具备真实校验/执行语义。
- 让 WS 前端插件的 JavaClass 扩展、资源元数据和后端模型保持一致。
- 让 `ws-subscribe` 脚本在 `init`、业务消息、`close` 场景中获得兼容且一致的上下文变量。
- 补充关键测试，避免固定返回 `ok` 或旧包名残留再次出现。

**Non-Goals:**

- 不重构 WebSocket session 管理、Redis 广播或 JWT 签名策略。
- 不建立真实 WebSocket 连接来测试控制台脚本运行；控制台测试使用模拟上下文。
- 不修改 MagicAPI 资源文件格式的已有必填字段。
- 不新增外部依赖或升级 MagicAPI/Spring Boot/JWT 版本。

## Decisions

### Decision 1: `/ws/execute` 使用模拟上下文执行脚本

控制台测试应按脚本 `id` 从 `WsSubscribeInfoMagicResourceStorage` 读取订阅脚本，构造 `MagicScriptContext` 并执行。上下文使用可预测的模拟值，例如 `appId` 来自订阅 `key`，`type`/`businessType` 来自 `businessId`，`clientId`/`sessionId` 使用测试标识，`params` 使用请求体传入的可选参数。

备选方案是发起真实 WebSocket 连接并触发 handler。该方案更接近端到端，但需要 token、session、网络握手和异步关闭处理，控制台单次脚本测试成本过高，也容易误伤真实客户端。因此本次选择模拟上下文。

### Decision 2: `/ws/datasource/test` 复用后端校验并验证 token 生成能力

数据源测试不能只返回 `ok`。它应执行与保存一致的必填字段和 `other` JSON 校验，并验证该 `key + secret` 能通过 `WsModule` 或同等逻辑生成 token。这样可以发现缺少全局 token secret、数据源 secret 无效、JSON 配置错误等实际运行问题。

备选方案是只调用 storage `validate`。这能覆盖字段规则，但不能覆盖 token secret 配置，因此不足以代表 WebSocket 接入可用性。

### Decision 3: 前端代码提示只暴露真实 `WsModule` 能力

前端 `magic-ws-datasource.js` 应改为真实类 `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.module.WsModule`，并移除不存在的 `WsProducer` 类型和 `extra === 'producer'` 过滤。代码提示应围绕 `ws['key']` 动态模块和 `broadcastMessageToClient`、`broadcastDistributedMessageToAllClient`、`generateToken` 等真实方法表达。

备选方案是新增 Producer 抽象来适配旧前端。该方案扩大范围且当前后端没有生产者模型，不符合最小修复原则。

### Decision 4: 以兼容方式统一脚本上下文变量

所有订阅脚本执行场景都提供 `type` 和 `businessType`。业务消息继续保留 `params`，生命周期脚本可不强制提供消息参数。这样既符合现有 spec 中业务消息要求的 `type`，也兼容已有可能使用 `businessType` 的 init 脚本。

备选方案是统一只保留 `type`。该方案更简洁，但可能破坏已有脚本。

## Risks / Trade-offs

- 控制台模拟执行不能覆盖真实 session 行为 → 通过文档和测试明确其用途是脚本逻辑测试，不替代端到端连接验证。
- 复用 storage 校验可能依赖 MagicResourceService 状态 → 对 controller 层添加单元测试，必要时拆出可测试的校验辅助方法。
- 前端 JavaClass API 受 MagicAPI 编辑器约束 → 保持最小改动，只修复类名、过滤条件和暴露类型，不重写编辑器插件机制。
- 同时提供 `type` 和 `businessType` 会增加上下文字段冗余 → 这是兼容性成本，可在文档中说明二者等价。

## Migration Plan

1. 后端先补齐 `/ws/execute` 与 `/ws/datasource/test` 的真实行为和测试。
2. 再统一 handler 中脚本上下文变量，保持向后兼容。
3. 最后修复前端插件配置和 README 中控制台测试说明。
4. 回滚时可分别回退控制器、handler、前端配置；不涉及数据迁移。

## Open Questions

无阻塞问题。默认采用模拟上下文执行控制台脚本测试，不建立真实 WebSocket session。
```

## openspec/changes/fix-magic-ws-console-query/tasks.md

- Source: openspec/changes/fix-magic-ws-console-query/tasks.md
- Lines: 1-25
- SHA256: febd62eda64f227f426b31a48d53d431de00326d6596f0a67d76432aaa623a6f

```md
## 1. 后端控制台接口

- [ ] 1.1 为 `/ws/execute` 增加按订阅脚本 id 加载、模拟上下文执行和错误返回逻辑
- [ ] 1.2 为 `/ws/datasource/test` 增加必填字段、`other` JSON 和 token 生成能力校验
- [ ] 1.3 补充 `MagicWsController` 单元测试覆盖成功、未知脚本、非法数据源和缺少 token secret 场景

## 2. 脚本上下文兼容

- [ ] 2.1 统一 `init` 脚本上下文，同时注入 `type` 与 `businessType`
- [ ] 2.2 统一业务消息脚本上下文，同时注入 `type` 与 `businessType`，保留 `params`
- [ ] 2.3 统一 `close` 脚本上下文，同时注入 `type` 与 `businessType`
- [ ] 2.4 补充或更新 `BaseDynamicWebSocketHandler` 测试验证上下文变量兼容

## 3. 前端控制台插件

- [ ] 3.1 修复 `magic-ws-datasource.js` 中旧 `com.jugui` 包名和不存在的 `WsProducer` 引用
- [ ] 3.2 调整 WS 数据源代码提示逻辑，使其围绕真实 `WsModule` 与 `ws['key']` 动态模块能力
- [ ] 3.3 统一 `magic-ws-subscribe.js` 的 `requirePath` 与后端 `WsSubscribeInfoMagicResourceStorage` 规则
- [ ] 3.4 增加静态校验或测试，防止旧包名、Producer 残留和路径规则不一致回归

## 4. 文档与验证

- [ ] 4.1 更新 WS 插件 README，说明控制台测试语义、模拟上下文和 `type/businessType` 兼容变量
- [ ] 4.2 运行 OpenSpec 校验，确认 delta spec 格式有效
- [ ] 4.3 运行 WS 插件相关 Maven 测试，确认变更未破坏现有标准 WebSocket 行为
```

## openspec/changes/fix-magic-ws-console-query/specs/standard-websocket-plugin/spec.md

- Source: openspec/changes/fix-magic-ws-console-query/specs/standard-websocket-plugin/spec.md
- Lines: 1-45
- SHA256: 89b6aaa7b6968656a2344faff6c501883fe0302b60f2eb776b6249469a223972

```md
## ADDED Requirements

### Requirement: MagicAPI console websocket resource validation
The plugin SHALL make MagicAPI console validation for WebSocket resources reflect real backend behavior instead of returning unconditional success.

#### Scenario: Datasource test validates usable configuration
- **WHEN** a MagicAPI console user tests a `ws-datasource`
- **THEN** the plugin SHALL validate required datasource fields, JSON-formatted optional configuration, and token generation prerequisites before returning success

#### Scenario: Datasource test reports invalid configuration
- **WHEN** a MagicAPI console user tests a `ws-datasource` with missing required fields, invalid JSON configuration, or missing token signing configuration
- **THEN** the plugin SHALL return a failure result instead of reporting `ok`

### Requirement: MagicAPI console websocket subscribe execution
The plugin SHALL allow MagicAPI console users to test enabled `ws-subscribe` scripts by script id with a deterministic simulated WebSocket context.

#### Scenario: Subscribe script test executes selected script
- **WHEN** a MagicAPI console user runs a `ws-subscribe` script test by script id
- **THEN** the plugin SHALL load that script, execute it with appId, clientId, sessionId, type, businessType, and params in context, and return the script result or structured failure

#### Scenario: Subscribe script test rejects unknown script
- **WHEN** a MagicAPI console user runs a `ws-subscribe` script test with an unknown script id
- **THEN** the plugin SHALL return a failure result and SHALL NOT execute unrelated scripts

### Requirement: MagicAPI console websocket editor metadata consistency
The plugin SHALL keep WebSocket console resource metadata and JavaClass hints consistent with backend classes and storage rules.

#### Scenario: WebSocket module class hint uses real backend class
- **WHEN** the MagicAPI editor loads WebSocket plugin code hints
- **THEN** the plugin SHALL reference the real `WsModule` backend class and SHALL NOT reference old package names or nonexistent producer classes

#### Scenario: Subscribe resource path setting matches backend storage
- **WHEN** the MagicAPI editor registers the `ws-subscribe` resource type
- **THEN** its path requirement metadata SHALL match the backend `WsSubscribeInfoMagicResourceStorage` path requirement

### Requirement: WebSocket script context compatibility
The plugin SHALL provide compatible business type variables for all WebSocket subscribe script execution paths.

#### Scenario: Lifecycle script receives compatible type variables
- **WHEN** the plugin executes an `init` or `close` `ws-subscribe` script
- **THEN** the script context SHALL include both `type` and `businessType` with the matched business identifier

#### Scenario: Business message script receives compatible type variables
- **WHEN** the plugin executes a business-message `ws-subscribe` script
- **THEN** the script context SHALL include both `type` and `businessType` with the incoming message type and SHALL include `params` with the parsed JSON message
```

