---
change: add-nebula-jdbc-client-framework-integration
design-doc: docs/superpowers/specs/2026-07-17-nebula-jdbc-client-framework-integration-design.md
base-ref: a85124d86a5a88a5107ce5f109b811c346d9c3e3
---

# Nebula JDBC 客户端框架集成实施计划

> **给 agentic workers：** 必须使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 逐任务执行本计划。步骤使用 checkbox（`- [ ]`）追踪。

**Goal:** 让 `fw-plus-nebula-jdbc` 以标准 JDBC 查询路径兼容 DriverManager、JdbcTemplate、MyBatis XML Mapper 和 MyBatis 注解 Mapper。

**Architecture:** JDBC 层做薄适配：`Statement` / `PreparedStatement` 统一提交 SQL 文本、类型化参数绑定和执行意图到 `NebulaSqlExecutor`。当前模块先提供可测试的执行服务边界与默认 nGQL 透传实现，后续 SQL parser / SQL-to-nGQL converter 落地后替换 executor 内部转换逻辑；框架集成测试使用 fake executor，不依赖真实 NebulaGraph。

**Tech Stack:** Java 1.8、Spring Boot 2.7.18 依赖管理、Maven Wrapper、JUnit 4、Mockito、Spring JDBC、MyBatis。

## Global Constraints

- 只修改 `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc` 及本 change 的 OpenSpec task 勾选。
- 严格兼容 JDK 1.8，不使用 `List.of()`、`Map.copyOf()`、`Optional.orElseThrow()` 等 Java 9+ API。
- Maven 命令从 `fw-plus-core` 执行，使用 `./mvnw`，不用系统 `mvn`。
- 不接入真实 NebulaGraph，不引入 Testcontainers。
- 不实现 MyBatis 写入、MyBatis-Plus BaseMapper / Wrapper、JPA/Hibernate、完整事务。
- 不在 JDBC 层重复实现 SQL parser core 或 SQL-to-nGQL converter。
- 不支持 batch、generated keys、CallableStatement、多结果集、完整事务时返回标准 JDBC 异常，不能静默成功。
- 每个任务完成后运行该任务验证命令，勾选 `openspec/changes/add-nebula-jdbc-client-framework-integration/tasks.md` 对应条目，并提交一次。

---

## 输入引用

- Design Doc: `docs/superpowers/specs/2026-07-17-nebula-jdbc-client-framework-integration-design.md`
- OpenSpec tasks: `openspec/changes/add-nebula-jdbc-client-framework-integration/tasks.md`
- 当前 base-ref: `a85124d86a5a88a5107ce5f109b811c346d9c3e3`

## 文件结构

- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/pom.xml`
  - 增加 test scope 的 `spring-jdbc`、`mybatis` 依赖。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/NebulaSqlExecutor.java`
  - JDBC 层 SQL 执行服务边界。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/NebulaSqlExecutorProvider.java`
  - 保存默认 executor，提供测试可替换入口。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/DirectNebulaSqlExecutor.java`
  - 默认兼容实现：校验语义后调用 `NebulaConnection.execute(sql)`；后续 converter 接入时只替换此类内部逻辑。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaBoundParameter.java`
  - 单个 JDBC 参数值、JDBC type、scale/length。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaParameterBindings.java`
  - 1-based 参数集合、占位符计数、完整性校验、只读视图。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaConnection.java`
  - `createStatement()` / `prepareStatement(String)` 把 executor 传入 statement。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaStatementImpl.java`
  - 查询与更新路径委托 `NebulaSqlExecutor`，保留资源状态和 JDBC 异常语义。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaPreparedStatementImpl.java`
  - 用 `NebulaParameterBindings` 替代字符串替换和 `Map<Object,Object>`。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaParameterMetaData.java`
  - 从 `NebulaParameterBindings` 读取参数数量、类型、class name。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaResultSet.java`
  - 补齐 label 读取、`findColumn`、关闭状态和 `wasNull()` 语义。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaResultSetMetaData.java`
  - 补齐列标签、列名、JDBC 类型、class name、nullable/read-only 等框架常用方法。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaDatabaseMetaData.java`
  - 补齐 JdbcTemplate/MyBatis 常见探测方法，能力方法返回明确 false。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/test/FakeNebulaSqlExecutor.java`
  - 测试专用 executor，记录 SQL 和绑定参数，返回可控 ResultSet。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/test/FakeNebulaResultSetFactory.java`
  - 构造 Nebula client `ResultSet` mock，集中处理列名、行、类型。
- Create: 标准 JDBC、JdbcTemplate、MyBatis 测试类，路径在各任务中列出。

## 任务执行规则

- 每个任务都按 Red-Green-Refactor 执行：先写失败测试，确认失败，再写最小实现，最后跑验证命令。
- 每个任务提交前检查：
  - `git diff --check`
  - 任务列出的 Maven Wrapper 命令
  - 勾选对应 OpenSpec task
- 每个任务提交命令模板：

```bash
git add <本任务文件> openspec/changes/add-nebula-jdbc-client-framework-integration/tasks.md
git commit -m "<scope>: <message>"
```

---

### Task 1: 梳理标准 JDBC 使用路径并建立 executor 注入点

对应 OpenSpec task: `1.0 梳理正常 JDBC 使用路径：DriverManager/DataSource、Connection、Statement、PreparedStatement、ResultSet、ResultSetMetaData 和 DatabaseMetaData。`

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/NebulaSqlExecutor.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/NebulaSqlExecutorProvider.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/DirectNebulaSqlExecutor.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/statement/NebulaStatementExecutorPathTest.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaConnection.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaStatementImpl.java`
- Modify: `openspec/changes/add-nebula-jdbc-client-framework-integration/tasks.md`

