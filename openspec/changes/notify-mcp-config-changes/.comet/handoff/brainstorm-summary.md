# Brainstorm Summary

- Change: notify-mcp-config-changes
- Date: 2026-07-08

## 已确认事实

- 当前 change 使用 full workflow，处于 design 阶段。
- OpenSpec canonical spec 位于 `openspec/changes/notify-mcp-config-changes/specs/standard-mcp-server-plugin/spec.md`。
- MCP 已有 `McpNotificationService.broadcast(...)`，支持 `tools/resources/prompts` 三类 `list_changed` 通知，并保留 SSE capability 过滤与 MagicNotify 集群广播。
- 外部配置刷新已有 `ConfigChangeEvent` 与 `@OnConfigChange(prefix = ...)` 分发机制。
- MagicAPI `config` 资源刷新目前由 `ConfigMagicDynamicRegistry` 更新 `Environment` 并调用 `ContextRefresher.refresh()`，尚未发布 `ConfigChangeEvent`。

## 确认的技术方案

- 最终选择方案 1：新增 MCP 专用配置变化监听组件，并让 MagicAPI `config` 资源刷新后发布通用 `ConfigChangeEvent`。
- `McpConfigurationChangeNotifier` 放在 MCP notification 包内，通过 `@OnConfigChange(prefix = "magic-api.plugin.mcp")` 接入现有配置事件分发。
- `ConfigMagicDynamicRegistry` 只负责在 `ContextRefresher.refresh()` 完成后发布通用 `ConfigChangeEvent`，不直接依赖 MCP 类。
- 已确认 `protocol/auth/headers` 变化发送 `tools/resources/prompts` 三类 `list_changed`，作为完整 MCP 能力视图刷新信号。

## 关键取舍与风险

- `protocol/auth/headers` 变化没有唯一对应的 MCP list endpoint，已确认使用三类 list_changed 作为完整刷新信号。
- MagicAPI `config` 资源路径应避免依赖 MCP 类，保证 MCP disabled/absent 时兼容。
- 通知必须在 `ContextRefresher.refresh()` 完成后发送，避免客户端刷新时读到旧配置。

## 测试策略

- 覆盖 tools/api 配置变化只触发 tools list_changed。
- 覆盖 protocol/auth/headers 配置变化触发 tools/resources/prompts 三类 list_changed，且同一刷新内去重。
- 覆盖非 MCP 配置变化不触发通知。
- 覆盖 MagicAPI config 资源刷新后发布配置变化事件，且 MCP disabled/absent 时不依赖 MCP bean。

## Spec Patch

- 暂无。

## 状态

- 用户已确认最终设计摘要，Design Doc 已写入 `docs/superpowers/specs/2026-07-08-notify-mcp-config-changes-design.md`。
