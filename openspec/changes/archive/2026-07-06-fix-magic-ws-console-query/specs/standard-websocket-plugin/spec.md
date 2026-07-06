## ADDED Requirements

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
