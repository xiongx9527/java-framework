# Comet Design Handoff

- Change: add-nebula-mysql-trigger-subset
- Phase: design
- Mode: compact
- Context hash: 68ca36c332cecdb9f2732208fa568fc04e6d12aaa909dd1381e2905d488fc080

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/add-nebula-mysql-trigger-subset/proposal.md

- Source: openspec/changes/add-nebula-mysql-trigger-subset/proposal.md
- Lines: 1-23
- SHA256: e00fb0cd2af2cac4ad97ef59f879e2b1c663242fa9b96fc545edd2633d218555

```md
## Why

用户要求支持触发器。由于只有经网关的写入可被网关可靠观察，需要提供网关侧 trigger 子集，并明确不捕获绕过网关的原生 Nebula 写入。

## What Changes

- 支持 CREATE TRIGGER 和 DROP TRIGGER 的基础元数据。
- 支持对经网关执行的 INSERT、UPDATE、DELETE 触发 BEFORE/AFTER 逻辑。
- trigger body 内 SQL 复用 SQL→nGQL 写入能力。
- 提供递归触发保护和明确边界文档。

## Capabilities

### New Capabilities
- `nebula-mysql-trigger-subset`: MySQL 触发器子集，覆盖本 change 的需求、边界和验收场景。

### Modified Capabilities

## Impact

- 依赖 SQL 写入/DDL、MySQL 协议高级支持和 routine/元数据能力的一部分。
- 影响网关写入执行链路。
- 需要严格处理失败策略和递归触发风险。

```

## openspec/changes/add-nebula-mysql-trigger-subset/design.md

- Source: openspec/changes/add-nebula-mysql-trigger-subset/design.md
- Lines: 1-43
- SHA256: 20ecfc949b324e1acbdf109e7333935fb86cea986a077664f2908a4bb34e3937

```md
## Context

本 change 属于 Nebula MySQL 协议深度兼容路线。前置能力包括 SQL parser、SQL→nGQL 查询/写入转换、MySQL 协议网关基础能力。由于本 change 触及 MySQL 高级语义，必须明确兼容边界，避免承诺完整 MySQL Server 等价。

## Goals / Non-Goals

**Goals:**

- 支持 CREATE TRIGGER 和 DROP TRIGGER 的基础元数据。
- 支持对经网关执行的 INSERT、UPDATE、DELETE 触发 BEFORE/AFTER 逻辑。
- trigger body 内 SQL 复用 SQL→nGQL 写入能力。
- 提供递归触发保护和明确边界文档。

**Non-Goals:**

- 不承诺 100% MySQL Server 全量兼容。
- 不捕获或伪造无法可靠观察的 Nebula 原生变更。
- 不破坏现有 nGQL JDBC 直连和基础 MySQL 网关能力。

## Decisions

- trigger 子集只对网关可观察写入生效。
- BEFORE/AFTER 语义以网关执行顺序定义，不承诺完整 MySQL 事务回滚等价。
- 必须实现递归保护和失败策略，避免触发器造成无限写入。

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

## openspec/changes/add-nebula-mysql-trigger-subset/tasks.md

- Source: openspec/changes/add-nebula-mysql-trigger-subset/tasks.md
- Lines: 1-15
- SHA256: 79e9fa410858fc7ecc73fbd3e8fa35b182d19b5782308b6e6427db44aac8d81a

```md
## 1. Trigger 子集设计

- [ ] 1.1 定义支持的 CREATE/DROP TRIGGER 语法和 BEFORE/AFTER 范围。
- [ ] 1.2 定义触发器元数据、执行顺序、失败策略和递归限制。

## 2. Trigger 实现

- [ ] 2.1 实现 trigger 定义保存和删除。
- [ ] 2.2 在 INSERT/UPDATE/DELETE 网关写入路径触发 BEFORE/AFTER 逻辑。
- [ ] 2.3 实现递归保护和不支持语法错误。

## 3. 验证

- [ ] 3.1 补充 INSERT/UPDATE/DELETE 触发测试。
- [ ] 3.2 补充绕过网关边界、递归限制和失败策略测试。

```

## openspec/changes/add-nebula-mysql-trigger-subset/specs/nebula-mysql-trigger-subset/spec.md

- Source: openspec/changes/add-nebula-mysql-trigger-subset/specs/nebula-mysql-trigger-subset/spec.md
- Lines: 1-50
- SHA256: e9c2d39d4c8ba442395df27c9c8303b5498c1c97def8f6580713c98565974820

```md
## ADDED Requirements

### Requirement: Gateway writes can fire supported triggers
系统 SHALL 对经网关执行的受支持 INSERT、UPDATE、DELETE 触发已定义的 BEFORE/AFTER trigger。

#### Scenario: INSERT 触发
- **WHEN** 经网关执行 INSERT 
- **THEN** 匹配 trigger 被执行。

#### Scenario: UPDATE/DELETE 触发
- **WHEN** 经网关执行 UPDATE 或 DELETE 
- **THEN** 匹配 trigger 被执行。

#### Scenario: BEFORE trigger 失败
- **WHEN** BEFORE trigger 执行失败
- **THEN** 系统不执行主写入并返回明确错误。

#### Scenario: AFTER trigger 执行条件
- **WHEN** 主写入执行失败
- **THEN** 系统不执行 AFTER trigger。

### Requirement: Trigger definitions are managed as gateway metadata
系统 SHALL 支持创建和删除 trigger 定义，并将其作为网关元数据管理。

#### Scenario: 创建触发器
- **WHEN** 客户端执行受支持 CREATE TRIGGER 
- **THEN** 系统保存 trigger 定义。

#### Scenario: 删除触发器
- **WHEN** 客户端执行 DROP TRIGGER 
- **THEN** 系统移除 trigger 定义。

#### Scenario: 删除不存在的触发器
- **WHEN** 客户端执行 DROP TRIGGER 删除不存在的 trigger
- **THEN** 系统按明确错误或 IF EXISTS 幂等语义响应。

### Requirement: Trigger boundaries and recursion protection are explicit
系统 SHALL 明确 trigger 只对经网关写入生效，并限制递归触发。

#### Scenario: 绕过网关写入
- **WHEN** 原生 Nebula 写入不会触发网关 trigger，系统文档明确该边界。
- **THEN** 系统产生符合要求的结果

#### Scenario: 递归触发
- **WHEN** trigger body 导致递归触发超过限制
- **THEN** 系统中止并返回明确错误。

#### Scenario: 不支持的 trigger body
- **WHEN** trigger body 包含不支持语法
- **THEN** 系统拒绝创建或执行并返回明确错误。

```
