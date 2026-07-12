# Comet Design Handoff

- Change: standardize-websocket-plugin
- Phase: design
- Mode: compact
- Context hash: 81f404ea5a7398f0869756fc0eb7cd45553ad155935599c48a9065de45b00905

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/standardize-websocket-plugin/proposal.md

- Source: openspec/changes/standardize-websocket-plugin/proposal.md
- Lines: 1-29
- SHA256: 80291ad87d1f0f03bd9a5d275d2e55184696967b25f150c42fe50bbe20ded3dc

```md
## Why

当前 MagicAPI WebSocket 插件已经具备基础连接、脚本订阅和服务端推送能力，但部分行为仍偏业务约定而非 WebSocket 标准语义，例如控制帧、关闭码、握手失败、非法消息、二进制消息和错误反馈边界不够清晰。按标准整改后，可降低客户端对接成本，提高浏览器、网关、负载均衡、集群部署和生产运维场景下的一致性与可诊断性。

## What Changes

- 标准化 WebSocket 握手：在握手阶段完成必要参数、token、appId、Origin 和子协议处理，失败时明确拒绝握手或使用标准关闭语义。
- 标准化控制帧：支持标准 Ping/Pong 控制帧，保留文本 `ping` / `pong` 兼容行为。
- 标准化消息处理：明确文本 JSON 业务消息格式、非法 JSON、缺少 `type`、未知 `type`、脚本异常和非文本消息的处理规则。
- 标准化关闭语义：按场景使用合理 close code，例如正常关闭、协议错误、策略违规、消息类型不支持、服务异常、服务过载、服务重启。
- 补齐功能完整性：连接生命周期、业务消息、服务端点对点推送、全量广播、多 session、Redis 集群广播、并发发送和资源清理均需有明确行为与测试覆盖。
- 更新对接文档：说明标准客户端行为、兼容行为、错误处理、心跳、关闭码和集群注意事项。

## Capabilities

### New Capabilities
- `standard-websocket-plugin`: 规范 MagicAPI WebSocket 插件的握手、消息、控制帧、关闭语义、脚本联动、服务端推送和集群广播行为。

### Modified Capabilities

无。

## Impact

- 代码范围：`fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws`。
- 测试范围：`fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws`。
- 文档范围：`fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-ws/README.md`。
- 对外行为：保留现有 MagicAPI 脚本资源模型和主要 API 名称；对明显不合理的边界行为允许调整为标准 WebSocket 行为。
- 依赖：优先使用 Spring WebSocket 现有能力，不新增重量级协议依赖。
```

## openspec/changes/standardize-websocket-plugin/design.md

- Source: openspec/changes/standardize-websocket-plugin/design.md
- Lines: 1-103
- SHA256: 14dd9a28cd89de528630486e78c4e227860f14f2bdf48c7d65672ed476b553c7

[TRUNCATED]

