**fw-plus-ai-mcp-spring-boot-starter**

本模块为 `fw-plus-ai` 提供 MCP Server 的 Spring Boot Starter。默认基于 Spring MVC（Servlet）暴露 Streamable HTTP 端点；如应用已是响应式栈，可额外引入 WebFlux 传输，并由自动配置按应用类型互斥装配。

## 依赖

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-mcp-spring-boot-starter</artifactId>
</dependency>
```

## 配置项

```yaml
fw-plus:
  ai:
    mcp:
      enabled: true
      sse-enabled: true
      sse-path: /mcp/sse
      websocket-enabled: false
      websocket-path: /mcp/ws
      allowed-origins: "*"
      instances:
        - name: default
          path: /mcp
          session-timeout-seconds: 1800
          enabled: true
```

- `fw-plus.ai.mcp.enabled`：是否启用 MCP Server 自动配置；默认 `true`。
- `fw-plus.ai.mcp.sse-enabled`：是否启用 SSE (Streamable HTTP) 传输；默认 `true`。
- `fw-plus.ai.mcp.sse-path`：SSE 端点路径；默认 `/mcp/sse`。
- `fw-plus.ai.mcp.websocket-enabled`：是否启用 WebSocket 传输；默认 `false`。
- `fw-plus.ai.mcp.websocket-path`：WebSocket 端点路径；默认 `/mcp/ws`。
- `fw-plus.ai.mcp.allowed-origins`：端点允许的来源；默认不过滤。
- `fw-plus.ai.mcp.instances`：多实例配置；未配置时会提供一个默认实例。
- 每个实例支持 `name`、`path`、`session-timeout-seconds`、`enabled`。

## 传输栈选择

| 传输类型 | 引入依赖 | 生效配置 |
| --- | --- | --- |
| Streamable HTTP (SSE) | Starter 默认传递 `spring-boot-starter-web` | `McpWebMvcAutoConfiguration` |
| Streamable HTTP (SSE) - Reactive | 额外引入 `spring-boot-starter-webflux` | `McpWebFluxAutoConfiguration` |
| WebSocket | Starter 默认传递 `spring-boot-starter-websocket` | `McpWebSocketAutoConfiguration` |

SSE 和 WebSocket 传输独立开关，可以同时启用。

## 扩展 MCP 能力

支持两种扩展方式。

### SPI 扩展

实现对应 Provider 接口并在 Spring 容器中声明为 Bean：

- `McpToolProvider`
- `McpResourceProvider`
- `McpPromptProvider`
- `McpCompletionProvider`

```java
@Component
public class EchoToolProvider implements McpToolProvider {
    @Override
    public List<McpTool> listTools(McpProviderContext context, McpPageRequest pageRequest) {
        return Collections.singletonList(
                McpTool.builder()
                        .name("sample-echo")
                        .description("回传客户端输入的文本")
                        .build());
    }

    @Override
    public McpToolResult callTool(McpProviderContext context, McpToolCallRequest request) {
        return McpToolResult.success("sample-echo: " + request.getArguments().get("message"));
    }
}
```

### 注解扩展

在任意 Spring Bean 的 public 方法上使用 `@McpTool` 暴露工具：

```java
@Component
public class SampleToolBean {

    @McpTool(name = "sample-bean-echo", description = "样例注解工具")
    public String beanEcho(@McpToolParam(name = "message", required = true) String message) {
        return "sample-bean-echo: " + message;
    }
}
```

`@McpTool` 仅用于 Tool；Resource 和 Prompt 仍通过 SPI 提供。

## 客户端使用配置说明

客户端连接时需要携带 `Content-Type: application/json`，Streamable HTTP 场景下通过 `Mcp-Session-Id` 维持会话；WebSocket 场景下每个连接对应一个会话。

### 会话流程

1. 客户端向实例路径发送 `initialize` 请求，服务端返回 `protocolVersion`、`serverInfo`、`capabilities`，并通过响应头 `Mcp-Session-Id` 下发会话 ID。
2. 客户端发送 `notifications/initialized` 通知，完成初始化。
3. 后续请求在请求头中携带 `Mcp-Session-Id`；`DELETE` 请求可主动结束会话。
4. SSE 连接断开后，客户端可通过 `Last-Event-ID` 恢复事件流。

### Streamable HTTP (SSE) 客户端

```bash
# 1. 初始化并获取会话 ID
SESSION_ID=$(curl -i -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-25","clientInfo":{"name":"demo-client","version":"1.0.0"}}}' \
  | grep -i 'mcp-session-id' | awk '{print $2}' | tr -d '\r')

# 2. 发送 initialized 通知
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "Mcp-Session-Id: $SESSION_ID" \
  -d '{"jsonrpc":"2.0","id":null,"method":"notifications/initialized"}'

# 3. 调用工具
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "Mcp-Session-Id: $SESSION_ID" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"sample-echo","arguments":{"message":"hello"}}}'

# 4. 建立 SSE 事件流接收通知
curl -N -H "Accept: text/event-stream" -H "Mcp-Session-Id: $SESSION_ID" http://localhost:8080/mcp/sse
```

### WebSocket 客户端

```bash
# 使用 wscat 连接
wscat -c ws://localhost:8080/mcp/ws

# 连接成功后发送 initialize
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-25","clientInfo":{"name":"demo-client","version":"1.0.0"}}}

# 发送 initialized
{"jsonrpc":"2.0","id":null,"method":"notifications/initialized"}

# 调用工具
{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"sample-echo","arguments":{"message":"hello"}}}
```

### 接收服务端变更通知

服务端会在以下事件发生时主动推送通知：

- `notifications/tools/list_changed`：工具列表发生变更。
- `notifications/resources/list_changed`：资源列表发生变更。
- `notifications/resources/updated`：指定资源内容发生变更，参数包含 `uri`。
- `notifications/prompts/list_changed`：提示词列表发生变更。
- `notifications/message`：日志消息通知，参数包含 `level`、`logger`、`data`。

客户端处理建议：

- Streamable HTTP：保持 SSE 长连接，持续读取 `data` 字段中的 JSON-RPC 通知。
- WebSocket：保持连接打开，持续读取服务端推送的 JSON 文本。

### 跨域与安全

- `fw-plus.ai.mcp.allowed-origins` 用于控制 SSE/Streamable HTTP 的跨域访问；配置为 `*` 时不限制来源。
- WebSocket 路径由 `fw-plus.ai.mcp.websocket-path` 控制，建议在生产环境结合网关或防火墙限制访问来源。

## 参考样例

可参考 `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-mcp-server-sample`，其展示了握手、`tools/list`、`tools/call`、`resources/list`、`prompts/list` 的最小调用。
