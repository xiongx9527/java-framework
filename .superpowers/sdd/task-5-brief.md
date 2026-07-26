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

