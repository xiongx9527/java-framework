# Brainstorm Summary

- Change: add-nebula-mysql-binlog-compatible-stream
- Date: 2026-07-17

## 确认的技术方案

采用“网关事件日志 + 稳定 position + 可插拔持久化”的方案。binlog-compatible stream 位于 MySQL 网关写入/DDL 执行成功边界之后，消费写入/DDL change 输出的 normalized mutation descriptor，生成内部稳定的 `NebulaBinlogEvent`，再通过 `BinlogEventStore` 追加并分配 `BinlogPosition`。

复制协议子集不生成事件，只消费 `BinlogEventReader`。该边界把 CDC/审计事件源和 MySQL replication protocol 适配分开，便于多个分区并行。

## 关键取舍与风险

- 不承诺完整 MySQL binlog、GTID、row image 或真实主从语义，只提供 binlog-compatible event stream。
- 只捕获经 MySQL 网关成功执行的写入和受支持 DDL，绕过网关的 Nebula 原生写入不产生事件。
- 默认策略为事件追加失败则写入不对外报告成功，避免静默丢失 CDC/审计事件。
- 不提供跨 Nebula 执行与事件存储的分布式事务，需在文档和错误语义中固定一致性边界。

## 测试策略

- 事件模型测试覆盖 DML、DDL、position、序列化和边界字段。
- store 测试覆盖 append、readAfter、currentPosition、过期 position 和并发顺序。
- 写入路径集成测试使用 fake executor 或 gateway fixture，验证成功写入生成事件、失败执行不生成事件。
- 边界测试验证绕过网关写入不会产生虚假事件。
- 消费测试验证从指定 position 读取和持续轮询读取。

## Spec Patch

无。本轮设计不修改 OpenSpec delta spec。
