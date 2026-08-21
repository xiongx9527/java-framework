---
change: dynamic-jdbc-class-options
design-doc: docs/superpowers/specs/2026-07-23-dynamic-jdbc-class-options-design.md
base-ref: b874215ca46cef35a65c9d04ce727e3d02998233
archived-with: 2026-07-23-dynamic-jdbc-class-options
---

# Dynamic JDBC Class Options Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 为 Magic-API 控制台提供登录后加载的动态 JDBC Driver 和 DataSource 类型候选，下拉只显示当前 classpath 存在且可用的类。

**Architecture:** 后端新增候选服务和受登录保护的候选接口，按候选基线、配置追加项、ClassLoader 存在性和 DataSourceBuilder 可构建能力生成结果。前端在控制台登录完成后请求接口，成功时覆盖现有常量，失败时保留默认常量并继续允许手动输入。

**Tech Stack:** Java 8、Spring Boot 2.7.18、Magic-API 2.2.2、Vue3、Maven Wrapper、JUnit。

## Global Constraints

- 所有 Java 代码必须兼容 JDK 1.8，不能使用 Java 9+ API。
- 不改变 datasource JSON 格式，仍使用 `driverClassName` 和 `type` 字段。
- 不实现运行时上传 jar、远程下载依赖或自定义 ClassLoader 热加载。
- Nebula、Neo4j、MongoDB SQL 属于 JDBC Driver 候选，不属于 DataSource 类型候选。
- 动态候选接口必须在 Magic-API 控制台登录认证通过后加载，未登录请求必须被拒绝。
- datasource 表单 Driver 和 DataSource 类型字段必须继续支持手动输入。

---

## File Structure

- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/datasource/options/MagicJdbcClassOptionProperties.java`
  - 绑定追加 JDBC Driver 和 DataSource 类型候选配置。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/datasource/options/MagicJdbcClassOptions.java`
  - 候选接口返回 DTO。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/datasource/options/MagicJdbcClassOptionProvider.java`
  - 生成过滤后的 `jdbcDrivers` 和 `datasourceTypes`。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/MagicAPIBaseAutoConfiguration.java`
  - 注册属性类和候选 provider bean，并传入 `DataSourceBuilder` 映射。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/controller/MagicAPIController.java`
  - 新增受登录保护的候选接口。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/src/components/magic-editor.vue`
  - 登录完成后请求候选接口并覆盖常量。
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/datasource/options/MagicJdbcClassOptionProviderTest.java`
  - 覆盖后端候选过滤。
- Test: 根据现有前端测试条件，增加最小 smoke 或手动验证记录候选接口加载行为。

### Task 1: 后端候选 Provider

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/datasource/options/MagicJdbcClassOptionProperties.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/datasource/options/MagicJdbcClassOptions.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/datasource/options/MagicJdbcClassOptionProvider.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/datasource/options/MagicJdbcClassOptionProviderTest.java`

**Interfaces:**
- Produces: `MagicJdbcClassOptionProvider#getOptions(): MagicJdbcClassOptions`
- Produces: `MagicJdbcClassOptions#getJdbcDrivers(): List<String>`
- Produces: `MagicJdbcClassOptions#getDatasourceTypes(): List<String>`
- Consumes later: `MagicAPIController` will call `provider.getOptions()`.

- [x] **Step 1: 编写失败测试，验证 Driver 候选按 classpath 过滤**

在测试中创建 provider，追加一个必然存在的类和一个不存在的类：

```java
@Test
public void shouldFilterJdbcDriversByClasspath() {
    MagicJdbcClassOptionProperties properties = new MagicJdbcClassOptionProperties();
    properties.setDriverCandidates(Arrays.asList(
            "java.lang.String",
            "com.example.MissingDriver"));

    MagicJdbcClassOptionProvider provider = new MagicJdbcClassOptionProvider(
            properties,
            Collections.<String, DataSourceBuilder>emptyMap(),
            MagicJdbcClassOptionProviderTest.class.getClassLoader());

    MagicJdbcClassOptions options = provider.getOptions();

    assertTrue(options.getJdbcDrivers().contains("java.lang.String"));
    assertFalse(options.getJdbcDrivers().contains("com.example.MissingDriver"));
}
```

- [x] **Step 2: 运行测试确认失败**

