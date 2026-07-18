---
comet_change: add-nebula-mysql-protocol-write-and-advanced-support
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-18-add-nebula-mysql-protocol-write-and-advanced-support
status: final
---

# Nebula MySQL 协议写入与高级兼容技术设计

## 1. 背景与目标

本 change 在只读 MySQL 协议网关之上扩展写入、DDL、prepared statement、权限/安全策略、SSL 和客户端 metadata 兼容能力。它的目标不是实现完整 MySQL Server，而是让 Java MySQL JDBC Driver、GUI/BI 工具在受控子集内可以完成常见查询、写入、DDL 和元数据探测。

本 change 依赖只读网关提供的 protocol adapter、session、dispatcher、text result encoder 和 error mapper，并依赖 SQL write/DDL support 提供统一写入/DDL 执行端口。

## 2. 范围与非目标

### 范围

- 扩展 MySQL 协议网关支持受控 INSERT、UPDATE、DELETE 和 DDL。
- 支持 Java MySQL JDBC Driver 常见 prepared statement 路径。
- 提供可配置认证、权限和 SSL/TLS 策略。
- 增强 `SHOW`、`DESCRIBE`、`information_schema` 兼容响应。
- 建立客户端兼容矩阵和不支持协议路径的明确错误。

### 非目标

- 不实现完整 MySQL 用户系统、授权表、角色继承或行列级权限。
- 不实现 MySQL 事务、锁、隔离级别、XA、savepoint 或 server-side cursor。
- 不实现 binlog、复制协议、routine、trigger；这些由后续独立 changes 承接。
- 不实现 LOAD DATA、LOCAL INFILE、插件生态或完整 MySQL 8 行为。
- 不改变现有 `jdbc:nebula://...` nGQL 直连路径。

## 3. 推荐方案

采用“扩展只读网关 dispatcher + 稳定执行端口 + 客户端兼容层”的方案。

```text
MySQL packet
  -> MysqlCommandDispatcher
  -> Query / Write / DDL / Prepared / Metadata handlers
  -> Nebula SQL query or mutation execution ports
  -> Mysql OK / ERR / ResultSet packets
```

协议层只负责 MySQL wire protocol、session 状态、参数绑定和错误映射。SQL 语义继续下沉到 parser、query converter、write/DDL converter 和执行端口，避免协议层重复解析 SQL。

## 4. 核心组件

### 4.1 `MysqlAdvancedCommandDispatcher`

在只读 dispatcher 基础上扩展命令白名单：

- `COM_QUERY`
- `COM_STMT_PREPARE`
- `COM_STMT_EXECUTE`
- `COM_STMT_CLOSE`
- `COM_STMT_RESET`
- `COM_PING`
- `COM_QUIT`
- 可选 `COM_INIT_DB`

仍明确拒绝 LOAD DATA、复制协议、binlog dump、完整 process/control 命令和未设计命令。拒绝时返回稳定 ERR packet，而不是断开连接或静默忽略。

### 4.2 `MysqlWriteQueryHandler`

职责：

- 接收 COM_QUERY 文本 SQL。
- 对 SELECT 和只读 metadata 延续只读网关处理逻辑。
- 对 INSERT、UPDATE、DELETE 调用 `NebulaSqlMutationExecutor.executeMutation`。
- 对受支持 DDL 调用 `executeDdl` 或 metadata handler。
- 对事务语句返回明确不支持或受限支持错误。

写入成功返回 OK packet，包含 affected rows、warnings 和 server status。DDL 成功也返回 MySQL 风格 OK packet。错误通过统一 `MysqlErrorMapper` 映射。

### 4.3 Prepared Statement 支持

prepared statement 分为 prepare、execute、close 三段。

`COM_STMT_PREPARE`：

- 保存 statement id、SQL 模板、参数数量、初步 statement kind 和结果列/参数 metadata。
- 不在 prepare 阶段执行 SQL。
- 若 parser 无法识别参数或语句类型，返回明确错误。

`COM_STMT_EXECUTE`：

- 解析 MySQL binary protocol 参数值。
- 将参数绑定转换为 parser/query/write 层可消费的 bindings。
- SELECT 调用查询执行端口，写入/DDL 调用 mutation executor。
- 返回 binary 或文本兼容结果。最小实现可先返回 JDBC Driver 可接受的结果集编码，但必须在测试中固定。

`COM_STMT_CLOSE` / `COM_STMT_RESET`：

- 清理 session statement registry。
- 不泄漏参数、SQL 或结果状态。

