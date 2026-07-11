---
comet_change: expand-mcp-config-notifications
role: technical-design
canonical_spec: openspec
---

# 扩展 MCP 配置通知覆盖面技术设计

## 背景

OpenSpec change `expand-mcp-config-notifications` 的 canonical spec 位于 `openspec/changes/expand-mcp-config-notifications/`。

已归档的 `notify-mcp-config-changes` 已建立运行期配置刷新到 MCP 客户端通知的主链路：配置刷新发布 `ConfigChangeEvent`，`ConfigChangeEventListener` 分发给 `@OnConfigChange(prefix = "magic-api.plugin.mcp")` 监听方法，`McpConfigurationChangeNotifier` 根据变化 key 映射到现有 MCP list_changed 通知，再由 `McpNotificationService.broadcast(...)` 发送到已连接客户端。

本次设计不重做事件链路，只补齐 `McpConfigurationChangeNotifier` 对更多客户端可见 MCP 配置 key 的识别范围，并用单元测试固定新增映射、去重和忽略逻辑。

## 目标

- 扩展 Boot 层 MCP 配置变化通知器对 `magic-api.plugin.mcp.*` 的覆盖面。
- 对影响客户端能力视图或交互语义的 key，复用既有：
  - `notifications/tools/list_changed`
  - `notifications/resources/list_changed`
  - `notifications/prompts/list_changed`
- 保持同一次配置刷新内 notification method 去重和确定性顺序。
- 明确忽略不适合用 list_changed 表达的 MCP key，避免无差别广播。
- 只修改 Boot MCP notifier 及相关测试。

## 非目标

- 不修改 `ConfigMagicDynamicRegistry`、`ConfigChangeEvent`、`ConfigChangeEventListener`。
- 不修改 `McpNotificationService`、SSE stream、MagicNotify cluster。
- 不新增 MCP JSON-RPC 通知类型，不改变协议结构。
- 不调整前端控制台、Cloud、AI、Product 层代码。
- 不把所有 `magic-api.plugin.mcp.*` key 自动映射为通知。

## 当前链路

```text
配置刷新来源
  ├─ 外部 config client
  └─ MagicAPI config 资源
        ↓
ConfigChangeEvent
        ↓
ConfigChangeEventListener
        ↓
@OnConfigChange(prefix = "magic-api.plugin.mcp")
        ↓
McpConfigurationChangeNotifier
        ↓
McpNotificationService.broadcast(method)
        ↓
本地 SSE 会话 + MagicNotify 集群广播
```

本次只改变 `McpConfigurationChangeNotifier` 内部的 key → notification method 映射。

## 设计决策

### 1. 采用分类映射

继续使用现有通知器集中维护配置 key 映射。通用配置事件层不理解 MCP 业务语义，避免把 MCP 专属判断泄漏到 `ConfigMagicDynamicRegistry` 或 `ConfigChangeEvent`。

分类规则：

| 配置影响 | 通知策略 |
| --- | --- |
| 改变 tools 暴露范围 | `notifications/tools/list_changed` |
| 改变 resources 视图 | `notifications/resources/list_changed` |
| 改变 prompts 视图 | `notifications/prompts/list_changed` |
| 改变整体能力视图、协议、认证、header、endpoint、session 或传输交互假设 | tools/resources/prompts 三类 list_changed |
| 只影响服务端内部限制或自动配置生命周期 | 不发送 list_changed |

### 2. 保留既有映射

现有行为保持兼容：

- `magic-api.plugin.mcp.tools.functions.*` → `notifications/tools/list_changed`
- `magic-api.plugin.mcp.tools.api.*` → `notifications/tools/list_changed`
- `magic-api.plugin.mcp.protocol.*` → tools/resources/prompts 三类 list_changed
- `magic-api.plugin.mcp.auth.*` → tools/resources/prompts 三类 list_changed
- `magic-api.plugin.mcp.headers.*` → tools/resources/prompts 三类 list_changed

### 3. 新增精确 key 映射

新增覆盖范围采用精确 key，不使用宽泛前缀：

- `magic-api.plugin.mcp.endpoint` → tools/resources/prompts 三类 list_changed
- `magic-api.plugin.mcp.session-timeout-seconds` → tools/resources/prompts 三类 list_changed
- `magic-api.plugin.mcp.sse-stream-enabled` → tools/resources/prompts 三类 list_changed
- `magic-api.plugin.mcp.trusted-origins` → tools/resources/prompts 三类 list_changed

