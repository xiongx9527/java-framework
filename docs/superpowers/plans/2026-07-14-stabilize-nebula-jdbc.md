---
change: stabilize-nebula-jdbc
design-doc: docs/superpowers/specs/2026-07-14-stabilize-nebula-jdbc-design.md
base-ref: 85908d71d9146167f277cae498c5d0954d986e25
---

# stabilize-nebula-jdbc Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按 Design Doc `docs/superpowers/specs/2026-07-14-stabilize-nebula-jdbc-design.md` 稳定 `fw-plus-nebula-jdbc` 的配置解析、Nebula client 访问边界、资源生命周期、语句分类和 ResultSet 游标行为。

**Architecture:** 采用 Design Doc 中“全面 façade + 生命周期管理器”方案：公开 `com.vesoft.nebula.jdbc` JDBC 入口保持兼容，内部新增配置、client façade、生命周期、语句分类、ResultSet 游标等小组件。所有高风险逻辑先通过不依赖真实 NebulaGraph 的单元测试固定行为，再最小改造现有 `NebulaConnection`、`NebulaStatementImpl`、`NebulaResultSet` 使用这些组件。

**Tech Stack:** Java 1.8、Maven、JUnit、Mockito、NebulaGraph Java client、JDBC `java.sql.*`。

## Global Constraints

- 仅修改 `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc` 及其测试代码。
- 保持 `com.vesoft.nebula.jdbc` 对外入口、`java.sql.*` 接口实现方法签名和 `jdbc:nebula://...` URL 格式兼容。
- 不实现 SQL 转 nGQL。
- 不实现 MySQL wire protocol 或独立网关服务。
- 不修改 Boot、Cloud、AI、Product 层。
- 不要求真实 NebulaGraph 集成测试。
- 不升级 Nebula Java client 大版本。
- 不追求完整 JDBC 规范覆盖，只修复当前模块基础稳定性问题。
- 测试不依赖真实 NebulaGraph，不引入 Testcontainers。
- 每个 OpenSpec task 完成验收后再勾选 `openspec/changes/stabilize-nebula-jdbc/tasks.md`；本计划阶段不勾选任务。
- Comet build 阶段每个 task 验收后应立即按任务边界提交，不积攒多个任务一起提交。

---

## 参考依据

- 技术设计：`docs/superpowers/specs/2026-07-14-stabilize-nebula-jdbc-design.md`
- OpenSpec tasks：`openspec/changes/stabilize-nebula-jdbc/tasks.md`
- 当前 base-ref：`85908d71d9146167f277cae498c5d0954d986e25`

## 文件结构规划

本 change 只在 Nebula JDBC 模块内工作：

- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/pom.xml`
  - 负责补充模块级测试依赖，优先使用父 POM/BOM 已管理版本；只加入测试 scope，不影响运行时依赖。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaConnection.java`
  - 保持 JDBC `Connection` 实现入口；迁移为使用配置模型和 client façade；保留 `createStatement`、`prepareStatement` 等公开行为。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaStatementImpl.java`
  - 保持 JDBC `Statement` 实现入口；替换轻量 nGQL 分类逻辑；接入生命周期检查。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaPreparedStatementImpl.java`
  - 仅在必要时调整继承自 `NebulaStatementImpl` 的生命周期/分类协作；不改变参数绑定边界。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaResultSet.java`
  - 保持 JDBC `ResultSet` 实现入口；接入 ResultSet 游标与生命周期组件；修复 before first、after last、`wasNull()` 边界。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/config/NebulaConnectionConfig.java`
  - 类型化、约定不可变的连接配置对象。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/config/NebulaConnectionConfigParser.java`
  - 复用 `NebulaJdbcUrlParser.parse(url, properties)` 并集中解析整数参数。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/client/NebulaClientFacade.java`
  - 隔离 `NebulaPool`、`Session`、原生 `ResultSet` 调用；统一 Nebula client 异常转 `SQLException`。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/client/NebulaPoolFactory.java`
  - 为 façade 提供可替换的 pool 创建边界，便于单元测试注入 mock。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/lifecycle/NebulaResourceLifecycle.java`
  - 统一资源关闭幂等和关闭后访问检查。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaStatementClassifier.java`
  - 轻量 nGQL query/update 分类，不做 SQL 或完整 nGQL parser。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/result/NebulaResultSetCursor.java`
  - 管理当前行号、可读状态、最近列访问和 `wasNull()` 语义。
