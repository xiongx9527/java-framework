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
| Java MySQL JDBC Driver | `Statement.executeQuery` / `executeUpdate` | Connector/J 8.0.33 真实 socket 端到端测试 | SELECT 返回 result set；INSERT/UPDATE/DELETE 返回 OK 和 affected rows |
| Java MySQL JDBC Driver | `PreparedStatement` SELECT/write | 自动单元测试覆盖 prepare/execute/close/reset 与参数绑定 | 参数绑定后执行，参数数量错误返回 1210 |
| DBeaver | metadata browser | `information_schema.tables`、`information_schema.columns`、`SHOW VARIABLES` fixture | 返回稳定列名/类型或明确不支持错误 |
| DataGrip / BI 工具 | schema introspection | `SELECT DATABASE()`、`SHOW TABLES`、`DESCRIBE` fixture | 返回兼容 result set |

## Build Exit Criteria 边界

本 change 的 build exit criteria 收敛为可自动证明的范围：

- Java MySQL JDBC Driver 通过 Connector/J 8.0.33 真实 socket 自动化测试验证查询、写入和客户端侧 prepared write。
- DBeaver/DataGrip/BI 常见 metadata SQL 通过 fixture 自动化测试验证列名、类型、空结果或明确不支持错误。
- protocol/security 路径通过单元测试验证权限拒绝、SSL required、事务不支持和 unsupported command。
- 真实 GUI/BI 客户端验收不作为本 change 的 build exit criteria；该项已拆出为后续 Manual Acceptance Follow-up，不得在本 change 中声明已完成。

## 验证记录

### 自动测试

命令：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway -am test -DfailIfNoTests=false
```

最新结果：

- PASS
- `fw-plus-nebula-jdbc`: 123 tests, failures 0, errors 0, skipped 0
- `fw-plus-nebula-mysql-gateway`: 39 tests, failures 0, errors 0, skipped 0

覆盖：

- COM_QUERY SELECT、INSERT、DDL handler 路径。
- Prepared statement prepare/execute/reset/close common path。
- Connector/J 8.0.33 真实 socket 端到端查询与写入。
- 权限拒绝、SSL required、事务不支持、unsupported command。
- `SHOW DATABASES`、`SHOW TABLES`、`DESCRIBE`、`SHOW VARIABLES`、`SELECT DATABASE()`、`information_schema.tables`、`information_schema.columns` fixture。

命令：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway -am -Dtest=NebulaMysqlGatewayConnectorJTest -DfailIfNoTests=false test
```

结果：

- PASS
- `NebulaMysqlGatewayConnectorJTest`: 1 test, failures 0, errors 0, skipped 0

覆盖：

- Connector/J 8.0.33 通过 `DriverManager.getConnection` 连接本地 ephemeral port 的真实 MySQL wire-level handshake。
- Connector/J 初始化 SQL、`SELECT DATABASE()`、`Statement.executeQuery("select name from person")`。
- `Statement.executeUpdate("INSERT ...")` 和客户端侧 `PreparedStatement` 写入，均返回 affected rows = 1。

命令：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway -am -Dtest=AdvancedQueryHandlerTest#shouldAcceptMysqlDriverSessionSetStatementsAsOk,NebulaMysqlGatewayConnectorJTest -DfailIfNoTests=false test
```

结果：

- PASS
- 2 tests, failures 0, errors 0, skipped 0

命令：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc test -Dtest=NebulaSqlWriteConverterTest,NebulaSqlDdlConverterTest,NebulaSqlMutationExecutorTest,DirectNebulaSqlExecutorTest
```

结果：

- PASS
- 16 tests, failures 0, errors 0, skipped 0

命令：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway -am -Dtest=MysqlMetadataHandlerTest,AdvancedQueryHandlerTest#shouldRouteCommonMetadataQueries -DfailIfNoTests=false test
```

结果：

- PASS
- `AdvancedQueryHandlerTest#shouldRouteCommonMetadataQueries`: 1 test, failures 0, errors 0, skipped 0
- `MysqlMetadataHandlerTest`: 3 tests, failures 0, errors 0, skipped 0