**Interfaces:**
- Produces:
  - `NebulaSqlExecutor.executeQuery(NebulaConnection connection, String sql, NebulaParameterBindings bindings) throws SQLException`
  - `NebulaSqlExecutor.executeUpdate(NebulaConnection connection, String sql, NebulaParameterBindings bindings) throws SQLException`
  - `NebulaSqlExecutorProvider.getExecutor()`
  - `NebulaSqlExecutorProvider.setExecutor(NebulaSqlExecutor executor)`
  - `NebulaSqlExecutorProvider.reset()`
- Consumes:
  - Existing `NebulaConnection.execute(String nGql)`
  - Existing `NebulaStatementClassifier.checkQuery(String)` / `checkUpdate(String)`

- [x] **Step 1: Write failing executor path test**

Create `NebulaStatementExecutorPathTest` with a fake executor and assert `Statement.executeQuery(sql)` delegates the exact SQL and empty bindings:

```java
@Test
public void executeQueryShouldDelegateSqlAndEmptyBindingsToExecutor() throws Exception {
    RecordingExecutor executor = new RecordingExecutor();
    NebulaSqlExecutorProvider.setExecutor(executor);
    NebulaConnection connection = mock(NebulaConnection.class);
    NebulaStatementImpl statement = new NebulaStatementImpl(connection);

    ResultSet resultSet = statement.executeQuery("SELECT name FROM player");

    assertSame(executor.resultSet, resultSet);
    assertEquals("SELECT name FROM player", executor.sql);
    assertEquals(0, executor.bindings.size());
}
```

- [x] **Step 2: Run the failing test**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaStatementExecutorPathTest test
```

Expected: FAIL because `NebulaSqlExecutor` and provider classes do not exist or `NebulaStatementImpl` still calls `NebulaConnection.execute` directly.

- [x] **Step 3: Add executor boundary and default implementation**

Implement:

```java
public interface NebulaSqlExecutor {
    ResultSet executeQuery(NebulaConnection connection, String sql, NebulaParameterBindings bindings) throws SQLException;
    int executeUpdate(NebulaConnection connection, String sql, NebulaParameterBindings bindings) throws SQLException;
}
```

Implement provider with a volatile executor and `reset()` restoring `new DirectNebulaSqlExecutor()`. Implement `DirectNebulaSqlExecutor` so query/update classification remains centralized:

```java
public ResultSet executeQuery(NebulaConnection connection, String sql, NebulaParameterBindings bindings) throws SQLException {
    NebulaStatementClassifier.checkQuery(sql);
    com.vesoft.nebula.client.graph.data.ResultSet nativeResultSet = connection.execute(sql);
    return new NebulaResultSet(nativeResultSet, null);
}
```

- [x] **Step 4: Wire `NebulaStatementImpl` to the executor**

Add constructor overload:

```java
public NebulaStatementImpl(NebulaConnection connection, NebulaSqlExecutor sqlExecutor) {
    this.nebulaConnection = connection;
    this.sqlExecutor = sqlExecutor;
}
```

Keep the existing constructor delegating to `NebulaSqlExecutorProvider.getExecutor()`. Change `executeQuery(String sql)` to set `currentResultSet = sqlExecutor.executeQuery(...)` and return it.

- [x] **Step 5: Verify and commit**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaStatementExecutorPathTest test
git diff --check
```

Then check off OpenSpec task `1.0` and commit:

```bash
git add fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql \
  fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaConnection.java \
  fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaStatementImpl.java \
  fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/statement/NebulaStatementExecutorPathTest.java \
  openspec/changes/add-nebula-jdbc-client-framework-integration/tasks.md
git commit -m "feat: add nebula jdbc sql executor boundary"
```

---

### Task 2: 梳理 JdbcTemplate 路径并加入测试依赖

对应 OpenSpec task: `1.1 梳理 JdbcTemplate 常见调用路径和所需 JDBC 方法。`

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/pom.xml`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/framework/NebulaJdbcTemplateContractTest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/test/FakeNebulaSqlExecutor.java`
- Modify: `openspec/changes/add-nebula-jdbc-client-framework-integration/tasks.md`

**Interfaces:**
- Consumes:
  - `NebulaSqlExecutorProvider.setExecutor(...)`
  - `NebulaSqlExecutor.executeQuery(...)`
- Produces:
  - Reusable `FakeNebulaSqlExecutor` with `getLastSql()` and `getLastBindings()`

- [x] **Step 1: Add failing JdbcTemplate contract test**

Create a test that configures a single `Connection` through `SingleConnectionDataSource`, then verifies JdbcTemplate uses `PreparedStatement`, binds arguments, and reads the result:

