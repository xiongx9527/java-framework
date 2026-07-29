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
| `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata test` | 0 | `BUILD SUCCESS`；86 tests，0 failures，0 errors，0 skipped。 |
| `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am -DfailIfNoTests=false -Dtest='*AskData*Test' test` | 0 | Reactor 7/7 `BUILD SUCCESS`；AskData 170 tests，0 failures，0 errors，0 skipped。 |
| `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -DskipTests package` | 0 | `BUILD SUCCESS`；测试按参数跳过。 |
| `openspec validate add-askdata-governance-permission-bridge --strict` | 0 | `Change 'add-askdata-governance-permission-bridge' is valid`。 |
| `git diff --check -- fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure docs/askdata openspec/changes/add-askdata-governance-permission-bridge docs/superpowers/reports/2026-07-29-add-askdata-governance-permission-bridge-verify.md .superpowers/sdd/final-fix-report.md` | 0 | 无输出，未发现空白错误。 |
| Java 8 文档示例 `javac -source 8 -target 8` 后运行 | 0 | 输出 `ASKDATA_GOVERNANCE_BRIDGE_EXAMPLE_OK`。 |

补充定向回归：SQL 校验、Flow、插件链、日志查询/存储和 Legacy embedding adapter 共 84 tests，0 failures，0 errors，0 skipped。

## Comet Full Verify 结论

| 维度 | 状态 | 结论 |
| --- | --- | --- |
| Completeness | PASS | `tasks.md` 22/22 已完成；4 个 delta spec 均已覆盖。 |
| Correctness | PASS | 治理授权、默认拒绝、授权摘要绑定、SQL 行列权限、插件后最终校验、监控日志治理和文档示例均有实现与测试证据。 |
| Coherence | PASS | `proposal.md`、`design.md`、`docs/superpowers/specs/2026-07-29-add-askdata-governance-permission-bridge-design.md`、delta spec 与实现一致；未发现规格/设计漂移。 |

### Completeness

- OpenSpec 状态显示 `progress.total=22`、`progress.complete=22`、`remaining=0`。
- 涉及的 4 个能力规格均纳入验证：`askdata-agent-observability-contracts`、`askdata-core-contracts`、`askdata-governance-permission-bridge`、`askdata-query-engine`。
- 最终验证命令覆盖 Core 契约、AI 自动配置、Starter 包装、OpenSpec strict、空白检查和 Java 8 文档示例。

### Correctness

- Core 治理 SPI 保持 Java 8 且不暴露 Spring、LangChain4j、IAM 或存储供应商类型；默认治理提供器 fail-closed。
- 授权摘要绑定 `projectId`、`operatorId`、`dataSourceKey`、用途、管理员范围、版本和来源，并在 Flow 元数据、Prompt、SQL 插件链、最终 SQL 校验、执行和日志查询之间保持同一摘要。
- SQL 授权覆盖未授权数据源/表/列、空或未知列集合、SELECT 别名污染、CTE 与真实别名冲突、`alias.*`、插件收窄摘要后访问被移除表、插件删除行级谓词后的最终谓词重落实。
- 监控日志治理覆盖本项目管理员、平台管理员显式授权范围、普通操作人拒绝，以及拒绝/异常/SQL 授权失败的脱敏事件。
- `AskDataGovernanceBridgeDocumentationExample` 通过 `javac -source 8 -target 8` 和运行验证，输出 `ASKDATA_GOVERNANCE_BRIDGE_EXAMPLE_OK`。

### Coherence

- 变更实现保持 Core 契约优先，业务 IAM 通过治理桥接 SPI 映射，不把业务权限框架引入 Core。
- Starter 自动配置采用默认拒绝和 `@ConditionalOnMissingBean` 覆盖入口，与设计中的 fail-closed 和业务覆盖边界一致。
- 文档与 SQLBot/WrenAI 矩阵明确外部系统联通非目标，未宣称企业微信、钉钉、飞书、DataEase、Dify、Coze、n8n 已真实联通。

### Issues

- Critical：无。
- Important：无。
- Minor：无。
- 非阻塞环境警告：Maven/Jansi restricted method、Guava `sun.misc.Unsafe` 弃用、SLF4J multiple bindings、本地 `fw-plus-ai-boot-autoconfigure` POM invalid、Starter empty JAR 警告；均未导致验证命令失败。

## 已知警告与修复记录

- Core 测试与 Starter 打包均出现 Jansi restricted method 和 Guava `sun.misc.Unsafe` 运行时弃用警告，命令正常结束。
- Starter 打包出现本地 `fw-plus-ai-boot-autoconfigure` POM invalid 警告，并提示 Starter 无源码、生成空 JAR；退出码仍为 0。
- final review 修复过程中，Core 契约、列级校验、JDBC 最终命令、行级谓词、元数据净化、治理事件、自动配置和日志治理测试均先取得预期 RED，再由对应实现转为 GREEN。
- 完整 AI 回归曾为 161 tests、1 failure：插件执行前过早过滤未被模型 SQL 引用的 request option，导致插件无法新增 `:tenantId`。修复为插件输入可见 option、插件后按最终 SQL 过滤 JDBC 参数后，最终 163 tests 全绿。
- 第二轮列级 RED 为 31 tests、2 failures：空列集合和同一 SELECT 中的输出别名会错误放行真实列。修复为显式表级标记、空/未知列 fail-closed、查询块内 SELECT 别名引用和 CTE/真实别名冲突检测后，31 tests 全绿。
- 第三轮授权边界 RED 覆盖插件收窄摘要后改写到已移除表、以及 CTE/真实别名冲突下的限定通配符 `alias.*`；修复后 Core 定向 24 tests、AI 定向 41 tests、Core 全量 86 tests、AI Reactor 170 tests 全绿。
- 恶意插件删除 `WHERE` 的定向测试先 RED，修复为插件后按最终收窄摘要重新落实全部必需行级谓词后 GREEN。
- 当前 Core `AiEmbeddingStoreAdapter` 不包含 `getFramework()`；安装当前 Core 模块后，AI `clean compile` 在错误 `@Override` 处稳定 RED。移除该方法并改为验证 `supports(definition)` 后，Head clean compile 与 adapter 2 tests 全绿。
- SQL 授权拒绝、监控日志显式拒绝/治理异常和基础日志治理字段均取得预期 RED；修复后 Flow/日志查询 36 tests 与 Core 观测契约 5 tests 全绿。
- 首次执行未附加 `-DfailIfNoTests=false` 的 AI `-am` 命令在无测试的 `fw-plus-ai-boot-common` 被 Surefire 拒绝，未进入目标模块；按复审确认增加该参数后 Reactor 7/7 和 AskData 170 tests 全绿。

## 任务状态

- 已勾选 6.1 至 6.5；OpenSpec strict、Core、AI Head Reactor、Starter package 和 diff check 均有第三轮最终授权修复后的新鲜证据。

## 外部集成非目标

本 change 仅验证治理桥接边界，不实施企业微信、钉钉、飞书、DataEase、Dify、Coze、n8n、MCP Server 或 Vue3 控制台的真实租户联调；不引入业务 IAM、JustAuth、Spring Security、Sa-Token、数据库迁移工具或 Java 17 运行时类型到 Core 公共 API。
