# TinyEngine Console Starter 集成设计

## 背景

`fw-plus-tiny-engine-spring-boot-starter` 当前只声明了 Boot、TinyEngine Core、MyBatis-Plus 和 Web 依赖。TinyEngine Core 已包含实体、Mapper、Service、SQL 资源与基础自动配置，但 starter 未包含 TinyEngine 前端源码、生产构建产物或完整 Web API，应用引入 starter 后无法直接使用设计器。

本次变更把 `/Users/xiongxiao/Workspace/Idea/codes/TinyEngine` 的生产设计器集成进 starter，采用 Magic-API console 的组织模式：前端源码归属于 Maven 模块，由 Maven 生命周期构建，生产产物随 JAR 发布。集成完成后不再依赖开发机上的 TinyEngine 绝对路径。

## 目标

- starter 内置可独立构建的 TinyEngine pnpm workspace 源码。
- Maven 构建自动生成前端生产产物并打入 JAR。
- 启用 TinyEngine 后可通过 `/tiny-engine/` 打开设计器。
- 前端资源和 API 支持宿主应用的 `server.servlet.context-path`。
- Spring MVC 与 WebFlux Controller 覆盖生产设计器实际使用的 TinyEngine API，并复用现有 Core Service。
- TinyEngine Core 提供框架无关的用户、认证、租户和权限 SPI，供 Magic-API、Sa-Token、Spring Security 或业务 IAM 实现。
- 提供独立 Magic-API TinyEngine 插件，在 Magic-API 控制台增加设计器入口。
- 保持 `fw.plus.tiny-engine.enabled=false` 的默认关闭语义。
- 提供明确、可重复的上游源码同步方式和来源版本记录。

## 非目标

- 不在运行时启动 Node.js、Vite 或 Koa mockServer。
- 不把 TinyEngine 的 `.git`、`.codegraph`、`node_modules`、构建缓存或本地产物放入 starter。
- 不改变 TinyEngine 上游仓库的构建和发布流程。
- 不把 TinyEngine 业务实体、Mapper 或 Service 从 Core 移入 starter。
- 不把 mockServer 中仅用于演示、伪造或外部平台代理的行为原样移植到 Java。

## 模块边界

### Starter

`fw-plus-tiny-engine-spring-boot-starter` 负责：

- `src/console` 中的 TinyEngine 前端 workspace 源码与锁文件。
- Maven `generate-resources` 阶段的 pnpm 安装与生产构建。
- 将生产产物复制到 `META-INF/resources/tiny-engine/`。
- 声明 TinyEngine Core、Boot 自动配置、Web 和 MyBatis-Plus 依赖。
- classpath 资源存在性测试和前端来源清单。

同步到 `src/console` 的内容包括生产构建所需的根配置、`designer-demo`、`packages`、`mockServer` workspace 包、`scripts`、`patches`、锁文件及许可证。同步排除 `.git`、`.codegraph`、`node_modules`、所有 `dist`、测试报告、编辑器缓存和临时文件。`mockServer` 仅用于满足 workspace 依赖解析及契约参考，不作为运行时进程发布。

### Core

`fw-plus-tiny-engine-core` 继续负责：

- TinyEngine 领域模型、DTO、Mapper、Service 和动态模型能力。
- SQL 初始化资源和数据库访问。
- 用户主体、凭据、认证、账号、租户和权限的中立契约。
- 与 Servlet、静态资源和前端构建无关的业务逻辑。

Core 保持 Java 8 和框架边界，不引入 Node、Vite 或前端依赖。

### 用户体系 SPI

TinyEngine Core 定义中立 SPI，不依赖 Magic-API、Sa-Token、Spring Security、Servlet 或 WebFlux：

- `TinyEngineAuthenticationProvider`：登录、根据凭据解析当前主体、退出。
- `TinyEngineAccountProvider`：注册和找回密码；实现不支持时返回明确的 unsupported capability。
- `TinyEngineTenantProvider`：查询可用租户、切换租户、创建租户。
- `TinyEnginePermissionEvaluator`：校验主体对 TinyEngine 权限标识的访问能力。
- `TinyEngineUserContext`：向现有 Core Service 暴露当前 `TinyEngineUser`，由请求身份上下文适配，不自行解析具体工具的 token。
- `TinyEngineUserSystemService`：Core 中的基础用户体系服务，编排认证、账号、租户和权限 SPI，并向 Boot 暴露稳定的框架无关方法。