不支持 server-side cursor、多结果集、高级 streaming 和复杂 metadata flags 时必须在 capability 或错误中明确。

### 4.4 权限与安全策略

本 change 不实现完整 MySQL 授权表，采用可配置策略：

- 用户认证：沿用只读网关 authenticator，可扩展用户配置。
- 权限策略：按 user/database/schema/operation 判断是否允许 read、write、ddl、metadata。
- SSL/TLS：通过配置启用，支持必需 SSL、可选 SSL 或禁用。
- 安全审计：记录连接、用户、操作类型、成功/失败和 trace id，避免记录敏感参数明文。

权限失败映射为 MySQL 风格 access denied 或 command denied 错误。策略检查必须发生在执行端口之前。

### 4.5 Metadata 兼容层

GUI/BI 工具和 Java MySQL JDBC Driver 会发起大量 metadata 探测。兼容层负责常见响应：

- `SHOW DATABASES`
- `SHOW TABLES`
- `SHOW COLUMNS` / `DESCRIBE`
- `SHOW VARIABLES` 的安全子集
- `SELECT DATABASE()`
- 常见 `information_schema.tables`、`information_schema.columns` 查询子集

metadata 结果来自 schema mapping 和 Nebula metadata handler，不直接扫描未授权对象。不支持的 metadata 查询返回空结果或明确错误，具体选择由兼容矩阵固定。

### 4.6 `MysqlProtocolCompatibilityProfile`

建议引入兼容 profile：

- `STRICT`：只声明和支持已实现 capability，不做额外兼容模拟。
- `JDBC_COMPATIBLE`：优先兼容 Java MySQL JDBC Driver 常见握手、prepared statement 和 metadata。
- `BI_COMPATIBLE`：额外兼容 GUI/BI metadata 探测。

profile 影响 capability flags、server version、metadata 响应和错误策略，但不能扩大 SQL 语义边界。

## 5. 错误映射

建议补充错误映射：

| 场景 | MySQL error code | SQLState | 说明 |
|------|------------------|----------|------|
| 权限不足 | 1142 | 42000 | Command denied |
| 事务不支持 | 1196 | HY000 | Transaction rollback not available |
| prepared 不支持结构 | 1235 | HY000 | Prepared path unsupported |
| 参数数量不匹配 | 1210 | HY000 | Incorrect arguments |
| SSL required | 3159 | HY000 | Connections using insecure transport are prohibited |
| 写入转换失败 | 1235 | 42000 | SQL write is not supported |
| DDL 不支持 | 1235 | 42000 | DDL is not supported |

错误消息应说明“Nebula MySQL gateway 子集不支持该能力”，不要暗示完整 MySQL Server 行为。

## 6. 与其他分区的边界

- SQL write/DDL 分区负责转换和执行语义，本 change 只调用其稳定端口。
- binlog stream 分区在写入成功边界之后消费 mutation/ddl descriptor，本 change 不生成事件格式。
- routine 和 trigger 分区可复用 prepared/write handler，但解释执行和递归保护不在本 change 内。
- replication 分区处理复制命令，本 change 对相关命令返回明确不支持，直到 replication change 接管。

## 7. 测试策略

- 协议单元测试：dispatcher 命令白名单、prepared lifecycle、OK/ERR/resultset packet。
- 参数绑定测试：字符串、数字、布尔、日期、null、二进制安全拒绝。
- 写入/DDL handler 测试：调用 fake mutation executor，验证 OK packet、affected rows 和错误映射。
- 权限/SSL 测试：认证成功但权限不足、SSL required、禁用 DDL。
- metadata 兼容测试：Java MySQL JDBC Driver 常见 metadata SQL、至少两个 GUI/BI 工具探测 fixture。
- 边界测试：事务语句、LOAD DATA、replication command、未知 command、unsupported prepared path。

测试默认不依赖真实 NebulaGraph。客户端兼容可通过协议 fixture 和可选手动验收记录补充。

## 8. 验收要点

- COM_QUERY 可执行受支持写入和 DDL，并返回 MySQL 风格 OK/ERR。
- Java MySQL JDBC Driver 常见 PreparedStatement SELECT 和写入路径可完成参数绑定、执行和关闭。
- 权限和 SSL 策略可配置，失败路径返回明确 MySQL 协议错误。
- GUI/BI metadata 探测可获得兼容响应或稳定不支持错误。
- binlog、replication、routine、trigger 职责没有混入本 change。