```java
@Test
public void jdbcTemplateQueryForObjectShouldBindArgumentsAndReadScalar() throws Exception {
    FakeNebulaSqlExecutor executor = FakeNebulaSqlExecutor.singleRow(
        new String[] {"name"},
        new Object[][] {{"Tim"}}
    );
    NebulaSqlExecutorProvider.setExecutor(executor);
    Connection connection = mockOpenConnection();
    JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));

    String value = jdbcTemplate.queryForObject(
        "SELECT name FROM player WHERE id = ?",
        new Object[] {1001L},
        String.class
    );

    assertEquals("Tim", value);
    assertEquals("SELECT name FROM player WHERE id = ?", executor.getLastSql());
    assertEquals(Long.valueOf(1001L), executor.getLastBindings().get(1).getValue());
}
```

- [x] **Step 2: Run test to confirm dependency and behavior gaps**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaJdbcTemplateContractTest test
```

Expected: FAIL because `spring-jdbc` is not on the test classpath or parameter bindings are not yet available.

- [x] **Step 3: Add test dependencies**

Add to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-jdbc</artifactId>
    <scope>test</scope>
</dependency>
```

Do not specify a version; let Spring Boot 2.7.18 dependency management resolve it.

- [x] **Step 4: Implement reusable fake executor test helper**

`FakeNebulaSqlExecutor` must store SQL and bindings:

```java
public ResultSet executeQuery(NebulaConnection connection, String sql, NebulaParameterBindings bindings) throws SQLException {
    this.lastSql = sql;
    this.lastBindings = bindings.copy();
    return resultSet;
}
```

If constructing native Nebula result mocks is noisy, use Mockito to mock `com.vesoft.nebula.client.graph.data.ResultSet` and wrap it with `NebulaResultSet`.

- [x] **Step 5: Verify and commit**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaJdbcTemplateContractTest test
git diff --check
```

Then check off OpenSpec task `1.1` and commit:

```bash
git add fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/pom.xml \
  fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/framework/NebulaJdbcTemplateContractTest.java \
  fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/test/FakeNebulaSqlExecutor.java \
  openspec/changes/add-nebula-jdbc-client-framework-integration/tasks.md
git commit -m "test: capture jdbc template query contract"
```

---

### Task 3: 梳理 MyBatis XML / 注解 Mapper 路径

对应 OpenSpec task: `1.2 梳理 MyBatis XML / 注解 Mapper 常见调用路径和所需 JDBC 方法。`

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/pom.xml`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/framework/NebulaMyBatisContractTest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/framework/PlayerRecord.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/framework/PlayerMapper.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/resources/com/vesoft/nebula/jdbc/framework/PlayerMapper.xml`
- Modify: `openspec/changes/add-nebula-jdbc-client-framework-integration/tasks.md`

**Interfaces:**
- Consumes:
  - `FakeNebulaSqlExecutor`
  - `NebulaParameterBindings`
- Produces:
  - MyBatis XML Mapper and annotation Mapper contract tests for later metadata tasks.

- [x] **Step 1: Add failing MyBatis tests**

Use `Environment`, `JdbcTransactionFactory`, `Configuration`, and `SqlSessionFactoryBuilder` to create an in-memory MyBatis configuration over a `SingleConnectionDataSource`. Include both XML and annotation mapper methods:

```java
public interface PlayerMapper {
    PlayerRecord selectById(@Param("id") Long id);

    @Select("SELECT name AS playerName, age FROM player WHERE id = #{id}")
    PlayerRecord selectAnnotatedById(@Param("id") Long id);
}
```

Assert:

```java
PlayerRecord player = mapper.selectById(1001L);
assertEquals("Tim", player.getPlayerName());
assertEquals(Integer.valueOf(18), player.getAge());
assertEquals(Long.valueOf(1001L), executor.getLastBindings().get(1).getValue());
```

- [x] **Step 2: Run test to confirm dependency and JDBC gaps**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaMyBatisContractTest test
```

Expected: FAIL because `mybatis` is not on the test classpath or metadata / parameter APIs are incomplete.

- [x] **Step 3: Add MyBatis test dependency**

Add:

```xml
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis</artifactId>
    <scope>test</scope>
</dependency>
```

Do not specify a version; use dependency management inherited by the module. If Maven reports no managed version, use the project-managed MyBatis version already present in `fw-plus-parent` or dependencies BOM rather than adding a new arbitrary version.

- [x] **Step 4: Keep mapper SQL in query-only scope**

XML mapper must contain only:

```xml
<select id="selectById" resultType="com.vesoft.nebula.jdbc.framework.PlayerRecord">
    SELECT name AS playerName, age FROM player WHERE id = #{id}
</select>
```

Do not add insert/update/delete mapper methods in this change.

- [x] **Step 5: Verify and commit**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaMyBatisContractTest test
git diff --check
```

Then check off OpenSpec task `1.2` and commit:

```bash
git add fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/pom.xml \
  fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/framework \
  fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/resources/com/vesoft/nebula/jdbc/framework \
  openspec/changes/add-nebula-jdbc-client-framework-integration/tasks.md
git commit -m "test: capture mybatis query mapper contract"
```

---

### Task 4: 定义不支持能力边界与异常语义

对应 OpenSpec task: `1.3 定义不支持的 batch、generated keys、CallableStatement、事务、多结果集和完整 DatabaseMetaData 边界。`

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/statement/NebulaUnsupportedJdbcFeatureTest.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaConnection.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaStatementImpl.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaPreparedStatementImpl.java`
- Modify: `openspec/changes/add-nebula-jdbc-client-framework-integration/tasks.md`

