# Nebula MySQL Routine 子集 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Nebula JDBC/MySQL 兼容层中实现有限 routine 子集的元数据保存、删除、CALL 解释执行骨架和明确不支持边界。

**Architecture:** 先合入已完成的 SQL parser 与 SQL write/DDL 本地成果，routine 模块基于 JDBC 层现有 `NebulaSqlExecutor` 执行端口实现，不改变原 nGQL 直连路径。新增 routine parser/metadata/interpreter/service 位于 `com.vesoft.nebula.jdbc.sql.routine`，`NebulaStatementImpl` 只在入口处路由 `CREATE PROCEDURE`、`DROP PROCEDURE`、`CALL` 和明确不支持的 routine 语句。

**Tech Stack:** Java 1.8、JUnit 4、Maven Wrapper、Spring Boot 2.7.18 依赖管理、Druid SQL parser（来自 SQL parser core 本地成果）。

## Global Constraints

- 全程使用 UTF-8；Java 代码保持 JDK 1.8，不使用 Java 9+ API。
- 不修改 `org.ssssssss` Magic-API 依赖版本。
- 不合并 dev、不 push、不 archive。
- 每个 OpenSpec task 完成后勾选 `openspec/changes/add-nebula-mysql-routine-subset/tasks.md` 并提交。
- routine 子集只支持 `CREATE PROCEDURE`、`DROP PROCEDURE`、`CALL` 基础路径；函数、`OUT`/`INOUT`、游标、handler、动态 SQL、临时表、事务控制、循环、递归必须明确拒绝。
- 过程体 SQL 复用现有 SQL execution port；routine 解释器不得字符串拼接生成动态 SQL。

---

## File Structure

- `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/routine/`：新增 routine 语句解析、元数据、校验、解释执行和服务类。
- `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaStatementImpl.java`：接入 routine service 路由，不影响普通 query/update。
- `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/sql/routine/`：新增 parser/repository/interpreter/service 测试。
- `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/statement/`：补充 statement routine 路由测试。
- `openspec/changes/add-nebula-mysql-routine-subset/tasks.md`：按完成项勾选。

### Task 0: 合入 SQL parser/write/DDL 本地成果

**Files:**
- Merge from: `codex/comet-add-nebula-sql-write-and-ddl-support`
- Do not merge: `codex/comet-add-nebula-mysql-protocol-write-and-advanced-support` unless it contains stable code beyond design-only artifacts.

**Interfaces:**
- Consumes: 当前 routine design 分支。
- Produces: `NebulaSqlExecutor` 可继续作为 routine body SQL execution port；SQL parser/write 支持类可供后续 routine 识别复用。

- [x] **Step 1: Merge local write/DDL branch**

Run:

```bash
git merge --no-ff codex/comet-add-nebula-sql-write-and-ddl-support -m "merge: bring nebula sql write ddl support"
```

Expected: merge succeeds or only Comet artifact conflicts that can be resolved by preserving current routine change state.

- [x] **Step 2: Verify build baseline**

Run:

```bash
cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc test
```

Expected: existing module tests pass. If this fails, load `systematic-debugging` before changing code.

