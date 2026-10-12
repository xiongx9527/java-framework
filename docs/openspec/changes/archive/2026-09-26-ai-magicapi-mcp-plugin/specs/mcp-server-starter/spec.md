# Spec Delta

## MODIFIED Requirements

### Requirement: 与 Magic-API 集成

MCP Server SHALL 通过 AI 层 `magicapi-mcp-plugin` capability（基于 mcp starter / aj-mcp 内核）将 Magic-API 能力暴露给 MCP 客户端：函数工具默认注册、API 工具可配置放行、任务能力可注册；工具暴露范围受分组/路径/method/opt-in 规则约束；插件同时提供 mcp-resource / mcp-prompt 资源类型、Sa-Token OAuth2 认证与 scope、SSE 变更通知与集群分发。

#### Scenario: Magic-API 工具自动注册
- **WHEN** 用户启用 Magic-API MCP 插件
- **THEN** Magic-API 中的函数接口自动注册为 MCP 工具，AI 客户端可通过 MCP 调用

#### Scenario: 函数工具自动注册
- **WHEN** 用户启用 Magic-API MCP 插件
- **THEN** Magic-API 函数自动注册为 MCP 工具，AI 客户端可通过 `tools/list` 列出并 `tools/call` 调用

#### Scenario: API 工具按配置放行
- **WHEN** 用户启用 API 工具并配置分组/路径/method/opt-in 规则
- **THEN** 仅命中匹配规则的 Magic-API HTTP API 注册为 MCP 工具，其余不暴露

#### Scenario: 认证与 scope
- **WHEN** 插件启用认证且请求携带合法 token 与目标能力的 scope
- **THEN** MCP 请求可访问对应工具/资源/提示词；缺失 token、scope 不符时被拒绝

#### Scenario: 变更通知与集群
- **WHEN** 工具、资源或提示词变更且客户端已建立 SSE 连接（或集群已启用）
- **THEN** 本节点 SSE 客户端收到对应 list_changed / updated 通知，跨节点经 Redis 分发