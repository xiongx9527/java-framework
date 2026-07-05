## ADDED Requirements

### Requirement: Streamable HTTP MCP endpoint
The system SHALL expose a configurable MCP Streamable HTTP endpoint for the SSE plugin. The endpoint MUST support standard JSON-RPC POST requests and MUST support GET for server-to-client SSE streaming when the client requests `text/event-stream`.

#### Scenario: JSON-RPC initialize over POST
- **WHEN** a client sends a valid `initialize` JSON-RPC request to the MCP endpoint with an acceptable `Accept` header
- **THEN** the system returns a JSON-RPC response containing `protocolVersion`, `capabilities`, and `serverInfo`

#### Scenario: SSE stream over GET
- **WHEN** a client sends GET to the MCP endpoint with `Accept: text/event-stream`
- **THEN** the system opens an SSE stream or returns a standards-compatible rejection if streaming is disabled by configuration

### Requirement: MCP transport header validation
The system SHALL validate MCP HTTP transport headers for standard-mode requests. POST requests MUST accept `application/json` and `text/event-stream` response types, and requests after initialization MUST honor the negotiated `MCP-Protocol-Version` and `Mcp-Session-Id` semantics.

#### Scenario: Missing unacceptable Accept header
- **WHEN** a client sends a standard MCP POST request without an acceptable response media type
- **THEN** the system rejects the request with an HTTP client error and does not execute any MagicAPI tool

#### Scenario: Unknown session id
- **WHEN** a client sends a request with an unknown `Mcp-Session-Id`
- **THEN** the system rejects the request with an HTTP client error and does not create a new implicit session

### Requirement: MCP lifecycle enforcement
The system SHALL enforce the MCP lifecycle for each session. A session MUST process `initialize` before tool operations, and MUST record `notifications/initialized` before treating the session as ready for normal tool requests.

#### Scenario: Tool request before initialize
- **WHEN** a client sends `tools/list` or `tools/call` before a successful `initialize`
- **THEN** the system returns a JSON-RPC error response and does not execute any MagicAPI script

#### Scenario: Initialized notification completes lifecycle
- **WHEN** a client sends `notifications/initialized` after a successful `initialize`
- **THEN** the system marks the session ready without sending a JSON-RPC response body for the notification

### Requirement: MagicAPI tools adapter
The system SHALL expose configured MagicAPI APIs as MCP tools. `tools/list` MUST return tool definitions with `name`, `description`, and `inputSchema`; `tools/call` MUST execute the selected MagicAPI API with supplied arguments and return MCP content blocks.

#### Scenario: List tools from configured group
- **WHEN** an initialized client sends `tools/list`
- **THEN** the system returns only the MagicAPI APIs allowed by the MCP datasource configuration

#### Scenario: Call known tool
- **WHEN** an initialized client sends `tools/call` for a known MagicAPI API with valid arguments
- **THEN** the system executes the API and returns a result with `content` and `isError`

#### Scenario: Call unknown tool
- **WHEN** an initialized client sends `tools/call` for an unknown tool name
- **THEN** the system returns a JSON-RPC error and does not execute any script

### Requirement: Legacy HTTP+SSE compatibility
The system SHALL preserve the existing legacy HTTP+SSE-style connection flow for MCP-enabled SSE datasources where feasible. The legacy SSE `endpoint` event MUST point to a real POST endpoint that shares the same JSON-RPC processing behavior as the standard endpoint.

#### Scenario: Legacy endpoint event points to working POST route
- **WHEN** a legacy SSE MCP client connects to an MCP-enabled datasource
- **THEN** the first MCP endpoint event contains a POST URL that the server actually handles

### Requirement: MCP transport security
The system SHALL validate request origins for MCP HTTP transports and MUST provide configuration for trusted origins. The server MUST reject untrusted origins before processing JSON-RPC messages.

#### Scenario: Untrusted origin rejected
- **WHEN** a client sends an MCP request with an untrusted `Origin`
- **THEN** the system rejects the request and does not execute lifecycle or tool logic

### Requirement: Existing SSE behavior compatibility
The system SHALL keep existing SSE datasource, subscription, lifecycle script, and broadcast behavior compatible for non-MCP usage.

#### Scenario: Ordinary broadcast remains available
- **WHEN** existing MagicScript calls `sse.broadcastMessageToClient` or `sse.broadcastDistributedMessageToAllClient`
- **THEN** the system delivers messages using the existing SSE datasource/session behavior
