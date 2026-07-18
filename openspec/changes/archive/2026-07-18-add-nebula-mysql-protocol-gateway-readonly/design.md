## Context

本 change 属于 Nebula SQL / MySQL 协议支持路线的一部分。当前 `fw-plus-nebula-jdbc` 已具备稳定的 nGQL JDBC 基础能力，但完整 SQL 与 MySQL 协议能力需要分阶段建设。

前置依赖：依赖 `add-nebula-sql-parser-core` 和 `add-nebula-sql-to-ngql-query-support`。

## Goals / Non-Goals

**Goals:**

- 新增 MySQL wire protocol 服务端基础网关。
- 支持握手、认证、COM_QUERY 和文本协议结果集返回。
- 支持 SELECT、复杂只读 SQL、SHOW DATABASES、SHOW TABLES、DESCRIBE 等只读语句，复杂 SQL 语义由 SQL→nGQL 查询转换层判定。
- 支持 MySQL CLI 和兼容 MySQL wire protocol 客户端的基础连接与查询，包括客户端发起的复杂只读 SQL；GUI 客户端真实验收在具备客户端环境后作为后续人工验收执行。

**Non-Goals:**


- MySQL binlog、复制协议、存储过程和触发器不在本 change 内实现；这些能力由后续独立 changes 承接。
- 不扩大到本拆分项之外的后续能力。
- 不破坏现有 `jdbc:nebula://...` nGQL 直连路径。
- 不追求 100% MySQL Server 或 SQL 标准兼容。

## Decisions

- 第一阶段只读，降低协议和语义风险。
- 协议编解码与 SQL 执行层分离，便于后续写入和高级协议扩展。
- 认证先采用可配置的简单认证，权限体系留给高级协议 change。

## Risks / Trade-offs

- **[Risk] SQL 与图模型语义存在天然差异。** → Mitigation：采用明确白名单和可测试映射，不支持时返回清晰错误。
- **[Risk] 客户端兼容范围可能快速扩大。** → Mitigation：按 change 分阶段交付，每阶段定义验收客户端和协议子集；当前 build 退出条件只要求可自动/CLI 证明的连接查询，GUI 客户端验收保留为后续人工记录。
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
