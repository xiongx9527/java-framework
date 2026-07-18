## Context

本 change 属于 Nebula MySQL 协议深度兼容路线。前置能力包括 SQL parser、SQL→nGQL 查询/写入转换、MySQL 协议网关基础能力。由于本 change 触及 MySQL 高级语义，必须明确兼容边界，避免承诺完整 MySQL Server 等价。

## Goals / Non-Goals

**Goals:**

- 新增 MySQL 复制协议子集支持。
- 支持基础复制握手和从指定 position 拉取事件。
- 支持 `COM_BINLOG_DUMP` 或等价子集。
- 对 GTID、完整 slave 兼容等不支持能力返回明确错误。
- 通过当前模块内的 replication-style 自动化测试证明 handler、position、stream adapter 和错误边界。

**Non-Goals:**

- 不承诺 100% MySQL Server 全量兼容。
- 不捕获或伪造无法可靠观察的 Nebula 原生变更。
- 不破坏现有 nGQL JDBC 直连和基础 MySQL 网关能力。
- 不在本 change 中伪造或声明真实 CDC/复制客户端端到端验收；该验收在 MySQL gateway 网络入口可运行后作为后续 Manual Acceptance Follow-up 处理。

## Decisions

- 复制协议只做 CDC/同步可用子集，不承诺完整 MySQL slave 接入。
- 协议层不生成事件，只消费 binlog-compatible stream。
- 所有不支持命令必须明确返回错误，避免客户端误判一致性。

## Risks / Trade-offs

- **[Risk] MySQL 高级语义与 NebulaGraph 原生能力不等价。** → Mitigation：以子集和兼容层方式实现，所有边界写入 spec 和错误响应。
- **[Risk] 客户端可能假设完整 MySQL 行为。** → Mitigation：建立兼容矩阵，不支持能力返回明确错误。
- **[Risk] 事件、routine 或 trigger 可能引入一致性风险。** → Mitigation：只对网关可观察路径承诺，并提供持久化、幂等、递归保护和失败策略。
- **[Risk] 当前仓库缺少可运行的真实 CDC/复制客户端网络入口验收。** → Mitigation：当前 change 只把 adapter 与 replication-style 自动化测试作为 build exit criteria，并记录后续人工验收项。

## Migration Plan

1. 先完成前置 SQL 与 MySQL 基础协议 changes。
2. 在本 change 中设计协议/语义子集、测试矩阵和 binlog stream adapter 自动化验收。
3. 以开关或独立模块形式启用高级能力。
4. 在 MySQL gateway 网络入口可运行后，用真实 CDC/复制客户端补充端到端事件读取验收。
5. 如出现兼容性问题，可关闭该能力而不影响基础查询网关。

## Open Questions

- 目标 CDC、复制、BI 或客户端工具的优先级是什么？
- 事件日志和元数据应使用本地存储、数据库还是外部存储？
- 是否需要 Spring Boot starter 或 Magic-API 插件暴露配置？
- 后续 Manual Acceptance Follow-up 选用哪个真实 CDC/复制客户端作为首个验收目标？
