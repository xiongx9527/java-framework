## Why

用户希望用 SQL 查询 NebulaGraph，但现有 JDBC 入口只能直接执行 nGQL。需要在 parser 核心之上实现查询语义映射，让 SQL 查询可转换为 nGQL 并复用现有 Nebula 执行链路。

## What Changes

- 新增 SQL SELECT 到 nGQL 的转换层。
- 支持点查询、边查询、投影、过滤、排序、分页，以及复杂 SQL 查询（多表 JOIN、子查询、聚合、GROUP BY、HAVING、UNION、函数表达式）的可映射子集。
- 通过现有 `NebulaClientFacade` 执行转换后的 nGQL。
- 对无法安全映射到 Nebula 图语义的复杂 SQL 返回明确错误，并说明不支持的结构。

## Capabilities

### New Capabilities
- `nebula-sql-to-ngql-query-support`: SQL 查询到 nGQL 转换，覆盖本 change 的需求、边界和验收场景。

### Modified Capabilities

## Impact


- MySQL binlog、复制协议、存储过程和触发器不在本 change 内实现；这些能力由后续独立 changes 承接。
- 依赖 `add-nebula-sql-parser-core` 的解析结果。
- 影响 `NebulaStatementImpl` 或新增 SQL statement 执行路径。
- 可能扩展 ResultSet 元数据映射。
