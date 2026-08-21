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

#### Scenario: 矩阵 artifact 区分机器事实源和说明文档
- **WHEN** 维护者查看本 change 交付的矩阵 artifact
- **THEN** `nebula-sql-ast-coverage-matrix.json` 是后续 parser core 完整性测试消费的机器可读事实源，`nebula-sql-ast-coverage-matrix.md` 仅作为人类可读说明和维护文档

### Requirement: Framework-generated SQL shapes are represented
系统 SHALL 在 AST 覆盖矩阵中纳入 正常 JDBC、MyBatis 和 JdbcTemplate 常见 SQL 形态，确保 JDBC 上层框架生成的 SQL 能被 parser core 验证。

#### Scenario: JDBC 参数占位符
- **WHEN** SQL 包含 `?` 参数占位符、SELECT 1、SELECT COUNT(*)、IN 参数列表、分页、SHOW/DESCRIBE 或框架 metadata 探测 SQL
- **THEN** 覆盖矩阵包含对应 Druid AST 类型、Nebula 参数表达式和测试 SQL

#### Scenario: 列别名和动态条件
- **WHEN** SQL 包含列别名、动态 SQL 展开后的可选 WHERE 条件、IN 条件或标准 JDBC metadata 探测 SQL
- **THEN** 覆盖矩阵包含对应别名、条件表达式和测试要求
