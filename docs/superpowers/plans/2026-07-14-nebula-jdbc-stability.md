---
change: stabilize-fw-plus-nebula-jdbc
design-doc: docs/superpowers/specs/2026-07-14-nebula-jdbc-stability-design.md
base-ref: 5d4968cedfc3cef20e982714fa44853ef2d870ac
---

# Nebula JDBC Stability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按 `docs/superpowers/specs/2026-07-14-nebula-jdbc-stability-design.md` 稳定 `fw-plus-nebula-jdbc` 的 URL/属性解析、连接生命周期、Statement/PreparedStatement、ResultSet/MetaData 和 unsupported JDBC 方法语义。

**Architecture:** 采用“契约测试优先 + 小范围测试接缝”。先在 `src/test/java` 下建立无真实 Nebula 服务依赖的契约测试，再在 `com.vesoft.nebula.jdbc` 现有实现中集中收紧解析、状态校验、资源释放和异常语义；生产代码只允许引入受保护工厂方法、包内可见构造器、小型 helper 等内部接缝，不改变 public JDBC API。

**Tech Stack:** Java 1.8、Maven Wrapper、Spring Boot 2.7.18 依赖管理、`com.vesoft:client:3.8.4`、JDBC 标准接口、现有项目 POJO 风格测试类。

## Global Constraints

- 事实源：OpenSpec delta spec `nebula-jdbc-stability` 与设计文档 `docs/superpowers/specs/2026-07-14-nebula-jdbc-stability-design.md`。
- 保持 Java 1.8、现有包名、Maven 坐标和公共 JDBC 类型不变。
- 不升级 `com.vesoft:client`，不要求真实 Nebula 服务参与本次单元测试。
- `NebulaJdbcUrlParser.parse(url, properties)` 的返回值是连接有效配置的单一事实源。
- 非法输入应在创建或初始化连接池前失败，并向调用方表现为清晰的 `SQLException` 或 OpenSpec 已允许的解析异常。
- `NebulaConnection.close()` 必须幂等；初始化部分失败必须释放已创建 session 和 pool。
- 原本明确 unsupported 的 JDBC 功能继续抛 `SQLFeatureNotSupportedException`，不要被关闭状态校验误报为其他异常。
- `ResultSet` 与 `MetaData` 保持 JDBC 一基列索引规则；索引 `0`、负数或大于列数失败为 `SQLException`。
- 验证命令以 OpenSpec tasks 为准：`cd fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc` 与 `cd fw-plus-core && ./mvnw install -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -DskipTests`。
- 每个实现任务验收通过后，按 Comet build 规则勾选 `openspec/changes/stabilize-fw-plus-nebula-jdbc/tasks.md` 中对应项并提交；本计划创建阶段不勾选、不提交。

---

## File Structure

### 生产代码文件

- `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/pom.xml`
  - 职责：仅当父级依赖管理未提供可执行测试能力时，补充最小 test scope 依赖；不得引入重量级 mock 框架。
- `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/utils/NebulaJdbcUrlParser.java`
  - 职责：集中 URL、graph space、host:port、凭据和数字型连接池选项解析；对畸形 URL 和非法数字输入给出明确异常。
- `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaDriver.java`
  - 职责：在 `connect` 边界拒绝非法 URL，并把解析/连接创建错误统一转换为 SQL 层异常。
- `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaConnection.java`
  - 职责：使用解析后的 `Properties` 作为唯一配置来源；提供最小内部测试接缝；保证初始化失败清理、关闭幂等、关闭后受支持方法失败。
- `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaStatementImpl.java`
  - 职责：统一 Statement 关闭状态、空 SQL、query/update 分类和当前结果状态。
- `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaPreparedStatementImpl.java`
  - 职责：复用 Statement 分类与关闭校验；在执行前校验 raw SQL 与参数完整性。
- `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaResultSet.java`
  - 职责：统一 ResultSet 关闭状态、游标有效性、空结果列集合和一基列索引校验。
- `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaResultSetMetaData.java`
  - 职责：共享列索引校验；空结果列数返回 `0`；受支持 metadata 方法不抛 unrelated runtime exception。

### 测试代码文件

- `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/utils/NebulaJdbcUrlParserTest.java`
  - 覆盖 OpenSpec 任务 1.3、2.1、2.2 的 URL、凭据和数字连接池配置解析。
- `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/NebulaConnectionLifecycleTest.java`
  - 覆盖 OpenSpec 任务 2.3、3.1、3.2、3.3 的连接创建边界、失败清理和关闭后访问。
- `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/statement/NebulaStatementContractTest.java`
  - 覆盖 OpenSpec 任务 4.1、4.3 的 Statement/PreparedStatement 分类、空 SQL、关闭后执行、当前结果状态。
- `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/NebulaResultSetContractTest.java`
  - 覆盖 OpenSpec 任务 4.2、4.3 的 ResultSet/MetaData 游标、列索引、空结果和关闭后读取。
- `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/NebulaUnsupportedMethodsTest.java`
  - 覆盖 OpenSpec 任务 1.1、5.1、5.2 的代表性 unsupported 方法和有意支持的简单状态方法。

---

### Task 1: 基线梳理与测试基础

**OpenSpec 对齐:** 1.1、1.2、1.3。

**Files:**
- Modify if needed: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/pom.xml`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/utils/NebulaJdbcUrlParserTest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/NebulaUnsupportedMethodsTest.java`
- Reference: `docs/superpowers/specs/2026-07-14-nebula-jdbc-stability-design.md`
- Reference: `openspec/changes/stabilize-fw-plus-nebula-jdbc/tasks.md`

**Interfaces:**
- Consumes: `NebulaJdbcUrlParser.parse(String jdbcUrl, Properties defaults)`, `NebulaJdbcUrlParser.getAddresses(String url)`。
- Produces: 可执行的 POJO 测试基线；后续任务复用本任务中的 `assertThrows`、`assertEquals`、`assertContains` 风格 helper。

- [ ] **Step 1: 确认测试运行器是否需要额外依赖**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -DskipTests=false
```
Expected:
```text
BUILD SUCCESS
```
如果因为没有任何测试而成功，不修改 `pom.xml`。如果新增测试编译提示缺少测试框架，优先使用项目已有 POJO 测试风格；只有 Maven 无法发现/执行 POJO 测试时，才在模块 `pom.xml` 增加 JUnit 4 test 依赖：

```xml
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 2: 写入 URL 解析失败测试**

Create `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/utils/NebulaJdbcUrlParserTest.java` with this initial content:

