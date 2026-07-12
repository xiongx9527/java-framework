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

#### Scenario: Invalid parameters
- **WHEN** a client sends a supported method with invalid parameters
- **THEN** the system returns JSON-RPC error code `-32602`

### Requirement: Sa-Token OAuth2 protected resource
The system SHALL protect MCP endpoints using Sa-Token OAuth2 Bearer token validation and capability-specific permission or scope checks.

#### Scenario: Missing access token
- **WHEN** a client calls the MCP endpoint without a required Bearer token
- **THEN** the system returns `401` with a standard `WWW-Authenticate` challenge

#### Scenario: Insufficient scope
- **WHEN** an authenticated client lacks the required MCP scope for a requested feature
- **THEN** the system returns `403` and does not dispatch the feature logic

#### Scenario: Protected resource metadata
- **WHEN** a client requests OAuth protected resource metadata for the MCP endpoint
- **THEN** the system returns metadata that identifies the MCP resource and Sa-Token OAuth2 authorization information

### Requirement: MCP tools
The system SHALL expose configured MagicAPI-backed and provider-backed tools through `tools/list` and `tools/call`.

#### Scenario: List tools with pagination
- **WHEN** an initialized and authorized client sends `tools/list` with or without a cursor
- **THEN** the system returns a page of tool definitions and an optional opaque `nextCursor`

#### Scenario: Call tool returns content and structured data
- **WHEN** an initialized and authorized client calls a known tool
- **THEN** the system executes the tool and returns valid MCP content, `isError`, and structured content when available

#### Scenario: Tool list changed notification
- **WHEN** registered tools change for an active client session
- **THEN** the system sends `notifications/tools/list_changed` only to sessions that negotiated the related capability

### Requirement: MCP resources
The system SHALL expose custom MCP resources through `resources/list`, `resources/read`, `resources/templates/list`, subscriptions, update notifications, and list change notifications.

#### Scenario: List resources with pagination
- **WHEN** an initialized and authorized client sends `resources/list`
- **THEN** the system returns a page of resources and an optional opaque `nextCursor`

#### Scenario: Read resource by URI
- **WHEN** an initialized and authorized client sends `resources/read` for a valid resource URI
- **THEN** the system returns valid MCP text or blob resource contents with metadata

#### Scenario: List resource templates
- **WHEN** an initialized and authorized client sends `resources/templates/list`
- **THEN** the system returns available parameterized resource templates

#### Scenario: Resource subscription update
- **WHEN** a subscribed resource changes
- **THEN** the system sends `notifications/resources/updated` to subscribed sessions

### Requirement: MCP prompts
The system SHALL expose custom prompts through `prompts/list` and `prompts/get` with argument validation and multi-message responses.

#### Scenario: List prompts with pagination
- **WHEN** an initialized and authorized client sends `prompts/list`
- **THEN** the system returns a page of prompt definitions and an optional opaque `nextCursor`

#### Scenario: Get prompt with arguments
- **WHEN** an initialized and authorized client sends `prompts/get` with valid arguments
- **THEN** the system returns prompt messages rendered for those arguments

#### Scenario: Prompt list changed notification
- **WHEN** registered prompts change for an active client session
- **THEN** the system sends `notifications/prompts/list_changed` only to sessions that negotiated the related capability

### Requirement: Completion
The system SHALL support `completion/complete` for prompt arguments and resource template arguments when a provider supplies completion candidates.

#### Scenario: Complete prompt argument
- **WHEN** a client requests completion for a supported prompt argument
- **THEN** the system returns bounded completion values according to the MCP completion response shape

### Requirement: Logging
The system SHALL support MCP logging level configuration and server-to-client logging notifications when negotiated.

#### Scenario: Set logging level
- **WHEN** an initialized and authorized client sends `logging/setLevel`
- **THEN** the system records the requested level for that session

#### Scenario: Send log message notification
- **WHEN** the server emits an MCP log message for a session
- **THEN** the system sends `notifications/message` only when logging is enabled for that session

### Requirement: Client capability requests
The system SHALL support server-initiated requests for client roots, sampling, and elicitation only when the client advertised the corresponding capability.

#### Scenario: Roots request requires capability
- **WHEN** server logic requests `roots/list` for a session whose client advertised roots support
- **THEN** the system sends a JSON-RPC request to that client session and correlates the response

#### Scenario: Unsupported client capability
- **WHEN** server logic requests sampling or elicitation for a session that did not advertise support
- **THEN** the system fails with a protocol error and does not send the request

### Requirement: Cancellation and progress
The system SHALL support MCP cancellation notifications and progress notifications for long-running operations.

#### Scenario: Cancel in-flight request
- **WHEN** a client sends `notifications/cancelled` for an in-flight request
- **THEN** the system marks the operation cancelled and propagates cancellation to providers when possible

#### Scenario: Progress notification
- **WHEN** a provider reports progress for an operation with a progress token
- **THEN** the system sends `notifications/progress` for the associated session

### Requirement: Security controls
The system SHALL enforce Origin validation, resource URI validation, provider authorization, input validation, output size limits, and output filtering before returning MCP feature results.

#### Scenario: Untrusted origin rejected
- **WHEN** a request includes an Origin that is not trusted by configuration
- **THEN** the system rejects the request before token validation or JSON-RPC dispatch

#### Scenario: Resource URI traversal rejected
- **WHEN** a client requests a resource URI that violates the allowed URI scheme or path rules
- **THEN** the system rejects the request and does not call the resource provider
