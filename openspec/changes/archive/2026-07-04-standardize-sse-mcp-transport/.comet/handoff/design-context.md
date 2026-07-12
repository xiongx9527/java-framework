# Comet Design Handoff

- Change: standardize-sse-mcp-transport
- Phase: design
- Mode: compact
- Context hash: fbe607643ad978bc36d1bd4b9fbfeb8290e125c102b3d79f975b2de41b41f0eb

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/standardize-sse-mcp-transport/proposal.md

- Source: openspec/changes/standardize-sse-mcp-transport/proposal.md
- Lines: 1-26
- SHA256: df4d8e80eeaf7676965f3e9922102f44ad8004f9a9daa9a6b996ca2ebb4aa0c5

```md
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
```

## openspec/changes/standardize-sse-mcp-transport/design.md

- Source: openspec/changes/standardize-sse-mcp-transport/design.md
- Lines: 1-67
- SHA256: 09f4c2d42a09516cd4597eb4fee4f0a0f70e7a31bc5fecaf27f9464f41b66f33

```md
## Context

The SSE plugin currently supports MagicAPI-managed SSE datasources and subscriptions. It also has MCP-related code that sends an `endpoint` event and processes JSON-RPC messages, but the message entrypoint is exposed through `SseModule.message(...)` rather than a normal HTTP endpoint. The current flow resembles the older MCP HTTP+SSE transport, while the current MCP standard uses Streamable HTTP with a single endpoint supporting POST and GET.

The implementation must live in the Spring Boot 3 / Java 17 autoconfigure layer and follow the existing MagicAPI plugin style. Existing MagicScript users of `sse.connect`, broadcast APIs, datasource storage, and lifecycle scripts must not be broken by the MCP transport work.

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
```

## openspec/changes/standardize-sse-mcp-transport/tasks.md

- Source: openspec/changes/standardize-sse-mcp-transport/tasks.md
- Lines: 1-31
- SHA256: ec8284a7e70c05c84553c46c9a06c2a16b26ee0f13fcff21cb8a8168e115867c

```md
## 1. Transport Entry Points

- [ ] 1.1 Add a configurable standard MCP endpoint to the SSE plugin auto-configuration.
- [ ] 1.2 Implement POST request handling for JSON-RPC MCP messages with standard headers.
- [ ] 1.3 Implement GET request handling for optional Streamable HTTP SSE streams.
- [ ] 1.4 Update legacy endpoint-event generation so it points to an actual POST route.

## 2. Session and Lifecycle

- [ ] 2.1 Add explicit MCP session state for session id, datasource key, protocol version, lifecycle phase, and optional SSE emitter.
- [ ] 2.2 Enforce initialize-before-tool-use and initialized-notification lifecycle rules.
- [ ] 2.3 Add cleanup for completed, timed-out, and invalid MCP sessions without breaking ordinary SSE sessions.

## 3. Protocol Validation and Security

- [ ] 3.1 Validate `Accept`, `MCP-Protocol-Version`, and `Mcp-Session-Id` for standard MCP requests.
- [ ] 3.2 Add trusted Origin validation for MCP endpoints.
- [ ] 3.3 Return standards-compatible JSON-RPC errors for malformed requests, unknown methods, unknown tools, and invalid lifecycle state.

## 4. MagicAPI Tool Adapter

- [ ] 4.1 Keep `tools/list` backed by configured MagicAPI API groups and return valid MCP tool schemas.
- [ ] 4.2 Keep `tools/call` backed by MagicAPI script execution and return MCP content blocks.
- [ ] 4.3 Ensure tool invocation cannot bypass datasource MCP enablement or lifecycle checks.

## 5. Tests and Documentation

- [ ] 5.1 Add tests for standard initialize, initialized notification, tools/list, and tools/call flows.
- [ ] 5.2 Add tests for invalid headers, untrusted origins, unknown sessions, unknown methods, and unknown tools.
- [ ] 5.3 Add tests for legacy endpoint-event compatibility.
- [ ] 5.4 Update plugin documentation or sample notes for endpoint paths, headers, sessions, origins, and legacy mode.
```

## openspec/changes/standardize-sse-mcp-transport/specs/mcp-transport/spec.md

- Source: openspec/changes/standardize-sse-mcp-transport/specs/mcp-transport/spec.md
- Lines: 1-70
- SHA256: 960cab15ee9c2ca6b2259492394bbf92db19420a8a41e222688befb3af64806f

