# fw-plus-magicapi-plugin-mcp

独立的 MagicAPI MCP Server 插件，提供 Streamable HTTP 端点、Sa-Token OAuth2 资源服务器校验、MagicAPI 函数/API 工具暴露、控制台维护的 MCP resources/prompts、SSE 客户端通知，以及 Redis-only 集群事件分发。

插件不依赖已删除的 SSE 插件，不复用业务 `ssehttp`，也不新增任何 MCP datasource 目录。

## 依赖与启用

在业务应用引入 MCP starter 所属依赖后，默认启用：

```yaml
magic-api:
  plugin:
    mcp:
      enabled: true
      endpoint: /mcp
      protocol:
        default-version: 2025-11-25
        supported-versions:
          - 2025-11-25
          - 2025-06-18
```

默认端点：

- `POST /mcp`：JSON-RPC 请求
- `GET /mcp`：MCP SSE 流
- `DELETE /mcp`：删除 session，并释放本地 SSE emitter
- `GET /mcp/.well-known/oauth-protected-resource`：OAuth Protected Resource Metadata
- `POST /mcp/diagnostics/execute`：控制台诊断执行入口，复用 MCP JSON-RPC、session、scope 与 provider 语义

初始化默认协商 `2025-11-25`。如客户端传入不支持的 `protocolVersion`，服务端返回 JSON-RPC invalid params 错误且不会创建可用 session。成功响应会写回 `MCP-Protocol-Version`，初始化响应还会写回 `Mcp-Session-Id`。

## 认证与 Scope

MCP 侧作为 OAuth2 Protected Resource 使用 Sa-Token OAuth2 做 token 校验。插件不内置 Authorization Server。

```yaml
magic-api:
  plugin:
    mcp:
      auth:
        enabled: true
        resource: fw-plus-mcp
        authorization-server: http://localhost:3080
        tools-scope: mcp:tools
        resources-scope: mcp:resources
        prompts-scope: mcp:prompts
        logging-scope: mcp:logging
        roots-scope: mcp:roots
        sampling-scope: mcp:sampling
        elicitation-scope: mcp:elicitation
```

可通过提供自定义 `McpAccessTokenVerifier` Bean 扩展 token 解析和 scope 映射。关闭鉴权仅建议用于本地调试：

```yaml
magic-api:
  plugin:
    mcp:
      auth:
        enabled: false
```

## Header 配置

默认 header 名称兼容现有 MCP 客户端：

```yaml
magic-api:
  plugin:
    mcp:
      headers:
        accept: Accept
        protocol-version: MCP-Protocol-Version
        session-id: Mcp-Session-Id
        origin: Origin
```

如网关统一改写 header，可在这里配置自定义名称。

非 `initialize` 的 POST 请求携带未知、过期或删除的 session header 时，会在传输层直接返回客户端错误，避免进入 tools/resources/prompts 业务分发。

## Tools 暴露范围

函数工具默认启用，API 工具默认关闭，避免把已有 MagicAPI HTTP API 意外暴露给 MCP client。

```yaml
magic-api:
  plugin:
    mcp:
      tools:
        functions:
          enabled: true
          include-groups: []
          include-paths: []
          exclude-paths: []
        api:
          enabled: false
          include-groups:
            - /mcp/**
          include-paths:
            - /internal/**
          exclude-paths:
            - /internal/admin/**
          methods:
            - GET
            - POST
          require-opt-in: true
          opt-in-option-name: mcpTool
```

匹配规则：

- `exclude-paths` 优先级最高。
- `include-groups` 使用 MagicAPI group path 做 Ant 风格匹配。
- `include-paths` 使用 API path 或 function path 做 Ant 风格匹配。
- `methods` 只对 API 生效。
- `require-opt-in=true` 时，API 需要在 options/attributes 中显式设置 `mcpTool=true`。

Tool schema：

- 如果 Magic API/function metadata 中包含 `mcpInputSchema`，插件会优先使用该 JSON object 作为 `tools/list` 的 `inputSchema`。
- `mcpInputSchema` 无效或缺失时，安全回退为 `{ "type": "object" }`，不影响工具列出和调用。
- `tools/call` 仍返回 MCP content 与 `structuredContent`。

## Resources 与 Prompts

控制台新增两个 MagicAPI 资源类型：

- `mcp-resource`
- `mcp-prompt`

这两个资源类型是代码类资源，不是 datasource。文件内容由控制台维护，后端 storage 会校验 URI、MIME、prompt name、arguments、messages 等字段。MCP Prompt 的 `arguments` 与 `messages` 在控制台中使用 JSON 组件编辑，建议始终按结构化 JSON 维护。

