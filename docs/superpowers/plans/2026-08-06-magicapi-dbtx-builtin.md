# Magic-API Built-in DBTX Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将已实现的 Magic-API dbtx/local/Seata 后端能力从两个临时 plugin artifact 迁移为现有 `fw-plus-magicapi-spring-boot-starter` 的内置能力。

**Architecture:** 事务核心与 Seata 适配继续位于 `fw-plus-boot-autoconfigure` 的 `magicapi.modules.dbtx` / `magicapi.modules.seata` 包，由 `MagicAPIBaseAutoConfiguration` 直接导入。主 starter 不传递 Seata 客户端；业务需要 Seata 时自行引入 `io.seata:seata-spring-boot-starter`，自动配置通过属性和 classpath 条件启用。

**Tech Stack:** Java 8、Spring Boot 2.7.18、Spring JDBC 5.3.x、Magic-API 2.2.2、Seata 2.0.0、JUnit 5、ApplicationContextRunner、Maven

## Global Constraints

- 不保留或新增 fw-plus dbtx/Seata plugin、starter artifact、classpath marker、前端 bundle、`MagicPluginConfiguration` 或 `Plugin` 元数据。
- 唯一的 fw-plus Magic-API starter 是 `fw-plus-magicapi-spring-boot-starter`。
- 实现代码继续位于 `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx` 或 `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.seata`。
- 配置前缀为 `magic-api.dbtx`；`enabled=true`、`default-coordinator=local`、`local-enabled=true`、`seata-enabled=false`。
- 主 starter 不传递 Seata 客户端；业务通过标准 `io.seata:seata-spring-boot-starter` 和 `seata.*` 配置启用 Seata。
- 配置的默认协调器不可用时必须启动失败；非默认协调器不可用时允许启动，显式脚本调用返回 `DBTX_COORDINATOR_UNAVAILABLE`。
- local 与 Seata 可共存；一 JVM 只使用一个 Seata 客户端和一个逻辑集群，不实现脚本级 Server 切换。
- 目标 JDK 1.8，不使用 Java 9+ API；Magic-API 固定 2.2.2，Seata 固定 2.0.0。
- 直接在当前 `dev` 工作区增量迁移，保留用户改动；共享 POM 和自动配置文件只精确暂存本任务 hunk。
- 当前工作区已有旧 Task 6 未提交的 validator/test 修复，归 Task 2 迁移所有；不得作为旧 plugin 配置单独提交。

---

### Task 1: Remove temporary plugin artifacts and prove the main starter boundary

**Files:**
- Delete: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-dbtx/`
- Delete: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-seata/`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/pom.xml`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-dependencies/pom.xml`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/pom.xml`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/boot/starter/magicapi/MagicApiDbTxBuiltinClasspathTest.java`

**Interfaces:**
- Removes: artifacts `fw-plus-magicapi-plugin-dbtx` and `fw-plus-magicapi-plugin-seata` plus both marker classes.
- Preserves: `fw-plus-magicapi-spring-boot-starter` as the only fw-plus artifact consumed by local dbtx users.
- Proves: the main starter exposes `DbTxModule`, `LocalDbTxCoordinator`, and Spring `DataSourceTransactionManager` without exposing Seata as a transitive dependency.

- [ ] **Step 1: Write the main starter classpath contract test**

Add `spring-boot-starter-test` with `test` scope to the main starter so the
module can host this contract test. This test-only dependency must not change
the main starter's consumer dependency graph.

```java
@Test
void mainMagicApiStarterMustExposeBuiltInLocalDbTx() {
    assertNotNull(DbTxModule.class);
    assertNotNull(LocalDbTxCoordinator.class);
    assertNotNull(DataSourceTransactionManager.class);
}
```

Also use Maven dependency-tree verification instead of a classloading assertion for the negative dependency contract:

```bash
mvn -f fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/pom.xml \
  dependency:tree -Dincludes=io.seata:seata-spring-boot-starter
```