- Create test package: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/...`
  - 按组件分别新增本地单元测试，不连接真实 NebulaGraph。

## 执行顺序总览

OpenSpec tasks 映射：

- Task 1 覆盖 OpenSpec 1.1–1.5：先建立测试基线与失败测试。
- Task 2 覆盖 OpenSpec 2.1–2.3：实现配置模型、整数解析与连接初始化单一事实源。
- Task 3 覆盖 OpenSpec 3.1：实现生命周期管理并接入 Connection/Statement/ResultSet。
- Task 4 覆盖 OpenSpec 3.2：实现 Statement 轻量分类器并接入。
- Task 5 覆盖 OpenSpec 3.3：实现 ResultSet 游标边界与 `wasNull()` 语义。
- Task 6 覆盖 OpenSpec 4.1–4.3：模块验证、范围检查、勾选 OpenSpec tasks、准备进入下一阶段。

### Task 1: 测试基线与覆盖补强

**对应 OpenSpec tasks:** 1.1、1.2、1.3、1.4、1.5

**目标:** 补齐本模块单元测试依赖并写出能暴露当前问题的失败测试，不连接真实 NebulaGraph。

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/pom.xml`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/config/NebulaConnectionConfigParserTest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/statement/NebulaStatementClassifierTest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/lifecycle/NebulaResourceLifecycleTest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/result/NebulaResultSetCursorTest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/client/NebulaClientFacadeTest.java`

**Interfaces:**
- Consumes: 当前 `NebulaJdbcUrlParser`、`NebulaPropertyKey`、`NebulaConnection`、`NebulaStatementImpl`、`NebulaResultSet` 行为。
- Produces: 后续任务要实现的测试期望：
  - `NebulaConnectionConfigParser.parse(String url, Properties properties): NebulaConnectionConfig`
  - `NebulaStatementClassifier.checkQuery(String ngql): void`
  - `NebulaStatementClassifier.checkUpdate(String ngql): void`
  - `NebulaResourceLifecycle.close(ThrowingCloseAction closeAction): void`
  - `NebulaResourceLifecycle.checkOpen(String resourceName): void`
  - `NebulaResultSetCursor.next(): boolean`
  - `NebulaResultSetCursor.checkReadable(): void`
  - `NebulaResultSetCursor.recordColumnAccess(ValueWrapper value): void`
  - `NebulaResultSetCursor.wasNull(): boolean`
  - `NebulaClientFacade.open(NebulaConnectionConfig config): void`
  - `NebulaClientFacade.executeNgql(String ngql): com.vesoft.nebula.client.graph.data.ResultSet`

**验证方式:**
- 运行 `cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaConnectionConfigParserTest,NebulaStatementClassifierTest,NebulaResourceLifecycleTest,NebulaResultSetCursorTest,NebulaClientFacadeTest`
- 预期：新增测试因目标类或目标行为尚不存在而失败，且不尝试连接真实 NebulaGraph。

**提交边界建议:** `test: add nebula jdbc stability baseline tests`

- [x] **Step 1: 确认测试框架依赖**

检查父 POM 是否已管理 JUnit/Mockito。若未能直接在模块运行测试，则在模块 `pom.xml` 的 `<dependencies>` 下新增测试依赖；优先添加 `junit:junit` 与 `org.mockito:mockito-core` 的 `test` scope，避免引入运行时依赖。

建议依赖片段：

```xml
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>
```

如果父级未管理版本导致 Maven 报缺失版本，再回查 `fw-plus-core/fw-plus-project/fw-plus-parent/pom.xml` 与 `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`，按项目已有测试依赖版本补充，禁止升级 Nebula Java client 大版本。

- [x] **Step 2: 为配置解析写失败测试**

在 `NebulaConnectionConfigParserTest.java` 中覆盖：URL query 合并、`Properties` 合并、`graphspace`、`user`、`password`、`graphAddress`、整数默认值、字符串数字、`Number`、空白字符串、非数字字符串。

测试用例名称建议：

```java
@Test
public void parseShouldMergeUrlQueryAndInputPropertiesIntoTypedConfig() throws Exception

@Test
public void parseShouldSupportStringNumberAndTrimSpacesForPoolOptions() throws Exception

@Test
public void parseShouldSupportNumberInstancesForPoolOptions() throws Exception

@Test
public void parseShouldUseDefaultsWhenPoolOptionsMissing() throws Exception

@Test
public void parseShouldRejectBlankAndNonNumericPoolOptionsWithFieldName() throws Exception
```

关键断言：

```java
assertEquals("test_space", config.getGraphspace());
assertEquals("root", config.getUsername());
assertEquals("nebula", config.getPassword());
assertEquals(1, config.getMinConnsSize());
assertEquals(10, config.getMaxConnsSize());
assertTrue(exception.getMessage().contains("minConnsSize"));
```

- [x] **Step 3: 为 Statement 分类写失败测试**

在 `NebulaStatementClassifierTest.java` 中覆盖大小写、换行、多空白、update 关键字拒绝、未知/空白/null 拒绝。

测试用例名称建议：

```java
@Test
public void checkQueryShouldAcceptMixedCaseAndWhitespaceQueryNgql() throws Exception