```md
## Context

WS 插件当前通过 Spring WebSocket `WebSocketHandler` 承载连接，MagicAPI 资源提供 `ws-datasource` 和 `ws-subscribe` 两类动态资源，脚本通过 `ws` 模块进行点对点或广播推送。此前已经修复过 token 密钥、跨域默认值、并发发送、Redis handler 类型、cache replacement、资源重复校验等问题，但要达到“按 WebSocket 标准对接”的目标，还需要把协议层行为和业务脚本层行为分清。

当前约束：

- Java 1.8，Spring Boot 2.7.x，优先保持现有依赖体系。
- 兼容 MagicAPI 现有脚本资源：`ws-datasource`、`ws-subscribe`、`ws` 模块。
- 不引入 STOMP 等上层协议；本次只规范裸 WebSocket 插件。
- 不强制破坏既有文本 `ping` 和 JSON `{type: ...}` 客户端约定，但标准行为优先。

## Goals / Non-Goals

### Goals

- 握手认证和来源校验前置化，失败路径可诊断且符合 Spring WebSocket 语义。
- 支持标准 Ping/Pong 控制帧，文本 `ping` 仅作为兼容路径。
- 对 Text/Binary/Ping/Pong/Close 的行为边界做清晰实现。
- 对非法消息、未知业务类型、脚本异常、容量限制和服务端关闭使用合理 close code 或结构化错误响应。
- 保证多 session、并发发送、Redis 集群广播、资源清理不会因标准化改造退化。
- 补齐关键单元测试和 README 对接说明。

### Non-Goals

- 不实现 STOMP、SockJS、Socket.IO 或 MQTT。
- 不改 MagicAPI 引擎核心。
- 不引入新持久化会话存储。
- 不做客户端 SDK。
- 不要求所有历史非标准客户端立即迁移；合理保留兼容行为。

## Decisions

### 1. 协议层与业务层分离

决定：协议层负责握手、控制帧、close code、消息类型判定和基础错误；业务层只处理合法文本 JSON 业务消息并分发到 `ws-subscribe.businessId`。

备选方案：继续在业务消息处理里混合处理 token、ping、JSON、关闭逻辑。该方案改动小，但标准语义不清，难以测试和维护。

理由：WebSocket 标准行为应先于业务脚本执行，避免脚本承担协议职责。

### 2. 标准 Ping/Pong 优先，文本 ping 兼容

决定：实现 `handleMessage` 对 `PingMessage` 返回 `PongMessage`；`PongMessage` 可记录活跃时间但不触发业务脚本；文本 `ping` 继续返回 `pong` 以兼容旧客户端。

备选方案：删除文本 `ping`。该方案更纯粹，但会破坏已有对接。

理由：标准客户端可使用控制帧，历史客户端仍可平滑迁移。

### 3. 非文本消息默认拒绝

决定：BinaryMessage 默认不进入 MagicAPI 脚本，按不支持的数据类型处理，可关闭连接或返回错误；优先使用标准关闭码 `1003`。

备选方案：把 binary 暴露给脚本。该方案需要设计脚本变量、编码和大消息策略，超出本次裸 WS 标准化范围。

理由：当前插件定位是 MagicAPI 文本业务消息，明确拒绝比隐式忽略更安全。

### 4. 业务错误默认返回结构化错误，协议错误关闭连接

决定：非法 JSON、缺少 `type`、未知 `type` 等请求层错误返回 JSON 错误消息；明显协议/策略问题（不支持消息类型、token/appId 失效、容量过载）使用 close code 关闭。脚本异常返回错误消息，必要时使用 `1011` 关闭。

备选方案：所有错误都关闭连接。该方案严格但用户体验差，业务调试成本高。

理由：区分“连接还能继续使用的业务请求错误”和“连接不应继续存在的协议/策略错误”。

### 5. 关闭脚本只执行一次

决定：通过 session attribute 标记或原子状态保证 `close` 业务脚本在 `afterConnectionClosed`、主动清理、缓存驱逐等路径中只执行一次。

备选方案：保持现状由多路径调用 `end`。该方案容易重复触发下线逻辑。

理由：关闭事件是生命周期语义，重复触发会导致业务侧状态不一致。

### 6. 配置保持轻量，优先修正默认行为

决定：本次不大规模新增配置项；如果需要开关，仅增加少量兼容性配置，例如是否保留文本 ping、非法业务消息是否关闭连接。

备选方案：为每个行为都增加配置。该方案灵活但配置复杂，容易形成不可测试组合。

理由：标准化目标是收敛行为，而不是扩大行为矩阵。

```

Full source: openspec/changes/standardize-websocket-plugin/design.md

## openspec/changes/standardize-websocket-plugin/tasks.md

- Source: openspec/changes/standardize-websocket-plugin/tasks.md
- Lines: 1-19
- SHA256: b9bf16a57fb623b5dfe2b6bbf0a8c8d1e202a818ff977338902cce5821668933

```md
## 1. Protocol Contract Tests

- [ ] 1.1 Add tests for handshake validation, token/appId failure, and Origin/subprotocol behavior
- [ ] 1.2 Add tests for Ping/Pong control frames, text ping compatibility, binary rejection, and invalid text messages
- [ ] 1.3 Add tests for lifecycle script once-only execution and failed-handshake no-script behavior
- [ ] 1.4 Add tests for server push, multi-session delivery, and Redis/self-origin broadcast behavior where practical

## 2. WebSocket Standardization

- [ ] 2.1 Refactor handler message dispatch to separate control frames, text business messages, unsupported messages, and close handling
- [ ] 2.2 Standardize close statuses and structured error responses for invalid JSON, missing type, unknown type, script failure, unsupported data, overload, and shutdown
- [ ] 2.3 Ensure session registration, cleanup, send locks, cache eviction, and close script execution are idempotent and resource-safe
- [ ] 2.4 Review handshake interceptor/registration configuration for token, trusted Origin, and subprotocol negotiation correctness

## 3. Documentation and Verification

- [ ] 3.1 Update WS plugin README with standard WebSocket contract, compatibility notes, error response shape, close code mapping, and browser Ping limitations
- [ ] 3.2 Run targeted autoconfigure tests and fix regressions within the WS plugin scope
- [ ] 3.3 Run the selected Maven verification command for `fw-plus-boot-project/fw-plus-boot-autoconfigure`
```

