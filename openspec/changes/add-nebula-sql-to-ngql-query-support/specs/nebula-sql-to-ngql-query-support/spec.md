## ADDED Requirements

### Requirement: SQL SELECT can be converted to nGQL
系统 SHALL 将受支持 SQL SELECT 查询转换为等价或受控等价的 nGQL。

#### Scenario: 点查询转换
- **WHEN** `SELECT * FROM person WHERE id = 'p1'` 可转换为针对 person tag 的 nGQL 查询。
- **THEN** 系统产生符合要求的结果

#### Scenario: 过滤排序分页转换
- **WHEN** 包含 WHERE、ORDER BY、LIMIT 的受支持 SELECT 可转换为包含对应条件的 nGQL。
- **THEN** 系统产生符合要求的结果

### Requirement: Edge and relationship queries are supported within defined bounds
系统 SHALL 支持边查询和受限关系映射，并明确不能映射的 JOIN 或图遍历边界。

#### Scenario: 边查询
- **WHEN** 查询 edge 映射表
- **THEN** 转换器生成对应 edge 查询 nGQL。

#### Scenario: 不支持的 JOIN
- **WHEN** 输入无法映射到 Nebula 图关系的 JOIN 
- **THEN** 返回明确错误。

### Requirement: Complex SQL queries are converted when graph semantics are defined
系统 SHALL 支持复杂 SQL 查询的可映射子集，包括 JOIN、子查询、聚合、GROUP BY、HAVING、UNION、函数和表达式；当语义无法安全映射到 NebulaGraph 时，系统 SHALL 返回明确错误。

#### Scenario: JOIN 映射为图关系查询
- **WHEN** SQL JOIN 可以根据 schema 映射规则对应到 tag/edge 关系
- **THEN** 转换器生成等价或受控等价的 nGQL 图关系查询

#### Scenario: 聚合和分组转换
- **WHEN** SQL 包含可由 nGQL 表达的聚合、GROUP BY 或 HAVING
- **THEN** 转换器生成包含对应聚合和过滤语义的 nGQL

#### Scenario: 不可映射复杂 SQL
- **WHEN** SQL 包含无法映射到 NebulaGraph 的复杂子查询、UNION 或函数表达式
- **THEN** 转换器拒绝执行并返回包含不支持结构的明确错误

### Requirement: Converted queries use existing Nebula execution path
系统 SHALL 复用现有 Nebula client façade 执行转换后的 nGQL，并返回 JDBC ResultSet。

#### Scenario: SQL 查询执行
- **WHEN** JDBC 调用方执行受支持 SQL SELECT 
- **THEN** 内部转换为 nGQL 后通过现有执行链路返回结果集。

