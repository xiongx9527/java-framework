# 目标

解决上一个 change（swagger-plugin-webflux，已归档合并）遗留的三项加固，全部由独立 Verifier 在归档时记录为"可选加固"：

1. **H1 `/doc.html` 端到端断言**：webflux 应用经真实 HTTP 访问 knife4j 文档 UI 入口 `/doc.html`，替代此前仅"类路径资源存在"的弱断言。
2. **H2 文档内容反映已注册接口**：断言 swaggerJson 输出的 `paths` 包含已注册的 magic-api 接口路径、且不带 `/magic-api/web` base 前缀（spec「文档切面」场景此前未覆盖）。
3. **H3 修正 `@ConditionalOnMissingBean` 怪癖**：`magicSwaggerUiConfigProperties`（servlet 与 webflux 两条支线）上的 `@ConditionalOnMissingBean(SwaggerUiConfigParameters.class)` 检查的是**另一类型**的 bean（从 servlet 支线照搬），语义错误且行为依赖 bean 注册顺序；修正为确定性行为并补测试。

# 范围

- `MagicSwaggerWebFluxTest`：新增 `/doc.html` 的 WebTestClient 断言（reactive runner + 默认资源链路，期望非 404）。
- 新增栈无关单元测试（如 `SwaggerEndpointSupportTest`）：以 mock 的 `MagicApiRegistryView` 提供固定 `ApiInfo` 列表，断言 `SwaggerProvider.swaggerJson()` 输出包含该接口 path、`basePath` 符合预期、且无 `/magic-api/web` 前缀——直接覆盖 spec「paths 包含当前 magic-api 已注册接口」与「双栈内容一致」的核心机制（两条支线共享同一 `SwaggerProvider`/`SwaggerEndpointSupport`，共享层断言对双栈同时成立）。
- `MagicSwaggerConfiguration` 与 `MagicSwaggerWebFluxConfiguration`：移除 `magicSwaggerUiConfigProperties` 方法上错误类型的 `@ConditionalOnMissingBean(SwaggerUiConfigParameters.class)`（该方法以 `SwaggerUiConfigProperties` 为参数，原条件类型与之矛盾且依赖注册顺序）。修正后两个 UI 配置 bean 在两条支线均确定性创建；`@Primary` 语义保持。
- 为 H3 补行为测试：配置类装配后 `magicSwaggerUiConfigProperties`/`magicSwaggerUiConfigParameters` 均存在，且 knife4j UI 参数含 magic-api 分组 URL。

# 非目标

- 不改动 swagger 双栈主结构（@ConditionalOnWebApplication 互斥、W3 接线、S2 视图、U2 依赖）。
- 不动 magic-api 引擎、其他插件（mcp 断链等）、ws/mcp 既有行为。
- 不引入新的外部依赖；不改 `magic-api.swagger.location` 默认值等既有配置语义。

# 验收示例

- A1（H1）：reactive 应用启动后 `GET /doc.html` 返回 200（或框架定义的 3xx 重定向到 UI），WebTestClient 断言，非类路径资源断言。
- A2（H2）：单测以固定 `ApiInfo`（如 path `/demo/hello`、method GET）经 `MagicApiRegistryView` 供给，断言 `swaggerJson().getPaths()` 含 `prefix + groupPath + /demo/hello` 形态的键、`basePath` 为 contextPath、无 `/magic-api/web` 前缀。
- A3（H3）：两条支线的配置类在对应 runner 下均产生 `magicSwaggerUiConfigProperties` 与 `magicSwaggerUiConfigParameters` bean；`getConfigParameters()` 的 `urls` 含 `magic-api.swagger.name` 分组且指向 `magic-api.swagger.location`；servlet 侧既有契约不回归（端点路径/内容/时机不变）。

# 约束与不变量

- Spring Boot 2.7.18 / Java 8 / javax；构建依赖 aliyun 镜像；根聚合器构建需 `-nsu`（防远程旧 0.1.1-SNAPSHOT 覆盖本地 BOM）。
- servlet 栈端点行为（路径、内容、注册时机）零变化；本 change 唯一有意改变的 servlet 行为是 UI 配置 bean 由"顺序依赖"变为"确定性创建"（该 bean 仅向 knife4j UI 注入分组 URL，不改端点行为），需在 Shape 确认时明示。
- 双栈行为对称：H3 的修正必须同时作用于两条支线。
- 术语遵循 CONTEXT.md（宿主栈/栈支线/双栈一致/文档切面/控制台切面）。

# 决策

- D1 H2 采用共享层单测（mock registry 视图）而非容器内注册 ApiInfo：两条支线共享 `SwaggerProvider`/`SwaggerEndpointSupport`，共享层断言即双栈断言，避免为造 ApiInfo 引入容器级脆弱性。
- D2 H3 修正方式为移除错误类型的条件注解（不改方法签名、不加新条件类）：方法以 `SwaggerUiConfigProperties` 为入参，任何以其缺失为前提的条件都自相矛盾。
- D3 H1 优先用 reactive runner + Boot 默认资源链路断言 `/doc.html`；若默认资源链路不服务 `META-INF/resources/doc.html`，再按 Verifier 建议补 springdoc webflux UI 自动配置后断言。

# 待解决问题

（当前无 [blocking] 项。）

# 验证预期

- `MagicSwaggerWebFluxTest` 全量（含新增 /doc.html 断言）+ 新增 `SwaggerEndpointSupportTest` + 两条支线 UI 配置行为测试。
- magicapi 全包回归（`com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.**.*Test`）+ swagger 插件模块测试。
- ws 族/sample 在 worktree 旧基线的既有失败不在本 change 范围（主工作区对照基线判定）。