**Interfaces:**
- Consumes:
  - Existing `ExceptionBuilder.buildUnsupportedOperationException()`
- Produces:
  - Standard `SQLFeatureNotSupportedException` for unsupported feature paths.

- [x] **Step 1: Write failing unsupported feature tests**

Cover:

```java
assertThrows(SQLFeatureNotSupportedException.class, new SqlRunnable() {
    public void run() throws SQLException { connection.prepareCall("CALL p()"); }
});
assertThrows(SQLFeatureNotSupportedException.class, new SqlRunnable() {
    public void run() throws SQLException { statement.addBatch("INSERT VERTEX player(name) VALUES 1:(\"Tim\")"); }
});
assertThrows(SQLFeatureNotSupportedException.class, new SqlRunnable() {
    public void run() throws SQLException { statement.getGeneratedKeys(); }
});
```

Because this module uses JUnit 4, implement a local helper:

```java
private static <T extends SQLException> T assertThrows(Class<T> type, SqlRunnable runnable) throws Exception {
    try {
        runnable.run();
        fail("Expected " + type.getName());
    } catch (SQLException exception) {
        assertTrue(type.isInstance(exception));
        return type.cast(exception);
    }
}
```

- [x] **Step 2: Run test and confirm failures**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaUnsupportedJdbcFeatureTest test
```

Expected: FAIL for methods that currently throw generic `SQLException`, `RuntimeException`, or return misleading values.

- [x] **Step 3: Normalize unsupported method behavior**

Change unsupported paths to:

```java
throw ExceptionBuilder.buildUnsupportedOperationException();
```

For capability probes, prefer false over throwing when JDBC frameworks reasonably probe support:

```java
public boolean supportsBatchUpdates() throws SQLException {
    return false;
}
```

- [x] **Step 4: Verify and commit**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaUnsupportedJdbcFeatureTest test
git diff --check
```

Then check off OpenSpec task `1.3` and commit:

```bash
git add fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc \
  fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/statement/NebulaUnsupportedJdbcFeatureTest.java \
  openspec/changes/add-nebula-jdbc-client-framework-integration/tasks.md
git commit -m "fix: normalize unsupported jdbc feature errors"
```

---

### Task 5: 实现 PreparedStatement 类型化参数绑定

对应 OpenSpec task: `2.1 支持 PreparedStatement ? 参数绑定和参数传递到 SQL parser / 转换层。`

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaBoundParameter.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaParameterBindings.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/statement/NebulaParameterBindingsTest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/statement/NebulaPreparedStatementBindingsTest.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaPreparedStatementImpl.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaParameterMetaData.java`
- Modify: `openspec/changes/add-nebula-jdbc-client-framework-integration/tasks.md`

**Interfaces:**
- Produces:
  - `NebulaParameterBindings(int parameterCount)`
  - `void bind(int index, Object value, int jdbcType)`
  - `void bind(int index, Object value, int jdbcType, Integer scaleOrLength)`
  - `void validateComplete() throws SQLException`
  - `NebulaBoundParameter get(int index) throws SQLException`
  - `Map<Integer, NebulaBoundParameter> asMap()`
  - `NebulaParameterBindings copy()`
- Consumes:
  - `PreparedStatement.setObject`, `setString`, `setInt`, `setLong`, `setBoolean`, `setDouble`, `setDate`, `setTimestamp`, `setNull`

- [x] **Step 1: Write failing parameter model tests**

Cover 1-based indexing, out-of-range index, missing binding, JDBC type retention:

```java
NebulaParameterBindings bindings = new NebulaParameterBindings(2);
bindings.bind(1, "Tim", Types.VARCHAR);
bindings.bind(2, 18, Types.INTEGER);
bindings.validateComplete();
assertEquals("Tim", bindings.get(1).getValue());
assertEquals(Types.INTEGER, bindings.get(2).getJdbcType());
```

- [x] **Step 2: Write failing PreparedStatement binding tests**

Assert the fake executor receives typed parameters without SQL string replacement:

```java
PreparedStatement ps = new NebulaPreparedStatementImpl(connection, "SELECT * FROM player WHERE id = ? AND active = ?", executor);
ps.setLong(1, 1001L);
ps.setBoolean(2, true);
ps.executeQuery();

assertEquals("SELECT * FROM player WHERE id = ? AND active = ?", executor.getLastSql());
assertEquals(Types.BIGINT, executor.getLastBindings().get(1).getJdbcType());
assertEquals(Boolean.TRUE, executor.getLastBindings().get(2).getValue());
```

- [x] **Step 3: Run failing tests**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaParameterBindingsTest,NebulaPreparedStatementBindingsTest test
```

Expected: FAIL because `NebulaParameterBindings` does not exist and `NebulaPreparedStatementImpl` still stores raw values in a map.

- [x] **Step 4: Implement binding model and PreparedStatement setters**

Implement setter mappings:

```java
setString -> Types.VARCHAR
setInt -> Types.INTEGER
setLong -> Types.BIGINT
setBoolean -> Types.BOOLEAN
setDouble -> Types.DOUBLE
setDate -> Types.DATE
setTimestamp -> Types.TIMESTAMP
setNull -> supplied sqlType
setObject(Object) -> infer String/Integer/Long/Boolean/Double/Date/Timestamp, fallback Types.JAVA_OBJECT
```

