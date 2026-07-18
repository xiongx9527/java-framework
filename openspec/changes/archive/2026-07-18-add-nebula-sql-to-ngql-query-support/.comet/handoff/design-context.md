# Comet Design Handoff

- Change: add-nebula-sql-to-ngql-query-support
- Phase: design
- Mode: compact
- Context hash: 4134485c7e0959a26e48377e831a4f6b2d99469dfe967db8c06069f8a53a7f13

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/add-nebula-sql-to-ngql-query-support/proposal.md

- Source: openspec/changes/add-nebula-sql-to-ngql-query-support/proposal.md
- Lines: 1-25
- SHA256: 61ae2d9b1abfd361ec167b598f3bae113b2e87d9f9935278314e5f2c1645f935

```md
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

```

## openspec/changes/add-nebula-sql-to-ngql-query-support/design.md

- Source: openspec/changes/add-nebula-sql-to-ngql-query-support/design.md
- Lines: 1-47
- SHA256: 5e1945ddf5de645da31cd13ea640680c93e382515352795d504e40e5aef73c06

```md
## Context

本 change 属于 Nebula SQL / MySQL 协议支持路线的一部分。当前 `fw-plus-nebula-jdbc` 已具备稳定的 nGQL JDBC 基础能力，但完整 SQL 与 MySQL 协议能力需要分阶段建设。

前置依赖：依赖 `add-nebula-sql-parser-core` 提供稳定 SQL 中间表示。

## Goals / Non-Goals

**Goals:**

- 新增 SQL SELECT 到 nGQL 的转换层。
- 支持点查询、边查询、投影、过滤、排序、分页，以及 JOIN、子查询、聚合、GROUP BY、HAVING、UNION、函数表达式等复杂 SQL 的可映射子集。
- 通过现有 `NebulaClientFacade` 执行转换后的 nGQL。
- 对无法安全映射的 SQL 返回明确错误。

**Non-Goals:**


- MySQL binlog、复制协议、存储过程和触发器不在本 change 内实现；这些能力由后续独立 changes 承接。
- 不扩大到本拆分项之外的后续能力。
- 不破坏现有 `jdbc:nebula://...` nGQL 直连路径。
- 不追求 100% MySQL Server 或 SQL 标准兼容。

## Decisions

- 转换层只消费 parser 中间表示，不直接解析 SQL 字符串。
- schema 映射规则独立建模，用于区分 tag、edge、属性和 id 字段。
- 复杂 SQL 支持采用显式白名单：JOIN、子查询、聚合、UNION 和函数表达式必须有明确图语义映射；不能映射时 fail fast。

## Risks / Trade-offs

- **[Risk] 复杂 SQL 与图模型语义存在天然差异。** → Mitigation：采用明确白名单和可测试映射，不支持时返回清晰错误。
- **[Risk] 客户端兼容范围可能快速扩大。** → Mitigation：按 change 分阶段交付，每阶段定义验收客户端和协议子集。
- **[Risk] 引入第三方 parser 或协议库可能带来许可证和 Java 8 兼容风险。** → Mitigation：在 design/build 阶段先做依赖评估，再落地实现。

## Migration Plan

1. 在本 change 内完成设计与测试边界确认。
2. 以不破坏现有 nGQL 直连能力为前提新增能力。
3. 每个后续 change 只依赖前置产物的稳定 API，不回头扩大前置范围。
4. 如发现范围过大，优先继续拆分而不是在单个 change 中堆叠。

## Open Questions

- 是否采用第三方 parser / protocol 库，或自研轻量实现？
- SQL schema 与 Nebula tag/edge/property/id 的最终映射配置放在哪一层？
- 是否需要 Spring Boot starter、Magic-API 插件或独立服务入口？

```

## openspec/changes/add-nebula-sql-to-ngql-query-support/tasks.md

- Source: openspec/changes/add-nebula-sql-to-ngql-query-support/tasks.md
- Lines: 1-22
- SHA256: cd2bcc18a6695b3c8fe1946bab21eced78cef1e5b4fa128dc5c6f9a563065343

```md
## 1. 语义映射设计

- [ ] 1.1 定义 query converter 输入/输出边界：消费 parser-core SELECT AST/IR，输出 `NgqlQueryPlan` 或等价查询计划。
- [ ] 1.2 定义 SQL 表/列到 Nebula tag/edge/property/id/src/dst/rank 的显式 schema mapping 模型。
- [ ] 1.3 定义受支持 SELECT、WHERE、ORDER BY、LIMIT/OFFSET、JOIN、子查询、聚合、GROUP BY、HAVING、UNION、函数和表达式白名单。
- [ ] 1.4 定义 query-only 错误模型，明确拒绝写入、DDL、事务和不可映射复杂 SQL。

## 2. 转换与执行实现

- [ ] 2.1 实现点查询 SQL→nGQL 转换。
- [ ] 2.2 实现边查询 SQL→nGQL 转换。
- [ ] 2.3 实现过滤、排序、分页转换。
- [ ] 2.4 实现可映射 JOIN、子查询、聚合、GROUP BY、HAVING、UNION 和函数表达式转换。
- [ ] 2.5 提供最小查询执行适配，将转换后的 nGQL 交给现有 Nebula 执行链路，并保持 nGQL 直连路径不变。
- [ ] 2.6 暴露可供 JDBC framework integration 和 MySQL read-only gateway 复用的查询计划边界。

## 3. 验证

- [ ] 3.1 补充转换器单元测试和 golden tests，覆盖 IR、schema mapping、期望 nGQL、输出列描述和 trace。
- [ ] 3.2 补充无法映射 SQL 的错误测试，覆盖缺失 mapping、不支持 JOIN、复杂子查询、UNION、函数和表达式。
- [ ] 3.3 补充 query-only 边界测试，确认 INSERT、UPDATE、DELETE、DDL 和事务语句被明确拒绝。
- [ ] 3.4 补充执行路径测试，确认转换后的 nGQL 复用现有 Nebula 执行链路且 nGQL 直连路径不经过 converter。

```

## openspec/changes/add-nebula-sql-to-ngql-query-support/specs/nebula-sql-to-ngql-query-support/spec.md

- Source: openspec/changes/add-nebula-sql-to-ngql-query-support/specs/nebula-sql-to-ngql-query-support/spec.md
- Lines: 1-93
- SHA256: f83dd795058a6ad8669d436bd88983aa0101a3e4d7c9a9e9b52c50cafb257a6f

[TRUNCATED]

```md
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

```

Full source: openspec/changes/add-nebula-sql-to-ngql-query-support/specs/nebula-sql-to-ngql-query-support/spec.md
