# Task 5 实施报告：Nebula JDBC 保守接入公共模块

## 状态

DONE

## RED 命令与失败摘要

1. `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-core/fw-plus-project/fw-plus-graph/pom.xml -pl fw-plus-graph-jdbc -Dtest=GraphJdbcLifecycleTest,GraphJdbcParameterBindingsTest test`
   - 失败：公共生命周期在 close action 执行前即标记 closed；公共 parameter bindings 缺少保存 `jdbcType` 与 `scaleOrLength` 的 typed binding API。
2. `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-core/fw-plus-project/fw-plus-graph/pom.xml -pl fw-plus-nebula-jdbc -am -DfailIfNoTests=false -Dtest=NebulaUnsupportedJdbcFeatureTest test`
   - 失败：Nebula unsupported 异常仍使用调用栈生成的消息，未委托公共 `GraphJdbcDefaults.unsupportedOperation()`。
3. 初始基线命令在 `fw-plus-graph-jdbc` 模块报告 `No tests were executed`，因此后续 reactor 命令使用 `-DfailIfNoTests=false`；Nebula 测试本身不依赖真实 NebulaGraph 服务。

## GREEN 命令与通过摘要

1. `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-core/fw-plus-project/fw-plus-graph/pom.xml -pl fw-plus-graph-jdbc -Dtest=GraphJdbcLifecycleTest,GraphJdbcParameterBindingsTest test`
   - 通过：7 个测试。
2. `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-core/fw-plus-project/fw-plus-graph/pom.xml -pl fw-plus-nebula-jdbc -am -DfailIfNoTests=false -Dtest=NebulaResourceLifecycleTest,NebulaPreparedStatementBindingsTest,NebulaDatabaseMetaDataTest,NebulaUnsupportedJdbcFeatureTest,NebulaResultSetCursorMovementTest,NebulaJdbcTemplateContractTest,NebulaMyBatisContractTest test`
   - 通过：41 个测试。
3. `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-core/fw-plus-project/fw-plus-graph/pom.xml -pl fw-plus-graph-jdbc test`
   - 通过：20 个测试。
4. `git diff --check`
   - 通过：无空白错误。
5. Nebula 范围检查：`git diff --stat -- .../nebula/jdbc/sql .../nebula/jdbc/mysql`
   - 仅 `NebulaParameterBindings.java` 有变更；未修改 MySQL、SQL AST/parser/query/write/trigger/routine 或 replication。

## 变更文件

- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcLifecycle.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcParameterBindings.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcLifecycleTest.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcParameterBindingsTest.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/NebulaDatabaseMetaData.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/NebulaParameterMetaData.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/lifecycle/NebulaResourceLifecycle.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/sql/NebulaParameterBindings.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/utils/ExceptionBuilder.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/utils/JdbcWrapperSupport.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/lifecycle/NebulaResourceLifecycleTest.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/statement/NebulaUnsupportedJdbcFeatureTest.java`

## 实施说明

- `NebulaResourceLifecycle` 以兼容 facade 形式委托公共生命周期。公共 lifecycle 调整为在 close action 完成或抛出 `SQLException` 后标记 closed，保持 Nebula 原有关闭时序。
- `NebulaParameterBindings` 将 typed binding 委托给公共容器，并在 Nebula 边界还原 `NebulaParameterBinding`，保留 value、JDBC type、scale/length 及 copy 的 batch 快照语义。
- `NebulaParameterMetaData` 仅委托无协议的参数计数、nullable/mode 默认行为；动态 class/type/precision/scale 继续由 Nebula typed binding 计算。
- `NebulaDatabaseMetaData` 仅委托空 catalog 和固定 `TAG`/`EDGE` table types；根据 scout 保守边界，`getSchemas()`、`getColumns()`、`getPrimaryKeys()`、`getIndexInfo()` 的 Nebula 语义保持原实现。
- `ExceptionBuilder` 改为公共 unsupported 异常，异常类型仍为 `SQLFeatureNotSupportedException`。旧测试中偶然依赖调用栈消息包含 `NebulaConnection` 的断言已收敛为 `SQLException` 类型断言。

## 风险信号自评

低。公共模块未引入具体图数据库、Druid、Spring 或 MyBatis 依赖；Nebula 原生执行器、ResultSet 映射、Driver URL/类名、服务声明及协议路径均未改动。公共 lifecycle 的关闭时序与 Nebula 既有语义已由本地测试覆盖。

## 仍需关注点

- unsupported 异常消息由调用栈描述统一为 `Unsupported JDBC feature`；异常类型保持不变，但任何依赖旧消息文本的外部调用方需要自行确认。
- 本任务仅覆盖本地 fake executor 与框架契约测试，未连接真实 NebulaGraph 服务，符合任务验收边界。

## 提交哈希

a9cd12dc369d78c7c2ed9efd2eb0c3f7ad48c620（实现提交）。

## Task 5 Fixer 补充报告

### 状态

DONE

### TDD 与修复说明

1. 现有 `NebulaResourceLifecycleTest` 的关闭连接 accessor 辅助断言只验证 `SQLException` 类型，未验证生命周期传入的 `"Connection"` resourceName。恢复消息断言后，`rollback(Savepoint)` 暴露为不经过 `checkClosed()` 的明确 unsupported 操作；因此将其移出生命周期 accessor 集合，避免把公共 unsupported 异常的固定消息误当作资源关闭消息。其余实际调用 `checkClosed()` 的 connection accessor 均断言消息包含 `"Connection"`。
2. 首次图聚合验证发现 `NebulaJdbcComplianceTest.unsupportedUnwrapShouldThrowSqlException` 失败：Task 5 直接委托公共 wrapper 后，将 Nebula 原有 `Cannot unwrap ...` 失败消息变成 `No wrapped object ...`。`JdbcWrapperSupport` 现在在非空目标接口不匹配时保留 Nebula 的历史消息；匹配和空接口路径继续由 `GraphJdbcWrapperSupport` 委托处理。
3. `DirectNebulaSqlExecutorTest` 已覆盖 typed `NebulaParameterBindings` 从执行器渲染边界进入 SQL 的路径；本次将该类加入定向验收命令，实际执行 17 项测试。

### Maven Wrapper 与系统 Maven 证据

1. 在 `fw-plus-core` 先执行：

```bash
./mvnw -pl fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc -am -Dtest=NebulaResourceLifecycleTest,NebulaPreparedStatementBindingsTest,NebulaDatabaseMetaDataTest,NebulaUnsupportedJdbcFeatureTest,NebulaResultSetCursorMovementTest,DirectNebulaSqlExecutorTest,NebulaJdbcTemplateContractTest,NebulaMyBatisContractTest test
```

失败于环境：`.mvn/wrapper/maven-wrapper.properties` 与 `.mvn/wrapper/maven-wrapper.jar` 缺失，随后 `MavenWrapperMain` 类找不到。该失败发生在 Maven 启动前，不是代码或测试失败。

2. 根据本任务已确认的回退规则，使用 Zulu 8 系统 Maven 执行：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-core/fw-plus-project/fw-plus-graph/pom.xml -pl fw-plus-nebula-jdbc -am -DfailIfNoTests=false -Dtest=NebulaResourceLifecycleTest,NebulaPreparedStatementBindingsTest,NebulaDatabaseMetaDataTest,NebulaUnsupportedJdbcFeatureTest,NebulaResultSetCursorMovementTest,DirectNebulaSqlExecutorTest,NebulaJdbcTemplateContractTest,NebulaMyBatisContractTest test
```

通过：58 个测试，0 failures，0 errors，0 skipped；包含 `DirectNebulaSqlExecutorTest` 的 17 个测试。

3. 图 JDBC 聚合验证：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-core/fw-plus-project/fw-plus-graph/pom.xml test
```

首次 RED：`NebulaJdbcComplianceTest.unsupportedUnwrapShouldThrowSqlException` 断言旧 Nebula unwrap 消息失败，Neo4j reactor 因此跳过。完成 facade 兼容修复后 GREEN：公共图 JDBC 20 个测试、Nebula 244 个测试（2 个真实服务 smoke 跳过）、Neo4j 27 个测试（3 个真实服务 smoke 跳过）均通过；0 failures，0 errors。

### 风险信号

- Maven Wrapper 仍缺少 `.mvn/wrapper/*`，当前工作树必须以指定的 Zulu 8 系统 Maven 回退执行验证。
- 真实 NebulaGraph 与 Neo4j 服务 smoke 测试按既有条件跳过；本次未连接真实服务。
