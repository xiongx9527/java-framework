## Context

`notify-mcp-config-changes` 已建立 MCP 配置变化通知主链路：外部 config client 与 MagicAPI `config` 资源都会发布通用 `ConfigChangeEvent`，`McpConfigurationChangeNotifier` 通过 `@OnConfigChange(prefix = "magic-api.plugin.mcp")` 消费 MCP 配置 key，并复用 `McpNotificationService.broadcast(...)` 发送既有 MCP list_changed 通知。

当前通知器覆盖 `tools.functions.*`、`tools.api.*`、`protocol.*`、`auth.*`、`headers.*`。后续改进重点不是重做事件链路，而是对照 `MagicAPIMcpProperties` 和 MCP handler/provider 使用点，补齐那些会影响客户端可见能力视图或交互语义、但尚未纳入映射的配置 key。

约束：
- 仅修改 Boot 层 MCP/config 相关后端实现与测试。
- 继续使用既有 `ConfigChangeEvent`、`@OnConfigChange` 和 `McpNotificationService.broadcast(...)`。
- 不新增 MCP 自定义通知协议，不改变 JSON-RPC 消息结构。

## Goals / Non-Goals

**Goals:**
- 建立明确的 MCP 配置 key 分级规则：哪些运行期配置变化应触发能力刷新通知，哪些不应触发。
- 扩展 `McpConfigurationChangeNotifier` 对更多影响客户端能力或交互语义的 `magic-api.plugin.mcp.*` key 的通知映射。
- 保持同批刷新内通知 method 去重和确定性顺序。
- 用单元测试覆盖新增映射、去重和忽略逻辑。

**Non-Goals:**
- 不改变 MCP JSON-RPC 协议、初始化返回结构或 capability 字段定义。
- 不调整前端控制台、Cloud/AI/Product 层代码。
- 不重构 `McpNotificationService`、SSE stream、MagicNotify 集群广播或 MagicAPI config 事件发布机制。
- 不把所有 `magic-api.plugin.mcp.*` 配置无差别映射为通知。

## Decisions

### 1. 以“客户端可见能力视图或交互语义”作为纳入标准

候选 key 必须满足至少一个条件才纳入通知映射：

- 会改变 `tools/list`、`resources/list`、`prompts/list` 或相关能力可见性。
- 会改变 MCP 初始化后客户端对协议、会话、认证、header 或 endpoint 交互的假设。
- 会改变 logging、completion 等已声明 capability 的可用性或使用边界。

不满足这些条件的配置即使属于 `magic-api.plugin.mcp.*`，也不应触发 list_changed，以避免无意义广播。

### 2. 通知器继续集中维护 key → notification method 映射

继续把 MCP 业务语义集中在 `McpConfigurationChangeNotifier`，不让 `ConfigMagicDynamicRegistry` 或通用 config event 层理解 MCP 细节。

理由：归档变更已经证明该结构兼容 config client、MagicAPI config 资源和 MCP disabled/absent 场景；本次只扩展映射，不改变事件架构。

### 3. 新增映射优先复用现有三类 list_changed

新增 key 根据影响范围映射到已有通知：

- 影响工具暴露范围的配置 → `notifications/tools/list_changed`。
- 影响资源视图的配置 → `notifications/resources/list_changed`。
- 影响提示词视图的配置 → `notifications/prompts/list_changed`。
- 影响整体能力视图、会话/传输交互语义或多类能力声明的配置 → tools/resources/prompts 三类通知组合。

不引入新事件，例如 `notifications/config/changed`。

### 4. 具体 key 清单在实现阶段基于代码确认

proposal 中列出的 endpoint、session、logging、completion 是候选方向，最终纳入范围应以 `MagicAPIMcpProperties` 当前字段及其使用点为准。实现阶段需要逐项确认：

- 配置是否支持运行期刷新后被相关 bean 读取。
- 配置变化是否需要客户端重新拉取 list 或重新理解能力。
- 是否已有其他资源变更通知覆盖该场景。

## Risks / Trade-offs

- [Risk] 过度覆盖会导致无意义 list_changed 广播。→ 使用“客户端可见能力视图或交互语义”标准筛选 key，并为忽略场景补测试。
- [Risk] 某些配置变化可能影响 endpoint/header，但旧连接未必能通过 list_changed 完整恢复。→ 本次仅复用现有通知作为刷新信号，不承诺连接迁移或客户端重连策略。
- [Risk] completion/logging 等能力是否由配置直接控制需要代码确认。→ 在 build 阶段先对照 `MagicAPIMcpProperties` 和 handler 使用点，再决定是否纳入。
- [Risk] 只做单元测试可能遗漏真实 Spring 事件分发差异。→ 复用已有 `@OnConfigChange` 分发测试；如新增 key 只影响通知映射，单元测试足够覆盖主要风险。

## Migration Plan

- 默认行为保持兼容：未命中的配置仍不触发通知。
- 新增命中的配置变化只发送既有 list_changed 通知；客户端无需协议升级。
- 如新增映射造成噪音，可回滚 `McpConfigurationChangeNotifier` 的对应 key 映射和测试。

## Open Questions

- `MagicAPIMcpProperties` 中 endpoint/session/logging/completion 相关字段的最终 key 名称与运行期读取方式，需要在实现阶段以当前代码为准确认。
- endpoint 或 session timeout 变化是否应发送三类 list_changed，还是应只记录为非 list 能力变化，需要结合当前客户端刷新行为确认。
