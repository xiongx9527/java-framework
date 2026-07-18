---
comet_change: add-nebula-sql-write-and-ddl-support
role: technical-design
canonical_spec: openspec
---

# Nebula SQL 写入与 DDL 支持技术设计

## 1. 背景与目标

本 change 在 SQL parser core 和 SQL SELECT 查询转换能力之后，补齐 INSERT、UPDATE、DELETE 和基础 DDL 到 nGQL 的转换与执行。目标是让 JDBC 和 MySQL 协议网关可以通过同一套写入/DDL 边界执行受支持 SQL，同时明确 SQL 事务、复杂 DDL 和关系模型到图模型之间的语义差异。

写入与 DDL 必须复用 parser core 的稳定 IR，并复用查询转换阶段形成的 schema mapping 模型。不能在写入路径中重新猜测 SQL 表、列、vertex id、edge src/dst/rank 或 Nebula 属性映射。

## 2. 范围与非目标

### 范围

- 新增 INSERT、UPDATE、DELETE 到 nGQL 的转换与执行。
- 新增基础 CREATE/DROP/DESCRIBE 类 DDL 到 Nebula tag/edge schema 操作的映射。
- 定义写入安全条件、schema mapping 约束和错误模型。
- 定义写入/DDL 执行结果 descriptor，供 MySQL gateway、binlog stream、routine 和 trigger 分区复用。
- 为 JDBC `executeUpdate` 和 MySQL 网关写入路径提供稳定执行端口。

### 非目标

- 不实现 MySQL 事务、锁、隔离级别、回滚日志或跨语句原子性。
- 不实现 binlog、复制协议、routine、trigger 或完整 MySQL 权限体系。
- 不支持复杂 ALTER、索引、外键、约束、存储引擎选项、分区表或 MySQL 专有 DDL。
- 不改变现有 nGQL 直连路径；原始 nGQL 仍可按当前 JDBC 行为执行。
- 不承诺完整 SQL 标准或 MySQL Server 等价语义。

## 3. 推荐方案

采用“独立 SQL write/DDL converter + 执行端口 + mutation descriptor”的方案。

```text
SQL text
  -> parser-core.parse(sql)
  -> SQL write/DDL IR
  -> NebulaSqlWriteConverter / NebulaSqlDdlConverter
  -> NebulaMutationPlan / NebulaDdlPlan
  -> NebulaSqlMutationExecutor
  -> NebulaMutationResult
  -> gateway/JDBC/binlog/trigger consumers
```

converter 只负责转换和安全判定，不访问 NebulaGraph。executor 负责调用现有 Nebula 执行链路。执行结果中必须包含结构化 descriptor，供 binlog-compatible stream 和 trigger/routine 执行链路消费。

## 4. 核心组件

### 4.1 `NebulaSqlWriteConverter`

建议接口：

```java
public interface NebulaSqlWriteConverter {
	NebulaMutationPlan convert(SqlStatementIr statement,
							   NebulaWriteBindings bindings,
							   NebulaSchemaMappingRegistry schemaMapping) throws SQLException;
}
```

设计约束：

- 仅接受 INSERT、UPDATE、DELETE IR。
- 缺少 mapping、id、src/dst/rank 或安全条件时直接失败。
- 不执行 Nebula 查询，不读取当前数据，不做事务模拟。
- 输出稳定的 mutation plan、诊断 trace 和安全说明。

### 4.2 `NebulaSqlDdlConverter`

建议接口：

```java
public interface NebulaSqlDdlConverter {
	NebulaDdlPlan convert(SqlStatementIr statement,
						  NebulaSchemaMappingRegistry schemaMapping) throws SQLException;
}
```

支持子集：

- `CREATE TABLE` 映射为 `CREATE TAG` 或 `CREATE EDGE`，必须通过显式选项或配置判断 object kind。
- `DROP TABLE` 映射为 `DROP TAG` 或 `DROP EDGE`。
- `DESCRIBE` / `DESC` 可映射到 schema metadata 查询，若已由 metadata handler 承接，则本 converter 输出 metadata plan。

拒绝复杂 ALTER、索引、约束、外键、MySQL 存储选项和无法映射的列类型。

### 4.3 `NebulaMutationPlan`

写入计划包含：

- `mutationKind`：`INSERT_VERTEX`、`INSERT_EDGE`、`UPDATE_VERTEX`、`UPDATE_EDGE`、`DELETE_VERTEX`、`DELETE_EDGE`。
- `ngql`：待执行 nGQL。
- `target`：tag/edge/table mapping。
- `keys`：VID 或 src/dst/rank。
- `columns`：属性映射。
- `affectedRowsPolicy`：如何解释 Nebula 返回结果。
- `trace`：SQL IR、mapping 命中、拒绝或降级说明。

plan 不能只是一段 nGQL 字符串。后续 binlog、trigger、routine 都需要读取 target、keys 和 mutation kind。

### 4.4 `NebulaMutationResult`

执行结果是跨分区契约。

建议字段：

- `success`
- `affectedRows`
- `mutationDescriptor`
- `ddlDescriptor`
- `warnings`
- `traceId`

`mutationDescriptor` 至少包含目标对象、操作类型、标识列、属性变更和原 SQL/nGQL 引用。binlog stream 根据 descriptor 生成事件，不重新解析 SQL。

### 4.5 `NebulaSqlMutationExecutor`

