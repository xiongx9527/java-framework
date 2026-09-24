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

**WebFlux 实现**：见 D9。

**Alternatives considered**：
- 固定 `/message?uuid=`：现有实现，uuid 在 query string，会话密钥强度弱于路径段，且与 Quarkus 不一致；升级为本方案。
- 统一 `@RequestMapping("/**")` 手写 URI 解析：路由粗、丢精确匹配，排除。
- 注解多路径占位：动态 endpoint 无法在编译期表达，排除。

### D4: runtime 工厂与生命周期

`McpRuntimeFactory` 构建 runtime 集：先按顶层配置生成 **default runtime**（`key=default`），再按 `servers[]` 逐项生成命名 runtime；每 runtime 创建自己的 `McpServer`、`FeatureMgr`、registry、`McpAuthService`（默认为 `PermitAllAuthService` 放行占位，见 D5）；`McpServerInitializer` 改为 per-runtime 循环 `featureMgr.init(basePackage)` + `server.start()`，并各自拉取 `McpTransportSync`。

**Rationale**：responsibility 与现有 `McpAutoConfiguration` 的 Bean 组装保持一致，只是从"构造一套"变为"循环构造多套"，并额外生成 default。

### D5: 认证 SPI 占位（默认放行；认证为后续独立 change）

本次**不落地认证授权**，仅保留最小接口面，避免后续接入认证时重构所有 handler：

`McpAuthService.authenticate(McpRequest) -> McpPrincipal(subject, scopes)`；默认实现 `PermitAllAuthService` 返回 `anonymous`（恒放行，即旧单实例行为）。`McpPrincipal` 在无认证时可为 `null`（匿名），会话归属校验（D6）不依赖它。`@ConditionalOnMissingBean(McpAuthService.class)` 允许用户提前覆盖。

**Rationale**：MCP 规范授权是 **OPTIONAL**（Authorization is OPTIONAL），不实现 `.well-known`/Bearer 校验完全合规。认证（Sa-Token 应用内会话或 OAuth2/JWKS 外部 AS）、scope 裁剪、401/403 语义均移入后续 change；占位接口保证届时只替换 SPI 实现、handler 签名不动。

### D6: 会话归属校验（防跨 runtime 串台；无认证前置）

每 runtime 维护 `sessionId → session` 归属表。SSE 消息入口 `POST {endpoint}/messages/{uuid}` 第一道校验 uuid 是否存在于**本 runtime** 的表中：不存在（伪造/过期/已 remove）直接返回失效响应，不进内核。`McpStreamableHttp` 经 `Mcp-Session-Id` header 同理校验。这是后续接入认证时「principal ↔ 会话绑定」的挂载点。

**Rationale**：这是**多实例正确性**约束而非安全约束——即使不做认证，uuid 归属校验也必须做，否则跨 runtime 的会话可被任意调用（串台）。

### D7: 工具归属 = 注解绑定（scope 裁剪为后续演进）

**归属决定（Quarkus 对齐）**：新增 `@McpServer("key")` 注解（Java 1.8 自实现，`fw-plus-ai-mcp` 核心模块），作用在 `@McpService` 类或 `@Tool`/`@Resource`/`@Prompt` 方法上，把该能力绑定到指定 runtime；**未标注的能力归 default server**（顶层配置）。同一类的方法可分别绑定到不同 server——与 Quarkus 的 `@McpServer` 语义完全一致。

**scope / 按客户端裁剪能力面**：属认证授权范畴，**本次不实现**，留作后续演进锚点——届时以「能力类型级 + 包前缀归组（`@RequireScope` 注解覆盖）」模型接入。`@McpServer("key")` 绑定的 runtime 已天然圈定各 server 的方法面，是将来 scope 过滤的执行范围基础。

**Alternatives considered**：
- 仅靠包前缀归属（`com.demo.weather` → weather）：能覆盖大部分场景，但无法表达"同一类方法分属不同 server"，注解绑定更通用。

### D8: 集群广播按 routeKey 定位

`McpRedisBroadcaster.broadcast(sessionId, message)` 扩展 `routeKey` 字段进 payload；`McpRedisMessageConsumer.deliver` 先按 routeKey 从本机 runtime 表定位对应 registry，再做 sendOther/broadcastOther，防止多实例下广播被每个 SSE instance 各投一遍。

**Rationale**：频道仍一条 `fw-plus:mcp:broadcast`，但消费端能到达正确的 registry 实例。

### D9: WebFlux 适配（同步内核 → 非阻塞栈）

