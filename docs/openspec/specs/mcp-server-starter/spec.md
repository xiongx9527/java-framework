# mcp-server-starter Specification

## Purpose
为 Spring Boot 应用提供轻量 MCP Server Starter 依赖包，用户只需添加 starter 依赖并配置属性，即可通过 `fw-plus-ai-boot-autoconfigure` 将 Java 业务能力通过 MCP 协议暴露给 AI 客户端，支持 HTTP/SSE 和 Streamable HTTP 传输，并可根据环境动态启用集群 Redis 广播。

## Requirements

### Requirement: 依赖型 Starter

`fw-plus-ai-mcp-spring-boot-starter`  SHALL 为纯依赖 POM，不包含任何 Java 代码，仅聚合 `fw-plus-ai-boot-autoconfigure` 及其传递依赖。

#### Scenario: 依赖聚合
- **WHEN** 用户添加 `fw-plus-ai-mcp-spring-boot-starter` 依赖到 Spring Boot 应用
- **THEN** 传递依赖自动引入 `fw-plus-ai-boot-autoconfigure`、`fw-plus-ai-mcp`、`aj-mcp-server` 等，无需额外配置

#### Scenario: Starter 无代码
- **WHEN** 用户查看 starter 模块内容
- **THEN** 仅存在 `pom.xml`，无 `src/main/java` 目录

### Requirement: 自动配置 MCP Server

Spring Boot 应用添加 starter 依赖后，SHALL 由 `fw-plus-ai-boot-autoconfigure` 依据 `fw-plus.ai.mcp.servers` 自动配置一个或多个 MCP runtime；未配置 `servers` 时按顶层 `fw-plus.ai.mcp.*` 配置回落为单个 runtime，兼容既有单实例行为，用户无需手动编写配置类。

#### Scenario: 自动配置生效
- **WHEN** 用户添加 `fw-plus-ai-mcp-spring-boot-starter` 依赖到 Spring Boot 应用
- **THEN** 应用启动时按 `servers` 配置自动创建对应数量的 MCP runtime 并注册 `@McpService` 注解的类

#### Scenario: 无配置也能运行
- **WHEN** 用户未配置任何 MCP 相关属性
- **THEN** MCP Server 使用默认配置启动单个 runtime，默认使用 HTTP/SSE 传输

#### Scenario: 多 runtime 由 servers 驱动
- **WHEN** `fw-plus.ai.mcp.servers` 配置了多个条目
- **THEN** 每个条目生成一个独立 runtime，各持有独立注册表与会话表

### Requirement: 多传输方式支持

每个 MCP runtime SHALL 支持 HTTP/SSE、Streamable HTTP 两类传输方式，用户可通过每个 runtime 的配置属性独立选择传输；不同 runtime 可分别使用不同传输，传输取值白名单仅含 `http-sse`、`streamable-http`（大小写不敏感）。

#### Scenario: HTTP/SSE 传输
- **WHEN** 某 runtime 配置 `transport=http-sse` 并设置 `server.port`
- **THEN** 该 runtime 通过 HTTP 端点提供 SSE 传输（`GET {endpoint}/sse` 与 `POST {endpoint}/messages/{uuid}`），支持事件推送

#### Scenario: Streamable HTTP 传输
- **WHEN** 某 runtime 配置 `transport=streamable-http`
- **THEN** 该 runtime 通过单个 HTTP 端点（`POST/GET/DELETE {endpoint}`）处理所有 JSON-RPC 消息

#### Scenario: 混合传输并存
- **WHEN** 一个 runtime 配置 `http-sse`、另一个配置 `streamable-http`
- **THEN** 各自端点同时生效，不同 runtime 会话互不串流

### Requirement: 注解扫描与注册

MCP Server SHALL 自动扫描指定包路径下标记了 `@McpService` 的类，并将其中的工具、资源、提示词方法注册到 MCP Server。

#### Scenario: 包扫描注册
- **WHEN** 用户配置 `fw-plus.ai.mcp.base-package=com.example.mcp`
- **THEN** 系统扫描该包下所有 `@McpService` 类并注册其公开方法

