# Comet Design Handoff

- Change: add-standard-mcp-server-plugin
- Phase: design
- Mode: compact
- Context hash: 94b8021ae883193e70eea55cff1fc24e6879d46141a89abbabc6068083217afa

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/add-standard-mcp-server-plugin/proposal.md

- Source: openspec/changes/add-standard-mcp-server-plugin/proposal.md
- Lines: 1-28
- SHA256: e50c2079698344925a94115e048d014307b0132e76e0d50469f9ce50febdef17

```md
## Why

MagicAPI needs a standards-compliant MCP server integration that can be used by normal MCP clients without depending on the removed SSE plugin or the business `ssehttp` module. The new plugin should expose MagicAPI capabilities, custom resources, and prompts through the official MCP protocol while reusing the project's Sa-Token OAuth2 authentication model.

## What Changes

- Add an independent MagicAPI MCP plugin module, `fw-plus-magicapi-plugin-mcp`.
- Add a standard MCP Streamable HTTP transport with session lifecycle, JSON-RPC dispatching, notifications, cancellation, progress, ping, and strict HTTP/header semantics.
- Add MCP server capabilities for tools, resources, resource templates, prompts, completion, logging, resource subscriptions, and dynamic list change notifications.
- Add server-side support for invoking client capabilities when the client declares them, including roots, sampling, and elicitation.
- Add Sa-Token OAuth2 protected-resource integration for MCP endpoints, including Bearer token validation, scope/permission checks, protected resource metadata, and standard unauthorized/forbidden responses.
- Add custom MCP resource management split into standard `resources` and `prompts`; do not introduce `mcp-datasource` or any `*-datasource` model.
- Keep `ssehttp` unchanged and do not restore the deleted SSE plugin.

## Capabilities

### New Capabilities
- `standard-mcp-server-plugin`: Full standard MCP server support for MagicAPI through an independent plugin, including transport, lifecycle, tools, resources, prompts, completion, logging, client-capability calls, and Sa-Token OAuth2 protection.

### Modified Capabilities

## Impact

- Affected Boot autoconfigure code: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp`.
- Affected plugin starter tree: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-mcp`.
- Affected dependency management and aggregation: Boot BOM, MagicAPI plugin aggregator POM, MagicAPI plugin auto-configuration, and MagicAPI sample dependencies.
- Dependencies: Sa-Token OAuth2 support will be reused for authentication and authorization. No SSE plugin dependency and no datasource-style resource dependency should be introduced.
- Runtime behavior: MCP clients can connect through a standard HTTP endpoint, discover tools/resources/prompts, invoke MagicAPI-backed tools, read custom resources, render prompts, and receive standard protocol notifications according to negotiated capabilities.
```

## openspec/changes/add-standard-mcp-server-plugin/design.md

- Source: openspec/changes/add-standard-mcp-server-plugin/design.md
- Lines: 1-86
- SHA256: a1a6ac326c2e6ee9ed14fb83a34df1cd8a466f0d2e43c48bf56b95a9e45b7844

[TRUNCATED]

```md
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
```

Full source: openspec/changes/add-standard-mcp-server-plugin/design.md

## openspec/changes/add-standard-mcp-server-plugin/tasks.md

- Source: openspec/changes/add-standard-mcp-server-plugin/tasks.md
- Lines: 1-66
- SHA256: 2c3438004afcc43ab695580400c020d8da5a67fe35f4de7a06e88c16f7d47114

