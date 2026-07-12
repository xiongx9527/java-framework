# Comet Design Handoff

- Change: notify-mcp-config-changes
- Phase: design
- Mode: compact
- Context hash: 39e095aafc9f3a7a1edc282c584a7fb5644e73ba48fbcf9d920fc0a716ac83f9

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/notify-mcp-config-changes/proposal.md

- Source: openspec/changes/notify-mcp-config-changes/proposal.md
- Lines: 1-27
- SHA256: 2781b448c9fb269218d7d41c825811303b4afe78cc55f207e995bb87a10e668c

```md
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

```

## openspec/changes/notify-mcp-config-changes/design.md

- Source: openspec/changes/notify-mcp-config-changes/design.md
- Lines: 1-75
- SHA256: 6cc72836ac08527192a7fbb0df70814030d3154487eedae7b8c371f8300f615f

```md
## Context

现有 MCP 插件已经具备 tools/resources/prompts 的 `list_changed` 通知能力，并通过 `McpNotificationService.broadcast(...)` 复用 SSE 本地发送、client capability 过滤和 MagicNotify 集群广播链路。

配置刷新侧已有两条相关路径：

- config client / Apollo / Nacos 等外部配置刷新会发布 `ConfigChangeEvent`，并可通过 `@OnConfigChange(prefix = ...)` 统一消费。
- MagicAPI `config` 资源由 `ConfigMagicDynamicRegistry` 监听 `FileEvent` / `GroupEvent`，解析 YAML 后更新 Spring `Environment` 并调用 `ContextRefresher.refresh()`；目前这条路径没有额外发布 MCP 变更通知。

`MagicAPIMcpProperties` 中的 `tools.functions`、`tools.api`、`protocol`、`auth`、`headers` 变化会影响 MCP 客户端可见能力、工具暴露范围或访问语义，需要通知已连接客户端重新拉取能力列表。

## Goals / Non-Goals

**Goals:**
- 识别 `magic-api.plugin.mcp.*` 配置变化，并按影响范围发送已有 MCP list_changed 通知。
- 兼容 config client 发布的 `ConfigChangeEvent` 与 MagicAPI `config` 资源变化路径。
- 复用 `McpNotificationService.broadcast(...)`，保持现有 SSE、capability 过滤与集群传播行为。
- 避免非 MCP 配置变化触发 MCP 通知。

**Non-Goals:**
- 不新增自定义 MCP 配置变更通知方法。
- 不改变 MCP JSON-RPC 消息结构、初始化流程或现有 tools/resources/prompts/list 响应格式。
- 不重构 MagicAPI config 插件的整体刷新机制。
- 不把所有 Spring Environment 变化都无差别广播给 MCP 客户端。

## Decisions

### 1. 新增 MCP 配置变化通知组件

新增一个专门的 MCP 配置变化通知组件，放在 MCP notification 包内，由 `MagicAPIMcpConfiguration` 注册为 bean。该组件负责：

- 消费配置变化 key 集合；
- 判断是否属于 `magic-api.plugin.mcp.tools.functions.*`、`magic-api.plugin.mcp.tools.api.*`、`magic-api.plugin.mcp.protocol.*`、`magic-api.plugin.mcp.auth.*`、`magic-api.plugin.mcp.headers.*`；
- 根据影响范围去重后调用 `McpNotificationService.broadcast(...)`。

理由：把“哪些配置影响 MCP 客户端能力”集中在 MCP 插件内，避免把 MCP 业务判断散落到 config client 或 MagicAPI config 插件中。

### 2. ConfigChangeEvent 通过现有 `@OnConfigChange` 接入

组件方法使用现有配置事件机制监听 `magic-api.plugin.mcp` 前缀的 `ConfigChangeEvent`。外部配置刷新、Apollo、Nacos 等路径已经统一发布该事件，因此无需新增事件总线。

理由：复用已有 `ConfigChangeEventListener`，避免重复实现 prefix 过滤与 bean 分发逻辑。

### 3. MagicAPI `config` 资源变化后补发统一 ConfigChangeEvent 或调用 MCP 通知组件

`ConfigMagicDynamicRegistry` 已经监听 `config` 类型 `FileEvent` / `GroupEvent` 并刷新 Spring Environment。实现时应优先在其处理出 flatten 后的配置 key 时，向 Spring 发布 `ConfigChangeEvent`，使 MagicAPI config 与 config client 走同一套消费链路。

如现有注册流程不便安全拿到变化 key，可退而求其次让 `ConfigMagicDynamicRegistry` 依赖一个可选的 MCP 配置变化通知组件，传入当前 config 资源解析出的 key 集合。无论哪种方式，都应避免仅因为任意 config 文件变化就发送 MCP 通知。

理由：统一到 `ConfigChangeEvent` 可以让后续其他配置监听者也复用 MagicAPI config 刷新事件；但需要保证不会破坏现有 config 插件启动条件和无 MCP 场景。

### 4. 通知类型复用现有 list_changed

- `tools.functions` 或 `tools.api` 变化发送 `notifications/tools/list_changed`。
- `protocol`、`auth`、`headers` 变化发送已有能力列表变更通知组合，以提示客户端重新获取 MCP 视图；实现阶段优先使用 `tools/resources/prompts` 三类 list_changed，且同一事件内每类最多发送一次。

理由：用户明确要求发送“能力列表变更”，并且当前 MCP server capabilities 已声明 listChanged 支持；复用已有通知可以保持客户端兼容性与集群转发能力。

## Risks / Trade-offs

- [Risk] MagicAPI `config` 文件可能包含多个非 MCP 配置，粗粒度触发会造成无意义通知。→ 解析 flatten 后的 key，只在命中目标 MCP 前缀时通知。
- [Risk] `protocol/auth/headers` 不直接对应某一个 list endpoint。→ 采用已有 list_changed 通知组合作为刷新信号，不引入非标准事件。
- [Risk] 在无 MCP 插件或 MCP disabled 场景下 config 插件不应依赖 MCP 类导致启动失败。→ MCP 相关监听组件由 `MagicAPIMcpConfiguration` 注册；MagicAPI config 侧如需协作应使用可选依赖或仅发布通用 `ConfigChangeEvent`。
- [Risk] 同一次配置刷新可能包含多个相关 key，重复广播会打扰客户端。→ 对通知 method 去重。
- [Risk] Context refresh 与事件发布顺序不当可能让客户端刷新时读到旧配置。→ 在 `contextRefresher.refresh()` 完成之后再触发 MCP 通知。

## Migration Plan

- 默认行为保持兼容：没有配置变化时不发送额外通知。
- 已连接客户端会在相关配置变化后收到已有 MCP list_changed 通知；不需要客户端升级协议。
- 回滚时移除新增监听组件与 MagicAPI config 事件发布/桥接逻辑即可恢复当前行为。

## Open Questions

- `protocol/auth/headers` 变化最终发送三类 list_changed 还是只发送 tools list_changed，需要在实现时结合当前客户端使用方式确认；本 change 默认按能力列表变更信号处理，优先覆盖 tools/resources/prompts 三类。

```

