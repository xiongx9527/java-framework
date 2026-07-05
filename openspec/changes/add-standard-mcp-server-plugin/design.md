## Context

The SSE plugin has been removed and must not be restored. The remaining `ssehttp` module is a business MagicAPI module and must stay independent from MCP. The new MCP support therefore needs to be delivered as a standalone MagicAPI plugin that follows the existing `fw-plus-magicapi-plugin-*` module style while exposing the official MCP server protocol over Streamable HTTP.

The project already contains Sa-Token and Sa-Token OAuth2 integration points. MCP authorization should reuse that infrastructure instead of reimplementing an authorization server inside the MCP plugin. The plugin is responsible for MCP protected-resource behavior and endpoint authorization; token issuing, login, authorization code handling, and refresh token handling remain owned by Sa-Token OAuth2.

## Goals / Non-Goals

**Goals:**
- Provide a standalone `fw-plus-magicapi-plugin-mcp` starter and autoconfiguration.
- Implement standard MCP Streamable HTTP transport for server endpoints, including POST, GET, DELETE, SSE responses, session lifecycle, and header validation.
- Implement JSON-RPC 2.0 dispatch for MCP base protocol and server features.
- Support tools, resources, resource templates, prompts, completion, logging, resource subscriptions, cancellation, progress, and list change notifications.
- Support server-initiated requests to client capabilities when negotiated, including roots, sampling, and elicitation.
- Protect MCP endpoints with Sa-Token OAuth2 Bearer token validation and capability-specific scopes.
- Manage custom MCP content as two standard categories: `resources` and `prompts`.
- Avoid datasource-style resource management and avoid any SSE plugin coupling.

**Non-Goals:**
- Do not rebuild MagicAPI's whole plugin architecture.
- Do not restore `fw-plus-magicapi-plugin-sse`.
- Do not modify or remove `ssehttp`.
- Do not introduce `mcp-datasource`, `resources-datasource`, `prompts-datasource`, or any `*-datasource` model for MCP.
- Do not implement a standalone OAuth authorization server in this plugin.
- Do not require Spring Security as the primary auth stack.

## Decisions

### Decision 1: Standalone MagicAPI MCP plugin

Create `fw-plus-magicapi-plugin-mcp` under the existing MagicAPI plugin starter aggregator, with autoconfiguration under `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp`. The plugin registers its own controller, services, providers, and MagicAPI plugin metadata.

Alternative considered: attach MCP to `ssehttp` or restore the SSE plugin. This is rejected because MCP and business SSE have different protocols, sessions, and lifecycle semantics, and the user explicitly removed the SSE plugin while preserving `ssehttp`.

### Decision 2: Streamable HTTP as the only standard transport

Expose a configurable MCP endpoint, defaulting to `/mcp`, that implements Streamable HTTP. POST handles JSON-RPC requests, notifications, and responses. GET opens an SSE stream when supported and requested. DELETE terminates the session when a session id is provided.

Alternative considered: keep old HTTP+SSE endpoint-event behavior. This is rejected because it belongs to the deleted SSE plugin and is not needed for a standalone MCP server.

### Decision 3: Explicit MCP session model

Use an MCP-specific session service that tracks session id, negotiated protocol version, client capabilities, lifecycle state, authenticated principal, active SSE emitter, last activity, subscriptions, and in-flight requests. Expired or deleted sessions return standard transport errors instead of silently creating new sessions.

Alternative considered: reuse servlet sessions or Sa-Token sessions directly. This is rejected because MCP session ids are protocol-level transport state, while Sa-Token sessions are authentication state.

### Decision 4: Sa-Token OAuth2 protected resource integration

MCP endpoint access is protected by Sa-Token OAuth2 Bearer tokens. The plugin validates token presence, login state, and scopes/permissions before dispatching feature logic. It also returns standard `WWW-Authenticate` information and exposes OAuth protected resource metadata for clients.

Alternative considered: implement OAuth authorization code/token endpoints inside the MCP plugin. This is rejected as duplicated authentication infrastructure and would conflict with the project's existing Sa-Token ownership.

### Decision 5: Resources and prompts are first-class MCP content

Expose custom content through standard MCP categories: `resources` and `prompts`. Internally, the plugin may load content from static definitions, MagicAPI scripts, or Java provider beans, but the public MCP contract remains `resources/list`, `resources/read`, `resources/templates/list`, `prompts/list`, and `prompts/get`.

Alternative considered: create datasource-style MCP resources. This is rejected because the requirement explicitly excludes datasource models and because resources/prompts are protocol content, not connection pools.

### Decision 6: Provider SPI instead of hard-coded content sources

Define Java extension points for tools, resources, prompt providers, completion, logging, and client request execution. MagicAPI-backed default providers can be added without preventing business applications from registering their own providers.

Alternative considered: only expose MagicAPI APIs and files. This is rejected because MCP needs typed protocol behavior and the framework should support business-specific content without editing the plugin core.

## Risks / Trade-offs

- Broad MCP surface area may become large for one change -> split implementation tasks by protocol layer, feature provider, auth, and integration tests.
- Sa-Token OAuth2 behavior can vary by application configuration -> keep auth behind a small MCP auth service and test unauthorized, forbidden, and success paths with focused stubs.
- Dynamic MagicAPI content can change while clients are connected -> use provider events to drive `list_changed` notifications and keep provider reads authoritative.
- Server-initiated roots/sampling/elicitation need active client sessions -> only enable these calls after client capabilities are negotiated and fail with protocol errors when unsupported.
- Resource and prompt content can leak sensitive data -> enforce scope checks, URI validation, output limits, and content filtering before returning data.

## Migration Plan

1. Add the OpenSpec capability and implement the standalone plugin module wiring.
2. Add transport, JSON-RPC, session, auth, and metadata foundation.
3. Add tools, resources, prompts, templates, completion, logging, subscriptions, and notifications.
4. Add client-capability request support for roots, sampling, and elicitation.
5. Add tests for protocol compliance, auth decisions, provider behavior, and Maven module wiring.
6. Rollback strategy: remove the MCP starter dependency or disable `magic-api.plugin.mcp.enabled`; no SSE or datasource migration is required.

## Open Questions

- Whether the default endpoint should be `/mcp` or prefixed by the configured MagicAPI web base path.
- Whether default resource/prompt definitions should be loaded from MagicAPI resource storage directories, Spring configuration, or both.
- Exact Sa-Token scope naming can start with `mcp:tools`, `mcp:resources`, `mcp:prompts`, `mcp:logging`, `mcp:sampling`, `mcp:roots`, and `mcp:elicitation`, but should remain configurable.
