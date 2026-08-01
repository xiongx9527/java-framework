## Why

用户要求支持存储过程。由于 NebulaGraph 不提供 MySQL 存储过程执行引擎，需要在网关侧提供有限的 routine 解析、保存和解释执行能力。

## What Changes

- 支持 CREATE PROCEDURE、DROP PROCEDURE、CALL 的基础路径。
- 保存 routine 定义到网关元数据。
- 支持参数、顺序 SQL、简单变量和简单条件。
- 过程体内 SQL 复用 SQL→nGQL 查询/写入能力。
- 对游标、handler、动态 SQL、复杂事务等返回明确错误。

## Capabilities

### New Capabilities
- `nebula-mysql-routine-subset`: MySQL 存储过程与函数子集，覆盖本 change 的需求、边界和验收场景。

### Modified Capabilities

## Impact

- 依赖 SQL parser、SQL 查询/写入转换和 MySQL 协议高级支持。
- 可能新增 routine 元数据存储和解释执行器。
- 影响权限、安全和执行超时策略。
