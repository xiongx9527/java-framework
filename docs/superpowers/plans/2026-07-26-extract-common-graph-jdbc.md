---
change: extract-common-graph-jdbc
design-doc: docs/superpowers/specs/2026-07-26-extract-common-graph-jdbc-design.md
base-ref: 97f4d9d0c96697bcfcb7446ee9b89e92ff47dcea
---

# 抽象公共图数据库 JDBC 标准层实施计划

> **给 agentic workers:** REQUIRED SUB-SKILL: 使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 按任务逐项实施。步骤使用 checkbox（`- [ ]`）语法跟踪。

**目标:** 在 `fw-plus-graph` 下新增 `fw-plus-graph-jdbc` 公共模块，并让 Neo4j 与 Nebula JDBC 驱动复用无协议的标准 JDBC 基础能力，同时保持现有对外入口兼容。

**架构:** 公共模块只沉淀 JDBC 标准形状：默认返回值、不支持能力异常、`unwrap/isWrapperFor`、资源生命周期、参数绑定、batch、RowSet 与 DatabaseMetaData 空结果集。Neo4j 先从现有 `Neo4jJdbcProxies` 下沉集中代理逻辑；Nebula 以保守接入为主，先复用生命周期、异常、包装、参数和元数据形状，不重写大体量原生结果映射。

**Tech Stack:** Java 1.8, Maven Wrapper, JUnit 4, JDBC `java.sql`, `javax.sql.rowset.CachedRowSet`, `com.sun.rowset.RowSetMetaDataImpl`。

## 全局约束

- 使用 `/Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core/mvnw`，不要使用系统 `mvn`。
- Core 层保持 Java 1.8；不要使用 `List.of()`、`Map.copyOf()`、`Optional.orElseThrow()` 无参形式、模块系统或 Java 9+ API。
- 公共模块包名前缀固定为 `com.xiongx9527.fw.plus.graph.jdbc`。
- 公共模块不得依赖 `com.vesoft`、`org.neo4j.driver`、Druid、Spring、MyBatis 或具体图数据库包名。
- 不改变 `fw-plus-nebula-jdbc`、`fw-plus-neo4j-jdbc` 的 artifactId、Driver 类名、`META-INF/services/java.sql.Driver` 和 JDBC URL 识别规则。
- 本阶段本地测试优先，不要求连接真实 NebulaGraph 或 Neo4j 服务。
- 实施时保留用户已有改动；提交只包含当前任务相关文件。

---

## 文件结构规划

- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/pom.xml`  
  公共图 JDBC 模块 POM，继承 `fw-plus-graph`，只声明 `junit` test 依赖。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/pom.xml`  
  在 `<modules>` 中按 `fw-plus-graph-jdbc`、`fw-plus-nebula-jdbc`、`fw-plus-neo4j-jdbc` 排序。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/pom.xml`  
  增加 `fw-plus-graph-jdbc` 编译依赖，保留 `fw-plus-common`、Nebula SDK、Druid 和测试依赖。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc/pom.xml`  
  增加 `fw-plus-graph-jdbc` 编译依赖，保留 Neo4j Java Driver 和测试依赖。
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcDefaults.java`  
  JDBC 默认返回值、不支持能力异常、JDBC 类型推断。
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcWrapperSupport.java`  
  `unwrap/isWrapperFor` 通用行为。
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcLifecycle.java`  
  资源关闭状态、重复 close 幂等、关闭后访问检查。
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcColumn.java`  
  公共 RowSet 列定义，包含列名、列标签和 JDBC 类型。
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcResult.java`  
  公共查询结果抽象，表达列、行、更新数量和是否有 ResultSet。
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcExecutor.java`  
  具体驱动执行器适配接口。
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcSqlPreprocessor.java`  
  无协议 SQL 文本预处理接口，由具体驱动提供 SQL-to-Cypher 或 identity 行为。
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcParameterBindings.java`  
  Java 8 兼容的 PreparedStatement 参数绑定容器。
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcParameterMetaData.java`  
  基础 `ParameterMetaData` 实现。
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcRowSets.java`  
  `CachedRowSet` 和固定列形状空结果集构造工具。
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcMetaDataSupport.java`  
  schemas、catalogs、table types、columns、primary keys、index info 的标准空结果集形状。
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcStatementSupport.java`  
  Statement/PreparedStatement 动态代理骨架、状态、batch、queryTimeout、fetchSize、closeOnCompletion。
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcDefaultsTest.java`  
  默认值、不支持异常、unwrap/isWrapperFor 和类型推断测试。
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcLifecycleTest.java`  
  close 幂等与关闭后访问测试。
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcParameterBindingsTest.java`  
  参数绑定、null、clearParameters 和参数元数据测试。
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcRowSetsTest.java`  
  列名、列标签、类型推断、空结果集和 generated keys 形状测试。
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcMetaDataSupportTest.java`  
  schemas、catalogs、table types、columns、primary keys、index info 形状测试。
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcStatementSupportTest.java`  
  Statement/PreparedStatement 执行、batch、状态和参数传递测试。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/neo4j/jdbc/Neo4jJdbcProxies.java`  
  仅保留 Neo4j facade 方法，委托公共 `GraphJdbcStatementSupport`、`GraphJdbcRowSets` 和 `GraphJdbcMetaDataSupport`。
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/neo4j/jdbc/Neo4jGraphJdbcExecutor.java`  
  将 `Neo4jQueryExecutor` 和 `Neo4jQueryResult` 适配为公共 `GraphJdbcExecutor`/`GraphJdbcResult`。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/neo4j/jdbc/Neo4jDriverTest.java`  
  增强 DriverManager、Statement、PreparedStatement、参数、batch、metadata 回归断言。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/lifecycle/NebulaResourceLifecycle.java`  
  保持类名和现有 API，内部委托 `GraphJdbcLifecycle`。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/utils/ExceptionBuilder.java`  
  不支持能力异常委托 `GraphJdbcDefaults.unsupportedOperation()`。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/utils/JdbcWrapperSupport.java`  
  `unwrap/isWrapperFor` 委托 `GraphJdbcWrapperSupport`。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/sql/NebulaParameterBindings.java`  
  保持现有类名，内部复用 `GraphJdbcParameterBindings`。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/NebulaParameterMetaData.java`  
  委托 `GraphJdbcParameterMetaData`。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/NebulaDatabaseMetaData.java`  
  空结果集形状委托 `GraphJdbcMetaDataSupport`，保留 Nebula 可从 `SHOW TAGS/EDGES/INDEXES` 读取的具体逻辑。
- Modify: Nebula 现有测试文件  
  `NebulaResourceLifecycleTest.java`、`NebulaPreparedStatementBindingsTest.java`、`NebulaDatabaseMetaDataTest.java`、`NebulaUnsupportedJdbcFeatureTest.java`、`NebulaJdbcTemplateContractTest.java`、`NebulaMyBatisContractTest.java`，确认接入公共层后行为不变。

### Task 1: 建立公共 Maven 模块与依赖顺序

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/pom.xml`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/pom.xml`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/pom.xml`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc/pom.xml`
- Test: Maven 模块解析与空模块测试

**Interfaces:**
- Consumes: 现有 `fw-plus-graph` 聚合父 POM。
- Produces: Maven artifact `com.xiongx9527.fw.plus:fw-plus-graph-jdbc:${revision}`，供 Neo4j/Nebula 模块依赖。

- [ ] **Step 1: 写入公共模块 POM**

将 `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/pom.xml` 创建为：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.xiongx9527.fw.plus</groupId>
        <artifactId>fw-plus-graph</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>fw-plus-graph-jdbc</artifactId>

    <properties>
        <main.basedir>${basedir}/../../..</main.basedir>
    </properties>

    <dependencies>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 调整聚合模块顺序**

把 `fw-plus-core/fw-plus-project/fw-plus-graph/pom.xml` 的 `<modules>` 改为：

```xml
    <modules>
        <module>fw-plus-graph-jdbc</module>
        <module>fw-plus-nebula-jdbc</module>
        <module>fw-plus-neo4j-jdbc</module>
    </modules>
