---
change: add-nebula-sql-parser-core
design-doc: docs/superpowers/specs/2026-07-17-nebula-sql-parser-core-design.md
base-ref: ce556fc260050b85ced425180f850a7fc9299256
---

# Nebula SQL Parser Core Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `fw-plus-nebula-jdbc` 内新增与执行层解耦的 Druid MySQL parser core，输出稳定 Nebula SQL IR。

**Architecture:** Parser API 位于 `com.vesoft.nebula.jdbc.sql.parser`，IR 位于 `parser.ast`，错误模型位于 `parser.error`，Druid 适配器位于 `parser.druid`。对外只暴露自有 IR 和结构化异常，Druid AST 不出 `parser.druid` 包，也不接入现有 JDBC nGQL 直连路径。

**Tech Stack:** Java 8、JUnit 4、Maven Wrapper、`com.alibaba:druid:1.2.24`。

## Global Constraints

- 只在 build 阶段实现 `add-nebula-sql-parser-core`，不合并、不 push、不 archive。
- 遵守 Java 8，不使用 record、sealed class、`List.of()`、`Map.copyOf()` 等 Java 9+ API。
- 使用 `fw-plus-core` 模块内 Maven Wrapper 运行验证。
- Parser core 不创建 Nebula 连接、不调用 `NebulaClientFacade`、不生成 nGQL、不做 schema 映射。
- Raw SQL 仅能作为脱敏截断诊断字段，不能作为 required 条目的语义 fallback。
- 矩阵事实源为 `openspec/changes/archive/2026-07-17-define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json`。

---

### Task 1: Parser 依赖、API、IR 和错误模型骨架

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/pom.xml`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/parser/NebulaSqlParser.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/parser/NebulaSqlParseResult.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/parser/NebulaSqlParameter.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/parser/ParserDiagnostic.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/parser/StatementKind.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/parser/ast/*.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/parser/error/*.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/sql/parser/NebulaSqlParserApiTest.java`

**Interfaces:**
- Produces: `NebulaSqlParser.parse(String)`, `NebulaSqlParseResult`, `NebulaSqlStatement`, `NebulaExpression`, `NebulaSqlParseException`。

- [ ] **Step 1: Write failing API and error model tests**

Create `NebulaSqlParserApiTest` with assertions for invalid input diagnostics, SELECT parse shape, and Java-facing API immutability.

- [ ] **Step 2: Run targeted test to verify it fails**

Run: `./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaSqlParserApiTest test`
Expected: compile failure because parser API classes do not exist.

- [ ] **Step 3: Add Druid dependency and parser API/IR/error skeleton**

Add unmanaged dependency usage for `com.alibaba:druid` because version is already controlled by `fw-plus-dependencies`.

- [ ] **Step 4: Run targeted test to verify API compiles**

Run: `./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaSqlParserApiTest test`
Expected: tests compile and parser behavior failures now point at missing adapter behavior, not missing classes.

- [ ] **Step 5: Commit**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/pom.xml fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/parser fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/sql/parser/NebulaSqlParserApiTest.java
git commit -m "feat(nebula): add SQL parser core API"
```

### Task 2: Druid adapter statement 和表达式解析

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/parser/druid/DruidNebulaSqlParser.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/parser/druid/DruidNebulaAstAdapter.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/parser/ast/*.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/sql/parser/DruidNebulaSqlParserTest.java`

**Interfaces:**
- Consumes: Task 1 parser API and IR classes.
- Produces: `DruidNebulaSqlParser` supporting SELECT、INSERT、UPDATE、DELETE、CREATE/DROP TABLE、SHOW、DESCRIBE、JOIN、子查询、UNION、GROUP BY、HAVING、ORDER BY、LIMIT、函数、二元表达式、IN、LIKE、IS NULL、参数占位符。

- [ ] **Step 1: Write failing adapter tests**

Create tests covering the examples from the design doc: basic SELECT, JOIN, subquery, group/having, union, INSERT, UPDATE, DELETE, CREATE TABLE, SHOW TABLES, SHOW COLUMNS, DESCRIBE, parameter order.

- [ ] **Step 2: Run targeted test to verify it fails**

Run: `./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=DruidNebulaSqlParserTest test`
Expected: failures identify missing Druid adapter behavior.

- [ ] **Step 3: Implement Druid parser and adapter**

Use `SQLUtils.parseStatements(sql, JdbcConstants.MYSQL)`, reject empty and multi-statement input, convert known Druid nodes to IR, and create `ADAPTER_GAP` errors for unknown nodes instead of raw fallback.

- [ ] **Step 4: Run targeted test to verify it passes**

Run: `./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=DruidNebulaSqlParserTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/parser fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/sql/parser/DruidNebulaSqlParserTest.java
git commit -m "feat(nebula): adapt Druid SQL AST to parser IR"
```

### Task 3: 矩阵驱动测试、隔离测试和方言文档

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/sql/parser/NebulaSqlParserMatrixTest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/sql/parser/NebulaSqlParserIsolationTest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/resources/META-INF/fw-plus-nebula-sql-parser.md`
- Modify: `openspec/changes/add-nebula-sql-parser-core/tasks.md`

**Interfaces:**
- Consumes: Task 2 parser behavior and archived coverage matrix JSON.
- Produces: Matrix acceptance coverage, parser isolation assertions, first-stage dialect boundary documentation, completed task checkboxes.

- [ ] **Step 1: Write matrix and isolation tests**

Matrix test reads archived JSON, extracts `entries`, and validates `required` entries parse to the expected Nebula IR kind, `explicitlyUnsupported` entries throw `UNSUPPORTED_FEATURE`, and `deferred` entries throw `DEFERRED_FEATURE`.

- [ ] **Step 2: Run targeted tests to verify failures or gaps**

Run: `./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaSqlParserMatrixTest,NebulaSqlParserIsolationTest test`
Expected: failures only for unsupported/deferred routing or missing matrix edge cases.

- [ ] **Step 3: Fix adapter gaps and add dialect boundary document**

Document supported statement families, unsupported stored procedure handling, deferred trigger handling, raw SQL diagnostic rules, and downstream converter boundaries.

- [ ] **Step 4: Run targeted and module verification**

Run: `./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc test`
Expected: PASS.

- [ ] **Step 5: Check Comet tasks and commit**

Mark `openspec/changes/add-nebula-sql-parser-core/tasks.md` tasks 1.0 through 3.4 complete after matching tests pass.

```bash
git add fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/sql/parser fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/resources/META-INF/fw-plus-nebula-sql-parser.md openspec/changes/add-nebula-sql-parser-core/tasks.md
git commit -m "test(nebula): verify SQL parser matrix coverage"
```

## Self-Review

- Spec coverage: stable IR、复杂 SQL 构造、结构化错误、矩阵验收、纯解析隔离均映射到 Task 1-3。
- Placeholder scan: 本计划未使用 TBD/TODO/implement later 等占位语。
- Type consistency: Parser API、IR、error、Druid adapter 命名在任务间一致。
