---
comet_change: add-magicapi-webflux-support
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-28-add-magicapi-webflux-support
status: final
---

# Magic-API WebFlux Controller 与 Servlet 隔离设计

## 背景

当前 Boot 层 `MagicAPIBaseAutoConfiguration` 通过 Servlet Web 条件启用，并导入 `MagicAPIController`、`AuthConfiguration`、基础模块、资源服务、数据源动态注册和插件自动配置。该路径已经服务现有 Servlet/MVC 应用，且包含 Servlet-only 类型，例如 `FilterRegistrationBean`、`MagicHttpServletRequest`、Servlet `Filter`、Spring MVC controller mapping 等。

本变更的确认边界是：Servlet 环境进入目前已有路径，WebFlux 环境进入新的改造路径。本次只处理 Magic-API controller/管理接口与 Servlet-only 隔离，不实现完整 WebFlux 动态 API runtime。

## 目标

- Servlet/MVC 应用继续使用现有 Magic-API 自动配置、controller、auth filter 和 plugin controller registration 路径。
- WebFlux 应用新增独立管理接口入口，覆盖 `optionList`、`datasource/options`、`resource reload`、`single resource reload`、`datasource/myjdbc/test`。
- WebFlux-only 应用上下文不创建 Servlet-only bean，包括 `FilterRegistrationBean`、Servlet request/response 依赖和 MVC mapping 依赖。
- WebFlux 管理接口返回结构与现有 Servlet controller 等价。

## 非目标

- 不实现任意 Magic-API 动态脚本 API 的完整 WebFlux runtime。
- 不将所有插件 Web controller 或控制台前端入口改造成 Reactive。
- 不升级或替换 Magic-API 2.2.2。
- 不改变 Servlet/MVC 下已有路径、权限、响应结构和插件注册语义。

## 推荐架构

采用“公共管理服务 + 双 Web 入口”：

1. 保留现有 Servlet 入口。
   `MagicAPIBaseAutoConfiguration`、`MagicAPIController`、`AuthConfiguration` 和插件 MVC controller registration 继续由 Servlet 条件保护。必要时只补更明确的 `ConditionalOnWebApplication(Type.SERVLET)` 或 `ConditionalOnClass`，不迁移已有逻辑。

2. 抽取管理接口公共服务。
   从 `MagicAPIController` 中提取不依赖 Servlet request/response 的业务逻辑，例如 option list 生成、数据源 class options 获取、resource refresh、single resource refresh、datasource test。公共服务返回现有 `JsonBean` 和 payload 类型，供 Servlet 和 WebFlux 两个入口复用。

3. 新增 WebFlux 管理入口。
   新增 WebFlux 专用配置和 controller/handler，使用 `ConditionalOnWebApplication(Type.REACTIVE)` 与 Magic-API classpath/enable 条件启用。它调用公共管理服务，并在需要权限的接口处使用 WebFlux request adapter 或抽取后的授权 helper。

4. 明确隔离 Servlet-only bean。
   `AuthFilter`、`FilterRegistrationBean`、MVC mapping 和 Servlet plugin controller registration 保持在 Servlet 路径。WebFlux-only 测试必须证明这些 bean 不会被创建。

## 组件设计

### MagicApiManagementService

职责：承载管理接口业务逻辑，不依赖 Servlet 或 WebFlux 类型。

建议方法：

- `JsonBean<List<List<String>>> options()`
- `JsonBean<MagicJdbcClassOptions> datasourceOptions(MagicRequestAccess access)` 或在入口层先授权后调用无 request 参数方法
- `JsonBean<Boolean> reloadResource(String resource, MagicRequestAccess access)`
- `JsonBean<Boolean> reloadSingleResource(String type, String id, MagicRequestAccess access)`
- `JsonBean<String> test(DataSourceInfo info)`

其中 `MagicRequestAccess` 可以是小型内部抽象，用于承载 Magic-API 授权所需的用户和属性读取能力；如果实现中发现现有 authorization interceptor 只能接收 `MagicHttpServletRequest`，则优先实现一个轻量 WebFlux adapter，而不是扩大授权模型。

### Servlet 入口

现有 `MagicAPIController` 保留原路径和注解。实现上可以改为薄 controller：参数解析、Servlet request 传入、调用 `MagicApiManagementService`。这样 Servlet 外部行为保持不变，同时减少 WebFlux 入口重复逻辑。

`AuthConfiguration` 和 `AuthFilter` 继续只属于 Servlet 路径。若当前类上条件不够明确，应增加 Servlet Web 条件，确保 Reactive context 不加载。

### WebFlux 入口

新增 WebFlux 专用配置，例如 `MagicAPIWebFluxManagementAutoConfiguration`，由 Reactive Web 条件启用，并导入 WebFlux controller 或 functional handler。

