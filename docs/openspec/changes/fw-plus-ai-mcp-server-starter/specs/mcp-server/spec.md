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

### 6.1 Tools

1. `tools/list`：返回全部已注册工具，含 `name`、`description`、`inputSchema`（JSON Schema，遵循 JSON Schema 2020-12 默认方言）、可选 `annotations` 与 2025-11-25 的可选 `icon` 透传。
2. `tools/call`：按名称定位工具，参数校验失败返回工具执行错误结果；正常返回 content 数组（文本/资源链接等）与 `isError` 标记。
3. 工具列表变化时向会话发送 `notifications/tools/list_changed`。

### 6.2 Resources

1. `resources/list`：列举静态资源（含名称、URI、描述、mimeType）。
2. `resources/templates/list`：列举资源模板。
3. `resources/read`：按 URI（含模板展开后 URI）读取资源，返回文本或二进制 blob 内容；URI 无对应提供者时返回规范错误。
4. `resources/subscribe` / `resources/unsubscribe`：维护订阅关系；已订阅资源变化时发送 `notifications/resources/updated`。
5. 资源列表变化时发送 `notifications/resources/list_changed`。

### 6.3 Prompts

1. `prompts/list`：列举提示词模板，含名称、描述、参数声明（名称、描述、required）。
2. `prompts/get`：按名称与参数返回由 role 与 content 组成的消息列表；缺失必填参数返回参数无效错误。
3. 列表变化时发送 `notifications/prompts/list_changed`。

### 6.4 Completion

- `completion/complete`：针对 prompt 参数引用或资源路径引用的补全上下文，返回候选值列表（含可选 total 与 hasMore 分页语义），由已注册的 `McpCompletionProvider` 提供。

### 6.5 Logging

1. `logging/setLevel`：设置会话日志级别（debug/info/warning/error）。
2. 服务端按会话级别通过 `notifications/message` 下发日志（logger、data）；未设置时按默认级别。

## 7. 扩展模型：Spring Bean 发现

两种扩展方式可共存，均通过 Spring Boot 自动配置收集并注册到 Server 注册表。

### 7.1 Provider SPI（正式扩展点）

业务方声明以下接口的 Spring Bean（支持每个接口多个 Bean）：

- `McpToolProvider`：提供工具定义与调用执行。
- `McpResourceProvider`：提供资源/模板定义、读取与（可选）变化回调。
- `McpPromptProvider`：提供提示词定义与渲染。
- `McpCompletionProvider`：提供参数/路径补全候选。

SPI 模型（`McpTool`、`McpResourceInfo`、`McpResourceTemplate`、`McpResourceContent`、`McpPromptInfo`、`McpPromptArgument`、`McpPromptResult`、`McpToolCallRequest`、`McpToolResult`、分页/补全请求与结果等）语义与 MCP 规范字段一致；Provider 上下文允许访问会话级信息（如进度令牌）。

### 7.2 `@McpTool` 方法注解（便捷模式，仅 Tool）

1. 标注在 Spring Bean 的 public 方法上（`@McpTool(name=..., description=...)`），启动时扫描注册为工具；清理半成品中重复的 `McpTool`/`McpToolAnnotation` 注解与 `McpToolInterface.java` 文件名错乱，只保留一个规范注解类型。
2. 根据方法参数（含 `@McpToolParam` 或等价元数据）自动生成 `inputSchema`；基本类型、String、枚举、JavaBean、List/Map 均有确定的映射规则，不支持的参数类型在启动期快速失败并给出明确错误。
3. 方法返回值自动包装为工具结果；方法抛异常时返回 `isError: true` 的工具结果（不破坏 JSON-RPC 层）。
4. Resource 与 Prompt 的扩展只走 Provider SPI，不提供注解模式。

## 8. Spring Boot 自动配置

