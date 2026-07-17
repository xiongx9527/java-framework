---
comet_change: add-nebula-mysql-routine-subset
role: technical-design
canonical_spec: openspec
---

# Nebula MySQL Routine 子集技术设计

## 1. 背景与目标

本 change 属于 Nebula MySQL 协议深度兼容路线。前置能力包括 SQL parser core、SQL 到 nGQL 查询转换、SQL 写入/DDL 转换、MySQL protocol gateway 基础能力和高级协议扩展。NebulaGraph 不提供 MySQL 存储过程执行引擎，因此本 change 只在 MySQL 网关侧实现有限 routine 子集的解析、元数据保存和解释执行。

当前 `fw-plus-nebula-jdbc` 稳定路径仍是 nGQL 直连：`NebulaStatementImpl` 和 `NebulaPreparedStatementImpl` 通过 `NebulaSqlExecutor` 执行，`NebulaStatementClassifier` 只做 nGQL 首词分类。本 change 不改变该路径，也不把 `jdbc:nebula://` 的 `Statement.execute` 改造成 MySQL routine 入口。

目标：

- 支持 `CREATE PROCEDURE`、`DROP PROCEDURE`、`CALL` 的基础路径。
- 保存 routine 定义到网关元数据，并按 graphspace/schema 隔离。
- 支持 `IN` 参数、顺序 SQL、局部变量、`SET` 和简单 `IF/ELSEIF/ELSE`。
- 过程体 SQL 复用前置 SQL 到 nGQL 查询/写入执行能力。
- 对函数返回值、`OUT`/`INOUT`、游标、handler、动态 SQL、临时表、事务控制、循环、递归和复杂多结果集语义返回明确错误。

非目标：

- 不实现完整 MySQL Server routine 引擎。
- 不实现 MySQL 事务、锁、游标、handler 或动态 SQL 语义。
- 不捕获 Nebula 原生变更，也不让 routine 影响绕过网关的 nGQL/JDBC 写入。
- 不新增 Spring Boot starter 或 Magic-API 插件；如需要自动配置，由后续 change 承接。

## 2. 推荐方案

采用 **MySQL 网关侧 Routine Service + Routine Interpreter**。

```text
COM_QUERY sql
  -> MysqlCommandDispatcher
  -> RoutineStatementRouter
      -> parser-core routine IR
      -> RoutineService
          -> RoutineMetadataRepository
          -> RoutineInterpreter
              -> SQL execution port
                  -> parser-core
                  -> SQL-to-nGQL query/write converter
                  -> Nebula execution
  -> MySQL OK / resultset / ERR packet
```

该方案把 routine 作为 MySQL 网关高级兼容层处理。协议层负责识别和错误映射，routine 模块负责元数据和解释执行，前置 SQL execution port 负责具体 SQL 到 nGQL 的转换和执行。各层不直接依赖 Druid AST，不使用 raw SQL fallback 作为语义执行路径。

不采用文本宏展开。宏展开实现短，但会把参数、变量、条件分支和安全控制压成字符串替换，难以保证错误定位、权限和注入边界。

不采用完整 MySQL routine 引擎。完整引擎会引入事务、游标、handler、动态 SQL、函数返回值和多结果集等大量 NebulaGraph 无法等价承载的语义，超出本 change 的有限子集目标。

## 3. 模块与组件

### 3.1 `RoutineStatementRouter`

该组件挂在 MySQL gateway 的 COM_QUERY 分发路径中，早于普通 read/write query handler。

职责：

- 识别 `CREATE PROCEDURE`、`DROP PROCEDURE`、`CALL`。
- 对明显 routine 但非目标的语句返回明确不支持，例如 `CREATE FUNCTION`、`ALTER PROCEDURE`、`SHOW PROCEDURE STATUS`。
- 将 routine SQL 交给 parser core，获得自有 routine IR。
- 保证普通 SELECT、INSERT、UPDATE、DELETE、DDL 和 metadata SQL 继续走原有 SQL execution port。

识别层只做路由，不做完整语义解释。复杂语法是否支持由 parser core 和 routine validator 判定。

### 3.2 Parser Core Routine IR

parser core 需要扩展 routine 相关 IR，但 Druid 类型只允许留在 parser adapter 内部。

建议 IR：

- `CreateRoutineStatement`
  - routine kind：第一阶段只接受 `PROCEDURE`
  - routine name：包含 schema/graphspace 和 normalized name
  - parameters：参数名、mode、类型；第一阶段只接受 `IN`
  - body：statement list
  - original definition hash：用于诊断和覆盖检测
- `DropRoutineStatement`
  - routine name
  - `ifExists`
- `CallRoutineStatement`
  - routine name
  - argument expressions
- `RoutineBodyStatement`
  - `SqlBodyStatement`：过程体内普通 SQL
  - `SetVariableStatement`
  - `IfStatement`
  - `UnsupportedRoutineStatement`