@Test
public void checkUpdateShouldAcceptMixedCaseAndWhitespaceUpdateNgql() throws Exception

@Test
public void checkQueryShouldRejectUpdateKeyword() throws Exception

@Test
public void checkUpdateShouldRejectQueryAndUnknownStatements() throws Exception

@Test
public void classifierShouldRejectNullBlankAndUnknownNgql() throws Exception
```

关键输入：

```java
"  MATCH\n(v)\tRETURN v  "
"\n  INSERT   VERTEX person(name) VALUES \"p1\":(\"tom\")"
"create tag person(name string)"
"select * from person"
"   "
null
```

- [x] **Step 4: 为生命周期写失败测试**

在 `NebulaResourceLifecycleTest.java` 中固定关闭幂等、首次关闭执行一次、关闭后访问抛 `SQLException`、首次关闭底层异常能传播。

测试用例名称建议：

```java
@Test
public void closeShouldExecuteActionOnlyOnce() throws Exception

@Test
public void checkOpenShouldThrowSQLExceptionAfterClose() throws Exception

@Test
public void closeShouldExposeFirstCloseFailureAsSQLException() throws Exception
```

关键断言：

```java
assertEquals(1, closeCount.get());
assertTrue(lifecycle.isClosed());
assertTrue(exception.getMessage().contains("Connection"));
```

- [x] **Step 5: 为 ResultSet 游标写失败测试**

在 `NebulaResultSetCursorTest.java` 中固定 before first、after last、最近列访问、未读取列前 `wasNull()` 的行为。按 Design Doc 优先选择“未读取列前调用 `wasNull()` 抛明确 `SQLException`”。

测试用例名称建议：

```java
@Test
public void checkReadableShouldRejectBeforeFirstAndAfterLast() throws Exception

@Test
public void nextShouldMoveCursorWithinRows() throws Exception

@Test
public void wasNullShouldReturnLastColumnNullStateAfterColumnAccess() throws Exception

@Test
public void wasNullShouldRejectCallsBeforeColumnAccess() throws Exception
```

说明：`ValueWrapper` 如难以构造真实 null 值，可先用 Mockito mock：

```java
ValueWrapper nullValue = mock(ValueWrapper.class);
when(nullValue.isNull()).thenReturn(true);
```

- [x] **Step 6: 为 Nebula client façade 写失败测试**

在 `NebulaClientFacadeTest.java` 中通过 Mockito mock `NebulaPool`、`Session`、Nebula 原生 `ResultSet`，覆盖 pool 初始化参数来自 `NebulaConnectionConfig`、session 使用配置用户名密码、`use <graphspace>` 失败转 `SQLException`、`executeNgql` 异常转 `SQLException`、重复 close 稳定。

测试用例名称建议：

```java
@Test
public void openShouldInitializePoolGetSessionAndUseGraphspaceFromConfig() throws Exception

@Test
public void openShouldThrowSQLExceptionWhenUseGraphspaceFails() throws Exception

@Test
public void executeNgqlShouldWrapNebulaClientExceptionAsSQLException() throws Exception

@Test
public void closeShouldReleaseSessionAndClosePoolOnlyOnce() throws Exception
```

- [x] **Step 7: 运行新增测试确认失败原因可解释**

Run:

```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core
./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaConnectionConfigParserTest,NebulaStatementClassifierTest,NebulaResourceLifecycleTest,NebulaResultSetCursorTest,NebulaClientFacadeTest
```

Expected: FAIL，失败原因应为新类不存在或当前行为未满足测试期望；不得出现真实 NebulaGraph 连接超时。

- [x] **Step 8: 提交测试基线**

```bash
git add fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/pom.xml \
        fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/config/NebulaConnectionConfigParserTest.java \
        fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/statement/NebulaStatementClassifierTest.java \
        fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/lifecycle/NebulaResourceLifecycleTest.java \
        fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/result/NebulaResultSetCursorTest.java \
        fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/client/NebulaClientFacadeTest.java
git commit -m "test: add nebula jdbc stability baseline tests"
```

### Task 2: 连接配置与连接池参数稳定化

**对应 OpenSpec tasks:** 2.1、2.2、2.3

**目标:** 让连接初始化统一使用 `NebulaJdbcUrlParser.parse` 后的合并配置，并集中、安全解析连接池整数参数。

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/config/NebulaConnectionConfig.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/config/NebulaConnectionConfigParser.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/client/NebulaPoolFactory.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/client/NebulaClientFacade.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaConnection.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/config/NebulaConnectionConfigParserTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/client/NebulaClientFacadeTest.java`

