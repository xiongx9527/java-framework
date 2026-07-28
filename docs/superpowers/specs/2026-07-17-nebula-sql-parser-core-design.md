---
comet_change: add-nebula-sql-parser-core
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-18-add-nebula-sql-parser-core
status: final
---

# Nebula SQL Parser Core 技术设计

## 背景

`fw-plus-nebula-jdbc` 当前稳定路径是 nGQL 直连：`NebulaStatementImpl`/`NebulaPreparedStatementImpl` 接收 nGQL 字符串，经 `NebulaStatementClassifier` 做轻量 query/update 分类，再通过 `NebulaConnection.execute` 调用 `NebulaClientFacade.executeNgql`。这条路径不解析 SQL，也不应该被 parser core 的引入破坏。

本 change 是 Nebula SQL / MySQL 协议路线的底座。它的职责是在执行层之前新增独立、可测试的 SQL parser core，输出稳定的 Nebula SQL 中间表示，供后续 `add-nebula-sql-to-ngql-query-support`、`add-nebula-sql-write-and-ddl-support`、MySQL protocol 网关和 JDBC 框架集成消费。

前置 `define-nebula-sql-ast-coverage-matrix` 已归档，机器事实源是 `openspec/changes/archive/2026-07-17-define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json`。该矩阵锁定 `com.alibaba:druid:1.2.24`，包含 23 个条目：21 个 `required`、1 个 `explicitlyUnsupported`、1 个 `deferred`。Parser core 的验收必须直接消费该 JSON 或由它生成测试输入。

## 目标与非目标

目标：

- 使用 Druid MySQL parser 解析 SQL，并通过 adapter 输出自有 Nebula SQL IR。
- 覆盖矩阵中 required 条目的语句、查询结构、表达式、metadata 和框架生成 SQL 形态。
- 表达 SELECT、INSERT、UPDATE、DELETE、基础 DDL、SHOW/DESCRIBE、JOIN、子查询、UNION、聚合、GROUP BY、HAVING、ORDER BY、LIMIT、函数、表达式、别名、字面量和参数占位符。
- 对语法错误、明确不支持和延期语法返回结构化错误。
- 保持 parser core 与 Nebula 执行、schema 映射、MySQL protocol 编解码解耦。

非目标：

- 不生成 nGQL。
- 不做 SQL 表/列到 Nebula tag/edge/property/id 的 schema 映射。
- 不调用 `NebulaConnection`、`NebulaClientFacade` 或任何 NebulaGraph 连接。
- 不接入 JDBC Statement 执行路径。
- 不实现 MySQL wire protocol、binlog、复制协议、存储过程或触发器。
- 不追求 100% MySQL Server 或 SQL 标准兼容。

## 推荐方案

采用 **Druid MySQL parser + 自有 Nebula SQL IR adapter**。

Druid 负责词法、语法和 MySQL dialect AST 生成；adapter 负责把 Druid AST 转成项目自有 IR。后续 query converter、write/DDL converter、JDBC 框架适配和 MySQL protocol 网关只依赖自有 IR，不直接依赖 Druid AST、Druid visitor 或具体 dialect 类。

不采用自研轻量 parser。虽然依赖更少，但 JOIN、子查询、UNION、GROUP BY/HAVING、函数、框架生成 SQL 和错误定位会显著扩大实现成本，并且会绕开前置 Druid AST 覆盖矩阵。

不采用“直接暴露 Druid AST”。它实现短，但会把后续所有转换层和协议层绑定到 Druid 版本与 AST 结构，违背 open 阶段 design 中“避免 MySQL 协议、JDBC 和转换器直接依赖 Druid AST”的决策。

## 模块与包边界

优先在现有 `fw-plus-nebula-jdbc` 模块内新增独立 SQL parser 包，例如：

- `com.vesoft.nebula.jdbc.sql.parser`
- `com.vesoft.nebula.jdbc.sql.parser.ast`
- `com.vesoft.nebula.jdbc.sql.parser.druid`
- `com.vesoft.nebula.jdbc.sql.parser.error`

当前 `fw-plus-nebula` 聚合下只有 `fw-plus-nebula-jdbc` 子模块。先在同一 Maven artifact 内落地可以降低拆分成本；只要 API 不依赖 JDBC 执行类、不暴露 Druid 类型，后续需要独立 `fw-plus-nebula-sql-parser` 模块时可以平滑迁移。

`fw-plus-nebula-jdbc` 需要新增 `com.alibaba:druid` 依赖，版本来自 core BOM 已管理的 `druid.version=1.2.24`。不引入 JSqlParser 作为本 change 的 parser 实现，因为前置矩阵和验收边界均围绕 Druid MySQL AST。

## 对外 API

建议暴露一个窄接口：

```java
public interface NebulaSqlParser {
	NebulaSqlParseResult parse(String sql) throws NebulaSqlParseException;
}
```

`NebulaSqlParseResult` 至少包含：

