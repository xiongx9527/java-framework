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
- SERVLET / REACTIVE 互斥测试。
- SPI Bean 与 `@McpTool` Bean 的自动发现测试。

### 6.4 样例验证

- `fw-plus-ai-mcp-server-sample` 启动后可通过 HTTP JSON-RPC 完成握手和工具调用。

## 7. 风险与缓解

| 风险 | 影响 | 缓解 |
| --- | --- | --- |
| 自研内核与官方规范细节不一致 | 客户端兼容性 | 集成测试覆盖 spec 关键场景，Build 阶段增加协议对照检查 |
| 双栈依赖共存时的端点预期混乱 | 运维配置错误 | 文档明确说明互斥规则，并在自动配置测试中验证 |
| 内存会话丢失 | 重启后客户端需重新握手 | 明确非目标，由业务安全层或网关处理会话保持 |
| `@McpTool` 扫描误伤框架 Bean | 意外暴露内部方法 | 跳过框架包和 fw-plus 自身包，失败时记录警告日志 |

## 8. 实现顺序建议

1. 修复并冻结 core 层编译与测试基线。
2. 完成 transport 双栈实现和集成测试。
3. 完成自动配置、配置属性和 Starter 空壳。
4. 补充样例、文档和最终构建链验证。