```md
## ADDED Requirements

### Requirement: Streamable HTTP MCP endpoint
The system SHALL expose a configurable MCP Streamable HTTP endpoint for the SSE plugin. The endpoint MUST support standard JSON-RPC POST requests and MUST support GET for server-to-client SSE streaming when the client requests `text/event-stream`.

#### Scenario: JSON-RPC initialize over POST
- **WHEN** a client sends a valid `initialize` JSON-RPC request to the MCP endpoint with an acceptable `Accept` header
- **THEN** the system returns a JSON-RPC response containing `protocolVersion`, `capabilities`, and `serverInfo`

#### Scenario: SSE stream over GET
- **WHEN** a client sends GET to the MCP endpoint with `Accept: text/event-stream`
- **THEN** the system opens an SSE stream or returns a standards-compatible rejection if streaming is disabled by configuration

### Requirement: MCP transport header validation
The system SHALL validate MCP HTTP transport headers for standard-mode requests. POST requests MUST accept `application/json` and `text/event-stream` response types, and requests after initialization MUST honor the negotiated `MCP-Protocol-Version` and `Mcp-Session-Id` semantics.

#### Scenario: Missing unacceptable Accept header
- **WHEN** a client sends a standard MCP POST request without an acceptable response media type
- **THEN** the system rejects the request with an HTTP client error and does not execute any MagicAPI tool

#### Scenario: Unknown session id
- **WHEN** a client sends a request with an unknown `Mcp-Session-Id`
- **THEN** the system rejects the request with an HTTP client error and does not create a new implicit session

### Requirement: MCP lifecycle enforcement
The system SHALL enforce the MCP lifecycle for each session. A session MUST process `initialize` before tool operations, and MUST record `notifications/initialized` before treating the session as ready for normal tool requests.

#### Scenario: Tool request before initialize
- **WHEN** a client sends `tools/list` or `tools/call` before a successful `initialize`
- **THEN** the system returns a JSON-RPC error response and does not execute any MagicAPI script

#### Scenario: Initialized notification completes lifecycle
- **WHEN** a client sends `notifications/initialized` after a successful `initialize`
- **THEN** the system marks the session ready without sending a JSON-RPC response body for the notification

### Requirement: MagicAPI tools adapter
The system SHALL expose configured MagicAPI APIs as MCP tools. `tools/list` MUST return tool definitions with `name`, `description`, and `inputSchema`; `tools/call` MUST execute the selected MagicAPI API with supplied arguments and return MCP content blocks.

#### Scenario: List tools from configured group
- **WHEN** an initialized client sends `tools/list`
- **THEN** the system returns only the MagicAPI APIs allowed by the MCP datasource configuration

#### Scenario: Call known tool
- **WHEN** an initialized client sends `tools/call` for a known MagicAPI API with valid arguments
- **THEN** the system executes the API and returns a result with `content` and `isError`

#### Scenario: Call unknown tool
- **WHEN** an initialized client sends `tools/call` for an unknown tool name
- **THEN** the system returns a JSON-RPC error and does not execute any script

### Requirement: Legacy HTTP+SSE compatibility
The system SHALL preserve the existing legacy HTTP+SSE-style connection flow for MCP-enabled SSE datasources where feasible. The legacy SSE `endpoint` event MUST point to a real POST endpoint that shares the same JSON-RPC processing behavior as the standard endpoint.

#### Scenario: Legacy endpoint event points to working POST route
- **WHEN** a legacy SSE MCP client connects to an MCP-enabled datasource
- **THEN** the first MCP endpoint event contains a POST URL that the server actually handles

### Requirement: MCP transport security
The system SHALL validate request origins for MCP HTTP transports and MUST provide configuration for trusted origins. The server MUST reject untrusted origins before processing JSON-RPC messages.

#### Scenario: Untrusted origin rejected
- **WHEN** a client sends an MCP request with an untrusted `Origin`
- **THEN** the system rejects the request and does not execute lifecycle or tool logic

### Requirement: Existing SSE behavior compatibility
The system SHALL keep existing SSE datasource, subscription, lifecycle script, and broadcast behavior compatible for non-MCP usage.

#### Scenario: Ordinary broadcast remains available
- **WHEN** existing MagicScript calls `sse.broadcastMessageToClient` or `sse.broadcastDistributedMessageToAllClient`
- **THEN** the system delivers messages using the existing SSE datasource/session behavior
```

