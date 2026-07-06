## MODIFIED Requirements
### Requirement: Streamable HTTP transport
The system SHALL expose a configurable MCP Streamable HTTP endpoint that supports POST, GET, and DELETE according to negotiated session behavior, and GET SHALL maintain an MCP-owned server-to-client SSE stream for notifications and server-initiated requests when streaming is enabled.

#### Scenario: Initialize response includes session id header
- **WHEN** a client sends a valid `initialize` request and sessions are enabled
- **THEN** the HTTP response SHALL include the configured MCP session header with the created session id

#### Scenario: SSE stream rejects invalid session
- **WHEN** a client sends GET to the MCP endpoint with an unknown, expired, deleted, or not-ready `Mcp-Session-Id`
- **THEN** the system returns a client error and does not open an SSE stream

### Requirement: JSON-RPC dispatcher
The system SHALL implement strict JSON-RPC 2.0 request, notification, response, batch, error, and unknown-method handling for MCP messages.

#### Scenario: Invalid JSON-RPC version
- **WHEN** a client sends a JSON-RPC request without `jsonrpc: "2.0"`
- **THEN** the system returns JSON-RPC error code `-32600`

#### Scenario: Batch request
- **WHEN** a client sends a JSON-RPC batch containing one or more requests
- **THEN** the system dispatches each item and returns a response array for request items while omitting notification-only items

### Requirement: MCP lifecycle and ping
The system SHALL enforce MCP lifecycle ordering and support the base `ping` request.

#### Scenario: Feature operations before initialization are rejected
- **WHEN** a client sends feature requests such as `tools/list`, `resources/read`, or `prompts/get` before a successful initialize and initialized flow
- **THEN** the system returns a JSON-RPC lifecycle error and does not execute feature logic