executor 负责调用现有 Nebula 执行链路：

```java
public interface NebulaSqlMutationExecutor {
	NebulaMutationResult executeMutation(NebulaMutationPlan plan) throws SQLException;

	NebulaMutationResult executeDdl(NebulaDdlPlan plan) throws SQLException;
}
```

JDBC `executeUpdate`、MySQL gateway 写入路径、routine body 和 trigger body 都应调用该端口或其上层 facade，而不是各自拼接 nGQL。

## 5. 写入转换规则

### 5.1 INSERT

支持条件：

- 目标表必须有明确 tag 或 edge mapping。
- tag INSERT 必须提供 VID/idColumn。
- edge INSERT 必须提供 src、dst 和可选 rank。
- 属性列必须全部可映射到 Nebula 属性或明确为系统列。

拒绝条件：

- 缺少 vertex id、edge src/dst。
- 多行 INSERT 中任一行不满足 mapping。
- `INSERT ... SELECT`、`ON DUPLICATE KEY UPDATE` 等需要关系型执行语义的语句。

### 5.2 UPDATE

支持条件：

- UPDATE tag 时 WHERE 必须能定位 VID，或明确允许受控属性更新。
- UPDATE edge 时 WHERE 必须能定位 src/dst/rank 或受控 edge 范围。
- SET 子句只包含可映射属性列。

默认拒绝无 WHERE 的 UPDATE。若后续需要批量更新，必须通过显式配置和额外测试开启。

### 5.3 DELETE

支持条件：

- DELETE tag 时 WHERE 能定位 VID 或受控范围。
- DELETE edge 时 WHERE 能定位 src/dst/rank 或受控 edge 范围。

默认拒绝无 WHERE 的 DELETE。DELETE 只删除映射对象，不模拟关系型级联、外键或触发器副作用；trigger 分区可在网关执行链路上追加行为。

## 6. DDL 映射规则

`CREATE TABLE` 必须通过 mapping hint、配置或约定判断是 tag 还是 edge。推荐在 SQL comment hint 或网关配置中显式指定，避免根据列名猜测图对象类型。

示例约束：

- tag table 必须有 id column。
- edge table 必须有 src、dst，可选 rank。
- 列类型只支持 Nebula 可映射类型。
- 不支持 MySQL engine、charset、collation、index、foreign key、check constraint 等选项。

DDL 成功后输出 `ddlDescriptor`，包含 schema 对象、操作类型、列定义和 mapping 变更摘要，供 metadata 和 binlog stream 使用。

## 7. 错误模型

错误必须结构化：

- `NEBULA_SQL_WRITE_UNSUPPORTED_STATEMENT`
- `NEBULA_SQL_WRITE_MISSING_ID`
- `NEBULA_SQL_WRITE_UNMAPPED_COLUMN`
- `NEBULA_SQL_WRITE_UNSAFE_UPDATE`
- `NEBULA_SQL_DDL_UNSUPPORTED_OPTION`
- `NEBULA_SQL_TRANSACTION_UNSUPPORTED`

JDBC 层可映射为 `SQLFeatureNotSupportedException` 或 `SQLException`。MySQL gateway 可映射为 ERR packet。错误消息必须说明不支持原因，不允许回退为原始 SQL 直通。

## 8. 事务与一致性边界

BEGIN、COMMIT、ROLLBACK 默认返回不支持。单条写入或 DDL 的成功以 Nebula 执行结果为准，不模拟 MySQL 事务。

若写入执行成功但后续 binlog append、trigger after hook 或 routine 后续语句失败，跨分区策略由对应 change 定义。本 change 只保证 mutation executor 返回足够的 descriptor 和 trace，让上层可以做一致性决策。

## 9. 测试策略

- converter 单元测试：INSERT、UPDATE、DELETE、CREATE/DROP/DESCRIBE 的 golden nGQL 和拒绝场景。
- mapping 测试：tag、edge、缺少 id、缺少 src/dst、未映射列、类型不兼容。
- executor 测试：使用 fake Nebula executor 验证 plan 被正确执行，result descriptor 正确生成。
- JDBC 路径测试：`executeUpdate` 对受支持写入返回 affectedRows，对事务和复杂 DDL 返回明确异常。
- 边界测试：无 WHERE UPDATE/DELETE、复杂 ALTER、索引、外键、`INSERT ... SELECT`、事务语句。

测试不依赖真实 NebulaGraph；真实 Nebula 联调可作为后续兼容验证。

## 10. 分区依赖

- 依赖 `add-nebula-sql-parser-core` 的稳定 IR。
- 复用 `add-nebula-sql-to-ngql-query-support` 的 schema mapping 模型。
- 被 `add-nebula-mysql-protocol-write-and-advanced-support` 用作 MySQL 网关写入/DDL 执行边界。
- 被 `add-nebula-mysql-binlog-compatible-stream` 消费 mutation/ddl descriptor。
- routine 和 trigger 分区通过同一 executor 复用写入能力。

## 11. 验收要点

- 受支持 INSERT、UPDATE、DELETE 可转换为 nGQL 并通过统一 executor 执行。
- 受支持基础 DDL 可映射为 Nebula schema 操作或 metadata plan。
- 不安全写入、复杂 DDL 和事务语义返回明确错误。
- 执行结果提供稳定 descriptor，后续 binlog、trigger、routine 不需要重新解析 SQL。