中立数据对象包括 `TinyEngineCredential`、`TinyEnginePrincipal`、`TinyEngineLoginRequest`、`TinyEngineLoginResult`、`TinyEngineRegistrationRequest`、`TinyEnginePasswordResetRequest`、`TinyEngineTenant` 和 `TinyEngineTenantCreateRequest`。凭据只表达 token、tenantId 和必要的标准字段，不包含 Servlet/WebFlux 请求对象。

Core 同时维护权限常量、输入校验、未登录/无权限/不支持能力异常、主体到现有 `TinyEngineUser` 的映射，以及 `TinyEngineUserSystemService` 默认编排实现。现有 `UserService`、`TenantService` 和相关实体继续作为 TinyEngine 本地用户/租户数据能力，但 Core 不内置密码散列、token 签发或第三方 SSO 实现。

Boot Web 层负责从 `Authorization: Bearer` 和 TinyEngine 组织头中构造 `TinyEngineCredential`，调用 Core 的 `TinyEngineUserSystemService`，并暴露 TinyEngine 前端需要的接口：

- `POST /platform-center/api/user/login`
- `POST /platform-center/api/user/register`
- `POST /platform-center/api/user/forgot-password`
- `GET /platform-center/api/user/me`
- `GET /platform-center/api/user/tenant`
- `GET /platform-center/api/logout`
- `POST /platform-center/api/tenant/create`

MVC 与 WebFlux 必须返回相同 JSON 契约。WebFlux 在进入阻塞 Scheduler 前解析并捕获 `TinyEnginePrincipal`，执行任务时显式绑定到 `TinyEngineUserContext`，不得依赖丢失请求上下文的普通 ThreadLocal。

TinyEngine 不提供 Magic-API 或 Sa-Token 的内置实现。外部工具通过业务 Bean 实现上述 SPI；同一应用可以替换为 Magic-API、Sa-Token、Spring Security、Keycloak、MaxKey 或自有 IAM。Magic-API TinyEngine 插件本次仍只提供 `statusBarItems` 入口，不承担认证适配。

当 `fw.plus.tiny-engine.security.enabled=true` 时，缺少 `TinyEngineAuthenticationProvider` 或 `TinyEnginePermissionEvaluator` 必须启动失败，不得回退到 `system` 用户。只有显式设置 `security.enabled=false` 的受信开发环境才允许使用 `SystemTinyEngineUserContext`，生产文档必须标明该模式不提供身份隔离。

### Boot Autoconfigure

`fw-plus-boot-autoconfigure` 负责：

- TinyEngine 总开关、Web 开关、路径和资源配置。
- TinyEngine Spring MVC Controller 与 Core Service 的装配。
- TinyEngine WebFlux Controller、静态资源处理和阻塞调用调度器的装配。
- `/tiny-engine` 到 `/tiny-engine/` 的规范化跳转。
- classpath 静态资源和 SPA 入口映射。
- 业务 Bean 覆盖默认 Controller/Adapter 时的让位条件。

TinyEngine 通用自动配置不再限制为 `ConditionalOnWebApplication.Type.SERVLET`。Web 层拆为互斥的 Servlet 和 Reactive 子配置：Servlet 子配置只在 `Type.SERVLET` 注册，Reactive 子配置只在 `Type.REACTIVE` 且 WebFlux 类存在时注册。纯 WebFlux 应用不得加载 Servlet Controller、Servlet API 或 MVC 资源处理器。

### Magic-API TinyEngine Plugin

新增 `fw-plus-magicapi-plugin-tiny-engine`，归属于 `fw-plus-magicapi-plugin` 聚合器。该插件只提供 TinyEngine 入口，不新增 Magic-Script 模块、动态数据源或 Magic-API 资源类型。

插件负责：

- 依赖 `fw-plus-tiny-engine-spring-boot-starter`，确保入口对应的设计器和后端能力位于同一应用。
- 在 `src/console` 中提供符合现有 Magic-API 插件协议的前端代码。
- 通过 Magic-API 前端插件返回值的 `statusBarItems` 注册一个 TinyEngine 图标入口和中英文标题。
- 点击后在新标签页打开宿主应用的 TinyEngine console 路径。
- 将构建产物打入 `magic-editor/plugins`，沿用 Magic-API 插件父 POM 的构建和资源复制机制。

