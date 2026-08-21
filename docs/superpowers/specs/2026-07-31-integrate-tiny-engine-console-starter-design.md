# TinyEngine Console Starter 集成设计

## 背景

`fw-plus-tiny-engine-spring-boot-starter` 当前只声明了 Boot、TinyEngine Core、MyBatis-Plus 和 Web 依赖。TinyEngine Core 已包含实体、Mapper、Service、SQL 资源与基础自动配置，但 starter 仍需要补齐完整 Web API、MVC/WebFlux 适配、用户体系 SPI 和 Magic-API 入口。

2026-07-31 需求变更：TinyEngine 前端不再放入 starter。上游 `designer-demo` 是前端演示宿主壳，`mockServer` 是本地开发 mock 后端，二者均不进入 fw-plus starter 的源码、构建或发布产物。TinyEngine 前端由独立工程或外部静态站点部署，Magic-API 插件只提供 `statusBarItems` 入口跳转到配置地址。

后端能力以 `/Users/xiongxiao/Workspace/Idea/codes/tiny-engine-backend-java` 为完整生产参考源，而不是只参考前端 `mockServer` 或只迁移 Controller。当前参考源为 `release/v2.10.x` 分支、提交 `c116fa649122390baf212f53d82358617c63924d`，工作区必须保持干净后再抽取能力清单。该仓库中的 `base`、`app`、Mapper XML、SQL、动态模型、登录、RAG/AI、部署配置、定时任务、过滤器、文档和模型资产都必须进入覆盖审计；不能直接内置到 fw-plus 的内容也必须明确写出适配目标或延后原因。

## 目标

- starter 只内置 TinyEngine 后端能力，不内置 TinyEngine pnpm workspace、`designer-demo`、`mockServer` 或 `dist`。
- Maven 构建不需要 Node、pnpm，也不读取 `/Users/xiongxiao/Workspace/Idea/codes/TinyEngine` 等外部前端路径。
- TinyEngine 前端由独立工程或外部静态站点部署，并调用本 starter 暴露的后端 API。
- Magic-API TinyEngine 入口可打开同源默认路径 `/tiny-engine/`，也可通过插件属性打开外部 TinyEngine 前端 URL。
- Spring MVC 与 WebFlux Controller 覆盖生产设计器实际使用的 TinyEngine API，并复用现有 Core Service。
- 后端 Java 参考项目的业务能力、数据模型、Mapper、SQL、配置、部署与辅助资产有完整覆盖矩阵，迁入、适配或显式延后均有记录。
- TinyEngine Core 提供框架无关的用户、认证、租户和权限 SPI，供 Magic-API、Sa-Token、Spring Security 或业务 IAM 实现。
- 提供独立 Magic-API TinyEngine 插件，在 Magic-API 控制台增加设计器入口。
- 保持 `fw.plus.tiny-engine.enabled=false` 的默认关闭语义。
- 提供明确、可重复的上游源码同步方式和来源版本记录。

## 非目标

- 不在构建期或运行时启动 Node.js、Vite、pnpm 或 Koa mockServer。
- 不把 TinyEngine 前端源码、`.git`、`.codegraph`、`node_modules`、构建缓存、本地产物或 `dist` 放入 starter。
- 不改变 TinyEngine 上游仓库的构建和发布流程；前端部署独立处理。
- 不把 TinyEngine 业务实体、Mapper 或 Service 从 Core 移入 starter。
- 不把 mockServer 中仅用于演示、伪造或外部平台代理的行为原样移植到 Java。
- 不把 `tiny-engine-backend-java` 的独立 Spring Boot 应用、Docker Compose、Nginx 配置或本地模型目录按原样塞进 starter 运行时；这些内容必须被映射为 fw-plus sample、部署文档、可选适配器或显式非目标。

## 模块边界

### Starter

`fw-plus-tiny-engine-spring-boot-starter` 负责：

