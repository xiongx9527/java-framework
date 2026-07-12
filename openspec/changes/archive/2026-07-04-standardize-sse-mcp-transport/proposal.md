## Why

The current MagicAPI SSE plugin exposes a custom SSE transport with partial MCP behavior, but it does not provide a standards-compliant MCP HTTP endpoint. This prevents standard MCP clients from reliably connecting, negotiating lifecycle state, invoking tools, and handling sessions according to the current MCP transport rules.

## What Changes

- Add a standards-compliant MCP Streamable HTTP transport for the MagicAPI SSE plugin.
- Add a real HTTP endpoint for JSON-RPC MCP messages instead of requiring callers to invoke MagicScript module methods directly.
- Preserve the existing MagicAPI SSE broadcast and dynamic datasource behavior.
- Preserve backward compatibility for the current legacy HTTP+SSE-style endpoint event flow where feasible.
- Add transport, lifecycle, session, header, origin, and tool invocation validation.
- Add focused tests that prove standard MCP behavior and legacy compatibility.

## Capabilities

### New Capabilities
- `mcp-transport`: Standard MCP transport support for MagicAPI-backed tools over Streamable HTTP, with legacy SSE compatibility.

### Modified Capabilities

## Impact

- Affected code: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse`.
- Affected plugin starter: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-sse`.
- Affected behavior: MCP clients will use explicit HTTP endpoints and standard headers/session semantics; existing ordinary SSE broadcast APIs remain compatible.
- Dependencies: no new dependency is expected unless existing Spring MVC and `SseEmitter` APIs cannot satisfy the transport contract.
