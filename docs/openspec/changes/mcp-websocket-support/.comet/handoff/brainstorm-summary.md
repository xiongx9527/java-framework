# Brainstorm Summary

- Change: mcp-websocket-support
- Date: 2026-09-22

## 确认的技术方案

- WebSocket 端点挂载到 `/mcp/ws`，SSE 挂载到 `/mcp/sse`，两者独立注册、互不干扰
- WebSocket transport 继承 `AbstractStreamableTransport`，复用会话注册表、事件恢复缓冲、Mcp-Session-Id 管理等已有逻辑，仅替换 SSE 发送通道为 `WebSocketSession.send`
- 会话外部化通过 `McpSessionStore` 接口 + 条件装配：`InMemoryMcpSessionStore`（默认）和 `RedisMcpSessionStore`（Redis 在 classpath 时自动激活）
- Redis 会话存储使用 `RedisTemplate<String, String>`，Key 格式 `mcp:session:{sessionId}`，TTL 与 `session-timeout-seconds` 一致
- WebSocket 同时支持 MVC 和 WebFlux 两个栈（修订 D5）

## 关键取舍与风险

- 迁移到 Spring AI MCP 被排除：Spring AI 1.0.x 要求 Java 17+，项目当前 Java 8
- WebSocket 连接资源消耗：高并发场景需监控连接数，配置合理超时
- Redis 单点风险：依赖现有业务 Redis 的高可用架构
- 会话序列化兼容性：使用 `LinkedHashMap` 保证字段顺序，新增字段可选

## 测试策略

- WebSocket transport 单测覆盖：连接建立、消息收发、异常关闭
- 条件装配测试：Redis 在 classpath 时自动激活 RedisMcpSessionStore，否则回退内存
- 全量 Maven 编译验证

## Spec Patch

无。specs 无需修改。
