---
comet_change: add-mcp-resources-prompts-console-cluster
role: technical-design
canonical_spec: openspec
---

# MCP Resources Prompts Console Cluster Design

## Context

The MCP plugin already has standalone protocol handlers, provider SPI, Sa-Token OAuth2 integration, and a Streamable HTTP endpoint. The missing pieces are operational: users cannot maintain MCP resources and prompts from the MagicAPI console, API exposure as tools is not scoped, server-to-client notifications are only shaped as maps but not delivered, controller headers are hard-coded, and session state is local only.

The local `config` plugin provides the pattern for console-managed resource types. It registers a console bundle, a resource type, `MagicResourceStorage`, and a dynamic registry that reacts to MagicAPI file events. MCP resources and prompts should follow this pattern and stay separate from datasource resources.

Cluster behavior follows the project rule: Redis plus `magic-api.plugin.cluster.enabled=true` is the boundary for cluster mode. Without Redis-backed cluster support, MCP remains single-node with in-memory session and local SSE emitter state.

## Architecture

### Resource And Prompt Model

Add two MagicAPI resource folders:

- `mcp-resource`, displayed in the console as `resources`
- `mcp-prompt`, displayed in the console as `prompts`

Each folder gets:

- entity class extending `PathMagicEntity`
- storage class extending `AbstractPathMagicResourceStorage`
- dynamic registry listening to file/group events
- console-backed provider implementing MCP provider SPI

The resource provider translates saved `mcp-resource` files into `McpResourceDefinition`, `McpResourceReadResult`, and optional templates. The prompt provider translates saved `mcp-prompt` files into `McpPromptDefinition` and `McpPromptResult`.

Provider SPI remains additive. Existing application-defined `McpResourceProvider`, `McpPromptProvider`, and `McpToolProvider` beans continue to work.

### API Tool Scope

Extend `MagicAPIMcpProperties` with tool exposure settings. API-backed tools are disabled by default and become visible only when all configured checks pass:

- API exposure enabled
- included group/path/method match
- excluded path does not match
- explicit opt-in metadata is present when required

Function-backed tools remain supported and gain equivalent enable/scope controls where practical. This keeps business APIs from being accidentally exposed to MCP clients.

### Transport Headers

Move fixed header names out of `@RequestHeader`. `MagicMcpController` reads `HttpServletRequest` directly using configured names:

- accept header
- protocol version header
- session id header
- origin header

Default names remain `Accept`, `MCP-Protocol-Version`, `Mcp-Session-Id`, and `Origin`, so existing clients keep working.

### Notifications And SSE

`GET /mcp` becomes a real MCP server-to-client SSE stream. The transport registers a local `SseEmitter` by session id and removes it on completion, timeout, error, delete, and session expiry.

An MCP notification service sends JSON-RPC notification objects over the session emitter:

- `notifications/tools/list_changed`
- `notifications/resources/list_changed`
- `notifications/resources/updated`
- `notifications/prompts/list_changed`

`resources/updated` is filtered through the resource subscription registry. List-change notifications are sent to sessions that negotiated the related capability.

### Redis Cluster Mode

Single-node mode uses local memory for session state and local emitters. Redis-backed cluster mode is active only when Redis support is present and the existing MagicAPI cluster plugin is enabled.

In cluster mode:

- session data moves behind a repository abstraction with Redis-capable implementation
- local file events publish MCP change events through Redis-backed cluster infrastructure
- nodes ignore self-loop duplicates
- only the node holding a local SSE emitter sends to that client

This avoids pretending a non-Redis deployment is clustered and avoids trying to move Servlet emitters across JVMs.

### Console Bundle

Add `src/console` under `fw-plus-magicapi-plugin-mcp` following existing plugin Vite structure. The bundle registers:

- left resources for `mcp-resource` and `mcp-prompt`
- i18n entries
- icons
- services
- toolbar editor panels

The starter POM builds/packages the bundle, and `MagicAPIMcpConfiguration.plugin()` references the generated IIFE file.

## Testing Strategy

Backend tests cover:

- storage validation for resources and prompts
- provider list/read/get translation
- API tool scope matching and rejected calls
- configurable header lookup
- session repository lifecycle
- notification payloads, capability filtering, subscription filtering, and emitter cleanup
- cluster-disabled fallback and cluster-event dispatch with fakes/mocks

Frontend verification covers:

- MCP console bundle builds
- generated bundle name matches `MagicAPIMcpConfiguration.plugin()`
- resource services declare expected language/default script/metadata merge behavior

Documentation verification covers README completeness for setup, OAuth, resources, prompts, API tool scope, custom headers, events, Redis cluster mode, validation examples, and troubleshooting.

## Risks

API exposure is the highest-risk surface. Defaults must be conservative and README examples must show explicit allow lists.

Prompt rendering should avoid arbitrary script execution unless a stored prompt type explicitly requires it. The first implementation should prefer structured messages and simple argument interpolation.

Cluster event delivery must avoid duplicate notifications. Events should carry enough origin information to ignore self-loop messages while still allowing other nodes to notify their local clients.

Console implementation should reuse existing plugin dependencies and build patterns to avoid frontend dependency churn.
