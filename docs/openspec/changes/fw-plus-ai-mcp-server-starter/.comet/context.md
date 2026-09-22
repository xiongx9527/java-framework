# Comet Design Handoff

- Change: fw-plus-ai-mcp-server-starter
- Phase: design
- Mode: compact
- Context hash: e7b85945c99f9893d4a8ba0e144fb726ecdc66a9eb6f56241fe39e8509220d23

Generated-by: comet-handoff.sh
Task hash policy: task-content-v1. Read tasks.md for live completion; excerpts are design-time context.

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## docs/openspec/changes/fw-plus-ai-mcp-server-starter/proposal.md

- Source: docs/openspec/changes/fw-plus-ai-mcp-server-starter/proposal.md
- Lines: 1-39
- SHA256: 201c4990cbc02dc7d5cd6d8f13fae89f1791d4a8cf7c8c41c779728d2fed671a

```md
# Proposal: fw-plus-ai-mcp-server-starter

## 问题

当前项目缺少一个面向 Java 8 / Spring Boot 2.7 的标准 MCP Server 启动器。Magic-API MCP 插件面向 Magic-API 场景，不能直接作为普通 Spring Boot 应用的标准 MCP 服务端使用；同时官方 Java SDK 基线要求过高，与本项目当前 Java 8 约束不兼容。业务系统若需要接入 MCP，仍缺少开箱即用的 Starter、传输层和扩展机制。

## 目标

在 fw-plus 中新增 `fw-plus-ai-mcp-spring-boot-starter`，提供：

- core 层自研、Java 8 兼容的 MCP 2025-11-25 协议内核；
- Spring Boot 自动配置，默认暴露 `/mcp` 端点；
- Streamable HTTP 传输，支持 MVC 与 WebFlux 两套实现，并按应用类型互斥装配；
- 通过 Spring Bean 扩展 Tools / Resources / Prompts / Completion 能力；
- 可运行样例与使用文档，构建链 core -> ai -> sample 可编译通过。

## 范围

- `fw-plus-core/.../fw-plus-ai-mcp`：协议模型、JSON-RPC 处理、Server 核心、注册表、传输抽象、MVC / WebFlux 两套实现、`@McpTool` 注解扫描支持。
- `fw-plus-ai/.../autoconfigure/mcp`：自动配置、配置属性、Provider Bean 收集与注解 Bean 扫描注册。
- `fw-plus-ai/.../fw-plus-ai-mcp-spring-boot-starter`：Starter 空壳构件、依赖传递与 Boot 2.7 兼容注册。
- `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-mcp-server-sample`：可运行样例。
- Starter 使用文档：坐标、依赖、配置项、SPI / 注解两种扩展方式、MVC / WebFlux 选择说明。
- 删除 `fw-plus-boot` 层重复空壳目录。

## 非目标

- 不实现 MCP Client。
- 不实现 Sampling、Roots、Tasks、Elicitation、OAuth。
- 不实现 2026-07-28 无状态协议改造。
- 不实现集群跨节点会话 / 通知。
- 不修改 Magic-API MCP 插件。

## 验收场景

- initialize 版本协商与 `Mcp-Session-Id` 握手可用，Tools / Resources / Prompts / Completion / Logging 可通过 JSON-RPC 正确响应，listChanged 等通知经 SSE 下发。
- 引入 Starter 后无自定义配置即可启动默认 `/mcp` 端点完成握手；`fw-plus.mcp.*` 可配置多实例与能力开关；MVC 与 WebFlux 按应用类型互斥装配。
- 声明为 Spring Bean 的 `McpToolProvider` / `McpResourceProvider` / `McpPromptProvider` 实现可被自动发现并暴露；`@McpTool` 注解 Bean 方法可被扫描注册并调用。
- `fw-plus-sample/fw-plus-ai-samples` 下可运行 MCP Server 样例可启动并完成握手 + 调用；构建链 core -> ai -> sample 编译通过。

```