```java
package com.vesoft.nebula.jdbc.utils;

import com.vesoft.nebula.client.graph.data.HostAddress;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

public class NebulaJdbcUrlParserTest {

    public void testParseShouldRejectNullUrl() {
        assertThrows(Exception.class, new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                NebulaJdbcUrlParser.parse(null, new Properties());
            }
        });
    }

    public void testParseShouldRejectNonNebulaPrefix() {
        Exception exception = assertThrows(Exception.class, new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                NebulaJdbcUrlParser.parse("jdbc:mysql://127.0.0.1:3306/test", new Properties());
            }
        });
        assertContains(exception.getMessage(), "jdbc:nebula:");
    }

    public void testParseShouldRejectMissingGraphSpace() {
        assertThrows(Exception.class, new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                NebulaJdbcUrlParser.parse("jdbc:nebula://127.0.0.1:9669/", credentials());
            }
        });
    }

    public void testGetAddressesShouldRejectMissingPort() {
        assertThrows(SQLException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                NebulaJdbcUrlParser.getAddresses("jdbc:nebula://127.0.0.1/test");
            }
        });
    }

    public void testGetAddressesShouldRejectNonNumericPort() {
        assertThrows(SQLException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                NebulaJdbcUrlParser.getAddresses("jdbc:nebula://127.0.0.1:not-number/test");
            }
        });
    }

    private Properties credentials() {
        Properties properties = new Properties();
        properties.setProperty(NebulaPropertyKey.USER.getKeyName(), "root");
        properties.setProperty(NebulaPropertyKey.PASSWORD.getKeyName(), "nebula");
        return properties;
    }

    private Exception assertThrows(Class<? extends Exception> type, ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Exception exception) {
            if (!type.isInstance(exception)) {
                throw new AssertionError("expected exception type " + type.getName() + " but was " + exception.getClass().getName(), exception);
            }
            return exception;
        }
        throw new AssertionError("expected exception type " + type.getName());
    }

    private void assertContains(String actual, String expectedPart) {
        if (actual == null || !actual.contains(expectedPart)) {
            throw new AssertionError("expected [" + actual + "] to contain [" + expectedPart + "]");
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
```

- [ ] **Step 3: 写入 URL 解析成功测试**

Append these methods to `NebulaJdbcUrlParserTest` before helper methods:

```java
    public void testParseShouldMergeDefaultsAndQueryProperties() throws Exception {
        Properties defaults = credentials();
        defaults.setProperty(NebulaPropertyKey.MINCONNSSIZE.getKeyName(), "1");

        Properties parsed = NebulaJdbcUrlParser.parse(
                "jdbc:nebula://127.0.0.1:9669,127.0.0.2:9670/test_space?timeout=3000&maxConnsSize=8",
                defaults
        );

        assertEquals("root", parsed.getProperty(NebulaPropertyKey.USER.getKeyName()));
        assertEquals("nebula", parsed.getProperty(NebulaPropertyKey.PASSWORD.getKeyName()));
        assertEquals("test_space", parsed.getProperty(NebulaPropertyKey.DBNAME.getKeyName()));
        assertEquals("1", parsed.getProperty(NebulaPropertyKey.MINCONNSSIZE.getKeyName()));
        assertEquals("3000", parsed.getProperty(NebulaPropertyKey.TIMEOUT.getKeyName()));
        assertEquals("8", parsed.getProperty(NebulaPropertyKey.MAXCONNSSIZE.getKeyName()));
        assertEquals("//127.0.0.1:9669,127.0.0.2:9670", parsed.getProperty(NebulaPropertyKey.DBADDRESS.getKeyName()));
    }

    public void testGetAddressesShouldReturnHostAddresses() throws Exception {
        List<HostAddress> addresses = NebulaJdbcUrlParser.getAddresses("jdbc:nebula://127.0.0.1:9669,127.0.0.2:9670/test_space");

        assertEquals(2, addresses.size());
        assertEquals("127.0.0.1", addresses.get(0).getHost());
        assertEquals(9669, addresses.get(0).getPort());
        assertEquals("127.0.0.2", addresses.get(1).getHost());
        assertEquals(9670, addresses.get(1).getPort());
    }
```

Also add this helper:

```java
    private void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected:<" + expected + "> but was:<" + actual + ">");
        }
    }
```

- [ ] **Step 4: Run test to verify it fails before implementation**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaJdbcUrlParserTest
```
Expected: FAIL. Typical failures before implementation include missing graph space accepted as `default` or non-numeric port escaping as `NumberFormatException` instead of SQL-layer parse failure.

- [ ] **Step 5: 审计公开 JDBC 类并写代表性 unsupported 基线测试**

Create `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/NebulaUnsupportedMethodsTest.java`:

```java
package com.vesoft.nebula.jdbc;

import com.vesoft.nebula.jdbc.statement.NebulaStatementImpl;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLException;

public class NebulaUnsupportedMethodsTest {

