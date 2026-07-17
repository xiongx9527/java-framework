## 1. 语义映射设计

- [x] 1.1 定义 query converter 输入/输出边界：消费 parser-core SELECT AST/IR，输出 `NgqlQueryPlan` 或等价查询计划。
- [x] 1.2 定义 SQL 表/列到 Nebula tag/edge/property/id/src/dst/rank 的显式 schema mapping 模型。
- [x] 1.3 定义受支持 SELECT、WHERE、ORDER BY、LIMIT/OFFSET、JOIN、子查询、聚合、GROUP BY、HAVING、UNION、函数和表达式白名单。
- [x] 1.4 定义 query-only 错误模型，明确拒绝写入、DDL、事务和不可映射复杂 SQL。

## 2. 转换与执行实现

- [x] 2.1 实现点查询 SQL→nGQL 转换。
- [x] 2.2 实现边查询 SQL→nGQL 转换。
- [x] 2.3 实现过滤、排序、分页转换。
- [ ] 2.4 实现可映射 JOIN、子查询、聚合、GROUP BY、HAVING、UNION 和函数表达式转换。
- [x] 2.5 提供最小查询执行适配，将转换后的 nGQL 交给现有 Nebula 执行链路，并保持 nGQL 直连路径不变。
- [x] 2.6 暴露可供 JDBC framework integration 和 MySQL read-only gateway 复用的查询计划边界。

## 3. 验证

- [x] 3.1 补充转换器单元测试和 golden tests，覆盖 IR、schema mapping、期望 nGQL、输出列描述和 trace。
- [x] 3.2 补充无法映射 SQL 的错误测试，覆盖缺失 mapping、不支持 JOIN、复杂子查询、UNION、函数和表达式。
- [x] 3.3 补充 query-only 边界测试，确认 INSERT、UPDATE、DELETE、DDL 和事务语句被明确拒绝。
- [x] 3.4 补充执行路径测试，确认转换后的 nGQL 复用现有 Nebula 执行链路且 nGQL 直连路径不经过 converter。
