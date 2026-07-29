# Task 4 实施报告

## 状态

DONE_WITH_CONCERNS

## RED

1. `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest='AskDataMonitoringLogQueryServiceTest,DefaultAskDataFlowExecutorTest' test`
   - 未进入测试执行：范围外 `LegacyEmbeddingStoreAiAdapter` 未实现 `AiEmbeddingStoreAdapter#getFramework()`，Maven 主编译失败。
2. 聚焦编译后执行：
   `../../mvnw surefire:test -Dtest='AskDataMonitoringLogQueryServiceTest,DefaultAskDataFlowExecutorTest'`
   - 预期失败：普通操作人查询日志返回 1 条；未授权 `secret` 列进入传给 Prompt 的 metadata。

## GREEN

1. 生成测试依赖类路径：
   `../../mvnw dependency:build-classpath -Dmdep.includeScope=test -Dmdep.pathSeparator=: -Dmdep.outputFile=target/task-4-test.classpath`
2. 聚焦编译 Task 4 生产类与测试类：
   `javac -source 8 -target 8 -cp "target/classes:$(tr -d '\\n' < target/task-4-test.classpath)" -d target/classes ...`
   `javac -source 8 -target 8 -cp "target/classes:$(tr -d '\\n' < target/task-4-test.classpath)" -d target/test-classes ...`
3. 聚焦 Surefire：
   `../../mvnw surefire:test -Dtest='AskDataMonitoringLogQueryServiceTest,DefaultAskDataFlowExecutorTest'`
   - 结果：22 tests，0 failures，0 errors。

## 实施内容

- `AskDataMetadataProvider` 新增 Java 8 default overload，兼容已有 `load(AskDataRequest)` 实现，并按 datasource、表、列或 `*` 授权对象收窄 metadata。
- Flow 的新建与恢复路径均调用 authorization-aware overload，Prompt 和后续 SQL 校验使用已收窄 metadata。
- `EmptyAskDataMetadataProvider` 显式兼容新 overload。
- 监控日志普通操作人默认拒绝；项目管理员保持同项目限制；平台管理员仅查询调用方显式传入的 scope。
- 新增列级 Prompt 过滤、项目管理员跨项目拒绝、平台管理员显式 scope 和普通操作人拒绝测试。

## 风险与关注项

- 完整 Maven 生命周期被范围外 `LegacyEmbeddingStoreAiAdapter` 编译缺陷阻断，未修改该文件。
- 监控日志管理员角色由宿主调用方传入；平台管理员可读取的边界仍由显式 `scope` 参数限定，宿主 IAM 必须负责授予该角色。
