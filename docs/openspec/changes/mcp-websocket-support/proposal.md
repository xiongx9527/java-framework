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
