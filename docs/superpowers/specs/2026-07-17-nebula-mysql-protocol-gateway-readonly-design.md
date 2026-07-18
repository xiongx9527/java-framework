---
comet_change: add-nebula-mysql-protocol-gateway-readonly
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-18-add-nebula-mysql-protocol-gateway-readonly
status: final
---

# Nebula MySQL 协议只读网关技术设计

## 1. 背景与目标

本 change 为 Nebula SQL / MySQL 协议路线提供第一阶段只读 MySQL wire protocol 网关。目标是让 MySQL CLI 和兼容 MySQL wire protocol 的客户端可以连接网关，执行受支持的只读 SQL 和基础 metadata 查询，并收到 MySQL 文本协议结果集。GUI 客户端真实验收依赖本地客户端环境；当前环境未安装 GUI 客户端时，只记录后续人工验收，不伪造通过结论。

当前 `fw-plus-nebula-jdbc` 已有 nGQL JDBC 执行基础，相关前置 change 将提供 SQL parser core 和 SQL-to-nGQL 查询转换能力。网关设计必须消费这些稳定边界，不在协议层重复实现复杂 SQL parser、Druid AST adapter 或 SQL-to-nGQL converter。

## 2. 范围与非目标

### 范围

- 新增独立 MySQL wire protocol 网关能力，建议落在 Nebula core 层独立子模块，例如 `fw-plus-nebula-mysql-gateway` 或等价命名。
- 支持 MySQL protocol v10 握手、基础能力协商、认证、连接生命周期和 COM_QUERY。
- 支持 SELECT、复杂只读 SQL 的可映射子集、SHOW DATABASES、SHOW TABLES、DESCRIBE。
- 将 COM_QUERY 中的只读 SQL 转发给 `NebulaSqlExecutor` 或等价稳定 SQL 查询服务。
- 将 SQL 查询服务或 Nebula 执行结果编码为 MySQL 文本协议结果集。
- 将认证失败、语法错误、converter 不支持、Nebula 执行失败、只读拒绝和非目标协议命令映射为客户端可理解的 ERR packet。
- 增加不依赖真实 NebulaGraph 的协议、查询分发、结果集编码和错误语义测试；本 change 的 build 退出条件要求 MySQL CLI 真实连接查询通过，并保留 GUI 客户端后续人工验收记录。

### 非目标

- 不实现 MySQL prepared statement 二进制协议、COM_STMT_*、LOAD DATA、LOCAL INFILE、多结果集高级语义或 server-side cursor。
- 不实现写入、DDL 执行、事务、锁、存储过程、函数、触发器、事件调度、binlog、复制协议或权限体系。
- 不在网关内直接依赖 Druid AST、converter 内部模型或 raw SQL fallback。
- 不改变现有 `jdbc:nebula://...` nGQL 直连路径。
- 不追求 100% MySQL Server 或 SQL 标准兼容。
- 不在本 change 中新增 Spring Boot starter 或 Magic-API 插件；若需要自动配置，后续独立 change 承接。

## 3. 推荐方案

采用独立协议层方案。

网关分为三段：

1. MySQL protocol adapter：负责网络连接、packet 编解码、握手、认证、COM_QUERY 分发、resultset/OK/ERR 编码。
2. Read-only query gateway：负责 SQL 分类、只读边界、metadata handler、调用 SQL 查询服务，以及统一错误映射。
3. Nebula SQL execution boundary：由前置 change 提供 `NebulaSqlExecutor` 或等价接口，内部消费 parser-core 和 SQL-to-nGQL converter，并最终复用 `NebulaConnection.execute(nGql)` / `NebulaClientFacade.executeNgql(nGql)`。

该方案避免把 MySQL 协议兼容问题压到 JDBC `Statement`，也避免协议层直接感知复杂 SQL AST。后续 write、binlog、routine、trigger change 可以在协议层扩展命令处理器和 session 状态，但不会反向污染本 change 的只读查询路径。

## 4. 模块与组件设计

