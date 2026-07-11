## MODIFIED Requirements

### Requirement: MCP configuration change notifications
The system SHALL detect runtime changes to MCP plugin configuration keys under `magic-api.plugin.mcp` that affect client-visible tools, resources, prompts, declared capabilities, protocol negotiation, session behavior, authentication, transport header semantics, endpoint interaction, logging, or completion semantics, and SHALL notify connected MCP clients using existing MCP list change notifications.

#### Scenario: Tool configuration changes
- **WHEN** a runtime configuration refresh changes any `magic-api.plugin.mcp.tools.functions.*` or `magic-api.plugin.mcp.tools.api.*` key
- **THEN** the system sends `notifications/tools/list_changed` over the MCP server-to-client stream to sessions that negotiated the tools list changed capability

#### Scenario: Protocol configuration changes
- **WHEN** a runtime configuration refresh changes any `magic-api.plugin.mcp.protocol.*` key
- **THEN** the system sends MCP list change notifications that prompt connected clients to refresh their MCP capability view

#### Scenario: Auth or header configuration changes
- **WHEN** a runtime configuration refresh changes any `magic-api.plugin.mcp.auth.*` or `magic-api.plugin.mcp.headers.*` key
- **THEN** the system sends MCP list change notifications that prompt connected clients to refresh their MCP capability view

#### Scenario: Additional client-visible MCP configuration changes
- **WHEN** a runtime configuration refresh changes an additional `magic-api.plugin.mcp.*` key that affects client-visible capability lists, declared capabilities, endpoint interaction, session behavior, logging, or completion semantics
- **THEN** the system sends the corresponding existing MCP list change notifications for the affected tools, resources, prompts, or overall capability view

#### Scenario: Non-client-visible MCP configuration changes are ignored
- **WHEN** a runtime configuration refresh changes an MCP configuration key that does not affect client-visible capability lists or MCP interaction semantics
- **THEN** the system does not send MCP list change notifications for that key

#### Scenario: Non-MCP configuration changes are ignored
- **WHEN** a runtime configuration refresh changes keys outside `magic-api.plugin.mcp.*`
- **THEN** the system does not send MCP list change notifications for that refresh

#### Scenario: Multiple related keys in one refresh are deduplicated
- **WHEN** a single runtime configuration refresh changes multiple keys that map to the same MCP list change notification
- **THEN** the system sends that MCP list change notification at most once for the refresh
