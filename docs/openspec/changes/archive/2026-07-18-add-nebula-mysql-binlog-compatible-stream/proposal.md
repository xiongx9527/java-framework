## Why

用户要求 MySQL binlog 支持，以便 CDC、同步或审计工具消费经 MySQL 协议网关产生的 Nebula 写入事件。NebulaGraph 不天然产出 MySQL binlog，因此需要定义网关侧 binlog-compatible event stream。

## What Changes

- 新增网关侧写入/DDL 事件日志模型。
- 将经网关执行的 INSERT、UPDATE、DELETE 和受支持 DDL 记录为 binlog-compatible event。
- 提供基础 position、event stream 和消费 API。
- 明确绕过网关直接写入 NebulaGraph 的变更不在捕获范围内。

## Capabilities

### New Capabilities
- `nebula-mysql-binlog-compatible-stream`: MySQL binlog 兼容事件流，覆盖本 change 的需求、边界和验收场景。

### Modified Capabilities

## Impact

- 依赖 MySQL 协议写入和 DDL 支持。
- 可能新增持久化事件日志、position 管理和 CDC 输出模块。
- 影响网关写入路径，需要保证事件生成与执行结果的一致性边界。
