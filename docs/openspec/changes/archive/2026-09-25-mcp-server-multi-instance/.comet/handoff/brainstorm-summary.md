# Brainstorm Summary

- Change: mcp-server-multi-instance
- Date: 2026-09-24

## 确认的技术方案

多 runtime 化的 MCP Server Starter，对齐 Quarkus MCP Server 模式（借鉴模式，不复制实现，仓库约束 Java 1.8）：

- **runtime 粒度**（D1）：每个 `McpServerRuntime` 持有独立 `McpServer` + `FeatureMgr` + 传输 registry（`ServerSseRegistry` 或 `ServerStreamableHttpRegistry`），依托内核实例字段隔离（`ServerSse.connections` / `ServerStreamableHttp.streams,sessions` / `McpServer.sessionStates` 均实例级）。
- **配置模型**（D2）：顶层 `fw-plus.ai.mcp.*` 即 default server（key=`default`），`servers[]` 追加命名 runtime（复用 `McpProperties` 递归绑定）；同 transport 下 endpoint 唯一，跨 transport 可重复。
- **编程式路由**（D3）+ 双栈（D9）：MVC 用 `RequestMappingHandlerMapping.registerMapping`（Controller 去注解、实例化持 runtime），SSE 动态消息路径 `{endpoint}/messages/{uuid}`（内核 `openSession` 第三参即下发路径，不改内核）；WebFlux 用 `RouterFunctions.route()` + HandlerFunction，同步内核调用 `Mono.fromCallable().subscribeOn(mcpThreadPool)`，SSE 出自实现 `java.io.Writer` 桥（底层 Writer 转发到 `FluxSink`）→ `Flux<String>`，`sink.onDispose` 清理会话；双栈互斥条件装配（`@ConditionalOnClass` + `@ConditionalOnWebApplication`）。
- **生命周期**（D4）：`McpRuntimeFactory` 构建 default+命名 runtime；`McpServerInitializer` 合并全部待扫包，`McpFeatureScanner` 一次扫描按 `@McpServer` 归属注册（反射调内核私有 `add*`，注册顺序与 `FeatureMgr#init` 一致），再 per-runtime `server.start()`。
- **认证 SPI 占位**（D5）——按用户 2026-09-24 决定**移出本次 change**：仅 `McpAuthService.authenticate(McpRequest) -> McpPrincipal` + `PermitAllAuthService` 默认放行（`McpPrincipal.ANONYMOUS`），`@ConditionalOnMissingBean` 可覆盖；Sa-Token / OAuth2+JWKS、scope 裁剪、401/403、`McpAuthFilter` 均为后续独立 change。
- **会话归属校验**（D6）：uuid / `Mcp-Session-Id` 必须 ∈ 本 runtime 会话表（core 层 `BaseServerRegistry.hasSession`），否则 404 失效响应，不进内核；MVC 与 Flux 行为一致。
- **能力归属**（D7）：`@McpServer("key")`（core `fw-plus-ai-mcp`）作用于 `@McpService` 类或方法，方法级覆盖类级，未标注归 default；绑定孤儿（key 未声明）启动报错。
- **集群广播**（D8）：频道仍 `fw-plus:mcp:broadcast`，payload 增加 `routeKey`，消费端据此定位本机对应 runtime 的 registry 再做 `sendOther`/`broadcastOther`。

## 关键取舍与风险

- 反射调用内核私有 `add*`（内核仅公开整包 `init`）：与项目既有 `ServerSseRegistry` 反射模式一致，有单元测试守卫，内核升级风险可接受。
- WebFlux 保留同步内核 + 专用池：阻塞只发生在池内，池大小可配（`fw-plus.ai.mcp.thread-pool-size`，默认 16）。
- SSE Writer 桥依赖 `sendFrame` 的 synchronized 保证单帧一致；底层 Writer 转发避免 println 分隔符丢失；连接断开 `doFinally/onDispose` 清会话。
- 单实例路径（无 `servers`、默认配置）行为与旧版一致；既有 7 个 MCP 测试需随装配结构改写（Bean selection → runtime/factory/路由/双栈契约）。
- 双栈行为漂移风险：路径→runtime 定位与归属校验语义收敛到相同实现，双栈共享契约用例集。

## 测试策略

- 配置绑定：default+命名 servers 绑定、endpoint 唯一性冲突报错（单测）。
- 注解归属：类级/方法级/未标注、孤儿 key 报错（`McpFeatureScanner` 单测）。
- runtime 隔离：两个 runtime 会话表互不可见。
- 双栈路由：同传输多 endpoint、异传输同 endpoint、同传输同 endpoint（冲突）；MVC 与 Flux 各一套集成测试（`ApplicationContextRunner` / WebTestClient）。
- 会话归属校验：伪造/跨 runtime uuid 返回失效响应，双栈一致。
- 集群广播：routeKey 定位，消息只投递目标 runtime 会话，不交叉重复。
- 认证占位：默认 `PermitAllAuthService` 行为与旧单实例一致。

## Spec Patch

无（delta spec 已在 Open 阶段定稿；认证授权相关内容已按用户决定从本次 change 移除，spec 中无遗留认证条款）。
