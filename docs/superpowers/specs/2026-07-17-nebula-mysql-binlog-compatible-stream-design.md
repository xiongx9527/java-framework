---
comet_change: add-nebula-mysql-binlog-compatible-stream
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-18-add-nebula-mysql-binlog-compatible-stream
status: final
---

# Nebula MySQL Binlog 兼容事件流技术设计

## 1. 背景与目标

本 change 为 Nebula MySQL 协议路线提供网关侧 binlog-compatible event stream。NebulaGraph 不产生 MySQL binlog，也无法观察绕过网关的原生写入，因此本设计只承诺记录经 MySQL 协议网关成功执行的写入和受支持 DDL，并以稳定 position 对外提供可消费事件流。

该能力位于写入/DDL 网关路径之后、复制协议子集之前。写入/DDL change 负责产生可判定的执行结果，binlog stream 负责把结果落为持久事件，replication change 只消费该事件流并做 MySQL 复制协议适配。

## 2. 范围与非目标

### 范围

- 定义 binlog-compatible event、position、事件状态和持久化存储边界。
- 在经网关执行成功的 INSERT、UPDATE、DELETE 和受支持 DDL 后生成事件。
- 提供按 position 拉取和持续读取事件的内部消费 API。
- 固定绕过网关写入不产生虚假事件的边界。
- 为复制协议、CDC、审计和后续运维工具提供统一事件源。

### 非目标

- 不承诺完整 MySQL binlog 文件格式、GTID、row image、statement/mixed mode 或真实 MySQL 主从语义。
- 不捕获 Nebula 原生客户端、nGQL 直连 JDBC 或其他绕过 MySQL 网关的写入。
- 不在本 change 中实现 MySQL 复制协议命令；复制协议由 `add-nebula-mysql-replication-protocol-subset` 承接。
- 不实现写入/DDL 转换本身，也不定义触发器、routine 或事务引擎。

## 3. 推荐方案

采用“网关事件日志 + 稳定 position + 可插拔持久化”的方案。

```text
MySQL gateway write/DDL command
  -> SQL write/DDL execution boundary
  -> success result with normalized mutation descriptor
  -> BinlogEventFactory
  -> BinlogEventStore.append(...)
  -> BinlogPosition
  -> BinlogEventReader / replication adapter / CDC consumer
```

事件生成必须位于执行成功边界之后。对于无法确认成功的异常、超时或部分失败，不生成成功事件；如需要记录诊断，可生成内部诊断事件，但不得对外伪装成已提交变更。

## 4. 核心模型

### 4.1 `NebulaBinlogEvent`

事件是稳定的内部契约，不直接等同 MySQL 原生 binlog packet。

建议字段：

- `position`：事件追加后分配的全局递增位置。
- `eventId`：全局唯一事件 id，用于幂等和排查。
- `eventType`：`WRITE_ROWS`、`UPDATE_ROWS`、`DELETE_ROWS`、`SCHEMA_CHANGE`、`HEARTBEAT`。
- `source`：网关实例、连接 id、session user、database/graphspace。
- `sql`：原始 SQL，可按配置脱敏或关闭保存。
- `ngql`：转换后的 nGQL，可按配置脱敏或关闭保存。
- `schema`：tag/edge/table/schema 名称和版本。
- `payload`：变更描述，包含主键/VID、src/dst/rank、属性变更和 DDL 摘要。
- `createdAt`：网关生成时间。

payload 不要求模拟 MySQL row image 的完整 before/after 语义。默认只记录网关可稳定获得的数据；如果写入转换层无法提供 before image，则事件必须标记 `beforeImageAvailable=false`。

### 4.2 `BinlogPosition`

position 是消费 API 的主契约。

建议形态：

```java
public final class BinlogPosition {
	private final long sequence;
	private final String storeId;
}
```

`sequence` 在同一 store 内单调递增。`storeId` 区分不同事件存储实例，避免消费者拿旧环境 position 误读新环境。复制协议适配层可以把 position 映射为 filename/offset，但内部不要被 MySQL binlog 文件模型绑死。

### 4.3 `BinlogEventStore`

事件存储接口负责原子追加、按 position 读取和保留策略。

```java
public interface BinlogEventStore {
	BinlogPosition append(NebulaBinlogEventDraft event) throws SQLException;

	BinlogEventBatch readAfter(BinlogPosition position, int maxEvents) throws SQLException;

	BinlogPosition currentPosition() throws SQLException;
}
```

默认实现可先提供内存 store 用于单元测试和开发模式；生产实现应支持可配置持久化，优先考虑嵌入式文件或关系型表存储。无论具体实现如何，接口必须保证同一 store 内 position 单调、读取顺序稳定、append 成功后可被后续 readAfter 看到。