```

- [ ] **Step 3: 为 Nebula 与 Neo4j 增加公共模块依赖**

在两个具体驱动 POM 的 `<dependencies>` 第一项加入：

```xml
        <dependency>
            <groupId>com.xiongx9527.fw.plus</groupId>
            <artifactId>fw-plus-graph-jdbc</artifactId>
            <version>${revision}</version>
        </dependency>
```

保留 Nebula 现有 `fw-plus-common`、`com.vesoft`、Druid、Mockito、Spring JDBC、MyBatis 依赖；保留 Neo4j 现有 `org.neo4j.driver:neo4j-java-driver` 依赖。

- [ ] **Step 4: 验证 Maven 可识别新模块**

Run:

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc test
```

Expected: `BUILD SUCCESS`。首次执行时公共模块没有测试，Surefire 不应报错；若父 POM 要求至少一个测试，则先在 Task 2 的 Step 1 创建测试后再运行同一命令。

- [ ] **Step 5: 提交模块骨架**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-graph/pom.xml \
  fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/pom.xml \
  fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/pom.xml \
  fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc/pom.xml
git commit -m "fw-plus-core: 新增公共图 JDBC 模块"
```

### Task 2: 公共默认行为、生命周期与 RowSet/MetaData 基础能力

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcDefaults.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcWrapperSupport.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcLifecycle.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcColumn.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcResult.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcRowSets.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcMetaDataSupport.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcDefaultsTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcLifecycleTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcRowSetsTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcMetaDataSupportTest.java`

**Interfaces:**
- Consumes: 仅 Java 标准库和 JDBC API。
- Produces:
  - `public final class GraphJdbcDefaults`
  - `public static SQLException unsupportedOperation()`
  - `public static Object defaultValue(Class<?> returnType)`
  - `public static int jdbcType(Object value)`
  - `public final class GraphJdbcWrapperSupport`
  - `public static <T> T unwrap(Object target, Class<T> iface) throws SQLException`
  - `public static boolean isWrapperFor(Object target, Class<?> iface)`
  - `public final class GraphJdbcLifecycle`
  - `public void close(ThrowingCloseAction action) throws SQLException`
  - `public void checkOpen(String resourceName) throws SQLException`
  - `public boolean isClosed()`
  - `public final class GraphJdbcColumn`
  - `public static GraphJdbcColumn of(String name, int jdbcType)`
  - `public static GraphJdbcColumn of(String name, String label, int jdbcType)`
  - `public final class GraphJdbcResult`
  - `public static GraphJdbcResult rows(List<String> columns, List<Map<String, Object>> rows)`
  - `public static GraphJdbcResult updateCount(int updateCount)`
  - `public boolean hasResultSet()`
  - `public final class GraphJdbcRowSets`
  - `public static ResultSet resultSet(GraphJdbcResult result) throws SQLException`
  - `public static ResultSet shapedResultSet(List<GraphJdbcColumn> columns, List<Map<String, Object>> rows) throws SQLException`
  - `public static ResultSet emptyGeneratedKeys() throws SQLException`
  - `public final class GraphJdbcMetaDataSupport`
  - `public static ResultSet emptySchemas() throws SQLException`
  - `public static ResultSet emptyCatalogs() throws SQLException`
  - `public static ResultSet tableTypes(String firstType, String secondType) throws SQLException`
  - `public static ResultSet emptyTables() throws SQLException`
  - `public static ResultSet emptyColumns() throws SQLException`
  - `public static ResultSet emptyPrimaryKeys() throws SQLException`
  - `public static ResultSet emptyIndexInfo() throws SQLException`

- [ ] **Step 1: 先写公共默认行为失败测试**

`GraphJdbcDefaultsTest.java` 覆盖：

