# MCP Server Starter 能力规格（mcp-server-starter）

`fw-plus-mcp-server-spring-boot-starter`（aj-mcp 内核之上的 Spring Boot starter）提供 MCP 服务端装配：HTTP 传输端点、认证 SPI、协议版本协商、会话管理、Redis 集群分发与 server 动态实例注册。Magic-API MCP 插件（`magicapi-mcp-plugin`）与自定义实现均基于本 starter 暴露能力。

## 传输与会话

单端点同时提供 Streamable HTTP 与 SSE 传输：`POST` 处理 JSON-RPC、`GET` 建立 SSE 流、`DELETE` 删除会话。协议版本协商成功后写回 `MCP-Protocol-Version` 与 `Mcp-Session-Id`；未知、过期或已删除的会话标识在传输层直接返回客户端错误，不进入业务分发。

## 自定义 header 名称

网关场景可能统一改写 header 名称。starter 提供四类 header 的名称配置：Accept、协议版本、会话 id、Origin。缺省值为内核默认名，即不配置时线上行为与本次改造前完全一致；用户配置自定义名称后，按配置名称解析 header，功能行为不变。

## 认证与 scope

入口认证由 `McpAuthService` SPI 承担：Sa-Token OAuth2 实现条件装配（sa-token-oauth2 在 classpath 且开启），缺省 `PermitAllAuthService`；用户提供自定义 `McpAuthService` Bean 时替换默认实现。校验通过的 token 写入 `McpPrincipal`，其中携带该 token 的 scopes。

capability scope 裁剪：入口在放行请求前检查 `McpPrincipal` 的 scopes 是否覆盖请求目标能力（tools / resources / prompts 及厂商扩展能力）。未覆盖时对目标能力的操作被拒绝，其他具备 scope 的能力不受影响；缺省配置（未启用 scope 校验）时不做裁剪，行为与改造前一致。关闭认证仅限本地调试，此时不校验 token 与 scope。

## 集群分发

协议消息经 `McpRedisBroadcaster` / `McpRedisMessageConsumer` 跨节点分发，接收方以 origin id 抑制回环；无 Redis 时相关 bean 不注册，退化为单节点行为。

## Scenario

以下为本 change 新增/修正的可验收场景（baseline 行为见上文正文，不重复列为验收项）。

- **Scenario S1**：starter 提供 Accept、协议版本、会话 id、Origin 四类 header 的名称配置键，缺省值等于当前内核默认 header 名；配置自定义名称后按配置名解析，功能行为不变，未配置时线上行为与改造前逐条一致。
- **Scenario S2**：入口消费 `McpPrincipal` 的 token scopes：token 缺少目标能力（tools/resources/prompts）对应 scope 时对该能力的请求被拒绝、其他能力不受影响，合法放行路径不变；缺省配置下不做 scope 裁剪，行为与改造前一致。
