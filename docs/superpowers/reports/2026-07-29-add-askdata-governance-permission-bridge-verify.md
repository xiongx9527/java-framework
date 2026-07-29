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
| `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata test` | 0 | `BUILD SUCCESS`；85 tests，0 failures，0 errors，0 skipped。 |
| `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am -DfailIfNoTests=false -Dtest='*AskData*Test' test` | 0 | Reactor 7/7 `BUILD SUCCESS`；AskData 169 tests，0 failures，0 errors，0 skipped。 |
| `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -DskipTests package` | 0 | `BUILD SUCCESS`；测试按参数跳过。 |
| `openspec validate add-askdata-governance-permission-bridge --strict` | 0 | `Change 'add-askdata-governance-permission-bridge' is valid`。 |
| `git diff --check -- fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure docs/askdata openspec/changes/add-askdata-governance-permission-bridge docs/superpowers/reports/2026-07-29-add-askdata-governance-permission-bridge-verify.md .superpowers/sdd/final-fix-report.md` | 0 | 无输出，未发现空白错误。 |
| Java 8 文档示例 `javac -source 8 -target 8` 后运行 | 0 | 输出 `ASKDATA_GOVERNANCE_BRIDGE_EXAMPLE_OK`。 |

补充定向回归：SQL 校验、Flow、插件链、日志查询/存储和 Legacy embedding adapter 共 83 tests，0 failures，0 errors，0 skipped。

## 已知警告与修复记录

- Core 测试与 Starter 打包均出现 Jansi restricted method 和 Guava `sun.misc.Unsafe` 运行时弃用警告，命令正常结束。
- Starter 打包出现本地 `fw-plus-ai-boot-autoconfigure` POM invalid 警告，并提示 Starter 无源码、生成空 JAR；退出码仍为 0。
- final review 修复过程中，Core 契约、列级校验、JDBC 最终命令、行级谓词、元数据净化、治理事件、自动配置和日志治理测试均先取得预期 RED，再由对应实现转为 GREEN。
- 完整 AI 回归曾为 161 tests、1 failure：插件执行前过早过滤未被模型 SQL 引用的 request option，导致插件无法新增 `:tenantId`。修复为插件输入可见 option、插件后按最终 SQL 过滤 JDBC 参数后，最终 163 tests 全绿。
- 第二轮列级 RED 为 31 tests、2 failures：空列集合和同一 SELECT 中的输出别名会错误放行真实列。修复为显式表级标记、空/未知列 fail-closed、查询块内 SELECT 别名引用和 CTE/真实别名冲突检测后，31 tests 全绿。
- 恶意插件删除 `WHERE` 的定向测试先 RED，修复为插件后按最终收窄摘要重新落实全部必需行级谓词后 GREEN。
- 当前 Core `AiEmbeddingStoreAdapter` 不包含 `getFramework()`；安装当前 Core 模块后，AI `clean compile` 在错误 `@Override` 处稳定 RED。移除该方法并改为验证 `supports(definition)` 后，Head clean compile 与 adapter 2 tests 全绿。
- SQL 授权拒绝、监控日志显式拒绝/治理异常和基础日志治理字段均取得预期 RED；修复后 Flow/日志查询 36 tests 与 Core 观测契约 5 tests 全绿。
- 首次执行未附加 `-DfailIfNoTests=false` 的 AI `-am` 命令在无测试的 `fw-plus-ai-boot-common` 被 Surefire 拒绝，未进入目标模块；按复审确认增加该参数后 Reactor 7/7 和 AskData 169 tests 全绿。

## 任务状态

- 已勾选 6.1 至 6.5；OpenSpec strict、Core、AI Head Reactor、Starter package 和 diff check 均有第二轮复审修复后的新鲜证据。

## 外部集成非目标

本 change 仅验证治理桥接边界，不实施企业微信、钉钉、飞书、DataEase、Dify、Coze、n8n、MCP Server 或 Vue3 控制台的真实租户联调；不引入业务 IAM、JustAuth、Spring Security、Sa-Token、数据库迁移工具或 Java 17 运行时类型到 Core 公共 API。
