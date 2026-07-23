# fw-plus-nebula-jdbc 使用说明

`fw-plus-nebula-jdbc` 是基于 `vesoft-inc/nebula-java` 的 NebulaGraph JDBC 适配模块。它保留底层 `nebula-java` 的连接执行能力，并在其上提供 JDBC 入口、nGQL 直连执行和 SQL 子集转换能力。

当前模块采用“双驱动互斥”方案：

- 默认构建：`com.vesoft:client:3.8.4`，用于保留 NebulaGraph 3.8 Docker 环境的现有能力。
- `-Pnebula-java-5` 构建：`com.vesoft:driver:5.3.0`，用于 NebulaGraph 5.x driver 能力。
- 同一个构建产物/classpath 里不能同时出现 `com.vesoft:client` 和 `com.vesoft:driver`。模块运行时也会检测这两个 SDK 的核心类，如果同时存在会直接抛出 `SQLException`。

`nebula-java` 5.3.0 的 POM 仍以 Java 8 编译，符合 fw-plus-core 的 JDK 1.8 约束。

注意：官方 `nebula-java` 5.x driver 面向 NebulaGraph 5.x。你当前 Docker 环境如果仍是 NebulaGraph 3.8，应使用默认 `client:3.8.4` 构建；5.3 driver 对 3.8 服务端的真实连接可能失败。

## 能力概览

已支持：

- `java.sql.Driver` 自动注册，驱动类为 `com.xiongx9527.fw.plus.graph.nebula.jdbc.NebulaDriver`。
- 通过 `DriverManager.getConnection(...)` 创建 `NebulaConnection`。
- 通过 `Statement` 执行 nGQL 查询和更新语句。
- 通过 `PreparedStatement` 使用 `?` 参数占位符。
- 读取 `ResultSet` 基础类型和列元数据。
- JDBC wrapper 探测：`unwrap(...)` / `isWrapperFor(...)`。
- `Connection` 本地状态：`clientInfo`、`catalog/schema`、`typeMap`、`networkTimeout`、`nativeSQL(...)`。
- `Connection` 本地对象工厂：`createBlob()`、`createClob()`、`createNClob()`、`createSQLXML()`、`createArrayOf(...)`、`createStruct(...)`。
- `Statement` 状态：`maxRows` 会限制返回行数；`maxFieldSize`、`fetchSize`、`fetchDirection`、`queryTimeout`、`poolable`、`closeOnCompletion` 保留 JDBC 本地状态。
- 基础 batch：`Statement.addBatch(...)` / `executeBatch()` 和 `PreparedStatement.addBatch()` / `executeBatch()`。
- 常用 `PreparedStatement` 参数：基础数值、字符串、日期时间、`setBytes(...)`、`setNString(...)`、`setAsciiStream(...)`、`setUnicodeStream(...)`、`setBinaryStream(...)`、`setCharacterStream(...)`、`setNCharacterStream(...)`、`setBlob(...)`、`setClob(...)`、`setNClob(...)`、`setSQLXML(...)`、`setArray(...)`、`setRowId(...)`、`setRef(...)`、`setObject(..., SQLType)`。
- `ResultSet` 基础 getter、文本/二进制流 getter、`getBlob(...)`、`getClob(...)`、`getNClob(...)`、`getSQLXML(...)`、`getArray(...)`、`getRowId(...)`、`getRef(...)`、`getURL(...)`、`getObject(..., Class<T>)` 常用类型转换。
- `DatabaseMetaData` 框架探测方法和标准空元数据结果集。
- 原生 `nebula-java` 访问入口：`NebulaConnection.getNativeClient()`、`NebulaConnection.getNativePool()`、`NebulaConnection.getNativeSession()`。这些方法返回 `Object`，实际类型取决于当前构建 profile。
- SQL parser core、SQL to nGQL 查询转换、写入/DDL 转换。
- 存储过程子集、Trigger 子集、binlog 事件抽象。
- SQL 读写、DDL 转换、Trigger 子集、binlog 事件抽象等配套能力。

暂不承诺完整 JDBC 兼容：

