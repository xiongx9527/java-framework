---
generated_from_state_version: 7
---

# Verification

## Current result

- Result: **Passed**
- Assurance: **skill-coordinated**
- Goal cycle: 1
- Iteration: 1
- Verifier attempt: 1
- Completed: 2026-08-10T16:26:51.376Z
- Summary: 独立只读审查覆盖 A1-A92，全部通过。Runtime 的六项检查均成功，未发现需要追加检查或返回 Build 的缺陷。

## Acceptance

| ID | Result | Source | Criterion | Reason |
| --- | --- | --- | --- | --- |
| A1 | passed | brief.md | A1：`fw-plus-magicapi-plugin-java-entity` 被插件聚合器和 Boot BOM 正确纳入，只有引入 marker 且配置启用时才激活自动配置；未引入或禁用时不改变现有 Magic-API 行为。 | 聚合 POM 与 Boot BOM 均纳入独立模块；marker、enabled 条件、缺失插件及 WebMVC/WebFlux 接线均有实现和测试。 |
| A2 | passed | brief.md | A2：`java-entity` Resource 可按 Magic-API 约定读取、保存、更新、删除、启停和启动恢复，持久化正文为 UTF-8 完整 Java 源码。 | Storage 和 DynamicRegistry 覆盖 UTF-8 Resource 读写、校验、事件增删改、启停及恢复。 |
| A3 | passed | brief.md | A3：每个 Resource 只接受一个顶级 `public` 类，Resource 名称必须匹配简单类名；重复全限定类名被拒绝，冲突简单名只取消歧义别名。 | 语法树分析器及测试覆盖单一顶级 public 类、名称匹配、重复 FQCN 和简单名歧义。 |
| A4 | passed | brief.md | A4：动态编译以 Java 8 为目标，全部启用实体共同编译；支持实体间引用，并在开发目录、普通 JAR 和 Spring Boot 可执行 JAR classpath 下解析所需依赖。 | 编译固定 Java 8 并全量同编；开发目录、普通 JAR、Boot 可执行 JAR及实体互引均有覆盖。 |
| A5 | passed | brief.md | A5：Lombok 常用注解会真实生成字节码；MyBatis-Plus、MyBatis-Flex、Fastjson 1.x、Fastjson2 和 Jackson 注解可编译，JSON 字段重命名与忽略行为由真实序列化器验证。 | 测试验证 Lombok 真实字节码、双 ORM 注解及三种 JSON 序列化器的重命名和忽略行为。 |
| A6 | passed | brief.md | A6：候选代际只有在编译、注解处理、类加载和注册表校验全部成功后才原子切换；失败或超时保留最后成功代际，冷启动失败不阻止 Spring/Magic-API 启动。 | 候选完成编译、加载和校验后才原子切换；失败、超时及冷启动失败均保留旧代际或安全降级。 |
| A7 | passed | brief.md | A7：全限定类名是稳定注册键，简单名仅在唯一时可用；读取路径看不到半构建 Map，当前注册表不保留已退休代际的强引用。 | FQCN 主键、唯一简单名别名、不可变 Map、并发读取及退休 ClassLoader 回收测试通过。 |
| A8 | passed | brief.md | A8：`entity` 模块可通过简单名或全限定类名取得描述器；`name()`、`version()`、`javaClass()`、`create()` 和 `create(map)` 行为符合规格，旧描述器可完成已开始调用。 | entity 模块实时解析当前注册表，描述器 API 和旧描述器续用均有测试覆盖。 |
| A9 | passed | brief.md | A9：`create(map)` 优先调用 setter，受控回退到实例字段，并拒绝静态、final、未知属性和不可转换值；缺少可访问无参构造器时返回明确错误。 | 描述器测试覆盖 setter 优先、字段回退、非法属性和值以及无参构造器错误。 |
| A10 | passed | brief.md | A10：新增、修改、删除和启停任一 Resource 都按完整实体集合重建；语法、Lombok、依赖或类加载错误返回带 Resource/行列的受限诊断且不破坏旧代际。 | Resource 事件按启用源码完整快照重建，诊断映射 Resource/行列且失败不替换旧代际。 |
| A11 | passed | brief.md | A11：单源码大小、总代码量、编译超时、诊断数量/长度和并发编译受到配置限制；仅有 JRE 时给出 compiler 不可用诊断而不是导致应用启动失败。 | 源码大小、总量、超时、诊断及并发均受配置限制，无 JavaCompiler 上下文可继续启动。 |
| A12 | passed | brief.md | A12：动态实体可选使用 ORM 注解，但插件本身不新增 Mapper、不预热 ORM 元数据、不改变静态 Mapper 注册表；插件禁用时 MyBatis-Plus/Flex 保持原行为。 | 本体仅提供动态实体 Class，不生成 Mapper、不预热 ORM 元数据，禁用条件隔离成立。 |
| A13 | passed | brief.md | A13：控制台注册“Java实体”资源入口，名称、类型、脚本资源和 Java 编辑流程与后端 `java-entity` Resource 契约一致。 | 控制台注册 java-entity、Java Monaco 及一致字段，Node 四项测试和 IIFE 构建通过。 |
| A14 | passed | brief.md | A14：目标插件、Boot 自动配置、聚合/BOM和控制台相关检查可重复通过，并使用 JDK 8 验证编译兼容性；发现的规格缺陷均有针对性回归测试。 | Runtime 记录的 JDK 8 测试矩阵、模块、聚合/BOM、控制台及 diff-check 六项检查全部通过。 |
| A15 | passed | brief.md | A15：README 与项目级文档准确说明依赖、配置、Resource 格式、描述器 API、热更新、限制和安全边界，不把外部 ORM 执行器误写成插件本体能力。 | README 与项目文档覆盖依赖、配置、Resource、描述器、热更新、限制、安全及 ORM 缓存边界。 |
| A16 | passed | specs/magicapi-dynamic-java-entity/spec.md | 系统应提供一种可选装的 Magic-API Resource 能力，用于保存、动态编译和加载 Java 8 实体源码。实体主要在 Magic-Script 中使用，可为普通 Java Bean，也可选择声明 Lombok、MyBatis-Plus、MyBatis-Flex、Fastjson 或 Jackson 注解。源码更新成功后无需重启应用即可使用新版本。 | 独立可选插件实现动态保存、Java 8 编译、加载和无重启热更新，并支持约定注解族。 |
| A17 | passed | specs/magicapi-dynamic-java-entity/spec.md | 系统应注册 `java-entity` Resource 类型，其正文为 UTF-8 编码的完整 Java 源文件。 | Storage 固定类型 java-entity、后缀 .java，并保留完整 UTF-8 Java 正文。 |
| A18 | passed | specs/magicapi-dynamic-java-entity/spec.md | Resource 应参与 Magic-API 现有的分组、读取、保存、更新、删除和应用启动恢复流程。 | MagicResourceStorage/MagicDynamicRegistry 接入原生资源服务和文件、分组、清空事件流程。 |
| A19 | passed | specs/magicapi-dynamic-java-entity/spec.md | 每个 Resource 第一版只允许一个顶级公开类，并应解析出唯一全限定类名。 | JavaSourceAnalyzer 只接受一个顶级 public class，并解析唯一 FQCN。 |
| A20 | passed | specs/magicapi-dynamic-java-entity/spec.md | Resource 名称应与顶级公开类简单名一致；重复全限定类名、多个顶级公开类或名称不一致时应拒绝候选版本。重复简单名时只拒绝该简单名别名，两个实体仍可通过全限定类名访问。 | 名称匹配、重复 FQCN、多个公开类和简单名冲突均有直接测试。 |
| A21 | passed | specs/magicapi-dynamic-java-entity/spec.md | 普通 Java Bean 可以直接编译和发布，不要求 ORM 注解、Lombok 注解、接口或框架基类。 | 普通无注解 Bean 编译、发布和 create(map) 集成测试通过。 |
| A22 | passed | specs/magicapi-dynamic-java-entity/spec.md | 源码可选择引用 MyBatis-Plus `@TableName`、`@TableId`、`@TableField` 和 MyBatis-Flex `@Table`、`@Id`、`@Column` 等当前项目版本提供的注解。 | 插件运行依赖包含 MP/Flex 注解，动态类反射验证两套注解生效。 |
| A23 | passed | specs/magicapi-dynamic-java-entity/spec.md | 源码可选择引用 Fastjson 1.x、Fastjson2 和 Jackson 当前项目版本提供的序列化注解。 | Fastjson 1.x、Fastjson2、Jackson 注解依赖和动态编译兼容测试通过。 |
| A24 | passed | specs/magicapi-dynamic-java-entity/spec.md | 第一版不从数据库元数据生成源码，用户直接维护 Resource 中的 Java 源码。 | 插件只接收用户维护的完整源码，不存在数据库元数据生成路径。 |
| A25 | passed | specs/magicapi-dynamic-java-entity/spec.md | 插件应在运行时提供与项目依赖管理一致的 Lombok 编译依赖，不得将 Lombok 仅作为插件构建期 `provided` 依赖。 | 插件 POM 将 Lombok 作为直接默认 compile/runtime 依赖，未声明 provided。 |
| A26 | passed | specs/magicapi-dynamic-java-entity/spec.md | 动态编译任务应启用注解处理，并使 Lombok 同时出现在编译 classpath 和 processor path。 | Lombok 同时进入编译 classpath 与 processor ClassLoader，CompilationTask 显式设置处理器。 |
| A27 | passed | specs/magicapi-dynamic-java-entity/spec.md | 系统应支持 Lombok 当前版本提供的常用注解，包括 `@Data`、`@Getter`、`@Setter`、`@Builder`、`@NoArgsConstructor`、`@AllArgsConstructor` 和 `@EqualsAndHashCode`。 | 运行依赖与处理器支持约定 Lombok 注解，常用生成能力有真实测试。 |
| A28 | passed | specs/magicapi-dynamic-java-entity/spec.md | Lombok 生成的方法、构造器和内部类型应包含在候选代际字节码中，并可被反射和 Magic-Script 正常访问。 | 测试通过反射实际调用 Lombok getter、setter、builder、构造器和内部 builder 类型。 |
| A29 | passed | specs/magicapi-dynamic-java-entity/spec.md | Lombok 处理错误应作为编译失败返回，诊断应包含 Resource、行号、列号和消息摘要。 | Lombok 处理失败走统一编译异常，诊断包含 Resource、行、列、code 和受限摘要。 |
| A30 | passed | specs/magicapi-dynamic-java-entity/spec.md | `create()` 与 `create(map)` 要求实体存在可访问的无参构造器；没有时实体仍可通过 `javaClass()` 获取，但创建调用应返回明确错误。 | javaClass 不依赖构造器，create 对缺失或不可访问无参构造器返回明确错误。 |
| A31 | passed | specs/magicapi-dynamic-java-entity/spec.md | 系统应使用 JDK `JavaCompiler`，以 `-source 8` 和 `-target 8` 编译源码，不得使用 Java 9 及以上 API。 | JavaCompiler 显式使用 source/target 8 和 UTF-8，完整检查在 Zulu JDK 8 下通过。 |
| A32 | passed | specs/magicapi-dynamic-java-entity/spec.md | 系统应使用当前应用可见的受控编译 classpath，并支持开发目录、普通 JAR 和 Spring Boot 可执行 JAR 中所需的编译依赖。 | Resolver 汇集受控应用 classpath 并展开和清理 Boot archive，专项测试通过。 |
| A33 | passed | specs/magicapi-dynamic-java-entity/spec.md | 每次新增、修改或删除 Resource 时，应将全部已启用动态实体源码作为一个候选代际共同编译。 | 新增、更新、删除、启停事件均从 sources 快照筛选全部启用实体后重建。 |
| A34 | passed | specs/magicapi-dynamic-java-entity/spec.md | 候选代际应使用新的 Generation ClassLoader 加载，不得在已经定义同名类的 ClassLoader 中覆盖字节码。 | 每次 compile 创建新的 GenerationClassLoader，并对子代动态字节码 child-first 加载。 |
| A35 | passed | specs/magicapi-dynamic-java-entity/spec.md | 编译、注解处理、类加载和注册表校验全部成功后，系统才可原子替换当前代际。 | 完成分析、编译、类加载及代际构造后才一次发布当前引用。 |
| A36 | passed | specs/magicapi-dynamic-java-entity/spec.md | 任一步骤失败时，系统应关闭候选代际可释放资源并保持最后一次成功代际可用，不得暴露半切换状态。 | 候选失败关闭候选 loader 且 current 不变，原子回滚和超时测试通过。 |
| A37 | passed | specs/magicapi-dynamic-java-entity/spec.md | 冷启动首次编译失败时，应用应继续启动，动态实体模块应返回不可用诊断，不能暴露未完整加载的类。 | 冷启动失败转为 version 0 unavailable 状态，注册不抛出且缺编译器上下文仍启动。 |
| A38 | passed | specs/magicapi-dynamic-java-entity/spec.md | 新调用应从当前注册表解析 Class；切换前已开始的调用可继续使用旧 Class，注册表不得主动破坏正在使用的旧实例。 | 新访问解析 current，旧描述器持有旧 Class 并可继续创建实例。 |
| A39 | passed | specs/magicapi-dynamic-java-entity/spec.md | 当前注册表不得保留已退休代际的强引用；旧代际在不存在外部实例、请求或第三方缓存引用后应可被回收。 | current 只指向新代际且不保留旧代际，活注册表 WeakReference 回收测试通过。 |
| A40 | passed | specs/magicapi-dynamic-java-entity/spec.md | 系统应限制单个源码大小、单次代际源码总量、编译时长、诊断输出长度和并发编译数量。 | 五类限制均有属性和执行路径，不响应中断的任务继续占用 Semaphore 许可。 |
| A41 | passed | specs/magicapi-dynamic-java-entity/spec.md | 动态编译 classpath 应包含项目依赖管理中的 Fastjson 1.x、Fastjson2 和 Jackson 注解包。 | 三种 JSON 依赖直接进入插件运行 classpath，并列入 marker 代码源解析。 |
| A42 | passed | specs/magicapi-dynamic-java-entity/spec.md | Fastjson 1.x 应支持 `com.alibaba.fastjson.annotation.JSONField`、`JSONType` 等运行时注解。 | Fastjson 1.x 完整依赖可见，JSONField 动态编译和真实序列化验证通过。 |
| A43 | passed | specs/magicapi-dynamic-java-entity/spec.md | Fastjson2 应支持 `com.alibaba.fastjson2.annotation.JSONField`、`JSONType` 等运行时注解。 | Fastjson2 完整依赖可见，JSONField 动态编译和真实序列化验证通过。 |
| A44 | passed | specs/magicapi-dynamic-java-entity/spec.md | Jackson 应支持 `com.fasterxml.jackson.annotation.JsonProperty`、`JsonIgnore`、`JsonFormat`、`JsonAlias`、`JsonInclude` 等运行时注解。 | Jackson annotations 依赖可见，约定常用注解已编译并通过序列化或反射验证。 |
| A45 | passed | specs/magicapi-dynamic-java-entity/spec.md | 当 Jackson databind 注解依赖可见时，源码可使用 `JsonSerialize` 和 `JsonDeserialize`；插件不得自行实例化用户声明的自定义序列化器执行不受控校验。 | databind 注解可编译保留，编译器不会实例化用户序列化器。 |
| A46 | passed | specs/magicapi-dynamic-java-entity/spec.md | JSON 注解不需要 Java annotation processor，编译器应保留注解及其属性供对应序列化器运行时读取。 | JSON 注解未配置 processor，运行时反射和真实序列化证明注解属性被保留。 |
| A47 | passed | specs/magicapi-dynamic-java-entity/spec.md | 测试应使用项目当前 Fastjson 1.x、Fastjson2 和 Jackson 序列化器验证字段重命名及忽略行为，而不只验证注解类可加载。 | 测试实际调用 Fastjson 1.x、Fastjson2 和 ObjectMapper 检查字段名与忽略结果。 |
| A48 | passed | specs/magicapi-dynamic-java-entity/spec.md | 注册表应以全限定类名作为稳定主键，记录 Resource 标识、源码摘要、代际版本和当前 `Class<?>`。 | 代际按 FQCN 保存 Class，并记录 Resource id、SHA-256 和 version。 |
| A49 | passed | specs/magicapi-dynamic-java-entity/spec.md | 简单类名只可作为唯一别名；两个包中存在同名类时应拒绝简单名别名，并要求使用全限定类名访问。 | 别名按简单名计数，只注册唯一项；冲突实体仍可按各自 FQCN 解析。 |
| A50 | passed | specs/magicapi-dynamic-java-entity/spec.md | 注册表切换应为单次原子引用替换，读取路径不得观察到可变的半构建 Map。 | 代际 Map 防御复制且不可变，发布使用单次 AtomicReference.set，并发一致性测试通过。 |
| A51 | passed | specs/magicapi-dynamic-java-entity/spec.md | Resource 删除后，新代际不再包含该类；后续新解析应返回实体不存在错误。 | 删除后全量重编新代际，已删除类解析报不存在，最后一个实体删除测试通过。 |
| A52 | passed | specs/magicapi-dynamic-java-entity/spec.md | 注册表不得将内部可变 Map、ClassLoader 或编译器对象暴露给 Magic-Script。 | Magic-Script 只获得描述器公开 API，不暴露内部 Map、ClassLoader 或编译器。 |
| A53 | passed | specs/magicapi-dynamic-java-entity/spec.md | 系统应注册名为 `entity` 的 Magic-Script 动态模块。 | EntityModule 使用 @MagicModule("entity") 注册并作为条件配置 Bean 创建。 |
| A54 | passed | specs/magicapi-dynamic-java-entity/spec.md | `entity.User` 应返回当前代际的实体描述器，而不是长期缓存的 `Class<?>`。 | getDynamicAttribute 每次调用 registry.resolve，不缓存 Class 或描述器。 |
| A55 | passed | specs/magicapi-dynamic-java-entity/spec.md | 描述器应至少提供 `name()`、`version()`、`javaClass()`、`create()` 和 `create(map)`。 | 描述器实现 name、version、javaClass、create 和 create(map)，并携带 Resource 元数据。 |
| A56 | passed | specs/magicapi-dynamic-java-entity/spec.md | `javaClass()` 应返回当前描述器对应的真实 Java Class，使后续 MyBatis-Plus、MyBatis-Flex 或其他 Java API 可以接收该类型。 | javaClass 返回描述器绑定的真实动态 Class，可供注解和 ORM 调用接收。 |
| A57 | passed | specs/magicapi-dynamic-java-entity/spec.md | `create()` 应通过可访问无参构造器创建实例。 | create 通过 getDeclaredConstructor().newInstance 调用可访问无参构造器。 |
| A58 | passed | specs/magicapi-dynamic-java-entity/spec.md | `create(map)` 应先创建实例，再按 Java Bean 属性写入值；优先使用 setter，并对没有 setter 的可写实例字段提供受控回退。 | create(map) 先构造实例，再查公共实例 setter，缺失时受控访问实例字段。 |
| A59 | passed | specs/magicapi-dynamic-java-entity/spec.md | 属性赋值应拒绝静态字段、final 字段、未知属性和不可转换值，并指出实体名和属性名。 | 静态/final/未知属性及不可转换值形成包含实体名和属性名的异常。 |
| A60 | passed | specs/magicapi-dynamic-java-entity/spec.md | 描述器属于取得它时的代际；源码切换后，再次访问 `entity.User` 应返回新描述器，已取得的旧描述器只允许完成当前调用，不应被全局缓存。 | 每次解析新建绑定当前代际的描述器，热更新测试证明新旧描述器分别工作。 |
| A61 | passed | specs/magicapi-dynamic-java-entity/spec.md | 动态编译 classpath 应允许源码引用项目当前版本的 MyBatis-Plus 和 MyBatis-Flex 注解。 | MP annotation 与 Flex core 均为插件运行依赖并进入动态编译 classpath。 |
| A62 | passed | specs/magicapi-dynamic-java-entity/spec.md | 普通实体、仅 MP 注解实体、仅 Flex 注解实体、双 ORM 注解实体和带 JSON 注解的实体均可发布。 | 普通、单 ORM、双 ORM 和 JSON 实体均可发布，编译器不要求 ORM 注解。 |
| A63 | passed | specs/magicapi-dynamic-java-entity/spec.md | 首期不要求存在表名、主键或字段注解，也不比较两套 ORM 映射是否一致。 | 分析和注册过程不检查表名、主键或两套 ORM 映射一致性。 |
| A64 | passed | specs/magicapi-dynamic-java-entity/spec.md | 首期不调用 MyBatis-Plus `TableInfoHelper` 或 MyBatis-Flex `TableInfoFactory` 预热元数据，避免将已退休 ClassLoader 固定在 ORM 静态缓存中。 | 发布路径未引用 TableInfoHelper/TableInfoFactory，外部 ORM 仅在自身调用路径处理元数据。 |
| A65 | passed | specs/magicapi-dynamic-java-entity/spec.md | 首期不生成 Mapper、不注册实体专属 Mapper、不修改静态 Mapper，也不实现 CRUD、分页、Join 或 MP-Join。 | 本体没有 Mapper 生成注册、CRUD、分页或 Join 实现，文档明确其为独立能力。 |
| A66 | passed | specs/magicapi-dynamic-java-entity/spec.md | 后续 ORM 执行器应通过注册表在每次新调用开始时取得当前 Class，并单独设计 ORM 缓存清理和版本过期策略。 | 本体提供每次调用实时 resolve 的稳定边界，外部 ORM 缓存和版本策略明确独立。 |
| A67 | passed | specs/magicapi-dynamic-java-entity/spec.md | 插件禁用或未引入时，不得改变现有 MyBatis-Plus、MyBatis-Flex、`MybatisFlexModule` 或静态 Mapper 的启动与运行行为。 | marker/配置关闭全部本体 Bean，禁用 Java Entity 不改变静态 Mapper 行为。 |
| A68 | passed | specs/magicapi-dynamic-java-entity/spec.md | 修改字段、方法、构造器、接口、继承关系或注解后，成功候选代际应完整替换旧代际。 | 任意成功快照均重新编译全部源码并用新 loader 和代际完整替换。 |
| A69 | passed | specs/magicapi-dynamic-java-entity/spec.md | Lombok 注解变化应重新执行注解处理，不得复用旧版本生成的方法。 | 每次 compile 新建 CompilationTask 和 Lombok processor，不复用旧生成字节码。 |
| A70 | passed | specs/magicapi-dynamic-java-entity/spec.md | 修改包名或公开类名应按删除旧类并新增新类处理，所有引用该类型的动态源码必须在同一候选代际中编译成功。 | 同 Resource id 的新包名或类名替换旧快照项，所有启用源码再共同编译。 |
| A71 | passed | specs/magicapi-dynamic-java-entity/spec.md | 静态字段状态、旧实体实例和线程本地状态不迁移到新代际。 | 新代际使用新 ClassLoader 和 Class，不迁移静态字段、旧实例或 ThreadLocal。 |
| A72 | passed | specs/magicapi-dynamic-java-entity/spec.md | 语法错误、依赖缺失、注解处理失败或实体间引用失败时，旧代际应继续服务。 | 解析、依赖、处理器和实体互引失败均在 current.set 前终止并保持旧代际。 |
| A73 | passed | specs/magicapi-dynamic-java-entity/spec.md | Java 源码编辑、编译和激活入口只允许受信任的 Magic-API 管理员使用。 | 插件复用 Magic-API 原生受认证 Resource 管理入口，不新增旁路编辑端点，文档限定受信管理员。 |
| A74 | passed | specs/magicapi-dynamic-java-entity/spec.md | 系统不承诺在同一 JVM 中安全执行不可信 Java 源码；部署方必须将源码编辑权限视为宿主 JVM 代码执行权限。 | README 和项目安全章节明确源码等同宿主 JVM 代码执行权限且不提供沙箱。 |
| A75 | passed | specs/magicapi-dynamic-java-entity/spec.md | 编译错误和运行错误不得泄露数据源密码、完整连接串、敏感环境变量或无关宿主文件内容。 | 诊断仅暴露 Resource 标识、位置、code 和截断单行摘要，不输出敏感配置。 |
| A76 | passed | specs/magicapi-dynamic-java-entity/spec.md | 编译器不得自动扫描或读取 Resource 源码未引用的任意宿主文件。 | 编译器只处理内存源码和受控 classpath，不扫描任意宿主目录或读取未引用源码文件。 |
| A77 | passed | specs/magicapi-dynamic-java-entity/spec.md | 插件必须兼容 JDK 8、Spring Boot 2.7.18 和项目维护的 Magic-API 2.2.2。 | 实现仅使用 Java 8 API，Zulu JDK 8 测试通过，Magic-API 版本未改。 |
| A78 | passed | specs/magicapi-dynamic-java-entity/spec.md | 插件必须作为独立 Maven 模块接入，禁用或未引入时不影响现有应用。 | 独立 Maven 模块、marker 条件与缺失/禁用上下文测试证明隔离性。 |
| A79 | passed | specs/magicapi-dynamic-java-entity/spec.md | 插件必须使用 UTF-8 处理源码和诊断信息。 | 源码、大小和摘要使用 UTF-8，javac 设置 UTF-8，持久化回归包含中文。 |
| A80 | passed | specs/magicapi-dynamic-java-entity/spec.md | 插件不得依赖运行环境中硬编码的文件路径；临时编译资源应使用受控临时目录或内存对象，并在候选结束后清理。 | 无硬编码环境路径，字节码在内存，Boot classpath 使用系统临时目录并在 close 清理。 |
| A81 | passed | specs/magicapi-dynamic-java-entity/spec.md | 给定无任何注解的普通 Java Bean，保存后可通过 `entity.User.create(map)` 创建并读写属性。 | 无注解 User 的 Resource 往返、entity 描述器和 create(map) 读写集成测试通过。 |
| A82 | passed | specs/magicapi-dynamic-java-entity/spec.md | 给定包含 Lombok `@Data` 和 `@NoArgsConstructor` 的源码，编译后 getter、setter 和无参构造器真实可用。 | 动态注解测试证明 @Data/@NoArgsConstructor 生成 getter、setter 和可用构造器。 |
| A83 | passed | specs/magicapi-dynamic-java-entity/spec.md | 给定包含 Lombok `@Builder` 的源码，生成的 builder 类型和方法可通过真实 Class 访问。 | 测试反射调用 builder、写值、build 并读取结果，证明内部类型及方法真实存在。 |
| A84 | passed | specs/magicapi-dynamic-java-entity/spec.md | 给定仅使用 MP 注解、仅使用 Flex 注解或同时使用两套注解的源码，均可编译并通过 `javaClass()` 取得真实类型。 | MP/Flex 依赖完整，动态类可分别或同时编译两套注解并取得真实类型。 |
| A85 | passed | specs/magicapi-dynamic-java-entity/spec.md | 给定 Fastjson 1.x、Fastjson2 或 Jackson 字段重命名和忽略注解，对应序列化器输出符合注解定义。 | 三种真实序列化器均验证 user_name 重命名且 password 被忽略。 |
| A86 | passed | specs/magicapi-dynamic-java-entity/spec.md | 给定两个互相引用的动态实体，它们在同一代际中编译并可互相解析。 | 交叉引用实体在同一代际编译并验证使用同一 loader。 |
| A87 | passed | specs/magicapi-dynamic-java-entity/spec.md | 给定存在语法或 Lombok 错误的更新源码，系统返回带行列信息的诊断，旧版本继续可用。 | 语法和类型错误诊断包含 Resource 和行列，失败更新保持旧 Class；Lombok 错误走同一路径。 |
| A88 | passed | specs/magicapi-dynamic-java-entity/spec.md | 给定成功修改字段、方法或注解的源码，新访问的描述器使用新 Class，已开始调用中的旧描述器仍可完成。 | 热更新测试证明新描述器使用新 Class 且版本递增，旧描述器仍可创建旧实例。 |
| A89 | passed | specs/magicapi-dynamic-java-entity/spec.md | 给定重复全限定类名或多个顶级公开类，系统拒绝候选代际并指出冲突 Resource；给定冲突简单名时，系统发布候选代际但不注册歧义别名。 | 多 public 类、重复 FQCN、名称不匹配和简单名歧义均有直接拒绝或发布测试。 |
| A90 | passed | specs/magicapi-dynamic-java-entity/spec.md | 删除已激活 Resource 后，后续新解析返回实体不存在，其他实体在新代际中继续可用。 | 删除按完整 sources 快照发布新代际，目标解析失败且其余实体继续可用。 |
| A91 | passed | specs/magicapi-dynamic-java-entity/spec.md | 冷启动包含错误源码时，Spring 上下文和现有 Magic-API HTTP 入口保持可用，动态实体入口返回明确不可用诊断。 | 冷启动错误转为 unavailable 状态而不阻止注册和 Spring 上下文启动。 |
| A92 | passed | specs/magicapi-dynamic-java-entity/spec.md | 任意实体新增、修改或删除都不会新增 Mapper、预热 ORM 元数据或改变现有静态 Mapper 注册表。 | Java Entity 事件只重建自身注册表，不新增 Mapper 或触发 ORM 元数据预热。 |

