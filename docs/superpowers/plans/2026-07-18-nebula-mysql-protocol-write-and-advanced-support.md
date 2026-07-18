---
change: add-nebula-mysql-protocol-write-and-advanced-support
design-doc: docs/superpowers/specs/2026-07-17-nebula-mysql-protocol-write-and-advanced-support-design.md
base-ref: 8351c1363299bea131187df5b5358927c0a4dbc8
---

# Nebula MySQL 协议写入与高级兼容实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在只读 MySQL 协议网关基础上接入受控写入/DDL、prepared statement 常见路径、安全策略和 metadata 兼容测试。

**Architecture:** 协议模块保持为 MySQL wire protocol 适配层，通过新增 advanced query handler 调用已有 SQL parser、query converter 与 write/DDL executor。权限、SSL 和兼容 profile 在 session/handler 入口统一判定，metadata 兼容由可测试的静态 handler 扩展。

**Tech Stack:** Java 8、JUnit 4、Maven Wrapper、Spring Boot 2.7.18 项目父依赖、已有 `fw-plus-nebula-jdbc` SQL parser/query/write API。

## Global Constraints

- Java 源码保持 Java 8 兼容，不能使用 Java 9+ API。
- 使用项目现有 Tab 缩进和 UTF-8 编码。
- 不修改 Magic-API 依赖版本。
- 不破坏现有 `jdbc:nebula://...` nGQL 直连路径。
- 不实现事务、binlog、复制协议、routine、trigger、LOAD DATA、完整 MySQL 用户系统。
- 每完成一个 OpenSpec task 后更新 `openspec/changes/add-nebula-mysql-protocol-write-and-advanced-support/tasks.md` 并提交。

---

### Task 1: 支持边界与客户端兼容矩阵

**Files:**
- Create: `docs/superpowers/reports/2026-07-18-nebula-mysql-protocol-write-and-advanced-support-build.md`
- Modify: `openspec/changes/add-nebula-mysql-protocol-write-and-advanced-support/tasks.md`

**Interfaces:**
- Consumes: design doc section 2, section 4, section 8.
- Produces: 持久化 build 记录，后续验证补充实际命令和客户端 fixture 结果。

- [x] 写入 report，明确 COM_QUERY 写入/DDL、prepared statement、权限、SSL、metadata、unsupported command 的支持边界。
- [x] 在 report 中记录 Java MySQL JDBC Driver、DBeaver、DataGrip/BI fixture 的兼容矩阵和不可自动验证项。
- [x] 勾选 tasks.md 的 1.1 和 1.2。
- [x] 提交：`docs+openspec: record nebula mysql advanced support boundaries`

### Task 2: COM_QUERY 写入与 DDL 接入

**Files:**
- Create/Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway/src/main/java/com/vesoft/nebula/jdbc/mysql/query/AdvancedQueryHandler.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway/src/main/java/com/vesoft/nebula/jdbc/mysql/command/MysqlCommandDispatcher.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway/pom.xml`
- Test: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway/src/test/java/com/vesoft/nebula/jdbc/mysql/query/AdvancedQueryHandlerTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway/src/test/java/com/vesoft/nebula/jdbc/mysql/command/MysqlCommandDispatcherTest.java`

**Interfaces:**
- Consumes: `NebulaSqlExecutionPort.executeQuery`, `NebulaSqlParser.parse`, `NebulaSqlWriteConverter.convert`, `NebulaSqlDdlConverter.convert`, `NebulaSqlMutationExecutor.executeMutation/executeDdl`.
- Produces: `AdvancedQueryHandler.handle(session, sql)` 返回 SELECT/metadata result set 或写入/DDL OK result。

- [x] 先写 `AdvancedQueryHandlerTest`，覆盖 INSERT 返回 affected rows、CREATE TABLE 返回 OK、事务语句返回不支持错误。
- [x] 运行单测确认失败：`./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway test -Dtest=AdvancedQueryHandlerTest,MysqlCommandDispatcherTest`
- [x] 实现 `AdvancedQueryHandler`，SELECT 和 metadata 复用只读逻辑，INSERT/UPDATE/DELETE 与 DDL 调用 mutation executor，错误通过 `MysqlErrorMapper` 映射。
- [x] 调整 dispatcher 构造器兼容旧测试并支持 advanced handler。
- [x] 运行单测通过。
- [x] 勾选 tasks.md 的 2.1。
- [x] 提交：`feat(nebula): route mysql com_query writes and ddl`

