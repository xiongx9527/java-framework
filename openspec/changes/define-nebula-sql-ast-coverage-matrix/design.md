## Context

`add-nebula-sql-parser-core` 需要做到 Druid MySQL parser 已能解析的 AST 类型不遗漏适配。该要求必须先变成可枚举、可测试的覆盖矩阵，否则 parser core 的完成边界不可验证。

## Goals / Non-Goals

**Goals:**

- 锁定 Druid SQL Parser 版本和 Java 8 兼容边界。
- 枚举 Druid MySQL parser 关键 AST 类型。
- 定义 Druid AST 到 Nebula AST / 中间表示的覆盖矩阵。
- 覆盖 MyBatis 和 JdbcTemplate 常见 SQL 形态，包括 `?` 参数、列别名、IN 条件和动态 SQL 展开后的条件组合。
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