Expected: no Seata dependency line in the main starter tree.

- [ ] **Step 2: Run the contract before cleanup**

```bash
mvn -f fw-plus-boot/fw-plus-boot-project/pom.xml \
  -pl :fw-plus-magicapi-spring-boot-starter -am \
  -Dtest=MagicApiDbTxBuiltinClasspathTest -DfailIfNoTests=false test
```

Expected: the positive classpath test passes; dependency-tree confirms Seata is not pulled by the main starter. Record this as characterization evidence before deleting the temporary artifacts.

- [ ] **Step 3: Remove the two plugin modules and dependency-management entries**

Remove exactly these aggregator entries:

```xml
<module>fw-plus-magicapi-plugin-dbtx</module>
<module>fw-plus-magicapi-plugin-seata</module>
```

Remove exactly the two matching Boot BOM dependencies. Delete both module directories, including POMs, marker classes, and marker tests. Do not add replacement artifacts or production dependencies to the main starter; the Step 1 test-scoped dependency is allowed.

- [ ] **Step 4: Verify the cleaned reactor and classpath**

Run the commands from Steps 1-2 again, then:

```bash
rg -n "fw-plus-magicapi-plugin-(dbtx|seata)" \
  fw-plus-boot/fw-plus-boot-project
```

Expected: classpath test passes, Seata remains absent from the main starter dependency tree, and the repository scan prints no artifact/POM references. Marker and old configuration references in autoconfigure are intentionally removed by Task 2, then checked by Task 3's final scan.

- [ ] **Step 5: Commit the artifact cleanup**

Stage only both deleted module directories, the two precise shared-POM hunks, the main-starter test dependency hunk, and the new main-starter test. Confirm no Spring Bean, Java Entity, MyBatis, TinyFlow, README, frontend, or other user changes are staged.

```bash
git commit -m "refactor(magicapi): 移除 dbtx 与 Seata 临时插件"
```

### Task 2: Convert plugin-gated configuration into built-in module configuration

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/starter/DbTxProperties.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/starter/MagicAPIDbTxConfiguration.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/seata/starter/MagicAPISeataConfiguration.java`
- Delete: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/seata/starter/MagicAPISeataProperties.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/MagicAPIBaseAutoConfiguration.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/MagicApiPluginAutoConfiguration.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/starter/MagicAPIDbTxConfigurationTest.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/seata/starter/MagicAPISeataConfigurationTest.java`
- Delete: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/starter/magicapi/dbtx/DbTxPluginMarker.java`
- Delete: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/starter/magicapi/seata/SeataPluginMarker.java`

**Interfaces:**
- Produces: `@ConfigurationProperties(prefix = "magic-api.dbtx")` with `enabled`, `defaultCoordinator`, `localEnabled`, `seataEnabled`, `timeoutMs`, and `namePrefix`.
- Produces: property-gated dbtx configuration imported directly by `MagicAPIBaseAutoConfiguration`.
- Produces: class-and-property-gated Seata configuration without plugin metadata.
- Preserves: independent `SmartInitializingSingleton` default-coordinator validation, including when a user supplies a custom `DbTxModule`.

- [ ] **Step 1: Rewrite the context tests for built-in semantics and verify RED**

Replace marker-based cases with these exact cases:

```java
// magic-api.dbtx.enabled=false -> no dbtx beans
// defaults -> local/module/registry/decorator present
// local-enabled=false + default local -> startup failure
// custom DbTxModule + unavailable default -> startup failure
// default seata while unavailable -> startup failure
// seata-enabled=false -> local starts, Seata absent
// seata-enabled=true + Seata class present -> local and Seata coexist
// FilteredClassLoader("io.seata") + default local -> local starts, explicit seata unavailable
```

Use `MagicAPIBaseAutoConfiguration` plus the minimum required test beans for the direct-import integration case. Delete same-FQCN marker fixtures and every marker classloader filter.

Run:

