# Comet Design Handoff

- Change: complete-magic-mcp-standard-plugin
- Phase: design
- Mode: compact
- Context hash: fe12ac8583afb5cfe0acd0334f5ac5b4645f7a8ce19d189e21edf467f5bd1772

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/complete-magic-mcp-standard-plugin/proposal.md

- Source: openspec/changes/complete-magic-mcp-standard-plugin/proposal.md
- Lines: 1-16
- SHA256: e4904399788dad2548415cd112b47e4e2439ae505f2122287391cf416ad57ad1

```md
# Complete Magic MCP Standard Plugin

## Why
Magic MCP 插件已经具备基础 Streamable HTTP、tools/resources/prompts、鉴权和控制台资源管理能力，但系统审查显示它仍停留在较早协议版本和基础查询能力层面：协议版本协商、capabilities 精确声明、server-to-client 请求闭环、控制台诊断预览、tool schema 生成和 SSE frame 兼容性仍不完整。若继续作为标准 MCP 插件发布，这些缺口会影响 MCP client 兼容性、排障体验和实际可用性。

## What Changes
- 升级协议目标到 MCP `2025-11-25`，实现受支持版本列表、版本协商和不支持版本错误。
- 强化 Streamable HTTP 语义：POST session 校验、协议版本响应头、SSE message event 输出。
- 精确声明 server capabilities，包括 listChanged、subscribe、logging、completion 以及可用的客户端请求能力。
- 完成 server-to-client 请求生命周期：roots/sampling/elicitation 请求发送、响应关联、错误、取消、超时清理。
- 增强 Magic API/function 暴露为 MCP tool 的 schema 质量。
- 增加 Magic 控制台 MCP 诊断/预览能力，用于验证 tools/resources/prompts 查询和调用。
- 补齐 README、OpenSpec 场景和回归测试。

## Scope
一期以兼容性和实用闭环为目标，不重写为独立 MCP SDK，不改变 MCP 资源/Prompt 的 Magic-API 存储模型，不引入 `mcp-datasource`。
```

## openspec/changes/complete-magic-mcp-standard-plugin/design.md

- Source: openspec/changes/complete-magic-mcp-standard-plugin/design.md
- Lines: 1-45
- SHA256: 8f92f1382f04ed2df7119d3b33212692219ab7cad2eccb6dac1ce5bec0152f0e

```md
# Design: Complete Magic MCP Standard Plugin

## 目标
把 Magic MCP 插件从“基础可用”提升到“标准兼容的一期完整能力”：MCP 2025-11-25 协议协商、精确能力声明、HTTP/SSE 传输语义、server-to-client 请求闭环、控制台诊断体验和工具 schema 质量。

## 方案概览
采用渐进补强，不替换现有模块边界：

1. 协议与传输层
   - 在 `MagicAPIMcpProperties` 增加 `protocol.supportedVersions`、`protocol.defaultVersion`、`clientRequestTimeoutSeconds` 等配置。
   - `McpLifecycleHandlers.initialize` 校验客户端 `protocolVersion`，只接受支持列表，默认返回 `2025-11-25`。
   - `McpHttpTransportService` 对非 initialize 且携带 session header 的 POST 做 session 存在/过期校验，并在响应中写回协议版本/session header。
   - `McpSseStreamService` 使用明确 SSE `message` event 发送 JSON-RPC message，提升跨 client 兼容性。

2. Capabilities 精确声明
   - 根据已注册 provider、订阅服务、通知能力和配置生成 capabilities。
   - tools/resources/prompts 声明 `listChanged`，resources 声明 `subscribe`，logging 声明存在，completion 与客户端请求能力按 provider/config 暴露。

3. Server-to-client 请求闭环
   - 在 session 中记录 pending client requests：id、method、capability、createdAt、timeoutAt、状态。
   - dispatcher 识别 JSON-RPC response/error 消息，交给 `McpClientRequestService` 完成匹配和状态更新。
   - 支持超时清理、取消标记、未匹配 response 忽略或记录。

4. Tool schema 增强
   - Magic API tool 从 path/query/body/options 尽可能生成 JSON Schema；缺失元数据时保留 `{type: object}` fallback。
   - Magic function tool 支持通过资源 metadata/options 读取参数描述；无法识别时不阻塞。
   - 维持 API 暴露默认关闭和 opt-in/scope 过滤。

5. 控制台诊断
   - 在 MCP 插件 console 增加诊断/预览入口：
     - resources: 输入/选择 URI 调用 `resources/read` 预览。
     - prompts: 输入 arguments 调用 `prompts/get` 预览。
     - tools: 查看 `tools/list` 并可填 arguments 调用 `tools/call`。
   - 诊断走后端受保护 endpoint，不直接绕过鉴权或 session 规则。

## 风险与取舍
- MCP 最新规范持续演进：本次锁定 `2025-11-25`，保留 supportedVersions 配置便于后续升级。
- Magic API 元数据不一定足够生成完整 schema：优先生成可确定字段，不能确定时 fallback，避免破坏现有功能。
- 控制台诊断可能涉及敏感 tool 调用：默认复用 MCP 鉴权与 scope，不新增无权限执行路径。

## 测试策略
- 单元测试：版本协商、capabilities、session 校验、SSE event、client response correlation、tool schema fallback。
- 控制器测试：POST session header、unsupported version、batch response、SSE open/send。
- provider 测试：API/function schema、scope/opt-in 不回退。
- 控制台可通过服务层和轻量 JS 测试/构建校验，若项目无前端测试则至少保证打包和静态结构。
```