第一阶段支持的 body 子集：

- `BEGIN ... END`
- 顺序 SQL
- `DECLARE` 简单局部变量，可选默认值
- `SET local_var = expression`
- 简单 `IF condition THEN ... ELSEIF ... ELSE ... END IF`

显式拒绝：

- `OUT`、`INOUT`
- `RETURN`、`CREATE FUNCTION`
- `CURSOR`
- `DECLARE ... HANDLER`
- `PREPARE`、`EXECUTE`、`DEALLOCATE PREPARE`
- `CREATE TEMPORARY TABLE`
- `START TRANSACTION`、`COMMIT`、`ROLLBACK`
- `WHILE`、`REPEAT`、`LOOP`
- `CALL` 自身或互相递归调用

### 3.3 `RoutineMetadataRepository`

routine 定义作为网关元数据管理，而不是写入 NebulaGraph schema。

建议接口：

```java
public interface RoutineMetadataRepository {
	RoutineDefinition find(RoutineName name) throws SQLException;
	void save(RoutineDefinition definition) throws SQLException;
	boolean drop(RoutineName name) throws SQLException;
}
```

`RoutineDefinition` 至少包含：

- graphspace/schema
- normalized routine name
- routine kind
- parameter definitions
- routine body IR 或可重新解析的规范化定义
- createdAt、updatedAt
- definition hash
- owner 或创建用户

默认 build 可提供内存实现和文件实现用于测试与单机运行。生产环境应通过配置选择持久化实现，并在文档中说明备份、迁移和多实例一致性责任。若当前 change 不实现分布式一致性，必须明确多实例同时写入同一 repository 的行为不作强一致承诺。

命名规则：

- routine 名称按 MySQL 非引号标识符大小写不敏感处理，内部使用 normalized lowercase key。
- 反引号标识符保留显示名，但 key 仍需要稳定规范化。
- graphspace/schema 是 key 的一部分，避免不同 database 下 routine 冲突。

覆盖策略：

- `CREATE PROCEDURE` 命中已存在名称时返回已存在错误。
- `CREATE OR REPLACE PROCEDURE` 不在第一阶段支持，除非 parser core 已显式定义该语义。
- `DROP PROCEDURE IF EXISTS` 对不存在定义返回 OK。
- `DROP PROCEDURE` 对不存在定义返回对象不存在错误。

### 3.4 `RoutineValidator`

validator 在保存前检查 routine 定义。

职责：

- 验证只包含支持的语法节点。
- 验证参数名和局部变量名不重复。
- 验证 `CALL` 不直接或间接引用当前 routine。
- 验证过程体 SQL 语句数量、嵌套深度和定义大小不超过配置限制。
- 验证过程体内 SQL 只调用前置 SQL execution port 支持的语句类型。

不支持能力优先在 `CREATE PROCEDURE` 阶段拒绝，这样元数据仓库不会保存不可执行定义。若历史元数据或兼容路径绕过保存校验，`CALL` 阶段仍必须再次防御并返回明确错误。

### 3.5 `RoutineInterpreter`

解释器负责执行已保存的 `RoutineDefinition`。

执行上下文：

- 当前 `MysqlProtocolSession`
- 当前 graphspace/schema
- authenticated user
- 输入参数值
- 局部变量 map
- execution budget
- call stack
- SQL execution port

执行流程：

```text
CALL proc(args)
  -> load RoutineDefinition
  -> bind IN parameters
  -> create RoutineExecutionContext
  -> execute body statements sequentially
      -> SET updates local variable
      -> IF evaluates condition and selects branch
      -> SQL statement resolves variables
      -> SQL execution port executes supported SQL
  -> return RoutineCallResult
```

变量解析规则：

- 参数和局部变量只允许作为表达式值参与过程体 SQL，不允许拼接 SQL 标识符或关键字。
- 变量替换必须通过类型化 binding 或 IR expression 注入，不允许字符串拼接生成动态 SQL。
- 参数数量和类型不匹配时，`CALL` 阶段返回参数错误。

条件表达式：

- 支持简单比较、布尔组合、`IS NULL`、字面量、参数和局部变量。
- 条件表达式不访问 NebulaGraph，不执行子查询。
- 复杂函数、子查询和不可确定表达式返回不支持错误。

结果策略：

- 如果过程体执行了一个或多个查询语句，第一阶段返回最后一个产生结果集的查询结果。
- 如果没有查询结果，返回 OK summary。
- 不承诺完整 MySQL 多结果集协议语义；若客户端要求多结果集能力，网关不应声明或不应在本 change 中启用。
- 任一语句失败时立即短路，后续语句不执行，并返回错误。由于事务是非目标，不承诺自动回滚已成功的 Nebula 写入。

