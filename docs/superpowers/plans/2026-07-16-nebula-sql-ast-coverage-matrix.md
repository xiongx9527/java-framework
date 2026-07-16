---
change: define-nebula-sql-ast-coverage-matrix
design-doc: docs/superpowers/specs/2026-07-16-nebula-sql-ast-coverage-matrix-design.md
base-ref: 8498dbc28d5b53b542dc2d5d05d12895268ffabc
---

# Nebula SQL AST 覆盖矩阵实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 交付可维护、可测试消费的 Druid MySQL AST 到 Nebula AST 覆盖矩阵 JSON 与 Markdown 说明。

**Architecture:** 本 change 不修改运行时代码，只新增矩阵 artifact 和维护说明。JSON 是后续 parser-core adapter 完整性测试的机器可读事实源，Markdown 只解释矩阵来源、字段语义、升级规则和维护方式。

**Tech Stack:** Markdown、JSON、Maven Wrapper、Druid `com.alibaba:druid:1.2.24`、Java 8 约束。

## Global Constraints

- 所有产物使用简体中文。
- Druid 基线锁定为 `com.alibaba:druid:1.2.24`，来源为 `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`。
- 不实现 parser adapter，不实现 SQL 到 nGQL 转换，不修改 `fw-plus-nebula-jdbc` 运行时代码。
- JSON 文件 `openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json` 是机器可读事实源。
- Markdown 文件 `openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.md` 只作为人类可读说明。
- 所有矩阵条目必须包含 `rawFallbackAllowed: false`。
- `supportLevel` 只允许 `required`、`explicitlyUnsupported`、`deferred`。
- 反射扫描命中不自动等于必须适配，但必须进入矩阵或写明排除理由。

---

## File Structure

- Create: `openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json`
  - 职责：保存版本元信息、枚举方法、分类、矩阵条目、测试消费规则和升级规则。
- Create: `openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.md`
  - 职责：解释 JSON 的字段语义、覆盖边界、维护流程和后续 parser-core 消费方式。
- Modify: `openspec/changes/define-nebula-sql-ast-coverage-matrix/tasks.md`
  - 职责：每个任务完成并验证后逐项勾选。

## Task 1: 建立 artifact 骨架和 Druid 基线证据

**Files:**
- Create: `openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json`
- Create: `openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.md`
- Modify: `openspec/changes/define-nebula-sql-ast-coverage-matrix/tasks.md`

**Interfaces:**
- Consumes: Design Doc 中的 JSON 顶层结构、Druid 基线和 artifact 双产物决策。
- Produces: 后续任务可追加 `entries`、`reflectedCandidates` 和 Markdown 章节的稳定文件结构。

- [ ] **Step 1: 记录当前 Druid 版本来源**

Run:

```bash
rg -n "druid.version|<artifactId>druid</artifactId>|druid-spring-boot-starter" fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml
```

Expected: 输出包含 `<druid.version>1.2.24</druid.version>`、`com.alibaba:druid` 和 `druid-spring-boot-starter` 的版本管理位置。

- [ ] **Step 2: 尝试解析 Druid POM 元信息**

Run:

```bash
cd fw-plus-core && ./mvnw -q dependency:get -Dartifact=com.alibaba:druid:1.2.24:pom -Dtransitive=false
```

Expected: 命令 exit 0 时，记录 Maven 能解析该 POM；若内部网络或仓库导致失败，不修改 Druid 版本，在 JSON `druid.verificationNotes` 中记录失败命令和原因。

- [ ] **Step 3: 创建 JSON 骨架**

Create `openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json` with this structure:

```json
{
  "schemaVersion": "nebula-sql-ast-coverage-matrix.v1",
  "druid": {
    "groupId": "com.alibaba",
    "artifactId": "druid",
    "version": "1.2.24",
    "versionSource": "fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml",
    "java8Compatible": null,
    "license": null,
    "verificationNotes": [
      "Build 阶段必须用 Maven 解析或构建结果补充许可证和 Java 8 兼容性证据。"
    ]
  },
  "artifacts": {
    "json": "openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json",
    "markdown": "openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.md"
  },
  "supportLevels": [
    "required",
    "explicitlyUnsupported",
    "deferred"
  ],
  "categories": [],
  "enumeration": {
    "sampleSqlDriven": true,
    "reflectionGapCheck": true,
    "packages": [
      "com.alibaba.druid.sql.ast",
      "com.alibaba.druid.sql.dialect.mysql.ast"
    ],
    "sampleSqlGroups": []
  },
  "entries": [],
  "reflectedCandidates": [],
  "parserCoreConsumption": {
    "matrixIsTestFactSource": true,
    "markdownIsTestFactSource": false,
    "failureMessageFields": [
      "id",
      "druidAstType",
      "exampleSql"
    ]
  },
  "upgradePolicy": {
    "rerunSampleSqlEnumeration": true,
    "rerunReflectionGapCheck": true,
    "reevaluateParserCoreAcceptanceBoundary": true
  }
}
```

