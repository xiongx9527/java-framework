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
- **THEN** the system exposes the MCP Streamable HTTP endpoint at `/mcp`

#### Scenario: Configured endpoint path
- **WHEN** `magic-api.plugin.mcp.endpoint` is configured with a valid path
- **THEN** the system exposes the MCP Streamable HTTP endpoint at the configured path

#### Scenario: JSON-RPC request over POST
- **WHEN** a client sends a valid MCP JSON-RPC request to the MCP endpoint with acceptable headers
- **THEN** the system dispatches the request and returns a valid JSON-RPC response or accepted notification response

#### Scenario: SSE stream over GET
- **WHEN** a client sends GET to the MCP endpoint with `Accept: text/event-stream`
- **THEN** the system opens a server-to-client SSE stream associated with the MCP session or returns a standards-compatible method error when streaming is disabled

#### Scenario: Session termination over DELETE
- **WHEN** a client sends DELETE with a valid `Mcp-Session-Id`
- **THEN** the system terminates that MCP session and releases associated transport resources

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
