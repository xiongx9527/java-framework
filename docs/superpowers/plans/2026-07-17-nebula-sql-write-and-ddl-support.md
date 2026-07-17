---
change: add-nebula-sql-write-and-ddl-support
design-doc: docs/superpowers/specs/2026-07-17-nebula-sql-write-and-ddl-support-design.md
base-ref: 4f550f242396d103ba024356c23b2a1917638bd7
---

# Nebula SQL 写入与 DDL 支持实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Nebula JDBC SQL 层补齐 INSERT、UPDATE、DELETE 和基础 DDL 到 nGQL 的转换、统一执行端口、结构化 descriptor 与拒绝错误。

**Architecture:** 复用 parser core 的 `NebulaSqlStatement` IR 与 query adapter 的 `NebulaSchemaMappingRegistry`、`TableMapping`、`ColumnMapping`。新增 `sql.write` 包只负责写入/DDL 转换、计划、结果 descriptor 与执行端口，`DirectNebulaSqlExecutor` 负责把 SQL update/DDL 路由到 mutation executor，原始 nGQL 直连路径保持可用。

**Tech Stack:** Java 8、JUnit 4、Mockito、Druid SQL parser、Nebula JDBC module。

## Global Constraints

- 使用简体中文维护计划、状态说明和验证记录。
- Core 层保持 Java 1.8，不使用 Java 9+ API。
- 不依赖真实 NebulaGraph，单元测试使用 fake executor 或纯 converter 测试。
- 不实现事务、复杂 ALTER、索引、外键、MySQL 专有 DDL、`INSERT ... SELECT`、`ON DUPLICATE KEY UPDATE`。
- 每个 OpenSpec task 完成后勾选 `openspec/changes/add-nebula-sql-write-and-ddl-support/tasks.md` 并提交。

---

## File Structure

- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/pom.xml`，继承上游 parser/query adapter 依赖。
- Create/merge: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/parser/**`，上游 parser core。
- Create/merge: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/query/**`，上游 query mapping 与 query adapter。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/write/NebulaMutationKind.java`，写入操作枚举。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/write/NebulaDdlKind.java`，DDL 操作枚举。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/write/NebulaWriteErrorCode.java`，结构化错误码。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/write/NebulaWriteConversionException.java`，转换拒绝异常。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/write/NebulaWriteBindings.java`，SQL 参数和执行上下文。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/write/NebulaMutationPlan.java`，写入执行计划。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/write/NebulaDdlPlan.java`，DDL 执行计划。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/write/NebulaMutationDescriptor.java`，写入 descriptor。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/write/NebulaDdlDescriptor.java`，DDL descriptor。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/write/NebulaMutationResult.java`，统一执行结果。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/write/NebulaSqlWriteConverter.java`，写入 converter 接口。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/write/DefaultNebulaSqlWriteConverter.java`，INSERT/UPDATE/DELETE converter。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/write/NebulaSqlDdlConverter.java`，DDL converter 接口。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/write/DefaultNebulaSqlDdlConverter.java`，CREATE/DROP/DESCRIBE converter。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/write/NebulaSqlMutationExecutor.java`，执行端口。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/write/DirectNebulaSqlMutationExecutor.java`，调用 `NebulaConnection.executeNgql` 的默认实现。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/DirectNebulaSqlExecutor.java`，路由 SQL 写入/DDL 并保留原始 nGQL。
- Test: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/sql/write/NebulaSqlWriteConverterTest.java`。
- Test: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/sql/write/NebulaSqlDdlConverterTest.java`。
- Test: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/sql/write/NebulaSqlMutationExecutorTest.java`。
- Test: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/sql/DirectNebulaSqlExecutorTest.java`。

### Task 1: 写入语义设计

**Files:**
- Modify: `openspec/changes/add-nebula-sql-write-and-ddl-support/tasks.md`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/write/NebulaMutationKind.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/write/NebulaWriteErrorCode.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/write/NebulaWriteConversionException.java`

**Interfaces:**
- Consumes: `TableMapping`, `ColumnRole` from query adapter.
- Produces: stable mutation kinds and error codes for converter, executor and tests.

- [x] **Step 1: Merge upstream parser/query adapter**

Run: `git merge feature/20260717/add-nebula-sql-to-ngql-query-support`
Expected: parser/query classes become available under `com.vesoft.nebula.jdbc.sql.parser` and `com.vesoft.nebula.jdbc.sql.query`.

- [x] **Step 2: Add error/kind enums**

Implement `NebulaMutationKind` with `INSERT_VERTEX`, `INSERT_EDGE`, `UPDATE_VERTEX`, `UPDATE_EDGE`, `DELETE_VERTEX`, `DELETE_EDGE`.
Implement `NebulaWriteErrorCode` with `NEBULA_SQL_WRITE_UNSUPPORTED_STATEMENT`, `NEBULA_SQL_WRITE_MISSING_ID`, `NEBULA_SQL_WRITE_UNMAPPED_COLUMN`, `NEBULA_SQL_WRITE_UNSAFE_UPDATE`, `NEBULA_SQL_DDL_UNSUPPORTED_OPTION`, `NEBULA_SQL_TRANSACTION_UNSUPPORTED`.

- [x] **Step 3: Add checked conversion exception**

Implement `NebulaWriteConversionException extends SQLException` with fields `NebulaWriteErrorCode errorCode` and getters. Message format must include the enum name and a human-readable reason.

- [x] **Step 4: Verify compile target**

Run: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -DskipTests compile`
Expected: compile passes or reports only issues introduced by the upstream merge that must be fixed before continuing.

- [x] **Step 5: Mark and commit**

Check tasks `1.1` if enums/errors document write safety boundaries in code and tests. Commit message: `feat(nebula): define sql write semantics`.

### Task 2: DDL 语义设计

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/write/NebulaDdlKind.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/write/NebulaDdlDescriptor.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/write/NebulaDdlPlan.java`

**Interfaces:**
- Consumes: parser `NebulaSchemaStatement`, query `TableKind`.
- Produces: DDL plan contract for converter and mutation executor.

- [x] **Step 1: Add DDL kind and descriptors**

Implement `NebulaDdlKind` with `CREATE_TAG`, `CREATE_EDGE`, `DROP_TAG`, `DROP_EDGE`, `DESCRIBE_TAG`, `DESCRIBE_EDGE`.
Implement immutable `NebulaDdlDescriptor` fields: `kind`, `schemaName`, `columns`, `sourceSql`, `ngql`.

- [x] **Step 2: Add DDL plan**

Implement immutable `NebulaDdlPlan` fields: `kind`, `ngql`, `descriptor`, `warnings`, `trace`.

- [x] **Step 3: Add constructor validation**

Reject blank `ngql`, null `kind`, and null `descriptor` with `IllegalArgumentException`.

- [x] **Step 4: Verify compile target**

Run: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -DskipTests compile`
Expected: compile passes.

- [x] **Step 5: Mark and commit**

Check task `1.2`. Commit message: `feat(nebula): define sql ddl mapping contract`.

### Task 3: INSERT 转换与执行

**Files:**
- Create: `DefaultNebulaSqlWriteConverter.java`
- Create: `NebulaSqlWriteConverter.java`
- Create: `NebulaWriteBindings.java`
- Create: `NebulaMutationPlan.java`
- Create: `NebulaMutationDescriptor.java`
- Create: `NebulaMutationResult.java`
- Create: `NebulaSqlMutationExecutor.java`
- Create: `DirectNebulaSqlMutationExecutor.java`
- Test: `NebulaSqlWriteConverterTest.java`
- Test: `NebulaSqlMutationExecutorTest.java`

**Interfaces:**
- Consumes: `NebulaInsertStatement`, `NebulaSchemaMappingRegistry`, `TableMapping`, `ColumnRole`.
- Produces: INSERT vertex/edge nGQL plans and execution result descriptors.

- [x] **Step 1: Write failing INSERT converter tests**

Add tests for `INSERT INTO person(id, name) VALUES ('p1', 'Tom')` producing `INSERT VERTEX person(name) VALUES "p1":("Tom")`, and edge insert with `src`, `dst`, optional `rank`.

- [x] **Step 2: Implement mutation plan and descriptors**

Use immutable classes with defensive copies for keys, columns, warnings and trace. `NebulaMutationResult` must expose `success`, `affectedRows`, `mutationDescriptor`, `ddlDescriptor`, `warnings`, `traceId`.

- [x] **Step 3: Implement INSERT conversion**

Resolve table mapping by SQL table name. For tag mappings require `ColumnRole.ID`; for edge mappings require `ColumnRole.SRC` and `ColumnRole.DST`, rank optional. Reject unmapped property columns.

- [x] **Step 4: Implement mutation executor**

`DirectNebulaSqlMutationExecutor.executeMutation` executes `plan.getNgql()` and returns `affectedRows` using policy `1` for successful single plan execution. `executeDdl` mirrors the behavior for DDL.

- [x] **Step 5: Run tests**

Run: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaSqlWriteConverterTest,NebulaSqlMutationExecutorTest test`
Expected: tests pass.

- [x] **Step 6: Mark and commit**

Check task `2.1`. Commit message: `feat(nebula): convert sql inserts to mutation plans`.

### Task 4: UPDATE/DELETE 转换与执行

**Files:**
- Modify: `DefaultNebulaSqlWriteConverter.java`
- Test: `NebulaSqlWriteConverterTest.java`

**Interfaces:**
- Consumes: `NebulaUpdateStatement`, `NebulaDeleteStatement`, mapping roles.
- Produces: UPDATE/DELETE vertex/edge nGQL plans and unsafe write errors.

- [x] **Step 1: Write failing UPDATE/DELETE tests**

Add tests for `UPDATE person SET name = 'Jerry' WHERE id = 'p1'`, `DELETE FROM person WHERE id = 'p1'`, edge update/delete with `src` and `dst`, and unsafe no-WHERE rejection.

- [x] **Step 2: Implement UPDATE conversion**

Require WHERE predicate that identifies VID for tags or src/dst for edges. Convert mapped SET properties to Nebula `UPDATE VERTEX` or `UPDATE EDGE` statements.

- [x] **Step 3: Implement DELETE conversion**

Require WHERE predicate that identifies VID for tags or src/dst for edges. Convert to `DELETE VERTEX` or `DELETE EDGE`.

- [x] **Step 4: Run tests**

Run: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaSqlWriteConverterTest test`
Expected: tests pass.

- [x] **Step 5: Mark and commit**

Check task `2.2`. Commit message: `feat(nebula): convert sql updates and deletes`.

### Task 5: 基础 DDL 映射

**Files:**
- Create: `NebulaSqlDdlConverter.java`
- Create: `DefaultNebulaSqlDdlConverter.java`
- Test: `NebulaSqlDdlConverterTest.java`

**Interfaces:**
- Consumes: `NebulaSchemaStatement`, `NebulaMetadataStatement`, `NebulaSchemaMappingRegistry`.
- Produces: CREATE/DROP/DESCRIBE nGQL plans and DDL unsupported errors.

- [x] **Step 1: Write failing DDL tests**

Add tests for CREATE tag, CREATE edge, DROP tag/edge, DESCRIBE tag/edge, unsupported ALTER/index/foreign key errors.

- [x] **Step 2: Implement DDL conversion**

Use mapping registry to determine `TableKind.TAG` or `TableKind.EDGE`. Generate `CREATE TAG`, `CREATE EDGE`, `DROP TAG`, `DROP EDGE`, `DESCRIBE TAG`, `DESCRIBE EDGE`.

- [x] **Step 3: Reject unsupported options**

Throw `NebulaWriteConversionException` with `NEBULA_SQL_DDL_UNSUPPORTED_OPTION` for complex ALTER, indexes, constraints, engine/charset/collation options, and unsupported SQL statement kinds.

- [x] **Step 4: Run tests**

Run: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaSqlDdlConverterTest test`
Expected: tests pass.

- [x] **Step 5: Mark and commit**

Check task `2.3`. Commit message: `feat(nebula): map sql ddl to nebula schema plans`.

### Task 6: JDBC 执行路由和错误测试

**Files:**
- Modify: `DirectNebulaSqlExecutor.java`
- Test: `DirectNebulaSqlExecutorTest.java`
- Test: `NebulaSqlWriteConverterTest.java`
- Test: `NebulaSqlDdlConverterTest.java`

**Interfaces:**
- Consumes: parser, write converter, DDL converter and mutation executor.
- Produces: JDBC `executeUpdate` path for SQL write/DDL and explicit transaction errors.

- [x] **Step 1: Add failing executor route tests**

Test SQL INSERT/UPDATE/DELETE/CREATE/DROP executed through `DirectNebulaSqlExecutor.executeUpdate` calls converted nGQL. Test native nGQL update still passes through. Test BEGIN/COMMIT/ROLLBACK throw `NEBULA_SQL_TRANSACTION_UNSUPPORTED`.

- [x] **Step 2: Implement routing**

In `DirectNebulaSqlExecutor.executeUpdate`, parse SQL-like statements with `DruidNebulaSqlParser`; route write statements to `DefaultNebulaSqlWriteConverter`, schema/metadata statements to `DefaultNebulaSqlDdlConverter`, and unsupported transaction statements to explicit exception. Keep existing `NebulaStatementClassifier.checkUpdate` for raw nGQL fallback.

- [x] **Step 3: Run targeted tests**

Run: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=DirectNebulaSqlExecutorTest,NebulaSqlWriteConverterTest,NebulaSqlDdlConverterTest test`
Expected: tests pass.

- [x] **Step 4: Mark and commit**

Check task `3.2` after transaction and complex DDL tests pass. Commit message: `feat(nebula): route sql write ddl execution`.

### Task 7: 验证与收敛

**Files:**
- Modify: `openspec/changes/add-nebula-sql-write-and-ddl-support/tasks.md`
- Optional create: `docs/superpowers/reports/2026-07-17-add-nebula-sql-write-and-ddl-support-build.md`

**Interfaces:**
- Consumes: all implementation and tests.
- Produces: completed build task checklist and validation evidence.

- [x] **Step 1: Run full Nebula JDBC tests**

Run: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc test`
Expected: tests pass.

- [x] **Step 2: Run compile in module context**

Run: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -DskipTests compile`
Expected: compile passes.

- [x] **Step 3: Mark remaining verification task**

Check task `3.1` after converter and DDL tests exist and pass.

- [x] **Step 4: Commit validation artifacts**

Commit message: `test(nebula): verify sql write ddl support`.

- [ ] **Step 5: Apply build guard when all tasks are complete**

Run: `comet guard add-nebula-sql-write-and-ddl-support build --apply`
Expected: guard passes only after plan, isolation, build_mode, tdd_mode, review_mode and tasks are complete.
