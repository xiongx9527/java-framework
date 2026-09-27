# 目标

为 `fw-plus-magicapi-plugin-swagger`（当前 servlet-only 的 magic-api 插件）新增 webflux 栈支线，使纯 WebFlux 应用下 swagger 插件的**文档切面**（Swagger 2.0 JSON 端点）与**控制台切面**（控制台页签 + knife4j 文档 UI）真实可用，达到**双栈一致**（共享核心逻辑，仅 HTTP 出入口随栈适配）。servlet 栈行为零变化（回归保持）。

# 范围

- `MagicSwaggerConfiguration`（`fw-plus-boot-autoconfigure` 的 `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger` 包）双栈化：servlet 支线保持现状；新增 REACTIVE 装配路径（在 `AutoConfiguration.imports` 登记与 SERVLET 条件互斥的条目）。
- `SwaggerProvider` 与 servlet 专属 registry 类型（`RequestMagicDynamicRegistry`）解耦：抽 fw 侧 registry 视图接口（D4），双栈共享。
- webflux 支线文档端点注册：参照 `MagicAPIWebFluxMapping` / `MagicAPIWebFluxControllerRegistrar` 既有模板，补 `registerPlugin("swagger")` 分支；contextPath 不再经 `ServletContext` 获取。
- 引入 `knife4j-openapi3-webflux-spring-boot-starter`（版本对齐 BOM 的 `knife4j-openapi3.version=4.5.0`，经 aliyun 镜像解析）。
- 测试：webflux 栈契约测试（端点注册、Swagger 2.0 JSON 内容、控制台 `/plugins` 含 swagger）+ servlet 回归。
- 移除 `CLAUDE.md` / `AGENTS.md` 中已失效的内网 Nexus 地址（`172.17.1.6:28081`），与 U2 依赖源决策配套。

范围细则以 Q1（接线深度）的答案为准。

# 非目标

- 引擎级全插件 webflux 接线（W1）——不在本 change 范围。
- mcp 式「表面双栈」（W2：真实 REACTIVE 运行时不注册）不作为交付目标。
- 不动上游 magic-api jar；不改动其他插件既有行为（mcp 插件在 webflux 下的断链问题另行立项）。
- 不引入 knife4j webflux 变体之外的新外部依赖；不做单应用双栈同时注册。

# 验收示例

（正式验收项以 Spec 中 `Scenario:` 场景为准，由 Runtime 生成编号）

- servlet 栈回归：既有 servlet 契约测试全部通过；文档端点路径与内容与改造前一致（默认 `/v2/api-docs/magic-api/swagger2.json`，Swagger 2.0 JSON，不加 base 前缀）。
- webflux 栈：纯 REACTIVE 应用（仅 `spring-boot-starter-webflux`）下 swagger 文档端点被注册并返回等价的 Swagger 2.0 JSON。
- webflux 控制台：`/plugins` 清单包含 swagger 条目，knife4j webflux 文档 UI（`/doc.html`）可访问。
- 互斥性：classpath 同时存在 servlet 与 reactive 时，按 Spring Boot 推导的应用类型只激活一条支线，不双注册、不双端点。

# 约束与不变量

- Spring Boot 2.7.18 / Java 8 / javax 命名空间；构建顺序 Core → Boot，产物经本地 Maven 传递。
- 单应用单栈互斥（`@ConditionalOnWebApplication`）；「双栈一致」= 共享核心逻辑，仅 HTTP 出入口随栈适配。
- 外部依赖经 aliyun 镜像解析（当前 VPN 环境，内网 Nexus 不可达）；knife4j 版本严格 4.5.0，与 MVC 支线对称。
- 术语遵循既定词汇：宿主栈、栈支线、双栈一致、文档切面、控制台切面。
- servlet 栈零行为变化是本 change 的硬约束。

# 决策

- D1 意图：规划改造 swagger 插件的 webflux 栈支线（grilling Q1=C）。
- D2 栈语义：单应用单栈，`@ConditionalOnWebApplication` 二选一；不加显式属性开关（grilling Q5=B1）。
- D3 文档 UI：U2——引入 `knife4j-openapi3-webflux-spring-boot-starter:4.5.0`（grilling Q6；aliyun/Central 实测可解析，与 MVC 支线同版本同厂商）。
- D4 registry 解耦：S2——抽 fw 侧 registry 视图接口，servlet 路径行为不变（grilling Q9）。
- D5 载体：Comet Native change（用户在 OpenSpec 与 Comet 之间选定 Native）。
- D6 内网 Nexus 地址从 CLAUDE.md/AGENTS.md 移除（主工作区已执行；本 change 同步 worktree 副本，保证合并后无分支残留）。
- D7 不新增 webflux 样例模块：改动位于既有插件内部，不新增 starter；契约测试承担演示与防回归作用（沿用 `MagicAPIWebFluxControllerTest` 的装配断言模式）。
- D8 根目录 `CONTEXT.md` 术语表纳入本 change 落地：保证「宿主栈/栈支线/双栈一致/文档切面/控制台切面」随 change 进入版本库；与主工作区未提交版本内容一致，合并无冲突。
- D9 接线深度：W3——swagger 独立接线。`AutoConfiguration.imports` 登记 REACTIVE 互斥条目，插件 bean 经 `ObjectProvider<List<MagicPluginConfiguration>>` 自然进入 webflux registrar，补 `registerPlugin("swagger")` 分支；不动引擎聚合层。已知边界：其他插件在 webflux 下仍缺席，spec 明示。

# 待解决问题

（当前无 [blocking] 项。）

# 验证预期

- `./mvnw compile`（worktree 根聚合）+ 新增 webflux 契约测试 + 既有 servlet 测试（含 `MagicApiSwaggerFrontendClasspathTest`、`MagicApiAllPluginsApplicationContextTest`、`MagicAPIWebFluxControllerTest` 等）。
- 依赖解析验证：`knife4j-openapi3-webflux-spring-boot-starter:4.5.0` 经 aliyun 可下载，且与 BOM 版本对齐。
- knife4j webflux 变体为非 optional（与 MVC 变体对称）：servlet 应用类路径新增惰性 `spring-webflux` jar，运行时无行为变化（springdoc webflux 自动配置均带栈条件，servlet 应用不激活）——该取舍已确认接受并写入 spec 已知边界。
