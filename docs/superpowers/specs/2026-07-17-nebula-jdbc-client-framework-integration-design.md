---
comet_change: add-nebula-jdbc-client-framework-integration
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-17-add-nebula-jdbc-client-framework-integration
status: final
---

# Nebula JDBC 客户端框架集成技术设计

## 1. 背景与目标

`fw-plus-nebula-jdbc` 当前已有 `NebulaDriver`、`NebulaConnection`、`NebulaStatementImpl`、`NebulaPreparedStatementImpl`、`NebulaResultSet`、`NebulaResultSetMetaData` 和 `NebulaDatabaseMetaData` 基础实现，但执行链路主要面向直接 nGQL 调用。`PreparedStatement` 现有参数处理以字符串替换为主，metadata 方法覆盖较少，无法稳定满足 Spring JdbcTemplate 和 MyBatis 等标准 JDBC 上层框架的调用习惯。

本 change 的目标是让 Nebula JDBC Driver 按标准 JDBC 查询路径被使用，并以 JdbcTemplate、MyBatis XML Mapper、MyBatis 注解 Mapper 作为验收消费者。设计重点不是在 JDBC 层重新实现 SQL parser 或 SQL-to-nGQL converter，而是在 JDBC 层建立清晰的薄适配边界，消费 parser/converter 能力并补齐框架所需的参数绑定、ResultSet metadata、DatabaseMetaData 和异常语义。

## 2. 范围与非目标

### 范围

- 修改 `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc` 及其测试。
- 支持 DriverManager/DataSource 获取连接、创建 Statement/PreparedStatement、执行查询、读取 ResultSet 和关闭资源。
- 支持 PreparedStatement `?` 参数绑定，参数以类型化结构传递给 SQL 执行服务。
- 补齐 JdbcTemplate 和 MyBatis 查询路径需要的 ResultSet、ResultSetMetaData、基础 DatabaseMetaData 行为。
- 补齐列标签、列名、JDBC 类型、Java class name、nullable/read-only 等框架映射常用 metadata。
- 明确 batch、generated keys、CallableStatement、多结果集、完整事务等不支持能力的标准 JDBC 异常语义。
- 增加不依赖真实 NebulaGraph 的标准 JDBC、JdbcTemplate、MyBatis 最小集成测试。

### 非目标

- 不在 JDBC 层重复实现 SQL parser core。
- 不在 JDBC 层重复实现 SQL-to-nGQL converter。
- 不实现 MyBatis-Plus BaseMapper / Wrapper 全量适配。
- 不实现 JPA/Hibernate。
- 不验收 MyBatis 写入路径；写入跟随 `add-nebula-sql-write-and-ddl-support` 后续验收。
- 不模拟完整关系型数据库事务。
- 不实现完整 DatabaseMetaData，只覆盖常见框架探测路径，其余能力明确失败。

## 3. 推荐方案

采用“JDBC 薄适配层 + SQL 执行服务边界”方案。

`Statement` 和 `PreparedStatement` 不直接承担 SQL 解析和 nGQL 转换。它们统一把 SQL 文本、参数绑定和执行意图交给 SQL 执行服务。SQL 执行服务消费并行 change 提供的 parser-core 与 SQL-to-nGQL converter，得到 nGQL 后复用现有 `NebulaConnection.execute(nGql)` 与 `NebulaResultSet` 返回标准 JDBC 结果。

该方案保持 JDBC 层职责清晰：

- JDBC 层负责标准接口、参数绑定、metadata、资源状态和异常语义。
- Parser-core 负责 SQL 到稳定中间表示。
- SQL-to-nGQL converter 负责将受支持 SQL 查询转换为 nGQL。
- Nebula client façade 继续负责真实 nGQL 执行。

## 4. 内部组件设计

### 4.1 `NebulaSqlExecutor`

新增 SQL 执行服务边界，建议放在 `com.vesoft.nebula.jdbc.sql` 或等价内部包。

职责：

- 接收原始 SQL、参数绑定列表和执行类型。
- 调用 parser-core 生成 SQL 中间表示。
- 调用 SQL-to-nGQL converter 生成 nGQL。
- 调用 `NebulaConnection.execute(nGql)` 执行。
- 返回 JDBC `ResultSet` 或执行结果。

建议接口：

```java
public interface NebulaSqlExecutor {
	ResultSet executeQuery(NebulaConnection connection, String sql, NebulaParameterBindings bindings) throws SQLException;
	int executeUpdate(NebulaConnection connection, String sql, NebulaParameterBindings bindings) throws SQLException;
}
```

在 parser/converter 落地前，测试可使用 fake executor 验证 JDBC 层提交的 SQL 和参数是否正确，不用真实 NebulaGraph。

### 4.2 `NebulaParameterBindings`

新增 PreparedStatement 参数模型，替代长期依赖字符串替换。

职责：

