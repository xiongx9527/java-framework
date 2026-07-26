# Task 2 实施报告

## 状态

DONE

## RED 命令与失败摘要

1. 默认行为和包装支持：

   ```bash
   JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-core/fw-plus-project/fw-plus-graph/pom.xml -pl fw-plus-graph-jdbc -Dtest=GraphJdbcDefaultsTest test
   ```

   结果：预期失败，测试编译报告 `GraphJdbcDefaults` 和 `GraphJdbcWrapperSupport` 不存在。

2. 默认异常和 JDBC 类型推断：

   ```bash
   JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-core/fw-plus-project/fw-plus-graph/pom.xml -pl fw-plus-graph-jdbc -Dtest=GraphJdbcDefaultsTest test
   ```

   结果：预期失败，测试编译报告 `GraphJdbcDefaults.unsupportedOperation()` 和 `jdbcType(...)` 不存在。

3. 生命周期：

   ```bash
   JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-core/fw-plus-project/fw-plus-graph/pom.xml -pl fw-plus-graph-jdbc -Dtest=GraphJdbcLifecycleTest test
   ```

   结果：预期失败，测试编译报告 `GraphJdbcLifecycle` 不存在。

4. RowSet、结果模型和元数据形状：

   ```bash
   JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-core/fw-plus-project/fw-plus-graph/pom.xml -pl fw-plus-graph-jdbc -Dtest=GraphJdbcRowSetsTest,GraphJdbcMetaDataSupportTest test
   ```

   结果：预期失败，测试编译报告 `GraphJdbcColumn`、`GraphJdbcResult`、`GraphJdbcRowSets` 和 `GraphJdbcMetaDataSupport` 不存在。

## GREEN 命令与通过摘要

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-core/fw-plus-project/fw-plus-graph/pom.xml -pl fw-plus-graph-jdbc test
```

结果：`BUILD SUCCESS`；4 个测试类共 11 项测试通过，失败、错误和跳过均为 0。

## 提交哈希

`3758de085ab54359d3b529147c068ccd44876ea8` - `fw-plus-core: 添加图 JDBC 公共基础能力`

## 变更文件

- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcColumn.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcDefaults.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcLifecycle.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcMetaDataSupport.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcResult.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcRowSets.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcWrapperSupport.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcDefaultsTest.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcLifecycleTest.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcMetaDataSupportTest.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcRowSetsTest.java`

## 风险信号自评

- `CachedRowSet` 在 Zulu 8 上拒绝零列 metadata，`emptyGeneratedKeys()` 因而使用仅支持空结果语义的标准 `ResultSet` 代理；测试覆盖了零列和零行合同。
- `CachedRowSet` 按列名而不是 label 解析字符串列访问，公共实现使用代理将 label 映射回列名，保持不同 name/label 时的 JDBC 读取语义。
- 本任务未接入或回归 Neo4j/Nebula 业务模块，符合 Task 2 的范围限制；后续接入任务仍需执行具体驱动测试。

## Reviewer 反馈

### Important
- `GraphJdbcRowSets.emptyGeneratedKeys()` 代理未实现关闭状态：`close()` 是空操作，`isClosed()` 永远返回 false，关闭后 `next()`、`getMetaData()` 等仍可调用。需要使用 `GraphJdbcLifecycle` 或等价状态，关闭后 `isClosed()` 返回 true，除 `isClosed()` 外的方法拒绝使用并抛出 `SQLException`。

## Reviewer Important 修复报告

### RED

命令：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-core/fw-plus-project/fw-plus-graph/pom.xml -pl fw-plus-graph-jdbc -Dtest=GraphJdbcRowSetsTest test
```

新增关闭生命周期测试失败：`emptyGeneratedKeysShouldRejectOperationsAfterClose` 在关闭后调用 `next()` 时未抛出 `SQLException`，证明原实现未执行关闭状态检查。

### GREEN

同一命令复跑通过：`GraphJdbcRowSetsTest` 共 5 项测试，Failures、Errors、Skipped 均为 0，`BUILD SUCCESS`。

### 提交哈希

`47a7af5867a553691819386c527f162c5aa37204` - `fix: enforce generated keys result set lifecycle`

### 变更文件

- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcRowSets.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcRowSetsTest.java`

### 修复内容

`emptyGeneratedKeys()` 使用 `GraphJdbcLifecycle` 管理关闭状态；`close()` 幂等，`isClosed()` 在关闭后返回 true，其他方法在关闭后统一抛出 `SQLException`。测试覆盖关闭前访问、关闭后状态、重复关闭以及 `next()`/`getMetaData()` 的拒绝行为。