- 声明 TinyEngine Core、Boot 自动配置和 MyBatis-Plus 依赖。
- 不强制传递 Servlet 或 WebFlux starter，由宿主应用显式选择 Web 技术栈。
- 作为应用侧引入 TinyEngine 后端能力的公开入口。
- 不包含 `src/console`、前端锁文件、pnpm 构建、静态资源复制或 classpath 前端资源测试。

TinyEngine 前端源码不作为 Maven module 子目录同步。若后续需要前端，可作为独立工程、独立 artifact 或外部静态站点处理，不回到本 starter 内置发布模式。

### Core

`fw-plus-tiny-engine-core` 继续负责：

- TinyEngine 领域模型、DTO、Mapper、Service 和动态模型能力。
- SQL 初始化资源和数据库访问。
- 用户主体、凭据、认证、账号、租户和权限的中立契约。
- 与 Servlet、静态资源和前端构建无关的业务逻辑。

Core 保持 Java 8 和框架边界，不引入 Node、Vite 或前端依赖。

`tiny-engine-backend-java/base` 中属于领域基础能力的内容优先落在 Core：公共模型与异常、类型处理器、实体、DTO、Mapper、Mapper XML、Service、动态模型、保存 Hook、SQL 安全工具、Schema/JSON/文件安全工具、RAG/AI 的中立数据对象和可选 Provider 契约。与具体 Spring Web、Servlet 过滤器、JWT、RestTemplate、Swagger、定时任务或部署环境绑定的内容不进入 Core，而是放到 Boot 自动配置、starter sample、文档或外部适配器。

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
- TinyEngine WebFlux Controller 和阻塞调用调度器的装配。
- 业务 Bean 覆盖默认 Controller/Adapter 时的让位条件。

TinyEngine 通用自动配置不再限制为 `ConditionalOnWebApplication.Type.SERVLET`。Web 层拆为互斥的 Servlet 和 Reactive 子配置：Servlet 子配置只在 `Type.SERVLET` 注册，Reactive 子配置只在 `Type.REACTIVE` 且 WebFlux 类存在时注册。纯 WebFlux 应用不得加载 Servlet Controller、Servlet API 或 MVC 资源处理器。

### Magic-API TinyEngine Plugin

新增 `fw-plus-magicapi-plugin-tiny-engine`，归属于 `fw-plus-magicapi-plugin` 聚合器。该插件只提供 TinyEngine 入口，不新增 Magic-Script 模块、动态数据源或 Magic-API 资源类型。

插件负责：

- 在 `src/console` 中提供符合现有 Magic-API 插件协议的前端代码。
- 通过 Magic-API 前端插件返回值的 `statusBarItems` 注册一个 TinyEngine 图标入口和中英文标题。
- 点击后在新标签页打开同源默认 TinyEngine 路径或 `pluginData.attributes.url` 配置的外部前端地址。
- 将构建产物打入 `magic-editor/plugins`，沿用 Magic-API 插件父 POM 的构建和资源复制机制。

入口 URL 默认从浏览器当前页面和宿主 context-path 推导，默认打开 `/tiny-engine/`；也支持 `pluginData.attributes.path` 覆盖同源路径，或 `pluginData.attributes.url` 打开独立部署的 TinyEngine 前端。不得硬编码开发机路径。

## 前端构建与资源布局

本 starter 没有前端构建与资源布局。POM 不配置 `exec-maven-plugin` 执行 Node/pnpm，不把 `designer-demo/dist` 复制到 `META-INF/resources/tiny-engine/`，也不提供 `tiny-engine.frontend.skip` 之类的前端构建开关。

前端部署由独立工程负责。它可以选择同源 `/tiny-engine/`、网关路径或独立域名，但这属于外部部署和 Magic-API 入口配置，不属于 starter JAR 内容。

## 静态资源访问

starter 不注册 TinyEngine classpath 静态资源、SPA 首页或资源回退。`/tiny-engine/` 是否存在由外部前端部署、网关或宿主应用自行决定。

