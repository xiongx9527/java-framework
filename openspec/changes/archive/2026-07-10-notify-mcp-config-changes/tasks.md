## 1. MCP 配置变化通知组件

- [x] 1.1 新增 MCP 配置变化通知组件，集中判断 `magic-api.plugin.mcp.tools.functions.*`、`tools.api.*`、`protocol.*`、`auth.*`、`headers.*` 配置 key。
- [x] 1.2 通过现有 `ConfigChangeEvent` / `@OnConfigChange` 机制接入外部配置刷新事件。
- [x] 1.3 复用 `McpNotificationService.broadcast(...)` 发送去重后的 `tools/resources/prompts` list_changed 通知。

## 2. MagicAPI config 资源兼容

- [x] 2.1 调整 MagicAPI `config` 资源动态注册刷新链路，在配置刷新应用后暴露变更 key 给统一配置变化通知机制。
- [x] 2.2 确保 MCP 插件禁用或缺失时，MagicAPI config 刷新不依赖 MCP notification bean 且行为保持兼容。

## 3. 自动配置与测试

- [x] 3.1 在 MCP 自动配置中注册新增配置变化通知组件，并保持现有启用条件。
- [x] 3.2 新增单元测试覆盖 MCP tools/api 配置变化触发 `notifications/tools/list_changed`、非 MCP 配置变化不触发、同一刷新内通知去重。
- [x] 3.3 新增或调整测试覆盖 MagicAPI config 资源变化路径与 MCP disabled/absent 兼容场景。
- [x] 3.4 运行 Boot autoconfigure 模块中 MCP/config 相关测试，确认现有通知、集群广播和 lifecycle 行为未回归。
