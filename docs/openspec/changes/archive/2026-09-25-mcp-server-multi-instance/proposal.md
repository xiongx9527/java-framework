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