    public void testStatementUnsupportedMethodsShouldThrowSqlFeatureNotSupportedException() {
        NebulaStatementImpl statement = new NebulaStatementImpl(null);

        assertSqlFeatureNotSupported(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                statement.setMaxRows(10);
            }
        });
        assertSqlFeatureNotSupported(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                statement.cancel();
            }
        });
    }

    public void testResultSetMetaDataUnsupportedMethodsShouldThrowSqlFeatureNotSupportedException() {
        final NebulaResultSetMetaData metaData = NebulaResultSetMetaData.getInstance(null);

        assertSqlFeatureNotSupported(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                metaData.getColumnType(1);
            }
        });
        assertSqlFeatureNotSupported(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                metaData.unwrap(Object.class);
            }
        });
    }

    private void assertSqlFeatureNotSupported(ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (SQLFeatureNotSupportedException expected) {
            return;
        } catch (SQLException exception) {
            throw new AssertionError("expected SQLFeatureNotSupportedException but was " + exception.getClass().getName(), exception);
        } catch (Exception exception) {
            throw new AssertionError("expected SQLFeatureNotSupportedException but was " + exception.getClass().getName(), exception);
        }
        throw new AssertionError("expected SQLFeatureNotSupportedException");
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
```

- [ ] **Step 6: Implement minimal URL/parser changes to pass Task 1 parser tests**

Modify `NebulaJdbcUrlParser.parse` and `NebulaJdbcUrlParser.getAddresses` so they explicitly reject null/blank URL, missing graph space, blank host, blank/non-numeric port. Keep public signatures unchanged:

```java
public static Properties parse(String jdbcUrl, Properties defaults) throws URISyntaxException {
    if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
        throw new URISyntaxException(String.valueOf(jdbcUrl), "jdbc url must not be blank.");
    }
    if (defaults == null) {
        defaults = new Properties();
    }
    if (!jdbcUrl.startsWith(JDBC_NEBULA_PREFIX)) {
        throw new URISyntaxException(jdbcUrl, "'" + JDBC_NEBULA_PREFIX + "' prefix is mandatory.");
    }

    String uriString = jdbcUrl.substring(JDBC_PREFIX.length());
    URI uri = new URI(uriString);
    Properties jdbcProperties = parseUriQueryPart(uri.getQuery(), defaults);

    String path = uri.getPath();
    if (path == null || path.isEmpty() || path.equals("/")) {
        throw new URISyntaxException(uriString, "graph space path is mandatory.");
    }
    Matcher matcher = DB_PATH_PATTERN.matcher(path);
    if (!matcher.matches()) {
        throw new URISyntaxException("wrong space name path: " + path, uriString);
    }
    jdbcProperties.put(NebulaPropertyKey.DBNAME.getKeyName(), matcher.group(1));

    int indexOfAddress = jdbcUrl.indexOf("//");
    int indexOfSpace = jdbcUrl.indexOf("/", indexOfAddress + 2);
    if (indexOfAddress < 0 || indexOfSpace <= indexOfAddress + 2) {
        throw new URISyntaxException(uriString, "host:port address is mandatory.");
    }
    jdbcProperties.put(NebulaPropertyKey.DBADDRESS.getKeyName(), jdbcUrl.substring(indexOfAddress, indexOfSpace));
    return jdbcProperties;
}
```

```java
public static List<HostAddress> getAddresses(String url) throws SQLException {
    if (url == null || url.trim().isEmpty()) {
        throw new SQLException("url must not be blank.");
    }
    int startIdx = url.indexOf("//") + 2;
    int endIdx = url.lastIndexOf("/");
    if (startIdx < 2 || endIdx <= startIdx) {
        throw new SQLException("url has wrong host format.");
    }

    String ipStr = url.substring(startIdx, endIdx);
    String[] ipStrs = ipStr.split(",");
    List<HostAddress> addressList = new ArrayList<>();
    for (String hostAndPort : ipStrs) {
        String[] ipPort = hostAndPort.split(":");
        if (ipPort.length != 2 || ipPort[0].trim().isEmpty() || ipPort[1].trim().isEmpty()) {
            throw new SQLException("url has wrong host format.");
        }
        try {
            addressList.add(new HostAddress(ipPort[0].trim(), Integer.parseInt(ipPort[1].trim())));
        } catch (NumberFormatException e) {
            throw new SQLException("url has wrong port format: " + ipPort[1], e);
        }
    }
    return addressList;
}
```

- [ ] **Step 7: Run Task 1 tests**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaJdbcUrlParserTest,NebulaUnsupportedMethodsTest
```
Expected:
```text
BUILD SUCCESS
```

- [ ] **Step 8: Commit Task 1**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework
git add fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/pom.xml fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/utils/NebulaJdbcUrlParser.java fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/utils/NebulaJdbcUrlParserTest.java fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/NebulaUnsupportedMethodsTest.java openspec/changes/stabilize-fw-plus-nebula-jdbc/tasks.md
git commit -m "test(nebula-jdbc): add parser stability contract tests"
```
Expected: commit created after OpenSpec Task 1.1-1.3 are checked off in `tasks.md`.

---

### Task 2: URL 解析与连接配置边界

**OpenSpec 对齐:** 2.1、2.2、2.3。

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaConnection.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaDriver.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/utils/NebulaJdbcUrlParser.java`
- Create/Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/NebulaConnectionLifecycleTest.java`

**Interfaces:**
- Consumes: Task 1 parser validation and test helper style。
- Produces: `NebulaConnection` 内部接缝：`protected NebulaPool createNebulaPool()`、`protected NebulaPoolConfig createNebulaPoolConfig(Properties parsedProperties)`、`protected List<HostAddress> getAddresses(String url)`、`protected Session getSession(NebulaPool pool, Properties parsedProperties)`；后续生命周期测试依赖这些方法覆盖资源清理。

- [ ] **Step 1: Write failing tests for parsed properties as single source**

Create `NebulaConnectionLifecycleTest.java`:

```java
package com.vesoft.nebula.jdbc;

import com.vesoft.nebula.client.graph.NebulaPoolConfig;
import com.vesoft.nebula.client.graph.data.HostAddress;
import com.vesoft.nebula.client.graph.net.NebulaPool;
import com.vesoft.nebula.client.graph.net.Session;
import com.vesoft.nebula.jdbc.utils.NebulaPropertyKey;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

public class NebulaConnectionLifecycleTest {

    public void testConnectionShouldUseParsedPropertiesForGraphSpaceAndCredentials() {
        Properties defaults = new Properties();
        defaults.setProperty(NebulaPropertyKey.USER.getKeyName(), "root");
        defaults.setProperty(NebulaPropertyKey.PASSWORD.getKeyName(), "nebula");

        TestableConnection connection = assertCreatesConnection(
                "jdbc:nebula://127.0.0.1:9669/space_from_url?user=user_from_url&password=password_from_url",
                defaults
        );

        assertEquals("space_from_url", connection.getSchemaUnchecked());
        assertEquals("user_from_url", connection.capturedUser);
        assertEquals("password_from_url", connection.capturedPassword);
    }

    public void testInvalidPoolNumberShouldFailBeforePoolCreation() {
        Properties properties = credentials();
        properties.setProperty(NebulaPropertyKey.MAXCONNSSIZE.getKeyName(), "bad-number");

        TestableConnection connection = assertThrowsDuringCreate(
                "jdbc:nebula://127.0.0.1:9669/test_space",
                properties
        );

        assertEquals(0, connection.createPoolCalls);
    }

    private TestableConnection assertCreatesConnection(String url, Properties properties) {
        try {
            return new TestableConnection(url, properties);
        } catch (SQLException exception) {
            throw new AssertionError("expected connection creation to succeed", exception);
        }
    }

    private TestableConnection assertThrowsDuringCreate(String url, Properties properties) {
        TestableConnection connection = new TestableConnection();
        try {
            connection.construct(url, properties);
        } catch (SQLException expected) {
            return connection;
        }
        throw new AssertionError("expected SQLException");
    }

    private Properties credentials() {
        Properties properties = new Properties();
        properties.setProperty(NebulaPropertyKey.USER.getKeyName(), "root");
        properties.setProperty(NebulaPropertyKey.PASSWORD.getKeyName(), "nebula");
        return properties;
    }

