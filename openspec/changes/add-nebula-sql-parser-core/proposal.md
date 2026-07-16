## Why

当前 Nebula JDBC 只稳定了 nGQL 直连基础能力，尚不能接受用户熟悉的 SQL 语句。要支持后续 SQL→nGQL 转换和 MySQL 协议接入，必须先建立独立、可测试的 SQL 解析核心。

## What Changes

- 新增 SQL parser 核心能力，输出稳定的 SQL AST 或中间表示。
- 覆盖 SELECT、INSERT、UPDATE、DELETE、基础 DDL 以及复杂 SQL 构造（多表 JOIN、子查询、聚合、GROUP BY、HAVING、UNION、函数、表达式、别名）的语法识别框架。
- 提供方言边界、标识符、字面量、表达式、表/列引用、参数占位符、复杂查询结构和嵌套查询解析。
- 对非法 SQL 或暂不支持的语法返回明确错误，不直接执行 Nebula 查询。

## Capabilities

### New Capabilities
- `nebula-sql-parser-core`: SQL 解析核心，覆盖本 change 的需求、边界和验收场景。

### Modified Capabilities

## Impact


- MySQL binlog、复制协议、存储过程和触发器不在本 change 内实现；这些能力由后续独立 changes 承接。
- 影响 `fw-plus-nebula-jdbc` 或新建相邻 SQL 子包。
- 可能引入 SQL parser 依赖或自研轻量 parser，需要在 design 阶段决策。
- 为后续 SQL→nGQL 和 MySQL 协议 change 提供基础能力。