- [ ] **Step 4: 创建 Markdown 骨架**

Create `openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.md` with sections:

```markdown
# Nebula SQL AST 覆盖矩阵

## 事实源

JSON 文件是后续 parser-core 完整性测试消费的机器可读事实源。本文档只解释矩阵字段、覆盖边界和维护流程。

## Druid 基线

- GroupId: `com.alibaba`
- ArtifactId: `druid`
- Version: `1.2.24`
- Version source: `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`

## 字段说明

## 分类

## 样例 SQL 枚举

## 反射缺口检查

## 支持级别

## Raw Fallback 禁止规则

## Parser-Core 消费方式

## Druid 升级规则
```

- [ ] **Step 5: 验证 JSON 可解析**

Run:

```bash
python3 -m json.tool openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json >/tmp/nebula-sql-ast-coverage-matrix.json
```

Expected: exit 0，无 JSON 解析错误。

- [ ] **Step 6: 勾选 OpenSpec 任务 1.1**

Modify `openspec/changes/define-nebula-sql-ast-coverage-matrix/tasks.md`:

```markdown
- [x] 1.1 确认 Druid SQL Parser 版本、许可证和 Java 8 兼容性。
```

如果许可证或 Java 8 兼容性证据只能记录为“待 Maven 解析环境确认”，不要勾选 1.1；先在 JSON `verificationNotes` 中记录阻塞证据，再暂停处理。

- [ ] **Step 7: 提交**

Run:

```bash
git add openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.md openspec/changes/define-nebula-sql-ast-coverage-matrix/tasks.md
git commit -m "docs: add nebula sql ast matrix baseline"
```

## Task 2: 定义分类、字段结构和升级规则

**Files:**
- Modify: `openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json`
- Modify: `openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.md`
- Modify: `openspec/changes/define-nebula-sql-ast-coverage-matrix/tasks.md`

**Interfaces:**
- Consumes: Task 1 的 JSON 顶层结构。
- Produces: 后续矩阵条目必须引用的 `categories`、字段语义、升级策略和枚举方法。

- [ ] **Step 1: 填充 JSON 分类**

Set `categories` to:

```json
[
  {
    "id": "statement",
    "description": "SELECT、INSERT、UPDATE、DELETE、DDL、SHOW、DESCRIBE 等语句节点。"
  },
  {
    "id": "queryShape",
    "description": "JOIN、子查询、UNION、GROUP BY、HAVING、ORDER BY、LIMIT 等查询结构。"
  },
  {
    "id": "expression",
    "description": "二元表达式、函数调用、字面量、列引用、别名、IN、LIKE、IS NULL、参数占位符。"
  },
  {
    "id": "frameworkGenerated",
    "description": "JDBC、MyBatis、JdbcTemplate 常见探测和动态 SQL 展开形态。"
  },
  {
    "id": "unsupportedOrDeferred",
    "description": "Druid 能解析但当前 Nebula SQL 路线明确不支持或延期的语法。"
  }
]
```

- [ ] **Step 2: 填充样例 SQL 分组**

Set `enumeration.sampleSqlGroups` to include these groups:

```json
[
  {
    "id": "select-basic",
    "constructs": [
      "SELECT 1",
      "SELECT COUNT(*)",
      "列别名",
      "WHERE 条件",
      "ORDER BY",
      "LIMIT"
    ]
  },
  {
    "id": "select-complex",
    "constructs": [
      "JOIN",
      "子查询",
      "聚合",
      "GROUP BY",
      "HAVING",
      "UNION",
      "函数",
      "表达式"
    ]
  },
  {
    "id": "write-and-ddl",
    "constructs": [
      "INSERT",
      "UPDATE",
      "DELETE",
      "CREATE TABLE",
      "DROP TABLE",
      "SHOW",
      "DESCRIBE"
    ]
  },
  {
    "id": "framework-generated",
    "constructs": [
      "? 参数",
      "IN 参数列表",
      "分页",
      "metadata 探测 SQL",
      "动态 SQL 展开后的条件组合"
    ]
  }
]
```

- [ ] **Step 3: 在 Markdown 写明字段结构**

Under `## 字段说明`, document every entry field:

```markdown
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
```

- [ ] **Step 4: 在 Markdown 写明 Druid 升级规则**

Under `## Druid 升级规则`, write:

```markdown
升级 Druid 版本时必须重新运行样例 SQL 枚举和反射缺口检查。维护者需要比较新增、删除或行为变化的 AST 类型，并重新评估 `supportLevel`、`adapterRequirement`、`testCaseId` 以及矩阵内容 hash 是否改变 parser-core 的验收边界。
```