aj-mcp 内核（`ServerSse`/`ServerStreamableHttp`）是同步模型：`openSession(sessionId, PrintWriter, endpointPath)`、`post(...) -> HttpResult`，且 `SseSession.sendFrame(String)` 对 `writer` 字段加锁（字节码已确认，**同步写在线程间安全**）。WebFlux 栈下按「**同步入 Mono + 专用线程池**」适配，不改造内核为异步：

**传输适配层（`McpWebBinding` 双栈实现）**：
- 传入侧：`Mono.fromCallable(() -> { 同步请求处理; return 结果; }).subscribeOn(mcpThreadPool)`，把同步内核调用移出 Netty event loop；
- 传出侧（SSE）：自实现 `java.io.Writer` 实现，把内核 `sendFrame`/`broadcast` 的写事件**桥接到 `Flux<ServerSentEvent<String>>`**；因 `sendFrame` 本身 synchronized，多线程（请求线程+心跳线程）写同一 Writer 桥不会撕裂帧，再经 `Flux.create(sink -> ...)` 推给客户端；
- 会话归属校验、失效响应语义与 MVC 完全一致，仅 HTTP 出入口换成 `ServerHttpRequest/ServerHttpResponse`。

**专用线程池**：`mcpThreadPool`（如 `Executors.newFixedThreadPool`，可配置），而非在 Netty event loop 上直接调用阻塞内核。请求处理与 SSE 写帧都在该池执行并回连响应式管道。

**Route 注册**：WebFlux 侧用 `RouterFunctions.route()` + `HandlerFunction` 组装，而非"去注解 Controller + `registerMapping`"（无 `RequestMappingHandlerMapping`）。路径布局与 MVC 相同（`{endpoint}/sse`、`{endpoint}/messages/{uuid}`、`{endpoint}`），从同一份路径→runtime 表按 endpoint 匹配。

**无认证 Filter（本次）**：不落地认证，双栈均无 Filter 层；后续接入认证时在栈无关层加 SPI（MVC `javax.servlet.Filter` / Flux `WebFilter`），共享同一路径→runtime 定位与会话归属校验。

**Rationale**：「同步入 Mono + 专用池」是官方 SSE SDK 在响应式服务端的通行适配方式；不改内核、不重写协议，只把同步边界收敛到池内；`McpWebBinding` 抽象让后续维护两种栈互不侵扰。

**Alternatives considered**：
- 把内核重写为响应式（Sinks/Mono 贯穿）：改动 `fw-plus-ai-mcp` 核心与 aj-mcp 交接面，风险高、收益低于本次范围，排除。
- 双栈各自独立路由/绑定而不复用核心：重复代码多、易漂移，排除。

## Risks / Trade-offs

[路由注册与 MVC 协同 → registerMapping 基于 Spring 5.3 稳定 API；用 HandlerMethod 而非裸 handler 以复用 MVC 参数解析与异常处理]

[注解归属与包扫描冲突 → `@McpServer` 绑定优先于 default；未标注方法始终进 default server，绑定孤儿（key 未在 servers 声明）启动报错]

[多实例共享 JVM → 明确为逻辑隔离，故障隔离需真多实例（独立进程/容器）/独立 MCP Server】

[per-instance 生命周期 → initializer 按 runtime 启停；registry 的 heartbeat/maintenance 定时任务随实例 start/close]

[WebFlux 阻塞内核入 Mono → 阻塞只发生在专用池；池耗尽不可抢占 Netty event loop；提供线程池大小配置与监控]

[WebFlux SSE Writer 桥 → 依赖 sendFrame 的 synchronized 保证单帧一致；桥实现的 flush/close 需与响应式 Sink 生命周期对齐（连接断开会话清除）]

[双栈行为漂移 → 路径→runtime 定位与会话归属校验逻辑收敛到栈无关抽象，双栈共享契约测试（同一用例集跑 MVC 与 Flux 两套 container）]

[认证遗留占位 → 认证为后续独立 change；占位 SPI 默认放行不影响现有行为，接入时只替换实现不重构 handler]

## Sequence

```
请求 → McpWebBinding 定位 runtime(path→runtime 表)
     → 会话归属校验(uuid / Mcp-Session-Id ∈ 本 runtime 会话表, 否则失效响应)
     → (Flux 仅) Mono.fromCallable(同步处理).subscribeOn(mcpThreadPool)
     → McpXxxController / HandlerFunction 分发
     → runtime.registry → McpServer.processMessage → 工具执行
     → (Flux SSE) Writer桥 → Flux<ServerSentEvent> 回写客户端
```