```java
@Test
public void defaultValueShouldMatchJdbcProxyPrimitiveReturnTypes() {
    assertNull(GraphJdbcDefaults.defaultValue(Void.TYPE));
    assertEquals(Boolean.FALSE, GraphJdbcDefaults.defaultValue(Boolean.TYPE));
    assertEquals(Integer.valueOf(0), GraphJdbcDefaults.defaultValue(Integer.TYPE));
    assertEquals(Long.valueOf(0L), GraphJdbcDefaults.defaultValue(Long.TYPE));
    assertEquals(Float.valueOf(0F), GraphJdbcDefaults.defaultValue(Float.TYPE));
    assertEquals(Double.valueOf(0D), GraphJdbcDefaults.defaultValue(Double.TYPE));
    assertEquals(Short.valueOf((short) 0), GraphJdbcDefaults.defaultValue(Short.TYPE));
    assertEquals(Byte.valueOf((byte) 0), GraphJdbcDefaults.defaultValue(Byte.TYPE));
    assertNull(GraphJdbcDefaults.defaultValue(String.class));
}

@Test
public void wrapperSupportShouldExposeOnlyTargetAssignableTypes() throws Exception {
    Object target = "value";
    assertTrue(GraphJdbcWrapperSupport.isWrapperFor(target, String.class));
    assertEquals("value", GraphJdbcWrapperSupport.unwrap(target, String.class));
    assertFalse(GraphJdbcWrapperSupport.isWrapperFor(target, java.sql.Connection.class));
}
```

Run:

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc -Dtest=GraphJdbcDefaultsTest test
```

Expected: 编译失败，提示 `GraphJdbcDefaults` 或 `GraphJdbcWrapperSupport` 不存在。

- [ ] **Step 2: 实现 `GraphJdbcDefaults` 与 `GraphJdbcWrapperSupport`**

实现要点：

```java
public final class GraphJdbcDefaults {
    private GraphJdbcDefaults() {
    }

    public static SQLException unsupportedOperation() {
        return new SQLFeatureNotSupportedException("Unsupported JDBC feature");
    }

    public static Object defaultValue(Class<?> returnType) {
        if (returnType == Void.TYPE) {
            return null;
        }
        if (returnType == Boolean.TYPE) {
            return Boolean.FALSE;
        }
        if (returnType == Integer.TYPE) {
            return Integer.valueOf(0);
        }
        if (returnType == Long.TYPE) {
            return Long.valueOf(0L);
        }
        if (returnType == Float.TYPE) {
            return Float.valueOf(0F);
        }
        if (returnType == Double.TYPE) {
            return Double.valueOf(0D);
        }
        if (returnType == Short.TYPE) {
            return Short.valueOf((short) 0);
        }
        if (returnType == Byte.TYPE) {
            return Byte.valueOf((byte) 0);
        }
        return null;
    }

    public static int jdbcType(Object value) {
        if (value instanceof Integer) {
            return Types.INTEGER;
        }
        if (value instanceof Long) {
            return Types.BIGINT;
        }
        if (value instanceof Float || value instanceof Double) {
            return Types.DOUBLE;
        }
        if (value instanceof Boolean) {
            return Types.BOOLEAN;
        }
        return Types.VARCHAR;
    }
}
```

`GraphJdbcWrapperSupport.unwrap` 对可分配类型返回 `iface.cast(target)`，否则抛出 `SQLException("No wrapped object for " + iface.getName())`；`isWrapperFor` 返回 `iface != null && target != null && iface.isInstance(target)`。

- [ ] **Step 3: 写生命周期失败测试**

`GraphJdbcLifecycleTest.java` 覆盖：

```java
@Test
public void closeShouldRunActionOnlyOnce() throws Exception {
    final int[] count = new int[] {0};
    GraphJdbcLifecycle lifecycle = new GraphJdbcLifecycle();
    lifecycle.close(new GraphJdbcLifecycle.ThrowingCloseAction() {
        @Override
        public void close() throws SQLException {
            count[0]++;
        }
    });
    lifecycle.close(new GraphJdbcLifecycle.ThrowingCloseAction() {
        @Override
        public void close() throws SQLException {
            count[0]++;
        }
    });
    assertEquals(1, count[0]);
    assertTrue(lifecycle.isClosed());
}

@Test(expected = SQLException.class)
public void checkOpenShouldRejectClosedResource() throws Exception {
    GraphJdbcLifecycle lifecycle = new GraphJdbcLifecycle();
    lifecycle.close(null);
    lifecycle.checkOpen("Statement");
}
```

Run:

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc -Dtest=GraphJdbcLifecycleTest test
```

Expected: 编译失败，提示 `GraphJdbcLifecycle` 不存在。

- [ ] **Step 4: 实现 `GraphJdbcLifecycle`**

实现要求：

```java
public final class GraphJdbcLifecycle {
    private boolean closed;

    public interface ThrowingCloseAction {
        void close() throws SQLException;
    }

    public synchronized void close(ThrowingCloseAction action) throws SQLException {
        if (closed) {
            return;
        }
        closed = true;
        if (action != null) {
            action.close();
        }
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    public synchronized void checkOpen(String resourceName) throws SQLException {
        if (closed) {
            throw new SQLException(resourceName + " is closed");
        }
    }
}
```

- [ ] **Step 5: 写 RowSet 与元数据形状失败测试**

`GraphJdbcRowSetsTest.java` 覆盖：

```java
@Test
public void resultSetShouldUseColumnLabelsTypesAndRows() throws Exception {
    Map<String, Object> row = new LinkedHashMap<String, Object>();
    row.put("name", "Tim");
    row.put("age", Integer.valueOf(42));
    List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
    rows.add(row);
    ResultSet rs = GraphJdbcRowSets.resultSet(GraphJdbcResult.rows(Arrays.asList("name", "age"), rows));
    assertTrue(rs.next());
    assertEquals("Tim", rs.getString("name"));
    assertEquals(42, rs.getInt("age"));
    assertEquals(Types.INTEGER, rs.getMetaData().getColumnType(2));
    assertFalse(rs.next());
}

@Test
public void emptyGeneratedKeysShouldHaveNoColumnsAndNoRows() throws Exception {
    ResultSet rs = GraphJdbcRowSets.emptyGeneratedKeys();
    assertEquals(0, rs.getMetaData().getColumnCount());
    assertFalse(rs.next());
}
```

`GraphJdbcMetaDataSupportTest.java` 覆盖：

