## MODIFIED Requirements

### Requirement: MagicAPI console websocket resource validation
The plugin SHALL make MagicAPI console validation for WebSocket resources reflect real backend behavior instead of returning unconditional success.

#### Scenario: Datasource test validates usable configuration
- **WHEN** a MagicAPI console user tests a `ws-datasource`
- **THEN** the plugin SHALL validate required datasource fields, JSON-formatted optional configuration, and token generation prerequisites before returning success

#### Scenario: Datasource test reports invalid configuration
- **WHEN** a MagicAPI console user tests a `ws-datasource` with missing required fields, invalid JSON configuration, or missing token signing configuration
- **THEN** the plugin SHALL return a failure result instead of reporting `ok`

### Requirement: Structured recoverable error response
The plugin SHALL return a structured text error message for recoverable business-message errors and SHALL reserve WebSocket close codes for protocol, policy, unsupported-data, overload, and server-failure conditions.

#### Scenario: Recoverable business message error
- **WHEN** a connected client sends a text business message that is valid WebSocket data but invalid for MagicAPI business dispatch
- **THEN** the plugin SHALL send a JSON text response containing `success=false`, an error code, a human-readable message, and a timestamp while keeping the session usable

#### Scenario: Script execution error
- **WHEN** a matched `ws-subscribe` script throws while processing a business message
- **THEN** the plugin SHALL send a JSON text response with error code `SCRIPT_ERROR` while keeping the session usable

#### Scenario: Non-recoverable protocol or policy error
- **WHEN** a connected client violates WebSocket protocol, sends unsupported data, fails policy checks, overloads the service, or triggers unrecoverable server failure
- **THEN** the plugin SHALL close the connection with a suitable WebSocket close status instead of returning a business error message