覆盖：

- `SELECT DATABASE()`、`SHOW VARIABLES`。
- DBeaver/DataGrip 常见 `information_schema.tables` 和 `information_schema.columns` 探测 SQL。
- `SHOW DATABASES`、`SHOW TABLES`、`DESCRIBE` 和 table-not-found 错误语义。

### 环境约束

- `fw-plus-core/mvnw` 存在，但仓库未跟踪 `fw-plus-core/.mvn/wrapper/maven-wrapper.properties` 和 wrapper jar；在当前 worktree 直接执行 `./mvnw` 会失败。
- 当前 shell `JAVA_HOME` 为空，系统默认 `java` 为 JDK 24；验证命令显式使用本机 Zulu 8。

### Review Gate

- `.comet.yaml` 当前 `review_mode: standard`。
- 当前工具面未提供可用的 reviewer 子代理派发工具，因此未伪造子代理审查。
- 已在主会话对本轮 Comet/OpenSpec 文档 diff 做轻量审查：tasks、plan、proposal、design、delta spec、build report 和 Manual Acceptance Follow-up 对当前 build exit criteria 的描述一致；未发现把真实 GUI/BI 客户端验收伪造成已完成的问题。
- 本轮范围收敛没有新增生产代码；CRITICAL/IMPORTANT 审查发现：无。

### 后续手动/端到端验收

- `3.1 使用 Java MySQL JDBC Driver 验证查询和写入` 已通过 Connector/J 8.0.33 真实 socket 端到端自动化测试。
- `3.2` 已调整为当前 change 内可自动证明的 JDBC driver、metadata fixture、protocol/security 验证范围。
- 真实 GUI/BI 客户端验收已记录为后续 Manual Acceptance Follow-up：当前环境未发现可运行的 DBeaver、DataGrip、Tableau、Power BI、Metabase、DbVisualizer、SQuirreL、SQL Workbench 或 MySQL Workbench 客户端应用/命令，也未发现对应运行进程；已用 DBeaver/DataGrip/BI 常见 metadata SQL fixture 覆盖自动测试，但这不替代真实客户端手动验收。

GUI/BI 客户端检查命令：

```bash
command -v dbeaver || true
command -v datagrip || true
command -v tableau || true
command -v metabase || true
find /Applications "$HOME/Applications" -maxdepth 3 \( -iname '*DBeaver*.app' -o -iname '*DataGrip*.app' -o -iname '*Tableau*.app' -o -iname '*Power BI*.app' -o -iname '*Metabase*.app' -o -iname '*DbVisualizer*.app' -o -iname '*SQuirreL*.app' -o -iname '*MySQL Workbench*.app' -o -iname '*SQL Workbench*.app' \) -print 2>/dev/null
mdfind "kMDItemKind == 'Application' && (kMDItemFSName == '*DBeaver*' || kMDItemFSName == '*DataGrip*' || kMDItemFSName == '*Tableau*' || kMDItemFSName == '*Power BI*' || kMDItemFSName == '*Metabase*' || kMDItemFSName == '*DbVisualizer*' || kMDItemFSName == '*SQuirreL*' || kMDItemFSName == '*MySQL Workbench*')" 2>/dev/null || true
ps aux | rg -i 'DBeaver|DataGrip|Tableau|Power BI|Metabase|DbVisualizer|SQuirreL|MySQL Workbench|SQL Workbench' | rg -v 'rg -i' || true
```

结果：未发现匹配应用、命令或运行进程。

后续人工验收事项：

- 需要在安装了至少两个 GUI/BI 客户端的环境中连接本地 Nebula MySQL gateway，执行连接测试、schema/table metadata 浏览和 SELECT 查询。
- 后续验收结果应写入独立 follow-up 或新 change；本 change 不伪造真实 GUI/BI 客户端验收。