```bash
mvn -f fw-plus-boot/fw-plus-boot-project/pom.xml \
  -pl :fw-plus-boot-autoconfigure -am \
  -Dtest=MagicAPIDbTxConfigurationTest,MagicAPISeataConfigurationTest \
  -DfailIfNoTests=false test
```

Expected RED: old marker and `magic-api.plugin.*` conditions prevent the new property-only contexts from producing the required beans.

- [ ] **Step 2: Migrate properties and dbtx configuration**

Change the property class to:

```java
@ConfigurationProperties(prefix = "magic-api.dbtx")
public class DbTxProperties {
    private boolean enabled = true;
    private String defaultCoordinator = "local";
    private boolean localEnabled = true;
    private boolean seataEnabled = false;
    private int timeoutMs = 60000;
    private String namePrefix = "magic-api-dbtx";

    public boolean isSeataEnabled() {
        return seataEnabled;
    }

    public void setSeataEnabled(boolean seataEnabled) {
        this.seataEnabled = seataEnabled;
    }
}
```

Preserve the existing JavaBean accessors for the other five properties unchanged.

For `MagicAPIDbTxConfiguration`:

- remove the marker `@ConditionalOnClass`;
- use prefix `magic-api.dbtx` for `enabled` and `local-enabled`;
- remove `implements MagicPluginConfiguration`, `Plugin` imports, and `plugin()`;
- keep the independent `SmartInitializingSingleton` validator outside the conditional module factory;
- absorb the existing uncommitted validator/test fix and do not preserve old plugin prefixes.

- [ ] **Step 3: Migrate Seata configuration and import ownership**

For `MagicAPISeataConfiguration`:

```java
@ConditionalOnClass(name = "io.seata.tm.api.GlobalTransactionContext")
@ConditionalOnProperty(prefix = "magic-api.dbtx", name = "seata-enabled", havingValue = "true")
```

Remove marker checks, `MagicAPISeataProperties`, `MagicPluginConfiguration`, `Plugin`, and `plugin()`.

Add both module configurations directly to `MagicAPIBaseAutoConfiguration`:

```java
@Import({
    // existing entries remain in their current order
    MagicAPIDbTxConfiguration.class,
    MagicAPISeataConfiguration.class
})
```

Remove only their imports and `@Import` entries from `MagicApiPluginAutoConfiguration`; preserve all user Spring Bean/plugin changes.

- [ ] **Step 4: Run built-in auto-configuration and core regression tests**

```bash
mvn -f fw-plus-boot/fw-plus-boot-project/pom.xml \
  -pl :fw-plus-boot-autoconfigure -am \
  -Dtest=MagicAPIDbTxConfigurationTest,MagicAPISeataConfigurationTest,DbTxModuleTest,DbTxAwareDataSourceTest,LocalDbTxCoordinatorTest,LocalDbTxCoordinatorH2Test,SeataDataSourceAdapterTest,SeataDbTxCoordinatorTest \
  -DfailIfNoTests=false test
```

Expected: built-in context matrix and Task 2-5 regression tests pass. If unrelated current MyBatisFlex/TinyEngine syntax or tests block full main compilation, use a documented targeted compiler/test isolation only for diagnosis; do not claim the normal Maven command passed until it actually does.

- [ ] **Step 5: Commit the built-in auto-configuration migration**

Precisely stage only Task 2 paths/hunks, including the previously uncommitted validator/test changes now adapted to built-in semantics. Confirm old `magic-api.plugin.dbtx`, `magic-api.plugin.seata`, marker, and plugin metadata references are absent from staged production/test code.

```bash
git commit -m "refactor(magicapi): 内置 dbtx 与 Seata 自动配置"
```

### Task 3: Add built-in documentation, Seata SQL, and aggregate verification

**Files:**
- Create: `docs/magicapi-dbtx.md`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/main/resources/META-INF/sql/seata/mysql-undo-log.sql`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/boot/starter/magicapi/MagicApiDbTxResourceTest.java`
- Test: all dbtx/Seata files retained from the previous implementation.