MCP resource 示例：

```json
{
  "uri": "mcp://docs/readme",
  "name": "README",
  "mimeType": "text/plain",
  "description": "项目说明",
  "template": false
}
```

MCP prompt 示例：

```json
{
  "name": "summarize",
  "description": "总结输入内容",
  "arguments": [
    {"name": "topic", "required": true, "description": "主题"}
  ],
  "messages": [
    {
      "role": "user",
      "content": {
        "type": "text",
        "text": "请总结 {{topic}}"
      }
    }
  ]
}
```

Prompt 字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `arguments` | JSON Array | Prompt 参数定义数组，元素包含 `name`、`description`、`required` |
| `messages` | JSON Array | MCP Prompt 消息数组，元素包含 `role` 和结构化 `content` |
| `content.type` | String | 内容类型，文本消息使用 `text` |
| `content.text` | String | 文本内容，支持 `{{argumentName}}` 占位符 |

仍可通过 Spring Bean 扩展 provider：

- `McpToolProvider`
- `McpResourceProvider`
- `McpPromptProvider`
- `McpCompletionProvider`

## SSE 通知

`GET /mcp` 会注册本节点本地 SSE emitter。资源、提示词、工具相关文件变化时，服务端向已连接客户端发送 JSON-RPC notification：

- `notifications/tools/list_changed`
- `notifications/resources/list_changed`
- `notifications/resources/updated`
- `notifications/prompts/list_changed`

`resources/updated` 仅发送给已订阅对应 URI 的 session。session 删除、超时、完成或发送失败时会释放 emitter。

SSE 输出使用标准 `message` event，data 为 JSON-RPC message，可承载通知和 server-to-client request。

## Server-to-client 请求

插件支持服务端向客户端发起以下 capability request：

- `roots/list`
- `sampling/createMessage`
- `elicitation/create`

请求创建前会检查 initialize 阶段记录的 client capabilities。每个请求会记录 id、method、capability、session、创建时间、超时时间和状态。客户端返回 JSON-RPC response/error 后，dispatcher 会按 id 关联并将 pending request 标记为 resolved 或 failed；超时清理会标记为 timed out。

## 控制台诊断

控制台资源类型 `mcp-resource` 与 `mcp-prompt` 已提供运行预览入口：

- `mcp-resource` 预览调用 `resources/read`，用于检查 URI 与内容输出。
- `mcp-prompt` 预览调用 `prompts/get`，可通过 `previewArguments` 传入参数。
- 后端诊断入口为 `POST /mcp/diagnostics/execute`，不会绕过 MCP 鉴权、session 或 provider 过滤。

## Redis-only 集群

跨节点通知只在 Redis-backed MagicAPI cluster 启用时生效：

```yaml
magic-api:
  plugin:
    cluster:
      enabled: true
```

要求 Redis 插件和 MagicAPI cluster 相关 Bean 已存在。未启用 cluster 时，MCP 保持单节点本地行为。

集群模式下：

- 本地变更先通知本节点 SSE client，再通过 `MagicNotifyService` 发布 MCP change event。
- 接收远端 MCP change event 的节点只通知本节点持有的本地 SSE emitter。
- event 携带 origin id，节点会忽略自身回环事件，避免重复通知。

## 验证示例

初始化：

```bash
curl -sS http://localhost:8080/mcp \
  -H 'Accept: application/json' \
  -H 'Content-Type: application/json' \
  -H 'MCP-Protocol-Version: 2025-11-25' \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}'
```

列出 tools：

```bash
curl -sS http://localhost:8080/mcp \
  -H 'Accept: application/json' \
  -H 'Content-Type: application/json' \
  -H 'Mcp-Session-Id: <session-id>' \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}'
```

打开 SSE：

```bash
curl -N http://localhost:8080/mcp \
  -H 'Accept: text/event-stream' \
  -H 'Mcp-Session-Id: <session-id>'
```

## 排查

- 返回 `401/403`：检查 Sa-Token OAuth2 token、`authorization-server`、resource 和 scope。
- `tools/list` 为空：确认 `tools.api.enabled`、include/exclude、method 和 opt-in 配置；function 则检查 group/path 过滤。
- `resources/read` 找不到内容：确认控制台资源类型为 `mcp-resource`，URI 完整且合法。
- SSE 无通知：确认客户端已使用同一个 session 打开 `GET /mcp`，resource update 还需要先订阅 URI。
- 集群无通知：确认 `magic-api.plugin.cluster.enabled=true`、Redis cluster 监听正常，以及本节点确实有本地 SSE 连接。