Run:

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicJdbcClassOptionProviderTest test
```

Expected: FAIL，因为测试类和 provider 尚不存在。

- [x] **Step 3: 实现配置属性和 DTO**

`MagicJdbcClassOptionProperties` 使用 Java 8 getter/setter：

```java
@ConfigurationProperties(prefix = "fw.plus.magicapi.jdbc")
public class MagicJdbcClassOptionProperties {
    private List<String> driverCandidates = new ArrayList<String>();
    private List<String> datasourceTypeCandidates = new ArrayList<String>();

    public List<String> getDriverCandidates() {
        return driverCandidates;
    }

    public void setDriverCandidates(List<String> driverCandidates) {
        this.driverCandidates = driverCandidates == null ? new ArrayList<String>() : driverCandidates;
    }

    public List<String> getDatasourceTypeCandidates() {
        return datasourceTypeCandidates;
    }

    public void setDatasourceTypeCandidates(List<String> datasourceTypeCandidates) {
        this.datasourceTypeCandidates = datasourceTypeCandidates == null ? new ArrayList<String>() : datasourceTypeCandidates;
    }
}
```

`MagicJdbcClassOptions` 保存不可变副本：

```java
public class MagicJdbcClassOptions {
    private final List<String> jdbcDrivers;
    private final List<String> datasourceTypes;

    public MagicJdbcClassOptions(List<String> jdbcDrivers, List<String> datasourceTypes) {
        this.jdbcDrivers = Collections.unmodifiableList(new ArrayList<String>(jdbcDrivers));
        this.datasourceTypes = Collections.unmodifiableList(new ArrayList<String>(datasourceTypes));
    }

    public List<String> getJdbcDrivers() {
        return jdbcDrivers;
    }

    public List<String> getDatasourceTypes() {
        return datasourceTypes;
    }
}
```

- [x] **Step 4: 实现 Provider 的 Driver 过滤**

默认 Driver 基线必须包含传统数据库、Nebula、Neo4j、MongoDB SQL：

```java
private static final String[] DEFAULT_JDBC_DRIVER_CANDIDATES = new String[] {
        "com.mysql.jdbc.Driver",
        "com.mysql.cj.jdbc.Driver",
        "oracle.jdbc.driver.OracleDriver",
        "org.postgresql.Driver",
        "com.microsoft.sqlserver.jdbc.SQLServerDriver",
        "com.ibm.db2.jcc.DB2Driver",
        "com.xiongx9527.fw.plus.graph.nebula.jdbc.NebulaDriver",
        "com.xiongx9527.fw.plus.graph.neo4j.jdbc.Neo4jDriver",
        "com.mongodb.jdbc.MongoDriver"
};
```

实现 `filterPresentClasses(Collection<String>)`：跳过 blank，使用 `LinkedHashSet` 去重，使用 `ClassUtils.isPresent(className, classLoader)` 判断存在。

- [x] **Step 5: 运行 Driver 过滤测试确认通过**

Run:

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicJdbcClassOptionProviderTest test
```

Expected: Driver 过滤测试 PASS。

- [x] **Step 6: 提交 Task 1**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/datasource/options fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/datasource/options/MagicJdbcClassOptionProviderTest.java
git commit -m "feat: add magic jdbc class option provider"
```

### Task 2: DataSource 类型可构建过滤

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/datasource/options/MagicJdbcClassOptionProvider.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/datasource/options/MagicJdbcClassOptionProviderTest.java`

**Interfaces:**
- Consumes: `MagicJdbcClassOptionProvider#getOptions()`
- Produces: `datasourceTypes` 只包含类存在且有 builder 或安全默认构建支持的类型。

- [x] **Step 1: 编写失败测试，验证 DataSource 类型过滤**

添加测试：

```java
@Test
public void shouldFilterDatasourceTypesByClasspathAndBuilderSupport() {
    MagicJdbcClassOptionProperties properties = new MagicJdbcClassOptionProperties();
    properties.setDatasourceTypeCandidates(Arrays.asList(
            "java.lang.String",
            "java.lang.Integer",
            "com.example.MissingDataSource"));

    Map<String, DataSourceBuilder> builders = new HashMap<String, DataSourceBuilder>();
    builders.put("java.lang.String", new NoopDataSourceBuilder());

    MagicJdbcClassOptionProvider provider = new MagicJdbcClassOptionProvider(
            properties,
            builders,
            MagicJdbcClassOptionProviderTest.class.getClassLoader());

    MagicJdbcClassOptions options = provider.getOptions();

    assertTrue(options.getDatasourceTypes().contains("java.lang.String"));
    assertFalse(options.getDatasourceTypes().contains("java.lang.Integer"));
    assertFalse(options.getDatasourceTypes().contains("com.example.MissingDataSource"));
}
```