**Interfaces:**
- Documents: built-in local defaults, explicit Seata dependency, standard `seata.*` server configuration, both script overloads, best-effort local outcomes, nesting/thread restrictions, hot-reload restriction, and `undo_log` requirement.
- Produces: main-starter classpath SQL resource `META-INF/sql/seata/mysql-undo-log.sql`.

- [ ] **Step 1: Write the SQL resource availability test and verify RED**

```java
@Test
void mysqlUndoLogMustShipWithTheMainMagicApiStarter() throws Exception {
    ClassPathResource resource = new ClassPathResource(
            "META-INF/sql/seata/mysql-undo-log.sql");
    assertTrue(resource.exists());
    String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    assertTrue(sql.contains("CREATE TABLE `undo_log`"));
    assertTrue(sql.contains("UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)"));
}
```

Run:

```bash
mvn -f fw-plus-boot/fw-plus-boot-project/pom.xml \
  -pl :fw-plus-magicapi-spring-boot-starter -am \
  -Dtest=MagicApiDbTxResourceTest -DfailIfNoTests=false test
```

Expected RED: resource is absent.

- [ ] **Step 2: Add the Seata 2.0 MySQL undo table**

Use the Seata 2.0 MySQL schema with:

- primary key `id`;
- unique key `ux_undo_log (xid, branch_id)`;
- `rollback_info`, `context`, `log_status`, `log_created`, `log_modified`;
- index on `log_created`.

Keep the SQL in the main Magic-API starter resource path; do not create a Seata plugin/starter module.

- [ ] **Step 3: Write the built-in usage documentation**

`docs/magicapi-dbtx.md` must show:

```yaml
magic-api:
  dbtx:
    enabled: true
    default-coordinator: local
    local-enabled: true
    seata-enabled: false
```

Document that Seata users directly add `io.seata:seata-spring-boot-starter`, then configure one logical cluster through standard `seata.*`. Include both `dbtx.transaction` overloads, local `DBTX_HEURISTIC_MIXED` semantics, `undo_log`, nested subset rules, synchronous callback restriction, and used-proxy hot-removal rejection.

- [ ] **Step 4: Run focused, starter, and aggregate verification**

```bash
mvn -f fw-plus-boot/fw-plus-boot-project/pom.xml \
  -pl :fw-plus-boot-autoconfigure,:fw-plus-magicapi-spring-boot-starter -am \
  -Dtest=MagicAPIDbTxConfigurationTest,MagicAPISeataConfigurationTest,DbTxModuleTest,DbTxAwareDataSourceTest,LocalDbTxCoordinatorTest,LocalDbTxCoordinatorH2Test,SeataDataSourceAdapterTest,SeataDbTxCoordinatorTest,MagicApiDbTxBuiltinClasspathTest,MagicApiDbTxResourceTest \
  -DfailIfNoTests=false test

./fw-plus-core/mvnw -f fw-plus-sample/pom.xml validate

rg -n "fw-plus-magicapi-plugin-(dbtx|seata)|DbTxPluginMarker|SeataPluginMarker|magic-api\.plugin\.(dbtx|seata)" \
  fw-plus-boot docs/magicapi-dbtx.md

git diff --check
```

Expected: focused tests pass, main starter tests pass, sample aggregation validates, old artifact/marker/config references are absent from production code and the active user documentation, and `git diff --check` is clean. Historical design/implementation plans under `docs/superpowers` intentionally describe the removed names and are excluded from this active-reference scan. Report the existing Maven Wrapper metadata issue if the sample command cannot start; use the repository's verified system-Maven equivalent without claiming the Wrapper command passed.

- [ ] **Step 5: Commit documentation and SQL**

Stage only `docs/magicapi-dbtx.md`, the SQL resource, and its resource test.

```bash
git commit -m "docs(magicapi): 说明内置 dbtx 与 Seata 配置"
```