- [ ] **Step 5: 验证字段和枚举值**

Run:

```bash
python3 - <<'PY'
import json
path = "openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json"
data = json.load(open(path, encoding="utf-8"))
assert data["druid"]["version"] == "1.2.24"
assert set(data["supportLevels"]) == {"required", "explicitlyUnsupported", "deferred"}
assert {item["id"] for item in data["categories"]} == {"statement", "queryShape", "expression", "frameworkGenerated", "unsupportedOrDeferred"}
assert data["upgradePolicy"]["rerunSampleSqlEnumeration"] is True
assert data["upgradePolicy"]["rerunReflectionGapCheck"] is True
PY
```

Expected: exit 0。

- [ ] **Step 6: 勾选 OpenSpec 任务 1.2、1.3、2.1、2.2**

Modify `tasks.md`:

```markdown
- [x] 1.2 定义 Druid 版本升级时的 AST 覆盖矩阵更新规则。
- [x] 1.3 制定 MySQL parser AST 类型枚举方法。
- [x] 2.1 定义 Nebula AST 顶层分类和表达式分类。
- [x] 2.2 建立 Druid AST 到 Nebula AST 的覆盖矩阵字段结构。
```

- [ ] **Step 7: 提交**

Run:

```bash
git add openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.md openspec/changes/define-nebula-sql-ast-coverage-matrix/tasks.md
git commit -m "docs: define nebula sql ast matrix schema"
```

## Task 3: 填充查询、表达式和框架生成 SQL 矩阵条目

**Files:**
- Modify: `openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json`
- Modify: `openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.md`
- Modify: `openspec/changes/define-nebula-sql-ast-coverage-matrix/tasks.md`

**Interfaces:**
- Consumes: Task 2 的分类和字段结构。
- Produces: 覆盖 SELECT、JOIN、子查询、聚合、GROUP BY、HAVING、UNION、函数、表达式、别名、参数占位符、JDBC/MyBatis/JdbcTemplate 常见 SQL 形态的矩阵条目。

- [ ] **Step 1: 添加 SELECT 与查询结构条目**

Append entries with IDs and constructs:

```json
[
  {
    "id": "SQL-STMT-SELECT-BASIC",
    "category": "statement",
    "druidAstType": "com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock",
    "sqlConstruct": "基础 SELECT",
    "exampleSql": "SELECT id, name FROM person WHERE age > ? ORDER BY name LIMIT 10",
    "nebulaAstKind": "SelectStatement",
    "adapterRequirement": "输出选择列、数据源、WHERE、ORDER BY、LIMIT 和参数占位符的结构化 AST。",
    "supportLevel": "required",
    "testCaseId": "parseSelectBasic",
    "rawFallbackAllowed": false,
    "notes": "覆盖 JDBC 参数占位符和分页。"
  },
  {
    "id": "SQL-QUERY-JOIN",
    "category": "queryShape",
    "druidAstType": "com.alibaba.druid.sql.ast.statement.SQLJoinTableSource",
    "sqlConstruct": "JOIN",
    "exampleSql": "SELECT p.id, o.amount FROM person p JOIN orders o ON p.id = o.person_id",
    "nebulaAstKind": "JoinRelation",
    "adapterRequirement": "输出左右表、JOIN 类型和 ON 条件表达式。",
    "supportLevel": "required",
    "testCaseId": "parseJoinRelation",
    "rawFallbackAllowed": false,
    "notes": "转换层是否能映射到 nGQL 不在本 change 判断。"
  },
  {
    "id": "SQL-QUERY-SUBQUERY",
    "category": "queryShape",
    "druidAstType": "com.alibaba.druid.sql.ast.statement.SQLSubqueryTableSource",
    "sqlConstruct": "FROM 子查询",
    "exampleSql": "SELECT t.id FROM (SELECT id FROM person WHERE age > 18) t",
    "nebulaAstKind": "SubquerySource",
    "adapterRequirement": "输出子查询数据源和别名。",
    "supportLevel": "required",
    "testCaseId": "parseSubquerySource",
    "rawFallbackAllowed": false,
    "notes": "子查询语义支持由后续 parser-core 测试消费。"
  },
  {
    "id": "SQL-QUERY-UNION",
    "category": "queryShape",
    "druidAstType": "com.alibaba.druid.sql.ast.statement.SQLUnionQuery",
    "sqlConstruct": "UNION",
    "exampleSql": "SELECT id FROM person_a UNION SELECT id FROM person_b",
    "nebulaAstKind": "UnionQuery",
    "adapterRequirement": "输出 UNION 左右查询和 union 类型。",
    "supportLevel": "required",
    "testCaseId": "parseUnionQuery",
    "rawFallbackAllowed": false,
    "notes": "保留结构，不承诺 nGQL 映射。"
  },
  {
    "id": "SQL-QUERY-GROUP-HAVING",
    "category": "queryShape",
    "druidAstType": "com.alibaba.druid.sql.ast.statement.SQLSelectGroupByClause",
    "sqlConstruct": "GROUP BY / HAVING",
    "exampleSql": "SELECT city, COUNT(*) c FROM person GROUP BY city HAVING COUNT(*) > 1",
    "nebulaAstKind": "GroupByClause",
    "adapterRequirement": "输出分组表达式和 HAVING 条件表达式。",
    "supportLevel": "required",
    "testCaseId": "parseGroupByHaving",
    "rawFallbackAllowed": false,
    "notes": "聚合函数由表达式条目覆盖。"
  }
]
```

