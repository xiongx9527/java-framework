# Magic-API Dynamic Java Entity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增独立可选装的 Magic-API 动态 Java 实体插件，使完整 Java 8 源码可被全代际编译、原子热更新，并通过 `entity` 模块创建实例或取得真实 `Class<?>`。

**Architecture:** 业务实现放在现有 `fw-plus-boot-autoconfigure` 的 `magicapi.plugin.javaentity` 包，独立 Maven 插件模块负责携带 Lombok、MyBatis-Plus、MyBatis-Flex、Fastjson 1.x、Fastjson2 和 Jackson 运行时编译依赖。Resource 事件只维护源码快照，串行协调器每次将全部源码交给 JDK `JavaCompiler` 共同编译；候选代际完成源码分析、注解处理、字节码生成、类加载和别名校验后，才通过单个 `AtomicReference` 替换当前不可变代际。

**Tech Stack:** Java 8、Spring Boot 2.7.18、Magic-API 2.2.2、JDK JavaCompiler/JavacTask、Lombok、MyBatis-Plus 3.3.2、MyBatis-Flex 1.11.4、Fastjson/Fastjson2 2.0.53、Jackson 2.20.1、JUnit 5。

## Global Constraints

- 生产代码只使用 Java 8 API，动态编译固定传入 `-source 8 -target 8 -encoding UTF-8`。
- 不修改、fork 或升级 Magic-API 2.2.2、MyBatis-Plus、MyBatis-Flex、MP-Join。
- 首期不实现 CRUD、分页、Join、MP-Join、动态 Mapper 或 ORM 元数据预热。
- 全限定类名是稳定主键；简单类名仅在当前代际唯一时注册为别名。
- 每个 Resource 只允许一个顶级公开类，Resource 名称必须等于公开类简单名。
- 候选代际失败时保留最后一次成功代际；首次失败不得阻止 Spring 应用启动。
- Lombok 必须出现在运行时编译 classpath 和 processor path，每次代际重编译都重新执行 annotation processing。
- Java 源码是受信任管理员能力，不提供同 JVM 内的不可信代码沙箱。
- 编译诊断只返回 Resource、行列号、诊断代码和受限消息，不输出环境变量、连接串或无关宿主文件。
- 保留主工作区和本 worktree 中与本变更无关的现有修改，不提交 Comet runtime 大文件。

---

### Task 1: Maven module and dependency boundary

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-java-entity/pom.xml`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/pom.xml`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-dependencies/pom.xml`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/pom.xml`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/JavaEntityDependencyCompatibilityTest.java`

**Interfaces:**
- Consumes: Boot BOM 中受管版本，以及 `fw-plus-boot-autoconfigure` 的集中业务实现边界。
- Produces: `fw-plus-magicapi-plugin-java-entity` 可选模块；测试 classpath 可见 Lombok、MP、Flex、Fastjson 1.x、Fastjson2、Jackson annotations/databind。

- [ ] **Step 1: Write the failing dependency compatibility test**

  新建 `JavaEntityDependencyCompatibilityTest`，分别用 `Class.forName` 断言以下类型可见：`lombok.Data`、`com.baomidou.mybatisplus.annotation.TableName`、`com.mybatisflex.annotation.Table`、`com.alibaba.fastjson.annotation.JSONField`、`com.alibaba.fastjson2.annotation.JSONField`、`com.fasterxml.jackson.annotation.JsonProperty`、`com.fasterxml.jackson.databind.annotation.JsonSerialize`。

- [ ] **Step 2: Run test and verify RED**

  Run: `./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure -Dtest=JavaEntityDependencyCompatibilityTest test`

  Expected: FAIL because the test class or one of the explicit JSON/compiler dependencies is not yet configured.

- [ ] **Step 3: Add the independent module and managed dependencies**

  新模块继承 `fw-plus-magicapi-plugin`，依赖 `fw-plus-boot-autoconfigure`、`magic-api`、`lombok`、`mybatis-plus-core`、`mybatis-flex-core`、`fastjson`、`fastjson2`、`jackson-annotations`、`jackson-databind`；这些依赖不能标为 `provided`。在插件聚合 POM 增加 module，在 Boot BOM 增加新 artifact 的 dependencyManagement，在 autoconfigure POM 增加测试所需依赖并保持现有 optional 依赖语义。

- [ ] **Step 4: Run test and reactor validation for GREEN**

  Run: `./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure,fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-java-entity -am -Dtest=JavaEntityDependencyCompatibilityTest -Dsurefire.failIfNoSpecifiedTests=false test`

  Expected: PASS and the new module resolves entirely from managed versions.

- [ ] **Step 5: Commit**

  ```bash
  git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-dependencies/pom.xml fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/pom.xml fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/pom.xml fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-java-entity fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/JavaEntityDependencyCompatibilityTest.java
  git commit -m "feat(magicapi): 接入动态实体插件模块"
  ```

### Task 2: Source model, syntax-tree analysis, and diagnostics

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/model/JavaEntityInfo.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/compiler/JavaSourceUnit.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/compiler/JavaEntityDiagnostic.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/compiler/JavaEntityCompilationException.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/compiler/JavaSourceAnalyzer.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/compiler/JavaSourceAnalyzerTest.java`

