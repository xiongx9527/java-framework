# Comet Design Handoff

- Change: define-nebula-sql-ast-coverage-matrix
- Phase: design
- Mode: compact
- Context hash: 2160356a4041843f65b0e6d081d3b89a4d144ff86204985a765cd6730a4ac2e0

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/define-nebula-sql-ast-coverage-matrix/proposal.md

- Source: openspec/changes/define-nebula-sql-ast-coverage-matrix/proposal.md
- Lines: 1-25
- SHA256: 424a86adb974e8838899cbf0c2ee9cbc59c6b1c2b147a369e89f7f22119694c2

```md
## Why

用户要求 SQL parser core 不允许对 Druid MySQL parser 已能解析的 AST 类型遗漏适配。为了让该要求可验证，必须先锁定 Druid 版本、枚举 MySQL AST 覆盖范围，并定义 Nebula 自定义 AST 的覆盖矩阵和验收规则。

## What Changes

- 定义 Druid SQL Parser 版本锁定策略。
- 枚举该版本 MySQL parser 可产出的关键 AST 类型和 SQL 构造。
- 定义 Druid AST 到 Nebula AST / 中间表示的覆盖矩阵。
- 定义 adapter 完整性测试策略，确保 parser core 不允许 raw fallback 或遗漏适配。
- 覆盖 正常 JDBC、MyBatis 和 JdbcTemplate 常见 SQL 形态，包括 `?` 参数、SELECT 1、SELECT COUNT(*)、列别名、IN 条件、分页、SHOW/DESCRIBE 和动态 SQL 展开后的条件组合。
- 明确后续 Druid 升级时必须重新评估 AST 覆盖矩阵。

## Capabilities

### New Capabilities
- `nebula-sql-ast-coverage-matrix`: 定义 Druid MySQL AST 到 Nebula AST 的覆盖矩阵、版本锁定和验收边界。

### Modified Capabilities

## Impact

- 影响后续 `add-nebula-sql-parser-core` 的设计与验收边界。
- 不直接实现 SQL parser 或 adapter，只产出覆盖矩阵和测试策略。
- 为复杂 SQL、DDL、函数、表达式、JOIN、子查询、UNION 等全量适配提供可验证基线。

```

## openspec/changes/define-nebula-sql-ast-coverage-matrix/design.md

- Source: openspec/changes/define-nebula-sql-ast-coverage-matrix/design.md
- Lines: 1-49
- SHA256: fe526e8f1adfbc149ccded2ea53ecca82a0ab13828376f60ee8f6d5ec5c65704

```md
## Context

`add-nebula-sql-parser-core` 需要做到 Druid MySQL parser 已能解析的 AST 类型不遗漏适配。该要求必须先变成可枚举、可测试的覆盖矩阵，否则 parser core 的完成边界不可验证。

## Goals / Non-Goals

**Goals:**

- 锁定 Druid SQL Parser 版本和 Java 8 兼容边界。
- 枚举 Druid MySQL parser 关键 AST 类型。
- 定义 Druid AST 到 Nebula AST / 中间表示的覆盖矩阵。
- 覆盖 正常 JDBC、MyBatis 和 JdbcTemplate 常见 SQL 形态，包括 `?` 参数、SELECT 1、SELECT COUNT(*)、列别名、IN 条件、分页、SHOW/DESCRIBE 和动态 SQL 展开后的条件组合。
- 定义 adapter 完整性测试策略和 raw fallback 禁止规则。
- 为 `add-nebula-sql-parser-core` 提供可执行输入。

**Non-Goals:**

- 不实现 parser adapter。
- 不实现 SQL→nGQL 转换。
- 不实现 MySQL 协议、binlog、复制、routine 或 trigger。
- 不承诺覆盖未来 Druid 版本新增 AST，升级必须重新评估。

## Decisions

- 覆盖矩阵先作为独立 OpenSpec change 交付，降低 parser core 的不确定性。
- 覆盖矩阵以固定 Druid 版本为基线，而不是浮动依赖。
- 矩阵字段至少包含：Druid AST 类型、SQL 构造、示例 SQL、Nebula AST 类型、适配要求、测试用例名称、备注。
- raw SQL 只允许作为 debug 字段，不允许作为语义 fallback。
- 后续 parser core 的测试必须能发现矩阵内缺失 adapter。

## Risks / Trade-offs

- **[Risk] Druid AST 类型数量较多。** → Mitigation：先覆盖 MySQL parser 实际产出的关键 AST 类型，并把版本和枚举方法写入矩阵。
- **[Risk] 矩阵维护成本高。** → Mitigation：矩阵成为 Druid 升级检查清单，避免隐式破坏。
- **[Risk] 覆盖矩阵与实现脱节。** → Mitigation：parser core 必须把矩阵作为测试输入或生成测试依据。

## Migration Plan

1. 确认 Druid 版本和 Java 8 兼容性。
2. 调研并枚举 MySQL parser 常见 AST 类型。
3. 设计 Nebula AST 类型分类。
4. 形成覆盖矩阵与 adapter 测试策略。
5. 将矩阵路径写入 `add-nebula-sql-parser-core` 设计文档作为前置输入。

## Open Questions

- 覆盖矩阵以 Markdown、YAML、JSON 还是测试资源形式维护？
- 是否用自动化反射/样例 SQL 收集 Druid AST 类型？
- Druid 版本最终选型是否固定在某个已验证 Java 8 兼容版本？

```

## openspec/changes/define-nebula-sql-ast-coverage-matrix/tasks.md

