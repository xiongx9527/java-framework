## Context

本 change 属于 Nebula SQL / MySQL 协议支持路线的一部分。当前 `fw-plus-nebula-jdbc` 已具备稳定的 nGQL JDBC 基础能力，但完整 SQL 与 MySQL 协议能力需要分阶段建设。

前置依赖：依赖 `add-nebula-sql-parser-core` 提供稳定 SQL 中间表示。

## Goals / Non-Goals

**Goals:**

- 新增 SQL SELECT 到 nGQL 的转换层。
- 支持点查询、边查询、投影、过滤、排序、分页，以及 JOIN、子查询、聚合、GROUP BY、HAVING、UNION、函数表达式等复杂 SQL 的可映射子集。
- 通过现有 `NebulaClientFacade` 执行转换后的 nGQL。
- 对无法安全映射的 SQL 返回明确错误。

**Non-Goals:**


- MySQL binlog、复制协议、存储过程和触发器不在本 change 内实现；这些能力由后续独立 changes 承接。
- 不扩大到本拆分项之外的后续能力。
- 不破坏现有 `jdbc:nebula://...` nGQL 直连路径。
- 不追求 100% MySQL Server 或 SQL 标准兼容。

## Decisions

- 转换层只消费 parser 中间表示，不直接解析 SQL 字符串。
- schema 映射规则独立建模，用于区分 tag、edge、属性和 id 字段。
- 复杂 SQL 支持采用显式白名单：JOIN、子查询、聚合、UNION 和函数表达式必须有明确图语义映射；不能映射时 fail fast。
- Build 阶段 2.4 收敛为可验证子集：显式 tag-edge-tag IR、`COUNT(mapped_column)` 聚合/GROUP BY/HAVING 别名过滤、同形 UNION 和 `lower`/`upper`/`length` 函数白名单。子查询转换继续返回 `UNSUPPORTED_SUBQUERY`，建议拆分为后续 graph subquery template change。

## Risks / Trade-offs

- **[Risk] 复杂 SQL 与图模型语义存在天然差异。** → Mitigation：采用明确白名单和可测试映射，不支持时返回清晰错误。
- **[Risk] 客户端兼容范围可能快速扩大。** → Mitigation：按 change 分阶段交付，每阶段定义验收客户端和协议子集。
- **[Risk] 引入第三方 parser 或协议库可能带来许可证和 Java 8 兼容风险。** → Mitigation：在 design/build 阶段先做依赖评估，再落地实现。

## Migration Plan

1. 在本 change 内完成设计与测试边界确认。
2. 以不破坏现有 nGQL 直连能力为前提新增能力。
3. 每个后续 change 只依赖前置产物的稳定 API，不回头扩大前置范围。
4. 如发现范围过大，优先继续拆分而不是在单个 change 中堆叠。
5. 子查询转换不在本 change 内伪完成；后续 change 应先定义 nGQL 子查询/管道模板、相关子查询拒绝策略和中间结果物化边界。

## Open Questions

- 是否采用第三方 parser / protocol 库，或自研轻量实现？
- SQL schema 与 Nebula tag/edge/property/id 的最终映射配置放在哪一层？
- 是否需要 Spring Boot starter、Magic-API 插件或独立服务入口？