**Interfaces:**
- Consumes: `JavaEntityInfo#getId()`, `getName()`, `getScript()` from Magic-API `PathMagicEntity`.
- Produces: `JavaSourceUnit(String resourceId, String resourceName, String source, String packageName, String simpleName, String className)`; `JavaSourceAnalyzer#analyze(JavaEntityInfo)`; immutable diagnostic fields `resourceId`, `line`, `column`, `code`, `message`.

- [ ] **Step 1: Write failing analyzer tests**

  用完整源码 fixture 覆盖默认包、命名包、注解修饰公开类、内部类；另写失败用例覆盖零个/多个顶级公开类、Resource 名称与公开类名不一致。断言 `className` 为手工给定字面量，错误只包含当前 Resource 定位信息。

- [ ] **Step 2: Run analyzer tests and verify RED**

  Run: `./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure -Dtest=JavaSourceAnalyzerTest test`

  Expected: compilation FAIL because analyzer types do not exist.

- [ ] **Step 3: Implement syntax-tree analysis**

  `JavaSourceAnalyzer` 使用 `ToolProvider.getSystemJavaCompiler()` 创建 `JavacTask` 并遍历 `CompilationUnitTree#getTypeDecls()`；仅接受 `ClassTree` 且 modifiers 包含 `Modifier.PUBLIC` 的单个顶级类型。不得用正则解析 package 或 class；缺少系统 compiler 时抛出带 `compiler.unavailable` code 的受控异常。

- [ ] **Step 4: Run analyzer tests and verify GREEN**

  Run: `./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure -Dtest=JavaSourceAnalyzerTest test`

  Expected: PASS.

- [ ] **Step 5: Commit**

  ```bash
  git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/compiler/JavaSourceAnalyzerTest.java
  git commit -m "feat(magicapi): 分析动态实体 Java 源码"
  ```

### Task 3: In-memory compiler and generation classloader

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/compiler/MemoryJavaFileManager.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/compiler/GenerationClassLoader.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/compiler/CompilationClasspathResolver.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/compiler/CompiledGeneration.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/compiler/InMemoryJavaEntityCompiler.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/compiler/InMemoryJavaEntityCompilerTest.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/compiler/CompilationClasspathResolverTest.java`

**Interfaces:**
- Consumes: `List<JavaSourceUnit>`, parent `ClassLoader`, compiler options.
- Produces: `InMemoryJavaEntityCompiler#compile(long version, List<JavaSourceUnit>) -> CompiledGeneration`; `CompiledGeneration#getClasses()`, `getClassLoader()`, `getVersion()`; `GenerationClassLoader` only defines candidate bytecode and delegates dependencies to the application classloader.

