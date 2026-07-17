# Nebula SQL AST 覆盖矩阵

## 事实源

JSON 文件是后续 parser-core 完整性测试消费的机器可读事实源；Markdown 文档只作说明，不能替代 JSON 作为测试事实源。

## Druid 基线

- 组 ID（GroupId）: `com.alibaba`
- 构件 ID（ArtifactId）: `druid`
- 版本（Version）: `1.2.24`
- 版本来源（Version source）: `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`
- 许可证：已验证为 `Apache 2`，来源为 Maven Central POM（`https://repo1.maven.org/maven2/com/alibaba/druid/1.2.24/druid-1.2.24.pom`），许可证 URL 为 `http://www.apache.org/licenses/LICENSE-2.0.txt`。
- Java 8 兼容性：已验证。Maven Central JAR（`https://repo1.maven.org/maven2/com/alibaba/druid/1.2.24/druid-1.2.24.jar`）包含 1893 个 class，最小和最大 major version 52，表明字节码兼容 Java 8。
- 证据说明：`./mvnw -q dependency:get -Dartifact=com.alibaba:druid:1.2.24:pom -Dtransitive=false` 因当前 worktree 缺少 `.mvn/wrapper/maven-wrapper.properties` 及 Maven Wrapper 主类而失败，未进入依赖解析；上述 Maven Central POM/JAR 直接证据作为替代来源。

## 字段说明

- `id`：稳定矩阵 ID，用于测试失败定位。
- `category`：顶层分类，必须来自 JSON `categories`。
- `druidAstType`：Druid AST 类型全限定名或明确类名。
- `sqlConstruct`：SQL 构造名称。
- `exampleSql`：能触发该 AST 的示例 SQL。
- `nebulaAstKind`：Nebula AST / 中间表示分类。
- `adapterRequirement`：后续 adapter 需要实现的结构化输出或错误边界。
- `supportLevel`：`required`、`explicitlyUnsupported` 或 `deferred`。
- `testCaseId`：后续 parser-core 测试或生成测试的稳定引用。
- `rawFallbackAllowed`：固定为 `false`。
- `notes`：排除理由、限制条件或维护说明。
- `schemaVersion` 固定标识矩阵结构版本。
- `druid` 记录解析器依赖坐标、来源和验证状态。
- `artifacts` 记录双产物路径。
- `supportLevels` 定义条目的支持级别枚举。
- `entryRequirements` 是后续任务和 parser-core 测试必须消费的机器约束，定义每个矩阵条目的必填字段、Raw Fallback 禁止值和支持级别枚举。
- `categories`、`entries` 和 `reflectedCandidates` 已交付：23 个矩阵条目和 41 个经锁定 Druid JAR 核验、并由样例 SQL 实际 AST 遍历审计的候选记录共同定义当前验收边界。
- `enumeration` 描述样例 SQL 驱动枚举和反射缺口检查的范围。
- `parserCoreConsumption` 定义测试事实源和失败诊断字段。
- `upgradePolicy` 定义 Druid 升级后的重新验证动作。

## 分类

矩阵条目必须归入以下顶层分类：

- `statement`：SELECT、INSERT、UPDATE、DELETE、DDL、SHOW、DESCRIBE 等语句节点。
- `queryShape`：JOIN、子查询、UNION、GROUP BY、HAVING、ORDER BY、LIMIT 等查询结构。
- `expression`：二元表达式、函数调用、字面量、列引用、别名、IN、LIKE、IS NULL、参数占位符。
- `frameworkGenerated`：JDBC、MyBatis、JdbcTemplate 常见探测和动态 SQL 展开形态。
- `unsupportedOrDeferred`：Druid 能解析但当前 Nebula SQL 路线明确不支持或延期的语法。

## 样例 SQL 枚举

枚举应以可执行样例 SQL 为入口，覆盖 `com.alibaba.druid.sql.ast` 与 `com.alibaba.druid.sql.dialect.mysql.ast`，并记录 SQL 到 AST 类型的映射。

Task 3 增加以下查询、表达式和框架生成 SQL 覆盖条目：

- 查询结构：`SQL-STMT-SELECT-BASIC`、`SQL-QUERY-JOIN`、`SQL-QUERY-SUBQUERY`、`SQL-QUERY-UNION`、`SQL-QUERY-GROUP-HAVING`。
- 表达式：`SQL-EXPR-BINARY`、`SQL-EXPR-FUNCTION`、`SQL-EXPR-ALIAS`、`SQL-EXPR-PARAMETER`、`SQL-EXPR-IN`。
- 框架生成形态：`SQL-FRAMEWORK-SELECT-ONE`、`SQL-FRAMEWORK-COUNT`、`SQL-FRAMEWORK-METADATA`、`SQL-FRAMEWORK-DYNAMIC-WHERE`。

