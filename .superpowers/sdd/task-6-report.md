# Task 6 验证报告：AskData 治理权限桥接

## 状态

`DONE_WITH_CONCERNS`

## 验证结果

1. `openspec validate add-askdata-governance-permission-bridge --strict`
   - 退出码：0。
   - 摘要：`Change 'add-askdata-governance-permission-bridge' is valid`。

2. `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata test`
   - 退出码：0，`BUILD SUCCESS`。
   - 测试：79 项，0 failures，0 errors，0 skipped。
   - 警告：Jansi restricted method 与 Guava `sun.misc.Unsafe` 的 JDK 运行时弃用警告，未影响结果。

3. `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest='*AskData*Test' test`
   - 退出码：1，`BUILD FAILURE`；测试未启动。
   - 阻塞：范围外文件 `LegacyEmbeddingStoreAiAdapter.java:27` 未实现 `AiEmbeddingStoreAdapter#getFramework()`。未修改该文件。

4. `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -DskipTests package`
   - 退出码：0，`BUILD SUCCESS`；按参数跳过测试。
   - 警告：本地 `fw-plus-ai-boot-autoconfigure` POM invalid、Starter 无源码且生成空 JAR，均未使打包失败。

5. `git diff --check -- fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure docs/askdata openspec/changes/add-askdata-governance-permission-bridge`
   - 退出码：0，无输出。

6. `! rg -n "TBD|TODO|后续实现|未实现|外部集成预留" docs/askdata/starter openspec/changes/add-askdata-governance-permission-bridge docs/superpowers/specs/2026-07-29-add-askdata-governance-permission-bridge-design.md`
   - 退出码：0，无匹配。

## 勾选项

- 已勾选：6.1、6.2、6.4。
- 未勾选：6.3，因自动配置模块的范围外编译失败未能运行测试。

## 外部集成边界

本 change 不实施企业微信、钉钉、飞书、DataEase、Dify、Coze、n8n、MCP Server 或 Vue3 控制台的真实联调；也不将业务 IAM、JustAuth、Spring Security、Sa-Token、数据库迁移工具或 Java 17 运行时类型引入 Core 公共 API。