- 事务、`Savepoint`、可更新 `ResultSet`、`GeneratedKeys`。
- NebulaGraph 当前没有可等价映射 JDBC 多语句事务、提交、回滚和保存点的服务端语义，因此 `setAutoCommit(false)`、`commit()`、`rollback()`、`Savepoint` 不能像关系型数据库一样实现；驱动会明确抛出 `SQLFeatureNotSupportedException`，不会伪装支持。
- 完整 `CallableStatement`。
- 外键、交叉引用等关系型约束元数据。
- 完整关系型 `DatabaseMetaData` 语义。当前 `getTables(...)` 已映射 `SHOW TAGS` / `SHOW EDGES`，`getColumns(...)` 已映射 `DESCRIBE TAG/EDGE`，`getIndexInfo(...)` 已映射 `SHOW TAG INDEXES` / `SHOW EDGE INDEXES` + `DESCRIBE ... INDEX`，`getPrimaryKeys(...)` 会暴露 Nebula 点边身份列。
- Struct、Ref、RowId 已提供 JDBC 本地对象和参数/读取桥接，但不代表 NebulaGraph 原生具备关系型 STRUCT/REF/ROWID 类型。
- 完整 MySQL SQL 语法兼容。

## Maven 依赖

在 fw-plus 内部模块中使用时，依赖当前模块即可：

```xml
<dependency>
	<groupId>com.xiongx9527.fw.plus</groupId>
	<artifactId>fw-plus-nebula-jdbc</artifactId>
	<version>${revision}</version>
</dependency>
```

如果在外部项目中使用，需要确保同时能解析 fw-plus 相关制品和当前 profile 对应的 `nebula-java` 依赖。默认是 `com.vesoft:client:3.8.4`；启用 `nebula-java-5` profile 后是 `com.vesoft:driver:5.3.0`。

## 驱动 profile

默认构建 NebulaGraph 3.8 兼容产物：

```bash
cd fw-plus-core
mvn -q -pl fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc test
```

依赖树中只应出现：

```text
com.vesoft:client:jar:3.8.4:compile
```

构建 Nebula Java 5.3 产物：

```bash
cd fw-plus-core
mvn -q -pl fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc -Pnebula-java-5 test
```

依赖树中只应出现：

```text
com.vesoft:driver:jar:5.3.0:compile
```

不要在业务项目里额外手工加入另一套 `com.vesoft` SDK。如果确实需要访问另一套 SDK，请拆成不同构建产物或不同运行进程，不能放在同一个 classpath。

## JDBC URL

URL 格式：

```text
jdbc:nebula://host1:port1,host2:port2/graphspace?key=value&key2=value2
```

其中 URL path 中的 `graphspace` 对应 NebulaGraph 图空间。在 JDBC 使用语境里，它可以理解为数据库名；连接创建后会执行 `USE <graphspace>`，`Connection.getCatalog()` / `getSchema()` 也按当前图空间返回。

示例：

```text
jdbc:nebula://127.0.0.1:9669/basketballplayer?user=root&password=nebula&maxConnsSize=10&timeout=1000
```

常用参数：

| 参数 | 说明 | 默认值 |
| --- | --- | --- |
| `user` | NebulaGraph 用户名 | 无 |
| `password` | NebulaGraph 密码 | 无 |
| `graphspace` | 图空间名，也就是 JDBC 语境里的数据库名，通常从 URL path 解析 | `default` |
| `minConnsSize` | NebulaPool 最小连接数 | `0` |
| `maxConnsSize` | NebulaPool 最大连接数 | `10` |
| `timeout` | NebulaPool 超时时间 | `0` |
| `idleTime` | 空闲时间 | `0` |
| `intervalIdle` | 空闲检测间隔 | `-1` |
| `waitTime` | 等待时间 | `0` |

也可以通过 `Properties` 传入参数。URL query 参数会覆盖同名 `Properties` 参数。

## 基础用法

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