1. 配置前缀 `fw-plus.mcp`，支持总开关（`enabled`，默认开启）与多实例列表 `instances[]`，每实例可配置：`name`、`path`、`enabled`、`server-name`、`server-version`、`instructions`、`tools-enabled`、`resources-enabled`、`prompts-enabled`、`completions-enabled`、`logging-enabled`、会话超时等。
2. 未配置 instances 时提供一个默认实例（默认端点 `/mcp`、默认 serverInfo），做到真正开箱即用。
3. 自动配置条件：
   - classpath 存在 core 类；
   - `fw-plus.mcp.enabled` 不为 false；
   - SERVLET / REACTIVE 端点按 Web 应用类型互斥装配；
   - WebFlux 端点仅在 reactor/webflux 类存在时装配。
4. Starter 通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 与 `META-INF/spring.factories` 双注册（Boot 2.7 兼容）。
5. 配置属性提供 `spring-boot-configuration-processor` 元数据，IDE 可提示。

## 9. 测试与验证要求

1. core 单元测试：JSON-RPC 序列化与错误码、注册表注册/冲突、版本协商、`@McpTool` schema 生成与结果包装、Provider 调用路径。
2. 传输集成测试：
   - MVC：启动 Spring Boot 测试上下文，覆盖 initialize → notifications/initialized → 携带 `Mcp-Session-Id` 的 tools/list、tools/call、resources/list/read/subscribe、prompts/list/get、completion/complete、logging/setLevel + SSE 通知、GET SSE 流、DELETE 会话；
   - WebFlux：等价的响应式握手与至少 tools/resources/prompts 主链路。
3. 自动配置测试：缺省实例开箱即用、`enabled=false` 不装配、SERVLET/REACTIVE 互斥、SPI Bean 与注解 Bean 均被发现。
4. 样例应用可启动，以 JSON-RPC over HTTP 完成一次完整握手与工具调用。

## 10. 验收场景

Scenario: A1 - 创建完整的 fw-plus-ai-mcp 模块
- [ ] `fw-plus-ai-mcp`（com.xiongx9527.fw.plus）位于 core 层 fw-plus-ai 聚合下，修复全部编译错误（含类名/文件名错乱、缺失 SPI、Builder 字段），在 JDK 1.8 下 `mvn compile` 与 `mvn test` 通过；构件经 BOM 管理，AI 层以正确坐标引用。

Scenario: A2 - 完整实现 MCP 2025-11-25 协议规范
- [ ] 集成测试证明：initialize 版本协商（接受 2025-11-25/2025-06-18，拒绝其他版本）与 `Mcp-Session-Id` 会话成立后，Tools（list/call）、Resources（list/read/templates/subscribe）、Prompts（list/get）、Completion（complete）、Logging（setLevel/通知）均可通过 JSON-RPC 正确响应，listChanged 等通知经 SSE 下发；JSON-RPC 错误码与工具错误结果（isError）符合规范；Sampling/Roots/Tasks/Elicitation/OAuth 不实现。

Scenario: A3 - 创建 Spring Boot Starter 提供自动配置
- [ ] 引入 `fw-plus-ai-mcp-spring-boot-starter` 后，无自定义配置即可启动默认 `/mcp` 端点完成握手；`fw-plus.mcp.*` 可配置多实例与能力开关；MVC 与 WebFlux 按应用类型互斥装配，WebFlux 为 optional；fw-plus-boot 层重复空壳目录已删除。

Scenario: A4 - 支持通过 Spring Bean 扩展 MCP 能力
- [ ] 测试证明：声明为 Spring Bean 的 `McpToolProvider`/`McpResourceProvider`/`McpPromptProvider` 实现被自动发现并经对应 list/get/call 暴露；`@McpTool` 注解 Bean 方法被扫描注册、自动生成 inputSchema 并可被 tools/call 调用。

Scenario: A5 - 提供完整的使用文档和示例
- [ ] `fw-plus-sample/fw-plus-ai-samples` 下新增可运行的 MCP Server 样例（含 Tool/Resource/Prompt Bean 与配置文件，可启动并完成握手+调用）；Starter 提供使用文档（坐标、依赖、配置项、SPI/注解两种扩展方式、MVC/WebFlux 选择说明）；构建链 core→ai→sample 编译通过。