- [ ] **Step 2: 添加表达式条目**

Append expression entries:

```json
[
  {
    "id": "SQL-EXPR-BINARY",
    "category": "expression",
    "druidAstType": "com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr",
    "sqlConstruct": "二元表达式",
    "exampleSql": "SELECT * FROM person WHERE age >= ? AND status = 'ACTIVE'",
    "nebulaAstKind": "BinaryExpression",
    "adapterRequirement": "输出操作符、左右表达式和逻辑组合。",
    "supportLevel": "required",
    "testCaseId": "parseBinaryExpression",
    "rawFallbackAllowed": false,
    "notes": "覆盖 WHERE 中的比较和 AND 组合。"
  },
  {
    "id": "SQL-EXPR-FUNCTION",
    "category": "expression",
    "druidAstType": "com.alibaba.druid.sql.ast.expr.SQLAggregateExpr",
    "sqlConstruct": "聚合函数",
    "exampleSql": "SELECT COUNT(*) FROM person",
    "nebulaAstKind": "FunctionCallExpression",
    "adapterRequirement": "输出函数名、参数列表和聚合标记。",
    "supportLevel": "required",
    "testCaseId": "parseAggregateFunction",
    "rawFallbackAllowed": false,
    "notes": "普通函数可复用函数调用表达式分类。"
  },
  {
    "id": "SQL-EXPR-ALIAS",
    "category": "expression",
    "druidAstType": "com.alibaba.druid.sql.ast.statement.SQLSelectItem",
    "sqlConstruct": "列别名",
    "exampleSql": "SELECT name AS display_name FROM person",
    "nebulaAstKind": "SelectItem",
    "adapterRequirement": "输出投影表达式和 alias。",
    "supportLevel": "required",
    "testCaseId": "parseSelectAlias",
    "rawFallbackAllowed": false,
    "notes": "覆盖 MyBatis/JdbcTemplate 常见列别名。"
  },
  {
    "id": "SQL-EXPR-PARAMETER",
    "category": "expression",
    "druidAstType": "com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr",
    "sqlConstruct": "? 参数占位符",
    "exampleSql": "SELECT * FROM person WHERE id = ?",
    "nebulaAstKind": "ParameterExpression",
    "adapterRequirement": "输出占位符索引或稳定参数节点，不得展开为字符串。",
    "supportLevel": "required",
    "testCaseId": "parseParameterPlaceholder",
    "rawFallbackAllowed": false,
    "notes": "JDBC PreparedStatement 基础能力。"
  },
  {
    "id": "SQL-EXPR-IN",
    "category": "expression",
    "druidAstType": "com.alibaba.druid.sql.ast.expr.SQLInListExpr",
    "sqlConstruct": "IN 条件",
    "exampleSql": "SELECT * FROM person WHERE id IN (?, ?, ?)",
    "nebulaAstKind": "InListExpression",
    "adapterRequirement": "输出目标表达式和 IN 参数列表。",
    "supportLevel": "required",
    "testCaseId": "parseInListExpression",
    "rawFallbackAllowed": false,
    "notes": "覆盖 MyBatis 动态 IN 列表。"
  }
]
```

- [ ] **Step 3: 添加框架生成 SQL 条目**

Append framework entries:

