## 1. Module Wiring

- [x] 1.1 Create `fw-plus-magicapi-plugin-mcp` starter module with Maven metadata matching existing MagicAPI plugin starters.
- [x] 1.2 Add MCP plugin dependency management to the Boot BOM.
- [x] 1.3 Add MCP plugin module to the MagicAPI plugin aggregator POM.
- [x] 1.4 Register MCP auto-configuration in `MagicApiPluginAutoConfiguration`.
- [x] 1.5 Add sample dependency and minimal sample configuration without introducing datasource resources.

## 2. Transport, Session, and JSON-RPC Foundation

- [x] 2.1 Add MCP configuration properties for enablement, endpoint path, protocol versions, session timeout, SSE stream, request limits, trusted origins, and feature toggles.
- [x] 2.2 Implement Streamable HTTP controller for POST, GET, DELETE, SSE response handling, and protected resource metadata.
- [x] 2.3 Implement MCP session service for lifecycle state, protocol version, client capabilities, principal, emitter, subscriptions, in-flight requests, expiration, and deletion.
- [x] 2.4 Implement JSON-RPC model, parser, dispatcher, request/notification/response distinction, batch handling, and standard error mapping.
- [x] 2.5 Implement initialize, initialized notification, ping, cancellation, and progress notification handling.
- [x] 2.6 Add tests for transport headers, HTTP status behavior, session lifecycle, JSON-RPC errors, ping, cancellation, and progress.

## 3. Sa-Token OAuth2 Security

- [x] 3.1 Implement Sa-Token OAuth2 Bearer token extraction and validation for MCP requests.
- [x] 3.2 Implement capability-specific scope or permission checks for tools, resources, prompts, logging, roots, sampling, and elicitation.
- [x] 3.3 Implement standard `401`, `403`, `WWW-Authenticate`, and OAuth protected resource metadata responses.
- [x] 3.4 Implement Origin validation, URI validation, input validation, output size limits, and output filtering hooks.
- [x] 3.5 Add tests for missing token, invalid token, insufficient scope, trusted origins, and rejected invalid resource URIs.

## 4. Tools

- [x] 4.1 Define tool provider SPI and default MagicAPI-backed tool provider.
- [x] 4.2 Implement `tools/list` with pagination, annotations, input schemas, output schemas, and list change capability declaration.
- [x] 4.3 Implement `tools/call` with MagicAPI execution, content blocks, structured content, error results, output filtering, and provider authorization.
- [x] 4.4 Implement `notifications/tools/list_changed` for negotiated sessions.
- [x] 4.5 Add tests for list pagination, known tool calls, unknown tool calls, structured content, and list change notification gating.

## 5. Resources

- [x] 5.1 Define resource provider SPI for standard `resources` content, resource templates, reads, subscriptions, and provider events.
- [x] 5.2 Implement `resources/list` with pagination, metadata, annotations, and authorization.
- [x] 5.3 Implement `resources/read` for text and blob contents with MIME type and URI validation.
- [x] 5.4 Implement `resources/templates/list` for parameterized resources.
- [x] 5.5 Implement `resources/subscribe`, `resources/unsubscribe`, `notifications/resources/updated`, and `notifications/resources/list_changed`.
- [x] 5.6 Add tests for resource listing, reads, templates, subscriptions, updates, list changes, authorization, and content limits.

## 6. Prompts and Completion

- [x] 6.1 Define prompt provider SPI for standard `prompts` content, prompt arguments, rendering, and provider events.
- [x] 6.2 Implement `prompts/list` with pagination, argument metadata, annotations, and authorization.
- [x] 6.3 Implement `prompts/get` with required argument validation, rendering, multi-message output, and resource references when supplied by providers.
- [x] 6.4 Implement `notifications/prompts/list_changed` for negotiated sessions.
- [x] 6.5 Implement `completion/complete` for prompt arguments and resource template arguments.
- [x] 6.6 Add tests for prompt listing, prompt rendering, validation failures, list change notification gating, and completion results.

## 7. Logging and Client Capabilities

- [x] 7.1 Implement `logging/setLevel` and `notifications/message` with per-session level state and authorization.
- [x] 7.2 Implement server-initiated `roots/list` request support gated by negotiated client capabilities.
- [x] 7.3 Implement server-initiated `sampling/createMessage` request support gated by negotiated client capabilities.
- [x] 7.4 Implement server-initiated `elicitation/create` request support gated by negotiated client capabilities.
- [x] 7.5 Add tests for logging level behavior, log notification gating, client request correlation, and unsupported client capability errors.

## 8. Integration and Verification

- [x] 8.1 Verify no references to deleted SSE plugin packages or `mcp-datasource`/`*-datasource` MCP content models are introduced.
- [x] 8.2 Run OpenSpec validation for the new change and strict specs.
- [x] 8.3 Run focused Maven tests for Boot autoconfigure and MagicAPI plugin aggregator modules.
- [x] 8.4 Run sample module build or test with `-am` to confirm dependency wiring.
- [x] 8.5 Update task status and prepare the change for Comet design/build transition.
