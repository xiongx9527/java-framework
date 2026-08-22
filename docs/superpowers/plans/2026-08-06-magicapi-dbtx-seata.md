# Magic-API DBTX and Seata Plugins Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Magic-API 内部 JDBC 数据源实现独立 dbtx 本地事务插件和独立 Seata AT 插件，并允许脚本使用可配置默认协调器或显式选择 `local`/`seata`。

**Architecture:** `DbTxModule` 负责脚本 API、嵌套上下文和协调器路由；`LocalDbTxCoordinator` 使用每个 `DataSourceNode` 的 Spring 本地事务管理器；Seata 插件通过 dbtx 的数据源适配 SPI 延迟创建 `DataSourceProxy`，并使用单 JVM、单逻辑集群的 Seata 客户端执行全局事务。Magic-API 运行时数据源统一由 `DbTxAwareDataSource` 包装，以限制未声明数据源访问并正确管理连接池生命周期。

**Tech Stack:** Java 8、Spring Boot 2.7.18、Spring JDBC 5.3.x、Magic-API 2.2.2、Seata 2.0.0、JUnit 5、Mockito、H2、Maven Wrapper

## Global Constraints

- 所有实现代码放在 `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx` 或 `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.seata`。
- 两个独立 starter 只承载 marker、依赖和插件级说明；不得把实现放入 `magicapi.plugin` 包。
- 目标 JDK 为 1.8，不使用 `List.of`、`Map.of`、records、模式匹配或其他 Java 9+ API。
- Magic-API 固定使用仓库管理的 2.2.2，不修改 `org.ssssssss` 依赖版本或内部 Nexus 配置。
- `dbtx.transaction(keys, callback)` 使用可配置默认协调器；`dbtx.transaction(mode, keys, callback)` 显式覆盖默认值。
- 默认协调器为 `local`；默认值指向禁用或不可用协调器时必须启动失败，不允许静默降级。
- local 与 seata 可在同一应用实例并存；相同模式的子集嵌套加入外层，扩大范围或跨模式嵌套失败。
- Seata 采用一 JVM 一个客户端、一个逻辑集群，可配置多个 Server 节点；不实现脚本级独立集群切换。
- 回调只支持同步同线程执行；所有 ThreadLocal 和 Spring 事务资源必须在所有结果路径清理。
- 直接在当前 `dev` 工作区增量实现，保留现有用户改动；共享 POM 和自动配置文件只追加必要项，不排序或格式化整段。

---

### Task 1: Scaffold the two independent plugin starters

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-dbtx/pom.xml`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-dbtx/src/main/java/com/xiongx9527/fw/plus/boot/starter/magicapi/dbtx/DbTxPluginMarker.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-dbtx/src/test/java/com/xiongx9527/fw/plus/boot/starter/magicapi/dbtx/DbTxPluginRegistrationTest.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-seata/pom.xml`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-seata/src/main/java/com/xiongx9527/fw/plus/boot/starter/magicapi/seata/SeataPluginMarker.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-seata/src/test/java/com/xiongx9527/fw/plus/boot/starter/magicapi/seata/SeataPluginDependencyTest.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/pom.xml`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-dependencies/pom.xml`

**Interfaces:**
- Produces: marker classes `DbTxPluginMarker` and `SeataPluginMarker` used by Task 6 conditional configuration.
- Produces: artifacts `fw-plus-magicapi-plugin-dbtx` and `fw-plus-magicapi-plugin-seata`; the Seata artifact depends on dbtx plus `io.seata:seata-spring-boot-starter`.

- [ ] **Step 1: Write marker and dependency contract tests**

```java
@Test
void markerMustBeLoadableFromDbTxPlugin() {
    assertEquals("DbTxPluginMarker", DbTxPluginMarker.class.getSimpleName());
}

@Test
void seataPluginMustExposeBothMarkers() {
    assertNotNull(SeataPluginMarker.class);
    assertNotNull(DbTxPluginMarker.class);
    assertNotNull(io.seata.tm.api.GlobalTransactionContext.class);
}
```

- [ ] **Step 2: Run the new starter tests and verify they fail**

Run:

```bash
./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml \
  -pl :fw-plus-magicapi-plugin-dbtx,:fw-plus-magicapi-plugin-seata -am test
```

Expected: Maven fails because the modules and marker classes do not exist.

- [ ] **Step 3: Add the two modules, markers, and dependency declarations**

The dbtx starter inherits `fw-plus-magicapi-plugin`. The Seata starter additionally declares:

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.boot</groupId>
    <artifactId>fw-plus-magicapi-plugin-dbtx</artifactId>
