---
comet_change: add-nebula-sql-to-ngql-query-support
role: technical-design
canonical_spec: openspec
---

# Nebula SQL SELECT 到 nGQL 查询转换技术设计

## 1. 背景与目标

`fw-plus-nebula-jdbc` 当前已有稳定的 nGQL 直连执行链路：`NebulaStatementImpl` 接收语句后通过 `NebulaConnection` 调用 `NebulaClientFacade.executeNgql`。本 change 的目标是在该链路之上新增 query-only SQL SELECT 到 nGQL 的转换能力，让上层入口可以把 SQL 查询转换为 NebulaGraph 可执行的 nGQL，同时保持现有 nGQL 直连路径不被破坏。

本 change 依赖 `add-nebula-sql-parser-core` 提供稳定 SQL AST/IR。转换层不直接解析 SQL 字符串，也不把 Druid AST 暴露为公共转换契约。parser core 尽量表达 SQL 结构，query converter 负责判断哪些 SELECT 语义可以安全映射到 NebulaGraph，不能映射时明确失败。

## 2. 范围与非目标

### 范围

- 新增独立 SQL SELECT query converter 核心。
- 消费 parser-core 的 SELECT AST/IR、可选参数绑定视图和 schema mapping。
- 输出 `NgqlQueryPlan` 或等价查询计划，包含 nGQL 文本、输出列描述、转换 trace 和语义保真级别。
- 支持点查询、边查询、投影、WHERE、ORDER BY、LIMIT/OFFSET。
- 支持 JOIN、子查询、聚合、GROUP BY、HAVING、UNION、函数表达式的可映射子集。
- 对无法安全映射到 NebulaGraph 的结构返回稳定、可诊断的错误。
- 提供将查询计划交给现有 Nebula 执行链路的最小接入边界。

### 非目标

- 不实现 INSERT、UPDATE、DELETE、DDL 或事务语义，这些由 `add-nebula-sql-write-and-ddl-support` 承接。
- 不实现 PreparedStatement 参数绑定、JdbcTemplate/MyBatis 验收、ResultSetMetaData 或 DatabaseMetaData，这些由 `add-nebula-jdbc-client-framework-integration` 承接。
- 不实现 MySQL wire protocol、认证、COM_QUERY 编解码或 MySQL 文本协议结果集，这些由 `add-nebula-mysql-protocol-gateway-readonly` 承接。
- 不重新实现 SQL parser，不直接依赖 Druid AST 作为转换层公共 API。
- 不模拟完整关系型数据库查询优化器，不承诺 100% SQL 标准或 MySQL 兼容。

## 3. 推荐方案

采用“独立 Query Converter 核心”方案。

转换器位于 parser core 与执行入口之间，职责是把结构化 SELECT IR 转为明确的 nGQL 查询计划。JDBC integration 和 MySQL gateway 都只消费该查询转换能力，不在各自 change 中重复定义 SQL 到 nGQL 的查询语义。

```text
SQL text
  -> parser-core.parse(sql)
  -> SQL SELECT AST/IR
  -> NebulaSqlQueryConverter.convert(ir, bindings, schemaMapping)
  -> NgqlQueryPlan
  -> existing Nebula execution path
```

该方案相比直接改造 `NebulaStatementImpl` 更适合并行 change：它让转换规则、schema mapping、错误模型和测试 fixture 都沉在可复用核心中，JDBC 和 MySQL gateway 只负责入口协议与结果集适配。

## 4. 核心组件

### 4.1 `NebulaSqlQueryConverter`

建议新增查询转换入口接口：

```java
public interface NebulaSqlQueryConverter {
	NgqlQueryPlan convert(SqlStatementIr statement,
						  NebulaQueryBindings bindings,
						  NebulaSchemaMappingRegistry schemaMapping) throws SQLException;
}
```

设计约束：

- `statement` 必须是 parser-core 产出的稳定 IR。
- 非 SELECT 语句直接拒绝，错误指向 query-only 边界。
- converter 不访问 NebulaGraph，不创建连接，不执行 nGQL。
- converter 不修改输入 IR，转换状态只存在于本次 `convert` 调用。

### 4.2 `NgqlQueryPlan`

查询计划是转换层输出给执行入口和协议入口的稳定对象。

建议字段：

- `ngql`：可执行 nGQL 文本。
- `queryKind`：`VERTEX_QUERY`、`EDGE_QUERY`、`RELATION_QUERY`、`AGGREGATE_QUERY`、`SET_QUERY` 等。
- `columns`：输出列描述，包含列名、列标签、表达式来源、建议 JDBC 类型和 Nebula 类型。
- `fidelity`：`EQUIVALENT` 或 `CONTROLLED_EQUIVALENT`，说明是否为严格等价或受控等价。
- `trace`：转换 trace，记录 SQL 结构、mapping 命中、生成 nGQL 片段和限制说明。

`NgqlQueryPlan` 不负责 JDBC `ResultSet` 行为，也不负责 MySQL 文本协议编码。它只提供足够稳定的转换结果和列描述，供后续 change 使用。