## openspec/changes/notify-mcp-config-changes/tasks.md

- Source: openspec/changes/notify-mcp-config-changes/tasks.md
- Lines: 1-17
- SHA256: 7d26fcea97aca676cc38f65c9ca378a37d88d4487783f1f5ce1d556be7ad4d81

```md
## 1. MCP 配置变化通知组件

- [ ] 1.1 新增 MCP 配置变化通知组件，集中判断 `magic-api.plugin.mcp.tools.functions.*`、`tools.api.*`、`protocol.*`、`auth.*`、`headers.*` 配置 key。
- [ ] 1.2 通过现有 `ConfigChangeEvent` / `@OnConfigChange` 机制接入外部配置刷新事件。
- [ ] 1.3 复用 `McpNotificationService.broadcast(...)` 发送去重后的 `tools/resources/prompts` list_changed 通知。

## 2. MagicAPI config 资源兼容

- [ ] 2.1 调整 MagicAPI `config` 资源动态注册刷新链路，在配置刷新应用后暴露变更 key 给统一配置变化通知机制。
- [ ] 2.2 确保 MCP 插件禁用或缺失时，MagicAPI config 刷新不依赖 MCP notification bean 且行为保持兼容。

## 3. 自动配置与测试

- [ ] 3.1 在 MCP 自动配置中注册新增配置变化通知组件，并保持现有启用条件。
- [ ] 3.2 新增单元测试覆盖 MCP tools/api 配置变化触发 `notifications/tools/list_changed`、非 MCP 配置变化不触发、同一刷新内通知去重。
- [ ] 3.3 新增或调整测试覆盖 MagicAPI config 资源变化路径与 MCP disabled/absent 兼容场景。
- [ ] 3.4 运行 Boot autoconfigure 模块中 MCP/config 相关测试，确认现有通知、集群广播和 lifecycle 行为未回归。

```