### 3.6 权限与安全

routine 权限依附 MySQL gateway 已有认证/权限边界。

最小策略：

- `CREATE PROCEDURE`、`DROP PROCEDURE` 需要 routine 管理权限。
- `CALL` 需要 routine 执行权限。
- 过程体内 SQL 仍按当前用户/session 的 SQL execution 权限检查，不因 routine 存在而绕过写入、DDL 或 metadata 权限。
- 客户端错误消息不泄露完整连接串、密码、敏感配置或完整过程定义。

安全保护：

- 最大执行时间。
- 最大执行语句数。
- 最大 IF 嵌套深度。
- 最大 routine 定义大小。
- 最大参数数量。
- 禁止递归和循环。
- 禁止动态 SQL。

### 3.7 错误映射

内部错误应稳定，协议层再映射为 MySQL ERR packet。

建议错误码类别：

- `ROUTINE_ALREADY_EXISTS`
- `ROUTINE_NOT_FOUND`
- `ROUTINE_UNSUPPORTED_SYNTAX`
- `ROUTINE_UNSUPPORTED_FEATURE`
- `ROUTINE_INVALID_ARGUMENT`
- `ROUTINE_PERMISSION_DENIED`
- `ROUTINE_EXECUTION_TIMEOUT`
- `ROUTINE_EXECUTION_LIMIT_EXCEEDED`
- `ROUTINE_BODY_SQL_FAILED`

MySQL 协议映射建议：

| 场景 | MySQL error code | SQLState |
|------|------------------|----------|
| routine 不存在 | 1305 | 42000 |
| routine 已存在 | 1304 | 42000 |
| 语法错误 | 1064 | 42000 |
| 不支持语义 | 1235 | 42000 |
| 参数错误 | 1318 | 42000 |
| 权限拒绝 | 1044 或 1142 | 42000 |
| 执行超时或预算耗尽 | 1317 | 70100 |
| 过程体 SQL 失败 | 保留原 SQL 错误或 1105 | HY000 |

错误消息必须说明“网关 routine 子集不支持该能力”，不能暗示完整 MySQL Server 已执行失败。

## 4. 与相邻 Change 的边界

`add-nebula-sql-parser-core`：

- 提供 routine DDL/CALL 的 parser IR。
- 不执行 routine，不访问 repository。
- 不把 Druid AST 暴露给 gateway 或 routine service。

`add-nebula-sql-to-ngql-query-support`：

- 过程体 SELECT 通过 SQL execution port 复用 query converter。
- routine interpreter 不直接生成查询 nGQL。

`add-nebula-sql-write-and-ddl-support`：

- 过程体 INSERT/UPDATE/DELETE/DDL 通过 SQL execution port 复用 write/DDL converter。
- 事务、回滚和复杂 DDL 仍由 write/DDL change 的边界决定；routine 不额外模拟。

`add-nebula-mysql-protocol-gateway-readonly` 和 `add-nebula-mysql-protocol-write-and-advanced-support`：

- routine 依赖 MySQL gateway 的 COM_QUERY 分发、session、认证、错误包和可选 prepared statement 文本/二进制参数能力。
- routine 不改变握手能力声明，不主动开启多结果集高级能力。

`add-nebula-mysql-trigger-subset`：

- trigger 和 routine 可复用元数据 repository 抽象、执行预算和错误模型。
- trigger 只对网关可观察写入生效；routine CALL 不应绕过 trigger change 对写入路径的设计。如果 trigger change 尚未完成，routine 不主动实现 trigger 执行。

现有 nGQL JDBC 路径：

- `NebulaStatementImpl`、`NebulaPreparedStatementImpl`、`NebulaSqlExecutor` 继续执行 nGQL。
- 本 change 不把 `CALL`、`CREATE PROCEDURE` 或 MySQL routine 语法加入 nGQL classifier。

## 5. 数据流

### 5.1 创建过程

```text
COM_QUERY "CREATE PROCEDURE p(IN id VARCHAR(64)) BEGIN SELECT ...; END"
  -> RoutineStatementRouter
  -> parser-core parse CreateRoutineStatement
  -> RoutineValidator validate subset
  -> RoutineMetadataRepository.save
  -> OK packet
```

若定义包含游标、handler、动态 SQL、事务或循环：

```text
CREATE PROCEDURE ...
  -> validator rejects
  -> ERR unsupported feature
  -> repository unchanged
```

### 5.2 删除过程

```text
COM_QUERY "DROP PROCEDURE p"
  -> parser-core parse DropRoutineStatement
  -> permission check
  -> RoutineMetadataRepository.drop
  -> OK or routine-not-found ERR
```

`DROP PROCEDURE IF EXISTS p` 在不存在时返回 OK。

### 5.3 调用过程