以上条目的 `supportLevel` 均为 `required`，是 parser-core adapter 完整性的输入，必须纳入后续结构化 AST 完整性测试。

## 反射缺口检查

本次反射缺口检查同时覆盖类存在性和实际 AST：使用 `SQLUtils.parseStatements(exampleSql, JdbcConstants.MYSQL)` 解析全部 23 个样例 SQL，并从每个 `SQLStatement` 调用 `accept(SQLASTVisitorAdapter)`。visitor 的 `preVisit` 会随 Druid 的递归下降记录每个访问到的 `SQLObject` 具体运行时类，最终得到 39 个去重实际 AST 类。该集合逐项与 `entries[].druidAstType` 和 `reflectedCandidates[].className` 核对，任何实际类都必须是 entry 主类型，或有明确候选处置。

锁定 JAR `/tmp/druid-1.2.24-review-check.jar`（`com.alibaba:druid:1.2.24`，SHA-256 `1db6e535ce91e4982f5d27882950e35811a3a3c7d58576c865b1cb088794fe0e`）仍用于 `jar tf` 的候选类存在性核对，但该 `/tmp` 路径只是本次临时核验输入。可移植复核依据是 Maven 坐标、SHA-256、上述解析/递归遍历方法和 JSON 内的处置记录；复核者可从 Maven Central 重新取得相同坐标的 JAR。

`reflectedCandidates` 现有 41 条记录，包含每个实际遍历到但未作为 entry 主类型的类，以及为反射类存在性保留的候选类。`coveredByMatrix` 表示该类本身是矩阵 entry；`coveredByContainingEntry` 表示外层语句、查询、表源、`SQLLimit`、`SQLOrderBy`、`SQLSelect`、`SQLSelectOrderByItem`、`SQLSelectStatement` 等容器由关联 entry 递归输出，因而不单列 adapter acceptance entry。对于 `DESCRIBE person`，主 `druidAstType` 和 `coveredByMatrix` 候选均为 `MySqlExplainStatement`；`SQLDescribeStatement` 虽存在于 Druid 1.2.24 JAR，但不是该样例的实际顶层 AST，故保留并明确记录其反射存在性处置。`excludedInternalContainer` 处置 DDL/DML 内部组装节点；`excludedLiteralOrIdentifier` 处置 `SQLIdentifierExpr`、`SQLIntegerExpr`、字符串、属性引用和通配符等叶子，它们必须作为父语句/表达式的结构化内容保留，但不能把每种叶子扩张为独立 SQL 构造。`explicitlyUnsupported` 和 `deferred` 分别保持 CALL 与 TRIGGER 的既有边界。每项都包含类名、处置状态、关联 entry 和理由。

## 支持级别

- `required`：后续 parser-core 必须输出结构化 Nebula AST / 中间表示。
- `explicitlyUnsupported`：Druid 能解析，但当前路线明确不支持；必须返回结构化不支持错误。
- `deferred`：不进入当前 parser-core 必做范围；必须写明原因和后续 change 边界。

## Raw Fallback 禁止规则

raw SQL 仅可用于 debug、trace 或错误上下文，默认关闭且必须显式开启。任何保留的 raw SQL 都必须脱敏并截断，禁止记录绑定参数值；它绝不能作为语义 fallback。矩阵内所有条目的 `rawFallbackAllowed` 必须为 `false`。后续 Parser-Core 如果用 raw SQL 替代结构化 Nebula AST，完整性测试必须失败。

## Parser-Core 消费方式

后续 `add-nebula-sql-parser-core` 必须读取 JSON 矩阵或从 JSON 生成测试输入。`required` 条目缺 adapter 时测试失败；`explicitlyUnsupported` 条目必须返回结构化不支持错误；`deferred` 条目不进入当前必做范围，但必须保留延期原因。失败信息至少包含 `id`、`druidAstType` 和 `exampleSql`。

## Druid 升级规则

升级 Druid 版本时必须重新运行样例 SQL 枚举和反射缺口检查。维护者需要比较新增、删除或行为变化的 AST 类型，并重新评估 `supportLevel`、`adapterRequirement`、`testCaseId` 以及矩阵内容 hash 是否改变 parser-core 的验收边界。