- [ ] **Step 1: Write failing compiler tests for plain and cross-resource entities**

  编译 `demo.User` 和引用它的 `demo.Dept`，反射实例化并读写字段；另传入语法错误，断言 exception diagnostics 中的 Resource ID、正数行列号和受限消息。

- [ ] **Step 2: Run compiler tests and verify RED**

  Run: `./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure -Dtest=InMemoryJavaEntityCompilerTest,CompilationClasspathResolverTest test`

  Expected: compilation FAIL because compiler types do not exist.

- [ ] **Step 3: Implement memory file objects and compiler options**

  输入源码用 `SimpleJavaFileObject` 的 `string:///demo/User.java` URI；输出字节码由 forwarding file manager 收集。每次任务设置 `-source 8 -target 8 -encoding UTF-8 -classpath <resolved>`，显式启用 annotation processing，不复用 file manager、compiler task 或 candidate classloader。

- [ ] **Step 4: Implement classpath resolution including Boot executable JARs**

  合并 `java.class.path`、可见 `URLClassLoader` URLs 和代码来源 URL；普通目录/JAR 直接加入。遇到 Spring Boot `BOOT-INF/classes` 或 `BOOT-INF/lib/*.jar` 嵌套项时，将只读 archive 内容复制到一次编译任务拥有的受控临时目录，返回可由 javac 读取的目录/JAR路径；`CompiledGeneration#close()` 和失败路径都递归释放该临时目录。路径来源不进入用户诊断。

- [ ] **Step 5: Run plain compiler tests and verify GREEN**

  Run: `./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure -Dtest=InMemoryJavaEntityCompilerTest,CompilationClasspathResolverTest test`

  Expected: PASS, cross-resource types use the same `GenerationClassLoader`.

- [ ] **Step 6: Commit**

  ```bash
  git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/compiler fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/compiler
  git commit -m "feat(magicapi): 实现动态实体内存编译器"
  ```

### Task 4: Lombok, ORM, Fastjson, and Jackson compiler compatibility

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/compiler/InMemoryJavaEntityCompiler.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/compiler/InMemoryJavaEntityCompilerTest.java`

**Interfaces:**
- Consumes: runtime compiler dependencies from Task 1.
- Produces: fresh Lombok processor execution per compile; runtime-retained MP/Flex/Fastjson/Jackson annotations on generated classes.

- [ ] **Step 1: Add failing Lombok and ORM annotation tests**

  编译一个同时声明 `@Data`、`@Builder`、`@NoArgsConstructor`、MP `@TableName/@TableField` 和 Flex `@Table/@Column` 的实体；断言 Lombok getter/setter/builder 真正可调用，并从反射读取两套表/字段注解值。

- [ ] **Step 2: Add failing real JSON serialization tests**

  分别编译 Fastjson 1.x、Fastjson2 和 Jackson 实体。每个实体对 `userName` 使用各自重命名注解，对 `password` 使用各自忽略注解；分别调用 `JSON.toJSONString`、Fastjson2 `JSON.toJSONString`、Jackson `ObjectMapper.writeValueAsString`，断言字面量字段名存在且 `password` 不存在。Jackson fixture 同时编译 `JsonAlias/JsonFormat/JsonInclude` 和 databind 的 `JsonSerialize/JsonDeserialize` 声明。

- [ ] **Step 3: Run compatibility tests and verify RED**

  Run: `./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure -Dtest=InMemoryJavaEntityCompilerTest test`

  Expected: FAIL because processor path or runtime compiler classpath is incomplete.

- [ ] **Step 4: Configure annotation processor path and classloader context**

  将解析出的 Lombok jar 同时加入 compiler classpath 和 `-processorpath`，用 `lombok.launch.AnnotationProcessorHider$AnnotationProcessor` 显式触发处理；编译期间临时设置线程 context classloader 为应用 classloader，并在 finally 恢复。JSON/ORM 注解不配置 processor。

- [ ] **Step 5: Run compatibility tests and verify GREEN**

  Run: `./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure -Dtest=InMemoryJavaEntityCompilerTest test`

  Expected: PASS for Lombok generated APIs, MP/Flex reflection, and all three real serializers.

- [ ] **Step 6: Commit**

  ```bash
  git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/compiler/InMemoryJavaEntityCompiler.java fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/compiler/InMemoryJavaEntityCompilerTest.java
  git commit -m "feat(magicapi): 支持动态实体常用注解"
  ```

### Task 5: Immutable generation registry and atomic hot update

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/runtime/DynamicEntityGeneration.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/runtime/DynamicEntityRegistry.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/runtime/DynamicEntityStatus.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/runtime/DynamicEntityRegistryTest.java`

