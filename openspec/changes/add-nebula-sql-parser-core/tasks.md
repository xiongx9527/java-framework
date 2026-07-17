## 1. 调研与架构决策

- [ ] 1.0 读取 `define-nebula-sql-ast-coverage-matrix` 产出的 Druid AST 覆盖矩阵，并确认实现范围。
- [ ] 1.1 评估自研 parser 与第三方 SQL parser 的兼容性、许可证、Java 8 支持和 AST 可控性。
- [ ] 1.2 定义 SQL AST / 中间表示和错误模型。
- [ ] 1.3 定义 Druid AST adapter 与自有 Nebula SQL IR 的包边界，确保后续转换层和协议层不依赖 Druid AST。
- [ ] 1.4 定义 raw SQL 诊断字段、脱敏截断规则和禁止语义 fallback 的测试断言。

## 2. Parser 核心实现

- [ ] 2.1 实现 SELECT 基础语法解析。
- [ ] 2.2 实现 INSERT、UPDATE、DELETE 基础语法解析。
- [ ] 2.3 实现基础 DDL 语法解析。
- [ ] 2.4 实现 JOIN、子查询、聚合、GROUP BY、HAVING、UNION、函数、表达式和别名等复杂 SQL 构造解析。
- [ ] 2.5 实现非法语法和不支持语法的明确错误。
- [ ] 2.6 保留 `?` 参数占位符的 1-based 位置和出现顺序，不在 parser core 中绑定参数值。

## 3. 测试与文档

- [ ] 3.1 补充 parser 单元测试，覆盖读写、DDL、复杂 SQL、非法 SQL 和暂不支持语法。
- [ ] 3.2 记录第一阶段 SQL 方言边界和后续转换层依赖。
- [ ] 3.3 补充矩阵驱动测试，读取前置覆盖矩阵 JSON，按 required、explicitlyUnsupported、deferred 分流验证 adapter 行为。
- [ ] 3.4 补充纯解析隔离测试，验证 parser API 不创建 Nebula 连接、不调用 `NebulaClientFacade`、不生成 nGQL。
