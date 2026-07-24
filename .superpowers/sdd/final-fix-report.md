# AskData Final Review Fix Report

## 状态

DONE_WITH_CONCERNS

Critical 和 Important findings 已处理并补回归测试。保留的 concern 是：当前 `AskDataAgent` 只有名称、可变上下文和摘要结果，没有“替换哪个默认步骤”、结构化步骤输出、失败策略及恢复契约，因此本次实现了安全的主 Agent 扩展执行、日志与 checkpoint 归并，但没有允许它直接替换 SQL 生成/校验/执行步骤。完整替换能力需要独立 change 先扩展 Agent/step contract。

## TDD 证据

### RED

1. `./mvnw ... -Dtest=DefaultAskDataSqlValidatorTest,JdbcAskDataSqlExecutorTest,DefaultAskDataFlowExecutorTest,AskDataResumeFlowTest,AskDataAutoConfigurationTest test`
   - testCompile 失败，明确暴露缺失的 metadata 校验入口、`enforceLimit` 构造语义、checkpoint 身份字段和执行器物理上限。
2. `./mvnw ... -Dtest=AskDataResumeFlowTest#shouldRejectResumeWhenEffectiveExecutionPolicyChanged test`
   - 1 test failure；全局执行策略变化后当前实现仍恢复成功。
3. `./mvnw ... -Dtest=DefaultAskDataSqlValidatorTest#shouldRejectSideEffectFunctions test`
   - 1 test failure；双引号引用的 `nextval`/`setval` 可绕过原始词法扫描。

### GREEN

1. Starter 全量测试：84 tests，0 failures，0 errors，BUILD SUCCESS。
2. Reactor `clean install -DskipTests`：9/9 modules SUCCESS，BUILD SUCCESS。
3. `git diff --check`：通过。
4. Java 9+ API 扫描：无匹配。
5. SQLBot/占位文本扫描：无匹配。
6. 验证 JVM：Zulu OpenJDK `1.8.0_402`。

## Findings 处理

### Critical

- SQL 安全边界：实现 Java 8 保守词法/token 扫描，拒绝多语句、可执行注释、写关键字、`SELECT INTO`、`FOR UPDATE`/`FOR SHARE`/`LOCK IN SHARE MODE`、`setval`/`nextval` 等副作用函数（含 quoted identifier）；在 metadata 表列表非空时解析 CTE、子查询、`FROM`/`JOIN`/逗号表引用并拒绝未授权表。
- Resume 安全：checkpoint 持久化并校验 question、datasourceKey、modelKey、有效 maxRows、有效 execute 和 dialect/conversationId；执行前保存 `EXECUTING`，恢复始终检查会话最新 checkpoint，遇到不确定执行状态返回 `RESUME_FAILED`，不重复执行。

### Important

- Agent/子 Agent：自动注入非 `AskDataSubAgent` 的主 Agent；消费 `AskDataDispatchResult`，把主/子 Agent 成功、失败、超时和输出写入 context attributes 与步骤日志；归并后保存 checkpoint。
- maxRows：收紧超大顶层 `LIMIT`、拒绝 `LIMIT ALL`；JDBC `PreparedStatement#setMaxRows` 与 ResultSet 读取循环双层物理限行。
- 模型错误码：单独捕获 `AskDataModelException`，保留 `DEFAULT_MODEL_NOT_CONFIGURED`、`MODEL_NOT_FOUND` 等原错误码。
- execute 默认值：全局关闭始终跳过；全局开启时 request.execute 缺省执行，仅显式 false 跳过；checkpoint 比较有效执行决策。

### Minor

- 多个 `ContentRetriever` 使用 `orderedStream()` 全部适配并组合。
- `sql.enforceLimit` 已接入默认 SQL validator；关闭时不改写 SQL，但 JDBC 物理限行仍生效。

## 残余风险与边界

- 未引入 AST。扫描器刻意保守，覆盖常见 SELECT/CTE、注释、引号、括号层级、表引用和顶层 LIMIT；数据库特有语法或未知副作用函数可能被拒绝或未列入 denylist。生产执行仍应使用数据库只读账号/只读事务作为最终权限边界。
- metadata 表列表为空时视为“没有可执行的 scope 信息”，不会做表白名单约束；提供非空 metadata 时才强制白名单。
- 主 Agent 当前作为默认流程前置扩展执行，不直接替换安全关键步骤。直接替换需要新增明确的 step key、结构化输出、失败策略和 checkpoint 恢复契约。
