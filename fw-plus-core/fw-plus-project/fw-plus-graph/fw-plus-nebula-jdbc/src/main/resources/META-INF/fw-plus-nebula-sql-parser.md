# fw-plus Nebula SQL Parser Core

本模块提供第一阶段 Nebula SQL parser core，入口为 `NebulaSqlParser`，默认实现为 `DruidNebulaSqlParser`。

支持边界：

- 使用 Druid MySQL parser 解析单条 SQL，多语句输入返回 `UNSUPPORTED_FEATURE`。
- 输出自有 IR：SELECT、INSERT、UPDATE、DELETE、CREATE/DROP TABLE、SHOW、DESCRIBE、JOIN、子查询、UNION、GROUP BY、HAVING、ORDER BY、LIMIT、函数、表达式、别名和 `?` 参数占位符。
- `?` 只记录 1-based 出现位置，不绑定参数值，不做字符串替换。
- `CALL ...` 返回 `UNSUPPORTED_FEATURE`，矩阵 ID 为 `SQL-UNSUPPORTED-PROCEDURE`。
- `CREATE TRIGGER ...` 由 trigger DDL handler 在 SQL 执行入口处理；parser-core 返回 `UNSUPPORTED_FEATURE`，矩阵 ID 为 `SQL-UNSUPPORTED-TRIGGER-DDL`。
- raw SQL 只作为脱敏截断后的诊断摘要保存，`ParserDiagnostic.isRawFallback()` 对成功 required 路径保持 `false`。

非目标：

- 不生成 nGQL。
- 不创建 Nebula 连接，不调用 `NebulaClientFacade`。
- 不做 SQL 表、列、DDL 到 Nebula tag、edge、property、id 的 schema 映射。
- 不接入现有 `NebulaStatementImpl` / `NebulaPreparedStatementImpl` nGQL 直连执行路径。

后续 SQL to nGQL converter、写入/DDL converter 和 JDBC 框架集成只应消费 `NebulaSqlParseResult` 与 `parser.ast` 包中的自有 IR，不应直接依赖 Druid AST。
