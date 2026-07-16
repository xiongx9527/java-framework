## 1. Druid 版本与枚举策略

- [x] 1.1 确认 Druid SQL Parser 版本、许可证和 Java 8 兼容性。
- [x] 1.2 定义 Druid 版本升级时的 AST 覆盖矩阵更新规则。
- [x] 1.3 制定 MySQL parser AST 类型枚举方法。

## 2. 覆盖矩阵设计

- [x] 2.1 定义 Nebula AST 顶层分类和表达式分类。
- [x] 2.2 建立 Druid AST 到 Nebula AST 的覆盖矩阵字段结构。
- [x] 2.3 覆盖 SELECT、JOIN、子查询、聚合、GROUP BY、HAVING、UNION、函数、表达式、别名和参数占位符。
- [x] 2.4 覆盖 INSERT、UPDATE、DELETE 和基础 DDL。
- [x] 2.5 覆盖正常 JDBC/MyBatis/JdbcTemplate 常见 SQL 形态：`?` 参数、SELECT 1、SELECT COUNT(*)、列别名、IN 条件、分页、SHOW/DESCRIBE、metadata 探测 SQL 和动态 SQL 展开后的条件组合。

## 3. 测试策略与交付

- [x] 3.1 定义 adapter 完整性测试策略，要求矩阵内节点缺失适配时测试失败。
- [x] 3.2 定义 raw fallback 禁止规则和测试断言。
- [ ] 3.3 输出覆盖矩阵文档或测试资源，供 `add-nebula-sql-parser-core` 使用。