#### Scenario: 默认包扫描
- **WHEN** 用户未配置 `base-package`
- **THEN** 系统扫描启动类所在包及其子包

### Requirement: 配置属性

MCP Server SHALL 提供 `fw-plus.ai.mcp.*` 前缀的配置属性，支持配置服务器名称、版本、默认传输方式、默认基础包路径，以及 `servers` 列表（每项含 `key`、`endpoint`、`transport`、`base-packages`）。顶层属性作为单实例回落默认值；`servers` 条目中未显式配置的 `transport`、`endpoint`、`server.name`、`server.version`、`base-packages` 继承顶层取值；顶层也未配置时才使用 runtime 内置默认值（`http-sse` / `/mcp`）。

#### Scenario: 自定义服务器信息
- **WHEN** 用户配置 `fw-plus.ai.mcp.server.name=my-server` 和 `fw-plus.ai.mcp.server.version=1.0.0`
- **THEN** MCP Server 在初始化时向客户端报告自定义的名称和版本

#### Scenario: servers 列表配置传输
- **WHEN** 用户为某 runtime 配置传输类型不在白名单内（`http-sse` / `streamable-http` 之外的取值）
- **THEN** 应用启动失败并给出明确的传输类型不受支持提示（校验 fail-fast）

### Requirement: 集群 Redis 广播

MCP Server SHALL 支持可选的集群 Redis 广播能力。当环境中存在 `StringRedisTemplate` 时自动启用 Redis 广播，否则仅使用本地广播。广播 payload SHALL 携带 `routeKey`，消费端按目标 runtime 定位本机对应注册表投递。

#### Scenario: Redis 广播启用
- **WHEN** 应用包含 `spring-boot-starter-data-redis` 依赖并配置了 Redis 连接
- **THEN** MCP Server 通过 Redis 向集群中其他节点的客户端广播通知

#### Scenario: 多实例下广播不交叉
- **WHEN** 集群中某节点发布广播
- **THEN** 各节点按 routeKey 将消息仅投递给目标 runtime 的会话，不向其它 runtime 重复下发

#### Scenario: 无 Redis 降级
- **WHEN** 应用未引入 Redis 依赖
- **THEN** MCP Server 仅使用本地广播，不影响正常运行

### Requirement: 与 Magic-API 集成

MCP Server SHALL 支持将 Magic-API 的接口、函数、任务自动注册为 MCP 工具，使 Magic-API 脚本能力可通过 MCP 协议访问。

#### Scenario: Magic-API 工具自动注册
- **WHEN** 用户启用 Magic-API MCP 集成
- **THEN** Magic-API 中的 API 接口自动注册为 MCP 工具，AI 客户端可通过 MCP 调用

### Requirement: 多实例 runtime

`fw-plus-ai-mcp-spring-boot-starter` SHALL 支持在同一 Spring Boot 应用中暴露多个逻辑 MCP Server，每个 runtime 独立持有 `McpServer`、特性管理器、传输注册表与认证 SPI（默认放行占位）。

#### Scenario: servers 列表配置
- **WHEN** 用户配置 `fw-plus.ai.mcp.servers` 列表（每项含 `key`、`endpoint`、`transport`、`base-packages`）
- **THEN** 每个配置项启动一个独立 MCP runtime，各持有独立注册表与会话表

#### Scenario: 顶层配置兼容单实例
- **WHEN** 用户未配置 `servers`，仅配置顶层 `fw-plus.ai.mcp.transport/endpoint/base-packages`
- **THEN** 按旧行为启动单个 MCP Server，路径与既有版本一致

#### Scenario: 同传输多实例共存
- **WHEN** 两个 runtime 同为 `http-sse` 但 endpoint 不同（如 `/weather-mcp` 与 `/order-mcp`）
- **THEN** 各自注册独立 `/sse` 与 `/messages/{uuid}` 路径，会话互不串流

