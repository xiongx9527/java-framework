# Comet Design Handoff

- Change: mcp-server-multi-instance
- Phase: design
- Mode: compact
- Context hash: 84dcede56ea0b3dc1e98ae644a2065cc9d5c6ec6a805b816fa0413ed09fda466

Generated-by: comet-handoff.sh
Task hash policy: task-content-v1. Read tasks.md for live completion; excerpts are design-time context.

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## docs/openspec/changes/mcp-server-multi-instance/proposal.md

- Source: docs/openspec/changes/mcp-server-multi-instance/proposal.md
- Lines: 1-29
- SHA256: d79ac211add50379454ff961bbcbb6c1ed22cf22af4696b816416b989bf7a6bc

```md
# Proposal

## Why

当前 `fw-plus-ai-mcp-spring-boot-starter`（聚合 `fw-plus-ai-boot-autoconfigure`）只支持**单一 MCP Server**：一个 `McpServer` 实例、一个 endpoint、一台份认证配置。随着业务能力膨胀，一个进程内往往需要暴露多个逻辑 MCP 服务（如天气工具与订单工具），且不同客户端应只看到/使用授权内的工具、资源、提示词子集。现状无法表达这种"一台进程、多套能力、按客户端裁剪"的诉求。

## What Changes

- **多实例 runtime 化（Quarkus 对齐）**：新增 `McpServerRuntime` 抽象，每个 runtime 独立持有 `McpServer`、`FeatureMgr`、传输注册表、认证 SPI（默认放行占位）；`McpAutoConfiguration` 下沉为工厂，按 `default server + servers[]` 循环构建。顶层配置即 default server（旧单实例路径零改动）。
- **注解归属绑定**：新增 `@McpServer("key")` 注解（类/方法级），把 `@McpService`/`@Tool`/`@Resource`/`@Prompt` 绑定到指定 runtime；未标注的能力归 default server，同一类方法可分属不同 server。
- **编程式路由注册 + 双栈**：`McpWebBinding` 栈无关抽象；WebMVC 用 `registerMapping` 动态注册，WebFlux 用 `RouterFunctions.route()` + 「同步入 Mono + 专用线程池」适配同步内核（SSE 经 Writer 桥到 `Flux<ServerSentEvent>`）。
- **会话归属校验**：SSE 消息入口与 Streamable 会话入口校验 uuid / `Mcp-Session-Id` 属于本 runtime 会话表，防跨 runtime 串台。
- **认证 SPI 占位**：`McpAuthService` 仅保留接口面，默认 `PermitAllAuthService` 恒放行；完整认证（Sa-Token 应用内会话 / OAuth2+JWKS 外部 AS）、scope 裁剪、401/403 语义留作后续独立 change。
- **集群广播定位**：Redis 广播 payload 增加 `routeKey`，消费端按路由定位本机对应 runtime 的注册表投递，避免多实例交叉下发。
- **向后兼容**：`servers` 为空时沿用现有顶层配置走单实例旧路径，老配置零改动。

## Capabilities

### New Capabilities
- `mcp-runtime-multi-instance`: 同一 Spring Boot 应用暴露多个逻辑 MCP Server（不同/相同 endpoint、不同传输），能力经 `@McpServer("key")` 注解归属到指定 runtime；认证授权与按客户端 scope 裁剪列为后续独立 change

### Modified Capabilities
- `mcp-server-starter`: 由"单实例条件 Bean"演进为"runtime 工厂 + 编程式路由"，传输/registry 由全局单例变为 per-runtime 实例

## Impact

- 新增代码：`fw-plus-ai-boot-autoconfigure` 下 `mcp/runtime/*`（`McpServerRuntime`、`McpRuntimeFactory`、`McpRoutingRegistrar`）、`mcp/web/*`（`McpWebBinding` 双栈实现）、`mcp/auth/*`（`McpAuthService` 占位 SPI + `McpPrincipal` + `PermitAllAuthService`）
- 新增核心注解：`fw-plus-ai-mcp` 下 `@McpServer("key")`
- 依赖：本次**无新增外部依赖**（认证所需 jjwt/Sa-Token 留到后续认证 change 引入，均 optional）
- 影响范围：fw-plus-ai 层 `fw-plus-ai-boot-autoconfigure` 与 starter 文档
```

## docs/openspec/changes/mcp-server-multi-instance/design.md

- Source: docs/openspec/changes/mcp-server-multi-instance/design.md
- Lines: 1-170
- SHA256: 4a8c295883e063b28bd8ea5dc3e50057b8372a0b289f5a729d0be97443761cb0

[TRUNCATED]

```md
---
comet_change: mcp-server-multi-instance
change_ref: docs/openspec/changes/mcp-server-multi-instance
role: technical-design
canonical_spec: openspec
---

# Design

## Context

`fw-plus-ai-mcp-spring-boot-starter` 聚合 `fw-plus-ai-boot-autoconfigure`，基于 aj-mcp 内核提供 MCP Server 能力。现状的是**单实例**设计：

- `McpAutoConfiguration` 注册全局单例 `McpServer`/registry/controller Bean，传输靠 `@ConditionalOnProperty(transport=...)` **二选一**；
- `McpProperties` 是单一配置对象，`endpoint` 单值、无认证配置；
- Controller 用注解占位符路径 `@GetMapping("${fw-plus.ai.mcp.endpoint:/mcp}/sse")`，无多端点能力；
- 五个 HTTP 入口（SSE: GET `/sse`、POST `/message`；Streamable: POST/GET/DELETE `/mcp`）各自持有全局单例 registry。

需求：同一进程暴露多个逻辑 MCP 服务（不同/相同 endpoint、不同传输）。该模型对齐 **Quarkus MCP Server 扩展** 的多实例设计（default server + 命名 servers 追加 + `@McpServer("key")` 注解绑定工具归属），仅借鉴其模式、不引入其实现（Quarkus 3.x 需 Java 17，本仓库约束 Java 1.8）。认证授权与按客户端 scope 裁剪列为后续独立演进，本次不落地。

## Goals / Non-Goals

**Goals：**
- 多 runtime 共存：每 runtime 独立 `McpServer` + `FeatureMgr` + 传输注册表
- 传输正交于实例：同一传输可多实例、不同传输可并存（registry 实例级隔离）
- 编程式路由注册，支持不同 endpoint 或相同 endpoint 的多实例
- **WebMVC 与 WebFlux 双栈支持**：同一套 runtime/会话归属校验逻辑在两个 Web 栈下均可运行
- 会话归属校验：uuid / `Mcp-Session-Id` 必须属于本 runtime 的会话表，防跨 runtime 串台
- 集群 Redis 广播按 `routeKey` 定位目标 runtime
- 认证 SPI 占位：仅留默认放行的 `McpAuthService` 接口面，认证/scope 裁剪为后续 change
- 向后兼容：顶层配置即 default server，未配置 `servers` 时行为与旧单实例一致

**Non-Goals：**
- 不复制 aj-mcp 内核；不改动其 package-private 方法签名
- 不在 WebFlux 上改造内核为异步非阻塞——内核为同步模型，WebFlux 下用「同步入 Mono + 专用线程池」适配（阻塞移出 Netty event loop）
- 不做故障/资源隔离（单实例共享 JVM；多 runtime 是逻辑隔离，非物理隔离）
- 不支持 JSON-RPC batch（内核天然不支持）
- 不做 MCP Client
- 不做认证授权与 scope 裁剪：`McpAuthService` 仅保留默认放行占位（返回 `anonymous`，恒放行），认证（Sa-Token 应用内会话 / OAuth2+JWKS 外部 AS）与按客户端裁剪能力面为后续独立 change

## Decisions

### D1: registry 与传输的实例粒度 = runtime 粒度

每个 `McpServerRuntime` 持有自己的 `ServerSseRegistry` **或** `ServerStreamableHttpRegistry`（按 transport 二选一注入 runtime 字段），不再作为全局单例 Bean。通过字节码确认：`ServerSse.connections`、`ServerStreamableHttp.streams/sessions`、`McpServer.sessionStates` 均为**实例字段**，内核无静态可变状态，N 个实例天然隔离，两种 registry 可同时存在。

**Rationale**：传输互斥是"自动配置单例 Bean"的产物，不是内核限制；runtime 化后 registry 归实例所有，不同实例可任意选协议。

**Alternatives considered**：
- 多 runtime 共享一个 registry：会话表无法区分归属，排除。

### D2: 配置模型：default server + 命名 servers 追加（Quarkus 对齐）

对齐 Quarkus 的 `default server + 命名 server` 模型：

- **default server**：顶层 `fw-plus.ai.mcp.*` 配置即 default server（`key=default`，endpoint 默认 `/mcp`），旧单实例路径零改动即为其一个特例；
- **命名 servers**：`fw-plus.ai.mcp.servers[].key` 声明追加实例，每项可配 `key/server/transport/endpoint/basePackages`；
- 二者的 endpoint 可相同（转传输不同时路径天然不重叠）或不同（同传输时须唯一）。

`McpProperties` 增 `List<McpProperties> servers`（复用自身类型递归嵌套）。`auth` 配置预留到后续认证 change，不在此处声明。

**Rationale**：与 Quarkus 完全一致的能力模型（每 server 独立 root path、独立安全策略、聚合多个逻辑服务），向后兼容由"顶层即 default"天然满足。

**约束**：同一 transport 下 endpoint 必须唯一（否则 `registerMapping` 冲突）；不同 transport 间 endpoint 可相同（路径组合天然不重叠）。

**Alternatives considered**：
- 顶层 + `servers` 互斥报错：与 Quarkus default server 语义不符且不友好，排除。
- `servers` 非空时丢弃顶层：丧失 default server 概念，排除。

### D3: 编程式路由注册（方案 A）+ 动态消息路径（Quarkus 对齐）+ 双栈抽象

**共享的 runtime/会话归属校验核心与 Web 栈无关**，路由注册与 HTTP 出入参随栈变化。引入 `McpWebBinding` 抽象（每栈一个实现），统一暴露「按 runtime 注册一套路径」「收到请求 → 定位 runtime → 会话归属校验 → 分发」流程。

**WebMVC 实现**：Controller 去 `@RestController` 注解路径，改为实例化对象持有 runtime；`McpRoutingRegistrar` 用 `RequestMappingHandlerMapping.registerMapping(RequestMappingInfo, handler, Method)` 注册。路径：

- http-sse：`{endpoint}/sse`（GET，produces `text/event-stream`）+ `{endpoint}/messages/{uuid}`（POST，`@PathVariable` 动态段）
- streamable：`{endpoint}`（POST / GET / DELETE）

对齐 Quarkus 的关键点：**SSE 消息路径是动态段 `{endpoint}/messages/{uuid}`，而非固定 `/message?uuid=`**。内核 `ServerSse.openSession(sessionId, writer, endpointPath)` 的第三个参数即要下发的端点路径（`event: endpoint\ndata: {endpoint}/messages/{uuid}`），**无需改内核**，只需 Controller 把 uuid 从 query 参数改为路径段。路径即会话密钥，防劫持能力与 Quarkus 一致。`registerMapping` 的 `patterns` 支持 `{uuid}` 模板变量，HandlerMethod 用 `@PathVariable("uuid")` 解析，MVC 参数解析与 `@ControllerAdvice` 异常处理继续生效。


```

Full source: docs/openspec/changes/mcp-server-multi-instance/design.md

## docs/openspec/changes/mcp-server-multi-instance/tasks.md

- Source: docs/openspec/changes/mcp-server-multi-instance/tasks.md
- Lines: 1-39
- SHA256: 43a18c7185cb24dcd0320f682ff81211f56d7367f45461516ed3474377e2bd6e

```md
# Tasks

## 1. 配置模型扩展

- [ ] 1.1 `McpProperties` 顶层配置即 default server（key=default），新增 `List<McpProperties> servers` 命名列表（每项 `key/server/transport/endpoint/basePackages`）；验证 default+命名servers 绑定单测通过
- [ ] 1.2 定义 endpoint 校验规则：同一 transport 下 default+命名 servers 的 endpoint 唯一，不同 transport 允许重复；冲突时启动抛明确异常；`@McpServer` 绑定的 key 未在 servers 声明时报错；验证无效配置场景单测

## 1.5 注解归属绑定

- [ ] 1.5.1 新增 `@McpServer("key")` 注解（Java 1.8，`fw-plus-ai-mcp` 核心），作用于类/方法级，声明能力归属的 runtime；`McpServerInitializer` 扫描时按注解路由工具/资源/提示词到对应 runtime，未标注进 default server；同一类方法可分属不同 server；验证注解路由单测

## 2. runtime 化与实例隔离

- [ ] 2.1 新增 `McpServerRuntime` 抽象，字段含 `key/endpoint/server/featureMgr/registry/authService(默认 PermitAll 放行)`；registry 与 server 实例归属 runtime，不再全局单例；验证字节码/运行时实例隔离单测（两个 runtime 会话表互不可见）
- [ ] 2.2 新增 `McpRuntimeFactory`：先按顶层配置生成 default runtime，再按 `servers[]` 逐项生成命名 runtime；`McpAutoConfiguration` 下沉为调用工厂而非直接装配单例 Bean
- [ ] 2.3 `McpServerInitializer` 改为按 runtime 循环 `featureMgr.init(basePackage)` + `server.start()`，并拉取各 runtime 自己的 `McpTransportSync`；验证多 runtime 启停与单实例等价路径

## 3. 编程式路由注册（双栈）

- [ ] 3.1 新增 `McpWebBinding` 栈无关抽象 + `McpRoutingRegistrar`；`McpTransportController`/`McpSseController`/`McpStreamableHttpController` 去掉注解占位符路径，改为实例化对象持有 runtime
- [ ] 3.2 WebMVC：用 `RequestMappingHandlerMapping.registerMapping` 为每个 runtime 注册 SSE（`GET {ep}/sse` + `POST {ep}/messages/{uuid}`，`@PathVariable` 动态段）与 Streamable（`POST/GET/DELETE {ep}`）路径；验证 MVC 参数解析与异常处理仍生效
- [ ] 3.3 WebFlux：`RouterFunctions.route()` + HandlerFunction 组装两套路径布局；同步内核调用包 `Mono.fromCallable().subscribeOn(mcpThreadPool)`，专用线程池可配置；SSE 出口自实现 Writer 桥 → `Flux<ServerSentEvent<String>>`；握手即返回 Flux、连接断开经 `doFinally` 清理会话（不占池线程）；验证响应式调用链
- [ ] 3.4 会话归属校验入双栈通用路径：`POST {ep}/messages/{uuid}` 与 Streamable 会话入口校验 uuid / `Mcp-Session-Id` ∈ 本 runtime 会话表，不存在则返回失效响应；MVC 与 Flux 行为一致（防串台契约测试）
- [ ] 3.5 验证同传输多 endpoint、异传输同 endpoint、同传输同 endpoint（报冲突）三种路由场景，MVC 与 Flux 各一套集成测试

## 4. 认证 SPI 占位（本次仅接口面，默认放行）

- [ ] 4.1 新增 `McpPrincipal(subject, scopes)` 与 `McpAuthService.authenticate(McpRequest) -> McpPrincipal` SPI；默认 `PermitAllAuthService` 返回 `anonymous` 恒放行（旧行为）；`@ConditionalOnMissingBean` 允许用户覆盖；单测验证默认行为与旧单实例一致
- [ ] 4.2 （后续 change，不实现）Sa-Token 应用内会话 / OAuth2+JWKS 外部 AS 认证、scope 裁剪、401/403 语义——本 change 仅保留 SPI 挂载点，不落地实现

## 5. 集群广播 routeKey

- [ ] 5.1 `McpRedisBroadcaster` payload 增加 `routeKey`；`McpRedisMessageConsumer.deliver` 按 routeKey 定位本机对应 runtime 的 registry 再投递
- [ ] 5.2 验证多 runtime 下广播只投递到目标 runtime 的会话，不交叉重复下发

## 6. 文档与样例

- [ ] 6.1 README 补充 `servers` 多实例配置、`@McpServer` 归属注解、会话归属校验（uuid/`Mcp-Session-Id`）失效语义、WebMVC/WebFlux 双栈适配说明，并标注认证授权为后续独立 change
- [ ] 6.2 样例增加两个不同 endpoint 的 runtime 演示（如 weather 与 order），并分别以 MVC 与 Flux 容器启动验证，验证样例可启动且各自只暴露 `@McpServer` 绑定的能力面
- [ ] 6.3 全量单测 + 集成测试通过（MVC 与 Flux 契约测试同一用例集）；`./fw-plus-core/mvnw -f fw-plus-sample/pom.xml validate` 样例入口可用
```

## docs/openspec/changes/mcp-server-multi-instance/.openspec.yaml

- Source: docs/openspec/changes/mcp-server-multi-instance/.openspec.yaml
- Lines: 1-1
- SHA256: b0b17d81d9879c1c530bcf1bd09219678dbbcacdcc6e8dd5b150f70fb59d6d58

```md
schema: spec-driven
created: 2026-09-24
```

## docs/openspec/changes/mcp-server-multi-instance/specs/mcp-server-starter/spec.md

- Source: docs/openspec/changes/mcp-server-multi-instance/specs/mcp-server-starter/spec.md
- Lines: 1-81
- SHA256: 611b9b028945fdc2e5a1968e8118c61ff5134f56fddb5edd9cec6688ac14ba09

[TRUNCATED]

```md
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

```

Full source: docs/openspec/changes/mcp-server-multi-instance/specs/mcp-server-starter/spec.md
