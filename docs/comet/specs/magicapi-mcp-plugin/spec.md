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

## 认证与 scope

插件自身不实现认证：入口认证由 starter 的 `McpAuthService` SPI 承担（Sa-Token OAuth2 实现条件装配，缺省 `PermitAllAuthService`），魔法插件只向 runtime 提供能力。token 缺失、过期、签名无效或已吊销时请求在入口被拒绝（401/403 语义），不进入业务分发。

## 传输与协议

单个 HTTP 端点同时提供 Streamable HTTP 传输与 SSE 流：`POST` 处理 JSON-RPC 消息、`GET` 建立 SSE 流、`DELETE` 删除会话并释放连接。协议版本协商成功后写回 `MCP-Protocol-Version` 与会话标识 `Mcp-Session-Id`，不支持的版本返回 invalid params 且不创建会话；未知或已过期的会话标识在传输层直接拒绝。

## 变更通知与集群

工具/资源/提示词注册内容变化时向已连接客户端分别推送 `tools/list_changed`、`resources/list_changed`、`prompts/list_changed`；资源订阅场景仅向订阅该 URI 的会话推送 `resources/updated`。会话删除、超时、完成或发送失败时释放对应 SSE emitter。

Redis-backed Magic-API cluster 启用时，本节点先通知本地 SSE 客户端再发布集群事件，其他节点落地时忽略自身 origin（回环抑制）、仅通知本节点持有的连接；未启用集群时保持单节点本地行为，整组集群 bean 不注册。

## Server-to-client 请求

服务端可向客户端发起 roots/list、sampling/createMessage、elicitation/create 请求，发起前检查会话 initialize 阶段声明的 client capabilities，pending request 记录 id/method/capability/session/创建与超时时间，客户端返回或超时后标记为 resolved/failed/timed out。

## 扩展 provider 与控制台诊断

用户通过 Spring Bean（`MagicApiMcpCapabilityProvider`）提供自定义工具、资源、提示词与 completion 实现，与动态能力并存。诊断入口预览 `mcp-resource` / `mcp-prompt` 时分别调用 `resources/read` 与 `prompts/get`，不绕过认证、会话与 provider 过滤。

## Scenario

以下为本 change 新增/修正的可验收场景（baseline 行为见上文正文，不重复列为验收项）。

- **Scenario M1**：控制台创建的 `mcp-server` 实例出现在 `MagicDynamicMcpServer.serverInfos()` 的结果中，`MagicMcpServerMagicDynamicRegistry.defaultMappings()` 覆盖到该实例；unregister 后从结果中消失。
- **Scenario M2**：`MagicApiMcpCapabilityRegistrar.unregisterInstance` 后，该实例在 `registeredTemplates` 与 `registeredCompletions` 中的登记项被清空；重新注册同一实例不残留旧增量状态。
- **Scenario M3**：`MagicApiMcpToolRegistry` 构建工具快照时，同名工具冲突以 provider（自定义能力 Bean）优先，与 `registeredResources` / `registeredPrompts` 的 `putIfAbsent` 语义一致；动态资源先占名后 provider 同名工具仍在快照中。
- **Scenario M4**：`samePrompt` 比对 prompt 的消息内容而非仅序列化 JSON：仅改文案（或角色、内容结构）再次注册时刷新对外暴露的 prompt；内容一致时不重复通知。
- **Scenario M5**：`MagicApiMcpToolRegistrar.resourceService()`、`MagicApiMcpServerRegistry.emptyArguments()`、`MagicApiMcpServerRegistry.isRegistered()`、`MagicApiMcpServerRegistry.newInstanceRegistrar()` 与 `MagicApiMcpToolRegistry.snapshot()` 上方无主 javadoc 已删除，全仓无调用方与残留引用。
