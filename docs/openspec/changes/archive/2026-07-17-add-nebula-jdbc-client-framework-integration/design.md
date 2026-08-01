## Context

目标是让 Nebula JDBC Driver 像正常 JDBC Driver 一样使用。MyBatis 和 JdbcTemplate 都只是标准 JDBC 行为的上层消费者；只实现 SQL parser 或 MySQL 协议并不足以保证框架可用，还必须补齐 DriverManager/DataSource、Connection、Statement、PreparedStatement、ResultSet、ResultSetMetaData、基础 DatabaseMetaData、列标签、JDBC 类型和异常语义。

## Goals / Non-Goals

**Goals:**

- 支持标准 JDBC 查询路径，包括 DriverManager/DataSource、Connection、Statement、PreparedStatement、ResultSet 和 metadata。
- 支持 JdbcTemplate 常见查询和 RowMapper 映射。
- 支持 MyBatis XML / 注解 Mapper 常见查询和 resultMap 映射。
- 支持 PreparedStatement `?` 参数绑定。
- 支持列名、列别名、ResultSetMetaData、基础 DatabaseMetaData 和 JDBC 类型映射。
- 明确不支持能力的异常语义。

**Non-Goals:**

- 不做 MyBatis-Plus BaseMapper / Wrapper 全量适配。
- 不做 JPA/Hibernate。
- 不模拟完整关系型数据库事务。
- 不绕过 JDBC 直接接 MyBatis 插件。
- 不实现所有 DatabaseMetaData 方法，但常见探测方法必须可用或明确返回标准不支持异常。

## Decisions

- 主目标是标准 JDBC 兼容；MyBatis/JdbcTemplate 是验收框架，不做专用旁路。
- 框架 SQL 形态必须回补到 `define-nebula-sql-ast-coverage-matrix`，确保参数占位符和列别名能被 parser core 结构化。
- 查询兼容先于写入兼容；写入兼容依赖 `add-nebula-sql-write-and-ddl-support`。
- 不支持的 JDBC 能力必须明确失败，避免框架误判成功。

## Risks / Trade-offs

- **[Risk] 框架会调用大量 JDBC metadata 方法。** → Mitigation：先覆盖 MyBatis/JdbcTemplate 常见路径，其他方法明确返回不支持。
- **[Risk] 图模型与 POJO 映射存在差异。** → Mitigation：通过列别名、类型映射和 schema 映射约束明确结果形态。
- **[Risk] 动态 SQL 展开形态多样。** → Mitigation：parser 矩阵覆盖动态 SQL 展开后的常见 SQL，而不是 MyBatis XML 本身。

## Migration Plan

1. 等待 SQL parser core 和查询转换能力提供稳定 SQL 执行路径。
2. 补齐 PreparedStatement 参数绑定和 ResultSetMetaData。
3. 添加 JdbcTemplate 和 MyBatis 最小集成测试。
4. 根据测试发现补充 JDBC metadata 和类型映射。

## Open Questions

- 是否需要 Spring JDBC 和 MyBatis 作为 test scope 依赖？
- MyBatis 写入场景是否与 SQL 写入 change 同步验收？
- MyBatis-Plus 是否后续单独拆分 change？
