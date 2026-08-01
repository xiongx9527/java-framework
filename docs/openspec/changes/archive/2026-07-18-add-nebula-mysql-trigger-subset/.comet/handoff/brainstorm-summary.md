# Brainstorm Summary

- Change: add-nebula-mysql-trigger-subset
- Date: 2026-07-17

## 确认的技术方案

采用网关写入执行链路的 trigger-aware executor 方案。CREATE TRIGGER / DROP TRIGGER 只作为 MySQL 网关元数据 DDL 处理；INSERT、UPDATE、DELETE 经网关进入写入执行端口时，基于解析后的写入计划确定目标表与事件类型，再按 `(schema, table, event, timing)` 查找触发器。BEFORE trigger 在主写入前执行，AFTER trigger 仅在主写入成功后执行。trigger body 作为受控 SQL 片段复用 SQL 到 nGQL 写入能力，并通过 `TriggerExecutionContext` 记录触发链、递归深度和当前事件。

该方案不修改现有 nGQL JDBC 直连路径，也不尝试捕获绕过 MySQL 网关的原生 Nebula 写入。OLD/NEW 上下文在第一阶段按能力受限处理：INSERT 支持 NEW，DELETE 支持 OLD，UPDATE 支持 OLD 和 NEW；如果前置写入计划无法安全提供行前/行后数据，则只支持不依赖 OLD/NEW 的 trigger body，并返回明确不支持错误。

## 关键取舍与风险

- 把 trigger 放在网关写入层，而不是底层 `NebulaSqlExecutor`。这样牺牲对原生 nGQL 写入的覆盖，但符合 OpenSpec 的可靠观察边界。
- 不承诺完整 MySQL 事务回滚等价。BEFORE 失败阻止主写入；主写入失败不执行 AFTER；AFTER 失败返回错误并记录主写入已发生的边界。
- trigger body 再次执行写入可能导致递归或环路。实现必须维护触发链、最大深度和明确错误，默认最大深度建议为 8，可配置。
- OLD/NEW 行上下文与图模型映射存在天然差异。第一阶段只支持可由写入计划或受控查询安全获得的上下文，不做关系数据库级行镜像承诺。

## 测试策略

- DDL 与元数据：CREATE TRIGGER、DROP TRIGGER、重复名称、不存在 trigger、按 schema/table 隔离和持久化恢复。
- 执行顺序：INSERT/UPDATE/DELETE 分别覆盖 BEFORE、AFTER、无匹配 trigger、多 trigger 顺序。
- 失败策略：BEFORE 失败阻止主写入；主写入失败不执行 AFTER；AFTER 失败返回错误并暴露主写入已发生的边界。
- 递归保护：同表递归和互相触发达到深度限制后中止并返回明确错误。
- 边界：绕过 MySQL 网关的 `NebulaConnection.execute(nGQL)` 不触发 trigger；nGQL JDBC 直连路径保持不变。

## Spec Patch

- 补充 BEFORE trigger 失败时主写入不执行的验收场景。
- 补充 AFTER trigger 仅在主写入成功后执行的验收场景。
- 补充 DROP 不存在 trigger 的明确错误或 `IF EXISTS` 语义场景。
- 补充 trigger body 中不支持语法返回明确错误的场景。
