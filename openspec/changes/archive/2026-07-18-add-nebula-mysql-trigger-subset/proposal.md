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
