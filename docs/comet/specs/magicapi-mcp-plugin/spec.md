# Magic-API MCP 插件能力规格（magicapi-mcp-plugin）

AI 层 `fw-plus-ai-magicapi-plugin-mcp-server` 基于 mcp starter（aj-mcp 内核）把 Magic-API 能力暴露为 MCP 能力：函数与 API 可注册为 MCP 工具，控制台维护 `mcp-resource` / `mcp-prompt` / `mcp-server` 三类资源类型，能力变更经 SSE 通知与 Redis 集群分发跨节点传播，并提供控制台诊断入口。插件不反向依赖任何 Boot 层已删除模块。

## 模块结构

实现位于 `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure` 的 `magicapi.plugin.mcp` 包，分包：`config/`、`resource/`（`model/`、`service/`、`storage/`）、`spi/`、`starter/`、`cluster/`、`web/`。

## 工具暴露

Magic-API 函数默认注册为 MCP 工具，HTTP API 默认关闭、需显式 `tools.api.enabled=true`。暴露范围由分组（group）、请求路径、HTTP method 与 opt-in 开关约束，语义为「排除优先、白名单命中外放」：Ant 风格匹配 group path 与 path，`exclude-paths` 优先级最高，`require-opt-in=true` 时仅带显式 opt-in 标记的 API 外放。`inputSchema` 取元数据 `mcpInputSchema`，缺失或无效时安全回退为 `{ "type": "object" }`，工具仍可列出与调用。

工具快照由 `MagicApiMcpToolRegistry` 统一构建，来源包括 Magic-API 动态能力与用户通过 `MagicApiMcpCapabilityProvider` Bean 提供的自定义能力。

## 资源类型

`mcp-resource` 与 `mcp-prompt` 由控制台维护文件内容，插件负责 storage 校验（资源 URI/MIME/name/description/template；prompt 的 name/arguments/messages，message 含 role 与结构化 content、文本支持 `{{argumentName}}` 占位符）并按 MCP 语义暴露：`resources/list`、`resources/read`、`prompts/list`、`prompts/get`，带 template 标记的资源按 MCP 语义暴露 resource template 与 completion。

`mcp-server` 资源类型支持控制台维护的 MCP server 动态实例：每个实例拥有独立的 transport registry 与端点路由，`MagicDynamicMcpServer` 在资源创建时构建独立 server、在资源删除时销毁，`McpRoutingRegistrar` 提供运行期 `registerRuntime/unregisterRuntime`。

## 实例认证 provider

Magic-API MCP 插件为 `magic-api.plugin.mcp.servers[]` 静态实例和控制台创建、修改、启停的动态 `mcp-server` 资源提供逐实例认证方式选择。provider 清单与 MCP Server Starter 完全一致：`PermitAllAuthService` 以及 Sa-Token 客户端可验证的全部认证方式——登录 token（`StpUtil`）、OAuth2 access token、JWT、API Key（`sa-token-apikey`）、SSO ticket（客户端向外部 SSO 服务端校验）。每个实例只选择一种 provider，实例间不得继承或共享认证。不提供 Custom provider：不再支持选择 Spring Bean 或 `.ms` function 做认证适配，插件不再提供 `.ms` 认证适配器。

各 Sa-Token provider 按 classpath 可选装配：对应 sa-token 模块存在时该 provider 可在控制台选择；模块缺失时该 provider 不可选，已配置但模块缺失时 fail-closed 拒绝请求，不回退到其他 provider。版本统一由 `sa-token.version`（1.46.0）管理。

MCP server 只做验证，不实现认证服务与 Sa-Token 服务端：不提供登录端点、OAuth2 授权服务器、SSO 服务端、quick-login 端点与令牌签发；不实现任何带页面操作的认证授权（无登录页、SSO 登录/回调页、授权确认页等浏览器页面交互入口），认证一律通过请求携带的凭据头/参数无界面完成。quick-login 模块没有客户端凭据验证 API，不纳入 provider 清单。