## docs/openspec/changes/fw-plus-ai-mcp-server-starter/design.md

- Source: docs/openspec/changes/fw-plus-ai-mcp-server-starter/design.md
- Lines: 1-102
- SHA256: 1c910f29e40e63f51e69f7f5a77d6f75a3257c27d1def7cd83d2f271378026ea

[TRUNCATED]

```md
---
comet_change: fw-plus-ai-mcp-server-starter
role: technical-design
canonical_spec: openspec
---

# Design: fw-plus-ai-mcp-server-starter

## 1. 设计约束与假设

- Java 8 基线，Spring Boot 2.7.18，不使用 Java 9+ API。
- 不引入官方 MCP Java SDK；不修改 Magic-API MCP 插件。
- 支持双传输栈，但同一应用上下文只装配一套端点。
- 不实现集群会话存储、OAuth、Sampling、Roots、Tasks、Elicitation。

## 2. 总体架构

采用“core 协议内核 + fw-plus-ai 自动配置 + Starter 依赖传递”的三层结构：

- `fw-plus-ai-mcp`：负责 JSON-RPC、Server 核心、注册表、SPI、传输抽象和双栈实现。
- `fw-plus-ai-boot-autoconfigure`：负责配置属性、实例管理、Provider 收集、`@McpTool` 扫描、Web 栈条件装配。
- `fw-plus-ai-mcp-spring-boot-starter`：负责传递默认依赖和注册自动配置。

两套传输实现共享同一份协议逻辑，避免行为和测试双轨漂移。

## 3. 核心模块设计

### 3.1 协议与 Server 核心

- `McpAsyncServerImpl` 作为主分发器，处理 `initialize`、`notifications/initialized`、`ping`、`tools/*`、`resources/*`、`prompts/*`、`completion/complete`、`logging/setLevel` 与通知方法。
- `initialize` 只接受 `2025-11-25` 和 `2025-06-18`；成功时回显客户端版本，并返回实例实际开启的 `capabilities`、`serverInfo` 和可选 `instructions`。
- 会话状态保存在内存 `ConcurrentHashMap` 中；未携带有效 `Mcp-Session-Id` 访问需会话方法时返回规范错误状态。
- `McpServerBuilder` 负责组合 `McpServerInfo`、`McpServerCapabilities`、工具/资源/提示词/补全提供者，并暴露构建入口。

### 3.2 注册表与扩展点

- `McpToolRegistry`、`McpResourceRegistry`、`McpPromptRegistry`、`McpCompletionRegistry` 分别维护对应能力。
- Provider SPI 允许业务方注册多个 Bean；自动配置在 `McpAutoConfiguration` 中收集并注入实例。
- `@McpTool` 扫描只作用于业务 Bean；框架包和 `com.xiongx9527.fw.plus.ai.mcp` 包被显式跳过，减少误注册。

### 3.3 传输抽象

- `McpStreamableTransport` 定义 POST/GET/DELETE 与 SSE 写入抽象。
- Servlet 实现基于 `SseEmitter`；WebFlux 实现基于 `Flux<ServerSentEvent>`。
- `AbstractStreamableTransport` 抽取会话创建、过期判断、Origin 校验和 SSE 事件写入公共逻辑，降低双栈实现漂移。

## 4. Spring Boot 自动配置

- `fw-plus.mcp.enabled` 控制总开关，默认开启。
- `fw-plus.mcp.instances[].name`、`path`、`enabled`、`server-name`、`server-version`、`instructions`、`tools-enabled`、`resources-enabled`、`prompts-enabled`、`completions-enabled`、`logging-enabled`、`session-timeout-seconds` 控制实例行为。
- 未配置 `instances` 时提供默认实例，默认路径 `/mcp`。
- MVC 配置仅在 `SERVLET` 应用且存在 `DispatcherServlet`、`SseEmitter`、`RequestMappingHandlerMapping` 时生效。
- WebFlux 配置仅在 `REACTIVE` 应用且存在 `DispatcherHandler`、`ServerSentEvent`、`Flux` 时生效。
- 若业务应用同时引入 MVC 和 WebFlux 依赖，也只按当前 Web 应用类型装配一套端点。

## 5. Starter 与依赖策略

- Starter 默认传递 `spring-boot-starter-web`，保证普通 Spring MVC 应用开箱即用。
- `spring-boot-starter-webflux` 在 Starter 中设为 `optional`，由需要响应式传输的使用方显式引入。
- core 层 `fw-plus-ai-mcp` 对 `spring-boot-starter-web` 和 `spring-boot-starter-webflux` 均使用 `optional`，避免强依赖某一栈。
- AI 层和 sample 层通过 BOM 或父 POM 管理版本，避免跨层散改版本号。

## 6. 测试策略

### 6.1 Core 单元测试

- JSON-RPC 请求/响应/错误码序列化验证。
- `initialize` 版本协商：支持版本、不支持版本、缺少版本时的行为。
- 注册表：重复注册、空注册、Provider 调用异常包装。
- `@McpTool`：schema 生成、基本类型映射、异常返回 `isError: true`。

### 6.2 传输集成测试

- **Servlet**：`MockMvc` + `SseEmitter` 验证 initialize → `notifications/initialized` → 带 `Mcp-Session-Id` 的 tools/resources/prompts/completion/logging 调用，以及 SSE 通知和 DELETE 会话清理。
- **WebFlux**：`WebTestClient` 验证响应式握手、主能力和 SSE 事件流。

### 6.3 自动配置测试

- 默认实例无配置启动即可访问 `/mcp`。
- `enabled=false` 时不注册端点。

```

Full source: docs/openspec/changes/fw-plus-ai-mcp-server-starter/design.md

## docs/openspec/changes/fw-plus-ai-mcp-server-starter/tasks.md

- Source: docs/openspec/changes/fw-plus-ai-mcp-server-starter/tasks.md
- Lines: 1-11
- SHA256: f27833cf48d97cbb1ed15ac89573fbfdbe6bdc8df3751b0f763719bb71350b6d

```md
# Tasks: fw-plus-ai-mcp-server-starter

- [ ] T1 修复 core 层 `fw-plus-ai-mcp` 编译问题，确保 JDK 1.8 下 `mvn compile` 与 `mvn test` 通过，并恢复 BOM 管理。
- [ ] T2 完成 MCP 2025-11-25 协议核心能力：initialize 版本协商、`Mcp-Session-Id` 会话、Tools、Resources、Prompts、Completion、Logging、listChanged 通知与错误码。
- [ ] T3 完成 Spring MVC 与 WebFlux 两套 Streamable HTTP 传输实现，覆盖 POST / GET / DELETE 与 SSE。
- [ ] T4 完成 `fw-plus-ai-boot-autoconfigure` 自动配置：默认实例、`fw-plus.mcp.*` 配置、多实例、能力开关、MVC / WebFlux 互斥装配。
- [ ] T5 完成 SPI 扩展与 `@McpTool` 注解扫描，支持 Provider Bean 与方法注解两种扩展方式。
- [ ] T6 删除 `fw-plus-boot` 层重复空壳目录，并确认 AI 层以正确坐标引用 core 构件。
- [ ] T7 新增 `fw-plus-ai-mcp-server-sample` 样例，完成握手与主能力调用验证。
- [ ] T8 补充 Starter 使用文档，覆盖坐标、依赖、配置项、扩展方式与传输栈选择说明。
- [ ] T9 完成构建链验证：core -> ai -> sample 编译通过，集成测试与样例可运行。

```

## docs/openspec/changes/fw-plus-ai-mcp-server-starter/specs/mcp-server/spec.md

- Source: docs/openspec/changes/fw-plus-ai-mcp-server-starter/specs/mcp-server/spec.md
- Lines: 1-169
- SHA256: 1b9cc5eb9297399f1d29534c7be939f3b539a6c52413d09c67451deb5124ec98

[TRUNCATED]

```md
# fw-plus MCP Server 能力规格

## 1. 能力概述

fw-plus 提供一个**不依赖 Magic-API、不依赖官方 Java SDK** 的标准 MCP（Model Context Protocol）Server 实现，面向 Java 8 / Spring Boot 2.7 应用。业务系统引入 Starter、完成最小配置后，即通过 Streamable HTTP 暴露 MCP 服务端，并通过 Spring Bean 扩展 Tools、Resources、Prompts、Completion 能力。

协议锚定版本 **2025-11-25**（有会话模型），并兼容 **2025-06-18** 客户端。

### 非目标

- MCP Client（独立待办）。
- Sampling（`sampling/createMessage`）、Roots（`roots/list`）等 server→client 反向请求能力。
- 实验性 Tasks（`tasks/*`）、Elicitation（`elicitation/create`，含 URL mode）。
- OAuth 2.1 授权流程、CIMD、增量 scope consent、XAA 等 2025-11-25 授权增强；端点不内置鉴权，由业务方安全层负责。
- stdio 传输；集群跨节点会话/通知；Redis 会话存储。
- 2026-07-28 无状态协议（无握手、`server/discover`、MRTR、`subscriptions/listen`）。
- Spring Boot 3 / Java 17 桥接；对 Magic-API MCP 插件的任何修改。

## 2. 模块与构件

| 构件 | GroupId | 位置 | 职责 |
| --- | --- | --- | --- |
| `fw-plus-ai-mcp` | `com.xiongx9527.fw.plus` | `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-mcp` | 自研协议内核：JSON-RPC 模型、Server 核心与注册表、SPI 接口、传输抽象及 MVC/WebFlux 两套实现、注解扫描支持 |
| `fw-plus-ai-boot-autoconfigure`（mcp 包） | `com.xiongx9527.fw.plus.ai` | `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/mcp` | `McpAutoConfiguration`、配置属性、MVC/WebFlux 条件装配、Provider Bean 收集与注解 Bean 扫描注册 |
| `fw-plus-ai-mcp-spring-boot-starter` | `com.xiongx9527.fw.plus.ai` | `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-mcp-spring-boot-starter` | Starter 空壳构件：POM 依赖 + `AutoConfiguration.imports`（与 `spring.factories` 双注册，兼容 Boot 2.7） |
| `fw-plus-ai-mcp-server-sample` | —（样例） | `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-mcp-server-sample` | 可运行样例 |

要求：

1. core 构件由 `fw-plus-dependencies`（core BOM）恢复 dependencyManagement 条目；AI 层 BOM 管理 starter 构件，并以正确坐标 `com.xiongx9527.fw.plus:fw-plus-ai-mcp` 引用内核。
2. 全部构件版本为 `0.1.1-SNAPSHOT`（`${revision}`），Java 源码/目标 1.8，UTF-8，Tab 缩进。
3. 删除 fw-plus-boot 层的重复空壳目录 `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-ai-mcp-spring-boot-starter/`。
4. core 模块对 Spring Web 的依赖范围须允许同时支撑 MVC 与 WebFlux 两套传输，且不把任何一栈强加给使用方；Starter 默认传递 Spring MVC，WebFlux 依赖为 `optional`。
5. 不修改 Magic-API MCP 插件、RAG、tinyflow 等无关模块的已跟踪文件；tinyflow 未跟踪前端资源保持原样。

## 3. 初始化握手与版本协商

1. 客户端向端点 `POST` JSON-RPC 请求 `initialize`，参数含 `protocolVersion`、`capabilities`、`clientInfo`。
2. 服务端：
   - 当 `protocolVersion` 为 `2025-11-25` 或 `2025-06-18` 时，握手成功，响应 `protocolVersion` 回显客户端请求版本（服务端在该会话内按该版本行为应答）；响应同时包含 `capabilities`（仅声明实例实际开启的能力）、`serverInfo`（名称、版本，可选 description）与可选 `instructions`。
   - 当版本为其他值时，返回 JSON-RPC 错误（协议层版本不支持），不创建会话。
3. 握手响应通过 HTTP 响应头 `Mcp-Session-Id` 下发会话标识；后续同一 SSE 会话的 `POST`/`GET`/`DELETE` 请求须携带该头。未携带有效会话头访问需要会话的方法时返回规范的会话错误 HTTP 状态。
4. 客户端随后发送 `notifications/initialized` 通知，会话进入正常服务状态。
5. `ping` 方法在已初始化会话中返回空 result。

## 4. JSON-RPC 消息与错误处理

1. 请求/响应/通知遵循 JSON-RPC 2.0：`jsonrpc`、`id`、`method`、`params`；批量请求不是 Streamable HTTP 必需能力。
2. 错误使用标准 JSON-RPC 错误码：解析错误 `-32700`、无效请求 `-32600`、方法不存在 `-32601`、参数无效 `-32602`、内部错误 `-32603`；工具执行业务失败使用 MCP 工具错误结果（`isError: true` 的 tool result），而非协议错误，以支持模型自我纠正。
3. 未开启/未注册能力对应的方法返回方法不存在或能力未启用错误。
4. 所有响应可通过 Jackson 正确序列化/反序列化；字段命名与 MCP 规范一致（camelCase），不向客户端泄漏 Java 内部类型。

## 5. Streamable HTTP 传输

端点基址可配置（默认 `/mcp`），支持多实例（每个实例独立名称与路径）。

### 5.1 通用行为

1. `POST`：`Content-Type: application/json`，请求体为单条 JSON-RPC 消息；无返回服务端通知时响应 `Content-Type: application/json` 的单条 JSON-RPC 响应；需要流式返回时按规范使用 `text/event-stream`。
2. `GET`：建立/恢复该会话的 SSE 流，用于服务端→客户端通知；支持服务端按规范主动断开（轮询式 SSE）与基于事件 ID 的恢复。
3. `DELETE`：关闭会话、释放会话资源（SSE sink、订阅关系），返回规范 HTTP 状态。
4. 会话标识头严格使用 `Mcp-Session-Id`（纠正半成品中的 `X-Session-Id`）。
5. 资源订阅、listChanged、日志、进度等服务端通知经该会话 SSE 流下发。
6. 无效 `Origin` 头返回 HTTP 403。

### 5.2 Spring MVC（Servlet）实现

- 使用 `spring-webmvc`：`@RestController` 接收 POST/GET/DELETE；GET 流通过 `SseEmitter`（或等价 Servlet 异步机制）实现；会话注册表默认内存实现（`ConcurrentHashMap`），会话超时可配置。

### 5.3 Spring WebFlux 实现

- 使用 spring-webflux + reactor：响应式 Controller 与 `Flux<ServerSentEvent>`（或等价机制）实现 POST/GET/DELETE。
- 自动配置通过 `@ConditionalOnWebApplication(type = SERVLET | REACTIVE)` 互斥装配，同一应用上下文不会同时注册两套端点。
- 两套传输共享同一套 core Server 核心、注册表与 SPI，协议行为完全一致。

> 边界条件：若业务应用同时存在 `spring-boot-starter-web` 和 `spring-boot-starter-webflux`，服务端仍只按实际 Web 应用类型装配一套端点，不保证双栈同时生效。

## 6. 服务端能力

能力可通过实例配置独立开关（tools/resources/prompts/completions/logging），能力声明与实际分发保持一致。

```

Full source: docs/openspec/changes/fw-plus-ai-mcp-server-starter/specs/mcp-server/spec.md