</dependency>
<dependency>
    <groupId>io.seata</groupId>
    <artifactId>seata-spring-boot-starter</artifactId>
</dependency>
```

Add both child modules to the plugin aggregator and both artifacts with `${project.version}` to the Boot BOM. Marker classes are empty `final` classes with private constructors.

- [ ] **Step 4: Run the starter tests**

Run the command from Step 2.

Expected: both marker/dependency tests pass; no unrelated plugin files are staged.

- [ ] **Step 5: Commit the starter skeleton**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-dbtx \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-seata \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/pom.xml \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-dependencies/pom.xml
git commit -m "feat(magicapi): 添加 dbtx 与 Seata 插件骨架"
```

### Task 2: Implement dbtx contracts, routing, and nested context

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/DbTxModule.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/coordinator/DbTxCoordinator.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/coordinator/DbTxCoordinatorRegistry.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/model/DbTxDefinition.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/model/DbTxContext.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/model/DbTxContextHolder.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/exception/DbTxException.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/exception/DbTxErrorCode.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/starter/DbTxProperties.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/DbTxModuleTest.java`

**Interfaces:**
- Produces: `DbTxCoordinator#getName()` and `Object execute(DbTxDefinition, DbTxCallback) throws Throwable`.
- Produces: immutable `DbTxDefinition(String coordinator, List<String> dataSourceKeys, int timeoutMs, String name)`.
- Produces: `DbTxContextHolder.current()`, `open(DbTxDefinition)`, and `clear()` for Tasks 3-5.
- Consumes: `java.util.function.Function<?, ?>` from Magic-Script calls.

- [ ] **Step 1: Write failing module routing and nesting tests**

Cover these exact cases with fake coordinators that count executions:

```java
assertEquals("local", module.resolveCoordinator(null));
assertEquals("seata", module.resolveCoordinator(" SEATA "));
assertThrows(DbTxException.class,
        () -> module.transaction("unknown", Arrays.asList("a"), callback));
```

Also verify default configuration override, duplicate/missing/blank keys, same-mode subset nesting, rollback-only after a caught inner exception, scope expansion rejection, cross-mode rejection, callback return passthrough, and ThreadLocal cleanup.

- [ ] **Step 2: Run `DbTxModuleTest` and verify it fails**

```bash
./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml \
  -pl :fw-plus-boot-autoconfigure -am \
  -Dtest=DbTxModuleTest test
```

Expected: compilation fails because dbtx contracts do not exist.

- [ ] **Step 3: Implement the minimal contracts and module**

Use the exact public signatures:

```java
@MagicModule("dbtx")
public class DbTxModule {
    public Object transaction(Collection<String> keys, Function<?, ?> callback)
            throws Throwable;
    public Object transaction(String coordinator, Collection<String> keys,
                              Function<?, ?> callback) throws Throwable;
}

public interface DbTxCallback {
    Object execute() throws Throwable;
}

public interface DbTxCoordinator {
    String getName();
    Object execute(DbTxDefinition definition, DbTxCallback callback) throws Throwable;
}
```

Normalize and sort keys once in the outer call. Nested calls may only use the same coordinator and a subset of the outer key set. Any inner throwable marks the outer context rollback-only before rethrowing.

- [ ] **Step 4: Run `DbTxModuleTest` and verify it passes**

Run the command from Step 2.

Expected: all routing, validation, nesting, rollback-only, and cleanup tests pass.

- [ ] **Step 5: Commit the dbtx contracts**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/DbTxModuleTest.java
git commit -m "feat(magicapi): 定义 dbtx 脚本与协调器契约"
```

### Task 3: Wrap Magic-API runtime data sources and enforce declared keys

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/datasource/MagicDataSourceDecorator.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/datasource/DbTxDataSourceAdapter.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/datasource/DbTxAwareDataSource.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/datasource/DbTxDataSourceDecorator.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/datasource/service/MyDataSourceMagicDynamicRegistry.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/MagicAPIBaseAutoConfiguration.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/datasource/DbTxAwareDataSourceTest.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/datasource/service/MyDataSourceMagicDynamicRegistryDecoratorTest.java`

**Interfaces:**
- Consumes: `DbTxContextHolder.current()` from Task 2.
- Produces: `MagicDataSourceDecorator#decorate(String, DataSource)` and `beforeRemove(String, DataSource)`.
- Produces: `DbTxDataSourceAdapter#getCoordinator()`, `prepare(String, DataSource)`, and `close(DataSource)` for Task 5.
- Produces: `DbTxAwareDataSource#prepare(String coordinator)` used before Seata begin.

- [ ] **Step 1: Write failing wrapper and registry integration tests**

