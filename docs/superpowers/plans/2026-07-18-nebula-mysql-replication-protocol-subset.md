---
change: add-nebula-mysql-replication-protocol-subset
design-doc: docs/superpowers/specs/2026-07-17-nebula-mysql-replication-protocol-subset-design.md
base-ref: 2c33c0e4a1d441ba7438ad813082dad8332ba134
---

# Nebula MySQL 复制协议子集 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Nebula JDBC 模块中提供可被后续 MySQL gateway dispatcher 接入的复制协议子集骨架。

**Architecture:** 本轮不依赖尚未稳定的 binlog-compatible stream 实现，先定义 replication 层自己的消费端口、position codec、命令处理器和错误映射。测试使用 fake stream reader 验证 COM_REGISTER_SLAVE、COM_BINLOG_DUMP、position 续读和不支持命令边界。

**Scope update 2026-07-18:** 合入 `dev` 后，前置 binlog-compatible stream 已归档并提供稳定 `BinlogEventReader` API。本 change 已新增 `BinlogEventReaderStreamReader` 作为 replication 层 adapter，并用 `BinlogEventReaderStreamReaderTest` 证明从 `NebulaBinlogEvent` 到 replication stream event 的类型映射、payload 透传和 position 推进。真实 CDC/复制客户端端到端验收仍依赖尚未发现的 MySQL gateway 网络入口，作为后续 Manual Acceptance Follow-up，不计入当前 build gate。

**Tech Stack:** Java 1.8、JUnit 4、Maven Wrapper、`fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc`。

## Global Constraints

- 全程保持 Java 1.8 兼容，不使用 Java 9+ API。
- 不修改 `add-nebula-mysql-binlog-compatible-stream` change 状态。
- 不硬编码 binlog stream 分支的未稳定实现类，只通过本 change 定义的端口适配。
- 不改动现有 nGQL JDBC 直连路径。
- 复制协议层不得调用 SQL parser、SQL converter 或 Nebula 执行链。

---

### Task 1: 复制协议命令与错误边界

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/mysql/replication/ReplicationCommandHandlerTest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/mysql/replication/*.java`

**Interfaces:**
- Produces: `ReplicationCommandHandler.handle(ReplicationCommandRequest, ReplicationSessionState)` 返回 `ReplicationCommandResult`。
- Produces: `ReplicationError` 固定 MySQL error code、SQLState 和消息。

- [x] **Step 1: Write failing tests**

覆盖注册、未启用、GTID 不支持、未知复制命令和未认证边界。

- [x] **Step 2: Run test to verify it fails**

Run: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=ReplicationCommandHandlerTest test`

- [x] **Step 3: Implement minimal production code**

新增 command 常量、request/result、session state、error mapper 和 handler。

- [x] **Step 4: Run test to verify it passes**

Run: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=ReplicationCommandHandlerTest test`

### Task 2: COM_BINLOG_DUMP 与 position 适配端口

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/mysql/replication/ReplicationPositionCodecTest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/mysql/replication/BinlogStreamReader.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/mysql/replication/BinlogReadRequest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/mysql/replication/ReplicationPositionCodec.java`

**Interfaces:**
- Consumes: Task 1 的 `ReplicationCommandHandler`。
- Produces: `ReplicationPositionCodec.decode(String fileName, long offset)`。
- Produces: `BinlogStreamReader.open(BinlogReadRequest request)`。

- [x] **Step 1: Write failing tests**

覆盖 `nebula-binlog.000001` 起点、续读 token、非法 file/offset、position 不可用错误映射。

- [x] **Step 2: Run test to verify it fails**

Run: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=ReplicationPositionCodecTest test`

- [x] **Step 3: Implement minimal production code**

实现 file/offset 到内部 stream position 的稳定文本适配，保留后续替换真实 stream API 的端口。

- [x] **Step 4: Run test to verify it passes**

Run: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=ReplicationPositionCodecTest test`

### Task 3: 读取事件流和文档化未完成对接点

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/mysql/replication/ReplicationDumpFlowTest.java`
- Modify: `openspec/changes/add-nebula-mysql-replication-protocol-subset/tasks.md`

**Interfaces:**
- Consumes: Task 1/2 的 handler、stream reader、position codec。
- Produces: fake stream 下的 dump flow，可返回 format-description 占位事件和 stream 事件。

- [x] **Step 1: Write failing tests**

覆盖 COM_BINLOG_DUMP 打开 reader、记录 session position、从最后 position 续读、stream disabled/position unavailable 返回 ERR。

- [x] **Step 2: Run test to verify it fails**

Run: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=ReplicationDumpFlowTest test`

- [x] **Step 3: Implement minimal production code**

补齐 dump flow、cursor lifecycle 和 fake-stream 可验证行为。

- [x] **Step 4: Run focused and module tests**

Run: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=ReplicationCommandHandlerTest,ReplicationPositionCodecTest,ReplicationDumpFlowTest test`

- [x] **Step 5: Update OpenSpec tasks and commit**

勾选已完成的独立任务，把当前 build gate 收敛为 handler、position、binlog stream adapter 和 replication-style 自动化测试；真实 CDC/复制客户端验收记录为后续 Manual Acceptance Follow-up，不伪造成已完成。

## Manual Acceptance Follow-up

当前 worktree 未发现可运行的 MySQL gateway 网络入口，无法执行真实 CDC/复制客户端端到端验收。后续应在网络入口可运行后，至少用一个真实 CDC/复制客户端验证连接、dump、读取事件、断线续读和不支持能力错误响应，并将结果纳入单独 change 或人工验收记录。