后端 API 仍由 MVC/WebFlux Controller 暴露，并遵守 `fw.plus.tiny-engine` 配置。后端实现不得从外部文件系统读取前端源码或 dist。

## API 与后端能力适配

API 与后端能力适配以 `tiny-engine-backend-java` 的 Controller、Service、DTO、Result 模型、Mapper XML、SQL 和测试为主，以 TinyEngine 生产前端中的实际请求校验入口是否被使用，以 `mockServer/src/routes` 作为次级响应结构和路径兼容参考。不得只按 mockServer 生成假数据，也不得只迁 Controller 而漏掉 Service、Mapper、SQL、配置或辅助能力。

后端参考源必须先形成覆盖矩阵，至少包含：

- `base/src/main/java/com/tinyengine/it/common/**`：`Result`、基础实体、异常、日志注解、类型处理器、文件安全、Hash、Schema/JSON 工具；映射到 Core common/support 和 Boot 全局错误处理。
- `base/src/main/java/com/tinyengine/it/model/**`：全部实体和 DTO；映射到 Core model，字段、JSON 结构和类型处理器需要有契约测试。
- `base/src/main/java/com/tinyengine/it/mapper/**` 与 `base/src/main/resources/mappers/*.xml`：28 个 Mapper Java/XML；映射到 Core mapper 和资源目录，名称集合不得缺失。
- `app/src/main/resources/sql/**`、`docker-deploy-data/mysql/init/**`：H2/MySQL/PostgreSQL DDL 与初始化数据；映射到 Core `META-INF/tiny-engine/sql` manifest 和 sample 初始化文档。
- `base/src/main/java/com/tinyengine/it/service/**`：app/material/platform/service extend；映射到 Core Service 或可选能力 Provider。
- `base/src/main/java/com/tinyengine/it/dynamic/**`：动态模型 Controller/DAO/DTO/Service/SQL Provider；映射到 Core dynamic 和 MVC/WebFlux API。
- `base/src/main/java/com/tinyengine/it/login/**`：登录配置、SSO、密码策略、JWT/SM2/SM3 工具、LoginController/Service；抽象为 Core 用户体系 SPI 和可选 Boot 适配，不在 Core 固化具体 JWT/SSO 实现。
- `base/src/main/java/com/tinyengine/it/rag/**`、`gateway/ai/**`、`config/AiChatConfig.java`、`OpenAIConfig.java`、`all-MiniLM-L6-v2/**`：RAG、向量存储、AI Chat、模型资产；基础请求/响应和 Provider 契约进 Core，具体 OpenAI/向量库/ONNX 模型加载作为可选适配或文档，不强制 starter 依赖。
- `app/src/main/java/**`、`app/src/main/resources/application*.yml`、`logback.xml`、`checkstyle.xml`、`webapp/**`：独立应用启动、过滤器、OpenAPI、调度清理、环境配置和传统 webapp；映射到 sample、Boot 可覆盖 Bean 和运维文档，不作为通用 starter 强绑定。
- `Dockerfile`、`docker-compose.yml`、`example-docker.yml`、`docker-deploy-data/**`、`documents/**`、`settings/**`：部署、MySQL 初始化、组件/AI 文档和代码规范；映射到 docs 或 sample 验证资料，不能默默丢弃。

第一阶段覆盖设计器核心闭环：

- 应用：列表、详情、创建、更新、删除、Schema 获取。
- 页面：列表、详情、创建、更新、删除、历史与发布数据。
- 区块：列表、详情、创建、更新、删除、分组、构建/发布状态。
- 物料和组件：物料、组件库、组件、业务分类。
- 模型和数据源：模型 CRUD、数据源 CRUD、动态模型访问。
- 模板、国际化和资源：模板、语言、词条、资源与资源分组。
- 用户上下文：复用 `TinyEngineUserContext`，允许宿主提供自定义实现。

