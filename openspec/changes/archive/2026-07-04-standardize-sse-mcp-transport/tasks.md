## 1. Transport Entry Points

- [x] 1.1 Add a configurable standard MCP endpoint to the SSE plugin auto-configuration.
- [x] 1.2 Implement POST request handling for JSON-RPC MCP messages with standard headers.
- [x] 1.3 Implement GET request handling for optional Streamable HTTP SSE streams.
- [x] 1.4 Update legacy endpoint-event generation so it points to an actual POST route.

## 2. Session and Lifecycle

- [x] 2.1 Add explicit MCP session state for session id, datasource key, protocol version, lifecycle phase, and optional SSE emitter.
- [x] 2.2 Enforce initialize-before-tool-use and initialized-notification lifecycle rules.
- [x] 2.3 Add cleanup for completed, timed-out, and invalid MCP sessions without breaking ordinary SSE sessions.

## 3. Protocol Validation and Security

- [x] 3.1 Validate `Accept`, `MCP-Protocol-Version`, and `Mcp-Session-Id` for standard MCP requests.
- [x] 3.2 Add trusted Origin validation for MCP endpoints.
- [x] 3.3 Return standards-compatible JSON-RPC errors for malformed requests, unknown methods, unknown tools, and invalid lifecycle state.

## 4. MagicAPI Tool Adapter

- [x] 4.1 Keep `tools/list` backed by configured MagicAPI API groups and return valid MCP tool schemas.
- [x] 4.2 Keep `tools/call` backed by MagicAPI script execution and return MCP content blocks.
- [x] 4.3 Ensure tool invocation cannot bypass datasource MCP enablement or lifecycle checks.

## 5. Tests and Documentation

- [x] 5.1 Add tests for standard initialize, initialized notification, tools/list, and tools/call flows.
- [x] 5.2 Add tests for invalid headers, untrusted origins, unknown sessions, unknown methods, and unknown tools.
- [x] 5.3 Add tests for legacy endpoint-event compatibility.
- [x] 5.4 Update plugin documentation or sample notes for endpoint paths, headers, sessions, origins, and legacy mode.
