**fw-plus-ai-mcp-spring-boot-starter**

本模块为 `fw-plus-ai` 提供 MCP Server 的 Spring Boot Starter。纯依赖型 POM，不包含 Java 代码；所有自动配置和 Bean 注入由 `fw-plus-ai-boot-autoconfigure` 提供。

基于 `aj-mcp` 实现，支持 HTTP/SSE 和 Streamable HTTP 两类传输方式，通过 `fw-plus.ai.mcp.transport` 属性切换。

## 依赖

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-mcp-spring-boot-starter</artifactId>
</dependency>
```

## 快速开始

### 1. 添加依赖

将上述依赖添加到 Spring Boot 应用的 `pom.xml` 中。

### 2. 编写 MCP 服务

使用 `@McpService` 注解标记服务类，使用 `@Tool`、`@Resource`、`@Prompt` 注解声明能力：

```java
@McpService
public class MyMcpServices {

    @Tool(description = "两个整数相加")
    public String add(@ToolArg("a") Integer a, @ToolArg("b") Integer b) {
        return String.valueOf(a + b);
    }

    @Resource(uri = "config:///application", description = "应用配置")
    public String configuration() {
        return "mode=production";
    }

    @Prompt(description = "代码审查提示词")
    public String codeReviewPrompt(@ToolArg("code") String code) {
        return "请审查以下代码：\n" + code;
    }
}
```

### 3. 配置 application.yaml

```yaml
fw-plus:
  ai:
    mcp:
      server:
        name: my-mcp-server
        version: 1.0.0
      transport: http-sse  # http-sse 或 streamable-http
      endpoint: /mcp  # MCP 端点路径，两类传输共用
      base-packages: com.example.mcp,com.example.other  # 逗号分隔，与启动类所在包取并集
```

### 4. 启动应用

应用启动后，MCP Server 将自动启用并监听 HTTP 端点。

## 配置属性

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `fw-plus.ai.mcp.enabled` | MCP 自动配置总开关，设为 `false` 时关闭全部 MCP Bean | `true` |
| `fw-plus.ai.mcp.server.name` | MCP Server 名称，仅用于协议握手 | `fw-plus-mcp-server` |
| `fw-plus.ai.mcp.server.version` | MCP Server 版本 | `1.0.0` |
| `fw-plus.ai.mcp.transport` | 传输方式：`http-sse` 或 `streamable-http` | `http-sse` |
| `fw-plus.ai.mcp.endpoint` | MCP 端点路径。HTTP/SSE 为 `{endpoint}/sse` 与 `{endpoint}/message`，Streamable HTTP 为 `{endpoint}` | `/mcp` |
| `fw-plus.ai.mcp.base-packages` | 扫描 `@McpService` 的包路径，逗号分隔可配多个；与 `@SpringBootConfiguration` 启动类所在包取并集 | 启动类所在包 |

## 传输方式

### HTTP/SSE（默认）

客户端通过 POST 请求发送 JSON-RPC 消息，通过 GET 请求建立 SSE 长连接接收服务器通知。

### Streamable HTTP

客户端通过单个 HTTP 端点发送所有 JSON-RPC 消息，通过响应头 `Mcp-Session-Id` 维护会话。

## 集群 Redis 广播

引入 `spring-boot-starter-data-redis` 并配置 Redis 连接后，MCP Server 自动切换为集群注册表（`RedisServerSseRegistry` / `RedisServerStreamableHttpRegistry`），通过固定频道 `fw-plus:mcp:broadcast` 在节点间转发消息。

每个节点启动时随机生成 `serverId`，随消息一起发布。消息语义分两类：

- 定向消息（`sessionId` 非空）：本机持有该会话则直接投递，否则经 Redis 转交，由持有该会话的节点投递；其它节点收到后仅按本机会话持有情况决定是否投递，不再转回频道。
- 广播消息（`sessionId` 为空）：先下发本机会话，再发布到频道；其它节点收到后只做本地分发，不回灌频道。

发布端与消费端的 `serverId` 守卫共同保证同一份消息不在节点间循环。spring-data-redis 缺失时（如纯单机部署）自动装配整体跳过集群 Bean，仅使用本地注册表，无需额外配置。

## 参考样例

可参考 `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-mcp-server-sample`，其展示了 `@McpService`、`@Tool`、`@Resource`、`@Prompt` 的最小示例。
