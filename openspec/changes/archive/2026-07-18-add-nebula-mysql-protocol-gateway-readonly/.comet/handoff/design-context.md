# Comet Design Handoff

- Change: add-nebula-mysql-protocol-gateway-readonly
- Phase: design
- Mode: compact
- Context hash: 4c333f63b2f8192277a07fb2ab0856006eec2dbd1017c666f409982d64a5de6f

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/add-nebula-mysql-protocol-gateway-readonly/proposal.md

- Source: openspec/changes/add-nebula-mysql-protocol-gateway-readonly/proposal.md
- Lines: 1-25
- SHA256: 5dc2489bc538374142173c221eaf8e643f60e56737033b8e4097cfd449b5c9c6

```md
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

```

## openspec/changes/add-nebula-mysql-protocol-gateway-readonly/design.md

- Source: openspec/changes/add-nebula-mysql-protocol-gateway-readonly/design.md
- Lines: 1-47
- SHA256: 9653444df85753833ebc916c4a54a3b09919e9678e1015abd20ca7e6a6d631b2

```md
## Context

本 change 属于 Nebula SQL / MySQL 协议支持路线的一部分。当前 `fw-plus-nebula-jdbc` 已具备稳定的 nGQL JDBC 基础能力，但完整 SQL 与 MySQL 协议能力需要分阶段建设。

前置依赖：依赖 `add-nebula-sql-parser-core` 和 `add-nebula-sql-to-ngql-query-support`。

## Goals / Non-Goals

**Goals:**

- 新增 MySQL wire protocol 服务端基础网关。
- 支持握手、认证、COM_QUERY 和文本协议结果集返回。
- 支持 SELECT、复杂只读 SQL、SHOW DATABASES、SHOW TABLES、DESCRIBE 等只读语句，复杂 SQL 语义由 SQL→nGQL 查询转换层判定。
- 支持 MySQL CLI、DBeaver、DataGrip、Navicat 等客户端的基础连接与查询，包括客户端发起的复杂只读 SQL。

**Non-Goals:**


- MySQL binlog、复制协议、存储过程和触发器不在本 change 内实现；这些能力由后续独立 changes 承接。
- 不扩大到本拆分项之外的后续能力。
- 不破坏现有 `jdbc:nebula://...` nGQL 直连路径。
- 不追求 100% MySQL Server 或 SQL 标准兼容。

## Decisions

- 第一阶段只读，降低协议和语义风险。
- 协议编解码与 SQL 执行层分离，便于后续写入和高级协议扩展。
- 认证先采用可配置的简单认证，权限体系留给高级协议 change。

## Risks / Trade-offs

- **[Risk] SQL 与图模型语义存在天然差异。** → Mitigation：采用明确白名单和可测试映射，不支持时返回清晰错误。
- **[Risk] 客户端兼容范围可能快速扩大。** → Mitigation：按 change 分阶段交付，每阶段定义验收客户端和协议子集。
- **[Risk] 引入第三方 parser 或协议库可能带来许可证和 Java 8 兼容风险。** → Mitigation：在 design/build 阶段先做依赖评估，再落地实现。

## Migration Plan

1. 在本 change 内完成设计与测试边界确认。
2. 以不破坏现有 nGQL 直连能力为前提新增能力。
3. 每个后续 change 只依赖前置产物的稳定 API，不回头扩大前置范围。
4. 如发现范围过大，优先继续拆分而不是在单个 change 中堆叠。

## Open Questions

- 是否采用第三方 parser / protocol 库，或自研轻量实现？
- SQL schema 与 Nebula tag/edge/property/id 的最终映射配置放在哪一层？
- 是否需要 Spring Boot starter、Magic-API 插件或独立服务入口？

```

## openspec/changes/add-nebula-mysql-protocol-gateway-readonly/tasks.md

- Source: openspec/changes/add-nebula-mysql-protocol-gateway-readonly/tasks.md
- Lines: 1-20
- SHA256: d62afe081401a20e403a4e53dddf4bd152ba2b380235cac50d98127c87e02443

```md
## 1. 协议基础

- [ ] 1.1 评估 MySQL protocol 实现方式和模块边界。
- [ ] 1.2 实现握手、认证和连接生命周期。
- [ ] 1.3 实现协议命令白名单和非目标命令错误语义。

## 2. 只读查询