测试内定义 `NoopDataSourceBuilder`，`buildDataSource` 可返回 `null`，因为此测试只校验 builder 存在性。

- [x] **Step 2: 运行测试确认失败**

Run:

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicJdbcClassOptionProviderTest test
```

Expected: FAIL，因为 DataSource 类型尚未按 builder 支持过滤。

- [x] **Step 3: 实现 DataSource 类型过滤**

合并 `MyDataSourceMagicDynamicRegistry.DATA_SOURCE_TYPE_NAMES`、`dataSourceBuilderMap.keySet()` 和配置追加项。保留类存在且满足以下任一条件的类型：

```text
dataSourceBuilderMap.containsKey(type)
或 type 在 DEFAULT_CONSTRUCTIBLE_DATASOURCE_TYPES 中
```

`DEFAULT_CONSTRUCTIBLE_DATASOURCE_TYPES` 可先使用 `MyDataSourceMagicDynamicRegistry.DATA_SOURCE_TYPE_NAMES`，但最终仍需经过 classpath 判断。

- [x] **Step 4: 运行测试确认通过**

Run:

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicJdbcClassOptionProviderTest test
```

Expected: PASS。

- [x] **Step 5: 提交 Task 2**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/datasource/options/MagicJdbcClassOptionProvider.java fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/datasource/options/MagicJdbcClassOptionProviderTest.java
git commit -m "feat: filter constructible datasource type options"
```

### Task 3: 受登录保护的候选接口

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/MagicAPIBaseAutoConfiguration.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/controller/MagicAPIController.java`
- Test: existing controller test location if present, otherwise add provider-level test and smoke record.

**Interfaces:**
- Consumes: `MagicJdbcClassOptionProvider#getOptions()`
- Produces: `GET ${magic-api.web:/magic-api/web}/datasource/options`

- [x] **Step 1: 注册属性和 provider bean**

在 `MagicAPIBaseAutoConfiguration` 添加 `@EnableConfigurationProperties` 支持 `MagicJdbcClassOptionProperties`，并新增 bean：

```java
@Bean
public MagicJdbcClassOptionProvider magicJdbcClassOptionProvider(
        MagicJdbcClassOptionProperties properties,
        Map<String, DataSourceBuilder> dataSourceBuilderMap) {
    return new MagicJdbcClassOptionProvider(properties, dataSourceBuilderMap,
            MagicAPIBaseAutoConfiguration.class.getClassLoader());
}
```

如果当前类已存在 `@EnableConfigurationProperties(DataSourceProperties.class)`，改为数组形式。

- [x] **Step 2: 给 Controller 注入 provider**

扩展 `MagicAPIController` 构造方法，增加 `MagicJdbcClassOptionProvider` 字段。保持 `MagicAPIBaseAutoConfiguration` 中 `@Import(MagicAPIController.class)` 可正常注入。

- [x] **Step 3: 新增候选接口并复用登录认证**

新增方法：

```java
@GetMapping("${magic-api.web:/magic-api/web}/datasource/options")
@ResponseBody
public JsonBean<MagicJdbcClassOptions> datasourceOptions(MagicHttpServletRequest request) {
    isTrue(allowVisit(request, Authorization.VIEW), PERMISSION_INVALID);
    return new JsonBean<MagicJdbcClassOptions>(magicJdbcClassOptionProvider.getOptions());
}
```

如果 `Authorization.VIEW` 在当前 Magic-API 版本不存在，使用项目现有 datasource 读取接口采用的等价权限常量。不得使用 `@Valid(requireLogin = false)`。

- [x] **Step 4: 运行后端测试**

Run:

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicJdbcClassOptionProviderTest test
```

Expected: PASS。

- [x] **Step 5: 提交 Task 3**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/MagicAPIBaseAutoConfiguration.java fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/controller/MagicAPIController.java
git commit -m "feat: expose authenticated jdbc class options endpoint"
```

