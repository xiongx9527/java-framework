# mcp-server-starter Specification (delta)

## Purpose
在 `fw-plus-ai-mcp-spring-boot-starter` 既有单实例 MCP Server 能力之上，扩展为**多 runtime 共存 + `@McpServer` 注解归属 + 会话归属校验 + WebMVC/WebFlux 双栈**。认证授权与按客户端 scope 裁剪能力面列为后续独立 change，本次不承载。

## Requirements

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

Redis 广播 payload SHALL 携带 `routeKey`，消费端按目标 runtime 定位本机对应注册表投递。

#### Scenario: 多实例下广播不交叉
- **WHEN** 集群中某节点发布广播
- **THEN** 各节点按 routeKey 将消息仅投递给目标 runtime 的会话，不向其它 runtime 重复下发