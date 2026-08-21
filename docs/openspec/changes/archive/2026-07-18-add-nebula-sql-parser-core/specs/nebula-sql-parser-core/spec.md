## ADDED Requirements

### Requirement: SQL parser exposes a stable intermediate representation
系统 SHALL 将受支持 SQL 解析为稳定的 AST 或中间表示，供后续转换层消费。

#### Scenario: 解析 SELECT 语句
- **WHEN** 输入 `SELECT * FROM person WHERE id = 'p1'` 
- **THEN** 解析结果包含语句类型、目标表、投影列和过滤条件。

#### Scenario: 解析写入和 DDL 语句
- **WHEN** 输入基础 INSERT、UPDATE、DELETE 或 CREATE TABLE 语句
- **THEN** 解析结果标识语句类型和关键结构。

#### Scenario: 不暴露 Druid AST 给后续层
- **WHEN** 后续 SQL→nGQL、写入/DDL、JDBC 框架适配或 MySQL 协议能力消费 parser 输出
- **THEN** 它们只依赖稳定的 Nebula SQL 中间表示，不直接依赖 Druid AST 类型或 visitor 实现。

### Requirement: Complex SQL constructs are represented explicitly
系统 SHALL 解析复杂 SQL 构造，并在中间表示中显式表达 JOIN、子查询、聚合、分组、HAVING、UNION、函数、表达式和别名等结构。

#### Scenario: 解析多表 JOIN 与别名
- **WHEN** 输入包含多表 JOIN、表别名和列别名的 SELECT 语句
- **THEN** 解析结果包含 JOIN 类型、JOIN 条件、别名映射和投影结构

#### Scenario: 解析子查询和聚合
- **WHEN** 输入包含子查询、聚合函数、GROUP BY 或 HAVING 的 SELECT 语句
- **THEN** 解析结果保留嵌套查询、聚合表达式、分组字段和 HAVING 条件

#### Scenario: 解析 UNION 和函数表达式
- **WHEN** 输入包含 UNION、内置函数或复杂表达式
- **THEN** 解析结果能区分集合操作、函数调用和表达式树

#### Scenario: 保留参数占位符位置
- **WHEN** 输入包含 `?` 参数占位符的 SQL
- **THEN** 解析结果按出现顺序保留 1-based 参数位置，不做字符串替换或参数值绑定。

### Requirement: Unsupported SQL reports clear errors
系统 SHALL 对语法错误或暂不支持的 SQL 返回明确、可定位的异常信息。

#### Scenario: 非法 SQL
- **WHEN** 输入无法解析的 SQL 
- **THEN** 返回包含错误位置或错误原因的异常。

#### Scenario: 暂不支持语法
- **WHEN** 输入 parser 识别但当前不支持的 SQL 特性
- **THEN** 返回明确的不支持原因。

#### Scenario: 矩阵驱动的支持边界
- **WHEN** parser core 验证 `define-nebula-sql-ast-coverage-matrix` 产出的矩阵 JSON
- **THEN** `required` 条目必须产生结构化中间表示，`explicitlyUnsupported` 条目必须返回结构化不支持错误，`deferred` 条目必须保留延期原因。

#### Scenario: 禁止 raw SQL 语义兜底
- **WHEN** Druid 能解析的矩阵 required 条目进入 parser core
- **THEN** 系统不得用 raw SQL 文本替代结构化中间表示；raw SQL 仅可作为脱敏、截断后的诊断上下文。

### Requirement: Parser does not execute Nebula queries
SQL parser 核心 SHALL 只负责解析与基础语义校验，不直接访问 NebulaGraph。

#### Scenario: 纯解析调用
- **WHEN** 调用 parser API 
- **THEN** 不创建 Nebula 连接、不调用 `NebulaClientFacade`，只返回解析结果或错误。

#### Scenario: 不做 schema 映射或 nGQL 生成
- **WHEN** parser API 解析 SQL 表、列、DDL 或 JOIN 结构
- **THEN** 结果只保留 SQL 结构，不判断 tag/edge/property/id 映射，也不生成 nGQL。
