## Why

`MagicAPIMcpProperties` 支持通过 Spring 配置控制 MCP tools、API tool 暴露、协议版本、认证 scope 与传输 header，但这些配置在运行期通过 config client 或 MagicAPI `config` 资源刷新后，目前不会通知已连接的 MCP 客户端重新感知能力变化。

需要在 MCP 配置变化后复用现有 MCP server-to-client 通知链路，向客户端发送能力列表变更事件，避免客户端继续使用过期的 tools/resources/prompts 能力视图。

## What Changes

- 监听 `magic-api.plugin.mcp.*` 相关配置变化，覆盖 config client 发布的 `ConfigChangeEvent`，并兼容 MagicAPI `config` 资源变化刷新路径。
- 当 `tools.functions` 或 `tools.api` 相关配置变化时，发送 `notifications/tools/list_changed`。
- 当 `protocol`、`auth`、`headers` 相关配置变化时，发送 MCP 能力列表变更通知，提示客户端重新拉取能力视图。
- 非 `magic-api.plugin.mcp.*` 配置变化不得触发 MCP 能力列表变更通知。
- 复用现有 `McpNotificationService.broadcast(...)`、集群事件发布和 SSE 能力过滤行为，不引入新的 MCP 自定义事件协议。

## Capabilities

### New Capabilities

### Modified Capabilities
- `standard-mcp-server-plugin`: MCP 插件需要在配置刷新影响 MCP 能力或访问语义时发送已有 list_changed 通知。

## Impact

- Boot 层 MagicAPI MCP 插件配置与通知链路：`MagicAPIMcpProperties`、`MagicAPIMcpConfiguration`、MCP notification 相关类。
- 配置刷新链路：config client 的 `ConfigChangeEvent` / `@OnConfigChange` 机制，以及 MagicAPI `config` 资源动态注册刷新逻辑。
- 测试：新增配置变化触发 MCP list_changed 通知的单元测试，并覆盖自动配置注册行为。
- 不改变 MCP JSON-RPC 协议结构，不改变现有 tools/resources/prompts/list 行为，不改变非 MCP 配置刷新行为。