```java
@Test
public void metadataShapesShouldExposeStandardColumnNames() throws Exception {
    assertColumns(GraphJdbcMetaDataSupport.emptySchemas(), "TABLE_SCHEM", "TABLE_CATALOG");
    assertColumns(GraphJdbcMetaDataSupport.emptyCatalogs(), "TABLE_CAT");
    assertColumns(GraphJdbcMetaDataSupport.emptyTables(), "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "TABLE_TYPE", "REMARKS",
            "TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "SELF_REFERENCING_COL_NAME", "REF_GENERATION");
    assertColumns(GraphJdbcMetaDataSupport.emptyColumns(), "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME",
            "DATA_TYPE", "TYPE_NAME", "COLUMN_SIZE", "BUFFER_LENGTH", "DECIMAL_DIGITS", "NUM_PREC_RADIX",
            "NULLABLE", "REMARKS", "COLUMN_DEF", "SQL_DATA_TYPE", "SQL_DATETIME_SUB", "CHAR_OCTET_LENGTH",
            "ORDINAL_POSITION", "IS_NULLABLE", "SCOPE_CATALOG", "SCOPE_SCHEMA", "SCOPE_TABLE", "SOURCE_DATA_TYPE",
            "IS_AUTOINCREMENT", "IS_GENERATEDCOLUMN");
    assertColumns(GraphJdbcMetaDataSupport.emptyPrimaryKeys(), "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME",
            "COLUMN_NAME", "KEY_SEQ", "PK_NAME");
    assertColumns(GraphJdbcMetaDataSupport.emptyIndexInfo(), "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "NON_UNIQUE",
            "INDEX_QUALIFIER", "INDEX_NAME", "TYPE", "ORDINAL_POSITION", "COLUMN_NAME", "ASC_OR_DESC",
            "CARDINALITY", "PAGES", "FILTER_CONDITION");
}

@Test
public void tableTypesShouldUseGraphSpecificLabels() throws Exception {
    ResultSet rs = GraphJdbcMetaDataSupport.tableTypes("NODE", "RELATIONSHIP");
    assertTrue(rs.next());
    assertEquals("NODE", rs.getString("TABLE_TYPE"));
    assertTrue(rs.next());
    assertEquals("RELATIONSHIP", rs.getString("TABLE_TYPE"));
    assertFalse(rs.next());
}
```

辅助方法 `assertColumns(ResultSet rs, String... names)` 逐列断言 `rs.getMetaData().getColumnName(i)`。

Run:

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc -Dtest=GraphJdbcRowSetsTest,GraphJdbcMetaDataSupportTest test
```

Expected: 编译失败，提示 RowSet/MetaData 支持类不存在。

- [ ] **Step 6: 实现 `GraphJdbcColumn`、`GraphJdbcResult`、`GraphJdbcRowSets`、`GraphJdbcMetaDataSupport`**

实现边界：

```java
public final class GraphJdbcColumn {
    private final String name;
    private final String label;
    private final int jdbcType;

    public static GraphJdbcColumn of(String name, int jdbcType) {
        return new GraphJdbcColumn(name, name, jdbcType);
    }

    public static GraphJdbcColumn of(String name, String label, int jdbcType) {
        return new GraphJdbcColumn(name, label, jdbcType);
    }
}
```

```java
public final class GraphJdbcResult {
    private final List<String> columns;
    private final List<Map<String, Object>> rows;
    private final int updateCount;
    private final boolean resultSet;

    public static GraphJdbcResult rows(List<String> columns, List<Map<String, Object>> rows) {
        return new GraphJdbcResult(columns, rows, -1, true);
    }

    public static GraphJdbcResult updateCount(int updateCount) {
        return new GraphJdbcResult(Collections.<String>emptyList(),
                Collections.<Map<String, Object>>emptyList(), updateCount, false);
    }
}
```

`GraphJdbcRowSets.resultSet` 复用现有 Neo4j 逻辑：如果 columns 为空且 rows 非空，则使用第一行 key 顺序；用 `RowSetProvider.newFactory().createCachedRowSet()` 与 `RowSetMetaDataImpl` 构造只读结果。`GraphJdbcMetaDataSupport` 的列数组和类型数组直接采用当前 `Neo4jJdbcProxies` 中 `emptyTablesResultSet`、`emptyColumnsResultSet`、`emptySchemasResultSet`、`emptyCatalogsResultSet`、`emptyPrimaryKeysResultSet`、`emptyIndexInfoResultSet` 的形状。

- [ ] **Step 7: 跑公共基础能力测试**

Run:

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc test
```

Expected: `BUILD SUCCESS`，4 个公共测试类全部通过。

- [ ] **Step 8: 提交公共基础能力**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java \
  fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/test/java
git commit -m "fw-plus-core: 提取图 JDBC 公共基础能力"
```

### Task 3: 公共执行抽象、参数绑定与 Statement/PreparedStatement 支持

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcExecutor.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcSqlPreprocessor.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcParameterBindings.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcParameterMetaData.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcStatementSupport.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcParameterBindingsTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcStatementSupportTest.java`

**Interfaces:**
- Consumes: `GraphJdbcResult`, `GraphJdbcRowSets`, `GraphJdbcDefaults`, `GraphJdbcLifecycle`。
- Produces:
  - `public interface GraphJdbcExecutor { GraphJdbcResult execute(String sql, GraphJdbcParameterBindings bindings) throws SQLException; }`
  - `public interface GraphJdbcSqlPreprocessor { String preprocess(String sql) throws SQLException; }`
  - `public final class GraphJdbcParameterBindings`
  - `public void bind(int parameterIndex, Object value)`
  - `public Object get(int parameterIndex)`
  - `public Map<Integer, Object> asMap()`
  - `public void clear()`
  - `public int size()`
  - `public static int countQuestionMarks(String sql)`
  - `public final class GraphJdbcParameterMetaData implements ParameterMetaData`
  - `public final class GraphJdbcStatementSupport`
  - `public static Statement statement(Connection connection, GraphJdbcExecutor executor, GraphJdbcSqlPreprocessor preprocessor, int resultSetType, int resultSetConcurrency, int resultSetHoldability)`
  - `public static PreparedStatement preparedStatement(Connection connection, GraphJdbcExecutor executor, GraphJdbcSqlPreprocessor preprocessor, String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)`

- [ ] **Step 1: 写参数绑定失败测试**

`GraphJdbcParameterBindingsTest.java` 覆盖：

