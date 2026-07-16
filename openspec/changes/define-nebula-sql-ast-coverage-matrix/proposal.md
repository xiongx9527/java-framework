## Why

用户要求 SQL parser core 不允许对 Druid MySQL parser 已能解析的 AST 类型遗漏适配。为了让该要求可验证，必须先锁定 Druid 版本、枚举 MySQL AST 覆盖范围，并定义 Nebula 自定义 AST 的覆盖矩阵和验收规则。

## What Changes

- 定义 Druid SQL Parser 版本锁定策略。
- 枚举该版本 MySQL parser 可产出的关键 AST 类型和 SQL 构造。
- 定义 Druid AST 到 Nebula AST / 中间表示的覆盖矩阵。
- 定义 adapter 完整性测试策略，确保 parser core 不允许 raw fallback 或遗漏适配。
- 覆盖 MyBatis 和 JdbcTemplate 常见 SQL 形态，包括 `?` 参数、列别名、IN 条件和动态 SQL 展开后的条件组合。
- 明确后续 Druid 升级时必须重新评估 AST 覆盖矩阵。

## Capabilities

### New Capabilities
- `nebula-sql-ast-coverage-matrix`: 定义 Druid MySQL AST 到 Nebula AST 的覆盖矩阵、版本锁定和验收边界。

### Modified Capabilities

## Impact

- 影响后续 `add-nebula-sql-parser-core` 的设计与验收边界。
- 不直接实现 SQL parser 或 adapter，只产出覆盖矩阵和测试策略。
- 为复杂 SQL、DDL、函数、表达式、JOIN、子查询、UNION 等全量适配提供可验证基线。
