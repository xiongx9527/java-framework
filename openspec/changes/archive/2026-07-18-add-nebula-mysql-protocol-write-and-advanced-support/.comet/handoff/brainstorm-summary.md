# Brainstorm Summary

- Change: add-nebula-mysql-protocol-write-and-advanced-support
- Date: 2026-07-17

## 确认的技术方案

采用“扩展只读网关 dispatcher + 稳定执行端口 + 客户端兼容层”的方案。在只读 MySQL 网关基础上扩展 `COM_QUERY` 写入/DDL、`COM_STMT_PREPARE`、`COM_STMT_EXECUTE`、`COM_STMT_CLOSE`、权限/SSL 策略和 metadata 兼容响应。

协议层只处理 wire protocol、session、参数绑定、权限检查和错误映射。SQL 写入/DDL 语义由 `add-nebula-sql-write-and-ddl-support` 提供的执行端口负责。

## 关键取舍与风险

- 不实现完整 MySQL Server、完整用户系统、事务、锁、binlog、复制协议、routine 或 trigger。
- prepared statement 优先覆盖 Java MySQL JDBC Driver 常见路径，不承诺 server-side cursor、多结果集或完整 binary protocol 高级语义。
- 权限与 SSL 采用可配置策略，不模拟 MySQL 授权表。
- metadata 兼容通过 profile 固定，不扩大 SQL 语义边界。

## 测试策略

- 协议测试覆盖 dispatcher、prepared lifecycle、OK/ERR/resultset packet。
- 参数绑定测试覆盖常见 Java 类型和 null。
- handler 测试使用 fake query/mutation executor。
- 权限/SSL 测试覆盖拒绝路径。
- metadata 兼容测试覆盖 Java MySQL JDBC Driver 和 GUI/BI fixture。
- 边界测试覆盖事务、LOAD DATA、replication command、未知 command 和 unsupported prepared path。

## Spec Patch

无。本轮设计不修改 OpenSpec delta spec。
