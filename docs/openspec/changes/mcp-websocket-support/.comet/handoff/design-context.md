# Comet Design Handoff

- Change: mcp-websocket-support
- Phase: design
- Mode: compact
- Context hash: 5563624a68ca27aaf0e3b6870946c6ed3865b7ab0df5f90fed96d53a19a2a672

Generated-by: comet-handoff.sh
Task hash policy: task-content-v1. Read tasks.md for live completion; excerpts are design-time context.

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## docs/openspec/changes/mcp-websocket-support/proposal.md

- Source: docs/openspec/changes/mcp-websocket-support/proposal.md
- Lines: 1-34
- SHA256: 972060d8360f7be3b6a71a13d5237c228750014be9d42b9c07b58857ed5cfcaf

```md
# Proposal

## Why

当前 `fw-plus-ai-mcp-spring-boot-starter` 仅支持 Streamable HTTP（SSE）单传输协议。在微服务集群部署下，长连接会话绑定单机内存，实例宕机会导致所有活跃 MCP 会话丢失；同时缺少 WebSocket 传输，无法满足低延迟、双向推送的客户端场景。

本变更新增 WebSocket 传输支持，并引入可选的 Redis 会话外部化，使同一服务端可同时服务 SSE 和 WebSocket 两种客户端，同时支持单机和集群两种部署模式。

## What Changes

- 新增 WebSocket 传输端点 `/mcp/ws`，支持双向 JSON-RPC 消息
- 新增 SSE 传输端点 `/mcp/sse`，与原有 `/mcp` 路径对齐
- 新增 `McpSessionStore` 接口 + 内存/Redis 两种实现，Redis 可用时自动切换
- 配置属性新增 `sse-enabled`、`sse-path`、`websocket-enabled`、`websocket-path`
- 无 BREAKING 变更，原有 `/mcp` 路径行为不变

## Capabilities

### New Capabilities

- `mcp/websocket-transport`: WebSocket 传输层支持，包括端点、编解码、消息路由和会话生命周期
- `mcp/session-externalization`: 会话状态外部化，支持内存（默认）和 Redis（可选）两种存储后端，自动检测切换

### Modified Capabilities

无。本次变更仅新增能力，不修改已有 MCP 行为规格。

## Impact

- **新增模块**：`fw-plus-ai-mcp` 新增 `transport/websocket/` 和 `transport/session/` 包
- **新增配置**：`McpProperties` 新增 4 个属性（`sseEnabled`、`ssePath`、`websocketEnabled`、`websocketPath`）
- **新增自动配置**：`McpWebSocketAutoConfiguration`、`McpSessionAutoConfiguration`
- **新增依赖**：`fw-plus-ai-mcp-spring-boot-starter` 新增 `spring-boot-starter-websocket`（非 optional）
- **可选依赖**：Redis 会话存储依赖 `spring-boot-starter-data-redis`，通过条件装配自动激活

```

## docs/openspec/changes/mcp-websocket-support/design.md

- Source: docs/openspec/changes/mcp-websocket-support/design.md
- Lines: 1-101
- SHA256: 6fce050aeff0cdca5eac033c71cad5a98ec6162b8617df16e30fa502b13880b0

[TRUNCATED]

```md
---
comet_change: mcp-websocket-support
role: technical-design
canonical_spec: openspec
---

# Design

## Context

See proposal.md - Why. 当前 `fw-plus-ai-mcp-spring-boot-starter` 已有 Streamable HTTP 传输（`McpServletStreamableTransport` / `McpReactiveStreamableTransport`）和完整的 MCP 协议实现。新增 WebSocket 传输需要复用现有协议栈，仅替换传输层；会话外部化需要将当前内嵌在 transport 中的 `McpSession` 管理抽离为独立接口。

项目基于 Java 8 + Spring Boot 2.7，不能引入 Spring AI MCP（需 Java 17+）。WebSocket 能力通过 `spring-boot-starter-websocket` 提供，与 WebFlux 无关。

## Goals / Non-Goals

**Goals:**
- WebSocket 传输与现有 Streamable HTTP 并存，客户端可自由选择
- 会话状态可外部化，支持集群部署
- Redis 可用时自动切换，不可用时零额外依赖
- 配置与现有 `McpProperties` 风格一致

**Non-Goals:**
- 不替代现有 Streamable HTTP 实现
- 不引入 Spring WebFlux 依赖到 WebSocket 路径
- 不实现自定义 WebSocket 协议（沿用标准 `ws://` / `wss://`）