Verify that no context delegates to raw DataSource, local context delegates to raw, undeclared key throws `SQLException` before raw access, an adapter is prepared only once, duplicate wrapping is avoided, `close()` closes the raw pool, decorator order is stable, and registry build failure does not replace the old node.

Use a proxy DataSource with counters:

```java
assertSame(rawConnection, aware.getConnection());
contextHolder.open(definition("local", "a"));
assertThrows(SQLException.class, () -> datasourceForKeyB.getConnection());
```

- [ ] **Step 2: Run the two datasource tests and verify they fail**

```bash
./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml \
  -pl :fw-plus-boot-autoconfigure -am \
  -Dtest=DbTxAwareDataSourceTest,MyDataSourceMagicDynamicRegistryDecoratorTest test
```

Expected: compilation fails because the decorator and wrapper do not exist.

- [ ] **Step 3: Implement wrapper, adapter SPI, and registry hook**

Apply decorators after `buildDataSource` and before `magicDynamicDataSource.put`. Pass an ordered `List<MagicDataSourceDecorator>` from `MagicAPIBaseAutoConfiguration`; an empty list preserves current behavior. Before replacing or deleting a node, call `beforeRemove` on all decorators. Log registration errors instead of silently swallowing them, while preserving the existing boolean return contract.

`DbTxAwareDataSource#getConnection` must use the active context to validate the key and select raw/local or the prepared adapter delegate. Synchronize lazy delegate creation and close the raw DataSource exactly once.

- [ ] **Step 4: Run datasource tests and existing db browser tests**

```bash
./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml \
  -pl :fw-plus-boot-autoconfigure -am \
  -Dtest=DbTxAwareDataSourceTest,MyDataSourceMagicDynamicRegistryDecoratorTest,DefaultDbBrowserServiceTest test
```

Expected: all selected tests pass and existing db browser access still resolves nodes.

- [ ] **Step 5: Commit the data source extension**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/datasource \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/datasource/service/MyDataSourceMagicDynamicRegistry.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/MagicAPIBaseAutoConfiguration.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/datasource \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/datasource/service/MyDataSourceMagicDynamicRegistryDecoratorTest.java
git commit -m "feat(magicapi): 接入 dbtx 动态数据源包装"
```

### Task 4: Implement the local multi-data-source coordinator

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/coordinator/LocalDbTxCoordinator.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/model/DbTxOutcome.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/coordinator/LocalDbTxCoordinatorTest.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/coordinator/LocalDbTxCoordinatorH2Test.java`

**Interfaces:**
- Consumes: `DbTxCoordinator`, `DbTxDefinition`, `DbTxContextHolder`, and `MagicDynamicDataSource`.
- Produces: coordinator name `local` and structured commit outcome fields used by `DbTxException`.

- [ ] **Step 1: Write failing fault-injection and two-H2 tests**

Create two independent H2 DataSources, register keys `a` and `b`, create identical tables, and verify:

```java
coordinator.execute(definition("a", "b"), () -> {
    jdbcA.update("insert into tx_test(id) values (1)");
    jdbcB.update("insert into tx_test(id) values (1)");
    return "ok";
});
assertEquals(1, count(jdbcA));
assertEquals(1, count(jdbcB));
```

Add failure fixtures for second begin failure, callback failure, first/second reverse commit failure, rollback failure, ambient transaction detection, and resource cleanup.

- [ ] **Step 2: Run local coordinator tests and verify they fail**

```bash
./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml \
  -pl :fw-plus-boot-autoconfigure -am \
  -Dtest=LocalDbTxCoordinatorTest,LocalDbTxCoordinatorH2Test test
```

Expected: compilation fails because `LocalDbTxCoordinator` is absent.

- [ ] **Step 3: Implement phased begin, callback, commit, and rollback**

Open transactions in sorted key order and commit/rollback in reverse order. Keep callback, commit, and rollback in separate control-flow blocks. On callback failure, rethrow the original throwable and attach rollback failures with `addSuppressed`. On commit failure, mark the failing key `unknown`, leave already committed keys in `committed`, roll back only transactions not yet submitted for commit, and throw `DBTX_HEURISTIC_MIXED` when any key committed.

- [ ] **Step 4: Run local coordinator tests**

Run the command from Step 2.

Expected: H2 commit/rollback and all injected phase failures pass with no bound Spring resources afterward.

