# Comet Design Handoff

- Change: add-nebula-mysql-routine-subset
- Phase: design
- Mode: compact
- Context hash: 7c25fc7a926ccddbe2fa9a00aba960a6975d2a3b06a690be0c45bec7ffc012f4

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/add-nebula-mysql-routine-subset/proposal.md

- Source: openspec/changes/add-nebula-mysql-routine-subset/proposal.md
- Lines: 1-24
- SHA256: 20b61b1d62edc826ea42926dc57cb9276242e267a89281493794331598600f4a

```md
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

```

## openspec/changes/add-nebula-mysql-routine-subset/design.md

- Source: openspec/changes/add-nebula-mysql-routine-subset/design.md
- Lines: 1-44
- SHA256: e3a43d3d6a199cbdbc78ee45f3de378d0ca5341ccc4852e8d684f78f17b0ad41

```md
## Context

本 change 属于 Nebula MySQL 协议深度兼容路线。前置能力包括 SQL parser、SQL→nGQL 查询/写入转换、MySQL 协议网关基础能力。由于本 change 触及 MySQL 高级语义，必须明确兼容边界，避免承诺完整 MySQL Server 等价。

## Goals / Non-Goals

**Goals:**

- 支持 CREATE PROCEDURE、DROP PROCEDURE、CALL 的基础路径。
- 保存 routine 定义到网关元数据。
- 支持参数、顺序 SQL、简单变量和简单条件。
- 过程体内 SQL 复用 SQL→nGQL 查询/写入能力。
- 对游标、handler、动态 SQL、复杂事务等返回明确错误。

**Non-Goals:**

- 不承诺 100% MySQL Server 全量兼容。
- 不捕获或伪造无法可靠观察的 Nebula 原生变更。
- 不破坏现有 nGQL JDBC 直连和基础 MySQL 网关能力。

## Decisions

- routine 子集以网关解释器实现，不尝试模拟完整 MySQL 存储过程引擎。
- 过程定义作为网关元数据管理，需考虑迁移和备份。
- 执行过程必须有超时和递归/循环保护。

## Risks / Trade-offs

- **[Risk] MySQL 高级语义与 NebulaGraph 原生能力不等价。** → Mitigation：以子集和兼容层方式实现，所有边界写入 spec 和错误响应。
- **[Risk] 客户端可能假设完整 MySQL 行为。** → Mitigation：建立兼容矩阵，不支持能力返回明确错误。
- **[Risk] 事件、routine 或 trigger 可能引入一致性风险。** → Mitigation：只对网关可观察路径承诺，并提供持久化、幂等、递归保护和失败策略。

## Migration Plan

1. 先完成前置 SQL 与 MySQL 基础协议 changes。
2. 在本 change 中设计协议/语义子集和测试矩阵。
3. 以开关或独立模块形式启用高级能力。
4. 如出现兼容性问题，可关闭该能力而不影响基础查询网关。

## Open Questions

- 目标 CDC、复制、BI 或客户端工具的优先级是什么？
- 事件日志和元数据应使用本地存储、数据库还是外部存储？
- 是否需要 Spring Boot starter 或 Magic-API 插件暴露配置？

```

## openspec/changes/add-nebula-mysql-routine-subset/tasks.md

- Source: openspec/changes/add-nebula-mysql-routine-subset/tasks.md
- Lines: 1-15
- SHA256: 22d08df0abdb5ff69823509e3968bc6d69c55b0251ce08c200f34931b18d76a6

```md
## 1. Routine 子集设计

- [ ] 1.1 定义支持的 CREATE/CALL/DROP 语法和过程体语言子集。
- [ ] 1.2 定义 routine 元数据存储、权限和执行上下文。

## 2. Routine 实现

- [ ] 2.1 实现过程定义保存和删除。
- [ ] 2.2 实现 CALL 解释执行参数、变量、简单条件和顺序 SQL。
- [ ] 2.3 实现不支持特性的明确错误和执行保护。

## 3. 验证

- [ ] 3.1 补充创建/调用/删除过程测试。
- [ ] 3.2 补充复杂语法拒绝、超时和权限测试。

```

## openspec/changes/add-nebula-mysql-routine-subset/specs/nebula-mysql-routine-subset/spec.md

- Source: openspec/changes/add-nebula-mysql-routine-subset/specs/nebula-mysql-routine-subset/spec.md
- Lines: 1-50
- SHA256: 2f02b2bb33b8fc46d4c8a34a0ac02a6a923c2f465b2523f2ddd36f67811fa7fa

```md
## ADDED Requirements

### Requirement: Simple routines can be created and called
系统 SHALL 支持有限 MySQL 存储过程/函数子集的创建、删除和调用。

#### Scenario: 创建过程
- **WHEN** 客户端执行受支持 CREATE PROCEDURE 
- **THEN** 系统保存过程定义。

#### Scenario: 删除过程
- **WHEN** 客户端执行 DROP PROCEDURE 删除已保存过程
- **THEN** 系统删除该过程定义，后续 CALL 返回过程不存在错误。

#### Scenario: 调用过程
- **WHEN** 客户端执行 CALL 
- **THEN** 系统按定义执行受支持过程体。

#### Scenario: 调用未知过程
- **WHEN** 客户端执行 CALL 调用不存在的过程
- **THEN** 系统返回明确的过程不存在错误。

### Requirement: Routine bodies execute supported SQL sequentially
系统 SHALL 支持参数、简单变量、简单条件和顺序 SQL 执行，过程体 SQL 复用现有 SQL→nGQL 能力。

#### Scenario: 顺序执行
- **WHEN** 过程体包含多条受支持 SQL 
- **THEN** 系统按顺序执行并返回结果。

#### Scenario: 参数和简单变量
- **WHEN** 过程体使用 IN 参数、局部变量和 SET 赋值
- **THEN** 系统按调用参数和变量当前值执行过程体 SQL。

#### Scenario: 简单条件
- **WHEN** 过程体包含受支持 IF/ELSEIF/ELSE 条件
- **THEN** 系统只执行命中的分支。

#### Scenario: 执行保护
- **WHEN** 过程调用超过配置的超时、语句数量、嵌套深度或递归限制
- **THEN** 系统中止调用并返回明确的执行保护错误。

### Requirement: Unsupported routine language features fail explicitly
系统 SHALL 对复杂游标、handler、动态 SQL、临时表或复杂事务语义返回明确不支持错误。

#### Scenario: 复杂过程体
- **WHEN** 过程体包含游标或 handler 
- **THEN** 系统拒绝创建或执行并说明原因。

#### Scenario: 非目标 routine 语义
- **WHEN** 客户端使用 CREATE FUNCTION、OUT/INOUT 参数、动态 SQL、临时表、事务控制或循环语句
- **THEN** 系统返回明确不支持错误。

```
