---
comet_change: complete-magic-mcp-standard-plugin
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-06-complete-magic-mcp-standard-plugin
status: final
---

# Complete Magic MCP Standard Plugin Design

## Confirmed Direction
一期完整功能聚焦 MCP 标准兼容和可用闭环：协议版本升级到 `2025-11-25`、精确 capabilities、Streamable HTTP/SSE 语义、server-to-client 请求闭环、Magic 控制台诊断预览和 tool schema 增强。

## Architecture
- Keep existing packages: `protocol`, `web`, `core`, `client`, `tools`, `resources`, `prompts`, `notification`, `starter`.
- Add protocol configuration and negotiation in lifecycle/transport.
- Extend dispatcher to route JSON-RPC responses to client request service.
- Extend session state with pending client request records.
- Add a small console diagnostic endpoint/service rather than bypassing MCP handlers.

## Non-goals
- No MCP SDK rewrite.
- No `mcp-datasource`.
- No incompatible storage migration for `mcp-resource` or `mcp-prompt`.

## Validation
Run MCP-focused backend tests plus module compilation. Add tests around every new protocol and lifecycle behavior before implementation.