### 4.1 `NebulaMysqlGatewayServer`

职责：

- 监听 MySQL 协议端口，建议默认不与现有服务端口冲突，由配置指定。
- 管理 accept、channel lifecycle、idle timeout、连接关闭和资源释放。
- 注入认证服务、SQL 查询服务、metadata 服务和 packet codec。

实现方式优先评估 Java 8 兼容、许可证清晰的 MySQL packet 编解码库。若第三方库无法满足许可证、维护状态或协议子集可控性要求，则采用 Netty + 最小 packet 编解码实现。无论采用哪种方式，对内部组件暴露的接口都应保持 packet codec 与 query handler 解耦，便于测试替换。

### 4.2 `MysqlProtocolSession`

职责：

- 保存 connection id、server capability、client capability、sequence id、character set、database、authenticated user、server status。
- 跟踪握手是否完成、认证是否成功、当前 graphspace/database 选择。
- 为 COM_QUERY 提供只读 session context。

最小 capability 建议包含：

- `CLIENT_PROTOCOL_41`
- `CLIENT_SECURE_CONNECTION`
- `CLIENT_PLUGIN_AUTH`
- `CLIENT_CONNECT_WITH_DB`，仅当配置允许客户端指定默认 database 时启用
- `CLIENT_DEPRECATE_EOF` 可配置；默认可使用 EOF packet 以降低兼容风险

不声明或不支持的 capability 不应在握手中承诺，避免客户端进入本 change 不实现的协议路径。

### 4.3 `MysqlHandshakeHandler`

职责：

- 发送 MySQL protocol v10 初始握手包。
- 设置 server version、connection id、auth plugin data、capability flags、character set 和 status flags。
- 接收 handshake response，解析 username、auth response、database 和 auth plugin。
- 调用认证服务并在成功后返回 OK packet，失败返回 ERR packet 并关闭连接。

server version 建议使用稳定、低惊讶的 5.7 兼容字符串，例如 `5.7.0-fw-nebula-gateway`。这能减少 MySQL 8 客户端对 `caching_sha2_password` 的默认假设，也避免暗示完整 MySQL 8 server 行为。

### 4.4 `MysqlGatewayAuthenticator`

职责：

- 提供基础认证边界，不实现完整 MySQL 权限体系。
- 支持配置用户、密码和允许访问的默认 graphspace/database。
- 支持 `mysql_native_password` 兼容认证，使用 handshake scramble 校验客户端响应。
- 认证成功只表示该连接可使用只读网关；实际 Nebula 访问仍由网关配置或 Nebula JDBC 连接配置决定。

若后续需要把 MySQL 用户映射到 Nebula 用户、角色或细粒度权限，应作为权限体系 change 承接。本 change 不在 COM_QUERY 阶段做表级权限判断。

### 4.5 `MysqlCommandDispatcher`

职责：

- 只处理本 change 支持的命令：`COM_QUERY`、`COM_PING`、`COM_QUIT`。
- `COM_INIT_DB` 可选支持为切换 session database/graphspace，但必须只影响后续 metadata/query context，不执行 DDL。
- 对 `COM_STMT_PREPARE`、`COM_STMT_EXECUTE`、`COM_FIELD_LIST`、`COM_PROCESS_INFO`、replication/binlog 相关命令、LOAD DATA 相关命令返回明确不支持 ERR packet。

该 dispatcher 是协议命令白名单。即使 SQL 内容看起来只读，非 COM_QUERY 路径也不进入 SQL 查询服务，避免客户端通过未设计协议路径绕过边界。

### 4.6 `ReadonlyQueryHandler`

职责：

- 接收 COM_QUERY 的 SQL 文本和 `MysqlProtocolSession`。
- 先做轻量只读门禁：空 SQL、注释后空 SQL、明显写入/DDL/事务/routine/binlog/复制语句直接拒绝。
- 将 SELECT 和复杂只读 SQL 交给 SQL 查询服务，由 parser/converter 判定是否支持和能否映射。
- 将 SHOW DATABASES、SHOW TABLES、DESCRIBE 交给 metadata handler，或在前置 SQL 执行服务明确支持时交给该服务。
- 将执行结果转换为 `MysqlQueryResult`，供结果集编码器输出。