**Interfaces:**
- Consumes: analyzed `JavaEntityInfo` snapshot and `InMemoryJavaEntityCompiler`.
- Produces: `replace(JavaEntityInfo)`, `remove(String resourceId)`, `rebuild(Collection<JavaEntityInfo>)`, `resolve(String name)`, `status()`; `resolve` returns a descriptor tied to one immutable version.

- [ ] **Step 1: Write failing atomic-generation tests**

  覆盖首次发布、字段更新产生不同 `Class<?>`、失败更新保留旧 Class、删除后无法解析、两个包同名实体只能用 FQCN 访问、重复 FQCN 拒绝。并发测试用 latch 让多个 reader 循环读取，断言每次观察到的 descriptor version 与 class generation 一致。

- [ ] **Step 2: Run registry tests and verify RED**

  Run: `./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure -Dtest=DynamicEntityRegistryTest test`

  Expected: compilation FAIL because runtime types do not exist.

- [ ] **Step 3: Implement serialized rebuild and atomic swap**

  源码快照只在 `ReentrantLock` 内复制；候选编译在同一串行临界区执行，成功后构造不可变 FQCN map 和仅唯一项的 alias map，并通过 `AtomicReference#set` 一次切换。失败只更新 `DynamicEntityStatus`，不修改当前 generation。注册表不保存 retired generation 集合。

- [ ] **Step 4: Run registry tests and verify GREEN**

  Run: `./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure -Dtest=DynamicEntityRegistryTest test`

  Expected: PASS with no mixed-generation observations.

- [ ] **Step 5: Commit**

  ```bash
  git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/runtime fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/runtime
  git commit -m "feat(magicapi): 原子切换动态实体代际"
  ```

### Task 6: Entity descriptor and Magic-Script module

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/runtime/DynamicEntityDescriptor.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/module/EntityModule.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/runtime/DynamicEntityDescriptorTest.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/module/EntityModuleTest.java`

**Interfaces:**
- Consumes: `DynamicEntityRegistry#resolve(String)`.
- Produces: `@MagicModule("entity") EntityModule implements DynamicAttribute<EntityModule, DynamicEntityDescriptor>, DynamicModule<EntityModule>`; descriptor methods `name()`, `version()`, `javaClass()`, `create()`, `create(Map<String,Object>)`.

- [ ] **Step 1: Write failing descriptor creation tests**

  对含 setter 的属性断言优先调用 setter；对无 setter 的非 static/non-final 字段断言受控回退；对未知属性、final 字段、错误类型、无可访问无参构造器分别断言明确异常包含实体名和属性名。

- [ ] **Step 2: Write failing module lookup tests**

  直接调用 `getDynamicAttribute("User")` 和 FQCN 路径取得 descriptor，断言 `name/version/javaClass`；构造下一代后再次 lookup 得到新 descriptor，而旧 descriptor 仍能完成当前调用。

- [ ] **Step 3: Run descriptor/module tests and verify RED**

  Run: `./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure -Dtest=DynamicEntityDescriptorTest,EntityModuleTest test`

  Expected: compilation FAIL because descriptor/module types do not exist.

