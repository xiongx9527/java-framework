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

- [x] 3.1 Add configurable MCP header names with defaults for accept, protocol version, session id, and origin.
- [x] 3.2 Refactor `MagicMcpController` to read configured headers from `HttpServletRequest`.
- [x] 3.3 Introduce `McpSessionRepository` and move existing in-memory session map behind the repository abstraction.
- [x] 3.4 Keep local in-memory session behavior for non-Redis or non-cluster deployments.
- [x] 3.5 Add tests for custom header names and repository-backed session lifecycle.

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