public class NebulaJdbcExample {
	public static void main(String[] args) throws Exception {
		Properties properties = new Properties();
		properties.setProperty("user", "root");
		properties.setProperty("password", "nebula");
		properties.setProperty("maxConnsSize", "10");

		try (Connection connection = DriverManager.getConnection(
				"jdbc:nebula://127.0.0.1:9669/basketballplayer", properties);
			 Statement statement = connection.createStatement();
			 ResultSet resultSet = statement.executeQuery(
					 "SELECT id, name, age FROM player WHERE id = 'player100' LIMIT 10")) {

			while (resultSet.next()) {
				System.out.println(resultSet.getString("name"));
			}
		}
	}
}
```

说明：`SELECT` 是 JDBC SQL 主路径，需要配置 SQL 表到 Nebula TAG/EDGE 的映射后才能转换为 nGQL 执行；识别为 SQL `SELECT` 后如果缺少映射或转换失败，会按失败返回 `SQLException`。`MATCH`、`GO`、`YIELD` 等不是 SQL，它们走 nGQL 直通路径。

## PreparedStatement 用法

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class NebulaPreparedStatementExample {
	public static void main(String[] args) throws Exception {
		try (Connection connection = DriverManager.getConnection(
				"jdbc:nebula://127.0.0.1:9669/basketballplayer?user=root&password=nebula");
			 PreparedStatement statement = connection.prepareStatement(
					 "SELECT name, age FROM player WHERE id = ?")) {

			statement.setString(1, "player100");

			try (ResultSet resultSet = statement.executeQuery()) {
				while (resultSet.next()) {
					System.out.println(resultSet.getString(1));
				}
			}
		}
	}
}
```

当前 `PreparedStatement` 已支持常用基础类型参数，包括 `boolean`、`byte`、`short`、`int`、`long`、`float`、`double`、`String`、`Date`、`Time`、`Timestamp`、`null`、`URL` 和部分 `setObject` 场景。

流式参数支持以下方法，并会在执行前读入内存作为普通参数绑定：

- `setAsciiStream(...)`：按 ASCII 文本绑定为 `LONGVARCHAR`。
- `setCharacterStream(...)`：按文本绑定为 `LONGVARCHAR`。
- `setNCharacterStream(...)`：按文本绑定为 `LONGNVARCHAR`。
- `setBinaryStream(...)`：按字节数组绑定为 `LONGVARBINARY`。

NebulaGraph nGQL 没有 JDBC `VARBINARY` 的原生字面量。通过直连 nGQL 渲染 `setBytes(...)` / `setBinaryStream(...)` 时，当前会把字节数组编码为 Base64 字符串字面量；如果业务需要原样二进制语义，建议在应用层明确使用 Base64 字符串字段或原生客户端自定义协议处理。

`createBlob()`、`createClob()`、`createNClob()`、`createSQLXML()`、`createArrayOf(...)`、`createStruct(...)` 创建的是本地内存对象。`setBlob(...)`、`setClob(...)`、`setNClob(...)` 会在执行前读入内存，并分别按字节数组或字符串绑定；`setSQLXML(...)` 按 XML 字符串绑定；`setArray(...)` / `setObject(..., Object[])` 会按 Java 数组绑定，直通 nGQL 渲染时使用列表字面量；`setRowId(...)` 会按 UTF-8 字符串绑定，适合绑定 VID 字符串；`setRef(...)` 会绑定 `Ref.getObject()` 的值；`setObject(..., Struct)` 会按 `Types.STRUCT` 保留本地对象。这里提供的是 JDBC 参数便利映射，不代表 NebulaGraph 具备关系型数据库原生 LOB/Array/XML/Struct/Ref/RowId 存储语义。

`SQLXML.getSource(...)` / `setResult(...)` 支持 `StreamSource` / `StreamResult`、`DOMSource` / `DOMResult`、`SAXSource` / `SAXResult`、`StAXSource` / `StAXResult`。内部会物化为字符串，适合 JDBC 标准兼容和参数传递，不做 XML schema 校验。

## 执行更新

```java
try (Connection connection = DriverManager.getConnection(
		"jdbc:nebula://127.0.0.1:9669/basketballplayer?user=root&password=nebula");
	 Statement statement = connection.createStatement()) {

	statement.executeUpdate("INSERT INTO player(id, name, age) VALUES ('player100', 'Alice', 30)");
	statement.executeUpdate("UPDATE player SET age = 31 WHERE id = 'player100'");
	statement.executeUpdate("DELETE FROM player WHERE id = 'player100'");
}
```

