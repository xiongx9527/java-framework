# Nebula MySQL Trigger 子集 Implementation Plan

---
change: add-nebula-mysql-trigger-subset
design-doc: docs/superpowers/specs/2026-07-17-nebula-mysql-trigger-subset-design.md
base-ref: 874719db4c0b576fc1cceb7e2be0c05d4816fcc7
---

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Nebula MySQL 网关写入链路提供可测试的 trigger 元数据、DDL 子集、BEFORE/AFTER 执行 hook、递归保护和边界测试。

**Architecture:** 在 `com.vesoft.nebula.jdbc.sql.trigger` 下新增独立 trigger 子集组件，不修改 nGQL JDBC 直连执行语义。Trigger 执行通过 `NebulaSqlMutationExecutor` 装饰器包裹网关写入端口，并通过接口接入 body SQL 转换执行能力。

**Tech Stack:** Java 1.8, JUnit 4, Maven Wrapper, existing Nebula SQL parser/write/query packages.

## Global Constraints

- 只对经 MySQL 网关执行且转换为 `NebulaMutationPlan` 的 INSERT、UPDATE、DELETE 生效。
- 不捕获绕过网关的 `NebulaConnection.execute(nGQL)` 或现有 JDBC 直连写入。
- 不实现完整 MySQL trigger、事务、binlog、复制、routine、CALL、游标、handler、动态 SQL 或复杂 compound statement。
- BEFORE trigger 失败时主写入不得执行；主写入失败时 AFTER trigger 不得执行。
- AFTER trigger 失败可向客户端返回错误，但不承诺回滚已成功主写入。
- 递归触发必须有最大深度限制，默认最大深度为 8。
- fw-plus-core 保持 Java 1.8，不使用 Java 9+ API。

## File Structure

- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/trigger/TriggerEvent.java`
- Create: `.../trigger/TriggerTiming.java`
- Create: `.../trigger/TriggerDefinition.java`
- Create: `.../trigger/TriggerDefinitionDraft.java`
- Create: `.../trigger/TriggerMetadataStore.java`
- Create: `.../trigger/InMemoryTriggerMetadataStore.java`
- Create: `.../trigger/TriggerMetadataService.java`
- Create: `.../trigger/TriggerDdlHandler.java`
- Create: `.../trigger/TriggerExecutionContext.java`
- Create: `.../trigger/TriggerBodyExecutor.java`
- Create: `.../trigger/TriggerAwareMutationExecutor.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/sql/trigger/TriggerMetadataServiceTest.java`
- Test: `.../trigger/TriggerDdlHandlerTest.java`
- Test: `.../trigger/TriggerAwareMutationExecutorTest.java`

### Task 1: Trigger 语法边界与元数据模型

**Maps to OpenSpec:** 1.1

**Files:**
- Create enums and model classes in `.../sql/trigger/`
- Test: `TriggerMetadataServiceTest`

**Interfaces:**
- Produces: `TriggerDefinition`, `TriggerDefinitionDraft`, `TriggerEvent`, `TriggerTiming`
- Produces: immutable metadata fields `schemaName`, `triggerName`, `targetTable`, `event`, `timing`, `bodySql`, `order`, `enabled`

- [x] Step 1: Write tests for immutable metadata and event/timing mapping.
- [x] Step 2: Run targeted test and verify it fails because trigger classes do not exist.
- [x] Step 3: Implement minimal model and enum classes.
- [x] Step 4: Run targeted test and verify it passes.
- [x] Step 5: Check OpenSpec task `1.1` and commit with `feat(nebula): define trigger metadata model`.

### Task 2: Trigger 元数据存储、排序、失败策略与递归限制字段

**Maps to OpenSpec:** 1.2

**Files:**
- Create `TriggerMetadataStore`, `InMemoryTriggerMetadataStore`, `TriggerMetadataService`, `TriggerExecutionContext`
- Test: `TriggerMetadataServiceTest`

**Interfaces:**
- Produces: `create(TriggerDefinitionDraft)`, `drop(schemaName, triggerName, ifExists)`, `findEnabled(schemaName, targetTable, event, timing)`
- Produces: `TriggerExecutionContext.root(maxDepth)` and `enter(TriggerDefinition)`

- [ ] Step 1: Write tests for duplicate create, drop missing, drop-if-exists, stable ordering, disabled filtering, max-depth rejection.
- [ ] Step 2: Run targeted test and verify the new assertions fail.
- [ ] Step 3: Implement store/service/context.
- [ ] Step 4: Run targeted test and verify it passes.
- [ ] Step 5: Check OpenSpec task `1.2` and commit with `feat(nebula): add trigger metadata service`.

### Task 3: CREATE/DROP TRIGGER DDL handler

**Maps to OpenSpec:** 2.1

**Files:**
- Create `TriggerDdlHandler`
- Test: `TriggerDdlHandlerTest`

**Interfaces:**
- Produces: `boolean supports(String sql)`
- Produces: `TriggerDdlResult handle(String schemaName, String sql) throws SQLException`
- Consumes: `TriggerMetadataService`

- [ ] Step 1: Write tests for supported CREATE TRIGGER, DROP TRIGGER, DROP TRIGGER IF EXISTS and unsupported syntax.
- [ ] Step 2: Run targeted test and verify it fails.
- [ ] Step 3: Implement a conservative parser for the supported subset: single body SQL after `FOR EACH ROW`, no `BEGIN ... END`, `CALL`, transaction SQL, dynamic SQL, or nested trigger DDL in body.
- [ ] Step 4: Run targeted test and verify it passes.
- [ ] Step 5: Check OpenSpec task `2.1` and commit with `feat(nebula): handle trigger ddl metadata`.

### Task 4: BEFORE/AFTER write execution hook

**Maps to OpenSpec:** 2.2 and 3.1

**Files:**
- Create `TriggerBodyExecutor`, `TriggerAwareMutationExecutor`
- Test: `TriggerAwareMutationExecutorTest`

**Interfaces:**
- Consumes: `NebulaSqlMutationExecutor.executeMutation(NebulaMutationPlan)`
- Produces: `TriggerAwareMutationExecutor.executeMutation(NebulaMutationPlan)` ordering: BEFORE bodies, primary write, AFTER bodies

- [ ] Step 1: Write tests for INSERT, UPDATE and DELETE trigger ordering using fake executors.
- [ ] Step 2: Run targeted test and verify it fails.
- [ ] Step 3: Implement event mapping from `NebulaMutationKind` and trigger-aware executor.
- [ ] Step 4: Run targeted test and verify it passes.
- [ ] Step 5: Check OpenSpec tasks `2.2` and `3.1` if INSERT/UPDATE/DELETE paths are all covered, then commit with `feat(nebula): execute trigger hooks around writes`.

### Task 5: 递归保护和不支持 body 语法

**Maps to OpenSpec:** 2.3 and 3.2

**Files:**
- Modify `TriggerExecutionContext`, `TriggerDdlHandler`, `TriggerAwareMutationExecutor`
- Test: `TriggerDdlHandlerTest`, `TriggerAwareMutationExecutorTest`

**Interfaces:**
- Produces: explicit SQLException messages for recursion depth and unsupported body syntax

- [ ] Step 1: Write tests for self-recursive trigger, cross-trigger recursion, body `CALL`, body transaction SQL and nested CREATE/DROP TRIGGER rejection.
- [ ] Step 2: Run targeted tests and verify they fail.
- [ ] Step 3: Implement recursive context propagation and unsupported body validation.
- [ ] Step 4: Run targeted tests and verify they pass.
- [ ] Step 5: Check OpenSpec tasks `2.3` and `3.2` if boundary tests pass, then commit with `feat(nebula): guard trigger recursion and unsupported bodies`.

### Task 6: 文档边界和 gateway-only 接入说明

**Maps to OpenSpec:** 3.2

**Files:**
- Create or modify `fw-plus-core/.../src/main/resources/META-INF/fw-plus-nebula-sql-trigger.md`
- Modify delta spec only if implementation reveals a small missing scenario.

- [ ] Step 1: Document supported CREATE/DROP syntax, BEFORE/AFTER ordering, failure policy, recursion depth and gateway-only boundary.
- [ ] Step 2: Verify docs contain no full MySQL compatibility claim.
- [ ] Step 3: Commit with `docs(nebula): document trigger subset boundaries`.

### Task 7: Build verification and final build bookkeeping

**Maps to OpenSpec:** 3.1 and 3.2

**Files:**
- Modify `openspec/changes/add-nebula-mysql-trigger-subset/tasks.md`
- Possibly update `.comet` state through CLI only.

- [ ] Step 1: Run targeted module tests with Maven Wrapper.
- [ ] Step 2: Run relevant wider module test if time permits.
- [ ] Step 3: Record build evidence with `comet state record-check` if guard does not auto-detect the command.
- [ ] Step 4: If all OpenSpec tasks are checked and review gate is satisfied, run `comet guard add-nebula-mysql-trigger-subset build --apply`; otherwise leave phase in build and report remaining tasks.
