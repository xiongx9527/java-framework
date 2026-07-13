## Why

已归档的 `notify-mcp-config-changes` 已让 MCP tools、protocol、auth、headers 等配置变化触发客户端能力刷新通知，但 `MagicAPIMcpProperties` 中仍存在若干会影响客户端可见能力或运行期交互语义的配置尚未纳入通知覆盖。

需要扩展 MCP 配置变化通知覆盖面，避免客户端在 endpoint、session、logging、completion 等相关配置运行期刷新后继续使用过期的能力视图或交互假设。

## What Changes

- 扩展 Boot 层 MCP 配置变化通知器对更多 `magic-api.plugin.mcp.*` 配置 key 的识别范围。
- 对确认为影响客户端可见能力视图的配置变化，复用现有 `notifications/tools/list_changed`、`notifications/resources/list_changed`、`notifications/prompts/list_changed` 通知。
- 保持同一次配置刷新内 notification method 去重。
- 明确不影响客户端能力视图或交互语义的 MCP 配置 key 不触发通知。
- 通过单元测试覆盖新增 key 映射、去重和忽略逻辑。

## Capabilities

### New Capabilities

### Modified Capabilities
- `standard-mcp-server-plugin`: 扩展 MCP 配置变化通知需求，使更多影响客户端能力视图或交互语义的 `magic-api.plugin.mcp.*` 配置运行期变化能够触发既有 list_changed 通知。

## Impact

- Boot 层 MCP 插件配置通知链路：`McpConfigurationChangeNotifier` 及其测试。
- Boot 层 MCP 自动配置与属性使用点：对照 `MagicAPIMcpProperties`、lifecycle、logging、completion、transport/session 相关 handler 确认哪些 key 需要纳入通知。
- MagicAPI config 通用事件发布链路保持不变，只复用既有 `ConfigChangeEvent` 分发行为。
- 不改变 MCP JSON-RPC 协议结构，不改变现有 `McpNotificationService`、SSE 本地发送或 MagicNotify 集群广播机制。