只读门禁不是完整 SQL parser。它只承担快速拒绝和防越界职责，复杂 SQL 语义仍由 parser-core 与 SQL-to-nGQL converter 判断。门禁拒绝列表至少包括 `INSERT`、`UPDATE`、`DELETE`、`REPLACE`、`CREATE`、`ALTER`、`DROP`、`TRUNCATE`、`RENAME`、`GRANT`、`REVOKE`、`BEGIN`、`START TRANSACTION`、`COMMIT`、`ROLLBACK`、`CALL`、`CREATE PROCEDURE`、`CREATE FUNCTION`、`CREATE TRIGGER`、`SHOW BINLOG`、`SHOW MASTER`、`SHOW SLAVE`、`CHANGE MASTER`。

### 4.7 `NebulaSqlExecutionPort`

网关只依赖稳定查询端口，不直接依赖 converter 内部实现。

建议接口形态：

```java
public interface NebulaSqlExecutionPort {
	MysqlQueryResult executeQuery(MysqlGatewaySessionContext context, String sql) throws SQLException;
}
```

适配到 JDBC 侧时，可以内部调用 `NebulaSqlExecutor.executeQuery(connection, sql, emptyBindings)` 或等价服务。`emptyBindings` 是本 change 的明确边界：COM_QUERY 文本协议不支持 prepared statement 参数绑定。

执行端口应返回结构化列描述和行数据：

- column label
- column name
- JDBC type 或网关可映射类型
- nullable/read-only
- row values

如果前置执行服务暂时只能返回 JDBC `ResultSet`，网关 adapter 负责从 `ResultSetMetaData` 提取列描述并遍历结果行；不应让 protocol encoder 直接读取 JDBC 对象。

### 4.8 `MysqlMetadataHandler`

职责：

- `SHOW DATABASES`：返回可访问 graphspace 列表；在最小实现中可返回当前配置 graphspace。
- `SHOW TABLES`：返回当前 graphspace 下映射为 SQL table 的 tag/edge 名称。
- `DESCRIBE <table>` / `DESC <table>`：返回列名、类型、NULL、Key、Default、Extra 等 MySQL 客户端常见字段。

metadata 来源优先使用前置 schema mapping / SQL execution service 暴露的稳定接口；没有稳定接口时，可通过受控 nGQL metadata 查询适配，但该逻辑必须封装在 metadata handler 内，不能散落到 protocol encoder。

### 4.9 `MysqlTextResultSetEncoder`

职责：

- 编码 column count。
- 编码 column definition packet。
- 编码 row packet。
- 编码 EOF/OK terminator。
- 支持 ERR packet 中断。

类型映射以 SQL 执行结果列描述为准。缺失类型时采用保守映射：

- `String` -> `MYSQL_TYPE_VAR_STRING`
- `Integer` / `Long` -> `MYSQL_TYPE_LONGLONG` 或 `MYSQL_TYPE_LONG`
- `Double` / `Float` / `BigDecimal` -> `MYSQL_TYPE_DOUBLE` 或 `MYSQL_TYPE_NEWDECIMAL`
- `Boolean` -> `MYSQL_TYPE_TINY`
- `Date` / `Timestamp` -> `MYSQL_TYPE_DATETIME` 或 `MYSQL_TYPE_TIMESTAMP`
- `null` -> row value 使用 NULL 标记，列类型由列描述决定
- 其他对象 -> JSON 或 `toString()` 后作为 `MYSQL_TYPE_VAR_STRING`，并在设计和测试中固定语义

本 change 只实现文本协议结果集，不实现二进制 row encoding。

### 4.10 `MysqlErrorMapper`

职责：

- 把内部异常和拒绝原因映射为 ERR packet。
- 提供稳定错误码、SQLState 和消息。
- 日志保留诊断细节，客户端消息避免泄露密码、完整连接串或敏感配置。

