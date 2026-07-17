---
comet_change: add-nebula-mysql-replication-protocol-subset
role: technical-design
canonical_spec: openspec
---

# Nebula MySQL 复制协议子集技术设计

## 1. 背景与目标

本 change 为 Nebula MySQL 协议路线提供复制协议受控子集。目标是让 CDC 或同步客户端可以通过 MySQL 复制协议命令读取网关侧的 binlog-compatible event stream，而不是让 NebulaGraph 或网关伪装成完整 MySQL primary server。

当前仓库主线已经有 `fw-plus-nebula-jdbc` 基础 JDBC 能力；MySQL wire protocol 网关、写入/DDL 支持和 binlog-compatible stream 属于分阶段建设能力。只读网关设计中已把 `MysqlCommandDispatcher` 作为协议命令白名单扩展点，binlog-compatible stream OpenSpec 也明确 position 和事件消费 API 是前置契约。本设计建立在这些边界之上，不直接修改 nGQL JDBC 直连路径，也不在复制协议层生成事件。

## 2. 范围与非目标

### 范围

- 在 MySQL protocol gateway 的连接、握手、认证、session 和 packet codec 基础上扩展复制命令处理。
- 支持复制相关连接上下文，包含可选的 `COM_REGISTER_SLAVE` 或等价注册命令。
- 支持 `COM_BINLOG_DUMP` 或等价子集，从指定 position 读取 binlog-compatible stream。
- 将 binlog-compatible stream 的内部 position 映射为复制协议可传输的 file/offset 视图或稳定 token。
- 将 stream event 编码为 CDC 客户端可识别的最小 binlog-compatible event packet 序列。
- 对 GTID、完整 slave 生命周期、semi-sync、relay log、master status 写入语义和未知复制命令返回明确不支持错误。
- 补充协议、position、事件编码、不支持能力和客户端兼容测试。

### 非目标

- 不实现完整 MySQL replication server。
- 不支持 `COM_BINLOG_DUMP_GTID`、GTID set、multi-source replication、semi-sync、relay log 或真实 MySQL slave 状态机。
- 不承诺 MySQL row image、mixed/statement/row binlog mode、transaction boundary 或全局提交顺序与 MySQL 等价。
- 不捕获绕过 MySQL 网关直接写入 NebulaGraph 的变更。
- 不在复制协议层补偿、推导或伪造 binlog-compatible stream 中不存在的事件。
- 不改变现有 `jdbc:nebula://...` nGQL 直连能力。
- 不在本 change 中新增 Spring Boot starter 或 Magic-API 插件；如需自动配置，后续独立 change 承接。

## 3. 推荐方案

采用方案 A：复用 MySQL 网关 dispatcher 扩展复制协议子集。

复制能力作为 MySQL gateway 的一个可选 command handler 接入：

1. `MysqlProtocolSession` 继续负责连接、认证、capability、sequence id 和 lifecycle。
2. `MysqlCommandDispatcher` 新增 replication command 分发分支，但仍保持命令白名单。
3. `ReplicationCommandHandler` 处理 `COM_REGISTER_SLAVE`、`COM_BINLOG_DUMP` 和明确不支持的复制命令。
4. `BinlogStreamReader` 消费前置 binlog-compatible stream 的稳定 API。
5. `ReplicationEventPacketEncoder` 把 stream event 转为 MySQL replication event packet 子集。

该方案避免复制能力重复实现网络协议、握手、认证和连接管理，也让只读查询、写入/DDL、binlog event 生成和复制读取保持清晰边界。相比独立复制端口，它更贴合前置网关设计；相比非 MySQL 协议 API，它满足 CDC 客户端通过复制命令读取事件的核心目标。

## 4. 模块与组件设计

### 4.1 `ReplicationCommandHandler`

职责：

- 校验连接已认证，并检查复制能力是否启用。
- 解析复制命令 packet。
- 处理 `COM_REGISTER_SLAVE` 或等价注册命令，建立复制读取上下文。
- 处理 `COM_BINLOG_DUMP`，创建从指定 position 开始的 event stream cursor。
- 将 `COM_BINLOG_DUMP_GTID`、未知复制命令和未实现 slave 能力映射为 ERR packet。
- 在客户端断开、超时或 `COM_QUIT` 时关闭 stream cursor。

