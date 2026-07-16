## Why

在具备 binlog-compatible 事件流后，部分 CDC 或同步客户端希望通过 MySQL 复制协议读取事件。需要支持可控的复制协议子集，而不是承诺完整 MySQL 主从复制。

## What Changes

- 新增 MySQL 复制协议子集支持。
- 支持基础复制握手和从指定 position 拉取事件。
- 支持 `COM_BINLOG_DUMP` 或等价子集。
- 对 GTID、完整 slave 兼容等不支持能力返回明确错误。

## Capabilities

### New Capabilities
- `nebula-mysql-replication-protocol-subset`: MySQL 复制协议子集，覆盖本 change 的需求、边界和验收场景。

### Modified Capabilities

## Impact

- 依赖 binlog-compatible stream。
- 影响 MySQL 协议网关连接类型和命令分发。
- 可能需要针对 CDC 客户端做兼容测试。