- [ ] **Step 5: Commit the local coordinator**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/coordinator/LocalDbTxCoordinator.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/model/DbTxOutcome.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/coordinator
git commit -m "feat(magicapi): 实现 dbtx 本地多数据源事务"
```

### Task 5: Implement the Seata data source adapter and coordinator

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/seata/datasource/SeataDataSourceAdapter.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/seata/coordinator/SeataTransaction.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/seata/coordinator/SeataTransactionFactory.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/seata/coordinator/DefaultSeataTransactionFactory.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/seata/coordinator/SeataDbTxCoordinator.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/seata/datasource/SeataDataSourceAdapterTest.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/seata/coordinator/SeataDbTxCoordinatorTest.java`

**Interfaces:**
- Implements: `DbTxDataSourceAdapter` with coordinator name `seata`.
- Implements: `DbTxCoordinator` with coordinator name `seata`.
- Consumes: `DbTxAwareDataSource#prepare("seata")` for every participant before global begin.
- Produces: testable `SeataTransaction` abstraction exposing role, begin, commit, rollback, XID, and global status.

- [ ] **Step 1: Write failing adapter and coordinator tests**

Verify lazy single proxy creation, double-proxy unwrapping, unsupported JDBC failure before callback, Launcher begin/commit, Launcher callback rollback, Participant no begin/commit/rollback ownership, rollback failure suppression, commit failure without blind rollback, XID/status propagation, and used-proxy hot-removal rejection.

```java
when(transaction.isLauncher()).thenReturn(true);
assertEquals("ok", coordinator.execute(definition, () -> "ok"));
verify(transaction).begin(60000, "magic-api-dbtx-test");
verify(transaction).commit();
verify(transaction, never()).rollback();
```

- [ ] **Step 2: Run Seata tests and verify they fail**

```bash
./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml \
  -pl :fw-plus-boot-autoconfigure -am \
  -Dtest=SeataDataSourceAdapterTest,SeataDbTxCoordinatorTest test
```

Expected: compilation fails because Seata module classes do not exist.

- [ ] **Step 3: Implement the Seata adapter and coordinator**

`SeataDataSourceAdapter` creates `new DataSourceProxy(raw)` only when a participating wrapper calls `prepare("seata")`. It must reject unsupported databases before global begin, avoid wrapping an existing `SeataDataSourceProxy`, and track whether a proxy was initialized so `beforeRemove` can reject unsafe hot replacement.

`SeataDbTxCoordinator` prepares all participants before obtaining a transaction. Launcher owns begin/commit/rollback; Participant joins the existing XID and never ends it. A commit exception is wrapped with XID and the best available global status, without a follow-up rollback.

- [ ] **Step 4: Run Seata tests**

Run the command from Step 2.

Expected: all adapter lifecycle and Launcher/Participant tests pass without a live Seata Server.

- [ ] **Step 5: Commit the Seata implementation**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/seata \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/seata
git commit -m "feat(magicapi): 实现 Seata 全局事务协调器"
```

### Task 6: Wire conditional auto-configuration and validate service configuration

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/starter/MagicAPIDbTxConfiguration.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/seata/starter/MagicAPISeataProperties.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/seata/starter/MagicAPISeataConfiguration.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/MagicApiPluginAutoConfiguration.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/starter/MagicAPIDbTxConfigurationTest.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/seata/starter/MagicAPISeataConfigurationTest.java`
- Test fixture: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/starter/magicapi/dbtx/DbTxPluginMarker.java`
- Test fixture: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/starter/magicapi/seata/SeataPluginMarker.java`

**Interfaces:**
- Consumes: both marker class names, dbtx coordinator registry, local coordinator, Seata coordinator, and adapter.
- Produces: validated properties `magic-api.plugin.dbtx.*` and `magic-api.plugin.seata.enabled`.

- [ ] **Step 1: Write failing ApplicationContextRunner tests**

Cover these contexts:

- no dbtx marker: no dbtx beans;
- dbtx marker and default local: module and local coordinator present;
- disabled local with default local: startup failure;
- default seata without Seata plugin: startup failure;
- Seata plugin present and enabled: Seata coordinator/adapter present;
- `magic-api.plugin.seata.enabled=false`: local remains, Seata coordinator absent;
- `FilteredClassLoader("io.seata")`: dbtx local context starts, explicit Seata unavailable;
- two starter markers: no duplicate module bean.

- [ ] **Step 2: Run auto-configuration tests and verify they fail**

```bash
./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml \
  -pl :fw-plus-boot-autoconfigure -am \
  -Dtest=MagicAPIDbTxConfigurationTest,MagicAPISeataConfigurationTest test
```

Expected: compilation fails because the new configuration classes are absent.

- [ ] **Step 3: Implement conditional configuration and startup validation**

Use string-based marker conditions to avoid a compile-time dependency from autoconfigure to child starters:

