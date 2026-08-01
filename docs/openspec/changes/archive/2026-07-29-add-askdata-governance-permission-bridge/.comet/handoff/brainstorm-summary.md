# Brainstorm Summary

- Change: add-askdata-governance-permission-bridge
- Date: 2026-07-29
- Status: 已确认设计方案 A

## 确认的技术方案

采用方案 A：新增强类型治理授权摘要，并让该摘要贯穿 Core、Starter、元数据、Prompt、SQL 插件链、最终 SQL 校验、只读执行和管理员监控日志查询。默认实现采用拒绝优先；业务通过 Java 8 Core SPI 或 Spring Bean 覆盖接入真实项目成员、数据源、表/列授权和行级权限。

## 关键取舍与风险

- 选择强类型治理摘要而不是继续扩展字符串式 `AskDataPermissionEvaluator`，因为后者无法可靠表达列级授权、行级谓词、授权版本和管理员跨项目范围。
- 保留旧 evaluator 兼容，但只作为粗粒度能力判定来源，不作为完整对象/行列授权来源。
- 默认拒绝可能影响未接入治理层的样例和开发环境，因此文档提供显式开发测试桩；生产不提供全局放行配置。
- Core 类型会增加，但只建模 AskData 所需的授权摘要、对象引用、行级谓词和查询范围，不引入宿主 IAM 或框架类型。

## 测试策略

- Core 单元测试覆盖治理摘要、默认拒绝、跨项目/跨操作人、授权对象不可扩大。
- Starter 自动配置测试覆盖业务 Bean 优先、旧 evaluator 兼容、无治理实现拒绝。
- SQL 管线测试覆盖元数据过滤、行级谓词具名参数、恶意插件清空或扩大授权对象、最终 SQL 重校验。
- 观测测试覆盖权限拒绝事件、管理员日志查询审计、普通操作人拒绝。

## Spec Patch

无。当前 delta specs 已覆盖设计需要的新增验收场景，暂不需要回写额外 Spec Patch。
