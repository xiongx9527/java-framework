## Context

本 change 属于 Nebula SQL / MySQL 协议支持路线的一部分。当前 `fw-plus-nebula-jdbc` 已具备稳定的 nGQL JDBC 基础能力，但完整 SQL 与 MySQL 协议能力需要分阶段建设。

前置依赖：依赖前置 MySQL 只读网关和 SQL 写入/DDL 支持。

## Goals / Non-Goals

**Goals:**

- 扩展 MySQL 协议网关支持写入和 DDL。
- 支持 prepared statement 常见路径。
- 支持权限、SSL 或可配置安全策略。
- 增强 Java MySQL JDBC Driver、BI/GUI metadata fixture 的兼容，并记录真实 GUI/BI 客户端的后续人工验收事项。

**Non-Goals:**


- MySQL binlog、复制协议、存储过程和触发器不在本 change 内实现；这些能力由后续独立 changes 承接。
- 不扩大到本拆分项之外的后续能力。
- 不破坏现有 `jdbc:nebula://...` nGQL 直连路径。
- 不追求 100% MySQL Server 或 SQL 标准兼容。

## Decisions

- 高级协议能力必须建立在只读网关稳定后，不与基础协议混在同一阶段。
- prepared statement 支持优先覆盖 Java MySQL JDBC Driver 常见文本/二进制路径。
- 权限与 SSL 先做可配置策略，不实现完整 MySQL 用户系统。

## Risks / Trade-offs

- **[Risk] SQL 与图模型语义存在天然差异。** → Mitigation：采用明确白名单和可测试映射，不支持时返回清晰错误。
- **[Risk] 客户端兼容范围可能快速扩大。** → Mitigation：按 change 分阶段交付，每阶段定义自动验收客户端、metadata fixture、协议子集和后续人工验收事项。
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