认证成功返回的 `McpPrincipal` 必须携带 scopes，可为多个值或空集：OAuth2 access token 与 API Key 取凭据原生 scopes，JWT 取 claim 中的 scopes，登录 token 与 SSO ticket 无原生 scopes 时为空集；不提供 scope 映射配置。

API key 由内置 `sa-token-apikey` provider 承载：控制台不新增 API key 凭据字段，`mcp-server` 资源不得保存 API key 等任何 secret，只保存 provider 与非敏感选择字段。

未配置认证方式或所选凭据验证失败时，Servlet MVC 与 WebFlux 入口均以 HTTP 401 拒绝，且不进入 MCP 能力分发。启用 capability scope 检查后，非 PermitAll 主体缺少目标 scope（包括 scopes 为空）的请求返回 HTTP 403；显式选择 PermitAll 时绕过 capability scope 检查。

## 传输与协议

单个 HTTP 端点同时提供 Streamable HTTP 传输与 SSE 流：`POST` 处理 JSON-RPC 消息、`GET` 建立 SSE 流、`DELETE` 删除会话并释放连接。协议版本协商成功后写回 `MCP-Protocol-Version` 与会话标识 `Mcp-Session-Id`，不支持的版本返回 invalid params 且不创建会话；未知或已过期的会话标识在传输层直接拒绝。

## 变更通知与集群

工具/资源/提示词注册内容变化时向已连接客户端分别推送 `tools/list_changed`、`resources/list_changed`、`prompts/list_changed`；资源订阅场景仅向订阅该 URI 的会话推送 `resources/updated`。会话删除、超时、完成或发送失败时释放对应 SSE emitter。

Redis-backed Magic-API cluster 启用时，本节点先通知本地 SSE 客户端再发布集群事件，其他节点落地时忽略自身 origin（回环抑制）、仅通知本节点持有的连接；未启用集群时保持单节点本地行为，整组集群 bean 不注册。

## Server-to-client 请求

服务端可向客户端发起 roots/list、sampling/createMessage、elicitation/create 请求，发起前检查会话 initialize 阶段声明的 client capabilities，pending request 记录 id/method/capability/session/创建与超时时间，客户端返回或超时后标记为 resolved/failed/timed out。

## 扩展 provider 与控制台诊断

用户通过 Spring Bean（`MagicApiMcpCapabilityProvider`）提供自定义工具、资源、提示词与 completion 实现，与动态能力并存。诊断入口按必填 server key 解析单个 runtime，并使用该实例绑定的 `McpAuthService` 认证；缺少或无效认证时返回 401，未知 key 返回 404。诊断入口预览 `mcp-resource` / `mcp-prompt` 时分别调用 `resources/read` 与 `prompts/get`，不绕过实例认证、scope 检查或 provider 过滤。

## Scenario

以下为本 change 新增或修正的可验收场景；现有工具、资源、通知与诊断行为保持不变。

- **Scenario M1**：两个不同 key 的插件实例（无论来自静态配置还是动态 `mcp-server` 资源）分别选择各自 provider；所选认证方式与配置不跨实例串用。
- **Scenario M2**：动态实例创建、修改、启用和重建后，所选 provider 及其非敏感配置始终从当前资源读取；停用或删除实例后，其路由与会话按原有行为清理。
- **Scenario M3**：未配置认证方式或所选认证失败时，Servlet MVC 与 WebFlux 均返回 401，工具、资源、提示词及诊断能力均不执行；scope 不足（包括空 scopes）返回 403。
- **Scenario M4**：控制台可为 `mcp-server` 实例选择与 Starter 一致的 provider 清单（PermitAll + 可用的 Sa-Token 认证方式，classpath 缺失的不可选，无 Custom 选项）；资源只存 provider 与必要的非敏感选择字段，不存 API key 等任何凭据。
- **Scenario M5**：插件侧不存在带页面操作的认证授权入口与 Sa-Token 服务端端点；各 Sa-Token provider 认证成功返回携带 scopes 的主体（原生 scopes、JWT claim 或空集）。
