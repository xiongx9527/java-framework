# fw-plus-neo4j-jdbc 使用说明

`fw-plus-neo4j-jdbc` 是 fw-plus graph 模块下的 Neo4j JDBC 适配模块，面向 Java 8。底层使用 `org.neo4j.driver:neo4j-java-driver:4.4.26`，不依赖 Neo4j 官方新版 JDBC。

若运行环境是 Java 17 及以上，并直接使用 LangChain4j `0.35.0` 原始 `langchain4j-neo4j`，则应按上游依赖使用 `org.neo4j.driver:neo4j-java-driver:5.15.0` 和 `org.neo4j:neo4j-cypher-dsl-schema-name-support:2023.7.0`，不要用本 JDBC 模块的 `4.4.26` 作为 LangChain4j Java 17 依赖版本。

## JDBC URL

```text
jdbc:neo4j://127.0.0.1:7687/neo4j
jdbc:neo4j+s://host:7687/database
jdbc:bolt://host:7687/database
```

URL path 中的 `neo4j` 是 Neo4j database 名。

## 示例

```java
Properties properties = new Properties();
properties.setProperty("user", "neo4j");
properties.setProperty("password", "Neo4j@123456");

try (Connection connection = DriverManager.getConnection(
		"jdbc:neo4j://127.0.0.1:7687/neo4j", properties);
	 Statement statement = connection.createStatement();
	 ResultSet resultSet = statement.executeQuery("MATCH (n) RETURN n LIMIT 10")) {
	while (resultSet.next()) {
		Object node = resultSet.getObject("n");
	}
}
```

也可以执行当前模块支持的 SQL 子集：

```java
try (Connection connection = DriverManager.getConnection(
		"jdbc:neo4j://127.0.0.1:7687/neo4j", properties);
	 PreparedStatement statement = connection.prepareStatement(
			 "SELECT name, age FROM Person WHERE id = ?")) {
	statement.setString(1, "p1");
	try (ResultSet resultSet = statement.executeQuery()) {
		while (resultSet.next()) {
			System.out.println(resultSet.getString("name"));
		}
	}
}
```

## 支持能力

- `java.sql.Driver` 自动注册，驱动类为 `com.xiongx9527.fw.plus.graph.neo4j.jdbc.Neo4jDriver`。
- `DriverManager.getConnection(...)`。
- `Statement.executeQuery(...)` / `execute(...)` / `executeUpdate(...)`，会按 JDBC 语义区分查询结果和更新计数。
- `PreparedStatement` 的 `?` 参数绑定，会转换成 Cypher `$p1`、`$p2` 参数；禁止在 PreparedStatement 执行方法中再次传 SQL。
- SQL 子集转换为 Cypher：
	- `SELECT col1, col2 FROM Label WHERE prop = ? ORDER BY prop DESC LIMIT 10`
	- `INSERT INTO Label(id, name, age) VALUES (?, ?, ?)`
	- `UPDATE Label SET age = ? WHERE id = ?`
	- `DELETE FROM Label WHERE id = ?`
