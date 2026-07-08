---
comet_change: notify-mcp-config-changes
role: technical-design
canonical_spec: openspec
---

# MCP 配置变化通知设计

## 背景

MCP 插件已经通过 `McpNotificationService.broadcast(...)` 支持 `tools/resources/prompts` 三类 `list_changed` 通知，并保留 SSE 会话 capability 过滤与 MagicNotify 集群广播。当前缺口在配置刷新路径：外部配置中心会发布 `ConfigChangeEvent`，但 MCP 插件没有监听 MCP 配置变化；MagicAPI `config` 资源刷新只更新 Spring `Environment` 并调用 `ContextRefresher.refresh()`，没有把变化 key 暴露给统一配置事件机制。

本设计以 OpenSpec delta spec 为事实源，目标是在 MCP 相关配置运行期变化后，复用既有 list_changed 通知提醒客户端重新拉取能力视图，不新增 MCP 自定义协议事件。

## 方案

新增 `McpConfigurationChangeNotifier`，放在 MCP notification 包内，并由 `MagicAPIMcpConfiguration` 在 MCP 启用条件下注册。该组件通过 `@OnConfigChange(prefix = "magic-api.plugin.mcp")` 接入现有 `ConfigChangeEventListener`，方法参数使用过滤后的 `Map<String, Object>`，只处理 MCP 配置 key。

组件内部负责三件事：

- 判断 key 是否属于 `magic-api.plugin.mcp.tools.functions.*`、`magic-api.plugin.mcp.tools.api.*`、`magic-api.plugin.mcp.protocol.*`、`magic-api.plugin.mcp.auth.*`、`magic-api.plugin.mcp.headers.*`。
- 将命中的 key 映射到既有 MCP list_changed 通知。
- 对同一次刷新内的通知 method 去重后调用 `McpNotificationService.broadcast(...)`。

通知映射规则：

- `tools.functions.*`、`tools.api.*` 变化发送 `notifications/tools/list_changed`。
- `protocol.*`、`auth.*`、`headers.*` 变化发送 `notifications/tools/list_changed`、`notifications/resources/list_changed`、`notifications/prompts/list_changed` 三类通知，作为完整 MCP 能力视图刷新信号。
- 其他 `magic-api.plugin.mcp.*` key 和非 MCP key 不触发通知。

MagicAPI `config` 资源路径不直接依赖 MCP 类。`ConfigMagicDynamicRegistry` 在解析 YAML 得到 flatten key 后，继续按现有逻辑更新 `Environment` 并执行 `ContextRefresher.refresh()`；刷新完成后通过 `ApplicationEventPublisher` 发布 `ConfigChangeEvent`。这样 MagicAPI config 与 config client、Apollo、Nacos 进入同一套 `@OnConfigChange` 分发链路。

对于删除、禁用或取消注册配置资源的场景，`ConfigMagicDynamicRegistry` 应基于原 `ConfigInfo` 的脚本内容解析出 flatten key，并在属性源移除和刷新完成后发布事件。这样 MCP 客户端能感知能力收缩，而不是只在新增或保存时收到通知。

## 边界和兼容性

MCP 业务判断只存在于 MCP notification 包内，通用 config event 层不理解 MCP 语义。MagicAPI config 插件只发布通用 `ConfigChangeEvent`，因此在 MCP 插件禁用或缺失时不要求存在 MCP notification bean，配置刷新行为保持兼容。

事件发布顺序必须在 `ContextRefresher.refresh()` 完成之后，避免客户端收到通知后读取到旧配置。若 YAML 为空、配置未启用或属性源被移除，事件仍可携带原 key 集合；`McpConfigurationChangeNotifier` 只根据 key 判断，不依赖 value。

`McpNotificationService.broadcast(...)` 继续承担本地 SSE 发送、client capability 过滤和 MagicNotify 集群广播，不引入新的发送路径。

## 测试策略

新增 `McpConfigurationChangeNotifierTest`：

- `tools.functions.*` 和 `tools.api.*` 变化触发一次 tools list_changed。
- 多个 tools key 在同一次刷新内只触发一次 tools list_changed。
- `protocol.*`、`auth.*`、`headers.*` 变化触发 tools、resources、prompts 三类 list_changed。
- 非 MCP key 或未覆盖的 MCP key 不触发通知。

调整 `MagicAPIMcpConfigurationTest`，确认 MCP 自动配置注册 `McpConfigurationChangeNotifier`，并保持 `magic-api.plugin.mcp.enabled=false` 时不注册 MCP bean。

新增或调整 `ConfigMagicDynamicRegistry` 相关测试：

- MagicAPI `config` 资源更新 MCP key 时，`ContextRefresher.refresh()` 后发布 `ConfigChangeEvent`。
- 非 MCP 配置仍发布通用事件，但不会触发 MCP notifier。
- 配置删除、禁用或取消注册时仍携带原 flatten key。
- 未引入 MCP 自动配置时，config 刷新不依赖 MCP bean。

验证命令以 Boot autoconfigure 模块为边界，覆盖 MCP notification、starter、config registry 相关测试，并回跑现有 MCP 通知、集群广播和 lifecycle 测试。

## 风险

`protocol/auth/headers` 不直接对应单一 list endpoint。本设计按已确认决策发送三类 list_changed，代价是客户端刷新范围更大，但能避免能力视图局部过期。

MagicAPI config 删除路径需要从原脚本解析 key。如果原脚本解析失败，应保留现有错误处理，不应因为通知失败影响配置资源注册或取消注册主流程。