```json
[
  {
    "id": "SQL-FRAMEWORK-SELECT-ONE",
    "category": "frameworkGenerated",
    "druidAstType": "com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock",
    "sqlConstruct": "SELECT 1",
    "exampleSql": "SELECT 1",
    "nebulaAstKind": "SelectStatement",
    "adapterRequirement": "输出无表 SELECT 和数字字面量投影。",
    "supportLevel": "required",
    "testCaseId": "parseSelectOne",
    "rawFallbackAllowed": false,
    "notes": "常见连接探活 SQL。"
  },
  {
    "id": "SQL-FRAMEWORK-COUNT",
    "category": "frameworkGenerated",
    "druidAstType": "com.alibaba.druid.sql.ast.expr.SQLAggregateExpr",
    "sqlConstruct": "SELECT COUNT(*)",
    "exampleSql": "SELECT COUNT(*) FROM person",
    "nebulaAstKind": "FunctionCallExpression",
    "adapterRequirement": "输出 COUNT 聚合函数和星号参数。",
    "supportLevel": "required",
    "testCaseId": "parseSelectCount",
    "rawFallbackAllowed": false,
    "notes": "分页插件和 JdbcTemplate 常见形态。"
  },
  {
    "id": "SQL-FRAMEWORK-METADATA",
    "category": "frameworkGenerated",
    "druidAstType": "com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlShowColumnsStatement",
    "sqlConstruct": "metadata 探测 SQL",
    "exampleSql": "SHOW COLUMNS FROM person",
    "nebulaAstKind": "MetadataStatement",
    "adapterRequirement": "输出结构化 metadata 探测节点或明确不支持错误。",
    "supportLevel": "required",
    "testCaseId": "parseShowColumnsMetadata",
    "rawFallbackAllowed": false,
    "notes": "用于 JDBC metadata 和框架初始化探测。"
  },
  {
    "id": "SQL-FRAMEWORK-DYNAMIC-WHERE",
    "category": "frameworkGenerated",
    "druidAstType": "com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr",
    "sqlConstruct": "动态 SQL 展开后的条件组合",
    "exampleSql": "SELECT * FROM person WHERE 1 = 1 AND name LIKE ? AND status = ?",
    "nebulaAstKind": "BinaryExpression",
    "adapterRequirement": "输出完整 WHERE 表达式树，不把动态来源作为特殊语义处理。",
    "supportLevel": "required",
    "testCaseId": "parseDynamicWhereExpression",
    "rawFallbackAllowed": false,
    "notes": "MyBatis 动态 SQL 展开后进入 parser 的是普通 SQL。"
  }
]
```

- [ ] **Step 4: 在 Markdown 增加查询覆盖说明**

Under `## 样例 SQL 枚举`, summarize the IDs added in this task and state that `required` entries are parser-core adapter completeness inputs.

- [ ] **Step 5: 验证所有新增条目禁止 raw fallback**

Run:

```bash
python3 - <<'PY'
import json
path = "openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json"
data = json.load(open(path, encoding="utf-8"))
ids = [entry["id"] for entry in data["entries"]]
assert len(ids) == len(set(ids)), "duplicate matrix id"
for entry in data["entries"]:
    assert entry["rawFallbackAllowed"] is False, entry["id"]
    assert entry["supportLevel"] in {"required", "explicitlyUnsupported", "deferred"}, entry["id"]
    for key in ["id", "category", "druidAstType", "sqlConstruct", "exampleSql", "nebulaAstKind", "adapterRequirement", "testCaseId", "notes"]:
        assert entry.get(key), (entry["id"], key)
PY
```

Expected: exit 0。

- [ ] **Step 6: 勾选 OpenSpec 任务 2.3、2.5**

Modify `tasks.md`:

```markdown
- [x] 2.3 覆盖 SELECT、JOIN、子查询、聚合、GROUP BY、HAVING、UNION、函数、表达式、别名和参数占位符。
- [x] 2.5 覆盖正常 JDBC/MyBatis/JdbcTemplate 常见 SQL 形态：`?` 参数、SELECT 1、SELECT COUNT(*)、列别名、IN 条件、分页、SHOW/DESCRIBE、metadata 探测 SQL 和动态 SQL 展开后的条件组合。
```

- [ ] **Step 7: 提交**

Run:

```bash
git add openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.md openspec/changes/define-nebula-sql-ast-coverage-matrix/tasks.md
git commit -m "docs: cover nebula sql query ast matrix"
```

## Task 4: 填充写入、DDL、SHOW/DESCRIBE 和排除项

**Files:**
- Modify: `openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json`
- Modify: `openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.md`
- Modify: `openspec/changes/define-nebula-sql-ast-coverage-matrix/tasks.md`

**Interfaces:**
- Consumes: Task 2 的分类和 supportLevel 语义。
- Produces: 写入、基础 DDL、SHOW/DESCRIBE、明确不支持和延期条目。

- [ ] **Step 1: 添加写入和基础 DDL 条目**

Append entries:

