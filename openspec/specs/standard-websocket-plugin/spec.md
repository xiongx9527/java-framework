# standard-websocket-plugin Specification

## Purpose
Define the standard behavior for the MagicAPI WebSocket plugin, including handshake validation, protocol frame handling, text business-message dispatch, lifecycle scripts, console validation, structured errors, and operational documentation.
## Requirements
### Requirement: Standards-compliant handshake validation
The plugin SHALL validate required connection parameters, token, appId, trusted Origin, and negotiated subprotocol during the WebSocket handshake or immediately at connection establishment with deterministic failure behavior.

#### Scenario: Missing required connection parameter
- **WHEN** a client connects without the configured token parameter or clientId parameter
- **THEN** the plugin SHALL reject the connection or close it with a policy/protocol failure before registering the session

#### Scenario: Invalid token appId
- **WHEN** a client connects with an invalid, expired, or unknown appId token
- **THEN** the plugin SHALL reject the connection or close it without executing init scripts

#### Scenario: Trusted origin enforcement
- **WHEN** a browser client connects with an Origin not listed in `websocket.trustedDomain`
- **THEN** the plugin SHALL reject the handshake unless the configured trusted domain is explicitly `*`

#### Scenario: Subprotocol negotiation
- **WHEN** supported subprotocols are configured and the client offers one or more protocols
- **THEN** the plugin SHALL negotiate only configured protocols and SHALL NOT silently accept unsupported protocols

### Requirement: Standard WebSocket control frame handling
The plugin SHALL handle WebSocket control frames according to standard WebSocket semantics and keep existing text ping compatibility.

#### Scenario: Ping control frame
- **WHEN** a connected client sends a Ping control frame
- **THEN** the plugin SHALL respond with a Pong control frame without invoking business scripts

#### Scenario: Pong control frame
- **WHEN** a connected client sends a Pong control frame
- **THEN** the plugin SHALL accept it as a connection-level signal and SHALL NOT invoke business scripts

#### Scenario: Text ping compatibility
- **WHEN** a connected client sends text message `ping`
- **THEN** the plugin SHALL respond with text message `pong` for backward compatibility

### Requirement: Text business message contract
The plugin SHALL process business messages as text JSON objects with a non-blank `type` field and dispatch them to enabled `ws-subscribe` scripts for the same appId.

#### Scenario: Valid business message
- **WHEN** a connected client sends a JSON text message containing a non-blank `type` matching an enabled `ws-subscribe.businessId`
- **THEN** the plugin SHALL execute the matched script with appId, clientId, sessionId, type, and params in context

#### Scenario: Invalid JSON business message
- **WHEN** a connected client sends a text message that is neither `ping` nor valid JSON
- **THEN** the plugin SHALL return a structured error message or close with protocol error according to the configured policy

#### Scenario: Missing business type
- **WHEN** a connected client sends a JSON text message without a non-blank `type`
- **THEN** the plugin SHALL return a structured error message and SHALL NOT execute any business script

#### Scenario: Unknown business type
- **WHEN** a connected client sends a JSON text message whose `type` has no enabled matching subscription under the same appId
- **THEN** the plugin SHALL return a structured error message and keep the connection usable

### Requirement: Unsupported data type handling
The plugin SHALL explicitly handle unsupported WebSocket message types instead of silently ignoring them.

#### Scenario: Binary message received
- **WHEN** a connected client sends a binary message
- **THEN** the plugin SHALL reject it with close code 1003 or an equivalent unsupported-data handling path and SHALL NOT invoke MagicAPI scripts

#### Scenario: Oversized or service-overload condition
- **WHEN** the plugin cannot accept or retain a session due to configured capacity or resource pressure
- **THEN** the plugin SHALL close affected sessions with a service-overload close status and release session resources

### Requirement: Lifecycle script consistency
The plugin SHALL execute lifecycle scripts deterministically for successful session registration and closure.

#### Scenario: Init script after successful registration
- **WHEN** a connection is authenticated and registered successfully
- **THEN** the plugin SHALL execute enabled `init` scripts for that appId exactly once for that session

#### Scenario: Close script once
- **WHEN** a registered session is closed by client, server cleanup, cache eviction, or application shutdown
- **THEN** the plugin SHALL execute enabled `close` scripts at most once for that session

#### Scenario: Failed handshake no lifecycle script
- **WHEN** a connection fails authentication or validation before session registration
- **THEN** the plugin SHALL NOT execute init or close scripts for that connection

### Requirement: Server push and cluster broadcast integrity
The plugin SHALL preserve point-to-point push, all-client broadcast, multi-session client delivery, and Redis cluster broadcast behavior after standardization.

