## 1. 语义映射设计

- [ ] 1.1 定义 SQL 表/列到 Nebula tag/edge/property/id 的映射模型。
- [ ] 1.2 定义受支持 SELECT、WHERE、ORDER BY、LIMIT、JOIN、子查询、聚合、GROUP BY、HAVING、UNION、函数和表达式子集。

## 2. 转换与执行实现

- [ ] 2.1 实现点查询 SQL→nGQL 转换。
- [ ] 2.2 实现边查询 SQL→nGQL 转换。
- [ ] 2.3 实现过滤、排序、分页转换。
- [ ] 2.4 实现可映射 JOIN、子查询、聚合、GROUP BY、HAVING、UNION 和函数表达式转换。
- [ ] 2.5 接入 JDBC Statement 查询执行路径。

## 3. 验证

- [ ] 3.1 补充转换器单元测试和 SQL 查询执行测试，覆盖复杂 SQL 可映射和不可映射场景。
- [ ] 3.2 补充无法映射 SQL 的错误测试。
