# Comet Design Handoff

- Change: add-nebula-mysql-replication-protocol-subset
- Phase: design
- Mode: compact
- Context hash: c3015bd89794c4598ddaf06a19438f020086f114a859ca5a07d869bd616890e1

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/add-nebula-mysql-replication-protocol-subset/proposal.md

- Source: openspec/changes/add-nebula-mysql-replication-protocol-subset/proposal.md
- Lines: 1-23
- SHA256: 3241d24278d367be02f1aca494a00279ee3e6743038a41d3891d00252ed02683

```md
## Why

在具备 binlog-compatible 事件流后，部分 CDC 或同步客户端希望通过 MySQL 复制协议读取事件。需要支持可控的复制协议子集，而不是承诺完整 MySQL 主从复制。

## What Changes

- 新增 MySQL 复制协议子集支持。
- 支持基础复制握手和从指定 position 拉取事件。
- 支持 `COM_BINLOG_DUMP` 或等价子集。
- 对 GTID、完整 slave 兼容等不支持能力返回明确错误。

## Capabilities

### New Capabilities
- `nebula-mysql-replication-protocol-subset`: MySQL 复制协议子集，覆盖本 change 的需求、边界和验收场景。

### Modified Capabilities

## Impact

- 依赖 binlog-compatible stream。
- 影响 MySQL 协议网关连接类型和命令分发。
- 可能需要针对 CDC 客户端做兼容测试。

```

## openspec/changes/add-nebula-mysql-replication-protocol-subset/design.md

- Source: openspec/changes/add-nebula-mysql-replication-protocol-subset/design.md
- Lines: 1-43
- SHA256: f86ec9ff9a88af88a720fad6cc051a377e89543446f8aeda5062962acdf97523

```md
## Context

本 change 属于 Nebula MySQL 协议深度兼容路线。前置能力包括 SQL parser、SQL→nGQL 查询/写入转换、MySQL 协议网关基础能力。由于本 change 触及 MySQL 高级语义，必须明确兼容边界，避免承诺完整 MySQL Server 等价。

## Goals / Non-Goals

**Goals:**

- 新增 MySQL 复制协议子集支持。
- 支持基础复制握手和从指定 position 拉取事件。
- 支持 `COM_BINLOG_DUMP` 或等价子集。
- 对 GTID、完整 slave 兼容等不支持能力返回明确错误。

**Non-Goals:**

- 不承诺 100% MySQL Server 全量兼容。
- 不捕获或伪造无法可靠观察的 Nebula 原生变更。
- 不破坏现有 nGQL JDBC 直连和基础 MySQL 网关能力。

## Decisions

- 复制协议只做 CDC/同步可用子集，不承诺完整 MySQL slave 接入。
- 协议层不生成事件，只消费 binlog-compatible stream。
- 所有不支持命令必须明确返回错误，避免客户端误判一致性。

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

## openspec/changes/add-nebula-mysql-replication-protocol-subset/tasks.md

- Source: openspec/changes/add-nebula-mysql-replication-protocol-subset/tasks.md
- Lines: 1-15
- SHA256: f3387b065a19fd02f9e96fc201d24ebb54778347886456381dd9a6fbb11b7946

```md
## 1. 协议子集设计

- [ ] 1.1 定义支持的复制命令、状态机和错误码。
- [ ] 1.2 定义 position 与 binlog-compatible stream 的映射。

## 2. 协议实现

- [ ] 2.1 实现基础复制握手/注册上下文。
- [ ] 2.2 实现 COM_BINLOG_DUMP 或等价读取命令。
- [ ] 2.3 实现不支持复制能力的明确错误。

## 3. 兼容验证

- [ ] 3.1 使用至少一个 CDC/复制客户端验证事件读取。
- [ ] 3.2 补充 position、断连续读和不支持命令测试。

```

## openspec/changes/add-nebula-mysql-replication-protocol-subset/specs/nebula-mysql-replication-protocol-subset/spec.md

- Source: openspec/changes/add-nebula-mysql-replication-protocol-subset/specs/nebula-mysql-replication-protocol-subset/spec.md
- Lines: 1-38
- SHA256: efe8894e96add3f3d8d113416e9791ea1b22c1c8af8c501919c21844ea2304ca

```md
## ADDED Requirements

### Requirement: Clients can request event streaming through replication commands
系统 SHALL 支持 MySQL 复制协议的受控子集，使客户端可以通过复制命令读取网关侧事件。

#### Scenario: 注册或握手
- **WHEN** 客户端发起复制相关握手或注册
- **THEN** 系统建立事件读取上下文。

#### Scenario: 请求 binlog dump
- **WHEN** 客户端请求从指定 position 读取
- **THEN** 系统返回对应事件流。

### Requirement: Unsupported replication features fail explicitly
系统 SHALL 对完整 GTID 复制、真实 MySQL slave 语义或其他不支持命令返回明确错误。

#### Scenario: GTID 请求
- **WHEN** 客户端请求未支持的 GTID 复制能力
- **THEN** 系统返回明确不支持错误。

#### Scenario: 未知复制命令
- **WHEN** 客户端发送本子集未定义的复制命令
- **THEN** 系统返回明确不支持错误，且不进入普通查询或写入执行链路。

### Requirement: Replication stream uses gateway event positions
复制协议输出 SHALL 基于 binlog-compatible stream 的 position，不伪造全局 Nebula 变更顺序。

#### Scenario: position 对齐
- **WHEN** 客户端从已知 position 继续读取
- **THEN** 收到该 position 后的网关事件。

#### Scenario: position 不可用
- **WHEN** 客户端请求的 position 已过期、格式非法或不在可读取范围内
- **THEN** 系统返回明确 position 不可用错误，且不得静默跳到最新 position。

#### Scenario: 事件来源边界
- **WHEN** binlog-compatible stream 中不存在某个 Nebula 原生变更事件
- **THEN** 复制协议输出不得生成、补偿或伪造该事件。

```
