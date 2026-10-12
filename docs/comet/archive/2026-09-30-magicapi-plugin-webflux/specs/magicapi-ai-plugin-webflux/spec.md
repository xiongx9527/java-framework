# AI 层 magicapi 插件 WebFlux 支持

AI 层（`fw-plus-ai-boot-autoconfigure`）的 magicapi 插件（langchain4j、mcp）在 Servlet 与 WebFlux 两种 web 栈下均可装配其功能 bean 并注册 web 端点。WebFlux 支持严格参照 Boot 层 `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.webflux` 既有实现风格；引擎层（reactive `MagicResourceService`、`MagicConfiguration`、`MagicAPIWebFluxMapping`、动态 API 通道、`WebFluxMagicAPIArgumentResolver`）复用 Boot 层 reactive 引擎，AI 层只负责插件配置装配与插件控制器注册。

## AI 层装配链

`com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.MagicAPIBaseAutoConfiguration` 为环境无关入口：

- 条件：classpath 存在 `org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration`、`fw.plus.magicapi.enabled` 未关闭（缺省开启）。不再限定 `@ConditionalOnWebApplication(SERVLET)`。
- 排序：`@AutoConfigureAfter` 同时指向 fork 官方 `MagicAPIAutoConfiguration` 与 Boot 层 `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.MagicAPIBaseAutoConfiguration`——reactive 下 Boot 引擎的 `MagicResourceService` bean 定义先注册，AI 插件配置的 `@ConditionalOnBean(MagicResourceService.class)` 才能通过。
- 导入：`MagicApiPluginAutoConfiguration`（mcp + langchain4j 插件配置）与 `Langchain4jModuleConfiguration` 环境无关，双栈复用；另导入 WebFlux 条件桥。

WebFlux 条件桥 `com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.webflux.MagicAPIWebFluxConditionalConfiguration`：

- 自身不引用任何 webflux 类型（可被安全加载），以字符串条件 `@ConditionalOnClass(name = "org.springframework.web.reactive.config.WebFluxConfigurer")` 防止 classpath 无 spring-webflux 时加载失败。
- 仅导入 `MagicAPIWebFluxPluginAutoConfiguration`。

插件注册配置 `MagicAPIWebFluxPluginAutoConfiguration`：

- 条件：`@ConditionalOnWebApplication(type = REACTIVE)` 且 Boot 层 `MagicAPIWebFluxMapping` bean 已存在（reactive 引擎就绪）。
- 提供 `MagicAPIWebFluxPluginControllerRegistrar` bean（注入 `MagicAPIWebFluxMapping`、`MagicConfiguration`、`ApplicationContext`）。
- 不重复导入插件配置——`MagicApiPluginAutoConfiguration` 已由 Base 环境无关导入。

控制器注册器 `MagicAPIWebFluxPluginControllerRegistrar`：

- 实现 `SmartInitializingSingleton`，在 `afterSingletonsInstantiated()` 中按插件功能 bean 存在性门控（`ApplicationContext.getBeanProvider(...).getIfAvailable()`）注册：

| 控制器 | 门控 bean | 说明 |
| --- | --- | --- |
| `Langchain4jDatasourceController` | `Langchain4jModelFactoryRegistry` | 构造依赖 `ObjectProvider<MagicDynamicLangchain4jEmbeddingModelDataSource>`（可空） |
| `Langchain4jContentRetrieverController` | `ContentRetrieverProviderRegistry` | — |
| `Langchain4jContentInjectorController` | `Langchain4jContentInjectorModule` | — |
| `Langchain4jSystemMessageController` | systemmessage 配置的功能 bean | — |
| `MagicMcpServerController` | `MagicApiMcpProperties` | — |
| `MagicMcpDiagnosticController` | `MagicApiMcpProperties` | — |

- 注册走 Boot 层 `MagicAPIWebFluxMapping.registerController`（带 magic-api.web base 前缀），端点路径与 servlet 栈一致。
- content-injector / system-message 端点的 `MagicHttpServletRequest` 参数由 Boot 层已注册的 `WebFluxMagicAPIArgumentResolver`（`ServerWebExchange` → magic-api 请求抽象）适配，控制器代码零改动。

Servlet 栈行为不变：Base 去掉 SERVLET 限制后，`MagicApiPluginAutoConfiguration` 导入链与各插件 `controllerRegister()` 注册路径维持原状。

Scenario: reactive 应用下 langchain4j 插件配置生效
Given classpath 同时有 spring-boot-starter-web 与 spring-boot-starter-webflux 的应用以 web-application-type=reactive 启动
When 应用启动就绪
Then `Langchain4jModelFactoryRegistry`、`MagicDynamicLangchain4jChatModelDataSource` 等 langchain4j 插件 bean 存在于容器