    private void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected:<" + expected + "> but was:<" + actual + ">");
        }
    }

    private static class TestableConnection extends NebulaConnection {
        int createPoolCalls;
        String capturedUser;
        String capturedPassword;

        TestableConnection() throws SQLException {
            super();
        }

        TestableConnection(String url, Properties properties) throws SQLException {
            super(url, properties);
        }

        void construct(String url, Properties properties) throws SQLException {
            initialize(url, properties);
        }

        @Override
        protected NebulaPool createNebulaPool() {
            createPoolCalls++;
            return new NoopNebulaPool();
        }

        @Override
        protected Session getSession(NebulaPool pool, Properties parsedProperties) {
            capturedUser = parsedProperties.getProperty(NebulaPropertyKey.USER.getKeyName());
            capturedPassword = parsedProperties.getProperty(NebulaPropertyKey.PASSWORD.getKeyName());
            return null;
        }

        @Override
        protected void useGraphSpace(Session session, String graphSpace) {
        }

        @Override
        protected List<HostAddress> getAddresses(String url) throws SQLException {
            return NebulaJdbcUrlParser.getAddresses(url);
        }

        String getSchemaUnchecked() {
            try {
                return getSchema();
            } catch (SQLException exception) {
                throw new AssertionError(exception);
            }
        }
    }

    private static class NoopNebulaPool extends NebulaPool {
        @Override
        public void close() {
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails because接缝不存在或属性混用**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaConnectionLifecycleTest
```
Expected: FAIL with compilation errors for missing protected no-arg constructor / `initialize` / factory methods, or assertion failure showing raw `properties` are used instead of parsed properties.

- [ ] **Step 3: Add minimal internal connection seam and parsed-properties flow**

Modify `NebulaConnection`:

```java
protected NebulaConnection() {
}

protected NebulaConnection(String url, Properties properties) throws SQLException {
    initialize(url, properties);
}

protected void initialize(String url, Properties inputProperties) throws SQLException {
    Properties parsedProperties;
    try {
        parsedProperties = NebulaJdbcUrlParser.parse(url, inputProperties == null ? new Properties() : inputProperties);
        validateRequired(parsedProperties, NebulaPropertyKey.USER.getKeyName());
        validateRequired(parsedProperties, NebulaPropertyKey.PASSWORD.getKeyName());
        validateRequired(parsedProperties, NebulaPropertyKey.DBNAME.getKeyName());
    } catch (Exception e) {
        throw new SQLException("Invalid Nebula JDBC configuration", e);
    }

    this.properties = parsedProperties;
    this.graphSpace = parsedProperties.getProperty(NebulaPropertyKey.DBNAME.getKeyName());
    initNebulaPool(url, parsedProperties);
    try {
        this.nebulaSession = getSession(nebulaPool, parsedProperties);
        useGraphSpace(this.nebulaSession, this.graphSpace);
        log.debug(String.format("Access graph space [%s] succeeded", this.graphSpace));
    } catch (SQLException e) {
        cleanupQuietly();
        throw e;
    } catch (Exception e) {
        cleanupQuietly();
        throw new SQLException(e);
    }
}

private void validateRequired(Properties properties, String key) throws SQLException {
    String value = properties.getProperty(key);
    if (value == null || value.trim().isEmpty()) {
        throw new SQLException("Missing required Nebula JDBC property: " + key);
    }
}
```

Add numeric helpers and use them in `initNebulaPool`:

```java
private int getIntProperty(Properties properties, NebulaPropertyKey key, int defaultValue) throws SQLException {
    String value = properties.getProperty(key.getKeyName());
    if (value == null || value.trim().isEmpty()) {
        return defaultValue;
    }
    try {
        return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
        throw new SQLException("Invalid integer Nebula JDBC property: " + key.getKeyName() + "=" + value, e);
    }
}
```

```java
private void initNebulaPool(String url, Properties properties) throws SQLException {
    NebulaPoolConfig nebulaPoolConfig = createNebulaPoolConfig(properties);
    List<HostAddress> addressList = getAddresses(url);
    nebulaPool = createNebulaPool();
    try {
        long start = System.currentTimeMillis();
        nebulaPool.init(addressList, nebulaPoolConfig);
        long end = System.currentTimeMillis();
        log.debug("NebulaPool.init(addressList, nebulaPoolConfig) use {} ms", end - start);
    } catch (UnknownHostException | InvalidConfigException e) {
        cleanupQuietly();
        throw new SQLException(e);
    }
}
```

Add protected factory methods:

```java
protected NebulaPoolConfig createNebulaPoolConfig(Properties properties) throws SQLException {
    NebulaPoolConfig nebulaPoolConfig = new NebulaPoolConfig();
    nebulaPoolConfig.setMinConnSize(getIntProperty(properties, NebulaPropertyKey.MINCONNSSIZE, 0));
    nebulaPoolConfig.setMaxConnSize(getIntProperty(properties, NebulaPropertyKey.MAXCONNSSIZE, 10));
    nebulaPoolConfig.setTimeout(getIntProperty(properties, NebulaPropertyKey.TIMEOUT, 0));
    nebulaPoolConfig.setIdleTime(getIntProperty(properties, NebulaPropertyKey.IDLETIME, 0));
    nebulaPoolConfig.setIntervalIdle(getIntProperty(properties, NebulaPropertyKey.INTERVALIDLE, -1));
    nebulaPoolConfig.setWaitTime(getIntProperty(properties, NebulaPropertyKey.WAITTIME, 0));
    return nebulaPoolConfig;
}

protected List<HostAddress> getAddresses(String url) throws SQLException {
    return NebulaJdbcUrlParser.getAddresses(url);
}

protected NebulaPool createNebulaPool() {
    return new NebulaPool();
}

protected Session getSession(NebulaPool pool, Properties parsedProperties)
        throws IOErrorException, AuthFailedException, NotValidConnectionException, ClientServerIncompatibleException {
    return pool.getSession(
            parsedProperties.getProperty(NebulaPropertyKey.USER.getKeyName()),
            parsedProperties.getProperty(NebulaPropertyKey.PASSWORD.getKeyName()),
            true
    );
}

protected void useGraphSpace(Session session, String graphSpace) throws SQLException, IOErrorException {
    ResultSet result = session.execute("use " + graphSpace);
    if (!result.isSucceeded()) {
        throw new SQLException(String.format("Access graph space [%s] failed. Error code: %d, Error message: %s",
                graphSpace, result.getErrorCode(), result.getErrorMessage()));
    }
}
```

- [ ] **Step 4: Update Driver connect exception boundary**

Modify `NebulaDriver.connect` to preserve invalid URL behavior and wrap connection creation consistently:

```java
@Override
public Connection connect(String url, Properties properties) throws SQLException {
    if (!acceptsURL(url)) {
        throw new SQLException("url: " + url + " is not accepted, "
                + "url example: jdbc:nebula://host_ip1:port,host_ip2:port/graphSpace "
                + "make sure your url match this format.");
    }
    try {
        NebulaConnection jdbcConnection = new NebulaConnection(url, properties == null ? new Properties() : properties);
        log.debug("Get JDBCConnection succeeded");
        return jdbcConnection;
    } catch (SQLException e) {
        throw e;
    } catch (Exception e) {
        throw new SQLException("Failed to create Nebula JDBC connection", e);
    }
}
```

- [ ] **Step 5: Run Task 2 tests**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaJdbcUrlParserTest,NebulaConnectionLifecycleTest
```
Expected:
```text
BUILD SUCCESS
```

- [ ] **Step 6: Commit Task 2**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework
git add fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaConnection.java fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaDriver.java fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/utils/NebulaJdbcUrlParser.java fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/NebulaConnectionLifecycleTest.java openspec/changes/stabilize-fw-plus-nebula-jdbc/tasks.md
git commit -m "fix(nebula-jdbc): use parsed connection configuration"
```
Expected: commit created after OpenSpec Task 2.1-2.3 are checked off in `tasks.md`.

---

### Task 3: 资源生命周期

**OpenSpec 对齐:** 3.1、3.2、3.3。

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaConnection.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/NebulaConnectionLifecycleTest.java`

**Interfaces:**
- Consumes: Task 2 `cleanupQuietly()` and connection factory seam。
- Produces: 幂等 `close()`、部分初始化失败清理保障、关闭后 `createStatement()`/`prepareStatement(String)`/`execute(String)` 统一 `SQLException`。

- [ ] **Step 1: Add failing lifecycle tests**

Append to `NebulaConnectionLifecycleTest`:

```java
    public void testCloseShouldBeIdempotentAndReleaseResourcesOnce() throws Exception {
        TestableConnection connection = new TestableConnection("jdbc:nebula://127.0.0.1:9669/test_space", credentials());

        connection.close();
        connection.close();

        assertEquals(true, connection.isClosed());
        assertEquals(1, connection.releaseSessionCalls);
        assertEquals(1, connection.closePoolCalls);
    }

    public void testInitializationFailureShouldCleanupCreatedResources() {
        Properties properties = credentials();
        TestableConnection connection = new TestableConnection();
        connection.failUseGraphSpace = true;

        try {
            connection.construct("jdbc:nebula://127.0.0.1:9669/test_space", properties);
        } catch (SQLException expected) {
            assertEquals(1, connection.releaseSessionCalls);
            assertEquals(1, connection.closePoolCalls);
            return;
        }
        throw new AssertionError("expected SQLException");
    }

    public void testClosedConnectionShouldRejectSupportedOperations() throws Exception {
        TestableConnection connection = new TestableConnection("jdbc:nebula://127.0.0.1:9669/test_space", credentials());
        connection.close();

        assertSQLException(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                connection.createStatement();
            }
        });
        assertSQLException(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                connection.prepareStatement("match (n) return n");
            }
        });
        assertSQLException(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                connection.execute("show spaces");
            }
        });
    }
```

Extend `TestableConnection` with counters:

```java
        int releaseSessionCalls;
        int closePoolCalls;
        boolean failUseGraphSpace;

        @Override
        protected void releaseSessionQuietly(Session session) {
            releaseSessionCalls++;
        }

        @Override
        protected void closePoolQuietly(NebulaPool pool) {
            closePoolCalls++;
        }

        @Override
        protected void useGraphSpace(Session session, String graphSpace) throws SQLException {
            if (failUseGraphSpace) {
                throw new SQLException("use graph space failed");
            }
        }
```

Add helper:

```java
    private void assertSQLException(ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (SQLException expected) {
            return;
        } catch (Exception exception) {
            throw new AssertionError("expected SQLException but was " + exception.getClass().getName(), exception);
        }
        throw new AssertionError("expected SQLException");
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
```

- [ ] **Step 2: Run lifecycle tests to verify failures**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaConnectionLifecycleTest
```
Expected: FAIL because current `close()` calls `checkClosed()` and throws on second close, and initialization failure does not release partial resources.

- [ ] **Step 3: Implement cleanup helpers and idempotent close**

Modify `NebulaConnection`:

```java
@Override
public void close() throws SQLException {
    if (this.isClosed) {
        return;
    }
    cleanupQuietly();
    this.isClosed = true;
    log.debug("JDBCConnection closed");
}

protected void cleanupQuietly() {
    releaseSessionQuietly(this.nebulaSession);
    this.nebulaSession = null;
    closePoolQuietly(this.nebulaPool);
    this.nebulaPool = null;
}

protected void releaseSessionQuietly(Session session) {
    if (session != null) {
        try {
            session.release();
        } catch (Exception e) {
            log.debug("Ignore Nebula session release failure", e);
        }
    }
}

protected void closePoolQuietly(NebulaPool pool) {
    if (pool != null) {
        try {
            pool.close();
        } catch (Exception e) {
            log.debug("Ignore Nebula pool close failure", e);
        }
    }
}
```

Keep `checkClosed()` for supported operations:

```java
private void checkClosed() throws SQLException {
    if (this.isClosed()) {
        throw new SQLException("Connection already closed.");
    }
}
```

- [ ] **Step 4: Ensure unsupported methods remain unsupported after close**

Add test to `NebulaConnectionLifecycleTest`:

```java
    public void testUnsupportedConnectionMethodsShouldRemainUnsupportedAfterClose() throws Exception {
        TestableConnection connection = new TestableConnection("jdbc:nebula://127.0.0.1:9669/test_space", credentials());
        connection.close();

        try {
            connection.prepareCall("call proc()");
        } catch (java.sql.SQLFeatureNotSupportedException expected) {
            return;
        }
        throw new AssertionError("expected SQLFeatureNotSupportedException");
    }
```

Do not add `checkClosed()` to methods that immediately throw `ExceptionBuilder.buildUnsupportedOperationException()`.

- [ ] **Step 5: Run Task 3 tests**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaConnectionLifecycleTest,NebulaUnsupportedMethodsTest
```
Expected:
```text
BUILD SUCCESS
```

- [ ] **Step 6: Commit Task 3**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework
git add fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaConnection.java fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/NebulaConnectionLifecycleTest.java openspec/changes/stabilize-fw-plus-nebula-jdbc/tasks.md
git commit -m "fix(nebula-jdbc): make connection lifecycle deterministic"
```
Expected: commit created after OpenSpec Task 3.1-3.3 are checked off in `tasks.md`.

---

### Task 4: Statement 与 PreparedStatement 契约

**OpenSpec 对齐:** 4.1、4.3 中 Statement/PreparedStatement 部分。

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaStatementImpl.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaPreparedStatementImpl.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/statement/NebulaStatementContractTest.java`

**Interfaces:**
- Consumes: `NebulaConnection.execute(String nGql)`。
- Produces: `protected void validateSqlText(String nGql)`、`protected boolean isQuery(String nGql)`、`protected boolean isUpdate(String nGql)`、`protected String firstKeyword(String nGql)`；PreparedStatement 执行前参数完整性校验。

- [ ] **Step 1: Write failing Statement classification tests**

Create `NebulaStatementContractTest.java`:

```java
package com.vesoft.nebula.jdbc.statement;

import java.sql.SQLException;

public class NebulaStatementContractTest {

    public void testExecuteQueryShouldRejectBlankSqlBeforeConnectionExecution() {
        NebulaStatementImpl statement = new NebulaStatementImpl(null);
        assertSQLException(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                statement.executeQuery("   ");
            }
        });
    }

    public void testExecuteQueryShouldRejectUpdateOnlySql() {
        NebulaStatementImpl statement = new NebulaStatementImpl(null);
        assertSQLExceptionContains("executeQuery", new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                statement.executeQuery("  INSERT VERTEX person(name) VALUES '1':('a')");
            }
        });
    }

    public void testExecuteUpdateShouldRejectQueryOnlySql() {
        NebulaStatementImpl statement = new NebulaStatementImpl(null);
        assertSQLExceptionContains("executeUpdate", new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                statement.executeUpdate("  MATCH (n) RETURN n");
            }
        });
    }

    public void testClosedStatementShouldRejectSupportedOperations() throws Exception {
        NebulaStatementImpl statement = new NebulaStatementImpl(null);
        statement.close();

        assertSQLExceptionContains("closed", new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                statement.execute("show spaces");
            }
        });
        assertSQLExceptionContains("closed", new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                statement.getResultSet();
            }
        });
    }

    private void assertSQLException(ThrowingRunnable runnable) {
        assertSQLExceptionContains(null, runnable);
    }

    private void assertSQLExceptionContains(String expectedMessagePart, ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (SQLException expected) {
            if (expectedMessagePart != null && !expected.getMessage().toLowerCase().contains(expectedMessagePart.toLowerCase())) {
                throw new AssertionError("expected message to contain " + expectedMessagePart + " but was " + expected.getMessage(), expected);
            }
            return;
        } catch (Exception exception) {
            throw new AssertionError("expected SQLException but was " + exception.getClass().getName(), exception);
        }
        throw new AssertionError("expected SQLException");
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
```

- [ ] **Step 2: Write failing PreparedStatement parameter tests**

Append to `NebulaStatementContractTest`:

```java
    public void testPreparedStatementShouldRejectMissingParametersBeforeExecution() {
        NebulaPreparedStatementImpl statement = new NebulaPreparedStatementImpl(null, "MATCH (n) WHERE id(n) == ? RETURN n");

        assertSQLExceptionContains("param", new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                statement.executeQuery();
            }
        });
    }

    public void testPreparedStatementShouldRejectBlankRawSql() {
        NebulaPreparedStatementImpl statement = new NebulaPreparedStatementImpl(null, "  ");

        assertSQLException(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                statement.execute();
            }
        });
    }
```

- [ ] **Step 3: Run tests to verify failures**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaStatementContractTest
```
Expected: FAIL because blank SQL may reach `nebulaConnection.execute`, `getResultSet()` may ignore closed state, and keyword parsing is token-based with weak boundary handling.

- [ ] **Step 4: Centralize SQL validation and classification**

Modify `NebulaStatementImpl`:

```java
protected void validateSqlText(String nGql) throws SQLException {
    if (nGql == null || nGql.trim().isEmpty()) {
        throw new SQLException("nGql must not be blank.");
    }
}

protected String firstKeyword(String nGql) throws SQLException {
    validateSqlText(nGql);
    String normalized = nGql.trim().toLowerCase();
    while (normalized.startsWith("--")) {
        int newline = normalized.indexOf('\n');
        if (newline < 0) {
            throw new SQLException("nGql must contain executable statement.");
        }
        normalized = normalized.substring(newline + 1).trim();
    }
    int space = normalized.indexOf(' ');
    int newline = normalized.indexOf('\n');
    int tab = normalized.indexOf('\t');
    int end = normalized.length();
    if (space >= 0) {
        end = Math.min(end, space);
    }
    if (newline >= 0) {
        end = Math.min(end, newline);
    }
    if (tab >= 0) {
        end = Math.min(end, tab);
    }
    return normalized.substring(0, end);
}

protected boolean isQuery(String nGql) throws SQLException {
    return Arrays.asList(queryKeyword).contains(firstKeyword(nGql));
}

protected boolean isUpdate(String nGql) throws SQLException {
    return Arrays.asList(updateKeyword).contains(firstKeyword(nGql));
}
```

Use these methods:

```java
@Override
public boolean execute(String nGql) throws SQLException {
    this.checkClosed();
    validateSqlText(nGql);
    this.nGql = nGql;
    com.vesoft.nebula.client.graph.data.ResultSet nebulaResultSet = this.nebulaConnection.execute(nGql);
    isExecuteSucceed = nebulaResultSet.isSucceeded();
    if (!isExecuteSucceed) {
        throw new SQLException(String.format("nGql \"%s\" executed failed, error code: %d, error message: %s",
                nGql, nebulaResultSet.getErrorCode(), nebulaResultSet.getErrorMessage()));
    }
    this.currentResultSet = new NebulaResultSet(nebulaResultSet, this);
    return true;
}

@Override
public void checkReadOnly(String nGql) throws SQLException {
    this.checkClosed();
    if (isUpdate(nGql)) {
        throw new SQLException("Method executeQuery() can only execute nGql to query data, please use executeUpdate().");
    }
    if (!isQuery(nGql)) {
        throw new SQLException("Method executeQuery() can only execute query nGql.");
    }
}

@Override
public void checkUpdate(String nGql) throws SQLException {
    this.checkClosed();
    if (!isUpdate(nGql)) {
        throw new SQLException("Method executeUpdate() can only execute nGql to update data, please use executeQuery() or execute().");
    }
}

@Override
public ResultSet getResultSet() throws SQLException {
    this.checkClosed();
    if (this.currentResultSet != null) {
        ResultSet returnedResultSet = this.currentResultSet;
        this.currentResultSet = null;
        return returnedResultSet;
    }
    throw new SQLException("No current ResultSet is available.");
}
```

- [ ] **Step 5: Tighten PreparedStatement execution**

Modify `NebulaPreparedStatementImpl`:

```java
@Override
public boolean execute() throws SQLException {
    this.checkClosed();
    validateSqlText(this.rawNGQL);
    this.nGql = replacePlaceHolderWithParam(this.rawNGQL);
    validateSqlText(this.nGql);
    return this.execute(this.nGql);
}

protected String replacePlaceHolderWithParam(String rawNGQL) throws SQLException {
    validateSqlText(rawNGQL);
    Integer index = 1;
    String digested = rawNGQL;
    Matcher matcher = NAMED_PARAMETER_REGEX.matcher(digested);
    while (matcher.find()) {
        if (!parameters.containsKey(index)) {
            throw new SQLException(String.format("Can not get param in index [%d], please check your nGql.", index));
        }
        Object param = parameters.get(index);
        if (param != null) {
            String paramTypeName = param.getClass().getTypeName();
            switch (paramTypeName) {
                case ("java.lang.String"):
                    param = String.format("\"%s\"", param);
                    break;
                case ("java.sql.Date"):
                    param = String.format("date(\"%s\")", param);
                    break;
                case ("java.sql.Time"):
                    param = String.format("time(\"%s\")", param);
                    break;
                case ("java.util.Date"):
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
                    String datetimeString = formatter.format(param);
                    param = String.format("datetime(\"%s\")", datetimeString);
                    break;
                default:
                    break;
            }
        }
        digested = NAMED_PARAMETER_REGEX.matcher(digested).replaceFirst(Objects.toString(param));
        index++;
    }
    return digested;
}
```

- [ ] **Step 6: Run Task 4 tests**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaStatementContractTest
```
Expected:
```text
BUILD SUCCESS
```

- [ ] **Step 7: Commit Task 4**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework
git add fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaStatementImpl.java fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaPreparedStatementImpl.java fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/statement/NebulaStatementContractTest.java openspec/changes/stabilize-fw-plus-nebula-jdbc/tasks.md
git commit -m "fix(nebula-jdbc): enforce statement execution contracts"
```
Expected: commit created after OpenSpec Task 4.1 and Statement-related Task 4.3 coverage are checked off in `tasks.md`.

---

### Task 5: ResultSet 与 MetaData 契约

**OpenSpec 对齐:** 4.2、4.3 中 ResultSet/MetaData 部分。

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaResultSet.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaResultSetMetaData.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/NebulaResultSetContractTest.java`

**Interfaces:**
- Consumes: `NebulaResultSetMetaData.getInstance(NebulaResultSet nebulaResultSet)`。
- Produces: `NebulaResultSet.checkIndex(int index)` null-safe；`NebulaResultSet.checkResultSetCursor()` validates `currentRowNumber >= 0 && currentRowNumber < resultRowSize`；metadata methods share `checkColumnIndex(int column)`。

- [ ] **Step 1: Write failing metadata tests for empty result**

Create `NebulaResultSetContractTest.java`:

```java
package com.vesoft.nebula.jdbc;

import java.sql.SQLException;
import java.util.Collections;

public class NebulaResultSetContractTest {

    public void testMetaDataShouldReturnZeroColumnCountForNullResultSet() throws Exception {
        NebulaResultSetMetaData metaData = NebulaResultSetMetaData.getInstance(null);

        assertEquals(0, metaData.getColumnCount());
        assertSQLException(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                metaData.getColumnName(1);
            }
        });
    }

    public void testMetaDataShouldRejectZeroAndNegativeIndexes() {
        final TestResultSet resultSet = new TestResultSet(Collections.singletonList("name"));
        final NebulaResultSetMetaData metaData = NebulaResultSetMetaData.getInstance(resultSet);

        assertSQLException(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                metaData.getColumnName(0);
            }
        });
        assertSQLException(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                metaData.getColumnLabel(-1);
            }
        });
        assertSQLException(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                metaData.getSchemaName(2);
            }
        });
    }

    private void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected:<" + expected + "> but was:<" + actual + ">");
        }
    }

    private void assertSQLException(ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (SQLException expected) {
            return;
        } catch (Exception exception) {
            throw new AssertionError("expected SQLException but was " + exception.getClass().getName(), exception);
        }
        throw new AssertionError("expected SQLException");
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
```

Add package-private test subclass in the same file:

```java
class TestResultSet extends NebulaResultSet {
    private final java.util.List<String> names;

    TestResultSet(java.util.List<String> names) {
        super(null, null);
        this.names = names;
    }

    @Override
    public java.util.List<String> getColumnNames() {
        return names;
    }

    @Override
    public com.vesoft.nebula.client.graph.data.ResultSet getNativeNebulaResultSet() {
        return null;
    }
}
```

- [ ] **Step 2: Run tests to verify failures**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaResultSetContractTest
```
Expected: FAIL because `NebulaResultSet` constructor dereferences null native result and metadata schema access can dereference null native result.

- [ ] **Step 3: Make ResultSet constructor and column helpers empty-result safe**

Modify `NebulaResultSet` constructor and helpers:

```java
public NebulaResultSet(ResultSet nebulaResultSet, Statement statement) {
    this.nativeNebulaResultSet = nebulaResultSet;
    this.statement = statement;
    this.columnNames = new ArrayList<>();
    this.resultRowSize = 0;
    if (nativeNebulaResultSet != null && !nativeNebulaResultSet.isEmpty()) {
        this.resultRowSize = nativeNebulaResultSet.rowsSize();
        this.columnNames = nativeNebulaResultSet.getColumnNames();
    } else if (nativeNebulaResultSet != null && nativeNebulaResultSet.getColumnNames() != null) {
        this.columnNames = nativeNebulaResultSet.getColumnNames();
    }
}
```

Add import:

```java
import java.util.ArrayList;
```

Update index and cursor checks:

```java
public void checkIndex(int index) throws SQLException {
    if (columnNames == null || index <= 0 || index > columnNames.size()) {
        int columnCount = columnNames == null ? 0 : columnNames.size();
        throw new SQLException(String.format("column index [%d] is invalid, column count is [%d].", index, columnCount));
    }
}

public void checkResultSetCursor() throws SQLException {
    if (currentRowNumber < 0 || currentRowNumber >= resultRowSize) {
        throw new SQLException(String.format("The cursor of ResultSet is in invalid position [%d] (count from 0), please check it.", currentRowNumber));
    }
}
```

- [ ] **Step 4: Centralize metadata column index validation**

Modify `NebulaResultSetMetaData`:

```java
@Override
public String getColumnName(int column) throws SQLException {
    checkColumnIndex(column);
    return this.nebulaResultSet.getColumnNames().get(column - 1);
}

@Override
public String getSchemaName(int column) throws SQLException {
    checkColumnIndex(column);
    if (this.nebulaResultSet.getNativeNebulaResultSet() == null) {
        return "";
    }
    return this.nebulaResultSet.getNativeNebulaResultSet().getSpaceName();
}

private void checkColumnIndex(int column) throws SQLException {
    int columnCount = this.getColumnCount();
    if (column <= 0 || column > columnCount) {
        throw new SQLException(String.format("The numbers of column is [%d], your column index [%d] is invalid.", columnCount, column));
    }
}
```

- [ ] **Step 5: Add ResultSet closed/read-before-next tests**

Append to `NebulaResultSetContractTest`:

```java
    public void testResultSetShouldRejectReadWhenClosed() throws Exception {
        final TestResultSet resultSet = new TestResultSet(Collections.singletonList("name"));
        resultSet.close();

        assertSQLException(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                resultSet.findColumn("name");
            }
        });
    }

    public void testResultSetShouldRejectColumnAccessBeforeCursorPositioned() {
        final TestResultSet resultSet = new TestResultSet(Collections.singletonList("name"));

        assertSQLException(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                resultSet.checkResultSetCursor();
            }
        });
    }
