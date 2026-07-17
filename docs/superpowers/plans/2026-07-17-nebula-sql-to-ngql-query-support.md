---
change: add-nebula-sql-to-ngql-query-support
design-doc: docs/superpowers/specs/2026-07-17-nebula-sql-to-ngql-query-support-design.md
base-ref: ce556fc260050b85ced425180f850a7fc9299256
---

# Nebula SQL To nGQL Query Support Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增可复用的 SQL SELECT 到 nGQL query converter 核心，并保持现有 nGQL 直连路径不变。

**Architecture:** 在 `fw-plus-nebula-jdbc` 内新增 `com.vesoft.nebula.jdbc.sql.query` 包，包含 converter 入口、adapter-ready SELECT IR 边界、schema mapping、query plan、错误模型和执行适配。当前 `add-nebula-sql-parser-core` 分支尚未落地 Java API，因此本 plan 用最小稳定 SELECT IR 表达 parser core 预期输出；后续 parser core 合并后只需添加 adapter，不改变 converter 核心。

**Tech Stack:** Java 8、JUnit 4、Mockito、现有 `NebulaConnection.execute(String)` 执行链路、Maven wrapper。

## Global Constraints

- 使用 Java 8，不使用 record、sealed class、`List.of()`、`Map.copyOf()` 等 Java 9+ API。
- 不直接解析 SQL 字符串，不引入 Druid AST 作为 query converter 公共契约。
- 不实现 INSERT、UPDATE、DELETE、DDL、事务、PreparedStatement 完整绑定、ResultSetMetaData、MySQL wire protocol。
- 缺失 schema mapping、不可映射结构和 query-only 越界必须返回明确 `SQLException`。
- 每个可验证行为先补测试并确认失败，再实现最小代码。

---

### Task 1: Query Plan、Schema Mapping 与 Converter 边界

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/query/*.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/sql/query/NebulaSqlQueryConverterTest.java`

**Interfaces:**
- Produces: `NebulaSqlQueryConverter.convert(NebulaQueryStatement, NebulaQueryBindings, NebulaSchemaMappingRegistry)`
- Produces: `NgqlQueryPlan` with `ngql`、`queryKind`、`columns`、`fidelity`、`trace`
- Produces: `NebulaSchemaMappingRegistry` and `TableMapping` / `ColumnMapping`

- [x] **Step 1: Write failing tests**
  - 点查询 `SELECT * FROM person WHERE id = 'p1'` 的 IR + tag mapping 转为包含 `MATCH (person:person)`、`id(person) == "p1"`、`RETURN person.id AS id, person.name AS name` 的 plan。
  - 断言输出列、trace、`QueryFidelity.EQUIVALENT`。

- [x] **Step 2: Run focused test and verify RED**
  - Run: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaSqlQueryConverterTest test`
  - Expected: compile fail or test fail because query package does not exist.

- [x] **Step 3: Implement minimal boundary and tag conversion**
  - Add Java 8 POJOs/enums for query statement, expressions, mapping and plan.
  - Implement `DefaultNebulaSqlQueryConverter` for single tag table, `*` projection, id equality filter.

- [x] **Step 4: Run focused test and verify GREEN**
  - Same command as Step 2.
  - Expected: test pass.

- [x] **Step 5: Commit**
  - `git add ... && git commit -m "feat: add nebula query converter boundary"`

### Task 2: Edge Query、Filter、Order 和 Pagination

**Files:**
- Modify: query package from Task 1
- Test: `NebulaSqlQueryConverterTest.java`

**Interfaces:**
- Consumes: Task 1 query model and schema mapping.
- Produces: edge query conversion and nGQL clauses for boolean filter、`IN`、`IS NULL`、`ORDER BY`、`LIMIT/OFFSET`。

- [x] **Step 1: Write failing tests**
  - Edge table `follow` with `src_id`、`dst_id`、`rank` mapping converts to `MATCH (src)-[follow:follow]->(dst)` and src/dst predicates.
  - Property filter、order by、limit offset are rendered in stable clause order.

- [x] **Step 2: Verify RED**
  - Run focused Maven test.
  - Expected: unsupported edge/filter/order/pagination failures.

- [x] **Step 3: Implement minimal conversion**
  - Add expression renderer for comparison、AND/OR、IN、IS NULL、property references、literal values。
  - Add order and pagination rendering.

- [x] **Step 4: Verify GREEN**
  - Run focused Maven test.

- [x] **Step 5: Commit**
  - `git add ... && git commit -m "feat: convert nebula edge queries and clauses"`

### Task 3: Complex Query Whitelist、错误模型与执行适配

**Files:**
- Modify: query package
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/sql/query/NebulaSqlQueryExecutionAdapterTest.java`

**Interfaces:**
- Produces: stable error codes including query-only, missing mapping, unsupported join/subquery/union/function.
- Produces: `NebulaSqlQueryExecutionAdapter.executeQuery(NebulaConnection, NgqlQueryPlan)`。

- [x] **Step 1: Write failing tests**
  - Missing table/column mapping reports stable error code.
  - INSERT/UPDATE/DDL statement kind is rejected with query-only error.
  - Unsupported JOIN/subquery/union/function reports stable unsupported error.
  - Execution adapter passes `plan.getNgql()` into `NebulaConnection.execute` and wraps failed native result as `SQLException`.

- [x] **Step 2: Verify RED**
  - Run query package tests.

- [x] **Step 3: Implement errors and execution adapter**
  - Add `NebulaQueryConversionException` extending `SQLException` and `NebulaQueryErrorCode` enum.
  - Add unsupported flags to query statement and fail closed.
  - Add execution adapter without changing `DirectNebulaSqlExecutor` native nGQL path.

- [x] **Step 4: Verify GREEN**
  - Run query package tests.

- [x] **Step 5: Commit**
  - `git add ... && git commit -m "feat: add nebula query conversion errors and execution adapter"`

### Task 4: Golden/Negative 覆盖、任务勾选与 Build 验证

**Files:**
- Modify: `openspec/changes/add-nebula-sql-to-ngql-query-support/tasks.md`
- Test: query package tests

**Interfaces:**
- Consumes: all previous tasks.
- Produces: build evidence and updated task checklist.

- [x] **Step 1: Add golden and negative tests**
  - Golden: tag point query、edge query、filter/order/limit trace.
  - Negative: query-only boundary、missing mapping、unsupported complex structures。

- [x] **Step 2: Run focused tests**
  - `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=com.vesoft.nebula.jdbc.sql.query.*Test test`

- [x] **Step 3: Run module tests**
  - `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc test`

- [x] **Step 4: Update Comet tasks**
  - Check completed tasks in `openspec/changes/add-nebula-sql-to-ngql-query-support/tasks.md` only for this change.
  - Do not edit `add-nebula-sql-parser-core` state or tasks.

- [ ] **Step 5: Commit and guard**
  - `git add ... && git commit -m "test: cover nebula sql to ngql query conversion"`
  - Run `comet guard add-nebula-sql-to-ngql-query-support build --apply` if review gate and build evidence permit.

## Integration Dependency Notes

- `add-nebula-sql-parser-core` must later provide `NebulaSqlParseResult` / `NebulaSelectStatement` or equivalent stable IR. This plan keeps converter input isolated so a parser adapter can map those parser classes into `NebulaQueryStatement`.
- Until parser core is implemented, this change cannot honestly claim end-to-end SQL text parsing. It can complete converter core, schema mapping, nGQL rendering, diagnostics, and execution handoff.
