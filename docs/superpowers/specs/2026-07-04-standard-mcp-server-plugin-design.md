---
comet_change: add-standard-mcp-server-plugin
role: technical-design
canonical_spec: openspec
---

# Standard MCP Server Plugin Design

## Objective

Add an independent MagicAPI MCP plugin that implements a standards-compliant MCP Server without restoring the deleted SSE plugin, without modifying `ssehttp`, and without introducing datasource-style MCP content.

The implementation uses a protocol core plus Provider SPI architecture. Sa-Token OAuth2 supplies authentication and authorization; the MCP plugin only implements the MCP protected-resource behavior around its own endpoints.

## Architecture

```text
MCP HTTP Controller
  -> Transport / Session / JSON-RPC Core
  -> Method Handlers
       tools/*
       resources/*
       prompts/*
       completion/*
       logging/*
       client capability calls
  -> Provider SPI
       McpToolProvider
       McpResourceProvider
       McpPromptProvider
       McpCompletionProvider
  -> Sa-Token OAuth2 Auth Service
```

The controller layer owns HTTP transport semantics: POST, GET, DELETE, SSE stream setup, headers, status codes, and protected resource metadata. The protocol layer owns MCP session state, JSON-RPC parsing, lifecycle, cancellation, progress, and method dispatch. Feature handlers are thin protocol adapters around provider interfaces.

## Module Integration

Create `fw-plus-magicapi-plugin-mcp` under the existing MagicAPI plugin starter aggregator. The autoconfigure implementation lives under:

`com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp`

The plugin is added to the Boot dependency management, MagicAPI plugin aggregator POM, `MagicApiPluginAutoConfiguration`, and the MagicAPI sample dependency set. It must not reference deleted `plugin.sse` classes.

## Transport And Session

The default endpoint is `/mcp`, configurable through `magic-api.plugin.mcp.endpoint`. The endpoint implements Streamable HTTP:

- POST for JSON-RPC requests, notifications, and responses.
- GET for server-to-client SSE streams when negotiated and enabled.
- DELETE for session termination.

MCP sessions are protocol sessions, not Sa-Token sessions. A session records id, protocol version, lifecycle state, client capabilities, authenticated principal, SSE emitter, subscriptions, in-flight requests, last activity, and expiration metadata. Unknown, expired, or deleted session ids are rejected rather than silently replaced.

## JSON-RPC Core

The JSON-RPC core should be deliberately small and testable:

- Request, notification, response, error, and batch models.
- Dispatcher table from method name to handler.
- Standard error mapping for parse error, invalid request, method not found, invalid params, internal error, lifecycle errors, auth errors, and provider errors.
- Notification handling with `202 Accepted` where the MCP HTTP transport requires no response body.

Base methods include `initialize`, `notifications/initialized`, `ping`, `notifications/cancelled`, and `notifications/progress`.

## Sa-Token OAuth2

The MCP plugin uses Sa-Token OAuth2 as the authorization foundation. It does not implement authorization code endpoints, login, access token issuing, refresh token issuing, or OAuth client management.

The MCP auth service handles:

- Bearer token extraction from `Authorization`.
- Sa-Token token/login validation.
- Scope or permission checks per MCP capability.
- `401`, `403`, and `WWW-Authenticate`.
- OAuth protected resource metadata for the MCP endpoint.

Default scopes are configurable and start with:

- `mcp:tools`
- `mcp:resources`
- `mcp:prompts`
- `mcp:logging`
- `mcp:roots`
- `mcp:sampling`
- `mcp:elicitation`

Sa-Token integration should be conditionally loaded by classpath and configuration, so applications without Sa-Token OAuth2 fail clearly when auth is enabled instead of failing with unrelated class errors.

## Provider SPI

MCP content is not a datasource. The public protocol categories are `resources` and `prompts`; tools remain MCP tools. Internal sources are provider implementations.

Suggested interfaces:

```java
public interface McpToolProvider {
    McpPage<McpToolDefinition> listTools(McpProviderContext context, McpPageRequest pageRequest);
    McpToolResult callTool(McpProviderContext context, McpToolCallRequest request);
}

public interface McpResourceProvider {
    McpPage<McpResourceDefinition> listResources(McpProviderContext context, McpPageRequest pageRequest);
    McpResourceReadResult readResource(McpProviderContext context, McpResourceReadRequest request);
    McpPage<McpResourceTemplate> listResourceTemplates(McpProviderContext context, McpPageRequest pageRequest);
}

public interface McpPromptProvider {
    McpPage<McpPromptDefinition> listPrompts(McpProviderContext context, McpPageRequest pageRequest);
    McpPromptResult getPrompt(McpProviderContext context, McpPromptGetRequest request);
}
```

Default providers can include Java Bean providers, static configuration providers, and MagicAPI script-backed providers. Provider output is validated and filtered by the protocol layer before it is returned.

## Feature Handling

Tools support `tools/list`, `tools/call`, `notifications/tools/list_changed`, annotations, input schema, output schema, content blocks, `structuredContent`, and error results.

Resources support `resources/list`, `resources/read`, `resources/templates/list`, `resources/subscribe`, `resources/unsubscribe`, `notifications/resources/updated`, and `notifications/resources/list_changed`.

Prompts support `prompts/list`, `prompts/get`, argument metadata, required argument validation, multi-message output, and `notifications/prompts/list_changed`.

Completion supports `completion/complete` for prompt arguments and resource template arguments.

Logging supports `logging/setLevel` and `notifications/message`, gated by negotiated capabilities and authorization.

Client capability calls support `roots/list`, `sampling/createMessage`, and `elicitation/create` only after the client advertises support. Unsupported client capabilities return protocol errors without sending requests.

## Security Controls

Security checks run before provider execution:

- Origin validation.
- Bearer token validation.
- Scope/permission validation.
- MCP lifecycle validation.
- Resource URI scheme and path validation.
- Input parameter validation.
- Output size limits.
- Output filtering and optional redaction hooks.

These checks are separate so that Origin failures, auth failures, protocol lifecycle failures, and provider failures are observable and testable.

## Testing Strategy

Use focused tests rather than relying on one large integration test:

- Controller tests for HTTP methods, headers, status codes, endpoint path, and OAuth metadata.
- Dispatcher tests for JSON-RPC parsing, method lookup, standard errors, notification behavior, batch behavior, and ping.
- Session tests for initialize, initialized, expiration, DELETE, emitter cleanup, subscriptions, and in-flight requests.
- Auth tests for missing token, invalid token, insufficient scope, trusted origin rejection, and disabled auth behavior.
- Feature tests for tools/resources/prompts/completion/logging and negotiated notifications.
- Client capability tests for roots, sampling, elicitation, correlation, and unsupported capability errors.
- Maven tests for autoconfigure, plugin aggregator, and sample dependency wiring.

## Build Notes

Keep the implementation Java 8 compatible. Avoid Java 9+ collection factory APIs and avoid adding Spring Security as a required dependency. Reuse project-local dependencies and patterns where possible.

The implementation should treat the existing dirty SSE deletion as a separate completed decision and must not reintroduce any `plugin.sse` references.
