## Context

本 change 属于 Nebula SQL / MySQL 协议支持路线的一部分。当前 `fw-plus-nebula-jdbc` 已具备稳定的 nGQL JDBC 基础能力，但完整 SQL 与 MySQL 协议能力需要分阶段建设。

前置依赖：`define-nebula-sql-ast-coverage-matrix`。本 change 按该矩阵实现 Druid MySQL AST 到 Nebula AST 的完整 adapter。

## Goals / Non-Goals

**Goals:**

- 新增 SQL parser 核心能力，输出稳定的 SQL AST 或中间表示，并能表达复杂 SQL 查询结构。
- 覆盖 SELECT、INSERT、UPDATE、DELETE、基础 DDL 以及 JOIN、子查询、聚合、GROUP BY、HAVING、UNION、函数、表达式、别名等复杂 SQL 构造的语法识别框架。
- 提供方言边界、标识符、字面量、表达式、表/列引用和参数占位符解析。
- 对非法 SQL 或暂不支持的语法返回明确错误，不直接执行 Nebula 查询。

**Non-Goals:**


- MySQL binlog、复制协议、存储过程和触发器不在本 change 内实现；这些能力由后续独立 changes 承接。
- 不扩大到本拆分项之外的后续能力。
- 不破坏现有 `jdbc:nebula://...` nGQL 直连路径。
- 不追求 100% MySQL Server 或 SQL 标准兼容。

## Decisions

- 按 `define-nebula-sql-ast-coverage-matrix` 的覆盖矩阵实现完整 adapter，parser 输出为与执行层解耦的中间表示，避免 MySQL 协议、JDBC 和转换器直接依赖 Druid AST。
- SQL 方言能力采用分层白名单：parser 尽量完整表达复杂 SQL 结构，转换层再决定哪些语义可映射到 nGQL，不能映射时返回明确错误。
- 错误模型使用 JDBC 可传播的异常类型或内部解析异常，并保留原始 SQL 与错误位置。

## Risks / Trade-offs

- **[Risk] 复杂 SQL 与图模型语义存在天然差异。** → Mitigation：parser 保留复杂结构，转换层采用明确白名单和可测试映射，不支持时返回清晰错误。
- **[Risk] 客户端兼容范围可能快速扩大。** → Mitigation：按 change 分阶段交付，每阶段定义验收客户端和协议子集。
- **[Risk] 引入第三方 parser 或协议库可能带来许可证和 Java 8 兼容风险。** → Mitigation：在 design/build 阶段先做依赖评估，再落地实现。

## Migration Plan

1. 在本 change 内完成设计与测试边界确认。
2. 以不破坏现有 nGQL 直连能力为前提新增能力。
3. 每个后续 change 只依赖前置产物的稳定 API，不回头扩大前置范围。
4. 如发现范围过大，优先继续拆分而不是在单个 change 中堆叠。

## Open Questions

- 是否采用第三方 parser / protocol 库，或自研轻量实现？
- SQL schema 与 Nebula tag/edge/property/id 的最终映射配置放在哪一层？
- 是否需要 Spring Boot starter、Magic-API 插件或独立服务入口？
