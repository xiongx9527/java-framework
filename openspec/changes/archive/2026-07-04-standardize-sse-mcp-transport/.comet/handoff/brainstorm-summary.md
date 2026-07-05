# Brainstorm Summary

- Change: standardize-sse-mcp-transport
- Date: 2026-07-03

## 确认的技术方案

新增独立 MCP transport controller 和 session service，复用现有 SSE handler 中的 MagicAPI tool adapter 逻辑，并把旧 SSE endpoint-event flow 接到同一套 JSON-RPC processor。标准模式提供真实 MCP HTTP endpoint，默认 `/mcp`，POST 处理 JSON-RPC，GET 在 `Accept: text/event-stream` 时打开 SSE stream。标准会话使用 `Mcp-Session-Id` header，旧兼容模式保留 query `sessionId`。

## 关键取舍与风险

- 取舍：不把标准 MCP endpoint 继续隐藏在 MagicScript module 方法里，因为标准 MCP 客户端需要真实 HTTP endpoint。
- 取舍：标准模式使用 `Mcp-Session-Id` header，旧兼容模式保留 query `sessionId`。
- 风险：Origin 校验可能改变原先宽松访问行为，需要配置可信来源。
- 风险：现有 MCP 逻辑集中在 `BaseDynamicSseHandler`，实现时需要拆出较小的 processor/session 组件，避免继续扩大单文件职责。

## 测试策略

新增 Spring MVC transport 层测试，覆盖 initialize、initialized、tools/list、tools/call、header 校验、Origin 拒绝、未知 session、旧 endpoint-event 指向真实 route。补充普通 SSE broadcast 兼容性回归测试。

## Spec Patch

当前无必须回写的 spec patch。现有 delta spec 已覆盖 Streamable HTTP、header/session、lifecycle、tools adapter、legacy compatibility、security 和普通 SSE 兼容。
