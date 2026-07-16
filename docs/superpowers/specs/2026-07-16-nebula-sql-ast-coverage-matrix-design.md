---
comet_change: define-nebula-sql-ast-coverage-matrix
role: technical-design
canonical_spec: openspec
---

# Nebula SQL AST 覆盖矩阵设计

## 背景

`add-nebula-sql-parser-core` 需要按 Druid MySQL parser 已能解析的 AST 类型实现完整 adapter。当前 `fw-plus-nebula-jdbc` 已有 nGQL JDBC 直连能力，但尚未形成 SQL AST adapter 体系，因此必须先把 parser-core 的验收边界固化成可读、可审阅、可测试的覆盖矩阵。

本 change 只定义矩阵、版本基线、枚举方法和测试策略，不实现 parser adapter，不实现 SQL 到 nGQL 转换，也不扩展 MySQL 协议、binlog、routine 或 trigger。

## 设计目标

- 锁定 Druid SQL Parser 基线版本，并记录 Java 8 兼容性、许可证和升级规则。
- 枚举 Druid MySQL parser 在常见 SQL 形态下实际产出的关键 AST 类型。
- 定义 Druid AST 到 Nebula AST / 中间表示的覆盖矩阵字段和支持级别。
- 让后续 parser-core 能消费机器可读矩阵作为 adapter 完整性测试事实源。
- 明确 raw SQL 只能作为 debug / trace 信息，不能作为语义 fallback。

## Druid 基线

矩阵以仓库现有 core BOM 管理的 `com.alibaba:druid:1.2.24` 为基线。版本来源是 `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml` 中的 `<druid.version>1.2.24</druid.version>`，同一 BOM 同时管理 `com.alibaba:druid` 和 `com.alibaba:druid-spring-boot-starter`。

许可证和 Java 8 兼容性不在设计阶段凭记忆断言。build 阶段需要通过依赖 POM、Maven 解析或实际构建验证补齐证据，并写入矩阵版本元信息。

## 交付产物

本 change 交付两个 artifact：

- `openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json`
- `openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.md`

JSON 是唯一机器可读事实源，后续 `add-nebula-sql-parser-core` 的 adapter 完整性测试必须直接读取它或从它生成测试输入。Markdown 只用于审阅、解释和维护说明，不能作为测试事实源。

## JSON 结构

JSON 顶层建议包含以下概念字段：

```json
{
  "schemaVersion": "nebula-sql-ast-coverage-matrix.v1",
  "druid": {
    "groupId": "com.alibaba",
    "artifactId": "druid",
    "version": "1.2.24",
    "java8Compatible": null,
    "license": null,
    "verificationNotes": []
  },
  "artifacts": {
    "json": "openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json",
    "markdown": "openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.md"
  },
  "enumeration": {
    "sampleSqlDriven": true,
    "reflectionGapCheck": true,
    "packages": [
      "com.alibaba.druid.sql.ast",
      "com.alibaba.druid.sql.dialect.mysql.ast"
    ]
  },
  "entries": []
}
```

每个 `entries` 条目至少包含：

- `id`：稳定矩阵 ID，用于测试失败定位。
- `category`：语句、查询结构、表达式、DDL、框架探测等顶层分类。
- `druidAstType`：Druid AST 类型全限定名或明确类名。
- `sqlConstruct`：对应 SQL 构造。
- `exampleSql`：能触发该 AST 的示例 SQL。
- `nebulaAstKind`：Nebula AST / 中间表示分类。
- `adapterRequirement`：后续 adapter 需要实现的行为或错误边界。
- `supportLevel`：`required`、`explicitlyUnsupported` 或 `deferred`。
- `testCaseId`：后续 parser-core 测试或生成测试的稳定引用。
- `rawFallbackAllowed`：固定为 `false`。
- `notes`：排除理由、限制条件或维护说明。

## AST 枚举流程

枚举采用“样例 SQL 驱动 + 反射扫描兜底”。

样例 SQL 驱动负责确认 Druid MySQL parser 在真实 SQL 输入下实际产出的 AST 类型，覆盖 SELECT、JOIN、子查询、聚合、GROUP BY、HAVING、UNION、函数、表达式、参数占位符、INSERT、UPDATE、DELETE、基础 DDL、SHOW、DESCRIBE、metadata 探测 SQL，以及 JDBC、MyBatis、JdbcTemplate 常见 SQL 形态。