```md
## 1. Module Wiring

- [ ] 1.1 Create `fw-plus-magicapi-plugin-mcp` starter module with Maven metadata matching existing MagicAPI plugin starters.
- [ ] 1.2 Add MCP plugin dependency management to the Boot BOM.
- [ ] 1.3 Add MCP plugin module to the MagicAPI plugin aggregator POM.
- [ ] 1.4 Register MCP auto-configuration in `MagicApiPluginAutoConfiguration`.
- [ ] 1.5 Add sample dependency and minimal sample configuration without introducing datasource resources.

## 2. Transport, Session, and JSON-RPC Foundation

- [ ] 2.1 Add MCP configuration properties for enablement, endpoint path, protocol versions, session timeout, SSE stream, request limits, trusted origins, and feature toggles.
- [ ] 2.2 Implement Streamable HTTP controller for POST, GET, DELETE, SSE response handling, and protected resource metadata.
- [ ] 2.3 Implement MCP session service for lifecycle state, protocol version, client capabilities, principal, emitter, subscriptions, in-flight requests, expiration, and deletion.
- [ ] 2.4 Implement JSON-RPC model, parser, dispatcher, request/notification/response distinction, batch handling, and standard error mapping.
- [ ] 2.5 Implement initialize, initialized notification, ping, cancellation, and progress notification handling.
- [ ] 2.6 Add tests for transport headers, HTTP status behavior, session lifecycle, JSON-RPC errors, ping, cancellation, and progress.

## 3. Sa-Token OAuth2 Security

- [ ] 3.1 Implement Sa-Token OAuth2 Bearer token extraction and validation for MCP requests.
- [ ] 3.2 Implement capability-specific scope or permission checks for tools, resources, prompts, logging, roots, sampling, and elicitation.
- [ ] 3.3 Implement standard `401`, `403`, `WWW-Authenticate`, and OAuth protected resource metadata responses.
- [ ] 3.4 Implement Origin validation, URI validation, input validation, output size limits, and output filtering hooks.
- [ ] 3.5 Add tests for missing token, invalid token, insufficient scope, trusted origins, and rejected invalid resource URIs.

## 4. Tools

- [ ] 4.1 Define tool provider SPI and default MagicAPI-backed tool provider.
- [ ] 4.2 Implement `tools/list` with pagination, annotations, input schemas, output schemas, and list change capability declaration.
- [ ] 4.3 Implement `tools/call` with MagicAPI execution, content blocks, structured content, error results, output filtering, and provider authorization.
- [ ] 4.4 Implement `notifications/tools/list_changed` for negotiated sessions.
- [ ] 4.5 Add tests for list pagination, known tool calls, unknown tool calls, structured content, and list change notification gating.

## 5. Resources

- [ ] 5.1 Define resource provider SPI for standard `resources` content, resource templates, reads, subscriptions, and provider events.
- [ ] 5.2 Implement `resources/list` with pagination, metadata, annotations, and authorization.
- [ ] 5.3 Implement `resources/read` for text and blob contents with MIME type and URI validation.
- [ ] 5.4 Implement `resources/templates/list` for parameterized resources.
- [ ] 5.5 Implement `resources/subscribe`, `resources/unsubscribe`, `notifications/resources/updated`, and `notifications/resources/list_changed`.
- [ ] 5.6 Add tests for resource listing, reads, templates, subscriptions, updates, list changes, authorization, and content limits.

## 6. Prompts and Completion

- [ ] 6.1 Define prompt provider SPI for standard `prompts` content, prompt arguments, rendering, and provider events.
- [ ] 6.2 Implement `prompts/list` with pagination, argument metadata, annotations, and authorization.
- [ ] 6.3 Implement `prompts/get` with required argument validation, rendering, multi-message output, and resource references when supplied by providers.
- [ ] 6.4 Implement `notifications/prompts/list_changed` for negotiated sessions.
- [ ] 6.5 Implement `completion/complete` for prompt arguments and resource template arguments.
- [ ] 6.6 Add tests for prompt listing, prompt rendering, validation failures, list change notification gating, and completion results.

## 7. Logging and Client Capabilities

- [ ] 7.1 Implement `logging/setLevel` and `notifications/message` with per-session level state and authorization.
- [ ] 7.2 Implement server-initiated `roots/list` request support gated by negotiated client capabilities.
- [ ] 7.3 Implement server-initiated `sampling/createMessage` request support gated by negotiated client capabilities.
- [ ] 7.4 Implement server-initiated `elicitation/create` request support gated by negotiated client capabilities.
- [ ] 7.5 Add tests for logging level behavior, log notification gating, client request correlation, and unsupported client capability errors.

## 8. Integration and Verification

- [ ] 8.1 Verify no references to deleted SSE plugin packages or `mcp-datasource`/`*-datasource` MCP content models are introduced.
- [ ] 8.2 Run OpenSpec validation for the new change and strict specs.
- [ ] 8.3 Run focused Maven tests for Boot autoconfigure and MagicAPI plugin aggregator modules.
- [ ] 8.4 Run sample module build or test with `-am` to confirm dependency wiring.
- [ ] 8.5 Update task status and prepare the change for Comet design/build transition.
```

## openspec/changes/add-standard-mcp-server-plugin/specs/standard-mcp-server-plugin/spec.md

