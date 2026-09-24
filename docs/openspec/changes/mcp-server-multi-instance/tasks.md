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