```json
[
  {
    "id": "SQL-STMT-INSERT",
    "category": "statement",
    "druidAstType": "com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement",
    "sqlConstruct": "INSERT",
    "exampleSql": "INSERT INTO person (id, name) VALUES (?, ?)",
    "nebulaAstKind": "InsertStatement",
    "adapterRequirement": "输出目标表、列列表和值列表。",
    "supportLevel": "required",
    "testCaseId": "parseInsertStatement",
    "rawFallbackAllowed": false,
    "notes": "转换层是否映射为点或边不在本 change 内判断。"
  },
  {
    "id": "SQL-STMT-UPDATE",
    "category": "statement",
    "druidAstType": "com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement",
    "sqlConstruct": "UPDATE",
    "exampleSql": "UPDATE person SET name = ? WHERE id = ?",
    "nebulaAstKind": "UpdateStatement",
    "adapterRequirement": "输出目标表、赋值列表和 WHERE 条件。",
    "supportLevel": "required",
    "testCaseId": "parseUpdateStatement",
    "rawFallbackAllowed": false,
    "notes": "WHERE 表达式复用表达式 adapter。"
  },
  {
    "id": "SQL-STMT-DELETE",
    "category": "statement",
    "druidAstType": "com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlDeleteStatement",
    "sqlConstruct": "DELETE",
    "exampleSql": "DELETE FROM person WHERE id = ?",
    "nebulaAstKind": "DeleteStatement",
    "adapterRequirement": "输出目标表和 WHERE 条件。",
    "supportLevel": "required",
    "testCaseId": "parseDeleteStatement",
    "rawFallbackAllowed": false,
    "notes": "禁止 raw SQL 作为删除语义。"
  },
  {
    "id": "SQL-STMT-CREATE-TABLE",
    "category": "statement",
    "druidAstType": "com.alibaba.druid.sql.ast.statement.SQLCreateTableStatement",
    "sqlConstruct": "CREATE TABLE",
    "exampleSql": "CREATE TABLE person (id BIGINT PRIMARY KEY, name VARCHAR(64))",
    "nebulaAstKind": "CreateSchemaStatement",
    "adapterRequirement": "输出结构化建表节点或明确不支持错误。",
    "supportLevel": "required",
    "testCaseId": "parseCreateTableStatement",
    "rawFallbackAllowed": false,
    "notes": "基础 DDL 必须可表达，语义转换由后续 change 决定。"
  },
  {
    "id": "SQL-STMT-DROP-TABLE",
    "category": "statement",
    "druidAstType": "com.alibaba.druid.sql.ast.statement.SQLDropTableStatement",
    "sqlConstruct": "DROP TABLE",
    "exampleSql": "DROP TABLE person",
    "nebulaAstKind": "DropSchemaStatement",
    "adapterRequirement": "输出结构化 drop 节点或明确不支持错误。",
    "supportLevel": "required",
    "testCaseId": "parseDropTableStatement",
    "rawFallbackAllowed": false,
    "notes": "基础 DDL 覆盖项。"
  }
]
```

- [ ] **Step 2: 添加 SHOW/DESCRIBE 条目**

Append entries:

```json
[
  {
    "id": "SQL-STMT-SHOW-TABLES",
    "category": "statement",
    "druidAstType": "com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlShowTablesStatement",
    "sqlConstruct": "SHOW TABLES",
    "exampleSql": "SHOW TABLES",
    "nebulaAstKind": "MetadataStatement",
    "adapterRequirement": "输出 metadata 查询节点。",
    "supportLevel": "required",
    "testCaseId": "parseShowTablesStatement",
    "rawFallbackAllowed": false,
    "notes": "JDBC metadata 常见入口。"
  },
  {
    "id": "SQL-STMT-DESCRIBE",
    "category": "statement",
    "druidAstType": "com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlDescribeStatement",
    "sqlConstruct": "DESCRIBE",
    "exampleSql": "DESCRIBE person",
    "nebulaAstKind": "MetadataStatement",
    "adapterRequirement": "输出目标对象和描述动作。",
    "supportLevel": "required",
    "testCaseId": "parseDescribeStatement",
    "rawFallbackAllowed": false,
    "notes": "覆盖 DESCRIBE / DESC 形态。"
  }
]
```

- [ ] **Step 3: 添加明确不支持和延期条目**

Append entries:

```json
[
  {
    "id": "SQL-UNSUPPORTED-PROCEDURE",
    "category": "unsupportedOrDeferred",
    "druidAstType": "com.alibaba.druid.sql.ast.statement.SQLCallStatement",
    "sqlConstruct": "CALL / routine",
    "exampleSql": "CALL refresh_person()",
    "nebulaAstKind": "UnsupportedStatement",
    "adapterRequirement": "返回结构化不支持错误，错误信息包含矩阵 ID。",
    "supportLevel": "explicitlyUnsupported",
    "testCaseId": "rejectRoutineCall",
    "rawFallbackAllowed": false,
    "notes": "routine 不在本路线当前范围。"
  },
  {
    "id": "SQL-DEFERRED-TRIGGER",
    "category": "unsupportedOrDeferred",
    "druidAstType": "com.alibaba.druid.sql.ast.statement.SQLCreateTriggerStatement",
    "sqlConstruct": "CREATE TRIGGER",
    "exampleSql": "CREATE TRIGGER trg BEFORE INSERT ON person FOR EACH ROW SET @x = 1",
    "nebulaAstKind": "DeferredStatement",
    "adapterRequirement": "当前 parser-core 不作为必做项；若 Druid 能解析，必须返回结构化延期或不支持错误。",
    "supportLevel": "deferred",
    "testCaseId": "deferCreateTrigger",
    "rawFallbackAllowed": false,
    "notes": "trigger 由未来独立 change 决定。"
  }
]
```