- [ ] **Step 4: Implement JavaBean assignment and dynamic module**

  `create(map)` 使用 `Introspector.getBeanInfo` 建立 setter 索引，优先调用单参数 write method；无 setter 时逐级查找声明字段，只允许实例、非 final 字段。只接受反射本身可赋值以及常见 number/boolean/char/string/enum 转换，其他值拒绝。模块本身不缓存 descriptor 或 Class。

- [ ] **Step 5: Run descriptor/module tests and verify GREEN**

  Run: `./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure -Dtest=DynamicEntityDescriptorTest,EntityModuleTest test`

  Expected: PASS.

- [ ] **Step 6: Commit**

  ```bash
  git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/runtime/DynamicEntityDescriptor.java fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/module fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/runtime/DynamicEntityDescriptorTest.java fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/module/EntityModuleTest.java
  git commit -m "feat(magicapi): 提供 entity 动态模块"
  ```

### Task 7: java-entity Resource lifecycle and auto-configuration

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/service/JavaEntityMagicResourceStorage.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/service/JavaEntityMagicDynamicRegistry.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/starter/MagicAPIJavaEntityConfiguration.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/MagicApiPluginAutoConfiguration.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/service/JavaEntityMagicResourceStorageTest.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/service/JavaEntityMagicDynamicRegistryTest.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/starter/MagicAPIJavaEntityConfigurationTest.java`

**Interfaces:**
- Consumes: Magic-API `MagicResourceStorage<JavaEntityInfo>`, `MagicDynamicRegistry<JavaEntityInfo>`, FileEvent/GroupEvent; Task 5 registry and Task 6 module.
- Produces: Resource folder/type `java-entity`; Spring property `magic-api.plugin.java-entity.enabled` default true; startup-safe beans for storage, registry, compiler and module.

- [ ] **Step 1: Write failing storage round-trip tests**

  用包含完整 Java 源码的 `JavaEntityInfo` 调用 `write` 再 `read`，断言 UTF-8 源码逐字保留、folder 为 `java-entity`、mapping key 基于 FQCN；空源码和名称不匹配由 analyzer-backed validate 拒绝。

- [ ] **Step 2: Write failing lifecycle and context tests**

  对 `register/unregister` 断言每次都用当前完整快照 rebuild，失败更新返回 false 且旧 generation 可用；用 `ApplicationContextRunner` 验证默认启用、property 禁用、`ToolProvider` 不可用状态不阻止上下文启动，并验证 `EntityModule` bean 存在。

- [ ] **Step 3: Run resource/config tests and verify RED**

  Run: `./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure -Dtest=JavaEntityMagicResourceStorageTest,JavaEntityMagicDynamicRegistryTest,MagicAPIJavaEntityConfigurationTest test`

  Expected: compilation FAIL because integration types do not exist.

- [ ] **Step 4: Implement Resource registry and event handling**

  `JavaEntityMagicDynamicRegistry` 实现 Magic-API registry contract，但不继承逐 Resource 映射实现；内部 snapshot 以 resource ID 为键，`register` 先放入候选 snapshot 并全量 rebuild，失败仍保留源码候选供后续互相引用实体到齐后重试，当前可用 generation 不变；`unregister` 删除后全量 rebuild。FileEvent/GroupEvent 仅处理 `java-entity` 类型并复用相同方法。

- [ ] **Step 5: Implement conditional auto-configuration**

  配置类用 `@ConditionalOnClass(MagicConfiguration.class)` 和 `@ConditionalOnProperty(prefix="magic-api.plugin.java-entity", name="enabled", havingValue="true", matchIfMissing=true)`；`ToolProvider.getSystemJavaCompiler()==null` 时仍创建 registry/module，但 status 为 unavailable，首次 Resource 编译失败不会从 bean factory 抛出。

- [ ] **Step 6: Run resource/config tests and verify GREEN**

  Run: `./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure -Dtest=JavaEntityMagicResourceStorageTest,JavaEntityMagicDynamicRegistryTest,MagicAPIJavaEntityConfigurationTest test`

  Expected: PASS.

- [ ] **Step 7: Commit**

  ```bash
  git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/MagicApiPluginAutoConfiguration.java fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/service fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/starter fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/service fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/starter
  git commit -m "feat(magicapi): 接入动态实体资源生命周期"
  ```

### Task 8: Integration verification, limits, and usage documentation

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/starter/JavaEntityProperties.java`
- Modify: compiler/runtime classes from Tasks 3 and 5 to enforce limits.
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity/DynamicJavaEntityIntegrationTest.java`
- Create: `docs/magicapi-dynamic-java-entity.md`

**Interfaces:**
- Consumes: all public interfaces from Tasks 1-7.
- Produces: configurable defaults `max-source-bytes=262144`, `max-total-source-bytes=2097152`, `compile-timeout=30s`, `max-diagnostics=100`, `max-diagnostic-message-length=500`; end-user Resource and Magic-Script examples.

- [ ] **Step 1: Write failing limits and end-to-end tests**

  覆盖单源码/总源码超限、诊断数量和单消息截断、compile timeout；端到端 fixture 经 storage round-trip、registry register、`EntityModule#getDynamicAttribute("User")#create(map)`，随后更新源码并断言新 Class 生效、旧 descriptor 可继续调用。