```

Update `findColumn` in `NebulaResultSet` to call `checkClosed()` and handle null `columnNames`:

```java
@Override
public int findColumn(String columnLabel) throws SQLException {
    this.checkClosed();
    if (columnNames == null) {
        throw new SQLException("No columns are available.");
    }
    int index = 0;
    for (int i = 0; i < columnNames.size(); ++i) {
        if (columnLabel.equals(columnNames.get(i))) {
            index = i + 1;
        }
    }
    if (index <= 0) {
        throw new SQLException(String.format("No such column [%s] found, please check your parameters.", columnLabel));
    }
    return index;
}
```

- [ ] **Step 6: Run Task 5 tests**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaResultSetContractTest
```
Expected:
```text
BUILD SUCCESS
```

- [ ] **Step 7: Commit Task 5**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework
git add fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaResultSet.java fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaResultSetMetaData.java fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/NebulaResultSetContractTest.java openspec/changes/stabilize-fw-plus-nebula-jdbc/tasks.md
git commit -m "fix(nebula-jdbc): enforce result set boundaries"
```
Expected: commit created after OpenSpec Task 4.2 and ResultSet/MetaData-related Task 4.3 coverage are checked off in `tasks.md`.

---

### Task 6: 不支持方法语义与最终验证

**OpenSpec 对齐:** 5.1、5.2、5.3、5.4。

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaConnection.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaStatementImpl.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaPreparedStatementImpl.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaResultSet.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/NebulaUnsupportedMethodsTest.java`