WebFlux 入口应使用同一 Magic-API web base path。接口路径与 Servlet 管理接口保持逻辑一致：

- `${magic-api.web:/magic-api/web}/optionList`
- `${magic-api.web:/magic-api/web}/datasource/options`
- `${magic-api.web:/magic-api/web}/{resource}/reloadResource`
- `${magic-api.web:/magic-api/web}/{type}/reload/{id}`
- `${magic-api.web:/magic-api/web}/datasource/myjdbc/test`

对于需要授权的接口，WebFlux 入口负责把 `ServerWebExchange` 中的用户、属性、header 或 session 信息映射到 Magic-API 授权检查需要的最小对象。

### Cloud 与 AI 层

Cloud/AI 层不复制 WebFlux 管理接口实现。它们只检查是否需要把现有 `ConditionalOnWebApplication(Type.SERVLET)` 拆分或补充 Reactive 条件，确保：

- Servlet 应用仍走当前路径。
- WebFlux 应用不会因为 Cloud/AI Magic-API 插件配置导入 Servlet-only bean。
- WebFlux 管理接口基础能力由 Boot 层提供。

## 数据流

Servlet 管理接口：

```text
Servlet Request -> MagicAPIController -> MagicApiManagementService -> MagicConfiguration / ResourceService / JdbcOptionProvider -> JsonBean
```

WebFlux 管理接口：

```text
ServerWebExchange -> WebFlux Management Controller/Handler -> WebFlux request adapter or auth helper -> MagicApiManagementService -> JsonBean/Mono<JsonBean>
```

公共服务保持同步返回即可。WebFlux 入口可以用 `Mono.fromSupplier(...)` 包装；如 datasource test 或 reload 在实践中可能阻塞，可在入口层选择合适 scheduler，但这不是完整动态 API runtime 的承诺。

## 错误处理

- 授权失败继续使用 Magic-API 现有异常/`JsonBean` 语义。
- datasource test 保持现有行为：连接测试失败时返回错误消息，成功时返回 `ok`。
- resource reload 继续通过 `MyDefaultMagicResourceService` 刷新资源；类型转换失败或服务不可用时应暴露与 Servlet 等价的错误行为。

## 测试策略

1. Servlet 回归测试。
   使用现有 MVC 测试或 ApplicationContextRunner，确认 Servlet 环境下 `MagicAPIController`、`AuthFilter`、`FilterRegistrationBean` 和插件 controller registration 仍按当前路径装配。

2. WebFlux 装配测试。
   使用 Reactive ApplicationContextRunner 或 WebFlux 测试上下文，确认 enabled 时 WebFlux 管理入口注册，`fw.plus.magicapi.enabled=false` 时不注册。

3. WebFlux-only 隔离测试。
   断言 WebFlux-only context 中不存在 Magic-API Servlet `FilterRegistrationBean`、MVC `RequestMappingHandlerMapping` 依赖和 Servlet-only controller registration bean。

4. 管理接口行为测试。
   覆盖 option list、datasource options、datasource test、resource reload、single resource reload。重点断言返回 payload shape 与 Servlet 等价。

5. 范围边界测试或文档断言。
   验证报告中明确记录完整动态 API runtime 不属于本 change 验收范围。

## 实施顺序

1. 先抽取公共管理服务，并让现有 Servlet controller 调用该服务，确保 Servlet 测试仍通过。
2. 增加 Servlet-only 条件防护，避免 Reactive context 加载 Servlet filter/MVC registration。
3. 新增 WebFlux 管理入口和必要 request/auth adapter。
4. 补 Cloud/AI 条件边界测试和必要配置调整。
5. 运行 Boot 定向测试，并根据实际影响补跑 Cloud/AI 定向测试。

## Build 隔离

用户已明确 build 阶段需要 worktree。进入 build 阶段后应选择 worktree 隔离，并在创建 worktree 前确保当前 OpenSpec 产物、Design Doc 和后续 Plan 在 worktree 中可访问。若 Comet build 规则要求先提交计划或设计产物，应按规则提交最小必要产物后再创建 worktree。

## 风险与缓解

- 授权适配风险：WebFlux request 与 `MagicHttpServletRequest` 语义不完全一致。缓解方式是在入口层构造最小授权上下文，并用授权通过/失败测试固定行为。
- Servlet 回归风险：抽公共服务可能改变 controller 行为。缓解方式是让 Servlet controller 保持原路径、注解和返回结构，并先跑回归测试。
- 范围误解风险：WebFlux 管理接口可用不等于完整动态 API runtime 可用。缓解方式是在 spec、tasks、Design Doc 和验证报告中保持一致边界。
