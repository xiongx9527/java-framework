## 1. JDBC 框架兼容边界

- [ ] 1.1 梳理 JdbcTemplate 常见调用路径和所需 JDBC 方法。
- [ ] 1.2 梳理 MyBatis XML / 注解 Mapper 常见调用路径和所需 JDBC 方法。
- [ ] 1.3 定义不支持的 batch、generated keys、CallableStatement、事务和 DatabaseMetaData 边界。

## 2. JDBC 行为补强

- [ ] 2.1 支持 PreparedStatement `?` 参数绑定和参数传递到 SQL parser / 转换层。
- [ ] 2.2 补齐 ResultSetMetaData、列名、列别名和 JDBC 类型映射。
- [ ] 2.3 明确不支持 JDBC 能力的异常语义。

## 3. 框架集成验证

- [ ] 3.1 补充 JdbcTemplate queryForObject 和 RowMapper 测试。
- [ ] 3.2 补充 MyBatis XML Mapper 参数查询测试。
- [ ] 3.3 补充 MyBatis 注解 Mapper 和 resultMap / POJO 映射测试。
- [ ] 3.4 将 MyBatis/JdbcTemplate 常见 SQL 形态回补到 AST 覆盖矩阵。
