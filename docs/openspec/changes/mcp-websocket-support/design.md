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
- `McpWebSocketTransport` 核心逻辑共享，仅端点适配层有差异
- 避免未来 WebFlux 用户被迫回退到 MVC

**替代方案**：仅 MVC 栈 — 代码量少约 30%，但 WebFlux 用户无 WebSocket 能力，与项目已有的双栈 SSE 设计不一致。

## Risks / Trade-offs

- **WebSocket 连接资源消耗**：每个连接占用一个线程/回调，高并发场景需监控连接数 → 服务端配置 `server.max-http-request-header-size` 并设置合理的连接超时
- **Redis 单点风险**：如果 Redis 宕机，集群模式下会话丢失 → 与现有业务 Redis 共用，依赖其高可用架构
- **会话序列化兼容性**：未来新增会话字段时，Redis 中旧格式需兼容 → 会话 JSON 使用 `LinkedHashMap` 保证字段顺序，新增字段可选

## Migration Plan

1. **Phase 1**：新增 Core 层 transport/session 接口和实现，新增 Autoconfigure 层配置类
2. **Phase 2**：`fw-plus-ai-mcp-spring-boot-starter` 新增 `spring-boot-starter-websocket` 依赖，注册新端点
3. **Phase 3**：样例应用增加 WebSocket 客户端示例
4. **回滚策略**：通过 `sse-enabled=false` / `websocket-enabled=false` 独立关闭任一传输，无需回滚代码

## Open Questions

无。
