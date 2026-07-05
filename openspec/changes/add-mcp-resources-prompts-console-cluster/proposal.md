## Why

The current MCP plugin exposes the protocol endpoints and provider SPI, but it has no MagicAPI console for maintaining MCP resources and prompts, no configurable API tool exposure boundary, and no working notification path for list changes. In clustered deployments its in-memory session and empty SSE stream also make MCP clients unreliable when traffic lands on different nodes.

## What Changes

- Add MagicAPI console integration for MCP `resources` and `prompts` as first-class resource types, not datasource resources.
- Persist MCP resources and prompts through MagicAPI resource storage and expose them through `McpResourceProvider` and `McpPromptProvider`.
- Add configurable MagicAPI API exposure as MCP tools, including include/exclude scope controls and explicit opt-in support.
- Send MCP client notifications when API tools, MCP resources, or MCP prompts change:
  - `notifications/tools/list_changed`
  - `notifications/resources/list_changed`
  - `notifications/resources/updated`
  - `notifications/prompts/list_changed`
- Make `MagicMcpController` request header names configurable while preserving current defaults.
- Add cluster-ready MCP session and SSE notification design so multi-node deployments can keep client sessions and change events coherent.
- Replace the current MCP README with a detailed接入说明 covering dependencies, configuration, OAuth, resources, prompts, API tools, events, cluster deployment, validation examples, and troubleshooting.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `standard-mcp-server-plugin`: Extend the MCP plugin requirements for console-managed resources/prompts, API tool scope controls, configurable request headers, client change notifications, cluster support, and detailed integration documentation.

## Impact

- Affected backend modules:
  - `fw-plus-boot-project/fw-plus-boot-autoconfigure/.../magicapi/plugin/mcp/**`
  - existing MagicAPI resource storage/dynamic registry integration points.
- Affected starter module:
  - `fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-mcp/**`
- Affected documentation:
  - `fw-plus-magicapi-plugin-mcp/README.md`
- No datasource-based MCP resource model will be introduced.
- No restoration or reuse of the deleted SSE plugin is in scope; MCP uses its own transport and may reuse existing MagicAPI cluster notification infrastructure where appropriate.