- [ ] 2.1 实现 COM_QUERY 文本查询处理。
- [ ] 2.2 接入 SQL 查询转换和 Nebula 执行结果，包括复杂只读 SQL 的可映射子集。
- [ ] 2.3 实现 MySQL 结果集编码。
- [ ] 2.4 实现 SHOW/DESCRIBE 元数据响应。
- [ ] 2.5 实现只读拒绝、parser/converter/Nebula 执行错误到 MySQL ERR 响应的映射。
- [ ] 2.6 固定网关依赖的 parser、SQL 查询转换、JDBC/Nebula 执行和 metadata 稳定接口。

## 3. 客户端验证

- [ ] 3.1 用 MySQL CLI 和至少一个 GUI 客户端验证连接查询。
- [ ] 3.2 补充只读拒绝写入测试。
- [ ] 3.3 补充协议握手、认证失败、非目标命令、结果集编码和 metadata 查询测试。

```

## openspec/changes/add-nebula-mysql-protocol-gateway-readonly/specs/nebula-mysql-protocol-gateway-readonly/spec.md

- Source: openspec/changes/add-nebula-mysql-protocol-gateway-readonly/specs/nebula-mysql-protocol-gateway-readonly/spec.md
- Lines: 1-50
- SHA256: 5ba496bb7e9e4e360cc7b4b9ca3db954a184e479d4cd0d151ae7b3b1de9af426

```md
## ADDED Requirements

### Requirement: MySQL clients can establish read-only connections
系统 SHALL 提供 MySQL wire protocol 只读服务端，使常见 MySQL 客户端可连接。

#### Scenario: 客户端握手
- **WHEN** MySQL CLI 或 GUI 客户端连接网关
- **THEN** 完成握手和基础认证。

#### Scenario: 认证失败
- **WHEN** 客户端使用错误用户名、密码或不允许的默认 database 连接网关
- **THEN** 网关返回 MySQL 客户端可理解的认证错误并关闭连接。

#### Scenario: 非目标协议命令被拒绝
- **WHEN** 客户端发送本 change 不支持的 prepared statement、LOAD DATA、binlog、复制或 routine 相关协议命令
- **THEN** 网关返回明确不支持错误，且不进入 SQL 查询转换或 Nebula 执行链路。

### Requirement: COM_QUERY read statements are executed
系统 SHALL 接收 COM_QUERY 文本查询，将受支持 SQL 查询（包括复杂只读 SQL 的可映射子集）转给 SQL→nGQL 查询能力并返回 MySQL 协议结果集。

#### Scenario: SELECT 查询
- **WHEN** 客户端执行 SELECT 
- **THEN** 网关返回 MySQL 文本协议结果集。

#### Scenario: 复杂只读 SQL 转发
- **WHEN** 客户端通过 COM_QUERY 执行 JOIN、子查询、聚合、GROUP BY、HAVING、UNION 或函数表达式等复杂只读 SQL
- **THEN** 网关将 SQL 交给 SQL→nGQL 查询能力判定和转换，不在协议层直接依赖 parser 或 converter 内部模型。

#### Scenario: SHOW 和 DESCRIBE
- **WHEN** 客户端执行 SHOW TABLES 或 DESCRIBE 
- **THEN** 网关返回结构信息。

#### Scenario: 结果集类型编码
- **WHEN** 查询结果包含 NULL、字符串、数字、布尔、日期时间或其他对象值
- **THEN** 网关使用 MySQL 文本协议返回稳定列定义和行数据编码。

#### Scenario: 查询错误语义
- **WHEN** SQL 语法错误、SQL→nGQL 转换不支持或 Nebula 执行失败
- **THEN** 网关返回包含稳定错误码、SQLState 或明确错误原因的 MySQL ERR 响应。

### Requirement: Read-only gateway rejects writes clearly
只读网关 SHALL 拒绝写入、DDL 和事务语句，并返回 MySQL 客户端可理解的错误。

#### Scenario: 写入被拒绝
- **WHEN** 客户端执行 INSERT、UPDATE、DELETE 或 DDL 
- **THEN** 返回明确只读错误。

#### Scenario: 事务与高级能力被拒绝
- **WHEN** 客户端执行 BEGIN、START TRANSACTION、COMMIT、ROLLBACK、CALL、TRIGGER、binlog 或复制相关语句
- **THEN** 网关返回明确只读或不支持错误，且不调用 Nebula 执行链路。

```