- `NebulaSqlStatement statement`：解析后的自有 IR 根节点。
- `StatementKind kind`：`SELECT`、`INSERT`、`UPDATE`、`DELETE`、`CREATE_SCHEMA`、`DROP_SCHEMA`、`METADATA` 等稳定枚举。
- `List<NebulaSqlParameter> parameters`：按出现顺序和 1-based index 记录 `?` 占位符。
- `ParserDiagnostic diagnostic`：可选诊断信息，包含 SQL 摘要、矩阵 ID、源 parser 类型等非语义字段。

`parse` 成功返回 IR；语法错误、明确不支持和延期语法抛出结构化异常。后续 JDBC 或 MySQL protocol 层负责把内部异常转成 `SQLException` 或 MySQL 协议错误，不要求 parser core 自己绑定某一种外部错误格式。

## IR 设计

IR 使用 Java 8 兼容的普通类和枚举，不使用 record、sealed class、`List.of()` 等 Java 9+ API。

核心节点建议包括：

- `NebulaSqlStatement`：根接口，暴露 `StatementKind getKind()`。
- `NebulaSelectStatement`：投影、from、where、groupBy、having、orderBy、limit、union。
- `NebulaInsertStatement`：目标表、列列表、values、selectSource。
- `NebulaUpdateStatement`：目标表、set items、where。
- `NebulaDeleteStatement`：目标表、where。
- `NebulaSchemaStatement`：CREATE/DROP table 类 DDL 的结构化描述。
- `NebulaMetadataStatement`：SHOW TABLES、SHOW COLUMNS、DESCRIBE 等 metadata 形态。
- `NebulaTableSource`：表、别名、join、子查询 source。
- `NebulaJoin`：join 类型、左右 source、join 条件。
- `NebulaExpression`：二元表达式、函数调用、聚合函数、列引用、字面量、参数、IN、LIKE、IS NULL、属性引用。
- `NebulaSelectItem`：表达式、别名、通配符。

IR 的职责是“表达 SQL 结构”，不是“解释图语义”。例如 `JOIN` 只保留 join 类型和条件，不判断是否能映射为 edge traversal；`CREATE TABLE` 只保留表名、列定义和约束信息，不判断应创建 TAG 还是 EDGE；`id` 列只作为普通列引用保留，不在 parser 层绑定到 VID。

## Druid Adapter

`DruidNebulaSqlParser` 负责调用 Druid：

```text
DruidNebulaSqlParser.parse(sql)
  -> SQLUtils.parseStatements(sql, JdbcConstants.MYSQL)
  -> 校验单语句或明确拒绝多语句
  -> DruidNebulaAstAdapter.adapt(SQLStatement)
  -> NebulaSqlParseResult
```

Adapter 规则：

- Druid AST 只在 `parser.druid` 包内可见。
- 所有 `required` 矩阵条目必须有 adapter 覆盖或由包含节点递归输出。
- `explicitlyUnsupported` 条目必须返回 `UNSUPPORTED_FEATURE` 错误，错误包含矩阵 ID 和原因。
- `deferred` 条目必须返回 `DEFERRED_FEATURE` 错误，错误包含延期原因和后续 change 边界。
- Druid 内部容器、字面量、标识符等不单独扩张成独立 SQL 构造，但必须作为父节点的结构化内容保留。

多语句输入默认拒绝，避免 parser core 在未定义批处理和事务语义时给后续执行层制造歧义。后续如要支持 batch，应由独立 change 定义语义和返回模型。

## 错误模型

建议定义内部异常：

```java
public class NebulaSqlParseException extends Exception {
	private final NebulaSqlParseError error;
}
```

`NebulaSqlParseError` 包含：

- `ErrorKind kind`：`SYNTAX_ERROR`、`UNSUPPORTED_FEATURE`、`DEFERRED_FEATURE`、`ADAPTER_GAP`、`INVALID_INPUT`。
- `String message`：面向调用方的清晰原因。
- `String matrixId`：矩阵驱动路径下的稳定 ID，可为空。
- `String druidAstType`：adapter 缺口定位信息，可为空。
- `Integer line`、`Integer column`：Druid 能提供时记录。
- `String sqlSnippet`：脱敏、截断后的 SQL 摘要，仅用于诊断。

不得把 raw SQL 当作语义 fallback。raw SQL 只能进入诊断字段，并且默认脱敏、截断，禁止记录绑定参数值。

## 参数占位符

Parser core 必须识别 `?` 并保留 1-based 参数位置和出现顺序：

```text
SELECT * FROM person WHERE id = ? AND status IN (?, ?)
  -> parameters: [1, 2, 3]
```

IR 中的参数节点只表示“这里有第 N 个占位符”，不保存参数值。PreparedStatement 的参数绑定、JDBC 类型、空值和类型转换由后续 JDBC/framework integration change 负责。SQL→nGQL converter 后续消费 IR + 参数绑定模型，不能依赖 parser 做字符串替换。

## 与相邻 Change 的接口边界

`add-nebula-sql-to-ngql-query-support`：

- 只消费 `NebulaSqlParseResult` / `NebulaSelectStatement`。
- 负责 schema 映射、图语义白名单、SELECT 到 nGQL 输出和执行接入。
- 不直接解析 SQL 字符串，不依赖 Druid AST。

