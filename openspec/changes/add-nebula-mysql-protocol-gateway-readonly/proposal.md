## Why

许多客户端和 BI 工具通过 MySQL wire protocol 接入数据源。为了让这些工具查询 NebulaGraph，需要提供兼容基础 MySQL 协议的只读网关，并复用 SQL 查询转换能力。

## What Changes

- 新增 MySQL wire protocol 服务端基础网关。
- 支持握手、认证、COM_QUERY 和文本协议结果集返回。
- 支持 SELECT、复杂只读 SQL、SHOW DATABASES、SHOW TABLES、DESCRIBE 等只读语句，复杂 SQL 语义由 SQL→nGQL 查询转换层判定。
- 支持 MySQL CLI、DBeaver、DataGrip、Navicat 等客户端的基础连接与查询，包括客户端发起的复杂只读 SQL。

## Capabilities

### New Capabilities
- `nebula-mysql-protocol-gateway-readonly`: MySQL 协议只读网关，覆盖本 change 的需求、边界和验收场景。

### Modified Capabilities

## Impact


- MySQL binlog、复制协议、存储过程和触发器不在本 change 内实现；这些能力由后续独立 changes 承接。
- 可能新增 `fw-plus-nebula-mysql-gateway` 子模块或 starter。
- 依赖 SQL parser core 和 SQL 查询转换。
- 可能引入 MySQL protocol 编解码依赖或 Netty 服务端。
