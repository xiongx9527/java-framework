# magicapi-mcp-plugin Specification

## Purpose
在 AI 层基于 mcp starter（aj-mcp 内核）提供 Magic-API MCP 插件：将 Magic-API 的函数、API、任务自动暴露为 MCP 工具，新增控制台维护的 mcp-resource 与 mcp-prompt 资源类型，并提供 Sa-Token OAuth2 认证、SSE 变更通知、Redis 集群分发与诊断能力。

## Requirements

### Requirement: 工具暴露
插件 SHALL 支持将 Magic-API 的函数与 API 自动注册为 MCP 工具，且函数工具默认启用、API 工具默认关闭。用户 SHALL 能通过分组（group）、请求路径、HTTP method 与 opt-in 开关配置工具暴露范围，规则以「排除优先、白名单命中外放」为语义。

#### Scenario: 函数工具默认暴露
- **WHEN** 启用 Magic-API MCP 插件且未额外配置工具范围
- **THEN** 全部 Magic-API 函数注册为 MCP 工具，`tools/list` 列出、`tools/call` 可调用

#### Scenario: API 工具需显式启用
- **WHEN** 插件启用且未设置 `tools.api.enabled=true`
- **THEN** Magic-API 的 HTTP API 不注册为 MCP 工具，`tools/list` 不包含任何 API 对应工具

#### Scenario: 路径与分组过滤
- **WHEN** 配置包含 include-groups / include-paths / exclude-paths
- **THEN** 以 Ant 风格匹配 Magic-API group path 与 API/function path，`exclude-paths` 优先级最高，仅同时命中 include 且未命中 exclude 的能力注册为工具

#### Scenario: API 方法过滤
- **WHEN** 配置指定 `methods`（如 GET、POST）且启用 API 工具
- **THEN** 仅 HTTP method 命中的 API 注册为工具，其余不暴露

#### Scenario: opt-in 显式放行
- **WHEN** `require-opt-in=true` 且 API 的 options/attributes 未显式设置 opt-in 标记
- **THEN** 该 API 不注册为工具；设置了 opt-in 标记后才外放

#### Scenario: inputSchema 优先与回退
- **WHEN** Magic-API 函数/API 元数据包含 `mcpInputSchema`
- **THEN** 该 JSON object 作为 `tools/list` 返回的 `inputSchema`
- **WHEN** `mcpInputSchema` 缺失或无效
- **THEN** 安全回退为 `{ "type": "object" }`，工具仍可列出与调用，`tools/call` 返回 MCP content 与 `structuredContent`

### Requirement: resources 与 prompts 资源类型
插件 SHALL 提供 `mcp-resource` 与 `mcp-prompt` 两类 Magic-API 代码类资源类型，由控制台维护文件内容，插件负责存储校验（URI、MIME、prompt name、arguments、messages 等字段）并按 MCP 语义对外暴露。

#### Scenario: mcp-resource 校验与读取
- **WHEN** 控制台维护一个 `mcp-resource`，含合法 URI、name、mimeType、description 与 template 标记
- **THEN** 插件将其注册为 MCP resource，`resources/list` 列出、`resources/read` 按 URI 返回内容
- **WHEN** 资源 URI 非法或字段缺失
- **THEN** 后端 storage 拒绝并返回校验错误，不注册该资源

#### Scenario: mcp-prompt 校验与获取
- **WHEN** 控制台维护一个 `mcp-prompt`，含合法 name、arguments 与 messages（message 含 role 与结构化 content，文本支持 `{{argumentName}}` 占位符）
- **THEN** 插件将其注册为 MCP prompt，`prompts/list` 列出、`prompts/get` 按 name 展开占位符后返回
- **WHEN** prompt 字段缺失或与 MCP 语义不符
- **THEN** storage 拒绝并返回校验错误

#### Scenario: 资源模板与 completion
- **WHEN** 控制台维护带 template 标记的 resource，且配置了 completion 定义
- **THEN** 插件按 MCP 语义暴露 resource template 与 completion，`completion/complete` 可返回建议参数

### Requirement: 认证与 scope
插件 SHALL 作为 OAuth2 Protected Resource 校验访问凭据，支持按 MCP 能力（tools、resources、prompts 及厂商扩展能力）的 scope 裁剪，且允许用户替换 token 解析与 scope 映射实现；关闭认证仅限本地调试。

#### Scenario: 有效 token 放行
- **WHEN** MCP 请求携带合法的 Sa-Token OAuth2 token 且具备请求能力的对应 scope
- **THEN** 请求进入业务分发（tools/resources/prompts），结果按配置返回

#### Scenario: 无效或缺失 token 拒绝
- **WHEN** token 缺失、过期、签名无效或已吊销
- **THEN** 请求被拒绝并返回客户端错误（401/403 语义），不进入业务分发

#### Scenario: scope 裁剪
- **WHEN** token 合法的但缺少请求能力（tools/resources/prompts 等）对应的 scope
- **THEN** 对目标能力的操作被拒绝，其他具备 scope 的能力不受影响

#### Scenario: 认证关闭
- **WHEN** 明确配置关闭认证
- **THEN** MCP 端点放行所有请求，不校验 token 与 scope（仅限本地调试）