- [ ] **Step 4: 在 Markdown 增加不支持和延期说明**

Under `## 支持级别`, write:

```markdown
- `required`：后续 parser-core 必须输出结构化 Nebula AST / 中间表示。
- `explicitlyUnsupported`：Druid 能解析，但当前路线明确不支持；必须返回结构化不支持错误。
- `deferred`：不进入当前 parser-core 必做范围；必须写明原因和后续 change 边界。
```

- [ ] **Step 5: 验证写入和 DDL 覆盖存在**

Run:

```bash
python3 - <<'PY'
import json
path = "openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json"
data = json.load(open(path, encoding="utf-8"))
constructs = {entry["sqlConstruct"] for entry in data["entries"]}
for expected in ["INSERT", "UPDATE", "DELETE", "CREATE TABLE", "DROP TABLE", "SHOW TABLES", "DESCRIBE"]:
    assert expected in constructs, expected
for entry in data["entries"]:
    assert entry["rawFallbackAllowed"] is False, entry["id"]
PY
```

Expected: exit 0。

- [ ] **Step 6: 勾选 OpenSpec 任务 2.4**

Modify `tasks.md`:

```markdown
- [x] 2.4 覆盖 INSERT、UPDATE、DELETE 和基础 DDL。
```

- [ ] **Step 7: 提交**

Run:

```bash
git add openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.md openspec/changes/define-nebula-sql-ast-coverage-matrix/tasks.md
git commit -m "docs: cover nebula sql write and ddl ast matrix"
```

## Task 5: 定义 adapter 完整性测试策略和 parser-core 消费规则

**Files:**
- Modify: `openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json`
- Modify: `openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.md`
- Modify: `openspec/changes/define-nebula-sql-ast-coverage-matrix/tasks.md`

**Interfaces:**
- Consumes: 已填充的 `entries`。
- Produces: 后续 `add-nebula-sql-parser-core` 可以直接采用的测试规则。

- [ ] **Step 1: 完善 JSON `parserCoreConsumption`**

Set `parserCoreConsumption` to:

```json
{
  "matrixIsTestFactSource": true,
  "markdownIsTestFactSource": false,
  "requiredBehavior": {
    "required": "解析 exampleSql，断言 Druid AST 类型被 adapter 覆盖，并产生 nebulaAstKind。",
    "explicitlyUnsupported": "解析到该结构时返回结构化不支持错误，错误中包含矩阵 ID 或稳定错误码。",
    "deferred": "不作为当前 parser-core 必过实现项，但测试报告或文档必须能说明延期原因。"
  },
  "rawSqlPolicy": {
    "debugTraceAllowed": true,
    "semanticFallbackAllowed": false
  },
  "failureMessageFields": [
    "id",
    "druidAstType",
    "exampleSql"
  ]
}
```

- [ ] **Step 2: 在 Markdown 写入 parser-core 消费方式**

Under `## Parser-Core 消费方式`, write:

```markdown
后续 `add-nebula-sql-parser-core` 必须读取 JSON 矩阵或从 JSON 生成测试输入。`required` 条目缺 adapter 时测试失败；`explicitlyUnsupported` 条目必须返回结构化不支持错误；`deferred` 条目不进入当前必做范围，但必须保留延期原因。失败信息至少包含 `id`、`druidAstType` 和 `exampleSql`。
```

- [ ] **Step 3: 在 Markdown 写入 raw fallback 禁止规则**

Under `## Raw Fallback 禁止规则`, write:

```markdown
raw SQL 只允许作为 debug、trace 或错误上下文保存，不允许作为语义 fallback。矩阵内所有条目的 `rawFallbackAllowed` 必须为 `false`。后续 parser-core 如果用 raw SQL 替代结构化 Nebula AST，完整性测试必须失败。
```

- [ ] **Step 4: 验证 parser-core 消费规则**

Run:

```bash
python3 - <<'PY'
import json
path = "openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json"
data = json.load(open(path, encoding="utf-8"))
consume = data["parserCoreConsumption"]
assert consume["matrixIsTestFactSource"] is True
assert consume["markdownIsTestFactSource"] is False
assert consume["rawSqlPolicy"]["semanticFallbackAllowed"] is False
for field in ["id", "druidAstType", "exampleSql"]:
    assert field in consume["failureMessageFields"]
assert all(entry["rawFallbackAllowed"] is False for entry in data["entries"])
PY
```

