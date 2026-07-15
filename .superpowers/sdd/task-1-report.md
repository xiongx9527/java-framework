# Task 1 Implementer Report

Status: DONE_WITH_CONCERNS

## 实现内容

Task 1 的 Nebula JDBC 基线测试与最小 GREEN 实现已提交。

Commit: `9f11685 test: add nebula jdbc stability baseline tests`

## 测试与结果

Implementer 报告：使用系统 Maven 3.6.3 重跑指定 5 组测试，`22 tests, 0 failures, 0 errors`，`BUILD SUCCESS`。

指定测试范围：

```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core
mvn test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaConnectionConfigParserTest,NebulaStatementClassifierTest,NebulaResourceLifecycleTest,NebulaResultSetCursorTest,NebulaClientFacadeTest
```

## TDD Evidence

### RED

Implementer 报告称已按 Task 1 要求先观察目标类/行为缺失导致失败，且未连接真实 NebulaGraph。详细原始输出未写入报告文件，因为 implementer 称其执行环境存在“Do NOT Write report/summary/findings/analysis .md files”约束。

### GREEN

Implementer 报告：使用系统 Maven 3.6.3 重跑指定 5 组测试，`22 tests, 0 failures, 0 errors`，`BUILD SUCCESS`。

注意：brief 指定 `./mvnw`，但 implementer 发现 `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/.mvn/wrapper` 缺失，改用系统 Maven 3.6.3。

## 变更文件

来自提交 `9f11685` 的 stat：

- `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/pom.xml`
- `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/client/NebulaClientFacade.java`
- `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/config/NebulaConnectionConfig.java`
- `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/config/NebulaConnectionConfigParser.java`
- `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/lifecycle/NebulaResourceLifecycle.java`
- `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/lifecycle/ThrowingCloseAction.java`
- `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/result/NebulaResultSetCursor.java`
- `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaStatementClassifier.java`
- `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/client/NebulaClientFacadeTest.java`
- `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/config/NebulaConnectionConfigParserTest.java`
- `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/lifecycle/NebulaResourceLifecycleTest.java`
- `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/result/NebulaResultSetCursorTest.java`
- `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/statement/NebulaStatementClassifierTest.java`

## 自检结果与顾虑

- DONE_WITH_CONCERNS。
- `./mvnw` 无法按 brief 运行，原因是 `fw-plus-core/.mvn/wrapper` 文件缺失；已改用系统 Maven 3.6.3。
- diff 为 826 insertions，命中 `review_mode: standard` 的 `diff > 200` 风险信号。
- 无跨模块、安全敏感、并发、数据迁移风险。
- public API 新增限于 Task 1 brief 目标组件。

## 风险信号自报

- 跨模块/跨子系统协调改动：否。
- 安全敏感面：否。
- 并发、锁、共享可变状态：否。
- 数据或 schema 迁移：否。
- 公共 API 契约或对外接口变更：新增 Task 1 brief 明确要求的目标组件接口。
- DONE_WITH_CONCERNS：是。
- 单任务 diff 超过 200 行：是，826 insertions。


---

# Task 1 Reviewer Fix Report

Status: DONE

## 修复内容

- 修复 `NebulaClientFacade.open()` 失败路径：`use graphspace` 返回失败或 Nebula 客户端异常时，释放已获取的 `Session`，并关闭已初始化的 `NebulaPool`。
- 修复 `NebulaClientFacade.executeNgql()` 生命周期检查：未 open 时抛出包含 `not open` 的 `SQLException`；已 close 时抛出包含 `closed` 的 `SQLException`；避免 NPE 或继续调用已释放 session。
- 小范围修复 `parseAddresses()`：仅捕获非数字端口的 `NumberFormatException`，包装为包含 `graphAddress` 和地址片段的 `SQLException`。
- 保持用户确认的 Task 1 GREEN 边界，未移除生产实现，未恢复 RED 基线。

## RED 证据

命令：

```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core
mvn test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaClientFacadeTest
```

输出摘要：`Tests run: 9, Failures: 2, Errors: 2`，失败点包括：

- `openShouldReleaseSessionAndClosePoolWhenUseGraphspaceFails`: `Wanted but not invoked: session.release()`。
- `executeNgqlShouldThrowSQLExceptionWhenNotOpened`: `NullPointerException`。
- `executeNgqlShouldThrowSQLExceptionWhenClosed`: `Expected SQLException`。
- `openShouldWrapNonNumericGraphAddressPortAsSQLException`: `NumberFormatException: For input string: "not-a-port"`。

备注：优先命令 `./mvnw ...` 因 `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/.mvn/wrapper/maven-wrapper.properties` 与 wrapper jar 缺失无法启动，按 brief 回退系统 Maven 3.6.3。

## GREEN 证据

命令 1：

```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core
mvn test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaClientFacadeTest
```

输出摘要：`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

命令 2：

```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core
mvn test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaConnectionConfigParserTest,NebulaStatementClassifierTest,NebulaResourceLifecycleTest,NebulaResultSetCursorTest,NebulaClientFacadeTest
```

输出摘要：`Tests run: 26, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

## 变更文件

- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/client/NebulaClientFacade.java`
- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/client/NebulaClientFacadeTest.java`
- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/.superpowers/sdd/task-1-report.md`

## 风险信号

- 跨模块/跨子系统协调改动：否。
- 安全敏感面：否。
- 并发、锁、共享可变状态：否。
- 数据或 schema 迁移：否。
- 公共 API 契约或对外接口变更：否；仅补强现有 facade 错误处理语义。
- 已知环境噪声：Maven settings 存在 `Unrecognised tag: 'repository'` 警告；测试运行出现 SLF4J NOP logger 警告；不影响本次目标测试 GREEN。


---

# Task 1 Additional Reviewer Fix Report

Status: DONE

## 修复内容

- `NebulaStatementClassifier.checkUpdate()` 改为基于首个有效语句关键字分类：首关键字为 update 类则接受，首关键字为 query 类则拒绝，避免合法 update 语句中后续属性名/标识符命中 `show`、`go`、`desc` 等 query 关键字时被误拒。
- `NebulaClientFacade.open()` 检查 `NebulaPool.init(...)` 返回值：返回 `false` 时抛出包含 `graphAddress` 的 `SQLException`，不调用 `getSession`，并关闭 pool 清理资源。
- 未勾选 plan 或 OpenSpec task，未修改其它模块源码。

## RED 命令与失败摘要

RED 在全新临时目录 `/private/tmp/nebula-jdbc-red-check.LLgtCZ` 中用当前新增测试 + HEAD 未修复生产代码验证，避免覆盖工作树草稿修复。

命令：

```bash
cd /private/tmp/nebula-jdbc-red-check.LLgtCZ/fw-plus-core
mvn test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaStatementClassifierTest,NebulaClientFacadeTest
```

失败摘要：`Tests run: 16, Failures: 2, Errors: 0, Skipped: 0`，`BUILD FAILURE`。

- `NebulaStatementClassifierTest.checkUpdateShouldAcceptQueryKeywordAfterLeadingUpdateKeyword`: `Update nGql must not contain query keyword: go`。
- `NebulaClientFacadeTest.openShouldThrowSQLExceptionAndClosePoolWhenPoolInitReturnsFalse`: 触发 `NullPointerException` 分支，说明 init=false 后仍进入 `getSession`。

## GREEN 命令与通过摘要

命令 1：

```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core
mvn test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaStatementClassifierTest,NebulaClientFacadeTest
```

通过摘要：`Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

命令 2：

```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core
mvn test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaConnectionConfigParserTest,NebulaStatementClassifierTest,NebulaResourceLifecycleTest,NebulaResultSetCursorTest,NebulaClientFacadeTest
```

通过摘要：`Tests run: 28, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

备注：本次按用户指定命令使用系统 Maven 3.6.3；未使用 `./mvnw`。

## 变更文件

- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/client/NebulaClientFacade.java`
- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaStatementClassifier.java`
- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/client/NebulaClientFacadeTest.java`
- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/statement/NebulaStatementClassifierTest.java`
- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/.superpowers/sdd/task-1-report.md`

## 风险信号

- Maven settings 存在 `Unrecognised tag: 'repository'` 警告。
- `fw-plus-auth-satoken` POM 存在重复 `cn.dev33:sa-token-core` 依赖警告。
- 最终回归测试中出现 SLF4J NOP logger 警告。
- 以上均为既有环境/依赖警告，未影响本次目标测试通过。


## Task 1 Review Fix - 2026-07-15

Status: DONE

修复内容:
- 为 `NebulaStatementClassifier.checkQuery()` 增加首关键字未知但后续含 query keyword 的回归测试。
- 将 `checkQuery()` 改为与 `checkUpdate()` 一致的首个有效语句关键字分类：首关键字为 update 时抛 query/update 类型错误；首关键字为 query 时接受；其它首关键字抛 query keyword 缺失/unknown 错误。

RED 命令与失败摘要:
- 命令: `cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && mvn test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaStatementClassifierTest`
- 结果: BUILD FAILURE；`NebulaStatementClassifierTest` Tests run: 7, Failures: 1, Errors: 0, Skipped: 0。
- 失败摘要: `checkQueryShouldRejectUnknownLeadingKeywordEvenWhenQueryKeywordAppearsLater` 未收到预期 `SQLException`，证明旧实现会接受 `foo MATCH (v) RETURN v` / `select match from person`。

GREEN 命令与通过摘要:
- 命令: `cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && mvn test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaStatementClassifierTest`
- 结果: BUILD SUCCESS；Tests run: 7, Failures: 0, Errors: 0, Skipped: 0。
- 最终回归命令: `cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && mvn test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaConnectionConfigParserTest,NebulaStatementClassifierTest,NebulaResourceLifecycleTest,NebulaResultSetCursorTest,NebulaClientFacadeTest`
- 最终回归结果: BUILD SUCCESS；Tests run: 29, Failures: 0, Errors: 0, Skipped: 0。

变更文件:
- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaStatementClassifier.java`
- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/statement/NebulaStatementClassifierTest.java`
- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/.superpowers/sdd/task-1-report.md`

风险信号:
- Maven settings 存在既有 warning：`Unrecognised tag: 'repository'`。
- 聚合模型存在既有 warning：`fw-plus-auth-satoken` 重复声明 `cn.dev33:sa-token-core`。
- 测试运行中出现既有 SLF4J NOP logger 提示与 deprecated API 编译提示；本次修复未改动相关区域。