入口 URL 从浏览器当前页面和宿主 context-path 推导，并使用 TinyEngine 的 `web.console-path` 默认值 `/tiny-engine/`。不得硬编码协议、域名、端口或开发机路径。若 TinyEngine 被禁用或入口返回 404，插件显示明确提示，不影响 Magic-API 控制台其他功能。

## 前端构建与资源布局

Maven 使用 `exec-maven-plugin`，在 `generate-resources` 阶段于 `src/console` 执行：

1. `pnpm install --frozen-lockfile`
2. `pnpm build:prod`

随后由 Maven resources 处理把 `designer-demo/dist` 复制到 `META-INF/resources/tiny-engine/`。TinyEngine Vite 默认使用相对资源路径 `base: './'`，因此静态资源可随宿主 context-path 工作。前端 API 使用同源相对地址，不硬编码主机、端口或本地路径。

构建失败必须使 Maven 构建失败。不得在缺少 pnpm、锁文件不一致或前端编译失败时静默打出不完整 JAR。为便于纯 Java 快速验证，可提供显式的 `tiny-engine.frontend.skip` Maven 属性，但发布和完整验证不得启用该属性。

## 静态资源访问

启用 `fw.plus.tiny-engine.enabled=true` 且 `fw.plus.tiny-engine.web.enabled=true` 后：

- `GET /tiny-engine` 返回重定向到 `/tiny-engine/`。
- `GET /tiny-engine/` 返回 classpath 中的 `index.html`。
- `GET /tiny-engine/{asset}` 返回对应构建资源。
- 前端若使用客户端路由，非 API 且无文件扩展名的 `/tiny-engine/**` 请求回退到 `index.html`。
- 静态资源不存在时返回 404，不回退到外部文件系统。

资源映射必须基于 Spring `Resource`/classpath API，不拼接硬编码文件系统路径。

## API 适配

API 适配以 TinyEngine 生产前端中的实际请求为主，以 `mockServer/src/routes` 为响应结构和路径兼容参考。第一阶段覆盖设计器核心闭环：

- 应用：列表、详情、创建、更新、删除、Schema 获取。
- 页面：列表、详情、创建、更新、删除、历史与发布数据。
- 区块：列表、详情、创建、更新、删除、分组、构建/发布状态。
- 物料和组件：物料、组件库、组件、业务分类。
- 模型和数据源：模型 CRUD、数据源 CRUD、动态模型访问。
- 模板、国际化和资源：模板、语言、词条、资源与资源分组。
- 用户上下文：复用 `TinyEngineUserContext`，允许宿主提供自定义实现。

Controller 只负责协议映射、参数校验和响应包装，业务逻辑调用现有 Service。已有 Service 不足以实现的生产接口，应先扩展 Core 契约并添加单元测试，不在 Controller 中复制 mock 数据逻辑。

平台认证、外部素材 CDN、AI 服务和代码生成等需要外部系统的能力使用明确的可选适配接口。宿主未提供适配器时返回稳定的“不支持/未配置”错误，不伪造成功。

## WebMVC 与 WebFlux Controller

Servlet 和 Reactive Controller 暴露相同的路径、HTTP 方法、请求结构、响应结构与错误码。协议映射可共享无 Web 框架依赖的 facade/handler，但不得让 WebFlux Controller 调用 Servlet 类型。

TinyEngine Core 的 MyBatis-Plus/JDBC Service 保持阻塞式实现。WebFlux Controller 使用 `Mono.fromCallable` 或 `Mono.fromRunnable` 包装每次 Core Service 调用，并通过 TinyEngine 专用 Scheduler 执行，禁止在 Reactor Netty 事件循环直接调用数据库或文件系统。集合结果默认返回 `Mono<Result<List<T>>>` 以保持现有 JSON 契约；只有真正的流式能力才返回 `Flux`，不为普通 CRUD 人为制造逐行流。

涉及用户隔离的 WebFlux 调用先在请求链中解析 `TinyEnginePrincipal`，再由 Reactive executor 把该主体绑定到任务作用域，任务完成或失败后必须清理。多个并发请求之间不得串用用户或租户。

