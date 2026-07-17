## ADDED Requirements

### Requirement: Query converter consumes parser-core IR
系统 SHALL 只消费 `add-nebula-sql-parser-core` 提供的稳定 AST 或中间表示来进行 SQL SELECT 到 nGQL 的转换，不得在查询转换层直接解析 SQL 字符串，也不得把 Druid AST 作为公共转换契约。

#### Scenario: 转换器消费 SELECT IR
- **WHEN** parser core 返回受支持 SELECT 的稳定 IR
- **THEN** 查询转换器基于该 IR 和 schema mapping 生成 nGQL 查询计划。

#### Scenario: 转换层不重新解析 SQL
- **WHEN** 调用 SQL 查询转换能力
- **THEN** 转换器不直接调用 SQL parser 或 Druid AST API 作为语义入口。

### Requirement: Query conversion is read-only
系统 SHALL 将本能力限制为 query-only SQL SELECT 转换，并拒绝写入、DDL 和事务语句。

#### Scenario: 写入和 DDL 被拒绝
- **WHEN** parser IR 表示 INSERT、UPDATE、DELETE、DDL 或事务语句
- **THEN** 查询转换器返回明确 query-only 错误，并说明写入和 DDL 由 `add-nebula-sql-write-and-ddl-support` 承接。

### Requirement: SQL SELECT can be converted to nGQL
系统 SHALL 将受支持 SQL SELECT 查询转换为等价或受控等价的 nGQL。

#### Scenario: 点查询转换
- **WHEN** `SELECT * FROM person WHERE id = 'p1'` 的 SELECT IR 命中 person tag 和 id 字段映射
- **THEN** 转换器生成针对 person tag 的 nGQL 查询计划。

#### Scenario: 过滤排序分页转换
- **WHEN** 包含 WHERE、ORDER BY、LIMIT 或 OFFSET 的受支持 SELECT 可转换为 nGQL
- **THEN** 查询计划包含对应过滤、排序和分页语义。

### Requirement: Schema mapping defines graph semantics explicitly
系统 SHALL 使用显式 schema mapping 区分 SQL table/column 与 Nebula tag、edge、vertex id、edge src/dst/rank 和 property 字段的关系。

#### Scenario: Tag 表映射
- **WHEN** SQL table 被声明为 tag mapping
- **THEN** mapping 明确 tag 名称、vertex id 列和 property 列。

#### Scenario: Edge 表映射
- **WHEN** SQL table 被声明为 edge mapping
- **THEN** mapping 明确 edge 名称、src id、dst id、可选 rank 和 property 列。

#### Scenario: 缺失映射
- **WHEN** 查询引用未映射表、未映射列或歧义图关系
- **THEN** 转换器拒绝转换并返回包含缺失 mapping 的明确错误。

### Requirement: Edge and relationship queries are supported within defined bounds
系统 SHALL 支持边查询和受限关系映射，并明确不能映射的 JOIN 或图遍历边界。

#### Scenario: 边查询
- **WHEN** 查询 edge 映射表
- **THEN** 转换器生成对应 edge 查询 nGQL。

#### Scenario: 不支持的 JOIN
- **WHEN** 输入无法映射到 Nebula 图关系的 JOIN 
- **THEN** 返回包含不支持 JOIN 类型、原因和稳定错误码的明确错误。

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
- **THEN** 转换器拒绝执行并返回包含不支持结构、原因和稳定错误码的明确错误

### Requirement: Converted queries use existing Nebula execution path
系统 SHALL 复用现有 Nebula client façade 执行转换后的 nGQL，并返回 JDBC ResultSet。

#### Scenario: SQL 查询执行
- **WHEN** JDBC 调用方执行受支持 SQL SELECT 
- **THEN** 内部转换为 nGQL 后通过现有执行链路返回结果集。

#### Scenario: nGQL 直连路径保持不变
- **WHEN** 调用方使用现有 nGQL 直连能力
- **THEN** 系统不强制经过 SQL 查询转换器，且保持现有 nGQL 执行语义。

### Requirement: Query converter exposes reusable integration boundary
系统 SHALL 将 SQL 到 nGQL 查询语义集中在 query converter 中，供 JDBC framework integration 和 MySQL protocol gateway 复用，避免在入口层重复定义转换语义。

#### Scenario: JDBC framework integration 复用查询转换
- **WHEN** 后续 `add-nebula-jdbc-client-framework-integration` 接入标准 JDBC 查询路径
- **THEN** 其调用本能力提供的 query converter 或查询计划边界，而不是重新实现 SQL 到 nGQL 查询转换。

#### Scenario: MySQL read-only gateway 复用查询转换
- **WHEN** 后续 `add-nebula-mysql-protocol-gateway-readonly` 处理 COM_QUERY SELECT
- **THEN** 其调用本能力判断复杂只读 SQL 是否可映射，并只负责协议和结果编码。
