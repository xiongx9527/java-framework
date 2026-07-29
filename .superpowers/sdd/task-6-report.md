# Task 6 验证报告：AskData 治理权限桥接

## 状态

`DONE`

## 当前验证结果

1. Core AskData 全量测试
   - 命令：`cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata test`
   - 退出码：0，`BUILD SUCCESS`。
   - 测试：86/86，0 failures，0 errors，0 skipped。
   - 复审指令中记录的 85/85 是本轮新增授权收窄回归前的计数；当前计数已包含该新增测试。

2. AI AskData reactor 测试
   - 命令：`cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am -DfailIfNoTests=false -Dtest='*AskData*Test' test`
   - 退出码：0，`BUILD SUCCESS`。
   - Reactor：7/7 success。
   - 测试：170/170，0 failures，0 errors，0 skipped。
   - 复审指令中记录的 169/169 是本轮新增 CTE 限定通配符回归前的计数；当前计数已包含该新增测试。

3. 定向 SQL 回归
   - Core：`AskDataSqlPipelineTest` 24/24 通过。
   - AI：`DefaultAskDataSqlValidatorTest` 与 `AskDataSqlInterceptorAutoConfigurationTest` 合计 41/41 通过。

4. Starter 打包
   - 命令：`cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -DskipTests package`
   - 退出码：0，`BUILD SUCCESS`。

5. OpenSpec
   - 命令：`openspec validate add-askdata-governance-permission-bridge --strict`
   - 退出码：0，`Change 'add-askdata-governance-permission-bridge' is valid`。

6. Diff 检查
   - 命令：`git diff --check -- fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure .superpowers/sdd/task-6-report.md`
   - 退出码：0，无输出。

## 本轮修复

- 插件收窄 `authorizationSummary` 时，最终 Core 命令的表授权集合由最终摘要重新派生，防止改写 SQL 继续使用原始宽集合。
- CTE 名称与真实表别名冲突时，限定通配符 `alias.*` fail-closed。

## 历史修复记录

此前自动配置模块曾因范围外的 `LegacyEmbeddingStoreAiAdapter#getFramework()` 编译问题而未启动 AskData 测试。本次使用 reactor 命令已完成 7/7 构建和 170/170 AskData 测试，旧失败不再是当前阻塞项。

## 已知非阻塞警告

- Maven/Jansi 的受限方法与 Guava `sun.misc.Unsafe` 弃用警告。
- 测试运行时存在多个 SLF4J binding 提示。
- Starter 打包提示本地 `fw-plus-ai-boot-autoconfigure` POM invalid，且 Starter 无源码而生成空 JAR；均未影响本次打包成功。
