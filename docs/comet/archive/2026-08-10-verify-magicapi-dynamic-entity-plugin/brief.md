# Outcome

系统性检查当前 `fw-plus-magicapi-plugin-java-entity` 实现是否满足已确认的 Magic-API 动态 Java 实体目标规格。发现属于当前规格的实现缺陷或测试缺口时完成修复，并通过独立验收确认插件的资源生命周期、动态编译、代际热更新、脚本访问、兼容性和隔离边界可靠。

# Scope

- 审查并验证独立 Maven 模块、Boot BOM、插件聚合、自动配置和控制台插件接线。
- 审查 `java-entity` Resource 的读取、保存、更新、删除、启停和启动恢复生命周期。
- 审查 Java 8 内存编译、Lombok 注解处理、常用 ORM/JSON 注解 classpath、诊断与资源限制。
- 审查全量代际编译、ClassLoader 生命周期、原子切换、失败回滚和并发读取语义。
- 审查 `entity` Magic-Script 模块、实体描述器、实例创建和属性赋值边界。
- 审查现有自动化测试；对确认的问题补充最小必要实现与回归测试。
- 核对插件 README 和项目级动态实体文档与实际行为的一致性。

# Non-goals

- 不新增动态 Mapper、CRUD、分页、Join 或 MP-Join 执行能力。
- 不修改 MyBatis-Plus、MyBatis-Flex 或 MP-Join 的版本和内部实现。
- 不主动预热或注册 ORM 静态实体元数据。
- 不从数据库元数据生成 Java 源码，不导出源码供宿主离线编译。
- 不承诺为不可信 Java 源码提供 JVM 沙箱。
- 不修改 TinyEngine、Tinyflow 或当前工作区中的其他 active change。
- 不为了审查而进行与验收项无关的重构。

# Acceptance examples

- A1：`fw-plus-magicapi-plugin-java-entity` 被插件聚合器和 Boot BOM 正确纳入，只有引入 marker 且配置启用时才激活自动配置；未引入或禁用时不改变现有 Magic-API 行为。
- A2：`java-entity` Resource 可按 Magic-API 约定读取、保存、更新、删除、启停和启动恢复，持久化正文为 UTF-8 完整 Java 源码。
- A3：每个 Resource 只接受一个顶级 `public` 类，Resource 名称必须匹配简单类名；重复全限定类名被拒绝，冲突简单名只取消歧义别名。
- A4：动态编译以 Java 8 为目标，全部启用实体共同编译；支持实体间引用，并在开发目录、普通 JAR 和 Spring Boot 可执行 JAR classpath 下解析所需依赖。
- A5：Lombok 常用注解会真实生成字节码；MyBatis-Plus、MyBatis-Flex、Fastjson 1.x、Fastjson2 和 Jackson 注解可编译，JSON 字段重命名与忽略行为由真实序列化器验证。
- A6：候选代际只有在编译、注解处理、类加载和注册表校验全部成功后才原子切换；失败或超时保留最后成功代际，冷启动失败不阻止 Spring/Magic-API 启动。
- A7：全限定类名是稳定注册键，简单名仅在唯一时可用；读取路径看不到半构建 Map，当前注册表不保留已退休代际的强引用。
- A8：`entity` 模块可通过简单名或全限定类名取得描述器；`name()`、`version()`、`javaClass()`、`create()` 和 `create(map)` 行为符合规格，旧描述器可完成已开始调用。
- A9：`create(map)` 优先调用 setter，受控回退到实例字段，并拒绝静态、final、未知属性和不可转换值；缺少可访问无参构造器时返回明确错误。
- A10：新增、修改、删除和启停任一 Resource 都按完整实体集合重建；语法、Lombok、依赖或类加载错误返回带 Resource/行列的受限诊断且不破坏旧代际。
- A11：单源码大小、总代码量、编译超时、诊断数量/长度和并发编译受到配置限制；仅有 JRE 时给出 compiler 不可用诊断而不是导致应用启动失败。
- A12：动态实体可选使用 ORM 注解，但插件本身不新增 Mapper、不预热 ORM 元数据、不改变静态 Mapper 注册表；插件禁用时 MyBatis-Plus/Flex 保持原行为。
- A13：控制台注册“Java实体”资源入口，名称、类型、脚本资源和 Java 编辑流程与后端 `java-entity` Resource 契约一致。
- A14：目标插件、Boot 自动配置、聚合/BOM和控制台相关检查可重复通过，并使用 JDK 8 验证编译兼容性；发现的规格缺陷均有针对性回归测试。
- A15：README 与项目级文档准确说明依赖、配置、Resource 格式、描述器 API、热更新、限制和安全边界，不把外部 ORM 执行器误写成插件本体能力。

# Constraints and invariants

- 兼容 JDK 8、Spring Boot 2.7.18 和项目维护的 Magic-API 2.2.2，不引入 Java 9+ API。
- 保持 `org.ssssssss` Magic-API 内部仓库依赖版本不变。
- 动态源码具备宿主 JVM 代码执行能力，只允许受信任的 Magic-API 管理员编辑和激活。
- 编译诊断不得泄露数据源密码、完整连接串、环境变量或无关宿主文件内容。
- 候选代际失败不得暴露部分新、部分旧的实体集合。
- 保留 `dev` 主工作区和其他 worktree 中的现有改动；仅在本 change 的隔离 worktree 中推进。

# Decisions

- 以 `docs/comet/legacy-changes/magicapi-dynamic-entity-plugin` 中已确认的 brief 和完整规格为需求基线。
- 新 change 命名为 `verify-magicapi-dynamic-entity-plugin`，在分支 `comet/verify-magicapi-dynamic-entity-plugin` 的独立 worktree 中执行。
- 本次是“审查并修复”而非重新设计：只修复可由规格和测试证明的问题。
- 检查包括插件控制台接线；MyBatis-Plus/Flex 动态执行器仅验证不破坏本插件边界，不纳入新增功能范围。

# Open questions

无阻塞问题。用户已确认按 A1-A15 审查并修复动态 Java 实体插件。

# Verification expectations

- 使用 JDK 8 运行 Java Entity 插件模块及 Boot 自动配置中的相关 Maven 测试。
- 运行插件聚合与 BOM 的 Maven 模型/构建校验，确认依赖和模块接线。
- 运行控制台 Node 测试并核对构建资源名称。
- 对原子切换、失败回滚、并发读取、ClassLoader 释放、编译限制和诊断脱敏执行针对性测试或静态验证。
- 由新的只读 Verifier 逐项验收 A1-A15，并独立检查实现、测试结果和文档。