- Source: openspec/changes/add-standard-mcp-server-plugin/specs/standard-mcp-server-plugin/spec.md
- Lines: 1-198
- SHA256: c8fcbbdd310d41b9ae3d5c2060dfc2514f6d63725ff567ed696ccd170c9c25ee

[TRUNCATED]

```md
## ADDED Requirements

### Requirement: Standalone MCP plugin module
The system SHALL provide an independent MagicAPI MCP plugin module that can be enabled without the deleted SSE plugin and without the business `ssehttp` module.

#### Scenario: Plugin starts without SSE plugin
- **WHEN** an application includes the MCP plugin starter and does not include the deleted SSE plugin
- **THEN** the MCP plugin auto-configuration starts without referencing SSE plugin classes

#### Scenario: SSEHTTP remains independent
- **WHEN** the MCP plugin is enabled or disabled
- **THEN** the existing `ssehttp` MagicAPI module behavior is not changed by the MCP plugin

### Requirement: No datasource model for MCP content
The system SHALL manage MCP tools, resources, and prompts without introducing `mcp-datasource` or any `*-datasource` resource model.

#### Scenario: MCP content is registered without datasource
- **WHEN** MCP resources or prompts are configured
- **THEN** the system registers them through MCP content providers rather than datasource storage

### Requirement: Streamable HTTP transport
The system SHALL expose a configurable MCP Streamable HTTP endpoint that supports POST, GET, and DELETE according to negotiated session behavior.

#### Scenario: Default endpoint path
- **WHEN** the MCP plugin starts without an explicit endpoint override
- **THEN** the system exposes the MCP Streamable HTTP endpoint at `/mcp`

#### Scenario: Configured endpoint path
- **WHEN** `magic-api.plugin.mcp.endpoint` is configured with a valid path
- **THEN** the system exposes the MCP Streamable HTTP endpoint at the configured path

#### Scenario: JSON-RPC request over POST
- **WHEN** a client sends a valid MCP JSON-RPC request to the MCP endpoint with acceptable headers
- **THEN** the system dispatches the request and returns a valid JSON-RPC response or accepted notification response

#### Scenario: SSE stream over GET
- **WHEN** a client sends GET to the MCP endpoint with `Accept: text/event-stream`
- **THEN** the system opens a server-to-client SSE stream or returns a standards-compatible method error when streaming is disabled

#### Scenario: Session termination over DELETE
- **WHEN** a client sends DELETE with a valid `Mcp-Session-Id`
- **THEN** the system terminates that MCP session and releases associated transport resources

### Requirement: MCP header and HTTP semantics
The system SHALL validate MCP transport headers and use standard HTTP statuses for authentication, authorization, unsupported methods, expired sessions, and accepted notifications.

#### Scenario: Missing acceptable response media type
- **WHEN** a POST request does not accept `application/json` or `text/event-stream`
- **THEN** the system rejects the request before dispatching JSON-RPC logic

#### Scenario: Expired session id
- **WHEN** a client sends a request with an expired or unknown `Mcp-Session-Id`
- **THEN** the system returns a client error and does not create an implicit replacement session

#### Scenario: Notification accepted
- **WHEN** a client sends a valid JSON-RPC notification that does not require a response body
- **THEN** the system returns `202 Accepted`

### Requirement: MCP lifecycle and ping
The system SHALL enforce MCP lifecycle ordering and support the base `ping` request.

#### Scenario: Initialize establishes session
- **WHEN** a client sends a valid `initialize` request
- **THEN** the system returns protocol version, server info, capabilities, and an MCP session id when sessions are enabled

#### Scenario: Operations before initialization are rejected
- **WHEN** a client sends feature requests before a successful initialize flow
- **THEN** the system returns a JSON-RPC lifecycle error and does not execute feature logic

#### Scenario: Ping returns empty result
- **WHEN** a client sends `ping`
- **THEN** the system returns a JSON-RPC response with an empty result object

### Requirement: JSON-RPC dispatcher
The system SHALL implement strict JSON-RPC 2.0 request, notification, response, batch, error, and unknown-method handling for MCP messages.

#### Scenario: Unknown method
- **WHEN** a client sends a request for an unsupported method
- **THEN** the system returns JSON-RPC error code `-32601`

```

Full source: openspec/changes/add-standard-mcp-server-plugin/specs/standard-mcp-server-plugin/spec.md

