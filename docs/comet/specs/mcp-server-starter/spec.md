# MCP Server Starter 能力规格（mcp-server-starter）

`fw-plus-mcp-server-spring-boot-starter`（aj-mcp 内核之上的 Spring Boot starter）提供 MCP 服务端装配：HTTP 传输端点、按 runtime key 隔离的认证 SPI、协议版本协商、会话管理、Redis 集群分发与 server 动态实例注册。Magic-API MCP 插件（`magicapi-mcp-plugin`）与自定义实现均基于本 starter 暴露能力。

## 传输与会话

单端点同时提供 Streamable HTTP 与 SSE 传输：`POST` 处理 JSON-RPC、`GET` 建立 SSE 流、`DELETE` 删除会话。协议版本协商成功后写回 `MCP-Protocol-Version` 与 `Mcp-Session-Id`；未知、过期或已删除的会话标识在传输层直接返回客户端错误，不进入业务分发。

## 自定义 header 名称

网关场景可能统一改写 header 名称。starter 提供四类 header 的名称配置：Accept、协议版本、会话 id、Origin。缺省值为内核默认名；用户配置自定义名称后，按配置名称解析 header，功能行为不变。

## 实例认证 provider 与 capability scope

每个 MCP runtime 独立配置认证方式。Starter 默认 runtime 与 `fw-plus.ai.mcp.servers[]` 每个命名 runtime 均支持逐实例选择 provider，清单为：`PermitAllAuthService` 与 Sa-Token 客户端可验证的全部认证方式——登录 token（`StpUtil`）、OAuth2 access token、JWT、API Key（`sa-token-apikey`）、SSO ticket（客户端向外部 SSO 服务端校验）。各 runtime 不得继承其他实例配置。Starter 必须使用真实 `McpAuthService` 实现承载 provider 行为，而不只是增加 UI/配置字段。

MCP server 只做验证，不实现认证服务，也不实现 Sa-Token 服务端：不提供登录端点、OAuth2 授权服务器、SSO 服务端、quick-login 端点与令牌签发；各 Sa-Token provider 仅调用对应模块的客户端验证 API。MCP server 不实现任何带页面操作的认证授权（无登录页、SSO 登录/回调页、授权确认页等浏览器页面交互入口），认证一律通过请求携带的凭据头/参数无界面完成。quick-login 模块没有客户端凭据验证 API，不纳入 provider 清单。不提供 Custom provider：不支持按实例注入 Java `McpAuthService` Bean，应用自定义校验逻辑不在本能力范围内。

每个实例仅选择一种 provider，不叠加认证。PermitAll 必须显式选择，不得作为默认值。无 provider 配置、所选凭据无效或验证未通过时，Servlet MVC 与 WebFlux 均以 HTTP 401 拒绝请求，不进入 MCP 内核，也不回退到其他 provider 或其他 runtime。显式选择 PermitAll 时同时绕过 capability scope 检查；其他认证方式仍按已启用的 scope 规则执行。

provider 按 classpath 可选装配：对应 sa-token 模块存在于 classpath 时该 provider 可选；模块缺失时该 provider 不可配置，已配置但模块缺失时 fail-closed（拒绝请求，不回退）。版本统一由 `sa-token.version`（1.46.0）管理。

认证成功返回的 `McpPrincipal` 必须携带 scopes，可为多个值或空集：OAuth2 access token 与 API Key 取凭据原生 scopes，JWT 取 claim 中的 scopes，登录 token 与 SSO ticket 无原生 scopes 时为空集；不提供 scope 映射配置。

API key 由内置 `sa-token-apikey` provider 承载，凭据经 Sa-Token API Key 验证 API 校验；MCP 不保存 secret，也不提供 API key 专用配置字段。

认证成功的请求仍可执行 capability scope 裁剪：入口检查 `McpPrincipal` 中 token scopes 是否覆盖请求目标能力（tools / resources / prompts）。显式启用顶层 `scope-check-enabled` 时，该设置对默认、命名及插件 runtime 生效；缺少或未覆盖目标 capability scope 的请求以 HTTP 403 拒绝，其他具备 scope 的能力不受影响。未启用时不做 capability scope 裁剪。scope 裁剪不能替代实例认证。

## 集群分发

协议消息经 `McpRedisBroadcaster` / `McpRedisMessageConsumer` 跨节点分发，接收方以 origin id 抑制回环；无 Redis 时相关 bean 不注册，退化为单节点行为。

## Scenario

以下为本 change 新增或修正的可验收场景。

- **Scenario S1**：`default` runtime 与两个不同 key 的命名 runtime 分别选择各自 provider（PermitAll 与不同 Sa-Token 认证方式）；认证选择及配置不会跨实例继承。
- **Scenario S2**：runtime 未配置认证方式、所选凭据无效或验证未通过时，MVC 与 WebFlux 均返回 401，MCP 内核不执行请求；任何 provider 都不得跨实例回退。
- **Scenario S3**：替换或更新一个 runtime 的认证 provider 不改变其他 runtime 的认证结果；某 sa-token 模块从 classpath 移除后，对应 provider 不可配置，已配置的实例 fail-closed 拒绝而非回退。
- **Scenario S4**：显式开启 capability scope 裁剪时，非 PermitAll 主体缺少目标 capability scope（包括 scopes 为空）的请求在 MVC 和 WebFlux 均以 HTTP 403 拒绝；显式选择 PermitAll 的 runtime 绕过 scope 裁剪。
- **Scenario S5**：各 Sa-Token provider 认证成功返回携带 scopes 的 `McpPrincipal`（OAuth2/API Key 取原生 scopes、JWT 取 claim，登录 token/SSO ticket 为空集）；MCP 侧不存在任何带页面操作的认证授权入口与 Sa-Token 服务端端点。