### Task 1: Routine parser and unsupported boundary

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/routine/RoutineStatement.java`
- Create: `.../RoutineStatementParser.java`
- Create: `.../RoutineException.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/sql/routine/RoutineStatementParserTest.java`

**Interfaces:**
- Produces: `RoutineStatementParser.parse(String sql)` returning `RoutineStatement`; `RoutineException` with stable SQLState/errorCode.

- [x] **Step 1: Write failing parser tests**

Cover `CREATE PROCEDURE p(IN id VARCHAR(64)) BEGIN SELECT * FROM person WHERE id = id; END`, `DROP PROCEDURE IF EXISTS p`, `CALL p('v1')`, plus rejection of `CREATE FUNCTION`, `OUT`, `INOUT`, `CURSOR`, `HANDLER`, `PREPARE`, `START TRANSACTION`, `WHILE`, `LOOP`.

- [x] **Step 2: Run parser tests and verify RED**

Run:

```bash
cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=RoutineStatementParserTest test
```

Expected: compilation/test failure because routine classes do not exist.

- [x] **Step 3: Implement minimal parser**

Use conservative string scanning for first routine subset only. Preserve original body SQL statements as strings, split semicolon-delimited body statements only after extracting `BEGIN ... END`, and reject unsupported keywords before saving.

- [x] **Step 4: Verify GREEN and commit**

Run the same parser test command. Then commit:

```bash
git add fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/sql/routine fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/sql/routine/RoutineStatementParserTest.java openspec/changes/add-nebula-mysql-routine-subset/tasks.md
git commit -m "feat(nebula): define mysql routine subset parser"
```

### Task 2: Routine metadata repository and create/drop service

**Files:**
- Create: `.../RoutineName.java`
- Create: `.../RoutineDefinition.java`
- Create: `.../RoutineParameter.java`
- Create: `.../RoutineMetadataRepository.java`
- Create: `.../InMemoryRoutineMetadataRepository.java`
- Create: `.../RoutineService.java`
- Test: `.../RoutineMetadataRepositoryTest.java`
- Test: `.../RoutineServiceTest.java`

**Interfaces:**
- Consumes: `RoutineStatementParser.parse`.
- Produces: `RoutineService.create(String schema, String user, String sql)`, `drop(String schema, String sql)`, `call(String schema, String user, String sql, NebulaConnection connection, NebulaSqlExecutor executor)`.

- [x] **Step 1: Write failing repository/service tests**

Cover save/find/drop, duplicate create error 1304, missing drop error 1305, `DROP PROCEDURE IF EXISTS` OK, schema isolation, lowercase normalization.

- [x] **Step 2: Run tests and verify RED**

Run:

```bash
cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=RoutineMetadataRepositoryTest,RoutineServiceTest test
```

Expected: failure because metadata/service classes are absent or incomplete.

- [x] **Step 3: Implement minimal metadata and service**

Use static default in-memory repository for JDBC statement routing and instance repository for tests. Enforce duplicate/missing behavior and unsupported syntax validation before save.

- [x] **Step 4: Verify GREEN and commit**

Run the same tests. Then commit with message `feat(nebula): persist mysql routine definitions`.

### Task 3: CALL interpreter skeleton

**Files:**
- Create: `.../RoutineExecutionContext.java`
- Create: `.../RoutineInterpreter.java`
- Create: `.../RoutineCallResult.java`
- Test: `.../RoutineInterpreterTest.java`
- Modify: `.../RoutineService.java`

**Interfaces:**
- Consumes: `RoutineDefinition`, `NebulaSqlExecutor`.
- Produces: sequential execution of body SQL, last resultset return, OK summary for updates-only body, argument count validation, statement budget rejection, direct recursive `CALL` rejection.

- [ ] **Step 1: Write failing interpreter tests**

Cover argument binding count, sequential SQL order, last resultset returned, no-resultset OK, first SQL failure short-circuits, statement limit, body `CALL` rejected.

- [ ] **Step 2: Run tests and verify RED**

Run:

```bash
cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=RoutineInterpreterTest,RoutineServiceTest test
```

Expected: failure because CALL execution is not implemented.

- [ ] **Step 3: Implement minimal interpreter**

Classify each body SQL through existing query/update path: call `executeQuery` for SELECT-like statements, otherwise `executeUpdate`. Store `currentResultSet` only for last query.

- [ ] **Step 4: Verify GREEN and commit**

Run the same tests. Then commit with message `feat(nebula): execute mysql routine call skeleton`.

### Task 4: Statement route and metadata exposure tests

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaStatementImpl.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaDatabaseMetaData.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/statement/NebulaStatementRoutineTest.java`

**Interfaces:**
- Consumes: static/default `RoutineService`.
- Produces: `Statement.execute("CREATE PROCEDURE...")` returns `false`, `CALL` returns `true` when resultset exists and `false` for update-only, `DROP` returns `false`; `supportsStoredProcedures()` returns true when routine subset is available.

- [ ] **Step 1: Write failing statement route tests**

Cover create/call/drop through `NebulaStatementImpl` with recording executor and verify ordinary SELECT still goes through original path.

- [ ] **Step 2: Run tests and verify RED**

Run:

```bash
cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaStatementRoutineTest,NebulaStatementExecutorPathTest test
```

Expected: failure because statement routing is not connected.

- [ ] **Step 3: Implement minimal routing**

In `execute`, route routine statements before `isQuery`; in `executeQuery`, reject create/drop and allow CALL only if resultset exists; in `executeUpdate`, allow create/drop and update-only CALL. Keep `checkReadOnly` and `checkUpdate` unchanged for nGQL compatibility.

- [ ] **Step 4: Verify GREEN and commit**

Run the same tests and commit with message `feat(nebula): route mysql routine statements`.

### Task 5: Final verification and task closure

**Files:**
- Modify: `openspec/changes/add-nebula-mysql-routine-subset/tasks.md`
- Optional: `openspec/changes/add-nebula-mysql-routine-subset/specs/nebula-mysql-routine-subset/spec.md` only if implementation reveals a small missing acceptance boundary.

- [ ] **Step 1: Run focused module tests**

Run:

```bash
cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc test
```

- [ ] **Step 2: Run Comet build check**

Run:

```bash
comet state check add-nebula-mysql-routine-subset build --recover
```

- [ ] **Step 3: Do not run build guard if tasks are intentionally left incomplete**

Only run `comet guard add-nebula-mysql-routine-subset build --apply` if all tasks are checked, review gate is satisfied, and the user has not limited this worker to partial build.
