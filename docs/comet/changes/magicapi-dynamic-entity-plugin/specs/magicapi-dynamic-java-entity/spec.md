# Magic-API 动态 Java 实体

## 目标

系统应提供一种可选装的 Magic-API Resource 能力，用于保存、动态编译和加载 Java 8 实体源码。实体主要在 Magic-Script 中使用，可为普通 Java Bean，也可选择声明 Lombok、MyBatis-Plus、MyBatis-Flex、Fastjson 或 Jackson 注解。源码更新成功后无需重启应用即可使用新版本。

## Java 源码 Resource

- 系统应注册 `java-entity` Resource 类型，其正文为 UTF-8 编码的完整 Java 源文件。
- Resource 应参与 Magic-API 现有的分组、读取、保存、更新、删除和应用启动恢复流程。
- 每个 Resource 第一版只允许一个顶级公开类，并应解析出唯一全限定类名。
- Resource 名称应与顶级公开类简单名一致；重复全限定类名、重复简单名别名、多个顶级公开类或名称不一致时应拒绝候选版本。
- 普通 Java Bean 可以直接编译和发布，不要求 ORM 注解、Lombok 注解、接口或框架基类。
- 源码可选择引用 MyBatis-Plus `@TableName`、`@TableId`、`@TableField` 和 MyBatis-Flex `@Table`、`@Id`、`@Column` 等当前项目版本提供的注解。
- 源码可选择引用 Fastjson 1.x、Fastjson2 和 Jackson 当前项目版本提供的序列化注解。
- 第一版不从数据库元数据生成源码，用户直接维护 Resource 中的 Java 源码。

## Lombok 注解处理

- 插件应在运行时提供与项目依赖管理一致的 Lombok 编译依赖，不得将 Lombok 仅作为插件构建期 `provided` 依赖。
- 动态编译任务应启用注解处理，并使 Lombok 同时出现在编译 classpath 和 processor path。
- 系统应支持 Lombok 当前版本提供的常用注解，包括 `@Data`、`@Getter`、`@Setter`、`@Builder`、`@NoArgsConstructor`、`@AllArgsConstructor` 和 `@EqualsAndHashCode`。
- Lombok 生成的方法、构造器和内部类型应包含在候选代际字节码中，并可被反射和 Magic-Script 正常访问。
- Lombok 处理错误应作为编译失败返回，诊断应包含 Resource、行号、列号和消息摘要。
- `create()` 与 `create(map)` 要求实体存在可访问的无参构造器；没有时实体仍可通过 `javaClass()` 获取，但创建调用应返回明确错误。

## 动态编译与代际类加载

- 系统应使用 JDK `JavaCompiler`，以 `-source 8` 和 `-target 8` 编译源码，不得使用 Java 9 及以上 API。
- 系统应使用当前应用可见的受控编译 classpath，并支持开发目录、普通 JAR 和 Spring Boot 可执行 JAR 中所需的编译依赖。
- 每次新增、修改或删除 Resource 时，应将全部已启用动态实体源码作为一个候选代际共同编译。
- 候选代际应使用新的 Generation ClassLoader 加载，不得在已经定义同名类的 ClassLoader 中覆盖字节码。
- 编译、注解处理、类加载和注册表校验全部成功后，系统才可原子替换当前代际。
- 任一步骤失败时，系统应关闭候选代际可释放资源并保持最后一次成功代际可用，不得暴露半切换状态。
- 冷启动首次编译失败时，应用应继续启动，动态实体模块应返回不可用诊断，不能暴露未完整加载的类。
- 新调用应从当前注册表解析 Class；切换前已开始的调用可继续使用旧 Class，注册表不得主动破坏正在使用的旧实例。
- 当前注册表不得保留已退休代际的强引用；旧代际在不存在外部实例、请求或第三方缓存引用后应可被回收。
- 系统应限制单个源码大小、单次代际源码总量、编译时长、诊断输出长度和并发编译数量。

## JSON 注解兼容

- 动态编译 classpath 应包含项目依赖管理中的 Fastjson 1.x、Fastjson2 和 Jackson 注解包。
- Fastjson 1.x 应支持 `com.alibaba.fastjson.annotation.JSONField`、`JSONType` 等运行时注解。
- Fastjson2 应支持 `com.alibaba.fastjson2.annotation.JSONField`、`JSONType` 等运行时注解。
- Jackson 应支持 `com.fasterxml.jackson.annotation.JsonProperty`、`JsonIgnore`、`JsonFormat`、`JsonAlias`、`JsonInclude` 等运行时注解。
- 当 Jackson databind 注解依赖可见时，源码可使用 `JsonSerialize` 和 `JsonDeserialize`；插件不得自行实例化用户声明的自定义序列化器执行不受控校验。
- JSON 注解不需要 Java annotation processor，编译器应保留注解及其属性供对应序列化器运行时读取。
- 测试应使用项目当前 Fastjson 1.x、Fastjson2 和 Jackson 序列化器验证字段重命名及忽略行为，而不只验证注解类可加载。

## 实体注册表

- 注册表应以全限定类名作为稳定主键，记录 Resource 标识、源码摘要、代际版本和当前 `Class<?>`。
- 简单类名只可作为唯一别名；两个包中存在同名类时应拒绝简单名别名，并要求使用全限定类名访问。
- 注册表切换应为单次原子引用替换，读取路径不得观察到可变的半构建 Map。
- Resource 删除后，新代际不再包含该类；后续新解析应返回实体不存在错误。
- 注册表不得将内部可变 Map、ClassLoader 或编译器对象暴露给 Magic-Script。

