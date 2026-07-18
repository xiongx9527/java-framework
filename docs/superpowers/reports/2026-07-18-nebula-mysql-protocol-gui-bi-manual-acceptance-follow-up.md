# Nebula MySQL 协议 GUI/BI 手动验收 Follow-up

Change: `add-nebula-mysql-protocol-write-and-advanced-support`

## 背景

当前 worktree 环境未发现可运行的 DBeaver、DataGrip、Tableau、Power BI、Metabase、DbVisualizer、SQuirreL、SQL Workbench 或 MySQL Workbench 客户端应用/命令，也未发现对应运行进程。因此，本 change 不声明真实 GUI/BI 客户端验收已完成。

本 change 已通过自动化测试覆盖可重复证明的范围：

- Connector/J 8.0.33 真实 socket 路径：连接、初始化 SQL、查询、写入和客户端侧 prepared write。
- DBeaver/DataGrip/BI 常见 metadata SQL fixture：`SELECT DATABASE()`、`SHOW VARIABLES`、`SHOW TABLES`、`DESCRIBE`、`information_schema.tables`、`information_schema.columns`。
- protocol/security 路径：权限拒绝、SSL required、事务不支持和 unsupported command。

## 后续验收范围

在安装了至少两个真实 GUI/BI 客户端的环境中，补充以下人工验收：

1. 使用客户端连接本地 Nebula MySQL gateway。
2. 验证连接握手、认证失败和权限失败提示是否稳定。
3. 浏览 database/schema/table metadata。
4. 展开表字段，确认列名、类型和空 metadata 响应可被客户端接受。
5. 执行 `SELECT` 查询并确认结果集展示正常。
6. 执行一条受支持写入语句，确认客户端展示 affected rows 或成功状态。
7. 记录客户端名称、版本、连接参数、执行 SQL、观察结果、日志和失败截图。

## 建议客户端组合

- DBeaver + DataGrip。
- 或 DBeaver + Metabase / Tableau / Power BI / DbVisualizer / SQuirreL / MySQL Workbench 中任意一个。

## 处理方式

该 follow-up 可以作为后续独立 change 或 Manual Acceptance 记录执行。完成前，不得把真实 GUI/BI 客户端验收写成已通过。