## Decisions

### D1: WebSocket 端点独立于 SSE 端点

**决策**：WebSocket 挂载到 `/mcp/ws`，SSE 挂载到 `/mcp/sse`，两者独立注册、互不干扰。

**理由**：
- 路径分离清晰，负载均衡可按路径分别路由
- 客户端连接时无需协商传输类型
- 与 MCP 社区常见实践一致

**替代方案**：单路径 `/mcp` 根据 `Upgrade: websocket` 头动态切换 — 实现复杂，且与 Spring MVC `SseEmitter` 的 GET 处理冲突。

### D2: WebSocket transport 复用 `AbstractStreamableTransport` 基础设施

**决策**：WebSocket transport 继承 `AbstractStreamableTransport`，复用会话注册表、事件恢复缓冲、`Mcp-Session-Id` 管理、协议版本协商等已有逻辑；仅替换 SSE 发送通道为 `WebSocketSession.send`。

**理由**：
- 会话生命周期、事件恢复、`Last-Event-ID` 等复杂逻辑已实现且验证过
- 行为一致性高：两种传输对客户端的 MCP 协议体验完全相同
- 新增代码量最小

**替代方案**：全新独立 transport — 需重写全部会话管理，行为易出现偏差。

### D3: 会话外部化通过 `McpSessionStore` 接口 + 条件装配

**决策**：定义 `McpSessionStore` 接口，提供 `InMemoryMcpSessionStore`（默认）和 `RedisMcpSessionStore`（Redis 在 classpath 时自动激活）两个实现。

**理由**：
- 零依赖回退：无 Redis 时行为与当前完全一致
- 扩展性：未来可添加其他存储后端（如 Hazelcast、数据库）
- Spring 条件装配成熟可靠

**替代方案**：强制 Redis 依赖 — 影响现有用户，违背"可选"原则。

### D4: Redis 会话存储使用 Spring Data Redis

**决策**：`RedisMcpSessionStore` 使用 `RedisTemplate<String, String>` 存储序列化后的会话 JSON，Key 为 `mcp:session:{sessionId}`，过期时间与 `session-timeout-seconds` 一致。

**理由**：
- 项目已有 Redis 能力，`RedisTemplate` 是标准抽象
- String 序列化简单可靠，避免复杂对象序列化问题
- TTL 由 Redis 原生支持，无需应用层定时清理

**替代方案**：Redisson — 额外依赖，当前场景不需要分布式锁。

### D5: WebSocket 同时支持 MVC 和 WebFlux 两个栈

**决策**：WebSocket 端点同时提供 Spring MVC（`AbstractWebSocketHandler`）和 Spring WebFlux（`WebSocketHandler`）两个版本，各自独立条件装配。同一应用两种栈同时存在时两个端点都会注册，Spring 按实际 Web 应用类型路由。

**理由**：
- 与 SSE 双栈对称：项目已有 `McpWebMvcAutoConfiguration` 和 `McpWebFluxAutoConfiguration`，WebSocket 遵循相同模式
- `spring-boot-starter-websocket` 对两栈都适用，无额外依赖成本

```

Full source: docs/openspec/changes/mcp-websocket-support/design.md

## docs/openspec/changes/mcp-websocket-support/tasks.md

- Source: docs/openspec/changes/mcp-websocket-support/tasks.md
- Lines: 1-31
- SHA256: 21260e4116493bcb6ff229d23b0084ae51d21a486bc27e971052af02c1578a84

```md
# Tasks

## 1. Core 层：会话外部化接口与实现

- [ ] 1.1 新增 `McpSessionStore` 接口（`save`/`load`/`remove`/`touch`），定义在 `fw-plus-ai-mcp` 的 `transport/session` 包，验证接口契约清晰
- [ ] 1.2 新增 `InMemoryMcpSessionStore` 实现，将当前 `AbstractStreamableTransport` 内嵌的会话管理逻辑迁移至此，验证单机模式行为不变
- [ ] 1.3 新增 `RedisMcpSessionStore` 实现，使用 `RedisTemplate<String, String>` 存储序列化会话 JSON，Key 格式 `mcp:session:{sessionId}`，验证 Redis 可用时正确读写和过期