## openspec/changes/notify-mcp-config-changes/specs/standard-mcp-server-plugin/spec.md

- Source: openspec/changes/notify-mcp-config-changes/specs/standard-mcp-server-plugin/spec.md
- Lines: 1-39
- SHA256: a50fd0a52286d2e08ee406c08bf21399741c0a3e48061396e0bdb80a5801b43f

```md
## ADDED Requirements

### Requirement: MCP configuration change notifications
The system SHALL detect runtime changes to MCP plugin configuration keys under `magic-api.plugin.mcp` that affect client-visible tools, protocol, authentication, or transport header semantics, and SHALL notify connected MCP clients using existing MCP list change notifications.

#### Scenario: Tool configuration changes
- **WHEN** a runtime configuration refresh changes any `magic-api.plugin.mcp.tools.functions.*` or `magic-api.plugin.mcp.tools.api.*` key
- **THEN** the system sends `notifications/tools/list_changed` over the MCP server-to-client stream to sessions that negotiated the tools list changed capability

#### Scenario: Protocol configuration changes
- **WHEN** a runtime configuration refresh changes any `magic-api.plugin.mcp.protocol.*` key
- **THEN** the system sends MCP list change notifications that prompt connected clients to refresh their MCP capability view

#### Scenario: Auth or header configuration changes
- **WHEN** a runtime configuration refresh changes any `magic-api.plugin.mcp.auth.*` or `magic-api.plugin.mcp.headers.*` key
- **THEN** the system sends MCP list change notifications that prompt connected clients to refresh their MCP capability view

#### Scenario: Non-MCP configuration changes are ignored
- **WHEN** a runtime configuration refresh changes keys outside `magic-api.plugin.mcp.*`
- **THEN** the system does not send MCP list change notifications for that refresh

#### Scenario: Multiple related keys in one refresh are deduplicated
- **WHEN** a single runtime configuration refresh changes multiple keys that map to the same MCP list change notification
- **THEN** the system sends that MCP list change notification at most once for the refresh

### Requirement: MagicAPI config resource changes trigger MCP configuration notifications
The system SHALL apply MCP configuration change notification behavior to MagicAPI `config` resource changes after those resources update the Spring Environment.

#### Scenario: MagicAPI config resource updates MCP tools configuration
- **WHEN** a MagicAPI `config` resource is created, saved, moved, deleted, enabled, disabled, or otherwise refreshed and the resulting configuration keys include `magic-api.plugin.mcp.tools.*`
- **THEN** the system sends the corresponding MCP list change notifications after the configuration refresh is applied

#### Scenario: MagicAPI config resource updates unrelated configuration
- **WHEN** a MagicAPI `config` resource changes only keys outside `magic-api.plugin.mcp.*`
- **THEN** the system refreshes configuration without sending MCP list change notifications

#### Scenario: MCP plugin disabled or absent
- **WHEN** MagicAPI `config` resources change while the MCP plugin is disabled or absent
- **THEN** the configuration refresh continues to work without requiring MCP notification beans

```