该 handler 不执行 SQL，不调用 SQL parser/converter，也不访问 Nebula 原生写入路径。它只消费 binlog-compatible stream 暴露的读取 API。

### 4.2 `ReplicationSessionState`

职责：

- 保存 replica/server id、requested file、requested offset、当前 stream position、dump flags 和读取状态。
- 记录该连接是否进入 replication streaming mode。
- 记录最近发送事件的 position，用于日志和诊断。
- 维护 heartbeat 时间和 idle timeout。

`ReplicationSessionState` 挂在 `MysqlProtocolSession` 下，不能污染普通 COM_QUERY 连接路径。未进入 replication mode 的连接仍按普通 MySQL gateway 命令处理。

### 4.3 `ReplicationPositionCodec`

职责：

- 在复制协议的 file/offset 视图和 binlog-compatible stream 内部 position 之间转换。
- 支持从客户端给定 position 继续读取。
- 对未知、过期、被清理或格式非法的 position 返回结构化错误。
- 保证 position 编码稳定、可日志化、可用于测试断言。

推荐把 stream 内部 position 作为事实源。复制协议可暴露类似 `nebula-binlog.000001` + offset 的兼容视图，也可以在 file 名或 offset 中编码稳定 token；但不应声称该坐标等价于真实 MySQL binlog 文件坐标。

position 处理原则：

- `position=0` 或配置定义的起点表示从当前可用最早事件开始。
- 已确认存在的 position 从其后续事件开始返回。
- 过期 position 返回明确错误，不能静默跳到最新 position。
- 未知 future position 返回明确错误或等待策略必须可配置，默认返回错误。

### 4.4 `BinlogStreamReader`

职责：

- 作为复制协议层消费 binlog-compatible stream 的唯一端口。
- 根据 decoded stream position 创建 cursor。
- 支持批量读取和持续阻塞/轮询读取。
- 支持 heartbeat 或空闲保活。
- 暴露 stream retention、position unavailable、stream disabled 等明确错误。

建议接口形态：

```java
public interface BinlogStreamReader {
	BinlogStreamCursor open(BinlogReadRequest request) throws BinlogStreamException;
}
```

`BinlogReadRequest` 至少包含 session id、replica id、start position、是否持续读取、batch size 和 timeout。协议层不直接读取事件持久化表或存储实现。

### 4.5 `ReplicationEventPacketEncoder`

职责：

- 将 binlog-compatible event 转为复制协议 event packet。
- 固定 event header 中 timestamp、server id、event size、next position 和 flags 的生成规则。
- 对 DML、DDL、heartbeat、rotate/format 描述事件使用稳定子集。
- 不支持或无法可靠表达的 event 类型返回明确编码错误，并终止或跳过策略可配置；默认终止并报错。

最小事件子集建议：

- Format description event：连接进入 dump 后先发送，说明子集版本。
- Rotate event：在兼容 file 视图变化时发送；若 file 视图固定，可只在 stream 起点发送。
- Query/schema event：表达受支持 DDL 或 schema 变化。
- Table map + row-like event：表达经网关捕获的 DML 变化。
- Heartbeat event：长时间无新事件时保活。

事件内容必须来自 binlog-compatible stream，不在 encoder 中推导业务变更。

### 4.6 `ReplicationErrorMapper`

职责：

- 把复制协议拒绝、stream 错误和 position 错误映射为 MySQL ERR packet。
- 提供稳定 error code、SQLState 和消息。
- 客户端消息说明“复制协议子集不支持该能力”或“position 不可用”，避免暗示完整 MySQL 行为。

建议映射：

| 场景 | MySQL error code | SQLState | 说明 |
|------|------------------|----------|------|
| 复制能力未启用 | 1235 | HY000 | Replication subset is not enabled |
| 未认证发送复制命令 | 1045 | 28000 | Access denied |
| `COM_BINLOG_DUMP_GTID` | 1235 | HY000 | GTID replication is not supported |
| 未知复制命令 | 1235 | HY000 | Replication command is not supported |
| position 格式非法 | 1236 | HY000 | Could not parse requested binlog position |
| position 已过期或被清理 | 1236 | HY000 | Requested binlog position is no longer available |
| stream 未启用或不可用 | 1105 | HY000 | Binlog-compatible stream is unavailable |
| 事件编码失败 | 1105 | HY000 | Could not encode replication event |