说明：

- `executeUpdate(...)` 返回值不等价于传统关系型数据库的精确影响行数。
- NebulaGraph 部分写入语义可能出现服务端返回失败但部分数据已写入的情况，调用方应按 NebulaGraph 原生语义处理。
- 标准 SQL `UPDATE` / `DELETE` 需要 SQL 表映射到 Nebula TAG/EDGE 后才能转换执行。
- TAG 更新/删除必须带 VID 条件，例如 `WHERE id = 'player100'`。
- EDGE 更新/删除必须带起点和终点条件，例如 `WHERE src_id = 'player100' AND dst_id = 'team204'`；如果映射了 rank，也可以额外带 rank 条件。
- 不带身份条件的全表 `UPDATE` / `DELETE` 会被拒绝，避免误删或误改全图数据。

EDGE 示例：

```java
statement.executeUpdate(
		"UPDATE serve SET end_year = 2024 WHERE src_id = 'player100' AND dst_id = 'team204'");
statement.executeUpdate(
		"DELETE FROM serve WHERE src_id = 'player100' AND dst_id = 'team204'");
```

也可以直接执行 NebulaGraph 原生 nGQL：

```java
statement.executeUpdate("UPDATE VERTEX ON player \"player100\" SET age = 31");
statement.executeUpdate("DELETE VERTEX \"player100\"");
statement.executeUpdate("UPDATE EDGE ON serve \"player100\"->\"team204\" SET end_year = 2024");
statement.executeUpdate("DELETE EDGE serve \"player100\"->\"team204\"");
```

## ResultSet 用法

```java
try (ResultSet resultSet = statement.executeQuery("SELECT id, name FROM player LIMIT 10")) {
	while (resultSet.next()) {
		String id = resultSet.getString("id");
		System.out.println(id);
	}
}
```

## nGQL 直通

为了保留 `vesoft-inc/nebula-java` 和早期驱动的能力，`Statement` / `PreparedStatement` 仍支持直接执行 NebulaGraph nGQL：

```java
try (ResultSet resultSet = statement.executeQuery("MATCH (v) RETURN v LIMIT 10")) {
	while (resultSet.next()) {
		Object value = resultSet.getObject(1);
		System.out.println(value);
	}
}
```

建议新代码优先使用 SQL 子集；需要 NebulaGraph 专有图查询能力时，再使用 nGQL 直通。

当前 `ResultSet` 支持基础读取、列名读取、游标移动、本地 fetch 状态和元数据读取。可更新结果集相关方法不支持。

常用类型化读取：

```java
Integer age = resultSet.getObject("age", Integer.class);
String name = resultSet.getObject("name", String.class);
BigDecimal score = resultSet.getBigDecimal("score");
Reader nameReader = resultSet.getNCharacterStream("name");
Blob payload = resultSet.getBlob("payload");
Clob content = resultSet.getClob("content");
SQLXML xml = resultSet.getSQLXML("xml_text");
java.sql.Array tags = resultSet.getArray("tags");
URL homepage = resultSet.getURL("homepage");
```

## 元数据

```java
try (Connection connection = DriverManager.getConnection(url, properties)) {
	System.out.println(connection.getMetaData().getDatabaseProductName());
	System.out.println(connection.getMetaData().getDriverName());
}
```

当前 `DatabaseMetaData` 已实现常用框架探测方法，并为 `getTables(...)`、`getColumns(...)`、`getPrimaryKeys(...)`、`getIndexInfo(...)`、`getTypeInfo()` 等方法返回符合 JDBC 列结构的结果集。`getTables(...)` 会通过 `SHOW TAGS` / `SHOW EDGES` 返回 TAG/EDGE，`getColumns(...)` 会通过 `DESCRIBE TAG/EDGE` 返回字段名、Nebula 类型、JDBC 类型、顺序和可空信息。

`getPrimaryKeys(...)` 的返回是 Nebula 身份列映射：

- TAG：返回 `__vid`，表示点 VID。
- EDGE：返回 `__src`、`__dst`、`__rank`，表示边的起点、终点和 rank。

这不是关系型数据库主键约束，只是让依赖 JDBC metadata 的框架能发现 Nebula 点边身份。