- 按 1-based JDBC 参数索引保存参数。
- 保存参数值、JDBC 类型和可选 scale/length。
- 校验索引必须在合法范围内。
- 在执行前校验所有 `?` 参数均已绑定。
- 为 parser/converter 提供稳定、类型化的参数视图。

最小支持类型：

- `setObject(int, Object)`
- `setObject(int, Object, int)`
- `setString`
- `setInt`
- `setLong`
- `setBoolean`
- `setDouble`
- `setDate`
- `setTimestamp`
- `setNull`

不支持的流式、LOB、SQLXML、Array 等参数类型返回 `SQLFeatureNotSupportedException`。

### 4.3 `NebulaPreparedStatementImpl`

调整 PreparedStatement 行为：

- 构造时统计 `?` 参数数量。
- `setXxx` 方法写入 `NebulaParameterBindings`，不直接替换 SQL 字符串。
- `executeQuery()` 校验查询语义和参数完整性后调用 `NebulaSqlExecutor.executeQuery(...)`。
- `executeUpdate()` 当前不作为 MyBatis 写入验收目标；如果 converter/write change 尚未提供能力，应明确返回标准 JDBC 异常。
- `executeQuery(String)`、`executeUpdate(String)`、`execute(String)` 等带 SQL 参数的 PreparedStatement 方法继续按 JDBC 规范拒绝调用。

保留现有 `NebulaPreparedStatement` 对外类型，但内部状态从 `Map<Object,Object>` 过渡到参数绑定模型。

### 4.4 `NebulaStatementImpl`

调整 Statement 查询路径：

- `executeQuery(String sql)` 不再假定输入是 nGQL；它把 SQL 交给 `NebulaSqlExecutor`。
- 现有 nGQL 直连能力如需保留，应通过明确内部方法或兼容模式隔离，避免 SQL 与 nGQL 混用导致框架路径不可预测。
- `executeUpdate(String sql)` 当前不作为本 change 的查询验收重点；当写入 converter 不可用时明确失败。
- `getResultSet()`、`getUpdateCount()`、`getMoreResults()` 等框架可能调用的方法应提供稳定返回或标准不支持异常。

### 4.5 `NebulaResultSetMetaData`

补齐框架映射所需方法：

- `getColumnCount`
- `getColumnName`
- `getColumnLabel`
- `getColumnType`
- `getColumnTypeName`
- `getColumnClassName`
- `isNullable`
- `isReadOnly`
- `isWritable`
- `isDefinitelyWritable`
- `isAutoIncrement`
- `isCaseSensitive`
- `isSigned`
- `getPrecision`
- `getScale`

列标签优先使用 SQL projection alias；没有 alias 时使用原列名或 converter 输出的稳定列名。JDBC 类型映射由结果列描述提供，缺失时使用保守类型：

- 字符串：`Types.VARCHAR` / `java.lang.String`
- 整数：`Types.BIGINT` 或 `Types.INTEGER`
- 浮点：`Types.DOUBLE`
- 布尔：`Types.BOOLEAN`
- 日期时间：`Types.TIMESTAMP`
- 其他：`Types.JAVA_OBJECT` / `java.lang.Object`

### 4.6 `NebulaResultSet`

确保 ResultSet 满足框架读取习惯：

- `findColumn(String columnLabel)` 支持列标签。
- `getObject(String)`、`getString(String)`、`getInt(String)` 等按 label 读取稳定工作。
- `wasNull()` 不出现 NPE，并在读取列后返回最近列空值状态。
- `getMetaData()` 返回上述增强后的 `NebulaResultSetMetaData`。
- 关闭后访问、游标未定位、列索引越界均返回明确 `SQLException`。

### 4.7 `NebulaDatabaseMetaData`

只实现 JdbcTemplate/MyBatis 常见探测所需基础方法：

- `getConnection`
- `getURL`
- `getUserName`
- `getDatabaseProductName`
- `getDatabaseProductVersion`
- `getDriverName`
- `getDriverVersion`
- `getDriverMajorVersion`
- `getDriverMinorVersion`
- `isReadOnly`
- `supportsTransactions`
- `getDefaultTransactionIsolation`
- `supportsBatchUpdates`
- `supportsGetGeneratedKeys`
- `supportsMultipleResultSets`
- `supportsNamedParameters`
- `supportsResultSetType`
- `supportsResultSetConcurrency`
- `getIdentifierQuoteString`

无法安全支持的方法返回 `SQLFeatureNotSupportedException`。布尔 capability 方法优先返回明确 `false`，避免框架把“不支持”误判成可用能力。

## 5. 数据流

### 5.1 Statement 查询

```text
Statement.executeQuery(sql)
  -> NebulaStatementImpl.checkClosed()
  -> NebulaSqlExecutor.executeQuery(connection, sql, emptyBindings)
      -> parser-core.parse(sql)
      -> sql-to-ngql converter.convert(ast, emptyBindings)
      -> NebulaConnection.execute(ngql)
      -> NebulaResultSet
  -> ResultSet
```

### 5.2 PreparedStatement 查询