### 4.3 `NebulaSchemaMappingRegistry`

schema mapping 是本 change 的关键边界。转换器不能根据表名和列名猜测图模型，必须通过显式 mapping 判断 SQL 表、列与 Nebula tag/edge/id/property 的关系。

建议模型：

- `TableMapping`
  - `sqlTable`
  - `objectKind`：`TAG` 或 `EDGE`
  - `nebulaName`
  - `idColumn`：vertex id 映射，仅 tag 表使用
  - `srcColumn`、`dstColumn`、`rankColumn`：edge 映射
  - `properties`：SQL 列到 Nebula 属性的映射
  - `direction`：edge 查询默认方向
- `ColumnMapping`
  - `sqlColumn`
  - `nebulaProperty`
  - `role`：`VID`、`SRC_ID`、`DST_ID`、`RANK`、`PROPERTY`
  - `type`

后续 `add-nebula-sql-write-and-ddl-support` 可以复用该 mapping 模型，但写入安全、DDL schema 创建和事务差异不在本 change 内处理。

### 4.4 错误模型

转换失败必须是结构化、可诊断的失败，不允许回退为 raw SQL 或执行不确定 nGQL。

建议错误信息包含：

- 稳定错误码，例如 `NEBULA_SQL_QUERY_UNSUPPORTED_JOIN`。
- 不支持结构，例如 `JOIN`、`UNION`、`FUNCTION`、`SUBQUERY`。
- 原因说明，例如“JOIN 条件无法映射到 tag-edge-tag 关系”。
- SQL IR 位置或表达式路径。
- 对应 schema mapping 名称或缺失字段。

JDBC 层可把该错误转换为 `SQLFeatureNotSupportedException` 或带稳定错误码的 `SQLException`。MySQL gateway 可把该错误转换为 MySQL 客户端可理解的错误包。

## 5. 转换规则

### 5.1 点查询

点查询以 tag table mapping 为基础。典型 SQL：

```sql
SELECT * FROM person WHERE id = 'p1'
```

转换条件：

- `person` 映射为 tag。
- `id` 是该 tag 的 vertex id 列。
- WHERE 中存在可识别的 id 等值条件，或存在可由 nGQL 表达的属性过滤条件。

输出 nGQL 可以采用 `FETCH PROP ON`、`MATCH` 或项目最终约定的查询模板。Design Doc 不强制具体 nGQL 方言模板，但 build 阶段必须把模板固化为 golden test。

### 5.2 边查询

边查询以 edge table mapping 为基础。典型 SQL：

```sql
SELECT * FROM follow WHERE src_id = 'p1' AND dst_id = 'p2'
```

转换条件：

- SQL table 映射为 edge。
- mapping 明确 `src_id`、`dst_id` 和可选 `rank`。
- 过滤条件能定位起点、终点或受控边属性范围。

不能把任意普通表当作 edge 查询，也不能在缺少 src/dst mapping 时猜测边方向。

### 5.3 投影、过滤、排序、分页

支持子集：

- 投影：`*`、普通列、表别名列、列别名、可映射表达式。
- WHERE：比较运算、布尔组合、`IN`、`IS NULL`、可映射函数。
- ORDER BY：输出列、属性列、可映射表达式。
- LIMIT/OFFSET：映射为 nGQL 支持的分页语法。

限制：

- 引用未映射列时失败。
- ORDER BY 使用不可投影或不可计算表达式时失败。
- 函数必须在白名单内，并有明确 nGQL 表达形式。

### 5.4 JOIN

JOIN 只支持可以明确映射为图关系查询的结构。

可支持形态：

- tag-edge-tag 关系。
- JOIN 条件命中 edge 的 src/dst 与 tag id。
- 表别名和列别名在 parser IR 中可解析。

拒绝形态：

- 笛卡尔积。
- 任意关系型多表 JOIN。
- 非等值 JOIN。
- 多跳关系推断，除非 mapping 明确声明并有 nGQL 模板。
- JOIN 条件缺失、歧义或跨多个 edge mapping 无法判定。

### 5.5 子查询、聚合、GROUP BY、HAVING、UNION、函数

复杂 SQL 采用白名单。

子查询只在能内联为 nGQL 子表达式或受控查询片段时支持。相关子查询、依赖外层复杂表达式、需要客户端中间结果物化的形态默认拒绝。

聚合、GROUP BY、HAVING 只支持 nGQL 可表达的聚合函数和分组字段。HAVING 必须能映射到聚合结果过滤。

UNION 只支持左右查询输出列数量、类型和语义一致，且 nGQL 能表达集合操作的形态；否则拒绝。

函数表达式必须建立 SQL 函数到 nGQL 函数的白名单映射。未知函数、语义差异大的函数和需要 MySQL 专有运行时语义的函数均拒绝。

Build 阶段收敛实现的复杂查询子集：