**Interfaces:**
- Consumes:
  - `NebulaJdbcUrlParser.parse(String jdbcUrl, Properties defaults): Properties`
  - `NebulaJdbcUrlParser.getAddresses(String url): List<HostAddress>`
  - `NebulaPropertyKey` key names.
- Produces:
  - `NebulaConnectionConfig` getters：`getGraphspace()`、`getUsername()`、`getPassword()`、`getGraphAddresses()`、`getMinConnsSize()`、`getMaxConnsSize()`、`getTimeout()`、`getIdleTime()`、`getIntervalIdle()`、`getWaitTime()`、`getMergedProperties()`。
  - `NebulaConnectionConfigParser.parse(String url, Properties properties): NebulaConnectionConfig`。
  - `NebulaClientFacade.open(NebulaConnectionConfig config): void`。
  - `NebulaClientFacade.executeNgql(String ngql): ResultSet`。
  - `NebulaClientFacade.close(): void`。
  - `NebulaClientFacade.isClosed(): boolean`。

**验证方式:**
- Run: `cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaConnectionConfigParserTest,NebulaClientFacadeTest`
- Expected: PASS。

**提交边界建议:** `fix: stabilize nebula jdbc connection config`

- [x] **Step 1: 实现 NebulaConnectionConfig**

创建不可变配置对象。字段使用 `private final`，构造时复制 `Properties`，`getMergedProperties()` 返回副本，避免调用方修改单一事实源。

必须包含字段：

```java
private final String graphspace;
private final String username;
private final String password;
private final List<HostAddress> graphAddresses;
private final int minConnsSize;
private final int maxConnsSize;
private final int timeout;
private final int idleTime;
private final int intervalIdle;
private final int waitTime;
private final Properties mergedProperties;
```

- [x] **Step 2: 实现 NebulaConnectionConfigParser.parse**

解析流程必须与 Design Doc 4.2 一致：

1. 输入 `Properties` 为 null 时使用空 `Properties`。
2. 调用 `NebulaJdbcUrlParser.parse(url, defaults)` 得到合并属性。
3. 调用 `NebulaJdbcUrlParser.getAddresses(url)` 得到地址列表。
4. 从合并属性读取 `graphspace`、`user`、`password`。
5. 使用集中方法解析整数。
6. 返回 `NebulaConnectionConfig`。

整数默认值沿用当前 `NebulaConnection.initNebulaPool`：

```java
minConnsSize: 0
maxConnsSize: 10
timeout: 0
idleTime: 0
intervalIdle: -1
waitTime: 0
```

整数解析规则：

```java
private static int parseInt(Properties properties, NebulaPropertyKey key, int defaultValue) throws SQLException
```

- 缺失返回默认值。
- `Number` 返回 `((Number) value).intValue()`。
- `String` 先 `trim()`，空串抛 `SQLException`。
- 非数字抛 `SQLException`，消息包含参数名和非法值。

- [x] **Step 3: 实现 NebulaPoolFactory 与 NebulaClientFacade**

`NebulaPoolFactory` 提供：

```java
public NebulaPool create() {
    return new NebulaPool();
}
```

`NebulaClientFacade` 默认构造器使用新 factory；增加 package-private 构造器接收 factory 便于测试。

`open(config)` 行为：

1. 用 `NebulaPoolConfig` 设置所有 pool 参数。
2. `factory.create()` 创建 pool。
3. `pool.init(config.getGraphAddresses(), poolConfig)`。
4. `pool.getSession(config.getUsername(), config.getPassword(), true)`。
5. `session.execute("use " + config.getGraphspace())`。
6. `use` 失败时抛 `SQLException`，消息包含 graphspace、error code、error message。

`executeNgql(ngql)` 行为：

- 关闭后访问抛 `SQLException`。
- 捕获 `IOErrorException` 并包装为 `SQLException`，保留 cause。

`close()` 行为：

- 重复调用稳定。
- 首次关闭释放 session 并关闭 pool。

- [x] **Step 4: 改造 NebulaConnection 使用配置与 façade**

替换当前构造器中混用原始 `properties` 的逻辑。关键行为：

```java
NebulaConnectionConfig config = NebulaConnectionConfigParser.parse(url, properties);
this.properties = config.getMergedProperties();
this.graphSpace = config.getGraphspace();
this.clientFacade = new NebulaClientFacade();
this.clientFacade.open(config);
```

`execute(String nGql)` 改为：

```java
this.checkClosed();
return this.clientFacade.executeNgql(nGql);
```

`close()` 暂时可调用 `clientFacade.close()` 并设置 `isClosed`，生命周期幂等在 Task 3 统一完善。

