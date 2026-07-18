## Why

只读网关只能满足查询和浏览需求。用户要求完整支持写入、DDL、prepared statement、权限、SSL 和 Java MySQL JDBC Driver / BI 工具高级兼容，需要在只读网关基础上扩展高级协议能力。

## What Changes

- 扩展 MySQL 协议网关支持写入和 DDL。
- 支持 prepared statement 常见路径。
- 支持权限、SSL 或可配置安全策略。
- 增强 Java MySQL JDBC Driver、BI/GUI metadata fixture 的兼容，并记录真实 GUI/BI 客户端的后续人工验收事项。

## Capabilities

### New Capabilities
- `nebula-mysql-protocol-write-and-advanced-support`: MySQL 协议写入与高级兼容，覆盖本 change 的需求、边界和验收场景。

### Modified Capabilities

## Impact


- MySQL binlog、复制协议、存储过程和触发器不在本 change 内实现；这些能力由后续独立 changes 承接。
- 依赖只读 MySQL 协议网关和 SQL 写入/DDL 支持。
- 可能影响 boot starter/autoconfigure、部署配置和安全配置。
- 需要更完整的协议测试、客户端兼容矩阵和真实 GUI/BI 客户端后续人工验收记录。