- Source: openspec/changes/define-nebula-sql-ast-coverage-matrix/tasks.md
- Lines: 1-19
- SHA256: b255f5f1466fc063d9a046368358e1ccc8f96ec18658cbdff84726d86bf75305

```md
## 1. Druid 版本与枚举策略

- [ ] 1.1 确认 Druid SQL Parser 版本、许可证和 Java 8 兼容性。
- [ ] 1.2 定义 Druid 版本升级时的 AST 覆盖矩阵更新规则。
- [ ] 1.3 制定 MySQL parser AST 类型枚举方法。

## 2. 覆盖矩阵设计

- [ ] 2.1 定义 Nebula AST 顶层分类和表达式分类。
- [ ] 2.2 建立 Druid AST 到 Nebula AST 的覆盖矩阵字段结构。
- [ ] 2.3 覆盖 SELECT、JOIN、子查询、聚合、GROUP BY、HAVING、UNION、函数、表达式、别名和参数占位符。
- [ ] 2.4 覆盖 INSERT、UPDATE、DELETE 和基础 DDL。
- [ ] 2.5 覆盖正常 JDBC/MyBatis/JdbcTemplate 常见 SQL 形态：`?` 参数、SELECT 1、SELECT COUNT(*)、列别名、IN 条件、分页、SHOW/DESCRIBE、metadata 探测 SQL 和动态 SQL 展开后的条件组合。

## 3. 测试策略与交付

- [ ] 3.1 定义 adapter 完整性测试策略，要求矩阵内节点缺失适配时测试失败。
- [ ] 3.2 定义 raw fallback 禁止规则和测试断言。
- [ ] 3.3 输出覆盖矩阵文档或测试资源，供 `add-nebula-sql-parser-core` 使用。

```

## openspec/changes/define-nebula-sql-ast-coverage-matrix/specs/nebula-sql-ast-coverage-matrix/spec.md

- Source: openspec/changes/define-nebula-sql-ast-coverage-matrix/specs/nebula-sql-ast-coverage-matrix/spec.md
- Lines: 1-52
- SHA256: fcd5c7464035f9dbe93d633be99c980be648724230edc66fad7bab003a55d1f3

```md
## ADDED Requirements

### Requirement: Druid parser version is locked for AST coverage
系统 SHALL 为 SQL AST 覆盖矩阵锁定一个明确的 Druid SQL Parser 版本，并以该版本的 MySQL parser AST 类型作为验收基线。

#### Scenario: 版本锁定
- **WHEN** 维护者查看 AST 覆盖矩阵
- **THEN** 能看到明确的 Druid 版本、Java 8 兼容性说明和升级评估规则

#### Scenario: Druid 版本升级
- **WHEN** 后续 change 尝试升级 Druid 版本
- **THEN** 必须重新评估 AST 类型变化并更新覆盖矩阵

### Requirement: AST coverage matrix maps Druid nodes to Nebula nodes
系统 SHALL 定义 Druid MySQL AST 类型到 Nebula AST / 中间表示类型的覆盖矩阵，覆盖 SELECT、写入、DDL、JOIN、子查询、聚合、GROUP BY、HAVING、UNION、函数、表达式、别名和参数占位符等构造。

#### Scenario: 覆盖矩阵包含复杂查询构造
- **WHEN** 矩阵列出 Druid AST 类型
- **THEN** 每个复杂 SQL 构造都有对应 Nebula AST 类型、测试 SQL 和适配状态

#### Scenario: 覆盖矩阵包含写入和 DDL 构造
- **WHEN** 矩阵列出 INSERT、UPDATE、DELETE 和 DDL 相关 AST 类型
- **THEN** 每个构造都有对应 Nebula AST 类型、测试 SQL 和适配状态

### Requirement: Adapter completeness is testable
系统 SHALL 定义后续 parser adapter 的完整性测试策略，确保覆盖矩阵内 AST 类型不得缺失适配，不得通过 raw fallback 作为语义输出。

#### Scenario: 矩阵内节点缺失 adapter
- **WHEN** 后续 parser core 遇到覆盖矩阵内 Druid AST 类型但未实现 adapter
- **THEN** 测试必须失败并指出缺失的 AST 类型

#### Scenario: raw fallback 被用作语义输出
- **WHEN** adapter 试图用 raw SQL 代替结构化 Nebula AST
- **THEN** 测试必须失败

### Requirement: Matrix artifact is reusable by parser implementation
系统 SHALL 将覆盖矩阵作为后续 parser core 的可读、可维护输入产物，使实现任务能直接引用。

#### Scenario: parser core 读取矩阵
- **WHEN** 实现 `add-nebula-sql-parser-core`
- **THEN** 开发者可以根据覆盖矩阵逐项实现 adapter 和测试

### Requirement: Framework-generated SQL shapes are represented
系统 SHALL 在 AST 覆盖矩阵中纳入 正常 JDBC、MyBatis 和 JdbcTemplate 常见 SQL 形态，确保 JDBC 上层框架生成的 SQL 能被 parser core 验证。

#### Scenario: JDBC 参数占位符
- **WHEN** SQL 包含 `?` 参数占位符、SELECT 1、SELECT COUNT(*)、IN 参数列表、分页、SHOW/DESCRIBE 或框架 metadata 探测 SQL
- **THEN** 覆盖矩阵包含对应 Druid AST 类型、Nebula 参数表达式和测试 SQL

#### Scenario: 列别名和动态条件
- **WHEN** SQL 包含列别名、动态 SQL 展开后的可选 WHERE 条件、IN 条件或标准 JDBC metadata 探测 SQL
- **THEN** 覆盖矩阵包含对应别名、条件表达式和测试要求

```