建议映射：

| 场景 | MySQL error code | SQLState | 说明 |
|------|------------------|----------|------|
| 认证失败 | 1045 | 28000 | Access denied |
| 语法错误 / parser 失败 | 1064 | 42000 | You have an error in your SQL syntax |
| 只读拒绝 | 1290 | HY000 | The server is running in read-only mode |
| converter 不支持语义 | 1235 | 42000 | This version does not support requested SQL |
| metadata 对象不存在 | 1146 | 42S02 | Table does not exist |
| 非目标协议命令 | 1235 | HY000 | Command is not supported |
| Nebula 执行失败 | 1105 | HY000 | Unknown error with Nebula diagnostic id |

错误消息应说明“只读网关不支持该语句/命令”，而不是伪装成 MySQL Server 已执行失败。

## 5. 数据流

### 5.1 连接与认证

```text
client connect
  -> GatewayServer accept
  -> MysqlHandshakeHandler send initial handshake
  -> client handshake response
  -> MysqlGatewayAuthenticator authenticate
  -> MysqlProtocolSession authenticated
  -> OK packet
```

认证失败：

```text
client handshake response
  -> authenticator rejects
  -> ERR 1045 / SQLState 28000
  -> close channel
```

### 5.2 COM_QUERY SELECT

```text
COM_QUERY sql
  -> MysqlCommandDispatcher
  -> ReadonlyQueryHandler
      -> lightweight read-only gate
      -> NebulaSqlExecutionPort.executeQuery(context, sql)
          -> parser-core.parse(sql)
          -> sql-to-ngql converter.convert(ast, emptyBindings)
          -> NebulaConnection.execute(ngql)
      -> MysqlQueryResult
  -> MysqlTextResultSetEncoder
  -> MySQL text resultset packets
```

### 5.3 SHOW / DESCRIBE

```text
COM_QUERY "SHOW TABLES"
  -> ReadonlyQueryHandler
  -> MysqlMetadataHandler.showTables(session database)
  -> MysqlQueryResult(columns=["Tables_in_<db>"], rows=[...])
  -> MysqlTextResultSetEncoder
```

### 5.4 只读拒绝

```text
COM_QUERY "INSERT INTO person ..."
  -> ReadonlyQueryHandler
  -> readonly gate rejects before converter
  -> MysqlErrorMapper read-only error
  -> ERR 1290 / SQLState HY000
```

## 6. 稳定依赖接口

本 change 只依赖以下稳定边界：

- Parser-core：将 SQL 文本解析为稳定中间表示，并对语法错误提供可定位异常。
- SQL-to-nGQL query support：消费 parser 中间表示和 schema mapping，将可映射只读 SQL 转为 nGQL；不可映射时返回明确不支持。
- JDBC/Nebula execution：`NebulaConnection.execute(String nGql)` 或 `NebulaClientFacade.executeNgql(String nGql)` 执行 nGQL 并返回 Nebula `ResultSet`。
- SQL execution port：建议由 JDBC 集成 change 或 query support change 提供 `NebulaSqlExecutor` 等价服务，屏蔽 parser/converter/Nebula 执行细节。
- Schema / metadata mapping：提供 SQL table/column 到 Nebula tag/edge/property/id 的只读描述；网关只读取，不在协议层维护复杂映射规则。

禁止依赖：

- Druid AST 具体类。
- converter 私有模型。
- raw SQL fallback 作为语义执行路径。
- 写入、DDL、binlog、routine、trigger change 的未稳定接口。

## 7. 错误处理与客户端语义

- 握手包格式错误：返回协议错误并关闭连接。
- 认证失败：返回 1045，关闭连接。
- 未认证发送命令：返回 ERR 并关闭连接。
- 空 SQL：返回 OK packet 或明确语法错误二选一；建议按 MySQL 行为返回 OK，测试固定。
- SELECT parser 语法错误：返回 1064。
- converter 无法映射：返回 1235 或稳定 SQLException 映射。
- Nebula 执行失败：返回 1105，并在服务端日志记录 Nebula error code/message 和 request id。
- 写入、DDL、事务、routine、trigger、binlog、复制：返回 1290 或 1235，不调用 converter，不调用 Nebula 执行。
- 客户端请求不支持协议命令：返回 1235，不静默成功。

