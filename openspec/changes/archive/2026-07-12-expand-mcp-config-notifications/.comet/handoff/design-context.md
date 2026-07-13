# Comet Design Handoff

- Change: expand-mcp-config-notifications
- Phase: design
- Mode: compact
- Context hash: 806187f41b6012f50598a73524efe0fabf7bd4ad275d414baedac0015aba328b

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/expand-mcp-config-notifications/proposal.md

- Source: openspec/changes/expand-mcp-config-notifications/proposal.md
- Lines: 1-27
- SHA256: 0c683d1e1d206f4752a7b085774dd94191cf4f1d0b165ecd1e1acb9922722a9b

```md
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

```

## openspec/changes/expand-mcp-config-notifications/design.md

- Source: openspec/changes/expand-mcp-config-notifications/design.md
- Lines: 1-79
- SHA256: 21fc38d923d413333dffc630ae966bcc517161778d4f624e750729b2c4da71ad

```md
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

```

## openspec/changes/expand-mcp-config-notifications/tasks.md

- Source: openspec/changes/expand-mcp-config-notifications/tasks.md
- Lines: 1-18
- SHA256: c378739ceb9bbeb9387e7cba259a9ad9490ec0ae6eb4b0a9e56c7d3131d5ac8d

```md
## 1. 配置属性影响面确认

- [ ] 1.1 对照 `MagicAPIMcpProperties` 梳理当前 `magic-api.plugin.mcp.*` 属性树，标记已覆盖、候选新增和明确忽略的 key 前缀。
- [ ] 1.2 追踪候选新增 key 在 lifecycle、transport/session、logging、completion、provider/handler 中的使用点，确认是否影响客户端可见能力视图或交互语义。
- [ ] 1.3 形成最终 key → notification method 映射清单，并记录不纳入通知的 MCP key 理由。

## 2. 通知器映射扩展

- [ ] 2.1 为新增覆盖 key 编写 `McpConfigurationChangeNotifierTest` 失败用例，覆盖对应 tools/resources/prompts 或能力视图刷新通知。
- [ ] 2.2 扩展 `McpConfigurationChangeNotifier` 的 key 前缀识别和通知映射，保持 Java 8 兼容和确定性顺序。
- [ ] 2.3 补充同批刷新内新增 key 与既有 key 混合时的去重测试。
- [ ] 2.4 补充未纳入 MCP key 和非 MCP key 不触发通知的回归测试。

## 3. 自动配置与兼容验证

- [ ] 3.1 运行 `McpConfigurationChangeNotifierTest`，确认新增映射、去重和忽略逻辑通过。
- [ ] 3.2 运行 `MagicAPIMcpConfigurationTest` 和 `ConfigMagicDynamicRegistryTest`，确认 MCP disabled/absent 与 MagicAPI config 通用事件链路不回归。
- [ ] 3.3 运行 Boot autoconfigure 模块中 MCP/config 相关定向测试，确认现有通知、集群广播和 lifecycle 行为未回归。

```

## openspec/changes/expand-mcp-config-notifications/specs/standard-mcp-server-plugin/spec.md

- Source: openspec/changes/expand-mcp-config-notifications/specs/standard-mcp-server-plugin/spec.md
- Lines: 1-32
- SHA256: 2428da40ef8e733de14aad6c6e6d46209b1c3934f1ba1c4dfbff7c7667c36e06

```md
## MODIFIED Requirements

### Requirement: MCP configuration change notifications
The system SHALL detect runtime changes to MCP plugin configuration keys under `magic-api.plugin.mcp` that affect client-visible tools, resources, prompts, declared capabilities, protocol negotiation, session behavior, authentication, transport header semantics, endpoint interaction, logging, or completion semantics, and SHALL notify connected MCP clients using existing MCP list change notifications.

#### Scenario: Tool configuration changes
- **WHEN** a runtime configuration refresh changes any `magic-api.plugin.mcp.tools.functions.*` or `magic-api.plugin.mcp.tools.api.*` key
- **THEN** the system sends `notifications/tools/list_changed` over the MCP server-to-client stream to sessions that negotiated the tools list changed capability

#### Scenario: Protocol configuration changes
- **WHEN** a runtime configuration refresh changes any `magic-api.plugin.mcp.protocol.*` key
- **THEN** the system sends MCP list change notifications that prompt connected clients to refresh their MCP capability view

#### Scenario: Auth or header configuration changes
- **WHEN** a runtime configuration refresh changes any `magic-api.plugin.mcp.auth.*` or `magic-api.plugin.mcp.headers.*` key
- **THEN** the system sends MCP list change notifications that prompt connected clients to refresh their MCP capability view

#### Scenario: Additional client-visible MCP configuration changes
- **WHEN** a runtime configuration refresh changes an additional `magic-api.plugin.mcp.*` key that affects client-visible capability lists, declared capabilities, endpoint interaction, session behavior, logging, or completion semantics
- **THEN** the system sends the corresponding existing MCP list change notifications for the affected tools, resources, prompts, or overall capability view

#### Scenario: Non-client-visible MCP configuration changes are ignored
- **WHEN** a runtime configuration refresh changes an MCP configuration key that does not affect client-visible capability lists or MCP interaction semantics
- **THEN** the system does not send MCP list change notifications for that key

#### Scenario: Non-MCP configuration changes are ignored
- **WHEN** a runtime configuration refresh changes keys outside `magic-api.plugin.mcp.*`
- **THEN** the system does not send MCP list change notifications for that refresh

#### Scenario: Multiple related keys in one refresh are deduplicated
- **WHEN** a single runtime configuration refresh changes multiple keys that map to the same MCP list change notification
- **THEN** the system sends that MCP list change notification at most once for the refresh

```
