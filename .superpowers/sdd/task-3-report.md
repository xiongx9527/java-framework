# Task 3 实现报告

## Status
DONE

## 实现内容
- 新增并统一使用 `NebulaResourceLifecycle.ThrowingCloseAction` 嵌套函数式接口。
- `NebulaResourceLifecycle.close(...)` 实现幂等关闭：已关闭时直接返回；首次关闭执行 action；action 成功后标记 closed；action 抛 `SQLException` 时传播原异常并标记 closed，避免重复触发底层释放。
- `NebulaConnection` 使用 `NebulaResourceLifecycle` 替代本地 `isClosed` 布尔值，`close()` 委托 Task 2 的 `NebulaClientFacade.close()`，关闭后访问消息包含 `Connection`。
- `NebulaStatementImpl` 使用 `NebulaResourceLifecycle` 替代本地 `isClosed` 布尔值，`close()` 幂等并清理 `currentResultSet` 引用，关闭后访问消息包含 `Statement`。
- `NebulaResultSet` 使用 `NebulaResourceLifecycle` 替代本地 `isClosed` 布尔值，`close()` 幂等并只清理内部状态，不关闭外部 `Statement`，关闭后访问消息包含 `ResultSet`。
- 增强 `NebulaResourceLifecycleTest`，覆盖生命周期关闭时机、失败后幂等、Statement 关闭后访问、ResultSet 关闭后访问。

## RED 命令与失败摘要
- Wrapper 尝试：`/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/mvnw test -f /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaResourceLifecycleTest`
  - 结果：无法运行，`fw-plus-core/mvnw` 引用 `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/.mvn/wrapper/maven-wrapper.properties` 与 `maven-wrapper.jar`，当前不存在。
  - 处理：按 brief fallback 使用系统 Maven 3.6.3。
- RED：`cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && mvn test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaResourceLifecycleTest`
  - 结果：BUILD FAILURE。
  - 摘要：`Tests run: 6, Failures: 3, Errors: 0, Skipped: 0`。
  - 预期失败：
    - `closeShouldKeepLifecycleOpenUntilActionReturns`：`expected:<false> but was:<true>`，证明旧实现 action 返回前已标记 closed。
    - `statementShouldRejectAccessAfterClose`：关闭后 `getResultSet()` 未以包含 `Statement` 的 `SQLException` 拒绝访问。
    - `resultSetShouldRejectStatusAccessAfterClose`：关闭后 `isBeforeFirst()` 未抛 `SQLException`。

## GREEN 命令与通过摘要
- GREEN 目标测试：`cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && mvn test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaResourceLifecycleTest`
  - 结果：BUILD SUCCESS。
  - 摘要：`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`。
- 必跑测试：`cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && mvn test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaResourceLifecycleTest,NebulaClientFacadeTest`
  - 结果：BUILD SUCCESS。
  - 摘要：`Tests run: 17, Failures: 0, Errors: 0, Skipped: 0`。
- 扩展测试：`cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && mvn test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaConnectionConfigParserTest,NebulaStatementClassifierTest,NebulaResourceLifecycleTest,NebulaResultSetCursorTest,NebulaClientFacadeTest`
  - 结果：BUILD SUCCESS。
  - 摘要：`Tests run: 36, Failures: 0, Errors: 0, Skipped: 0`。

## 变更文件
- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/lifecycle/NebulaResourceLifecycle.java`
- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaConnection.java`
- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/statement/NebulaStatementImpl.java`
- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaResultSet.java`
- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/lifecycle/NebulaResourceLifecycleTest.java`
- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/.superpowers/sdd/task-3-report.md`

## 风险信号（Comet 风险清单逐项自报）
- Scope 扩散：未发现；仅修改 brief 允许的 Nebula JDBC 生命周期文件、对应测试和报告文件。
- 架构漂移：低；新增统一生命周期 helper 并接入三个 JDBC 资源，未改变 NebulaClientFacade 所有权边界。
- 行为兼容风险：中低；`ResultSet` 的若干状态/元数据访问方法关闭后现在会抛 `SQLException`，符合本 Task 目标，但可能暴露依赖旧宽松行为的调用方。
- 测试替身风险：低；新增测试使用 Mockito mock Nebula 原生 ResultSet，仅用于构造 JDBC ResultSet，不验证 mock 行为。
- 外部依赖风险：低；未使用真实 NebulaGraph/Testcontainers。
- 构建环境风险：中；`./mvnw` 因缺少仓库根 `.mvn/wrapper` 文件无法运行，已按 brief fallback 到系统 Maven 3.6.3；Maven settings 存在既有 warning（unrecognised tag `repository`、重复依赖 warning），测试仍通过。
- 并发风险：未处理；生命周期 helper 不是线程安全实现，本 Task 未要求并发关闭语义。
- Comet 流程风险：未修改 OpenSpec tasks、Comet 检查点或 plan 勾选。