`add-nebula-sql-write-and-ddl-support`：

- 消费 `NebulaInsertStatement`、`NebulaUpdateStatement`、`NebulaDeleteStatement`、`NebulaSchemaStatement`。
- 负责写入安全条件、事务差异、CREATE TABLE 到 TAG/EDGE 的映射。
- parser core 不猜测 id、tag、edge 或 property 映射。

MySQL protocol 系列：

- 只读网关消费 query converter 和 metadata 能力，不直接访问 Druid AST。
- 写入/高级协议通过 write/DDL converter 执行，不在 parser core 中处理 COM_QUERY、prepared statement 二进制协议、认证、权限或 SSL。
- protocol 层负责把 parser/converter 错误映射成 MySQL 客户端可理解的错误包。

JDBC 客户端框架集成：

- PreparedStatement 参数绑定应作为类型化参数模型传给 converter。
- parser core 只保留 `?` 位置，不做参数字符串替换。
- ResultSetMetaData、列标签和 JDBC 类型由执行/结果层提供，不在 parser core 中生成。

现有 nGQL 直连路径：

- `NebulaStatementClassifier` 继续服务 nGQL query/update 分类。
- 本 change 不把 `Statement.executeQuery(String)` 改成 SQL 执行入口。
- SQL 执行路径由后续 query converter/JDBC integration change 明确接入。

## 矩阵驱动验收

Build 阶段必须加入测试，读取：

`openspec/changes/archive/2026-07-17-define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json`

测试分流：

- `required`：解析 `exampleSql`，断言生成 `nebulaAstKind` 对应 IR，且没有 raw fallback。
- `explicitlyUnsupported`：断言返回 `UNSUPPORTED_FEATURE`，错误包含矩阵 ID。
- `deferred`：断言返回 `DEFERRED_FEATURE` 或测试报告明确记录延期原因。

失败信息至少包含：

- `id`
- `supportLevel`
- `druidAstType`
- `exampleSql`
- 实际错误类型或缺失 IR 类型

这样后续 Druid 升级、矩阵调整或 adapter 漏项都能通过测试定位，而不是只靠人工阅读文档。

## 测试策略

测试分三层：

1. Parser API 单元测试：覆盖 OpenSpec 中的基础 SELECT、写入、DDL、复杂 SQL、非法 SQL 和不支持语法。
2. Adapter 矩阵测试：读取前置矩阵 JSON，验证全部 required/unsupported/deferred 条目。
3. 纯解析隔离测试：通过代码依赖边界或 fake classpath 断言 parser 测试不需要真实 NebulaGraph，不创建连接，不调用 `NebulaClientFacade`。

重点用例：

- `SELECT * FROM person WHERE id = 'p1'`
- `SELECT p.id, o.amount FROM person p JOIN orders o ON p.id = o.person_id`
- `SELECT t.id FROM (SELECT id FROM person WHERE age > 18) t`
- `SELECT city, COUNT(*) c FROM person GROUP BY city HAVING COUNT(*) > 1`
- `SELECT id FROM person_a UNION SELECT id FROM person_b`
- `INSERT INTO person (id, name) VALUES (?, ?)`
- `UPDATE person SET name = ? WHERE id = ?`
- `DELETE FROM person WHERE id = ?`
- `CREATE TABLE person (id BIGINT PRIMARY KEY, name VARCHAR(64))`
- `SHOW TABLES`、`SHOW COLUMNS FROM person`、`DESCRIBE person`
- `CALL refresh_person()` 返回 unsupported
- `CREATE TRIGGER ...` 返回 deferred

## 风险与缓解

复杂 SQL 和图模型语义差异大。缓解方式是 parser 保留结构，不承诺可执行；后续 converter 按 schema 映射和白名单判断。

IR 类型可能膨胀。缓解方式是先覆盖矩阵和 OpenSpec 要求的结构，避免把每个 Druid 叶子节点都变成公开节点；字面量、标识符、属性引用作为表达式树的一部分保留。

Druid AST 行为可能随版本变化。缓解方式是锁定 BOM 版本，并要求升级时重新运行矩阵枚举和 parser-core adapter 测试。

后续层可能误用 raw SQL。缓解方式是在 API 和测试中明确 raw SQL 是诊断字段，不是语义输出；required 矩阵条目不能返回 raw-only 结果。

## Build 阶段执行顺序

1. 在 `fw-plus-nebula-jdbc` 中加入 Druid 依赖和 parser 包骨架。
2. 定义 IR、parser API、错误模型和 Druid adapter 边界。
3. 实现基础 statement 和 expression adapter。
4. 接入矩阵驱动测试，并先让测试暴露缺口。
5. 补齐 required adapter，固定 unsupported/deferred 错误。
6. 增加文档，说明第一阶段 SQL 方言边界和后续 converter 依赖。

整个 build 阶段仍不得改变现有 nGQL 直连执行行为，除非后续 SQL query support change 明确接入。