**Interfaces:**
- Consumes: Tasks 1-5 contract tests。
- Produces: 完整模块验证证据；OpenSpec tasks 5.1-5.4 可勾选。

- [ ] **Step 1: Add tests for silent no-op setters and stable state getters**

Append to `NebulaUnsupportedMethodsTest`:

```java
    public void testConnectionSilentNoOpSettersShouldBecomeUnsupported() throws Exception {
        NebulaConnection connection = new NebulaConnection();

        assertSqlFeatureNotSupported(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                connection.setReadOnly(true);
            }
        });
        assertSqlFeatureNotSupported(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                connection.setTransactionIsolation(java.sql.Connection.TRANSACTION_READ_COMMITTED);
            }
        });
    }

    public void testConnectionStableStateMethodsShouldReturnDocumentedValues() throws Exception {
        NebulaConnection connection = new NebulaConnection();

        assertEquals(false, connection.getAutoCommit());
        assertEquals(false, connection.isReadOnly());
        assertEquals(java.sql.Connection.TRANSACTION_NONE, connection.getTransactionIsolation());
    }
```

Add helper:

```java
    private void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected:<" + expected + "> but was:<" + actual + ">");
        }
    }
```

- [ ] **Step 2: Run unsupported tests to verify failures**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaUnsupportedMethodsTest
```
Expected: FAIL because `setReadOnly` and `setTransactionIsolation` are currently silent no-ops.

- [ ] **Step 3: Convert truly unsupported no-op setters to SQLFeatureNotSupportedException**

Modify `NebulaConnection`:

```java
@Override
public void setAutoCommit(boolean autoCommit) throws SQLException {
    if (autoCommit) {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }
}

