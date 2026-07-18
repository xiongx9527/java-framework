---
change: add-nebula-mysql-binlog-compatible-stream
design-doc: docs/superpowers/specs/2026-07-17-nebula-mysql-binlog-compatible-stream-design.md
base-ref: 65db9422f8918b205df81b8e462e4b683a2741c2
---

# Nebula MySQL Binlog Compatible Stream Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Nebula JDBC 的 MySQL 兼容写入路径提供网关侧 binlog-compatible event stream。

**Architecture:** 在 `com.vesoft.nebula.jdbc.sql.binlog` 下新增稳定事件模型、position、内存事件存储和 reader。写入/DDL 执行成功后由 descriptor 工厂生成事件并追加到 store；原生 nGQL fallback 和失败执行不生成事件。

**Tech Stack:** Java 1.8、JUnit 4、Mockito、现有 Nebula JDBC SQL write/DDL descriptor。

## Global Constraints

- Core 模块保持 JDK 1.8，不使用 Java 9+ API。
- 不实现真实 MySQL binlog packet、GTID、row image、statement/mixed mode。
- 只捕获经 MySQL 网关成功执行的 INSERT、UPDATE、DELETE 和受支持 DDL。
- 绕过网关或原生 nGQL fallback 不产生 binlog-compatible 事件。
- 事件追加失败默认阻止写入对外成功。

### Task 1: Event Model, Position, Store And Reader

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/binlog/BinlogPosition.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/binlog/NebulaBinlogEventType.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/binlog/NebulaBinlogEvent.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/binlog/NebulaBinlogEventDraft.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/binlog/BinlogEventBatch.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/binlog/BinlogEventStore.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/binlog/BinlogEventReader.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/binlog/InMemoryBinlogEventStore.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/sql/binlog/InMemoryBinlogEventStoreTest.java`

**Interfaces:**
- Produces: `BinlogEventStore.append(NebulaBinlogEventDraft)`, `BinlogEventStore.readAfter(BinlogPosition, int)`, `BinlogEventReader.readAfter(BinlogPosition, int, long)`.

- [ ] **Step 1: Write failing store/reader tests**

Cover monotonic positions, immutable payload maps, `readAfter(null, max)` from beginning, `readAfter(position, max)` after a known position, stale/wrong store position error, and wait polling after append.

- [ ] **Step 2: Run red test**

Run: `./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=InMemoryBinlogEventStoreTest test`
Expected: compilation failure because binlog model classes do not exist.

- [ ] **Step 3: Implement minimal event model/store/reader**

Use immutable defensive copies, `AtomicLong` sequence allocation, store id validation, and a synchronized monitor for simple wait polling.

- [ ] **Step 4: Run green test**

Run: `./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=InMemoryBinlogEventStoreTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

Commit message: `feat(nebula): add binlog event store model`

### Task 2: Descriptor Event Factory

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/binlog/NebulaBinlogEventFactory.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/sql/binlog/NebulaBinlogEventFactoryTest.java`

**Interfaces:**
- Consumes: `NebulaMutationDescriptor`, `NebulaDdlDescriptor`.
- Produces: `NebulaBinlogEventFactory.forMutation(...)`, `NebulaBinlogEventFactory.forDdl(...)`.

- [ ] **Step 1: Write failing factory tests**

Verify INSERT/UPDATE/DELETE map to `WRITE_ROWS`/`UPDATE_ROWS`/`DELETE_ROWS`, DDL maps to `SCHEMA_CHANGE`, payload contains descriptor kind, target/schema name, keys/properties/columns, `sourceSql`, `ngql`, `traceId`, and `beforeImageAvailable=false`.

- [ ] **Step 2: Run red test**

Run: `./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaBinlogEventFactoryTest test`
Expected: compilation failure because factory does not exist.

- [ ] **Step 3: Implement minimal factory**

Keep mapping deterministic and avoid reparsing SQL. Reject null descriptors with `IllegalArgumentException`.

- [ ] **Step 4: Run green test**

Run: `./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaBinlogEventFactoryTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

Commit message: `feat(nebula): map write ddl descriptors to binlog events`

### Task 3: Gateway Write/DDL Hook

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/DirectNebulaSqlExecutor.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/sql/DirectNebulaSqlExecutorTest.java`

**Interfaces:**
- Consumes: `NebulaBinlogEventStore`, `NebulaBinlogEventFactory`.
- Produces: constructor overload `DirectNebulaSqlExecutor(NebulaSchemaMappingRegistry, BinlogEventStore)`.

- [ ] **Step 1: Write failing hook tests**

Add tests proving successful SQL INSERT and DROP TABLE append events, native nGQL fallback appends nothing, native execution failure appends nothing, and append failure causes `SQLException` containing `binlog-compatible stream append failed`.

- [ ] **Step 2: Run red test**

Run: `./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=DirectNebulaSqlExecutorTest test`
Expected: failure because constructor/hook behavior is missing.

- [ ] **Step 3: Implement minimal hook**

Append only after `DirectNebulaSqlMutationExecutor` reports successful mutation/DDL result with a descriptor. Keep default constructor behavior unchanged by using null store as disabled stream.

- [ ] **Step 4: Run green test**

Run: `./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=DirectNebulaSqlExecutorTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

Commit message: `feat(nebula): append binlog events after gateway writes`

### Task 4: Boundary Tests And Task Closure

**Files:**
- Modify: `openspec/changes/add-nebula-mysql-binlog-compatible-stream/tasks.md`
- Test: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/sql/binlog/InMemoryBinlogEventStoreTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/sql/DirectNebulaSqlExecutorTest.java`

**Interfaces:**
- Consumes all earlier tasks.
- Produces checked OpenSpec task list for completed implementation.

- [ ] **Step 1: Add any missing boundary assertions**

Confirm empty reads are stable, wrong store positions fail clearly, and direct native executor/fallback does not synthesize binlog events.

- [ ] **Step 2: Run focused module tests**

Run: `./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=InMemoryBinlogEventStoreTest,NebulaBinlogEventFactoryTest,DirectNebulaSqlExecutorTest test`
Expected: PASS.

- [ ] **Step 3: Update OpenSpec tasks**

Check completed items in `openspec/changes/add-nebula-mysql-binlog-compatible-stream/tasks.md`.

- [ ] **Step 4: Run broader module verification**

Run: `./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc test`
Expected: PASS or report blocker with logs.

- [ ] **Step 5: Commit**

Commit message: `test(nebula): verify binlog stream boundaries`
