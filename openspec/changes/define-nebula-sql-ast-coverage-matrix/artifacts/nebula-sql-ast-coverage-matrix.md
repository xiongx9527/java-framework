# Nebula SQL AST 覆盖矩阵

## 事实源

JSON 文件是后续 parser-core 完整性测试消费的机器可读事实源；Markdown 文档只作说明，不能替代 JSON 作为测试事实源。

## Druid 基线

- GroupId: `com.alibaba`
- ArtifactId: `druid`
- Version: `1.2.24`
- Version source: `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`
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
- `categories`、`entries` 和 `reflectedCandidates` 由后续枚举任务填充。
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

反射检查用于发现 Druid AST 包中未被样例 SQL 枚举覆盖的候选类型。候选类型进入 `reflectedCandidates`，再由维护者决定支持、明确不支持或延期。

## 支持级别

- `required`：后续 parser-core 必须输出结构化 Nebula AST / 中间表示。
- `explicitlyUnsupported`：Druid 能解析，但当前路线明确不支持；必须返回结构化不支持错误。
- `deferred`：不进入当前 parser-core 必做范围；必须写明原因和后续 change 边界。

## Raw Fallback 禁止规则

raw SQL 只允许作为 debug、trace 或错误上下文保存，不允许作为语义 fallback。矩阵内所有条目的 `rawFallbackAllowed` 必须为 `false`。后续 parser-core 如果用 raw SQL 替代结构化 Nebula AST，完整性测试必须失败。

## Parser-Core 消费方式

后续 `add-nebula-sql-parser-core` 必须读取 JSON 矩阵或从 JSON 生成测试输入。`required` 条目缺 adapter 时测试失败；`explicitlyUnsupported` 条目必须返回结构化不支持错误；`deferred` 条目不进入当前必做范围，但必须保留延期原因。失败信息至少包含 `id`、`druidAstType` 和 `exampleSql`。

## Druid 升级规则

升级 Druid 版本时必须重新运行样例 SQL 枚举和反射缺口检查。维护者需要比较新增、删除或行为变化的 AST 类型，并重新评估 `supportLevel`、`adapterRequirement`、`testCaseId` 以及矩阵内容 hash 是否改变 parser-core 的验收边界。