专用 Scheduler 由自动配置提供可覆盖 Bean，使用有界线程数、有界队列和可识别线程名前缀。线程数、队列容量和任务超时纳入 `fw.plus.tiny-engine.webflux` 配置。队列饱和或超时返回受控的服务繁忙错误，不无限堆积任务。

静态资源在 Servlet 环境使用 MVC `ResourceHandlerRegistry`，在 Reactive 环境使用 WebFlux `ResourceHandlerRegistry`。两种实现都读取同一 classpath 位置和 `web.console-path`，并遵守相同的首页、资源 404 与 SPA 回退规则。

starter 不得通过同时传递 Servlet 与 Reactive starter 强迫 Spring Boot 选择 MVC。Web 框架依赖保持可选，宿主应用显式选择 `spring-boot-starter-web` 或 `spring-boot-starter-webflux`；TinyEngine sample 分别提供 MVC 与 WebFlux 验证入口，或使用两个测试应用证明同一 starter 可运行于两种 WebApplicationType。

## 配置

保留现有 `fw.plus.tiny-engine` 前缀，并扩展 Web 配置：

- `enabled`：总开关，默认 `false`。
- `web.enabled`：Web 与静态资源开关，默认 `true`。
- `web.base-path`：API 的宿主前缀，保持现有兼容语义。
- `web.console-path`：设计器路径，默认 `/tiny-engine`。
- `webflux.scheduler.thread-count`：阻塞 Core 调用线程数，提供保守默认值。
- `webflux.scheduler.queue-capacity`：阻塞任务最大排队数，提供有界默认值。
- `webflux.scheduler.timeout`：单次阻塞调用超时，默认使用明确的有限时长。
- `security.enabled`：用户体系强制开关，默认 `true`；关闭时只适用于受信开发环境。
- `resources.enabled`：SQL/资源装配开关，保持现有语义。

路径在绑定后统一规范化，拒绝空白、缺少前导 `/`、尾部重复 `/` 或包含路径穿越片段的非法值。Controller 映射与静态资源映射使用同一规范化结果。

Magic-API 入口默认跟随 `web.console-path`。如果 Magic-API 前端插件无法在构建时读取 Spring 配置，则由同源轻量配置端点返回启用状态和 console 路径；插件不得复制第二套独立路径配置。

## 错误处理

- 参数错误返回 HTTP 400 和稳定错误码。
- 缺少、无效或过期凭据返回 HTTP 401；权限不足返回 HTTP 403。
- 安全模式启用但缺少必须的认证/权限 SPI 时应用启动失败。
- 资源不存在返回 HTTP 404。
- 未配置的外部能力返回 HTTP 501 或项目统一的不支持错误。
- 数据库冲突和约束错误转换为受控业务错误，不泄漏 SQL、凭证或文件路径。
- 前端构建错误直接终止 Maven 构建并保留 pnpm/Vite 原始诊断。
- SPA 回退不得吞掉 API 404 或静态资源 404。

## 上游同步

提供一个显式同步脚本，从参数指定的 TinyEngine 工作区更新 `src/console`。脚本必须：

- 校验源目录包含 `pnpm-lock.yaml`、`pnpm-workspace.yaml`、`designer-demo` 和 `packages`。
- 使用固定排除清单，不复制本地依赖、Git 元数据和构建产物。
- 写入来源提交 SHA、同步时间、许可证和排除清单。
- 同步完成后提示运行前端构建、Maven 资源测试和差异检查。

脚本只作为维护工具，Maven 正常构建不得调用外部 TinyEngine 路径。

## 测试与验收

### 前端

- 在 starter 的 `src/console` 执行 `pnpm install --frozen-lockfile`。
- 执行 `pnpm build:prod`，确认生成 `designer-demo/dist/index.html` 和静态资源。
- 校验构建产物不包含开发服务地址或本机绝对路径。

### Maven 与打包