保留 `getConnectionConfig()` 返回 `Properties` 的兼容行为。

- [x] **Step 5: 运行配置与 façade 测试**

Run:

```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core
./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaConnectionConfigParserTest,NebulaClientFacadeTest
```

Expected: PASS。

- [x] **Step 6: 提交配置稳定化改动**

```bash
git add fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/config/NebulaConnectionConfig.java \
        fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/config/NebulaConnectionConfigParser.java \
        fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/client/NebulaPoolFactory.java \
        fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/client/NebulaClientFacade.java \
        fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaConnection.java \
        fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/config/NebulaConnectionConfigParserTest.java \
        fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/client/NebulaClientFacadeTest.java
git commit -m "fix: stabilize nebula jdbc connection config"
```

- [x] **Step 7: 勾选 OpenSpec tasks 2.1、2.2、2.3**

仅在本任务测试通过并提交后，编辑 `openspec/changes/stabilize-nebula-jdbc/tasks.md` 勾选：

```markdown
- [x] 2.1 调整 `NebulaConnection` 初始化逻辑，统一使用 `NebulaJdbcUrlParser.parse` 后的合并配置
- [x] 2.2 实现连接池整数参数的集中解析逻辑，支持字符串和数字类型并提供明确异常
- [x] 2.3 确认 `graphspace`、用户名、密码和连接池参数均来自同一合并配置来源
```

随后提交任务勾选：

```bash
git add openspec/changes/stabilize-nebula-jdbc/tasks.md
git commit -m "chore: mark nebula jdbc connection config tasks complete"
```

### Task 3: 资源生命周期稳定化

**对应 OpenSpec tasks:** 3.1

**目标:** 统一 `Connection.close()`、`Statement.close()`、`ResultSet.close()` 的重复关闭行为和关闭后访问异常。

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/lifecycle/NebulaResourceLifecycle.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaConnection.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaStatementImpl.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaResultSet.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/lifecycle/NebulaResourceLifecycleTest.java`

**Interfaces:**
- Consumes: Task 2 的 `NebulaClientFacade.close()`。
- Produces:
  - `NebulaResourceLifecycle.close(ThrowingCloseAction closeAction): void`
  - `NebulaResourceLifecycle.checkOpen(String resourceName): void`
  - `NebulaResourceLifecycle.isClosed(): boolean`
  - `NebulaResourceLifecycle.ThrowingCloseAction` 函数式接口。

**验证方式:**
- Run: `cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaResourceLifecycleTest`
- Expected: PASS。
- 可追加运行已有相关测试：`-Dtest=NebulaResourceLifecycleTest,NebulaClientFacadeTest`。

**提交边界建议:** `fix: make nebula jdbc resource close idempotent`

- [x] **Step 1: 实现 NebulaResourceLifecycle**

实现约定：

- 初始 `closed=false`。
- `close(action)`：如果已关闭直接返回；首次调用执行 action；action 成功后设置 closed；若 action 抛 `SQLException`，向上传播且设置 closed，避免调用方反复触发底层释放。
- `checkOpen(resourceName)`：关闭后抛 `SQLException`，消息包含资源名。

推荐接口：

```java
public final class NebulaResourceLifecycle {
    public interface ThrowingCloseAction {
        void close() throws SQLException;
    }

    public void close(ThrowingCloseAction closeAction) throws SQLException
    public void checkOpen(String resourceName) throws SQLException
    public boolean isClosed()
}
```

- [x] **Step 2: 接入 NebulaConnection**

将 `NebulaConnection` 的 `isClosed` boolean 替换或委托给 `NebulaResourceLifecycle`。

`close()` 语义：

```java
lifecycle.close(new NebulaResourceLifecycle.ThrowingCloseAction() {
    @Override
    public void close() throws SQLException {
        clientFacade.close();
        log.debug("JDBCConnection closed");
    }
});
```

`checkClosed()` 语义：

```java
lifecycle.checkOpen("Connection");
```

`isClosed()` 返回 `lifecycle.isClosed()`。

- [x] **Step 3: 接入 NebulaStatementImpl**

将 `isClosed` boolean 替换或委托给 `NebulaResourceLifecycle`。`close()` 重复调用不抛异常；`checkClosed()` 消息包含 `Statement`。

注意：`NebulaPreparedStatementImpl` 继承 `NebulaStatementImpl`，应自动获得同样语义；如构造器受影响，只做最小调整。

- [x] **Step 4: 接入 NebulaResultSet**

将 `NebulaResultSet.close()` 改为生命周期幂等关闭。关闭 action 通常只需要清理内部状态，不应关闭外部 `Statement`，避免改变 JDBC 资源所有权。`checkClosed()` 消息包含 `ResultSet`。

- [x] **Step 5: 运行生命周期测试**

Run:

```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core
./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaResourceLifecycleTest,NebulaClientFacadeTest
```

Expected: PASS。

- [x] **Step 6: 提交生命周期改动**

```bash
git add fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/lifecycle/NebulaResourceLifecycle.java \
        fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaConnection.java \
        fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaStatementImpl.java \
        fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaResultSet.java \
        fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/lifecycle/NebulaResourceLifecycleTest.java