### Task 3: Prepared Statement 常见路径

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway/src/main/java/com/vesoft/nebula/jdbc/mysql/prepare/MysqlPreparedStatementRegistry.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway/src/main/java/com/vesoft/nebula/jdbc/mysql/prepare/MysqlPreparedStatement.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway/src/main/java/com/vesoft/nebula/jdbc/mysql/prepare/MysqlPreparedStatementHandler.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway/src/main/java/com/vesoft/nebula/jdbc/mysql/protocol/MysqlCommand.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway/src/main/java/com/vesoft/nebula/jdbc/mysql/command/MysqlCommandDispatcher.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway/src/test/java/com/vesoft/nebula/jdbc/mysql/prepare/MysqlPreparedStatementHandlerTest.java`

**Interfaces:**
- Consumes: `AdvancedQueryHandler.handle(session, renderedSql)`.
- Produces: prepare/execute/close/reset command support for common JDBC text payloads and deterministic errors for unsupported binary payloads.

- [x] 先写 tests，覆盖 prepare 分配 statement id、execute SELECT 参数绑定、execute INSERT 参数绑定、close/reset 清理、参数数量不匹配。
- [x] 运行 prepared 单测确认失败。
- [x] 实现 registry、statement 模型、文本安全参数绑定和 dispatcher 命令分发。
- [x] 对完整 binary protocol 未覆盖路径返回 MySQL 1235/1210 错误，不断连。
- [x] 运行 prepared 与 dispatcher 单测通过。
- [x] 勾选 tasks.md 的 2.2。
- [x] 提交：`feat(nebula): add mysql prepared statement common path`

### Task 4: 安全、权限与 SSL 策略

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway/src/main/java/com/vesoft/nebula/jdbc/mysql/security/MysqlGatewaySecurityPolicy.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway/src/main/java/com/vesoft/nebula/jdbc/mysql/security/MysqlGatewayOperation.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway/src/main/java/com/vesoft/nebula/jdbc/mysql/security/MysqlSslMode.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway/src/main/java/com/vesoft/nebula/jdbc/mysql/server/NebulaMysqlGatewayServerConfig.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway/src/main/java/com/vesoft/nebula/jdbc/mysql/query/AdvancedQueryHandler.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway/src/test/java/com/vesoft/nebula/jdbc/mysql/security/MysqlGatewaySecurityPolicyTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway/src/test/java/com/vesoft/nebula/jdbc/mysql/query/AdvancedQueryHandlerTest.java`

**Interfaces:**
- Consumes: session user/database, statement kind and command type.
- Produces: configurable read/write/ddl/metadata permission checks and SSL required failure mapping before execution.

- [x] 先写 tests，覆盖 write denied、ddl denied、metadata allowed、SSL required with non-SSL session。
- [x] 运行安全相关单测确认失败。
- [x] 实现 immutable security policy 和 operation 判断。
- [x] 在 handler 执行端口前调用 policy，权限失败映射 1142，SSL required 映射 3159。
- [x] 运行安全和 query 单测通过。
- [x] 勾选 tasks.md 的 2.3 和 3.3。
- [x] 提交：`feat(nebula): enforce mysql gateway security policy`

### Task 5: Metadata 兼容增强

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway/src/main/java/com/vesoft/nebula/jdbc/mysql/query/StaticMysqlMetadataHandler.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway/src/main/java/com/vesoft/nebula/jdbc/mysql/query/AdvancedQueryHandler.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway/src/test/java/com/vesoft/nebula/jdbc/mysql/query/MysqlMetadataHandlerTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway/src/test/java/com/vesoft/nebula/jdbc/mysql/query/AdvancedQueryHandlerTest.java`

**Interfaces:**
- Consumes: `MysqlMetadataHandler.showDatabases/showTables/describe`.
- Produces: compatible result sets for `SHOW VARIABLES`, `SELECT DATABASE()`, common `information_schema.tables/columns` fixtures.

- [x] 先写 tests，覆盖 JDBC metadata 查询、DBeaver/DataGrip 常见 `information_schema` fixture、SHOW VARIABLES。
- [x] 运行 metadata 单测确认失败。
- [x] 扩展 metadata handler 和 query routing，授权后返回稳定列名/类型。
- [x] 对 unsupported metadata query 返回明确不支持错误或空结果，行为写入 build report。
- [x] 运行 metadata 与 query 单测通过。
- [x] 勾选 tasks.md 的 2.4。
- [x] 提交：`feat(nebula): improve mysql metadata compatibility`

### Task 6: 构建验证与验收记录

**Files:**
- Modify: `docs/superpowers/reports/2026-07-18-nebula-mysql-protocol-write-and-advanced-support-build.md`
- Modify: `openspec/changes/add-nebula-mysql-protocol-write-and-advanced-support/tasks.md`

**Interfaces:**
- Consumes: 全部实现和测试。
- Produces: build 阶段验证证据；若无法真实 GUI/BI 手动验证，记录为 fixture-based 验证和阻塞项。

- [x] 运行模块测试：`JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway -am test -DfailIfNoTests=false`
- [x] 运行相关 JDBC 测试：`JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc test -Dtest=NebulaSqlWriteConverterTest,NebulaSqlDdlConverterTest,NebulaSqlMutationExecutorTest,DirectNebulaSqlExecutorTest`
- [x] 在 build report 记录 Java MySQL JDBC Driver 路径的自动测试证据。
- [x] 对 GUI/BI 客户端验证，若无可运行客户端，则记录 DBeaver/DataGrip/BI metadata fixture 覆盖与手动阻塞。
- [ ] 勾选 tasks.md 的 3.1、3.2。
- [ ] 提交：`test(nebula): verify mysql advanced protocol compatibility`
