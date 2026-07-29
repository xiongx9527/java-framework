# AskData 治理权限桥接第二轮最终修复报告

## 状态

DONE_WITH_CONCERNS

## 提交

实现提交：`5373c5f91eb1720c5f6d35da41560789007259a5` - `fix(askdata): close governance bridge re-review gaps`

## RED

- 列级授权 RED：`DefaultAskDataSqlValidatorTest` 31 tests 中 2 failures；空列集合错误表示全部列，`select id as secret, secret from orders` 中 SELECT 别名全局污染并放行真实 `secret`。
- 行级谓词 RED：恶意插件收到已注入谓词后删除 `WHERE` 并保留摘要，最终命令未重新落实 `tenant_id = :tenantId`。
- Head 自洽 RED：先安装当前 Core `fw-plus-ai-store-embedding`，再对 AI 执行 `clean compile`，`LegacyEmbeddingStoreAiAdapter.java:[44,9]` 报“方法不会覆盖或实现超类型的方法”。
- 观测 RED：Core 基础监控日志缺 dataSource、拒绝码、授权版本、授权来源；SQL 授权失败只有普通生命周期事件；监控日志显式拒绝和治理异常返回空列表但不写拒绝审计。
- 回归 RED：关键组合首次为 83 tests、1 error，自动配置的 Core 最终校验把授权对象 `*` 转换为普通列但丢失显式表级标记。

## GREEN

- `DefaultAskDataSqlValidatorTest`：31 tests，全部通过；空/未知列 fail-closed，显式表级授权独立建模，SELECT 别名仅在所属查询块安全位置识别，CTE/真实别名冲突拒绝。
- 恶意插件删除谓词定向测试：1 test，通过；插件后以最终收窄摘要为事实源重新落实全部必需谓词和具名参数。
- 当前 Core 安装后的 AI `clean compile`：`BUILD SUCCESS`；`LegacyEmbeddingStoreAiAdapterTest` 2 tests 全绿。
- Core 观测契约：5 tests 全绿；Flow 与日志查询：36 tests 全绿；关键组合回归：83 tests 全绿。
- `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata test`：85 tests，0 failures，0 errors，0 skipped。
- `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am -DfailIfNoTests=false -Dtest='*AskData*Test' test`：Reactor 7/7 成功，AskData 169 tests，0 failures，0 errors，0 skipped。
- `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -DskipTests package`：`BUILD SUCCESS`。
- `openspec validate add-askdata-governance-permission-bridge --strict`：通过。
- `git diff --check -- ... .superpowers/sdd/final-fix-report.md`：通过。

## 修复内容

- 授权元数据增加显式表级授权标记，空列集合不再表示全部列；列解析只允许当前查询块 `ORDER BY` 中的 SELECT 别名引用，真实表别名与 CTE/派生名冲突直接 fail-closed。
- 插件后不再只注入新增谓词，而是从插件链最终摘要重新落实全部必需行级谓词；最终摘要继续传入最终元数据、命令和 Core 校验。
- 移除 Legacy embedding adapter 对当前 Core 不存在方法的错误 `@Override`，测试改为验证框架支持判定。
- SQL 授权失败发布脱敏治理事件；日志查询拒绝和治理异常写入同一 Core 观测存储；基础日志结构化保留数据源、拒绝码、授权版本和授权来源。
- 文档和 OpenSpec 任务状态更新为第二轮复审后的行为与新鲜验证证据。

## 改动文件

- Core：`AskDataEventPublisher.java`、`AskDataMonitoringLog.java`、`AskDataMonitoringLogStore.java`、`AskDataObservabilityContractTest.java`。
- AI 实现：`AskDataAutoConfiguration.java`、`DefaultAskDataFlowExecutor.java`、`AskDataTable.java`、`AskDataMonitoringLogQueryService.java`、`AskDataSqlAuthorizationProcessor.java`、`LegacyEmbeddingStoreAiAdapter.java`。
- AI 测试：`DefaultAskDataFlowExecutorTest.java`、`AskDataMonitoringLogQueryServiceTest.java`、`DefaultAskDataSqlValidatorTest.java`、`LegacyEmbeddingStoreAiAdapterTest.java`。
- 文档：`docs/askdata/starter/sql-safety.md`、OpenSpec `tasks.md`、验证报告和本报告。

## 顾虑

- Important 6 的历史范围顾虑仍在：此前已用普通提交移除明显无关的 neutral-ai OpenSpec/设计产物，但分支上游仍包含 neutral-ai 实现提交；彻底拆分需要历史整理，本轮按要求不扩大范围、不重写历史。
- Starter package 仍输出既有的本地 autoconfigure POM invalid 和空 Starter JAR 警告；构建退出码为 0。
