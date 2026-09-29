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

#### 单实例（key 固定为 `default`）

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

#### 多实例（`servers` 追加命名 runtime）

顶层配置仍作为 `default` runtime（endpoint 默认 `/mcp`），`servers` 列表可追加任意数量的命名 runtime，每个实例拥有独立的 `endpoint` 与能力面：

```yaml
fw-plus:
  ai:
    mcp:
      server:
        name: my-mcp-server
        version: 1.0.0
      transport: streamable-http
      servers:
        - key: weather
          transport: streamable-http
          endpoint: /mcp/weather
        - key: order
          transport: streamable-http
          endpoint: /mcp/order
```

多实例规则：

- 命名项未显式配置的 `transport`、`endpoint`、`server.name`、`server.version`、`base-packages` 均继承顶层取值；顶层也未配置时才取内置默认值（`http-sse` / `/mcp`）。
- 因此在顶层配好 `transport` 后，`servers[]` 每项通常只需给出 `key` 与自己的 `endpoint`；同一 `transport` 下各实例 `endpoint` 冲突时启动失败，不同 `transport` 的 runtime 可共用同一 `endpoint`。
- 命名 runtime 必须显式给出 `key`（缺失或空白时启动即失败），其余字段均有继承路径，单实例向多实例迁移无需为每个 runtime 重复配置。
- 每个 runtime 独立扫描并通过 `@McpServer("key")` 归属能力，未标注的 `@McpService` 归属 `default`。

### 4. 归属能力到指定 runtime

默认情况下 `@McpService` 归入 `default` runtime；通过类级 `@McpServer("key")` 可将该类全部能力归属到命名 runtime：

```java
@McpService
@McpServer("weather")
public class WeatherToolProvider {
    @Tool(value = "weather-now", description = "查询天气")
    public String now(@ToolArg("city") String city) {
        return "sunny in " + city;
    }
}
```

方法级 `@McpServer("key")` 可单独覆盖类级归属。未匹配到任何 runtime 的 key 会抛出启动异常提示未解析的 server key。

### 5. 启动应用

应用启动后，所有已配置的 MCP Server 将自动注册对应端点：HTTP/SSE 为 `{endpoint}/sse` 与 `{endpoint}/message`，Streamable HTTP 为 `{endpoint}`。多实例场景下各端点独立对外提供服务。

## 配置属性

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `fw-plus.ai.mcp.enabled` | MCP 自动配置总开关，设为 `false` 时关闭全部 MCP Bean | `true` |
| `fw-plus.ai.mcp.server.name` | MCP Server 名称，仅用于协议握手 | `fw-plus-mcp-server` |
| `fw-plus.ai.mcp.server.version` | MCP Server 版本 | `1.0.0` |
| `fw-plus.ai.mcp.transport` | 传输方式：`http-sse` 或 `streamable-http` | `http-sse` |
| `fw-plus.ai.mcp.endpoint` | MCP 端点路径。HTTP/SSE 为 `{endpoint}/sse` 与 `{endpoint}/message`，Streamable HTTP 为 `{endpoint}` | `/mcp` |
| `fw-plus.ai.mcp.base-packages` | 扫描 `@McpService` 的包路径，逗号分隔可配多个；与 `@SpringBootConfiguration` 启动类所在包取并集 | 启动类所在包 |
| `fw-plus.ai.mcp.servers[].key` | 命名 runtime 的唯一标识，同时作为集群 `routeKey` | 无（必填） |
| `fw-plus.ai.mcp.servers[].endpoint` | 命名 runtime 端点路径；同 `transport` 下须唯一 | 继承顶层，顶层未配置时为 `/mcp` |
| `fw-plus.ai.mcp.servers[].transport` | 命名 runtime 传输方式，同顶层取值 | 继承顶层，顶层未配置时为 `http-sse` |
| `fw-plus.ai.mcp.servers[].server.name` | 命名 runtime 的协议名称 | 继承顶层 |
| `fw-plus.ai.mcp.servers[].server.version` | 命名 runtime 的协议版本 | 继承顶层 |
| `fw-plus.ai.mcp.servers[].base-packages` | 命名 runtime 的扫描包路径 | 继承顶层 |
| `fw-plus.ai.mcp.thread-pool-size` | WebFlux 栈同步内核调用专用线程池大小 | `16` |
| `fw-plus.ai.mcp.headers.accept` | 网关改写 header 名时自定义的 Accept 头名；未配置取内核默认 `Accept` | 内核默认 |
| `fw-plus.ai.mcp.headers.protocol-version` | 自定义的协议版本头名；未配置取内核默认 `MCP-Protocol-Version` | 内核默认 |
| `fw-plus.ai.mcp.headers.session-id` | 自定义的会话 id 头名；未配置取内核默认 `Mcp-Session-Id` | 内核默认 |
| `fw-plus.ai.mcp.headers.origin` | 自定义的 Origin 头名；未配置取内核默认 `Origin` | 内核默认 |
| `fw-plus.ai.mcp.scope-check-enabled` | capability scope 裁剪总开关：开启后入口按已认证 token 的 scopes 拒绝未覆盖目标能力（tools/resources/prompts）的请求；关闭时不做裁剪 | `false` |