Controller 只负责协议映射、参数校验和响应包装，业务逻辑调用现有 Service。已有 Service 不足以实现的生产接口，应先扩展 Core 契约并添加单元测试，不在 Controller 中复制 mock 数据逻辑。

平台认证、外部素材 CDN、AI 服务、RAG/向量存储、本地模型、代码生成和发布等需要外部系统或大体积资产的能力使用明确的可选适配接口。宿主未提供适配器时返回稳定的“不支持/未配置”错误，不伪造成功。任何延后项必须在覆盖矩阵中标注“延后原因、目标适配点、用户可见行为和后续任务”，不能从清单中删除。

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
- `webflux.scheduler.thread-count`：阻塞 Core 调用线程数，提供保守默认值。
- `webflux.scheduler.queue-capacity`：阻塞任务最大排队数，提供有界默认值。
- `webflux.scheduler.timeout`：单次阻塞调用超时，默认使用明确的有限时长。
- `security.enabled`：用户体系强制开关，默认 `true`；关闭时只适用于受信开发环境。
- `resources.enabled`：SQL/资源装配开关，保持现有语义。

路径在绑定后统一规范化，拒绝空白、缺少前导 `/`、尾部重复 `/` 或包含路径穿越片段的非法值。Controller 映射与静态资源映射使用同一规范化结果。

Magic-API 入口默认打开同源 `/tiny-engine/`，也允许通过插件属性 `path` 覆盖同源路径，或通过 `url` 指向独立部署的前端。

## 错误处理

- 参数错误返回 HTTP 400 和稳定错误码。
- 缺少、无效或过期凭据返回 HTTP 401；权限不足返回 HTTP 403。
- 安全模式启用但缺少必须的认证/权限 SPI 时应用启动失败。
- 资源不存在返回 HTTP 404。
- 未配置的外部能力返回 HTTP 501 或项目统一的不支持错误。
- 数据库冲突和约束错误转换为受控业务错误，不泄漏 SQL、凭证或文件路径。
- 不存在内置前端构建错误；Maven 构建失败只反映 Java/Maven 或 Magic-API 插件自身构建问题。
- 后端 API 404 不由 TinyEngine starter 的 SPA 回退吞掉，因为 starter 不注册 SPA 回退。

## 上游同步

不提供 TinyEngine 前端源码同步脚本。上游 `/Users/xiongxiao/Workspace/Idea/codes/TinyEngine` 只作为外部前端工程，不参与 starter 构建。

## 测试与验收

### 前端边界

- 确认 starter 模块不存在 `src/console`、`designer-demo`、`mockServer`、`pnpm-lock.yaml` 或前端 `dist`。
- 确认 starter Maven 构建不执行 Node/pnpm，也不读取外部 TinyEngine 前端绝对路径。

### Maven 与打包

- 构建 starter，检查 JAR 不包含 `META-INF/resources/tiny-engine/**`、`designer-demo`、`mockServer` 或前端源码。
- 检查 starter 依赖树，确认未引入 Servlet/WebFlux starter、Node/Koa 运行时或无关 provider SDK。
- 构建 `fw-plus-magicapi-plugin-tiny-engine`，检查 JAR 包含对应 `magic-editor/plugins` 前端产物。
- 检查 Magic-API 插件没有重复打包 TinyEngine console 源码或产物。

### Spring