反射扫描负责检查 Druid AST 包和 MySQL dialect 包中是否存在未被样例触达的候选类型。反射命中不自动等于当前必须适配，但每个命中类型必须进入矩阵，或在矩阵 / Markdown 中写明排除原因，例如非 MySQL parser 产物、内部辅助节点、未来 change 承接、或当前路线明确不支持。

## 分类与支持级别

矩阵分类应至少覆盖：

- `statement`：SELECT、INSERT、UPDATE、DELETE、DDL、SHOW、DESCRIBE。
- `queryShape`：JOIN、子查询、UNION、GROUP BY、HAVING、ORDER BY、LIMIT。
- `expression`：二元表达式、函数调用、字面量、列引用、别名、IN、LIKE、IS NULL、参数占位符。
- `frameworkGenerated`：SELECT 1、SELECT COUNT(*)、JDBC metadata 探测、动态 SQL 展开后的条件组合。
- `unsupportedOrDeferred`：Druid 可解析但当前 Nebula SQL 路线不进入必做范围的语法。

`supportLevel` 语义如下：

- `required`：后续 parser-core 必须输出结构化 Nebula AST / 中间表示，缺 adapter 时测试失败。
- `explicitlyUnsupported`：Druid 能解析，但当前路线明确不支持；parser-core 必须返回结构化不支持错误，而不是 raw fallback。
- `deferred`：不进入当前 parser-core 必做范围，但必须写明原因和后续 change 承接边界。

## Raw Fallback 规则

矩阵内所有条目的 `rawFallbackAllowed` 必须为 `false`。后续 parser-core 可以保留原始 SQL 作为 debug、trace 或错误上下文，但不能把 raw SQL 当作语义输出替代结构化 AST。测试断言需要区分“保留原文用于诊断”和“用原文绕过 adapter”。

## Parser-Core 消费方式

`add-nebula-sql-parser-core` 应把 JSON 矩阵作为测试输入。推荐方式是在测试启动时读取 `entries`，按 `supportLevel` 分流：

- `required` 条目：解析 `exampleSql`，断言 Druid AST 类型被 adapter 覆盖，并产生对应 `nebulaAstKind`。
- `explicitlyUnsupported` 条目：断言返回结构化不支持错误，错误中包含矩阵 ID 或稳定错误码。
- `deferred` 条目：不作为 parser-core 必过实现项，但测试报告或文档必须能说明其延期原因。

失败信息至少包含 `id`、`druidAstType` 和 `exampleSql`，便于直接回到矩阵定位缺口。

## Druid 升级规则

任何后续 change 升级 Druid 版本时，必须重新运行样例 SQL 枚举和反射缺口检查，并重新评估：

- 新增、删除或行为变化的 Druid AST 类型。
- 矩阵条目的 `supportLevel`、`adapterRequirement` 和 `testCaseId` 是否仍有效。
- JSON 版本元信息或内容 hash 变化是否改变 parser-core 的验收边界。

升级评估结果应进入矩阵 artifact，而不是只写在临时讨论或提交说明中。

## 测试策略

本 change 的 build 阶段验证重点是产物质量，而不是 parser adapter 行为：

- JSON 能被标准解析器读取，字段齐全，`rawFallbackAllowed` 全部为 `false`。
- Markdown 与 JSON 指向同一矩阵版本，并明确 Markdown 不作为测试事实源。
- 样例 SQL 枚举覆盖 OpenSpec 要求的 SQL 形态。
- 反射扫描缺口都有矩阵条目或排除理由。
- Druid 版本、许可证、Java 8 兼容性验证结论记录在 JSON 元信息中。

后续 parser-core 的测试策略在本矩阵中定义，由 `add-nebula-sql-parser-core` 落地执行。

## 风险与取舍

采用 JSON + Markdown 的组合，会比单一 Markdown 多一点维护成本，但能让后续测试直接消费矩阵，避免“文档说完整、实现不可验证”的漂移。

Druid AST 类型数量较多，因此矩阵不承诺无差别适配所有 Druid 内部类。设计要求是：样例 SQL 实际产出的关键类型必须覆盖；反射扫描发现的候选类型必须有条目或排除理由。

许可证与 Java 8 兼容性需要在 build 阶段验证后写入 artifact。设计阶段只锁定检查要求和记录位置。

## 执行边界

本 Design Doc 指导当前 change 的 build 阶段创建覆盖矩阵 artifact 和维护说明。它不要求修改 `fw-plus-nebula-jdbc` 的运行时代码，也不提前实现 `add-nebula-sql-parser-core` 的 adapter。
