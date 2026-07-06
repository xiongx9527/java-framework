## MODIFIED Requirements
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

### Requirement: Client capability requests
The system SHALL support server-to-client requests for client capabilities with request correlation, response handling, timeout cleanup, cancellation, and unsupported capability errors.

#### Scenario: Server-to-client request is correlated
- **WHEN** the server sends `roots/list`, `sampling/createMessage`, or `elicitation/create`
- **THEN** the request is stored with id, method, capability, session, and timeout metadata until a response, error, cancellation, or timeout resolves it

#### Scenario: Client response resolves pending request
- **WHEN** a client sends a JSON-RPC response or error with an id matching a pending server-to-client request
- **THEN** the system records the result or error and removes or marks the request as resolved

#### Scenario: Timed out client request cleaned up
- **WHEN** a pending server-to-client request exceeds the configured timeout
- **THEN** the system marks it timed out and releases pending state

### Requirement: MCP tools
The system SHALL expose MagicAPI functions and explicitly enabled MagicAPI APIs as MCP tools with useful schemas, content, structured content, and safe execution behavior.

#### Scenario: Tool schema generated from Magic metadata
- **WHEN** Magic API or function metadata describes parameters
- **THEN** `tools/list` returns an input schema including known parameter names, descriptions, and required flags

#### Scenario: Tool schema falls back safely
- **WHEN** Magic metadata is insufficient to generate a precise schema
- **THEN** `tools/list` returns a valid object schema without failing list or call operations

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