git commit -m "fix: make nebula jdbc resource close idempotent"
```

- [x] **Step 7: 勾选 OpenSpec task 3.1**

仅在测试通过并提交后，编辑 `openspec/changes/stabilize-nebula-jdbc/tasks.md` 勾选：

```markdown
- [x] 3.1 调整 `Connection.close()`、`Statement.close()`、`ResultSet.close()` 的重复关闭行为，避免非必要异常
```

随后提交：

```bash
git add openspec/changes/stabilize-nebula-jdbc/tasks.md
git commit -m "chore: mark nebula jdbc lifecycle task complete"
```

### Task 4: Statement nGQL 类型分类稳定化

**对应 OpenSpec tasks:** 3.2

**目标:** 用独立分类器替换 `NebulaStatementImpl` 中基于 `split(" ")` 的脆弱分类逻辑，支持大小写、换行、多空白、制表符，并明确拒绝非法类型。

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaStatementClassifier.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaStatementImpl.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/statement/NebulaStatementClassifierTest.java`

**Interfaces:**
- Consumes: 无外部依赖，纯逻辑。
- Produces:
  - `NebulaStatementClassifier.checkQuery(String ngql): void`
  - `NebulaStatementClassifier.checkUpdate(String ngql): void`
  - `NebulaStatementImpl.checkReadOnly(String nGql)` 委托 `checkQuery`。
  - `NebulaStatementImpl.checkUpdate(String nGql)` 委托 `checkUpdate`。

**验证方式:**
- Run: `cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaStatementClassifierTest`
- Expected: PASS。

**提交边界建议:** `fix: stabilize nebula statement classification`

- [ ] **Step 1: 实现 NebulaStatementClassifier**

固定关键字集合与 Design Doc 一致：

```java
private static final Set<String> UPDATE_KEYWORDS = new HashSet<>(Arrays.asList(
    "update", "delete", "insert", "upsert", "create", "drop", "alter", "rebuild"
));
private static final Set<String> QUERY_KEYWORDS = new HashSet<>(Arrays.asList(
    "match", "lookup", "go", "fetch", "find", "subgraph", "show", "desc", "describe"
));
```

规范化逻辑：

```java
private static List<String> tokenize(String ngql) throws SQLException {
    if (ngql == null) {
        throw new SQLException("nGQL must not be null.");
    }
    String normalized = ngql.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    if (normalized.isEmpty()) {
        throw new SQLException("nGQL must not be blank.");
    }
    return Arrays.asList(normalized.split(" "));
}
```

分类规则：

- `checkQuery`：先拒绝 update 关键字，再接受 query 关键字，否则抛 expected query。
- `checkUpdate`：接受 update 关键字；否则抛 expected update。

- [ ] **Step 2: 改造 NebulaStatementImpl 委托分类器**

保留 `checkReadOnly(String nGql)` 与 `checkUpdate(String nGql)` 方法签名，内部改为：

```java
NebulaStatementClassifier.checkQuery(nGql);
NebulaStatementClassifier.checkUpdate(nGql);
```

删除或停止使用 `updateKeyword`、`queryKeyword` 数组，避免双重事实源。

- [ ] **Step 3: 运行分类测试**

Run:

```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core
./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaStatementClassifierTest
```

Expected: PASS。

- [ ] **Step 4: 提交分类改动**

```bash
git add fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaStatementClassifier.java \
        fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaStatementImpl.java \
        fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/statement/NebulaStatementClassifierTest.java
git commit -m "fix: stabilize nebula statement classification"
```

- [ ] **Step 5: 勾选 OpenSpec task 3.2**

仅在测试通过并提交后，编辑 `openspec/changes/stabilize-nebula-jdbc/tasks.md` 勾选：

```markdown
- [x] 3.2 改善 `NebulaStatementImpl` 的 nGQL 类型分类逻辑，支持常见空白和大小写格式
```

随后提交：

```bash
git add openspec/changes/stabilize-nebula-jdbc/tasks.md
git commit -m "chore: mark nebula statement classification task complete"
```

### Task 5: ResultSet 游标边界与 wasNull 稳定化

**对应 OpenSpec tasks:** 3.3