- Cypher 直通：`MATCH`、`RETURN`、`CREATE`、`MERGE`、`UNWIND`、`CALL` 等 Cypher 语句会直接执行。
- 基础 batch：`Statement.addBatch(...)` / `executeBatch()` 和 `PreparedStatement.addBatch()` / `executeBatch()`。
- JDBC 管理事务：`setAutoCommit(false)`、`commit()`、`rollback()` 已映射到 Neo4j 原生事务；`setAutoCommit(true)` 会提交当前事务。
- `Connection` 本地状态：`clientInfo`、`catalog/schema`、`readOnly`、`typeMap`、`networkTimeout`、`nativeSQL(...)`。
- `Connection` 本地对象工厂：`createBlob()`、`createClob()`、`createNClob()`、`createSQLXML()`、`createArrayOf(...)`、`createStruct(...)`。
- `Statement` 本地状态：`maxRows`、`maxFieldSize`、`fetchSize`、`fetchDirection`、`queryTimeout`、`poolable`、`closeOnCompletion`。
- 常用 `PreparedStatement` 参数：基础类型、`setBytes(...)`、`setAsciiStream(...)`、`setUnicodeStream(...)`、`setBinaryStream(...)`、`setCharacterStream(...)`、`setNCharacterStream(...)`、`setBlob(...)`、`setClob(...)`、`setNClob(...)`、`setSQLXML(...)`、`setArray(...)`、`setRowId(...)`、`setRef(...)`、`setObject(...)`。
- `PreparedStatement.getParameterMetaData()` 会按 `?` 占位符数量返回输入参数元数据；具体类型在执行前无法从 Cypher/SQL 子集可靠推断，因此返回 `JAVA_OBJECT`。
- `Blob` / `Clob` / `NClob` / `SQLXML` / `Array` / `Struct` / `RowId` / `Ref` 本地 JDBC 对象。
- `SQLXML.getSource(...)` / `setResult(...)` 支持 `Stream`、`DOM`、`SAX`、`StAX` 类型，内部物化为 XML 字符串。
- `ResultSet` 基础读取和列元数据，当前由内存 `CachedRowSet` 承载。
- `DatabaseMetaData` 常用框架探测方法，并为 `getTables(...)`、`getColumns(...)`、`getSchemas(...)`、`getCatalogs()`、`getTableTypes()`、`getPrimaryKeys(...)`、`getIndexInfo(...)` 返回符合 JDBC 列结构的结果集；事务、批处理、ResultSet 类型等能力探测会按当前实现返回。
- JDBC wrapper 探测：`unwrap(...)` / `isWrapperFor(...)`。

## 边界

- 查询语言以 Cypher 为主，不承诺完整标准 SQL。当前 SQL 转换只覆盖单 Label 的基础 CRUD；`JOIN`、子查询、聚合、分组、多表关系映射、复杂表达式不会伪装支持，会抛出 `SQLException`。
- SQL 子集中的 `INSERT` 映射为创建节点，`UPDATE` / `DELETE` 映射为按 Label 匹配节点后更新/删除；它不是关系型数据库的行表语义。
- `executeUpdate(...)` 的返回值来自 Neo4j counters 汇总，不等价于传统关系型数据库的精确影响行数。
- 不支持 `Savepoint`、可更新 `ResultSet`、真实服务端 `GeneratedKeys`、完整 `CallableStatement`、外键/交叉引用约束。`Statement.getGeneratedKeys()` 会返回符合 JDBC 列结构的空结果集，避免框架探测时异常，但 Neo4j 不会像关系型自增主键一样生成键值。
- 事务隔离级别按 Neo4j 原生事务语义处理，JDBC 层只暴露 `TRANSACTION_READ_COMMITTED` 兼容值，不支持切换为其它关系型隔离级别。
- `DatabaseMetaData.getPrimaryKeys(...)`、`getIndexInfo(...)` 当前提供标准列结构；Neo4j 没有关系型主键/外键语义，后续只能映射 Neo4j constraint/index 元数据，不能等同关系型约束。
- `Blob` / `Clob` / `NClob` / `SQLXML` / `Array` / `Struct` / `RowId` / `Ref` 是 JDBC 本地兼容对象和参数桥接，不代表 Neo4j 原生具备这些 SQL 类型。
- 不要和 Neo4j 官方 JDBC 同时放入同一个 classpath，避免 `jdbc:neo4j:` URL 的 DriverManager 选择冲突。

## 本地 smoke 测试

```bash
cd fw-plus-core
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home \
mvn -q -pl fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc \
	-Dtest=Neo4jLiveJdbcSmokeTest \
	-Dneo4j.jdbc.live=true \
	-Dneo4j.jdbc.live.host=127.0.0.1 \
	-Dneo4j.jdbc.live.port=7687 \
	-Dneo4j.jdbc.live.database=neo4j \
	-Dneo4j.jdbc.live.user=neo4j \
	-Dneo4j.jdbc.live.password='Neo4j@123456' \
	test
```