- 构建 starter，检查 JAR 包含 `META-INF/resources/tiny-engine/index.html` 和关键资源。
- 运行 classpath 资源测试，防止只在源码目录存在、JAR 中缺失。
- 检查 starter 依赖树，确认未引入 Node/Koa 运行时或无关 provider SDK。
- 构建 `fw-plus-magicapi-plugin-tiny-engine`，检查 JAR 包含对应 `magic-editor/plugins` 前端产物。
- 检查插件依赖树包含 TinyEngine starter，且没有重复打包 TinyEngine console 源码或产物。

### Spring

- 自动配置测试覆盖默认关闭、显式启用、Web 关闭、自定义 Bean 覆盖、非法路径，以及 Servlet/Reactive 配置互斥。
- MockMvc 覆盖入口重定向、首页、静态资源、SPA 回退、API 404 不回退。
- `ReactiveWebApplicationContextRunner` 与 `WebTestClient` 覆盖 WebFlux Controller 路由、首页、静态资源、SPA 回退和 API 404。
- WebFlux 测试验证阻塞 Service 不在 Reactor Netty 事件循环执行，并覆盖 Scheduler 队列饱和、超时和自定义 Bean 覆盖。
- 用户 SPI 契约测试覆盖登录、当前用户、退出、租户查询/切换、注册/找回密码不支持和权限拒绝。
- MVC/WebFlux 用户接口契约测试使用同一测试 Provider，确认 token、用户和租户响应一致。
- WebFlux 并发测试确认两个 token 的主体与租户不会在线程切换后串用。
- 安全模式测试覆盖缺 Provider 启动失败、业务 Bean 覆盖和显式关闭后才允许 system 用户。
- 纯 Reactive classpath 回归测试确认不创建 Servlet Controller、Servlet Filter 或 MVC ResourceHandler。
- API 契约测试覆盖每个核心领域至少一个读写流程及错误响应。
- TinyEngine 入口配置端点覆盖默认路径、自定义 context-path、自定义 console-path 和禁用状态。

### Magic-API 插件

- 前端契约测试确认插件只返回一个 TinyEngine `statusBarItems` 入口，不返回 `resource`、`toolbars`，也不注册 Magic-Script 模块。
- 验证入口 URL 从当前宿主地址和配置端点生成，不含固定域名或端口。
- 验证 TinyEngine 禁用或入口不可达时给出受控提示，不阻断 Magic-API 控制台初始化。

### 端到端

- 分别以 Servlet 和 Reactive WebApplicationType 启动 TinyEngine sample，并启用数据库测试配置。
- 在浏览器打开 `/tiny-engine/`，确认页面非空、资源无 404、控制台无启动错误。
- 完成创建应用、创建页面、保存页面、重新加载并读取保存结果的最小闭环。
- 在带 `server.servlet.context-path` 的配置下重复首页和核心 API 验证。

## 实施顺序

1. 建立 starter 前端目录、同步脚本、来源清单和许可证。
2. 调整 TinyEngine Vite 生产配置并完成独立 pnpm 构建。
3. 接入 Maven 生命周期和 JAR classpath 资源测试。
4. 拆分 Servlet/Reactive 自动配置，完成两套静态资源、SPA 映射和 WebFlux 阻塞调度器。
5. 在 Core 定义用户、认证、租户和权限 SPI，在 Boot 实现 MVC/WebFlux 用户接口与请求主体传播。
6. 按生产前端请求清单实现 MVC/WebFlux Controller 适配和缺失 Core 契约。
7. 新增 `fw-plus-magicapi-plugin-tiny-engine`、`statusBarItems` 入口和配置端点。
8. 扩展 Servlet/WebFlux sample 配置、测试用户 Provider 与数据库测试数据。
9. 运行前端、用户 SPI、Magic-API 插件、Maven、MockMvc、WebTestClient 和浏览器端到端验证。

## 完成标准

只有同时满足以下条件才算完成：starter 在不引用外部 TinyEngine 路径的情况下可重复构建；JAR 内含完整生产设计器资源；同一 starter 在 Servlet 与纯 Reactive 应用中均可启动；两种 Web 栈的首页、静态资源、用户接口和核心 API 可访问；安全模式不回退 system 用户；WebFlux 不在事件循环执行阻塞 Core 调用且不串用请求主体；核心应用/页面保存闭环通过；Magic-API 插件能从控制台正确打开同一应用的 TinyEngine 设计器；API、静态资源、用户 SPI、插件入口和 context-path 测试通过；现有无关脏文件未被修改。