#### Scenario: 不同传输并存
- **WHEN** 一个 runtime 配置 `http-sse`、另一个配置 `streamable-http` 且 endpoint 相同（均 `/mcp`）
- **THEN** SSE 路径（`/mcp/sse`、`/mcp/messages/{uuid}`）与 Streamable 单端点（`/mcp` 的 POST/GET/DELETE）同时生效

#### Scenario: 同传输 endpoint 冲突
- **WHEN** 两个 `servers` 配置为相同 transport 且相同 endpoint
- **THEN** 启动失败并给出明确的端点到实例冲突提示（不允许覆盖注册）

### Requirement: 编程式路由注册

MCP 接口路径 SHALL 通过 `RequestMappingHandlerMapping.registerMapping` 编程式注册，替代注解占位符路径。

#### Scenario: 动态路径注册
- **WHEN** 应用启动且存在多个 runtime
- **THEN** 每个 runtime 的 endpoint 方法注册为对应 HandlerMethod，MVC 参数解析与异常处理均生效

#### Scenario: SSE 与 Streamable 路径细节
- **WHEN** runtime 传输为 `http-sse`
- **THEN** 注册 `GET {endpoint}/sse`（produces `text/event-stream`）与 `POST {endpoint}/messages/{uuid}`（动态消息路径，uuid 为路径段而非 query 参数）
- **WHEN** runtime 传输为 `streamable-http`
- **THEN** 注册 `POST/GET/DELETE {endpoint}` 单个端点

### Requirement: 双栈支持

MCP Server SHALL 同时支持 Spring WebMVC 与 Spring WebFlux 两种 Web 栈，共享同一套 runtime 与会话归属校验逻辑，仅 HTTP 出入口随栈适配。

#### Scenario: WebMVC 运行
- **WHEN** 应用使用 `spring-boot-starter-web`（MVC）并配置 MCP runtime
- **THEN** 通过 `RequestMappingHandlerMapping.registerMapping` 注册各 runtime 路径，MVC 参数解析与异常处理生效

#### Scenario: WebFlux 运行
- **WHEN** 应用使用 `spring-boot-starter-webflux` 并无 MVC 依赖
- **THEN** 通过 `RouterFunctions.route()` 注册相同路径布局；同步内核调用包 `Mono.fromCallable().subscribeOn(mcpThreadPool)` 在专用线程池执行，不在 Netty event loop 阻塞

#### Scenario: 双栈会话归属校验一致性
- **WHEN** 同一 runtime 配置运行于 MVC 与 Flux 应用
- **THEN** uuid / `Mcp-Session-Id` 归属校验与失效响应行为对客户端完全一致（校验逻辑收敛于栈无关抽象，两栈均无 HTTP Filter 层）

#### Scenario: WebFlux SSE 出口
- **WHEN** WebFlux 应用使用 http-sse 传输
- **THEN** SSE 写帧经 Writer 桥接入 `Flux<ServerSentEvent<String>>` 推给客户端，多线程写同一会话帧不撕裂

### Requirement: 会话归属校验

MCP 会话（SSE 的 `uuid` 与 Streamable 的 `Mcp-Session-Id`）SHALL 校验其属于接收请求的 runtime，防止跨 runtime 串台。此校验是**多实例正确性**约束，不依赖认证信息（认证为后续 change）。

#### Scenario: 跨 runtime 串台拒绝
- **WHEN** 客户端使用 A runtime 的会话 uuid 调用 B runtime 的 `POST {endpoint}/messages/{uuid}`，或携带不属于本 runtime 的 `Mcp-Session-Id`
- **THEN** 请求被拒绝并返回失效响应（会话不存在/不属于本 runtime）；该模型依赖"动态消息路径 uuid 即会话密钥，不可枚举/不可伪造"

#### Scenario: 合法会话放行
- **WHEN** 客户端携带的 uuid / `Mcp-Session-Id` 存在于本 runtime 会话表
- **THEN** 请求进入对内核的处理流程，会话正常收发
