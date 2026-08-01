# Brainstorm Summary

- Change: add-nebula-jdbc-client-framework-integration
- Date: 2026-07-17

## 确认的技术方案

采用方案 B：`fw-plus-nebula-jdbc` 做标准 JDBC 薄适配层。JDBC 层负责标准接口行为、参数绑定、metadata、异常语义和框架兼容测试；SQL parser core 与 SQL-to-nGQL converter 作为依赖能力通过明确接口消费，不在当前 change 内重复实现。

执行链路为：`Statement/PreparedStatement` 提交 SQL 文本和参数绑定，SQL 执行服务调用 parser/converter 得到 nGQL，再复用现有 `NebulaConnection.execute(nGql)` 与 `NebulaResultSet` 返回 JDBC 结果。

当前 change 只验收标准 JDBC 查询路径、Spring JdbcTemplate 查询路径和 MyBatis Mapper 查询路径。MyBatis 写入、MyBatis-Plus BaseMapper/Wrapper 和完整事务不纳入当前 change；写入能力跟随 `add-nebula-sql-write-and-ddl-support` 后续验收。

## 关键取舍与风险

- 不在 JDBC 层直接解析 SQL，避免重复 `add-nebula-sql-parser-core` 和 `add-nebula-sql-to-ngql-query-support` 的职责。
- 当前 `fw-plus-nebula-jdbc` 执行链路仍偏 nGQL，落地时需要引入 SQL 执行服务边界，并为 parser/converter 尚未完成的状态提供测试替身。
- 框架会调用大量 metadata 方法，设计只补齐 JdbcTemplate/MyBatis 常见路径，其余方法明确抛 `SQLFeatureNotSupportedException` 或标准 `SQLException`。
- 图模型与 POJO 映射依赖稳定列标签、列类型和 class name，ResultSetMetaData 是当前 change 的关键验收点。

## 测试策略

测试不依赖真实 NebulaGraph。`fw-plus-nebula-jdbc` 使用现有 JUnit/Mockito，并在 test scope 增加 Spring JDBC 与 MyBatis 最小依赖；通过 fake/mock SQL 执行链验证 DriverManager、PreparedStatement、JdbcTemplate、MyBatis XML Mapper、MyBatis 注解 Mapper/POJO 映射路径。

重点覆盖：

- PreparedStatement 参数数量、类型化绑定、缺失参数和越界参数。
- ResultSet 列标签、列名、JDBC 类型、Java class name、nullable/read-only 等 metadata。
- DatabaseMetaData 基础探测方法和不支持能力异常语义。
- JdbcTemplate `queryForObject`、RowMapper。
- MyBatis XML Mapper 参数查询、注解 Mapper、resultMap/POJO 映射。

## Spec Patch

- 回写当前 delta spec，补充 MyBatis 写入、MyBatis-Plus BaseMapper/Wrapper 和完整事务不属于当前验收；调用不支持 JDBC 能力时应明确失败。