**目标:** 集中管理 `ResultSet` 当前行号、可读状态和最近列访问，避免 before first、after last、`wasNull()` 调用顺序造成非预期 NPE。

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/result/NebulaResultSetCursor.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaResultSet.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/result/NebulaResultSetCursorTest.java`

**Interfaces:**
- Consumes: Nebula client `ValueWrapper` 的 `isNull()`。
- Produces:
  - `NebulaResultSetCursor(int rowCount)` 构造器。
  - `boolean next()`：推进游标。
  - `int getCurrentRowNumber()`：供 `NebulaResultSet` 调用 `nativeNebulaResultSet.rowValues(currentRowNumber)`。
  - `void checkReadable()`：before first 或 after last 抛 `SQLException`。
  - `void recordColumnAccess(ValueWrapper value)`：记录最近列访问。
  - `boolean wasNull()`：未读取列前抛明确 `SQLException`；读取后返回最近值 `isNull()`。

**验证方式:**
- Run: `cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaResultSetCursorTest`
- Expected: PASS。

**提交边界建议:** `fix: stabilize nebula result set cursor boundaries`

- [ ] **Step 1: 实现 NebulaResultSetCursor**

推荐状态：

```java
private final int rowCount;
private int currentRowNumber = -1;
private boolean afterLast = false;
private ValueWrapper lastColumnAccess;
private boolean hasColumnAccess = false;
```

`next()` 语义：

- 若 `currentRowNumber + 1 < rowCount`，行号 +1，清空最近列访问，返回 true。
- 否则设置 `currentRowNumber = rowCount`、`afterLast = true`，清空最近列访问，返回 false。

`checkReadable()` 语义：

- `currentRowNumber < 0`：抛 before first。
- `afterLast || currentRowNumber >= rowCount`：抛 after last。

`wasNull()` 语义：

- 未读取任何列前抛 `SQLException`，消息包含 `wasNull` 和 column access。
- 已读取列后返回 `lastColumnAccess == null || lastColumnAccess.isNull()`。

- [ ] **Step 2: 改造 NebulaResultSet.next 与 getObject**

将当前行号推进交给 cursor。`getObject(int columnIndex)` 顺序应为：

1. `checkClosed()`。
2. `cursor.checkReadable()`。
3. `checkIndex(columnIndex)`。
4. `currentRow = nativeNebulaResultSet.rowValues(cursor.getCurrentRowNumber())`。
5. `ValueWrapper result = currentRow.get(columnIndex - 1)`。
6. `cursor.recordColumnAccess(result)`。
7. `lastColumnAccess = result` 可保留用于兼容内部已有代码，但 `wasNull()` 应委托 cursor。
8. 返回 `result`。

`wasNull()` 改为：

```java
this.checkClosed();
return cursor.wasNull();
```

`checkResultSetCursor()` 可保留但委托 `cursor.checkReadable()`，避免重复边界逻辑。

- [ ] **Step 3: 明确空结果集行为**

当原生结果集为空或 `rowsSize() == 0`：

- `next()` 返回 false。
- 任何 `getObject` / `getString` / `getInt` 等读取方法抛 after last 或 no current row 的 `SQLException`。
- `wasNull()` 在未读取列前抛明确 `SQLException`，不再 NPE。

在 `NebulaResultSet` 构造器中，即使 `nativeNebulaResultSet.isEmpty()` 也应初始化 cursor：

```java
this.resultRowSize = nativeNebulaResultSet.isEmpty() ? 0 : nativeNebulaResultSet.rowsSize();
this.cursor = new NebulaResultSetCursor(this.resultRowSize);
```

- [ ] **Step 4: 运行 ResultSet 游标测试**

Run:

```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core
./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaResultSetCursorTest
```

Expected: PASS。

- [ ] **Step 5: 提交 ResultSet 边界改动**

```bash
git add fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/result/NebulaResultSetCursor.java \
        fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaResultSet.java \
        fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/result/NebulaResultSetCursorTest.java
git commit -m "fix: stabilize nebula result set cursor boundaries"
```

- [ ] **Step 6: 勾选 OpenSpec task 3.3**

仅在测试通过并提交后，编辑 `openspec/changes/stabilize-nebula-jdbc/tasks.md` 勾选：

```markdown
- [x] 3.3 改善 `NebulaResultSet` 游标边界和 `wasNull()` 的异常语义，避免非预期 NPE
```

随后提交：

```bash
git add openspec/changes/stabilize-nebula-jdbc/tasks.md
git commit -m "chore: mark nebula result set task complete"
```

### Task 6: 模块验证与收尾

**对应 OpenSpec tasks:** 1.1–1.5、4.1、4.2、4.3

**目标:** 运行模块级测试，确认变更范围符合 Design Doc 非目标，并完成 OpenSpec tasks 勾选准备进入下一阶段。

**Files:**
- Modify: `openspec/changes/stabilize-nebula-jdbc/tasks.md`
- Inspect only: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/**`
- Inspect only: repository diff

