# Task 6 实施报告：普通 DML 拦截器和 H2 纵向闭环

## 状态

- 完成 `Executor.update`、`StatementHandler.prepare`、`StatementHandler.update` 三层真实 MyBatis 调用闭环。
- `prepare` 先执行 `proceed()`，随后从实际 `StatementHandler` 读取最终 `BoundSql`，并使用实际 JDBC Connection 做方言选择、metadata 和 before snapshot。
- 普通单 SQL 统一使用 `List<StatementChangePlan>` / `List<StatementChange>`，未把接口限制为单语句结构。

## RED / GREEN

### 首轮 RED

命令：

```bash
mvn -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mybatis-datachange \
  -Dtest=DataChangeInterceptorH2Test,DataChangeExecutionContextHolderTest test
```

结果：测试编译失败，缺少 `DataChangeInterceptor`、`DataChangeEngine`、执行上下文/Holder，以及精确错误码，符合 Task 6 尚未实现的预期。

### 补充 RED / GREEN

- 真实 MyBatis 单列 INSERT 生成 `VALUES(?)`，暴露已有 SQL 分析仅接受 `RowConstructor` 的缺口；新增最小方言测试先失败，再扩展单行单列表达式支持后通过。
- MyBatis `ConnectionLogger` 会记录内部快照绑定值；新增真实 H2 日志测试先观察到 `ACTIVE(String)`，随后在 `JdbcSnapshotExecutor` 内仅解包 MyBatis 日志代理、复用同一底层事务连接，测试通过。

### 最终 GREEN

定向命令同上，结果：18 tests，0 failures，0 errors，0 skipped，BUILD SUCCESS。

模块全量：

```bash
mvn -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mybatis-datachange test
```

结果：112 tests，0 failures，0 errors，0 skipped，BUILD SUCCESS。

## 真实 H2 行为

- INSERT：读取 MyBatis 回填 generated key，并按 identity 查询数据库默认值。
- UPDATE：before/after 值、RowIdentity 正确；更新后行离开原 WHERE 时仍按更新前捕获 identity 查询。
- DELETE：物理删除由 before snapshot 生成；配置删除列时识别逻辑删除。
- 复合键：`TENANT_ID + USER_ID` 顺序和值保持正确。
- 零影响行、未监听表不产生事件；`maxRows + 1` 在原 DML 前失败且数据不变。
- DML 本身异常不产生事件，外层上下文最终清理。

## 事务与非事务证据

- `JdbcTransactionStateResolver` 仅按实际 Connection 的 `getAutoCommit()` 判断：false 为 `TRANSACTIONAL`，true 为 `BEST_EFFORT`。
- 手工事务 before snapshot 使用 `FOR UPDATE`；handler 或 after snapshot 失败向 Mapper 抛出，显式 rollback 后业务值保持原值。
- auto-commit 下同类后置失败保留 Mapper 影响行数与已提交业务值，仅向 ErrorHandler 报告精确失败码；after snapshot 失败不向 Handler 分发事件。
- 前置 snapshot 超限在两种模式下均 fail-closed。

## 前置契约修正

- 删除同义旧码 `SNAPSHOT`、`HANDLER`，全量替换为 `SNAPSHOT_QUERY_FAILED`、`HANDLER_FAILED`。
- 保留 `SNAPSHOT_LIMIT_EXCEEDED`、`GENERATED_KEY_UNAVAILABLE` 等独立错误码。
- `DataChangeFailure#getCode()` 提供失败码读取契约；全模块回归覆盖 Task 3-5。

## ThreadLocal 与安全边界

- `DataChangeExecutionContextHolder` 使用栈式 ThreadLocal，嵌套 scope 关闭后恢复外层，最外层关闭时调用 `remove()`。
- Executor 异常、DML 异常、零影响行、handler 内嵌套 Mapper 异常均由 `try/finally`/scope 保证清理。
- Snapshot、Handler、ErrorHandler 均在递归抑制 scope 中执行。
- 内部异常转换为固定安全消息，不附带 SQL 绑定值；内部快照绕过 MyBatis JDBC 日志代理，避免参数和结果行进入 MyBatis DEBUG 日志。

## 范围

- 新增 brief 指定的 6 个 Task 6 生产类、H2/Holder 测试和 Mapper 夹具。
- 最小前置修改限于精确错误码、H2 metadata 大小写、单列 INSERT AST、快照日志代理解包及对应回归测试。
- 未实现 Batch、多语句和多表执行扩展，留给 Task 7。

