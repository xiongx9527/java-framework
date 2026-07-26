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