```text
COM_QUERY "CALL p('v1')"
  -> parser-core parse CallRoutineStatement
  -> load RoutineDefinition
  -> permission check
  -> RoutineInterpreter.bind IN args
  -> execute supported body statements
  -> RoutineCallResult
  -> MySQL resultset or OK packet
```

### 5.4 过程体 SQL

```text
SqlBodyStatement
  -> resolve routine variables as typed values
  -> SQL execution port
      -> parser-core
      -> query/write converter
      -> Nebula execution
  -> query result or update summary
```

过程体 SQL 不走字符串拼接，也不允许变量替换成表名、列名或 SQL 片段。

## 6. 配置

建议配置项：

- `routine.enabled`：默认关闭或跟随高级 MySQL gateway 开关。
- `routine.repository.type`：`memory`、`file`、外部持久化实现。
- `routine.repository.location`：文件实现路径。
- `routine.maxDefinitionBytes`
- `routine.maxParameters`
- `routine.maxStatements`
- `routine.maxIfDepth`
- `routine.maxExecutionMillis`
- `routine.allowDdlInBody`
- `routine.allowWriteInBody`

写入和 DDL 是否允许由配置与 SQL execution port 双重决定。即使配置允许，前置 converter 不支持时仍返回明确错误。

## 7. 测试策略

### 7.1 Parser / IR 测试

覆盖：

- `CREATE PROCEDURE p() BEGIN SELECT 1; END`
- `CREATE PROCEDURE p(IN id VARCHAR(64)) BEGIN SELECT * FROM person WHERE id = id; END`
- `DROP PROCEDURE p`
- `DROP PROCEDURE IF EXISTS p`
- `CALL p('v1')`
- `DECLARE`、`SET`、`IF/ELSEIF/ELSE`
- `CREATE FUNCTION`、`OUT`、`INOUT`、游标、handler、动态 SQL、事务、循环返回不支持。

### 7.2 Repository 测试

覆盖：

- 保存后可查询。
- 重复创建失败。
- 删除已存在定义成功。
- `IF EXISTS` 删除不存在定义成功。
- graphspace/schema 隔离。
- 名称大小写归一。
- 文件 repository 重启后加载。

### 7.3 Interpreter 测试

使用 fake SQL execution port，断言：

- 参数按位置绑定。
- 局部变量赋值和读取正确。
- IF 分支只执行命中分支。
- 多条 SQL 按顺序执行。
- 第一条失败时后续不执行。
- 最后一个 resultset 作为 CALL 结果返回。
- 无 resultset 时返回 OK summary。
- 超时、语句数、嵌套深度和递归保护生效。

### 7.4 Gateway 分发与错误映射测试

覆盖：

- routine 语句走 routine handler。
- 普通 SQL 不受 routine router 影响。
- unsupported routine 特性映射为 MySQL ERR。
- routine 不存在、参数数量错误、权限拒绝和过程体 SQL 失败的错误码稳定。
- 未开启 routine 配置时，`CREATE PROCEDURE`、`DROP PROCEDURE`、`CALL` 返回明确不支持或功能关闭错误。

### 7.5 端到端 smoke

在 fake 或可选真实 Nebula 环境中验证：

- 创建过程。
- 调用过程执行 SELECT。
- 调用过程执行受支持写入后查询。
- 删除过程后再次 CALL 返回 routine 不存在。

真实 Nebula 环境只作为 smoke，不作为所有单元测试的前置条件。

## 8. 风险与缓解

MySQL routine 语义与 NebulaGraph 能力不等价。缓解方式是以子集为唯一承诺，并对不支持能力给出稳定错误。

过程体 SQL 可能产生部分成功。由于事务非目标，设计明确失败短路但不自动回滚。文档和错误消息必须说明这一点。

元数据一致性可能在多实例部署中复杂化。缓解方式是将 repository 作为可插拔边界，默认实现服务于单机和测试，生产部署需要选择外部持久化实现。

动态 SQL 和字符串替换有注入风险。缓解方式是完全禁止动态 SQL，变量只能作为类型化值进入过程体 SQL。

客户端可能假设完整 MySQL 多结果集和函数语义。缓解方式是不声明相关高级 capability，CALL 第一阶段只返回最后一个 resultset 或 OK summary，函数和多结果集高级语义明确不支持。

## 9. Build 阶段建议顺序

1. 扩展 parser core routine IR 和 unsupported 错误。
2. 新增 `RoutineMetadataRepository`、`RoutineDefinition`、`RoutineName`。
3. 实现 validator，先固定支持/拒绝矩阵。
4. 实现 interpreter，使用 fake SQL execution port 驱动单元测试。
5. 接入 MySQL gateway routine router 和错误映射。
6. 增加 repository、interpreter、gateway 分发和 smoke 测试。
7. 补充配置文档，说明子集边界、执行保护和生产 repository 责任。

