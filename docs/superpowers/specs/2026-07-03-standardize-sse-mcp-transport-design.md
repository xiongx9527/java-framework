---
comet_change: standardize-sse-mcp-transport
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-04-standardize-sse-mcp-transport
status: final
---

# Standardize SSE MCP Transport Design

## Context

The MagicAPI SSE plugin currently mixes ordinary SSE broadcast support with partial MCP behavior inside `BaseDynamicSseHandler` and `SseModule`. That design can serve MagicScript callers, but it does not expose the HTTP transport surface that standard MCP clients expect.

The canonical OpenSpec change is `openspec/changes/standardize-sse-mcp-transport`. This document defines the implementation approach only; the OpenSpec delta spec remains the source of requirements.

## Architecture

Add a dedicated MCP transport layer under the existing SSE plugin package:

- `MagicMcpController`: Spring MVC controller for the configured MCP endpoint.
- `McpSessionService`: owns standard MCP sessions, lifecycle phase, protocol version, optional SSE emitter, and cleanup.
- `McpJsonRpcProcessor`: validates and dispatches JSON-RPC methods.
- `MagicApiMcpToolAdapter`: maps MagicAPI APIs to MCP tools and executes `tools/call`.
- `McpTransportProperties`: typed configuration derived from `magic-api.plugin.sse`, including endpoint path, trusted origins, legacy endpoint path, and stream enablement.

The controller handles transport concerns only: headers, Origin, HTTP status, session id headers, and request/response media type. The processor handles JSON-RPC and lifecycle semantics. The tool adapter handles MagicAPI resource lookup and script execution. Existing ordinary SSE broadcast methods stay in the existing handler path.

## Data Flow

Standard Streamable HTTP flow:

1. Client sends `POST /mcp` with a JSON-RPC `initialize` request and `Accept: application/json, text/event-stream`.
2. Controller validates Origin and Accept headers.
3. Session service creates or resumes a session and returns `Mcp-Session-Id` when stateful mode is used.
4. Processor returns `protocolVersion`, `capabilities`, and `serverInfo`.
5. Client sends `notifications/initialized`; session service marks the session ready.
6. Client sends `tools/list` or `tools/call`; processor checks lifecycle state and delegates to the MagicAPI tool adapter.

Optional streaming flow:

1. Client sends `GET /mcp` with `Accept: text/event-stream`.
2. Controller validates Origin and session headers.
3. Session service attaches an `SseEmitter` to the session.
4. Server responses or notifications can be delivered through the stream when needed.

Legacy compatibility flow:

1. Existing `sse.connect(appId, secret, clientId)` remains available.
2. If the datasource is MCP-enabled, the legacy SSE connection sends an `endpoint` event.
3. The endpoint points to a real POST route that delegates to the same JSON-RPC processor.
4. Legacy query `sessionId` is translated into the corresponding MCP session internally.

## Lifecycle Rules

Each MCP session has a small explicit state machine:

- `NEW`: created but not initialized.
- `INITIALIZED`: `initialize` returned successfully.
- `READY`: `notifications/initialized` received.
- `CLOSED`: stream completed, timeout, explicit cleanup, or invalidation.

Only `initialize` is accepted in `NEW`. `notifications/initialized` is accepted in `INITIALIZED`. Tool methods are accepted only in `READY`. Notifications do not produce JSON-RPC response bodies.

## Header and Security Rules

The standard endpoint validates:

- `Accept`: POST must accept `application/json` and `text/event-stream`; GET must accept `text/event-stream`.
- `MCP-Protocol-Version`: required after initialization when configured for strict mode; unsupported versions are rejected.
- `Mcp-Session-Id`: required for existing stateful sessions; unknown ids are rejected.
- `Origin`: rejected unless absent for non-browser callers or present in configured trusted origins, depending on strict-origin configuration.

Secrets must not be passed in standard MCP query parameters. Legacy compatibility may continue to support existing secret usage, but new standard clients should use configured server-side datasource selection and headers.

## MagicAPI Tool Adapter

`tools/list` reads the configured MagicAPI group from datasource MCP configuration and returns tools with:

- `name`: stable MagicAPI API id.
- `description`: MagicAPI description or a generated fallback.
- `inputSchema`: JSON Schema object generated from MagicAPI parameters.

`tools/call` validates the tool name, resolves the MagicAPI API, maps MCP `arguments` into the script context, executes the script, and returns MCP content blocks:

```json
{
  "content": [
    { "type": "text", "text": "..." }
  ],
  "isError": false
}
```

Execution errors are reported as `isError: true` only when the script ran and failed as a tool result. Transport, lifecycle, unknown method, invalid params, and unknown tool errors remain JSON-RPC errors.

## Compatibility

Existing ordinary SSE behavior remains compatible:

- `SseModule.connect(...)` still returns `SseEmitter`.
- `broadcastMessageToClient` and `broadcastDistributedMessageToAllClient` keep the existing behavior.
- datasource and subscribe resource storage names remain unchanged.
- lifecycle scripts for ordinary SSE connections continue to run.

Legacy MCP endpoint-event clients remain supported if configured, but the generated endpoint must call a real HTTP route instead of relying on undocumented MagicScript invocation.

## Testing Strategy

Add focused tests around the new transport layer:

- Standard initialize succeeds and returns required MCP fields.
- Initialized notification transitions the session to ready without a response body.
- Tool methods before initialize or before initialized notification return JSON-RPC errors.
- `tools/list` returns only configured MagicAPI tools.
- `tools/call` invokes known tools and rejects unknown tools.
- Missing or unacceptable `Accept` headers are rejected.
- Unknown `Mcp-Session-Id` is rejected.
- Untrusted Origin is rejected before tool execution.
- Legacy endpoint event points to a route handled by the controller.
- Existing ordinary broadcast APIs still route through current SSE behavior.

The first implementation task should write failing controller/session tests before introducing the controller so transport behavior is pinned down early.

## Risks

- `BaseDynamicSseHandler` is already large; adding standard MCP directly there would make it harder to test and maintain. Split MCP-specific classes instead.
- Some deployments may rely on permissive Origin behavior. Default behavior should be documented, and trusted origins should be explicit.
- MagicAPI resource service can be difficult to instantiate in isolated tests. Prefer mocking the MagicAPI lookup boundary in the tool adapter tests.

## Build Notes

Use JDK 1.8 and the module-local Maven build. Prefer focused tests for `fw-plus-boot-autoconfigure` before running broader boot builds.
