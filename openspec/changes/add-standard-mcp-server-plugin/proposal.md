## Why

MagicAPI needs a standards-compliant MCP server integration that can be used by normal MCP clients without depending on the removed SSE plugin or the business `ssehttp` module. The new plugin should expose MagicAPI capabilities, custom resources, and prompts through the official MCP protocol while reusing the project's Sa-Token OAuth2 authentication model.

## What Changes

- Add an independent MagicAPI MCP plugin module, `fw-plus-magicapi-plugin-mcp`.
- Add a standard MCP Streamable HTTP transport with session lifecycle, JSON-RPC dispatching, notifications, cancellation, progress, ping, and strict HTTP/header semantics.
- Add MCP server capabilities for tools, resources, resource templates, prompts, completion, logging, resource subscriptions, and dynamic list change notifications.
- Add server-side support for invoking client capabilities when the client declares them, including roots, sampling, and elicitation.
- Add Sa-Token OAuth2 protected-resource integration for MCP endpoints, including Bearer token validation, scope/permission checks, protected resource metadata, and standard unauthorized/forbidden responses.
- Add custom MCP resource management split into standard `resources` and `prompts`; do not introduce `mcp-datasource` or any `*-datasource` model.
- Keep `ssehttp` unchanged and do not restore the deleted SSE plugin.

## Capabilities

### New Capabilities
- `standard-mcp-server-plugin`: Full standard MCP server support for MagicAPI through an independent plugin, including transport, lifecycle, tools, resources, prompts, completion, logging, client-capability calls, and Sa-Token OAuth2 protection.

### Modified Capabilities

## Impact

- Affected Boot autoconfigure code: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp`.
- Affected plugin starter tree: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-mcp`.
- Affected dependency management and aggregation: Boot BOM, MagicAPI plugin aggregator POM, MagicAPI plugin auto-configuration, and MagicAPI sample dependencies.
- Dependencies: Sa-Token OAuth2 support will be reused for authentication and authorization. No SSE plugin dependency and no datasource-style resource dependency should be introduced.
- Runtime behavior: MCP clients can connect through a standard HTTP endpoint, discover tools/resources/prompts, invoke MagicAPI-backed tools, read custom resources, render prompts, and receive standard protocol notifications according to negotiated capabilities.