#### Scenario: Point-to-point push to multi-session client
- **WHEN** server script calls `broadcastMessageToClient(clientId, message)` for a client with multiple open sessions under the target appId
- **THEN** the plugin SHALL deliver the message to every valid session for that client

#### Scenario: All-client broadcast
- **WHEN** server script calls `broadcastDistributedMessageToAllClient(message)`
- **THEN** the plugin SHALL deliver the message to all valid sessions under the target appId

#### Scenario: Redis cluster broadcast loop avoidance
- **WHEN** Redis cluster broadcast is enabled and a node receives a broadcast message originated by itself
- **THEN** the plugin SHALL ignore the self-originated message to avoid duplicate delivery loops

### Requirement: Documentation and diagnostics
The plugin SHALL document the standard connection contract, compatibility behavior, close semantics, and common troubleshooting steps.

#### Scenario: Client integration documentation
- **WHEN** a developer reads the WS plugin README
- **THEN** the documentation SHALL describe token acquisition, connection URL, Origin/subprotocol rules, standard Ping/Pong, text ping compatibility, business message format, error response shape, and close code meanings

#### Scenario: Safe diagnostic logging
- **WHEN** the plugin logs connection, token, script, or close diagnostics
- **THEN** the logs SHALL NOT expose full token values or data source secrets

### Requirement: Structured recoverable error response
The plugin SHALL return a structured text error message for recoverable business-message errors and SHALL reserve WebSocket close codes for protocol, policy, unsupported-data, overload, and server-failure conditions.

#### Scenario: Recoverable business-message error
- **WHEN** a connected client sends a text business message that is valid WebSocket data but invalid for MagicAPI business dispatch
- **THEN** the plugin SHALL send a JSON text error containing `type`, `code`, and `message` fields and SHALL keep the connection usable

#### Scenario: Non-recoverable protocol or policy error
- **WHEN** a connected client violates protocol, authentication, policy, unsupported data type, or capacity constraints
- **THEN** the plugin SHALL close the connection with a suitable WebSocket close status instead of returning a business error message

### Requirement: MagicAPI console websocket resource validation
The plugin SHALL make MagicAPI console validation for WebSocket resources reflect real backend behavior instead of returning unconditional success.

#### Scenario: Datasource test validates usable configuration
- **WHEN** a MagicAPI console user tests a `ws-datasource`
- **THEN** the plugin SHALL validate required datasource fields, JSON-formatted optional configuration, and token generation prerequisites before returning success

#### Scenario: Datasource test reports invalid configuration
- **WHEN** a MagicAPI console user tests a `ws-datasource` with missing required fields, invalid JSON configuration, or missing token signing configuration
- **THEN** the plugin SHALL return a failure result instead of reporting `ok`

### Requirement: MagicAPI console websocket subscribe execution
The plugin SHALL allow MagicAPI console users to test enabled `ws-subscribe` scripts by script id with a deterministic simulated WebSocket context.

#### Scenario: Subscribe script test executes selected script
- **WHEN** a MagicAPI console user runs a `ws-subscribe` script test by script id
- **THEN** the plugin SHALL load that script, execute it with appId, clientId, sessionId, type, businessType, and params in context, and return the script result or structured failure

#### Scenario: Subscribe script test rejects unknown script
- **WHEN** a MagicAPI console user runs a `ws-subscribe` script test with an unknown script id
- **THEN** the plugin SHALL return a failure result and SHALL NOT execute unrelated scripts

### Requirement: MagicAPI console websocket editor metadata consistency
The plugin SHALL keep WebSocket console resource metadata and JavaClass hints consistent with backend classes and storage rules.

#### Scenario: WebSocket module class hint uses real backend class
- **WHEN** the MagicAPI editor loads WebSocket plugin code hints
- **THEN** the plugin SHALL reference the real `WsModule` backend class and SHALL NOT reference old package names or nonexistent producer classes

#### Scenario: Subscribe resource path setting matches backend storage
- **WHEN** the MagicAPI editor registers the `ws-subscribe` resource type
- **THEN** its path requirement metadata SHALL match the backend `WsSubscribeInfoMagicResourceStorage` path requirement

### Requirement: WebSocket script context compatibility
The plugin SHALL provide compatible business type variables for all WebSocket subscribe script execution paths.

#### Scenario: Lifecycle script receives compatible type variables
- **WHEN** the plugin executes an `init` or `close` `ws-subscribe` script
- **THEN** the script context SHALL include both `type` and `businessType` with the matched business identifier

#### Scenario: Business message script receives compatible type variables
- **WHEN** the plugin executes a business-message `ws-subscribe` script
- **THEN** the script context SHALL include both `type` and `businessType` with the incoming message type and SHALL include `params` with the parsed JSON message
