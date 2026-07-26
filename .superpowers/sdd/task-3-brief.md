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