## Magic-Script 访问

- 系统应注册名为 `entity` 的 Magic-Script 动态模块。
- `entity.User` 应返回当前代际的实体描述器，而不是长期缓存的 `Class<?>`。
- 描述器应至少提供 `name()`、`version()`、`javaClass()`、`create()` 和 `create(map)`。
- `javaClass()` 应返回当前描述器对应的真实 Java Class，使后续 MyBatis-Plus、MyBatis-Flex 或其他 Java API 可以接收该类型。
- `create()` 应通过可访问无参构造器创建实例。
- `create(map)` 应先创建实例，再按 Java Bean 属性写入值；优先使用 setter，并对没有 setter 的可写实例字段提供受控回退。
- 属性赋值应拒绝静态字段、final 字段、未知属性和不可转换值，并指出实体名和属性名。
- 描述器属于取得它时的代际；源码切换后，再次访问 `entity.User` 应返回新描述器，已取得的旧描述器只允许完成当前调用，不应被全局缓存。

## ORM 兼容边界

- 动态编译 classpath 应允许源码引用项目当前版本的 MyBatis-Plus 和 MyBatis-Flex 注解。
- 普通实体、仅 MP 注解实体、仅 Flex 注解实体、双 ORM 注解实体和带 JSON 注解的实体均可发布。
- 首期不要求存在表名、主键或字段注解，也不比较两套 ORM 映射是否一致。
- 首期不调用 MyBatis-Plus `TableInfoHelper` 或 MyBatis-Flex `TableInfoFactory` 预热元数据，避免将已退休 ClassLoader 固定在 ORM 静态缓存中。
- 首期不生成 Mapper、不注册实体专属 Mapper、不修改静态 Mapper，也不实现 CRUD、分页、Join 或 MP-Join。
- 后续 ORM 执行器应通过注册表在每次新调用开始时取得当前 Class，并单独设计 ORM 缓存清理和版本过期策略。
- 插件禁用或未引入时，不得改变现有 MyBatis-Plus、MyBatis-Flex、`MybatisFlexModule` 或静态 Mapper 的启动与运行行为。

## 热更新语义

- 修改字段、方法、构造器、接口、继承关系或注解后，成功候选代际应完整替换旧代际。
- Lombok 注解变化应重新执行注解处理，不得复用旧版本生成的方法。
- 修改包名或公开类名应按删除旧类并新增新类处理，所有引用该类型的动态源码必须在同一候选代际中编译成功。
- 静态字段状态、旧实体实例和线程本地状态不迁移到新代际。
- 语法错误、依赖缺失、注解处理失败或实体间引用失败时，旧代际应继续服务。

## 安全与权限

- Java 源码编辑、编译和激活入口只允许受信任的 Magic-API 管理员使用。
- 系统不承诺在同一 JVM 中安全执行不可信 Java 源码；部署方必须将源码编辑权限视为宿主 JVM 代码执行权限。
- 编译错误和运行错误不得泄露数据源密码、完整连接串、敏感环境变量或无关宿主文件内容。
- 编译器不得自动扫描或读取 Resource 源码未引用的任意宿主文件。

## 兼容性与隔离

- 插件必须兼容 JDK 8、Spring Boot 2.7.18 和项目维护的 Magic-API 2.2.2。
- 插件必须作为独立 Maven 模块接入，禁用或未引入时不影响现有应用。
- 插件必须使用 UTF-8 处理源码和诊断信息。
- 插件不得依赖运行环境中硬编码的文件路径；临时编译资源应使用受控临时目录或内存对象，并在候选结束后清理。

## 验收场景

- 给定无任何注解的普通 Java Bean，保存后可通过 `entity.User.create(map)` 创建并读写属性。
- 给定包含 Lombok `@Data` 和 `@NoArgsConstructor` 的源码，编译后 getter、setter 和无参构造器真实可用。
- 给定包含 Lombok `@Builder` 的源码，生成的 builder 类型和方法可通过真实 Class 访问。
- 给定仅使用 MP 注解、仅使用 Flex 注解或同时使用两套注解的源码，均可编译并通过 `javaClass()` 取得真实类型。
- 给定 Fastjson 1.x、Fastjson2 或 Jackson 字段重命名和忽略注解，对应序列化器输出符合注解定义。
- 给定两个互相引用的动态实体，它们在同一代际中编译并可互相解析。
- 给定存在语法或 Lombok 错误的更新源码，系统返回带行列信息的诊断，旧版本继续可用。
- 给定成功修改字段、方法或注解的源码，新访问的描述器使用新 Class，已开始调用中的旧描述器仍可完成。
- 给定重复全限定类名、冲突简单名别名或多个顶级公开类，系统拒绝候选代际并指出冲突 Resource。
- 删除已激活 Resource 后，后续新解析返回实体不存在，其他实体在新代际中继续可用。
- 冷启动包含错误源码时，Spring 上下文和现有 Magic-API HTTP 入口保持可用，动态实体入口返回明确不可用诊断。
- 任意实体新增、修改或删除都不会新增 Mapper、预热 ORM 元数据或改变现有静态 Mapper 注册表。