### Task 4: 前端登录后加载候选

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/src/components/magic-editor.vue`
- Possibly modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/src/scripts/service/magic-api.js`

**Interfaces:**
- Consumes: `GET /datasource/options` relative to Magic-API web path.
- Produces: successful response overrides `constants.JDBC_DRIVERS` and `constants.DATASOURCE_TYPES`.

- [x] **Step 1: 定位登录完成状态**

在 `magic-editor.vue` 中定位 `constants.LOGINED` 或登录成功事件的设置位置。候选接口调用必须放在登录完成后；不能在未登录初始化阶段调用。

- [x] **Step 2: 实现加载函数**

新增函数：

```js
const loadJdbcClassOptions = () => {
    return request.send('GET', '/datasource/options').then(res => {
        const data = res.data || res
        if(data.jdbcDrivers && data.jdbcDrivers.length){
            constants.JDBC_DRIVERS = data.jdbcDrivers
        }
        if(data.datasourceTypes && data.datasourceTypes.length){
            constants.DATASOURCE_TYPES = data.datasourceTypes
        }
    }).catch(e => {
        console.warn('load jdbc class options failed', e)
    })
}
```

按当前 request 工具实际签名调整调用方式，保持失败时不阻塞控制台。

- [x] **Step 3: 在登录完成后调用**

在登录成功回调或初始化确认登录后调用：

```js
loadJdbcClassOptions()
```

如果已有配置注入了 `config.jdbcDrivers` 或 `config.datasourceTypes`，接口结果优先级应高于默认常量；是否高于显式 config 取决于现有配置语义。若 config 被视为用户显式覆盖，则接口只在 config 缺失时覆盖对应字段。

- [x] **Step 4: 验证手动输入保留**

确认 `magic-datasource-datasource.vue` 中两个 `magic-select` 仍保留 `inputable`：

```vue
<magic-select inputable ... />
```

- [x] **Step 5: 运行前端构建或最小检查**

Run:

```bash
cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console && npm run build
```

Expected: PASS。

- [x] **Step 6: 提交 Task 4**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/src/components/magic-editor.vue fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/src/scripts/service/magic-api.js
git commit -m "feat: load jdbc class options after console login"
```

### Task 5: 端到端验证与 OpenSpec 任务收尾

**Files:**
- Modify: `openspec/changes/dynamic-jdbc-class-options/tasks.md`
- Possibly create: `docs/superpowers/reports/2026-07-23-dynamic-jdbc-class-options-build.md`

**Interfaces:**
- Consumes: completed backend provider, authenticated endpoint, frontend loader.
- Produces: checked OpenSpec task list and build evidence.

- [x] **Step 1: 运行后端模块测试**

Run:

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicJdbcClassOptionProviderTest test
```

Expected: PASS。

- [x] **Step 2: 运行前端构建**

Run:

```bash
cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console && npm run build
```

Expected: PASS。

- [x] **Step 3: 记录登录接口验证**

若能启动应用，使用已登录会话访问：

```text
GET /magic-api/web/datasource/options
```

Expected: 返回 `jdbcDrivers` 和 `datasourceTypes`。未登录访问应按 Magic-API 控制台权限规则被拒绝。

若无法启动应用，在 build report 中记录原因，并以 Controller 权限代码审查和单元测试作为替代证据。

- [x] **Step 4: 勾选 OpenSpec tasks**

将 `openspec/changes/dynamic-jdbc-class-options/tasks.md` 中已完成任务全部改为 `- [x]`。

- [x] **Step 5: 提交 Task 5**

```bash
git add openspec/changes/dynamic-jdbc-class-options/tasks.md docs/superpowers/reports/2026-07-23-dynamic-jdbc-class-options-build.md
git commit -m "chore: record dynamic jdbc option verification"
```

## Self-Review

- Spec coverage: 计划覆盖动态候选接口、classpath 过滤、DataSource 可构建过滤、登录后加载、未登录拒绝、手动输入保留、Nebula/Neo4j/MongoDB SQL 候选和 datasource JSON 兼容。
- 文档完整性扫描: 未使用未完成标记。
- Type consistency: `MagicJdbcClassOptionProvider#getOptions()`、`MagicJdbcClassOptions#getJdbcDrivers()`、`MagicJdbcClassOptions#getDatasourceTypes()` 在各任务中保持一致。
