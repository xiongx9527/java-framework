# Nebula MySQL 协议写入与高级兼容 Build 记录

Change: `add-nebula-mysql-protocol-write-and-advanced-support`

## 支持边界

### COM_QUERY

支持：

- `SELECT` 与只读 SQL 查询：继续走只读网关的 SQL query execution port。
- `INSERT`、`UPDATE`、`DELETE`：通过 SQL parser 和 write converter 转为 nGQL mutation plan 后执行。
- 受控 DDL：`CREATE TABLE`、`DROP TABLE` 和 schema metadata 类语句通过 DDL converter 转为 nGQL DDL plan 后执行。
- 只读 metadata：`SHOW DATABASES`、`SHOW TABLES`、`DESCRIBE`、`DESC`，以及本 change 增补的 JDBC/GUI 常见 metadata fixture。

明确不支持：

- 事务语句：`BEGIN`、`START TRANSACTION`、`COMMIT`、`ROLLBACK`。
- `LOAD DATA`、`LOCAL INFILE`、binlog、复制协议、存储过程、触发器、server-side cursor、多结果集。
- 复杂 DDL：`ALTER TABLE`、索引 DDL、完整 MySQL 8 DDL 语义。

### Prepared Statement

支持：

- `COM_STMT_PREPARE` 保存 SQL 模板、statement id、参数数量和初步语句类型。
- 常见 JDBC 路径的 `COM_STMT_EXECUTE` 参数绑定后执行 SELECT、写入和受控 DDL。
- `COM_STMT_CLOSE` 与 `COM_STMT_RESET` 清理 session statement 状态。

受限：

- 完整 MySQL binary protocol 类型矩阵仅覆盖常见 Java 值；未知类型返回稳定错误。
- 不支持 server-side cursor、streaming、多结果集。

### 权限与 SSL

策略为轻量配置，不实现完整 MySQL 授权表：

- 按 operation 区分 `READ`、`WRITE`、`DDL`、`METADATA`。
- 权限失败映射为 MySQL `1142 / 42000`。
- SSL required 失败映射为 MySQL `3159 / HY000`。
- 策略检查必须发生在 query/mutation/DDL execution port 之前。

### 错误响应

- 不支持能力返回 ERR packet，不断连。
- 参数数量不匹配返回 MySQL `1210 / HY000`。
- prepared unsupported 返回 MySQL `1235 / HY000`。
- SQL write/DDL converter 失败返回 MySQL 风格错误，消息说明这是 Nebula MySQL gateway 子集限制。

## 客户端兼容矩阵

| 客户端 | 路径 | 本 change 验收方式 | 预期 |
|--------|------|--------------------|------|
| Java MySQL JDBC Driver | `Statement.executeQuery` / `executeUpdate` | 自动单元测试 fixture 固定 query/write/DDL handler 行为 | SELECT 返回 result set；INSERT/UPDATE/DELETE/DDL 返回 OK 和 affected rows |
| Java MySQL JDBC Driver | `PreparedStatement` SELECT/write | 自动单元测试覆盖 prepare/execute/close/reset 与参数绑定 | 参数绑定后执行，参数数量错误返回 1210 |
| DBeaver | metadata browser | `information_schema.tables`、`information_schema.columns`、`SHOW VARIABLES` fixture | 返回稳定列名/类型或明确不支持错误 |
| DataGrip / BI 工具 | schema introspection | `SELECT DATABASE()`、`SHOW TABLES`、`DESCRIBE` fixture | 返回兼容 result set |

## 验证记录

待 Task 6 补充实际命令输出。
