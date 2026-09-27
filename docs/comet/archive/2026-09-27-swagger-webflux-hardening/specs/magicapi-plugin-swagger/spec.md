# magicapi-plugin-swagger 规格

`fw-plus-magicapi-plugin-swagger` 为 magic-api 提供接口文档能力，含两个对外切面：**文档切面**（Swagger 2.0 JSON 端点）与**控制台切面**（magic-api 控制台 swagger 页签 + knife4j 文档 UI）。插件在两种宿主栈（servlet / reactive）下均为可用状态，达到双栈一致；单个应用只激活一条栈支线。

## 术语

宿主栈、栈支线、双栈一致、文档切面、控制台切面的定义见根目录 `CONTEXT.md`。

## 装配与开关

- **servlet 支线**：`MagicSwaggerConfiguration` 经 `MagicAPIWebMvcAutoConfiguration` → `MagicApiPluginAutoConfiguration` 的 @Import 链加载，条件为 servlet 应用。
- **webflux 支线**：同一能力在 `AutoConfiguration.imports` 中登记独立条目，条件为 reactive 应用；两条支线条件互斥，单应用只实例化一条。
- **插件清单**：webflux 支线的 `MagicPluginConfiguration`（`plugin()` 方法产出的插件描述）由 Spring 收集进 `MagicAPIWebFluxControllerRegistrar` 的 `ObjectProvider<List<MagicPluginConfiguration>>`，`registerPlugin("swagger")` 据此注册 swagger 的 HTTP 出入口。
- **开关**：`magic-api.plugin.swagger.enabled`（默认 `true`）两条支线一致；关闭时两条支线均不注册任何端点与页签。
- **依赖对称**：servlet 支线依赖 `knife4j-openapi3-spring-boot-starter`；webflux 支线依赖 `knife4j-openapi3-webflux-spring-boot-starter`；两者版本一致（4.5.0），由 BOM 统一管理。
- **核心共享**：`SwaggerProvider` 不依赖 servlet 专属 registry 类型，经 fw 侧 registry 视图接口（`MagicApiRegistryView`）获取当前注册的 magic-api 接口映射；servlet 与 webflux 两条支线复用同一 `SwaggerEndpointSupport` / `SwaggerProvider` 实现。
- **UI 配置确定性**：两条支线的 knife4j UI 配置 bean（`magicSwaggerUiConfigProperties` / `magicSwaggerUiConfigParameters`）的创建不依赖 bean 注册顺序或其他类型 bean 的在场情况（不得使用与入参类型矛盾的条件注解）。

## 文档切面

Scenario: webflux 应用中文档端点可用
Given 仅引入 `spring-boot-starter-webflux` 的应用同时引入 fw-plus magic-api starter 与 swagger 插件
When 应用启动就绪
Then 文档端点（路径取 `magic-api.swagger.location`，默认 `/v2/api-docs/magic-api/swagger2.json`）返回 200 与 application/json，内容为 Swagger 2.0 文档（含 info、paths、definitions），paths 包含当前 magic-api 已注册接口，且路径不带 `/magic-api/web` base 前缀

Scenario: servlet 应用中文档端点行为不变
Given 仅引入 servlet Web 栈的应用同时引入 fw-plus magic-api starter 与 swagger 插件
When 应用启动就绪
Then 文档端点路径、返回内容、注册时机与改造前一致（改造前行为即回归基线）

Scenario: 两条支线生成的文档内容一致
Given 同一组 magic-api 接口资源分别部署于 servlet 应用与 reactive 应用
When 分别请求各自的文档端点
Then 除 basePath 等宿主相关字段外，两组文档的 paths 与 definitions 一致

Scenario: 插件关闭时无端点
Given `magic-api.plugin.swagger.enabled=false`
When 应用启动（任一宿主栈）
Then 不注册 swagger 文档端点

## 控制台切面

Scenario: webflux 控制台拉取到 swagger 插件
Given reactive 应用启动就绪
When 请求控制台插件清单端点（`{magic-api.web}/plugins`）
Then 列表中包含 swagger 条目，其 `javascriptFilename` 指向本插件 console 构建产物（`magic-swagger.<version>.iife.js`）

Scenario: webflux 下 knife4j 文档 UI 可访问
Given reactive 应用启动就绪
When 请求 knife4j 文档 UI 入口（`/doc.html`）
Then 返回 200，页面可加载并可查看上述文档端点的内容

Scenario: knife4j UI 分组确定性注入
Given 应用启动就绪（任一宿主栈）
When 读取 knife4j UI 配置参数（`SwaggerUiConfigParameters.getConfigParameters()`）
Then 其中 urls 含以 `magic-api.swagger.name` 命名的分组且指向 `magic-api.swagger.location`；该结果不依赖 bean 注册顺序，也不依赖其他类型 bean 是否在场（两条支线行为一致）

Scenario: 控制台页签在 servlet 下不变
Given servlet 应用启动就绪
When 控制台加载插件清单并渲染状态栏
Then swagger 页签的出现方式、顺序与点击行为与改造前一致

## 栈支线互斥

Scenario: 混合 classpath 的 servlet 应用只激活 servlet 支线
Given 应用 classpath 同时存在 spring-webmvc 与 spring-webflux，且按 SERVLET 类型启动
When 应用启动
Then 仅 servlet 支线配置类实例化并注册端点；webflux 支线配置类不实例化；文档端点在应用中只有一个实例

Scenario: 纯 reactive 应用只激活 webflux 支线
Given 应用 classpath 仅有 spring-webflux（无 spring-webmvc）
When 应用启动
Then 仅 webflux 支线配置类实例化并注册端点；servlet 支线的 `ServletContext` 相关装配不发生

## 已知边界

- 除 swagger 外的其他 magic-api 插件在 reactive 应用下仍不注册（webflux 支线的插件清单默认只有 swagger）；这不是本规格要改变的行为。
- 本规格不改变 magic-api 引擎、其他插件与 ws/mcp 等既有能力的行为。
- knife4j webflux 变体为非 optional（与 MVC 变体对称，保证 webflux 应用可传递获得该依赖）；其代价是 servlet 应用类路径新增一个惰性的 `spring-webflux` jar，运行时无行为变化（springdoc webflux 自动配置均带 `@ConditionalOnWebApplication` 条件，servlet 应用不会激活）。
- webflux 支线的文档端点只响应 GET；servlet 支线以反射注册端点、不限制 HTTP 方法。读取 Swagger JSON 按 GET 使用，两条支线功能等价。
