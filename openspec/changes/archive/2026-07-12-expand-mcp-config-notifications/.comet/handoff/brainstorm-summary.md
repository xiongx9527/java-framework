# Brainstorm Summary

- Change: expand-mcp-config-notifications
- Date: 2026-07-11

## 确认的技术方案

采用方案 B：分类映射。继续沿用配置刷新来源 → `ConfigChangeEvent` → `ConfigChangeEventListener` → `McpConfigurationChangeNotifier` → `McpNotificationService.broadcast(...)` 的链路，只扩展 `McpConfigurationChangeNotifier` 内部 key 映射。

只修改 `McpConfigurationChangeNotifier` 及相关测试；不修改 `ConfigMagicDynamicRegistry`、`ConfigChangeEvent`、`ConfigChangeEventListener`、`McpNotificationService`、SSE stream、MagicNotify cluster、MCP JSON-RPC 协议结构或前端控制台。

保留既有映射：

- `magic-api.plugin.mcp.tools.functions.*` → `notifications/tools/list_changed`
- `magic-api.plugin.mcp.tools.api.*` → `notifications/tools/list_changed`
- `magic-api.plugin.mcp.protocol.*` → tools/resources/prompts 三类 list_changed
- `magic-api.plugin.mcp.auth.*` → tools/resources/prompts 三类 list_changed
- `magic-api.plugin.mcp.headers.*` → tools/resources/prompts 三类 list_changed

新增精确 key 映射：

- `magic-api.plugin.mcp.endpoint` → tools/resources/prompts 三类 list_changed
- `magic-api.plugin.mcp.session-timeout-seconds` → tools/resources/prompts 三类 list_changed
- `magic-api.plugin.mcp.sse-stream-enabled` → tools/resources/prompts 三类 list_changed
- `magic-api.plugin.mcp.trusted-origins` → tools/resources/prompts 三类 list_changed

明确忽略：

- `magic-api.plugin.mcp.enabled`
- `magic-api.plugin.mcp.max-request-bytes`
- `magic-api.plugin.mcp.max-response-chars`
- 其他未列出的 `magic-api.plugin.mcp.*` key
- 非 MCP key

## 关键取舍与风险

- list_changed 不是配置变更专用事件；对 endpoint/session/sse/trusted origins 变化只能作为客户端刷新/重建假设的信号，不能保证旧连接自动迁移。
- 过度广播会带来噪音，因此以“客户端可见能力视图或交互语义”为纳入标准，并用测试固定忽略逻辑。
- Spring relaxed binding 使 Java 字段 `sessionTimeoutSeconds` 对应配置 key 通常为 `session-timeout-seconds`；实现和测试应使用实际配置 key 字符串。
- `enabled` 代表自动配置生命周期，运行期 list_changed 无法表达 bean 创建/销毁，因此保持忽略。

## 测试策略

- 扩展 `McpConfigurationChangeNotifierTest`，覆盖新增精确 key 触发三类 list_changed。
- 覆盖新增 key 与既有 key 混合时按 method 去重。
- 覆盖 `enabled`、`max-request-bytes`、`max-response-chars`、未知 MCP key 和非 MCP key 的忽略逻辑。
- 回跑 `MagicAPIMcpConfigurationTest`、`ConfigMagicDynamicRegistryTest` 和 MCP/config 定向测试。

## Spec Patch

无。当前 delta spec 已覆盖本次确认的新增 client-visible 配置变化与 non-client-visible 配置忽略场景。