**Interfaces:**
- Consumes: Task 1–5 的全部实现与测试。
- Produces: 已验证的模块测试结果、已勾选 OpenSpec tasks、准备进入 Comet 后续 verify 阶段的工作树。

**验证方式:**
- 首选：`cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc`
- 如依赖上游模块未安装：`cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -am`
- 范围检查：`git diff --name-only 85908d71d9146167f277cae498c5d0954d986e25...HEAD`

**提交边界建议:** `chore: complete nebula jdbc stability tasks`

- [ ] **Step 1: 运行模块测试**

Run:

```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core
./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc
```

Expected: PASS。

如果出现上游模块依赖未构建导致解析失败，运行：

```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core
./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -am
```

Expected: PASS。

若出现测试失败或构建失败，必须按 Comet 规则加载 `systematic-debugging` skill，先定位根因，再修复。

- [ ] **Step 2: 检查变更范围不越界**

Run:

```bash
git -C /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework diff --name-only 85908d71d9146167f277cae498c5d0954d986e25...HEAD
```

Expected: 只出现以下路径范围：

```text
fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/
openspec/changes/stabilize-nebula-jdbc/tasks.md
```

不得出现：

```text
fw-plus-boot/
fw-plus-cloud/
fw-plus-ai/
fw-plus-product/
```

- [ ] **Step 3: 检查未引入非目标能力**

Run:

```bash
git -C /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework diff 85908d71d9146167f277cae498c5d0954d986e25...HEAD -- fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc
```

检查结果必须满足：

- 没有 SQL 转 nGQL parser、AST、语法转换器实现。
- 没有 MySQL wire protocol、网关、server socket、Netty 监听服务实现。
- 没有真实 NebulaGraph 集成测试、Testcontainers、外部服务地址依赖。
- 没有 Nebula Java client 大版本升级。

- [ ] **Step 4: 勾选测试与验证相关 OpenSpec tasks**

在 Task 1–5 的测试和提交均完成后，编辑 `openspec/changes/stabilize-nebula-jdbc/tasks.md` 勾选：

```markdown
- [x] 1.1 确认 `fw-plus-nebula-jdbc` 当前 Maven 测试配置，补充必要的单元测试依赖但不引入真实 NebulaGraph 集成环境要求
- [x] 1.2 为 JDBC URL 解析和连接配置合并行为补充单元测试
- [x] 1.3 为连接池数字参数解析的字符串值、默认值和非法值场景补充单元测试
- [x] 1.4 为 Statement 查询/更新语句分类的大小写、换行、多空白和非法类型场景补充单元测试
- [x] 1.5 为资源重复关闭和 ResultSet 边界行为补充单元测试或可隔离验证
- [x] 4.1 运行 `fw-plus-nebula-jdbc` 模块测试并修复失败
- [x] 4.2 检查变更不引入 SQL 转 nGQL、MySQL wire protocol 或跨层改动
```

注意：`4.3 更新任务勾选并准备进入 Comet design 阶段` 原文疑似阶段名写错。执行时若当前 Comet 状态仍为 build，应理解为“准备进入 Comet verify 阶段”；不要手工改 OpenSpec task 文案，除非用户明确要求修正文案。

- [ ] **Step 5: 勾选 OpenSpec task 4.3**

确认所有 tasks 已完成后，勾选：

```markdown
- [x] 4.3 更新任务勾选并准备进入 Comet design 阶段
```

- [ ] **Step 6: 提交收尾勾选**

```bash
git add openspec/changes/stabilize-nebula-jdbc/tasks.md
git commit -m "chore: complete nebula jdbc stability tasks"
```

- [ ] **Step 7: 进入 build guard 前检查**

Run:

```bash
git -C /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework status --short
```

Expected: clean。

然后按 Comet build 流程运行 build 阶段 guard，不在本实施计划中提前执行 verify 阶段动作。

## 自检清单

- [ ] 覆盖 Design Doc 第 4.1–4.6 节：配置、parser、client façade、lifecycle、statement classifier、result cursor。
- [ ] 覆盖 Design Doc 第 8.1–8.6 测试策略。
- [ ] 覆盖 OpenSpec tasks 1.1–1.5、2.1–2.3、3.1–3.3、4.1–4.3。
- [ ] 无 SQL 转 nGQL、MySQL wire protocol、跨层改动、真实 NebulaGraph 集成测试。
- [ ] 每个 task 均有目标、涉及文件/组件、验证方式、提交边界建议。
- [ ] 每个实现 task 完成后才勾选对应 OpenSpec task，并单独提交。
