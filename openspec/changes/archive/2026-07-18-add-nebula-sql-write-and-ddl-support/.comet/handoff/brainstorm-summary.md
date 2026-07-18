# Brainstorm Summary

- Change: add-nebula-sql-write-and-ddl-support
- Date: 2026-07-17

## 确认的技术方案

采用“独立 SQL write/DDL converter + 执行端口 + mutation descriptor”的方案。写入和 DDL 转换复用 parser core 的稳定 IR，并复用查询转换阶段形成的 schema mapping 模型，输出 `NebulaMutationPlan` / `NebulaDdlPlan`，再通过统一 `NebulaSqlMutationExecutor` 调用现有 Nebula 执行链路。

执行结果必须包含 `mutationDescriptor` 或 `ddlDescriptor`，供 MySQL 网关、binlog stream、routine 和 trigger 分区复用。

## 关键取舍与风险

- 不模拟 MySQL 事务、锁、回滚或完整 DDL 语义。
- 默认拒绝缺少 id/src/dst/rank 或无 WHERE 的危险 UPDATE/DELETE。
- 不支持复杂 ALTER、索引、外键、MySQL engine/charset/collation 等专有 DDL 选项。
- 不允许 raw SQL fallback；不能安全映射时返回结构化错误。

## 测试策略

- converter golden 测试覆盖 INSERT、UPDATE、DELETE、CREATE/DROP/DESCRIBE。
- mapping 测试覆盖 tag、edge、缺少关键列、未映射列和类型不兼容。
- executor 测试使用 fake Nebula executor 验证 plan 执行和 descriptor 输出。
- JDBC 路径测试覆盖 executeUpdate 和错误语义。
- 边界测试覆盖事务、复杂 DDL、无 WHERE UPDATE/DELETE、`INSERT ... SELECT`。

## Spec Patch

无。本轮设计不修改 OpenSpec delta spec。
