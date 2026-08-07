# Task 7 实施报告：JDBC Batch、多语句和多表聚合

## 状态

- 完成 Task 7，未实现 Task 8-12。
- 变更仅位于公共 `fw-plus-mybatis-datachange` 模块及本报告。
- 未引入 Plus、Flex、Spring 或 `MpBase` 依赖。
- 使用系统 `mvn`，未使用缺失 wrapper 文件的 `mvnw`。

## 实现

### Batch 生命周期

- `DataChangeInterceptor` 增加 brief 要求的 `StatementHandler.batch`、`Executor.flushStatements/commit/rollback/query` 六组签名。
- `StatementHandler.batch` 在真实 `addBatch()` 前，用当前 Handler 的最终 `BoundSql` 和参数读取 before；复用 PreparedStatement 时仍为每次参数重新建计划，没有把 Batch 降级为普通 update。
- `DataChangeBatchCoordinator` 以 Executor 对象身份维护待处理状态，并按 MyBatis `BatchResult` 的 MappedStatement + SQL 分组对齐实际 flush。
- commit/query 在存在待处理 Batch 时显式先调用一次 `flushStatements()`，完成 after、Diff 和派发后再进入原方法；原方法内部第二次 flush 为空。
- rollback 在原方法执行前只 discard，不读取 after、不派发事件。
- 一次实际 flush 统一派发一个 `DataChangeEvent`；跨多个 BatchResult 分组仍为一个事件。
- 同一 `RowIdentity` 只保留批次第一次 before 和实际 flush 后最终 after；复合主键按值语义去重。
- Batch INSERT 在实际 flush 后使用 MyBatis 已回填的生成键读取 after。
- `SUCCESS_NO_INFO` 保持 `-2`；`EXECUTE_FAILED` 对应 BatchResult 不生成成功变更；多条正数 count 求和作为 Statement 层 `affectedRows`。

### 多语句与多表

- `JdbcUpdateCountCollector` 按 JDBC `getMoreResults()/getUpdateCount()` 顺序收集更新计数，可跨过 ResultSet，保留 `SUCCESS_NO_INFO`。
- `DataChangeEngine` 按 `statementIndex` 捕获 count 和身份，并在 Statement 层输出 `statementIndex/affectedRows`。
- CALL 保持 non-auditable；CALL 在前、DML 在后时只生成 DML 的 `StatementChange`，索引不压缩。
- 同一 SQL 的多个目标表聚合进一个 `StatementChange`，每表各有 `TableChange`，影响行数只保存在 Statement 层。
- 多目标 DELETE 快照保留原 DML 根表作为 snapshot source，避免第二目标表错误地作为 JOIN 根表导致快照 SQL 无效。
- `RowChange#getBefore()/getAfter()` 提供按列名组织的只读值视图。

### 安全边界

- 新增代码不记录 SQL、绑定参数或异常 cause；内部错误仅使用固定错误标识。
- JDBC update-count 读取失败、Coordinator 对齐失败和 after 失败继续复用 Task 6 的 TRANSACTIONAL/BEST_EFFORT 安全边界。
- BEST_EFFORT 不把 flush 后的审计失败伪装成业务失败；TRANSACTIONAL 处理器失败可阻止 commit。
- 快照参数继续通过既有 TypeHandler/JDBC binder 绑定。
- 默认快照上限与只监听显式配置表的行为未改变。

## TDD 证据

### RED

命令：

```bash
mvn -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mybatis-datachange \
  -Dtest=DataChangeBatchH2Test,DataChangeMultiStatementTest,DataChangeMultiTableTest,JdbcUpdateCountCollectorTest test
```

关键输出：

```text
[ERROR] COMPILATION ERROR
[ERROR] 找不到符号: 方法 getBefore()/getAfter()
[ERROR] 找不到符号: 类 JdbcUpdateCountCollector
[ERROR] 找不到符号: 方法 captureStatementResults(...)
[ERROR] 找不到符号: 方法 getStatementIndex()/getAffectedRows()
[INFO] 14 errors
[INFO] BUILD FAILURE
```

测试先于生产实现写入；失败原因均为 Task 7 API/行为尚不存在。

### 调试中的有效失败

第一段 GREEN 后，多语句与 Collector 6 个测试通过，多表测试稳定失败：

```text
Tests run: 7, Failures: 0, Errors: 1
DataChangeMultiTableTest.multiTableStatementKeepsOneStatementAndTwoTables
DataChangeException: SNAPSHOT_QUERY_FAILED
```

根因是多目标 DELETE 的第二目标快照以目标表而非原 DELETE 根表构造 `FROM`，却复用原 JOIN/WHERE。修复 `ChangeTarget.snapshotSourceTable` 后该测试通过。

### GREEN

Task 7 定向集合：

```bash
mvn -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mybatis-datachange \
  -Dtest=DataChangeBatchH2Test,DataChangeMultiStatementTest,DataChangeMultiTableTest,JdbcUpdateCountCollectorTest test
```

```text
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Task 6 H2 回归：

```bash
mvn -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mybatis-datachange -Dtest=DataChangeInterceptorH2Test test
```

```text
Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

模块全量最终验证：

```bash
mvn -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mybatis-datachange test
```

```text
Tests run: 134, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 文件范围

生产代码新增：

- `interceptor/BatchChangePlan.java`
- `interceptor/DataChangeBatchCoordinator.java`
- `interceptor/JdbcUpdateCountCollector.java`

生产代码修改：

- `api/RowChange.java`
- `api/StatementChange.java`
- `dialect/AbstractJSqlParserDataChangeDialect.java`
- `dialect/ChangeTarget.java`
- `dialect/H2DataChangeDialect.java`
- `interceptor/DataChangeEngine.java`
- `interceptor/DataChangeExecutionContext.java`
- `interceptor/DataChangeInterceptor.java`

测试新增：

- `interceptor/DataChangeBatchH2Test.java`
- `interceptor/DataChangeMultiStatementTest.java`
- `interceptor/DataChangeMultiTableTest.java`
- `interceptor/JdbcUpdateCountCollectorTest.java`

测试 fixture 修改：

- `interceptor/fixture/TestUserMapper.java`
- `interceptor/fixture/TestUserMapper.xml`

## 自审

- 精确核对 brief 所列拦截签名，全部存在且参数类型一致。
- Batch 仍执行 `PreparedStatement.addBatch()/executeBatch()`，未改写为逐条 update。
- 检查 `git diff --check` 无空白错误。
- 检查生产源码无 `com.baomidou`、`com.mybatisflex`、`org.springframework`、`MpBase` 引用。
- 检查新增异常文本均为固定标识，不包含 SQL、参数或底层异常内容。
- 检查一次 flush 的多分组只派发一个事件，rollback 不派发，CALL 不生成空 Statement。

## 顾虑

- H2 2.1.214 对分号多语句只暴露第一个 update count，因此多语句 Statement 分组使用真实 Engine/H2 快照与显式 JDBC counts 验证；`JdbcUpdateCountCollector` 的完整 `getMoreResults()` 链使用 JDBC Statement mock 覆盖。MySQL 等能返回完整结果链的驱动仍建议在后续集成环境补一条真实驱动测试。
- 测试运行时 MyBatis 自身 DEBUG 日志会打印 fixture 参数；Task 7 新增生产代码没有新增日志，也不会把参数写入 SQL、异常或日志。
