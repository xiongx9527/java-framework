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

- `Accept`
- `MCP-Protocol-Version`
- `Mcp-Session-Id`
- `Origin`

Alternative considered: use `${...}` placeholders in `@RequestHeader`. Reading from the request is clearer, testable, and avoids relying on annotation placeholder edge cases.

### 7. README is part of the deliverable

The plugin README must be detailed enough for application teams to integrate the MCP plugin without reading implementation code. It will cover dependency setup, configuration, OAuth/Sa-Token, console resource maintenance, API tool scope, event behavior, Redis cluster deployment, custom header names, request examples, and troubleshooting.

## Risks / Trade-offs

- API tool exposure can leak business operations if defaults are too broad → default to conservative behavior and document explicit opt-in.
- Redis cluster support can introduce partial behavior when Redis exists but `magic-api.plugin.cluster.enabled` is false → only advertise cluster behavior when the cluster plugin is active.
- SSE emitters are local JVM objects and cannot be shared across nodes → broadcast events across Redis, but send only from the node that owns the connection.
- Prompt rendering can be abused if arbitrary script execution is allowed → keep the first version to structured prompt messages and simple argument interpolation unless existing MagicAPI script execution is explicitly required by the stored prompt type.
- Console build tooling may require Node dependencies during Maven build → follow the existing plugin console packaging pattern and avoid adding new frontend dependencies.

## Migration Plan

1. Existing provider-based MCP tools/resources/prompts continue to work.
2. Existing clients keep using current header names unless the application changes `magic-api.plugin.mcp.headers`.
3. Existing single-node deployments continue using in-memory behavior.
4. Applications that need cluster behavior must include Redis support and enable `magic-api.plugin.cluster.enabled=true`.
5. New console-managed MCP resources are added as new MagicAPI resource folders and do not migrate existing datasource resources.

Rollback is limited to disabling `magic-api.plugin.mcp.enabled` or removing the MCP plugin starter. Saved `mcp-resource` and `mcp-prompt` files are isolated resource folders and do not affect existing API/function/datasource resources.