## 2. Core 层：WebSocket Transport 实现

- [ ] 2.1 新增 `McpWebSocketTransport`，继承 `AbstractStreamableTransport`，覆写 SSE 发送方法为 `WebSocketSession.send`，验证双向 JSON-RPC 消息路由正确
- [ ] 2.2 新增 `McpWebSocketEndpoint`（MVC 栈），继承 `AbstractWebSocketHandler`，处理握手、消息接收和连接关闭，验证端点可建立连接并处理 JSON-RPC
- [ ] 2.3 新增 `McpWebFluxWebSocketEndpoint`（WebFlux 栈），实现 `WebSocketHandler`，处理 `Flux<WebSocketMessage>`，验证 Reactive 端点正常工作
- [ ] 2.4 编写 WebSocket transport 单测，覆盖连接建立、消息收发、异常关闭场景

## 3. Autoconfigure 层：装配与配置

- [ ] 3.1 修改 `McpProperties`，新增 `sseEnabled`、`ssePath`、`websocketEnabled`、`websocketPath` 四个属性，验证配置绑定正确
- [ ] 3.2 新增 `McpSessionAutoConfiguration`，条件装配 `InMemoryMcpSessionStore`（默认）和 `RedisMcpSessionStore`（Redis 在 classpath 时激活），验证自动切换逻辑
- [ ] 3.3 新增 `McpWebSocketAutoConfiguration`（MVC）和 `McpWebFluxWebSocketAutoConfiguration`（WebFlux），条件装配 WebSocket transport 和端点，验证与现有 SSE 装配互不干扰

## 4. Starter 层：依赖与注册

- [ ] 4.1 在 `fw-plus-ai-mcp-spring-boot-starter/pom.xml` 新增 `spring-boot-starter-websocket` 依赖，验证 Maven 构建通过
- [ ] 4.2 更新 `README.md`，新增 WebSocket 和 SSE 端点配置说明，验证文档准确

## 5. 样例与验证

- [ ] 5.1 在 `fw-plus-ai-mcp-server-sample` 中增加 WebSocket 客户端示例代码，验证 WebSocket 连接、工具调用完整流程
- [ ] 5.2 在 `fw-plus-ai-mcp-server-sample` 中增加 SSE 端点示例，验证 `/mcp/sse` 正常工作
- [ ] 5.3 运行全量 Maven 编译（`mvnw clean install -DskipTests`），验证无编译错误

```

## docs/openspec/changes/mcp-websocket-support/specs/mcp/session-externalization/spec.md

- Source: docs/openspec/changes/mcp-websocket-support/specs/mcp/session-externalization/spec.md
- Lines: 1-47
- SHA256: a94c942e8eea4ccf0bbbf345edd62b4f45d7877222395759936ed1be07428504

```md
# Spec Delta

## Purpose

允许 MCP 会话状态存储在外部系统（Redis）而非单机内存，使集群环境下多实例共享会话数据，支持故障转移和横向扩展。

## ADDED Requirements

### Requirement: 会话状态可存储到外部系统
The system SHALL 提供 `McpSessionStore` 抽象接口，支持将 MCP 会话数据持久化到外部存储。

#### Scenario: 会话创建时写入外部存储
- **WHEN** 新 MCP 会话建立（SSE 或 WebSocket）
- **THEN** 会话数据被写入外部存储（Redis 或内存）

#### Scenario: 会话读取时从外部存储获取
- **WHEN** 客户端携带 `Mcp-Session-Id` 重新连接
- **THEN** 服务端从外部存储加载会话数据并恢复

### Requirement: Redis 可用时自动使用 Redis 存储
The system SHALL 在 `spring-boot-starter-data-redis` 在 classpath 且 Redis 连接可用时，自动选择 Redis 作为会话存储后端。

#### Scenario: Redis 可用时选择 Redis
- **WHEN** 应用引入 Redis 依赖且 Redis 连接正常
- **THEN** 会话数据存储到 Redis，所有实例共享

#### Scenario: Redis 不可用时回退内存
- **WHEN** 应用未引入 Redis 依赖，或 Redis 连接失败
- **THEN** 会话数据存储在单机内存，行为与当前版本一致

### Requirement: 会话超时与过期清理
The system SHALL 支持会话超时机制，外部存储实现的会话应设置过期时间，内存实现的会话应定期清理。