服务端日志应记录 connection id、replica id、start position、stream position 和诊断 id，但不能泄露密码、连接串或敏感配置。

## 5. 协议状态机

### 5.1 连接与注册

```text
client connect
  -> MysqlHandshakeHandler
  -> MysqlGatewayAuthenticator
  -> MysqlProtocolSession authenticated
  -> client sends COM_REGISTER_SLAVE (optional)
  -> ReplicationCommandHandler stores ReplicationSessionState
  -> OK packet
```

`COM_REGISTER_SLAVE` 是兼容性入口，不代表网关实现完整 MySQL slave 管理。若客户端跳过注册直接发送 `COM_BINLOG_DUMP`，系统可按配置允许并用 connection id 作为 replica id；默认建议允许，以兼容轻量 replication client。

### 5.2 `COM_BINLOG_DUMP`

```text
COM_BINLOG_DUMP(file, position, flags, serverId)
  -> ReplicationCommandHandler
  -> ReplicationPositionCodec.decode(file, position)
  -> BinlogStreamReader.open(startPosition)
  -> ReplicationEventPacketEncoder.formatDescription()
  -> loop:
       read next event
       encode event packet
       write packet with continuous sequence id
       update ReplicationSessionState.lastPosition
```

读取循环在以下条件结束：

- 客户端断开或发送 quit。
- stream cursor 报不可恢复错误。
- position 不可用。
- 配置为一次性读取且到达当前末尾。
- 服务端 shutdown 或连接超时。

### 5.3 不支持命令

```text
COM_BINLOG_DUMP_GTID / unknown replication command
  -> ReplicationCommandHandler
  -> ReplicationErrorMapper
  -> ERR 1235
```

不支持命令不得被当作 OK，也不得回退到普通 COM_QUERY 或其他协议路径。

## 6. 与前置能力的稳定依赖

本 change 只依赖以下稳定边界：

- MySQL gateway protocol adapter：握手、认证、session、packet codec、command dispatcher、OK/ERR packet 编码。
- MySQL gateway write/DDL path：只作为 binlog-compatible stream 的事件来源，不被复制协议直接调用。
- Binlog-compatible stream：提供 event model、position model、持久化/保留策略和按 position 消费 API。
- Gateway 配置：启用/禁用复制协议子集、允许客户端、batch size、heartbeat、position retention 错误策略。

禁止依赖：

- Druid AST 具体类。
- SQL-to-nGQL converter 私有模型。
- Nebula 原生变更监听或无法可靠观察的写入路径。
- 未稳定的 routine、trigger 或外部 CDC 存储内部实现。

## 7. Position 语义

复制协议 position 是 binlog-compatible stream position 的兼容表达，不是 MySQL 全局 binlog 坐标。

规则：

- stream position 是唯一事实源。
- 复制协议 file/offset 仅用于客户端续读和日志展示。
- 每个发送成功的 event 都必须对应一个 next position。
- 断线后客户端使用最后确认的 position 续读，应收到该 position 后的事件。
- position 过期、未知或格式非法必须返回明确错误。
- 不允许为了保持连接成功而静默跳到 latest position。

如果前置 stream 支持 retention watermark，`ReplicationPositionCodec` 应在打开 cursor 前校验 start position 是否早于 watermark，并返回 position unavailable 错误。

## 8. Capability 与客户端兼容边界

网关不应在握手中声明会诱导客户端进入完整复制能力的额外 capability。复制命令支持应由命令白名单和配置控制，而不是通过假装完整 MySQL server 来提升兼容性。

兼容目标优先级：

1. 轻量 MySQL replication client：可连接、注册、dump、读取事件、断线续读。
2. CDC 工具的基础 dump path：可通过 `COM_BINLOG_DUMP` 读取事件。
3. 对依赖 GTID、完整 server variables、完整 `SHOW MASTER STATUS` 或复杂 MySQL 元数据的工具，返回明确不支持或要求后续独立 change。

如果客户端在 dump 前执行 `SHOW MASTER STATUS`、`SHOW BINARY LOGS` 等探测 SQL，本 change 可通过前置 gateway metadata handler 返回子集响应或明确不支持，但复制读取的事实源仍是 `COM_BINLOG_DUMP` 和 stream position。

