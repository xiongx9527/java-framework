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
  mcp:
    enabled: true
    allowed-origins: "*"
    instances:
      - name: default
        path: /mcp
        session-timeout-seconds: 1800
        enabled: true
```

- `fw-plus.mcp.enabled`：是否启用 MCP Server 自动配置；默认 `true`。
- `fw-plus.mcp.allowed-origins`：Streamable HTTP 端点允许的来源；默认不过滤。
- `fw-plus.mcp.instances`：多实例配置；未配置时会提供一个默认实例。
- 每个实例支持 `name`、`path`、`session-timeout-seconds`、`enabled`。
- 默认端点路径为 `/mcp`；`initialize` 会返回 `2025-11-25` 版本并生成 `Mcp-Session-Id`。

## 传输栈选择

| 应用类型 | 引入依赖 | 生效配置 |
| --- | --- | --- |
| Spring MVC / Servlet | Starter 默认传递 `spring-boot-starter-web` | `McpWebMvcAutoConfiguration` |
| Spring WebFlux | 额外引入 `spring-boot-starter-webflux` | `McpWebFluxAutoConfiguration` |

两个传输配置互斥：Spring MVC 应用不会装配 WebFlux MCP Bean，反之亦然。

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
