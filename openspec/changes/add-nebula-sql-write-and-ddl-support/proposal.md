## Why

只支持 SQL 查询无法满足完整数据管理需求。用户要求 SQL 写入与 DDL 都可用，因此需要在解析核心基础上支持 INSERT、UPDATE、DELETE 和基础 DDL 到 nGQL 的转换。

## What Changes

- 新增 INSERT、UPDATE、DELETE 到 nGQL 的转换与执行。
- 新增基础 DDL 到 Nebula tag/edge schema 操作的映射。
- 定义 SQL 表结构与 Nebula tag/edge schema 的映射约束。
- 对事务、复杂 DDL 或不兼容语义返回明确错误。

## Capabilities

### New Capabilities
- `nebula-sql-write-and-ddl-support`: SQL 写入与 DDL 支持，覆盖本 change 的需求、边界和验收场景。

### Modified Capabilities

## Impact


- MySQL binlog、复制协议、存储过程和触发器不在本 change 内实现；这些能力由后续独立 changes 承接。
- 依赖 parser core，建议依赖查询转换中的 schema 映射模型。
- 影响 JDBC executeUpdate 路径和可能的 metadata 行为。
- 需要更严格的安全边界和错误回滚说明。