## Checks

| Check | Command | Working directory | Status | Exit | Duration |
| --- | --- | --- | --- | ---: | ---: |
| JDK 8 Java Entity autoconfigure test matrix | JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home /usr/local/bin/mvn -q -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=DynamicJavaEntityIntegrationTest,CompilationClasspathResolverTest,InMemoryJavaEntityCompilerTest,JavaSourceAnalyzerTest,MagicAPIJavaEntityConfigurationTest,EntityModuleTest,JavaEntityMagicDynamicRegistryTest,JavaEntityMagicResourceStorageTest,DynamicEntityRegistryTest,DynamicEntityDescriptorTest test | fw-plus-boot | passed | 0 | 55536 ms |
| JDK 8 Java Entity plugin module tests | JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home /usr/local/bin/mvn -q -pl fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-java-entity test | fw-plus-boot | passed | 0 | 10541 ms |
| JDK 8 Magic-API plugin aggregator validation | JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home /usr/local/bin/mvn -q -pl fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin -am validate -DskipTests | fw-plus-boot | passed | 0 | 3700 ms |
| JDK 8 Boot BOM validation | JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home /usr/local/bin/mvn -q -pl fw-plus-boot-project/fw-plus-boot-dependencies validate -DskipTests | fw-plus-boot | passed | 0 | 3856 ms |
| Java Entity console tests and embedded Vite build | --test test/java-entity-console.test.mjs | fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-java-entity/src/console | passed | 0 | 1656 ms |
| Git whitespace validation | diff --check | . | passed | 0 | 62 ms |

## Blockers

_None._

## Risks and skipped work

- 不响应线程中断的 javac/处理器任务无法被 JVM 强杀，会持续占用配置的并发许可；实现以 compiler.busy 限流并已在文档说明。
- 外部 MyBatis XML 的 TypeAliasRegistry、MappedStatement 等缓存可持有旧动态 Class；Java Entity 本体不刷新第三方缓存，实体结构变化需按文档重启服务。

## Previous iterations

| Goal cycle | Iteration | Attempt | Outcome | Unresolved | Summary | Completed |
| ---: | ---: | ---: | --- | --- | --- | --- |
| 1 | 1 | 1 | pass | — | 独立只读审查覆盖 A1-A92，全部通过。Runtime 的六项检查均成功，未发现需要追加检查或返回 Build 的缺陷。 | 2026-08-10T16:26:51.376Z |

## Conclusion

独立只读审查覆盖 A1-A92，全部通过。Runtime 的六项检查均成功，未发现需要追加检查或返回 Build 的缺陷。