```java
@Test
public void bindingsShouldStoreByOneBasedParameterIndexAndClear() {
    GraphJdbcParameterBindings bindings = new GraphJdbcParameterBindings();
    bindings.bind(1, "Tom");
    bindings.bind(2, null);
    assertEquals("Tom", bindings.get(1));
    assertNull(bindings.get(2));
    assertEquals(Integer.valueOf(2), Integer.valueOf(bindings.size()));
    bindings.clear();
    assertEquals(Integer.valueOf(0), Integer.valueOf(bindings.size()));
}

@Test
public void parameterMetadataShouldExposeCountAndNullableUnknown() throws Exception {
    ParameterMetaData metaData = new GraphJdbcParameterMetaData(2);
    assertEquals(2, metaData.getParameterCount());
    assertEquals(ParameterMetaData.parameterNullableUnknown, metaData.isNullable(1));
    assertEquals(Types.JAVA_OBJECT, metaData.getParameterType(1));
}

@Test
public void countQuestionMarksShouldIgnoreQuotedText() {
    assertEquals(2, GraphJdbcParameterBindings.countQuestionMarks("MATCH (n) WHERE n.name = ? AND n.note = '?' RETURN ?"));
}
```

Run:

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc -Dtest=GraphJdbcParameterBindingsTest test
```

Expected: 编译失败，提示参数类不存在。

- [ ] **Step 2: 实现参数绑定和参数元数据**

实现要求：

```java
public final class GraphJdbcParameterBindings {
    private final Map<Integer, Object> values = new LinkedHashMap<Integer, Object>();

    public void bind(int parameterIndex, Object value) {
        if (parameterIndex < 1) {
            throw new IllegalArgumentException("parameterIndex must be greater than 0");
        }
        values.put(Integer.valueOf(parameterIndex), value);
    }

    public Object get(int parameterIndex) {
        return values.get(Integer.valueOf(parameterIndex));
    }

    public Map<Integer, Object> asMap() {
        return Collections.unmodifiableMap(values);
    }

