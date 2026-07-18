# Comet Design Handoff

- Change: add-nebula-sql-write-and-ddl-support
- Phase: design
- Mode: compact
- Context hash: 6273e0a1f1ad31f41c7e60a2cfbbdb8ca020cb83d1d486db2ae2a8898bcc03c3

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/add-nebula-sql-write-and-ddl-support/proposal.md

- Source: openspec/changes/add-nebula-sql-write-and-ddl-support/proposal.md
- Lines: 1-25
- SHA256: 5be30953026424a595638f1deda3130fec6040319aaee22e02a29cc6e9a4b5ac

```md
## Why

只支持 SQL 查询无法满足完整数据管理需求。用户要求 SQL 写入与 DDL 都可用，因此需要在解析核心基础上支持 INSERT、UPDATE、DELETE 和基础 DDL 到 nGQL 的转换。

## What Changes

- 新增 INSERT、UPDATE、DELETE 到 nGQL 的转换与执行。
- 新增基础 DDL 到 Nebula tag/edge schema 操作的映射。
- 定义 SQL 表结构与 Nebula tag/edge schema 的映射约束。
- 对事务、复杂 DDL 或不兼容语义返回明确错误。

## Capabilities

### New Capabilities
- `nebula-sql-write-and-ddl-support`: SQL 写入与 DDL 支持，覆盖本 change 的需求、边界和验收场景。

### Modified Capabilities

## Impact


- MySQL binlog、复制协议、存储过程和触发器不在本 change 内实现；这些能力由后续独立 changes 承接。
- 依赖 parser core，建议依赖查询转换中的 schema 映射模型。
- 影响 JDBC executeUpdate 路径和可能的 metadata 行为。
- 需要更严格的安全边界和错误回滚说明。

```

## openspec/changes/add-nebula-sql-write-and-ddl-support/design.md

- Source: openspec/changes/add-nebula-sql-write-and-ddl-support/design.md
- Lines: 1-47
- SHA256: 771dabd9f70856ba96fbc9046a34de8b61cf99616f7cfabde68e5839330ee2a0

```md
## Context

本 change 属于 Nebula SQL / MySQL 协议支持路线的一部分。当前 `fw-plus-nebula-jdbc` 已具备稳定的 nGQL JDBC 基础能力，但完整 SQL 与 MySQL 协议能力需要分阶段建设。

前置依赖：依赖 `add-nebula-sql-parser-core`，建议复用查询转换阶段形成的 schema 映射模型。

## Goals / Non-Goals

**Goals:**

- 新增 INSERT、UPDATE、DELETE 到 nGQL 的转换与执行。
- 新增基础 DDL 到 Nebula tag/edge schema 操作的映射。
- 定义 SQL 表结构与 Nebula tag/edge schema 的映射约束。
- 对事务、复杂 DDL 或不兼容语义返回明确错误。

**Non-Goals:**


- MySQL binlog、复制协议、存储过程和触发器不在本 change 内实现；这些能力由后续独立 changes 承接。
- 不扩大到本拆分项之外的后续能力。
- 不破坏现有 `jdbc:nebula://...` nGQL 直连路径。
- 不追求 100% MySQL Server 或 SQL 标准兼容。

## Decisions

- 写入和 DDL 转换复用 parser 中间表示与 schema 映射。
- 以安全可验证为优先，缺少 id 或映射不明确的写入语句不自动猜测。
- 事务能力作为非目标，不模拟 MySQL 事务。

## Risks / Trade-offs

- **[Risk] SQL 与图模型语义存在天然差异。** → Mitigation：采用明确白名单和可测试映射，不支持时返回清晰错误。
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

## openspec/changes/add-nebula-sql-write-and-ddl-support/tasks.md

- Source: openspec/changes/add-nebula-sql-write-and-ddl-support/tasks.md
- Lines: 1-15
- SHA256: 3f8bcf8941639775eaa35d1c1730d18ffc405fac992e4fb7191f09e6b6389023

```md
## 1. 写入语义设计

- [ ] 1.1 定义 INSERT/UPDATE/DELETE 支持边界和安全条件。
- [ ] 1.2 定义 DDL 到 tag/edge 的映射规则。

## 2. 写入与 DDL 实现

- [ ] 2.1 实现 INSERT 转换与执行。
- [ ] 2.2 实现 UPDATE/DELETE 转换与执行。
- [ ] 2.3 实现基础 CREATE/DROP/DESCRIBE 类 DDL 映射。

## 3. 验证

- [ ] 3.1 补充写入和 DDL 转换测试。
- [ ] 3.2 补充事务和复杂 DDL 不支持错误测试。

```

## openspec/changes/add-nebula-sql-write-and-ddl-support/specs/nebula-sql-write-and-ddl-support/spec.md

- Source: openspec/changes/add-nebula-sql-write-and-ddl-support/specs/nebula-sql-write-and-ddl-support/spec.md
- Lines: 1-31
- SHA256: 8ff536fde95fa2d31e837bc6bb2906ea45cdffd3de3ff9ca490d2f129f18aba6

```md
## ADDED Requirements

### Requirement: SQL write statements can be converted to nGQL
系统 SHALL 将受支持 INSERT、UPDATE、DELETE 转换为对应 nGQL 写入语句。

#### Scenario: INSERT 转换
- **WHEN** `INSERT INTO person(id, name) VALUES ('p1', 'Tom')` 可转换为 Nebula 插入语句。
- **THEN** 系统产生符合要求的结果

#### Scenario: UPDATE 和 DELETE 转换
- **WHEN** 带 id 或受控条件的 UPDATE/DELETE 可转换为对应 nGQL，不能安全转换时返回错误。
- **THEN** 系统产生符合要求的结果

### Requirement: SQL DDL maps to Nebula schema operations
系统 SHALL 将受支持基础 DDL 映射到 Nebula tag/edge schema 操作。

#### Scenario: CREATE TABLE 映射
- **WHEN** 受支持 CREATE TABLE 可按映射规则生成 CREATE TAG 或 CREATE EDGE。
- **THEN** 系统产生符合要求的结果

#### Scenario: 不支持 DDL
- **WHEN** 复杂 ALTER、索引或 MySQL 专有选项不可映射时返回明确错误。
- **THEN** 系统产生符合要求的结果

### Requirement: Unsupported transactional semantics are explicit
系统 SHALL 明确 SQL 写入和 DDL 与 Nebula 事务语义的差异，不假装支持不可保证的事务行为。

#### Scenario: 事务语句
- **WHEN** 输入 BEGIN、COMMIT、ROLLBACK 等事务语句
- **THEN** 返回明确不支持或受限支持信息。


```