`executeQuery()` must call `bindings.validateComplete()` and then `sqlExecutor.executeQuery(nebulaConnection, rawSql, bindings.copy())`.

- [x] **Step 5: Update `NebulaParameterMetaData`**

Return:

```java
getParameterCount() -> bindings.size()
getParameterType(int) -> bound jdbcType or Types.JAVA_OBJECT when unbound
getParameterClassName(int) -> bound value class name or Object.class.getName()
getParameterMode(int) -> ParameterMetaData.parameterModeIn
isNullable(int) -> ParameterMetaData.parameterNullableUnknown
```

- [x] **Step 6: Verify and commit**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaParameterBindingsTest,NebulaPreparedStatementBindingsTest test
git diff --check
```

Then check off OpenSpec task `2.1` and commit:

```bash
git add fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement \
  fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaParameterMetaData.java \
  fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/statement \
  openspec/changes/add-nebula-jdbc-client-framework-integration/tasks.md
git commit -m "feat: add typed prepared statement bindings"
```

---

### Task 6: 补齐 ResultSetMetaData、DatabaseMetaData 与类型映射

对应 OpenSpec task: `2.2 补齐 ResultSetMetaData、基础 DatabaseMetaData、列名、列别名和 JDBC 类型映射。`

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/NebulaResultSetMetaDataTest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/NebulaDatabaseMetaDataTest.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaResultSetMetaData.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaDatabaseMetaData.java`
- Modify: `openspec/changes/add-nebula-jdbc-client-framework-integration/tasks.md`

**Interfaces:**
- Consumes:
  - `NebulaResultSet.getColumnNames()`
  - `NebulaConnection.getSchema()`
- Produces:
  - Stable metadata methods used by JdbcTemplate and MyBatis.

- [x] **Step 1: Write failing ResultSetMetaData tests**

Assert:

```java
ResultSetMetaData metaData = resultSet.getMetaData();
assertEquals(2, metaData.getColumnCount());
assertEquals("playerName", metaData.getColumnLabel(1));
assertEquals("playerName", metaData.getColumnName(1));
assertEquals(Types.JAVA_OBJECT, metaData.getColumnType(1));
assertEquals(Object.class.getName(), metaData.getColumnClassName(1));
assertEquals(ResultSetMetaData.columnNullableUnknown, metaData.isNullable(1));
assertTrue(metaData.isReadOnly(1));
assertFalse(metaData.isWritable(1));
```

- [x] **Step 2: Write failing DatabaseMetaData tests**

Assert common probes:

```java
DatabaseMetaData metaData = connection.getMetaData();
assertEquals(connection, metaData.getConnection());
assertEquals("NebulaGraph", metaData.getDatabaseProductName());
assertFalse(metaData.supportsTransactions());
assertEquals(Connection.TRANSACTION_NONE, metaData.getDefaultTransactionIsolation());
assertFalse(metaData.supportsBatchUpdates());
assertFalse(metaData.supportsGetGeneratedKeys());
assertFalse(metaData.supportsMultipleResultSets());
assertFalse(metaData.supportsNamedParameters());
assertTrue(metaData.supportsResultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE));
assertTrue(metaData.supportsResultSetConcurrency(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY));
```

- [x] **Step 3: Run failing tests**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaResultSetMetaDataTest,NebulaDatabaseMetaDataTest test
```

Expected: FAIL because metadata methods currently throw unsupported exceptions.

- [x] **Step 4: Implement metadata defaults**

Implement column index validation once:

```java
private void checkColumnIndex(int column) throws SQLException {
    int columnCount = getColumnCount();
    if (column < 1 || column > columnCount) {
        throw new SQLException(String.format("The numbers of column is [%d], your column index [%d] is invalid.", columnCount, column));
    }
}
```

Map missing native type information conservatively to `Types.JAVA_OBJECT` / `Object.class.getName()`. Return false for writable, auto-increment, searchable, currency; return true for read-only.

- [x] **Step 5: Implement DatabaseMetaData common probes**

Return stable values:

```java
getConnection() -> connection
getDatabaseProductName() -> PRODUCT_NAME
getDatabaseProductVersion() -> ""
getDriverName() -> "Nebula JDBC Driver"
getDriverVersion() -> "0.1.1-SNAPSHOT"
isReadOnly() -> true
supportsTransactions() -> false
getDefaultTransactionIsolation() -> Connection.TRANSACTION_NONE
getIdentifierQuoteString() -> "`"
```

- [x] **Step 6: Verify and commit**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaResultSetMetaDataTest,NebulaDatabaseMetaDataTest test
git diff --check
```

Then check off OpenSpec task `2.2` and commit:

```bash
git add fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaResultSetMetaData.java \
  fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaDatabaseMetaData.java \
  fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/NebulaResultSetMetaDataTest.java \
  fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/NebulaDatabaseMetaDataTest.java \
  openspec/changes/add-nebula-jdbc-client-framework-integration/tasks.md
git commit -m "feat: add jdbc framework metadata defaults"
```

---

### Task 7: 明确不支持 JDBC 能力的异常语义