@Override
public boolean getAutoCommit() throws SQLException {
    return false;
}

@Override
public void setReadOnly(boolean readOnly) throws SQLException {
    if (readOnly) {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }
}

@Override
public boolean isReadOnly() throws SQLException {
    return false;
}

@Override
public void setTransactionIsolation(int level) throws SQLException {
    if (level != Connection.TRANSACTION_NONE) {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }
}

@Override
public int getTransactionIsolation() throws SQLException {
    return Connection.TRANSACTION_NONE;
}
```

If tests decide stricter behavior is required for all setter calls, use this version instead and update stable-state test to cover getters only:

```java
@Override
public void setReadOnly(boolean readOnly) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
}

@Override
public void setTransactionIsolation(int level) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
}
```

- [ ] **Step 4: Audit representative placeholder returns**

Review these methods and either keep stable documented value or change to `ExceptionBuilder.buildUnsupportedOperationException()`:

```text
NebulaConnection.getClientInfo()
NebulaConnection.getClientInfo(String name)
NebulaConnection.setSchema(String schema)
NebulaStatementImpl.getWarnings()
NebulaStatementImpl.clearWarnings()
NebulaStatementImpl.setCursorName(String name)
NebulaResultSet.getWarnings()
NebulaResultSet.clearWarnings()
```

Apply this rule:
- If the method reads local stable state, keep it and add a test.
- If the method silently changes unsupported remote/JDBC behavior, throw `SQLFeatureNotSupportedException`.
- If the method is a harmless warning API and returning null is JDBC-compatible, keep it and document through test.

- [ ] **Step 5: Run full module tests**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc
```
Expected:
```text
BUILD SUCCESS
```

