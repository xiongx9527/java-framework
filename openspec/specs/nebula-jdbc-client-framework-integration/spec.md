# nebula-jdbc-client-framework-integration Specification

## Purpose
TBD - created by archiving change add-nebula-jdbc-client-framework-integration. Update Purpose after archive.
## Requirements
### Requirement: Standard JDBC usage paths are supported
系统 SHALL 支持像正常 JDBC Driver 一样使用 Nebula JDBC Driver 的常见路径，包括 DriverManager 或 DataSource 获取连接、创建 Statement/PreparedStatement、执行查询、读取 ResultSet 和关闭资源。

#### Scenario: DriverManager 查询
- **WHEN** 应用使用 DriverManager 获取 Nebula JDBC 连接并通过 Statement 执行 SELECT
- **THEN** Driver 返回可遍历 ResultSet，资源关闭行为符合 JDBC 清理习惯

#### Scenario: PreparedStatement 参数绑定
- **WHEN** 应用使用 PreparedStatement 和 `?` 参数执行查询
- **THEN** Driver 正确绑定参数、执行 SQL 并返回结果

#### Scenario: 不支持 JDBC 能力
- **WHEN** 应用调用当前不支持的事务、batch、generated keys、CallableStatement 或多结果集能力
- **THEN** Driver 返回标准 JDBC 异常，而不是静默成功或抛出非预期异常

### Requirement: JdbcTemplate can execute parameterized queries
系统 SHALL 支持 Spring JdbcTemplate 通过 Nebula JDBC Driver 执行带 `?` 参数的查询，并正确读取标量和行映射结果。

#### Scenario: queryForObject 标量查询
- **WHEN** JdbcTemplate 执行带 `?` 参数的 SELECT 标量查询
- **THEN** Nebula JDBC Driver 完成参数绑定、SQL 解析和结果读取，并返回目标标量值

#### Scenario: RowMapper POJO 映射
- **WHEN** JdbcTemplate 使用 RowMapper 读取包含列别名的查询结果
- **THEN** ResultSet 和 ResultSetMetaData 提供稳定列名、列别名和类型信息

### Requirement: MyBatis mapper queries can execute through JDBC
系统 SHALL 支持 MyBatis Mapper XML 和注解 SQL 通过 Nebula JDBC Driver 执行常见查询，并映射到 Java 对象。

#### Scenario: XML Mapper 参数查询
- **WHEN** MyBatis XML Mapper 使用 `#{id}` 参数生成 JDBC PreparedStatement 查询
- **THEN** Nebula JDBC Driver 正确处理 `?` 占位符和参数绑定并返回映射结果

#### Scenario: 注解 Mapper 查询
- **WHEN** MyBatis 注解 Mapper 执行带列别名的 SELECT 查询
- **THEN** 查询结果可映射到 Java POJO 属性

#### Scenario: 当前框架非目标边界
- **WHEN** 应用尝试通过当前能力验收 MyBatis 写入、MyBatis-Plus BaseMapper/Wrapper 或完整事务语义
- **THEN** 这些路径不属于当前能力验收范围；若调用触达当前不支持的 JDBC 能力，Driver 返回明确的标准 JDBC 异常

### Requirement: JDBC metadata supports framework mapping
系统 SHALL 提供标准 JDBC、MyBatis 和 JdbcTemplate 常见映射所需的 ResultSetMetaData、列标签、列类型和基础 DatabaseMetaData 行为。

#### Scenario: 列别名映射
- **WHEN** SQL 使用 `AS` 定义列别名
- **THEN** ResultSetMetaData 和 ResultSet 读取方法返回标准 JDBC 客户端和框架可识别的列标签

#### Scenario: 不支持 JDBC 能力
- **WHEN** 框架调用当前不支持的 batch、generated keys、CallableStatement 或复杂事务能力
- **THEN** Driver 返回明确的 SQLFeatureNotSupportedException 或 SQLException，而不是静默失败

### Requirement: Framework SQL shapes are covered by parser matrix
系统 SHALL 确保 AST 覆盖矩阵包含 MyBatis 和 JdbcTemplate 常见 SQL 形态，例如参数占位符、列别名、IN 条件和动态 SQL 展开后的条件组合。

#### Scenario: 参数占位符被覆盖
- **WHEN** 后续 parser core 验证 AST 覆盖矩阵
- **THEN** `?` 参数、IN 参数列表和列别名相关 SQL 形态均有矩阵条目