对应 OpenSpec task: `2.3 明确不支持 JDBC 能力的异常语义。`

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/statement/NebulaUnsupportedJdbcFeatureTest.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaDatabaseMetaData.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaStatementImpl.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaPreparedStatementImpl.java`
- Modify: `openspec/changes/add-nebula-jdbc-client-framework-integration/tasks.md`

**Interfaces:**
- Consumes:
  - `ExceptionBuilder.buildUnsupportedOperationException()`
- Produces:
  - Consistent `SQLFeatureNotSupportedException` for unsupported streams, LOBs, generated keys, batch, multi-result, write paths not covered by current change.

- [x] **Step 1: Extend unsupported feature tests**

Cover:

```java
preparedStatement.executeQuery("SELECT 1");
preparedStatement.executeUpdate("UPDATE player SET name = ?");
preparedStatement.setBinaryStream(1, new ByteArrayInputStream(new byte[] {1}), 1);
preparedStatement.setBlob(1, mock(Blob.class));
statement.getMoreResults();
statement.executeBatch();
connection.setAutoCommit(false);
connection.commit();
connection.rollback();
```

Each assertion must require `SQLFeatureNotSupportedException` when the method represents an unsupported capability.

- [x] **Step 2: Run failing test**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaUnsupportedJdbcFeatureTest test
```

Expected: FAIL on methods that currently throw generic `SQLException`, return a misleading value, or throw `RuntimeException`.

- [x] **Step 3: Normalize unsupported exceptions**

Rules:

```java
PreparedStatement.executeQuery(String) -> SQLException with "cannot be called on PreparedStatement"
Unsupported capability methods -> SQLFeatureNotSupportedException
Boolean capability probes -> false
getMoreResults() -> false when no multiple result support
getUpdateCount() -> -1 for query-only path unless an update path executed
```

- [x] **Step 4: Verify and commit**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaUnsupportedJdbcFeatureTest test
git diff --check
```

Then check off OpenSpec task `2.3` and commit:

```bash
git add fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc \
  fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/statement/NebulaUnsupportedJdbcFeatureTest.java \
  openspec/changes/add-nebula-jdbc-client-framework-integration/tasks.md
git commit -m "fix: make unsupported jdbc semantics explicit"
```

---

### Task 8: 补充标准 JDBC DriverManager / PreparedStatement / ResultSet 测试

对应 OpenSpec task: `3.0 补充标准 JDBC DriverManager/PreparedStatement/ResultSet/metadata 使用路径测试。`

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/NebulaStandardJdbcIntegrationTest.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaDriver.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaResultSet.java`
- Modify: `openspec/changes/add-nebula-jdbc-client-framework-integration/tasks.md`

**Interfaces:**
- Consumes:
  - `NebulaDriver`
  - `NebulaSqlExecutorProvider`
  - `NebulaResultSetMetaData`
- Produces:
  - End-to-end standard JDBC smoke coverage.

- [x] **Step 1: Write failing standard JDBC integration test**

Cover:

```java
Connection connection = DriverManager.getConnection(jdbcUrl, properties);
PreparedStatement statement = connection.prepareStatement("SELECT name AS playerName FROM player WHERE id = ?");
statement.setLong(1, 1001L);
ResultSet resultSet = statement.executeQuery();

assertTrue(resultSet.next());
assertEquals("Tim", resultSet.getString("playerName"));
assertEquals("Tim", resultSet.getObject(1));
assertFalse(resultSet.wasNull());
assertEquals(1, resultSet.findColumn("playerName"));
assertEquals("playerName", resultSet.getMetaData().getColumnLabel(1));
assertFalse(resultSet.next());
```

- [x] **Step 2: Run failing test**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaStandardJdbcIntegrationTest test
```

Expected: FAIL if driver registration, fake connection setup, ResultSet label reads, or metadata are incomplete.

- [x] **Step 3: Fix ResultSet label and cursor behavior**

Ensure:

```java
findColumn(String columnLabel) -> 1-based index by exact label
getObject(String columnLabel) -> getObject(findColumn(columnLabel))
getString(String columnLabel) -> getString(findColumn(columnLabel))
closed ResultSet access -> SQLException
cursor before first / after last reads -> SQLException
```

- [x] **Step 4: Verify and commit**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaStandardJdbcIntegrationTest,NebulaResultSetCursorTest test
git diff --check
```

Then check off OpenSpec task `3.0` and commit:

```bash
git add fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc \
  fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/NebulaStandardJdbcIntegrationTest.java \
  openspec/changes/add-nebula-jdbc-client-framework-integration/tasks.md
git commit -m "test: cover standard jdbc query path"
```

---

### Task 9: 补充 JdbcTemplate queryForObject 和 RowMapper 测试

对应 OpenSpec task: `3.1 补充 JdbcTemplate queryForObject 和 RowMapper 测试。`

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/framework/NebulaJdbcTemplateContractTest.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaResultSet.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaResultSetMetaData.java`
- Modify: `openspec/changes/add-nebula-jdbc-client-framework-integration/tasks.md`

**Interfaces:**
- Consumes:
  - `JdbcTemplate.queryForObject(String, Object[], Class<T>)`
  - `JdbcTemplate.query(String, Object[], RowMapper<T>)`
- Produces:
  - Framework-level proof that scalar and POJO RowMapper reads work.

- [x] **Step 1: Extend JdbcTemplate tests**

Add RowMapper test:

```java
List<PlayerRecord> rows = jdbcTemplate.query(
    "SELECT name AS playerName, age FROM player WHERE age > ?",
    new Object[] {10},
    new RowMapper<PlayerRecord>() {
        public PlayerRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            PlayerRecord record = new PlayerRecord();
            record.setPlayerName(rs.getString("playerName"));
            record.setAge(Integer.valueOf(rs.getInt("age")));
            return record;
        }
    }
);

