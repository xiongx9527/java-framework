# standard-mcp-server-plugin Specification

## Purpose
TBD - created by archiving change add-standard-mcp-server-plugin. Update Purpose after archive.
## Requirements
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
The system SHALL expose a configurable MCP Streamable HTTP endpoint that supports POST, GET, and DELETE according to negotiated session behavior, and GET SHALL maintain an MCP-owned server-to-client SSE stream for notifications and server-initiated requests when streaming is enabled.

#### Scenario: Protocol version header is returned
- **WHEN** a client sends a valid MCP request with a negotiated protocol version
- **THEN** the HTTP response SHALL include the configured protocol version header with the negotiated value

#### Scenario: POST rejects invalid session before dispatch
- **WHEN** a client sends a non-initialize POST with an unknown, expired, deleted, or invalid MCP session header
- **THEN** the system returns a client error before dispatching feature logic

#### Scenario: SSE emits message events
- **WHEN** the server sends a JSON-RPC notification or server-to-client request over GET SSE
- **THEN** the stream emits a standards-compatible `message` event containing the JSON-RPC message payload

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

#### Scenario: Supported protocol version negotiated
- **WHEN** a client initializes with a protocol version supported by the configured MCP server
- **THEN** the server returns that negotiated version, server info, capabilities, and session metadata

#### Scenario: Unsupported protocol version rejected
- **WHEN** a client initializes with an unsupported protocol version
- **THEN** the server returns a JSON-RPC invalid params error and does not create a usable MCP session

#### Scenario: Default protocol version is current
- **WHEN** a client initializes without an explicit protocol version
- **THEN** the server negotiates the configured default version, which SHALL default to `2025-11-25`

### Requirement: JSON-RPC dispatcher
The system SHALL implement strict JSON-RPC 2.0 request, notification, response, batch, error, and unknown-method handling for MCP messages.

#### Scenario: Invalid JSON-RPC version
- **WHEN** a client sends a JSON-RPC request without `jsonrpc: "2.0"`
- **THEN** the system returns JSON-RPC error code `-32600`

#### Scenario: Batch request
- **WHEN** a client sends a JSON-RPC batch containing one or more requests
- **THEN** the system dispatches each item and returns a response array for request items while omitting notification-only items

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
The system SHALL expose configured MagicAPI-backed and provider-backed tools through `tools/list` and `tools/call`, including scoped MagicAPI function tools and optionally scoped MagicAPI API tools.

#### Scenario: List tools with pagination
- **WHEN** an initialized and authorized client sends `tools/list` with or without a cursor
- **THEN** the system returns a page of tool definitions and an optional opaque `nextCursor`

#### Scenario: Call tool returns content and structured data
- **WHEN** an initialized and authorized client calls a known tool
- **THEN** the system executes the tool and returns valid MCP content, `isError`, and structured content when available

#### Scenario: Tool list changed notification
- **WHEN** provider-backed tools, function-backed tools, or scoped API-backed tools change for an active client session
- **THEN** the system sends `notifications/tools/list_changed` over the MCP server-to-client stream only to sessions that negotiated the related capability

### Requirement: MCP resources
The system SHALL expose provider-backed and console-managed MCP resources through `resources/list`, `resources/read`, `resources/templates/list`, subscriptions, update notifications, and list change notifications.

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
- **THEN** the system sends `notifications/resources/updated` to subscribed sessions over the MCP server-to-client stream

#### Scenario: Resource list changed notification
- **WHEN** an MCP resource is created, saved, moved, or deleted
- **THEN** the system sends `notifications/resources/list_changed` to sessions that negotiated the related capability

### Requirement: MCP prompts
The system SHALL expose provider-backed and console-managed custom prompts through `prompts/list` and `prompts/get` with argument validation and multi-message responses.

#### Scenario: List prompts with pagination
- **WHEN** an initialized and authorized client sends `prompts/list`
- **THEN** the system returns a page of prompt definitions and an optional opaque `nextCursor`

#### Scenario: Get prompt with arguments
- **WHEN** an initialized and authorized client sends `prompts/get` with valid arguments
- **THEN** the system returns prompt messages rendered for those arguments

#### Scenario: Prompt list changed notification
- **WHEN** provider-backed prompts or console-managed prompts change for an active client session
- **THEN** the system sends `notifications/prompts/list_changed` over the MCP server-to-client stream only to sessions that negotiated the related capability

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

### Requirement: MCP capabilities
The system SHALL advertise capabilities that match the enabled MCP handlers, providers, subscriptions, logging, completion, and client-request features.

#### Scenario: List changed capabilities advertised
- **WHEN** tools, resources, or prompts support list change notifications
- **THEN** the corresponding capability includes `listChanged: true`

#### Scenario: Resource subscription capability advertised
- **WHEN** resource subscription is enabled
- **THEN** the resources capability includes `subscribe: true`

#### Scenario: Optional capabilities omitted when unavailable
- **WHEN** completion providers or client-request features are unavailable
- **THEN** the server does not advertise unsupported capabilities

### Requirement: Console-managed MCP diagnostics
The system SHALL provide Magic console diagnostics for MCP tools, resources, and prompts without bypassing MCP authorization, session, or provider semantics.

#### Scenario: Resource read preview
- **WHEN** a console user previews an MCP resource
- **THEN** the diagnostic flow executes `resources/read` semantics and displays returned contents or JSON-RPC error details

#### Scenario: Prompt get preview
- **WHEN** a console user previews an MCP prompt with arguments
- **THEN** the diagnostic flow executes `prompts/get` semantics and displays rendered messages or JSON-RPC error details

#### Scenario: Tool list and call preview
- **WHEN** a console user opens MCP tool diagnostics
- **THEN** the console can list available tools and call a selected tool with JSON arguments through protected MCP diagnostic backend behavior