`getIndexInfo(...)` 会读取 Nebula 3.8 的 `SHOW TAG INDEXES` / `SHOW EDGE INDEXES`，再通过 `DESCRIBE TAG INDEX <name>` / `DESCRIBE EDGE INDEX <name>` 展开索引字段。Nebula 索引不是唯一约束，因此返回行的 `NON_UNIQUE=true`；当调用方传 `unique=true` 时会返回空结果。

## 原生 nebula-java 访问

JDBC 标准无法表达 `nebula-java` 的全部能力。需要直接使用官方客户端能力时，可以从 `NebulaConnection` 获取原生对象：

默认 3.8 profile：

```java
import com.vesoft.nebula.client.graph.net.NebulaPool;
import com.vesoft.nebula.client.graph.net.Session;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.NebulaConnection;

try (Connection connection = DriverManager.getConnection(url, properties)) {
	NebulaConnection nebulaConnection = connection.unwrap(NebulaConnection.class);
	Session session = (Session) nebulaConnection.getNativeSession();
	NebulaPool pool = (NebulaPool) nebulaConnection.getNativePool();

	session.execute("SHOW SPACES");
}
```

`-Pnebula-java-5` profile：

```java
import com.vesoft.nebula.driver.graph.net.NebulaClient;
import com.vesoft.nebula.driver.graph.net.NebulaPool;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.NebulaConnection;

try (Connection connection = DriverManager.getConnection(url, properties)) {
	NebulaConnection nebulaConnection = connection.unwrap(NebulaConnection.class);
	NebulaClient client = (NebulaClient) nebulaConnection.getNativeClient();
	NebulaPool pool = (NebulaPool) nebulaConnection.getNativePool();

	client.execute("SHOW SPACES");
}
```

原生对象生命周期仍由 `NebulaConnection.close()` 统一管理；调用方不要手动关闭从连接中取出的 `Session`、`NebulaClient` 或 `NebulaPool`。`nebula-java` 5.x 已移除旧 `Session` API，旧版 `getNativeSession()` 方法名仅为迁移便利保留：默认 3.8 profile 返回 `Session`，5.3 profile 返回 `NebulaClient`。

默认 3.8 profile 可继续使用官方 `Session` 的 `execute(...)`、`executeWithParameter(...)`、`executeJson(...)`、`ping(...)`、`release()` 等能力；5.3 profile 可继续使用官方 `NebulaClient` 的 `execute(...)`、`ping(...)`、`getSessionId()`、`getVersion()`、`scanNode(...)`、`scanEdge(...)` 等能力。这些属于官方 native API，不受 JDBC 标准接口限制。

## 不支持能力和原因

NebulaGraph 不是关系型数据库，因此本模块不会强行模拟所有 JDBC 语义：

- 事务：不支持 `setAutoCommit(false)`、`commit()`、`rollback()`。NebulaGraph 没有 JDBC 事务提交/回滚模型，`DatabaseMetaData.supportsTransactions()` 返回 `false`，`getDefaultTransactionIsolation()` 返回 `Connection.TRANSACTION_NONE`。
- `Savepoint`：不支持 `setSavepoint(...)`、`rollback(Savepoint)`、`releaseSavepoint(...)`。保存点依赖事务上下文，NebulaGraph 没有对应原生能力，`DatabaseMetaData.supportsSavepoints()` 返回 `false`。
- 可更新 `ResultSet`：不支持 `updateXxx(...)`、`insertRow()`、`updateRow()`、`deleteRow()`。Nebula 查询结果可能是点、边、路径、标量表达式或聚合结果，不能稳定映射为 JDBC 可定位、可回写的一行表记录。结果集保持 `CONCUR_READ_ONLY`。
- 自动生成键：不支持 `Statement.RETURN_GENERATED_KEYS` 和 `getGeneratedKeys()`。Nebula VID、边起点、终点和 rank 通常由调用方指定，不等价于关系型数据库自增键，`DatabaseMetaData.supportsGetGeneratedKeys()` 返回 `false`。
- 完整 `CallableStatement`：不支持 `Connection.prepareCall(...)` 返回标准可调用语句。模块内只提供自定义的过程子集，不承诺 JDBC 存储过程的 OUT 参数、INOUT 参数、多个结果集、游标参数等完整语义。
- 外键和交叉引用约束：不支持关系型外键约束元数据。`getImportedKeys(...)`、`getExportedKeys(...)`、`getCrossReference(...)` 返回符合 JDBC 列结构的空结果集。Nebula EDGE 表达图关系，但不是数据库强制的 foreign key constraint。
- 关系型专用类型的原生语义：`Blob`、`Clob`、`NClob`、`SQLXML`、`Array`、`Struct`、`Ref`、`RowId` 已提供参数/读取层面的本地桥接；这些对象会被转换为字符串、字节数组、数组或本地对象传递，不代表 NebulaGraph 有对应的原生存储类型、引用类型或 RowId 生命周期。