## 9. 错误处理与资源释放

- 未认证复制命令：返回认证错误并关闭连接。
- 复制能力关闭：返回 1235，不打开 stream cursor。
- 注册 packet 格式错误：返回协议错误。
- dump position 非法：返回 1236。
- dump position 过期：返回 1236。
- stream cursor 打开失败：返回 1105，并记录诊断 id。
- event 编码失败：默认返回 ERR 并关闭 stream；如未来支持跳过，必须显式配置并记录跳过事件。
- 客户端断开：关闭 cursor，不回滚已发送事件。
- 服务端 shutdown：关闭 cursor 和 channel，日志记录最后发送 position。

资源释放必须覆盖正常结束、异常结束、客户端断开和 idle timeout。stream cursor 不应由 Netty channel 或 socket lifecycle 之外的后台线程长期泄漏。

## 10. 测试策略

### 10.1 协议单元测试

- `COM_REGISTER_SLAVE` packet 解析、state 写入和 OK packet。
- 直接 `COM_BINLOG_DUMP` 可按配置建立读取上下文。
- `COM_BINLOG_DUMP_GTID` 返回 1235。
- 未知复制命令返回 1235。
- 未认证发送复制命令返回 1045 或关闭连接。
- sequence id 在 dump 事件发送过程中连续。

### 10.2 Position 测试

- 从起点读取最早可用事件。
- 从中间 position 续读，只返回该 position 后事件。
- 断线后用最后 position 续读不重复已确认事件。
- position 格式非法返回 1236。
- position 早于 retention watermark 返回 1236。
- future/未知 position 返回明确错误，默认不等待、不跳 latest。

### 10.3 Stream adapter 测试

- DML event 映射为 table-map + row-like event 子集。
- DDL event 映射为 query/schema event 子集。
- heartbeat 在空闲时发送。
- format description event 在 dump 起始发送。
- encoder 不从事件外部推导 row image。

### 10.4 客户端兼容测试

- 至少一个轻量 MySQL replication client 完成连接、可选注册、dump、读取事件。
- 客户端读取一批事件后断开，再从最后 position 续读。
- 客户端请求 GTID path 时收到明确不支持错误。
- stream 未启用或 position 过期时客户端收到明确错误。

### 10.5 边界测试

- 绕过网关直接写入 NebulaGraph 不产生复制事件。
- 复制命令不会调用 SQL parser、SQL converter 或 Nebula 执行链。
- 普通 COM_QUERY 连接不携带 replication state。
- 关闭复制能力不影响只读查询和普通写入/DDL 网关路径。

## 11. 实施顺序

1. 固定 MySQL gateway command dispatcher 的扩展接口。
2. 定义 `ReplicationSessionState`、`ReplicationCommandHandler` 和错误映射。
3. 定义 `ReplicationPositionCodec` 与 binlog-compatible stream position 适配契约。
4. 定义 `BinlogStreamReader` 端口和 fake stream 测试夹具。
5. 实现 `COM_REGISTER_SLAVE` 可选注册。
6. 实现 `COM_BINLOG_DUMP` 读取循环和 cursor lifecycle。
7. 实现最小 event packet encoder。
8. 补齐 position、unsupported command、stream unavailable 和客户端续读测试。
9. 做至少一个 CDC/replication client 验收记录。

## 12. 风险与缓解

- **前置 binlog-compatible stream position 尚未深度定稿。** 缓解：本 change 只依赖稳定读取端口和 position codec 契约；真实存储细节留在 stream 模块内。
- **客户端误认为网关支持完整 MySQL replication。** 缓解：capability 保守声明，不支持路径返回 1235/1236，文档和测试矩阵写明边界。
- **CDC 客户端依赖 GTID 或完整 metadata。** 缓解：把 GTID 明确列为非目标；可在后续 change 针对具体客户端补 metadata 子集。
- **position 保留期导致续读失败。** 缓解：返回明确 position unavailable 错误，并暴露 retention 配置和诊断日志。
- **event packet 子集与客户端期望不一致。** 缓解：先用 fake stream 做协议单元测试，再用至少一个真实 replication client 做验收；兼容矩阵记录不支持原因。
- **复制读取影响普通网关连接资源。** 缓解：为 dump cursor 设置独立 timeout、batch size、限流和关闭钩子，避免长连接泄漏。