- JOIN：仅支持由上游适配层显式提供的单跳 tag-edge-tag IR，转换器校验左右表为 tag、中间表为 edge 后生成 `MATCH (left:tag)-[edge:edge]->(right:tag)`。当前不从任意 SQL JOIN 条件中推断图语义。
- 聚合 / GROUP BY / HAVING：支持 `COUNT(mapped_column)`、普通映射列分组，以及引用聚合投影别名的 HAVING 条件，使用 `WITH ... WHERE ... RETURN ...` 管道表达。
- UNION：支持左右分支均为受支持 SELECT，且输出列数量、标签和 JDBC 类型一致的同形 UNION。
- 函数表达式：支持 `lower`、`upper`、`length` 安全白名单。
- 子查询：当前无稳定 graph subquery template 边界，继续返回 `UNSUPPORTED_SUBQUERY`。建议拆分为后续独立 change，先设计 nGQL 子查询/管道模板、相关子查询拒绝策略和是否允许客户端物化中间结果。

## 6. 执行接入边界

本 change 可以定义最小查询执行适配：

```text
NgqlQueryPlan
  -> NebulaConnection.execute(plan.getNgql())
  -> NebulaResultSet
```

该适配只证明转换后的 nGQL 能复用现有 Nebula 执行链路。它不负责：

- PreparedStatement `?` 参数绑定完整行为。
- ResultSetMetaData 完整映射。
- JdbcTemplate / MyBatis 集成验收。
- MySQL 协议结果集编码。

现有 nGQL 直连路径必须保留。SQL 查询入口应通过明确开关、明确 statement 类型或 SQL execution service 调用 converter，避免把所有输入都强行当作 SQL 解析。

## 7. 跨 Change 依赖

### `add-nebula-sql-parser-core`

上游依赖。必须提供稳定 AST/IR、语句类型、表/列引用、别名、表达式树、参数占位符和复杂 SQL 结构。query converter 只消费该 IR。

### `add-nebula-sql-write-and-ddl-support`

下游或并行依赖。可复用 schema mapping，但不得在本 change 内实现写入、DDL、事务或回滚语义。

### `add-nebula-jdbc-client-framework-integration`

下游集成层。负责 PreparedStatement 参数绑定、ResultSet/ResultSetMetaData、DatabaseMetaData、JdbcTemplate、MyBatis 和标准 JDBC 异常语义。本 change 只提供 query converter 和计划列描述。

### `add-nebula-mysql-protocol-gateway-readonly`

下游协议入口。负责 MySQL wire protocol、认证、COM_QUERY 和结果集编码。复杂只读 SQL 是否可执行由本 change 判断。

## 8. 测试策略

### 8.1 Converter 单元测试

使用 parser-core IR fixture 和 schema mapping fixture，不依赖真实 NebulaGraph。

覆盖：

- 点查询。
- 边查询。
- 投影、过滤、排序、分页。
- tag-edge-tag JOIN。
- 可映射聚合、GROUP BY、HAVING。
- 可映射函数表达式。
- 可映射 UNION。

### 8.2 Golden Test

每个 golden case 包含：

- SQL 输入或 parser IR fixture。
- schema mapping fixture。
- 期望 nGQL。
- 期望输出列描述。
- 期望 `fidelity`。
- 期望 trace 关键节点。

### 8.3 Negative Test

覆盖：

- INSERT、UPDATE、DELETE、DDL、事务语句被 query-only 边界拒绝。
- 缺失 schema mapping。
- 未映射列。
- 不支持 JOIN。
- 不支持子查询。
- 不支持 UNION 列形态。
- 不支持函数。
- ORDER BY / HAVING 引用不可映射表达式。

### 8.4 执行路径测试

使用 fake `NebulaConnection` 或 fake client façade 验证：

- converter 输出的 nGQL 被传给现有执行链路。
- Nebula 执行错误被保留为 `SQLException`。
- nGQL 直连路径不经过 SQL converter。

## 9. 风险与缓解

- 复杂 SQL 与图查询语义差异大。缓解方式是白名单和明确错误，不做隐式关系数据库模拟。
- schema mapping 一旦模糊会导致错误查询。缓解方式是缺 mapping 即失败，不猜测 tag、edge、id 或方向。
- 并行 change 容易重复设计入口行为。缓解方式是本 change 只定义 query converter 核心和最小执行适配，JDBC/MySQL 入口只消费该能力。
- nGQL 模板选择可能受 Nebula 版本影响。缓解方式是在 build 阶段用 golden test 固化模板，并在转换 trace 中记录模板策略。

## 10. Build 阶段执行边界

build 阶段应优先实现 converter 核心、schema mapping、错误模型和转换测试。若需要接入 `NebulaStatementImpl`，只做最小 SQL 查询入口或内部 execution service 调用，不扩展 PreparedStatement、metadata、JdbcTemplate、MyBatis 或 MySQL protocol。

2.4 范围记录：完整 JOIN、子查询、聚合、GROUP BY、HAVING、UNION 和函数表达式覆盖超出单个 build task。当前 change 只完成可验证的最小可映射子集，并保留子查询的明确错误路径；不要在 verify/archive 时把子查询转换描述为已支持。