这些能力如果强行做成“看似支持”，会让上层框架误判数据库能力，反而导致事务回滚无效、结果集回写不可预期、外键信息错误等问题。因此本模块对无法真实表达的标准 JDBC 能力保持明确失败或标准空结果。

## 后续边界

为了提高 JDBC 生态兼容度，同时不破坏现有 nGQL / SQL 能力，后续只建议继续补齐有明确 NebulaGraph 映射或 JDBC 本地对象语义的方法：

1. 继续补齐可映射的 `PreparedStatement.setObject(...)` 和 `ResultSet.getObject(..., Class<T>)` 边界类型。
2. 继续完善 `DatabaseMetaData` 中能由 Nebula schema / stats / config 命令可靠回答的元数据。
3. 明确记录所有 NebulaGraph 无等价语义的方法，保持标准 `SQLFeatureNotSupportedException`。

不建议强行模拟事务、可更新结果集、完整 `CallableStatement`、外键约束和自增生成键；这些能力没有可靠的 NebulaGraph 原生语义。

## 构建

在 `fw-plus-core` 仓库内使用 Maven Wrapper 构建：

```bash
cd fw-plus-core
./mvnw clean install -pl fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc -am -DskipTests
```

构建 5.3 driver 产物：

```bash
cd fw-plus-core
./mvnw clean install -pl fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc -am -DskipTests -Pnebula-java-5
```

如果本地 Maven Wrapper 缺少 `.mvn/wrapper` 文件，需要先恢复 wrapper 文件，或按项目约定使用可用的 Maven 环境执行同等命令。

## 本地 NebulaGraph smoke 测试

模块提供一个默认跳过的真实服务 smoke 测试：`NebulaLiveJdbcSmokeTest`。启用时需要传入本地 NebulaGraph 连接信息和已存在的 graph space：

```bash
cd fw-plus-core
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home \
mvn -q -pl fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc \
	-Dtest=NebulaLiveJdbcSmokeTest \
	-Dnebula.jdbc.live=true \
	-Dnebula.jdbc.live.host=127.0.0.1 \
	-Dnebula.jdbc.live.port=9669 \
	-Dnebula.jdbc.live.user=root \
	-Dnebula.jdbc.live.password=nebula \
	-Dnebula.jdbc.live.space=<已有图空间名> \
	test
```

如果没有传 `nebula.jdbc.live.space`，该测试会跳过；如果传入的图空间不存在，连接阶段会返回 NebulaGraph 的 `SpaceNotFound` 错误。

如果需要同时验证真实 Nebula metadata 映射，可以额外加：

```bash
-Dnebula.jdbc.live.metadata=true
```

已知兼容性：本机 Docker `vesoft/nebula-graphd:v3.8.0` 应使用默认 `com.vesoft:client:3.8.4`。使用 `-Pnebula-java-5` 的 `com.vesoft:driver:5.3.0` smoke 连接 3.8 服务端时，曾在连接阶段返回 `UNAVAILABLE: Network closed for unknown reason`。这是 driver 5.x 面向 NebulaGraph 5.x 的兼容边界，不是 JDBC SQL/ResultSet 适配层的单元测试失败。

该开关会在指定图空间内创建临时 TAG、EDGE 和索引，验证 `DatabaseMetaData.getPrimaryKeys(...)` / `getIndexInfo(...)` 后自动清理。