## 传输方式

### HTTP/SSE（默认）

客户端通过 POST 请求发送 JSON-RPC 消息，通过 GET 请求建立 SSE 长连接接收服务器通知。

### Streamable HTTP

客户端通过单个 HTTP 端点发送所有 JSON-RPC 消息，通过响应头 `Mcp-Session-Id` 维护会话。

## Web MVC 与 WebFlux 双栈适配

框架按应用类型自动选择传输实现，无需改动业务代码：

- **Servlet 应用（`spring-boot-starter-web`）**：由 `McpRoutingRegistrar` 动态注册 MVC 路由。HTTP/SSE 注册 `GET {endpoint}/sse` 与 `POST {endpoint}/message`；Streamable HTTP 注册 `POST {endpoint}`（含 session 初始化的 `GET {endpoint}`）。
- **Reactive 应用（`spring-boot-starter-webflux`）**：由 `McpWebfluxWebBinding` 注册函数式路由，行为与 MVC 一致（含会话校验）。

WebFlux 依赖为 optional，使用 WebFlux 栈时需按引入：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

## 会话归属校验

每个 runtime 的会话独立管理，请求携带的会话凭证只在该 runtime 内有效：

- HTTP/SSE 的 `sessionId`（Server-Sent Events URL 参数）与 Streamable HTTP 的 `Mcp-Session-Id` 请求头均按 runtime 校验归属。
- 使用其他 runtime 的会话凭证访问本端点时直接返回 **404**，不进入能力执行，天然隔离各 runtime 的能力面。

默认使用 `PermitAllAuthService`（不校验身份），会话归属校验始终生效；如需身份校验，可替换 `McpAuthService` 实现。

## 集群 Redis 广播

引入 `spring-boot-starter-data-redis` 并配置 Redis 连接后，MCP Server 自动切换为集群注册表（`RedisServerSseRegistry` / `RedisServerStreamableHttpRegistry`），通过固定频道 `fw-plus:mcp:broadcast` 在节点间转发消息。

每个节点启动时随机生成 `serverId`，随消息一起发布。多实例场景下，每条消息额外携带 `routeKey`（runtime key），节点仅投递本机持有对应 runtime 会话的消息，多 runtime 之间互不串扰。消息语义分两类：

- 定向消息（`sessionId` 非空）：本机持有该会话则直接投递，否则经 Redis 转交，由持有该会话的节点投递；其它节点收到后仅按本机会话持有情况决定是否投递，不再转回频道。
- 广播消息（`sessionId` 为空）：先下发本机会话，再发布到频道；其它节点收到后只做本地分发，不回灌频道。

发布端与消费端的 `serverId` 守卫共同保证同一份消息不在节点间循环。spring-data-redis 缺失时（如纯单机部署）自动装配整体跳过集群 Bean，仅使用本地注册表，无需额外配置。

## 参考样例

可参考 `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-mcp-server-sample`：

- 单实例最小示例：`@McpService`、`@Tool`、`@Resource`、`@Prompt`。
- 多实例示例：`servers` 配置 `weather`（`/mcp/weather`）与 `order`（`/mcp/order`）两个 runtime，通过 `@McpServer("weather")` / `@McpServer("order")` 归属能力，验证各 runtime 能力面隔离与跨 runtime 会话拒绝。