这些 key 会影响客户端对 endpoint、会话存活、stream 交互或跨域信任边界的假设。当前没有独立的 `notifications/config/changed`，因此复用三类 list_changed 作为客户端刷新或重建交互假设的信号。

### 4. 明确忽略的 key

以下 key 不触发通知：

- `magic-api.plugin.mcp.enabled`
- `magic-api.plugin.mcp.max-request-bytes`
- `magic-api.plugin.mcp.max-response-chars`
- 其他未列出的 `magic-api.plugin.mcp.*` key
- 非 MCP key

`enabled` 属于自动配置生命周期，运行期 list_changed 无法表达 bean 创建或销毁。请求/响应大小限制属于服务端保护策略，不改变客户端可见 list，也不适合用 list_changed 表达。

## 实现方案

### `McpConfigurationChangeNotifier`

最小实现方式：

1. 增加 4 个精确 key 常量：
   - `ENDPOINT_KEY`
   - `SESSION_TIMEOUT_SECONDS_KEY`
   - `SSE_STREAM_ENABLED_KEY`
   - `TRUSTED_ORIGINS_KEY`
2. 增加一个私有判断方法，例如 `isCapabilityViewKey(String key)`，集中判断这 4 个精确 key。
3. 在 `collectNotificationMethods(String key, Set<String> methods)` 中：
   - 先保留 tools 映射；
   - 再保留 protocol/auth/headers 映射；
   - 再处理新增精确 key；
   - 未命中则不添加 method。
4. 继续使用 `LinkedHashSet` 去重，保持发送顺序稳定。

不需要新增抽象、策略类、枚举映射表或外部配置。当前映射数量很小，直接常量 + 分支最易读、最少变更。

### 测试

扩展 `McpConfigurationChangeNotifierTest`：

1. 新增精确 key 触发三类 list_changed：
   - `endpoint`
   - `session-timeout-seconds`
   - `sse-stream-enabled`
   - `trusted-origins`
2. 新增 key 与既有 key 同批刷新时按 method 去重。
3. 明确忽略：
   - `enabled`
   - `max-request-bytes`
   - `max-response-chars`
   - 未知 MCP key
   - 非 MCP key
4. 保留既有 tools/protocol/auth/headers 测试。

定向回归命令使用 Boot autoconfigure 模块：

```bash
mvn -f /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-boot/pom.xml \
  -pl fw-plus-boot-project/fw-plus-boot-autoconfigure \
  -Dtest='McpConfigurationChangeNotifierTest,ConfigMagicDynamicRegistryTest,MagicAPIMcpConfigurationTest' test
```

如后续 build 阶段发现相关测试命名或模块路径变化，以当前代码为准调整命令，但验证范围不扩大到无关模块。

## 边界条件

- `properties == null` 或空 Map：不发送通知。
- 同一刷新批次包含多个映射到同一 method 的 key：每个 method 最多发送一次。
- 同一刷新批次同时包含新增 key、既有 key、忽略 key：只发送命中的 method。
- 非 MCP key：即使进入 notifier，也不发送通知。
- Spring relaxed binding 使用实际配置 key 字符串；`sessionTimeoutSeconds` 对应 `session-timeout-seconds`。
- list_changed 只作为刷新信号，不承诺旧 SSE 连接自动迁移到新 endpoint 或新 header 语义。

## 风险与处理

| 风险 | 处理 |
| --- | --- |
| 过度广播造成客户端噪音 | 使用精确 key 映射，不做 `magic-api.plugin.mcp.*` 全量兜底 |
| endpoint/session 变化无法靠 list_changed 完整恢复旧连接 | 明确本次只发送刷新信号，不改变连接迁移语义 |
| 新增 key 与当前属性树不一致 | build 阶段先对照 `MagicAPIMcpProperties` 与测试失败信息确认实际 key |
| 忽略 key 被误认为漏通知 | 用测试固定忽略列表和未知 key 行为 |

## Spec Patch

无。当前 OpenSpec delta spec 已覆盖新增 client-visible MCP 配置变化、非 client-visible key 忽略、非 MCP key 忽略和同批去重场景。