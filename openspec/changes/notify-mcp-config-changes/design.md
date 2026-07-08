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
