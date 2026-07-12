## ADDED Requirements

### Requirement: MCP configuration change notifications
The system SHALL detect runtime changes to MCP plugin configuration keys under `magic-api.plugin.mcp` that affect client-visible tools, protocol, authentication, or transport header semantics, and SHALL notify connected MCP clients using existing MCP list change notifications.

#### Scenario: Tool configuration changes
- **WHEN** a runtime configuration refresh changes any `magic-api.plugin.mcp.tools.functions.*` or `magic-api.plugin.mcp.tools.api.*` key
- **THEN** the system sends `notifications/tools/list_changed` over the MCP server-to-client stream to sessions that negotiated the tools list changed capability

#### Scenario: Protocol configuration changes
- **WHEN** a runtime configuration refresh changes any `magic-api.plugin.mcp.protocol.*` key
- **THEN** the system sends MCP list change notifications that prompt connected clients to refresh their MCP capability view

#### Scenario: Auth or header configuration changes
- **WHEN** a runtime configuration refresh changes any `magic-api.plugin.mcp.auth.*` or `magic-api.plugin.mcp.headers.*` key
- **THEN** the system sends MCP list change notifications that prompt connected clients to refresh their MCP capability view

#### Scenario: Non-MCP configuration changes are ignored
- **WHEN** a runtime configuration refresh changes keys outside `magic-api.plugin.mcp.*`
- **THEN** the system does not send MCP list change notifications for that refresh

#### Scenario: Multiple related keys in one refresh are deduplicated
- **WHEN** a single runtime configuration refresh changes multiple keys that map to the same MCP list change notification
- **THEN** the system sends that MCP list change notification at most once for the refresh

### Requirement: MagicAPI config resource changes trigger MCP configuration notifications
The system SHALL apply MCP configuration change notification behavior to MagicAPI `config` resource changes after those resources update the Spring Environment.

#### Scenario: MagicAPI config resource updates MCP tools configuration
- **WHEN** a MagicAPI `config` resource is created, saved, moved, deleted, enabled, disabled, or otherwise refreshed and the resulting configuration keys include `magic-api.plugin.mcp.tools.*`
- **THEN** the system sends the corresponding MCP list change notifications after the configuration refresh is applied

#### Scenario: MagicAPI config resource updates unrelated configuration
- **WHEN** a MagicAPI `config` resource changes only keys outside `magic-api.plugin.mcp.*`
- **THEN** the system refreshes configuration without sending MCP list change notifications

#### Scenario: MCP plugin disabled or absent
- **WHEN** MagicAPI `config` resources change while the MCP plugin is disabled or absent
- **THEN** the configuration refresh continues to work without requiring MCP notification beans
