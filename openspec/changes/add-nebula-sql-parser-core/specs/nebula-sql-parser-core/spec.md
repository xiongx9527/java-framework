## ADDED Requirements

### Requirement: SQL parser exposes a stable intermediate representation
系统 SHALL 将受支持 SQL 解析为稳定的 AST 或中间表示，供后续转换层消费。

#### Scenario: 解析 SELECT 语句
- **WHEN** 输入 `SELECT * FROM person WHERE id = 'p1'` 
- **THEN** 解析结果包含语句类型、目标表、投影列和过滤条件。

#### Scenario: 解析写入和 DDL 语句
- **WHEN** 输入基础 INSERT、UPDATE、DELETE 或 CREATE TABLE 语句
- **THEN** 解析结果标识语句类型和关键结构。

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

### Requirement: Unsupported SQL reports clear errors
系统 SHALL 对语法错误或暂不支持的 SQL 返回明确、可定位的异常信息。

#### Scenario: 非法 SQL
- **WHEN** 输入无法解析的 SQL 
- **THEN** 返回包含错误位置或错误原因的异常。

#### Scenario: 暂不支持语法
- **WHEN** 输入 parser 识别但当前不支持的 SQL 特性
- **THEN** 返回明确的不支持原因。

### Requirement: Parser does not execute Nebula queries
SQL parser 核心 SHALL 只负责解析与基础语义校验，不直接访问 NebulaGraph。

#### Scenario: 纯解析调用
- **WHEN** 调用 parser API 
- **THEN** 不创建 Nebula 连接、不调用 `NebulaClientFacade`，只返回解析结果或错误。