#### Scenario: 自定义 verifier
- **WHEN** 用户提供自定义的 token 解析与 scope 映射实现
- **THEN** 其解析结果代替默认逻辑，scope 映射按自定义实现生效

### Requirement: 传输与协议
插件 SHALL 通过单个 HTTP 端点同时提供 Streamable HTTP 传输与 SSE 流，支持 MCP 协议版本协商与会话语义，并支持在网关场景改写 header 名称。

#### Scenario: Streamable HTTP 端点
- **WHEN** 客户端对端点发送 `POST` JSON-RPC 请求或 `GET` SSE 请求、`DELETE` 删除会话
- **THEN** 插件分别处理 JSON-RPC 消息、建立 SSE 流、删除会话并释放连接资源

#### Scenario: 协议版本协商
- **WHEN** 客户端初始化请求 `protocolVersion` 受支持（如 `2025-11-25`、`2025-06-18`）
- **THEN** 协商成功并写回 `MCP-Protocol-Version`，创建会话并写回 `Mcp-Session-Id`
- **WHEN** 客户端传入不支持的 `protocolVersion`
- **THEN** 返回 JSON-RPC invalid params 错误，不创建可用会话

#### Scenario: 未知会话拦截
- **WHEN** 非 initialize 的请求携带未知、过期或已删除的会话标识
- **THEN** 在传输层直接返回客户端错误，不进入 tools/resources/prompts 业务分发

#### Scenario: 自定义 header 名称
- **WHEN** 网关统一改写 header，用户配置自定义 header 名称（Accept、协议版本、会话 id、Origin）
- **THEN** 插件按配置名称解析 header，功能行为不变

### Requirement: 变更通知
插件 SHALL 在资源、提示词、工具相关文件变化时，向已建立 SSE 连接的客户端推送对应 JSON-RPC notification，并在资源订阅场景仅向订阅对应 URI 的会话推送更新。

#### Scenario: 工具/资源/提示词列表变更通知
- **WHEN** 工具、资源或提示词注册内容发生变化
- **THEN** 服务端向已连接客户端分别推送 `tools/list_changed`、`resources/list_changed`、`prompts/list_changed` notification

#### Scenario: 资源订阅更新通知
- **WHEN** 某会话已订阅某资源 URI 且该 URI 内容更新
- **THEN** 仅向订阅该 URI 的会话推送 `resources/updated`，其他未订阅会话不收到

#### Scenario: 连接释放
- **WHEN** 会话删除、超时、完成或发送失败
- **THEN** 服务端释放对应的 SSE emitter，不再向该会话推送

### Requirement: 集群事件分发
插件 SHALL 在 Redis-backed Magic-API cluster 启用时，跨节点分发变更事件；未启用集群时保持单节点本地行为。

#### Scenario: 跨节点通知
- **WHEN** cluster 启用且某节点发生资源/提示词/工具变更
- **THEN** 本节点先通知本地 SSE 客户端，再发布集群事件；其他节点收到事件后仅通知本节点持有的本地 SSE 连接

#### Scenario: 回环抑制
- **WHEN** 节点收到自身发布的事件
- **THEN** 节点忽略该回环事件，不重复通知本地客户端

#### Scenario: 单节点行为
- **WHEN** cluster 未启用
- **THEN** 变更通知仅在本节点本地生效，不发起跨节点分发

### Requirement: Server-to-client 请求
插件 SHALL 支持服务端向客户端发起 MCP capability 请求（roots/list、sampling/createMessage、elicitation/create），并管理 pending request 的生命周期。

#### Scenario: capability 发起前校验
- **WHEN** 服务端需要向客户端发起某 capability 请求
- **THEN** 先检查该会话在 initialize 阶段声明的 client capabilities，仅对已声明的能力发起请求

#### Scenario: pending request 生命周期
- **WHEN** 服务端发起请求并记录 id、method、capability、session、创建/超时时间与状态
- **THEN** 客户端返回 response/error 后按 id 关联并标记为 resolved/failed；超时后标记为 timed out

### Requirement: 扩展 provider
插件 SHALL 允许用户通过 Spring Bean 提供自定义的工具、资源、提示词与 completion 实现，与 Magic-API 动态能力并存。

#### Scenario: 自定义 Bean 共存
- **WHEN** 用户提供自定义工具/资源/提示词/completion provider Bean
- **THEN** 其能力注册到 MCP Server，与 Magic-API 动态注册的能力一同列出与调用

### Requirement: 控制台诊断
插件 SHALL 提供诊断执行入口，用于检视 mcp-resource 与 mcp-prompt，且不绕过认证、会话与 provider 过滤。

#### Scenario: 资源与提示词预览
- **WHEN** 通过诊断入口预览 `mcp-resource` 或 `mcp-prompt`
- **THEN** 分别调用 `resources/read` 与 `prompts/get`（可携带预览参数）并返回结果

#### Scenario: 诊断受既有约束
- **WHEN** 通过诊断入口请求未获授权、无会话或已被过滤的能力
- **THEN** 诊断返回失败，行为与正常 MCP 请求一致，不绕过鉴权与过滤