## 8. 测试策略

### 8.1 协议单元测试

- Initial handshake packet 字段、capability flags、auth plugin、sequence id。
- Handshake response 解析 username、database、auth response。
- 认证成功返回 OK，认证失败返回 ERR 1045 并关闭连接。
- COM_QUERY、COM_PING、COM_QUIT dispatch。
- 非支持命令返回 ERR 1235。

### 8.2 只读查询测试

- SELECT 语句调用 fake `NebulaSqlExecutionPort`，断言 SQL 原文、session database 和空绑定传递正确。
- JOIN、子查询、聚合、UNION 等复杂只读 SQL 由 fake executor 接收，网关不提前拒绝。
- INSERT、UPDATE、DELETE、DDL、事务、CALL、TRIGGER、binlog/replication 相关语句在调用 executor 前被拒绝。
- parser/converter 抛出的 SQLException 映射为预期 ERR packet。

### 8.3 Metadata 测试

- SHOW DATABASES 返回当前可访问 graphspace。
- SHOW TABLES 返回 tag/edge 映射表。
- DESCRIBE 返回 MySQL 客户端可展示的列结构。
- 不存在表返回 1146 / 42S02。

### 8.4 结果集编码测试

- 单列、 多列、空结果集。
- NULL、字符串、整数、长整型、浮点、布尔、日期时间和对象 fallback。
- 列 label、列 type、read-only flag。
- EOF/OK terminator 与 sequence id 连续性。

### 8.5 客户端验收

- MySQL CLI：连接、`SELECT 1` 或受支持 SELECT、`SHOW TABLES`、`DESCRIBE`、写入拒绝。
- GUI 客户端后续人工验收：在安装 DBeaver、DataGrip、Navicat、TablePlus、Sequel Ace、MySQL Workbench 或等价客户端后执行连接测试、打开 schema/table 列表、执行 SELECT、写入拒绝。
- 客户端验收可以作为可选集成测试或手动验证记录，不应阻塞无 NebulaGraph 环境的单元测试。

## 9. 实施顺序

1. 确认模块边界和协议编解码实现方式。
2. 实现 handshake、认证和 session lifecycle。
3. 实现命令 dispatcher 的白名单与非目标命令拒绝。
4. 实现 COM_QUERY read-only handler 和 SQL execution port adapter。
5. 实现 SHOW/DESCRIBE metadata handler。
6. 实现文本结果集、OK、ERR、EOF 编码。
7. 补齐错误映射和日志诊断。
8. 增加协议、查询、metadata、结果集编码和只读拒绝测试。
9. 执行 MySQL CLI 真实验收；GUI 客户端验收在具备客户端环境后作为后续人工验收执行。

## 10. 风险与缓解

- 第三方协议库可能不兼容 Java 8 或许可证不适合。缓解：先做依赖评估；无法满足时使用 Netty + 最小 packet codec。
- MySQL 客户端会自动发送 metadata 探测 SQL。缓解：本 change 显式支持 SHOW/DESCRIBE，其他探测 SQL 交给 parser/converter 白名单或返回清晰不支持。
- SQL-to-nGQL converter 与网关并行推进，接口可能未最终落地。缓解：网关先依赖 `NebulaSqlExecutionPort`，测试使用 fake executor；真实 adapter 在稳定接口可用后接入。
- 客户端误以为网关是完整 MySQL Server。缓解：握手 capability 保守声明，非目标命令返回明确不支持，不静默成功。
- 结果集类型从 Nebula 值推断可能不稳定。缓解：优先消费 SQL 执行服务返回的列描述；缺失时采用保守类型映射并用测试固定 fallback。
