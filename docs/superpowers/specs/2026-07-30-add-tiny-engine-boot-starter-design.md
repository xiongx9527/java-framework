---
comet_change: add-tiny-engine-boot-starter
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-30-add-tiny-engine-boot-starter
status: final
---

# TinyEngine Boot Starter 深度设计

## 1. 目标与边界

本 change 在 `fw-plus-boot` 中交付 TinyEngine Spring Boot starter，使 Spring Boot 2.7.18 宿主应用通过引入 starter 和设置 `fw.plus.tiny-engine.enabled=true` 获得 TinyEngine Web、Mapper、Mapper XML 与 SQL 资源发现能力。Boot 层不复制 TinyEngine 领域逻辑，所有模型、Service、Mapper、动态 SQL 和 SQL 脚本以 `integrate-tiny-engine-core` 已迁入的 `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core` 为事实源。

当前 core 事实源为：

- Maven artifact：`com.xiongx9527.fw.plus:fw-plus-tiny-engine-core:${revision}`
- Java 包前缀：`com.xiongx9527.fw.plus.tinyengine`
- Mapper 包：`com.xiongx9527.fw.plus.tinyengine.mapper`
- Mapper XML：`classpath*:mapper/tinyengine/**/*.xml`
- SQL manifest：`META-INF/tiny-engine/sql/manifest.properties`
- SQL 资源目录：`META-INF/tiny-engine/sql/h2/`、`META-INF/tiny-engine/sql/mysql/`、`META-INF/tiny-engine/sql/postgresql/`

本 change 不实现 TinyEngine AI Chat/RAG、向量检索或 LangChain4j 专用扩展。用户要求使用最新 `abstract-ai-model-and-embedding` 结果，因此本 starter 不得新增旧 `fw-plus-ai-langchain4j-model`、`fw-plus-ai-langchain4j-store-embeding` 或旧 starter 坐标，也不得直接引入 AI provider SDK。后续 TinyEngine AI/RAG 只能通过 `fw-plus-ai-model` 与 `fw-plus-ai-store-embedding` 中立 registry/factory 对接，由当前 LangChain4j adapter starter 或未来 Java 17 adapter 提供实现。

## 2. 模块与依赖

新增 starter 位于 `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter`，并接入 `fw-plus-boot-starters` 聚合 POM。starter 依赖现有 `fw-plus-boot-starter`、`fw-plus-boot-autoconfigure` 的自动配置能力、Spring Web/MyBatis-Plus 所需依赖和 `fw-plus-tiny-engine-core`。如 Boot BOM 需要暴露 starter 坐标，则在 `fw-plus-boot-dependencies` 中以 `${revision}` 托管，不引入独立版本。

自动配置实现放在 `fw-plus-boot-autoconfigure` 的 `com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine` 包下，并同时写入：

- `META-INF/spring.factories`
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

这样保持 Spring Boot 2.7.18 兼容，也跟现有 Boot 自动配置模块风格一致。

## 3. 属性模型

属性前缀统一为 `fw.plus.tiny-engine`。建议结构：

- `enabled`：总开关，默认 `false`，关闭时不注册 TinyEngine Controller、Service 装配、Mapper 扫描和资源 Bean。
- `web.enabled`：Web Controller 开关，默认跟随总开关。
- `web.base-path`：可选 API 前缀，默认使用 TinyEngine 适配 Controller 的内置路径；宿主可显式配置统一前缀以规避冲突。
- `mapper.enabled`：Mapper 扫描与 XML locations 开关，默认跟随总开关。
- `mapper.base-package`：默认 `com.xiongx9527.fw.plus.tinyengine.mapper`。
- `mapper.mapper-locations`：默认 `classpath*:mapper/tinyengine/**/*.xml`。
- `resources.enabled`：资源发现开关，默认跟随总开关。
- `sql.enabled`：SQL 资源能力开关，默认跟随总开关。
- `sql.auto-initialize`：SQL 自动初始化开关，默认 `false`。
- `sql.database`：显式数据库类型，支持 `h2`、`mysql`、`postgresql`；未设置时只暴露 manifest/resource 列表，不猜测修改宿主数据库。

属性类保持 Java 8 写法，使用普通 POJO、嵌套静态类和显式 getter/setter 或项目现有 Lombok 风格。

## 4. 自动配置设计

顶层 `TinyEngineAutoConfiguration` 使用条件保护：

