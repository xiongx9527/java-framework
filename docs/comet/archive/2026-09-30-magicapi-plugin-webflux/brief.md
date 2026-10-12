# 目标

fw-plus-ai 内 langchain4j 与 mcp 两个 magicapi 插件的 web 端点在 WebFlux 应用（classpath 同时有 `spring-boot-starter-web` 与 `spring-boot-starter-webflux`，以 `spring.main.web-application-type=reactive` 运行）下可用，严格参照 Boot 层既有 `magicapi/webflux` 实现风格；同时修复 mongo 插件配置前缀拼写笔误，并把 swagger 插件重构成与其它插件一致的写法。

# 范围

1. **AI 层 reactive 装配链**（`fw-plus-ai-boot-autoconfigure` 的 `com.xiongx9527.fw.plus.ai.autoconfigure.magicapi`）：
   - `MagicAPIBaseAutoConfiguration` 去掉 `@ConditionalOnWebApplication(SERVLET)`，改为环境无关入口；`@AutoConfigureAfter` 追加 Boot 层 `MagicAPIBaseAutoConfiguration`（保证 reactive 下 Boot 引擎的 `MagicResourceService` bean 定义先注册）；`@Import` 追加新的 WebFlux 条件桥。
   - 新建 `magicapi/webflux/MagicAPIWebFluxConditionalConfiguration`（字符串 `@ConditionalOnClass(name=WebFluxConfigurer)` 桥，对齐 Boot 同名模式）→ `MagicAPIWebFluxPluginAutoConfiguration`（`@ConditionalOnWebApplication(REACTIVE)` + `@ConditionalOnBean(MagicAPIWebFluxMapping.class)`，注册 Registrar bean）。
   - 新建 `magicapi/webflux/MagicAPIWebFluxPluginControllerRegistrar`（`SmartInitializingSingleton`，对齐 Boot `MagicAPIWebFluxControllerRegistrar` 风格）：按功能 bean 存在性门控，注册 langchain4j 4 个控制器（datasource test / content-retriever test / content-injector execute / system-message execute）与 mcp 2 个控制器（server 管理 / diagnostics）。
2. **mongo 插件拼写修复**（Boot 层，2 处）：`MagicAPIMongoConfiguration` 的 `@ConditionalOnProperty` 前缀 `magic-api.plugin.mogno` → `magic-api.plugin.mongo`；对应测试属性同步修正。
3. **swagger 插件重构统一**（Boot 层）：合并 servlet/webflux 两个配置类为一个栈无关 `MagicSwaggerConfiguration`（`@Primary SwaggerUiConfigProperties/Parameters` 定制 bean 双栈生效）；新增统一注解控制器 `MagicSwaggerController`（GET 文档端点，文档生成懒初始化复用 `SwaggerEndpointSupport`/`SwaggerProvider`/`MagicApiRegistryView`）；servlet 走 `controllerRegister()` 编程注册（路径含 `${}` 占位符，fork servlet `Mapping` 不解析占位符，须解析后编程注册），webflux 走 `MagicAPIWebFluxControllerRegistrar` 的 swagger 分支；删除 `webflux/MagicSwaggerWebFluxConfiguration`、`webflux/MagicSwaggerWebFluxController` 及 spring.factories 对应条目；contextPath 统一为「优先 `magic-api.swagger.context-path` 配置，servlet 下缺省取 `ServletContext`（可选注入）」。
4. **测试**：AI 层新增 reactive 装配与控制器注册测试（`ReactiveWebApplicationContextRunner` + `WebTestClient`，对齐 Boot `MagicAPIWebFluxControllerTest` 模式）；SERVLET 回归测试；mongo 前缀测试修正；swagger 重构相关测试同步。
5. **文档**：`fw-plus-ai/README.md`、`README.zh-CN.md` 的 langchain4j 与 mcp 条目补「web 端点支持 Servlet/WebFlux 双栈」说明。

# 非目标

- 不改 Boot 层既有 reactive 引擎机制（`magicapi/webflux/` 9 个类）与 magic-api fork jar（fork 无 webflux 模块，引擎层直接复用 Boot 自建实现）。
- 不动 mcp 协议传输层（`McpRoutingRegistrar` / `McpWebfluxWebBinding` 已支持双栈）。
- WebFlux 下 magic-editor 工作台 WebSocket 在线调试不可用（Boot 层既有边界，保持）。
- druid 插件 servlet-only：既有设计，不改。
- WebFlux 下 Boot 层其它插件控制器（已由 Boot Registrar 支持）不在本次范围。

# 验收示例