Scenario: reactive 应用下 langchain4j web 端点注册
Given reactive 应用启动就绪且 langchain4j 插件配置生效
When 检查响应式 `RequestMappingHandlerMapping` 的 handler methods
Then 包含 `POST /langchain4j/{type}/datasource/test`、`POST /langchain4j/content-retriever/datasource/test`、`POST /langchain4j/content-injector/execute`、`POST /langchain4j/system-message/execute`

Scenario: reactive 应用下 mcp 插件装配与端点注册
Given reactive 应用启动就绪
When 检查容器与响应式 `RequestMappingHandlerMapping`
Then `MagicDynamicMcpServer`、`MagicApiMcpProperties` 等 mcp 插件 bean 存在；`/mcp/server/*` 与 `/mcp/diagnostics/*` 管理端点已注册并可访问；mcp 协议传输端点（SSE/streamable-http）行为不受影响

Scenario: SERVLET 应用行为回归
Given servlet 应用（仅 spring-boot-starter-web 或双依赖默认 servlet 运行）
When 应用启动
Then AI 层装配链与 langchain4j、mcp 端点行为与改动前一致

## mongo 插件配置前缀修正

Boot 层 mongo 插件的启用开关前缀由拼写笔误 `magic-api.plugin.mogno` 修正为 `magic-api.plugin.mongo`：

- `MagicAPIMongoConfiguration` 的 `@ConditionalOnProperty(prefix = "magic-api.plugin.mongo", name = "enabled", havingValue = "true", matchIfMissing = true)`。
- 测试中的属性同步修正；全仓不再出现旧键 `mogno`。
- 破坏性变更：外部若曾用旧键 `magic-api.plugin.mogno.enabled=false` 关闭插件，升级后失效，需改用新键。

Scenario: mongo 插件开关配置
Given 应用配置 `magic-api.plugin.mongo.enabled=false`
When 应用启动
Then mongo 插件不装配；未配置时缺省开启

## swagger 插件写法统一

Boot 层 swagger 插件合并为与其它插件一致的形态：

- 单一栈无关配置类 `MagicSwaggerConfiguration`（不再分 servlet/webflux 两个配置类）：`plugin()`、`@Primary SwaggerUiConfigProperties` / `SwaggerUiConfigParameters` 定制（swagger-ui 分组含 magic-api 文档 URL）双栈生效；`SwaggerUiConfigProperties` 等 springdoc 核心类型为栈无关类。
- 统一注解控制器 `MagicSwaggerController`：GET 文档端点，返回 Swagger 2.0 JSON；文档生成复用 `SwaggerEndpointSupport` / `SwaggerProvider` / `MagicApiRegistryView`，provider 懒初始化（registry 经容器延迟解析，避免与 `MagicConfiguration` 循环依赖）。
- servlet 栈：`controllerRegister()` 返回的注册器中编程注册（路径 `magic-api.swagger.location` 含 `${}` 占位符，fork servlet `Mapping` 不解析占位符，须先解析再 `mapping.register`），端点不带 magic-api.web 前缀。
- webflux 栈：`MagicAPIWebFluxControllerRegistrar` 的 swagger 分支构造统一控制器并以空 base 注册。
- contextPath 解析统一：优先配置项 `magic-api.swagger.context-path`；servlet 下缺省取 `ServletContext`（以 `ObjectProvider` 可选注入，reactive 下不存在时为空）。
- 删除 `plugin/swagger/webflux/MagicSwaggerWebFluxConfiguration`、`plugin/swagger/webflux/MagicSwaggerWebFluxController` 及 spring.factories 中对应条目。

Scenario: swagger 文档端点双栈注册
Given servlet 或 reactive 应用启动（springdoc 在 classpath）
When 请求 `magic-api.swagger.location`（缺省 `/v2/api-docs/magic-api/swagger2.json`）
Then 返回 Swagger 2.0 JSON 文档，内容实时反映当前 magic API 清单；端点不带 magic-api.web 前缀

Scenario: swagger-ui 分组配置
Given servlet 或 reactive 应用启动
When springdoc swagger-ui 解析文档分组
Then 分组列表包含 magic-api 文档 URL（contextPath 优先取 `magic-api.swagger.context-path` 配置，servlet 下缺省取 `ServletContext` context path）

## 已知边界

- WebFlux 下 magic-editor 工作台 WebSocket 在线调试不可用（Boot 层既有边界）。
- druid 插件保持 servlet-only（既有设计）。