- [ ] **Step 6: Run module install without tests**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && ./mvnw install -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -DskipTests
```
Expected:
```text
BUILD SUCCESS
```

- [ ] **Step 7: Commit Task 6**

Run:
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework
git add fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaConnection.java fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaStatementImpl.java fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaPreparedStatementImpl.java fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaResultSet.java fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/NebulaUnsupportedMethodsTest.java openspec/changes/stabilize-fw-plus-nebula-jdbc/tasks.md
git commit -m "fix(nebula-jdbc): clarify unsupported jdbc methods"
```
Expected: commit created after OpenSpec Task 5.1-5.4 are checked off in `tasks.md`.

---

## Final Verification

After all tasks complete, run:

```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && ./mvnw install -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -DskipTests
```

Expected for both commands:

```text
BUILD SUCCESS
```

Then inspect task status:

```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework && git status --short
```

Expected after all task commits:

```text

```

If verification fails, load `superpowers:systematic-debugging` before changing source code, identify the root cause, fix the smallest failing contract, rerun the failed command, and continue only after the command passes.

## Self-Review

- Spec coverage: Design sections 1-5 are mapped to Tasks 1-6; OpenSpec tasks 1.1-5.4 are represented by task headings and verification steps.
- Placeholder scan: This plan contains no unresolved implementation marker, and each code-changing step includes concrete file paths plus code or exact command.
- Type consistency: New internal connection seam names are introduced in Task 2 and reused by Task 3 only; Statement helper names are introduced and consumed within Task 4; ResultSet metadata helper is introduced and consumed within Task 5.
