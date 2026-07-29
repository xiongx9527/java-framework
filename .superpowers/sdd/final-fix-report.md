# AskData 治理权限桥接最终审查修复报告

## 状态

DONE_WITH_CONCERNS

## 提交

`9b50bff24aa34e894b2a92f49748e2802da6ac4e` - `fix(askdata): close governance bridge review gaps`

## RED

- Core 契约补充 RED：授权请求/摘要缺少用途、数据源、授权版本、来源、管理员范围和绑定校验，监控日志缺真实归属与项目范围查询契约。
- Starter SQL RED：只授权 `orders.id` 时 `SELECT secret FROM orders` 被放行；多表未限定列、CTE/真实别名冲突、插件新增行级参数和 JDBC 最终命令参数过滤存在越权或误拦截。
- Flow/Metadata/Observability RED：行级谓词摘要被拒绝或丢失，自定义 MetadataProvider 可把未授权 terms/examples/promptFragments 带入 Prompt，治理拒绝未发布脱敏事件。
- 日志查询治理 RED：原 Starter 独立授权 SPI 无法表达 Core 管理员范围，项目管理员不能查询本项目所有操作人，平台管理员范围和审计字段不完整。

## GREEN

- `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata test`：84 tests，0 failures，0 errors，0 skipped。
- `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest='*AskData*Test' test`：163 tests，0 failures，0 errors，0 skipped。
- `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -DskipTests package`：BUILD SUCCESS；保留既有 POM invalid 与空 starter JAR 警告。
- `openspec validate add-askdata-governance-permission-bridge --strict`：通过。
- `git diff --check -- fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure docs/askdata openspec/changes/add-askdata-governance-permission-bridge docs/superpowers/reports/2026-07-29-add-askdata-governance-permission-bridge-verify.md`：通过。
- Java 8 文档示例编译并运行，输出 `ASKDATA_GOVERNANCE_BRIDGE_EXAMPLE_OK`。

## 修复内容

- Core 授权模型补齐 `AskDataAuthorizationPurpose`、`AskDataAdministrativeScope`、数据源/用途绑定、拒绝码、授权版本和授权来源，并让摘要可校验是否绑定当前请求。
- SQL 授权处理器按强类型授权对象执行列级 fail-closed 校验，覆盖别名、未限定列、CTE 冲突和插件改写；行级谓词注入为受控片段并检测具名参数冲突。
- JDBC 执行器消费最终 `AskDataSqlCommand`，仅绑定最终 SQL 实际使用的请求参数和谓词参数。
- Flow 在模型调用前后保留授权摘要事实源，执行不可覆盖的元数据净化，并为治理拒绝/异常发布脱敏监控事件。
- 监控日志改为通过 Core governance 管理员范围授权，日志携带真实 owner context，查询审计记录主体、范围、结果数量和授权来源。
- 文档、OpenSpec delta spec、功能矩阵和验证报告更新为 final review 修复后的行为与证据。
- 用普通提交删除无关 `neutral-ai-adapter-autodiscovery` OpenSpec/设计产物，降低分支夹带范围。

## 改动文件

- 实现提交涉及 73 个路径；完整清单可用 `git show --name-only --format='' 9b50bff24aa34e894b2a92f49748e2802da6ac4e` 查看。
- 主要范围：`fw-plus-core/.../askdata/security`、`observability`、`storage`、`sql`；`fw-plus-ai/.../askdata/flow`、`sql`、`observability`、`autoconfigure`；AskData Starter/Core 测试；`docs/askdata/starter`；`openspec/changes/add-askdata-governance-permission-bridge`。

## 顾虑

- Important 6 只用普通提交移除了明显无关的 neutral-ai OpenSpec/设计产物。`LegacyEmbeddingStoreAiAdapter#getFramework()` 保留，因为移除会让当前分支编译失败；彻底拆分其上游 neutral-ai 历史需要独立分支整理或历史重写，本次未执行。
- Maven 仍输出 Jansi、Guava `Unsafe`、Starter 本地 POM invalid 和空 JAR 警告；验证命令退出码均为 0。
