## Context

本 change 属于 Nebula MySQL 协议深度兼容路线。前置能力包括 SQL parser、SQL→nGQL 查询/写入转换、MySQL 协议网关基础能力。由于本 change 触及 MySQL 高级语义，必须明确兼容边界，避免承诺完整 MySQL Server 等价。

## Goals / Non-Goals

**Goals:**

- 支持 CREATE TRIGGER 和 DROP TRIGGER 的基础元数据。
- 支持对经网关执行的 INSERT、UPDATE、DELETE 触发 BEFORE/AFTER 逻辑。
- trigger body 内 SQL 复用 SQL→nGQL 写入能力。
- 提供递归触发保护和明确边界文档。

**Non-Goals:**

- 不承诺 100% MySQL Server 全量兼容。
- 不捕获或伪造无法可靠观察的 Nebula 原生变更。
- 不破坏现有 nGQL JDBC 直连和基础 MySQL 网关能力。

## Decisions

- trigger 子集只对网关可观察写入生效。
- BEFORE/AFTER 语义以网关执行顺序定义，不承诺完整 MySQL 事务回滚等价。
- 必须实现递归保护和失败策略，避免触发器造成无限写入。

## Risks / Trade-offs

- **[Risk] MySQL 高级语义与 NebulaGraph 原生能力不等价。** → Mitigation：以子集和兼容层方式实现，所有边界写入 spec 和错误响应。
- **[Risk] 客户端可能假设完整 MySQL 行为。** → Mitigation：建立兼容矩阵，不支持能力返回明确错误。
- **[Risk] 事件、routine 或 trigger 可能引入一致性风险。** → Mitigation：只对网关可观察路径承诺，并提供持久化、幂等、递归保护和失败策略。

## Migration Plan

1. 先完成前置 SQL 与 MySQL 基础协议 changes。
2. 在本 change 中设计协议/语义子集和测试矩阵。
3. 以开关或独立模块形式启用高级能力。
4. 如出现兼容性问题，可关闭该能力而不影响基础查询网关。

## Open Questions

- 目标 CDC、复制、BI 或客户端工具的优先级是什么？
- 事件日志和元数据应使用本地存储、数据库还是外部存储？
- 是否需要 Spring Boot starter 或 Magic-API 插件暴露配置？