---

# 修复轮 1/5：Handler 生命周期与 prepare 资源边界

## Finding 1：Handler 生命周期统一失败处理

### 根因

- `DataChangeDispatcher` 原先在 suppression scope 和 `try/catch` 外调用 `getOrder()` 与 `supports()`。
- 两个生命周期方法中的嵌套 Mapper 会重新进入数据变更拦截器；方法异常也不会遵循 Handler 已约定的事务级别语义。
- `BEST_EFFORT` 无法安全报告后继续其他 Handler，`TRANSACTIONAL` 也无法统一转换为净化后的 `HANDLER_FAILED`。

### 新增回归测试

- `DataChangeDispatcherTest#bestEffortSupportsFailureIsSanitizedReportedAndDoesNotStopLaterHandlers`
- `DataChangeDispatcherTest#transactionalSupportsFailurePropagatesSanitizedHandlerFailure`
- `DataChangeDispatcherTest#invokesOrderAndSupportsInsideSuppressionScope`
- `DataChangeDispatcherTest#bestEffortOrderFailureIsReportedAndSkipsOnlyFailingHandler`
- `DataChangeInterceptorH2Test#suppressesNestedMapperCallFromHandlerSupports`

### RED / GREEN

Dispatcher 首轮 RED：8 tests 中 2 failures、2 errors；真实 H2 嵌套 Mapper RED：期望 1 个审计事件，实际 2 个。

修复后命令：

```bash
mvn -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mybatis-datachange \
  -Dtest=DataChangeDispatcherTest,DataChangeInterceptorH2Test#suppressesNestedMapperCallFromHandlerSupports test
```

结果：9 tests，0 failures，0 errors，BUILD SUCCESS。

实现将 `getOrder()`、`supports()`、`handle()` 全部纳入 `DataChangeScope.suppress()` 与统一失败转换；排序只使用已安全读取的整数 order。`BEST_EFFORT` 报告净化后的 `HANDLER_FAILED` 并继续，`TRANSACTIONAL` 直接抛出净化异常。

## Finding 2：prepare 后审计失败关闭 Statement

### 根因

MyBatis 在 `StatementHandler.prepare()` 返回后才把 Statement 赋给外层 `SimpleExecutor#doUpdate` 的局部变量。当前拦截器先取得底层返回值，再执行 before snapshot；若审计准备失败，拦截器抛出异常导致外层赋值未完成，MyBatis 的 `finally` 看到的 Statement 仍为 `null`，因此不能关闭已创建的 JDBC 资源。

### 新增回归测试

- `DataChangeInterceptorPrepareTest#closesPreparedStatementWhenBeforeSnapshotFailsWithoutLeakingCloseFailure`

测试使用真实 H2 Connection、真实 `DataChangeEngine` 和真实 before snapshot 查询路径；`StatementHandler.prepare()` 返回测试 Statement 后，故意让 before snapshot 产生 `SNAPSHOT_QUERY_FAILED`。Statement 的 `close()` 同时抛出包含 `secret-binding` 的 `SQLException`，用于验证关闭动作与异常隔离。

首轮 RED 命令：

```bash
mvn -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mybatis-datachange \
  -Dtest=DataChangeInterceptorPrepareTest test
```

结果：1 test，1 failure；期望 close 调用 1 次，实际 0 次。原始 `SNAPSHOT_QUERY_FAILED` 的错误码、固定消息、无 cause 和无 suppressed 断言均已先通过。

修复后同命令结果：1 test，0 failures，0 errors。`interceptPrepare` 仅在底层 prepare 已返回 Statement 且后续审计准备失败时显式关闭；suppression 或无执行上下文的直通路径不关闭。close 自身的任何失败被完全隔离，不替换原异常，也不进入 cause / suppressed，敏感绑定值不会泄漏。

## 最终验证

定向回归：

```bash
mvn -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mybatis-datachange \
  -Dtest=DataChangeDispatcherTest,DataChangeInterceptorPrepareTest,DataChangeInterceptorH2Test test
```

结果：26 tests，0 failures，0 errors，0 skipped，BUILD SUCCESS。

模块全量：

```bash
mvn -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mybatis-datachange test
```

结果：118 tests，0 failures，0 errors，0 skipped，BUILD SUCCESS。

`git diff --check` 通过；修复范围限于 `DataChangeDispatcher`、`DataChangeInterceptor` 及对应三个测试类。
