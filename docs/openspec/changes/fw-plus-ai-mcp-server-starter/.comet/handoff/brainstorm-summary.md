# Brainstorm Summary

- Change: fw-plus-ai-mcp-server-starter
- Date: 2026-09-21

## 确认的技术方案

保留当前已落地的自研内核 + 双传输栈 + Spring Boot 自动配置方案，不做推翻性重构：

- core 层继续使用 `fw-plus-ai-mcp` 自研 MCP 协议内核；
- 传输层继续采用 Streamable HTTP：Servlet 用 `SseEmitter`，WebFlux 用 `Flux<ServerSentEvent>`；
- 两套 transport 共享同一套 `McpAsyncServerImpl`、注册表和 SPI；
- 会话模型继续使用 `Mcp-Session-Id`，默认内存 `ConcurrentHashMap`，惰性过期清理；
- 自动配置继续使用 `fw-plus.mcp.*`，未配置时提供默认 `/mcp` 实例；
- MVC 与 WebFlux 按 `@ConditionalOnWebApplication` 互斥装配；
- Provider SPI 为正式扩展点；`@McpTool` 仅作为 Tool 便捷注解模式；
- Starter 继续放在 fw-plus-ai 层，并删除 fw-plus-boot 层重复空壳目录。

## 关键取舍与风险

- Java 8 兼容性：放弃官方 Java SDK，采用自研轻量内核，后续协议修订需手动跟进。
- 双栈互斥与可选依赖：Starter 默认传递 Spring MVC，WebFlux 为 optional；若使用方同时引入双栈，需以实际 Web 应用类型为准。
- 会话与通知可靠性：默认内存会话，实例重启会丢失 SSE 订阅与会话状态。
- 注解扫描范围：只扫描业务 Bean 的 `@McpTool`，并跳过框架包与 fw-plus 自身包。

## 测试策略

- core 单元测试：JSON-RPC、版本协商、注册表、`@McpTool` schema 生成、Provider 调用路径。
- 传输集成测试：Servlet 与 WebFlux 分别覆盖握手、会话、主能力与 SSE 通知。
- 自动配置测试：默认实例、开关、互斥装配、SPI Bean 与注解 Bean 发现。
- 样例验证：`fw-plus-ai-mcp-server-sample` 可启动并完成握手与调用。

## Spec Patch

- 在 `specs/mcp-server/spec.md` 中补充边界条件：若业务应用同时存在 `spring-boot-starter-web` 和 `spring-boot-starter-webflux`，服务端只按实际 Web 应用类型装配一套端点，不保证双栈同时生效。
