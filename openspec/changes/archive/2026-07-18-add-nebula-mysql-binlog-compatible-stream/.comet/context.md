# Comet Design Handoff

- Change: add-nebula-mysql-binlog-compatible-stream
- Phase: design
- Mode: compact
- Context hash: 0e99d9ac49c909709b12465c46e4988cd9ba873e049ab7ed12be5c117133f604

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/add-nebula-mysql-binlog-compatible-stream/proposal.md

- Source: openspec/changes/add-nebula-mysql-binlog-compatible-stream/proposal.md
- Lines: 1-23
- SHA256: 9215d679f4a3972fe988b4208169250cdfc2d27995f20ebf45812256c8289a71

```md
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

```

## openspec/changes/add-nebula-mysql-binlog-compatible-stream/design.md

- Source: openspec/changes/add-nebula-mysql-binlog-compatible-stream/design.md
- Lines: 1-43
- SHA256: 5f133829889a72e4a216e75a136a08fd2394e6d60d6ec4efd6ea30a91594fa8d

```md
## Context

本 change 属于 Nebula MySQL 协议深度兼容路线。前置能力包括 SQL parser、SQL→nGQL 查询/写入转换、MySQL 协议网关基础能力。由于本 change 触及 MySQL 高级语义，必须明确兼容边界，避免承诺完整 MySQL Server 等价。

## Goals / Non-Goals

**Goals:**

- 新增网关侧写入/DDL 事件日志模型。
- 将经网关执行的 INSERT、UPDATE、DELETE 和受支持 DDL 记录为 binlog-compatible event。
- 提供基础 position、event stream 和消费 API。
- 明确绕过网关直接写入 NebulaGraph 的变更不在捕获范围内。

**Non-Goals:**

- 不承诺 100% MySQL Server 全量兼容。
- 不捕获或伪造无法可靠观察的 Nebula 原生变更。
- 不破坏现有 nGQL JDBC 直连和基础 MySQL 网关能力。

## Decisions

- 将能力命名为 binlog-compatible stream，而不是完整 MySQL binlog，以避免承诺 GTID、row image、mixed mode 等完整 MySQL 语义。
- 事件生成放在网关写入执行路径之后，仅记录确认成功或明确可判定结果的操作。
- position 模型先内部稳定，再适配复制协议子集。

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

## openspec/changes/add-nebula-mysql-binlog-compatible-stream/tasks.md

- Source: openspec/changes/add-nebula-mysql-binlog-compatible-stream/tasks.md
- Lines: 1-15
- SHA256: e4b02615292538fa669e70212fe840f2b4abde112a63beea6061a5d2abf24d1a

```md
## 1. 事件模型设计

- [ ] 1.1 定义 binlog-compatible event 格式、position 模型和持久化策略。
- [ ] 1.2 定义 DML、DDL、错误和边界事件的表达方式。

## 2. 事件生成与消费

- [ ] 2.1 在网关写入路径生成 DML 事件。
- [ ] 2.2 在受支持 DDL 路径生成 schema 事件。
- [ ] 2.3 实现按 position 读取事件流。

## 3. 验证

- [ ] 3.1 补充写入/DDL 事件生成测试。
- [ ] 3.2 补充 position 消费和绕过网关边界测试。

```

## openspec/changes/add-nebula-mysql-binlog-compatible-stream/specs/nebula-mysql-binlog-compatible-stream/spec.md

- Source: openspec/changes/add-nebula-mysql-binlog-compatible-stream/specs/nebula-mysql-binlog-compatible-stream/spec.md
- Lines: 1-31
- SHA256: 0fad6487db85b3ae639e8c2f4c196c07199c00905fdb0fee0316a5e9f28b1275

```md
## ADDED Requirements

### Requirement: Gateway writes produce binlog-compatible events
系统 SHALL 为经 MySQL 协议网关成功执行的写入和 DDL 生成 binlog-compatible 事件。

#### Scenario: 写入事件生成
- **WHEN** 经网关执行 INSERT、UPDATE 或 DELETE 成功后，事件流中出现对应变更事件。
- **THEN** 系统产生符合要求的结果

#### Scenario: DDL 事件生成
- **WHEN** 经网关执行受支持 DDL 成功后，事件流中出现对应 schema 事件。
- **THEN** 系统产生符合要求的结果

### Requirement: Consumers can read events by position
系统 SHALL 提供可按 position 消费的事件流，使 CDC 工具可以持续读取网关侧事件。

#### Scenario: 从指定位置读取
- **WHEN** 消费者给定已有 position 
- **THEN** 系统从该位置之后返回事件。

#### Scenario: 持续读取
- **WHEN** 消费者持续订阅
- **THEN** 系统在新事件产生后可继续推送或拉取。

### Requirement: Capture boundary is explicit
系统 SHALL 明确只捕获经网关执行的写入，不声称捕获绕过网关直接写入 NebulaGraph 的变更。

#### Scenario: 绕过网关写入
- **WHEN** 当数据由 Nebula 原生客户端直接写入
- **THEN** binlog-compatible stream 不产生虚假事件，并在文档中说明边界。


```