assertEquals(1, rows.size());
assertEquals("Tim", rows.get(0).getPlayerName());
assertEquals(Integer.valueOf(18), rows.get(0).getAge());
```

- [x] **Step 2: Run failing test**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaJdbcTemplateContractTest test
```

Expected: FAIL if `getInt(String)`, `getMetaData()`, `findColumn`, or `wasNull()` do not match JdbcTemplate expectations.

- [x] **Step 3: Fix ResultSet getters used by RowMapper**

Implement / verify:

```java
getInt(String) delegates to getInt(int)
getLong(String) delegates to getLong(int)
getBoolean(String) delegates to getBoolean(int)
getDouble(String) delegates to getDouble(int)
getObject(String) delegates to getObject(int)
```

Each getter must record column access so `wasNull()` reflects the last read column.

- [x] **Step 4: Verify and commit**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaJdbcTemplateContractTest,NebulaResultSetMetaDataTest test
git diff --check
```

Then check off OpenSpec task `3.1` and commit:

```bash
git add fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaResultSet.java \
  fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaResultSetMetaData.java \
  fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/framework/NebulaJdbcTemplateContractTest.java \
  openspec/changes/add-nebula-jdbc-client-framework-integration/tasks.md
git commit -m "test: cover jdbc template scalar and row mapper queries"
```

---

### Task 10: 补充 MyBatis XML Mapper 参数查询测试

对应 OpenSpec task: `3.2 补充 MyBatis XML Mapper 参数查询测试。`

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/framework/NebulaMyBatisContractTest.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/resources/com/vesoft/nebula/jdbc/framework/PlayerMapper.xml`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaPreparedStatementImpl.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaResultSetMetaData.java`
- Modify: `openspec/changes/add-nebula-jdbc-client-framework-integration/tasks.md`

**Interfaces:**
- Consumes:
  - MyBatis XML mapper `#{id}` prepared statement path.
- Produces:
  - XML mapper query proof with parameter binding and POJO resultType mapping.

- [x] **Step 1: Add XML mapper parameter query test**

Assert MyBatis converts `#{id}` to `?`, binds the value, and maps aliases:

```java
PlayerRecord player = mapper.selectById(1001L);

assertEquals("Tim", player.getPlayerName());
assertEquals(Integer.valueOf(18), player.getAge());
assertEquals("SELECT name AS playerName, age FROM player WHERE id = ?", normalizeSql(executor.getLastSql()));
assertEquals(Long.valueOf(1001L), executor.getLastBindings().get(1).getValue());
```

- [x] **Step 2: Run failing test**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaMyBatisContractTest#xmlMapperShouldBindParameterAndMapPojo test
```

Expected: FAIL if MyBatis metadata probes or PreparedStatement setters are incomplete.

- [x] **Step 3: Fix PreparedStatement and metadata methods MyBatis calls**

Ensure these return stable values:

```java
PreparedStatement.getMetaData() -> current result metadata after execute, null before execute
PreparedStatement.getParameterMetaData() -> metadata backed by current bindings
ResultSetMetaData.getColumnLabel(int) -> alias
ResultSetMetaData.getColumnType(int) -> conservative JDBC type
```

- [x] **Step 4: Verify and commit**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaMyBatisContractTest test
git diff --check
```

Then check off OpenSpec task `3.2` and commit:

```bash
git add fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc \
  fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/framework/NebulaMyBatisContractTest.java \
  fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/resources/com/vesoft/nebula/jdbc/framework/PlayerMapper.xml \
  openspec/changes/add-nebula-jdbc-client-framework-integration/tasks.md
git commit -m "test: cover mybatis xml query mapper"
```

---

### Task 11: 补充 MyBatis 注解 Mapper 与 resultMap / POJO 映射测试

对应 OpenSpec task: `3.3 补充 MyBatis 注解 Mapper 和 resultMap / POJO 映射测试。`

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/framework/NebulaMyBatisContractTest.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/framework/PlayerMapper.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/resources/com/vesoft/nebula/jdbc/framework/PlayerMapper.xml`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaResultSet.java`
- Modify: `openspec/changes/add-nebula-jdbc-client-framework-integration/tasks.md`

**Interfaces:**
- Consumes:
  - MyBatis `@Select`
  - XML `<resultMap>`
- Produces:
  - Annotation mapper and resultMap proof for POJO mapping.

- [x] **Step 1: Add annotation Mapper test**

Assert:

```java
PlayerRecord player = mapper.selectAnnotatedById(1001L);
assertEquals("Tim", player.getPlayerName());
assertEquals(Integer.valueOf(18), player.getAge());
```

- [x] **Step 2: Add XML resultMap test**

XML:

```xml
<resultMap id="playerMap" type="com.vesoft.nebula.jdbc.framework.PlayerRecord">
    <result property="playerName" column="player_name"/>
    <result property="age" column="age"/>
</resultMap>
<select id="selectByIdWithResultMap" resultMap="playerMap">
    SELECT name AS player_name, age FROM player WHERE id = #{id}
</select>
```

Test:

