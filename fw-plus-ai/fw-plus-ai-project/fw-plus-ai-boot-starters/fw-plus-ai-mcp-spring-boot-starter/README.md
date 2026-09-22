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

## 参考样例

可参考 `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-mcp-server-sample`，其展示了握手、`tools/list`、`tools/call`、`resources/list`、`prompts/list` 的最小调用。