## 5. 事件生成边界

### 5.1 DML 事件

DML 事件由网关写入执行路径在成功后生成：

- INSERT -> `WRITE_ROWS`
- UPDATE -> `UPDATE_ROWS`
- DELETE -> `DELETE_ROWS`

事件生成输入应来自写入/DDL change 暴露的 normalized mutation descriptor，而不是重新解析原始 SQL。descriptor 至少需要说明目标 tag/edge、标识列、属性列、执行后的可见变更和转换 trace。

如果 Nebula 执行成功但事件追加失败，默认策略应返回失败并暴露明确错误，因为消费者依赖事件流进行 CDC 或审计时，静默丢事件比写入失败更危险。后续可通过配置允许 best-effort 审计模式，但默认不能 best-effort。

### 5.2 DDL 事件

受支持 DDL 成功后生成 `SCHEMA_CHANGE` 事件，记录 schema 对象、操作类型、schema version 或变更摘要。

DDL 事件不模拟 MySQL 的完整 DDL binlog 语义，只保证后续消费者能识别 schema 变化并重新加载映射。复杂 DDL 或当前 SQL 写入/DDL change 不支持的语义，不应进入事件流。

### 5.3 绕过网关边界

绕过 MySQL 网关的 Nebula 原生写入不产生事件。文档、错误信息和复制协议 handshake 信息都应避免暗示“全局 Nebula binlog”。如消费者要求完整数据库 CDC，应返回明确不支持说明。

## 6. 消费 API

消费 API 面向 replication adapter、CDC 工具和测试。

```java
public interface BinlogEventReader {
	BinlogEventBatch readAfter(BinlogPosition position, int maxEvents, long waitMillis) throws SQLException;
}
```

读取语义：

- `position == null` 时从当前 store 的起点或配置起点读取。
- 指定已知 position 时返回该 position 之后的事件。
- 指定过期 position 时返回明确错误，提示消费者重新初始化。
- `waitMillis > 0` 时可短轮询等待新事件，但不要求 build 阶段实现复杂推送。

复制协议子集应只依赖 `BinlogEventReader`，不要反向访问写入执行链路。

## 7. 错误处理与一致性

- 写入执行失败：不生成成功事件，向客户端返回原执行错误。
- 事件构造失败：写入结果不得对外报告成功，除非显式启用 best-effort 模式。
- 事件追加失败：默认整体失败，错误消息说明 binlog-compatible stream append failed。
- 消费 position 不存在或过期：返回明确错误，不从错误位置静默重放。
- payload 包含敏感字段时，按配置脱敏或只保留 schema/position/trace id。

本 change 不提供跨 Nebula 执行和事件存储的分布式事务。设计上通过“默认 append 失败即对外失败”降低丢事件风险，并在文档中明确一致性边界。

## 8. 测试策略

- 事件模型测试：验证 DML、DDL、heartbeat 或边界事件字段、position 单调性和序列化稳定性。
- store 测试：验证 append、readAfter、currentPosition、空结果、过期 position 和并发追加顺序。
- 写入路径集成测试：使用 fake write executor 或 gateway command fixture，确认成功 INSERT/UPDATE/DELETE/DDL 生成事件，失败执行不生成事件。
- 边界测试：绕过网关或直接调用 Nebula executor 不产生 binlog-compatible 事件。
- 消费测试：从指定 position 读取、持续轮询读取和 position 过期错误。

测试不依赖真实 NebulaGraph。需要真实客户端兼容的验证留给 replication change 和后续端到端验收。

## 9. 分区依赖

- 依赖 `add-nebula-sql-write-and-ddl-support` 提供稳定写入/DDL descriptor 和执行结果边界。
- 依赖 `add-nebula-mysql-protocol-write-and-advanced-support` 将 MySQL 网关写入路径接到事件生成 hook。
- 被 `add-nebula-mysql-replication-protocol-subset` 消费，用于从 position 输出复制事件。
- routine 和 trigger 的内部 SQL 如果经网关写入执行链路落地，也应复用同一事件 hook；递归和事务边界由对应 change 处理。

## 10. 验收要点

- 经网关成功写入和受支持 DDL 后产生对应事件。
- 消费者可以从指定 position 之后读取事件。
- 绕过网关写入不产生虚假事件，并有明确文档说明。
- 事件追加失败默认阻止写入对外成功，避免静默丢事件。
- replication adapter 可以只通过 `BinlogEventReader` 获取事件，不依赖写入链路内部实现。