#### Scenario: Redis 会话过期
- **WHEN** 会话在 Redis 中超过 `session-timeout-seconds` 未活跃
- **THEN** Redis 自动过期该键，下次访问时视为会话失效

#### Scenario: 内存会话清理
- **WHEN** 内存会话超过 `session-timeout-seconds` 未活跃
- **THEN** 清理线程定期移除过期会话

### Requirement: 会话外部化对客户端透明
The system SHALL 确保会话外部化的切换不改变客户端可见行为，客户端无需感知存储后端。

#### Scenario: 客户端不感知存储后端
- **WHEN** 客户端通过 SSE 或 WebSocket 进行 MCP 交互
- **THEN** 无论会话存储在内存还是 Redis，客户端体验一致

```

## docs/openspec/changes/mcp-websocket-support/specs/mcp/websocket-transport/spec.md

- Source: docs/openspec/changes/mcp-websocket-support/specs/mcp/websocket-transport/spec.md
- Lines: 1-58
- SHA256: 6d317363d4bc90f71d6a39058a585e0a94d817223437c933c4810a582e8b726a

```md
# Spec Delta

## Purpose

允许 MCP 客户端通过 WebSocket 协议与服务端通信，提供与 Streamable HTTP 等效的工具调用、资源读取、提示词和日志能力，同时支持服务端向客户端的双向实时推送。

## ADDED Requirements

### Requirement: WebSocket 端点可建立连接
The system SHALL 在配置路径上提供 WebSocket 端点，客户端完成握手后即进入 JSON-RPC 消息交换状态。

#### Scenario: 客户端连接 WebSocket 端点
- **WHEN** 客户端向配置的 WebSocket 路径发起 `ws://` 或 `wss://` 握手请求
- **THEN** 服务端完成握手并保持连接，等待 JSON-RPC 消息

#### Scenario: 连接被拒绝
- **WHEN** 客户端连接到一个未被 MCP 注册的 WebSocket 路径
- **THEN** 服务端返回 404 或拒绝握手

### Requirement: WebSocket 传输支持完整的 MCP JSON-RPC 生命周期
The system SHALL 通过 WebSocket 连接支持 MCP 协议的全部请求类型，包括 `initialize`、`tools/list`、`tools/call`、`resources/list`、`resources/read`、`prompts/list`、`prompts/get`、`completions` 和 `logging/setLevel`。

#### Scenario: 初始化握手
- **WHEN** 客户端通过 WebSocket 发送 `initialize` 请求
- **THEN** 服务端返回包含协议版本、服务端能力和指令的响应

#### Scenario: 工具调用
- **WHEN** 客户端通过 WebSocket 发送 `tools/call` 请求
- **THEN** 服务端执行工具并返回结果或错误响应

### Requirement: 服务端可通过 WebSocket 推送通知
The system SHALL 允许服务端通过已建立的 WebSocket 连接向客户端推送 JSON-RPC 通知消息，例如 `notifications/progress` 和 `notifications/logging/message`。

#### Scenario: 服务端推送日志通知
- **WHEN** 服务端产生日志事件且客户端已订阅日志级别
- **THEN** 服务端通过 WebSocket 向客户端发送 `notifications/logging/message` 通知

### Requirement: 连接异常时清理会话状态
The system SHALL 在 WebSocket 连接异常关闭（客户端断开、网络中断、服务端关闭）时，清理对应的 MCP 会话状态，防止内存泄漏。

#### Scenario: 客户端主动断开
- **WHEN** 客户端正常关闭 WebSocket 连接
- **THEN** 服务端清理该会话，不再接收新消息

#### Scenario: 客户端异常断开
- **WHEN** WebSocket 连接因网络原因异常中断
- **THEN** 服务端检测到连接关闭后清理该会话

### Requirement: WebSocket 传输可独立开关
The system SHALL 提供 `websocket-enabled` 配置属性，允许在不影响 Streamable HTTP 传输的情况下独立启用或禁用 WebSocket 传输。

#### Scenario: WebSocket 启用
- **WHEN** `websocket-enabled` 设置为 `true`
- **THEN** WebSocket 端点正常注册并接受连接

#### Scenario: WebSocket 禁用
- **WHEN** `websocket-enabled` 设置为 `false` 或未设置
- **THEN** WebSocket 端点不注册，连接请求被拒绝

```