- 自动配置测试覆盖默认关闭、显式启用、Web 关闭、自定义 Bean 覆盖、非法路径，以及 Servlet/Reactive 配置互斥。
- MockMvc 覆盖后端 API 路由、错误处理和无内置静态资源边界。
- `ReactiveWebApplicationContextRunner` 与 `WebTestClient` 覆盖 WebFlux Controller 路由、错误处理和无 Servlet 污染。
- WebFlux 测试验证阻塞 Service 不在 Reactor Netty 事件循环执行，并覆盖 Scheduler 队列饱和、超时和自定义 Bean 覆盖。
- 用户 SPI 契约测试覆盖登录、当前用户、退出、租户查询/切换、注册/找回密码不支持和权限拒绝。
- MVC/WebFlux 用户接口契约测试使用同一测试 Provider，确认 token、用户和租户响应一致。
- WebFlux 并发测试确认两个 token 的主体与租户不会在线程切换后串用。
- 安全模式测试覆盖缺 Provider 启动失败、业务 Bean 覆盖和显式关闭后才允许 system 用户。
- 纯 Reactive classpath 回归测试确认不创建 Servlet Controller、Servlet Filter 或 MVC ResourceHandler。
- API 契约测试覆盖每个核心领域至少一个读写流程及错误响应。
- 后端覆盖测试或审计脚本覆盖 `tiny-engine-backend-java` 的 Java 包、Controller 路由、Service、Mapper XML、SQL、配置、部署和文档/模型资产清单；每一项在覆盖矩阵中都有迁入、适配或显式延后记录。
- TinyEngine 后端配置覆盖默认路径、自定义 context-path 和禁用状态。

### Magic-API 插件

- 前端契约测试确认插件只返回一个 TinyEngine `statusBarItems` 入口，不返回 `resource`、`toolbars`，也不注册 Magic-Script 模块。
- 验证入口 URL 默认从当前宿主地址生成，也支持 `pluginData.attributes.path` 和 `pluginData.attributes.url`。

### 端到端

- 分别以 Servlet 和 Reactive WebApplicationType 启动 TinyEngine sample，并启用数据库测试配置。
- 完成创建应用、创建页面、保存页面、重新加载并读取保存结果的最小闭环。
- 在带 `server.servlet.context-path` 的配置下重复核心 API 验证。

## 实施顺序

1. 移除 starter 内已同步的 TinyEngine 前端源码、构建脚本、资源打包和前端 classpath 测试。
2. 拆分 Servlet/Reactive 自动配置，完成两套 Controller 和 WebFlux 阻塞调度器。
3. 在 Core 定义用户、认证、租户和权限 SPI，在 Boot 实现 MVC/WebFlux 用户接口与请求主体传播。
4. 建立 `tiny-engine-backend-java` 完整覆盖矩阵，先消项 Core 中已有/缺失的实体、DTO、Mapper、SQL、Service、dynamic、login、rag、app、deploy、documents 和模型资产。
5. 按后端覆盖矩阵实现 MVC/WebFlux Controller 适配和缺失 Core 契约。
6. 新增 `fw-plus-magicapi-plugin-tiny-engine`、`statusBarItems` 入口，并支持外部前端 URL。
7. 扩展 Servlet/WebFlux sample 配置、测试用户 Provider 与数据库测试数据，并记录部署、SQL、外部能力与延后项。
8. 运行用户 SPI、后端覆盖矩阵、Magic-API 插件、Maven、MockMvc 和 WebTestClient 验证。

## 完成标准

只有同时满足以下条件才算完成：starter 在不引用外部 TinyEngine 前端路径、不执行 Node/pnpm 的情况下可重复构建；JAR 不包含 TinyEngine 前端源码、`designer-demo`、`mockServer` 或 `META-INF/resources/tiny-engine/**`；`tiny-engine-backend-java` 的 `base`、`app`、Mapper XML、SQL、dynamic、login、rag、deploy、documents 和模型资产均在覆盖矩阵中被迁入、适配或显式延后；同一 starter 在 Servlet 与纯 Reactive 应用中均可启动；两种 Web 栈的用户接口和核心 API 可访问；安全模式不回退 system 用户；WebFlux 不在事件循环执行阻塞 Core 调用且不串用请求主体；核心应用/页面保存闭环通过；Magic-API 插件能从控制台正确打开同源默认路径或外部 TinyEngine 前端 URL；API、用户 SPI、后端覆盖矩阵、插件入口和 context-path 测试通过；现有无关脏文件未被修改。