    public void clear() {
        values.clear();
    }
}
```

`countQuestionMarks` 逐字符扫描，维护 single quote 和 double quote 状态，只统计未处于引号内的 `?`。`GraphJdbcParameterMetaData` 对所有参数返回 `Types.JAVA_OBJECT`、`ParameterMetaData.parameterModeIn`、`ParameterMetaData.parameterNullableUnknown`，`unwrap/isWrapperFor` 复用 `GraphJdbcWrapperSupport`。

- [ ] **Step 3: 写 Statement 支持失败测试**

`GraphJdbcStatementSupportTest.java` 覆盖：

```java
@Test
public void statementShouldExecuteQueryAndExposeResultSet() throws Exception {
    RecordingExecutor executor = new RecordingExecutor(GraphJdbcResult.rows(Arrays.asList("name"),
            Collections.<Map<String, Object>>singletonList(row("name", "Neo"))));
    Statement statement = GraphJdbcStatementSupport.statement(null, executor, identity(),
            ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
    ResultSet rs = statement.executeQuery("RETURN name");
    assertEquals("RETURN name", executor.sql);
    assertTrue(rs.next());
    assertEquals("Neo", rs.getString("name"));
    assertSame(rs, statement.getResultSet());
    assertEquals(-1, statement.getUpdateCount());
}

@Test
public void preparedStatementShouldBindParametersAndRejectStringExecute() throws Exception {
    RecordingExecutor executor = new RecordingExecutor(GraphJdbcResult.updateCount(3));
    PreparedStatement statement = GraphJdbcStatementSupport.preparedStatement(null, executor, identity(),
            "UPDATE n SET name = ?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
            ResultSet.CLOSE_CURSORS_AT_COMMIT);
    statement.setString(1, "Trinity");
    assertEquals(3, statement.executeUpdate());
    assertEquals("Trinity", executor.bindings.get(1));
    try {
        statement.executeQuery("SELECT 1");
        fail("PreparedStatement executeQuery(String) must fail");
    } catch (SQLException expected) {
        assertTrue(expected.getMessage().contains("cannot be called on PreparedStatement"));
    }
}

@Test
public void batchShouldReturnUpdateCountsAndClearBatch() throws Exception {
    RecordingExecutor executor = new RecordingExecutor(GraphJdbcResult.updateCount(1));
    Statement statement = GraphJdbcStatementSupport.statement(null, executor, identity(),
            ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
    statement.addBatch("A");
    statement.addBatch("B");
    assertArrayEquals(new int[] {1, 1}, statement.executeBatch());
    assertArrayEquals(new int[0], statement.executeBatch());
}
```

测试内定义 `RecordingExecutor implements GraphJdbcExecutor`，记录最后一次 `sql` 与 `GraphJdbcParameterBindings bindings`；`identity()` 返回直接返回原 SQL 的 `GraphJdbcSqlPreprocessor`；`row(String, Object)` 返回 `LinkedHashMap<String,Object>`。

Run:

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc -Dtest=GraphJdbcStatementSupportTest test
```

Expected: 编译失败，提示执行抽象或 Statement 支持类不存在。

- [ ] **Step 4: 实现执行接口与 Statement/PreparedStatement 动态代理**

实现边界：

```java
public interface GraphJdbcExecutor {
    GraphJdbcResult execute(String sql, GraphJdbcParameterBindings bindings) throws SQLException;
}

public interface GraphJdbcSqlPreprocessor {
    String preprocess(String sql) throws SQLException;
}
```

`GraphJdbcStatementSupport` 的代理处理方法必须覆盖当前 Neo4j 已有行为：

```text
executeQuery, executeUpdate, execute, close, isClosed, getResultSet, getUpdateCount,
getConnection, clearParameters, setNull, setObject, setString, setInt, setLong,
setBoolean, addBatch, clearBatch, executeBatch, getGeneratedKeys,
getParameterMetaData, getResultSetType, getResultSetConcurrency,
getResultSetHoldability, cancel, setMaxRows/getMaxRows, setMaxFieldSize/getMaxFieldSize,
setFetchSize/getFetchSize, setFetchDirection/getFetchDirection,
setQueryTimeout/getQueryTimeout, closeOnCompletion/isCloseOnCompletion,
setPoolable/isPoolable, unwrap, isWrapperFor
```

PreparedStatement 的 `executeQuery(String)`、`executeUpdate(String)`、`execute(String)` 抛出 `SQLException(name + "(String) cannot be called on PreparedStatement.")`。执行时先调用 `preprocessor.preprocess(sql)`，再调用 `executor.execute(processedSql, bindings)`；有结果集时使用 `GraphJdbcRowSets.resultSet`，无结果集时返回 `updateCount`。

- [ ] **Step 5: 跑公共 Statement 全量测试**

Run:

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc test
```

Expected: `BUILD SUCCESS`，公共模块 6 个测试类全部通过。

- [ ] **Step 6: 提交公共 Statement 支持**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java \
  fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/test/java
git commit -m "fw-plus-core: 提取图 JDBC Statement 公共支持"
```

### Task 4: Neo4j JDBC 适配公共模块

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/neo4j/jdbc/Neo4jJdbcProxies.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/neo4j/jdbc/Neo4jGraphJdbcExecutor.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/neo4j/jdbc/Neo4jDriverTest.java`

**Interfaces:**
- Consumes:
  - `GraphJdbcStatementSupport.statement(...)`
  - `GraphJdbcStatementSupport.preparedStatement(...)`
  - `GraphJdbcMetaDataSupport`
  - `GraphJdbcExecutor.execute(String, GraphJdbcParameterBindings)`
- Produces:
  - `final class Neo4jGraphJdbcExecutor implements GraphJdbcExecutor`
  - Neo4j `Neo4jJdbcProxies.statement(...)` 和 `preparedStatement(...)` 仍返回标准 JDBC `Statement`/`PreparedStatement`
  - Neo4j SQL-to-Cypher、Java Driver session、事务和 `Neo4jConnectionConfig` 留在 Neo4j 模块内

- [ ] **Step 1: 先增强 Neo4j 回归测试**

在 `Neo4jDriverTest.java` 增加或保留以下断言：

```java
@Test
public void shouldExposeMetadataShapesThroughCommonGraphJdbcSupport() throws Exception {
    Connection connection = DriverManager.getConnection("jdbc:neo4j://localhost:7687/neo4j", "neo4j", "password");
    DatabaseMetaData metaData = connection.getMetaData();
    assertColumns(metaData.getSchemas(), "TABLE_SCHEM", "TABLE_CATALOG");
    assertColumns(metaData.getCatalogs(), "TABLE_CAT");
    assertColumns(metaData.getColumns(null, null, null, null), "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME",
            "DATA_TYPE", "TYPE_NAME", "COLUMN_SIZE", "BUFFER_LENGTH", "DECIMAL_DIGITS", "NUM_PREC_RADIX",
            "NULLABLE", "REMARKS", "COLUMN_DEF", "SQL_DATA_TYPE", "SQL_DATETIME_SUB", "CHAR_OCTET_LENGTH",
            "ORDINAL_POSITION", "IS_NULLABLE", "SCOPE_CATALOG", "SCOPE_SCHEMA", "SCOPE_TABLE", "SOURCE_DATA_TYPE",
            "IS_AUTOINCREMENT", "IS_GENERATEDCOLUMN");
}

@Test
public void shouldKeepPreparedStatementParameterAndBatchBehavior() throws Exception {
    Connection connection = DriverManager.getConnection("jdbc:neo4j://localhost:7687/neo4j", "neo4j", "password");
    PreparedStatement statement = connection.prepareStatement("RETURN ? AS name");
    statement.setString(1, "Morpheus");
    ResultSet rs = statement.executeQuery();
    assertTrue(rs.next());
    assertEquals("Morpheus", rs.getString("name"));
    statement.clearParameters();
    statement.setNull(1, Types.VARCHAR);
    statement.addBatch();
    assertEquals(1, statement.executeBatch().length);
}
```

如果当前 `FakeNeo4jQueryExecutorFactory` 需要补充对参数返回的模拟，直接在测试类内用已有 fake factory 设置返回 `Neo4jQueryResult`，不要连接真实 Neo4j。

Run:

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc -Dtest=Neo4jDriverTest test
```

Expected: 当前实现通过已有场景；新增公共层依赖尚未接入前，新增“common support”命名测试可以先失败在行为断言或重复逻辑未下沉。

- [ ] **Step 2: 新增 Neo4j 公共执行器适配类**

创建 `Neo4jGraphJdbcExecutor.java`：

```java
final class Neo4jGraphJdbcExecutor implements GraphJdbcExecutor {
    private final Neo4jQueryExecutor delegate;

    Neo4jGraphJdbcExecutor(Neo4jQueryExecutor delegate) {
        this.delegate = delegate;
    }

    @Override
    public GraphJdbcResult execute(String cypher, GraphJdbcParameterBindings bindings) throws SQLException {
        Neo4jQueryResult result = delegate.execute(cypher, bindings.asMap());
        if (result.hasResultSet()) {
            return GraphJdbcResult.rows(result.getColumns(), result.getRows());
        }
        return GraphJdbcResult.updateCount(result.getUpdateCount());
    }
}
```

若 `Neo4jQueryExecutor.execute` 当前参数类型不是 `Map<Integer,Object>`，在该适配类中完成最小转换，不修改 `Neo4jQueryExecutor` 的包可见接口，避免扩大 Neo4j 变更面。

- [ ] **Step 3: 将 `Neo4jJdbcProxies` facade 委托公共模块**

`Neo4jJdbcProxies.statement(...)` 改为：

```java
static Statement statement(Neo4jConnection connection, Neo4jQueryExecutor executor, int resultSetType,
                           int resultSetConcurrency, int resultSetHoldability) {
    return GraphJdbcStatementSupport.statement(connection, new Neo4jGraphJdbcExecutor(executor),
            new GraphJdbcSqlPreprocessor() {
                @Override
                public String preprocess(String sql) throws SQLException {
                    return Neo4jSqlToCypherTranslator.translate(sql);
                }
            }, resultSetType, resultSetConcurrency, resultSetHoldability);
}
```

`preparedStatement(...)` 使用同一个 `GraphJdbcSqlPreprocessor`。保留 `databaseMetaData(...)` 中 Neo4j 产品名、版本、URL、用户名、事务能力等 Neo4j 专属返回值；把 `getTables/getColumns/getSchemas/getCatalogs/getTableTypes/getPrimaryKeys/getIndexInfo` 委托到 `GraphJdbcMetaDataSupport`。删除 `Neo4jJdbcProxies` 内部 `StatementHandler`、`resultSet`、`shapedResultSet`、`jdbcType`、`defaultValue`、空 metadata RowSet 方法。

- [ ] **Step 4: 跑 Neo4j 本地回归**

Run:

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc -am -Dtest=Neo4jDriverTest test
```

Expected: `BUILD SUCCESS`，`Neo4jDriverTest` 中 DriverManager URL、Statement、PreparedStatement、参数绑定、batch、metadata 断言全部通过；不启动真实 Neo4j。

- [ ] **Step 5: 验证 Neo4j 模块不新增服务入口变化**

Run:

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework
git diff -- fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc/src/main/resources/META-INF/services/java.sql.Driver
```

Expected: 无输出，表示 `java.sql.Driver` 服务声明未变。

- [ ] **Step 6: 提交 Neo4j 适配**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc/src/main/java \
  fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc/src/test/java
git commit -m "fw-plus-core: 让 Neo4j JDBC 复用公共图 JDBC 支持"
```

### Task 5: Nebula JDBC 保守接入公共模块

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/lifecycle/NebulaResourceLifecycle.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/utils/ExceptionBuilder.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/utils/JdbcWrapperSupport.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/sql/NebulaParameterBindings.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/NebulaParameterMetaData.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/NebulaDatabaseMetaData.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/lifecycle/NebulaResourceLifecycleTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/statement/NebulaPreparedStatementBindingsTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/NebulaDatabaseMetaDataTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/statement/NebulaUnsupportedJdbcFeatureTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/framework/NebulaJdbcTemplateContractTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/framework/NebulaMyBatisContractTest.java`

**Interfaces:**
- Consumes:
  - `GraphJdbcLifecycle`
  - `GraphJdbcDefaults.unsupportedOperation()`
  - `GraphJdbcWrapperSupport`
  - `GraphJdbcParameterBindings`
  - `GraphJdbcParameterMetaData`
  - `GraphJdbcMetaDataSupport`
- Produces:
  - Nebula 现有 public/package-visible 类名、方法签名和测试 fake executor 路径保持可用
  - `NebulaConnection`、`NebulaStatementImpl`、`NebulaPreparedStatementImpl` 继续使用原生 Nebula 执行器和 ResultSet 映射

- [ ] **Step 1: 先跑 Nebula 迁移前回归基线**

Run:

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc -Dtest=NebulaResourceLifecycleTest,NebulaPreparedStatementBindingsTest,NebulaDatabaseMetaDataTest,NebulaUnsupportedJdbcFeatureTest,NebulaJdbcTemplateContractTest,NebulaMyBatisContractTest test
```

Expected: `BUILD SUCCESS`。如果某个测试依赖当前未安装的本地服务，它必须被拆出或按现有测试约定跳过；不要把真实 NebulaGraph 作为本任务验收条件。

- [ ] **Step 2: 将 Nebula 生命周期委托公共生命周期**

`NebulaResourceLifecycle` 保留现有 `ThrowingCloseAction` 接口兼容层，并内部使用：

```java
private final GraphJdbcLifecycle delegate = new GraphJdbcLifecycle();

public void close(final ThrowingCloseAction action) throws SQLException {
    delegate.close(action == null ? null : new GraphJdbcLifecycle.ThrowingCloseAction() {
        @Override
        public void close() throws SQLException {
            action.close();
        }
    });
}

public void checkOpen(String resourceName) throws SQLException {
    delegate.checkOpen(resourceName);
}

public boolean isClosed() {
    return delegate.isClosed();
}
```

Run:

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc -am -Dtest=NebulaResourceLifecycleTest test
```

Expected: `BUILD SUCCESS`，重复 close 仍只关闭一次，关闭后访问仍抛出 `SQLException`。

- [ ] **Step 3: 将 Nebula 不支持异常与 wrapper 委托公共模块**

`ExceptionBuilder.buildUnsupportedOperationException()` 改为返回：

```java
return GraphJdbcDefaults.unsupportedOperation();
```

`JdbcWrapperSupport.unwrap(Object target, Class<T> iface)` 和 `isWrapperFor(Object target, Class<?> iface)` 改为直接委托 `GraphJdbcWrapperSupport`。保留 Nebula 工具类包名，避免影响已有 imports。

Run:

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc -am -Dtest=NebulaUnsupportedJdbcFeatureTest test
```

Expected: `BUILD SUCCESS`，不支持能力仍抛出 `SQLFeatureNotSupportedException` 或现有测试认可的 `SQLException` 子类；`unwrap/isWrapperFor` 语义不变。

- [ ] **Step 4: 将 Nebula 参数绑定与参数元数据委托公共模块**

`NebulaParameterBindings` 内部增加：

```java
private final GraphJdbcParameterBindings delegate = new GraphJdbcParameterBindings();
```

现有 `bind`、`get`、`clear`、`asMap`、`size` 方法委托到公共类；如果 Nebula 当前存在 `NebulaParameterBinding` 值对象，则在 `asMap` 或执行器边界做双向转换，保持 `NebulaSqlExecutor.executeQuery(NebulaConnection, String, NebulaParameterBindings)` 签名不变。

`NebulaParameterMetaData` 改为持有 `GraphJdbcParameterMetaData delegate`，已有方法逐个委托；Nebula 独有返回值如已存在不同断言，以现有测试为准，仅把相同行为迁到公共层。

Run:

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc -am -Dtest=NebulaPreparedStatementBindingsTest test
```

Expected: `BUILD SUCCESS`，`setString/setNull/clearParameters/executeBatch` 与 fake executor 收到的绑定值保持不变。

- [ ] **Step 5: 将 Nebula 空 metadata 结果集形状委托公共模块**

在 `NebulaDatabaseMetaData` 中：

```java
public ResultSet getSchemas() throws SQLException {
    return GraphJdbcMetaDataSupport.emptySchemas();
}

public ResultSet getCatalogs() throws SQLException {
    return GraphJdbcMetaDataSupport.emptyCatalogs();
}

public ResultSet getTableTypes() throws SQLException {
    return GraphJdbcMetaDataSupport.tableTypes("TAG", "EDGE");
}
```

对 `getColumns`、`getPrimaryKeys`、`getIndexInfo` 只在当前逻辑需要返回空结果集时委托公共形状；已有从 Nebula 命令读取 tag/edge/index 的路径继续留在 Nebula 模块。不要把 `NebulaResultSet` 或 `NebulaNativeResultSet` 替换为公共 RowSet，除非该方法本来就是静态空形状或本地行数组形状。

Run:

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc -am -Dtest=NebulaDatabaseMetaDataTest test
```

Expected: `BUILD SUCCESS`，空 schemas/catalogs/columns/primary keys/index info 的列名、列类型稳定；Nebula index 读取测试仍能通过 fake executor。

- [ ] **Step 6: 跑 Nebula 本地稳定性与框架集成回归**

Run:

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc -am -Dtest=NebulaResourceLifecycleTest,NebulaPreparedStatementBindingsTest,NebulaDatabaseMetaDataTest,NebulaUnsupportedJdbcFeatureTest,NebulaResultSetCursorMovementTest,NebulaJdbcTemplateContractTest,NebulaMyBatisContractTest test
```

Expected: `BUILD SUCCESS`，重复 close、ResultSet 边界、wasNull、unsupported、DriverManager/JdbcTemplate/MyBatis 常见读取路径均通过本地 fake 或本地构造数据验证。

- [ ] **Step 7: 验证 Nebula 协议能力未被公共层吸收**

Run:

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework
git diff --stat -- fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/sql \
  fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/mysql
```

Expected: 只出现 `NebulaParameterBindings.java` 的小幅变更；`sql/parser`、`sql/query`、`sql/write`、`sql/trigger`、`sql/routine`、`mysql/replication` 不出现协议迁移或大规模重写。

- [ ] **Step 8: 提交 Nebula 适配**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java \
  fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/test/java
git commit -m "fw-plus-core: 让 Nebula JDBC 复用公共图 JDBC 支持"
```

### Task 6: 全量验证、依赖边界检查与 OpenSpec 对齐

**Files:**
- Verify only: `fw-plus-core/fw-plus-project/fw-plus-graph/**`
- Verify only: `openspec/changes/extract-common-graph-jdbc/tasks.md`
- Verify only: `docs/superpowers/specs/2026-07-26-extract-common-graph-jdbc-design.md`

**Interfaces:**
- Consumes: Task 1-5 的所有产出。
- Produces: 可交付状态：公共模块存在、两个具体驱动依赖公共模块、测试通过、公共模块无具体 SDK 依赖、OpenSpec tasks 可勾选。

- [ ] **Step 1: 跑公共模块测试**

Run:

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 2: 跑 Neo4j 本地测试**

Run:

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc -am -Dtest=Neo4jDriverTest test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 3: 跑 Nebula 本地核心回归**

Run:

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc -am -Dtest=NebulaResourceLifecycleTest,NebulaPreparedStatementBindingsTest,NebulaDatabaseMetaDataTest,NebulaUnsupportedJdbcFeatureTest,NebulaResultSetCursorMovementTest,NebulaJdbcTemplateContractTest,NebulaMyBatisContractTest test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 4: 跑 `fw-plus-graph` 聚合测试**

Run:

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-graph -am test
```

Expected: `BUILD SUCCESS`。如果 `Neo4jLiveJdbcSmokeTest` 或 `NebulaLiveJdbcSmokeTest` 在当前 Surefire 配置中误运行并要求真实服务，先确认现有测试约定，再将 live smoke 测试限定到显式 profile 或按已有跳过机制处理；不要用真实服务可用性作为本 change 的成功条件。

- [ ] **Step 5: 检查公共模块依赖边界**

Run:

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework
rg -n "com\\.vesoft|org\\.neo4j|druid|springframework|mybatis|nebula|neo4j" fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc
```

Expected: 无输出。若测试类中字符串包含 `neo4j` 或 `nebula`，改为中性示例词，例如 `NODE`、`EDGE`、`graph`，保持公共模块无具体驱动命名。

- [ ] **Step 6: 检查服务声明和对外入口未变**

Run:

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework
git diff -- fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/resources/META-INF/services/java.sql.Driver \
  fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc/src/main/resources/META-INF/services/java.sql.Driver
```

Expected: 无输出。

- [ ] **Step 7: 检查 OpenSpec tasks 可逐项勾选**

对照 `openspec/changes/extract-common-graph-jdbc/tasks.md`：

```text
1.1-1.3 由 Task 1 覆盖
2.1-2.5 由 Task 2 和 Task 3 覆盖
3.1-3.3 由 Task 4 覆盖
4.1-4.4 由 Task 5 覆盖
5.1-5.3 由 Task 6 覆盖
```

Expected: 每个 OpenSpec 条目都能映射到一个已执行任务；没有新增第三种图数据库驱动；没有把 Nebula SQL AST、MySQL 协议、binlog、trigger、routine 迁入公共模块。

- [ ] **Step 8: 最终提交验证收口**

```bash
git status --short
git log --oneline --max-count=6
```

Expected: 工作区只有本 change 相关文件改动；最近提交包含 Task 1-5 的提交。若团队偏好单提交，在最终集成前用非交互式方式整理提交，并保留清晰提交信息：

```bash
git commit -m "fw-plus-core: 抽象公共图数据库 JDBC 标准层"
```

## 自检清单

- [ ] 公共模块 `fw-plus-graph-jdbc` 位于 `fw-plus-graph` 聚合中，且构建顺序早于 Nebula/Neo4j。
- [ ] 公共模块没有具体图数据库 SDK、Druid、Spring、MyBatis 运行时依赖。
- [ ] Neo4j 保留 URL、Driver、连接配置、SQL-to-Cypher、Java Driver 执行器。
- [ ] Nebula 保留连接池、nGQL/SQL 转换、原生结果映射、SQL AST、MySQL 协议、binlog、trigger、routine。
- [ ] 公共测试、Neo4j 本地测试、Nebula 本地稳定性与框架集成测试都通过。
- [ ] 所有 Java 代码兼容 Java 1.8。
- [ ] `META-INF/services/java.sql.Driver` 未变化。