## openspec/changes/standardize-websocket-plugin/specs/standard-websocket-plugin/spec.md

- Source: openspec/changes/standardize-websocket-plugin/specs/standard-websocket-plugin/spec.md
- Lines: 1-117
- SHA256: 2c5592afcd96e0186989e4be3bbdee1e4ad2ef178d0db30d76297d858c715b6a

[TRUNCATED]

```md
## ADDED Requirements

### Requirement: Standards-compliant handshake validation
The plugin SHALL validate required connection parameters, token, appId, trusted Origin, and negotiated subprotocol during the WebSocket handshake or immediately at connection establishment with deterministic failure behavior.

#### Scenario: Missing required connection parameter
- **WHEN** a client connects without the configured token parameter or clientId parameter
- **THEN** the plugin SHALL reject the connection or close it with a policy/protocol failure before registering the session

#### Scenario: Invalid token appId
- **WHEN** a client connects with an invalid, expired, or unknown appId token
- **THEN** the plugin SHALL reject the connection or close it without executing init scripts

#### Scenario: Trusted origin enforcement
- **WHEN** a browser client connects with an Origin not listed in `websocket.trustedDomain`
- **THEN** the plugin SHALL reject the handshake unless the configured trusted domain is explicitly `*`

#### Scenario: Subprotocol negotiation
- **WHEN** supported subprotocols are configured and the client offers one or more protocols
- **THEN** the plugin SHALL negotiate only configured protocols and SHALL NOT silently accept unsupported protocols

### Requirement: Standard WebSocket control frame handling
The plugin SHALL handle WebSocket control frames according to standard WebSocket semantics and keep existing text ping compatibility.

#### Scenario: Ping control frame
- **WHEN** a connected client sends a Ping control frame
- **THEN** the plugin SHALL respond with a Pong control frame without invoking business scripts

#### Scenario: Pong control frame
- **WHEN** a connected client sends a Pong control frame
- **THEN** the plugin SHALL accept it as a connection-level signal and SHALL NOT invoke business scripts

#### Scenario: Text ping compatibility
- **WHEN** a connected client sends text message `ping`
- **THEN** the plugin SHALL respond with text message `pong` for backward compatibility

### Requirement: Text business message contract
The plugin SHALL process business messages as text JSON objects with a non-blank `type` field and dispatch them to enabled `ws-subscribe` scripts for the same appId.

#### Scenario: Valid business message
- **WHEN** a connected client sends a JSON text message containing a non-blank `type` matching an enabled `ws-subscribe.businessId`
- **THEN** the plugin SHALL execute the matched script with appId, clientId, sessionId, type, and params in context

#### Scenario: Invalid JSON business message
- **WHEN** a connected client sends a text message that is neither `ping` nor valid JSON
- **THEN** the plugin SHALL return a structured error message or close with protocol error according to the configured policy

#### Scenario: Missing business type
- **WHEN** a connected client sends a JSON text message without a non-blank `type`
- **THEN** the plugin SHALL return a structured error message and SHALL NOT execute any business script

#### Scenario: Unknown business type
- **WHEN** a connected client sends a JSON text message whose `type` has no enabled matching subscription under the same appId
- **THEN** the plugin SHALL return a structured error message and keep the connection usable

### Requirement: Unsupported data type handling
The plugin SHALL explicitly handle unsupported WebSocket message types instead of silently ignoring them.

#### Scenario: Binary message received
- **WHEN** a connected client sends a binary message
- **THEN** the plugin SHALL reject it with close code 1003 or an equivalent unsupported-data handling path and SHALL NOT invoke MagicAPI scripts

#### Scenario: Oversized or service-overload condition
- **WHEN** the plugin cannot accept or retain a session due to configured capacity or resource pressure
- **THEN** the plugin SHALL close affected sessions with a service-overload close status and release session resources

### Requirement: Lifecycle script consistency
The plugin SHALL execute lifecycle scripts deterministically for successful session registration and closure.

#### Scenario: Init script after successful registration
- **WHEN** a connection is authenticated and registered successfully
- **THEN** the plugin SHALL execute enabled `init` scripts for that appId exactly once for that session

#### Scenario: Close script once
- **WHEN** a registered session is closed by client, server cleanup, cache eviction, or application shutdown
- **THEN** the plugin SHALL execute enabled `close` scripts at most once for that session

#### Scenario: Failed handshake no lifecycle script
- **WHEN** a connection fails authentication or validation before session registration
- **THEN** the plugin SHALL NOT execute init or close scripts for that connection
```

Full source: openspec/changes/standardize-websocket-plugin/specs/standard-websocket-plugin/spec.md