- [ ] **Step 2: Run integration tests and verify RED**

  Run: `./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure -Dtest=DynamicJavaEntityIntegrationTest test`

  Expected: FAIL because limits/properties are not implemented.

- [ ] **Step 3: Implement bounded compilation**

  编译工作提交给单线程 executor，并以 configured timeout 等待；超时取消 future、废弃候选并保留旧 generation。所有 byte/string 计数均基于 UTF-8，diagnostic 列表和消息长度在构造受控异常前截断。executor 在 Spring bean destroy 时 shutdown。

- [ ] **Step 4: Write usage documentation**

  文档给出 `java-entity` Resource JSON header + 分隔线 + Java 8 源码，分别展示普通 Bean、Lombok、MP/Flex 双注解、Fastjson/Jackson 注解；给出 Magic-Script：`var User = entity.User; var user = User.create({userName: '张三'}); User.javaClass()`，明确源码具备 JVM 代码执行权限且首期没有 CRUD/Mapper/Join。

- [ ] **Step 5: Run focused and reactor verification for GREEN**

  Run: `./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure -Dtest='*JavaEntity*Test,*DynamicEntity*Test,*InMemoryJavaEntityCompilerTest,*EntityModuleTest' test`

  Run: `./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure,fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-java-entity -am -DskipTests install`

  Run: `./fw-plus-core/mvnw -f fw-plus-sample/pom.xml validate`

  Expected: all commands PASS with Java 8 target bytecode and no dependency convergence error.

- [ ] **Step 6: Commit**

  ```bash
  git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/javaentity docs/magicapi-dynamic-java-entity.md docs/superpowers/plans/2026-08-05-magicapi-dynamic-java-entity.md docs/comet/changes/magicapi-dynamic-entity-plugin/brief.md
  git commit -m "docs(magicapi): 补充动态实体使用与验证"
  ```

## Self-review record

- Spec coverage: Tasks 2-8 cover完整源码 Resource、语法树解析、Java 8 编译、全代际互引、Lombok、MP/Flex、Fastjson 1.x/2、Jackson、原子切换、失败回滚、删除、并发、Magic-Script、JRE 降级、限制和可执行 JAR classpath。
- Non-goal coverage: no task creates Mapper, invokes ORM metadata caches, adds CRUD/Join/MP-Join, or changes MP/Flex/MP-Join.
- Placeholder scan: the plan contains no TBD/TODO/implement-later step; each code task has explicit input, observable test, command, expected RED/GREEN result, and commit boundary.
- Type consistency: `JavaSourceUnit -> CompiledGeneration -> DynamicEntityGeneration -> DynamicEntityDescriptor -> EntityModule` names and signatures are stable across tasks; Resource registry delegates only to `DynamicEntityRegistry`.
