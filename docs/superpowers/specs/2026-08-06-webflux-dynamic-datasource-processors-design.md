# WebFlux 动态数据源请求处理器设计

## 背景

`DsHeaderProcessor` 和 `DsSessionProcessor` 目前通过 `RequestContextHolder`、
`ServletRequestAttributes`、`HttpServletRequest` 和 `HttpSession` 读取请求数据。
因此 `@DS("#header.xxx")` 与 `@DS("#session.xxx")` 只能在 Servlet 请求中解析，
WebFlux 请求会得到 `null`。

本模块已经以可选依赖方式引入 `spring-boot-starter-webflux`，并已有按 Web 应用类型
装配配置的项目模式。本次变更在不改变现有注解写法的前提下补充 WebFlux 请求支持。

## 目标

- 在 WebFlux 请求处理过程中同步调用 JDBC/MyBatis 时支持
  `@DS("#header.<name>")` 和 `@DS("#session.<name>")`。
- 完全保留现有 Servlet 行为和处理器链顺序。
- WebFlux 不可用或应用不是 Reactive Web 类型时，不装配 WebFlux 桥接组件。
- 请求头、Session 或目标属性不存在时继续返回 `null`。
- 请求上下文必须按订阅和信号作用域隔离，避免并发请求串值或线程池残留。
- 保持 Java 8 和 Spring Boot 2.7.18 兼容。

## 非目标

- 不支持 `Mono`/`Flux` 内延迟执行的 JDBC 操作跨调度器传播动态数据源。
- 不改造 dynamic-datasource 使用的 `ThreadLocal` 路由模型。
- 不引入 R2DBC，也不改变 `@DS`、`#header`、`#session` 的语法。
- 不改动与 JDBC 动态数据源无关的 WebFlux 过滤器。

## 方案选择

采用“不可变请求快照 + Reactor Context + 受控 ThreadLocal 桥接”。

未采用仅由 `WebFilter` 在请求开始时设置、结束时清理 ThreadLocal 的方案，因为
WebFlux 请求可能在线程间交错，长时间占用 ThreadLocal 会导致上下文丢失或串值。

未采用响应式 AOP 路由改造，因为这需要同时重构 dynamic-datasource 的上下文模型，
明显超出同步 JDBC/MyBatis 调用的目标范围。

## 组件设计

### 请求快照

新增不依赖 Servlet 或 WebFlux 类型的不可变请求快照，保存：

- 请求 Header 的只读映射；
- 已解析 `WebSession` 属性的只读映射。

快照只复制路由解析所需的字符串查询数据，不持有 `ServerWebExchange`，避免请求对象
被线程上下文意外延长生命周期。

### 请求上下文持有器

新增包内请求上下文持有器，使用 ThreadLocal 暴露当前 WebFlux 请求快照，并提供
严格成对的设置、恢复能力。进入作用域时保存旧值，退出时恢复旧值或清理，支持嵌套
调用且避免污染复用线程。

### WebFlux 桥接

新增仅在 Reactive Web 应用且 WebFlux 类存在时生效的自动配置：

1. `WebFilter` 从 `ServerWebExchange` 复制 Header，并异步取得当前 `WebSession` 属性；
2. 将快照写入 Reactor Context；
3. Reactor 操作符桥接只在执行当前订阅的同步回调期间把快照暴露给请求上下文持有器；
4. 回调结束后立即恢复原 ThreadLocal，不把上下文保留到整个异步请求结束。

桥接使用唯一注册键，并在应用生命周期内幂等注册。没有本功能 Reactor Context 键的
订阅不执行 ThreadLocal 写入。

读取 `WebSession` 不修改其属性，也不显式调用 `start()`，因此空会话不会因本功能被
主动持久化。

### 处理器兼容

`DsHeaderProcessor` 和 `DsSessionProcessor` 保持现有 Servlet 读取逻辑优先：

1. 当前上下文是 `ServletRequestAttributes` 时，按原逻辑读取；
2. 否则尝试从 WebFlux 请求快照读取；
3. 两者均不可用时返回 `null`。

`DynamicDataSourceAopConfiguration` 的处理器链保持
Header -> Session -> SpEL，不改变现有自定义 `DsProcessor` 覆盖机制。

## 数据流

1. WebFlux 请求进入桥接过滤器。
2. 过滤器构造 Header 与 Session 属性快照并写入 Reactor Context。
3. Spring WebFlux 在该请求的同步信号回调中调用 Controller 或 Service。
4. `@DS` AOP 调用对应处理器。
5. 处理器从当前快照返回数据源名称，dynamic-datasource 按原机制完成同步路由。
6. 同步回调退出后，桥接立即恢复 ThreadLocal。

## 错误与边界处理

- 空 key、缺少 Header 名或 Session 属性名：沿用现有行为，返回 `null`。
- 没有 Web 请求上下文：返回 `null`，继续交给后续处理器。
- 没有已有 Session 或属性：返回 `null`，不创建持久化会话数据。
- Servlet 与 WebFlux 快照同时存在：Servlet 上下文优先，保证历史行为稳定。
- 请求取消、异常或正常完成：ThreadLocal 均按同步回调作用域恢复。

## 测试策略

按 TDD 分三层验证：

1. 处理器单元测试：WebFlux 快照可读取 Header/Session，缺失值返回 `null`，Servlet
   行为和优先级不变。
2. 桥接单元测试：上下文嵌套恢复、正常/异常/取消路径清理、无上下文订阅不受影响。
3. WebFlux 集成测试：通过真实 WebFlux 请求验证 `#header`、`#session` 在同步处理器
   调用中可见，并发请求不会串值。

同时运行现有 `DsHeaderProcessorTest`、`DsSessionProcessorTest`、
`DynamicDataSourceAopConfigurationTest` 以及 `fw-plus-boot-autoconfigure` 相关测试。

## 验收标准

- WebFlux 请求 Header `ds: tenant_a` 可被 `#header.ds` 解析为 `tenant_a`。
- WebFlux `WebSession` 属性 `ds=tenant_b` 可被 `#session.ds` 解析为 `tenant_b`。
- 两个并发请求使用不同值时，各自解析结果稳定且请求结束后线程上下文为空。
- Servlet 处理器现有测试全部通过。
- 非 Reactive Web 应用不会创建 WebFlux 桥接 Bean。
- 模块测试通过，且生产代码不使用 Java 9+ API。