- **Scenario A1**：reactive 应用（双依赖 + `web-application-type=reactive`）启动就绪后，langchain4j 插件配置生效——`Langchain4jModelFactoryRegistry`、`MagicDynamicLangchain4jChatModelDataSource` 等 bean 存在。
- **Scenario A2**：reactive 应用下 langchain4j 4 个 web 端点注册进响应式 `RequestMappingHandlerMapping`：`POST /langchain4j/{type}/datasource/test`、`POST /langchain4j/content-retriever/datasource/test`、`POST /langchain4j/content-injector/execute`、`POST /langchain4j/system-message/execute`；其中 B 类端点（content-injector / system-message 的 `MagicHttpServletRequest` 参数）由 Boot `WebFluxMagicAPIArgumentResolver` 适配，无需改动控制器。
- **Scenario A3**：reactive 应用下 mcp 插件功能 bean（`MagicDynamicMcpServer`、`MagicApiMcpProperties` 等）装配，`/mcp/server/*` 与 `/mcp/diagnostics/*` 管理端点注册并可访问；mcp 协议传输端点行为不受影响。
- **Scenario A4**：SERVLET 应用回归——AI 层原 servlet 装配链行为不变（`MagicApiPluginAutoConfiguration`、`Langchain4jModuleConfiguration` 导入链、各插件 `controllerRegister()` 注册路径），langchain4j 与 mcp 端点照常可用。
- **Scenario A5**：mongo 插件配置前缀为 `magic-api.plugin.mongo`（`enabled=false` 可关闭、缺省开启）；测试中不再出现旧键 `mogno`。
- **Scenario A6**：swagger 重构后单配置类栈无关装配：servlet 与 reactive 应用下文档端点均注册在 `magic-api.swagger.location`（缺省 `/v2/api-docs/magic-api/swagger2.json`，不带 magic-api.web 前缀），返回 Swagger 2.0 JSON；swagger-ui 分组 URL 两栈均可生成（contextPath：优先配置项，servlet 缺省取 `ServletContext`）。
- **Scenario A7**：全量编译与测试通过——AI 层 `./mvnw -o test`（fw-plus-ai-boot-autoconfigure）、Boot 层相关测试（mongo 修复回归、swagger 相关）、sample 插件清单测试 `MagicApiAllPlugins*`。

# 约束与不变量

- 严格参照 Boot 层 `magicapi/webflux` 风格：字符串条件桥防类加载失败、`SmartInitializingSingleton` Registrar、`BeanProvider` 门控、`WebApplicationContextRunner`/`ReactiveWebApplicationContextRunner` 测试模式。
- 不引入 Java 9+ API；源文件 UTF-8；Tab 缩进与周边一致。
- mongo 配置键变更属破坏性变更（旧键 `mogno` 失效），提交信息注明。
- swagger servlet 注册时机：`controllerRegister()` 内注册的 handler 必须懒初始化文档依赖（registry 经容器延迟解析），避免与 `MagicConfiguration`/registry 构成循环依赖。
- AI 层新增类不引用 servlet 专属 API（`javax/jakarta.servlet`）；`ServletContext` 仅以 `ObjectProvider` 可选形式出现在 swagger 栈无关配置类中。

# 决策

- D1：执行方式 Comet Native（用户显式调用 `/comet-native` 接管）；工作区隔离 `current`（工作区干净、无并行 change）。需求来源为本会话用户逐项确认的方案（plan 文件 cosmic-pulse-einstein-hMPddu-v.md）。
- D2：范围 = langchain4j + mcp reactive 支持、mongo 拼写修复、swagger 重构统一（用户逐项确认）。
- D3：引擎层复用 Boot reactive 实现，不改 Boot 层既有机制与 magic-api fork（用户确认「只改 fw-plus 侧」「参照 boot 的风格」）。
- D4：mcp magicapi 插件层纳入 reactive（用户确认「一并支持」）；协议传输层已支持不动（用户确认「mcp 应该是支持的」）。
- D5：swagger 重构统一为单配置类 + 统一控制器（用户确认「重构统一 swagger」）；受 springdoc 双栈分裂约束，仅统一注册入口与控制器，文档端点路径仍为 springdoc 标准位置。
- D6：mongo 键 `mogno` → `mongo`（用户确认修复）。
- D7：B 类端点零改动依赖 Boot `WebFluxMagicAPIArgumentResolver` 适配（既有机制）。

# 待解决问题

（无——全部决定已确认，无 `[blocking]` 项）

# 验证预期

- `cd fw-plus-ai && ./mvnw -o install -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am -DskipTests` 编译通过。
- `cd fw-plus-ai && ./mvnw -o test -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure` 全量通过（含新增 reactive 测试）。
- `cd fw-plus-boot && ./mvnw -o test -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='MagicAPIServletIsolationAutoConfigurationTest'` 及 swagger 相关测试通过。
- `./fw-plus-core/mvnw -f fw-plus-sample/pom.xml -o test -Dtest='MagicApiAllPlugins*'` 通过。
- Verifier 独立核对全部验收项（A1–A7），检查记录对应当前实现版本。