Expected: exit 0。

- [ ] **Step 5: 勾选 OpenSpec 任务 3.1、3.2**

Modify `tasks.md`:

```markdown
- [x] 3.1 定义 adapter 完整性测试策略，要求矩阵内节点缺失适配时测试失败。
- [x] 3.2 定义 raw fallback 禁止规则和测试断言。
```

- [ ] **Step 6: 提交**

Run:

```bash
git add openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.md openspec/changes/define-nebula-sql-ast-coverage-matrix/tasks.md
git commit -m "docs: define nebula sql parser matrix test policy"
```

## Task 6: 最终一致性验证和交付收尾

**Files:**
- Modify: `openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json`
- Modify: `openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.md`
- Modify: `openspec/changes/define-nebula-sql-ast-coverage-matrix/tasks.md`

**Interfaces:**
- Consumes: Task 1 到 Task 5 的全部 artifact。
- Produces: 全部 OpenSpec tasks 勾选，build guard 可进入 verify 的变更状态。

- [ ] **Step 1: 验证 JSON 格式**

Run:

```bash
python3 -m json.tool openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json >/tmp/nebula-sql-ast-coverage-matrix.json
```

Expected: exit 0。

- [ ] **Step 2: 验证矩阵覆盖关键 SQL 构造**

Run:

```bash
python3 - <<'PY'
import json
path = "openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json"
data = json.load(open(path, encoding="utf-8"))
constructs = {entry["sqlConstruct"] for entry in data["entries"]}
required = {
    "基础 SELECT",
    "JOIN",
    "FROM 子查询",
    "UNION",
    "GROUP BY / HAVING",
    "二元表达式",
    "聚合函数",
    "列别名",
    "? 参数占位符",
    "IN 条件",
    "SELECT 1",
    "SELECT COUNT(*)",
    "metadata 探测 SQL",
    "动态 SQL 展开后的条件组合",
    "INSERT",
    "UPDATE",
    "DELETE",
    "CREATE TABLE",
    "DROP TABLE",
    "SHOW TABLES",
    "DESCRIBE"
}
missing = sorted(required - constructs)
assert not missing, missing
PY
```

Expected: exit 0。

- [ ] **Step 3: 验证 Markdown 明确 JSON 是事实源**

Run:

```bash
rg -n "JSON.*事实源|Markdown.*说明|rawFallbackAllowed|Parser-Core|Druid 升级" openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.md
```

Expected: 输出匹配 JSON 事实源、Markdown 说明、raw fallback、Parser-Core 消费方式和 Druid 升级规则。

- [ ] **Step 4: 勾选 OpenSpec 任务 3.3**

Modify `tasks.md`:

```markdown
- [x] 3.3 输出覆盖矩阵文档或测试资源，供 `add-nebula-sql-parser-core` 使用。
```

- [ ] **Step 5: 验证 tasks.md 全部勾选**

Run:

```bash
grep -c -- "- \\[ \\]" openspec/changes/define-nebula-sql-ast-coverage-matrix/tasks.md
```

Expected: `0`。

- [ ] **Step 6: 运行 OpenSpec 校验**

Run:

```bash
openspec validate define-nebula-sql-ast-coverage-matrix --strict
```

Expected: exit 0。若 `openspec` 不可用，记录命令不可用，不把它当作通过证据。

- [ ] **Step 7: 运行 Comet build guard**

Run:

```bash
COMET_ENV="${COMET_ENV:-$(find . "$HOME"/.*/skills "$HOME/.config" "$HOME/.gemini" -path '*/comet/scripts/comet-env.mjs' -type f -print -quit 2>/dev/null)}"
COMET_SCRIPTS_DIR="$(node "$COMET_ENV")"
node "$COMET_SCRIPTS_DIR/comet-guard.mjs" define-nebula-sql-ast-coverage-matrix build --apply
```

Expected: 所有 build guard 检查通过，并推进到 `phase: verify`。

- [ ] **Step 8: 提交最终收尾**

Run:

```bash
git add openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.md openspec/changes/define-nebula-sql-ast-coverage-matrix/tasks.md
git commit -m "docs: finalize nebula sql ast coverage matrix"
```

## Self-Review Checklist

- [ ] 每个 OpenSpec task 1.1 到 3.3 都有对应计划步骤。
- [ ] JSON 是唯一机器可读事实源，Markdown 明确不是测试事实源。
- [ ] 每个矩阵条目都有 `rawFallbackAllowed: false`。
- [ ] `supportLevel` 只使用 `required`、`explicitlyUnsupported`、`deferred`。
- [ ] Druid 升级规则要求重新运行样例 SQL 枚举和反射缺口检查。
- [ ] 后续 parser-core 的失败信息要求包含 `id`、`druidAstType`、`exampleSql`。
