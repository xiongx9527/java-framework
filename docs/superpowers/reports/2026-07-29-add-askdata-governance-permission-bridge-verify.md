# AskData 治理权限桥接验证记录

日期：2026-07-29

Change：`add-askdata-governance-permission-bridge`
状态：`PASS`

## 执行环境

- 工作目录：`/Users/xiongxiao/Workspace/Idea/codes/java-framework`
- 分支：`feature/20260729/add-askdata-governance-permission-bridge`
- Maven Wrapper：Apache Maven 3.8.8。

## 验证证据

| 命令 | 退出码 | 结果摘要 |
| --- | ---: | --- |
| `openspec validate add-askdata-governance-permission-bridge --strict` | 0 | `Change 'add-askdata-governance-permission-bridge' is valid`。 |
| `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata test` | 0 | `BUILD SUCCESS`；79 tests，0 failures，0 errors，0 skipped。 |
| `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest='*AskData*Test' test` | 0 | `BUILD SUCCESS`；149 tests，0 failures，0 errors，0 skipped。 |
| `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -DskipTests package` | 0 | `BUILD SUCCESS`；测试按参数跳过。 |
| `git diff --check -- fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure docs/askdata openspec/changes/add-askdata-governance-permission-bridge` | 0 | 无输出，未发现空白错误。 |
| `! rg -n "TBD|TODO|后续实现|未实现|外部集成预留" docs/askdata/starter openspec/changes/add-askdata-governance-permission-bridge docs/superpowers/specs/2026-07-29-add-askdata-governance-permission-bridge-design.md` | 0 | 无匹配。 |

## 已知警告与修复记录

- Core 测试与 Starter 打包均出现 Jansi restricted method 和 Guava `sun.misc.Unsafe` 运行时弃用警告，命令正常结束。
- Starter 打包出现本地 `fw-plus-ai-boot-autoconfigure` POM invalid 警告，并提示 Starter 无源码、生成空 JAR；退出码仍为 0。
- AI 自动配置定向测试曾在测试前失败：`fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/store/embeding/LegacyEmbeddingStoreAiAdapter.java:27` 未覆盖 `AiEmbeddingStoreAdapter#getFramework()`；已由 `69719f81 fix(ai): implement legacy embedding adapter framework key` 修复。
- AI 自动配置定向测试随后出现 28 个旧 AskData Flow/Resume/Interceptor 用例失败，原因是测试 fixture 未显式授权、与默认治理拒绝语义冲突；已由 `90cff5fa test(askdata): authorize legacy flow verification fixtures` 修复。

## 任务状态

- 已勾选：6.1 OpenSpec 严格校验、6.2 Core AskData 测试、6.3 AI AskData 自动配置定向测试、6.4 文档/矩阵扫描与 `git diff --check`。
- 验证命令均已记录退出码与结果摘要；外部真实联调仍保持非目标。

## 外部集成非目标

本 change 仅验证治理桥接边界，不实施企业微信、钉钉、飞书、DataEase、Dify、Coze、n8n、MCP Server 或 Vue3 控制台的真实租户联调；不引入业务 IAM、JustAuth、Spring Security、Sa-Token、数据库迁移工具或 Java 17 运行时类型到 Core 公共 API。