```text
Connection.prepareStatement(sql)
  -> NebulaPreparedStatementImpl(sql)
      -> count '?' placeholders
      -> create NebulaParameterBindings

PreparedStatement.setXxx(index, value)
  -> NebulaParameterBindings.bind(index, value, jdbcType)

PreparedStatement.executeQuery()
  -> validate all placeholders are bound
  -> NebulaSqlExecutor.executeQuery(connection, sql, bindings)
      -> parser-core.parse(sql)
      -> sql-to-ngql converter.convert(ast, bindings)
      -> NebulaConnection.execute(ngql)
      -> NebulaResultSet
```

### 5.3 框架映射

```text
JdbcTemplate / MyBatis
  -> JDBC PreparedStatement
  -> setXxx parameters
  -> executeQuery
  -> ResultSet.next()
  -> ResultSet.getMetaData()
  -> getColumnLabel / getColumnType / getColumnClassName
  -> ResultSet.getObject(label/index)
  -> scalar / RowMapper / POJO resultMap
```

## 6. 错误处理

- 参数索引小于 1 或大于参数数量：`SQLException`，包含参数索引和参数总数。
- 执行前存在未绑定参数：`SQLException`，包含缺失参数索引。
- 不支持参数类型：`SQLFeatureNotSupportedException`。
- Parser 语法错误：转换为 `SQLException`，保留错误位置或原因。
- Converter 不支持 SQL 结构：转换为 `SQLFeatureNotSupportedException` 或包含稳定错误码的 `SQLException`。
- 不支持 batch、generated keys、CallableStatement、多结果集、完整事务：返回明确标准 JDBC 异常，不静默成功。
- metadata 无法提供完整信息时：常见映射方法返回保守值；高阶 metadata 方法明确不支持。

## 7. 测试策略

测试不依赖真实 NebulaGraph，不引入 Testcontainers。`fw-plus-nebula-jdbc` 已有 JUnit 和 Mockito，当前 change 可在 test scope 增加 Spring JDBC 与 MyBatis 最小依赖。MyBatis 版本由 core BOM 管理；Spring JDBC 版本跟随 Spring Boot 2.7.18 依赖管理。

### 7.1 标准 JDBC 测试

- DriverManager 获取连接。
- Statement 查询返回可遍历 ResultSet。
- PreparedStatement 参数绑定、缺失参数、越界参数。
- ResultSet label/index 读取。
- ResultSetMetaData 列标签、列名、JDBC 类型、class name。
- DatabaseMetaData 基础探测方法。
- 不支持 batch、generated keys、CallableStatement、多结果集、事务能力的异常语义。

### 7.2 JdbcTemplate 测试

- `queryForObject(sql, args, requiredType)` 标量查询。
- `query(sql, args, RowMapper)` POJO 映射。
- RowMapper 读取列标签和列类型。
- JdbcTemplate 常见 metadata 探测不触发非预期异常。

### 7.3 MyBatis 测试

- XML Mapper `#{id}` 参数查询。
- 注解 Mapper 参数查询。
- 列别名映射到 POJO 属性。
- `resultMap` 映射。
- 不包含 MyBatis 写入和 MyBatis-Plus BaseMapper/Wrapper 验收。

### 7.4 AST 矩阵回补

把 JdbcTemplate/MyBatis 常见 SQL 形态回补到已归档的 AST 覆盖矩阵事实源或后续 parser-core 测试输入中：

- `?` 参数占位符。
- `SELECT 1`。
- `SELECT COUNT(*)`。
- 列别名。
- `IN` 参数列表。
- 动态 SQL 展开后的可选 WHERE 条件。
- SHOW/DESCRIBE 或框架 metadata 探测 SQL。

## 8. 实施顺序

1. 增加 SQL 执行服务边界和参数绑定模型。
2. 调整 PreparedStatement 参数绑定和执行路径。
3. 调整 Statement 查询执行路径。
4. 补齐 ResultSet / ResultSetMetaData 框架映射方法。
5. 补齐基础 DatabaseMetaData 和不支持能力异常。
6. 增加标准 JDBC、JdbcTemplate、MyBatis 测试。
7. 将框架 SQL 形态回补到 AST 覆盖矩阵相关测试输入。

## 9. 风险与缓解

- Parser/converter 与当前 change 并行推进，接口可能尚未落地。缓解：先定义 JDBC 层消费接口，测试使用 fake executor 验证 JDBC 行为；真实接入在 parser/converter 可用后完成。
- metadata 方法数量多，容易被框架探测路径遗漏。缓解：以 JdbcTemplate/MyBatis 最小集成测试驱动补齐常见方法，其他方法明确不支持。
- 图模型与 POJO 映射存在差异。缓解：依赖列标签、类型映射和 converter 输出列描述，而不是让 JDBC 层猜测图语义。
- 旧 nGQL 直连用法可能与 SQL 查询路径混用。缓解：在设计和实现中明确 SQL 执行服务入口，保留兼容能力时隔离命名和测试。
