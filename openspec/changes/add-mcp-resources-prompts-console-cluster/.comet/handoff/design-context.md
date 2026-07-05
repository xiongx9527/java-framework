# Comet Design Handoff

- Change: add-mcp-resources-prompts-console-cluster
- Phase: design
- Mode: compact
- Context hash: 9a809363fbe94cacbd914039c3198ab7261f9794c659fcab56f478d42274d408

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/add-mcp-resources-prompts-console-cluster/proposal.md

- Source: openspec/changes/add-mcp-resources-prompts-console-cluster/proposal.md
- Lines: 1-39
- SHA256: c694d3d42fee54bdb849b960c00478a28fec0f6ee5e85173101f7dd42cdd5cca

```md
## Why

The current MCP plugin exposes the protocol endpoints and provider SPI, but it has no MagicAPI console for maintaining MCP resources and prompts, no configurable API tool exposure boundary, and no working notification path for list changes. In clustered deployments its in-memory session and empty SSE stream also make MCP clients unreliable when traffic lands on different nodes.

## What Changes

- Add MagicAPI console integration for MCP `resources` and `prompts` as first-class resource types, not datasource resources.
- Persist MCP resources and prompts through MagicAPI resource storage and expose them through `McpResourceProvider` and `McpPromptProvider`.
- Add configurable MagicAPI API exposure as MCP tools, including include/exclude scope controls and explicit opt-in support.
- Send MCP client notifications when API tools, MCP resources, or MCP prompts change:
  - `notifications/tools/list_changed`
  - `notifications/resources/list_changed`
  - `notifications/resources/updated`
  - `notifications/prompts/list_changed`
- Make `MagicMcpController` request header names configurable while preserving current defaults.
- Add cluster-ready MCP session and SSE notification design so multi-node deployments can keep client sessions and change events coherent.
- Replace the current MCP README with a detailed接入说明 covering dependencies, configuration, OAuth, resources, prompts, API tools, events, cluster deployment, validation examples, and troubleshooting.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `standard-mcp-server-plugin`: Extend the MCP plugin requirements for console-managed resources/prompts, API tool scope controls, configurable request headers, client change notifications, cluster support, and detailed integration documentation.

## Impact

- Affected backend modules:
  - `fw-plus-boot-project/fw-plus-boot-autoconfigure/.../magicapi/plugin/mcp/**`
  - existing MagicAPI resource storage/dynamic registry integration points.
- Affected starter module:
  - `fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-mcp/**`
- Affected documentation:
  - `fw-plus-magicapi-plugin-mcp/README.md`
- No datasource-based MCP resource model will be introduced.
- No restoration or reuse of the deleted SSE plugin is in scope; MCP uses its own transport and may reuse existing MagicAPI cluster notification infrastructure where appropriate.
```

## openspec/changes/add-mcp-resources-prompts-console-cluster/design.md

- Source: openspec/changes/add-mcp-resources-prompts-console-cluster/design.md
- Lines: 1-108
- SHA256: bfe6f3512093cdcaa5e1c84c55ce57391f555d40da4d5f97ead99fc6b8964df2

[TRUNCATED]

```md
## Context

The MCP plugin currently provides a standalone MagicAPI plugin and JSON-RPC handlers for tools, resources, prompts, completion, logging, and client capabilities. Its content model is still provider-only: resources and prompts cannot be maintained from the MagicAPI console, and the built-in MagicAPI tool provider exposes only functions without API scope controls.

The existing `config` plugin is the closest local pattern for this work. It registers a plugin bundle, a MagicAPI resource type, a `MagicResourceStorage`, and a dynamic registry that reacts to MagicAPI file events. MCP resources and prompts should follow that pattern rather than becoming datasource resources.

Cluster behavior in this project is Redis-based. A deployment is considered clustered only when Redis support is present and `magic-api.plugin.cluster.enabled=true` activates the existing MagicAPI cluster plugin. The MCP plugin must remain usable in single-node deployments without Redis, and must only enable cross-node session/event behavior when Redis cluster infrastructure is available.

## Goals / Non-Goals

**Goals:**

- Add MagicAPI console resource types for MCP `resources` and `prompts`.
- Persist those resources through MagicAPI storage and expose them through MCP providers.
- Allow MagicAPI `api` resources to be exposed as MCP tools only within configured scope.
- Send real MCP JSON-RPC notifications to connected clients when tools, resources, or prompts change.
- Make MCP transport header names configurable while keeping current defaults.
- Support Redis-backed cluster behavior when the project introduces Redis and enables the existing cluster plugin.
- Provide a detailed MCP README that can be used as a practical integration guide.

**Non-Goals:**

- Do not add `mcp-datasource`, `resources-datasource`, `prompts-datasource`, or any datasource-style MCP content model.
- Do not restore the deleted SSE plugin and do not reuse business `ssehttp` as MCP transport.
- Do not make non-Redis deployments behave as clustered deployments.
- Do not rewrite MagicAPI core resource APIs beyond the extension points needed by the MCP plugin.

## Decisions

### 1. Use MagicAPI resource types for MCP resources and prompts

MCP will add two resource folders:

- `mcp-resource`
- `mcp-prompt`

The console will display them as `resources` and `prompts`. Back-end classes will mirror the `config` plugin pattern:

- entity classes extending `PathMagicEntity`
- `AbstractPathMagicResourceStorage` implementations
- dynamic registries listening for file/group events
- provider implementations that translate saved entities to MCP provider results

Alternative considered: manage MCP resources and prompts only through Spring Bean providers. That keeps the current SPI simple but does not satisfy the requirement to add and maintain resources in the MagicAPI console.

### 2. Keep provider SPI and add console-backed providers

Existing `McpResourceProvider`, `McpPromptProvider`, and custom application beans remain supported. Console-backed providers are additive and should be ordered predictably. This keeps extension compatibility and allows Java-coded providers for advanced use cases.

### 3. Make API tool exposure opt-in and scoped

MagicAPI `api` resources can be exposed as MCP tools only when enabled by configuration. Scope controls include groups, paths, methods, excludes, and explicit opt-in metadata. The default must not accidentally expose every business API.

Function tools remain supported, but should also be subject to an enabled flag and scope configuration where practical.

### 4. Send client notifications through MCP-owned SSE transport

The current `GET /mcp` path returns an empty `SseEmitter`. It will become the outbound transport for JSON-RPC server-to-client notifications and requests. A transport service will map MCP sessions to active local emitters and send:

- `notifications/tools/list_changed`
- `notifications/resources/list_changed`
- `notifications/resources/updated`
- `notifications/prompts/list_changed`

Resource subscription state remains separate from list-change notification state. `resources/updated` is sent only when a concrete URI has subscribed sessions.

### 5. Redis is the cluster boundary

Single-node mode uses in-memory session and local emitter registries. Redis-backed cluster mode is enabled only when Redis and the existing MagicAPI cluster plugin are active. In that mode:

- session data uses a Redis-capable repository or a bean supplied by the Redis-enabled configuration
- list-change and resource-update events are broadcast through Redis-backed cluster notification infrastructure
- the node that owns the client's SSE connection performs the actual SSE send

Alternative considered: invent a separate MCP cluster bus. That would duplicate existing project cluster infrastructure and create a second operational model.

### 6. Read configurable headers from `HttpServletRequest`

`MagicMcpController` will stop binding fixed MCP header names through `@RequestHeader(value = "...")`. It will read headers by names configured under `magic-api.plugin.mcp.headers`, with defaults matching current behavior:

```

Full source: openspec/changes/add-mcp-resources-prompts-console-cluster/design.md

## openspec/changes/add-mcp-resources-prompts-console-cluster/tasks.md

- Source: openspec/changes/add-mcp-resources-prompts-console-cluster/tasks.md
- Lines: 1-68
- SHA256: b685fb969363b7618cea19285a53ecc339a59959a05ea897983ed8a65d8e47d3

```md
## 1. Backend Resource Model

- [ ] 1.1 Add MCP resource and prompt entity classes for `mcp-resource` and `mcp-prompt` MagicAPI files.
- [ ] 1.2 Add MagicAPI resource storage classes with validation for resource URI, MIME type, prompt name, arguments, and message content.
- [ ] 1.3 Add dynamic registries that listen to `mcp-resource` and `mcp-prompt` file/group events.
- [ ] 1.4 Add console-backed `McpResourceProvider` and `McpPromptProvider` implementations.
- [ ] 1.5 Register storage, registry, and providers in `MagicAPIMcpConfiguration`.
- [ ] 1.6 Add unit tests for storage validation and provider list/read/get behavior.

## 2. API And Function Tool Scope

- [ ] 2.1 Extend `MagicAPIMcpProperties` with tool exposure configuration for functions and APIs.
- [ ] 2.2 Add API scope matcher for enabled flag, include groups, include paths, exclude paths, methods, and explicit opt-in metadata.
- [ ] 2.3 Extend `MagicApiMcpToolProvider` to list and call scoped MagicAPI API resources without exposing disabled APIs.
- [ ] 2.4 Preserve existing function tool support and apply function scope configuration where applicable.
- [ ] 2.5 Add tests for disabled-by-default API exposure, included APIs, excluded APIs, and rejected calls.

## 3. Transport Headers And Sessions

- [ ] 3.1 Add configurable MCP header names with defaults for accept, protocol version, session id, and origin.
- [ ] 3.2 Refactor `MagicMcpController` to read configured headers from `HttpServletRequest`.
- [ ] 3.3 Introduce `McpSessionRepository` and move existing in-memory session map behind the repository abstraction.
- [ ] 3.4 Keep local in-memory session behavior for non-Redis or non-cluster deployments.
- [ ] 3.5 Add tests for custom header names and repository-backed session lifecycle.

## 4. Client Notifications And SSE

- [ ] 4.1 Replace the empty GET SSE behavior with a local MCP SSE emitter registry keyed by session id.
- [ ] 4.2 Add an MCP notification service that sends JSON-RPC notifications and server-initiated requests through the registered emitters.
- [ ] 4.3 Wire tool, resource, and prompt file events to `tools/list_changed`, `resources/list_changed`, `resources/updated`, and `prompts/list_changed`.
- [ ] 4.4 Ensure `resources/updated` is sent only to sessions subscribed to the changed resource URI.
- [ ] 4.5 Release emitters when sessions are deleted, expired, completed, or fail.
- [ ] 4.6 Add tests for notification payloads, subscription filtering, and emitter cleanup.

## 5. Redis Cluster Integration

- [ ] 5.1 Detect Redis-backed cluster mode only when Redis support is present and `magic-api.plugin.cluster.enabled=true`.
- [ ] 5.2 Add Redis-capable MCP session/event integration that is active only in cluster mode.
- [ ] 5.3 Broadcast MCP change events in cluster mode while preventing duplicate self-loop delivery.
- [ ] 5.4 Ensure only nodes with local connected client SSE emitters send notifications to those clients.
- [ ] 5.5 Add tests for cluster-disabled fallback and cluster-event dispatch behavior using fakes/mocks.

## 6. MagicAPI Console Plugin

- [ ] 6.1 Add `src/console` for the MCP plugin following the `config` plugin build pattern.
- [ ] 6.2 Register `mcp-resource` and `mcp-prompt` resource entries with icons, i18n, services, and editor toolbars.
- [ ] 6.3 Add Vue editor panels for MCP resource metadata/content and prompt metadata/arguments/messages.
- [ ] 6.4 Configure the MCP starter POM to build and package the console bundle.
- [ ] 6.5 Update `MagicAPIMcpConfiguration.plugin()` to expose the MCP console bundle.
- [ ] 6.6 Build the MCP console bundle and verify generated resources are packaged like existing plugins.

## 7. Documentation

- [ ] 7.1 Rewrite `fw-plus-magicapi-plugin-mcp/README.md` with dependency and enablement instructions.
- [ ] 7.2 Document Sa-Token OAuth2 protected resource setup, scopes, and token validation extension points.
- [ ] 7.3 Document `resources` and `prompts` console usage and file semantics.
- [ ] 7.4 Document API/function tool scope configuration and safe defaults.
- [ ] 7.5 Document custom MCP header configuration.
- [ ] 7.6 Document SSE notifications, client refresh behavior, and Redis-only cluster deployment.
- [ ] 7.7 Add curl-style validation examples and troubleshooting guidance.

## 8. Verification

- [ ] 8.1 Run MCP-focused unit tests in `fw-plus-boot-autoconfigure`.
- [ ] 8.2 Run the MCP plugin starter Maven build with skipped tests if frontend packaging requires it.
- [ ] 8.3 Run OpenSpec validation for `add-mcp-resources-prompts-console-cluster`.
- [ ] 8.4 Run `git diff --check`.
- [ ] 8.5 Review the implementation against every requirement in the delta spec before archive.
```

## openspec/changes/add-mcp-resources-prompts-console-cluster/specs/standard-mcp-server-plugin/spec.md

- Source: openspec/changes/add-mcp-resources-prompts-console-cluster/specs/standard-mcp-server-plugin/spec.md
- Lines: 1-150
- SHA256: 1a43388bd8d6f13c25faba78141292887b81472c446202f1368ab1d09668afd4

[TRUNCATED]

```md
## ADDED Requirements

### Requirement: Console-managed MCP resources and prompts
The system SHALL provide MagicAPI console resource types for MCP resources and MCP prompts, persisted through MagicAPI resource storage without using datasource resource models.

#### Scenario: Add MCP resource from console
- **WHEN** an authorized MagicAPI console user creates an item under the MCP `resources` resource type
- **THEN** the system persists it as an `mcp-resource` MagicAPI resource and makes it available to `resources/list` and `resources/read`

#### Scenario: Add MCP prompt from console
- **WHEN** an authorized MagicAPI console user creates an item under the MCP `prompts` resource type
- **THEN** the system persists it as an `mcp-prompt` MagicAPI resource and makes it available to `prompts/list` and `prompts/get`

#### Scenario: MCP console resources are not datasource
- **WHEN** MCP resources or prompts are saved from the console
- **THEN** the system stores them outside any `*-datasource` folder and does not create datasource runtime objects for them

### Requirement: Configurable MagicAPI API tool exposure
The system SHALL expose MagicAPI API resources as MCP tools only when enabled and only within the configured API scope.

#### Scenario: API tool exposure disabled by default
- **WHEN** `magic-api.plugin.mcp.tools.api.enabled` is false or absent
- **THEN** `tools/list` does not include MagicAPI API resources as MCP tools

#### Scenario: API tool exposure includes allowed scope
- **WHEN** API tool exposure is enabled and an API matches configured include groups, include paths, methods, and explicit opt-in rules
- **THEN** `tools/list` includes that API as an MCP tool definition

#### Scenario: API tool exposure excludes blocked scope
- **WHEN** an API matches configured exclude paths or does not satisfy configured groups, paths, methods, or explicit opt-in rules
- **THEN** `tools/list` does not include that API and `tools/call` does not execute it by MCP tool name

### Requirement: Configurable MCP transport header names
The system SHALL allow MCP transport request header names to be configured while preserving the current default header names.

#### Scenario: Default header names
- **WHEN** no MCP header override is configured
- **THEN** the MCP controller reads `Accept`, `MCP-Protocol-Version`, `Mcp-Session-Id`, and `Origin`

#### Scenario: Custom header names
- **WHEN** MCP header names are configured under `magic-api.plugin.mcp.headers`
- **THEN** the MCP controller reads those configured headers for accept, protocol version, session id, and origin handling

### Requirement: Redis-backed MCP cluster mode
The system SHALL treat MCP as clustered only when Redis support is present and the existing MagicAPI cluster plugin is enabled.

#### Scenario: Single-node mode without Redis cluster
- **WHEN** Redis cluster support is absent or `magic-api.plugin.cluster.enabled` is not true
- **THEN** the MCP plugin uses local in-memory session and local event delivery behavior

#### Scenario: Cluster mode with Redis
- **WHEN** Redis support is present and `magic-api.plugin.cluster.enabled=true`
- **THEN** the MCP plugin uses Redis-backed or Redis-coordinated session and event behavior for MCP cluster deployment

#### Scenario: Cluster event delivered by owning node
- **WHEN** an MCP change event is broadcast in Redis-backed cluster mode
- **THEN** only nodes with matching connected client sessions send the MCP notification over their local SSE emitters

### Requirement: Detailed MCP integration README
The system SHALL include a detailed MCP plugin README that documents practical integration and operation.

#### Scenario: README covers core setup
- **WHEN** a developer opens the MCP plugin README
- **THEN** it documents dependency usage, enabling configuration, endpoint configuration, Sa-Token OAuth2 configuration, and custom transport headers

#### Scenario: README covers resources prompts and tools
- **WHEN** a developer opens the MCP plugin README
- **THEN** it documents how to maintain MCP resources and prompts in the MagicAPI console and how to configure MagicAPI API tool exposure scope

#### Scenario: README covers runtime operations
- **WHEN** a developer opens the MCP plugin README
- **THEN** it documents client event behavior, Redis cluster requirements, validation examples, and troubleshooting steps

## MODIFIED Requirements

### Requirement: Streamable HTTP transport
The system SHALL expose a configurable MCP Streamable HTTP endpoint that supports POST, GET, and DELETE according to negotiated session behavior, and GET SHALL maintain an MCP-owned server-to-client SSE stream for notifications and server-initiated requests when streaming is enabled.

#### Scenario: Default endpoint path
- **WHEN** the MCP plugin starts without an explicit endpoint override
```

Full source: openspec/changes/add-mcp-resources-prompts-console-cluster/specs/standard-mcp-server-plugin/spec.md

