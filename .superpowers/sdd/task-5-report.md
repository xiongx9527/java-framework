# Task 5 Report

## Status

DONE

## RED / 前置检查

- `rg` 确认目标 Starter 文档未包含 `AskDataGovernanceProvider`、授权摘要/对象/行级谓词、默认拒绝、显式开发测试 Bean、监控日志授权器与策略的完整说明。
- `tasks.md` 中 1.1 至 5.3 在更新前均未勾选。

## 文档证据

- `sql-safety.md` 说明业务 IAM 到治理 SPI 的映射、受控 SQL 片段和具名参数、未落实谓词的 fail-closed、元数据先于 Prompt 过滤、最终 SQL 重校验及监控日志双 SPI。
- `configuration.md` 明确没有生产全局放行属性，开发/测试也必须显式注册受限 Bean；监控日志管理员读取默认拒绝。
- `plugin-sdk.md` 提供 Java 8 风格治理桥接片段，并声明外部企业 IAM、MCP、DataEase、Dify、Coze、n8n、企业微信、钉钉、飞书仍是业务集成契约或后续 change 范围。
- 功能矩阵只将治理相关行调整为“默认拒绝 + 业务桥接可验证”，外部集成行保持业务集成契约覆盖。

## GREEN

`git diff --check -- docs/askdata/starter openspec/changes/add-askdata-governance-permission-bridge` 已通过（退出码 0）。
