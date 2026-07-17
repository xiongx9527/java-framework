---
change: add-nebula-mysql-protocol-gateway-readonly
design-doc: docs/superpowers/specs/2026-07-17-nebula-mysql-protocol-gateway-readonly-design.md
base-ref: ce556fc260050b85ced425180f850a7fc9299256
---

# Nebula MySQL Protocol Gateway Readonly Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增 Java 8 兼容的 Nebula MySQL wire protocol 只读网关模块，支持基础握手、认证、COM_QUERY、只读拒绝、metadata 响应和 MySQL 文本协议结果集。

**Architecture:** 在 `fw-plus-nebula` 下新增 `fw-plus-nebula-mysql-gateway` 子模块，协议层、只读查询层、metadata 层和执行端口分离。真实 SQL-to-nGQL/Nebula 执行通过 `NebulaSqlExecutionPort` 端口接入，当前任务先用 fake port 完成协议和网关行为测试，等待 parser/ngql 上游稳定接口后补真实 adapter。

**Tech Stack:** Java 8、JUnit 4、Maven Wrapper、MySQL protocol v10 最小文本协议、`fw-plus-nebula-jdbc` 可选集成边界。

## Global Constraints

- Core 层严格 Java 1.8，不使用 Java 9+ API。
- 使用 `fw-plus-core/mvnw` 构建，不使用系统 `mvn`。
- 不修改 Magic-API 版本或内部 Nexus 依赖版本。
- 不改变现有 `jdbc:nebula://...` nGQL 直连路径。
- 网关不依赖 Druid AST、converter 私有模型或 raw SQL fallback。
- 本 change 不实现写入、DDL、事务、prepared statement、LOAD DATA、binlog、复制、routine、trigger 或完整权限体系。

---

## File Structure

- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/pom.xml`
  - 注册新子模块。
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway/pom.xml`
  - 新模块 POM，仅依赖 JUnit/Mockito 测试依赖，先不引入第三方协议库。
- Create: `.../src/main/java/com/vesoft/nebula/jdbc/mysql/protocol/*`
  - MySQL packet、capability、command、OK/ERR/EOF、handshake、text resultset 编解码。
- Create: `.../src/main/java/com/vesoft/nebula/jdbc/mysql/auth/*`
  - 配置型认证服务与 `mysql_native_password` scramble 校验。
- Create: `.../src/main/java/com/vesoft/nebula/jdbc/mysql/session/*`
  - session context、connection id、database、capability、sequence 管理。
- Create: `.../src/main/java/com/vesoft/nebula/jdbc/mysql/query/*`
  - `ReadonlyQueryHandler`、`NebulaSqlExecutionPort`、`MysqlMetadataHandler`、`MysqlQueryResult`。
- Create: `.../src/main/java/com/vesoft/nebula/jdbc/mysql/command/*`
  - 命令白名单、COM_QUERY/COM_PING/COM_QUIT/COM_INIT_DB 分发和非目标命令错误。
- Create: `.../src/main/java/com/vesoft/nebula/jdbc/mysql/server/*`
  - 最小 socket server 生命周期封装，供后续 starter 或应用嵌入。
- Create: `.../src/test/java/com/vesoft/nebula/jdbc/mysql/**/*Test.java`
  - 覆盖握手、认证、命令、只读拒绝、metadata、结果集和错误映射。

## Task 1: 模块边界与基础模型

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/pom.xml`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway/pom.xml`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway/src/main/java/com/vesoft/nebula/jdbc/mysql/query/MysqlColumn.java`
- Create: `.../MysqlQueryResult.java`
- Create: `.../MysqlGatewaySessionContext.java`
- Create: `.../NebulaSqlExecutionPort.java`

**Interfaces:**
- Produces: `NebulaSqlExecutionPort.executeQuery(MysqlGatewaySessionContext context, String sql) throws SQLException`
- Produces: immutable-style result objects backed by Java 8 collections.

- [ ] Add module POM and parent module entry.
- [ ] Add query result and session context model classes with Java 8 constructors/getters.
- [ ] Add unit tests for result model null row/value handling.
- [ ] Run: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway test`
- [ ] Commit: `feat: add nebula mysql gateway module`

## Task 2: MySQL packet 编解码与结果包

**Files:**
- Create: `.../protocol/MysqlPacket.java`
- Create: `.../protocol/MysqlPacketReader.java`
- Create: `.../protocol/MysqlPacketWriter.java`
- Create: `.../protocol/MysqlCapability.java`
- Create: `.../protocol/MysqlCommand.java`
- Create: `.../protocol/MysqlOkPacket.java`
- Create: `.../protocol/MysqlErrPacket.java`
- Create: `.../protocol/MysqlEofPacket.java`
- Create tests under `.../protocol/`