- `@ConditionalOnClass` 检查 TinyEngine core 关键类、Spring MVC、MyBatis-Plus 关键类。
- `@ConditionalOnWebApplication(type = SERVLET)` 保护 Web Controller。
- `@ConditionalOnProperty(prefix = "fw.plus.tiny-engine", name = "enabled", havingValue = "true")` 作为总开关。
- `@ConditionalOnMissingBean` 允许宿主覆盖默认 Controller、用户上下文、hook、资源发现或 SQL manifest Bean。

core 中已有 Service 实现带 `@Service`，Boot starter 的职责是让这些 Bean 在宿主上下文中可被发现。实现时优先采用精确 `@Import` 或受控 `@ComponentScan` 只纳入 `com.xiongx9527.fw.plus.tinyengine.service.impl`、`support.context`、`support.hook` 等必要包，避免扫描过宽。Mapper 使用 `MapperScannerConfigurer` 或仓库现有 MyBatis 自动配置扩展注册 `com.xiongx9527.fw.plus.tinyengine.mapper`。

Mapper XML locations 不能覆盖宿主已有配置，只能追加 TinyEngine 默认 pattern。若现有 MyBatis-Plus 配置没有可安全追加点，则提供一个明确的 TinyEngine mapper location customizer，并在测试中验证既有 locations 不丢失。

SQL 资源通过 core 中 `TinyEngineSqlScriptManifest` 或 Boot 包装 Bean 暴露为 `Resource` 列表。默认不执行 SQL。只有 `sql.auto-initialize=true` 且有明确 `sql.database` 时才允许初始化；首版可只提供发现能力和测试证据，避免对宿主库产生副作用。

## 5. Web 适配

Controller 位于 Boot autoconfigure 或其受控 web 子包，作为薄适配层委托 core Service。它可以参考 TinyEngine 原 Controller 的路由语义与 DTO，但不得复制 service 内部逻辑、动态 SQL 逻辑或 SQL 脚本。异常处理优先返回 core `Result`/异常语义，避免在 Boot 层引入新的业务错误模型。

Controller Bean 必须可被宿主覆盖。若宿主需要安全、租户或用户上下文定制，应通过已有 `TinyEngineUserContext`、`TinyEngineSaveHook` 等 core 扩展点注入，而不是在 starter 中写死认证实现。默认用户上下文可使用 core 的系统上下文，样例中要明确这是测试/基础运行路径。

## 6. 样例与验证

在 `fw-plus-sample/fw-plus-boot-samples` 新增 TinyEngine starter sample 或测试模块，并接入样例聚合 POM。样例验证聚焦 starter 装配，不承担完整 TinyEngine 业务回归。

必测场景：

- 启用：`fw.plus.tiny-engine.enabled=true` 时应用上下文启动，Controller Bean、core Service、Mapper 扫描、Mapper XML 和 SQL manifest/resource 均可发现。
- 禁用：`fw.plus.tiny-engine.enabled=false` 时 TinyEngine Controller、Service、Mapper 扫描与 SQL/XML 资源 Bean 不注册。
- 缺 classpath：使用 `FilteredClassLoader` 或等价方式模拟缺少 core/Web/MyBatis 类时不注册依赖 Bean。
- 覆盖：宿主提供同职责 Bean 时自动配置退让。
- SQL 安全：默认不执行 SQL；需要初始化的验证必须使用 H2 或隔离数据源。
- AI 边界：扫描 TinyEngine Boot starter、样例和文档，确认无旧 LangChain4j 专用 model/store 坐标、无旧 starter 坐标、无直接 AI provider SDK。

构建命令按风险分层执行：

```bash
./fw-plus-core/mvnw -f fw-plus-boot/pom.xml validate
./fw-plus-core/mvnw -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure,fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter -am test
./fw-plus-core/mvnw -f fw-plus-sample/pom.xml validate
```

最终检查还需要确认未修改 Magic-API 2.2.2 版本，未引入 Spring Boot 3/JDK 17 专属 API。

## 7. 风险与回滚

主要风险是 core 路径变化、MyBatis XML 追加失败、Controller 路由与宿主冲突、SQL 初始化误触发，以及后续 AI 能力重新耦合 LangChain4j。对应处理是：build 开始先核对 core artifact/包/资源；Mapper locations 做追加式测试；Controller 提供开关和 Bean 覆盖；SQL 默认只发现不执行；AI 相关只记录中立抽象边界，不在本 Boot starter 中落旧坐标。

回滚路径简单：宿主不引入 TinyEngine starter，或设置 `fw.plus.tiny-engine.enabled=false`。已发布 artifact 不需要影响未引入 starter 的应用。