```java
@ConditionalOnClass(name =
    "com.xiongx9527.fw.plus.boot.starter.magicapi.dbtx.DbTxPluginMarker")
```

Seata configuration additionally requires `io.seata.tm.api.GlobalTransactionContext`. Add both configuration classes to the existing `MagicApiPluginAutoConfiguration` import list without reordering unrelated entries. Validate only the configured default coordinator at startup; a non-default unavailable Seata coordinator remains a clear runtime error.

Both configuration classes implement `MagicPluginConfiguration` and return metadata without a console bundle:

```java
@Override
public Plugin plugin() {
    return new Plugin("dbtx"); // Seata configuration returns "seata"
}
```

Use the two same-FQCN test marker fixtures only inside the autoconfigure test source set so `@ConditionalOnClass(name=...)` can be tested without creating a production dependency cycle. Use `FilteredClassLoader` for the marker-absent and Seata-class-absent cases.

- [ ] **Step 4: Run auto-configuration and registration tests**

```bash
./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml \
  -pl :fw-plus-boot-autoconfigure,:fw-plus-magicapi-plugin-dbtx,:fw-plus-magicapi-plugin-seata -am test
```

Expected: new auto-configuration and starter tests pass.

- [ ] **Step 5: Commit auto-configuration**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/starter \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/seata/starter \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/MagicApiPluginAutoConfiguration.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/dbtx/starter \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/seata/starter
git commit -m "feat(magicapi): 装配 dbtx 与 Seata 插件"
```

### Task 7: Add plugin documentation, SQL, and final verification

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-dbtx/README.md`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-seata/README.md`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-seata/src/main/resources/META-INF/sql/seata/mysql-undo-log.sql`
- Test: all files added or changed in Tasks 1-6.

**Interfaces:**
- Documents: both script overloads, configuration defaults, local best-effort semantics, Seata single-client boundary, multi-node service list, `undo_log`, nested constraints, async limitation, and hot-reload restriction.
- Produces: standalone SQL for the Seata AT MySQL `undo_log` table.

- [ ] **Step 1: Write documentation contract assertions**

Extend the starter tests to read each README and assert it contains these exact contract tokens:

```java
assertTrue(readme.contains("dbtx.transaction"));
assertTrue(readme.contains("default-coordinator"));
assertTrue(readme.contains("DBTX_HEURISTIC_MIXED"));
assertTrue(readme.contains("tx-service-group"));
assertTrue(readme.contains("undo_log"));
assertTrue(readme.contains("一个 JVM"));
```

- [ ] **Step 2: Run starter tests and verify documentation assertions fail**

```bash
./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml \
  -pl :fw-plus-magicapi-plugin-dbtx,:fw-plus-magicapi-plugin-seata -am test
```

Expected: tests fail because README and SQL resources are absent.

- [ ] **Step 3: Write README files and MySQL undo SQL**

Document direct Server configuration with one logical cluster and comma-separated HA nodes:

```yaml
seata:
  application-id: ${SEATA_APPLICATION_ID:fw-plus-magicapi}
  tx-service-group: ${SEATA_TX_SERVICE_GROUP:magicapi_tx_group}
  service:
    vgroup-mapping:
      magicapi_tx_group: ${SEATA_CLUSTER:default}
    grouplist:
      default: ${SEATA_SERVER_ADDR:127.0.0.1:8091}
```

Use the Seata 2.0 MySQL `undo_log` schema with primary key `id`, unique `xid` plus `branch_id`, rollback context, status, log timestamps, and `log_created` index.

- [ ] **Step 4: Run focused and aggregate verification**

```bash
./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml \
  -pl :fw-plus-boot-autoconfigure,:fw-plus-magicapi-plugin-dbtx,:fw-plus-magicapi-plugin-seata -am test

./fw-plus-core/mvnw -f fw-plus-sample/pom.xml validate

git diff --check
```

Expected: focused tests pass, sample aggregation validates, and `git diff --check` prints no errors. If external Seata/MySQL are unavailable, report that real AT end-to-end verification remains an environment-gated test boundary; do not claim it passed.

- [ ] **Step 5: Review final scope and commit documentation**

Confirm `git status --short` contains no accidentally staged `node_modules`, frontend `dist`, assets, TinyFlow files, README changes outside the two new plugins, or other user work. Then commit only the two README files, SQL resource, and any documentation contract test changes:

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-dbtx/README.md \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-seata/README.md \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-seata/src/main/resources/META-INF/sql/seata/mysql-undo-log.sql
git commit -m "docs(magicapi): 补充 dbtx 与 Seata 插件说明"
```