**Interfaces:**
- Consumes: `MysqlQueryResult`
- Produces: packet payload readers/writers for later handshake and resultset encoders.

- [ ] Write packet reader/writer tests for length-encoded integers, strings, packet headers and sequence id.
- [ ] Implement Java 8 packet primitives without Netty dependency.
- [ ] Add OK/ERR/EOF packet tests with fixed MySQL codes and SQLState.
- [ ] Run module tests.
- [ ] Commit: `feat: add mysql protocol packet codecs`

## Task 3: 握手、认证与 session 生命周期

**Files:**
- Create: `.../session/MysqlProtocolSession.java`
- Create: `.../auth/MysqlGatewayUser.java`
- Create: `.../auth/MysqlGatewayAuthenticator.java`
- Create: `.../auth/StaticMysqlGatewayAuthenticator.java`
- Create: `.../auth/MysqlNativePassword.java`
- Create: `.../protocol/MysqlHandshakePacket.java`
- Create: `.../protocol/MysqlHandshakeResponse.java`
- Create: `.../protocol/MysqlHandshakeHandler.java`
- Create tests under `.../auth/` and `.../protocol/`

**Interfaces:**
- Produces: authenticated `MysqlProtocolSession` with username/database/capability.

- [ ] Write failing tests for initial handshake fields, auth plugin, capability flags and sequence id.
- [ ] Write failing tests for valid and invalid `mysql_native_password` scramble.
- [ ] Implement static authenticator with allowed default database checks.
- [ ] Implement handshake response parser for protocol 41 secure connection path.
- [ ] Run module tests.
- [ ] Commit: `feat: implement mysql handshake authentication`

## Task 4: 命令分发、只读门禁与错误映射

**Files:**
- Create: `.../command/MysqlCommandDispatcher.java`
- Create: `.../query/ReadonlyQueryHandler.java`
- Create: `.../query/ReadonlySqlGuard.java`
- Create: `.../query/MysqlErrorMapper.java`
- Create tests under `.../command/` and `.../query/`

**Interfaces:**
- Consumes: `NebulaSqlExecutionPort`
- Produces: ERR packet for unsupported commands and read-only rejection.

- [ ] Test COM_QUERY forwards SELECT/JOIN/UNION/GROUP BY strings to fake executor unchanged.
- [ ] Test INSERT/UPDATE/DELETE/DDL/transaction/CALL/TRIGGER/binlog/replication are rejected before executor call.
- [ ] Test COM_PING returns OK, COM_QUIT closes, unsupported commands return 1235.
- [ ] Implement guard with comment/whitespace stripping and first-keyword classification only.
- [ ] Run module tests.
- [ ] Commit: `feat: add readonly mysql command dispatcher`

## Task 5: Metadata 与文本结果集编码

**Files:**
- Create: `.../query/MysqlMetadataHandler.java`
- Create: `.../query/StaticMysqlMetadataHandler.java`
- Create: `.../protocol/MysqlTextResultSetEncoder.java`
- Create tests under `.../query/` and `.../protocol/`

**Interfaces:**
- Consumes: `MysqlQueryResult`
- Produces: MySQL text protocol column definitions and row packets.

- [ ] Test SHOW DATABASES, SHOW TABLES, DESCRIBE/DESC happy paths and table-not-found error.
- [ ] Test text resultsets for empty rows, null, string, integer, long, double, boolean, date/timestamp and object fallback.
- [ ] Implement conservative MySQL type mapping and NULL row marker.
- [ ] Run module tests.
- [ ] Commit: `feat: encode mysql text resultsets`

## Task 6: 最小 server 封装、文档证据与 Comet 收尾

**Files:**
- Create: `.../server/NebulaMysqlGatewayServer.java`
- Create: `.../server/NebulaMysqlGatewayServerConfig.java`
- Create: `.../server/NebulaMysqlGatewayConnectionHandler.java`
- Modify: `openspec/changes/add-nebula-mysql-protocol-gateway-readonly/tasks.md`
- Modify or create: `docs/superpowers/reports/2026-07-17-nebula-mysql-protocol-gateway-readonly-build.md`

**Interfaces:**
- Consumes: authenticator, metadata handler, execution port and dispatcher.
- Produces: embeddable start/stop lifecycle for future Boot starter.

- [ ] Implement simple Java socket accept loop with clean `start()`/`stop()` lifecycle and per-connection handler.
- [ ] Add lifecycle unit tests using fake socket streams where practical; document that MySQL CLI/GUI acceptance requires manual environment with a running port and stable upstream SQL executor.
- [ ] Update `tasks.md` checkboxes for completed independent scope; leave GUI/client items unchecked if no real client/Nebula environment exists.
- [ ] Run: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway test`
- [ ] Run: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-nebula test -DskipITs`
- [ ] Request standard code review before guard.
- [ ] Commit: `feat: add readonly nebula mysql gateway`
