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

## 第 1 轮 review 修复（2026-07-29）

### RED / 前置检查

- 文档中的 `OrdersGovernanceProvider` 仅为 Markdown 片段；原 `javac` 命令只编译明确不冒充生产 API 的 `AskDataPluginSdkExample.java`，无法覆盖真实 `AskDataGovernanceProvider`、授权对象或行级参数 API。
- 尝试运行 `./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=AskDataGovernanceBridgeDocumentationTest test` 时，新增示例尚不存在，且构建在测试编译前被既有 `LegacyEmbeddingStoreAiAdapter` 缺失 `AiEmbeddingStoreAdapter#getFramework()` 阻断。
- `configuration.md` 将默认策略表述为“只允许同项目管理员”，与默认 `AskDataMonitoringLogAccessAuthorizer.denyAll()` 的 fail-closed 链路不一致。

### GREEN

- 新增 `AskDataGovernanceBridgeDocumentationExample`，直接使用真实 Core API 实现 `AskDataGovernanceProvider`，返回 `AskDataAuthorizationSummary.allow(...)`，覆盖 `sales.orders` 的 `id`、`amount` 列和 `region_id = :regionId` 的 `AskDataSqlParameter` 具名绑定。
- `javac -source 8 -target 8 -cp fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/target/classes -d "$verify_dir" fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/documentation/AskDataGovernanceBridgeDocumentationExample.java` 通过；随后 `java -cp "$verify_dir":fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/target/classes com.xiongx9527.fw.plus.ai.autoconfigure.askdata.documentation.AskDataGovernanceBridgeDocumentationExample` 输出 `ASKDATA_GOVERNANCE_BRIDGE_EXAMPLE_OK`。
- `plugin-sdk.md`、`sql-safety.md` 和 `configuration.md` 统一为：默认授权器 fail-closed；业务显式授权解析器确认管理员资格后，策略再逐条收窄日志边界；策略不会仅根据调用方角色识别管理员。