```java
PlayerRecord player = mapper.selectByIdWithResultMap(1001L);
assertEquals("Tim", player.getPlayerName());
assertEquals(Integer.valueOf(18), player.getAge());
```

- [x] **Step 3: Run failing tests**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaMyBatisContractTest test
```

Expected: FAIL if alias lookup, snake_case label lookup, or getter conversion is incomplete.

- [x] **Step 4: Fix ResultSet label lookup for exact aliases**

`findColumn` must use labels as exposed by `getColumnLabel`; it must throw `SQLException` when no exact label exists. Do not implement fuzzy property matching in JDBC; MyBatis resultMap owns property-column mapping.

- [x] **Step 5: Verify and commit**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaMyBatisContractTest,NebulaStandardJdbcIntegrationTest test
git diff --check
```

Then check off OpenSpec task `3.3` and commit:

```bash
git add fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaResultSet.java \
  fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/framework \
  fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/resources/com/vesoft/nebula/jdbc/framework/PlayerMapper.xml \
  openspec/changes/add-nebula-jdbc-client-framework-integration/tasks.md
git commit -m "test: cover mybatis annotation and result map queries"
```

---

### Task 12: 回补框架 SQL 形态到 AST 覆盖矩阵并执行完整验证

对应 OpenSpec task: `3.4 将 MyBatis/JdbcTemplate 常见 SQL 形态回补到 AST 覆盖矩阵。`

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/framework/NebulaFrameworkSqlShapeTest.java`
- Modify: `openspec/changes/add-nebula-jdbc-client-framework-integration/tasks.md`

**Interfaces:**
- Consumes:
  - `NebulaSqlExecutor`
  - `NebulaParameterBindings`
- Produces:
  - Local executable coverage for framework SQL shapes until parser-core matrix is merged.

- [x] **Step 1: Add SQL shape matrix test**

Create parameterized-style JUnit 4 tests using a simple loop over cases:

```java
String[] sqlShapes = new String[] {
    "SELECT 1",
    "SELECT COUNT(*) FROM player",
    "SELECT name AS playerName FROM player WHERE id = ?",
    "SELECT name FROM player WHERE id IN (?, ?)",
    "SELECT name FROM player WHERE age > ? AND active = ?",
    "SHOW TAGS",
    "DESCRIBE TAG player"
};

for (String sql : sqlShapes) {
    PreparedStatement statement = connection.prepareStatement(sql);
    bindPlaceholders(statement, countPlaceholders(sql));
    statement.executeQuery();
    assertEquals(sql, executor.getLastSql());
}
```

The test should not assert parser AST internals in this module. It records the SQL forms that parser-core and converter changes must support and ensures JDBC passes them through without mutation.

- [x] **Step 2: Run failing SQL shape test**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaFrameworkSqlShapeTest test
```

Expected: FAIL if placeholder counting, binding validation, or query classification rejects supported framework SQL forms.

- [x] **Step 3: Fix placeholder counting and classification for SQL query forms**

Update `NebulaPreparedStatementImpl` placeholder counting to ignore `?` inside double-quoted string literals as existing code intends. Update query classification only if it blocks SQL query starts required by this change:

```java
QUERY_KEYWORDS includes "select", "show", "desc", "describe"
UPDATE_KEYWORDS includes write and DDL keywords, but write execution remains outside this change
```

- [x] **Step 4: Run full module verification**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc test
git diff --check
```

Expected: PASS. This is the final build evidence for the implementation branch before Comet build guard.

- [x] **Step 5: Check off final OpenSpec task and commit**

Check off OpenSpec task `3.4`, then commit:

```bash
git add fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/framework/NebulaFrameworkSqlShapeTest.java \
  fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement \
  openspec/changes/add-nebula-jdbc-client-framework-integration/tasks.md
git commit -m "test: cover framework sql shape pass through"
```

- [x] **Step 6: Record build evidence before leaving build phase**

Run the build command again if any changes occurred after Step 4:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc test
```

Then run Comet build guard from repository root:

```bash
comet guard add-nebula-jdbc-client-framework-integration build --apply
```

Expected: PASS and Comet phase advances to `verify`.

---

## 最终自检

- Spec coverage:
  - OpenSpec `1.0` 覆盖 Task 1。
  - OpenSpec `1.1` 覆盖 Task 2。
  - OpenSpec `1.2` 覆盖 Task 3。
  - OpenSpec `1.3` 覆盖 Task 4。
  - OpenSpec `2.1` 覆盖 Task 5。
  - OpenSpec `2.2` 覆盖 Task 6。
  - OpenSpec `2.3` 覆盖 Task 7。
  - OpenSpec `3.0` 覆盖 Task 8。
  - OpenSpec `3.1` 覆盖 Task 9。
  - OpenSpec `3.2` 覆盖 Task 10。
  - OpenSpec `3.3` 覆盖 Task 11。
  - OpenSpec `3.4` 覆盖 Task 12。
- Placeholder scan:
  - 占位短语扫描已通过，未保留未定义任务占位。
- Type consistency:
  - `NebulaParameterBindings`、`NebulaBoundParameter`、`NebulaSqlExecutor`、`NebulaSqlExecutorProvider` 的方法名在所有任务中保持一致。
  - `PlayerRecord`、`PlayerMapper`、`FakeNebulaSqlExecutor` 在框架测试任务中保持同名复用。
