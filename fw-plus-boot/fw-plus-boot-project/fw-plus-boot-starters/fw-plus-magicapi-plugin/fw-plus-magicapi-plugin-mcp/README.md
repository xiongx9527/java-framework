# fw-plus-magicapi-plugin-mcp

独立的 MagicAPI MCP Server 插件，默认端点为 `/mcp`，不依赖已删除的 SSE 插件，也不复用业务 `ssehttp`。

## 能力

- Streamable HTTP：`POST /mcp`、`GET /mcp`、`DELETE /mcp`
- JSON-RPC dispatcher：`initialize`、`notifications/initialized`、`ping`
- Tools：`tools/list`、`tools/call`
- Resources：`resources/list`、`resources/read`、`resources/templates/list`、`resources/subscribe`、`resources/unsubscribe`
- Prompts：`prompts/list`、`prompts/get`
- Completion：`completion/complete`
- Logging：`logging/setLevel`、`notifications/message`
- Server 调用 client capability：`roots/list`、`sampling/createMessage`、`elicitation/create`

## OAuth2

OAuth2 使用 Sa-Token OAuth2。插件只做 MCP Resource Server / Protected Resource 侧适配，不内置 Authorization Server。

默认配置：

```yaml
magic-api:
  plugin:
    mcp:
      enabled: true
      endpoint: /mcp
      auth:
        enabled: true
        resource: fw-plus-mcp
        authorization-server: http://localhost:3080
        tools-scope: mcp:tools
        resources-scope: mcp:resources
        prompts-scope: mcp:prompts
        logging-scope: mcp:logging
```

## Provider SPI

MCP 内容通过 Spring Bean Provider 扩展，不使用 datasource 资源目录：

- `McpToolProvider`
- `McpResourceProvider`
- `McpPromptProvider`
- `McpCompletionProvider`

不要新增 `mcp-datasource`、`resources-datasource`、`prompts-datasource` 或任何 `*-datasource` 类型来承载 MCP 内容。
