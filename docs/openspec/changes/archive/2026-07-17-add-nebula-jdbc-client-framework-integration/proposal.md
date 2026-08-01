## Why

用户要求 Nebula JDBC Driver 能像正常 JDBC Driver 一样使用，并被 MyBatis、Spring JdbcTemplate 等常见 Java JDBC 上层框架自然调用。虽然 SQL parser 和 SQL→nGQL 转换提供核心语义能力，但标准 JDBC 使用还依赖 DriverManager/DataSource、Connection、Statement、PreparedStatement、ResultSet、ResultSetMetaData、DatabaseMetaData、参数绑定、列别名、类型映射和标准异常语义。

## What Changes

- 支持标准 JDBC 使用路径：DriverManager/DataSource、Connection、Statement、PreparedStatement、ResultSet 和 metadata。
- 支持 Spring JdbcTemplate 常见查询路径。
- 支持 MyBatis Mapper XML / 注解 SQL 常见查询路径。
- 支持 PreparedStatement `?` 参数占位符和参数绑定。
- 支持 ResultSetMetaData、基础 DatabaseMetaData、列名、列别名和 JDBC 类型映射。
- 支持 RowMapper、Bean 映射和 MyBatis resultMap 所需行为。
- 明确 batch、事务、generated keys、CallableStatement、多结果集和完整 DatabaseMetaData 的边界，并使用标准 JDBC 异常表达不支持能力。

## Capabilities

### New Capabilities
- `nebula-jdbc-client-framework-integration`: 覆盖标准 JDBC 兼容基础，以及 MyBatis、JdbcTemplate 等 JDBC 上层框架验收行为。

### Modified Capabilities

## Impact

- 依赖 SQL parser core、SQL→nGQL 查询支持，写入场景依赖 SQL 写入与 DDL 支持。
- 影响 `fw-plus-nebula-jdbc` 的 PreparedStatement、ResultSet、ResultSetMetaData、Statement 执行路径。
- 可能需要补充测试依赖以覆盖 Spring JdbcTemplate 和 MyBatis 最小集成测试。
