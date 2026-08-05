# Outcome

为 Magic-API 提供一个独立、可选装的动态 Java 实体插件。用户通过新的 Magic-API Resource 保存完整 Java 源码，插件在运行时执行 Lombok 注解处理、编译和加载，使普通 Java 实体无需重启宿主即可在 Magic-Script 中创建、访问和热更新，并可将真实 `Class<?>` 交给 MyBatis-Plus 或 MyBatis-Flex 后续能力使用。

# Scope

- 新增独立的 Magic-API 插件模块，并接入现有插件聚合、BOM 和自动配置体系。
- 新增保存完整 Java 源码的 `java-entity` Resource 类型，接入资源读取、保存、更新、删除和启动恢复生命周期。
- 以 Java 8 为目标，在内存中对全部已启用实体源码执行代际编译和类加载。
- 支持普通 Java 实体，不要求声明 ORM 注解或继承框架基类。
- 支持 Lombok 注解处理，以及可选的 MyBatis-Plus、MyBatis-Flex、Fastjson 1.x、Fastjson2 和 Jackson 注解引用。
- 提供 `entity` Magic-Script 模块，通过 `entity.User` 获取当前实体描述器，创建实例并取得真实 Java Class。
- 支持同名实体源码热更新；候选代际完整成功后原子切换，失败时保留最后一次成功版本。
- 提供与编译、并发切换、ClassLoader 生命周期和失败回滚风险匹配的自动化测试。

# Non-goals

- 首期不实现新增、修改、删除、查询、分页或 Join 执行器。
- 首期不实现 MP-Join 适配。
- 不动态生成或注册 Mapper，不修改现有静态 Mapper。
- 不修改、fork 或升级 MyBatis-Plus、MyBatis-Flex 和 MP-Join。
- 不主动向 MyBatis-Plus 或 MyBatis-Flex 注册动态实体元数据，避免首期引入 ORM 静态缓存生命周期问题。
- 不替换现有 `MybatisFlexModule`、Magic-API 原生 `db` 模块或 DB 浏览器插件。
- 不从数据库元数据反向生成 Java 源码；源码由用户在 Resource 中实现。
- 不导出 Java 源码给宿主项目离线编译，不把动态实体注册为静态业务代码。
- 不并入或修改 `tinyflow-magicapi-knowledge-base` 变更。
- 不引入 Java 9 及以上 API，也不升级项目维护的 Magic-API 2.2.2。

# Acceptance examples

- 用户可新建 `java-entity` Resource，并保存一个兼容 Java 8 的普通实体类完整源码。
- 源码可使用 Lombok `@Data`、`@Builder`、`@NoArgsConstructor` 等注解，动态编译后生成的方法真实存在。
- 源码可选择使用 MyBatis-Plus、MyBatis-Flex 或两套 ORM 注解，也可以完全不使用 ORM 注解。
- 源码可使用 Fastjson 1.x、Fastjson2 和 Jackson 注解，动态实体经过对应序列化器处理时注解真实生效。
- Magic-Script 可通过 `entity.User.create(map)` 创建实体，通过 `entity.User.javaClass()` 取得当前 `Class<?>`。
- 多个实体源码可互相引用；保存任一源码后，全部实体在新的 ClassLoader 代际中重新编译并原子切换。
- 语法错误、Lombok 处理错误、重复别名或类加载失败时返回带行列信息的诊断，不破坏最后一次成功版本。
- 更新源码可增加或删除字段、方法和注解；新调用使用新版本，已开始的调用可继续使用旧版本。
- 禁用插件后，现有 Magic-API、MyBatis-Plus、MyBatis-Flex 和静态 Mapper 保持原有行为。

# Constraints and invariants

- 兼容 JDK 8、Spring Boot 2.7.18 和项目维护的 Magic-API 2.2.2。
- 运行环境必须提供 JDK `JavaCompiler`；仅有 JRE 时插件应给出明确不可用诊断，而不是导致应用启动失败。
- 遵循现有 Magic-API Resource、动态模块、集中自动配置和插件聚合边界。
- 每次激活以全部已启用实体为一个不可分割代际；不得暴露部分新、部分旧的实体集合。
- 动态类加载必须有确定的全限定类名、别名、版本和生命周期策略，当前注册表不得保留已退休代际的强引用。
- Java 源码具备执行任意 JVM 代码的能力，只允许受信任的 Magic-API 管理员编辑和激活，不承诺提供不可信代码沙箱。
- 编译诊断不得泄露数据源密码、完整连接串、敏感环境变量或无关宿主文件内容。
- 保留主工作区现有未提交改动；本变更只在独立 worktree 和分支中推进。

# Decisions

- 作为独立变更 `magicapi-dynamic-entity-plugin` 开发，使用分支 `codex/magicapi-dynamic-entity-plugin` 和隔离 worktree。
- 插件模块命名为 `fw-plus-magicapi-plugin-java-entity`，Resource 类型标识为 `java-entity`，Magic-Script 模块名为 `entity`。
- Resource 正文保存完整 Java 源码；第一版每个 Resource 只允许一个顶级公开类。
- 采用 JDK `JavaCompiler` 内存编译，不采用 Bean 元数据模拟、Instrumentation 热替换或仅导出 `.java` 文件。
- 全部已启用实体源码按代际一起编译，以支持实体间引用和结构性热更新。
- Lombok 作为插件运行时编译依赖，并在动态编译任务中启用注解处理。
- Fastjson 1.x、Fastjson2 和 Jackson 注解作为插件受支持编译依赖，不引入额外注解处理器。
- 普通实体与 ORM 注解实体同等可发布；ORM 注解不是激活前置条件。
- 首期只发布动态 Class 和实体创建能力，不预热 ORM 元数据，不提供 CRUD、Join 或 Mapper。
- `entity.<简单类名>` 只在别名唯一时可用；全限定类名始终是实体的稳定标识。
- 热更新采用候选 Generation ClassLoader 和原子注册表切换；候选失败时继续使用最后一次成功代际。

# Open questions

无阻塞问题。用户已于 2026-08-05 确认按当前完整规格进入实现。

# Verification expectations

- 单元测试覆盖普通实体、Lombok、MP/Flex 可选注解、Fastjson/Jackson 注解、跨 Resource 引用、重复类名和重复别名。
- 单元测试覆盖新增、修改、删除、并发读取、失败回滚和旧代际引用释放边界。
- 集成测试覆盖 Magic-Script `entity.User`、`create(map)`、`javaClass()` 和源码热更新。
- 验证目录 classpath 与 Spring Boot 可执行 JAR classpath 下的 Lombok 注解处理。
- 运行目标插件、插件聚合、BOM 和自动配置模块的 Maven 校验，确认依赖收敛和 Java 8 编译兼容。
