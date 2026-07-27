## Context

`fw-plus-boot` 当前以 `fw-plus-boot-autoconfigure` 承载自动配置，并通过 `META-INF/spring.factories` 与 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册顶层 AutoConfiguration。各 starter 位于 `fw-plus-boot-project/fw-plus-boot-starters`，通常依赖 `fw-plus-boot-starter` 与对应 core 模块，再由 starter 聚合 POM 统一纳入构建。

TinyEngine 参考项目当前包含 `base` 与 `app` 两层，其中 `base` 提供低代码领域对象、Service、Controller、Mapper XML，`app` 提供启动应用与 MySQL/H2/PostgreSQL SQL 资源。参考项目基线为 Spring Boot 3.1.9 与 Java 17，且包含 AI Chat/RAG 相关类；本 change 不直接迁移参考项目原始运行方式，而是依赖 `integrate-tiny-engine-core` 先把可复用领域能力整理为 fw-plus core 侧的 Java 8/Spring Boot 2.7.18 兼容模块，再由 Boot 层提供 Controller 适配与自动配置。

## Goals / Non-Goals

**Goals:**

- 在 `fw-plus-boot-autoconfigure` 中新增 TinyEngine 自动配置，并使用 `fw.plus.tiny-engine` 属性绑定控制启用行为。
- 新增 TinyEngine starter，封装宿主应用需要的 autoconfigure、web、MyBatis-Plus 与 core TinyEngine 依赖。
- 通过自动配置接入 core change 提供的 TinyEngine Service、Mapper、Mapper XML 与 SQL 初始化资源，并在 Boot 层提供 REST Controller 适配。
- 提供启用与禁用两类基础样例验证，覆盖 REST API 暴露、Mapper/XML 加载、SQL 资源可发现，以及总开关关闭时不注册 TinyEngine 控制器与服务。

**Non-Goals:**

- 不重复实现 TinyEngine core 领域模型、Service、Mapper、动态 SQL 或 SQL 脚本内容。
- 不实现 TinyEngine AI Chat/RAG、向量检索或 LangChain4j 扩展。
- 不迁移 `fw-plus-boot` 到 Spring Boot 3，不提升 JDK 到 17，不引入 Java 9+ API。
- 不修改 Magic-API 2.2.2 依赖版本或 Magic-API 插件行为。

## Decisions

1. **自动配置沿用 Boot 层现有注册方式。**  
   TinyEngine 顶层配置类放在 `com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine` 包下，并同时注册到 `spring.factories` 和 `AutoConfiguration.imports`。这样兼容当前 Spring Boot 2.7.18 的加载方式，也与仓库现有自动配置入口保持一致。备选方案是在 starter 中显式 `@Import`，但会要求宿主额外配置，starter 体验较弱。

2. **以 `fw.plus.tiny-engine` 作为统一配置前缀。**  
   设计一个总开关 `enabled`，并拆分 `web.enabled`、`mapper.enabled`、`resources.enabled`、`sql.enabled` 等子能力开关。总开关关闭时，所有 TinyEngine Controller、Service、Mapper 扫描和资源加载 Bean 都不得注册；子开关只控制对应能力。备选方案复用参考项目原属性，但其语义偏独立应用，不适合 fw-plus starter。

3. **Boot change 只装配 core change 产物并承载 Web 适配。**  
   Service、Mapper 接口、Mapper XML 与 SQL 资源的权威来源是 `integrate-tiny-engine-core`。Controller 属于 Boot 层 Web 适配，本 change 可参考 `tiny-engine-backend-java/base` 中的 Controller 路由语义实现薄适配，但不得复制或分叉 core 领域逻辑。这样可以避免 core 与 boot 两套实现漂移。

4. **Mapper 与 XML/SQL 资源采用 starter 可发现路径。**  
   自动配置需要明确扫描 core 中 TinyEngine Mapper 包，并将 XML 资源位置追加到 MyBatis 配置可识别的 mapper locations 中。SQL 初始化资源按数据库类型保留在 starter/core classpath 的稳定目录下，由属性选择或暴露为 Resource 列表供宿主初始化流程使用。备选方案要求宿主手写 `@MapperScan` 与 `mybatis-plus.mapper-locations`，但不满足“引入 starter 后即可配置启用”的目标。

5. **Controller 适配受属性和 Classpath 条件约束。**  
   Boot 层 TinyEngine REST Controller 适配只在 Servlet Web 应用、TinyEngine core classpath 存在且 `fw.plus.tiny-engine.enabled=true`、`fw.plus.tiny-engine.web.enabled=true` 时注册。若宿主已有同名或同职责 Bean，自动配置应通过条件注解允许宿主覆盖。这样可以降低对已有应用 Web 层的侵入。

6. **样例验证放在 `fw-plus-sample/fw-plus-boot-samples`。**  
   新增 TinyEngine starter sample 或扩展 boot samples 聚合，使用 Spring Boot 2.7.18、Java 8 兼容写法验证 starter 接入。样例只验证 starter 装配和基础 REST/资源发现，不承担完整 TinyEngine 业务回归。

## Risks / Trade-offs

- [Risk] `integrate-tiny-engine-core` 的包名、模块名或资源路径最终变化，导致 Boot 自动配置路径失配。 → Mitigation：实现前以 core change 的最终 POM、Service/Mapper 包和资源目录为准，并在 Boot 层单独确认 Controller 适配包与路由，任务中保留对这些路径的核对步骤。
- [Risk] MyBatis-Plus 版本与参考项目不同，Mapper XML 或 TypeHandler 需要兼容调整。 → Mitigation：兼容调整应优先落在 core change；Boot change 只验证 XML 被加载并能在样例中通过基础查询/上下文启动。
- [Risk] 自动注册 Controller 可能与宿主应用安全、路径或异常处理冲突。 → Mitigation：提供 `web.enabled` 子开关，并让关键 Bean 支持宿主覆盖；必要时提供 API base path 属性。
- [Risk] SQL 初始化策略如果自动执行，可能对宿主数据库造成副作用。 → Mitigation：默认只保证 SQL 资源随 starter 可发现；自动初始化必须受显式属性控制，并在样例中使用 H2 或隔离库验证。

## Migration Plan

1. 确认 `integrate-tiny-engine-core` 已提供 fw-plus core TinyEngine 模块、Java 8 兼容源码、Mapper XML 和 SQL 资源。
2. 在 Boot BOM 与 starter 聚合中加入 TinyEngine starter 与 core TinyEngine 依赖声明。
3. 加入 TinyEngine 自动配置、属性类和自动配置元数据，验证默认禁用/启用语义。
4. 增加 Boot sample 并通过 Maven validate/test 验证启用和禁用场景。
5. 如需回滚，移除 starter 依赖或关闭 `fw.plus.tiny-engine.enabled`；已发布模块可通过不引入 starter 避免影响宿主应用。

## Open Questions

- `integrate-tiny-engine-core` 最终采用的模块 artifactId、Service/Mapper 包名和资源路径是什么？
- TinyEngine REST API 是否需要统一 base path，例如 `/tiny-engine` 或保留参考项目原路径？
- SQL 资源默认只暴露还是支持显式自动初始化，初始化顺序是否由 core 提供清单？
