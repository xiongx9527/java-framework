## Context

The SSE plugin currently supports MagicAPI-managed SSE datasources and subscriptions. It also has MCP-related code that sends an `endpoint` event and processes JSON-RPC messages, but the message entrypoint is exposed through `SseModule.message(...)` rather than a normal HTTP endpoint. The current flow resembles the older MCP HTTP+SSE transport, while the current MCP standard uses Streamable HTTP with a single endpoint supporting POST and GET.

The implementation must live in the Spring Boot 2.7.18 / JDK 1.8 autoconfigure layer and follow the existing MagicAPI plugin style. Existing MagicScript users of `sse.connect`, broadcast APIs, datasource storage, and lifecycle scripts must not be broken by the MCP transport work.

## Goals / Non-Goals

**Goals:**
- Provide a real MCP Streamable HTTP endpoint for standard clients.
- Support MCP initialization, initialized notification, tools/list, and tools/call using JSON-RPC 2.0.
- Enforce MCP transport expectations for Accept headers, protocol version headers, session identifiers, origin checks, and lifecycle ordering.
- Keep legacy SSE endpoint-event compatibility available for clients that still use the old HTTP+SSE flow.
- Add tests for transport compliance and regressions around existing SSE behavior.

**Non-Goals:**
- Do not rebuild the entire MagicAPI plugin architecture.
- Do not change `fw-plus-auth`.
- Do not restore JavaParser or the MagicAPI Java plugin.
- Do not add non-tool MCP capabilities such as resources, prompts, roots, or sampling in this change.
- Do not require users to rewrite ordinary SSE broadcast scripts.

## Decisions

### Decision 1: Add a dedicated MCP transport controller

Create or extend an HTTP controller in the SSE plugin so MCP clients can call a configured endpoint directly. The default endpoint should be `/mcp`, with configuration support for deployment-specific paths. POST handles client JSON-RPC messages. GET opens an optional server-to-client SSE stream for Streamable HTTP notifications and responses when the client accepts `text/event-stream`.

Alternative considered: keep using `SseModule.message(...)` as the message entrypoint. This is rejected because standard MCP clients do not know how to invoke MagicScript module methods and expect HTTP transport semantics.

### Decision 2: Separate transport session state from ordinary SSE sessions

Introduce explicit MCP session state that tracks session id, datasource/app id, initialized lifecycle status, protocol version, emitter if present, and last activity. It can reuse existing cleanup mechanisms but should not overload `clientId@appId@uuid` as the only session identifier exposed to clients. Streamable HTTP should use `Mcp-Session-Id` headers; legacy SSE compatibility may continue to include `sessionId` in the endpoint URL.

Alternative considered: keep query-string `sessionId` only. This is rejected for standard mode because MCP uses headers for session continuity and query tokens are more likely to leak through logs and proxies.

### Decision 3: Keep tools as MagicAPI API wrappers

Continue mapping MagicAPI APIs to MCP tools. `tools/list` builds tool definitions from a configured MagicAPI group, and `tools/call` invokes the corresponding MagicAPI script with request arguments. The transport layer validates JSON-RPC and lifecycle state; the tool adapter handles MagicAPI-specific parameter mapping.

Alternative considered: introduce a separate tool registry unrelated to MagicAPI APIs. This is out of scope and would duplicate existing MagicAPI resource storage.

### Decision 4: Preserve legacy HTTP+SSE compatibility as a compatibility layer

When a datasource is configured for MCP legacy SSE mode, the server may still send the old `endpoint` event on connect. That endpoint must point to an actual POST route. Legacy mode should share request processing and tool execution code with Streamable HTTP to avoid divergent MCP behavior.

Alternative considered: remove the old endpoint-event flow. This risks breaking any current caller that was already using `sse.connect` and the generated endpoint.

## Risks / Trade-offs

- Standard MCP and legacy SSE modes have different session semantics -> keep a shared JSON-RPC processor but separate transport adapters.
- Origin validation may reject deployments that relied on permissive behavior -> provide explicit configuration for trusted origins and fail closed for MCP endpoints unless configured otherwise.
- Existing tests may be sparse around MagicAPI dynamic scripts -> add transport-level tests with mocked or lightweight MagicAPI resource service behavior where possible.
- Supporting both POST JSON responses and SSE responses increases branch coverage needs -> write tests for both `application/json` and `text/event-stream` Accept behavior.

## Migration Plan

1. Add the standard MCP controller and session state behind the existing SSE plugin auto-configuration.
2. Keep current `SseModule.connect` and ordinary broadcast APIs intact.
3. Update the legacy endpoint-event path so it points to the new real message endpoint.
4. Document the default MCP endpoint, required headers, trusted origins, and legacy compatibility behavior.
5. Rollback strategy: disable the new MCP endpoint through plugin configuration while leaving ordinary SSE behavior available.

## Open Questions

- The default endpoint is assumed to be `/mcp`; implementation should verify whether existing MagicAPI web base path should prefix it by default.
- The initial supported protocol version should include `2025-06-18`; legacy mode may still report `2024-11-05` when explicitly configured.
