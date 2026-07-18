# Comet Design Handoff

- Change: add-nebula-mysql-protocol-write-and-advanced-support
- Phase: design
- Mode: compact
- Context hash: 09dab744643696c42646afdad3135bc4f5875b2f65902bcd7cbed8efb48e8bb8

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/add-nebula-mysql-protocol-write-and-advanced-support/proposal.md

- Source: openspec/changes/add-nebula-mysql-protocol-write-and-advanced-support/proposal.md
- Lines: 1-25
- SHA256: 026cd2daf91782d62f72910ab9d7efeee98b4f7eab950e4048c6a702ba45d503

```md
## Why

只读网关只能满足查询和浏览需求。用户要求完整支持写入、DDL、prepared statement、权限、SSL 和 Java MySQL JDBC Driver / BI 工具高级兼容，需要在只读网关基础上扩展高级协议能力。

## What Changes

- 扩展 MySQL 协议网关支持写入和 DDL。
- 支持 prepared statement 常见路径。
- 支持权限、SSL 或可配置安全策略。
- 增强 Java MySQL JDBC Driver、BI 工具和 GUI 工具的 metadata 兼容。

## Capabilities

### New Capabilities
- `nebula-mysql-protocol-write-and-advanced-support`: MySQL 协议写入与高级兼容，覆盖本 change 的需求、边界和验收场景。

### Modified Capabilities

## Impact


- MySQL binlog、复制协议、存储过程和触发器不在本 change 内实现；这些能力由后续独立 changes 承接。
- 依赖只读 MySQL 协议网关和 SQL 写入/DDL 支持。
- 可能影响 boot starter/autoconfigure、部署配置和安全配置。
- 需要更完整的协议测试和客户端兼容矩阵。

```

## openspec/changes/add-nebula-mysql-protocol-write-and-advanced-support/design.md

- Source: openspec/changes/add-nebula-mysql-protocol-write-and-advanced-support/design.md
- Lines: 1-47
- SHA256: 72f5d6abcc3d7283467f8529f952b9c6b87ef459c8a6ddd1b22d7818546ab242

```md
## Context

本 change 属于 Nebula SQL / MySQL 协议支持路线的一部分。当前 `fw-plus-nebula-jdbc` 已具备稳定的 nGQL JDBC 基础能力，但完整 SQL 与 MySQL 协议能力需要分阶段建设。

前置依赖：依赖前置 MySQL 只读网关和 SQL 写入/DDL 支持。

## Goals / Non-Goals

**Goals:**

- 扩展 MySQL 协议网关支持写入和 DDL。
- 支持 prepared statement 常见路径。
- 支持权限、SSL 或可配置安全策略。
- 增强 Java MySQL JDBC Driver、BI 工具和 GUI 工具的 metadata 兼容。

**Non-Goals:**


- MySQL binlog、复制协议、存储过程和触发器不在本 change 内实现；这些能力由后续独立 changes 承接。
- 不扩大到本拆分项之外的后续能力。
- 不破坏现有 `jdbc:nebula://...` nGQL 直连路径。
- 不追求 100% MySQL Server 或 SQL 标准兼容。

## Decisions

- 高级协议能力必须建立在只读网关稳定后，不与基础协议混在同一阶段。
- prepared statement 支持优先覆盖 Java MySQL JDBC Driver 常见文本/二进制路径。
- 权限与 SSL 先做可配置策略，不实现完整 MySQL 用户系统。

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

## openspec/changes/add-nebula-mysql-protocol-write-and-advanced-support/tasks.md

- Source: openspec/changes/add-nebula-mysql-protocol-write-and-advanced-support/tasks.md
- Lines: 1-17
- SHA256: f4ac163ee38356389c5e99d65c8adf1cf6ed03d48f7a0a02a9210c1d63ffc373

```md
## 1. 高级协议设计

- [ ] 1.1 定义写入、DDL、prepared statement、权限、SSL 的支持边界。
- [ ] 1.2 定义客户端兼容矩阵和元数据响应策略。

## 2. 协议扩展实现

- [ ] 2.1 实现 MySQL 协议写入和 DDL 执行路径。
- [ ] 2.2 实现 prepared statement 常见路径。
- [ ] 2.3 实现认证/权限/SSL 配置策略。
- [ ] 2.4 增强 metadata 查询兼容。

## 3. 兼容验证

- [ ] 3.1 使用 Java MySQL JDBC Driver 验证查询和写入。
- [ ] 3.2 使用至少两个 GUI/BI 客户端验证元数据浏览和查询。
- [ ] 3.3 补充安全失败和不支持协议路径测试。

```

## openspec/changes/add-nebula-mysql-protocol-write-and-advanced-support/specs/nebula-mysql-protocol-write-and-advanced-support/spec.md

- Source: openspec/changes/add-nebula-mysql-protocol-write-and-advanced-support/specs/nebula-mysql-protocol-write-and-advanced-support/spec.md
- Lines: 1-35
- SHA256: a5cc60670f701479afbbee4b22f657a9dbc90238dc827ef8c73376904fe65b05

```md
## ADDED Requirements

### Requirement: MySQL protocol gateway supports writes and DDL
系统 SHALL 在 MySQL 协议网关中支持受控 SQL 写入和 DDL，并通过 SQL→nGQL 写入能力执行。

#### Scenario: 协议写入
- **WHEN** 客户端通过 MySQL 协议执行 INSERT/UPDATE/DELETE 
- **THEN** 网关完成转换并返回执行结果。

#### Scenario: 协议 DDL
- **WHEN** 客户端通过 MySQL 协议执行受支持 DDL 
- **THEN** 网关返回 MySQL 风格成功或错误响应。

### Requirement: Prepared statement common paths are supported
系统 SHALL 支持 Java MySQL JDBC Driver 常见 prepared statement 路径。

#### Scenario: Prepared SELECT
- **WHEN** Java MySQL JDBC Driver 使用 PreparedStatement 执行 SELECT 
- **THEN** 参数绑定后可查询。

#### Scenario: Prepared write
- **WHEN** PreparedStatement 执行受支持写入
- **THEN** 可完成参数绑定、转换和执行。

### Requirement: Advanced client compatibility is configurable
系统 SHALL 提供权限、SSL 或安全策略配置，并增强 GUI/BI 工具元数据探测兼容。

#### Scenario: 权限或认证失败
- **WHEN** 凭据或权限不满足
- **THEN** 客户端收到明确 MySQL 协议错误。

#### Scenario: 元数据探测
- **WHEN** BI 或 GUI 工具发起常见 metadata 查询
- **THEN** 网关返回兼容响应或明确不支持错误。


```
