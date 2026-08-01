# Brainstorm Summary

- Change: add-nebula-mysql-replication-protocol-subset
- Date: 2026-07-17

## 已确认事实

- 本 change 是 Comet full workflow 的 design 阶段，当前 phase 为 `design`。
- OpenSpec canonical spec 位于 `openspec/changes/add-nebula-mysql-replication-protocol-subset/specs/nebula-mysql-replication-protocol-subset/spec.md`。
- 本 change 目标是支持 MySQL 复制协议受控子集，让 CDC/同步客户端可通过复制命令读取网关侧事件。
- 非目标是不承诺完整 MySQL slave、GTID、全量 MySQL Server 行为，也不捕获绕过网关直接写入 NebulaGraph 的变更。
- 当前源码主线主要已有 `fw-plus-nebula-jdbc` 基础 JDBC 能力；MySQL 网关、写入/DDL、binlog-compatible stream 和复制协议属于分阶段设计链路。
- 前置设计中只读网关建议以独立 `fw-plus-nebula-mysql-gateway` 或等价模块实现，`MysqlCommandDispatcher` 已被设计为协议命令白名单扩展点。
- 前置 binlog-compatible stream OpenSpec 要求提供事件格式、position 模型、持久化策略和按 position 消费 API，但尚无已合并的深度 Design Doc。

## 候选方案

### 方案 A：在 MySQL 网关命令分发层新增复制模式适配器（已确认）

在现有/前置 MySQL protocol gateway 的握手、session、packet codec 和命令分发基础上扩展 replication command handler。仅声明和接受复制子集所需能力，处理 `COM_REGISTER_SLAVE` 可选注册、`COM_BINLOG_DUMP` 读取，并把 position 映射到 binlog-compatible stream 的内部 position。协议层只做命令、状态和 MySQL packet 适配，不生成事件。

### 方案 B：独立复制端口/服务（候选）

复制能力独立监听端口，复用认证和 binlog-compatible stream，但不复用普通 MySQL query gateway 的 session/dispatcher。隔离性更强，但会重复握手、认证、packet 编解码和配置面，客户端也可能需要额外连接配置。

### 方案 C：只暴露 HTTP/SSE 等非 MySQL 复制协议接口（不推荐）

直接给 CDC 客户端提供非 MySQL 协议的事件读取 API。实现简单，但不满足本 change “通过 MySQL 复制协议子集读取”的核心目标，只能作为底层消费 API 的测试或运维补充。

## 确认的技术方案

采用方案 A：在 MySQL 网关命令分发层新增 `ReplicationCommandHandler` / `ReplicationSessionState` / `BinlogStreamReader` 边界，支持最小复制协议子集。

支持范围：

- 连接仍走 MySQL protocol v10 handshake 和既有认证。
- 支持 `COM_REGISTER_SLAVE` 作为可选上下文注册；不要求完整 slave 元数据语义。
- 支持 `COM_BINLOG_DUMP`，从客户端给定 binlog file/position 或等价 position token 读取 binlog-compatible stream。
- 返回可被 CDC 客户端识别的 binlog-compatible event packets，包括 rotate/format/heartbeat/query/table-map/row-like 事件的最小稳定子集，具体事件由 binlog-compatible stream adapter 产出。
- 对 `COM_BINLOG_DUMP_GTID`、GTID set、semi-sync、relay log、master status 写入语义、真实 MySQL slave 生命周期等返回明确不支持错误。

## 关键取舍与风险

- 复用网关 dispatcher 可减少重复协议栈，但要求前置网关的 session、capability 和 command handler 足够可扩展。
- 复制协议容易被客户端理解为完整 MySQL 主从能力，必须在 capability、错误码、文档和测试矩阵中持续强调“子集”。
- binlog-compatible stream 的 position 尚未深度定稿，本设计需要定义一个适配契约：复制协议 position 是 stream position 的可序列化视图，而不是 MySQL 全局 binlog 坐标。
- 若 binlog stream 没有持久化或保留期不足，复制读取必须返回明确过期/不可用错误，不能从最近位置静默继续。

## 测试策略

- 协议单元测试：注册、dump、unsupported command、sequence id、错误包。
- position 测试：从起点、从中间 position、断线续读、过期 position、未知 position。
- stream adapter 测试：binlog-compatible event 到 MySQL replication packet 的字段映射。
- 客户端兼容测试：至少一个 CDC/复制客户端或轻量 MySQL replication client，可完成连接、请求 dump、读取事件和续读。
- 边界测试：GTID、完整 slave、绕过网关变更、stream 未启用、权限不足均返回明确错误。

## Spec Patch 候选

- 补充不支持命令场景覆盖 `COM_BINLOG_DUMP_GTID` 和未知复制命令。
- 补充 position 过期/不可用时必须返回明确错误，禁止静默跳到最新位置。
- 补充事件来源边界：复制协议只输出 binlog-compatible stream 已持久化事件，不生成、不补偿、不伪造事件。

## 用户确认

- 2026-07-17：用户确认采用方案 A，并要求继续创建最终 Design Doc、必要时回写最小 Spec Patch、记录 `design_doc`、运行 design guard，不进入 build 实现、不合并、不 push。