## openspec/changes/complete-magic-mcp-standard-plugin/tasks.md

- Source: openspec/changes/complete-magic-mcp-standard-plugin/tasks.md
- Lines: 1-12
- SHA256: cdde224b997d65935b0c8c742ff01b3b6f3c37ae6e8f3556e3dd4375dcc270cf

```md
# Tasks

- [ ] Add MCP 2025-11-25 protocol negotiation tests and implementation
- [ ] Add POST session validation and protocol response header behavior
- [ ] Generate precise MCP capabilities from enabled features
- [ ] Complete server-to-client request correlation, response, timeout, and cancellation handling
- [ ] Emit standards-compatible SSE message events
- [ ] Enhance Magic API/function tool schema generation with safe fallback
- [ ] Add protected console diagnostic backend for tools/resources/prompts preview
- [ ] Add console UI/service entries for MCP diagnostics and previews
- [ ] Update README/OpenSpec purpose and integration docs
- [ ] Run MCP backend tests and relevant console build/checks
```

## openspec/changes/complete-magic-mcp-standard-plugin/specs/standard-mcp-server-plugin/spec.md

- Source: openspec/changes/complete-magic-mcp-standard-plugin/specs/standard-mcp-server-plugin/spec.md
- Lines: 1-86
- SHA256: 99cae74a4d9594a3b04211d31f19f576fe85bc31d1be1de271763715396abe61

[TRUNCATED]

```md
## MODIFIED Requirements
### Requirement: Streamable HTTP transport
The system SHALL expose a configurable MCP Streamable HTTP endpoint that supports POST, GET, and DELETE according to negotiated session behavior, and GET SHALL maintain an MCP-owned server-to-client SSE stream for notifications and server-initiated requests when streaming is enabled.

#### Scenario: Protocol version header is returned
- **WHEN** a client sends a valid MCP request with a negotiated protocol version
- **THEN** the HTTP response SHALL include the configured protocol version header with the negotiated value

#### Scenario: POST rejects invalid session before dispatch
- **WHEN** a client sends a non-initialize POST with an unknown, expired, deleted, or invalid MCP session header
- **THEN** the system returns a client error before dispatching feature logic

#### Scenario: SSE emits message events
- **WHEN** the server sends a JSON-RPC notification or server-to-client request over GET SSE
- **THEN** the stream emits a standards-compatible `message` event containing the JSON-RPC message payload

### Requirement: MCP lifecycle and ping
The system SHALL enforce MCP lifecycle ordering and support the base `ping` request.

#### Scenario: Supported protocol version negotiated
- **WHEN** a client initializes with a protocol version supported by the configured MCP server
- **THEN** the server returns that negotiated version, server info, capabilities, and session metadata

#### Scenario: Unsupported protocol version rejected
- **WHEN** a client initializes with an unsupported protocol version
- **THEN** the server returns a JSON-RPC invalid params error and does not create a usable MCP session

#### Scenario: Default protocol version is current
- **WHEN** a client initializes without an explicit protocol version
- **THEN** the server negotiates the configured default version, which SHALL default to `2025-11-25`

### Requirement: MCP capabilities
The system SHALL advertise capabilities that match the enabled MCP handlers, providers, subscriptions, logging, completion, and client-request features.

#### Scenario: List changed capabilities advertised
- **WHEN** tools, resources, or prompts support list change notifications
- **THEN** the corresponding capability includes `listChanged: true`

#### Scenario: Resource subscription capability advertised
- **WHEN** resource subscription is enabled
- **THEN** the resources capability includes `subscribe: true`

#### Scenario: Optional capabilities omitted when unavailable
- **WHEN** completion providers or client-request features are unavailable
- **THEN** the server does not advertise unsupported capabilities

### Requirement: Client capability requests
The system SHALL support server-to-client requests for client capabilities with request correlation, response handling, timeout cleanup, cancellation, and unsupported capability errors.

#### Scenario: Server-to-client request is correlated
- **WHEN** the server sends `roots/list`, `sampling/createMessage`, or `elicitation/create`
- **THEN** the request is stored with id, method, capability, session, and timeout metadata until a response, error, cancellation, or timeout resolves it

#### Scenario: Client response resolves pending request
- **WHEN** a client sends a JSON-RPC response or error with an id matching a pending server-to-client request
- **THEN** the system records the result or error and removes or marks the request as resolved

#### Scenario: Timed out client request cleaned up
- **WHEN** a pending server-to-client request exceeds the configured timeout
- **THEN** the system marks it timed out and releases pending state

### Requirement: MCP tools
The system SHALL expose MagicAPI functions and explicitly enabled MagicAPI APIs as MCP tools with useful schemas, content, structured content, and safe execution behavior.

#### Scenario: Tool schema generated from Magic metadata
- **WHEN** Magic API or function metadata describes parameters
- **THEN** `tools/list` returns an input schema including known parameter names, descriptions, and required flags

#### Scenario: Tool schema falls back safely
- **WHEN** Magic metadata is insufficient to generate a precise schema
- **THEN** `tools/list` returns a valid object schema without failing list or call operations

### Requirement: Console-managed MCP diagnostics
The system SHALL provide Magic console diagnostics for MCP tools, resources, and prompts without bypassing MCP authorization, session, or provider semantics.

#### Scenario: Resource read preview
- **WHEN** a console user previews an MCP resource
- **THEN** the diagnostic flow executes `resources/read` semantics and displays returned contents or JSON-RPC error details

#### Scenario: Prompt get preview
```

Full source: openspec/changes/complete-magic-mcp-standard-plugin/specs/standard-mcp-server-plugin/spec.md

