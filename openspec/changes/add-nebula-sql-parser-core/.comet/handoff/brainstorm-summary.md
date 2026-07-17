# Brainstorm Summary

- Change: add-nebula-sql-parser-core
- Date: 2026-07-17

## 确认的技术方案

采用 Druid MySQL parser + 自有 Nebula SQL IR adapter。Parser core 以 `com.alibaba:druid:1.2.24` 为解析基线，消费前置 `define-nebula-sql-ast-coverage-matrix` 归档产出的 JSON 矩阵作为 adapter 完整性测试事实源。运行时 API 返回自有、与执行层解耦的 Nebula SQL 中间表示，不向 SQL→nGQL 转换层、写入/DDL 转换层、JDBC 框架适配层或 MySQL protocol 网关暴露 Druid AST。

Parser core 只负责 SQL 文本解析、基础结构规范化、参数占位符位置保留和结构化错误建模；不做 schema 映射、不生成 nGQL、不调用 `NebulaConnection`/`NebulaClientFacade`，也不改变现有 `jdbc:nebula://...` nGQL 直连路径。

## 关键取舍与风险

- 取舍：parser 尽量完整表达 SQL 结构，转换层再决定哪些语义可映射到 NebulaGraph。这样会增加 IR 类型数量，但能避免 parser 与查询/写入/协议执行策略耦合。
- 取舍：不采用自研轻量 parser，因为复杂 SQL、框架生成 SQL 和 Druid 矩阵验收会变成重复成本。
- 取舍：不把 Druid AST 暴露给后续层，避免后续 changes 对 Druid 版本、visitor 结构和 dialect 细节产生硬依赖。
- 风险：IR 过早承诺执行语义。缓解：IR 只表达语法结构、标识符、字面量、表达式树、参数位置和 statement kind；schema 映射、图语义白名单、nGQL 输出全部留给后续 converter changes。
- 风险：Druid 升级或 AST 行为漂移。缓解：矩阵驱动测试必须读取归档 JSON；required 条目缺 adapter 失败，explicitlyUnsupported/deferred 返回结构化错误。
- 风险：raw SQL 被误用为语义 fallback。缓解：raw SQL 只允许作为脱敏、截断后的诊断上下文，不能作为 IR 替代。

## 测试策略

- 单元测试覆盖 SELECT、INSERT、UPDATE、DELETE、CREATE/DROP、SHOW/DESCRIBE、JOIN、子查询、UNION、GROUP BY/HAVING、ORDER BY/LIMIT、函数、二元表达式、IN、LIKE、IS NULL、别名和 `?` 参数占位符。
- 矩阵驱动测试读取 `openspec/changes/archive/2026-07-17-define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json`，逐条验证 `supportLevel` 对应行为，并在失败信息中包含 `id`、`druidAstType`、`exampleSql`。
- 错误测试区分语法错误、暂不支持、延期语法和 adapter 缺口；错误对象包含稳定错误类型、原因、可选位置、矩阵 ID。
- 纯解析测试断言 parser API 不创建 Nebula 连接、不调用 `NebulaClientFacade`、不执行任何 nGQL。

## Spec Patch

回写 `openspec/changes/add-nebula-sql-parser-core/specs/nebula-sql-parser-core/spec.md`，补充以下验收边界：

- parser-core 必须消费前置矩阵 JSON 作为完整性测试事实源。
- parser-core 不得向后续转换层或协议层暴露 Druid AST。
- parser-core 必须保留 `?` 参数占位符的位置和顺序。
- raw SQL 只能用于诊断，不能作为语义 fallback。
- parser-core 不做 schema 映射、不生成 nGQL、不访问 NebulaGraph。

同步更新 `openspec/changes/add-nebula-sql-parser-core/tasks.md`，增加矩阵驱动测试、IR 边界和相邻 change 接口契约相关 build 任务。
